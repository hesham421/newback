<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# BACKEND EXECUTION PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Dialect : postgresql16
Framework : spring-boot-java (profile.stack.backend.framework)
Inputs : srs (v1, PRD-approved), db-script (v1), registry-srs (v1), registry-db (v1)
Open ADRs : 0
══════════════════════════════════════════════════════════════════

## PRE-GENERATION EXTRACTION — MDL v1 (working set; not part of the plan proper)

```
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      2 — ENT-MDL-001 (master), ENT-MDL-002 (lookup)
REQUIREMENTS  13 — REQ-MDL-001..013, each with 1 AC-MDL-*
RULES         4 — RULE-MDL-001..004
SCREENS       2 — SCR-REQ-MDL-001..002, composite per profile.conventions.composite_screen
PERMISSIONS   MDL_LOOKUPS, MDL_TYPE_REGISTRY page codes + PERM_<PAGE_CODE>_<ACTION>, gateway VIEW
LOOKUPS       none owned by MDL itself (SRS A6)
BUSINESS CODE none — no MDL entity has a platform-numbered business code
── FROM db-script ────────────────────────────────────────────────────────
TABLES        2 — MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE
PK GENERATION every table: `GENERATED ALWAYS AS IDENTITY`
COLUMNS       21 DBF-MDL-001..021
CONSTRAINTS   PK_*, UQ_MDL_LOOKUP_TYPE_KEY, UQ_MDL_LOOKUP_VALUE_TYPE_CODE, FK_LOOKUP_VALUE_TYPE; INDEXES IDX_*
XM            1 — XM-MDL-001 SOFT-READ → SEC_MODULE_REG, status ACTIVE
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES CONSUMED   ModuleRegistry (ENT-SEC-004, SEC) via XM-MDL-001
EXISTING LOOKUP KEYS        none reused (MDL is the mechanism, not a consumer of its own kind)
ID RANGES already used      API: none yet · QR: none yet
──────────────────────────────────────────────────────────────────────────
No row required §2A.3 extraction-failure handling.
```

## EXECUTION PLAN INDEX — MDL v1 — backend-execution-plan-mdl.md
Profile: erp · dialect: postgresql16 · framework: spring-boot-java
Open ADRs: 0

**ENTITY REGISTRY**
| ENT | Name | Table | Business code | Operations |
|---|---|---|---|---|
| ENT-MDL-001 | LookupType | MDL_LOOKUP_TYPE | none | create, read, search, update (name only), deactivate |
| ENT-MDL-002 | LookupValue | MDL_LOOKUP_VALUE | none | create, read, search, update, deactivate, reorder |

**FIELD REGISTRY**
| DBF | Property | Read-only | ENT |
|---|---|---|---|
| DBF-MDL-001 | lookupTypePk | Yes | ENT-MDL-001 |
| DBF-MDL-002 | key | create-only (immutable after, RULE-MDL-003) | ENT-MDL-001 |
| DBF-MDL-003 | ownerModuleCode | create-only | ENT-MDL-001 |
| DBF-MDL-004 | nameAr | No | ENT-MDL-001 |
| DBF-MDL-005 | nameEn | No | ENT-MDL-001 |
| DBF-MDL-006 | isActiveFl | Yes | ENT-MDL-001 |
| DBF-MDL-007 | createdBy | Yes | ENT-MDL-001 |
| DBF-MDL-008 | createdAt | Yes | ENT-MDL-001 |
| DBF-MDL-009 | updatedBy | Yes | ENT-MDL-001 |
| DBF-MDL-010 | updatedAt | Yes | ENT-MDL-001 |
| DBF-MDL-011 | lookupValuePk | Yes | ENT-MDL-002 |
| DBF-MDL-012 | lookupTypeId | create-only | ENT-MDL-002 |
| DBF-MDL-013 | code | create-only | ENT-MDL-002 |
| DBF-MDL-014 | nameAr | No | ENT-MDL-002 |
| DBF-MDL-015 | nameEn | No | ENT-MDL-002 |
| DBF-MDL-016 | sortOrder | No | ENT-MDL-002 |
| DBF-MDL-017 | isActiveFl | Yes | ENT-MDL-002 |
| DBF-MDL-018 | createdBy | Yes | ENT-MDL-002 |
| DBF-MDL-019 | createdAt | Yes | ENT-MDL-002 |
| DBF-MDL-020 | updatedBy | Yes | ENT-MDL-002 |
| DBF-MDL-021 | updatedAt | Yes | ENT-MDL-002 |

