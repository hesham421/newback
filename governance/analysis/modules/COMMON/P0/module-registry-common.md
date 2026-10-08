## MODULE REGISTRY — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Module Code    : COMMON   (package `com.erp.common`; no permission-registry module, no api-docs folder)
Bounded context: platform (shared foundation)
Layer / Type   : L1 / cross-cutting library (envelope, exceptions, search, base entities, helpers)     Execution tier : ROOT — first entry of `CORE_PACKAGE_LIST`
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 19b19a4)
Knowledge      : docs/steps/01-report.md, 05-report.md, 15-report.md; docs/DEVIATIONS.md [01], [05], [15]; docs/RELEASE.md; docs/CHANGELOG.md [1.2.0], [Unreleased]; .claude/skills/gov-enforce-error-handling, build-create-entity, build-create-service
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

ENTITIES OWNED   (mapped superclasses — no table of their own; ids assigned in P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| الكيان العام المدقَّق / Global auditable entity (`GlobalAuditableEntity`, `@MappedSuperclass`) | base of every global (tenant-less) entity: `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`, `UPDATED_AT`, `@Version VERSION` | SHARED (owner) — extended directly by exactly the allow-list `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry` (SEC), `Tenant` (TENANT), `AppConfiguration` (CU) | common/domain/GlobalAuditableEntity.java:22-53; sec/entity/ModuleRegistry.java; sec/entity/ScreenRegistry.java; sec/entity/ActionRegistry.java; tenant/entity/Tenant.java; cu/entity/AppConfiguration.java |
| الكيان الخاص بمستأجر / Tenant-scoped auditable entity (`AuditableEntity extends GlobalAuditableEntity`, `@MappedSuperclass`) | adds `@TenantId TENANT_ID` (NOT NULL, not updatable) | SHARED (owner) — the base of every other `@Entity` (21 tenant-aware entities, `TenantSchemaIntegrationTest:113`) | common/domain/AuditableEntity.java:27-37 |

Not entities, but owned runtime surface (no table): `ApiResponse`, `ApiError`, `FieldErrorItem`,
`OperationCode`, `GlobalExceptionHandler`, `FilterErrorResponseWriter` (web); `LocalizedException`,
`ErrorDetail`, `CommonErrorCodes` (exception); `Status`, `ServiceResult` (domain.status); `DomainRules`,
`StatusTransitions` (domain); `SearchRequest`, `SearchFilter`, `SearchOperator`, `SetAllowedFields`,
`FieldValueConverter` + `DefaultFieldValueConverter`, `BooleanFieldValueConverter`,
`InstantFieldValueConverter`, `SpecBuilder`, `PageableBuilder`, `ActiveFlagQueryHelper` (search);
`BaseSearchContractRequest` (dto); `LookupOptionResponse`, `OwnedLookups` (lookup);
`BooleanNumberConverter`, `BooleanCharYNConverter` (converter); `AuditEntityListener` (audit);
`SecurityContextHelper`, `Strings`, `UtcDates`, `PlainJson`, `TokenHasher` (util) — 37 classes under
common/.

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (Java enum `Status`) | حالة نتيجة الخدمة وتحويلها إلى HTTP / service status → HTTP | `SUCCESS` 200, `CREATED` 201, `UPDATED` 200, `NOT_FOUND` 404, `ALREADY_EXISTS` 409, `CONFLICT` 409, `BUSINESS_RULE_VIOLATION` 422, `VALIDATION_ERROR` 400, `PAYLOAD_TOO_LARGE` 413, `UNSUPPORTED_MEDIA_TYPE` 415, `UNAUTHORIZED` 401, `FORBIDDEN` 403, `TOO_MANY_REQUESTS` 429, `INTERNAL_ERROR` 500 | common/domain/status/Status.java:7-20 |
| (Java enum `SearchOperator`) | معامل التصفية / filter operator | `EQUALS`, `NOT_EQUALS`, `LIKE`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `IN` | common/search/SearchOperator.java:3-12 |
| (String constants, realm) | نطاق المستدعي / caller realm | `STAFF`, `CUSTOMER`, `SYSTEM`; `CUSTOMER_AUTHORITY = ROLE_CUSTOMER` | common/util/SecurityContextHelper.java:11-21 |

LOOKUPS CONSUMED
None (`OwnedLookups` is a helper the consuming module parameterises with its own keys).

SHARED ENTITIES CONSUMED
None — the foundation reads no table (no repository, no JDBC, no `EntityManager`).

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| (none in `com.erp`) | — | no `import com.erp.` outside `com.erp.common` in the package (verified at main @ 19b19a4) | common/**/*.java |
| Spring / Hibernate / Jakarta / Jackson / Lombok / OpenAPI annotations | library | `@RestControllerAdvice`, `MessageSource`, `Specification`, `Pageable`, `@MappedSuperclass`, `@TenantId`, `@Version`, `AttributeConverter`, `JsonMapper`, `@Schema` | common/web/GlobalExceptionHandler.java:9-25; common/domain/AuditableEntity.java:10; common/util/PlainJson.java:3 |
| tenant (runtime binding only) | Hibernate resolves the `@TenantId` of `AuditableEntity` through `TenantIdentifierResolver`; no Java dependency of common on tenant | tenant/config/TenantIdentifierResolver.java; common/domain/AuditableEntity.java:14-22 |
| autoconfigure (wiring) | `com.erp.common` is scanned first; the library's `messageSource` (application bundles, then `i18n/messages`, UTF-8, no system-locale fallback) feeds `GlobalExceptionHandler.resolveMessage` and `FilterErrorResponseWriter` | autoconfigure/ErpCoreAutoConfiguration.java:88-89; docs/DEVIATIONS.md [03] |
ROOT: YES.

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.common.*` (every public class above) | public API of the library (docs/RELEASE.md) | every core module, every application | XM-COMMON-001 | docs/RELEASE.md; governance/analysis/platform/PROJECT-OVERVIEW.md |
| `GlobalAuditableEntity` / `AuditableEntity` | base classes | every `@Entity` (ArchUnit rule 2) | — | `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:110-133` |
| `DomainRules` | guard | 17 Domain objects (cu, file, mdl, notif, sec, sequence, tenant) | — | cu/domain/AppConfigurationDomain.java; file/domain/FileCategoryDomain.java; mdl/domain/LookupTypeDomain.java, LookupValueDomain.java; notif/domain/NotificationChannelConfigDomain.java, NotificationTemplateDomain.java; sec/domain/ActionRegistryDomain.java, ModuleRegistryDomain.java, RoleActionGrantDomain.java, RoleDomain.java, RoleModuleGrantDomain.java, RoleScreenGrantDomain.java, ScreenRegistryDomain.java, SignupRequestDomain.java, UserDomain.java; sequence/domain/NumberSeriesDomain.java; tenant/domain/TenantDomain.java |
| `StatusTransitions` | guard | FILE `FileDocumentDomain`, NOTIF `NotificationLogDomain` | — | file/domain/FileDocumentDomain.java; notif/domain/NotificationLogDomain.java |
| `OwnedLookups` + `LookupOptionResponse` | lookup endpoint helper | FILE `FileLookupService`, NOTIF `NotificationLookupService` | — | file/service/FileLookupService.java; notif/service/NotificationLookupService.java |
| `FilterErrorResponseWriter` | filter-side envelope | SEC `SecSecurityErrorHandler`, TENANT `TenantResolutionFilter` | — | sec/security/SecSecurityErrorHandler.java; tenant/security/TenantResolutionFilter.java |
| `InstantFieldValueConverter` | search converter | AUDIT `AuditEventService`, SEC `SecSearchSupport` | — | audit/service/AuditEventService.java; sec/service/SecSearchSupport.java |
| `TokenHasher` | hashing | FILE `FileService` (content hash), SEC `CustomerVerifyToken`, `CustomerAccountService`, `DevPasswordResetSupportService`, `PasswordResetService` | — | file/service/FileService.java; sec/entity/CustomerVerifyToken.java; sec/service/*.java |
| `BooleanNumberConverter` (1/0) | JPA converter (`@Convert`) | CU `AppConfiguration`, FILE `FileCategory`, NOTIF `NotificationChannelConfig`, `NotificationTemplate` — the `SMALLINT IS_*_FL` columns of the pre-V10 CU/FILE/NOTIF tables; SEC's `IS_ACTIVE_FL` columns are `BOOLEAN` and use no converter | — | cu/entity/AppConfiguration.java:68; file/entity/FileCategory.java:71; notif/entity/NotificationChannelConfig.java:53; notif/entity/NotificationTemplate.java:84; sec/entity/User.java:112-117 |
| `BooleanCharYNConverter` (Y/N) | JPA converter | no `@Convert` site at main @ 19b19a4 (mentioned only in a comment of `sec/entity/User.java:112`); kept, public API | — | common/converter/BooleanCharYNConverter.java:7-24 |
| `Strings.truncate`, `UtcDates.startOfDay`, `PlainJson.MAPPER` | helpers | AUDIT (`AuditRecordingService`, `RequestInfoHolder`, `AuditEventStore`), NOTIF (`InAppChannelProvider`, `DispatchVariables`, `NotifLogSummaryReport`), SEC `SecUserListReport` | — | audit/service/*.java; audit/web/RequestInfoHolder.java; notif/channel/InAppChannelProvider.java; notif/service/DispatchVariables.java; notif/report/NotifLogSummaryReport.java; sec/report/SecUserListReport.java |
| `SecurityContextHelper` (`REALM_*`, `CUSTOMER_AUTHORITY`, `currentRealm`, `currentActorOrSystem`, `hasAuthority`) | helper | EVENTS `DomainEvent` (aliases), AUDIT `AuditApi` (aliases), REPORT `ReportService` (`hasAuthority`), every service | — | events/DomainEvent.java:37-46; audit/crossmodule/AuditApi.java:28-30; report/service/ReportService.java:77, :130 |
| `ActiveFlagQueryHelper` | search helper | no consumer today | — | common/search/ActiveFlagQueryHelper.java:7-19 |

PERMISSION MODULE → SCREEN → ACTIONS
None — no screen, no endpoint, no permission. The nine common error codes are answered by every
module's endpoints (`docs/api-docs/<module>/index.md` "Known Error Codes").

AUTO-DECISIONS
AUTO: module code COMMON for the analysis folder (package `com.erp.common`; the platform registry lists it as a package without an HTTP surface; CU's P0 described it as CU's foundation)
  FROM: governance/analysis/platform/project-registry.md; `../../CU/P0/module-registry-cu.md` SCOPE NOTE
  IF WRONG: rename the folder; no id changes.
AUTO: the two mapped superclasses classified SHARED (owner)
  FROM: every `@Entity` of the library extends one of them (ArchUnit rule 2)
  IF WRONG: none — the rule is enforced by the build.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Error expression and HTTP mapping | `LocalizedException` with registered bilingual codes; `Status` → HTTP in one handler; filter-side writer | ADR-COMMON-001 |
| 2 | Unsupported filter / paging | reject with 400 naming the fields; size ≤ 200; overflowing page → 400 | ADR-COMMON-002 |
| 3 | Entity hierarchy and global allow-list | `GlobalAuditableEntity` → `AuditableEntity`; five global entities, enforced by ArchUnit | ADR-COMMON-003 |

POLICIES OWNED (full text in business-policies-common.md)
POL-COMMON-001, POL-COMMON-002, POL-COMMON-003, POL-COMMON-004, POL-COMMON-005, POL-COMMON-006,
POL-COMMON-007
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

EXPOSED SURFACE — deltas
| Kind | Surface | Delta | Source |
|---|---|---|---|
| CHANGED (moved, on main) | `web.FilterErrorResponseWriter`; `search.InstantFieldValueConverter`; `util.SecurityContextHelper.currentCaller()` / `currentActorOrSystem()` / `currentRealm()` + `REALM_*` / `CUSTOMER_AUTHORITY`; `lookup.LookupOptionResponse`, `lookup.OwnedLookups`; `domain.StatusTransitions`, `domain.DomainRules`; `util.Strings.truncate`, `util.UtcDates.startOfDay`, `util.PlainJson.MAPPER`, `util.TokenHasher.sha256Hex(byte[])` | one copy in common replaces copies that lived in two or more modules; consumers listed above; no behaviour change | docs/CHANGELOG.md [Unreleased] Added |

ENTITIES OWNED — deltas: none on main. DEPENDENCIES — deltas: none on main.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
