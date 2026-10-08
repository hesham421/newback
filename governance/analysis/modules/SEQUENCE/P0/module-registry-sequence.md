## MODULE REGISTRY — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Module Code    : SEQUENCE   (package `com.erp.sequence`; permission-registry module `SEQUENCE`; api-docs folder `sequence`)
Bounded context: platform
Layer / Type   : L1 / core service (document numbering)     Execution tier : core (V14, erp-core plan step 09)
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 19b19a4)
Knowledge      : erp-core-plan/09-STEP-sequences-and-settings-api.md; docs/steps/09-report.md; docs/DEVIATIONS.md [09], [10]; docs/CONSUMING.md §3
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

ENTITIES OWNED   (entity ids are assigned in P1: ENT-SEQUENCE-001)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| سلسلة الترقيم (فترة) / NumberSeries (`CORE_NUMBER_SERIES`) | config (tenant-scoped, one row per code and period) | PRIVATE — no other module's table references it; other modules reach it only through `NumberSeriesApi` | sequence/entity/NumberSeries.java:28-48; V14__sequence_and_settings.sql:31-61 |

Not entities, but owned runtime surface (no table): the cross-module API `NumberSeriesApi`
(`next`, `preview`), the pattern parser `NumberPattern`, the enum `ResetPolicy`, the domain companion
`NumberSeriesDomain` and the provisioning contributor `SequenceTenantProvisioningContributor`
(sequence/crossmodule/NumberSeriesApi.java:12; sequence/domain/NumberPattern.java:24;
sequence/domain/ResetPolicy.java:9; sequence/domain/NumberSeriesDomain.java:16;
sequence/tenant/SequenceTenantProvisioningContributor.java:23).

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (value set of `CORE_NUMBER_SERIES.RESET_POLICY`) | سياسة إعادة الترقيم / reset policy | `NEVER`, `YEARLY`, `MONTHLY` — a CHECK constraint (`CHK_CORE_NUMBER_SERIES_RESET`) mirrored by the Java enum `ResetPolicy`, not an MDL lookup type (same pattern as ADR-SEC-001) | V14__sequence_and_settings.sql:37, :59; sequence/domain/ResetPolicy.java:9-16 |

