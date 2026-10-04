package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.RecurringTemplate;
import com.erp.fin.exception.FinErrorCodes;

/**
 * Domain companion for ENT-FIN-011 (RecurringTemplate), carrying the one decision scoped to the
 * template's own {@code IS_ACTIVE_FL} (DBF-FIN-117):
 *
 * <ul>
 *   <li><b>A deactivated template is never run</b> — API-FIN-014's active gate
 *       ({@code FIN-409-NOT-ACTIVE}) — {@link #assertCanRun()}.</li>
 * </ul>
 *
 * <p><b>Why this class exists now.</b> DATA-DOM-LOOKUP.md records "DOMAIN RULES: none scoped
 * alone" for ENT-FIN-011, and until this change that was accurate: a template's run posts through
 * the shared pipeline, whose rules (RULE-FIN-006..010) are all decided by other entities' Domain
 * objects. The active gate is not — its only fact is {@code RecurringTemplate.isActiveFl}. It
 * answers "is this operation allowed?", so build-create-entity's Domain Companion section and
 * gov-enforce-backend-contract A.0.1 (unconditional, "regardless of what a module's own design
 * docs say") require a Domain object, and A.5.18 makes the alternative — an {@code if} on
 * {@code isActiveFl} inlined in {@code RecurringTemplateService.run} — an automatic rejection.
 * A.0.7 is satisfied: this is the entity's only Domain object and it holds the one rule genuinely
 * scoped to it. The same reasoning {@code FiscalYearDomain} records for its own late creation.
 *
 * <p><b>The gate is a RECORDED HUMAN DECISION, not an SRS rule.</b> RULE-FIN-001..017 were each
 * read and none of them states that a deactivated template may not be run; AC-FIN-023 is written
 * "Given an active recurring template" and states no outcome for an inactive one. Before this
 * change API-FIN-036 set the flag and {@code run} ignored it, so a retired template still built
 * and posted an entry — a defect in shipped behaviour, closed here by decision rather than by a
 * rule that was always there.
 *
 * <p>Every fact is resolved by the service and passed in — this class never touches a repository
 * (A.0.3) and never calls another module (A.0.6).
 */
public final class RecurringTemplateDomain {

    private final Long recurringTemplatePk;
    private final String scheduleTypeCode;
    private final boolean active;

    private RecurringTemplateDomain(Long recurringTemplatePk,
                                    String scheduleTypeCode,
                                    boolean active) {
        this.recurringTemplatePk = recurringTemplatePk;
        this.scheduleTypeCode = scheduleTypeCode;
        this.active = active;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static RecurringTemplateDomain from(RecurringTemplate entity) {
        return new RecurringTemplateDomain(entity.getRecurringTemplatePk(),
            entity.getScheduleTypeCode(), Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * API-FIN-014 (run a template) — the template must still be active. A template is retired
     * through API-FIN-036 precisely so that it stops producing entries; running one anyway posts
     * a journal entry that RULE-FIN-016 then locks, recoverable only by reversing it individually.
     * Decision only — the service builds and posts the entry after this returns.
     *
     * <p>Applies to the direct endpoint and to any internal or scheduled trigger, because both
     * enter through {@code RecurringTemplateService.run(Long)}, which is the module's single run
     * path (SVC-API-INT.md: "the endpoint is the same whether a user or an internal scheduler
     * triggers it").
     *
     * @throws LocalizedException {@code FIN-409-NOT-ACTIVE} when the template is deactivated
     */
    public void assertCanRun() {
        if (!active) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_NOT_ACTIVE, recurringTemplatePk);
        }
    }

    public Long getRecurringTemplatePk() {
        return recurringTemplatePk;
    }

    public String getScheduleTypeCode() {
        return scheduleTypeCode;
    }

    public boolean isActive() {
        return active;
    }
}
