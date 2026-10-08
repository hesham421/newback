## BUSINESS POLICIES — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Module   : COMMON  Source of truth : the code at main @ 19b19a4 (erp-core 1.2.0 behaviour);
           docs/steps/01-report.md, 05-report.md, 15-report.md; docs/DEVIATIONS.md [01], [05], [15]; docs/RELEASE.md; docs/CHANGELOG.md
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

CLIENT-SPECIFIC POLICIES

POL-COMMON-001 — غلاف استجابة واحد / One response envelope
  Statement (ar) : يجب على النظام أن يجيب كل نقطة نهاية JSON بالغلاف نفسه `{success, data, error{code, message, fieldErrors}, timestamp}`، نجاحًا كان أو فشلًا، بما في ذلك الرفض الصادر من مرشّح servlet.
  Statement (en) : The system shall answer every JSON endpoint with the same envelope `{success, data, error{code, message, fieldErrors}, timestamp}`, on success and on failure alike, including a refusal raised by a servlet filter.
  Pattern   : ubiquitous
  Trigger   : Any HTTP response of a core or application endpoint
  Rationale : one client contract for every module; the frontend reads `success`, `data` and `error.code` only
  Source    : common/web/ApiResponse.java:11-33; common/web/OperationCode.java:10-14; common/web/GlobalExceptionHandler.java:38-60; common/web/FilterErrorResponseWriter.java:24-33
  Status    : CONFIRMED (as built)

POL-COMMON-002 — كل خطأ يحمل رمزًا مسجَّلًا ثنائي اللغة / Every error carries a registered bilingual code
  Statement (ar) : يجب على النظام ألا يرمي من أي مسار طلب إلا `LocalizedException` برمز مسجَّل في صنف `<Module>ErrorCodes` (أو `CommonErrorCodes`)، تكون قيمته مفتاح رسالته في `messages.properties` (الإنجليزية الأساسية) و`messages_ar.properties`، وأن يُترجم الرسالة بلغة الطلب.
  Statement (en) : The system shall throw, on any request path, only a `LocalizedException` with a code registered in a `<Module>ErrorCodes` class (or `CommonErrorCodes`), whose value is its message key in `messages.properties` (the English base) and `messages_ar.properties`, and shall resolve the message in the request's locale.
  Pattern   : ubiquitous
  Trigger   : Any business or validation failure
  Rationale : the code is the client's contract, the message is the user's; an unknown key answers the code itself and logs a warning
  Source    : common/exception/LocalizedException.java:27-33; common/web/GlobalExceptionHandler.java:225-233; `erp-core/src/main/resources/i18n/messages.properties`, `messages_ar.properties`; docs/DEVIATIONS.md [01] (no `messages_en`); .claude/skills/gov-enforce-error-handling
  Status    : CONFIRMED (as built)

POL-COMMON-003 — لا 500 لخطأ العميل / No 500 for a client mistake
  Statement (ar) : يجب على النظام أن يجيب خطأ العميل برمزه وحالته الصحيحة لا بـ `INTERNAL_ERROR`: التحقق 400، جسم مشوَّه 400، معامل مفقود أو خاطئ النوع 400، طريقة غير مدعومة 405، تعارض بيانات 409، تعديل متزامن 409، رفض الوصول 403، مسار مجهول 404؛ والاستثناء الذي يلفّ `LocalizedException` يُجاب برمز الأخير.
  Statement (en) : The system shall answer a client mistake with its own code and status, never `INTERNAL_ERROR`: validation 400, malformed body 400, missing or mistyped parameter 400, unsupported method 405, data-integrity conflict 409, concurrent modification 409, access denied 403, unknown path 404; and an exception that wraps a `LocalizedException` shall be answered with the wrapped exception's code and status.
  Pattern   : unwanted
  Trigger   : Any exception reaching the dispatcher
  Rationale : 1.2.0 hardening (docs/steps/15-report.md): three responses moved from 500 to their real status
  Source    : common/web/GlobalExceptionHandler.java:62-223; docs/CHANGELOG.md [1.2.0] Fixed; docs/DEVIATIONS.md [15]
  Status    : CONFIRMED (as built)

POL-COMMON-004 — لا إسقاط صامت لمرشّح بحث / A search filter is never silently dropped
  Statement (ar) : يجب على النظام رفض طلب بحث يسمّي حقلًا غير مدعوم أو يستخدم معاملًا لا يمكن تلبيته بـ 400 يسمّي كل حقل مخالف، لا أن يتجاهله ويعيد قائمة تبدو مفلترة.
  Statement (en) : The system shall reject a search naming an unsupported filter field, or using an operator it cannot honour, with 400 naming every offending field, instead of ignoring it and returning a list that looks filtered.
  Pattern   : unwanted
  Trigger   : Any `POST …/search`
  Rationale : until 2026-09-19 such filters were skipped; the SEC frontend E2E run found unfiltered lists that looked filtered
  Source    : common/search/SpecBuilder.java:43-64; common/dto/BaseSearchContractRequest.java:94-128; common/exception/CommonErrorCodes.java:20-27
  Status    : CONFIRMED (as built)

