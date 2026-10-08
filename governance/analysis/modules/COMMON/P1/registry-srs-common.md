## REGISTRY — P1 — COMMON v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 19b19a4). Java paths are relative to
`erp-core/src/main/java/com/erp/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-COMMON-001 | الكيان العام المدقَّق / Global auditable entity (`GlobalAuditableEntity`) | `@MappedSuperclass` (audit columns + `@Version`) | SHARED (owner) | REGISTERED (built) | common/domain/GlobalAuditableEntity.java:22-53 |
| ENT-COMMON-002 | الكيان الخاص بمستأجر / Tenant-scoped auditable entity (`AuditableEntity`) | `@MappedSuperclass` (+ `@TenantId TENANT_ID`) | SHARED (owner) | REGISTERED (built) | common/domain/AuditableEntity.java:27-37 |

Consumed
none — COMMON reads no table and imports no other `com.erp` package (verified).

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers | Status | Code location |
|---|---|---|---|---|---|
| XM-COMMON-001 | `com.erp.common.*` (37 classes: web, exception, domain, domain.status, dto, search, lookup, converter, audit, util) | public API of the library, additive only | every core module and application | ACTIVE (built) | docs/RELEASE.md; `../P0/module-registry-common.md` EXPOSED SURFACE |

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| `Status` (Java enum) | — | 14 | common/domain/status/Status.java:7-20 |
| `SearchOperator` (Java enum) | — | 8 | common/search/SearchOperator.java:3-12 |
| realm (String constants) | — | 3 (STAFF, CUSTOMER, SYSTEM) | common/util/SecurityContextHelper.java:11-18 |

Lookups consumed
none.

Screens
None — no screen, no endpoint (no SCR-REQ id).

Requirements
REQ count: 18 · AC count: 18 · RULE count: 10 · ENT count: 2 · SCR-REQ count: 0 · XM count: 1
Last sequence per atom: REQ: 018 · AC: 018 · ENT: 002 · RULE: 010 · SCR-REQ: none · XM: 001 · US: 006 · POL: 007

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-COMMON-001 / AC-COMMON-001 | Uniform response envelope | common/web/ApiResponse.java:11-33; common/web/OperationCode.java:10-14 | every `docs/api-docs/<module>/index.md` |
| REQ-COMMON-002 / AC-COMMON-002 | One coded exception | common/exception/LocalizedException.java:27-83 | `GlobalExceptionHandlerTest` |
| REQ-COMMON-003 / AC-COMMON-003 | Status → HTTP and message → locale | common/web/GlobalExceptionHandler.java:38-60, :225-233 | `GlobalExceptionHandlerTest`; TC-CORE-REPORT-004 (AR message) |
| REQ-COMMON-004 / AC-COMMON-004 | Framework failures as client errors | common/web/GlobalExceptionHandler.java:62-170 | TC-CORE-TENANT-010; api-docs "Other Possible Responses" |
| REQ-COMMON-005 / AC-COMMON-005 | Concurrent modification is 409 | common/web/GlobalExceptionHandler.java:152-161 | docs/DEVIATIONS.md [05] |
| REQ-COMMON-006 / AC-COMMON-006 | Unknown path is 404 | common/web/GlobalExceptionHandler.java:178-186 | `ErrorResponseHardeningIntegrationTest` (3 unknown-path tests); TC-CORE-CORE-007 |
| REQ-COMMON-007 / AC-COMMON-007 | A wrapped exception answers with its own code | common/web/GlobalExceptionHandler.java:195-223 | `ErrorResponseHardeningIntegrationTest.tenantContextMissing_wrappedByTheTransactionManager_isAnsweredWithItsOwnCode` |
| REQ-COMMON-008 / AC-COMMON-008 | The envelope from inside a filter | common/web/FilterErrorResponseWriter.java:24-33 | TC-CORE-TENANT-001 |
| REQ-COMMON-009 / AC-COMMON-009 | The search contract | common/search/SpecBuilder.java:21-41, :67-80 | TC-CORE-TENANT-011; `SecSearchFilterIntegrationTest` |
| REQ-COMMON-010 / AC-COMMON-010 | Reject an unsupported field or operator | common/search/SpecBuilder.java:43-64; common/dto/BaseSearchContractRequest.java:94-128 | `SecSearchFilterIntegrationTest` |
| REQ-COMMON-011 / AC-COMMON-011 | Paging | common/search/PageableBuilder.java:28-45 | `PageableBuilderTest` (4); `ErrorResponseHardeningIntegrationTest.aPageWhoseOffsetOverflows_is400ValidationErrorOnPage` |
| REQ-COMMON-012 / AC-COMMON-012 | Filter value conversion | common/search/InstantFieldValueConverter.java:26-35; common/search/BooleanFieldValueConverter.java:14-22 | `docs/api-docs/audit/` |
| REQ-COMMON-013 / AC-COMMON-013 | Audit columns are filled automatically | common/audit/AuditEntityListener.java:12-26 | — |
| REQ-COMMON-014 / AC-COMMON-014 | The tenant discriminator on every tenant-scoped entity | common/domain/AuditableEntity.java:34-37 | TC-CORE-TENANT-014…016; `TenantScopedQueryIntegrationTest` |
| REQ-COMMON-015 / AC-COMMON-015 | The global-entity allow-list | `CoreLibraryRulesArchTest.java:67-75`, `:110-133` | ArchUnit (`mvn verify`) |
| REQ-COMMON-016 / AC-COMMON-016 | Shared domain guards | common/domain/DomainRules.java:17-30; common/domain/StatusTransitions.java:22-33 | `TenantDomainTest`; `NotificationDomainsTest` |
| REQ-COMMON-017 / AC-COMMON-017 | Module-owned lookups | common/lookup/OwnedLookups.java:32-46 | `docs/api-docs/file/`, `docs/api-docs/notif/` lookup endpoints |
| REQ-COMMON-018 / AC-COMMON-018 | The security-context helper | common/util/SecurityContextHelper.java:27-82 | `DomainEventTest` |
| RULE-COMMON-001 | Every error is a `LocalizedException` with a registered bilingual code | common/exception/LocalizedException.java:27-33; common/web/GlobalExceptionHandler.java:195-233 | gov-enforce-error-handling; `GlobalExceptionHandlerTest` |
| RULE-COMMON-002 | `SpecBuilder` rejects unknown fields and operators | common/search/SpecBuilder.java:43-64 | `SecSearchFilterIntegrationTest` |
| RULE-COMMON-003 | Default 20, maximum 200, overflow rejected | common/search/PageableBuilder.java:15-16, :28-45 | `PageableBuilderTest` |
| RULE-COMMON-004 | `StatusTransitions` is a transition table | common/domain/StatusTransitions.java:9-33 | `NotificationDomainsTest` |
| RULE-COMMON-005 | `DomainRules` is two guards | common/domain/DomainRules.java:17-30 | `TenantDomainTest`, `CustomerDomainRulesTest` |
| RULE-COMMON-006 | `OwnedLookups` serves only the module's own keys | common/lookup/OwnedLookups.java:32-46 | FILE / NOTIF lookup endpoints |
| RULE-COMMON-007 | `FilterErrorResponseWriter` is the hand-written envelope | common/web/FilterErrorResponseWriter.java:24-45 | TC-CORE-TENANT-001…004 |
| RULE-COMMON-008 | `InstantFieldValueConverter` ISO-8601 → `Instant` | common/search/InstantFieldValueConverter.java:26-35 | audit query API |
| RULE-COMMON-009 | The small helpers | common/util/Strings.java:11-13; common/util/UtcDates.java:15-17; common/util/PlainJson.java:12; common/util/TokenHasher.java:21-34 | consumers in `../P0/module-registry-common.md` |
| RULE-COMMON-010 | `ActiveFlagQueryHelper` has no consumer | common/search/ActiveFlagQueryHelper.java:13-18 | grep at main @ 19b19a4 |
| ENT-COMMON-001 | Global auditable entity | common/domain/GlobalAuditableEntity.java:22-53 | `TenantSchemaIntegrationTest` (global entity set) |
| ENT-COMMON-002 | Tenant-scoped auditable entity | common/domain/AuditableEntity.java:27-37 | `TenantSchemaIntegrationTest:113` (21 tenant-aware entities) |
| XM-COMMON-001 | `com.erp.common.*` public API | docs/RELEASE.md | ArchUnit rule 1 (every core class may depend on `com.erp..`) |
| US-COMMON-001…006 | stories | `../P0_5/prd-common.md` | — |
| POL-COMMON-001…007 | policies | `../P0/business-policies-common.md` | — |

REQ ids (full text in srs-common.md → A4): REQ-COMMON-001, REQ-COMMON-002, REQ-COMMON-003,
REQ-COMMON-004, REQ-COMMON-005, REQ-COMMON-006, REQ-COMMON-007, REQ-COMMON-008, REQ-COMMON-009,
REQ-COMMON-010, REQ-COMMON-011, REQ-COMMON-012, REQ-COMMON-013, REQ-COMMON-014, REQ-COMMON-015,
REQ-COMMON-016, REQ-COMMON-017, REQ-COMMON-018

AC ids (full text in srs-common.md → A4, one per REQ above): AC-COMMON-001 … AC-COMMON-018

RULE ids (full text in srs-common.md → A5): RULE-COMMON-001, RULE-COMMON-002, RULE-COMMON-003,
RULE-COMMON-004, RULE-COMMON-005, RULE-COMMON-006, RULE-COMMON-007, RULE-COMMON-008, RULE-COMMON-009,
RULE-COMMON-010

Error codes (full table in srs-common.md → STANDALONE)
| Code | HTTP | Code location |
|---|---|---|
| `VALIDATION_ERROR` | 400 | common/web/GlobalExceptionHandler.java:62-121 |
| `INTERNAL_ERROR` | 500 | common/web/GlobalExceptionHandler.java:195-208 |
| `ACCESS_DENIED` | 403 | common/web/GlobalExceptionHandler.java:163-170 |
| `DATA_INTEGRITY_VIOLATION` | 409 | common/web/GlobalExceptionHandler.java:142-150 |
| `METHOD_NOT_ALLOWED` | 405 | common/web/GlobalExceptionHandler.java:127-140 |
| `UNSUPPORTED_FILTER_FIELD` | detail of 400 | common/search/SpecBuilder.java:54-64 |
| `UNSUPPORTED_FILTER_OPERATOR` | detail of 400 | common/dto/BaseSearchContractRequest.java:114-128 |
| `CONCURRENT_MODIFICATION` | 409 | common/web/GlobalExceptionHandler.java:152-161 |
| `NOT_FOUND` | 404 | common/web/GlobalExceptionHandler.java:178-186 |
All nine: common/exception/CommonErrorCodes.java:9-35; i18n `messages.properties` 15–22, 140; `messages_ar.properties` 12–19, 137.

Permissions
none.

Configuration
none of its own (the `messageSource` is contributed by `ErpCoreAutoConfiguration`, docs/DEVIATIONS.md [03]).

Decisions
ADR ids: ADR-COMMON-001, ADR-COMMON-002, ADR-COMMON-003 (all ACCEPTED, as built) — `governance/analysis/decisions/COMMON/`.

Event
"P1 completed: COMMON v1 (as built) — 2 mapped superclasses, 18 requirements, 18 acceptance criteria, 10 rules, 0 screen requirements, 1 XM, 3 ADRs"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Rules — delta
| Kind | Rule | Delta |
|---|---|---|
| CHANGED (moved) | RULE-COMMON-004…009, REQ-COMMON-018 members | `FilterErrorResponseWriter`, `InstantFieldValueConverter`, `SecurityContextHelper.currentCaller/currentActorOrSystem/currentRealm` + `REALM_*`/`CUSTOMER_AUTHORITY`, `LookupOptionResponse`, `OwnedLookups`, `StatusTransitions`, `DomainRules`, `Strings.truncate`, `UtcDates.startOfDay`, `PlainJson.MAPPER`, `TokenHasher.sha256Hex(byte[])` — one copy in common (docs/CHANGELOG.md [Unreleased]) |

Entities — delta: none on main. Error codes — delta: none on main.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
