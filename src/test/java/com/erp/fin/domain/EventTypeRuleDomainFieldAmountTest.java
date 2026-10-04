package com.erp.fin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.exception.LocalizedException;
import com.erp.fin.exception.FinErrorCodes;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * FIELD-sourced rule lines (API-FIN-020). Found by the fin-consumer E2E run 2026-09-24: an absent
 * field silently became ZERO and died on CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE as an unlocalized
 * DATA_INTEGRITY_VIOLATION, and a fully paid PURCHASE_INVOICE (remainingAmount = 0, all 50 in
 * 2026) could never post.
 */
class EventTypeRuleDomainFieldAmountTest {

    private static final String FIELD = "FIELD";
    private static final BigDecimal BASE = new BigDecimal("100");

    @Test
    void aPresentFieldIsTakenAsIs() {
        assertThat(EventTypeRuleDomain.sourcedAmount(FIELD, "paidAmount", BASE,
            Map.of("paidAmount", new BigDecimal("40.5000")))).isEqualByComparingTo("40.5");
    }

    @Test
    void anAbsentFieldIsNamedNotZero() {
        assertThatThrownBy(() -> EventTypeRuleDomain.sourcedAmount(FIELD, "netAmount", BASE,
            Map.of("amount", BASE)))
            .isInstanceOf(LocalizedException.class)
            .extracting("errorCode").isEqualTo(FinErrorCodes.FIN_422_MISSING_AMOUNT_FIELD);
        assertThatThrownBy(() -> EventTypeRuleDomain.sourcedAmount(FIELD, "netAmount", BASE, null))
            .extracting("errorCode").isEqualTo(FinErrorCodes.FIN_422_MISSING_AMOUNT_FIELD);
    }

    @Test
    void aNegativeFieldIsRefused() {
        assertThatThrownBy(() -> EventTypeRuleDomain.sourcedAmount(FIELD, "paidAmount", BASE,
            Map.of("paidAmount", new BigDecimal("-1"))))
            .extracting("errorCode").isEqualTo(FinErrorCodes.FIN_422_NEGATIVE_AMOUNT_FIELD);
    }

    @Test
    void aZeroFieldIsReturnedAndMarkedForOmission() {
        BigDecimal remaining = EventTypeRuleDomain.sourcedAmount(FIELD, "remainingAmount", BASE,
            Map.of("remainingAmount", BigDecimal.ZERO));
        assertThat(remaining).isZero();
        assertThat(EventTypeRuleDomain.isOmittedZeroLine(remaining)).isTrue();
        assertThat(EventTypeRuleDomain.isOmittedZeroLine(new BigDecimal("0.0001"))).isFalse();
    }
}
