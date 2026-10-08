# ADR-FILE-006 — Category list is POST `…/search` and a deactivated category cannot be reactivated (cf. ADR-SEC-003, ADR-MDL-006)

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (SRS Part B) — recorded after the fact (step 03)
Status  : ACCEPTED (non-breaking)

## Context
SRS API-FILE-007 described the category CRUD as `POST/GET/PUT/DELETE /api/v1/files/categories`, i.e. a
GET list with query parameters, and SCR-FILE-001 §B3 listed `categoryCode`, `nameAr`, `nameEn`,
`maxSizeBytes`, `allowedContentTypes`, `isActiveFl` as the drawer's editable fields. The platform's
build conventions (`build-create-controller` A.6.6: the shared search contract is POST-body-only;
`build-create-service`: separate activate / deactivate) and the precedents of SEC (ADR-SEC-003: every
collection read is `POST …/search` with the filter envelope) and MDL (ADR-MDL-006: `isActiveFl` is not
an input of the write form) applied when FILE was generated.

## Decision
- The category list is `POST /api/v1/files/categories/search` with `CategorySearchRequest`
  (`BaseSearchContractRequest`: filters, sort, page ≥ 0, size 1..200). The filter / sort whitelist is
  `categoryCode`, `nameAr`, `nameEn` (LIKE), `isActive` (EXACT), `createdAt`; a field outside it is
  dropped silently. There is no GET list.
- `PUT /api/v1/files/categories/{id}` accepts `nameAr`, `nameEn`, `maxSizeBytes`,
  `allowedContentTypes`, `allowPublic` (null keeps the value). `categoryCode` is immutable (it is the
  natural key, RULE-FILE-007, and the LOCAL / S3 key segment of every document stored under it) and
  `isActiveFl` is not an input of the update.
- `DELETE /api/v1/files/categories/{id}` is a soft deactivate (`IS_ACTIVE_FL = 0`, 204) with no
  child guard: documents already in the category keep it and stay readable; a new upload into it is
  refused 422 `FILE_CATEGORY_INACTIVE`; its public files keep being served (the public lookup checks
  `ALLOW_PUBLIC`, not the active flag). No activate endpoint exists — `FileCategory.activate()` is
  never exposed — so a deactivated category is retired for good through the API.

## Consequences
- The frontend's category screen (SCR-FILE-001) lists through the POST search, cannot toggle
  `isActiveFl` from the drawer, and offers "deactivate" as a one-way action; the P2_5 design records
  this in its own addendum.
- A category deactivated by mistake needs a new category with another code (the unique constraint
  is per tenant and code); an activate endpoint would be an additive MINOR change and would need the
  usual `PERM_FILE_CATEGORIES_UPDATE` gate.
- The deviation between B3 and the DTOs is the same kind ADR-MDL-006 recorded for MDL values.

## Traces
API-FILE-007 · ENTITY-FILE-002 · RULE-FILE-007 · SCR-FILE-001 §B3 · `FILE_CATEGORY_INACTIVE` ·
erp-core/src/main/java/com/erp/file/controller/FileCategoryController.java:27-30, 48-53, 69-77 ·
erp-core/src/main/java/com/erp/file/service/FileCategoryService.java:47-55, 84-100, 115-151 ·
erp-core/src/main/java/com/erp/file/dto/CategoryUpdateRequest.java · CategorySearchRequest.java ·
erp-core/src/main/java/com/erp/file/entity/FileCategory.java:99-105 ·
erp-core/src/main/java/com/erp/file/service/FileService.java:401-414 ·
docs/api-docs/file/endpoints/file-categories.md · docs/steps/03-report.md · ADR-SEC-003 · ADR-MDL-006