**API REGISTRY**
| API | Operation | Verb | Path | Traces (REQ, DBF) |
|---|---|---|---|---|
| API-MDL-001 | search types | POST | /api/v1/mdl/lookup-types/search | REQ-MDL-001 · DBF-MDL-002,003,004,005,006 |
| API-MDL-002 | create type | POST | /api/v1/mdl/lookup-types | REQ-MDL-001,REQ-MDL-002 · DBF-MDL-002,003,004,005 |
| API-MDL-003 | update type | PUT | /api/v1/mdl/lookup-types/{id} | REQ-MDL-003 · DBF-MDL-004,005 |
| API-MDL-004 | deactivate type | DELETE | /api/v1/mdl/lookup-types/{id} | REQ-MDL-004 · DBF-MDL-006 |
| API-MDL-005 | search values | POST | /api/v1/mdl/lookup-types/values/search | REQ-MDL-005 · DBF-MDL-012,013,014,015,016,017 |
| API-MDL-006 | create value | POST | /api/v1/mdl/lookup-types/{id}/values | REQ-MDL-006,REQ-MDL-007 · DBF-MDL-012,013,014,015,016 |
| API-MDL-007 | update value | PUT | /api/v1/mdl/lookup-values/{id} | REQ-MDL-008 · DBF-MDL-014,015,016 |
| API-MDL-008 | deactivate value | DELETE | /api/v1/mdl/lookup-values/{id} | REQ-MDL-009 · DBF-MDL-017 |
| API-MDL-009 | reorder values | PATCH | /api/v1/mdl/lookup-types/{id}/values/reorder | REQ-MDL-010 · DBF-MDL-016 |
| API-MDL-010 | browse registry by owner | POST | /api/v1/mdl/lookup-types/by-owner/search | REQ-MDL-013 · DBF-MDL-003,002,004,005 |
| API-MDL-011 | read values by key (consumer API) | GET | /api/v1/mdl/lookups | REQ-MDL-011,REQ-MDL-012 · DBF-MDL-002,013,014,015,016,006,017 |

**RULE REGISTRY**
| RULE | Name | Scope (ENT) | Message ar/en ✓ |
|---|---|---|---|
| RULE-MDL-001 | Reject a type for an unregistered owner module | ENT-MDL-001 | ✓ |
| RULE-MDL-002 | Reject a duplicate code within a type | ENT-MDL-002 | ✓ |
| RULE-MDL-003 | Key immutability after creation | ENT-MDL-001 | ✓ |
| RULE-MDL-004 | Exclude an inactive type's values from reads | ENT-MDL-001 | ✓ |

**SCREEN REGISTRY**
| Screen | Type | ENT | Permission names |
|---|---|---|---|
| MDL_LOOKUPS | secured | ENT-MDL-001, ENT-MDL-002 | PERM_MDL_LOOKUPS_VIEW, PERM_MDL_LOOKUPS_CREATE, PERM_MDL_LOOKUPS_UPDATE, PERM_MDL_LOOKUPS_DELETE |
| MDL_TYPE_REGISTRY | secured | ENT-MDL-001 | PERM_MDL_TYPE_REGISTRY_VIEW |

**LOOKUP REGISTRY** — none (MDL owns no lookup key of its own).

**QRC SUMMARY** — 15 QR ids, QR-MDL-001..015 — see Query Reference Catalog below.

**DB ALIGNMENT** — see manifest below — ALIGNED ✓ / issues: 0
**XM STATUS** — 1 (XM-MDL-001, SOFT-READ → SEC, ACTIVE)
**SECURITY** — 2 secured screens, data-driven role grants (no fixed role count)

## DB Alignment Manifest — MDL v1
All 21 rows: **status ✓ (aligned)**; XM column populated only for the one field the
SOFT-READ touches (ownerModuleCode) — a SOFT-READ is an application-level read, not a
column-level FK, so it is noted, not blocking.

