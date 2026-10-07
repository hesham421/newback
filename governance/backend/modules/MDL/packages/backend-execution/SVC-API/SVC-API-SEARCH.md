<!-- source: PHASE:SVC-API / SUB:SVC-API-SEARCH -->
<!-- context: SVC-API-HEADER.md — phase-level preamble -->
<!-- traces: DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006, DBF-MDL-012, DBF-MDL-013, DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-017, REQ-MDL-001, REQ-MDL-005, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013 -->
<!-- SUB:SVC-API-SEARCH:START traces=REQ-MDL-001,REQ-MDL-005,REQ-MDL-011,REQ-MDL-013 -->
### SUB — SVC-API-SEARCH (read-only)

<!-- API:API-MDL-001:START traces=REQ-MDL-001,DBF-MDL-002,DBF-MDL-003,DBF-MDL-004,DBF-MDL-005,DBF-MDL-006 -->
### API-MDL-001 — search lookup types
Endpoint     : POST /api/v1/mdl/lookup-types/search
Layers       : controller → `LookupTypeController.search` ; service → `LookupTypeService.search`
Request      : body `LookupTypeSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `key`, `ownerModuleCode`, `isActiveFl`, and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<LookupTypeResponse>` · `ApiResponse<Page<LookupTypeResponse>>`
Validations  : none (read-only)
Errors       : `MDL-500` only
Orchestration: load (QR-MDL-001) → map → return
Repository   : QR-MDL-001 · join NONE · transaction READ_ONLY
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_VIEW`
Localization : nameAr/nameEn returned
<!-- API:API-MDL-001:END -->

<!-- API:API-MDL-005:START traces=REQ-MDL-005,DBF-MDL-012,DBF-MDL-013,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016,DBF-MDL-017 -->
### API-MDL-005 — search values of a type
Endpoint     : POST /api/v1/mdl/lookup-types/values/search
Layers       : controller → `LookupValueController.search` ; service → `LookupValueService.search`
Request      : body `LookupValueSearchRequest` (BaseSearchContractRequest) — the CHILD variant: the parent `lookupTypeId` travels inside `filters[]` (never a path variable), plus `filters[]` of (field, operator, value) over `code`, and `page`, `size`, `sortField`, `sortDirection` (default sort = sortOrder)
Response     : 200 · `Page<LookupValueResponse>` · `ApiResponse<Page<LookupValueResponse>>`
Validations  : none
Errors       : `MDL-404-TYPE` (404, unknown or missing lookupTypeId)
Orchestration: load (QR-MDL-005) → map → return
Repository   : QR-MDL-005 · join NONE · transaction READ_ONLY
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_VIEW`
Localization : nameAr/nameEn returned
<!-- API:API-MDL-005:END -->

<!-- API:API-MDL-010:START traces=REQ-MDL-013,DBF-MDL-003,DBF-MDL-002,DBF-MDL-004,DBF-MDL-005 -->
### API-MDL-010 — browse registry by owner
Endpoint     : POST /api/v1/mdl/lookup-types/by-owner/search
Layers       : controller → `LookupTypeController.browseByOwner` ; service → `LookupTypeService.browseByOwner`
Request      : body `LookupTypeByOwnerSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `ownerModuleCode`, `key`; `isActiveFl` is NOT a client-supplied filter — the service applies "active types only" unconditionally; `page`/`size`/`sortField` are inherited but unused (response is not paginated)
Response     : 200 · `List<OwnerGroupResponse>` (ownerModuleCode → nested active LookupType list) · `ApiResponse<List<OwnerGroupResponse>>`
Validations  : none
Errors       : `MDL-500` only
Orchestration: load grouped (QR-MDL-010) → assemble → return
Repository   : QR-MDL-010 · join NONE (single-table, grouped in the service layer) · transaction READ_ONLY
Security     : screen MDL_TYPE_REGISTRY · permission `PERM_MDL_TYPE_REGISTRY_VIEW`
Localization : nameAr/nameEn per type
<!-- API:API-MDL-010:END -->

<!-- API:API-MDL-011:START traces=REQ-MDL-011,REQ-MDL-012,DBF-MDL-002,DBF-MDL-013,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016 -->
### API-MDL-011 — read values by key (consumer API)
Endpoint     : GET /api/v1/mdl/lookups
Layers       : controller → `LookupConsumerController.readByKey` ; service → `LookupConsumerService.readByKey`
Request      : query param `type` (the LookupType key, e.g. `PAYMENT_METHOD`)
Response     : 200 · `List<LookupValueResponse>` (active values only, ordered by sortOrder) — 404 if the key itself is unknown
Validations  : RULE-MDL-004 (full text: DATA-DOM §ENT-MDL-001) — resolve the type, confirm it is active, then return only its active values (REQ-MDL-012: unknown key → not-found, not empty success)
Errors       : `MDL-404-TYPE-KEY` (404)
Orchestration: resolve type by key (QR-MDL-015) → if not found: 404 → else: load its active values ordered by sortOrder (QR-MDL-011) → return (empty list is a valid success if the type is active but has zero active values, per the FIND_BY_CRITERIA default)
Repository   : QR-MDL-015, QR-MDL-011 · join intra-module (type → value, both owned by MDL) · transaction READ_ONLY
Security     : called by other modules' backends, not an end-user screen — gated the same as any secured API (permission `PERM_MDL_LOOKUPS_VIEW`, granted to the calling module's own service principal per the platform's module-to-module auth pattern, same mechanism as any other authenticated caller — no special "system" bypass)
Localization : nameAr/nameEn returned per value
<!-- API:API-MDL-011:END -->
<!-- SUB:SVC-API-SEARCH:END -->
