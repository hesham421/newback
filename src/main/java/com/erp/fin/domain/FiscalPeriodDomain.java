package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.exception.FinErrorCodes;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

/**
 * Domain companion for ENT-FIN-008 (FiscalPeriod), carrying the SRS rule CORE.md assigns to it:
 *
 * <ul>
 *   <li><b>RULE-FIN-014</b> — a Hard Closed (or Year-End Closed) period is never reopened
 *       ({@code FIN-409-NOT-REOPENABLE}, QR-FIN-040, API-FIN-024/026).</li>
 *   <li><b>RULE-FIN-028</b> (v2) — the event date must fall in a defined period
 *       ({@code FIN-422-NO-DEFINED-PERIOD}, QR-FIN-059, API-FIN-020) — {@link #assertDateCovered}.</li>
 * </ul>
 *
 * <p><b>RULE-FIN-015 is NOT a rule of this class</b> — it is enforced entirely by the
 * distinct-permission {@code @PreAuthorize} gate on the two close endpoints, which is what the
 * SRS asks for. See the block comment where its former guard method used to sit, below.
 *
 * <p><b>RULE-FIN-008 is deliberately absent.</b> DATA-DOM-MASTER.md lists the period-open-at-post
 * -time rule under ENT-FIN-008, but CORE.md assigns it to {@code JournalEntryDomain} (owned by
 * the DATA-DOM-TRANSACTIONAL sub). Implementing it here too would create exactly the duplicate
 * ownership A.0.7 exists to prevent.
 *
 * <p>Placement note: DATA-DOM-MASTER.md annotates both rules with "owner layer: service";
 * CORE.md's domain-class mandate and A.5.18 supersede that wording.
 *
 * <p><b>A.0.6 holds trivially.</b> This class imports, injects and calls no other module, and
 * since RULE-FIN-015's enforcement is the {@code @PreAuthorize} gate rather than a fact read
 * from SEC, nothing in FIN needs to resolve a cross-module fact on its behalf either.
 */
public final class FiscalPeriodDomain {

    /** A twelve-period fiscal year is the one shape that maps onto calendar months. */
    private static final int CALENDAR_MONTHS_PER_YEAR = 12;

    /** The Arabic locale whose CLDR month names the generated periods carry. */
    private static final Locale ARABIC = Locale.forLanguageTag("ar");

    /** Fallback (non-calendar) period label — see {@link #generatedPeriod}. */
    private static final String PERIOD_NAME_AR_PREFIX = "الفترة ";

    /** Fallback (non-calendar) period label — see {@link #generatedPeriod}. */
    private static final String PERIOD_NAME_EN_PREFIX = "Period ";

    private final Long fiscalPeriodPk;
    private final Integer periodNo;
    private final String statusCode;

    private FiscalPeriodDomain(Long fiscalPeriodPk, Integer periodNo, String statusCode) {
        this.fiscalPeriodPk = fiscalPeriodPk;
        this.periodNo = periodNo;
        this.statusCode = statusCode;
    }

    /**
     * REQ-FIN-031 — a period generated with its fiscal year always starts OPEN (SRS A7); there is
     * no create-time rule to evaluate, so this factory validates nothing.
     */
    public static FiscalPeriodDomain create(Integer periodNo) {
        return new FiscalPeriodDomain(null, periodNo, FiscalPeriod.STATUS_OPEN);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static FiscalPeriodDomain from(FiscalPeriod entity) {
        return new FiscalPeriodDomain(entity.getFiscalPeriodPk(),
            entity.getPeriodNo(),
            entity.getStatusCode());
    }

    /**
     * RULE-FIN-014 — API-FIN-024 (open period) and API-FIN-026's reopen attempts. HARD_CLOSE and
     * YEAR_END_CLOSE are terminal (SRS A7). Decision only — the service calls
     * {@code FiscalPeriod.open()} after this returns.
     *
     * @throws LocalizedException {@code FIN-409-NOT-REOPENABLE}
     */
    public void assertCanReopen() {
        if (FiscalPeriod.STATUS_HARD_CLOSE.equals(statusCode)
            || FiscalPeriod.STATUS_YEAR_END_CLOSE.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_NOT_REOPENABLE, periodNo);
        }
    }