LOOKUPS CONSUMED
None.

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |
|---|---|---|---|
| `CORE_TENANT` | TENANT | HARD-FK (`FK_CORE_NUMBER_SERIES_TENANT` on `TENANT_ID`) + SOFT-READ (`TenantLookupApi.codeOf` for the `{TENANT}` token) | every series belongs to one tenant; the rendered number may carry the tenant code |
Source: V14__sequence_and_settings.sql:58; sequence/service/NumberAllocationService.java:52, :99-101.

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| TENANT | HARD (FK + context) | `CORE_TENANT(ID)`; `TenantContext.require()` (the tenant numbered); Hibernate `@TenantId` through `AuditableEntity` | V14__sequence_and_settings.sql:58; sequence/service/NumberAllocationService.java:100; sequence/entity/NumberSeries.java:48 |
| TENANT | SOFT (crossmodule read) | `com.erp.tenant.crossmodule.TenantLookupApi.codeOf(Long)` — only when the pattern uses `{TENANT}` | sequence/service/NumberAllocationService.java:52, :99-101; sequence/domain/NumberSeriesDomain.java:68-71 |
| TENANT | SPI (implements) | `com.erp.tenant.TenantProvisioningContributor` — `SequenceTenantProvisioningContributor`, order 40 | sequence/tenant/SequenceTenantProvisioningContributor.java:23-44 |
| SEC | SPI (implements) | `com.erp.sec.permission.PermissionContributor` — `SequencePermissions` declares module `SEQUENCE`, screen `SEQUENCE_SERIES` and its two actions | sequence/permission/SequencePermissions.java:18-46 |
| audit | SOFT (entity listener) | `@Audited(entityType = "CORE_NUMBER_SERIES", ignore = {"nextValue"})` | sequence/entity/NumberSeries.java:41 |
| common | foundation | `AuditableEntity`, `ServiceResult`/`Status`, `LocalizedException`, `DomainRules.assertUnique`, `SpecBuilder`, `PageableBuilder`, `BaseSearchContractRequest`, `OperationCode`/`ApiResponse` | sequence/service/NumberSeriesService.java:3-10; sequence/domain/NumberSeriesDomain.java:3-5; sequence/controller/NumberSeriesController.java:3-4 |
ROOT: NO — depends on TENANT (hard), SEC (SPI), audit (listener), common (foundation). Nothing in core
depends on SEQUENCE.

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.sequence.crossmodule.NumberSeriesApi` — `String next(String code)`, `String preview(String code)` | crossmodule call (in-process, no `@PreAuthorize`) | none in core; applications and future business modules (docs/CONSUMING.md §3) | XM-SEQUENCE-003 | sequence/crossmodule/NumberSeriesApi.java:12-23; sequence/crossmodule/NumberSeriesApiImpl.java:13-28 |
| `CORE_NUMBER_SERIES` rows of PLATFORM | seed target | applications seed series in `V1000+` for `TENANT_ID = 1`; provisioning copies them | — | docs/CONSUMING.md §3 (seed example); sequence/tenant/SequenceTenantProvisioningContributor.java:34-42 |

PERMISSION MODULE → SCREEN → ACTIONS (registry rows; code-registered by `SequencePermissions`, upserted by SEC's catalog synchronizer at startup — never seeded by a migration)
| Registry module | Screen (page code) | Action code | Authority | Meaning | Source |
|---|---|---|---|---|---|
| `SEQUENCE` — الترقيم التسلسلي / Sequences | `SEQUENCE_SERIES` — سلاسل الترقيم / Number Series | `VIEW` — عرض | `PERM_SEQUENCE_SERIES_VIEW` | gateway action of the screen (RULE-SEC-007); gates `GET /{id}` and `POST /search` | sequence/permission/SequencePermissions.java:22-23, :28-29, :33, :44 |
| `SEQUENCE` | `SEQUENCE_SERIES` | `MANAGE` — إدارة | `PERM_SEQUENCE_SERIES_MANAGE` | create, update, activate, deactivate (the step file's `SEQUENCE:SERIES:MANAGE` in the registry's `PERM_<SCREEN>_<ACTION>` format) | sequence/permission/SequencePermissions.java:25-26, :45; docs/DEVIATIONS.md [09] |
Every tenant's super role (`SYS_ADMIN`, `IS_SUPER = TRUE`) holds both through the catalog; no grant
migration exists (sequence/permission/SequencePermissions.java:14-15). The rows are among the
"synchronizer-only" permissions that `PermissionCatalogIntegrationTest` leaves out of its seeded-catalog
comparison (docs/DEVIATIONS.md [09], test adaptations).

AUTO-DECISIONS
AUTO: `RESET_POLICY` is a CHECK-constrained column plus a Java enum, not an MDL lookup type
  FROM: V14__sequence_and_settings.sql:59; sequence/domain/ResetPolicy.java (the value drives period-key computation in code)
  IF WRONG: none — adding a policy is a core schema change (new CHECK) plus an enum constant, by design.
AUTO: NumberSeries classified PRIVATE
  FROM: no FK from any table to `CORE_NUMBER_SERIES`; the only cross-module access is `NumberSeriesApi` (plain `String` results)
  IF WRONG: none — nothing references a series (hence no usage endpoint, docs/DEVIATIONS.md [09]).
AUTO: PK column `ID` with `SEQ_CORE_NUMBER_SERIES`, not `NUMBER_SERIES_PK`
  FROM: new-table convention of the core chain (`db/migration/core/README.md` §4; docs/steps/09-report.md "Skills checked")
  IF WRONG: none — shipped core scripts are never edited.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Concurrency of allocation | anchor-row pessimistic lock in `REQUIRES_NEW`; gaps accepted | ADR-SEQUENCE-001 |
| 2 | Row model and reset | one row per (code, period), immutable reset policy, pattern must carry the period | ADR-SEQUENCE-002 |
| 3 | Lifecycle and gating | no delete (deactivate only); counters restart at 1 on provisioning; `NumberSeriesApi` ungated | ADR-SEQUENCE-003 |

POLICIES OWNED (full text in business-policies-sequence.md)
POL-SEQUENCE-001, POL-SEQUENCE-002, POL-SEQUENCE-003, POL-SEQUENCE-004, POL-SEQUENCE-005,
POL-SEQUENCE-006, POL-SEQUENCE-007, POL-SEQUENCE-008, POL-SEQUENCE-009, POL-SEQUENCE-010
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
