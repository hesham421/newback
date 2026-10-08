## REGISTRY — P1 — SEQUENCE v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 19b19a4). Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-SEQUENCE-001 | سلسلة الترقيم (فترة) / NumberSeries | config (tenant-scoped, one row per code and period) | PRIVATE | REGISTERED (built) | sequence/entity/NumberSeries.java:40-125; V14__sequence_and_settings.sql:31-61 |

Consumed
| Owner | What | Kind | XM | Code location |
|---|---|---|---|---|
| TENANT | `CORE_TENANT(ID)` (FK of `TENANT_ID`); `TenantLookupApi.codeOf` for `{TENANT}` | HARD-FK + SOFT-READ | XM-SEQUENCE-001 | V14__sequence_and_settings.sql:58; sequence/service/NumberAllocationService.java:52, :99-101 |
| TENANT | `TenantProvisioningContributor` | SPI implemented (order 40) | XM-SEQUENCE-002 | sequence/tenant/SequenceTenantProvisioningContributor.java:23-44 |
| SEC | `PermissionContributor` | SPI implemented | — | sequence/permission/SequencePermissions.java:18-46 |
| audit | `@Audited` | entity listener | — | sequence/entity/NumberSeries.java:41 |

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers / implementers | Status | Code location |
|---|---|---|---|---|---|
| XM-SEQUENCE-003 | `com.erp.sequence.crossmodule.NumberSeriesApi` — `next(String)`, `preview(String)` | crossmodule call (ungated) | none in core; applications | ACTIVE (built) | sequence/crossmodule/NumberSeriesApi.java:12-23; sequence/crossmodule/NumberSeriesApiImpl.java:13-28 |
Note: XM-SEQUENCE-001/002 are the sequence side of XM-TENANT-001/002 (`../../TENANT/P1/registry-srs-tenant.md`).

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| RESET_POLICY value set (`CHK_CORE_NUMBER_SERIES_RESET` + enum `ResetPolicy`) | ENT-SEQUENCE-001 | 3 (NEVER, YEARLY, MONTHLY) | V14__sequence_and_settings.sql:59; sequence/domain/ResetPolicy.java:9-16 |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code | Code location |
|---|---|---|---|
| SCR-REQ-SEQUENCE-001 | سلاسل الترقيم / Number Series | SEQUENCE_SERIES | sequence/permission/SequencePermissions.java:28-29 |

