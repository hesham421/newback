package com.erp.fin.exception;

/**
 * Module-specific error codes for Finance / General Ledger (FIN) — one constant per row of the
 * FIN v2 Error Catalog (packages/v2/backend-execution/_SECTIONS.md). The value is the runtime
 * code verbatim ({@code FIN-{http}[-{SLUG}]}) and serves as both the wire {@code code} in the
 * {@code ApiError} envelope and the i18n message key (the {@code MdlErrorCodes} convention).
 * {@link #FIN_403_SOD_VIOLATION} is struck in that catalog and has no thrower.
 */
public final class FinErrorCodes {

    private FinErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * RULE-FIN-002 — a dimension value whose code already exists under the same dimension
     * (QR-FIN-010; backed by {@code UQ_FIN_DIMENSION_VALUE_DIM_CODE}).
     * API: API-FIN-007. HTTP 409.
     */
    public static final String FIN_409_DIMVALUE_DUP = "FIN-409-DIMVALUE-DUP";

    /**
     * PLATFORM-STD (§6.4, one rule per event type, QR-FIN-014) — the event type already has an
     * active rule (backed by {@code UQ_FIN_EVENT_TYPE_RULE_CODE}).
     * API: API-FIN-010. HTTP 409.
     */
    public static final String FIN_409_RULE_DUP = "FIN-409-RULE-DUP";

    /**
     * RULE-FIN-003 — wrong remainder-line/target count: exactly one remainder is required
     * whenever any sibling line (ENT-FIN-010) or target (ENT-FIN-014) uses percentage
     * distribution (QR-FIN-016).
     * APIs: API-FIN-011, API-FIN-016. HTTP 409.
     */
    public static final String FIN_409_REMAINDER_COUNT = "FIN-409-REMAINDER-COUNT";

    /**
     * RULE-FIN-001 (create trigger) — a new sub-account was placed under a parent that is still
     * marked as accepting direct posting; the parent must be demoted first (QR-FIN-006).
     * API: API-FIN-002. HTTP 409.
     */
    public static final String FIN_409_PARENT_NOT_LEAF_ELIGIBLE = "FIN-409-PARENT-NOT-LEAF-ELIGIBLE";

    /**
     * RULE-FIN-001 (update trigger) — an account that already has sub-accounts cannot be marked
     * as accepting direct posting (QR-FIN-006).
     * APIs: API-FIN-002, API-FIN-003. HTTP 409.
     */
    public static final String FIN_409_HAS_CHILDREN = "FIN-409-HAS-CHILDREN";

    /**
     * RULE-FIN-007 — a posting line targets an account that is not a leaf or not active.
     * APIs: API-FIN-019, API-FIN-020, API-FIN-014, API-FIN-017. HTTP 409.
     */
    public static final String FIN_409_NOT_POSTABLE_ACCOUNT = "FIN-409-NOT-POSTABLE-ACCOUNT";

    /**
     * RULE-FIN-014 — an attempt to reopen a Hard Closed (or Year-End Closed) period; both are
     * terminal states per SRS A7 (QR-FIN-040).
     * APIs: API-FIN-024, API-FIN-026. HTTP 409.
     */
    public static final String FIN_409_NOT_REOPENABLE = "FIN-409-NOT-REOPENABLE";

    /**
     * RULE-FIN-015 — STRUCK in the v2 Error Catalog: both throw sites were deleted on 2026-09-12
     * (recorded human decision); the rule is satisfied by the distinct
     * {@code PERM_FIN_PERIODS_CLOSE_APPROVE} gate alone. The constant and its two bundle keys stay
     * on record with no thrower until a session removes all three together.
     */
    public static final String FIN_403_SOD_VIOLATION = "FIN-403-SOD-VIOLATION";

    /**
     * RULE-FIN-006 — the entry's total debits do not equal its total credits (QR-FIN-029,
     * POL-FIN-001). Thrown by {@code JournalEntryDomain.assertBalanced(...)}.
     * APIs: API-FIN-019, API-FIN-020, API-FIN-014, API-FIN-017. HTTP 409
     * ({@code Status.CONFLICT}).
     */
    public static final String FIN_409_UNBALANCED = "FIN-409-UNBALANCED";

