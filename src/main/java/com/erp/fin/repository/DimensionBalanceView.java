package com.erp.fin.repository;

import java.math.BigDecimal;

/**
 * QR-FIN-044 — the read-only projection behind API-FIN-032. One row per account AND dimension
 * value, which is the grouping POL-FIN-011 requires; never one row per base account.
 */
public interface DimensionBalanceView {

    Long getAccountId();

    String getAccountCode();

    String getAccountNameAr();

    String getAccountNameEn();

    String getNatureCode();

    Long getDimensionId();

    Long getDimensionValueId();

    String getDimensionValueCode();

    String getDimensionValueNameAr();

    String getDimensionValueNameEn();

    BigDecimal getDebitTotal();

    BigDecimal getCreditTotal();
}