| DBF | ENT | property | type | XM |
|---|---|---|---|---|
| DBF-MDL-001 | ENT-MDL-001 | lookupTypePk | Long | — |
| DBF-MDL-002 | ENT-MDL-001 | key | String | — |
| DBF-MDL-003 | ENT-MDL-001 | ownerModuleCode | String | XM-MDL-001 (validated, not FK-constrained) |
| DBF-MDL-004 | ENT-MDL-001 | nameAr | String | — |
| DBF-MDL-005 | ENT-MDL-001 | nameEn | String | — |
| DBF-MDL-006 | ENT-MDL-001 | isActiveFl | Boolean | — |
| DBF-MDL-007 | ENT-MDL-001 | createdBy | String | — |
| DBF-MDL-008 | ENT-MDL-001 | createdAt | Instant | — |
| DBF-MDL-009 | ENT-MDL-001 | updatedBy | String | — |
| DBF-MDL-010 | ENT-MDL-001 | updatedAt | Instant | — |
| DBF-MDL-011 | ENT-MDL-002 | lookupValuePk | Long | — |
| DBF-MDL-012 | ENT-MDL-002 | lookupTypeId | Long | — |
| DBF-MDL-013 | ENT-MDL-002 | code | String | — |
| DBF-MDL-014 | ENT-MDL-002 | nameAr | String | — |
| DBF-MDL-015 | ENT-MDL-002 | nameEn | String | — |
| DBF-MDL-016 | ENT-MDL-002 | sortOrder | Integer | — |
| DBF-MDL-017 | ENT-MDL-002 | isActiveFl | Boolean | — |
| DBF-MDL-018 | ENT-MDL-002 | createdBy | String | — |
| DBF-MDL-019 | ENT-MDL-002 | createdAt | Instant | — |
| DBF-MDL-020 | ENT-MDL-002 | updatedBy | String | — |
| DBF-MDL-021 | ENT-MDL-002 | updatedAt | Instant | — |

## Query Reference Catalog (QR-MDL-*)

> Logical specification only — never executable code.

| QR | Operation | Phase | API | Entity | Kind | Intent |
|---|---|---|---|---|---|---|
| QR-MDL-001 | FIND_BY_CRITERIA | SVC-API | API-MDL-001 | ENT-MDL-001 | FIND_BY_CRITERIA | search lookup types |
| QR-MDL-002 | SAVE | SVC-API | API-MDL-002 | ENT-MDL-001 | SAVE | create lookup type |
| QR-MDL-003 | UPDATE | SVC-API | API-MDL-003 | ENT-MDL-001 | UPDATE | rename lookup type |
| QR-MDL-004 | UPDATE | SVC-API | API-MDL-004 | ENT-MDL-001 | UPDATE | deactivate lookup type |
| QR-MDL-005 | FIND_BY_CRITERIA | SVC-API | API-MDL-005 | ENT-MDL-002 | FIND_BY_CRITERIA | search values of a type |
| QR-MDL-006 | SAVE | SVC-API | API-MDL-006 | ENT-MDL-002 | SAVE | create lookup value |
| QR-MDL-007 | UPDATE | SVC-API | API-MDL-007 | ENT-MDL-002 | UPDATE | update lookup value |
| QR-MDL-008 | UPDATE | SVC-API | API-MDL-008 | ENT-MDL-002 | UPDATE | deactivate lookup value |
| QR-MDL-009 | UPDATE | SVC-API | API-MDL-009 | ENT-MDL-002 | UPDATE (batch) | persist new sortOrder per reordered value |
| QR-MDL-010 | FIND_BY_CRITERIA | SVC-API | API-MDL-010 | ENT-MDL-001 | FIND_BY_CRITERIA | browse types grouped by ownerModuleCode |
| QR-MDL-011 | FIND_BY_CRITERIA | SVC-API | API-MDL-011 | ENT-MDL-001, ENT-MDL-002 | FIND_BY_CRITERIA | active values of an active type, by key, ordered by sortOrder |
| QR-MDL-012 | EXISTS | SVC-API | API-MDL-002 | (SEC_MODULE_REG, cross-module) | EXISTS | XM-MDL-001 / RULE-MDL-001: owner module registered in SEC? |
| QR-MDL-013 | EXISTS | SVC-API | API-MDL-002 | ENT-MDL-001 | EXISTS | uniqueness: key |
| QR-MDL-014 | EXISTS | SVC-API | API-MDL-006 | ENT-MDL-002 | EXISTS | RULE-MDL-002: code unique within lookupTypeId (also DB UQ) |
| QR-MDL-015 | FIND_ONE | SVC-API | API-MDL-011 | ENT-MDL-001 | FIND_ONE | RULE-MDL-004: resolve type by key and confirm isActiveFl=true |