    /**
     * RULE-FIN-008 — the target period is not Open at post time (QR-FIN-031, POL-FIN-004). Thrown
     * by {@code JournalEntryDomain.assertTargetPeriodOpen(...)} and by
     * {@code JournalEntryDomain.resolveReversalPeriodPk(...)} when RULE-FIN-012 finds no open
     * period to receive a reversal. Retryable: the rejection clears once the target period is
     * opened, and only a SOFT_CLOSE period can return to OPEN, so a retry needs an upper bound.
     * APIs: API-FIN-019, API-FIN-020, API-FIN-014, API-FIN-017, API-FIN-021. HTTP 409.
     */
    public static final String FIN_409_PERIOD_NOT_OPEN = "FIN-409-PERIOD-NOT-OPEN";

    /**
     * RULE-FIN-009 — a posting line cites a dimension value that does not belong to its stated
     * dimension, or is inactive (QR-FIN-032). Thrown by
     * {@code DimensionValueDomain.assertUsableOnLine(...)}.
     * APIs: API-FIN-019, API-FIN-020, API-FIN-014, API-FIN-017. HTTP 409.
     */
    public static final String FIN_409_INVALID_DIMENSION = "FIN-409-INVALID-DIMENSION";

    /**
     * RULE-FIN-004 — an entry has already been produced for this {@code eventReference}
     * (QR-FIN-026, POL-FIN-012; backed by {@code UQ_FIN_JOURNAL_ENTRY_EVENT_REF}). Thrown by
     * {@code JournalEntryDomain.create(...)}; v2 also by {@code FinPersistenceRefusalAdvisor} when
     * the store refuses a concurrent duplicate on that constraint (ADR-FIN-030, REQ-FIN-072). Not
     * retryable: the entry is already posted, so the consumer acknowledges the message on this code.
     * API: API-FIN-020. HTTP 409.
     */
    public static final String FIN_409_DUPLICATE_EVENT = "FIN-409-DUPLICATE-EVENT";

    /**
     * RULE-FIN-013 — a reverse action was requested on an entry that is not POSTED (QR-FIN-035).
     * Thrown by {@code JournalEntryDomain.assertCanReverse()}.
     * API: API-FIN-021. HTTP 409.
     */
    public static final String FIN_409_NOT_POSTED = "FIN-409-NOT-POSTED";

    /**
     * PLATFORM-STD — the row is not in the state this transition expects. The Error Catalog
     * registers this row against the period soft-close transition; it does not cover RULE-FIN-016
     * (the posting lock), which is enforced by omission and throws nothing — see
     * {@code JournalEntryDomain}'s class javadoc.
     *
     * <p>ALIGN-BE reuses the same row for API-FIN-027's re-run guard: a year-end close requested
     * on a fiscal year already CLOSED is the identical semantic ("the row is not in the state this
     * transition expects"), so no new code is invented for it. Thrown there by
     * {@code FiscalYearDomain.assertCanYearEndClose()}.
     * APIs: API-FIN-025, API-FIN-027. HTTP 409.
     */
    public static final String FIN_409_INVALID_TRANSITION = "FIN-409-INVALID-TRANSITION";

    /**
     * PLATFORM-STD — unknown journal entry id. The not-found code for the two read paths this
     * sub's repository layer serves (QR-FIN-035's load-before-reverse and QR-FIN-037's read-one);
     * raised by the service's {@code orElseThrow}, registered here with the rest of the posting
     * core's codes.
     * APIs: API-FIN-021, API-FIN-022. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_ENTRY = "FIN-404-ENTRY";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // SVC-API-CRUD — the service-layer codes API-FIN-002/003/004/006/007/010/011/013/016/019
    // actually throw. Every constant below is a verbatim Error Catalog row (_SECTIONS.md).
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * PLATFORM-STD (XM-FIN-001) — a submitted lookup-backed code is not an active value of its
     * MDL lookup type, or the type key itself is unknown/inactive (MDL's own
     * {@code MDL-404-TYPE-KEY}, translated at FIN's boundary, never surfaced raw).
     * APIs: many. HTTP 400 ({@code Status.VALIDATION_ERROR}).
     */
    public static final String FIN_400_INVALID_LOOKUP = "FIN-400-INVALID-LOOKUP";

    /**
     * PLATFORM-STD — duplicate account code (QR-FIN-005, backed by {@code UQ_FIN_ACCOUNT_CODE}).
     * API: API-FIN-002. HTTP 409 ({@code Status.ALREADY_EXISTS}).
     */
    public static final String FIN_409_ACCOUNT_DUP = "FIN-409-ACCOUNT-DUP";

