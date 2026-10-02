package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end chaos test: spins up a real TCP "audit upstream" inside a
 * Docker container, routes it through a Toxiproxy container, then drives
 * the {@link ResilientAuditSink} through the proxied connection while
 * injecting:
 *
 * <ul>
 *   <li>500ms latency ({@code ToxicType.LATENCY})</li>
 *   <li>20% dropped connections ({@code ToxicType.RESET_PEER} with toxicity)</li>
 *   <li>A subsequent 30% sustained loss to verify the booking SLA boundary</li>
 * </ul>
 *
 * <p>The Java Toxiproxy client only exposes {@code resetPeer} for
 * connection-level faults (no native "packet loss" toxic in this client
 * library), so we use {@code resetPeer} with varying toxicity to
 * simulate dropped connections and {@code latency} for the latency
 * scenario. The client library does not expose a "packetLoss" toxic
 * type — Toxiproxy server supports it natively but the Java client
 * wraps only the toxic types enumerated in
 * {@code eu.rekawek.toxiproxy.model.ToxicType}.</p>
 *
 * <p>The test is automatically skipped on hosts without Docker via
 * {@link ToxiproxyAvailability}. On hosts with Docker, the suite
 * verifies that the real fallback layer survives real network faults.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToxiproxyAuditSinkIntegrationTest {

    /** Image containing a tiny static "audit" server. Alpine + netcat in
     *  listen mode is sufficient for a TCP smoke-test; production wiring
     *  would point Toxiproxy at the real Kafka/gRPC upstream. */
    private static final DockerImageName ECHO_IMAGE =
            DockerImageName.parse("alpine:3.19");

    /** Toxiproxy 2.9.x — last release line that supports the LATENCY and
     *  RESET_PEER toxics we need. Newer lines are also fine. */
    private static final DockerImageName TOXIPROXY_IMAGE =
            DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.9.0");

    /** Shared Docker network so Toxiproxy and the audit upstream can
     *  resolve each other by container name. */
    private static final Network SHARED = Network.newNetwork();

    private ToxiproxyContainer toxiproxy;
    private GenericContainer<?> auditUpstream;
    private ToxiproxyClient toxiproxyClient;
    private ToxiproxyContainer.ContainerProxy proxy;

    @BeforeAll
    void startContainers() throws IOException {
        // Note: we intentionally do NOT call assumeDockerAvailable() here.
        // The @BeforeEach hook below ensures each test method is reported
        // as "skipped" rather than as a class-level abort. The container
        // start is a no-op when Docker is unavailable because the @BeforeEach
        // assumption short-circuits the test execution.
        if (!ToxiproxyAvailability.isDockerAvailable()) {
            return;
        }

        // Start a tiny "audit upstream" that always responds with a fixed
        // payload. Netcat in listen mode is good enough: it accepts the
        // connection, reads a line, writes back a fixed response.
        auditUpstream = new GenericContainer<>(ECHO_IMAGE)
                .withNetwork(SHARED)
                .withNetworkAliases("audit-upstream")
                .withCommand("sh", "-c",
                        "while true; do "
                                + "echo -e 'audit-ack\\r\\n' | nc -l -p 9099; "
                                + "done")
                .withExposedPorts(9099)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));
        auditUpstream.start();

        // Spin up Toxiproxy. We disable the container-managed proxy
        // creation (`getProxy(upstream, 9099)` is not available in 2.9.0
        // for arbitrary containers, only for known services), so we
        // create the proxy manually and point it at the upstream host.
        toxiproxy = new ToxiproxyContainer(TOXIPROXY_IMAGE)
                .withNetwork(SHARED);
        toxiproxy.start();
        toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        // Container-managed proxy: Testcontainers wires the listen port
        // back to the audit upstream container's port automatically, and
        // exposes a ContainerProxy object that knows both endpoints.
        proxy = toxiproxy.getProxy(auditUpstream, 9099);
    }

    @AfterAll
    void stopContainers() {
        if (toxiproxy != null) {
            toxiproxy.stop();
        }
        if (auditUpstream != null) {
            auditUpstream.stop();
        }
        SHARED.close();
    }

    @Test
    @DisplayName("500ms latency injected via Toxiproxy — sink still responds")
    void latencyToxicDoesNotBreakSink() throws IOException {
        ToxiproxyAvailability.assumeDockerAvailable();
        resetAllToxics();
        proxy.toxics().latency("latency-500", ToxicDirection.DOWNSTREAM, 500);

        final long start = System.nanoTime();
        try (Socket socket = new Socket(proxy.getContainerIpAddress(), proxy.getProxyPort())) {
            socket.getOutputStream().write("ping\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            socket.setSoTimeout(5_000);
            final byte[] buf = new byte[64];
            final int read = socket.getInputStream().read(buf);
            final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            assertTrue(read > 0, "echo server must respond");
            assertTrue(elapsedMs >= 400,
                    "expected latency >=400ms (configured 500), got " + elapsedMs + "ms");
        }
    }

    @Test
    @DisplayName("20% packet loss via Toxiproxy — fallback keeps bookings alive")
    void packetLossToxicKeepsBookingsAlive() throws IOException {
        ToxiproxyAvailability.assumeDockerAvailable();
        resetAllToxics();

        // resetPeer toxic closes the connection immediately with toxicity
        // 0.2 (~20% of connections are dropped at the proxy level). This
        // models the "20% dropped connections" requirement.
        proxy.toxics().resetPeer("resetPeer-20", ToxicDirection.DOWNSTREAM, 0)
                .setToxicity(20.0f);

        final UpstreamAuditChannel tcpChannel = new TcpUpstreamAuditChannel(
                proxy.getContainerIpAddress(), proxy.getProxyPort(), 2_000);
        final ResilientAuditSink sink = new ResilientAuditSink(tcpChannel, 1_000);
        final BookingSlaCoordinator coordinator = new BookingSlaCoordinator(sink);

        final int iterations = 100;
        for (int i = 0; i < iterations; i++) {
            final BookingSlaCoordinator.BookingResult result =
                    coordinator.executeBooking("booking-" + i, "rider-" + i);
            // The fallback degradation strategy must preserve the booking SLA:
            // every booking is accepted, even when 20% of audit writes are
            // dropped at the network layer.
            assertNotNull(result);
            assertTrue(result.outcome() != BookingSlaCoordinator.BookingOutcome.REJECTED,
                    "booking must never be rejected because of upstream audit faults");
        }
        assertEquals(iterations, coordinator.getBookingsAccepted());
    }

    @Test
    @DisplayName("30% sustained packet loss — booking SLA boundary")
    void sustainedLossAtBoundaryPreservesSla() throws IOException {
        ToxiproxyAvailability.assumeDockerAvailable();
        resetAllToxics();
        proxy.toxics().resetPeer("resetPeer-30", ToxicDirection.DOWNSTREAM, 0)
                .setToxicity(30.0f);
        proxy.toxics().latency("latency-500", ToxicDirection.DOWNSTREAM, 500);

        final UpstreamAuditChannel tcpChannel = new TcpUpstreamAuditChannel(
                proxy.getContainerIpAddress(), proxy.getProxyPort(), 4_000);
        final ResilientAuditSink sink = new ResilientAuditSink(tcpChannel, 5_000);
        final BookingSlaCoordinator coordinator = new BookingSlaCoordinator(sink);

        final int iterations = 50;
        for (int i = 0; i < iterations; i++) {
            try {
                final BookingSlaCoordinator.BookingResult result =
                        coordinator.executeBooking("booking-" + i, "rider-" + i);
                assertNotNull(result);
                assertTrue(result.outcome() != BookingSlaCoordinator.BookingOutcome.REJECTED,
                        "booking SLA violated at 30% packet loss");
            } catch (final RuntimeException ex) {
                fail("unhandled exception under 30% packet loss: " + ex.getMessage());
            }
        }
        assertEquals(iterations, coordinator.getBookingsAccepted());
    }

    /** Deletes every toxic currently attached to the proxy. */
    private void resetAllToxics() throws IOException {
        for (final Toxic toxic : proxy.toxics().getAll()) {
            toxic.remove();
        }
    }

    /**
     * Helper: minimal TCP-based upstream channel for the integration test.
     * Connects to the Toxiproxy-fronted audit server, sends the event id,
     * reads the "audit-ack" response and treats an IOException as a network
     * failure (which the {@link ResilientAuditSink} must absorb).
     */
    private static final class TcpUpstreamAuditChannel implements UpstreamAuditChannel {

        private final String host;
        private final int port;
        private final int timeoutMillis;

        TcpUpstreamAuditChannel(final String host, final int port, final int timeoutMillis) {
            this.host = host;
            this.port = port;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public void persist(final AuditEvent event) {
            try (Socket socket = new Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), timeoutMillis);
                socket.setSoTimeout(timeoutMillis);
                socket.getOutputStream().write(
                        (event.getId() + "\n").getBytes(StandardCharsets.UTF_8));
                socket.getOutputStream().flush();
                final byte[] buf = new byte[64];
                final int read = socket.getInputStream().read(buf);
                if (read <= 0) {
                    throw new SimulatedPartitionException(
                            "upstream closed the connection without responding");
                }
            } catch (final IOException ex) {
                throw new SimulatedPartitionException(
                        "TCP write to audit upstream failed: " + ex.getMessage());
            }
        }

        @Override
        public java.util.List<AuditEvent> read(final String actorId) {
            // Read path is exercised by the local fallback test, not the
            // network-level Toxiproxy integration. Returning an empty list
            // keeps the integration test focused on the write path.
            return java.util.Collections.emptyList();
        }
    }
}