Requirements
REQ count: 20 · AC count: 20 · RULE count: 14 · ENT count: 1 · SCR-REQ count: 1 · API count: 6 (+ 2 in-process) · XM count: 3
Last sequence per atom: REQ: 020 · AC: 020 · ENT: 001 · RULE: 014 · SCR-REQ: 001 · API: 006 · XM: 003 · US: 006 · POL: 010

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-SEQUENCE-001 / AC-SEQUENCE-001 | Create a series | sequence/service/NumberSeriesService.java:63-79 | TC-CORE-SEQ-001; `NumberSeriesApiIntegrationTest.create_201_withDefaults_then_duplicate_409_and_invalidPattern_400` |
| REQ-SEQUENCE-002 / AC-SEQUENCE-002 | Reject an invalid pattern | sequence/domain/NumberPattern.java:43-108 | TC-CORE-SEQ-004, -006; `NumberSeriesDomainTest` |
| REQ-SEQUENCE-003 / AC-SEQUENCE-003 | Reject a pattern that repeats under the policy | sequence/domain/NumberPattern.java:115-125 | TC-CORE-SEQ-005, -010 |
| REQ-SEQUENCE-004 / AC-SEQUENCE-004 | Reject a duplicate code | sequence/domain/NumberSeriesDomain.java:39 | TC-CORE-SEQ-003 |
| REQ-SEQUENCE-005 / AC-SEQUENCE-005 | Reject a malformed create request | sequence/dto/NumberSeriesCreateRequest.java:22-42 | TC-CORE-SEQ-007 |
| REQ-SEQUENCE-006 / AC-SEQUENCE-006 | Read a period row | sequence/service/NumberSeriesService.java:81-86, :143-146 | TC-CORE-SEQ-008; `NumberSeriesApiIntegrationTest.getById_200_and_404` |
| REQ-SEQUENCE-007 / AC-SEQUENCE-007 | Search period rows | sequence/service/NumberSeriesService.java:51-57, :88-99 | TC-CORE-SEQ-009; `NumberSeriesApiIntegrationTest.search_200_byCode_and_400_onAnUnsupportedFilter` |
| REQ-SEQUENCE-008 / AC-SEQUENCE-008 | Update prefix and pattern across the code | sequence/service/NumberSeriesService.java:101-115 | TC-CORE-SEQ-010, -011; `NumberSeriesApiIntegrationTest.update_200_appliesToTheCode_and_400_onAnInvalidPattern` |
| REQ-SEQUENCE-009 / AC-SEQUENCE-009 | Deactivate and re-activate | sequence/service/NumberSeriesService.java:117-141 | TC-CORE-SEQ-012; `NumberSeriesApiIntegrationTest.deactivate_then_activate_200_and_404` |
| REQ-SEQUENCE-010 / AC-SEQUENCE-010 | Allocate atomically | sequence/service/NumberAllocationService.java:55-78 | `NumberSeriesConcurrencyIntegrationTest.fiftyConcurrentCalls_produceFiftyUniqueConsecutiveNumbers` |
| REQ-SEQUENCE-011 / AC-SEQUENCE-011 | Start a new period at 1 | sequence/service/NumberAllocationService.java:67-73; sequence/mapper/NumberSeriesMapper.java:30-43 | `NumberSeriesIntegrationTest.yearlyReset_createsANewPeriodRow_startingAtOne_andLeavesThePreviousYearUntouched` |
| REQ-SEQUENCE-012 / AC-SEQUENCE-012 | Refuse an unconfigured or inactive code | sequence/service/NumberAllocationService.java:62-65, :107-109; sequence/domain/NumberSeriesDomain.java:57-61 | `NumberSeriesIntegrationTest.unknownOrInactiveCode_isSequenceNotConfigured_andNothingIsCreated` |
| REQ-SEQUENCE-013 / AC-SEQUENCE-013 | Preview without consuming | sequence/service/NumberAllocationService.java:80-97 | `NumberSeriesIntegrationTest.preview_showsTheNextNumber_withoutConsumingIt_andCodesAreCaseInsensitive` |
| REQ-SEQUENCE-014 / AC-SEQUENCE-014 | Render the number | sequence/domain/NumberPattern.java:133-153 | `NumberSeriesIntegrationTest.tenantToken_rendersTheTenantCode`; `NumberSeriesDomainTest.render_table` |
| REQ-SEQUENCE-015 / AC-SEQUENCE-015 | Period date from the clock | sequence/service/NumberAllocationService.java:103-105 | — |
| REQ-SEQUENCE-016 / AC-SEQUENCE-016 | Tenant isolation | sequence/entity/NumberSeries.java:48; sequence/repository/NumberSeriesRepository.java:13-15 | TC-CORE-SEQ-013; `NumberSeriesIntegrationTest.tenantsAandB_eachStartAtOne_forTheSameCode` |
| REQ-SEQUENCE-017 / AC-SEQUENCE-017 | Copy series into a new tenant | sequence/tenant/SequenceTenantProvisioningContributor.java:27-44 | TC-CORE-SEQ-002; `NumberSeriesIntegrationTest.tenantProvisioning_copiesTheSeriesDefinitions_withTheCounterBackAtOne` |
| REQ-SEQUENCE-018 / AC-SEQUENCE-018 | Authorisation of the admin API | sequence/service/NumberSeriesService.java:64, :82, :89, :102, :118, :131 | TC-CORE-SEQ-014; `NumberSeriesApiIntegrationTest.withoutAToken_401` |
| REQ-SEQUENCE-019 / AC-SEQUENCE-019 | Audit the configuration, not the counter | sequence/entity/NumberSeries.java:41 | TC-CORE-AUDIT-013; `AuditedEntitiesCoverageIntegrationTest.sequenceNumberSeries_configIsAudited_allocationIsNot` |
| REQ-SEQUENCE-020 / AC-SEQUENCE-020 | Cross-module number API contract | sequence/crossmodule/NumberSeriesApi.java:12-23 | ArchUnit `CrossModuleBoundaryArchTest` (`step09_apis_are_crossmodule_interfaces_allowed_by_the_boundary_rule`) |
| RULE-SEQUENCE-001 | Pattern grammar | sequence/domain/NumberPattern.java:27-30, :43-108 | `NumberSeriesDomainTest` (12 invalid patterns) |
| RULE-SEQUENCE-002 | Distinct under the reset policy | sequence/domain/NumberPattern.java:110-125 | TC-CORE-SEQ-005 |
| RULE-SEQUENCE-003 | Rendering rules | sequence/domain/NumberPattern.java:132-153 | `NumberSeriesDomainTest.render_table` |
| RULE-SEQUENCE-004 | One row per period, one anchor per code | V14__sequence_and_settings.sql:57; sequence/service/NumberSeriesService.java:109-111 | TC-CORE-SEQ-010 |
| RULE-SEQUENCE-005 | Allocation protocol | sequence/service/NumberAllocationService.java:55-78; sequence/repository/NumberSeriesRepository.java:31-36 | `NumberSeriesConcurrencyIntegrationTest` |
| RULE-SEQUENCE-006 | Preview locks and writes nothing | sequence/service/NumberAllocationService.java:80-97 | `NumberSeriesIntegrationTest.preview_…` |
| RULE-SEQUENCE-007 | Date source | sequence/service/NumberAllocationService.java:103-105; sequence/service/NumberSeriesService.java:148-150 | — |
| RULE-SEQUENCE-008 | Inactive counts as not configured | sequence/domain/NumberSeriesDomain.java:56-61 | `NumberSeriesIntegrationTest.unknownOrInactiveCode_…` |
| RULE-SEQUENCE-009 | No counter edits, no delete | sequence/dto/NumberSeriesUpdateRequest.java:20-30; sequence/controller/NumberSeriesController.java:24-28 | TC-CORE-SEQ-011 |
| RULE-SEQUENCE-010 | Defaults and normalisation | sequence/mapper/NumberSeriesMapper.java:15-27; sequence/entity/NumberSeries.java:94-109 | TC-CORE-SEQ-001 |
| RULE-SEQUENCE-011 | A new tenant starts at 1 | sequence/tenant/SequenceTenantProvisioningContributor.java:32-44 | TC-CORE-SEQ-002 |
| RULE-SEQUENCE-012 | No caching | sequence/service/NumberSeriesService.java:43-44 | `gov-enforce-caching-rules` (docs/steps/09-report.md) |
| RULE-SEQUENCE-013 | Allowed search and sort fields | sequence/service/NumberSeriesService.java:51-57 | TC-CORE-SEQ-009 |
| RULE-SEQUENCE-014 | Code uniqueness and immutability | sequence/domain/NumberSeriesDomain.java:37-43; sequence/entity/NumberSeries.java:61 | TC-CORE-SEQ-003, -011 |
| ENT-SEQUENCE-001 | NumberSeries | sequence/entity/NumberSeries.java:40-125 | `TenantSchemaIntegrationTest` (tenant column, unique) |
| XM-SEQUENCE-001 | CORE_TENANT FK + TenantLookupApi | V14__sequence_and_settings.sql:58; sequence/service/NumberAllocationService.java:99-101 | `NumberSeriesIntegrationTest.tenantToken_rendersTheTenantCode` |
| XM-SEQUENCE-002 | TenantProvisioningContributor (order 40) | sequence/tenant/SequenceTenantProvisioningContributor.java:27-30 | TC-CORE-SEQ-002 |
| XM-SEQUENCE-003 | NumberSeriesApi | sequence/crossmodule/NumberSeriesApi.java:12-23 | ArchUnit `CrossModuleBoundaryArchTest` |
| SCR-REQ-SEQUENCE-001 | SEQUENCE_SERIES | sequence/permission/SequencePermissions.java:28-29, :44-45 | `governance/frontend/modules/SEQUENCE/tests/specs/sequences/number-series.spec.ts` |
| API-SEQUENCE-001…006 | the six endpoints | sequence/controller/NumberSeriesController.java:38-74 | `docs/api-docs/sequence/index.md:122-131` |
| US-SEQUENCE-001…006 | stories | `../P0_5/prd-sequence.md` | — |
| POL-SEQUENCE-001…010 | policies | `../P0/business-policies-sequence.md` | — |

