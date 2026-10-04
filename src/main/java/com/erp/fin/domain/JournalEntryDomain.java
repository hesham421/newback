package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.exception.FinErrorCodes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain companion for ENT-FIN-004 (JournalEntry) — the posting aggregate's decision surface,
 * carrying the seven SRS rules CORE.md assigns to it — six as decision methods below, and
 * RULE-FIN-016 structurally (see the note after the list):
 *
 * <ul>
 *   <li><b>RULE-FIN-004</b> — a duplicate {@code eventReference} is rejected
 *       ({@code FIN-409-DUPLICATE-EVENT}, QR-FIN-026, API-FIN-020) — {@link #create}.</li>
 *   <li><b>RULE-FIN-006</b> — total debits must equal total credits
 *       ({@code FIN-409-UNBALANCED}, QR-FIN-029) — {@link #assertBalanced(List)}.</li>
 *   <li><b>RULE-FIN-008</b> — the target period must be Open at post time, <b>except for the
 *       year-end closing and opening entries</b> (srs-fin.md §A5, amended: API-FIN-027 requires
 *       every period Hard Closed before it runs, so those two entries are exempt by the rule
 *       itself) ({@code FIN-409-PERIOD-NOT-OPEN}, QR-FIN-031) —
 *       {@link #assertTargetPeriodOpen(String)} /
 *       {@link #checkTargetPeriodOpenForGeneratedEntry(String, String)}.</li>
 *   <li><b>RULE-FIN-011</b> — the reversal is exact and linked (QR-FIN-034; no distinct catalog
 *       code — a success-path build) — {@link #reversalDirectionOf(String)} decides each line's
 *       opposite direction; the link itself is plain state mutation
 *       ({@code JournalEntry.linkReversal(...)} / {@code JournalEntry.linkOriginal(...)}).</li>
 *   <li><b>RULE-FIN-012</b> — the reversal posts into the current open period when the original's
 *       is no longer Open (QR-FIN-036; no distinct catalog code — a success-path period
 *       substitution) — {@link #resolveReversalPeriodPk(Long, String, Long)}.</li>
 *   <li><b>RULE-FIN-017</b> — the submitted {@code fiscalYearId}, {@code periodId} and
 *       {@code docDate} must describe one accounting context ({@code FIN-400-PERIOD-NOT-IN-YEAR},
 *       {@code FIN-400-DOCDATE-OUTSIDE-PERIOD}, API-FIN-019) —
 *       {@link #assertHeaderCoherent(Long, Long, LocalDate, LocalDate, LocalDate)}.</li>
 *   <li><b>RULE-FIN-013</b> — reversing a non-POSTED entry is rejected
 *       ({@code FIN-409-NOT-POSTED}, QR-FIN-035), and so is reversing one that
     *       already carries a reversal link ({@code FIN-409-ALREADY-REVERSED}) —
     *       {@link #assertCanReverse(boolean)}.</li>
 * </ul>
 *
 * <p><b>RULE-FIN-016 (a POSTED entry is locked) carries no guard method and no error code here</b>
 * — it is enforced structurally, by omission, exactly as the execution plan's RULE→code coverage
 * line states ("enforced by omission, no code needed — no UPDATE/DELETE mapping exists on a POSTED
 * row"). {@code JournalEntryRepository} declares no {@code @Modifying} query and no derived
 * {@code deleteBy...}, and FIN's API registry declares no PUT and no DELETE on
 * {@code /journal-entries}, so no code path can edit or delete an existing entry at all. The SRS
 * message for the rule ("A posted entry is locked; correction is only through a reversal" /
 * "القيد المُرحَّل مقفل؛ التصحيح فقط عبر العكس") is recorded here for traceability; it is never
 * thrown. This mirrors {@code LookupTypeDomain}'s treatment of RULE-MDL-003 (key immutability):
 * owned by the domain class for traceability, enforced by shape, with no guard and no code.
 *
 * <p><b>No carve-out is needed.</b> FIN follows <b>classic reversal</b> (RULE-FIN-011): the
 * original entry stays POSTED for good, the mirror reversal is posted alongside it, and the two
 * rows are linked through {@code originalEntryId}/{@code reversalEntryId}. The reversal flow
 * (API-FIN-021) therefore never touches a POSTED row's accounting state at all — the only write
 * back to the original is the {@code REVERSAL_ENTRY_ID} back-reference (DBF-FIN-043) set by
 * {@code JournalEntry.linkReversal(...)}, a pure traceability link. Net ledger effect is zero
 * because both entries are POSTED and the reversal's lines carry the opposite direction. No
 * dedicated Error Catalog row should ever be requested for RULE-FIN-016; a positive "assert not
 * posted" guard would still be wrong, since reversal is a legal operation on a POSTED entry.
 *
 * <p>Placement note: DATA-DOM-TRANSACTIONAL.md annotates every one of these "— service", but
 * CORE.md mandates a domain class for every rule answering "is this operation allowed?" and
 * gov-enforce-backend-contract A.5.18 makes a business-rule {@code if} inlined in a service an
 * automatic rejection. CORE.md wins; the decisions live here.
 *
 * <p><b>Rules deliberately absent.</b> RULE-FIN-007 (leaf + active account) belongs to
 * {@code AccountDomain.assertPostable()} and RULE-FIN-009 (dimension value validity) to
 * {@code DimensionValueDomain.assertUsableOnLine(...)}; the service calls those once per line.
 * RULE-FIN-005 (no active rule for the event type) is a not-found precondition resolved by an
 * {@code orElseThrow} on QR-FIN-027, not a guard — see {@code EventTypeRuleDomain}.
 *
 * <p>Every fact is resolved by the service and passed in as a plain argument — this class never
 * touches a repository (A.0.3) and never calls another module (A.0.6).
 */
public final class JournalEntryDomain {

    /** DEBIT_CREDIT (SRS A6) — the debit side; compared as the plain code the column stores. */
    public static final String DIRECTION_DEBIT = "DEBIT";

    /** DEBIT_CREDIT (SRS A6) — the credit side. */
    public static final String DIRECTION_CREDIT = "CREDIT";

    /**
     * JOURNAL_TYPE — the year-end closing entry the system generates in API-FIN-027. Declared
     * here, in the class that owns RULE-FIN-008, because the rule's own exemption is stated in
     * terms of it; {@code JournalPostingService} re-exports this constant rather than defining a
     * second copy.
     */
    public static final String JOURNAL_TYPE_CLOSING = "CLOSING";

    /** JOURNAL_TYPE — the next year's opening entry (API-FIN-027); see {@link #JOURNAL_TYPE_CLOSING}. */
    public static final String JOURNAL_TYPE_OPENING = "OPENING";

    private final Long journalEntryPk;
    private final String docNo;
    private final String statusCode;

    private JournalEntryDomain(Long journalEntryPk, String docNo, String statusCode) {
        this.journalEntryPk = journalEntryPk;
        this.docNo = docNo;
        this.statusCode = statusCode;
    }

    /**
     * API-FIN-019 / API-FIN-020: RULE-FIN-004's pre-check (QR-FIN-026), v2's first detection. The
     * second is the store's refusal on {@code UQ_FIN_JOURNAL_ENTRY_EVENT_REF} for a concurrent
     * duplicate, translated to the same {@code FIN-409-DUPLICATE-EVENT} by
     * {@code FinPersistenceRefusalAdvisor} (ADR-FIN-030), so a duplicate is never a generic error.
     * A manual entry passes {@code null} / {@code false}.
     */
    public static JournalEntryDomain create(String eventReference,
                                            boolean eventReferenceAlreadyProcessed) {
        if (eventReferenceAlreadyProcessed) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_DUPLICATE_EVENT, eventReference);
        }
        return new JournalEntryDomain(null, null, JournalEntry.STATUS_DRAFT);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static JournalEntryDomain from(JournalEntry entity) {
        return new JournalEntryDomain(entity.getJournalEntryPk(),
            entity.getDocNo(),
            entity.getStatusCode());
    }

    /**
     * RULE-FIN-006 — QR-FIN-029, API-FIN-019/020/014/017 (POL-FIN-001). Decision only; the service
     * calls {@code JournalEntry.post(...)} after every post-time guard has returned.
     *
     * <p>Amounts are always positive (POL-FIN-005); the side comes from {@code directionCode}, so
     * the invariant is "sum of DEBIT amounts equals sum of CREDIT amounts", compared exactly —
     * {@code BigDecimal.compareTo}, never {@code equals}, so {@code 10.0000} and {@code 10.00}
     * agree.
     *
     * @param lines the entry's complete line set as it would stand once written, resolved by the
     *              service (submitted lines, or the rule engine's built lines)
     * @throws LocalizedException {@code FIN-409-UNBALANCED} when the two totals differ
     */
    public void assertBalanced(List<JournalLine> lines) {
        checkBalanced(lines).ifPresent(detail -> {
            throw new LocalizedException(Status.CONFLICT, detail.errorCode(), detail.args());
        });
    }

    /**
     * RULE-FIN-006, non-throwing sibling of {@link #assertBalanced(List)} — the single
     * implementation of the rule; {@code assertBalanced} delegates here. Returns the failure
     * instead of throwing it, so the manual-entry path (REQ-FIN-015 / AC-FIN-015) can evaluate
     * every guard and report them together. Same code, same message arguments as the throwing
     * form, so each entry localizes identically.
     *
     * @return {@code FIN-409-UNBALANCED} with both totals, or empty when the entry balances
     */
    public Optional<ErrorDetail> checkBalanced(List<JournalLine> lines) {
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        if (lines != null) {
            for (JournalLine line : lines) {
                BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
                if (DIRECTION_DEBIT.equalsIgnoreCase(line.getDirectionCode())) {
                    debitTotal = debitTotal.add(amount);
                } else {
                    creditTotal = creditTotal.add(amount);
                }
            }
        }
        if (debitTotal.compareTo(creditTotal) != 0) {
            return Optional.of(ErrorDetail.of(
                FinErrorCodes.FIN_409_UNBALANCED, debitTotal, creditTotal));
        }
        return Optional.empty();
    }

    /**
     * RULE-FIN-008 — QR-FIN-031, API-FIN-019/020/014/017 (POL-FIN-004). Checked at post time, not
     * build time. Decision only.
     *
     * @param targetPeriodStatusCode the target period's PERIOD_STATE code, read by the service
     * @throws LocalizedException {@code FIN-409-PERIOD-NOT-OPEN} when it is not {@code OPEN}
     */
    public void assertTargetPeriodOpen(String targetPeriodStatusCode) {
        checkTargetPeriodOpen(targetPeriodStatusCode).ifPresent(detail -> {
            throw new LocalizedException(Status.CONFLICT, detail.errorCode(), detail.args());
        });
    }

    /**
     * RULE-FIN-008, non-throwing sibling of {@link #assertTargetPeriodOpen(String)} — the single
     * implementation of the rule; {@code assertTargetPeriodOpen} delegates here. See
     * {@link #checkBalanced(List)} for why the sibling exists.
     *
     * <p>This form applies the gate unconditionally, and is what every path whose
     * {@code journalTypeCode} is supplied by the caller must use (API-FIN-019's manual entry):
     * the rule's exemption belongs to entries the system itself generates, never to a type a
     * client can name.
     *
     * @return {@code FIN-409-PERIOD-NOT-OPEN} with the offending state, or empty when Open
     */
    public Optional<ErrorDetail> checkTargetPeriodOpen(String targetPeriodStatusCode) {
        if (!FiscalPeriod.STATUS_OPEN.equals(targetPeriodStatusCode)) {
            return Optional.of(ErrorDetail.of(
                FinErrorCodes.FIN_409_PERIOD_NOT_OPEN, targetPeriodStatusCode));
        }
        return Optional.empty();
    }

    /**
     * RULE-FIN-008 as the amended rule states it, for an entry the system itself generated with a
     * {@code journalTypeCode} chosen in FIN code (the shared posting pipeline's five paths).
     * srs-fin.md §A5 now reads: "The system shall reject posting an entry whose period is not Open
     * at that moment, except the year-end closing and opening entries generated by the year-end
     * close (REQ-FIN-036), which are exempt" — API-FIN-027 requires every period of the year to be
     * Hard Closed before it runs, so a CLOSING entry could otherwise never post at all.
     *
     * <p>The exemption is part of the rule, not a per-call switch: it is decided here from what
     * the entry <i>is</i> ({@link #isYearEndGenerated(String)}), and no caller can ask for it. The
     * gate's own condition has one implementation — {@link #checkTargetPeriodOpen(String)} — which
     * this method delegates to whenever the exemption does not apply. RULE-FIN-006, 007 and 009
     * are untouched and continue to apply to both generated entries.
     *
     * @param journalTypeCode        the system-assigned JOURNAL_TYPE of the entry being posted
     * @param targetPeriodStatusCode the target period's PERIOD_STATE code
     * @return {@code FIN-409-PERIOD-NOT-OPEN}, or empty when Open or exempt
     */
    public Optional<ErrorDetail> checkTargetPeriodOpenForGeneratedEntry(
            String journalTypeCode, String targetPeriodStatusCode) {
        if (isYearEndGenerated(journalTypeCode)) {
            return Optional.empty();
        }
        return checkTargetPeriodOpen(targetPeriodStatusCode);
    }

    /**
     * Is this one of the two entries the year-end close generates (API-FIN-027)? The pair
     * RULE-FIN-008's amended statement exempts from the period gate, and the only JOURNAL_TYPE
     * values no client-facing create path may produce.
     */
    public static boolean isYearEndGenerated(String journalTypeCode) {
        return JOURNAL_TYPE_CLOSING.equalsIgnoreCase(journalTypeCode)
            || JOURNAL_TYPE_OPENING.equalsIgnoreCase(journalTypeCode);
    }

    /**
     * RULE-FIN-013 — QR-FIN-035, API-FIN-021. The rule's single implementation, carrying both
     * halves of REQ-FIN-030's stated rationale ("prevents double-reversal or reversing a
     * DRAFT/VOID row", srs-fin.md line 682):
     *
     * <ul>
     *   <li>only a POSTED entry can be reversed — a DRAFT has nothing to undo
     *       ({@code FIN-409-NOT-POSTED});</li>
     *   <li>an entry that already carries a reversal link cannot be reversed a second time
     *       ({@code FIN-409-ALREADY-REVERSED}).</li>
     * </ul>
     *
     * <p><b>Why the second half is an explicit guard now.</b> Under the pre-classic flow the
     * original was VOIDed by its own reversal, so a second attempt failed the status half by
     * accident. Classic reversal leaves the original POSTED for good, so the status half no longer
     * catches it: without this guard the same entry could be reversed twice, posting two mirrors
     * and leaving a net ledger effect of {@code −(original)} instead of zero. AC-FIN-030 always
     * required the rejection; only its incidental mechanism changed.
     *
     * <p>Decision only — the service builds the reversal and calls
     * {@code JournalEntry.linkReversal(...)} after this returns. The already-reversed fact is
     * resolved by the service from the loaded row's {@code REVERSAL_ENTRY_ID} (DBF-FIN-043) and
     * passed in as a plain argument; this class never touches a repository (A.0.3).
     *
     * @param alreadyReversed whether the entry already carries a reversal link
     * @throws LocalizedException {@code FIN-409-NOT-POSTED} or {@code FIN-409-ALREADY-REVERSED}
     */
    public void assertCanReverse(boolean alreadyReversed) {
        if (!JournalEntry.STATUS_POSTED.equals(statusCode)) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_NOT_POSTED, docNo);
        }
        if (alreadyReversed) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_ALREADY_REVERSED, docNo);
        }
    }

    /**
     * RULE-FIN-011 — QR-FIN-034, API-FIN-021: each reversal line mirrors its original with the
     * same amount and the opposite direction. A pure derivation, with no catalog code of its own
     * (a success-path build).
     *
     * <p>DEBIT_CREDIT is a two-value lookup (SRS A6) and the submitted code was validated against
     * MDL before the original was ever written (XM-FIN-001), so the mapping is total: DEBIT
     * becomes CREDIT, and anything else — necessarily CREDIT — becomes DEBIT.
     */
    public static String reversalDirectionOf(String directionCode) {
        return DIRECTION_DEBIT.equalsIgnoreCase(directionCode)
            ? DIRECTION_CREDIT
            : DIRECTION_DEBIT;
    }

    /**
     * RULE-FIN-012 — QR-FIN-036, API-FIN-021: the reversal posts into the original's own period
     * while that period is still Open, and into the current open period once it is not. A
     * decision, not a lookup — the service resolves both candidate periods (the original's, via
     * the loaded entry; the current open one, via the shared {@code SpecBuilder} search over
     * {@code FiscalPeriodRepository}) and passes them in.
     *
     * @param originalPeriodPk         the original entry's period
     * @param originalPeriodStatusCode that period's PERIOD_STATE code
     * @param currentOpenPeriodPk      the currently open period, or {@code null} if none is
     * @return the period the reversal must be posted into
     * @throws LocalizedException {@code FIN-409-PERIOD-NOT-OPEN} when the original's period is
     *                            closed and no open period exists to receive the reversal
     */
    public static Long resolveReversalPeriodPk(Long originalPeriodPk,
                                               String originalPeriodStatusCode,
                                               Long currentOpenPeriodPk) {
        if (FiscalPeriod.STATUS_OPEN.equals(originalPeriodStatusCode)) {
            return originalPeriodPk;
        }
        if (currentOpenPeriodPk == null) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_PERIOD_NOT_OPEN, originalPeriodStatusCode);
        }
        return currentOpenPeriodPk;
    }

    /**
     * The side a net movement sits on: a positive net (debits exceed credits) is a DEBIT balance,
     * anything else a CREDIT balance. A pure derivation over DEBIT_CREDIT (SRS A6), with no
     * catalog code of its own.
     *
     * <p>Added by SVC-API-INT for API-FIN-017 (which posts the source account's balance across
     * its targets) and API-FIN-027 (which closes each result account's balance to Retained
     * Earnings and reproduces each balance-sheet balance in the next year's opening entry). Both
     * paths must turn a signed net into a {@code directionCode}; doing it with an {@code if} in a
     * service body is what A.5.18 rejects, and {@link #reversalDirectionOf(String)} answers a
     * different question (the opposite of a stated side, RULE-FIN-011).
     *
     * @param net signed movement, debits minus credits
     * @return {@link #DIRECTION_DEBIT} or {@link #DIRECTION_CREDIT}
     */
    public static String directionOfNet(BigDecimal net) {
        BigDecimal safeNet = net == null ? BigDecimal.ZERO : net;
        return safeNet.compareTo(BigDecimal.ZERO) > 0 ? DIRECTION_DEBIT : DIRECTION_CREDIT;
    }

    /**
     * RULE-FIN-017 — API-FIN-019. The three header facts a manual entry submits independently —
     * {@code fiscalYearId}, {@code periodId} and {@code docDate} — must describe one and the same
     * accounting context: the period must belong to the submitted fiscal year (DBF-FIN-076 makes
     * the period's year a stored fact), and the document date must fall inside that period's own
     * {@code [startDate, endDate]} span (DBF-FIN-080/081, both NOT NULL).
     *
     * <p><b>Why it is a rule and not bookkeeping.</b> Without it an entry can be stamped with one
     * year, posted into another year's period and dated outside both: it then appears in one
     * year's period-scoped trial balance, is invisible to the balance sheet's {@code asOfDate}
     * cut-off, consumes a {@code docNo} from the wrong year's series, and corrupts the year-end
     * close, which scopes its closing balances purely by {@code journalEntry.fiscalYear}. Nothing
     * downstream can repair that, so it is rejected at the gate.
     *
     * <p><b>Fail-fast, not aggregated.</b> Unlike RULE-FIN-006/007/008/009 this is not a judgement
     * about the entry's lines but about the context they would be judged in: an incoherent header
     * makes RULE-FIN-008's own period check meaningless, so it is raised on its own, exactly like
     * the not-found preconditions that resolve the same three facts.
     *
     * <p><b>Scope.</b> Only the paths where a caller submits all three facts independently need
     * it. Every system-generated path derives them from one another and is coherent by
     * construction — {@code JournalPostingService.resolvePeriodContaining(...)} takes the period
     * FROM the date and the year FROM the period, and API-FIN-027's two entries take the year's
     * own last/first period and that year's own end/start date.
     *
     * @param submittedFiscalYearPk the {@code fiscalYearId} on the request
     * @param periodFiscalYearPk    the fiscal year the submitted period actually belongs to
     * @param docDate               the submitted document date
     * @param periodStartDate       the submitted period's first day (DBF-FIN-080)
     * @param periodEndDate         the submitted period's last day (DBF-FIN-081)
     * @throws LocalizedException {@code FIN-400-PERIOD-NOT-IN-YEAR} or
     *                            {@code FIN-400-DOCDATE-OUTSIDE-PERIOD}
     */
    public static void assertHeaderCoherent(Long submittedFiscalYearPk,
                                            Long periodFiscalYearPk,
                                            LocalDate docDate,
                                            LocalDate periodStartDate,
                                            LocalDate periodEndDate) {
        if (submittedFiscalYearPk == null || !submittedFiscalYearPk.equals(periodFiscalYearPk)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                FinErrorCodes.FIN_400_PERIOD_NOT_IN_YEAR,
                submittedFiscalYearPk, periodFiscalYearPk);
        }
        if (docDate == null || periodStartDate == null || periodEndDate == null
            || docDate.isBefore(periodStartDate) || docDate.isAfter(periodEndDate)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                FinErrorCodes.FIN_400_DOCDATE_OUTSIDE_PERIOD,
                docDate, periodStartDate, periodEndDate);
        }
    }

    /**
     * The two running side totals of an entry under construction, and the only place a
     * {@code directionCode} decides which side an amount lands on outside
     * {@link #checkBalanced(List)}. RULE-FIN-010's per-side remainder needs exactly these two
     * numbers, and computing them with an {@code if} on {@code DEBIT} in a service body is what
     * A.5.18 rejects — so the accumulator lives here, in the class that owns DEBIT_CREDIT.
     *
     * <p>Immutable: {@link #plus(String, BigDecimal)} returns a new instance.
     */
    public record SideTotals(BigDecimal debitTotal, BigDecimal creditTotal) {

        /** An entry with nothing assigned on either side yet. */
        public static SideTotals zero() {
            return new SideTotals(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        /** The same totals with {@code amount} added to the side {@code directionCode} names. */
        public SideTotals plus(String directionCode, BigDecimal amount) {
            BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
            return DIRECTION_DEBIT.equalsIgnoreCase(directionCode)
                ? new SideTotals(debitTotal.add(safeAmount), creditTotal)
                : new SideTotals(debitTotal, creditTotal.add(safeAmount));
        }

        /** The total already carried by {@code directionCode}'s own side. */
        public BigDecimal ownTotalOf(String directionCode) {
            return DIRECTION_DEBIT.equalsIgnoreCase(directionCode) ? debitTotal : creditTotal;
        }

        /** The total already carried by the side opposite {@code directionCode}. */
        public BigDecimal opposingTotalOf(String directionCode) {
            return DIRECTION_DEBIT.equalsIgnoreCase(directionCode) ? creditTotal : debitTotal;
        }
    }

    public Long getJournalEntryPk() {
        return journalEntryPk;
    }

    public String getDocNo() {
        return docNo;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
