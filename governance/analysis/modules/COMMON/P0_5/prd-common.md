# PRD — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Module          : COMMON     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-common, business-policies-common
Stories         : 6   Policies covered : 7/7   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. There is no endpoint of its own; the
behaviour is verified through every module's endpoints (`TC-CORE-CORE-001…008` of
`docs/test-api/core-test-plan.md` for the cross-cutting cases) and the JUnit classes under
`erp-core/src/test/java/com/erp/common/` (`GlobalExceptionHandlerTest`, `ErrorResponseHardeningIntegrationTest`,
`PageableBuilderTest`).

## USER STORIES

US-COMMON-001
  Title          : غلاف واحد ورموز ثابتة / One envelope and stable codes
  Story          : As an API client (the frontend, an integration), I need every endpoint of every module to answer the same envelope with a stable error code and a message in my language, so that one client layer handles every response.
  Priority       : HIGH — the contract of all 105 documented operations
  Success metric : every `docs/api-docs/<module>/index.md` documents the same `ApiResponse<T>` and the same nine common codes; `Accept-Language: ar` yields Arabic messages (TC-CORE-REPORT-004 as one instance)
  Traces         : POL-COMMON-001, POL-COMMON-002
  Source         : common/web/ApiResponse.java:11-33; common/web/GlobalExceptionHandler.java:38-60, :225-233
  Status         : AS-BUILT

US-COMMON-002
  Title          : رمي خطأ مرمَّز والحصول على HTTP الصحيح / Throw a coded error and get the right HTTP
  Story          : As a module developer, I need to throw one exception with a `Status` and a registered code (one or several at once) and have the right HTTP status, code, message and `fieldErrors` produced for me, so that no module writes its own advice.
  Priority       : HIGH
  Success metric : `GlobalExceptionHandlerTest`; every module's `Raised by` rows in the api-docs
  Traces         : POL-COMMON-002, POL-COMMON-003
  Source         : common/exception/LocalizedException.java:27-83; common/exception/ErrorDetail.java:25-39; common/web/GlobalExceptionHandler.java:38-60
  Status         : AS-BUILT

US-COMMON-003
  Title          : بحث موحّد بلا إسقاط صامت / Uniform search without silent drops
  Story          : As a module developer, I need a search contract (filters, operators, sort, page) that builds the JPA specification and the pageable for me and refuses what it cannot honour, so that a client never gets an unfiltered list that looks filtered.
  Priority       : HIGH
  Success metric : an unsupported field answers 400 with `fieldErrors[].field` = the field; `page` overflow answers 400 on `page` (`PageableBuilderTest`; `ErrorResponseHardeningIntegrationTest.aPageWhoseOffsetOverflows_is400ValidationErrorOnPage`)
  Traces         : POL-COMMON-004
  Source         : common/search/SpecBuilder.java:21-64; common/search/PageableBuilder.java:28-45; common/dto/BaseSearchContractRequest.java:46-61
  Status         : AS-BUILT

US-COMMON-004
  Title          : التدقيق والقفل والمستأجر مجانًا / Audit, lock and tenant for free
  Story          : As a module developer, I need my entity to inherit audit columns, an optimistic lock and the tenant discriminator by extending one base class, so that isolation and traceability never depend on my remembering them.
  Priority       : HIGH
  Success metric : every `@Entity` extends a base (ArchUnit rule 2); a stale update answers 409 `CONCURRENT_MODIFICATION`; a cross-tenant id answers 404 (TC-CORE-TENANT-014…016)
  Traces         : POL-COMMON-005, POL-COMMON-006
  Source         : common/domain/GlobalAuditableEntity.java:22-53; common/domain/AuditableEntity.java:27-37; common/audit/AuditEntityListener.java:10-27
  Status         : AS-BUILT

US-COMMON-005
  Title          : قواعد المجال المشتركة / Shared domain guards
  Story          : As a module developer, I need the common guard lines (uniqueness, required text, allowed status transition, module-owned lookups) in one place, so that every Domain object throws the same way with its own code.
  Priority       : MEDIUM
  Success metric : 17 Domain objects use `DomainRules`; FILE and NOTIF use `StatusTransitions`; FILE and NOTIF lookup endpoints use `OwnedLookups`
  Traces         : POL-COMMON-002
  Source         : common/domain/DomainRules.java:17-30; common/domain/StatusTransitions.java:22-33; common/lookup/OwnedLookups.java:32-46
  Status         : AS-BUILT

