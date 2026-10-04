package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.EventTypeRule;
import com.erp.fin.entity.RuleLine;
import com.erp.fin.exception.FinErrorCodes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Domain companion for ENT-FIN-009 (EventTypeRule), covering the two decisions that belong to
 * the rule aggregate:
 *
 * <ul>
 *   <li><b>One active rule per event type</b> (§6.4, QR-FIN-014) — {@code FIN-409-RULE-DUP},
 *       backed by {@code UQ_FIN_EVENT_TYPE_RULE_CODE}.</li>
 *   <li><b>RULE-FIN-003</b> — exactly one line marked as the remainder whenever the line set is a
 *       compound or percentage distribution — any sibling line uses {@code PERCENTAGE}
 *       distribution, or any line is already marked as the remainder ({@code
 *       FIN-409-REMAINDER-COUNT}, QR-FIN-016) — and that marker, {@code isRemainderFl}, must agree
 *       with the line's own REMAINDER type codes ({@code FIN-422-REMAINDER-MARKER}). The rule
 *       spans the whole line set of one rule, so it sits on the aggregate root rather than on
 *       {@code RuleLine}; A.0.7 allows at most one Domain object per entity and this is it.</li>
 * </ul>
 *
 * <p>Placement note: DATA-DOM-LOOKUP.md annotates RULE-FIN-003 with "— service", but CORE.md
 * mandates a domain class for every rule answering "is this operation allowed?", and A.5.18
 * makes a business-rule {@code if} inlined in a service an automatic rejection. CORE.md wins.
 *
 * <p>The service resolves every fact (uniqueness via QR-FIN-014, the prospective line set from
 * the request plus already-persisted siblings) and passes it in — this class never touches a
 * repository (A.0.3) and never calls another module (A.0.6).
 */
public final class EventTypeRuleDomain {

    /**
     * The {@code DISTRIBUTION_TYPE} lookup value that triggers RULE-FIN-003, per srs-fin.md §A6
     * ({@code DISTRIBUTION_TYPE: FIXED, PERCENTAGE, REMAINDER}). Compared here as the plain
     * code the column stores (XM-FIN-001); membership of the code in MDL is validated earlier,
     * in the service layer.
     */
    public static final String DISTRIBUTION_TYPE_PERCENTAGE = "PERCENTAGE";

    /** ACCOUNT_DERIVATION_TYPE (SRS A6) — the derivation value IS the account code. */
    public static final String ACCOUNT_DERIVATION_TYPE_CONSTANT = "CONSTANT";

    /**
     * ACCOUNT_DERIVATION_TYPE (SRS A6, v2) — the account is the one an active AccountMapping names
     * for the line's business field and the event's value (RULE-FIN-020, ADR-FIN-027).
     */
    public static final String ACCOUNT_DERIVATION_TYPE_MAPPING = "MAPPING";

    /** AMOUNT_SOURCE_TYPE (SRS A6) — the amount is a percentage of the event's base amount. */
    public static final String AMOUNT_SOURCE_TYPE_PERCENTAGE = "PERCENTAGE";

    /** AMOUNT_SOURCE_TYPE (SRS A6) — the amount is the rounding remainder (RULE-FIN-010). */
    public static final String AMOUNT_SOURCE_TYPE_REMAINDER = "REMAINDER";

    /** {@code NUMERIC(18,4)} — the smallest stored unit every computed amount rounds to. */
    public static final int AMOUNT_SCALE = 4;

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final String eventTypeCode;
    private final boolean active;

    private EventTypeRuleDomain(String eventTypeCode, boolean active) {
        this.eventTypeCode = eventTypeCode;
        this.active = active;
    }

