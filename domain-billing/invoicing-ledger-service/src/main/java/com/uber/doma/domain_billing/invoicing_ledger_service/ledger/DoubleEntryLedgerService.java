package com.uber.doma.domain_billing.invoicing_ledger_service.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoubleEntryLedgerService {
    private static final Logger log = LoggerFactory.getLogger(DoubleEntryLedgerService.class);

    public List<JournalEntry> recordTripSettlement(String tripId, long riderChargeCents, long driverPayoutCents, long uberPlatformFeeCents) {
        if (riderChargeCents != (driverPayoutCents + uberPlatformFeeCents)) {
            throw new IllegalArgumentException("Accounting Invariant Violated: Debits must equal Credits");
        }

        List<JournalEntry> entries = List.of(
            new JournalEntry("RIDER_RECEIVABLE", LedgerEntryType.DEBIT, riderChargeCents, "Rider fare charged for trip " + tripId),
            new JournalEntry("DRIVER_PAYABLE", LedgerEntryType.CREDIT, driverPayoutCents, "Driver earnings for trip " + tripId),
            new JournalEntry("UBER_REVENUE_TAKE_RATE", LedgerEntryType.CREDIT, uberPlatformFeeCents, "Platform commission for trip " + tripId)
        );

        log.info("[InvoicingLedger] Recorded balanced journal entry for Trip {}: Total Debits=${} == Total Credits=${}",
            tripId, riderChargeCents / 100.0, (driverPayoutCents + uberPlatformFeeCents) / 100.0);

        return entries;
    }
}
