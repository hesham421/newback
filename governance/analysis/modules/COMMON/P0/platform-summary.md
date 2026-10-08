# PLATFORM SUMMARY — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 19b19a4 (1.3.0-SNAPSHOT; since tag v1.2.0 the package
          gained the shared helpers listed in `docs/CHANGELOG.md` [Unreleased] — copies that lived in
          two or more modules, no behaviour change — recorded in the 1.3.0 addendum below)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`, and core migrations (`V<N>__*.sql`) to
`erp-core/src/main/resources/db/migration/core/`. `file:line` points at main @ 19b19a4.

This analysis was NOT produced before the code: `com.erp.common` pre-dates erp-core (it is the shared
foundation the original SEC / MDL / CU / FILE / NOTIF code was written against — `../../CU/P0/platform-summary.md`
treats it as CU's foundation) and was reshaped by erp-core plan steps 01 (bundles), 05
(`GlobalAuditableEntity`, `VERSION`, `CONCURRENT_MODIFICATION`) and 15 (`NOT_FOUND`, page overflow,
wrapped exceptions). It records what exists, so that every later foundation change (the
`com.erp.common.idempotency` mechanism of the tenant-maturity plan C.4) is written as an "Implementation
Addendum" on top of it. The full description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (the conventions table is its lines 135–146); this file describes only the foundation's
place in it.

## OVERVIEW
`com.erp.common` هو الأساس الذي تستهلكه كل وحدة ولا يعتمد هو على أي وحدة: غلاف الاستجابة الموحّد
`ApiResponse`، الاستثناء ذو الرمز المسجَّل `LocalizedException` وتحويل `Status` إلى HTTP في
`GlobalExceptionHandler`، أدوات البحث (`SpecBuilder`، `PageableBuilder`، `SearchRequest`)،
الكيانان الأساسيان `GlobalAuditableEntity` / `AuditableEntity` (أعمدة التدقيق، القفل التفاؤلي،
مميّز المستأجر)، محوّلات القيم المنطقية، `SecurityContextHelper`، وحرّاس المجال المشتركة. لا جدول ولا
نقطة نهاية؛ كل تغيير فيه إضافي فقط لأنه جزء من الواجهة العامة للمكتبة. [`governance/analysis/platform/PROJECT-OVERVIEW.md` packages table; docs/RELEASE.md]

`com.erp.common` is the foundation every module consumes and that depends on no module (it imports no
other `com.erp` package — verified: no `import com.erp.` outside `com.erp.common` in the package). It
holds the response envelope, the coded exception and its HTTP mapping, the search builders, the two
mapped superclasses every entity extends, the boolean converters, the security-context helper and the
shared domain guards. It owns no table and no endpoint, and every change to it is additive, because it
is public API of the versioned library (`docs/RELEASE.md`).

## THE FOUNDATION IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| Response envelope | `ApiResponse<T>` (`success`, `data`, `error`, `timestamp`; nulls omitted), `ApiError` (`code`, `message`, `fieldErrors`), `FieldErrorItem` (`field`, `message`); `OperationCode.craftResponse(ServiceResult)` maps `Status` → HTTP for successes | common/web/ApiResponse.java:11-33; common/web/ApiError.java:9-14; common/web/FieldErrorItem.java:8-12; common/web/OperationCode.java:8-15 |
| Coded exception | `LocalizedException(Status, errorCode, args…)`; multi-error form `(Status, List<ErrorDetail>)` and `withDetails(Status, code, details)`; `ErrorDetail(field, errorCode, args)` | common/exception/LocalizedException.java:27-83; common/exception/ErrorDetail.java:25-39 |
| Status → HTTP | `Status` enum: SUCCESS 200, CREATED 201, UPDATED 200, NOT_FOUND 404, ALREADY_EXISTS 409, CONFLICT 409, BUSINESS_RULE_VIOLATION 422, VALIDATION_ERROR 400, PAYLOAD_TOO_LARGE 413, UNSUPPORTED_MEDIA_TYPE 415, UNAUTHORIZED 401, FORBIDDEN 403, TOO_MANY_REQUESTS 429, INTERNAL_ERROR 500 | common/domain/status/Status.java:7-20 |
| Global handler | `GlobalExceptionHandler` (`@RestControllerAdvice`, the only advice): `LocalizedException` → its status and code (+ `fieldErrors`); bean validation → 400 `VALIDATION_ERROR` per field; malformed body → 400; missing / mistyped parameter → 400 with the parameter name; 405 `METHOD_NOT_ALLOWED` (+ `Allow`); `DataIntegrityViolationException` → 409; optimistic lock → 409 `CONCURRENT_MODIFICATION`; `AccessDeniedException` → 403 `ACCESS_DENIED`; unknown path → 404 `NOT_FOUND` (1.2.0); catch-all → the first wrapped `LocalizedException` (depth ≤ 16, 1.2.0) else 500 `INTERNAL_ERROR` | common/web/GlobalExceptionHandler.java:38-223 |
| Common error codes | `VALIDATION_ERROR`, `INTERNAL_ERROR`, `ACCESS_DENIED`, `DATA_INTEGRITY_VIOLATION`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_FILTER_FIELD`, `UNSUPPORTED_FILTER_OPERATOR` (both `fieldErrors` details only), `CONCURRENT_MODIFICATION`, `NOT_FOUND`; each with EN (`messages.properties`) and AR (`messages_ar.properties`) text | common/exception/CommonErrorCodes.java:9-35; `erp-core/src/main/resources/i18n/messages.properties:15-22`, `:140`; `messages_ar.properties:12-19`, `:137` |
| Filter-side envelope | `FilterErrorResponseWriter.write(messageSource, request, response, status, code)` writes the same JSON envelope by hand for refusals raised in a servlet filter (locale from the request; unknown code = its own message); used by SEC's `SecSecurityErrorHandler` and the tenant module's `TenantResolutionFilter` | common/web/FilterErrorResponseWriter.java:18-46; sec/security/SecSecurityErrorHandler.java; tenant/security/TenantResolutionFilter.java |
| Search contract | `SearchRequest` (`filters`, `sortField`, `sortDirection`, `page`, `size`), `SearchFilter` (`field`, `operator`, `value`), `SearchOperator` (EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN), `SetAllowedFields`, `FieldValueConverter` (+ `Default`, `Boolean`, `Instant`), `SpecBuilder.build` (unknown field → 400 naming every offending field), `PageableBuilder.from` (default 20, max 200, overflow → 400 on `page`), `BaseSearchContractRequest` (the DTO base; a lifted id filter must be scalar EQUALS) | common/search/*.java; common/search/SpecBuilder.java:21-64; common/search/PageableBuilder.java:28-45; common/dto/BaseSearchContractRequest.java:25-145 |
| Base entities | `GlobalAuditableEntity` (`CREATED_BY`/`UPDATED_BY` length 100, `CREATED_AT`/`UPDATED_AT`, `@Version VERSION NOT NULL`; `AuditEntityListener` fills them) and `AuditableEntity extends GlobalAuditableEntity` (+ `@TenantId TENANT_ID NOT NULL, updatable = false`); every `@Entity` extends one of them, and only the five listed entities may be global | common/domain/GlobalAuditableEntity.java:22-53; common/domain/AuditableEntity.java:27-37; common/audit/AuditEntityListener.java:10-27; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:67-75`, `:110-133` |
| Domain guards | `DomainRules.assertUnique` (409 `ALREADY_EXISTS`), `assertNotBlank` (400); `StatusTransitions(allowed, errorCode).assertAllowed(from, to)` (422 `BUSINESS_RULE_VIOLATION`) | common/domain/DomainRules.java:17-30; common/domain/StatusTransitions.java:22-33 |
| Owned lookups | `OwnedLookups.read(rawKey, ownedKeys, unknownKeyErrorCode, reader)` + `LookupOptionResponse(code, labelAr, labelEn)`: a module serves only its own MDL keys and re-maps MDL's 404 to its own code | common/lookup/OwnedLookups.java:32-46; common/lookup/LookupOptionResponse.java:18-28 |
| Converters, helpers | `BooleanNumberConverter` (1/0; CU, FILE, NOTIF `SMALLINT` flags), `BooleanCharYNConverter` (Y/N; no `@Convert` site today); `SecurityContextHelper` (`getCurrentUsername`, `hasAuthority`, `currentCaller`, `currentActorOrSystem`, `currentRealm`, `REALM_*`, `CUSTOMER_AUTHORITY`); `Strings.truncate`, `UtcDates.startOfDay`, `PlainJson.MAPPER`, `TokenHasher.sha256Hex`; `ActiveFlagQueryHelper` (no consumer) | common/converter/*.java; common/util/SecurityContextHelper.java:7-83; common/util/*.java; common/search/ActiveFlagQueryHelper.java:7-19 |
| Persistence | none of its own: no table, no migration; it defines the column conventions every table follows (`../P2/db-script-common.md`) | `../P2/db-script-common.md` |
| HTTP surface | none (no endpoint, no permission); its codes appear on every module's api-docs page ("Known Error Codes", "Status -> HTTP Status Reference") | `docs/api-docs/*/index.md`; governance/analysis/platform/project-registry.md "Packages without an HTTP surface" |
| Public API | `com.erp.common.*` is public API of the library: changes are additive only and at least a MINOR release (1.2.0 was a MINOR for `NOT_FOUND` + `handleNoResource`) | docs/RELEASE.md; docs/CHANGELOG.md [1.2.0] |

## REALMS INTERPLAY
| Realm | Foundation handling | Code location |
|---|---|---|
| STAFF / CUSTOMER / SYSTEM | `SecurityContextHelper.currentRealm()`: `SYSTEM` without a caller (none, unauthenticated or anonymous), `CUSTOMER` when the caller holds `ROLE_CUSTOMER`, otherwise `STAFF`; `currentActorOrSystem()` answers `system` without a caller; `getCurrentUsername()` (audit columns) answers `system` for a missing or unauthenticated authentication | common/util/SecurityContextHelper.java:27-33, :54-82 |
| Both | the envelope, the codes and the handler are realm-neutral; `REALM_MISMATCH` and the realm filter belong to SEC | `docs/api-docs/sec/index.md` |

## DEPENDENCY MAP
```
every core module, every application ──consumes (XM-COMMON-001: envelope, exception, search, base entities, helpers)──▶ COMMON
COMMON ──depends on──▶ nothing in com.erp (Spring, Hibernate, Jakarta, Jackson, Lombok, OpenAPI annotations only)
tenant (TenantIdentifierResolver) ──resolves the @TenantId of AuditableEntity──▶ COMMON (Hibernate binding, no Java dependency of common on tenant)
autoconfigure ──scans com.erp.common first in CORE_PACKAGE_LIST; contributes the messageSource (app bundles, then i18n/messages)──▶ COMMON
```
Build order: `com.erp.common` is the first entry of `CORE_PACKAGE_LIST`
(autoconfigure/ErpCoreAutoConfiguration.java:88-89). Tier: ROOT foundation (L1).

## DEFERRED (not in scope of the as-built foundation)
| Item | Reason / activation trigger |
|---|---|
| A governed error catalogue (ERR ids) in common | not built: common provides the exception infrastructure and nine shared codes; module codes live in each module's `<Module>ErrorCodes` (`../../CU/P1/srs-cu.md` A2) |
| `IS NULL` / `OR` operators in `SpecBuilder` | not built; a request lifts such fields out and the service hand-writes the predicate (docs/DEVIATIONS.md [09] "CU search with an owner restriction") |
| A 503 `Status` | not built; `FILE_STORAGE_UNAVAILABLE` uses `INTERNAL_ERROR` (docs/DEVIATIONS.md [07]) |
| Localising the malformed-body message | left hardcoded in English (`handleMalformedRequestBody`); one bundle key cannot carry two texts for `VALIDATION_ERROR` (common/web/GlobalExceptionHandler.java:78-86) |
| `com.erp.common.idempotency` (`CORE_IDEMPOTENCY_KEY`, `Idempotency-Key` header) | tenant-maturity plan C.4 (documented by the implementing run when it lands; reference rows in `docs/plans/tenant-maturity-analysis-reference.md`) |
| `PasswordPolicy` | lives in SEC (`com.erp.sec.domain`), not in common (plan D.1) |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-common.md`, `business-policies-common.md`.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

