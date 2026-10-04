# TEST REPORT — FIN v2 (backend) — 2026-09-23

Module-scoped digest of the `/FIN/v2/execute-backend-test` run. Distinct from
`api-verify`'s own raw output, which is left untouched at
`governance/shared/backend/modules/FIN/v2/test-api/fin_problems_report.md`.

| | |
|---|---|
| Module / version | FIN, v2 (change set CS-FIN-001) |
| Mechanism | `api-verify` skill (`.claude/skills/api-verify/SKILL.md`) — the sole adopted mechanism; no TestSprite tool was called |
| **Tier** | **Full** — api-docs **and** `analysis/modules/FIN/v2/test_gen/test-execution-manifest-fin.md` (20 KB) are both present |
| Target | `http://localhost:7272` (Dev/Test, localhost — `api-verify-config.md` §4.1) |
| RUN_ID | `191620` |
| api-docs | regenerated this run: `python3 generate.py --module FIN --function generate` → 42 endpoints, 10 groups, 56 error codes, contract ids 41/42 (1 DRIFT) |
| Generated script | `governance/shared/backend/modules/FIN/v2/test-api/test_fin_apis.py` |
| Gate (STEP 0.2) | PASS — all 8 v2 execution phases COMPLETE (CORE · DATA-DOM · SVC-API · DOC · INT-C · INT-R · SEC-BE · ALIGN-BE) |

## REQUIRED COVERAGE — the true TC count

`TEST-PLAN-BE-HEADER.md` says "TC count is 52"; `state.json` lists 90 `TC:` atoms in the
`atoms[]` array header. **Neither figure is the count.** Parsed from the delivered split:

| Source | `<!-- TC:…:START -->` blocks | unique |
|---|---|---|
| `packages/v2/backend-test/RULE-SCENARIOS.md` | 45 | 45 |
| `packages/v2/backend-test/API-SCENARIOS.md` | 44 | 44 |
| **split total** | **89** | **89** |
| `analysis/modules/FIN/v2/test_gen/backend-test-plan-fin.md` (flat, Shape B) | 89 | 89 |

- The two shapes **agree exactly** — the same 89 TC ids, no symmetric difference.
- `state.json`'s `atoms[]` holds **89 unique** entries (the "90" is `45 + 45` from the
  execution-state note, which double-counts `TC-FIN-094`: it has its block in
  RULE-SCENARIOS and is *mentioned* in API-SCENARIOS prose).
- The header's "52" is stale — it is the v1 TC count carried into the v2 header.
- Flat-file-only mentions with no block: `TC-FIN-053`, `TC-FIN-092`, `TC-FIN-130` — prose
  cross-references (frontend / retired ids), not TCs of this plan.

**REQUIRED COVERAGE = 89 TCs** — TC-FIN-001…052 (minus TC-FIN-053) and TC-FIN-093…129.

## Coverage ratio

**89 / 89 referenced · 87 PASS · 2 BLOCKED · 0 GAP · 0 FAIL**

## GOVERNED PLAN ↔ API-VERIFY COVERAGE — FIN v2 (tier: Full)

