<!-- source: PHASE:F1 / SUB:F1-SCR-MDL-001 -->
<!-- context: F1-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-001, AC-MDL-003, AC-MDL-005, AC-MDL-006, AC-MDL-008, AC-MDL-010, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, SCR-MDL-001, UXD-MDL-001 -->
<!-- SUB:F1-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F1 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F1-MODEL — ENT-MDL-001 — نوع اللوكب / LookupType
Source DTOs  : `LookupTypeResponse` (read) · `LookupTypeCreateRequest` · `LookupTypeUpdateRequest`
  lookupTypePk    : number · read-only (PK) · system-only · never shown as a business reference
  key             : string · maxLength 50 (db-script §1, ADR-MDL-010) · required on create,
                    **read-only on edit** — RULE-MDL-003, and the update request does not carry it
  ownerModuleCode : string · maxLength 10 · required on create, **read-only on edit** — a plain
                    string holding the code, never an enum and never a union of literals; its
                    valid set is the security module's registry (UXD-MDL-001)
  nameAr          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  isActiveFl      : boolean · read-only — flipped by API-MDL-004 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only (audit)

#### F1-MODEL — ENT-MDL-002 — قيمة اللوكب / LookupValue
Source DTOs  : `LookupValueResponse` (read) · `LookupValueCreateRequest` ·
               `LookupValueUpdateRequest` · `LookupValueReorderRequest`
  lookupValuePk : number · read-only (PK) · system-only
  lookupTypeId  : number · read-only — the path id of API-MDL-006, taken from the selected
                  parent, never typed
  code          : string · maxLength 50 · required on create, **read-only on edit** — the update
                  request does not carry it; unique within its type (RULE-MDL-002)
  nameAr        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  sortOrder     : number · required on create **and** on update — and the same field the reorder
                  writes through `{ orderedValueIds[] }`; two paths, one field
  isActiveFl    : boolean · read-only — flipped by API-MDL-008 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only

#### F1-SCREEN — SCR-MDL-001
Search model : master — key : string · LIKE · ownerModuleCode : string · EXACT (options from the
               UXD-MDL-001 hook) · isActiveFl : boolean · EXACT · plus page, size, sortDirection,
               all inside the one request object of API-MDL-001 (the only paged read on this
               screen). No `sortField` is modelled: QR-MDL-001 declares a single ordering
               (`ORDER BY key`) and PHASE 1 states the module offers no free-form sort parameter,
               so `sortDirection` is available on `key` alone and keying a variation the server
               cannot produce would fragment the cache for nothing (G7).
               detail — code : string · LIKE · lookupTypeId : number · EXACT (from the selected
               parent, not typed) — **no page, no size, no sort**: API-MDL-005 is unpaged
               (backend-execution-plan API-MDL-005, QR-MDL-005 `Pagination: NO`, SRS §B2), so
               none of the three is modelled and none belongs in this screen's detail cache key
               (a prior pass's correction — G2)
Form model   : type · create — key, ownerModuleCode, nameAr, nameEn (all required)
               type · edit   — nameAr, nameEn (required); key and ownerModuleCode read-only
               value · create — code, nameAr, nameEn, sortOrder (all required)
               value · edit   — nameAr, nameEn, sortOrder (required); code read-only
               reorder        — the ordered list of value ids, and only ever the type's complete
               value set (G3, see F2-QUERY VALUE REORDER); not a per-row edit and not a form
               excluded system fields : both PKs · lookupTypeId · both isActiveFl · the audit four
Container    : TREE_MASTER_DETAIL — a master list of types and, beside it, the selected type's
               values; each of the two entry surfaces models its own record and nothing else
The master read returns the published pagination envelope; the detail read returns a bare array,
returned whole because it is confined to one type. Both write models drop every property no
published write DTO carries, so no form offers a field the server would ignore (ADR-MDL-006).
<!-- SUB:F1-SCR-MDL-001:END -->
