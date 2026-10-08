# PRD — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Module          : SEQUENCE     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-sequence, business-policies-sequence
Stories         : 6   Policies covered : 10/10   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. HTTP verification: the `TC-CORE-SEQ-*`
cases of `docs/test-api/core-test-plan.md`; the frontend's E2E archive is
`governance/frontend/modules/SEQUENCE/tests/specs/sequences/number-series.spec.ts`.

## USER STORIES

US-SEQUENCE-001
  Title          : تهيئة سلسلة ترقيم / Configure a number series
  Story          : As a tenant administrator, I need to create a number series with a code, a prefix, a pattern and a reset policy, so that documents of that kind receive formatted, consecutive numbers.
  Priority       : HIGH — the module's entry point (09-STEP task 2)
  Success metric : `POST /api/v1/sequence/series` answers 201 with the defaults applied (TC-CORE-SEQ-001)
  Traces         : POL-SEQUENCE-001, POL-SEQUENCE-002, POL-SEQUENCE-003, POL-SEQUENCE-007, POL-SEQUENCE-009
  Source         : sequence/controller/NumberSeriesController.java:38-42; sequence/service/NumberSeriesService.java:63-79
  Status         : AS-BUILT

US-SEQUENCE-002
  Title          : صيانة سلسلة قائمة / Maintain an existing series
  Story          : As a tenant administrator, I need to change a series' prefix or pattern and to deactivate or re-activate it, so that numbering follows the organisation's format without ever reissuing a number.
  Priority       : MEDIUM
  Success metric : an update applies to every period row of the code; an inactive series is refused at allocation (TC-CORE-SEQ-010, -011, -012)
  Traces         : POL-SEQUENCE-001, POL-SEQUENCE-002, POL-SEQUENCE-003, POL-SEQUENCE-006, POL-SEQUENCE-010
  Source         : sequence/controller/NumberSeriesController.java:44-49, :64-74; sequence/service/NumberSeriesService.java:101-141
  Status         : AS-BUILT

US-SEQUENCE-003
  Title          : استعراض السلاسل وفتراتها / Browse series and their periods
  Story          : As a tenant administrator, I need to search the series of my tenant and open one period row, so that I can see each period's next value and whether the series is active.
  Priority       : MEDIUM
  Success metric : search and get answer only the caller's tenant (TC-CORE-SEQ-008, -009, -013)
  Traces         : POL-SEQUENCE-007, POL-SEQUENCE-009
  Source         : sequence/controller/NumberSeriesController.java:51-62; sequence/service/NumberSeriesService.java:81-99
  Status         : AS-BUILT

US-SEQUENCE-004
  Title          : سحب الرقم التالي / Draw the next document number
  Story          : As a module or application creating a document, I need the next number of a series, formatted and never duplicated under concurrency, so that every document of my tenant gets its own number.
  Priority       : HIGH
  Success metric : 50 concurrent calls produce 50 unique consecutive numbers (`NumberSeriesConcurrencyIntegrationTest.fiftyConcurrentCalls_produceFiftyUniqueConsecutiveNumbers`)
  Traces         : POL-SEQUENCE-004, POL-SEQUENCE-005, POL-SEQUENCE-007, POL-SEQUENCE-009, POL-SEQUENCE-010
  Source         : sequence/crossmodule/NumberSeriesApi.java:19; sequence/service/NumberAllocationService.java:55-78
  Status         : AS-BUILT

US-SEQUENCE-005
  Title          : معاينة الرقم التالي / Preview the next number
  Story          : As a module or application, I need to show the number a document would receive without consuming it, so that a user sees it before saving.
  Priority       : LOW
  Success metric : the preview equals the next allocated number and consumes nothing (`NumberSeriesIntegrationTest.preview_showsTheNextNumber_withoutConsumingIt_andCodesAreCaseInsensitive`)
  Traces         : POL-SEQUENCE-005, POL-SEQUENCE-007
  Source         : sequence/crossmodule/NumberSeriesApi.java:22; sequence/service/NumberAllocationService.java:80-97
  Status         : AS-BUILT