    /**
     * PLATFORM-STD — unknown account id.
     * APIs: API-FIN-003, API-FIN-004, API-FIN-013, API-FIN-016, API-FIN-019, API-FIN-028.
     * HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_ACCOUNT = "FIN-404-ACCOUNT";

    /**
     * PLATFORM-STD — duplicate dimension code (backed by {@code UQ_FIN_DIMENSION_CODE}).
     * API: API-FIN-006. HTTP 409 ({@code Status.ALREADY_EXISTS}).
     */
    public static final String FIN_409_DIMENSION_DUP = "FIN-409-DIMENSION-DUP";

    /**
     * PLATFORM-STD — unknown dimension id.
     * APIs: API-FIN-007, API-FIN-008, API-FIN-019, API-FIN-032. HTTP 404.
     */
    public static final String FIN_404_DIMENSION = "FIN-404-DIMENSION";

    /**
     * PLATFORM-STD — unknown dimension value id. Its own row rather than a reuse of
     * {@link #FIN_404_DIMENSION}: the catalog assigns a distinct 404 per entity, and raising the
     * parent's code here would tell the client the DIMENSION was missing when the value was.
     * Not to be confused with {@link #FIN_409_DIMVALUE_DUP}, which is the duplicate-code scenario.
     * API: API-FIN-035. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_DIMVALUE = "FIN-404-DIMVALUE";

    /**
     * PLATFORM-STD — unknown event-type rule id.
     * APIs: API-FIN-011, API-FIN-034. HTTP 404.
     */
    public static final String FIN_404_RULE = "FIN-404-RULE";

    /**
     * PLATFORM-STD — a recurring template was submitted without a {@code frequencyCode} while its
     * {@code scheduleTypeCode} is not {@code REVERSING} (SRS ENT-FIN-011: "yes (when
     * scheduleTypeCode=RECURRING)"). A conditional-requiredness input check, not an SRS RULE.
     * API: API-FIN-013. HTTP 400 ({@code Status.VALIDATION_ERROR}).
     */
    public static final String FIN_400_MISSING_FREQUENCY = "FIN-400-MISSING-FREQUENCY";

    /**
     * PLATFORM-STD — unknown fiscal year id. Registered here because API-FIN-019 resolves
     * {@code fiscalYearId} before it can build an entry; the catalog row is shared with
     * API-FIN-027. HTTP 404.
     */
    public static final String FIN_404_YEAR = "FIN-404-YEAR";

    /**
     * PLATFORM-STD — unknown period id. Registered here because API-FIN-019 resolves
     * {@code periodId} before it can build an entry; the catalog row is shared with
     * API-FIN-024/025/026. HTTP 404.
     */
    public static final String FIN_404_PERIOD = "FIN-404-PERIOD";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // SVC-API-INT — the codes API-FIN-014/017/020/021/023/024/025/026/027 add on top of the
    // posting-core set above. Every constant below is a verbatim Error Catalog row
    // (_SECTIONS.md, "Error Catalog — FIN v2").
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * RULE-FIN-005 — the submitted event's type has no active {@code EventTypeRule} (QR-FIN-027).
     * A not-found precondition: without a rule there is nothing to build an entry from, so it is
     * raised fail-fast by the service's {@code orElseThrow} before any other check runs.
     * API: API-FIN-020. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_NO_ACTIVE_RULE = "FIN-404-NO-ACTIVE-RULE";

    /**
     * PLATFORM-STD — unknown allocation rule id (QR-FIN-022).
     * API: API-FIN-017. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_ALLOCATION_RULE = "FIN-404-ALLOCATION-RULE";

    /**
     * PLATFORM-STD — duplicate fiscal year code (backed by {@code UQ_FIN_FISCAL_YEAR_CODE}).
     * API: API-FIN-023. HTTP 409 ({@code Status.ALREADY_EXISTS}).
     */
    public static final String FIN_409_YEAR_DUP = "FIN-409-YEAR-DUP";

    /**
     * PLATFORM-STD (§10.4 precondition) — year-end close was requested while at least one period
     * of the fiscal year is not Hard Closed. Thrown by
     * {@code FiscalPeriodDomain.assertHardClosedForYearEnd()}.
     * API: API-FIN-027. HTTP 409 ({@code Status.CONFLICT}).
     */
    public static final String FIN_409_PERIODS_NOT_CLOSED = "FIN-409-PERIODS-NOT-CLOSED";