| # | Kind | Delta | Source |
|---|---|---|---|
| 1 | CHANGED (moved, on main) | `web.FilterErrorResponseWriter` — the hand-written error envelope of the SEC and tenant filters, one copy | docs/CHANGELOG.md [Unreleased] Added; common/web/FilterErrorResponseWriter.java |
| 2 | CHANGED (moved, on main) | `search.InstantFieldValueConverter` — ISO-8601 filter values to `Instant` (AUDIT, SEC) | docs/CHANGELOG.md [Unreleased]; common/search/InstantFieldValueConverter.java |
| 3 | CHANGED (moved, on main) | `util.SecurityContextHelper.currentCaller()` / `currentActorOrSystem()` / `currentRealm()` and the `REALM_*` / `CUSTOMER_AUTHORITY` constants, which `DomainEvent` and `AuditApi` now alias | docs/CHANGELOG.md [Unreleased]; common/util/SecurityContextHelper.java:11-21, :54-82 |
| 4 | CHANGED (moved, on main) | `lookup.LookupOptionResponse` and `lookup.OwnedLookups` — the MDL-backed lookup endpoints of FILE and NOTIF (same JSON shape) | docs/CHANGELOG.md [Unreleased]; common/lookup/*.java |
| 5 | CHANGED (moved, on main) | `domain.StatusTransitions` (FILE document, NOTIF log) and `domain.DomainRules` (`assertUnique` / `assertNotBlank`, used by the Domain objects) | docs/CHANGELOG.md [Unreleased]; common/domain/StatusTransitions.java; common/domain/DomainRules.java |
| 6 | CHANGED (moved, on main) | `util.Strings.truncate`, `util.UtcDates.startOfDay`, `util.PlainJson.MAPPER`, `util.TokenHasher.sha256Hex(byte[])` | docs/CHANGELOG.md [Unreleased]; common/util/*.java |
| 7 | (not common) | `TenantContext.isPlatform()` was added to the tenant root package in the same change set | docs/CHANGELOG.md [Unreleased] |

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