REQ ids (full text in srs-sequence.md → A4): REQ-SEQUENCE-001, REQ-SEQUENCE-002, REQ-SEQUENCE-003,
REQ-SEQUENCE-004, REQ-SEQUENCE-005, REQ-SEQUENCE-006, REQ-SEQUENCE-007, REQ-SEQUENCE-008,
REQ-SEQUENCE-009, REQ-SEQUENCE-010, REQ-SEQUENCE-011, REQ-SEQUENCE-012, REQ-SEQUENCE-013,
REQ-SEQUENCE-014, REQ-SEQUENCE-015, REQ-SEQUENCE-016, REQ-SEQUENCE-017, REQ-SEQUENCE-018,
REQ-SEQUENCE-019, REQ-SEQUENCE-020

AC ids (full text in srs-sequence.md → A4, one per REQ above): AC-SEQUENCE-001 … AC-SEQUENCE-020

RULE ids (full text in srs-sequence.md → A5): RULE-SEQUENCE-001, RULE-SEQUENCE-002, RULE-SEQUENCE-003,
RULE-SEQUENCE-004, RULE-SEQUENCE-005, RULE-SEQUENCE-006, RULE-SEQUENCE-007, RULE-SEQUENCE-008,
RULE-SEQUENCE-009, RULE-SEQUENCE-010, RULE-SEQUENCE-011, RULE-SEQUENCE-012, RULE-SEQUENCE-013,
RULE-SEQUENCE-014