    /*
     * ─────────────────────────────────────────────────────────────────────────────────────────
     * RULE-FIN-015 — there is deliberately NO assertCanHardClose(...) method here.
     * ─────────────────────────────────────────────────────────────────────────────────────────
     * The rule, verbatim (srs-fin.md:1026-1030): "The system shall require the
     * period-close-approval action to be gated by a permission distinct from the
     * journal-entry-creation permission, enforced through the Security module." Its own
     * `Data source` line reads "DEFERRED — the permission matrix is the Security module's
     * declaration surface; FIN declares no permission entity in this version, so the separation
     * is enforced there and has no FIN-side field to read". REQ-FIN-038 (srs-fin.md:781-788)
     * says the same, and AC-FIN-038 (srs-fin.md:789-792) names the mechanism: "Then the system
     * denies it (the CORE interceptor, per SEC's own mechanism)."
     *
     * The rule is therefore satisfied — wholly — by the distinct-permission gate already on both
     * entry points: FiscalPeriodService.hardClose and FiscalYearService.yearEndClose carry
     * @PreAuthorize(...PERM_FIN_PERIODS_CLOSE_APPROVE), a permission distinct from
     * PERM_FIN_JOURNAL_ENTRIES_CREATE. Nothing further is required of this class, and — per the
     * Data source line above — there is no FIN-side fact for it to decide over.
     *
     * DO NOT RESTORE THE PREVIOUS IMPLEMENTATION. Until this was removed, an
     * assertCanHardClose(boolean, boolean) here consumed two facts resolved by a now-deleted
     * FinSeparationOfDutiesService, which read SEC's user directory and refused the close when
     * ANY single user in the system held both permissions — global user-set disjointness. That
     * is a stricter rule than the SRS states: it denied a perfectly clean approver because some
     * unrelated account elsewhere held both codes, and no REQ, AC or RULE asks for it. Removal
     * is a recorded human decision, not an oversight. Re-adding it would also re-create FIN's
     * only dependency on com.erp.sec.crossmodule (XM-FIN-002), which this removal retired.
     */