POL-COMMON-005 — كل صف مدقَّق ومقفول تفاؤليًا / Every row is audited and optimistically locked
  Statement (ar) : يجب على كل كيان JPA أن يرث `AuditableEntity` أو `GlobalAuditableEntity`، فيحمل `CREATED_BY/AT` و`UPDATED_BY/AT` يملؤها `AuditEntityListener` من المستخدم الحالي (أو `system`)، و`VERSION` يرفض التحديث القديم بـ 409 `CONCURRENT_MODIFICATION`.
  Statement (en) : Every JPA entity shall extend `AuditableEntity` or `GlobalAuditableEntity`, carrying `CREATED_BY/AT` and `UPDATED_BY/AT` filled by `AuditEntityListener` from the current user (or `system`), and a `VERSION` that refuses a stale update with 409 `CONCURRENT_MODIFICATION`.
  Pattern   : ubiquitous
  Trigger   : Every insert and update
  Rationale : "who changed what, when" and lost-update protection by construction (step 05)
  Source    : common/domain/GlobalAuditableEntity.java:27-53; common/audit/AuditEntityListener.java:12-26; common/web/GlobalExceptionHandler.java:152-161; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:110-114`; docs/DEVIATIONS.md [05]
  Status    : CONFIRMED (as built)

POL-COMMON-006 — المستأجر بالبناء / The tenant by construction
  Statement (ar) : يجب على كل كيان خاص بمستأجر أن يرث `AuditableEntity` فيحمل `TENANT_ID` كمميّز Hibernate `@TenantId` غير قابل للتحديث؛ ولا يجوز أن يكون الكيان عامًا (`GlobalAuditableEntity` مباشرة) إلا إذا كان في قائمة السماح المعلنة.
  Statement (en) : Every tenant-scoped entity shall extend `AuditableEntity`, carrying `TENANT_ID` as a non-updatable Hibernate `@TenantId` discriminator; an entity may be global (extending `GlobalAuditableEntity` directly) only when it is on the declared allow-list.
  Pattern   : ubiquitous
  Trigger   : Every entity; every query, join and load by id
  Rationale : POL-TENANT-007 — isolation holds without any query remembering it; the allow-list (`ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`, `Tenant`, `AppConfiguration`) is enforced by ArchUnit
  Source    : common/domain/AuditableEntity.java:12-37; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:67-75`, `:116-133`
  Status    : CONFIRMED (as built)

POL-COMMON-007 — الأساس واجهة عامة إضافية فقط / The foundation is public API, additive only
  Statement (ar) : يجب ألا يتغيّر `com.erp.common` إلا بالإضافة (عضو عام جديد، رمز جديد، معالج جديد)، وكل إضافة إصدار MINOR على الأقل؛ لا إزالة ولا تغيير توقيع.
  Statement (en) : `com.erp.common` shall change only by addition (a new public member, code or handler), each addition being at least a MINOR release; nothing is removed and no signature changes.
  Pattern   : ubiquitous
  Trigger   : Any change to the package
  Rationale : every module and every consuming application compiles against it (docs/RELEASE.md public API); 1.2.0 was a MINOR for one constant and one handler method
  Source    : docs/RELEASE.md; docs/CHANGELOG.md [1.2.0] ("It is a MINOR, not a PATCH, because `com.erp.common` gained public members"); docs/DEVIATIONS.md [15]
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the foundation owns no MDL lookup. Its value sets are Java enums: `Status` (14 values,
common/domain/status/Status.java:7-20) and `SearchOperator` (8 values, common/search/SearchOperator.java:3-12),
plus the realm constants of `SecurityContextHelper` (common/util/SecurityContextHelper.java:11-18).

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Governed error catalogue in common | not built; codes live per module | — | `../../CU/P1/srs-cu.md` A2 |
| `IS NULL` / `OR` search operators | not built; hand-written `Specification` per case | explicit need | docs/DEVIATIONS.md [09] |
| `Status` 503 | not built | explicit need | docs/DEVIATIONS.md [07] |
| Localised malformed-body message | hardcoded English | contract change (own wire code) | common/web/GlobalExceptionHandler.java:78-86 |
| Idempotency mechanism | not built in 1.2.0 | tenant-maturity plan C.4 (1.3.0) | docs/plans/tenant-maturity-plan.md |
| Password policy | not in common (SEC) | tenant-maturity plan D.1 | docs/plans/tenant-maturity-plan.md |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | How are errors expressed and mapped? | `LocalizedException` with a registered bilingual code; `Status` → HTTP in one handler; a filter-side writer for pre-dispatcher refusals | pre-erp-core design, hardened in steps 05 and 15, as built | ADR-COMMON-001 |
| 2 | What happens to an unsupported filter? | 400 naming every offending field; page size capped at 200 | 2026-09-19 decision (SEC E2E finding), as built | ADR-COMMON-002 |
| 3 | What does every entity inherit? | `GlobalAuditableEntity` (audit + `@Version`) → `AuditableEntity` (+ `@TenantId`); global entities only from the allow-list | erp-core plan step 05, as built; ArchUnit step 12 | ADR-COMMON-003 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

| # | Kind | Policy-level delta | Source |
|---|---|---|---|
| 1 | CHANGED (POL-COMMON-007 applied) | the helper move (`FilterErrorResponseWriter`, `InstantFieldValueConverter`, `SecurityContextHelper` realm/actor members, `LookupOptionResponse`, `OwnedLookups`, `StatusTransitions`, `DomainRules`, `Strings`, `UtcDates`, `PlainJson`, `TokenHasher`) is additive to `com.erp.common` (no behaviour change) and therefore a MINOR | docs/CHANGELOG.md [Unreleased] Added |

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
