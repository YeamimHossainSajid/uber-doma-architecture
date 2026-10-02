package com.uber.doma.domain_platform.audit_compliance_service.resilience;

import org.junit.jupiter.api.Assumptions;

import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility used by the Toxiproxy-backed integration tests to skip themselves
 * when Docker is not available on the build host. Skipping (rather than
 * failing) is the right behavior for chaos integration tests because the
 * build must continue to pass on developer laptops and CI runners that
 * don't have a Docker daemon.
 */
final class ToxiproxyAvailability {

    private ToxiproxyAvailability() {
    }

    /**
     * Checks whether Docker is reachable. Uses two cheap heuristics so the
     * check itself runs in milliseconds and doesn't interfere with the rest
     * of the build:
     *
     * <ol>
     *   <li>The {@code DOCKER_HOST} environment variable is set, OR</li>
     *   <li>The classic {@code /var/run/docker.sock} socket exists, OR</li>
     *   <li>A TCP probe to {@code 127.0.0.1:2375} succeeds (Docker Desktop).</li>
     * </ol>
     */
    static boolean isDockerAvailable() {
        final String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.isBlank()) {
            return true;
        }
        if (Files.exists(Path.of("/var/run/docker.sock"))
                || Files.exists(Path.of("C:/var/run/docker.sock"))) {
            return true;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 2375), 250);
            return true;
        } catch (final Exception ex) {
            return false;
        }
    }

    /**
     * Calls {@link Assumptions#assumeTrue(boolean)} so JUnit Jupiter reports
     * the test as <em>skipped</em> when Docker is unavailable.
     */
    static void assumeDockerAvailable() {
        Assumptions.assumeTrue(isDockerAvailable(),
                "Docker is not available; skipping Toxiproxy integration test.");
    }
}