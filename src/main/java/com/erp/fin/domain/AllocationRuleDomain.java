package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.AllocationRule;
import com.erp.fin.entity.AllocationTarget;
import com.erp.fin.exception.FinErrorCodes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Domain companion for ENT-FIN-013 (AllocationRule): RULE-FIN-003 as it applies to allocation
 * targets — exactly one target marked as the remainder whenever the target set is a compound or
 * percentage distribution ({@code FIN-409-REMAINDER-COUNT}, QR-FIN-016 reused, API-FIN-016), with
 * {@code isRemainderFl} as the single marker both this guard and the API-FIN-017 builder read
 * ({@code FIN-422-REMAINDER-MARKER} on disagreement).
 *
 * <p>The rule spans the whole target set of one allocation rule, so it sits on the aggregate
 * root rather than on {@code AllocationTarget} — A.0.7 allows at most one Domain object per
 * entity, and AllocationRule's own fields carry no separate rule ("DOMAIN RULES: none scoped
 * alone" in DATA-DOM-LOOKUP.md).
 *
 * <p>Placement note: DATA-DOM-LOOKUP.md annotates RULE-FIN-003 with "— service"; CORE.md's
 * domain-class mandate and A.5.18 supersede that wording.
 *
 * <p>The service resolves the prospective target set and passes it in — this class never
 * touches a repository (A.0.3) and never calls another module (A.0.6).
 */
public final class AllocationRuleDomain {

    /** DISTRIBUTION_TYPE (SRS A6) — the target that absorbs the rounding difference. */
    public static final String DISTRIBUTION_TYPE_REMAINDER = "REMAINDER";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Long allocationRulePk;
    private final Long sourceAccountId;
    private final boolean active;

    private AllocationRuleDomain(Long allocationRulePk, Long sourceAccountId, boolean active) {
        this.allocationRulePk = allocationRulePk;
        this.sourceAccountId = sourceAccountId;
        this.active = active;
    }

