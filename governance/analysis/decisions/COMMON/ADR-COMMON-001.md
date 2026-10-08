# ADR-COMMON-001 — `LocalizedException` with registered bilingual codes and the `Status` → HTTP envelope, including the filter-side writer

Module  : COMMON     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Six modules (SEC, MDL, CU, FILE, NOTIF, later TENANT, SEQUENCE, AUDIT, REPORT) answer the same clients
— the frontend and integrations — in Arabic and English. Every module raising its own exception types
and writing its own `@ControllerAdvice` would give each a different wire shape; a plain HTTP status
without a code would leave the client guessing which rule failed; hardcoded English messages would
ignore the Arabic user. Two more facts shaped the decision: a servlet filter (JWT, realm, tenant
resolution) refuses requests before the dispatcher, where no advice and no Jackson converter runs; and
some failures (the SEC user duplicate, the REPORT parameter check) must report several problems at
once. The pre-erp-core code already had `LocalizedException`, `Status` and one `GlobalExceptionHandler`
(`.claude/skills/gov-enforce-error-handling`); erp-core kept them and hardened them in steps 05
(`CONCURRENT_MODIFICATION`) and 15 (`NOT_FOUND`, wrapped exceptions, page overflow —
`docs/DEVIATIONS.md` [05], [15]; `docs/steps/15-report.md`).

## Decision
**One coded exception, one handler, one envelope, bilingual bundles, and a hand-written writer for
filters** (as built):
- a failure on a request path is a `LocalizedException(Status, errorCode, args…)`; several at once are
  `LocalizedException(Status, List<ErrorDetail>)` or `withDetails(Status, code, details)`; the code is a
  constant of a `<Module>ErrorCodes` class (or `CommonErrorCodes`) and its value is the i18n key, present
  in `messages.properties` (English base — there is no `messages_en`, `docs/DEVIATIONS.md` [01]) and
  `messages_ar.properties` (`erp-core/src/main/java/com/erp/common/exception/LocalizedException.java:27-83`;
  `common/exception/ErrorDetail.java:25-39`);
- `Status` maps to HTTP once: SUCCESS 200, CREATED 201, UPDATED 200, NOT_FOUND 404, ALREADY_EXISTS 409,
  CONFLICT 409, BUSINESS_RULE_VIOLATION 422, VALIDATION_ERROR 400, PAYLOAD_TOO_LARGE 413,
  UNSUPPORTED_MEDIA_TYPE 415, UNAUTHORIZED 401, FORBIDDEN 403, TOO_MANY_REQUESTS 429, INTERNAL_ERROR 500
  (`common/domain/status/Status.java:7-20`); `OperationCode.craftResponse(ServiceResult)` uses it for
  successes (`common/web/OperationCode.java:10-14`);
- `GlobalExceptionHandler` (`@RestControllerAdvice`, the only advice — feature modules never declare
  one, `gov-enforce-backend-contract` CU.7) answers `ApiResponse.failure(ApiError(code, message,
  fieldErrors))` with the message resolved in the request locale; framework failures are mapped to
  client errors (400 validation / malformed body / parameter, 405 with `Allow`, 409 data integrity, 409
  optimistic lock, 403 access denied, 404 unknown path); the catch-all answers the first wrapped
  `LocalizedException` in the cause chain (depth ≤ 16) and 500 `INTERNAL_ERROR` otherwise
  (`common/web/GlobalExceptionHandler.java:38-233`);
- `FilterErrorResponseWriter.write(messageSource, request, response, status, code)` writes the same
  envelope by hand for refusals raised in a filter, with the message resolved from `request.getLocale()`
  and the code itself when unknown (`common/web/FilterErrorResponseWriter.java:12-46`); it is the one
  copy SEC's `SecSecurityErrorHandler` and the tenant's `TenantResolutionFilter` use (moved into common
  in 1.3.0-SNAPSHOT, `docs/CHANGELOG.md` [Unreleased]);
- `fieldErrors[].field` carries, per handler, the detail's `field()` (or its code when null), the bean
  validation field, or the parameter name — documented on every api-docs page because the slot is not
  always a request field path (`docs/api-docs/sec/index.md` "What `error.fieldErrors[].field` carries").

Reasons:
1. **One client contract** for 105 operations across nine modules (`docs/api-docs/README.md`), the
   frontend reads `success`, `data`, `error.code` only.
2. **The code is the contract, the message is the user's**: a client matches on `TENANT_SUSPENDED`,
   a user reads "هذا المستأجر معلّق"; adding a locale is a bundle, not a code change.
3. **No 500 for a client mistake** (1.2.0): three responses that used to be 500 (`NOT_FOUND`, page
   overflow, wrapped `TENANT_CONTEXT_MISSING`) now carry their real status — a MINOR because it added a
   public constant and a public handler method (`docs/CHANGELOG.md` [1.2.0]).
4. **Filters are outside the dispatcher**: without the writer, a refused login would answer a
   container error page instead of the envelope.

## Consequences
- A raw `RuntimeException`, a generic not-found exception or a hardcoded message string on a request
  path is a governance violation (`gov-enforce-error-handling`); start-up failures (`IllegalStateException`
  from `ReportRegistry`, `FileStorageAutoConfiguration`) are the documented exception.
- Every new code needs two bundle lines; a missing line answers the code itself and logs a WARN — it
  does not fail the build.
- The malformed-body message stays hardcoded English: `VALIDATION_ERROR` already carries the generic
  text and one key cannot hold two texts (`GlobalExceptionHandler.java:78-86`).
- `UNSUPPORTED_FILTER_FIELD` / `UNSUPPORTED_FILTER_OPERATOR` are `fieldErrors` details under a top-level
  `VALIDATION_ERROR`, never a top-level wire code (ADR-COMMON-002).
- Changing the envelope, a `Status` mapping or a handler's status is a contract change across every
  module and would be a MAJOR; none is planned.

## Traces
REQ-COMMON-001, REQ-COMMON-002, REQ-COMMON-003, REQ-COMMON-004, REQ-COMMON-005, REQ-COMMON-006,
REQ-COMMON-007, REQ-COMMON-008 · RULE-COMMON-001, RULE-COMMON-007 · POL-COMMON-001, POL-COMMON-002,
POL-COMMON-003 · XM-COMMON-001 · docs/DEVIATIONS.md [01], [05], [15]; docs/steps/15-report.md;
docs/CHANGELOG.md [1.2.0], [Unreleased]
