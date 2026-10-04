package com.erp.fin.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * QR-FIN-042 — the read-only projection behind API-FIN-028's account ledger. A.2.8: a multi-table
 * read (FIN_JOURNAL_LINE joined to FIN_JOURNAL_ENTRY) returns a projection, never full entities.
 *
 * <p>It deliberately carries no running balance: the query returns the raw POSTED lines and the
 * running total is accumulated in order afterwards, so no balance is ever read from storage
 * (POL-FIN-009).
 */
public interface AccountLedgerLineView {

    Long getJournalEntryId();

    String getDocNo();

    LocalDate getDocDate();

    String getJournalTypeCode();

    String getEventReference();

    Long getJournalLineId();

    Integer getLineNo();

    BigDecimal getAmount();

    String getDirectionCode();

    String getDescriptionAr();

    String getDescriptionEn();
}
