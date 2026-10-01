package com.uber.doma.domain_billing.invoicing_ledger_service.ledger;

public record JournalEntry(
    String accountId,
    LedgerEntryType type,
    long amountCents,
    String description
) {}
