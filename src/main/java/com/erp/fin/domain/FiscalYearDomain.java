package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.exception.FinErrorCodes;

/**
 * Domain companion for ENT-FIN-007 (FiscalYear), carrying the one decision scoped to the year's
 * own {@code statusCode} (DBF-FIN-069, FISCAL_YEAR_STATUS):
 *
 * <ul>
 *   <li><b>A closed year is never closed again</b> — API-FIN-027's rerun guard
 *       ({@code FIN-409-INVALID-TRANSITION}) — {@link #assertCanYearEndClose()}.</li>
 * </ul>
 *
 * <p><b>Why this class exists now.</b> DATA-DOM-MASTER.md records "DOMAIN RULES: none scoped
 * alone" for ENT-FIN-007, and until ALIGN-BE that was accurate: every year-end decision was
 * about a period's state or an entry's lines. The rerun guard is not — its only fact is
 * {@code FiscalYear.statusCode}, and SRS A7 makes the year's lifecycle a one-way
 * {@code OPEN → CLOSED} set once by REQ-FIN-036. gov-enforce-backend-contract A.0.1 is
 * unconditional ("regardless of what a module's own design docs say"), and A.5.18 makes the
 * alternative — an {@code if} on {@code statusCode} in {@code FiscalYearService} — an automatic
 * rejection. A.0.7 is satisfied: this is the entity's only Domain object, and it holds the one
 * rule genuinely scoped to it.
 *
 * <p>Before ALIGN-BE a second year-end close was blocked only as a side effect: the first run had
 * already moved every period to YEAR_END_CLOSE, so {@code FiscalPeriodDomain}'s
 * {@code assertHardClosedForYearEnd()} failed on the first period with
 * {@code FIN-409-PERIODS-NOT-CLOSED} — a misleading code for "the year is already closed", and an
 * accident rather than a stated rule.
 *
 * <p>Every fact is resolved by the service and passed in — this class never touches a repository
 * (A.0.3) and never calls another module (A.0.6).
 */
public final class FiscalYearDomain {

    private final Long fiscalYearPk;
    private final String code;
    private final String statusCode;

    private FiscalYearDomain(Long fiscalYearPk, String code, String statusCode) {
        this.fiscalYearPk = fiscalYearPk;
        this.code = code;
        this.statusCode = statusCode;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static FiscalYearDomain from(FiscalYear entity) {
        return new FiscalYearDomain(entity.getFiscalYearPk(), entity.getCode(),
            entity.getStatusCode());
    }

    /**
     * API-FIN-027 (year-end close) — the year must still be OPEN. SRS A7's
     * {@code OPEN --(REQ-FIN-036)--> CLOSED} edge is the only edge the year has and it is
     * traversed once; re-running the close on a CLOSED year would post a second closing entry and
     * a second opening entry against balances the first run already carried forward, doubling the
     * next year's opening position. Decision only — the service posts both entries and calls
     * {@code FiscalYear.close()} after this returns.
     *
     * <p>Code reused rather than invented: {@code FIN-409-INVALID-TRANSITION} is the catalog's
     * existing PLATFORM-STD row for "the row is not in the state this transition expects", already
     * carrying exactly this meaning for the period soft-close transition.
     *
     * @throws LocalizedException {@code FIN-409-INVALID-TRANSITION} when the year is already CLOSED
     */
    public void assertCanYearEndClose() {
        if (!FiscalYear.STATUS_OPEN.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_INVALID_TRANSITION, statusCode);
        }
    }

    public Long getFiscalYearPk() {
        return fiscalYearPk;
    }

    public String getCode() {
        return code;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