US-SEQUENCE-006
  Title          : سلاسل جاهزة لكل مستأجر / Series ready in every tenant
  Story          : As an application, I need the series I seed for the platform to exist in every tenant created later, each counting from 1, so that numbering works without per-tenant set-up.
  Priority       : HIGH — without it a new tenant has no series and every allocation fails
  Success metric : provisioning copies PLATFORM's series definitions with `nextValue = 1` (TC-CORE-SEQ-002)
  Traces         : POL-SEQUENCE-008
  Source         : sequence/tenant/SequenceTenantProvisioningContributor.java:27-44; docs/CONSUMING.md §3 "Number series"
  Status         : AS-BUILT

## THE ALLOCATION STORY (US-SEQUENCE-004, as built)
1. A module calls `NumberSeriesApi.next("SALES_INVOICE")` inside its own business transaction —
   sequence/crossmodule/NumberSeriesApi.java:19.
2. `NumberAllocationService.next` opens its own transaction (`REQUIRES_NEW`) under the caller's tenant;
   the code is trimmed and upper-cased — sequence/service/NumberAllocationService.java:56-58, :111-113.
3. The series' anchor row (lowest id of the code in the tenant) is locked `FOR UPDATE`; an unknown code
   answers `SEQUENCE_NOT_CONFIGURED`; an inactive series answers the same —
   sequence/service/NumberAllocationService.java:61-65; sequence/domain/NumberSeriesDomain.java:57-61.
4. The period key of today is computed from the reset policy (`''`, `YYYY` or `YYYY-MM`); the row of
   that period is locked, or created at 1 when the period is new —
   sequence/service/NumberAllocationService.java:67-73; sequence/mapper/NumberSeriesMapper.java:30-43.
5. The counter is taken and moved on; the number is rendered from the pattern (prefix, date tokens,
   zero-padded sequence, tenant code when `{TENANT}` is used) —
   sequence/entity/NumberSeries.java:112-116; sequence/domain/NumberPattern.java:133-153.
6. The allocation commits before it returns. If the caller's transaction later rolls back, the number
   stays consumed — sequence/service/NumberAllocationService.java:23-32.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-SEQUENCE-001 | POL-SEQUENCE-001, -002, -003, -007, -009 | sequence/service/NumberSeriesService.java:63-79 |
| US-SEQUENCE-002 | POL-SEQUENCE-001, -002, -003, -006, -010 | sequence/service/NumberSeriesService.java:101-141 |
| US-SEQUENCE-003 | POL-SEQUENCE-007, -009 | sequence/service/NumberSeriesService.java:81-99 |
| US-SEQUENCE-004 | POL-SEQUENCE-004, -005, -007, -009, -010 | sequence/service/NumberAllocationService.java:55-78 |
| US-SEQUENCE-005 | POL-SEQUENCE-005, -007 | sequence/service/NumberAllocationService.java:80-97 |
| US-SEQUENCE-006 | POL-SEQUENCE-008 | sequence/tenant/SequenceTenantProvisioningContributor.java:27-44 |
Every policy POL-SEQUENCE-001 … POL-SEQUENCE-010 appears in at least one row above (001, 002, 003 →
US-001/002; 004, 005 → US-004/005; 006 → US-002; 007 → US-001/003/004/005; 008 → US-006; 009 →
US-001/003/004; 010 → US-002/004).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Lock strategy and gaps | anchor-row pessimistic lock in `REQUIRES_NEW`; gaps accepted | fixed decision of erp-core plan step 09, as built | ADR-SEQUENCE-001 |
| 2 | Period rows and reset | one row per (code, period); policy immutable; pattern carries the period | step 09, as built | ADR-SEQUENCE-002 |
| 3 | Delete / counter edits / gating | no delete, deactivate only; counter never edited; `NumberSeriesApi` ungated | step 09, as built | ADR-SEQUENCE-003 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (gap-free numbering, branch dimensions, delete, usage, counter edits) | not built (business-policies-sequence.md SCOPE EXCEPTIONS) | explicit future request; a business module that needs gap-free numbering numbers inside its own transaction |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
