# ADR-COMMON-002 — `SpecBuilder` rejects unknown filter fields/operators with 400; page size capped at 200

Module  : COMMON     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Every module's `POST …/search` shares one contract: `filters[]{field, operator, value}`, `sortField`,
`sortDirection`, `page`, `size`, built into a JPA `Specification` by `SpecBuilder` and a `Pageable` by
`PageableBuilder` (`.claude/skills/build-create-service`, "shared search builders"). Until 2026-09-19
`SpecBuilder` silently skipped a filter whose field was not in the endpoint's allowed set. The SEC
frontend E2E run then found two searches (API-SEC-025 `username`, API-SEC-012 `name`) rendering an
unfiltered list that looked filtered, with no way for any client to tell "the filter matched
everything" from "the filter was discarded" (`erp-core/src/main/java/com/erp/common/search/SpecBuilder.java:43-53`
Javadoc). A sibling defect sat in the lifted id filters of `BaseSearchContractRequest`: the operator a
client sent never reached the hand-written predicate, so `NOT_EQUALS` produced the `EQUALS` result set
and an `IN` list was skipped (`common/dto/BaseSearchContractRequest.java:94-113` Javadoc). On paging,
the 1.2.0 hardening found that a `page` whose offset overflowed `int` surfaced as a 500
(`docs/DEVIATIONS.md` [15] "Huge `page`"). The alternatives were to keep skipping (lenient), to clamp
(silently return a different page), or to reject with a precise 400.

## Decision
**Reject, naming every offending field; cap the page size; reject an overflowing page** (as built):
- `SpecBuilder.build(request, allowedFields, converter)` first calls `assertFieldsAllowed`: every filter
  whose field is outside `SetAllowedFields` becomes an `ErrorDetail.ofField(field,
  UNSUPPORTED_FILTER_FIELD, field)` and the request is refused with
  `LocalizedException.withDetails(VALIDATION_ERROR, VALIDATION_ERROR, details)` — 400, top-level code
  `VALIDATION_ERROR`, one `fieldErrors` entry per field (`common/search/SpecBuilder.java:21-64`;
  `common/exception/CommonErrorCodes.java:20-27`); a null converted value adds no predicate; the
  operators are `EQUALS`, `NOT_EQUALS`, `LIKE` (lower-cased contains), `GREATER_THAN`,
  `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `IN` (`common/search/SearchOperator.java:3-12`;
  `SpecBuilder.java:67-80`);
- a lifted id filter (`BaseSearchContractRequest.extractLongFilter`) must be scalar `EQUALS` (null
  operator = EQUALS); anything else is refused the same way with `UNSUPPORTED_FILTER_OPERATOR`; a
  non-numeric value is 400 `VALIDATION_ERROR` (`common/dto/BaseSearchContractRequest.java:79-144`);
  fields a request handles outside the generic set are lifted out through
  `toCommonSearchRequest(excludeFields)` before they reach `SpecBuilder`;
- `PageableBuilder.from(request, allowedSortFields)`: `page` < 0 → 0; `size` ≤ 0 → 20; `size` > 200 → 200;
  an unknown sort field → no sort; `page × size + size > Integer.MAX_VALUE` → 400 `VALIDATION_ERROR`
  with `fieldErrors[0].field = page` — rejected, not clamped (`common/search/PageableBuilder.java:15-16`,
  `:28-45`);
- `tenantId` is never a client filter field: Hibernate adds the tenant predicate
  (`docs/DEVIATIONS.md` [09] "CU search with an owner restriction").

Reasons:
1. **Silent drops are indistinguishable from success on the wire**; the E2E finding showed real
   screens affected (`SpecBuilder.java:47-52`).
2. **The envelope already has the slot**: `fieldErrors` carries one entry per bad field with a
   bilingual message, no new top-level code (`CommonErrorCodes.java:20-24`).
3. **Clamping a huge page would silently return a different page**; rejecting names the field
   (`docs/DEVIATIONS.md` [15]).
4. **200 rows is the shared maximum** every list endpoint and the report run page honour
   (`report/dto/ReportRunRequest.java:27-28`).

## Consequences
- A client sending a filter the endpoint does not support gets 400 with the field names; the frontend
  must use the fields each endpoint declares (`docs/api-docs/<module>/` search pages).
- A field with special semantics (an association's column, an `OR` across two columns, `IS NULL`) is
  not expressible in `SpecBuilder`; the request lifts it out and the service writes the predicate
  (`SessionService.usernameMatches`; CU's `AppConfigurationRepository.ownedBy`, `docs/DEVIATIONS.md` [09]).
- A list can never exceed 200 rows per request; the report export has its own cap (ADR-REPORT-003).
- `UNSUPPORTED_FILTER_FIELD` / `UNSUPPORTED_FILTER_OPERATOR` are detail codes only; the api-docs list
  them as such.
- The behaviour before 2026-09-19 is history: no endpoint relies on skipped filters.

## Traces
REQ-COMMON-009, REQ-COMMON-010, REQ-COMMON-011, REQ-COMMON-012 · RULE-COMMON-002, RULE-COMMON-003,
RULE-COMMON-008 · POL-COMMON-004 · docs/DEVIATIONS.md [09], [15]; common/search/SpecBuilder.java Javadoc
(2026-09-19 decision); common/dto/BaseSearchContractRequest.java Javadoc