| TC | sub | traces (AC / REQ / API) | scenario | test suite ref | result |
|---|---|---|---|---|---|
| TC-FIN-001 | API | AC-FIN-001,REQ-FIN-001,API-FIN-002 | an account is created inside the chart hierarchy | Account | PASS |
| TC-FIN-002 | RULE | AC-FIN-002,REQ-FIN-002,API-FIN-003 | an account with children cannot be marked as accepting direct posting | Account | PASS |
| TC-FIN-003 | RULE | AC-FIN-003,REQ-FIN-003,API-FIN-004 | deactivating an account blocks every later posting to it | JournalEntry (manual) | PASS |
| TC-FIN-004 | API | AC-FIN-004,REQ-FIN-004,API-FIN-006 | a dimension is created and is active | Dimension | PASS |
| TC-FIN-005 | API | AC-FIN-005,REQ-FIN-005,API-FIN-007 | a value is created under its dimension | DimensionValue | PASS |
| TC-FIN-006 | RULE | AC-FIN-006,REQ-FIN-006,API-FIN-007 | a duplicate code under the same dimension is refused | DimensionValue | PASS |
| TC-FIN-007 | API | AC-FIN-007,REQ-FIN-007,API-FIN-010 | an event type gets its single active rule | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-008 | API | AC-FIN-008,REQ-FIN-008,API-FIN-011 | a rule line records its three references separately | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-009 | RULE | AC-FIN-009,REQ-FIN-009,API-FIN-011 | a percentage line set with no remainder line is refused | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-010 | API | AC-FIN-010,REQ-FIN-010,API-FIN-020 | an arriving event is turned into an entry by its type's rule | JournalEntry (from event) | PASS |
| TC-FIN-011 | RULE | AC-FIN-011,REQ-FIN-011,API-FIN-020 | a repeated event reference never produces a second entry | JournalEntry (from event) | PASS |
| TC-FIN-012 | RULE | AC-FIN-012,REQ-FIN-012,API-FIN-020 | the remainder line absorbs the rounding difference exactly | Event entry — distribution / remainder | PASS |
| TC-FIN-013 | RULE | AC-FIN-013,REQ-FIN-013,API-FIN-020 | an event whose type has no active rule is refused | JournalEntry (from event) | PASS |
| TC-FIN-014 | API | AC-FIN-014,REQ-FIN-014,API-FIN-019 | a manual entry follows exactly the same path as any other source | JournalEntry (manual) | PASS |
| TC-FIN-015 | RULE | AC-FIN-015,REQ-FIN-015,API-FIN-019 | two failing validations are both returned and nothing is posted | JournalEntry (manual) | PASS |
| TC-FIN-016 | RULE | AC-FIN-016,REQ-FIN-016,API-FIN-022 | a POSTED entry cannot be deleted; only a reversal changes its effect | JournalEntry (manual) | PASS |
| TC-FIN-017 | RULE | AC-FIN-017,REQ-FIN-017,API-FIN-019 | a DRAFT that passes every check posts directly and locks | JournalEntry (manual) | PASS |
| TC-FIN-018 | RULE | AC-FIN-018,REQ-FIN-018,API-FIN-019 | an entry out of balance by the smallest currency unit is refused | JournalEntry (manual) | PASS |
| TC-FIN-019 | RULE | AC-FIN-019,REQ-FIN-019,API-FIN-019 | a line targeting a rollup account is refused, naming the line | JournalEntry (manual) | PASS |
| TC-FIN-020 | RULE | AC-FIN-020,REQ-FIN-020,API-FIN-019 | the period gate is read at post time, not at build time | JournalEntry (manual) | PASS |
| TC-FIN-021 | RULE | AC-FIN-021,REQ-FIN-021,API-FIN-019 | a line citing an inactive dimension value is refused | JournalEntry (manual) | PASS |
| TC-FIN-022 | API | AC-FIN-022,REQ-FIN-022,API-FIN-013 | a recurring or reversing template is stored with its schedule | RecurringTemplate | PASS |
| TC-FIN-023 | API | AC-FIN-023,REQ-FIN-023,API-FIN-014 | a due recurring template posts one entry and advances its next run date | RecurringTemplate | PASS |
| TC-FIN-024 | API | AC-FIN-024,REQ-FIN-024,API-FIN-014 | a reversing template's entry is mirrored in the next period | RecurringTemplate | PASS |
| TC-FIN-025 | API | AC-FIN-025,REQ-FIN-025,API-FIN-016 | an allocation rule is stored with its targets | AllocationRule | PASS |
| TC-FIN-026 | RULE | AC-FIN-026,REQ-FIN-026,API-FIN-017 | an allocation run distributes the source balance to the last unit | AllocationRule | PASS |
| TC-FIN-027 | API | AC-FIN-027,REQ-FIN-027,API-FIN-018 | a filtered search returns exactly the matching entries, unmodified | JournalEntry (manual) | PASS |
| TC-FIN-028 | API | AC-FIN-028,REQ-FIN-028,API-FIN-021 | a reversal mirrors the original line for line and links both ways | JournalEntry (reversal) | PASS |
| TC-FIN-029 | RULE | AC-FIN-029,REQ-FIN-029,API-FIN-021 | a reversal of an entry in a closed period posts into the current open period | JournalEntry (reversal) | PASS |
| TC-FIN-030 | RULE | AC-FIN-030,REQ-FIN-030,API-FIN-021 | an entry that already carries a reversal cannot be reversed again | JournalEntry (reversal) | PASS |
| TC-FIN-031 | API | AC-FIN-031,REQ-FIN-031,API-FIN-023 | a fiscal year is created with its periods, each Open | FiscalYear | PASS |
| TC-FIN-032 | RULE | AC-FIN-032,REQ-FIN-032,API-FIN-024 | a soft-closed period reopens | FiscalPeriod | PASS |
| TC-FIN-033 | RULE | AC-FIN-033,REQ-FIN-033,API-FIN-025 | soft-closing a period blocks normal posting into it | FiscalPeriod | PASS |
| TC-FIN-034 | RULE | AC-FIN-034,REQ-FIN-034,API-FIN-026 | hard-closing a period is permanent | FiscalPeriod | PASS |
| TC-FIN-035 | RULE | AC-FIN-035,REQ-FIN-035,API-FIN-024 | a hard-closed period never reopens | FiscalPeriod | PASS |
| TC-FIN-036 | API | AC-FIN-036,REQ-FIN-036,API-FIN-027 | year-end close posts a balanced closing entry and the next year's opening entry | FiscalYear year-end close | PASS |
| TC-FIN-037 | API | AC-FIN-037,REQ-FIN-037,API-FIN-026 | the close approval is recorded as an act of its own | FiscalPeriod | PASS |
| TC-FIN-038 | API | AC-FIN-038,REQ-FIN-038,API-FIN-026 | entry-creation permission alone does not buy a period close | Permission gate (RULE-FIN-015) | BLOCKED |
| TC-FIN-039 | API | AC-FIN-039,REQ-FIN-039,API-FIN-028 | the account ledger is computed live from posted lines | Reports | PASS |
| TC-FIN-040 | API | AC-FIN-040,REQ-FIN-040,API-FIN-029 | the trial balance always balances | Reports | PASS |
| TC-FIN-041 | API | AC-FIN-041,REQ-FIN-041,API-FIN-030 | the new year's opening balances equal the prior year's closing balances | Reports | PASS |
| TC-FIN-042 | API | AC-FIN-042,REQ-FIN-042,API-FIN-031 | the income statement opens a new year at zero | Reports | PASS |
| TC-FIN-043 | API | AC-FIN-043,REQ-FIN-043,API-FIN-032 | a dimension report never merges two dimension values into one account row | Reports | PASS |
| TC-FIN-044 | API | AC-FIN-044,REQ-FIN-044 | FIN registers itself into SEC as data | Registration (SEC + MDL) | PASS |
| TC-FIN-045 | API | AC-FIN-045,REQ-FIN-045 | FIN registers its fifteen lookup types into MDL | Registration (SEC + MDL) | PASS |
| TC-FIN-046 | API | AC-FIN-046,REQ-FIN-046,API-FIN-030,API-FIN-029,API-FIN-028,API-FIN-022 | a statement line drills all the way down to its source event | Reports | PASS |
| TC-FIN-047 | RULE | AC-FIN-009,REQ-FIN-009,API-FIN-011 | two remainder lines are refused as surely as none | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-048 | RULE | AC-FIN-012,REQ-FIN-012,API-FIN-020 | a remainder that computes to zero or less is refused | Event entry — distribution / remainder | PASS |
| TC-FIN-049 | RULE | AC-FIN-014,REQ-FIN-014,API-FIN-019 | a manual entry with no lines never reaches the rules | JournalEntry (manual) | PASS |
| TC-FIN-050 | API | AC-FIN-027,REQ-FIN-027,API-FIN-018 | a filter matching nothing is a successful empty page | JournalEntry (manual) | PASS |
| TC-FIN-051 | API | AC-FIN-031,REQ-FIN-031,API-FIN-023 | a period count other than twelve falls back to an even day split | FiscalYear | PASS |
| TC-FIN-052 | API | AC-FIN-038,REQ-FIN-038,API-FIN-027 | the same permission gates the year-end close | Permission gate (RULE-FIN-015) | BLOCKED |
| TC-FIN-093 | API | AC-FIN-047,REQ-FIN-047,API-FIN-020 | an account code carried in the event payload is never used | JournalEntry (from event) | PASS |
| TC-FIN-094 | RULE | AC-FIN-048,REQ-FIN-048,API-FIN-011 | a DIRECT rule line is refused as an unsupported derivation | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-095 | API | AC-FIN-049,REQ-FIN-049,API-FIN-011 | a MAPPING rule line records the business field that selects its account | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-096 | API | AC-FIN-050,REQ-FIN-050,API-FIN-020 | a cheque payment resolves to the bank account through its mapping | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-097 | API | AC-FIN-051,REQ-FIN-050,API-FIN-020 | a cash payment resolves to the cash account through its mapping | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-098 | API | AC-FIN-052,REQ-FIN-050,API-FIN-020 | an expense type resolves to its own expense account | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-099 | RULE | AC-FIN-053,REQ-FIN-051,API-FIN-020 | an event missing the business field its MAPPING line reads is refused | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-100 | RULE | AC-FIN-054,REQ-FIN-052,API-FIN-020 | a mapped account that has since gained a child is refused at post time | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-101 | RULE | AC-FIN-055,REQ-FIN-052,API-FIN-020 | a mapped account that has since been deactivated is refused at post time | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-102 | RULE | AC-FIN-056,REQ-FIN-053,API-FIN-020 | a business value with no mapping is refused, never defaulted | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-103 | RULE | AC-FIN-057,REQ-FIN-053,API-FIN-020 | an inactive mapping counts as no mapping | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-104 | API | AC-FIN-058,REQ-FIN-054,API-FIN-039 | an account mapping is created active | AccountMapping (v2) | PASS |
| TC-FIN-105 | API | AC-FIN-059,REQ-FIN-055,API-FIN-038 | a mapping search returns exactly the rows its filters match | AccountMapping (v2) | PASS |
| TC-FIN-106 | API | AC-FIN-060,REQ-FIN-056,API-FIN-040 | changing a mapping's account leaves its key untouched | AccountMapping (v2) | PASS |
| TC-FIN-107 | RULE | AC-FIN-061,REQ-FIN-057,API-FIN-041,API-FIN-020 | a deactivated mapping stops resolving from the next event on | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-108 | RULE | AC-FIN-062,REQ-FIN-058,API-FIN-040,API-FIN-022,API-FIN-028 | changing a mapping never reaches an entry already posted through it | Event entry — MAPPING derivation (v2) | PASS |
| TC-FIN-109 | RULE | AC-FIN-063,REQ-FIN-059,API-FIN-039 | a second active mapping for the same key is refused | AccountMapping (v2) | PASS |
| TC-FIN-110 | RULE | AC-FIN-064,REQ-FIN-060,API-FIN-039 | a mapping to a parent account is refused when it is saved | AccountMapping (v2) | PASS |
| TC-FIN-111 | RULE | AC-FIN-065,REQ-FIN-061,API-FIN-039 | a misspelt payment method is refused as a mapping value | AccountMapping (v2) | PASS |
| TC-FIN-112 | API | AC-FIN-066,REQ-FIN-062,API-FIN-011 | a rule line is saved with its two dimension tags | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-113 | API | AC-FIN-067,REQ-FIN-063,API-FIN-020 | a CONSTANT tag stamps its value on the event-built line | Event entry — dimension tags (v2) | PASS |
| TC-FIN-114 | API | AC-FIN-068,REQ-FIN-063,API-FIN-020 | a BUSINESS_FIELD tag resolves its value by code from the event | Event entry — dimension tags (v2) | PASS |
| TC-FIN-115 | RULE | AC-FIN-069,REQ-FIN-064,API-FIN-011 | a rule line tagging one dimension twice is refused | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-116 | RULE | AC-FIN-070,REQ-FIN-065,API-FIN-011 | a CONSTANT tag citing another dimension's value is refused | EventTypeRule + RuleLine (v2) | PASS |
| TC-FIN-117 | API | AC-FIN-071,REQ-FIN-066,API-FIN-020 | both resolved dimension values are validated and written on the line | Event entry — dimension tags (v2) | PASS |
| TC-FIN-118 | RULE | AC-FIN-072,REQ-FIN-067,API-FIN-020 | an event resolving to an inactive dimension value is refused | DimensionValue + Event entry — dimension tags (v2) | PASS |
| TC-FIN-119 | RULE | AC-FIN-073,REQ-FIN-068,API-FIN-020 | an event missing the business field a dimension tag names is refused | Event entry — dimension tags (v2) | PASS |
| TC-FIN-120 | RULE | AC-FIN-074,REQ-FIN-069,API-FIN-020 | an event dimension value that matches no defined code is refused | Event entry — dimension tags (v2) | PASS |
| TC-FIN-121 | API | AC-FIN-075,REQ-FIN-070,API-FIN-022 | an event-built line and a manual line read back in one dimension shape | Event entry — dimension tags (v2) | PASS |
| TC-FIN-122 | API | AC-FIN-076,REQ-FIN-071,API-FIN-032 | the dimension report groups event-built and manual lines together | Reports | PASS |
| TC-FIN-123 | RULE | AC-FIN-077,REQ-FIN-072,API-FIN-020 | a repeated event reference is answered as a duplicate | JournalEntry (from event) | PASS |
| TC-FIN-124 | RULE | AC-FIN-078,REQ-FIN-072,API-FIN-020 | two simultaneous events with one reference post once and never answer 500 | JournalEntry (from event) | PASS |
| TC-FIN-125 | RULE | AC-FIN-079,REQ-FIN-073,API-FIN-020 | an event into a soft-closed period gets the distinct period-not-open code | JournalEntry (from event) | PASS |
| TC-FIN-126 | RULE | AC-FIN-080,REQ-FIN-074,API-FIN-020 | an event date no period covers is a configuration error, not a retry signal | JournalEntry (from event) | PASS |
| TC-FIN-127 | API | AC-FIN-081,REQ-FIN-075,API-FIN-020 | the published catalogue documents the period-not-open code once | Published catalogue | PASS |
| TC-FIN-128 | RULE | AC-FIN-082,REQ-FIN-076,API-FIN-020 | a MAPPING remainder line still absorbs the rounding difference exactly | Event entry — distribution / remainder | PASS |
| TC-FIN-129 | API | AC-FIN-083,REQ-FIN-077,API-FIN-021,API-FIN-028,API-FIN-032 | an entry posted through a mapping and tags reverses like any other | Event entry — dimension tags (v2) | PASS |