    /**
     * API-FIN-010 (create event-type rule): one active rule per event type.
     *
     * @param eventTypeCode     the submitted ACCOUNTING_EVENT_TYPE code
     * @param ruleAlreadyExists QR-FIN-014's answer, resolved by the service
     * @throws LocalizedException {@code FIN-409-RULE-DUP} when the event type already has a rule
     */
    public static EventTypeRuleDomain create(String eventTypeCode, boolean ruleAlreadyExists) {
        if (ruleAlreadyExists) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_RULE_DUP, eventTypeCode);
        }
        return new EventTypeRuleDomain(eventTypeCode, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static EventTypeRuleDomain from(EventTypeRule entity) {
        return new EventTypeRuleDomain(entity.getEventTypeCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * Is this line the remainder line? {@code isRemainderFl} (DBF-FIN-103) is the schema's own
     * marker and the SINGLE one both this class's RULE-FIN-003 guard and the API-FIN-020 builder
     * read. Which column carries the marker is rule knowledge, so the predicate lives here rather
     * than being re-expressed at each call site.
     *
     * <p>Before ALIGN-BE the guard counted {@code isRemainderFl} while the builder keyed off
     * {@code amountSourceTypeCode = REMAINDER}; two lines could therefore satisfy the guard and
     * both behave as the remainder at run time. {@link #assertRemainderLineSetValid(List)} now
     * rejects any disagreement between the two columns ({@code FIN-422-REMAINDER-MARKER}), so the
     * single marker is well defined for every line this predicate ever sees.
     */
    public static boolean isRemainderLine(Boolean isRemainderFl) {
        return Boolean.TRUE.equals(isRemainderFl);
    }

    /**
     * Does this line's own type code declare a remainder, on either of the two code columns
     * ENT-FIN-010 carries? {@code amountSourceTypeCode = REMAINDER} (DBF-FIN-103's sibling
     * DBF-FIN-103/104) and {@code distributionTypeCode = REMAINDER} (DBF-FIN-105) both say so.
     */
    private static boolean declaresRemainder(RuleLine line) {
        return AMOUNT_SOURCE_TYPE_REMAINDER.equalsIgnoreCase(line.getAmountSourceTypeCode())
            || AllocationRuleDomain.DISTRIBUTION_TYPE_REMAINDER
                .equalsIgnoreCase(line.getDistributionTypeCode());
    }

    /**
     * API-FIN-011 (add rule line): RULE-FIN-003, in the amended form ALIGN-BE gives it. Decision
     * only — the service persists the line set after this returns.
     *
     * <p>Two checks, in order:
     *
     * <ol>
     *   <li><b>Marker agreement</b> — a line marked {@code isRemainderFl} must declare a REMAINDER
     *       type code, and a line declaring one must be marked. The rule's own marker is
     *       {@code isRemainderFl} (DBF-FIN-103); the disagreement is rejected rather than silently
     *       resolved, because "which line is the remainder" decides every other line's amount
     *       (RULE-FIN-010) ({@code FIN-422-REMAINDER-MARKER}).</li>
     *   <li><b>Exactly one remainder</b> — whenever the line set is a compound or percentage
     *       distribution (POL-FIN-006's own trigger: any sibling uses PERCENTAGE distribution, OR
     *       any line is already marked as the remainder). The second half is what ALIGN-BE adds:
     *       two FIXED-distribution lines both marked remainder used to pass unchallenged here and
     *       then die on {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE} at run time
     *       ({@code FIN-409-REMAINDER-COUNT}).</li>
     * </ol>
     *
     * @param prospectiveLines the rule's complete line set as it would stand after the write
     *                         (already-persisted siblings plus the submitted line), resolved by
     *                         the service
     * @throws LocalizedException {@code FIN-422-REMAINDER-MARKER} or
     *                            {@code FIN-409-REMAINDER-COUNT}
     */
    public void assertRemainderLineSetValid(List<RuleLine> prospectiveLines) {
        if (prospectiveLines == null || prospectiveLines.isEmpty()) {
            return;
        }
        for (RuleLine line : prospectiveLines) {
            if (isRemainderLine(line.getIsRemainderFl()) != declaresRemainder(line)) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                    FinErrorCodes.FIN_422_REMAINDER_MARKER, line.getLineNo());
            }
        }

        boolean anyPercentage = prospectiveLines.stream()
            .anyMatch(line -> DISTRIBUTION_TYPE_PERCENTAGE
                .equalsIgnoreCase(line.getDistributionTypeCode()));
        boolean anyRemainder = prospectiveLines.stream()
            .anyMatch(line -> isRemainderLine(line.getIsRemainderFl()));
        if (!anyPercentage && !anyRemainder) {
            return;
        }
        long remainderCount = prospectiveLines.stream()
            .filter(line -> isRemainderLine(line.getIsRemainderFl()))
            .count();
        if (remainderCount != 1L) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_REMAINDER_COUNT, remainderCount);
        }
    }

    /**
     * Does this rule line derive its account through an account mapping (RULE-FIN-020) rather
     * than by constant code? Which code means which derivation is rule knowledge, so the
     * predicate lives here, as {@link #isRemainderLine(Boolean)} does for the remainder marker.
     */
    public static boolean isMappingDerivation(String accountDerivationTypeCode) {
        return ACCOUNT_DERIVATION_TYPE_MAPPING.equalsIgnoreCase(accountDerivationTypeCode);
    }

    /**
     * RULE-FIN-010 (build half) — a rule line's amount for a given event, per its
     * AMOUNT_SOURCE_TYPE (SRS A6: {@code FIELD, PERCENTAGE, REMAINDER}):
     *
     * <ul>
     *   <li>{@code FIELD} — {@code amountSourceValue} (DBF-FIN-104) names the event amount
     *       field;</li>
     *   <li>{@code PERCENTAGE} — {@code amountSourceValue} is a percentage of the event's base
     *       amount, rounded to the smallest stored unit ({@code NUMERIC(18,4)},
     *       {@code HALF_UP}) exactly as the rule's own text requires;</li>
     *   <li>{@code REMAINDER} — <b>never reached</b>: the remainder line is identified by
     *       {@link #isRemainderLine(Boolean)} and computed last, as a difference, by
     *       {@link #remainderAmount(BigDecimal, BigDecimal)}; it is "never itself computed as a
     *       percentage". Reaching this method with a REMAINDER source means the line's marker and
     *       its type code disagree, which {@link #assertRemainderLineSetValid(List)} rejects —
     *       raised here too, so the derivation can never silently produce a normal amount for a
     *       remainder line ({@code FIN-422-REMAINDER-MARKER}).</li>
     * </ul>
     *
     * @return the line's amount
     * @throws LocalizedException {@code FIN-422-REMAINDER-MARKER} for a REMAINDER-sourced line,
     *     {@code FIN-422-INVALID-PERCENTAGE-VALUE} (code review fix) when {@code amountSourceValue}
     *     is not a well-formed decimal for a PERCENTAGE-sourced line, and for a FIELD-sourced line
     *     {@code FIN-422-MISSING-AMOUNT-FIELD} when the event carries no such amount or
     *     {@code FIN-422-NEGATIVE-AMOUNT-FIELD} when it is negative
     */
    public static BigDecimal sourcedAmount(String amountSourceTypeCode,
                                           String amountSourceValue,
                                           BigDecimal baseAmount,
                                           Map<String, BigDecimal> eventAmounts) {
        if (AMOUNT_SOURCE_TYPE_REMAINDER.equalsIgnoreCase(amountSourceTypeCode)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_REMAINDER_MARKER, amountSourceTypeCode);
        }
        if (AMOUNT_SOURCE_TYPE_PERCENTAGE.equalsIgnoreCase(amountSourceTypeCode)) {
            BigDecimal percentage;
            if (amountSourceValue == null) {
                percentage = BigDecimal.ZERO;
            } else {
                try {
                    percentage = new BigDecimal(amountSourceValue);
                } catch (NumberFormatException ex) {
                    throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                        FinErrorCodes.FIN_422_INVALID_PERCENTAGE_VALUE, amountSourceValue);
                }
            }
            BigDecimal base = baseAmount == null ? BigDecimal.ZERO : baseAmount;
            return base.multiply(percentage)
                .divide(ONE_HUNDRED, AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        // FIELD. An absent field used to become ZERO and die on CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE
        // as an unlocalized DATA_INTEGRITY_VIOLATION (fin-consumer E2E, 2026-09-24) — now named.
        BigDecimal fieldAmount = eventAmounts == null || amountSourceValue == null
            ? null : eventAmounts.get(amountSourceValue);
        if (fieldAmount == null) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_MISSING_AMOUNT_FIELD, amountSourceValue);
        }
        if (fieldAmount.signum() < 0) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_NEGATIVE_AMOUNT_FIELD, amountSourceValue, fieldAmount);
        }
        return fieldAmount;
    }

    /**
     * POL-FIN-005 — a sourced (non-remainder) line whose amount is exactly zero carries nothing and
     * is left out of the entry instead of posting a zero line the CHECK refuses. Real case: a
     * PURCHASE_INVOICE paid in full has {@code remainingAmount = 0} — every one of the 50 in 2026 —
     * and must still post {@code grossAmount} against {@code paidAmount}. A remainder line is never
     * omitted: its non-positive result stays {@code FIN-422-REMAINDER-NOT-POSITIVE}. The entry's
     * balance is still judged on the lines that remain (RULE-FIN-006).
     */
    public static boolean isOmittedZeroLine(BigDecimal sourcedAmount) {
        return sourcedAmount != null && sourcedAmount.signum() == 0;
    }

    /**
     * RULE-FIN-010 — the remainder line's amount, computed <b>per side</b>: the opposing side's
     * running total minus its own side's running total, after every percentage line has rounded.
     * It is never itself a percentage.
     *
     * <p><b>Why per side (ALIGN-BE).</b> A journal entry's remainder line exists to make one side
     * equal the other (POL-FIN-006, "the balancing guarantor"), so the difference it absorbs is a
     * difference between the two sides — not between a distributed total and the sum of every line
     * regardless of direction. The earlier side-blind form subtracted every line, debit and credit
     * alike, from the base amount: base 1000 with DEBIT 1000, CREDIT 60% = 600 and a CREDIT
     * remainder produced {@code 1000 − 1600 = −600}, a negative amount that failed
     * {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE} as an unlocalized data-integrity 409. Per side
     * the same case gives {@code 1000 − 600 = 400} and the entry balances exactly.
     *
     * <p>This remains the rule's single implementation: the event-build path (API-FIN-020, this
     * class) and the allocation path (API-FIN-017, {@code AllocationRuleDomain}) both reach it, so
     * the rounding difference is absorbed identically wherever a compound distribution is built.
     * The allocation path's arguments are the same two facts under different names — the source
     * line's amount is the opposing side's whole total, and the already-assigned targets are the
     * remainder's own side.
     *
     * @param opposingSideTotal   the total already carried by the side opposite the remainder line
     * @param ownSideTotalSoFar   the sum of every already-computed line on the remainder's own side
     * @throws LocalizedException {@code FIN-422-REMAINDER-NOT-POSITIVE} when no positive residue
     *                            is left for the remainder line to absorb (POL-FIN-005)
     */
    public static BigDecimal remainderAmount(BigDecimal opposingSideTotal,
                                             BigDecimal ownSideTotalSoFar) {
        BigDecimal safeOpposing =
            opposingSideTotal == null ? BigDecimal.ZERO : opposingSideTotal;
        BigDecimal safeOwn = ownSideTotalSoFar == null ? BigDecimal.ZERO : ownSideTotalSoFar;
        BigDecimal remainder =
            safeOpposing.subtract(safeOwn).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (remainder.signum() <= 0) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_REMAINDER_NOT_POSITIVE, safeOpposing, safeOwn);
        }
        return remainder;
    }

    /** FIN_DIMENSION_VALUE_SOURCE (SRS A6, v2) — the tag carries a fixed dimension value. */
    public static final String VALUE_SOURCE_CONSTANT = "CONSTANT";

    /** FIN_DIMENSION_VALUE_SOURCE (SRS A6, v2) — the tag's value is read from an event field. */
    public static final String VALUE_SOURCE_BUSINESS_FIELD = "BUSINESS_FIELD";

    /**
     * Is this dimension tag's value read from the event's business field (RULE-FIN-021/027)
     * rather than fixed on the tag (CONSTANT, RULE-FIN-026)?
     */
    public static boolean isBusinessFieldSource(String valueSourceCode) {
        return VALUE_SOURCE_BUSINESS_FIELD.equalsIgnoreCase(valueSourceCode);
    }

    /**
     * RULE-FIN-018 (v2) — API-FIN-011: the derivation type must be an active
     * ACCOUNT_DERIVATION_TYPE value, so CONSTANT and MAPPING are the only accepted derivations;
     * the membership fact is MDL's, fetched by the service and passed in.
     *
     * @throws LocalizedException {@code FIN-422-UNSUPPORTED-DERIVATION}
     */
    public static void assertDerivationSupported(String accountDerivationTypeCode,
                                                 boolean codeIsActiveDerivationType) {
        if (!codeIsActiveDerivationType) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_UNSUPPORTED_DERIVATION, accountDerivationTypeCode);
        }
    }

    /**
     * RULE-FIN-019 (v2) — API-FIN-011: a CONSTANT line carries an account code and no business
     * field; a MAPPING line carries a business field and no account code (the application half
     * of {@code CHK_FIN_RULE_LINE_DERIVATION_SPEC}). Any other type is RULE-FIN-018's rejection.
     *
     * @throws LocalizedException {@code FIN-422-DERIVATION-SPEC} or
     *                            {@code FIN-422-UNSUPPORTED-DERIVATION}
     */
    public static void assertDerivationSpec(String accountDerivationTypeCode,
                                            String accountDerivationValue,
                                            String accountBusinessFieldCode) {
        boolean hasValue = hasText(accountDerivationValue);
        boolean hasField = hasText(accountBusinessFieldCode);
        boolean specMatches;
        if (ACCOUNT_DERIVATION_TYPE_CONSTANT.equalsIgnoreCase(accountDerivationTypeCode)) {
            specMatches = hasValue && !hasField;
        } else if (ACCOUNT_DERIVATION_TYPE_MAPPING.equalsIgnoreCase(accountDerivationTypeCode)) {
            specMatches = hasField && !hasValue;
        } else {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_UNSUPPORTED_DERIVATION, accountDerivationTypeCode);
        }
        if (!specMatches) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_DERIVATION_SPEC, accountDerivationTypeCode);
        }
    }

    /**
     * RULE-FIN-025 (v2) — API-FIN-011: no two submitted tags of one line name the same dimension
     * (the application half of {@code UQ_FIN_RULE_LINE_DIM_LINE_DIM}).
     *
     * @param dimensionIds the {@code dimensionId} of every submitted tag, in order
     * @throws LocalizedException {@code FIN-422-DIMENSION-TAG-DUP}
     */
    public static void assertTagsDistinct(List<Long> dimensionIds) {
        if (dimensionIds == null) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        for (Long dimensionId : dimensionIds) {
            if (dimensionId != null && !seen.add(dimensionId)) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                    FinErrorCodes.FIN_422_DIMENSION_TAG_DUP, dimensionId);
            }
        }
    }

    /**
     * RULE-FIN-026 (v2) — API-FIN-011: a CONSTANT tag carries a value of its own dimension and no
     * business field; a BUSINESS_FIELD tag carries a business field and no value (the application
     * half of {@code CHK_FIN_RULE_LINE_DIM_VALUE_SPEC}, plus the ownership QR-FIN-061 reads).
     *
     * @param valueOwnerDimensionId the dimension the submitted {@code dimensionValueId} belongs
     *                              to (QR-FIN-061), or {@code null} when no value was submitted
     * @throws LocalizedException {@code FIN-422-DIMENSION-TAG-SOURCE}
     */
    public static void assertTagMatchesSource(String valueSourceCode, Long dimensionId,
                                              Long dimensionValueId, Long valueOwnerDimensionId,
                                              String businessFieldCode) {
        boolean hasField = hasText(businessFieldCode);
        boolean matches;
        if (VALUE_SOURCE_CONSTANT.equalsIgnoreCase(valueSourceCode)) {
            matches = dimensionValueId != null && !hasField
                && dimensionId != null && dimensionId.equals(valueOwnerDimensionId);
        } else if (VALUE_SOURCE_BUSINESS_FIELD.equalsIgnoreCase(valueSourceCode)) {
            matches = hasField && dimensionValueId == null;
        } else {
            matches = false;
        }
        if (!matches) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_DIMENSION_TAG_SOURCE, dimensionId);
        }
    }

    /**
     * RULE-FIN-021 (v2) — API-FIN-020 build: the event must carry the business field a MAPPING
     * line ({@code accountBusinessFieldCode}) or a BUSINESS_FIELD tag ({@code businessFieldCode})
     * names, as a key of its {@code fields} map (ADR-FIN-031). A missing or blank value is
     * "not carried"; never a default, never a guess.
     *
     * @return the event's value for that field, blanks trimmed (ADR-FIN-039)
     * @throws LocalizedException {@code FIN-422-MISSING-BUSINESS-FIELD}
     */
    public static String assertFieldPresent(String businessFieldCode,
                                            Map<String, String> eventFields) {
        String value = eventFields == null || businessFieldCode == null
            ? null : eventFields.get(businessFieldCode);
        if (!hasText(value)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_MISSING_BUSINESS_FIELD, businessFieldCode);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getEventTypeCode() {
        return eventTypeCode;
    }

    public boolean isActive() {
        return active;
    }
}
