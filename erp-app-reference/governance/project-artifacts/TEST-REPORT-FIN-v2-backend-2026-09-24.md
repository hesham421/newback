# TEST REPORT — FIN v2 backend — 2026-09-24

Module-scoped digest for the **api-verify half** of the FIN v2 backend test phase
(`TEST-PLAN-BE`). Distinct from api-verify's own raw output, which stays untouched at
`governance/shared/backend/modules/FIN/v2/test-api/fin_problems_report.md`.

Supersedes nothing: the 2026-09-23 digest is left in place as the record of the previous
run. This run is the remediation of the fifteen items that run's own review found.

| | |
|---|---|
| Mechanism | `api-verify` skill, **Full tier** (api-docs + `test-execution-manifest-fin.md` both present) |
| Governed plan | `governance/shared/backend/modules/FIN/packages/v2/backend-test/` — Shape A, **delivered and verified** (`RULE-SCENARIOS.md` 45 TC blocks + `API-SCENARIOS.md` 44 TC blocks = **89** unique TCs, `state.json` `"verified": true`) |
| Script | `governance/shared/backend/modules/FIN/v2/test-api/test_fin_apis.py` (regenerated this run) |
| Raw report | `governance/shared/backend/modules/FIN/v2/test-api/fin_problems_report.md` |
| Target | `http://localhost:7272` (Dev/Test — localhost, `api-verify-config.md` §4.1) |
| Run | RUN_ID `195569`, 2026-09-24 |
| Result | **126 asserted checks passed · 0 failed · 2 named skips · 1 observation** across 21 suites |

> The header of `TEST-PLAN-BE-HEADER.md` says "TC count is 52" and the previous
> `test_phases_note` says 90. Both are wrong; the delivered block count is **89**, counted
> independently twice. Already filed as `api_doc_gaps[6]`, factory-owned, still OPEN.

---

## 1. STEP 1.9 — GOVERNED PLAN ↔ API-VERIFY COVERAGE (all 89 rows)

Scoring rule applied, from `api-verify/SKILL.md` §3-E and its Description: an acceptance
criterion this instrument cannot exercise is **skipped and named as skipped**, never a
PASS. A case closed in the JUnit track is **not** an api-verify pass either and is marked
`COVERED (JUnit: Class#method)`, so the table says which instrument actually covers what.