    /**
     * PLATFORM-STD — unknown recurring template id (ENT-FIN-011, QR-FIN-019). Its own row: the
     * catalog assigns a distinct 404 per entity, and raising {@code FIN-404-RULE} here told the
     * client that an event-type rule was missing.
     * API: API-FIN-014. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_TEMPLATE = "FIN-404-TEMPLATE";
    // ─────────────────────────────────────────────────────────────────────────────────────────
    // SVC-API-SEARCH — the one code this sub's read-only endpoints add. Every other code the
    // seven searches, the entry read and the five reports raise (FIN-404-ACCOUNT,
    // FIN-404-DIMENSION, FIN-404-ENTRY, FIN-404-PERIOD) is already registered above. FIN-500 is
    // the infrastructure fall-through and is produced by the global handler, not thrown here.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * PLATFORM-STD — the caller asked to sort by a field that is not on the searched entity's
     * {@code ALLOWED_SORT_FIELDS} whitelist. Registered verbatim from the Error Catalog row
     * ({@code every search API | 400 | unrecognized sort field}).
     *
     * <p>The check is explicit because the shared {@code PageableBuilder} silently DROPS an
     * unrecognised sort field instead of signalling, which would return a differently ordered page
     * than the client asked for with no indication that anything was ignored. Same reasoning and
     * same shape as SEC's delivered {@code SecSearchSupport.assertSortAllowed}.
     * APIs: API-FIN-001, 005, 008, 009, 012, 015, 018. HTTP 400
     * ({@code Status.VALIDATION_ERROR}).
     */
    public static final String FIN_400_INVALID_SORT = "FIN-400-INVALID-SORT";

    /**
     * RULE-FIN-013 — a reverse action was requested on a POSTED entry that already carries a
     * reversal link ({@code REVERSAL_ENTRY_ID}, DBF-FIN-043). The second half of REQ-FIN-030's
     * rationale ("prevents double-reversal", srs-fin.md line 682), which used to be enforced
     * incidentally by the pre-classic VOID write and now needs its own code. Thrown by
     * {@code JournalEntryDomain.assertCanReverse(boolean)}.
     *
     * <p>409 rather than 422: the request is blocked by the existence of a specific referencing
     * record — the reversal entry already pointing at this one — which is the taxonomy's
     * {@code CONFLICT} row, not the "invariant violation that is NOT about a specific referencing
     * record" row.
     * APIs: API-FIN-021, API-FIN-014. HTTP 409 ({@code Status.CONFLICT}).
     */
    public static final String FIN_409_ALREADY_REVERSED = "FIN-409-ALREADY-REVERSED";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // ALIGN-BE — the four codes the accounting-correctness alignment adds. Every constant below
    // is a verbatim Error Catalog row (_SECTIONS.md, "Error Catalog — FIN v2").
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * RULE-FIN-003 / RULE-FIN-010 — a rule line (ENT-FIN-010) or allocation target (ENT-FIN-014)
     * whose {@code isRemainderFl} marker disagrees with its own {@code amountSourceTypeCode} /
     * {@code distributionTypeCode}: either the line declares a REMAINDER source without being
     * marked, or it is marked without declaring one. {@code isRemainderFl} (DBF-FIN-103 /
     * DBF-FIN-141) is the schema's own marker and the single one both the RULE-FIN-003 guard and
     * the distribution builder read; a disagreement makes "which line is the remainder" ambiguous,
     * which is exactly what RULE-FIN-010 must never be. Thrown by
     * {@code EventTypeRuleDomain.assertRemainderLineSetValid(...)} /
     * {@code AllocationRuleDomain.assertRemainderTargetSetValid(...)} and, defensively, by their
     * amount derivations.
     *
     * <p>422 rather than 409: an invariant violation inside the submitted/stored rule definition
     * itself, not a clash with a specific referencing record — the taxonomy's
     * {@code BUSINESS_RULE_VIOLATION} row.
     * APIs: API-FIN-011, API-FIN-016, API-FIN-017, API-FIN-020. HTTP 422.
     */
    public static final String FIN_422_REMAINDER_MARKER = "FIN-422-REMAINDER-MARKER";