US-COMMON-006
  Title          : الأساس مستقر عبر الإصدارات / A foundation stable across releases
  Story          : As a consuming application, I need `com.erp.common` to change only by addition and to announce each addition in a MINOR release, so that my code keeps compiling when I upgrade erp-core.
  Priority       : HIGH
  Success metric : docs/CHANGELOG.md [1.2.0] lists the two additions that made it a MINOR; nothing in common was removed between 1.1.0 and main
  Traces         : POL-COMMON-007
  Source         : docs/RELEASE.md; docs/CHANGELOG.md [1.2.0], [Unreleased]
  Status         : AS-BUILT

## THE ERROR STORY (US-COMMON-002, as built)
1. A Domain object or service throws `new LocalizedException(Status.X, ModuleErrorCodes.CODE, args…)` —
   or the multi-error form `new LocalizedException(Status.VALIDATION_ERROR, List<ErrorDetail>)` /
   `LocalizedException.withDetails(status, code, details)` — common/exception/LocalizedException.java:27-73.
2. `GlobalExceptionHandler.handleLocalizedException` answers `ex.getStatus().getHttpStatus()` with
   `error.code = ex.getErrorCode()`, `error.message = messageSource.getMessage(code, args, locale)`, and,
   for a multi-error exception, one `fieldErrors[]` entry per detail whose `field` is `detail.field()`
   or, when null, the detail's own code — common/web/GlobalExceptionHandler.java:38-60.
3. A refusal raised before the dispatcher (a servlet filter) writes the same envelope by hand through
   `FilterErrorResponseWriter` — common/web/FilterErrorResponseWriter.java:24-33.
4. Anything else is mapped by the dedicated handlers (400 / 405 / 409 / 403 / 404) or by the catch-all,
   which first looks for a wrapped `LocalizedException` — common/web/GlobalExceptionHandler.java:62-223.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-COMMON-001 | POL-COMMON-001, POL-COMMON-002 | common/web/ApiResponse.java:11-33 |
| US-COMMON-002 | POL-COMMON-002, POL-COMMON-003 | common/web/GlobalExceptionHandler.java:38-223 |
| US-COMMON-003 | POL-COMMON-004 | common/search/SpecBuilder.java:21-64 |
| US-COMMON-004 | POL-COMMON-005, POL-COMMON-006 | common/domain/AuditableEntity.java:27-37 |
| US-COMMON-005 | POL-COMMON-002 | common/domain/DomainRules.java:17-30 |
| US-COMMON-006 | POL-COMMON-007 | docs/RELEASE.md |
Every policy POL-COMMON-001 … POL-COMMON-007 appears in at least one row above (001 → US-001;
002 → US-001/002/005; 003 → US-002; 004 → US-003; 005, 006 → US-004; 007 → US-006).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | One advice and one exception type, or per-module error handling? | one `GlobalExceptionHandler`, one `LocalizedException`, registered bilingual codes | pre-erp-core design kept and hardened (steps 05, 15), as built | ADR-COMMON-001 |
| 2 | Skip or reject an unsupported filter? | reject, naming every field; cap the page size | 2026-09-19 decision, as built | ADR-COMMON-002 |
| 3 | One base entity or two? | `GlobalAuditableEntity` (audit + version) → `AuditableEntity` (+ tenant); global only from the allow-list | erp-core plan step 05 / 12, as built | ADR-COMMON-003 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (idempotent requests) | not built in 1.2.0 | tenant-maturity plan C.4 — documented by the implementing run when it lands; reference rows in `docs/plans/tenant-maturity-analysis-reference.md` |
| (IS NULL / OR search operators, 503 status, localised malformed-body message) | not built (business-policies-common.md SCOPE EXCEPTIONS) | explicit need |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

| # | Kind | Story delta | Source |
|---|---|---|---|
| 1 | CHANGED (US-COMMON-005 widened) | the shared guards and helpers now also include `StatusTransitions`, `DomainRules`, `OwnedLookups` + `LookupOptionResponse`, `FilterErrorResponseWriter`, `InstantFieldValueConverter`, `SecurityContextHelper` realm/actor members, `Strings`, `UtcDates`, `PlainJson`, `TokenHasher` — moved from module copies, no behaviour change | docs/CHANGELOG.md [Unreleased] |

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
