# ADR-SEQUENCE-003 — No delete — deactivate only; counters reset to 1 on tenant provisioning; NumberSeriesApi ungated

Module  : SEQUENCE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (API shape) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
The build skills prescribe a full CRUD shape for every entity — a `DELETE` endpoint, a usage endpoint and a
Usage DTO (`build-create-controller` A.6.5 / A.6.8, `build-create-dto`), a `@PreAuthorize` on every service
method (`build-create-service` A.5.2) — and the step-09 file left three things open: whether a series can be
deleted or its counter edited, what a tenant created after the series were seeded should hold, and whether
the in-process allocation API checks an authority (docs/DEVIATIONS.md [09]; docs/steps/09-report.md
"Decisions & deviations" 3, 8, 9).

## Decision
1. **No delete, no counter edit — deactivate instead.** `NumberSeriesController` maps create, update, get,
   search, activate and deactivate only (`erp-core/src/main/java/com/erp/sequence/controller/NumberSeriesController.java:24-28`,
   `:38-74`); the update body excludes `code`, `resetPolicy`, `periodKey` and `nextValue`
   (`sequence/dto/NumberSeriesUpdateRequest.java:11-13`), and the mapper never touches them
   (`sequence/mapper/NumberSeriesMapper.java:45-52`). Reason: an issued number must never be reissued; a
   deleted series or a rewound counter would allow exactly that. Deactivation makes the code "not
   configured" for allocation (`sequence/domain/NumberSeriesDomain.java:56-61`) while keeping every period
   row and its final counter. The only counter input is the optional starting `nextValue` of a create
   (`sequence/dto/NumberSeriesCreateRequest.java:40-42`), before any number exists. There is no usage
   endpoint because nothing references a series.
2. **Provisioning copies definitions, not counters.** `SequenceTenantProvisioningContributor` (order 40,
   after SEC, MDL and NOTIF) inserts for each code of the source tenant (PLATFORM) a copy of the anchor row
   — `CODE`, `PREFIX`, `PATTERN`, `RESET_POLICY`, `PERIOD_KEY`, `IS_ACTIVE` — with `NEXT_VALUE = 1`,
   `CREATED_BY` = the operator, explicit `TENANT_ID` in the insert and both predicates
   (`sequence/tenant/SequenceTenantProvisioningContributor.java:27-44`). Each tenant counts on its own
   from 1; series an application seeds for PLATFORM in `V1000+` (docs/CONSUMING.md §3) therefore exist in
   every later tenant.
3. **`NumberSeriesApi` carries no `@PreAuthorize`.** `next` and `preview` are in-process library calls made
   by another module from its own, already authorised work (a document creation) or from paths with no
   principal; they are never bound to a controller (`sequence/service/NumberAllocationService.java:41-43`;
   `sequence/crossmodule/NumberSeriesApiImpl.java:7-12`). The HTTP surface — the admin API — is gated by
   `PERM_SEQUENCE_SERIES_VIEW` / `PERM_SEQUENCE_SERIES_MANAGE` on every service method
   (`sequence/service/NumberSeriesService.java:64`, `:82`, `:89`, `:102`, `:118`, `:131`). Same precedent as
   CU's former internal `getValue` and the step-05 provisioning contributors (docs/DEVIATIONS.md [09]).

## Consequences
- Named deviations from the build skills, accepted and recorded: no `DELETE`, no usage endpoint, no Usage
  DTO, an ungated in-process service, one `VIEW` / `MANAGE` pair instead of VIEW / CREATE / UPDATE / DELETE
  (docs/steps/09-report.md "Skills checked"); `gov-validate-backend-feature` loses points only on these.
- A test or a clean-up that wants a series gone must delete rows with SQL outside the API — the API itself
  never will.
- Because the counter is immutable through the API, a series migrated from another system is created with
  its starting `nextValue` once; later corrections are impossible without SQL, by design.
- A tenant provisioned from PLATFORM shares PLATFORM's series *configuration* at that moment only; later
  changes to PLATFORM's series do not propagate (each tenant manages its own copy).
- The security of `NumberSeriesApi` rests on the caller: an application exposing it over HTTP must gate
  that endpoint itself. The module's public surface is bounded to `com.erp.sequence.crossmodule` by ArchUnit
  (`erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:61-62`).

## Traces
ENT-SEQUENCE-001 · REQ-SEQUENCE-008, REQ-SEQUENCE-009, REQ-SEQUENCE-012, REQ-SEQUENCE-017,
REQ-SEQUENCE-018, REQ-SEQUENCE-020 · RULE-SEQUENCE-008, RULE-SEQUENCE-009, RULE-SEQUENCE-011 ·
POL-SEQUENCE-005, POL-SEQUENCE-006, POL-SEQUENCE-008, POL-SEQUENCE-009 · DBF-SEQUENCE-008,
DBF-SEQUENCE-009 · XM-SEQUENCE-002, XM-SEQUENCE-003 · docs/DEVIATIONS.md [09] (row model;
provisioning; no `@PreAuthorize` on `NumberSeriesApi`); docs/steps/09-report.md "Decisions & deviations" 3, 8, 9