APIs (full table in srs-sequence.md → B5)
| API-ID | Method | Endpoint | Owning SCR-ID | Permission |
|---|---|---|---|---|
| API-SEQUENCE-001 | POST | /api/v1/sequence/series | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_MANAGE` |
| API-SEQUENCE-002 | POST | /api/v1/sequence/series/search | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_VIEW` |
| API-SEQUENCE-003 | GET | /api/v1/sequence/series/{id} | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_VIEW` |
| API-SEQUENCE-004 | PUT | /api/v1/sequence/series/{id} | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_MANAGE` |
| API-SEQUENCE-005 | PUT | /api/v1/sequence/series/{id}/activate | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_MANAGE` |
| API-SEQUENCE-006 | PUT | /api/v1/sequence/series/{id}/deactivate | SCR-REQ-SEQUENCE-001 | `PERM_SEQUENCE_SERIES_MANAGE` |
In-process (not HTTP): `NumberSeriesApi.next(code)`, `NumberSeriesApi.preview(code)` — XM-SEQUENCE-003, no `@PreAuthorize`.

Error codes (full table in srs-sequence.md → A4)
| Code | HTTP | Code location |
|---|---|---|
| `SEQUENCE_NOT_CONFIGURED` | 422 | sequence/service/NumberAllocationService.java:107-109; sequence/domain/NumberSeriesDomain.java:59 |
| `SEQUENCE_PATTERN_INVALID` | 400 | sequence/domain/NumberPattern.java:159-162 |
| `NUMBER_SERIES_NOT_FOUND` | 404 | sequence/service/NumberSeriesService.java:145 |
| `NUMBER_SERIES_CODE_DUPLICATE` | 409 | sequence/domain/NumberSeriesDomain.java:39 |
All four: sequence/exception/SequenceErrorCodes.java:14-24; i18n `messages.properties` 166–169, `messages_ar.properties` 163–166.

Permissions (Permissions Summary)
`PERM_SEQUENCE_SERIES_VIEW` (gateway; read, search), `PERM_SEQUENCE_SERIES_MANAGE` (create, update,
activate, deactivate) — module `SEQUENCE`, screen `SEQUENCE_SERIES`; code-registered by
`SequencePermissions`, never seeded (sequence/permission/SequencePermissions.java:20-46).

Decisions
ADR ids: ADR-SEQUENCE-001, ADR-SEQUENCE-002, ADR-SEQUENCE-003 (all ACCEPTED, as built) —
`governance/analysis/decisions/SEQUENCE/`.

Event
"P1 completed: SEQUENCE v1 (as built) — 1 entity, 20 requirements, 20 acceptance criteria, 14 rules, 1 screen requirement, 6 APIs, 3 XM, 3 ADRs"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