    /**
     * RULE-FIN-010 — the remainder line's computed amount is not positive: the non-remainder lines
     * on its own side already equal or exceed the opposing side's total, so there is no balancing
     * residue for it to absorb. Previously this produced a negative or zero amount that died on
     * {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE} as an unlocalized {@code
     * DATA_INTEGRITY_VIOLATION}. Thrown by {@code EventTypeRuleDomain.remainderAmount(...)}.
     *
     * <p>422 rather than 409: POL-FIN-005 ("amounts are always positive") is an invariant of the
     * entry being built, not a clash with an existing record.
     * APIs: API-FIN-017, API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_REMAINDER_NOT_POSITIVE = "FIN-422-REMAINDER-NOT-POSITIVE";

    /**
     * A FIELD-sourced rule line (ENT-FIN-010, {@code amountSourceTypeCode = FIELD}) names an amount
     * the event does not carry. Previously the missing amount became ZERO and died on
     * {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE} as an unlocalized {@code
     * DATA_INTEGRITY_VIOLATION} (found by the fin-consumer E2E run, 2026-09-24). Retrying the same
     * event cannot help: the producer or the rule must change. Thrown by {@code
     * EventTypeRuleDomain.sourcedAmount(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_MISSING_AMOUNT_FIELD = "FIN-422-MISSING-AMOUNT-FIELD";

    /**
     * A FIELD-sourced rule line's event amount is negative. POL-FIN-005: amounts are always
     * positive and the direction carries the sign — a producer sends the mirrored event type, not a
     * negative amount. Thrown by {@code EventTypeRuleDomain.sourcedAmount(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_NEGATIVE_AMOUNT_FIELD = "FIN-422-NEGATIVE-AMOUNT-FIELD";

    /**
     * Code review fix (correctness) — a rule line (ENT-FIN-010) with
     * {@code amountSourceTypeCode = PERCENTAGE} whose {@code amountSourceValue} (DBF-FIN-104, free
     * text) is not a well-formed decimal. Nothing upstream (the DTO, the create-time validation)
     * constrains this column's format when the type is PERCENTAGE, so a bad value persisted once at
     * rule-creation time previously surfaced as an unlocalized {@code NumberFormatException} —
     * caught nowhere and falling through to a generic 500 — on every subsequent event-driven build
     * for that event type. Thrown by {@code EventTypeRuleDomain.sourcedAmount(...)}.
     *
     * <p>422 rather than 400: the rule itself, not the current request, carries the invalid value —
     * the same "invariant violation inside the stored rule definition" reasoning as
     * {@link #FIN_422_REMAINDER_MARKER}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_INVALID_PERCENTAGE_VALUE = "FIN-422-INVALID-PERCENTAGE-VALUE";

    /**
     * RULE-FIN-017 — the submitted {@code periodId} belongs to a different fiscal year than the
     * submitted {@code fiscalYearId} (DBF-FIN-076 makes the period's year a stored fact). Thrown
     * by {@code JournalEntryDomain.assertHeaderCoherent(...)}.
     *
     * <p>400 rather than 409/422: nothing about the stored ledger is in conflict — both rows exist
     * and are individually valid; the request's own three header fields are mutually inconsistent,
     * which is the taxonomy's structural {@code VALIDATION_ERROR} row.
     * API: API-FIN-019. HTTP 400 ({@code Status.VALIDATION_ERROR}).
     */
    public static final String FIN_400_PERIOD_NOT_IN_YEAR = "FIN-400-PERIOD-NOT-IN-YEAR";

