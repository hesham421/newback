<!-- source: PHASE:SVC-API / SUB:SVC-API-CRUD -->
<!-- context: SVC-API-HEADER.md — phase-level preamble -->
<!-- traces: DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006, DBF-MDL-012, DBF-MDL-013, DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-017, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010 -->
<!-- SUB:SVC-API-CRUD:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010 -->
### SUB — SVC-API-CRUD

<!-- API:API-MDL-002:START traces=REQ-MDL-001,REQ-MDL-002,DBF-MDL-002,DBF-MDL-003,DBF-MDL-004,DBF-MDL-005 -->
### API-MDL-002 — create lookup type
Endpoint     : POST /api/v1/mdl/lookup-types
Layers       : controller → `LookupTypeController.create` ; service → `LookupTypeService.create`
Request      : body `{key, ownerModuleCode, nameAr, nameEn}` — excludes {lookupTypePk, isActiveFl, audit}
Response     : 201 · `LookupTypeResponse`
Validations  : RULE-MDL-001 (full text: DATA-DOM §ENT-MDL-001) — owner module must be registered in SEC (QR-MDL-012); uniqueness of key (QR-MDL-013)
Errors       : `MDL-409-MODULE-NOT-REGISTERED` (409), `MDL-409-TYPE-DUP` (409)
Orchestration: check RULE-MDL-001 via XM-MDL-001 (QR-MDL-012) → validate key uniqueness (QR-MDL-013) → persist (QR-MDL-002) → return
Repository   : QR-MDL-002, QR-MDL-012, QR-MDL-013 · join NONE (QR-MDL-012 is a separate cross-module call, not a SQL join) · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_CREATE`
Localization : nameAr/nameEn required
<!-- API:API-MDL-002:END -->

<!-- API:API-MDL-003:START traces=REQ-MDL-003,DBF-MDL-004,DBF-MDL-005 -->
### API-MDL-003 — update lookup type (name only)
Endpoint     : PUT /api/v1/mdl/lookup-types/{id}
Layers       : controller → `LookupTypeController.update` ; service → `LookupTypeService.update`
Request      : body `{nameAr, nameEn}` — excludes {lookupTypePk, key, ownerModuleCode, isActiveFl, audit}
Response     : 200 · `LookupTypeResponse`
Validations  : RULE-MDL-003 (full text: DATA-DOM §ENT-MDL-001) — enforced by DTO shape (key absent from the request)
Errors       : `MDL-404-TYPE` (404)
Orchestration: load → update names (QR-MDL-003) → return
Repository   : QR-MDL-003 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`
Localization : both name fields updatable
<!-- API:API-MDL-003:END -->

<!-- API:API-MDL-004:START traces=REQ-MDL-004,DBF-MDL-006 -->
### API-MDL-004 — deactivate lookup type
Endpoint     : DELETE /api/v1/mdl/lookup-types/{id}
Layers       : controller → `LookupTypeController.deactivate` ; service → `LookupTypeService.deactivate`
Request      : path `id`
Response     : 200 · confirmation `{lookupTypePk, isActiveFl: false}`
Validations  : none beyond existence
Errors       : `MDL-404-TYPE` (404)
Orchestration: load → `LookupType.deactivate()` → persist (QR-MDL-004) → (RULE-MDL-004 then applies automatically at the next API-MDL-011 read — no cascade write to values needed, since the exclusion is a read-time join filter, not a stored flag on each value)
Repository   : QR-MDL-004 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`
Localization : n/a
<!-- API:API-MDL-004:END -->

<!-- API:API-MDL-006:START traces=REQ-MDL-006,REQ-MDL-007,DBF-MDL-012,DBF-MDL-013,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016 -->
### API-MDL-006 — create lookup value
Endpoint     : POST /api/v1/mdl/lookup-types/{id}/values
Layers       : controller → `LookupValueController.create` ; service → `LookupValueService.create`
Request      : path `id` (lookupTypeId); body `{code, nameAr, nameEn, sortOrder}` — excludes {lookupValuePk, isActiveFl, audit}
Response     : 201 · `LookupValueResponse`
Validations  : RULE-MDL-002 (full text: DATA-DOM §ENT-MDL-002) — code unique within the type (QR-MDL-014)
Errors       : `MDL-409-VALUE-DUP` (409), `MDL-404-TYPE` (404)
Orchestration: validate type exists → check RULE-MDL-002 (QR-MDL-014) → persist (QR-MDL-006) → return
Repository   : QR-MDL-006, QR-MDL-014 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_CREATE`
Localization : nameAr/nameEn required
<!-- API:API-MDL-006:END -->

<!-- API:API-MDL-007:START traces=REQ-MDL-008,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016 -->
### API-MDL-007 — update lookup value
Endpoint     : PUT /api/v1/mdl/lookup-values/{id}
Layers       : controller → `LookupValueController.update` ; service → `LookupValueService.update`
Request      : body `{nameAr, nameEn, sortOrder}` — excludes {lookupValuePk, lookupTypeId, code, isActiveFl, audit}
Response     : 200 · `LookupValueResponse`
Validations  : none beyond existence
Errors       : `MDL-404-VALUE` (404)
Orchestration: load → update (QR-MDL-007) → return
Repository   : QR-MDL-007 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`
Localization : both name fields updatable
<!-- API:API-MDL-007:END -->

<!-- API:API-MDL-008:START traces=REQ-MDL-009,DBF-MDL-017 -->
### API-MDL-008 — deactivate lookup value
Endpoint     : DELETE /api/v1/mdl/lookup-values/{id}
Layers       : controller → `LookupValueController.deactivate` ; service → `LookupValueService.deactivate`
Request      : path `id`
Response     : 200 · confirmation `{lookupValuePk, isActiveFl: false}`
Validations  : none beyond existence
Errors       : `MDL-404-VALUE` (404)
Orchestration: load → `LookupValue.deactivate()` → persist (QR-MDL-008) → return
Repository   : QR-MDL-008 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`
Localization : n/a
<!-- API:API-MDL-008:END -->

<!-- API:API-MDL-009:START traces=REQ-MDL-010,DBF-MDL-016 -->
### API-MDL-009 — reorder lookup values
Endpoint     : PATCH /api/v1/mdl/lookup-types/{id}/values/reorder
Layers       : controller → `LookupValueController.reorder` ; service → `LookupValueService.reorder`
Request      : path `id` (lookupTypeId); body `{orderedValueIds: [Long]}`
Response     : 200 · `List<LookupValueResponse>` in the new order
Validations  : every id in `orderedValueIds` must belong to the given lookupTypeId
Errors       : `MDL-400-REORDER-MISMATCH` (400)
Orchestration: validate membership → assign sortOrder = list position for each id → persist (QR-MDL-009, batch update, one transaction) → return
Repository   : QR-MDL-009 · join NONE · transaction READ_WRITE
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`
Localization : n/a
<!-- API:API-MDL-009:END -->
<!-- SUB:SVC-API-CRUD:END -->