Standard operation defaults (SEC's Phase 1 CORE table applies verbatim, restated once in
Phase 1 below rather than duplicated per QR). Join governance: QR-MDL-011 joins
MDL_LOOKUP_TYPE → MDL_LOOKUP_VALUE (intra-module, both owned here) to filter by the type's
own `isActiveFl` and `key` — never joins to resolve a lookup LABEL for a consumer (the
consumer receives code+labels directly, since these ARE the labels, not a foreign lookup).
QR-MDL-012 is the module's one cross-module read (XM-MDL-001, SOFT-READ, no join — a
separate query against SEC's table, reconciled in the service layer, not a SQL JOIN across
schemas).

---

















## Error Catalog — MDL v1

Envelope: `LocalizedException → {code, messageAr, messageEn}`. Runtime code format: `MDL-{http}[-{SLUG}]`.

| code | RULE / PLATFORM-STD | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| MDL-409-MODULE-NOT-REGISTERED | RULE-MDL-001 | API-MDL-002 | 409 | owner module not in SEC's ModuleRegistry | الوحدة المالكة غير مسجّلة في وحدة الأمان | The owning module is not registered in the Security module |
| MDL-409-TYPE-DUP | PLATFORM-STD (uniqueness, ADR-SEC-002 convention reused) | API-MDL-002 | 409 | duplicate key | هذا المفتاح مستخدم بالفعل | This key is already in use |
| MDL-404-TYPE | PLATFORM-STD (not found) | API-MDL-003, 004, 005 | 404 | unknown lookup type id | نوع اللوكب غير موجود | Lookup type not found |
| MDL-409-VALUE-DUP | RULE-MDL-002 | API-MDL-006 | 409 | duplicate code within type | هذا الرمز مستخدم بالفعل ضمن هذا النوع | This code is already used within this type |
| MDL-404-VALUE | PLATFORM-STD (not found) | API-MDL-007, 008 | 404 | unknown lookup value id | قيمة اللوكب غير موجودة | Lookup value not found |
| MDL-400-REORDER-MISMATCH | PLATFORM-STD (referential) | API-MDL-009 | 400 | reordered id not under the given type | إحدى القيم لا تنتمي لهذا النوع | One of the values does not belong to this type |
| MDL-404-TYPE-KEY | RULE-MDL-004 | API-MDL-011 | 404 | unknown or inactive type key | مفتاح النوع غير موجود أو غير نشط | This lookup type key does not exist or is inactive |
| MDL-403-FORBIDDEN | PLATFORM-STD (SEC's CORE interceptor) | every secured API | 403 | missing permission | غير مصرح بهذا الإجراء | You are not authorized to perform this action |
| MDL-503 | PLATFORM-STD (infrastructure) | API-MDL-002 | 503 | SEC unreachable during XM-MDL-001's validation call | تعذّر التحقق من الوحدة المالكة مؤقتًا | Could not verify the owning module right now |
| MDL-500 | PLATFORM-STD (infrastructure) | any | 500 | unhandled server error | حدث خطأ في الخادم | A server error occurred |

Every PLATFORM-STD row here follows the same umbrella convention SEC's ADR-SEC-002
established; MDL does not raise a new ADR for it (it cites SEC's, consistent with that
ADR's own consequence: "A future module's own P3.1 may cite the same PLATFORM-STD
convention... without re-deriving this decision").

## Alignment self-check (ALIGN) — MDL v1

```
TRACEABILITY      ✓ every API-*/QR-*/RULE-*/DBF-*/XM-* used in a phase appears in the Plan Index; every PHASE/SUB/atom carries traces=; every traces target exists upstream
BINDING (§2A)     ✓ no placeholder; every column cites a DBF; every RULE message present in ar+en; business code: none applicable
MANIFEST (§4)     ✓ only the mandated columns; all 21 DBF listed; the 1 XM-touched field (ownerModuleCode) is noted, not ⏸ (SOFT-READ is never blocking)
QRC (§5)          ✓ every API with a DB operation has ≥1 QR; no join for a lookup label; exact generation object named (Phase 1 CORE)
API (R3)          ✓ every RULE in a Validations line has a catalog row; platform errors carry RULE=PLATFORM-STD (citing SEC's ADR-SEC-002 convention); create/update requests exclude PK/audit/immutable fields
CROSS-MODULE      ✓ 1 XM from db-script, 1 placed (XM-MDL-001), 0 mismatched; ACTIVE status correctly reflects SEC's already-gated state; inbound stub uses XM-INBOUND-STUB-2 notation
SECURITY (R7)     ✓ both secured APIs' screens declare PERM_*; ERP-4 (every mutation endpoint declares its PERM_*): checked — every POST/PUT/PATCH/DELETE API above states one
CORE (R1)         ✓ layers, domain placement, error signalling (`MDL-{http}[-{SLUG}]`), type mapping (incl. the stated sort_order→Integer deviation) all declared
DECISIONS         ✓ 0 new ADR this stage; SEC's ADR-SEC-002 convention correctly cited, not re-derived
RESULT            BLOCKED ✗ — 3 findings
```

**Coverage — ENT/DBF → phases → QR → XM**: ENT-MDL-001/002 each appear in DATA-DOM with
≥1 QR under REPOSITORY OPS; all 21 DBF appear in the DB Alignment Manifest and their
owning entity's FIELDS table; XM-MDL-001 appears in INT-C, INT-R and ENT-MDL-001's
CROSS-MODULE line.

**Coverage — RULE → API → catalog code**: RULE-MDL-001→API-MDL-002→
MDL-409-MODULE-NOT-REGISTERED · RULE-MDL-002→API-MDL-006→MDL-409-VALUE-DUP ·
RULE-MDL-003→API-MDL-003→(enforced by DTO shape, no distinct error code needed — a request
including `key` simply has it ignored, not rejected, since the field is absent from the
DTO's schema entirely) · RULE-MDL-004→API-MDL-011→MDL-404-TYPE-KEY.

**Coverage — XM → status → blocks → workaround**: XM-MDL-001 → ACTIVE → blocks none →
no workaround needed.

## QR id definitions (cross-reference index — full detail in Query Reference Catalog above)
**QR-MDL-001** — FIND_BY_CRITERIA search lookup types [ENT-MDL-001, API-MDL-001]
**QR-MDL-002** — SAVE create lookup type [ENT-MDL-001, API-MDL-002]
**QR-MDL-003** — UPDATE rename lookup type [ENT-MDL-001, API-MDL-003]
**QR-MDL-004** — UPDATE deactivate lookup type [ENT-MDL-001, API-MDL-004]
**QR-MDL-005** — FIND_BY_CRITERIA search values of a type [ENT-MDL-002, API-MDL-005]
**QR-MDL-006** — SAVE create lookup value [ENT-MDL-002, API-MDL-006]
**QR-MDL-007** — UPDATE update lookup value [ENT-MDL-002, API-MDL-007]
**QR-MDL-008** — UPDATE deactivate lookup value [ENT-MDL-002, API-MDL-008]
**QR-MDL-009** — UPDATE batch persist reordered sortOrder [ENT-MDL-002, API-MDL-009]
**QR-MDL-010** — FIND_BY_CRITERIA browse types grouped by owner [ENT-MDL-001, API-MDL-010]
**QR-MDL-011** — FIND_BY_CRITERIA active values of an active type by key [ENT-MDL-001, ENT-MDL-002, API-MDL-011]
**QR-MDL-012** — EXISTS owner module registered in SEC (XM-MDL-001) [ENT-MDL-001, API-MDL-002]
**QR-MDL-013** — EXISTS uniqueness of key [ENT-MDL-001, API-MDL-002]
**QR-MDL-014** — EXISTS uniqueness of code within lookupTypeId [ENT-MDL-002, API-MDL-006]
**QR-MDL-015** — FIND_ONE resolve type by key + confirm active [ENT-MDL-001, API-MDL-011]

## Registry content
See `registry-exec-be-mdl.md`.
══════════════════════════════════════════════════════════════════