    /**
     * RULE-FIN-017 — the submitted {@code docDate} falls outside the submitted period's
     * {@code [startDate, endDate]} span (DBF-FIN-080/081, both NOT NULL). Thrown by
     * {@code JournalEntryDomain.assertHeaderCoherent(...)}.
     *
     * <p>400 for the same reason as {@link #FIN_400_PERIOD_NOT_IN_YEAR}.
     * API: API-FIN-019. HTTP 400 ({@code Status.VALIDATION_ERROR}).
     */
    public static final String FIN_400_DOCDATE_OUTSIDE_PERIOD = "FIN-400-DOCDATE-OUTSIDE-PERIOD";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // ERROR ENVELOPE closure — the one catalog row that had never reached the wire. Registered
    // here so FIN's 403 carries a FIN code like every other FIN error, instead of the platform
    // ACCESS_DENIED envelope. Thrown only by {@code com.erp.fin.security.FinForbiddenAdvisor}.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * PLATFORM-STD — the caller holds no grant for the module/screen/action this endpoint
     * requires, so the {@code @PreAuthorize} guard on the FIN service denied the call. Raised by
     * {@code FinForbiddenAdvisor}, which catches Spring Security's {@code AccessDeniedException}
     * on any {@code com.erp.fin.service} method and re-raises it as this code — never thrown by
     * hand in a service. Distinct from {@link #FIN_403_SOD_VIOLATION}, a struck code with no
     * thrower.
     *
     * <p>A denial raised by the Spring Security filter chain, before any FIN service is entered,
     * is NOT this code — it is written by {@code SecSecurityErrorHandler} as
     * {@code SEC-403-FORBIDDEN} and never reaches {@code GlobalExceptionHandler}.
     * API: every secured API. HTTP 403 ({@code Status.FORBIDDEN}).
     */
    public static final String FIN_403_FORBIDDEN = "FIN-403-FORBIDDEN";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // DEACTIVATE RUN GATE — the one code the run-gate decision adds. Recorded human decision,
    // NOT an SRS rule: RULE-FIN-001..017 were each read and none of them states that a
    // deactivated definition may not be run (the closest, RULE-FIN-005, is about an event having
    // no ACTIVE EventTypeRule — a different entity and a different trigger). Before this row,
    // API-FIN-036/037 set IS_ACTIVE_FL to false and both run paths ignored it, so a retired
    // template or allocation rule still posted.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * RECORDED HUMAN DECISION (no RULE-FIN-* states it) — the definition row the caller asked to
     * run exists but is deactivated ({@code IS_ACTIVE_FL = false}), so the run is refused and no
     * journal entry is built or posted. Thrown by
     * {@code RecurringTemplateDomain.assertCanRun()} and
     * {@code AllocationRuleDomain.assertCanRun()}.
     *
     * <p><b>Not a 404.</b> {@link #FIN_404_TEMPLATE} and {@link #FIN_404_ALLOCATION_RULE} mean
     * "no such row"; the row here exists and is readable through API-FIN-012 / API-FIN-015, so
     * raising either would tell the client something untrue.
     *
     * <p><b>One row, not one per entity.</b> The catalog assigns a distinct 404 per entity
     * (FIN-404-ACCOUNT, FIN-404-DIMENSION, FIN-404-TEMPLATE, …) and a distinct duplicate code per
     * unique constraint, because in both cases WHICH entity is the entire information content.
     * Its "the row is in the wrong state" rows behave the opposite way: they are semantic, not
     * entity-scoped, and are reused verbatim the moment the identical semantic reappears on a
     * second entity — {@link #FIN_409_INVALID_TRANSITION} is shared by the period soft-close and
     * the fiscal-year year-end close, and {@link #FIN_409_REMAINDER_COUNT} /
     * {@link #FIN_422_REMAINDER_MARKER} are each shared by ENT-FIN-010 rule lines and
     * ENT-FIN-014 allocation targets. "The definition you named is deactivated" is one semantic
     * on two entities, and the caller already knows which one from the path it called.
     *
     * <p><b>409 rather than 422.</b> Every FIN refusal whose cause is the target row's own state
     * is {@code Status.CONFLICT} — {@link #FIN_409_NOT_POSTED}, {@link #FIN_409_NOT_REOPENABLE},
     * {@link #FIN_409_PERIOD_NOT_OPEN}, {@link #FIN_409_INVALID_TRANSITION}. FIN reserves 422 for
     * an invariant violated INSIDE a stored rule definition's own data
     * ({@link #FIN_422_REMAINDER_MARKER}, {@link #FIN_422_INVALID_PERCENTAGE_VALUE}). This is the
     * former.
     * APIs: API-FIN-014, API-FIN-017. HTTP 409 ({@code Status.CONFLICT}).
     */
    public static final String FIN_409_NOT_ACTIVE = "FIN-409-NOT-ACTIVE";

    /**
     * RULE-FIN-022 — an active mapping already holds the (event type, business field, business
     * value) key: by the pre-check (QR-FIN-052) or by the store's refusal on
     * {@code UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY}, translated by {@code FinPersistenceRefusalAdvisor}
     * (ADR-FIN-030). API: API-FIN-039. HTTP 409 ({@code Status.CONFLICT}).
     */
    public static final String FIN_409_MAPPING_DUP = "FIN-409-MAPPING-DUP";

    /**
     * RULE-FIN-018 — a rule line's accountDerivationTypeCode is not an active
     * ACCOUNT_DERIVATION_TYPE value (DIRECT included, retired in MDL). Thrown by
     * {@code EventTypeRuleDomain.assertDerivationSupported(...)}.
     * API: API-FIN-011. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_UNSUPPORTED_DERIVATION = "FIN-422-UNSUPPORTED-DERIVATION";

    /**
     * RULE-FIN-019 — a CONSTANT line without an account code or with a business field, or a
     * MAPPING line without a business field or with an account code. Thrown by
     * {@code EventTypeRuleDomain.assertDerivationSpec(...)}.
     * API: API-FIN-011. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_DERIVATION_SPEC = "FIN-422-DERIVATION-SPEC";

    /**
     * RULE-FIN-020 — no ACTIVE account mapping for the rule's event type, the line's business
     * field and the event's value (QR-FIN-055). Thrown by
     * {@code AccountMappingDomain.assertResolved(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_UNMAPPED_VALUE = "FIN-422-UNMAPPED-VALUE";

    /**
     * RULE-FIN-021 — the event does not carry the business field a MAPPING line or a
     * BUSINESS_FIELD tag names. Thrown by {@code EventTypeRuleDomain.assertFieldPresent(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_MISSING_BUSINESS_FIELD = "FIN-422-MISSING-BUSINESS-FIELD";

    /**
     * RULE-FIN-023 — the mapping's account is not a leaf or is inactive when the mapping is saved
     * (QR-FIN-060). Thrown by {@code AccountMappingDomain.assertAccountEligible(...)}.
     * APIs: API-FIN-039, API-FIN-040. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_MAPPED_ACCOUNT_INELIGIBLE = "FIN-422-MAPPED-ACCOUNT-INELIGIBLE";

    /**
     * RULE-FIN-024 — businessFieldCode is PAYMENT_METHOD and businessValue is not an active
     * PAYMENT_METHOD code. Thrown by {@code AccountMappingDomain.assertBusinessValueDefined(...)}.
     * API: API-FIN-039. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_INVALID_PAYMENT_METHOD = "FIN-422-INVALID-PAYMENT-METHOD";

    /**
     * RULE-FIN-025 — two tags of one submitted rule line name the same dimension. Thrown by
     * {@code EventTypeRuleDomain.assertTagsDistinct(...)}.
     * API: API-FIN-011. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_DIMENSION_TAG_DUP = "FIN-422-DIMENSION-TAG-DUP";

    /**
     * RULE-FIN-026 — a CONSTANT tag without a value of its own dimension or with a business
     * field, or a BUSINESS_FIELD tag without a business field or with a value. Thrown by
     * {@code EventTypeRuleDomain.assertTagMatchesSource(...)}.
     * API: API-FIN-011. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_DIMENSION_TAG_SOURCE = "FIN-422-DIMENSION-TAG-SOURCE";

    /**
     * RULE-FIN-027 — the event's value for a BUSINESS_FIELD tag equals the code of no dimension
     * value of the tagged dimension (QR-FIN-058). Thrown by
     * {@code DimensionValueDomain.assertResolvedByCode(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_UNRESOLVED_DIMENSION_VALUE = "FIN-422-UNRESOLVED-DIMENSION-VALUE";

    /**
     * RULE-FIN-028 — no fiscal period's [startDate, endDate] covers the event's docDate
     * (QR-FIN-059); a calendar-setup error, never the retryable period-not-open answer. Thrown by
     * {@code FiscalPeriodDomain.assertDateCovered(...)}.
     * API: API-FIN-020. HTTP 422 ({@code Status.BUSINESS_RULE_VIOLATION}).
     */
    public static final String FIN_422_NO_DEFINED_PERIOD = "FIN-422-NO-DEFINED-PERIOD";

    /**
     * PLATFORM-STD (ADR-FIN-030) — unknown account-mapping id in the path.
     * APIs: API-FIN-040, API-FIN-041. HTTP 404 ({@code Status.NOT_FOUND}).
     */
    public static final String FIN_404_ACCOUNT_MAPPING = "FIN-404-ACCOUNT-MAPPING";
}
