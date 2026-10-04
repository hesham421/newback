package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.exception.FinErrorCodes;
import java.util.Optional;

/**
 * Domain companion for ENT-FIN-003 (DimensionValue): RULE-FIN-002 — the system rejects a
 * dimension value whose {@code code} already exists under the same dimension
 * ({@code FIN-409-DIMVALUE-DUP}), backed at the database by
 * {@code UQ_FIN_DIMENSION_VALUE_DIM_CODE}.
 *
 * <p>Placement note: DATA-DOM-LOOKUP.md annotates RULE-FIN-002 with "— service", but CORE.md
 * mandates a domain class for every rule answering "is this operation allowed?" and
 * gov-enforce-backend-contract A.5.18 makes a business-rule {@code if} inlined in a service an
 * automatic rejection. CORE.md wins; the rule lives here.
 *
 * <p>The service resolves {@code codeAlreadyTakenInDimension} via QR-FIN-010
 * ({@code existsByDimension_DimensionPkAndCode}) and passes it in as a plain argument — this
 * class never touches a repository (A.0.3).
 */
public final class DimensionValueDomain {

    private final Long dimensionPk;
    private final String code;
    private final boolean active;

    private DimensionValueDomain(Long dimensionPk, String code, boolean active) {
        this.dimensionPk = dimensionPk;
        this.code = code;
        this.active = active;
    }

    /**
     * API-FIN-007 (create dimension value): RULE-FIN-002.
     *
     * @param dimensionPk                 the owning dimension's PK
     * @param code                        the submitted value code
     * @param codeAlreadyTakenInDimension QR-FIN-010's answer, resolved by the service
     * @throws LocalizedException {@code FIN-409-DIMVALUE-DUP} when the code is already used
     */
    public static DimensionValueDomain create(Long dimensionPk,
                                              String code,
                                              boolean codeAlreadyTakenInDimension) {
        if (codeAlreadyTakenInDimension) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_DIMVALUE_DUP, code);
        }
        return new DimensionValueDomain(dimensionPk, code, true);
    }

    /**
     * RULE-FIN-009 — QR-FIN-032, API-FIN-019/020/014/017: a posting line's cited dimension value
     * must belong to the dimension the line states, and must be active. Decision only — the
     * service calls this once per {@code JournalLineDimension} row before the entry is built, then
     * persists the line set after every post-time guard has returned.
     *
     * <p>Added by DATA-DOM-TRANSACTIONAL, which owns ENT-FIN-006: the rule decides about a
     * {@code DimensionValue}, and A.0.7 allows at most one Domain object per entity, so it lands
     * here rather than on a second Domain class of its own.
     *
     * @param statedDimensionPk the dimension the line claims this value belongs to
     *                          ({@code JournalLineDimension.dimension}), resolved by the service
     * @throws LocalizedException {@code FIN-409-INVALID-DIMENSION} when the value belongs to a
     *                            different dimension or is inactive
     */
    public void assertUsableOnLine(Long statedDimensionPk) {
        checkUsableOnLine(statedDimensionPk).ifPresent(detail -> {
            throw new LocalizedException(Status.CONFLICT, detail.errorCode(), detail.args());
        });
    }

    /**
     * RULE-FIN-009, non-throwing sibling of {@link #assertUsableOnLine(Long)} — the single
     * implementation of the rule; {@code assertUsableOnLine} delegates here, so the throwing API
     * is unchanged for every existing caller. It exists so the manual-entry path (REQ-FIN-015 /
     * AC-FIN-015) can evaluate every dimension tag without aborting and report all failures
     * together. Same code and same message argument as the throwing form.
     *
     * @return {@code FIN-409-INVALID-DIMENSION} with the value code, or empty when usable
     */
    public Optional<ErrorDetail> checkUsableOnLine(Long statedDimensionPk) {
        if (!active || dimensionPk == null || !dimensionPk.equals(statedDimensionPk)) {
            return Optional.of(
                ErrorDetail.of(FinErrorCodes.FIN_409_INVALID_DIMENSION, code));
        }
        return Optional.empty();
    }

    /**
     * RULE-FIN-027 (v2) — API-FIN-020 build, per BUSINESS_FIELD tag: the event's value must equal
     * the code of a dimension value of the tagged dimension (QR-FIN-058, exact code within the
     * dimension, ADR-FIN-026). A match that is inactive is RULE-FIN-009's rejection
     * ({@link #assertUsableOnLine(Long)}), not this one — the service calls that next.
     *
     * @param match QR-FIN-058's answer, as returned by the repository
     * @return the resolved value, for the RULE-FIN-009 check that follows
     * @throws LocalizedException {@code FIN-422-UNRESOLVED-DIMENSION-VALUE}
     */
    public static DimensionValueDomain assertResolvedByCode(Long dimensionPk, String code,
                                                            Optional<DimensionValue> match) {
        return match
            .map(DimensionValueDomain::from)
            .orElseThrow(() -> new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_UNRESOLVED_DIMENSION_VALUE, code, dimensionPk));
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static DimensionValueDomain from(DimensionValue entity) {
        return new DimensionValueDomain(
            entity.getDimension() == null ? null : entity.getDimension().getDimensionPk(),
            entity.getCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    public Long getDimensionPk() {
        return dimensionPk;
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