## Failure / skip taxonomy

| TC | Code | Detail |
|---|---|---|
| TC-FIN-038 | `TEST_STRUCTURE_FAILURE` | "entry-creation permission alone does not buy a period close" needs a principal that does **not** hold `PERM_FIN_PERIODS_CLOSE_APPROVE`. The run's own account is the bootstrap SYS_ADMIN, which V30 grants it to. Minting a SEC user + role is outside `api-verify` SKILL.md §3-I's single bounded exception (grant only FIN's own documented permissions to self, journalled, self-revoked), so the case is declared NOT COVERED rather than approximated. Not an application defect — no evidence of one either way. |
| TC-FIN-052 | `TEST_STRUCTURE_FAILURE` | Same cause, same gate (`PERM_FIN_PERIODS_CLOSE_APPROVE` on API-FIN-027). |

No `FAIL`. No `DB_PRECONDITION`, `ENVIRONMENT_FAILURE`, `MISSING_IMPLEMENTATION`,
`CONTRACT_BREAK`, `SERVER_ERROR` or `BUSINESS_LOGIC_ISSUE` was raised by any asserted call.

## Suites

| Suite | Passed | Failed |
|---|---|---|
| PREFLIGHT (stage A0) | 16 | 0 |
| Registration (SEC + MDL) | 3 | 0 |
| Dimension | 3 | 0 |
| Account | 8 | 0 |
| FiscalYear | 3 | 0 |
| FiscalPeriod | 6 | 0 |
| DimensionValue | 5 | 0 |
| AccountMapping (v2) | 7 | 0 |
| EventTypeRule + RuleLine (v2) | 11 | 0 |
| JournalEntry (manual) | 12 | 0 |
| JournalEntry (reversal) | 3 | 0 |
| JournalEntry (from event) | 8 | 0 |
| Event entry — MAPPING derivation (v2) | 10 | 0 |
| Event entry — dimension tags (v2) | 8 | 0 |
| Event entry — distribution / remainder | 3 | 0 |
| RecurringTemplate | 4 | 0 |
| AllocationRule | 3 | 0 |
| Reports | 7 | 0 |
| FiscalYear year-end close | 2 | 0 |
| Published catalogue | 1 | 0 |
| Permission gate (RULE-FIN-015) | 0 | 2 (both BLOCKED, see taxonomy) |
| **Total** | **123** | **2** |

## Observations (stage E — never pass/fail)

- **A second hard-close on an already hard-closed period** → HTTP 409 /
  `FIN-409-NOT-REOPENABLE`. The ENTITY CRUD CHECKLIST only says HARD_CLOSE is terminal;
  no artifact states which of `FIN-409-INVALID-TRANSITION` / `FIN-409-NOT-REOPENABLE` is
  owed, so this is recorded, not asserted.
- **The CLOSE_APPROVE gate resolves for a holder** — API-FIN-026 and API-FIN-027 were both
  reached by the run's principal (no `FIN-403-FORBIDDEN`). The deny side is TC-FIN-038/052,
  out of scope above.

## Notes carried out of the run

- **API-FIN-016 doc example contradicts RULE-FIN-003.** The api-docs Request Example for
  `POST /api/v1/fin/allocation-rules` shows a lone `PERCENTAGE` target with no remainder
  target; RULE-FIN-003 requires exactly one remainder target whenever any target is
  PERCENTAGE, so the example as printed is refused with `FIN-409-REMAINDER-COUNT`. Already
  recorded against the v1 run; still present. Generator-owned (the example is derived from
  the DTO's `@Schema`, which carries no rule).
- **Owed JUnit fix, not this run's.** `FinRegistrationInSecIntegrationTest` (TC-FIN-044)
  still asserts the v1 registry (12 screens / 27 actions). The live store holds 13 / 30,
  which this run verified through SEC's own `POST /api/v1/sec/registry/search` — so the
  *behaviour* is correct and it is the JUnit expectation that is stale. Tracked as the
  existing `api_doc_gaps[4]` entry; a separate fixing agent owns it. No `src/test/` file
  was touched here.

## Surviving records

Listed in full — never summarised as "cleaned up" — in
`governance/shared/backend/modules/FIN/v2/test-api/fin_problems_report.md`
("Surviving records" + "Permanent residue"). Headline: 17 POSTED JournalEntry rows
(immutable by design, RULE-FIN-016), 4 FiscalYears + 40 periods (no retirement endpoint),
2 Dimensions left ACTIVE (ENT-FIN-002 publishes no deactivate), and everything else
soft-deactivated through its own documented endpoint. 10 MDL `ACCOUNTING_EVENT_TYPE`
lookup values were created as permanent registry residue and each was retired
(HTTP 200) through MDL's own delete endpoint; cleanup SQL is printed per row.

## Privileges

No grant was created or revoked. Stage I was skipped entirely, no grant journal was
written, and no standing privilege was left behind.
