package com.erp.fin.repository;

import java.math.BigDecimal;

/**
 * QR-FIN-043 — the read-only projection behind the ONE aggregation shared by API-FIN-029 (trial
 * balance), API-FIN-030 (balance sheet) and API-FIN-031 (income statement). A.2.8: a grouped
 * multi-table read returns a projection, never full entities.
 *
 * <p>Debit and credit totals are returned separately and unsigned; turning them into the
 * account's normal-side presentation is POL-FIN-002 and belongs to the mapper, not the query.
 */
public interface AccountBalanceView {

    Long getAccountId();

    String getAccountCode();

    String getAccountNameAr();

    String getAccountNameEn();

    String getAccountTypeCode();

    String getNatureCode();

    BigDecimal getDebitTotal();

    BigDecimal getCreditTotal();
}