    /**
     * API-FIN-025 (soft-close) — SRS A7's {@code OPEN --(REQ-FIN-033, soft-close)--> SOFT_CLOSE}
     * edge is the only legal entry into SOFT_CLOSE, so any other current state is rejected.
     * Decision only — the service calls {@code FiscalPeriod.softClose()} after this returns.
     *
     * <p>Added by SVC-API-INT: API-FIN-025's Validations line ("current state must be OPEN") had
     * no guard on this class, and expressing it as an {@code if} in the service body is exactly
     * what gov-enforce-backend-contract A.5.18 rejects.
     *
     * @throws LocalizedException {@code FIN-409-INVALID-TRANSITION}
     */
    public void assertCanSoftClose() {
        if (!FiscalPeriod.STATUS_OPEN.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_INVALID_TRANSITION, statusCode);
        }
    }

    /**
     * API-FIN-027 (year-end close) — the §10.4 precondition: every period of the fiscal year must
     * already be Hard Closed before the year can be closed. Decision only — the service calls
     * {@code FiscalPeriod.yearEndClose()} on each period after this has returned for all of them.
     *
     * <p>Added by SVC-API-INT. It lives here rather than on a FiscalYear Domain object because
     * the fact it decides over is {@code FiscalPeriod.statusCode} (ENT-FIN-008, the rule's own
     * scope) and DATA-DOM-MASTER.md deliberately records "DOMAIN RULES: none scoped alone" for
     * ENT-FIN-007 — manufacturing a second Domain class for the year would breach A.0.7.
     *
     * @throws LocalizedException {@code FIN-409-PERIODS-NOT-CLOSED}
     */
    public void assertHardClosedForYearEnd() {
        if (!FiscalPeriod.STATUS_HARD_CLOSE.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_PERIODS_NOT_CLOSED, periodNo);
        }
    }

    /**
     * RULE-FIN-028 (v2) — API-FIN-020 build: some period's {@code [startDate, endDate]} must cover
     * the event's date (QR-FIN-059's answer, passed in by the service). A period that exists but
     * is not OPEN is RULE-FIN-008's business ({@code JournalEntryDomain},
     * {@code FIN-409-PERIOD-NOT-OPEN}), never this method's.
     *
     * @throws LocalizedException {@code FIN-422-NO-DEFINED-PERIOD}
     */
    public static FiscalPeriodDomain assertDateCovered(Optional<FiscalPeriod> covering,
                                                       LocalDate docDate) {
        return covering
            .map(FiscalPeriodDomain::from)
            .orElseThrow(() -> new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                FinErrorCodes.FIN_422_NO_DEFINED_PERIOD, docDate));
    }

    /**
     * One generated period's span and its two stored names — the result of
     * {@link #generatedPeriod(LocalDate, LocalDate, int, int)}. A plain value carrier: it holds no
     * entity and takes no decision of its own.
     */
    public record GeneratedPeriod(LocalDate startDate, LocalDate endDate,
                                  String nameAr, String nameEn) {
    }

    /**
     * REQ-FIN-031 / API-FIN-023 (QR-FIN-038) — how a fiscal year's span is divided and what each
     * generated period is called. SVC-API-INT.md's API-FIN-023 block now states this convention
     * explicitly; it is implemented once, here, so the mapper only builds the row.
     *
     * <ul>
     *   <li><b>Calendar months</b> — when {@code periodCount == 12}, the span starts on the first
     *       day of a month and covers exactly one whole year, each period IS a calendar month
     *       (period N = the Nth month of the span, from its 1st to its last day). Month names come
     *       from {@code java.time.Month.getDisplayName(TextStyle.FULL, locale)} — the JDK's own
     *       CLDR locale data — for Arabic and English, so neither language is hardcoded here.</li>
     *   <li><b>Even split</b> (any other period count, or a span that is not a whole year) — the
     *       days are divided into {@code periodCount} contiguous blocks, the first
     *       {@code totalDays % periodCount} blocks one day longer, so the last period ends exactly
     *       on the year's {@code endDate} and no day belongs to two periods. Those periods are
     *       named "الفترة N" / "Period N", the only naming available when a block is not a month.
     *       The two Arabic words here are period labels, not calendar data; the platform exposes
     *       no message-bundle mechanism for row content (its {@code MessageSource} bundles are the
     *       error-code/validation register, keyed by wire code).</li>
     * </ul>
     *
     * <p>Why here rather than in the mapper: the convention decides which calendar a posted entry
     * falls into and therefore what every period-scoped report shows, so it is module knowledge,
     * not row-copying. It is nevertheless a derivation — it permits and denies nothing — and so
     * throws no catalog code, exactly like {@code JournalEntryDomain.directionOfNet(...)}.
     *
     * @param yearStart   the fiscal year's own start date (DBF-FIN-067)
     * @param yearEnd     the fiscal year's own end date (DBF-FIN-068)
     * @param periodNo    1-based ordinal of the period being generated
     * @param periodCount how many periods the year is divided into (transient request input)
     */
    public static GeneratedPeriod generatedPeriod(LocalDate yearStart, LocalDate yearEnd,
                                                  int periodNo, int periodCount) {
        if (isWholeYearInCalendarMonths(yearStart, yearEnd, periodCount)) {
            LocalDate start = yearStart.plusMonths(periodNo - 1L);
            LocalDate end = start.plusMonths(1L).minusDays(1L);
            Month month = start.getMonth();
            return new GeneratedPeriod(start, end,
                month.getDisplayName(TextStyle.FULL, ARABIC),
                month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }

        long totalDays = ChronoUnit.DAYS.between(yearStart, yearEnd) + 1L;
        long baseLength = totalDays / periodCount;
        long extraDays = totalDays % periodCount;

        long daysBefore = baseLength * (periodNo - 1L) + Math.min(periodNo - 1L, extraDays);
        long ownLength = baseLength + (periodNo <= extraDays ? 1L : 0L);

        LocalDate start = yearStart.plusDays(daysBefore);
        LocalDate end = periodNo == periodCount ? yearEnd : start.plusDays(ownLength - 1L);

        return new GeneratedPeriod(start, end,
            PERIOD_NAME_AR_PREFIX + periodNo, PERIOD_NAME_EN_PREFIX + periodNo);
    }

    /**
     * Does the year divide into twelve calendar months? It must ask for twelve periods, begin on
     * the first day of a month, and end the day before the same date one year later — otherwise
     * month boundaries and period boundaries cannot coincide and the even split is used instead.
     */
    private static boolean isWholeYearInCalendarMonths(LocalDate yearStart, LocalDate yearEnd,
                                                       int periodCount) {
        return periodCount == CALENDAR_MONTHS_PER_YEAR
            && yearStart != null && yearEnd != null
            && yearStart.getDayOfMonth() == 1
            && yearEnd.equals(yearStart.plusYears(1L).minusDays(1L));
    }

    public Long getFiscalPeriodPk() {
        return fiscalPeriodPk;
    }

    public Integer getPeriodNo() {
        return periodNo;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