    /**
     * API-FIN-016 (create allocation rule with targets): validates RULE-FIN-003 over the
     * submitted target set at construction time.
     *
     * @param sourceAccountId  the rule's source account id (DBF-FIN-133)
     * @param submittedTargets the rule's complete target set as submitted
     * @throws LocalizedException {@code FIN-409-REMAINDER-COUNT} when any target uses
     *                            percentage distribution and the remainder-target count is not
     *                            exactly one
     */
    public static AllocationRuleDomain create(Long sourceAccountId,
                                              List<AllocationTarget> submittedTargets) {
        AllocationRuleDomain domain = new AllocationRuleDomain(null, sourceAccountId, true);
        domain.assertRemainderTargetSetValid(submittedTargets);
        return domain;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static AllocationRuleDomain from(AllocationRule entity) {
        return new AllocationRuleDomain(entity.getAllocationRulePk(),
            entity.getSourceAccount() == null ? null : entity.getSourceAccount().getAccountPk(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * Is this target the remainder target? {@code isRemainderFl} (DBF-FIN-141) is the schema's own
     * marker and the SINGLE one both this class's RULE-FIN-003 guard and the API-FIN-017 builder
     * read — the same alignment {@code EventTypeRuleDomain.isRemainderLine(Boolean)} makes on the
     * rule-line side. Before ALIGN-BE the guard counted {@code isRemainderFl} while
     * {@code AllocationRuleService} keyed off {@code distributionTypeCode = REMAINDER}.
     */
    public static boolean isRemainderTarget(Boolean isRemainderFl) {
        return Boolean.TRUE.equals(isRemainderFl);
    }

    /**
     * RULE-FIN-003 (reused for allocation targets), in the amended form ALIGN-BE gives it.
     * Decision only — the service persists the target set after this returns.
     *
     * <p>Marker agreement first ({@code isRemainderFl} ⟺ {@code distributionTypeCode = REMAINDER},
     * {@code FIN-422-REMAINDER-MARKER}), then exactly-one whenever the target set is a compound or
     * percentage distribution — any sibling uses PERCENTAGE, or any target is already marked as
     * the remainder (POL-FIN-006's own trigger).
     *
     * @param prospectiveTargets the rule's complete target set as it would stand after the
     *                           write, resolved by the service
     * @throws LocalizedException {@code FIN-422-REMAINDER-MARKER} or
     *                            {@code FIN-409-REMAINDER-COUNT}
     */
    public void assertRemainderTargetSetValid(List<AllocationTarget> prospectiveTargets) {
        if (prospectiveTargets == null || prospectiveTargets.isEmpty()) {
            return;
        }
        for (AllocationTarget target : prospectiveTargets) {
            boolean declaresRemainder = DISTRIBUTION_TYPE_REMAINDER
                .equalsIgnoreCase(target.getDistributionTypeCode());
            if (isRemainderTarget(target.getIsRemainderFl()) != declaresRemainder) {
                throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                    FinErrorCodes.FIN_422_REMAINDER_MARKER, target.getDistributionTypeCode());
            }
        }

        boolean anyPercentage = prospectiveTargets.stream()
            .anyMatch(target -> EventTypeRuleDomain.DISTRIBUTION_TYPE_PERCENTAGE
                .equalsIgnoreCase(target.getDistributionTypeCode()));
        boolean anyRemainder = prospectiveTargets.stream()
            .anyMatch(target -> isRemainderTarget(target.getIsRemainderFl()));
        if (!anyPercentage && !anyRemainder) {
            return;
        }
        long remainderCount = prospectiveTargets.stream()
            .filter(target -> isRemainderTarget(target.getIsRemainderFl()))
            .count();
        if (remainderCount != 1L) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_REMAINDER_COUNT, remainderCount);
        }
    }

    /**
     * RULE-FIN-010 as it applies to allocation targets (REQ-FIN-026, QR-FIN-028) — one target's
     * share of the source account's current balance, per its DISTRIBUTION_TYPE (SRS A6:
     * {@code FIXED, PERCENTAGE, REMAINDER}):
     *
     * <ul>
     *   <li>{@code FIXED} — {@code distributionValue} (DBF-FIN-145) is the amount itself;</li>
     *   <li>{@code PERCENTAGE} — it is a percentage of the source balance, rounded to the
     *       smallest stored unit;</li>
     *   <li>{@code REMAINDER} — <b>never reached</b>: the remainder target is identified by
     *       {@link #isRemainderTarget(Boolean)} and computed last, as a difference, through
     *       {@link EventTypeRuleDomain#remainderAmount(BigDecimal, BigDecimal)} — the
     *       single implementation of RULE-FIN-010, shared with the event-build path so both
     *       absorb the rounding difference identically. Reaching into that class for the shared
     *       piece follows the precedent already set by
     *       {@link #assertRemainderTargetSetValid(List)}, which compares against its
     *       {@code DISTRIBUTION_TYPE_PERCENTAGE} constant. Reaching this method with a REMAINDER
     *       distribution means marker and type code disagree
     *       ({@code FIN-422-REMAINDER-MARKER}).</li>
     * </ul>
     *
     * <p>Added by SVC-API-INT: API-FIN-017 has to turn each target into a posting amount, and the
     * branch is on a business code, so it belongs here rather than in the service body (A.5.18).
     *
     * @return the target's amount
     * @throws LocalizedException {@code FIN-422-REMAINDER-MARKER} for a REMAINDER-distributed
     *                            target
     */
    public static BigDecimal targetAmount(String distributionTypeCode,
                                          BigDecimal distributionValue,
                                          BigDecimal sourceBalance) {
        if (DISTRIBUTION_TYPE_REMAINDER.equalsIgnoreCase(distributionTypeCode)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_REMAINDER_MARKER, distributionTypeCode);
        }
        if (EventTypeRuleDomain.DISTRIBUTION_TYPE_PERCENTAGE
            .equalsIgnoreCase(distributionTypeCode)) {
            BigDecimal percentage =
                distributionValue == null ? BigDecimal.ZERO : distributionValue;
            BigDecimal base = sourceBalance == null ? BigDecimal.ZERO : sourceBalance;
            return base.multiply(percentage)
                .divide(ONE_HUNDRED, EventTypeRuleDomain.AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        return distributionValue == null ? BigDecimal.ZERO : distributionValue;
    }

    /**
     * API-FIN-017 (run an allocation rule) — the rule must still be active. A rule is retired
     * through API-FIN-037 precisely so that it stops distributing a balance; running one anyway
     * posts an ALLOCATION entry that RULE-FIN-016 then locks, recoverable only by reversing it.
     * Decision only — the service distributes and posts after this returns.
     *
     * <p><b>RECORDED HUMAN DECISION, not an SRS rule.</b> RULE-FIN-001..017 were each read and
     * none of them states that a deactivated allocation rule may not be run — RULE-FIN-003, the
     * rule this class otherwise owns, governs the remainder-target SET and says nothing about the
     * active flag. Before this change API-FIN-037 set {@code IS_ACTIVE_FL} and {@code run}
     * ignored it, so a retired rule still posted; {@link #isActive()} had no callers at all. The
     * gate closes that defect by decision, not by a rule that was always there.
     *
     * <p>The guard runs BEFORE the target set is loaded and before RULE-FIN-003 is evaluated: a
     * retired rule is refused for being retired, never for a remainder-target set that is only
     * examined on rules that may actually run.
     *
     * @throws LocalizedException {@code FIN-409-NOT-ACTIVE} when the rule is deactivated
     */
    public void assertCanRun() {
        if (!active) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_NOT_ACTIVE, allocationRulePk);
        }
    }

    public Long getAllocationRulePk() {
        return allocationRulePk;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public boolean isActive() {
        return active;
    }
}