| TC | sub | traces | scenario | api-verify ref | result |
|---|---|---|---|---|---|
| TC-FIN-001 | API | AC-FIN-001,REQ-FIN-001,API-FIN-002 | an account is created inside the chart hierarchy | `test_account` | **PASS** (api-verify) |
| TC-FIN-002 | RULE | AC-FIN-002,REQ-FIN-002,API-FIN-003 | an account with children cannot be marked as accepting direct posting | `test_account` | **PASS** (api-verify) |
| TC-FIN-003 | RULE | AC-FIN-003,REQ-FIN-003,API-FIN-004 | deactivating an account blocks every later posting to it | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-004 | API | AC-FIN-004,REQ-FIN-004,API-FIN-006 | a dimension is created and is active | `test_dimension` | **PASS** (api-verify) |
| TC-FIN-005 | API | AC-FIN-005,REQ-FIN-005,API-FIN-007 | a value is created under its dimension | `test_dimension_value` | **PASS** (api-verify) |
| TC-FIN-006 | RULE | AC-FIN-006,REQ-FIN-006,API-FIN-007 | a duplicate code under the same dimension is refused | `test_dimension_value` | **PASS** (api-verify) |
| TC-FIN-007 | API | AC-FIN-007,REQ-FIN-007,API-FIN-010 | an event type gets its single active rule | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-008 | API | AC-FIN-008,REQ-FIN-008,API-FIN-011 | a rule line records its three references separately | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-009 | RULE | AC-FIN-009,REQ-FIN-009,API-FIN-011 | a percentage line set with no remainder line is refused | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-010 | API | AC-FIN-010,REQ-FIN-010,API-FIN-020 | an arriving event is turned into an entry by its type's rule | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-011 | RULE | AC-FIN-011,REQ-FIN-011,API-FIN-020 | a repeated event reference never produces a second entry | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-012 | RULE | AC-FIN-012,REQ-FIN-012,API-FIN-020 | the remainder line absorbs the rounding difference exactly | `test_event_distribution` | **PASS** (api-verify) · also COVERED (JUnit: FinEventEntryV2IntegrationTest#aThirtyThreePercentPairLeavesExactlyThirtyFourOnTheRemainderLine) |
| TC-FIN-013 | RULE | AC-FIN-013,REQ-FIN-013,API-FIN-020 | an event whose type has no active rule is refused | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-014 | API | AC-FIN-014,REQ-FIN-014,API-FIN-019 | a manual entry follows exactly the same path as any other source | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-015 | RULE | AC-FIN-015,REQ-FIN-015,API-FIN-019 | two failing validations are both returned and nothing is posted | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-016 | RULE | AC-FIN-016,REQ-FIN-016,API-FIN-022 | a POSTED entry cannot be deleted; only a reversal changes its effect | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-017 | RULE | AC-FIN-017,REQ-FIN-017,API-FIN-019 | a DRAFT that passes every check posts directly and locks | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-018 | RULE | AC-FIN-018,REQ-FIN-018,API-FIN-019 | an entry out of balance by the smallest currency unit is refused | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-019 | RULE | AC-FIN-019,REQ-FIN-019,API-FIN-019 | a line targeting a rollup account is refused, naming the line | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-020 | RULE | AC-FIN-020,REQ-FIN-020,API-FIN-019 | the period gate is read at post time, not at build time | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-021 | RULE | AC-FIN-021,REQ-FIN-021,API-FIN-019 | a line citing an inactive dimension value is refused | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-022 | API | AC-FIN-022,REQ-FIN-022,API-FIN-013 | a recurring or reversing template is stored with its schedule | `test_recurring_template` | **PASS** (api-verify) |
| TC-FIN-023 | API | AC-FIN-023,REQ-FIN-023,API-FIN-014 | a due recurring template posts one entry and advances its next run date | `test_recurring_template` | **PASS** (api-verify) |
| TC-FIN-024 | API | AC-FIN-024,REQ-FIN-024,API-FIN-014 | a reversing template's entry is mirrored in the next period | `test_recurring_template` | **PASS** (api-verify) |
| TC-FIN-025 | API | AC-FIN-025,REQ-FIN-025,API-FIN-016 | an allocation rule is stored with its targets | `test_allocation_rule` | **PASS** (api-verify) |
| TC-FIN-026 | RULE | AC-FIN-026,REQ-FIN-026,API-FIN-017 | an allocation run distributes the source balance to the last unit | `test_allocation_rule` | **PASS** (api-verify) · also COVERED (JUnit: FinAllocationRoundingIntegrationTest#anAllocationRunDistributesTheWholeSourceBalanceToTheLastUnit) |
| TC-FIN-027 | API | AC-FIN-027,REQ-FIN-027,API-FIN-018 | a filtered search returns exactly the matching entries, unmodified | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-028 | API | AC-FIN-028,REQ-FIN-028,API-FIN-021 | a reversal mirrors the original line for line and links both ways | `test_reversal` | **PASS** (api-verify) |
| TC-FIN-029 | RULE | AC-FIN-029,REQ-FIN-029,API-FIN-021 | a reversal of an entry in a closed period posts into the current open period | `test_reversal` | **PASS** (api-verify) |
| TC-FIN-030 | RULE | AC-FIN-030,REQ-FIN-030,API-FIN-021 | an entry that already carries a reversal cannot be reversed again | `test_reversal` | **PASS** (api-verify) |
| TC-FIN-031 | API | AC-FIN-031,REQ-FIN-031,API-FIN-023 | a fiscal year is created with its periods, each Open | `test_fiscal_year` | **PASS** (api-verify) |
| TC-FIN-032 | RULE | AC-FIN-032,REQ-FIN-032,API-FIN-024 | a soft-closed period reopens | `test_fiscal_period` | **PASS** (api-verify) |
| TC-FIN-033 | RULE | AC-FIN-033,REQ-FIN-033,API-FIN-025 | soft-closing a period blocks normal posting into it | `test_fiscal_period` | **PASS** (api-verify) |
| TC-FIN-034 | RULE | AC-FIN-034,REQ-FIN-034,API-FIN-026 | hard-closing a period is permanent | `test_fiscal_period` | **PASS** (api-verify) |
| TC-FIN-035 | RULE | AC-FIN-035,REQ-FIN-035,API-FIN-024 | a hard-closed period never reopens | `test_fiscal_period` | **PASS** (api-verify) |
| TC-FIN-036 | API | AC-FIN-036,REQ-FIN-036,API-FIN-027 | year-end close posts a balanced closing entry and the next year's opening entry | `test_year_end_close` | **PASS** (api-verify) · also COVERED (JUnit: FinYearEndCoverageIntegrationTest#yearEndClose_postsABalancedClosingAndABalancedOpeningEntryFromTheYearsBalances) |
| TC-FIN-037 | API | AC-FIN-037,REQ-FIN-037,API-FIN-026 | the close approval is recorded as an act of its own | `test_fiscal_period` | **PASS** (api-verify) |
| TC-FIN-038 | API | AC-FIN-038,REQ-FIN-038,API-FIN-026 | entry-creation permission alone does not buy a period close | `test_permission_gate (skipped)` | **SKIPPED** in api-verify (named, not a pass) · **COVERED (JUnit: FinSoDCoverageIntegrationTest#hardClose_isDeniedForAPrincipalHoldingOnlyTheEntryCreationPermission)** |
| TC-FIN-039 | API | AC-FIN-039,REQ-FIN-039,API-FIN-028 | the account ledger is computed live from posted lines | `test_reports` | **PASS** (api-verify) |
| TC-FIN-040 | API | AC-FIN-040,REQ-FIN-040,API-FIN-029 | the trial balance always balances | `test_reports` | **PASS** (api-verify) |
| TC-FIN-041 | API | AC-FIN-041,REQ-FIN-041,API-FIN-030 | the new year's opening balances equal the prior year's closing balances | `test_reports` | api-verify: reachability smoke only · **COVERED (JUnit: FinYearEndContinuityIntegrationTest#everyBalanceSheetAccountOpensTheNewYearAtItsPriorYearClosingBalance)** |
| TC-FIN-042 | API | AC-FIN-042,REQ-FIN-042,API-FIN-031 | the income statement opens a new year at zero | `test_reports` | **PASS** (api-verify) · also COVERED (JUnit: FinYearEndContinuityIntegrationTest#everyResultAccountStandsAtZeroOnceTheYearIsClosed) |
| TC-FIN-043 | API | AC-FIN-043,REQ-FIN-043,API-FIN-032 | a dimension report never merges two dimension values into one account row | `test_reports` | **PASS** (api-verify) |
| TC-FIN-044 | API | AC-FIN-044,REQ-FIN-044 | FIN registers itself into SEC as data | `test_registration` | **PASS** (api-verify) · also COVERED (JUnit: FinRegistrationInSecIntegrationTest#finIsRegisteredIntoSecWithOneModuleItsScreensAndItsActions) |
| TC-FIN-045 | API | AC-FIN-045,REQ-FIN-045 | FIN registers its fifteen lookup types into MDL | `test_registration` | **PASS** (api-verify) |
| TC-FIN-046 | API | AC-FIN-046,REQ-FIN-046,API-FIN-030,API-FIN-029,API-FIN-028,API-FIN-022 | a statement line drills all the way down to its source event | `test_reports` | **PASS** (api-verify) |
| TC-FIN-047 | RULE | AC-FIN-009,REQ-FIN-009,API-FIN-011 | two remainder lines are refused as surely as none | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-048 | RULE | AC-FIN-012,REQ-FIN-012,API-FIN-020 | a remainder that computes to zero or less is refused | `test_event_distribution` | **PASS** (api-verify) |
| TC-FIN-049 | RULE | AC-FIN-014,REQ-FIN-014,API-FIN-019 | a manual entry with no lines never reaches the rules | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-050 | API | AC-FIN-027,REQ-FIN-027,API-FIN-018 | a filter matching nothing is a successful empty page | `test_journal_entry` | **PASS** (api-verify) |
| TC-FIN-051 | API | AC-FIN-031,REQ-FIN-031,API-FIN-023 | a period count other than twelve falls back to an even day split | `test_fiscal_year` | **PASS** (api-verify) |
| TC-FIN-052 | API | AC-FIN-038,REQ-FIN-038,API-FIN-027 | the same permission gates the year-end close | `test_permission_gate (skipped)` | **SKIPPED** in api-verify (named, not a pass) · **COVERED (JUnit: FinSoDCoverageIntegrationTest#yearEndClose_isDeniedForAPrincipalHoldingOnlyTheEntryCreationPermission)** |
| TC-FIN-093 | API | AC-FIN-047,REQ-FIN-047,API-FIN-020 | an account code carried in the event payload is never used | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-094 | RULE | AC-FIN-048,REQ-FIN-048,API-FIN-011 | a DIRECT rule line is refused as an unsupported derivation | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-095 | API | AC-FIN-049,REQ-FIN-049,API-FIN-011 | a MAPPING rule line records the business field that selects its account | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-096 | API | AC-FIN-050,REQ-FIN-050,API-FIN-020 | a cheque payment resolves to the bank account through its mapping | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-097 | API | AC-FIN-051,REQ-FIN-050,API-FIN-020 | a cash payment resolves to the cash account through its mapping | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-098 | API | AC-FIN-052,REQ-FIN-050,API-FIN-020 | an expense type resolves to its own expense account | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-099 | RULE | AC-FIN-053,REQ-FIN-051,API-FIN-020 | an event missing the business field its MAPPING line reads is refused | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-100 | RULE | AC-FIN-054,REQ-FIN-052,API-FIN-020 | a mapped account that has since gained a child is refused at post time | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-101 | RULE | AC-FIN-055,REQ-FIN-052,API-FIN-020 | a mapped account that has since been deactivated is refused at post time | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-102 | RULE | AC-FIN-056,REQ-FIN-053,API-FIN-020 | a business value with no mapping is refused, never defaulted | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-103 | RULE | AC-FIN-057,REQ-FIN-053,API-FIN-020 | an inactive mapping counts as no mapping | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-104 | API | AC-FIN-058,REQ-FIN-054,API-FIN-039 | an account mapping is created active | `test_account_mapping` | **PASS** (api-verify) |
| TC-FIN-105 | API | AC-FIN-059,REQ-FIN-055,API-FIN-038 | a mapping search returns exactly the rows its filters match | `test_account_mapping, —` | **PASS** (api-verify) |
| TC-FIN-106 | API | AC-FIN-060,REQ-FIN-056,API-FIN-040 | changing a mapping's account leaves its key untouched | `test_account_mapping` | **PASS** (api-verify) |
| TC-FIN-107 | RULE | AC-FIN-061,REQ-FIN-057,API-FIN-041,API-FIN-020 | a deactivated mapping stops resolving from the next event on | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-108 | RULE | AC-FIN-062,REQ-FIN-058,API-FIN-040,API-FIN-022,API-FIN-028 | changing a mapping never reaches an entry already posted through it | `test_event_mapping` | **PASS** (api-verify) |
| TC-FIN-109 | RULE | AC-FIN-063,REQ-FIN-059,API-FIN-039 | a second active mapping for the same key is refused | `test_account_mapping` | **PASS** (api-verify) |
| TC-FIN-110 | RULE | AC-FIN-064,REQ-FIN-060,API-FIN-039 | a mapping to a parent account is refused when it is saved | `test_account_mapping` | **PASS** (api-verify) |
| TC-FIN-111 | RULE | AC-FIN-065,REQ-FIN-061,API-FIN-039 | a misspelt payment method is refused as a mapping value | `test_account_mapping` | **PASS** (api-verify) |
| TC-FIN-112 | API | AC-FIN-066,REQ-FIN-062,API-FIN-011 | a rule line is saved with its two dimension tags | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-113 | API | AC-FIN-067,REQ-FIN-063,API-FIN-020 | a CONSTANT tag stamps its value on the event-built line | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-114 | API | AC-FIN-068,REQ-FIN-063,API-FIN-020 | a BUSINESS_FIELD tag resolves its value by code from the event | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-115 | RULE | AC-FIN-069,REQ-FIN-064,API-FIN-011 | a rule line tagging one dimension twice is refused | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-116 | RULE | AC-FIN-070,REQ-FIN-065,API-FIN-011 | a CONSTANT tag citing another dimension's value is refused | `test_event_rule` | **PASS** (api-verify) |
| TC-FIN-117 | API | AC-FIN-071,REQ-FIN-066,API-FIN-020 | both resolved dimension values are validated and written on the line | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-118 | RULE | AC-FIN-072,REQ-FIN-067,API-FIN-020 | an event resolving to an inactive dimension value is refused | `test_dimension_value, test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-119 | RULE | AC-FIN-073,REQ-FIN-068,API-FIN-020 | an event missing the business field a dimension tag names is refused | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-120 | RULE | AC-FIN-074,REQ-FIN-069,API-FIN-020 | an event dimension value that matches no defined code is refused | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-121 | API | AC-FIN-075,REQ-FIN-070,API-FIN-022 | an event-built line and a manual line read back in one dimension shape | `test_event_dimension_tags` | **PASS** (api-verify) |
| TC-FIN-122 | API | AC-FIN-076,REQ-FIN-071,API-FIN-032 | the dimension report groups event-built and manual lines together | `test_reports` | **PASS** (api-verify) · also COVERED (JUnit: FinEventEntryV2IntegrationTest#theDimensionReportSumsEventBuiltAndManualLinesIntoOneRow) |
| TC-FIN-123 | RULE | AC-FIN-077,REQ-FIN-072,API-FIN-020 | a repeated event reference is answered as a duplicate | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-124 | RULE | AC-FIN-078,REQ-FIN-072,API-FIN-020 | two simultaneous events with one reference post once and never answer 500 | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-125 | RULE | AC-FIN-079,REQ-FIN-073,API-FIN-020 | an event into a soft-closed period gets the distinct period-not-open code | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-126 | RULE | AC-FIN-080,REQ-FIN-074,API-FIN-020 | an event date no period covers is a configuration error, not a retry signal | `test_event_entry` | **PASS** (api-verify) |
| TC-FIN-127 | API | AC-FIN-081,REQ-FIN-075,API-FIN-020 | the published catalogue documents the period-not-open code once | `test_published_catalogue` | **PASS** (api-verify) |
| TC-FIN-128 | RULE | AC-FIN-082,REQ-FIN-076,API-FIN-020 | a MAPPING remainder line still absorbs the rounding difference exactly | `test_event_distribution` | **PASS** (api-verify) · also COVERED (JUnit: FinEventEntryV2IntegrationTest#aMappedRemainderLineStillLeavesExactlyThirtyFourOnTheMappedAccount) |
| TC-FIN-129 | API | AC-FIN-083,REQ-FIN-077,API-FIN-021,API-FIN-028,API-FIN-032 | an entry posted through a mapping and tags reverses like any other | `test_event_dimension_tags` | **PASS** (api-verify) · also COVERED (JUnit: FinEventEntryV2IntegrationTest#aMappedAndTaggedEntryReversesAccountsAmountsAndDimensionValuesToZero) |

**Coverage ratio: 89/89 REQUIRED-COVERAGE TCs referenced (100%).**

Broken down by the instrument that actually asserts the acceptance criterion:

| Outcome | Count | TCs |
|---|---|---|
| api-verify **PASS** (sole instrument) | 78 | all rows not listed below |
| api-verify **PASS**, also covered in JUnit | 9 | TC-FIN-012, 026, 036, 042, 044, 122, 128, 129 (and TC-FIN-042 only thinly in api-verify — see §3) |
| **COVERED (JUnit only)** — api-verify does reachability, not the AC | 1 | TC-FIN-041 |
| **SKIPPED in api-verify, COVERED (JUnit)** | 2 | TC-FIN-038, TC-FIN-052 |
| **GAP** (no instrument) | 0 | — |
| **FAIL** | 0 | — |

TC-FIN-038 and TC-FIN-052 are **no longer BLOCKED**: `FinSoDCoverageIntegrationTest`
asserts the `FIN-403-FORBIDDEN` denial itself, which api-verify cannot reach without
minting a SEC user and role — outside `SKILL.md` §3-I's single bounded exception.

---

## 2. The fifteen remediation items

| # | Item | Closed? | Assertion now in the script | Run result |
|---|---|---|---|---|
| 1 | TC-FIN-024 — `rev` computed then discarded | **Yes** | The mirror is found through the original's own `reversalEntryId` (API-FIN-022), **not** by searching `originalEntryId` — that field is not in API-FIN-018's documented filter set and is rejected 400. Asserts: exactly one linked mirror, it links back, line-for-line same amounts with the **opposite** `directionCode`, the mirror's `periodId` is **period N+1** in `ctx["periods"]` order, both entries individually balanced | PASS — entry in period 509, mirror in period 510, all four sub-assertions true |
| 2 | TC-FIN-033 — mandated step 2 never emitted | **Yes** | Step 1 asserts `statusCode == SOFT_CLOSE` on the PATCH; step 2 posts a balanced manual entry into that period and asserts `409 / FIN-409-PERIOD-NOT-OPEN` — the manifest's own `RULE-FIN-008 \| FIN-409-PERIOD-NOT-OPEN \| … TC-FIN-033 …` triple | PASS — `statusCode=SOFT_CLOSE`, then `409 / FIN-409-PERIOD-NOT-OPEN` |
| 3 | TC-FIN-039 — only HTTP 200 | **Yes** | `rows[].runningBalance` asserted row-to-row against `rows[].signedAmount`; every row carries `journalEntryId`; step 2 posts one further entry on the same account and asserts the row count is **+1** | PASS — 19 rows, running balance consistent, 19 → 20 after the further posting |
| 4 | TC-FIN-040 — wrong documented fields | **Yes** | Uses `totalDebitBalance` / `totalCreditBalance` / `balanced` (the balance columns AC-FIN-040 is about), cross-checks them against the sum of `rows[].debitBalance` / `creditBalance`, and guards `rows > 0`. Run both with `periodId` and with it omitted, per the TC's two steps | PASS — with period: 2 rows, 5011.0 / 5011.0, `balanced=true`; without: 18 rows, 20636.0675 / 20636.0675 |
| 5 | TC-FIN-043 — only HTTP 200 | **Yes** | Creates a second value of the same dimension, posts one balanced entry with **two DEBIT lines on the same account** under the two values, then asserts the report returns **exactly 2 rows** for that account with the two distinct `dimensionValueId`s — the api-docs' own grouping key, "one per account and dimension value" | PASS — account 1016 under values 88 and 93 → 2 rows |
| 6 | TC-FIN-046 — only the last hop | **Yes** | All four hops walked in order: balance sheet → trial-balance row found **by the `accountId` hop 1 handed over** → account ledger → `rows[].journalEntryId` → the entry → its `eventReference` | PASS — 1014 → row found → journalEntryId 336 → `eventReference 'EVT-195481-A'` |
| 7 | TC-FIN-105 — inactive row never created | **Yes, with a named substitution** | A **third, inactive** mapping is now created on the same event type before the search, and the search filters `eventTypeCode` + `isActiveFl=true`, asserting exactly 2 rows, `totalElements==2`, the inactive row excluded and no foreign event type in the page | PASS — see §3 for why the third row is keyed `EXPENSE_TYPE_CODE` rather than `PAYMENT_METHOD=TRANSFER` |
| 8 | TC-FIN-107 — no `isActiveFl` assertion | **Yes** | Step 1 now asserts `isActiveFl is False` on the API-FIN-041 response ("the row is kept, never hard-deleted") before step 2's `FIN-422-UNMAPPED-VALUE` | PASS |
| 9 | TC-FIN-108 — step 3 missing | **Yes** | After the mapping is repointed, both ledgers are read (API-FIN-028, named in the TC's own `traces=`): the **original** account's ledger still contains the entry, the **new** account's does not | PASS — ledger of 1017 contains entry 314 = true; ledger of 1018 = false |
| 10 | TC-FIN-121 — near-tautological keyset compare | **Yes** | A dedicated **one-tag** rule is built (the two-tag rule of TC-FIN-112/117 cannot serve this case — its line carries 2 rows, which TC-FIN-117 asserts). Asserts **exactly 1** dimension row on each debit line and the **same `dimensionValueId`** on both sides | PASS — event-built `[(66, 89)]`, manual `[(66, 89)]` |
| 11 | TC-FIN-129 structure | **Yes** | A new rule with a **MAPPING** debit line **and** a BRANCH tag; asserts the debit really resolved through the mapping, every line carries the BRANCH tag, the reversal mirrors accounts/amounts with the opposite direction, the two are linked both ways, and the reversal's `dimensionValueId`s **equal** the original's | PASS — original 320 / reversal 321, dimension values `[89, 89]` both sides |
| 12 | TC-FIN-036 — plan↔contract divergence claim | **Yes — the claim is REFUTED, verified independently** | `api-docs/endpoints/fin-fiscal-year-management.md` line 274 reads `Shape: YearEndCloseResponse` with `closingEntry.*` and `openingEntry \| JournalEntryResponse` at :313; `FiscalYearController.java:61-67` returns `ResponseEntity<ApiResponse<YearEndCloseResponse>>`. **Not** `FiscalYearResponse`, and it **does** return both entries. The script now asserts both entries present, each individually balanced, both POSTED — and that the closing entry **actually moved** the year's result balances (real activity is posted into the year before it is closed, so "balanced" cannot hold vacuously over two empty entries) | PASS — closing 383 (640.0/640.0, 2 lines), opening 384, both POSTED, no `FIN-409-PERIOD-NOT-OPEN` over the hard-closed periods |
| 13 | TC-FIN-042 — targeted a year never closed | **Yes** | Now targets `close_successor_id` — the successor of the year `test_year_end_close` genuinely closed — and refuses to assert against a substitute if that year does not exist. `test_year_end_close` was moved **before** `test_reports` in `main()` so the close has happened | PASS on the corrected target (fiscalYearId 291). See §3 for why this assertion is still thin |
| 14 | TC-FIN-127 | **Yes — now genuinely passes** | Asserts all five properties of the single catalogue row, not just the once-only clause: the value, the en message, the ar message, `409 CONFLICT`, and the `Retryable … clears once the target period is opened` note | PASS — exactly 1 row; all five present, none missing |
| 15 | `gate_resolves_for_holder` | **Deleted** | The hard-coded `return (True, …)` that made no call is gone. TC-FIN-038 / TC-FIN-052 are recorded through a new `skipped()` recorder that never enters the pass/fail totals and names the JUnit test that does cover them | n/a — 2 named skips in the report |

Both traps were handled. The `PERCENTAGE`-without-remainder refusal is left as the correct
behaviour it is (`FIN-409-REMAINDER-COUNT`, RULE-FIN-003) and the distribution fixtures
source their amount as `AMOUNT_SOURCE_TYPE = PERCENTAGE` while distributing `FIXED`.
The allocation precondition is now asserted **explicitly** — a new check proves an OPEN
fiscal period covers today before TC-FIN-026 runs, so a miss reads as a precondition, not
as a mysterious `FIN-404-PERIOD`.

---

## 3. Findings

### 3.1 No application defect was exposed

Every deeper assertion added above passed against the running application. Nothing in this
run produces an observed-vs-expected mismatch attributable to `src/main/`. That is the
honest headline: the fifteen items were defects **in the verification**, not in the code
it verifies.

### 3.2 `DB_PRECONDITION` — the host `PAYMENT_METHOD` value `TRANSFER` is retired in this environment

- **Observed**: `GET /api/v1/mdl/lookups?type=PAYMENT_METHOD` returns `CASH`, `CHEQUE` only.
  `POST /api/v1/mdl/lookup-types/{id}/values` with `code=TRANSFER` answers
  `409 MDL-409-VALUE-DUP` — the row exists but is inactive.
- **Expected**: the manifest's DEPENDENCY ORDER step 0 states that "every `PAYMENT_METHOD`
  value but `CHEQUE` (`CASH`, `TRANSFER`) are added in MDL by the deploying organisation",
  and TC-FIN-105's Preconditions require `TRANSFER` present so its mapping can be saved.
- **Cause, stated plainly**: an earlier api-verify run seeded `TRANSFER` and then retired
  it in its own teardown. MDL publishes **no activate endpoint** for a lookup value
  (`LookupValueController`: create / update{nameAr,nameEn,sortOrder} / `DELETE`=deactivate
  / reorder / search), so it cannot be restored through the API. This is exactly the
  permanent-residue hazard `SKILL.md` §3-F warns about, realised.
- **Handled**: `TRANSFER` is no longer added to this run's teardown list — it is host
  reference data, not run residue. The row is left in place when seeding succeeds. When it
  is already retired, the run records a precondition note with **suggested SQL it does not
  execute** (`SKILL.md` §3-H), and TC-FIN-105 builds its third inactive row on
  `EXPENSE_TYPE_CODE` instead, a business field `FIN_EVENT_BUSINESS_FIELD` holds and this
  run already uses. What TC-FIN-105 asserts is unchanged.
- **Suggested SQL for a human, NOT executed**:
  ```sql
  UPDATE MDL_LOOKUP_VALUE SET IS_ACTIVE_FL = TRUE
   WHERE CODE = 'TRANSFER'
     AND LOOKUP_TYPE_ID = (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE
                            WHERE KEY = 'PAYMENT_METHOD');
  ```
  Taxonomy: `DB_PRECONDITION`.

### 3.3 Two assertions that pass but remain thin — stated, not hidden

- **TC-FIN-042.** The income statement of the closed year's successor returns `groups`
  `['EXPENSE','REVENUE']` with **0 rows** and `netResult = 0`. "Every revenue and expense
  account shows a zero balance" therefore holds over an empty set. The target year is now
  correct (item 13) and no substitute is accepted, but the substantive assertion lives in
  the JUnit track — `FinYearEndContinuityIntegrationTest#everyResultAccountStandsAtZero
  OnceTheYearIsClosed` — which walks the accounts themselves.
- **TC-FIN-041.** api-verify only proves API-FIN-030 is reachable for a real fiscal year;
  it is labelled as a smoke check in the script and scored `COVERED (JUnit)` in §1, not as
  an api-verify pass.

### 3.4 One stage-E observation (never pass/fail)

A second hard-close on an already hard-closed period answers `409 / FIN-409-NOT-REOPENABLE`.
Which of `FIN-409-INVALID-TRANSITION` or `FIN-409-NOT-REOPENABLE` is owed is undocumented,
so it is observed rather than asserted.

---

## 4. Failure / skip taxonomy

Every failure and skip carries exactly one code from the command file's table.

| Item | Code | Detail |
|---|---|---|
| TC-FIN-038 skipped in api-verify | `TEST_STRUCTURE_FAILURE` | Needs a principal without `PERM_FIN_PERIODS_CLOSE_APPROVE`; minting a SEC user/role is outside `SKILL.md` §3-I. Not an application defect. **Covered in JUnit** — so it is a limitation of this instrument, not of the phase |
| TC-FIN-052 skipped in api-verify | `TEST_STRUCTURE_FAILURE` | Same cause, same JUnit coverage |
| `TRANSFER` unavailable as host data | `DB_PRECONDITION` | §3.2. Worked around without weakening TC-FIN-105's assertion |
| `check --function check` exit 2 | — | The single `contract-drift` FAIL is `api_doc_gaps[1]`, factory-owned, unchanged and still OPEN. `stale: PASS`, `deterministic: PASS` — the api-docs this run read are current |

**0 asserted failures.** No `MISSING_IMPLEMENTATION`, `VALIDATION_FAILURE`,
`SERVER_ERROR`, `CONTRACT_BREAK`, `API_REGRESSION`, `DATA_INTEGRITY_ISSUE` or
`BUSINESS_LOGIC_ISSUE` was raised.

---

## 5. Surviving records — the full list, not a summary

FIN publishes no hard-delete endpoint for any entity, so teardown uses the documented soft
deactivate and the rows survive. Every id from RUN_ID `195569`:

| Entity | Ids | State left |
|---|---|---|
| JournalEntry | 358, 359, 360, 361, 362, 363, 367, 368, 369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386 | **POSTED, immutable by design** (RULE-FIN-016 — no delete or void route exists) |
| AccountMapping | 48, 49, 50, 51, 52, 53, 54, 55, 56, 57 | deactivated (soft) |
| AllocationRule | 10 | deactivated (soft) |
| DimensionValue | 100, 101, 102, 103, 104, 105 | deactivated (soft) |
| RecurringTemplate | 11, 12 | deactivated (soft) |
| EventTypeRule | 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121 | deactivated (soft) |
| Account | 1033, 1034, 1035, 1036, 1037, 1038, 1039, 1040, 1041, 1042 | deactivated (soft) |
| FiscalYear | 287, 288, 289, 290, 291 — and their **40 periods** | left behind; no deactivate or delete endpoint exists (FiscalYear's only retirement is the year-end close) |
| Dimension | 69, 70 | **left behind ACTIVE** — ENT-FIN-002 publishes no deactivate endpoint at all |

### Permanent residue — reference tables other modules' rules read

MDL lookup values **155–166** (`ACCOUNTING_EVENT_TYPE`, run 195569) were each retired
through `DELETE /api/v1/mdl/lookup-values/{id}` → HTTP 200. Cleanup SQL if any is still
active: `UPDATE MDL_LOOKUP_VALUE SET IS_ACTIVE_FL = FALSE WHERE LOOKUP_VALUE_PK = <id>;`

`PAYMENT_METHOD` / `TRANSFER` was **not** touched by this run's teardown (§3.2).

### Privileges

No permission grant was created or revoked. The bootstrap `admin` already holds the FIN
permission set, so stage I was skipped entirely, no grant journal was written, and no
standing privilege was left behind.
