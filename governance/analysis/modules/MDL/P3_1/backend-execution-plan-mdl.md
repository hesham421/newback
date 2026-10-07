# BACKEND EXECUTION PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Dialect : postgresql16
Framework : spring-boot-java (profile.stack.backend.framework)
Inputs : srs (v1, PRD-approved) · db-script (v1) · registry-srs (v1) · registry-db (v1)
Governance : FULL — the db-script exists and binds both entities of the SRS; no entity is `PENDING DB`.
Open ADRs : 0 new at this stage — 10 earlier ADRs applied, none BLOCKED (see DECISIONS APPLIED)
══════════════════════════════════════════════════════════════════

## PRE-GENERATION EXTRACTION — MDL v1 (working set; not part of the plan proper)

```
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      2 — ENT-MDL-001 نوع اللوكب / LookupType, kind master, SHARED (owner)
                  ENT-MDL-002 قيمة اللوكب / LookupValue, kind lookup, SHARED (owner)
REQUIREMENTS  13 — REQ-MDL-001..013, 13 AC-MDL-001..013 (one AC per REQ)
RULES         4 — RULE-MDL-001..004, ar+en message on every one
SCREENS       2 — SCR-REQ-MDL-001 اللوكبات العامة / Generic Lookups (composite: master+detail,
                  search+entry on both levels) · SCR-REQ-MDL-002 سجل أنواع اللوكب حسب المالك /
                  Lookup-type registry by owner (composite: grouped browse, read-only)
PERMISSIONS   page MDL_LOOKUPS · VIEW (gateway) · CREATE · UPDATE · DELETE (soft deactivate)
              page MDL_TYPE_REGISTRY · VIEW (gateway) only
              roles: منسّق المنصة / Platform administrator · مدير قوائم الوحدة المالكة /
              Owning-module lookup manager · الحساب الخدمي للوحدة المستهلكة / consuming-module
              service account (VIEW on MDL_LOOKUPS only)
LOOKUPS       none owned, none consumed (SRS A6) — MDL is the mechanism every other module's
              coded lists run on; it does not consume itself
BUSINESS CODE none on either entity (SRS A3 §3.3 NUMBERING): `key` is a business key the
              registrar writes, not a number the platform generates; the value is identified
              by `code` within its type. No numbering-engine call anywhere in this module.
── FROM db-script ────────────────────────────────────────────────────────
TABLES        2 — MDL_LOOKUP_TYPE (ENT-MDL-001) · MDL_LOOKUP_VALUE (ENT-MDL-002)
PK GENERATION strategy `sequence` — SEQ_MDL_LOOKUP_TYPE and SEQ_MDL_LOOKUP_VALUE
              (db-script BLOCK 1), one per table; both PK columns are plain BIGINT NOT NULL
              with no identity clause, no sequence default and no trigger
COLUMNS       21 — DBF-MDL-001..021
CONSTRAINTS   PK_MDL_LOOKUP_TYPE · PK_MDL_LOOKUP_VALUE · UQ_MDL_LOOKUP_TYPE_KEY ·
              UQ_MDL_LOOKUP_VALUE_TYPE_CODE · FK_LOOKUP_VALUE_TYPE · no CHECK, no trigger
INDEXES       IDX_MDL_LOOKUP_TYPE_OWNER · IDX_MDL_LOOKUP_TYPE_NAME_AR ·
              IDX_MDL_LOOKUP_TYPE_NAME_EN · IDX_MDL_LOOKUP_VALUE_TYPE_SORT
XM            1 — XM-MDL-001 SOFT-READ, MDL_LOOKUP_TYPE.owner_module_code → SEC_MODULE_REG
              (SEC, ENT-SEC-004), status ACTIVE, no FK and no constraint
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES CONSUMED   ENT-SEC-004 (ModuleRegistry) — reached through XM-MDL-001, never
                           redeclared and never joined
EXISTING LOOKUP KEYS       none to reuse — this module registers no key of its own
ID RANGES already used     API: API-MDL-001..011 · QR: QR-MDL-001..015 — both continue from
                           the ranges registry-db records for this module version; nothing is
                           renumbered and neither sequence restarts. This run adds
                           QR-MDL-016 and QR-MDL-017 (the two sequence allocations the
                           `sequence` PK strategy makes explicit) and no API id.
──────────────────────────────────────────────────────────────────────────
§2A.3 extraction failure: no row. Every table, column, constraint, sequence, rule text,
message and permission name below is bound to an input; none is invented and none is PENDING.
```

## EXECUTION PLAN INDEX — MDL v1 — backend-execution-plan-mdl.md
Profile: erp · dialect: postgresql16 · framework: spring-boot-java
Open ADRs: 0 — analysis/decisions/MDL/

**ENTITY REGISTRY**
| ENT | Name (ar / en) | Table | Business code | Operations |
|---|---|---|---|---|
| ENT-MDL-001 | نوع اللوكب / LookupType | MDL_LOOKUP_TYPE | none — `key` is the registrar's business key | VIEW · CREATE · UPDATE · DELETE |
| ENT-MDL-002 | قيمة اللوكب / LookupValue | MDL_LOOKUP_VALUE | none — `code`, unique within its type | VIEW · CREATE · UPDATE · DELETE |

**FIELD REGISTRY**
| DBF | Property | Read-only | ENT |
|---|---|---|---|
| DBF-MDL-001 | lookupTypePk | Yes (drawn from the sequence) | ENT-MDL-001 |
| DBF-MDL-002 | key | Yes after creation (RULE-MDL-003) | ENT-MDL-001 |
| DBF-MDL-003 | ownerModuleCode | Yes after creation | ENT-MDL-001 |
| DBF-MDL-004 | nameAr | No | ENT-MDL-001 |
| DBF-MDL-005 | nameEn | No | ENT-MDL-001 |
| DBF-MDL-006 | isActiveFl | Yes (flipped by the deactivate endpoint only) | ENT-MDL-001 |
| DBF-MDL-007 | createdBy | Yes (audit) | ENT-MDL-001 |
| DBF-MDL-008 | createdAt | Yes (audit) | ENT-MDL-001 |
| DBF-MDL-009 | updatedBy | Yes (audit) | ENT-MDL-001 |
| DBF-MDL-010 | updatedAt | Yes (audit) | ENT-MDL-001 |
| DBF-MDL-011 | lookupValuePk | Yes (drawn from the sequence) | ENT-MDL-002 |
| DBF-MDL-012 | lookupTypeId | Yes after creation (the parent is chosen, never retyped) | ENT-MDL-002 |
| DBF-MDL-013 | code | Yes after creation (RULE-MDL-002 contract at consumers) | ENT-MDL-002 |
| DBF-MDL-014 | nameAr | No | ENT-MDL-002 |
| DBF-MDL-015 | nameEn | No | ENT-MDL-002 |
| DBF-MDL-016 | sortOrder | No | ENT-MDL-002 |
| DBF-MDL-017 | isActiveFl | Yes (flipped by the deactivate endpoint only) | ENT-MDL-002 |
| DBF-MDL-018 | createdBy | Yes (audit) | ENT-MDL-002 |
| DBF-MDL-019 | createdAt | Yes (audit) | ENT-MDL-002 |
| DBF-MDL-020 | updatedBy | Yes (audit) | ENT-MDL-002 |
| DBF-MDL-021 | updatedAt | Yes (audit) | ENT-MDL-002 |

**API REGISTRY**
| API | Operation | Verb | Path | Traces (REQ · DBF) |
|---|---|---|---|---|
| API-MDL-001 | search lookup types | POST | /api/v1/mdl/lookup-types/search | REQ-MDL-001, REQ-MDL-003, REQ-MDL-013 · DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006 |
| API-MDL-002 | create lookup type | POST | /api/v1/mdl/lookup-types | REQ-MDL-001, REQ-MDL-002 · DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006 |
| API-MDL-003 | update lookup type | PUT | /api/v1/mdl/lookup-types/{id} | REQ-MDL-003 · DBF-MDL-004, DBF-MDL-005, DBF-MDL-009, DBF-MDL-010 |
| API-MDL-004 | deactivate lookup type | DELETE | /api/v1/mdl/lookup-types/{id} | REQ-MDL-004 · DBF-MDL-006, DBF-MDL-009, DBF-MDL-010 |
| API-MDL-005 | search values of a type | POST | /api/v1/mdl/lookup-types/values/search | REQ-MDL-005 · DBF-MDL-012, DBF-MDL-013, DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-017 |
| API-MDL-006 | create lookup value | POST | /api/v1/mdl/lookup-types/{id}/values | REQ-MDL-006, REQ-MDL-007 · DBF-MDL-012, DBF-MDL-013, DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-017 |
| API-MDL-007 | update lookup value | PUT | /api/v1/mdl/lookup-values/{id} | REQ-MDL-008 · DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-020, DBF-MDL-021 |
| API-MDL-008 | deactivate lookup value | DELETE | /api/v1/mdl/lookup-values/{id} | REQ-MDL-009 · DBF-MDL-017, DBF-MDL-020, DBF-MDL-021 |
| API-MDL-009 | reorder a type's values | PATCH | /api/v1/mdl/lookup-types/{id}/values/reorder | REQ-MDL-010 · DBF-MDL-016, DBF-MDL-020, DBF-MDL-021 |
| API-MDL-010 | browse the type registry by owner | POST | /api/v1/mdl/lookup-types/by-owner/search | REQ-MDL-013 · DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006 |
| API-MDL-011 | read active values by key (consumer) | GET | /api/v1/mdl/lookups | REQ-MDL-011, REQ-MDL-012 · DBF-MDL-002, DBF-MDL-006, DBF-MDL-013, DBF-MDL-016, DBF-MDL-017 |

**RULE REGISTRY**
| RULE | Name (en) | Scope | ENT | Message ar/en |
|---|---|---|---|---|
| RULE-MDL-001 | The owner module is registered in SEC | on create | ENT-MDL-001 | ✓ |
| RULE-MDL-002 | No duplicate code within one type | on create / on update | ENT-MDL-002 | ✓ |
| RULE-MDL-003 | A type's key is immutable after creation | on update | ENT-MDL-001 | ✓ |
| RULE-MDL-004 | An inactive type hides its values from consumers | on evaluate (every consumer read) | ENT-MDL-001, ENT-MDL-002 | ✓ |

**SCREEN REGISTRY**
| Screen | Type | ENT | Permission names |
|---|---|---|---|
| MDL_LOOKUPS | composite (master+detail, search+entry on both levels) | ENT-MDL-001, ENT-MDL-002 | PERM_MDL_LOOKUPS_VIEW, PERM_MDL_LOOKUPS_CREATE, PERM_MDL_LOOKUPS_UPDATE, PERM_MDL_LOOKUPS_DELETE |
| MDL_TYPE_REGISTRY | composite (grouped browse, read-only) | ENT-MDL-001 | PERM_MDL_TYPE_REGISTRY_VIEW |

**LOOKUP REGISTRY** — MDL owns no lookup key and consumes none (SRS A6). The table is written
empty rather than omitted, so the absence is a stated fact and not a gap: every key the
platform holds belongs to the module that registers it here, through API-MDL-002 and
API-MDL-006, and is seeded by that module — never by this one.

| Lookup key | Used in field (`DBF-*`) | `ENT-*` | Table owner module |
|---|---|---|---|
| — | — | — | — |

**QRC SUMMARY**
| QR | Operation | Phase | ENT |
|---|---|---|---|
| QR-MDL-001 | FIND_BY_CRITERIA | SVC-API | ENT-MDL-001 |
| QR-MDL-002 | SAVE | SVC-API | ENT-MDL-001 |
| QR-MDL-003 | UPDATE | SVC-API | ENT-MDL-001 |
| QR-MDL-004 | UPDATE (deactivate) | SVC-API | ENT-MDL-001 |
| QR-MDL-005 | FIND_BY_CRITERIA | SVC-API | ENT-MDL-002 |
| QR-MDL-006 | SAVE | SVC-API | ENT-MDL-002 |
| QR-MDL-007 | UPDATE | SVC-API | ENT-MDL-002 |
| QR-MDL-008 | UPDATE (deactivate) | SVC-API | ENT-MDL-002 |
| QR-MDL-009 | UPDATE (batch reorder) | SVC-API | ENT-MDL-002 |
| QR-MDL-010 | FIND_BY_CRITERIA | SVC-API | ENT-MDL-001 |
| QR-MDL-011 | FIND_BY_CRITERIA | SVC-API | ENT-MDL-002 |
| QR-MDL-012 | EXISTS (cross-module) | SVC-API | ENT-SEC-004 via XM-MDL-001 |
| QR-MDL-013 | EXISTS | SVC-API | ENT-MDL-001 |
| QR-MDL-014 | EXISTS | SVC-API | ENT-MDL-002 |
| QR-MDL-015 | FIND_ONE | SVC-API | ENT-MDL-001 |
| QR-MDL-016 | NATIVE (sequence allocation) | SVC-API | ENT-MDL-001 |
| QR-MDL-017 | NATIVE (sequence allocation) | SVC-API | ENT-MDL-002 |

**DB ALIGNMENT** — see the manifest below — ALIGNED ✓ / issues: 0
**XM STATUS** — 0 deferred · 1 ACTIVE (XM-MDL-001 → SEC), placed in INT-C and resolved in INT-R
**SECURITY** — 2 screens × 3 roles
**DBF ROWS** — 21
**XM ROWS** — 1
**API ROWS** — 11
**QR ROWS** — 17

## DB Alignment Manifest — MDL v1

Column names, DB types and SRS references are sourced by lookup from the db-script and are
not reproduced here. A required column that no endpoint writes carries its reason on the row.

| DBF | ENT | plan property | plan type | XM | status |
|---|---|---|---|---|---|
| DBF-MDL-001 | ENT-MDL-001 | lookupTypePk | Long | — | ✓ · system-generated (drawn from the table's sequence, `{entity}Pk`) |
| DBF-MDL-002 | ENT-MDL-001 | key | String | — | ✓ |
| DBF-MDL-003 | ENT-MDL-001 | ownerModuleCode | String | XM-MDL-001 (SOFT-READ, never blocking) | ✓ |
| DBF-MDL-004 | ENT-MDL-001 | nameAr | String | — | ✓ |
| DBF-MDL-005 | ENT-MDL-001 | nameEn | String | — | ✓ |
| DBF-MDL-006 | ENT-MDL-001 | isActiveFl | Boolean | — | ✓ |
| DBF-MDL-007 | ENT-MDL-001 | createdBy | String | — | ✓ · system-generated (audit) |
| DBF-MDL-008 | ENT-MDL-001 | createdAt | Instant | — | ✓ · system-generated (audit) |
| DBF-MDL-009 | ENT-MDL-001 | updatedBy | String | — | ✓ · system-generated (audit) |
| DBF-MDL-010 | ENT-MDL-001 | updatedAt | Instant | — | ✓ · system-generated (audit) |
| DBF-MDL-011 | ENT-MDL-002 | lookupValuePk | Long | — | ✓ · system-generated (drawn from the table's sequence, `{entity}Pk`) |
| DBF-MDL-012 | ENT-MDL-002 | lookupTypeId | Long | — | ✓ |
| DBF-MDL-013 | ENT-MDL-002 | code | String | — | ✓ |
| DBF-MDL-014 | ENT-MDL-002 | nameAr | String | — | ✓ |
| DBF-MDL-015 | ENT-MDL-002 | nameEn | String | — | ✓ |
| DBF-MDL-016 | ENT-MDL-002 | sortOrder | Integer | — | ✓ |
| DBF-MDL-017 | ENT-MDL-002 | isActiveFl | Boolean | — | ✓ |
| DBF-MDL-018 | ENT-MDL-002 | createdBy | String | — | ✓ · system-generated (audit) |
| DBF-MDL-019 | ENT-MDL-002 | createdAt | Instant | — | ✓ · system-generated (audit) |
| DBF-MDL-020 | ENT-MDL-002 | updatedBy | String | — | ✓ · system-generated (audit) |
| DBF-MDL-021 | ENT-MDL-002 | updatedAt | Instant | — | ✓ · system-generated (audit) |

Legend ✓ aligned · ✗ type mismatch (finding) · ⏸ deferred XM.
No derived or computed property exists in this module: every plan property above is a column
of one of the two tables, so no row carries `— (derived)` and no row needs an ADR.
`ownerModuleCode` is marked with its XM and **not** ⏸ — a SOFT-READ carries no constraint and
blocks no column (db-script §2.1); the value is stored whether or not SEC is reachable at the
moment of a later read, and only the create-time check consults SEC at all.

## Query Reference Catalog (QR-MDL-*)

> Logical specification only, never executable code: the implementer rewrites every entry with
> the real entity classes, mapped property names and the project's query strategy. Copying an
> entry into production code is a violation.

#### QR-MDL-016 — allocate a lookup type primary key
Phase        : SVC-API
API          : API-MDL-002
Entity       : ENT-MDL-001
Operation    : NATIVE
Intent       : draw the next primary key for a new lookup type from the object the db-script declares
Logical spec : SELECT nextval FROM SEQUENCE SEQ_MDL_LOOKUP_TYPE
Join         : NONE
Transaction  : READ_WRITE (participates in the caller's create transaction)
Locking      : NONE — the sequence allocates atomically, so two simultaneous creates can never
               receive the same key. This is why the key is never computed as MAX+1.
Pagination   : NO
Filters      : —
Result shape : count (one BIGINT value)
Null handling: never null

#### QR-MDL-017 — allocate a lookup value primary key
Phase        : SVC-API
API          : API-MDL-006
Entity       : ENT-MDL-002
Operation    : NATIVE
Intent       : draw the next primary key for a new lookup value from the object the db-script declares
Logical spec : SELECT nextval FROM SEQUENCE SEQ_MDL_LOOKUP_VALUE
Join         : NONE
Transaction  : READ_WRITE (participates in the caller's create transaction)
Locking      : NONE — allocated atomically by the sequence
Pagination   : NO
Filters      : —
Result shape : count (one BIGINT value)
Null handling: never null

#### QR-MDL-001 — search lookup types
Phase        : SVC-API
API          : API-MDL-001
Entity       : ENT-MDL-001
Operation    : FIND_BY_CRITERIA
Intent       : the page of lookup types matching the screen's filters — the master list of SCR-REQ-MDL-001
Logical spec : SELECT … FROM MDL_LOOKUP_TYPE WHERE [key LIKE :key] AND [owner_module_code = :owner]
               AND [(name_ar LIKE :name OR name_en LIKE :name)] AND [is_active_fl = :active]
               ORDER BY key — page/size
Join         : NONE — single-table; the owner module code is displayed as the stored code and
               is never joined to SEC (join governance, and XM-MDL-001 is a SOFT-READ)
Transaction  : READ_ONLY
Locking      : NONE — nothing is decided on and written back
Pagination   : YES (Page<T>) — default size 20, maximum 200
Filters      : key: LIKE · ownerModuleCode: EXACT · name: LIKE over both languages at once ·
               isActiveFl: EXACT
Result shape : full entity
Null handling: updatedBy / updatedAt null until the first update
Notes        : an empty result is success with empty content, never "not found"

#### QR-MDL-002 — persist a new lookup type
Phase        : SVC-API
API          : API-MDL-002
Entity       : ENT-MDL-001
Operation    : SAVE
Intent       : store a registered lookup type, active, with its key, owner and both names
Logical spec : INSERT INTO MDL_LOOKUP_TYPE (lookup_type_pk, key, owner_module_code, name_ar,
               name_en, is_active_fl, created_by, created_at) VALUES (…)
Join         : NONE
Transaction  : READ_WRITE
Locking      : the UNIQUE constraint is the guard. Two simultaneous registrations of the same
               key must not both succeed: `UQ_MDL_LOOKUP_TYPE_KEY` lets exactly one INSERT
               commit and the loser's integrity violation is mapped to MDL-409-TYPE-DUP. The
               EXISTS pre-check (QR-MDL-013) is a friendly message, never the guard — two
               requests both pass it.
Pagination   : NO
Filters      : —
Result shape : full entity
Null handling: updated_by / updated_at stay null until the first update

#### QR-MDL-003 — rename a lookup type
Phase        : SVC-API
API          : API-MDL-003
Entity       : ENT-MDL-001
Operation    : UPDATE
Intent       : replace the stored Arabic and English names of an existing type (REQ-MDL-003)
Logical spec : UPDATE MDL_LOOKUP_TYPE SET name_ar = :nameAr, name_en = :nameEn,
               updated_by = :principal, updated_at = :now WHERE lookup_type_pk = :id
Join         : NONE
Transaction  : READ_WRITE
Locking      : NONE, and the reason is stated rather than assumed: the statement writes the
               values the request carried, not a value derived from a prior read, so two
               simultaneous renames end with one of the two submissions stored whole — never a
               mixture. `key` and `owner_module_code` are absent from the statement, so neither
               can be moved by a race (RULE-MDL-003). No version column exists in the
               db-script, so no optimistic-lock check is specified in v1.
Pagination   : NO
Filters      : lookupTypePk: EXACT
Result shape : full entity
Null handling: —

#### QR-MDL-004 — deactivate a lookup type
Phase        : SVC-API
API          : API-MDL-004
Entity       : ENT-MDL-001
Operation    : UPDATE
Intent       : take a type out of service without removing it or touching its values (REQ-MDL-004)
Logical spec : UPDATE MDL_LOOKUP_TYPE SET is_active_fl = FALSE, updated_by = :principal,
               updated_at = :now WHERE lookup_type_pk = :id AND is_active_fl = TRUE
Join         : NONE
Transaction  : READ_WRITE
Locking      : the conditional predicate carries the state, so two simultaneous deactivations
               of the same type cannot both affect a row. The second affects none, and that is
               success, not an error: the requested end state already holds and REQ-MDL-004
               names no "already deactivated" outcome. Nothing is written to the type's values
               — RULE-MDL-004 is a read-time filter, not a cascade.
Pagination   : NO
Filters      : lookupTypePk: EXACT · isActiveFl: EXACT
Result shape : count (rows affected) + the stored entity
Null handling: —

#### QR-MDL-005 — search the values of one type
Phase        : SVC-API
API          : API-MDL-005
Entity       : ENT-MDL-002
Operation    : FIND_BY_CRITERIA
Intent       : the detail list of SCR-REQ-MDL-001 — every value of the selected type, active and
               inactive alike, in display order (REQ-MDL-005, AC-MDL-005)
Logical spec : SELECT … FROM MDL_LOOKUP_VALUE WHERE lookup_type_id = :typeId
               AND [code LIKE :code] ORDER BY sort_order ASC, code ASC
Join         : NONE — the parent type is already the caller's selection; its name is not
               re-read here
Transaction  : READ_ONLY
Locking      : NONE — nothing is decided on and written back
Pagination   : NO — the detail is bounded by one type and is returned whole, ordered
               (SRS §B2); the master list is the paginated half
Filters      : lookupTypeId: EXACT (mandatory — the detail is confined to the selected type) ·
               code: LIKE
Result shape : full entity
Null handling: updatedBy / updatedAt null until the first update
Notes        : deactivated values are part of this result by requirement — they disappear only
               from the consumer read (AC-MDL-009)

#### QR-MDL-006 — persist a new lookup value
Phase        : SVC-API
API          : API-MDL-006
Entity       : ENT-MDL-002
Operation    : SAVE
Intent       : store a new value under a selected type, active, with its code, labels and rank
Logical spec : INSERT INTO MDL_LOOKUP_VALUE (lookup_value_pk, lookup_type_id, code, name_ar,
               name_en, sort_order, is_active_fl, created_by, created_at) VALUES (…)
Join         : NONE
Transaction  : READ_WRITE
Locking      : the UNIQUE constraint is the guard. Two simultaneous creates of the same code
               under the same type must not both succeed: `UQ_MDL_LOOKUP_VALUE_TYPE_CODE`
               admits exactly one and the loser's integrity violation is mapped to
               MDL-409-VALUE-DUP. QR-MDL-014 is the friendly pre-check, never the guard.
Pagination   : NO
Filters      : —
Result shape : full entity
Null handling: updated_by / updated_at stay null until the first update

#### QR-MDL-007 — update a lookup value
Phase        : SVC-API
API          : API-MDL-007
Entity       : ENT-MDL-002
Operation    : UPDATE
Intent       : replace the stored labels and rank of an existing value (REQ-MDL-008)
Logical spec : UPDATE MDL_LOOKUP_VALUE SET name_ar = :nameAr, name_en = :nameEn,
               sort_order = :sortOrder, updated_by = :principal, updated_at = :now
               WHERE lookup_value_pk = :id
Join         : NONE
Transaction  : READ_WRITE
Locking      : NONE, stated: every written column comes from the request, none from a prior
               read, so concurrent edits end with one submission stored whole. `code` and
               `lookup_type_id` are absent from the statement, so no race can move a value
               between types or change the contract its consumers stored.
Pagination   : NO
Filters      : lookupValuePk: EXACT
Result shape : full entity
Null handling: —

#### QR-MDL-008 — deactivate a lookup value
Phase        : SVC-API
API          : API-MDL-008
Entity       : ENT-MDL-002
Operation    : UPDATE
Intent       : withdraw a value from consumer reads without losing the rows that reference it
Logical spec : UPDATE MDL_LOOKUP_VALUE SET is_active_fl = FALSE, updated_by = :principal,
               updated_at = :now WHERE lookup_value_pk = :id AND is_active_fl = TRUE
Join         : NONE
Transaction  : READ_WRITE
Locking      : the state is carried in the predicate, so of two simultaneous deactivations only
               one affects a row; the other affects none and that is the same success — the
               requested end state holds either way, and REQ-MDL-009 names no second outcome.
Pagination   : NO
Filters      : lookupValuePk: EXACT · isActiveFl: EXACT
Result shape : count (rows affected) + the stored entity
Null handling: —

#### QR-MDL-009 — persist a new display order for a type's values
Phase        : SVC-API
API          : API-MDL-009
Entity       : ENT-MDL-002
Operation    : UPDATE
Intent       : set every submitted value's rank to its position in the submitted order (REQ-MDL-010)
Logical spec : UPDATE MDL_LOOKUP_VALUE SET sort_order = :position, updated_by = :principal,
               updated_at = :now WHERE lookup_value_pk = :id AND lookup_type_id = :typeId
               — one statement per submitted id, all inside one transaction
Join         : NONE
Transaction  : READ_WRITE — the whole reorder commits or none of it does, so no caller ever
               observes half a reordering
Locking      : two simultaneous reorders of the same type are possible and neither is refused:
               each writes an explicit rank per id in one transaction, so the later transaction's
               order is the one stored whole — there is no read-then-write and no derived value
               to lose. `sort_order` carries no UNIQUE constraint, so a value created
               concurrently with a reorder keeps its own rank and may tie; the tie is broken by
               `code` in every ordered read (QR-MDL-005, QR-MDL-011), which is why a tie is a
               display detail and not a corruption. The `lookup_type_id` predicate is what makes
               an id from another type unwritable rather than merely rejected beforehand.
Pagination   : NO
Filters      : lookupValuePk: SET (the submitted ids) · lookupTypeId: EXACT
Result shape : count (rows affected per statement; a count below the submitted size means an id
               did not belong to the type → MDL-400-REORDER-MISMATCH, and the transaction rolls back)
Null handling: —

#### QR-MDL-010 — browse lookup types grouped by owner module
Phase        : SVC-API
API          : API-MDL-010
Entity       : ENT-MDL-001
Operation    : FIND_BY_CRITERIA
Intent       : the audit view of SCR-REQ-MDL-002 — every active type, grouped under its owner
               module code (REQ-MDL-013, AC-MDL-013)
Logical spec : SELECT … FROM MDL_LOOKUP_TYPE WHERE is_active_fl = TRUE
               AND [owner_module_code = :owner] AND [key LIKE :key]
               ORDER BY owner_module_code ASC, key ASC
Join         : NONE — single-table. The grouping is assembled in the service layer from an
               ordered single-table read; the owner module's own name is NOT fetched from SEC,
               because the screen groups by the stored code and the module publishes no
               grouped-name contract for it (join governance; XM-MDL-001 stays a create-time check)
Transaction  : READ_ONLY
Locking      : NONE — nothing is decided on and written back
Pagination   : NO — the result is bounded by the number of registered modules (SRS §B2)
Filters      : ownerModuleCode: EXACT · key: LIKE
Result shape : projection (key, nameAr, nameEn, ownerModuleCode), assembled into owner groups
Null handling: —
Notes        : inactive types are excluded by the requirement itself, so this read carries no
               state filter for a caller to widen

#### QR-MDL-011 — the active values of an active type, by key
Phase        : SVC-API
API          : API-MDL-011
Entity       : ENT-MDL-002
Operation    : FIND_BY_CRITERIA
Intent       : the reason the module exists — a consuming module's backend asks for a type's
               values by key and receives the active ones, ordered (REQ-MDL-011, AC-MDL-011)
Logical spec : SELECT v.… FROM MDL_LOOKUP_VALUE v JOIN MDL_LOOKUP_TYPE t
               ON v.lookup_type_id = t.lookup_type_pk
               WHERE t.key = :key AND t.is_active_fl = TRUE AND v.is_active_fl = TRUE
               ORDER BY v.sort_order ASC, v.code ASC
Join         : required — intra-module (value → its type), because the caller addresses the set
               by the type's `key` and the value table carries no key column (db-script §2,
               inbound note). No ADR is owed: the join governance rule covers a join to another
               entity for a filter, and both entities are this module's own; nothing here joins
               a lookup label from a foreign table.
Transaction  : READ_ONLY
Locking      : NONE — nothing is decided on and written back
Pagination   : NO — a coded list is returned whole, ordered; consumers render it as a select list
Filters      : key: EXACT · both active flags: EXACT (fixed TRUE, never caller-supplied)
Result shape : projection (code, nameAr, nameEn, sortOrder) — the code is what the consumer
               stores, the names are what it displays
Null handling: —
Notes        : a type that exists and is active but holds no active value returns an empty list
               with success — that is the FIND_BY_CRITERIA default, and it is a different answer
               from the unknown key of REQ-MDL-012, which QR-MDL-015 separates before this query runs

#### QR-MDL-012 — is the owner module registered in SEC?
Phase        : SVC-API
API          : API-MDL-002
Entity       : ENT-SEC-004 (ModuleRegistry, reached through XM-MDL-001) — not a table of this module
Operation    : EXISTS
Intent       : RULE-MDL-001 — refuse a lookup type whose owner module code has no registry row
Logical spec : not SQL. This module issues no statement against SEC's schema and holds no FK to
               it: the question is asked through SEC's injected in-process interface (INT-C,
               XM-MDL-001) and the answer is a boolean. It is catalogued as a QR because it is a
               repository-level fact the create path depends on, and the implementer must see
               that it is NOT a join and NOT a query of this module's own tables.
Join         : NONE — a cross-module call, never a SQL join (db-script §2.1, precedent ADR-FIN-001)
Transaction  : READ_ONLY — it participates in the caller's create transaction as a read; it
               opens none of its own and it commits nothing in SEC
Locking      : NONE, and the limit is stated: the answer can go stale between this check and the
               INSERT, and that is accepted — RULE-MDL-001's own Test-Hint says a module later
               unregistered in SEC does not invalidate the types it already owns, so there is no
               invariant here for two requests to break.
Pagination   : NO
Filters      : ownerModuleCode: EXACT
Result shape : count (exists / does not)
Null handling: an absent or inactive registry row is the "does not exist" answer → MDL-409-MODULE-NOT-REGISTERED

#### QR-MDL-013 — is this type key already taken?
Phase        : SVC-API
API          : API-MDL-002
Entity       : ENT-MDL-001
Operation    : EXISTS
Intent       : answer a duplicate key with the catalog message instead of a raw integrity error
Logical spec : SELECT COUNT(*) FROM MDL_LOOKUP_TYPE WHERE key = :key
Join         : NONE
Transaction  : READ_ONLY (inside the create transaction)
Locking      : NONE — deliberately. This query is not the uniqueness guard and must not be read
               as one: `UQ_MDL_LOOKUP_TYPE_KEY` is (QR-MDL-002). Two simultaneous creates both
               pass this check.
Pagination   : NO
Filters      : key: EXACT
Result shape : count
Null handling: —
Notes        : the key is immutable (RULE-MDL-003), so no update path needs the
               "excluding the current PK" form of this check

#### QR-MDL-014 — is this code already used under this type?
Phase        : SVC-API
API          : API-MDL-006
Entity       : ENT-MDL-002
Operation    : EXISTS
Intent       : RULE-MDL-002 — a friendly refusal before the database's own would fire
Logical spec : SELECT COUNT(*) FROM MDL_LOOKUP_VALUE WHERE lookup_type_id = :typeId AND code = :code
Join         : NONE
Transaction  : READ_ONLY (inside the create transaction)
Locking      : NONE — deliberately: `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` is the guard (QR-MDL-006).
               The same code under a different type is legal and this predicate says so.
Pagination   : NO
Filters      : lookupTypeId: EXACT · code: EXACT
Result shape : count
Null handling: —

#### QR-MDL-015 — resolve a type by key and confirm it is active
Phase        : SVC-API
API          : API-MDL-011
Entity       : ENT-MDL-001
Operation    : FIND_ONE
Intent       : REQ-MDL-012 and RULE-MDL-004 — separate "no such key" from "a key whose values
               are simply all inactive" before the value read runs
Logical spec : SELECT … FROM MDL_LOOKUP_TYPE WHERE key = :key
Join         : NONE
Transaction  : READ_ONLY
Locking      : NONE — nothing is decided on and written back
Pagination   : NO
Filters      : key: EXACT
Result shape : full entity (the caller needs `is_active_fl` as well as existence)
Null handling: an empty result, and an inactive row, both answer MDL-404-TYPE-KEY — a consuming
               module is told the same thing either way, because in both cases the platform
               holds no usable list under that key (AC-MDL-012, RULE-MDL-004)

**Standard operation defaults** apply as the engine states them (FIND_ONE by PK → read-only,
not found → the catalog row; FIND_BY_CRITERIA → read-only, filters and allowed sort fields
declared per search, an empty result is success; SAVE → PK and audit fields system-set;
UPDATE → PK, business key and audit excluded from the request; deactivate → `soft` per
profile.stack.db.delete_semantics, the active flag flipped; EXISTS → uniqueness check) except
where an entry above overrides them.

**No usage check precedes deactivation**, and the reason is a requirement, not an omission: a
lookup type or value that consumers already stored is exactly what the soft flag preserves.
Deactivating removes the value from future consumer reads (RULE-MDL-004, REQ-MDL-011) and
leaves every stored code intact, so there is no "in use" state that could block the act —
AC-MDL-004 asserts the type's three values stay stored and unchanged.

**Join governance**: one join exists in this module, QR-MDL-011, and it is intra-module and
required by the key-addressed read. No query joins another module's table; no display name of a
coded value is joined anywhere, because this module IS the store those names come from and it
returns them directly; the grouped browse (QR-MDL-010) assembles its groups in the service
layer over a single-table ordered read rather than joining SEC.

## ERROR CATALOG — MDL v1

Envelope `LocalizedException → {code, messageAr, messageEn}`; the runtime code format is
declared once in PHASE 1 — CORE and every row below is an instance of it. Downstream consumers
cite the **code** and never reproduce the message text.

| code | RULE | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| MDL-409-MODULE-NOT-REGISTERED | RULE-MDL-001 | API-MDL-002 | 409 | the submitted owner module code has no registry row in SEC (QR-MDL-012) | الوحدة المالكة غير مسجّلة في وحدة الأمان | The owning module is not registered in the Security module |
| MDL-409-TYPE-DUP | PLATFORM-STD — ADR-SEC-002 | API-MDL-002 | 409 | the submitted key is already held by another type; the platform-unique key of REQ-MDL-001 is a structural constraint, not a business rule | هذا المفتاح مستخدم بالفعل | This key is already in use |
| MDL-409-VALUE-DUP | RULE-MDL-002 | API-MDL-006, API-MDL-007 | 409 | the submitted code already exists under the same type (QR-MDL-014, and `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` behind it) | هذا الرمز مستخدم بالفعل ضمن هذا النوع | This code is already used within this type |
| MDL-404-TYPE | PLATFORM-STD — ADR-SEC-002 | API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006, API-MDL-009 | 404 | the path or filter names a lookup type id that no row carries | نوع اللوكب غير موجود | The lookup type was not found |
| MDL-404-VALUE | PLATFORM-STD — ADR-SEC-002 | API-MDL-007, API-MDL-008 | 404 | the path names a lookup value id that no row carries | قيمة اللوكب غير موجودة | The lookup value was not found |
| MDL-400-REORDER-MISMATCH | PLATFORM-STD — ADR-SEC-002 | API-MDL-009 | 400 | a submitted id does not belong to the type in the path, so the reorder writes nothing and rolls back | إحدى القيم لا تنتمي لهذا النوع | One of the values does not belong to this type |
| MDL-404-TYPE-KEY | RULE-MDL-004 | API-MDL-011 | 404 | the requested key matches no type, or matches an inactive one (QR-MDL-015) — an unknown key is never answered with an empty success (REQ-MDL-012) | لا يوجد نوع لوكب بهذا المفتاح | No lookup type exists with this key |

**The platform's own rows, not this module's** — a forbidden response, an unauthenticated
request, a malformed body and an unexpected failure are raised by the shared handler every
module sits behind, with the platform's own code strings; this module mints no code for them
and must not, or two code strings would exist for one condition. They are listed here so an
endpoint block can cite them, and they are outside the module code format by construction:

| code | RULE | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| ACCESS_DENIED | PLATFORM-STD — ADR-SEC-002 | every endpoint of PHASE 3 | 403 | the caller lacks the screen's gateway permission or the action's own | لا تملك صلاحية تنفيذ هذا الإجراء | You are not authorised to perform this action |
| VALIDATION_ERROR | PLATFORM-STD — ADR-SEC-002 | every endpoint carrying a body | 400 | a required field is missing or exceeds the column width the db-script declares | البيانات المُرسلة غير صحيحة | The submitted data is not valid |
| INTERNAL_ERROR | PLATFORM-STD — ADR-SEC-002 | every endpoint of PHASE 3 | 500 | an unexpected server-side failure; the database error itself never reaches the caller | حدث خطأ غير متوقع | An unexpected error occurred |

**No row describes a network failure**, and that is a consequence of the platform's shape, not
an oversight: `profile.conventions.module_interface` is `in_process`, so XM-MDL-001 is an
injected call inside one deployable. There is no hop to time out and no service-unavailable
condition for this module to report. A 503 row was carried by the previous revision of this
plan for exactly that imagined failure; it is struck here, and it could not have been raised by
any code path — `profile.stack.backend.api.http_statuses` declares no 503 either.

**RULE-MDL-003 carries no catalog row, deliberately.** A type's key is immutable because it is
absent from the update request entirely (the rule's own Test-Hint): a submitted key is ignored,
never rejected, so a row here would be raisable by nothing. This is the same reading the SRS
states, not a decision taken here.

---

<!-- PHASE:CORE:START traces=REQ-MDL-005,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013 -->
## PHASE 1 — CORE

**Layers** — controller → service → mapper → domain → repository
(`profile.stack.backend.layers`). Domain-behaviour placement: `domain_classes` — the
single-entity invariants (activation state, rank assignment) are methods on the domain classes;
the service layer owns the cross-module check, the transaction boundary and the mapping between
request and domain.

**Type mapping — postgresql16 → Java** (from `profile.stack.db.syntax_map`; column types only —
the PK-generation clause is not a type):

| postgresql16 | Java |
|---|---|
| BIGINT (pk) | Long |
| VARCHAR(n) | String |
| BOOLEAN | Boolean |
| TIMESTAMPTZ | Instant |
| INTEGER | Integer |

The INTEGER row is not a deviation taken here: `profile.stack.db.syntax_map` carries no integer
row and the db-script declared the addition itself, for `sort_order`, in its own header. No
other type in this module departs from the map, and no ADR is owed.

**Primary keys** — strategy `sequence` (`profile.stack.db.pk_generation`). The application
draws every key from the table's own sequence before the insert: `SEQ_MDL_LOOKUP_TYPE`
(QR-MDL-016) and `SEQ_MDL_LOOKUP_VALUE` (QR-MDL-017). The db-script declares no identity clause,
no sequence default and no PK trigger, so a JPA identity or table generator would contradict the
deployed schema; the entity contract is `GenerationType.SEQUENCE`. This paragraph is the
correction db-script §4.2 asks this stage for — the previous revision of this plan carried
`GENERATED ALWAYS AS IDENTITY` on both BINDINGS lines, which described nothing that was built.

**Runtime error-code format** — `{MOD}-{http}[-{SLUG}]`
(`profile.stack.backend.api.error_code_format`; `{MOD}` is this module's code, `{http}` is the
row's HTTP status, `{SLUG}` is SCREAMING-KEBAB and the bracketed half is optional). Every
module row of the ERROR CATALOG above is an instance of this one string, and every status it
carries is one `profile.stack.backend.api.http_statuses` declares. The three platform rows
beside them carry the shared handler's own code strings and are not instances of it, which is
stated there rather than disguised.

**Error signalling** — `LocalizedException → {code, messageAr, messageEn}`; responses are
wrapped in `ApiResponse<T>`, pages in `Page<T>`.

**Transaction scope** — `READ_ONLY` for every FIND_* and EXISTS query reached on a read path;
`READ_WRITE` for every SAVE and UPDATE, with the reorder's whole batch inside one transaction.
No endpoint of this module needs `REQUIRES_NEW`.

**Search contract** — the filters and sort fields this module offers are exactly the ones the
two screen requirements name, and are not widened here:

| Screen | filters | ordering |
|---|---|---|
| SCR-REQ-MDL-001 master | key (LIKE) · ownerModuleCode (EXACT) · name (LIKE, both languages at once) · isActiveFl (EXACT) | key ASC — paged, default 20, maximum 200 |
| SCR-REQ-MDL-001 detail | lookupTypeId (EXACT, mandatory) · code (LIKE) | sortOrder ASC, code ASC — not paged, bounded by one type |
| SCR-REQ-MDL-002 | ownerModuleCode (EXACT) · key (LIKE) | ownerModuleCode ASC, key ASC — not paged, bounded by the module count |

An empty result is success on every one of them. A request to sort on any other field is
refused by the shared handler's VALIDATION_ERROR; the module offers no free-form sort parameter.

**Lookup values** — all LOV values are runtime-loaded from the lookup module and no enum is
hardcoded in an API or a field spec (`profile.conventions.lookups`). In this module the rule is
not vacuous, it is inverted: MDL is the store that serves it. API-MDL-011 is the runtime load
every other module performs, and this module consumes no coded list of its own (SRS A6).
`isActiveFl` is a platform boolean, not a coded list, and `ownerModuleCode` is a module code
read from SEC's registry, not a lookup key.

**Numbering** — document numbers come from the platform numbering engine and are never
generated in a module (`profile.conventions.numbering`). Neither entity is a numbered document:
`key` and `code` are written by the user and validated for uniqueness, never allocated
(SRS A3 §3.3 NUMBERING). No endpoint of PHASE 3 calls a numbering service.

**Workflow engine** — forbidden (`profile.conventions.workflow_engine`). Both entities are
two-state (active / inactive) with one transition and no reverse (SRS A7); that is a flag, not
a workflow, and no engine is introduced for it.

**Audit fields** — `createdBy`, `createdAt`, `updatedBy`, `updatedAt` are filled by the platform
on both tables and never appear in a request DTO. The db-script sets no database DEFAULT on
them deliberately, so exactly one writer exists.

**Identity** — the authenticated principal reaches the audit columns as a string handed over by
the standard platform interceptor. That is a value, not a dependency on SEC's table, and it is
why no XM row exists for it (db-script §2, precedent ADR-FIN-001).

**Authorization** — evaluated by the platform authorisation layer on the page code and the
action the operation carries. No role name is written into any service of this module: services
see permissions, never roles. `VIEW` is the gateway — without it no other permission applies.

**Cross-module access** — one mechanism, stated once and not chosen per row:
`profile.conventions.module_interface` is `in_process`, so this module reaches SEC through an
injected interface inside the same deployable. No HTTP client, no base path to another module,
no timeout, no retry policy and no network error path exists anywhere in this plan.

**Languages** — every message in this plan is present in ar and en, and every field of both
entities carries an ar and an en label (PHASE 2). Both entities are bilingual by requirement:
`nameAr` and `nameEn` are stored on each, and the consumer read returns both so the caller can
render either without a second call.
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013 -->
## PHASE 2 — DATA-DOM

Entity count is 2 — below the split self-check, so no SUB is opened and both entity blocks are
written flat, master first and its dependent lookup second, the order the db-script creates the
tables in.

#### ENT-MDL-001 — نوع اللوكب / LookupType      kind: master
BINDINGS   table `MDL_LOOKUP_TYPE` · PK `lookup_type_pk` (DBF-MDL-001) · PK generation
`sequence` → sequence `SEQ_MDL_LOOKUP_TYPE` (db-script BLOCK 1, one per table) · db-script
version v1. The PK column is a plain BIGINT NOT NULL: no identity clause, no default, no trigger.
BUSINESS CODE  none — the entity is not a numbered document and its identifier is never used
outside the system (SRS A3 §3.3 NUMBERING). `key` (DBF-MDL-002) plays the role the `master`
default field `code` would play: it is the registrar's own business key, unique platform-wide
through `UQ_MDL_LOOKUP_TYPE_KEY`, generated by nobody and immutable after creation
(RULE-MDL-003). No second column named `code` exists on this table and none is invented here.
DEFAULT FIELDS (profile.conventions.entity_defaults.master: nameAr, nameEn, code, isActiveFl,
createdBy, createdAt, updatedBy, updatedAt) — carried in full, with `key` standing in for `code`
by the SRS A3 note, and `ownerModuleCode` added above the set because the namespacing policy
requires it.
FIELDS
| DBF | property | column | type (postgresql16) | null | read-only | constraint | label ar / en |
|---|---|---|---|---|---|---|---|
| DBF-MDL-001 | lookupTypePk | lookup_type_pk | BIGINT | NOT NULL | Yes | PK_MDL_LOOKUP_TYPE | معرّف نوع اللوكب / LookupType id |
| DBF-MDL-002 | key | key | VARCHAR(50) | NOT NULL | create-only | UQ_MDL_LOOKUP_TYPE_KEY | المفتاح / Key |
| DBF-MDL-003 | ownerModuleCode | owner_module_code | VARCHAR(10) | NOT NULL | create-only | — (XM-MDL-001, application read) | رمز الوحدة المالكة / Owner module code |
| DBF-MDL-004 | nameAr | name_ar | VARCHAR(200) | NOT NULL | No | — (IDX_MDL_LOOKUP_TYPE_NAME_AR) | الاسم (عربي) / Name (Arabic) |
| DBF-MDL-005 | nameEn | name_en | VARCHAR(200) | NOT NULL | No | — (IDX_MDL_LOOKUP_TYPE_NAME_EN) | الاسم (إنجليزي) / Name (English) |
| DBF-MDL-006 | isActiveFl | is_active_fl | BOOLEAN | NOT NULL | Yes | — (DEFAULT TRUE) | نشط / Active |
| DBF-MDL-007 | createdBy | created_by | VARCHAR(100) | NOT NULL | Yes | — | أنشأه / Created by |
| DBF-MDL-008 | createdAt | created_at | TIMESTAMPTZ | NOT NULL | Yes | — | تاريخ الإنشاء / Created at |
| DBF-MDL-009 | updatedBy | updated_by | VARCHAR(100) | NULL | Yes | — | عدّله / Updated by |
| DBF-MDL-010 | updatedAt | updated_at | TIMESTAMPTZ | NULL | Yes | — | تاريخ التعديل / Updated at |

DTO MEMBERSHIP
- create-request `LookupTypeCreateRequest {key, ownerModuleCode, nameAr, nameEn}` — excludes the
  PK, the active flag (the platform sets it TRUE, REQ-MDL-001) and the four audit fields.
- update-request `LookupTypeUpdateRequest {nameAr, nameEn}` — excludes the PK (a path
  parameter), `key` (RULE-MDL-003 — absent from the schema, so a submitted key is ignored, not
  rejected), `ownerModuleCode` (create-only by the same reading), the active flag (only the
  deactivate endpoint moves it) and the audit fields.
- response `LookupTypeResponse {lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl,
  createdBy, createdAt, updatedBy, updatedAt}` — the PK and the business key are always present;
  there is no business code to add.

LOOKUP FIELDS  none — no property of this entity stores a lookup code. `ownerModuleCode` is a
module code validated against SEC's registry (XM-MDL-001), not a value of any coded list, and
the module owns no key of its own (SRS A6).

DOMAIN RULES
- **RULE-MDL-001** — الوحدة المالكة مسجَّلة في الأمان / The owner module is registered in SEC ·
  scope CREATE · trigger: on create · statement: "The system shall reject a lookup type
  registration whose owner module code has no ModuleRegistry row in SEC." · message ar:
  «الوحدة المالكة غير مسجّلة في وحدة الأمان» · en: "The owning module is not registered in the
  Security module" · Data source: ENT-MDL-001.ownerModuleCode · DB enforcement: none — no
  cross-module foreign key exists in this platform; the check is an application read carried by
  the XM-MDL-001 contract (QR-MDL-012) · owner layer: service. On create only: a module
  unregistered later does not invalidate the types it already owns (the rule's own Test-Hint).
- **RULE-MDL-003** — مفتاح النوع لا يتغيّر بعد الإنشاء / A type's key is immutable after creation ·
  scope UPDATE · trigger: on update · statement: "The system shall prevent editing a lookup
  type's key after creation." · message ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» · en:
  "A lookup type's key cannot be changed after creation" · Data source: ENT-MDL-001.key · DB
  enforcement: none and none needed — the key is absent from the update DTO and from QR-MDL-003's
  statement, so no code path can write it · owner layer: controller/service (DTO shape). No
  error-catalog row: nothing can raise one.
- **RULE-MDL-004** — النوع المعطَّل يحجب قيمه عن المستهلك / An inactive type hides its values from
  consumers · scope ALL · trigger: on evaluate, at every consumer read · statement: "While a
  lookup type is inactive, the system shall exclude its values from consumer reads." · message
  ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive" · Data source:
  ENT-MDL-001.isActiveFl, ENT-MDL-002.isActiveFl · DB enforcement: none — a read-time filter
  carried in QR-MDL-015 and in QR-MDL-011's own predicate, never a stored change: deactivating a
  type writes nothing to its values (AC-MDL-004) · owner layer: repository predicate + service.

STATE MACHINE  state column `is_active_fl` (DBF-MDL-006) · values TRUE (نشط / active), FALSE
(معطَّل / inactive) · initial TRUE at creation (REQ-MDL-001) · transition TRUE → FALSE, trigger
`deactivate`, actor the registrar · terminal FALSE — there is no reverse transition in v1
(reactivation is out of scope, SRS A2, and no endpoint offers it) · no invalid-transition rule
is needed: the second deactivation of an already-inactive type affects no row and reports the
same success (QR-MDL-004). The row is never removed.

CROSS-MODULE  XM-MDL-001 — SOFT-READ · local column `owner_module_code` (DBF-MDL-003) ·
target SEC · SEC_MODULE_REG (ENT-SEC-004) · status ACTIVE · no foreign key and no constraint.

OPERATIONS  VIEW · CREATE · UPDATE · DELETE — the four actions SCR-REQ-MDL-001 §B4 grants on the
page code `MDL_LOOKUPS`, where `DELETE` is the soft deactivation of REQ-MDL-004. Every one of
them is answered by an `API-*` block in PHASE 3 that names this entity; the read-only browse of
SCR-REQ-MDL-002 is the same `VIEW` action on a second page code.

REPOSITORY OPS → QR-MDL-016 (NATIVE, key allocation) · QR-MDL-001 (FIND_BY_CRITERIA) · QR-MDL-002
(SAVE) · QR-MDL-003 (UPDATE) · QR-MDL-004 (UPDATE, deactivate) · QR-MDL-010 (FIND_BY_CRITERIA,
grouped) · QR-MDL-013 (EXISTS, key uniqueness) · QR-MDL-015 (FIND_ONE, by key).

#### ENT-MDL-002 — قيمة اللوكب / LookupValue      kind: lookup
BINDINGS   table `MDL_LOOKUP_VALUE` · PK `lookup_value_pk` (DBF-MDL-011) · PK generation
`sequence` → sequence `SEQ_MDL_LOOKUP_VALUE` (db-script BLOCK 1, one per table) · db-script
version v1. The PK column is a plain BIGINT NOT NULL with no identity clause and no default.
BUSINESS CODE  none — the value is identified by `code` (DBF-MDL-013) within its type, unique
through `UQ_MDL_LOOKUP_VALUE_TYPE_CODE`, written by the user and never generated. The same code
under another type is legal, so it is not a platform-wide business number and no numbering
source exists for it.
DEFAULT FIELDS (profile.conventions.entity_defaults.lookup: code, nameAr, nameEn, sortOrder,
isActiveFl) — carried in full, plus the four audit fields, which the `lookup` default set omits
and the SRS A3 note deliberately adds here: these rows are edited by a user through a screen,
not seeded once, so the audit trail is owed.
FIELDS
| DBF | property | column | type (postgresql16) | null | read-only | constraint | label ar / en |
|---|---|---|---|---|---|---|---|
| DBF-MDL-011 | lookupValuePk | lookup_value_pk | BIGINT | NOT NULL | Yes | PK_MDL_LOOKUP_VALUE | معرّف القيمة / LookupValue id |
| DBF-MDL-012 | lookupTypeId | lookup_type_id | BIGINT | NOT NULL | create-only | FK_LOOKUP_VALUE_TYPE | نوع اللوكب / Lookup type |
| DBF-MDL-013 | code | code | VARCHAR(50) | NOT NULL | create-only | UQ_MDL_LOOKUP_VALUE_TYPE_CODE | الرمز / Code |
| DBF-MDL-014 | nameAr | name_ar | VARCHAR(200) | NOT NULL | No | — | الاسم (عربي) / Name (Arabic) |
| DBF-MDL-015 | nameEn | name_en | VARCHAR(200) | NOT NULL | No | — | الاسم (إنجليزي) / Name (English) |
| DBF-MDL-016 | sortOrder | sort_order | INTEGER | NOT NULL | No | — (DEFAULT 0, IDX_MDL_LOOKUP_VALUE_TYPE_SORT) | الترتيب / Sort order |
| DBF-MDL-017 | isActiveFl | is_active_fl | BOOLEAN | NOT NULL | Yes | — (DEFAULT TRUE) | نشط / Active |
| DBF-MDL-018 | createdBy | created_by | VARCHAR(100) | NOT NULL | Yes | — | أنشأها / Created by |
| DBF-MDL-019 | createdAt | created_at | TIMESTAMPTZ | NOT NULL | Yes | — | تاريخ الإنشاء / Created at |
| DBF-MDL-020 | updatedBy | updated_by | VARCHAR(100) | NULL | Yes | — | عدّلها / Updated by |
| DBF-MDL-021 | updatedAt | updated_at | TIMESTAMPTZ | NULL | Yes | — | تاريخ التعديل / Updated at |

DTO MEMBERSHIP
- create-request `LookupValueCreateRequest {code, nameAr, nameEn, sortOrder}` — the parent type
  arrives as the path parameter, never in the body (the type is chosen, not retyped); excludes
  the PK, the active flag (set TRUE by the platform, REQ-MDL-006) and the four audit fields.
- update-request `LookupValueUpdateRequest {nameAr, nameEn, sortOrder}` — excludes the PK, the
  parent type, `code` (the contract consumers stored), the active flag and the audit fields.
- reorder-request `LookupValueReorderRequest {orderedValueIds}` — ids only; the rank is the
  position in the submitted list, never a number the caller supplies (REQ-MDL-010).
- response `LookupValueResponse {lookupValuePk, lookupTypeId, code, nameAr, nameEn, sortOrder,
  isActiveFl, createdBy, createdAt, updatedBy, updatedAt}`. The consumer read returns the
  narrowed projection of QR-MDL-011, not this whole shape.

LOOKUP FIELDS  none — this entity is a lookup value; it does not reference one.

DOMAIN RULES
- **RULE-MDL-002** — لا رمز مكرَّر ضمن النوع الواحد / No duplicate code within one type · scope
  CREATE|UPDATE · trigger: on create / on update · statement: "The system shall reject a lookup
  value whose code already exists under the same lookup type." · message ar: «هذا الرمز مستخدم
  بالفعل ضمن هذا النوع» · en: "This code is already used within this type" · Data source:
  ENT-MDL-002.lookupTypeId, ENT-MDL-002.code · DB enforcement:
  `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` (lookup_type_id, code) — structural, and the real guard —
  with the service pre-check QR-MDL-014 producing the friendly message before the database
  would · owner layer: repository constraint + service. On the update path the rule cannot be
  violated at all: `code` is absent from the update DTO, so the row keeps the code it was
  created with; the trigger is listed as the SRS states it and the endpoint records why the
  update half raises nothing.
- **RULE-MDL-004** applies to this entity as well, as the SRS scopes it to both: an inactive
  type's values are excluded from consumer reads without any flag on the values changing. Its
  full text, message and Data source are stated once, in ENT-MDL-001 above; it is not restated
  here as a second rule.

STATE MACHINE  state column `is_active_fl` (DBF-MDL-017) · values TRUE (نشط / active), FALSE
(معطَّل / inactive) · initial TRUE at creation (REQ-MDL-006) · transition TRUE → FALSE, trigger
`deactivate`, actor the lookup manager · terminal FALSE — no reactivation in v1 (SRS A2) · the
deactivated value stays visible on the management screen and disappears only from the consumer
read (AC-MDL-009). The row is never removed.

CROSS-MODULE  none — this entity is internally dependent: it reaches its type through
`lookup_type_id` (DBF-MDL-012), an intra-module foreign key, and touches no other module.

OPERATIONS  VIEW · CREATE · UPDATE · DELETE — the same four actions of the page code
`MDL_LOOKUPS`, exercised on the detail half of the composite screen, where `DELETE` is the soft
deactivation of REQ-MDL-009 and the reorder of REQ-MDL-010 is an `UPDATE` of the rank column,
not a fifth action: SRS §B4 grants no other.

REPOSITORY OPS → QR-MDL-017 (NATIVE, key allocation) · QR-MDL-005 (FIND_BY_CRITERIA) · QR-MDL-006
(SAVE) · QR-MDL-007 (UPDATE) · QR-MDL-008 (UPDATE, deactivate) · QR-MDL-009 (UPDATE, batch
reorder) · QR-MDL-011 (FIND_BY_CRITERIA, the consumer read) · QR-MDL-014 (EXISTS, code uniqueness
within the type).
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013 -->
## PHASE 3 — SVC-API

API count is 11, at or above the split threshold of 8, so this phase is split before its first
block into the three groups the profile names — CRUD, SEARCH and INT — and every atom sits
inside one of them. The eleven are exactly the operations SRS §B5 names across both screen
requirements: none is added and none is dropped.

Three reads are `POST …/search` rather than `GET`, because their filter sets are composite and
that is the platform's search convention; SRS §B5 states the same three that way. ADR-MDL-002
records the divergence this stage now closes — the previous revision of this plan carried the
`GET` forms it had predicted before implementation, and it was the plan, not the requirement,
that was stale.

<!-- SUB:SVC-API-SEARCH:START traces=REQ-MDL-001,REQ-MDL-003,REQ-MDL-005,REQ-MDL-013 -->
### SUB SVC-API-SEARCH — the reads a screen performs

<!-- API:API-MDL-001:START traces=REQ-MDL-001,REQ-MDL-003,REQ-MDL-013,DBF-MDL-002,DBF-MDL-003,DBF-MDL-004,DBF-MDL-005,DBF-MDL-006 -->
### API-MDL-001 — search lookup types
Entity       : ENT-MDL-001 · operation VIEW
Endpoint     : POST /api/v1/mdl/lookup-types/search   verb: POST
Layers       : controller → `LookupTypeController.search` ; service → `LookupTypeService.search`
Request      : body `LookupTypeSearchRequest` — a composite filter set over the properties PHASE 1 declares for this screen: key (DBF-MDL-002, String, LIKE) · ownerModuleCode (DBF-MDL-003, String, EXACT) · name (DBF-MDL-004 and DBF-MDL-005 together, String, LIKE over both languages at once) · isActiveFl (DBF-MDL-006, Boolean, EXACT) · page, size (default 20, maximum 200). No path param. The verb carries a body and mutates nothing.
Response     : 200 · `Page<LookupTypeResponse>` {lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} · paginated (`Page<T>`) · envelope `ApiResponse<T>`
Validations  : none of this module's RULEs fires on a read — RULE-MDL-004 is scoped to the consumer read (API-MDL-011), not to the management screen, which shows inactive rows by requirement (AC-MDL-005). The requested sort field is confined to the set PHASE 1 declares.
Errors       : VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD). An empty page is success, never a not-found row.
Orchestration : authorise → bind the declared filters → load (QR-MDL-001, table MDL_LOOKUP_TYPE) → map into the page envelope. This endpoint writes no column of any kind.
Repository   : QR-MDL-001 · join NONE · transaction READ_ONLY
Concurrency  : NONE — this endpoint neither allocates a unique value nor reads-then-writes.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_VIEW` (the gateway action) — verified before processing
Localization : messages in ar + en; every row carries both name fields, so the caller renders either language without a second call
<!-- API:API-MDL-001:END -->

<!-- API:API-MDL-005:START traces=REQ-MDL-005,DBF-MDL-012,DBF-MDL-013,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016,DBF-MDL-017 -->
### API-MDL-005 — search the values of a type
Entity       : ENT-MDL-002 · operation VIEW
Endpoint     : POST /api/v1/mdl/lookup-types/values/search   verb: POST
Layers       : controller → `LookupValueController.search` ; service → `LookupValueService.search`
Request      : body `LookupValueSearchRequest` — lookupTypeId (DBF-MDL-012, Long, EXACT, mandatory: the detail is confined to the selected type, REQ-MDL-005) · code (DBF-MDL-013, String, LIKE). No paging: the result is bounded by one type and returned whole, ordered.
Response     : 200 · list of `LookupValueResponse` {lookupValuePk, lookupTypeId, code, nameAr, nameEn, sortOrder, isActiveFl, createdBy, createdAt, updatedBy, updatedAt}, ordered by sortOrder then code · not paginated · envelope `ApiResponse<T>`
Validations  : none — the management detail shows active and inactive values alike (AC-MDL-005); RULE-MDL-004 governs the consumer read only.
Errors       : MDL-404-TYPE (404, PLATFORM-STD — the filter names a type id no row carries) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD). A type with no values at all is an empty list and success.
Orchestration : authorise → bind the mandatory type filter → load (QR-MDL-005, table MDL_LOOKUP_VALUE) → map. This endpoint writes no column of any kind.
Repository   : QR-MDL-005 · join NONE · transaction READ_ONLY
Concurrency  : NONE — this endpoint neither allocates a unique value nor reads-then-writes.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_VIEW` (the gateway action) — verified before processing
Localization : messages in ar + en; both label fields are returned on every row
<!-- API:API-MDL-005:END -->

<!-- API:API-MDL-010:START traces=REQ-MDL-013,DBF-MDL-002,DBF-MDL-003,DBF-MDL-004,DBF-MDL-005,DBF-MDL-006 -->
### API-MDL-010 — browse the type registry by owner
Entity       : ENT-MDL-001 · operation VIEW
Endpoint     : POST /api/v1/mdl/lookup-types/by-owner/search   verb: POST
Layers       : controller → `LookupTypeController.browseByOwner` ; service → `LookupTypeService.browseByOwner`
Request      : body `LookupTypeByOwnerSearchRequest` — ownerModuleCode (DBF-MDL-003, String, EXACT — the group heading itself) · key (DBF-MDL-002, String, LIKE). No paging and no state filter: REQ-MDL-013 admits active types only, so `is_active_fl` (DBF-MDL-006) is fixed TRUE in the query and is not a parameter a caller can widen.
Response     : 200 · list of owner groups, each carrying its ownerModuleCode and its types {key, nameAr, nameEn, ownerModuleCode} · not paginated · envelope `ApiResponse<T>`
Validations  : none of this module's RULEs fires on this read. The grouping is assembled from the ordered result, not from a second query and not from a join to SEC.
Errors       : VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD). No registered type at all is an empty list and success.
Orchestration : authorise → bind the declared filters → load ordered by owner then key (QR-MDL-010, table MDL_LOOKUP_TYPE) → assemble the groups in the service layer → map. This endpoint writes no column of any kind.
Repository   : QR-MDL-010 · join NONE (single-table; the grouping is a service-layer fold) · transaction READ_ONLY
Concurrency  : NONE — this endpoint neither allocates a unique value nor reads-then-writes.
Security     : screen MDL_TYPE_REGISTRY · permission `PERM_MDL_TYPE_REGISTRY_VIEW` (the gateway action of its own page) — verified before processing. This is the one endpoint of the module served by the second page code.
Localization : messages in ar + en; each type row carries both names
<!-- API:API-MDL-010:END -->
<!-- SUB:SVC-API-SEARCH:END -->

<!-- SUB:SVC-API-CRUD:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010 -->
### SUB SVC-API-CRUD — the writes of the generic lookups screen

<!-- API:API-MDL-002:START traces=REQ-MDL-001,REQ-MDL-002,DBF-MDL-002,DBF-MDL-003,DBF-MDL-004,DBF-MDL-005,DBF-MDL-006 -->
### API-MDL-002 — create lookup type
Entity       : ENT-MDL-001 · operation CREATE
Endpoint     : POST /api/v1/mdl/lookup-types   verb: POST
Layers       : controller → `LookupTypeController.create` ; service → `LookupTypeService.create`
Request      : body `LookupTypeCreateRequest` — key (DBF-MDL-002, String, required, maximum 50, UQ_MDL_LOOKUP_TYPE_KEY) · ownerModuleCode (DBF-MDL-003, String, required, maximum 10, validated through XM-MDL-001) · nameAr (DBF-MDL-004, String, required, maximum 200) · nameEn (DBF-MDL-005, String, required, maximum 200). No path param, no query param. Excluded system fields: the key column of the table, the active flag and the four audit fields — a value supplied for any of them is ignored.
Response     : 201 · `LookupTypeResponse` {lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-001 "The system shall reject a lookup type registration whose owner module code has no ModuleRegistry row in SEC." (trigger: on create · ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» · en: "The owning module is not registered in the Security module") — checked through XM-MDL-001 (QR-MDL-012), an injected in-process call, never an HTTP request. Key uniqueness is structural (REQ-MDL-001): QR-MDL-013 produces the message, `UQ_MDL_LOOKUP_TYPE_KEY` produces the guarantee.
Errors       : MDL-409-MODULE-NOT-REGISTERED (409, RULE-MDL-001) · MDL-409-TYPE-DUP (409, PLATFORM-STD) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → validate (RULE-MDL-001) → integrate: ask SEC through XM-MDL-001 whether the owner module is registered (QR-MDL-012) → check the key is free (QR-MDL-013) → set the fields the request does not carry: the active flag is_active_fl (DBF-MDL-006) to TRUE (REQ-MDL-001), the audit columns created_by (DBF-MDL-007) and created_at (DBF-MDL-008) from the principal and the clock → allocate the key (QR-MDL-016, from SEQ_MDL_LOOKUP_TYPE) → persist (QR-MDL-002, table MDL_LOOKUP_TYPE)
Repository   : QR-MDL-016, QR-MDL-013, QR-MDL-012, QR-MDL-002 · join NONE (QR-MDL-012 is a cross-module call, not a SQL join) · transaction READ_WRITE
Concurrency  : two simultaneous registrations of the same key must not both be stored. The guard is `UQ_MDL_LOOKUP_TYPE_KEY`: exactly one INSERT commits and the other's integrity violation is translated to MDL-409-TYPE-DUP. QR-MDL-013 is not the guard — both requests pass it, which is precisely why the constraint is named here. The primary key is allocated atomically by the sequence (QR-MDL-016), so no two creates can collide on it either. The SEC check (QR-MDL-012) guards nothing concurrent by design: its answer may go stale, and RULE-MDL-001's Test-Hint accepts that.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_CREATE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before the method body runs
Localization : messages in ar + en (see Validations and the ERROR CATALOG); both name fields are required, so no type can be registered in one language only
<!-- API:API-MDL-002:END -->

<!-- API:API-MDL-003:START traces=REQ-MDL-003,DBF-MDL-004,DBF-MDL-005,DBF-MDL-009,DBF-MDL-010 -->
### API-MDL-003 — update lookup type
Entity       : ENT-MDL-001 · operation UPDATE
Endpoint     : PUT /api/v1/mdl/lookup-types/{id}   verb: PUT
Layers       : controller → `LookupTypeController.update` ; service → `LookupTypeService.update`
Request      : path param `{id}` → DBF-MDL-001 (Long, required) · body `LookupTypeUpdateRequest` — nameAr (DBF-MDL-004, String, required, maximum 200) · nameEn (DBF-MDL-005, String, required, maximum 200). Excluded system fields: the business key (RULE-MDL-003), the owner module code, the active flag and the four audit fields.
Response     : 200 · `LookupTypeResponse` {lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-003 "The system shall prevent editing a lookup type's key after creation." (trigger: on update · ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» · en: "A lookup type's key cannot be changed after creation") — enforced by DTO shape and by QR-MDL-003's statement, which never names the key column; a submitted key is ignored, not rejected, so this rule raises no catalog row and no branch exists to test.
Errors       : MDL-404-TYPE (404, PLATFORM-STD) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → load by id → an absent row raises MDL-404-TYPE → update the two names and write the columns the request does not carry: updated_by (DBF-MDL-009) and updated_at (DBF-MDL-010) from the principal and the clock (QR-MDL-003, table MDL_LOOKUP_TYPE) → map. The key, the owner module code and the active flag are not in the statement and are never written here.
Repository   : QR-MDL-003 · join NONE · transaction READ_WRITE
Concurrency  : NONE by the stated reading, not by omission: the endpoint allocates no unique value, and every column it writes comes from the request rather than from a prior read, so two simultaneous renames end with one submission stored whole. No version column exists in the db-script, so no optimistic-lock rejection is specified in v1; last writer wins, and both callers see their own submission echoed by the response they receive.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing
Localization : messages in ar + en; both names are required, so a rename cannot leave one language behind
<!-- API:API-MDL-003:END -->

<!-- API:API-MDL-004:START traces=REQ-MDL-004,DBF-MDL-006,DBF-MDL-009,DBF-MDL-010 -->
### API-MDL-004 — deactivate lookup type
Entity       : ENT-MDL-001 · operation DELETE
Endpoint     : DELETE /api/v1/mdl/lookup-types/{id}   verb: DELETE
Layers       : controller → `LookupTypeController.deactivate` ; service → `LookupTypeService.deactivate`
Request      : path param `{id}` → DBF-MDL-001 (Long, required). No query params and no body.
Response     : 200 · `LookupTypeResponse` {lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} with the flag now false · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-004 "While a lookup type is inactive, the system shall exclude its values from consumer reads." (trigger: on evaluate · ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive") — this endpoint is what makes the rule's condition true; the rule itself is enforced at read time by API-MDL-011, and no value of the type is written here (AC-MDL-004).
Errors       : MDL-404-TYPE (404, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → load by id → an absent row raises MDL-404-TYPE → flip the flag the request does not carry: is_active_fl (DBF-MDL-006) to FALSE, with updated_by (DBF-MDL-009) and updated_at (DBF-MDL-010) written from the principal and the clock, in one conditional statement (QR-MDL-004, table MDL_LOOKUP_TYPE) → map. The verb is DELETE and the effect is `soft` (profile.stack.db.delete_semantics): no row is removed here or anywhere in this module, and the type's values keep their own flags untouched.
Repository   : QR-MDL-004 · join NONE · transaction READ_WRITE
Concurrency  : two simultaneous deactivations of the same type must not produce two different outcomes. The state is carried in QR-MDL-004's own predicate, so only one statement affects a row; the other affects none, and that is reported as the same success, because the end state REQ-MDL-004 asks for holds either way and the SRS names no "already deactivated" error. Nothing is read, decided on and then written: the target state is a constant.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_DELETE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing
Localization : messages in ar + en
<!-- API:API-MDL-004:END -->

<!-- API:API-MDL-006:START traces=REQ-MDL-006,REQ-MDL-007,DBF-MDL-012,DBF-MDL-013,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016,DBF-MDL-017 -->
### API-MDL-006 — create lookup value
Entity       : ENT-MDL-002 · operation CREATE
Endpoint     : POST /api/v1/mdl/lookup-types/{id}/values   verb: POST
Layers       : controller → `LookupValueController.create` ; service → `LookupValueService.create`
Request      : path param `{id}` → lookup_type_id (DBF-MDL-012, Long, required, FK_LOOKUP_VALUE_TYPE) — the parent type is selected, never retyped · body `LookupValueCreateRequest` — code (DBF-MDL-013, String, required, maximum 50, UQ_MDL_LOOKUP_VALUE_TYPE_CODE) · nameAr (DBF-MDL-014, String, required, maximum 200) · nameEn (DBF-MDL-015, String, required, maximum 200) · sortOrder (DBF-MDL-016, Integer, required, 0 when the caller sends none). Excluded system fields: the value's own key, the active flag and the four audit fields.
Response     : 201 · `LookupValueResponse` {lookupValuePk, lookupTypeId, code, nameAr, nameEn, sortOrder, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-002 "The system shall reject a lookup value whose code already exists under the same lookup type." (trigger: on create · ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» · en: "This code is already used within this type") — QR-MDL-014 produces the message and `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` produces the guarantee. The same code under a different type is accepted, by the rule's own Test-Hint.
Errors       : MDL-409-VALUE-DUP (409, RULE-MDL-002) · MDL-404-TYPE (404, PLATFORM-STD — the path names a type that does not exist) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → confirm the parent type exists → validate (RULE-MDL-002, QR-MDL-014) → set the fields the request does not carry: the active flag is_active_fl (DBF-MDL-017) to TRUE (REQ-MDL-006), the audit columns created_by (DBF-MDL-018) and created_at (DBF-MDL-019) from the principal and the clock → allocate the key (QR-MDL-017, from SEQ_MDL_LOOKUP_VALUE) → persist with the parent id from the path (QR-MDL-006, table MDL_LOOKUP_VALUE)
Repository   : QR-MDL-017, QR-MDL-014, QR-MDL-006 · join NONE · transaction READ_WRITE
Concurrency  : two simultaneous creates of the same code under the same type must not both be stored. The guard is `UQ_MDL_LOOKUP_VALUE_TYPE_CODE`, which admits one INSERT and turns the other into MDL-409-VALUE-DUP; QR-MDL-014 is the friendly message and not the guard, since both requests pass it. The primary key comes from the sequence (QR-MDL-017) and cannot collide.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_CREATE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing
Localization : messages in ar + en; both labels are required, so no value can be created in one language only
<!-- API:API-MDL-006:END -->

<!-- API:API-MDL-007:START traces=REQ-MDL-008,DBF-MDL-014,DBF-MDL-015,DBF-MDL-016,DBF-MDL-020,DBF-MDL-021 -->
### API-MDL-007 — update lookup value
Entity       : ENT-MDL-002 · operation UPDATE
Endpoint     : PUT /api/v1/mdl/lookup-values/{id}   verb: PUT
Layers       : controller → `LookupValueController.update` ; service → `LookupValueService.update`
Request      : path param `{id}` → DBF-MDL-011 (Long, required) · body `LookupValueUpdateRequest` — nameAr (DBF-MDL-014, String, required, maximum 200) · nameEn (DBF-MDL-015, String, required, maximum 200) · sortOrder (DBF-MDL-016, Integer, required). Excluded system fields: the value's key, its parent type, its code, the active flag and the four audit fields.
Response     : 200 · `LookupValueResponse` {lookupValuePk, lookupTypeId, code, nameAr, nameEn, sortOrder, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-002 "The system shall reject a lookup value whose code already exists under the same lookup type." (trigger: on update · ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» · en: "This code is already used within this type") — the SRS scopes the rule to the update path too, and this endpoint closes it by shape: `code` is absent from the request and from QR-MDL-007's statement, so an update cannot create a duplicate and raises no row of its own. The catalog row for MDL-409-VALUE-DUP names this endpoint so the mapping is complete in both directions, and the only path that can emit it here is a database integrity violation that no request can provoke.
Errors       : MDL-404-VALUE (404, PLATFORM-STD) · MDL-409-VALUE-DUP (409, RULE-MDL-002 — unreachable through the request shape, listed because the rule names this trigger) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → load by id → an absent row raises MDL-404-VALUE → update the two labels and the rank, writing the columns the request does not carry: updated_by (DBF-MDL-020) and updated_at (DBF-MDL-021) from the principal and the clock (QR-MDL-007, table MDL_LOOKUP_VALUE) → map. The code, the parent type and the active flag are not in the statement.
Repository   : QR-MDL-007 · join NONE · transaction READ_WRITE
Concurrency  : NONE by the stated reading: no unique value is allocated and every written column comes from the request, not from a prior read, so concurrent edits end with one submission stored whole rather than a mixture. A concurrent reorder writes the same rank column (QR-MDL-009) and the later transaction wins; because `sort_order` carries no uniqueness constraint and every ordered read breaks ties by code, the outcome is a display order, never an inconsistency.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing
Localization : messages in ar + en
<!-- API:API-MDL-007:END -->

<!-- API:API-MDL-008:START traces=REQ-MDL-009,DBF-MDL-017,DBF-MDL-020,DBF-MDL-021 -->
### API-MDL-008 — deactivate lookup value
Entity       : ENT-MDL-002 · operation DELETE
Endpoint     : DELETE /api/v1/mdl/lookup-values/{id}   verb: DELETE
Layers       : controller → `LookupValueController.deactivate` ; service → `LookupValueService.deactivate`
Request      : path param `{id}` → DBF-MDL-011 (Long, required). No query params and no body.
Response     : 200 · `LookupValueResponse` {lookupValuePk, lookupTypeId, code, nameAr, nameEn, sortOrder, isActiveFl, createdBy, createdAt, updatedBy, updatedAt} with the flag now false · not paginated · envelope `ApiResponse<T>`
Validations  : none of this module's RULEs refuses a deactivation — SRS §B3 lists no rule for the act, and the deactivated value stays visible on the management screen while disappearing from the consumer read (AC-MDL-009).
Errors       : MDL-404-VALUE (404, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → load by id → an absent row raises MDL-404-VALUE → flip the flag the request does not carry: is_active_fl (DBF-MDL-017) to FALSE, with updated_by (DBF-MDL-020) and updated_at (DBF-MDL-021) written from the principal and the clock, in one conditional statement (QR-MDL-008, table MDL_LOOKUP_VALUE) → map. The effect is `soft`: the row survives, and every consumer row that stored this code keeps its meaning.
Repository   : QR-MDL-008 · join NONE · transaction READ_WRITE
Concurrency  : two simultaneous deactivations of the same value cannot produce two outcomes: the state is in QR-MDL-008's predicate, so one statement affects a row and the other affects none, and both report the same success — the end state REQ-MDL-009 asks for holds either way, and the SRS names no second outcome to distinguish.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_DELETE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing
Localization : messages in ar + en
<!-- API:API-MDL-008:END -->

<!-- API:API-MDL-009:START traces=REQ-MDL-010,DBF-MDL-016,DBF-MDL-020,DBF-MDL-021 -->
### API-MDL-009 — reorder a type's values
Entity       : ENT-MDL-002 · operation UPDATE
Endpoint     : PATCH /api/v1/mdl/lookup-types/{id}/values/reorder   verb: PATCH
Layers       : controller → `LookupValueController.reorder` ; service → `LookupValueService.reorder`
Request      : path param `{id}` → lookup_type_id (DBF-MDL-012, Long, required) · body `LookupValueReorderRequest` — orderedValueIds (list of DBF-MDL-011, required, the values in their new display order). No rank number is accepted from the caller: the rank is the position in the list (REQ-MDL-010).
Response     : 200 · list of `LookupValueResponse`, in the stored order · not paginated · envelope `ApiResponse<T>`
Validations  : none of this module's RULEs applies to a reorder; the one precondition is membership — every submitted id must belong to the type in the path, which QR-MDL-009 enforces in the statement rather than in a branch before it.
Errors       : MDL-400-REORDER-MISMATCH (400, PLATFORM-STD) · MDL-404-TYPE (404, PLATFORM-STD) · VALIDATION_ERROR (400, PLATFORM-STD) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → confirm the type exists → assign each submitted id its position as the new rank and persist all of them in one transaction, writing sort_order (DBF-MDL-016) and the columns the request does not carry, updated_by (DBF-MDL-020) and updated_at (DBF-MDL-021), on every row touched (QR-MDL-009, table MDL_LOOKUP_VALUE) → a statement that affects no row means the id does not belong to the type: raise MDL-400-REORDER-MISMATCH and roll the whole batch back → return the reordered list.
Repository   : QR-MDL-009 · join NONE · transaction READ_WRITE
Concurrency  : two simultaneous reorders of the same type are both allowed and neither is refused: each writes an explicit rank per id inside one transaction, nothing is read and then written back, and the later transaction's order is the one stored whole — a half-applied order is impossible because the batch is one transaction. A value created while a reorder is in flight is not in the submitted list, keeps its own rank and may tie with another; the tie is resolved by code in every ordered read, so it is a display detail, not a lost update.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_UPDATE`, gated by `PERM_MDL_LOOKUPS_VIEW` — both verified before processing. The reorder is an UPDATE of the rank column and SRS §B4 grants no separate action for it.
Localization : messages in ar + en
<!-- API:API-MDL-009:END -->
<!-- SUB:SVC-API-CRUD:END -->

<!-- SUB:SVC-API-INT:START traces=REQ-MDL-011,REQ-MDL-012 -->
### SUB SVC-API-INT — the surface other modules consume

<!-- API:API-MDL-011:START traces=REQ-MDL-011,REQ-MDL-012,DBF-MDL-002,DBF-MDL-006,DBF-MDL-013,DBF-MDL-016,DBF-MDL-017 -->
### API-MDL-011 — read active values by key (consumer)
Entity       : ENT-MDL-001, ENT-MDL-002 · operation VIEW
Endpoint     : GET /api/v1/mdl/lookups   verb: GET
Layers       : controller → `LookupConsumerController.readByKey` ; service → `LookupConsumerService.readByKey`
Request      : query param `type` → the lookup type key (DBF-MDL-002, String, required). No body and no paging: a coded list is returned whole, ordered.
Response     : 200 · list of `LookupValueResponse` narrowed to what a consumer needs — code (DBF-MDL-013), nameAr, nameEn, sortOrder (DBF-MDL-016) — ordered by sortOrder then code · not paginated · envelope `ApiResponse<T>`
Validations  : RULE-MDL-004 "While a lookup type is inactive, the system shall exclude its values from consumer reads." (trigger: on evaluate, at every consumer read · ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive") — enforced as a predicate over is_active_fl on both tables (DBF-MDL-006 and DBF-MDL-017), never as a branch after the read, and never as a write. An unknown key and an inactive type are both answered MDL-404-TYPE-KEY, because in both cases the platform holds no usable list under that key (REQ-MDL-012, AC-MDL-012). An active type whose values are all inactive returns an empty list with success — a different answer, deliberately.
Errors       : MDL-404-TYPE-KEY (404, RULE-MDL-004) · VALIDATION_ERROR (400, PLATFORM-STD — no key supplied) · ACCESS_DENIED (403, PLATFORM-STD) · INTERNAL_ERROR (500, PLATFORM-STD)
Orchestration : authorise → resolve the type by key and confirm it is active (QR-MDL-015, table MDL_LOOKUP_TYPE) → an absent or inactive type raises MDL-404-TYPE-KEY → load the type's active values ordered by rank (QR-MDL-011, table MDL_LOOKUP_VALUE joined to its type) → map into the narrowed projection. This endpoint writes no column of any kind.
Repository   : QR-MDL-015, QR-MDL-011 · join intra-module (value → type, both owned by this module; required because the caller addresses the set by key and the value table carries no key column) · transaction READ_ONLY
Concurrency  : NONE — this endpoint neither allocates a unique value nor reads-then-writes. A deactivation committed between QR-MDL-015 and QR-MDL-011 can only shrink the returned list, which is the same outcome the next call would give; nothing is decided on and written back.
Security     : screen MDL_LOOKUPS · permission `PERM_MDL_LOOKUPS_VIEW` (the gateway action) — verified before processing. This operation has no screen surface of its own: a consuming module's backend calls it, and its authorisation is the VIEW grant on MDL_LOOKUPS held by that module's service account (SRS §B4, ADR-MDL-007), not a permission of its own.
Localization : the response carries both labels on every value, so the consumer renders either language without a second call; error messages in ar + en
<!-- API:API-MDL-011:END -->
<!-- SUB:SVC-API-INT:END -->
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START traces=REQ-MDL-011,REQ-MDL-012 -->
## PHASE 4 — DOC

The implementer publishes `backend/modules/MDL/api-docs` from the surface it actually builds:
the eleven endpoints of PHASE 3 with their verbs, paths, request and response types, their HTTP
statuses and the error codes of the ERROR CATALOG. That published file — not this plan's
contract draft — is what the frontend stage and `api-verify` read, and the previous revision of
this plan is the standing example of why: its contract table predicted three `GET` collection
reads that were built as `POST …/search`, and every downstream reader that trusted the table
rather than the publication was wrong (ADR-MDL-002).

What the api-docs must carry for this module: the two base paths `/api/v1/mdl/lookup-types` and
`/api/v1/mdl/lookup-values`, plus the consumer path `/api/v1/mdl/lookups`; the `ApiResponse<T>`
and `Page<T>` envelopes; the paging defaults (20, maximum 200) on the one paged read; the filter
envelope the three search endpoints take; and the status → code table for every catalog row.
Nothing is documented that PHASE 3 does not specify, and no endpoint of PHASE 3 is left out.

The consumer half deserves its own line in the publication, because it is the only endpoint of
this module a human never calls: `GET /api/v1/mdl/lookups` is the runtime load every other
module performs for every coded list it owns, and its response shape is the contract those
modules bind to. No other document, report, export or printable output is produced by this
module in v1.
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START traces=REQ-MDL-001,REQ-MDL-002 -->
## PHASE 5 — INT-C

One `XM-*` row, below the split threshold (1 < 5) — no SUB is opened. The db-script register
declares exactly this row, and this stage mints none: nothing written above is the first reader
of another module's data.

| XM | classification | target | interface | the thing the target publishes | status |
|---|---|---|---|---|---|
| XM-MDL-001 | SOFT-READ | SEC · ENT-SEC-004 (ModuleRegistry, table SEC_MODULE_REG) | injected in-process interface (`profile.conventions.module_interface: in_process`) | a module-registry read on SEC's own cross-module package — see the row below | ACTIVE |

<!-- XM:XM-MDL-001:START traces=REQ-MDL-001,REQ-MDL-002 -->
### XM-MDL-001 — is the owner module registered in SEC?
Target        : module SEC · entity ENT-SEC-004 (ModuleRegistry) · classification SOFT-READ
Interface     : an in-process interface published by SEC and injected into this module's
lookup-type service — `profile.conventions.module_interface` is `in_process`, so this platform
is ONE deployable: there is no HTTP client, no base path to SEC, no timeout and no network error
path, and therefore no ERROR CATALOG row describing a network failure that cannot occur. The
operation is the existence read behind `SecModuleRegistryApi` — the same shape as SEC's already
published `SecUserDirectoryApi`, injected the same way. The access is by module code alone and
carries no key: a SOFT-READ never becomes a foreign key (db-script §2.1). One thing is owed
elsewhere and is recorded rather than asserted: SEC's own P3.1 artifacts register only
`SecUserDirectoryApi` as an exposed surface, so the registry read named here is not yet written
down on SEC's side. That is SEC's artifact to correct on its own re-run; this plan does not edit
another module's, and ENT-SEC-004 — the entity this row depends on — is defined in SEC's
registry and resolves today.
Contract      : data required = whether the submitted code has a registry row in SEC; fallback
if absent = reject the registration with MDL-409-MODULE-NOT-REGISTERED (RULE-MDL-001) and store
no lookup type at all (AC-MDL-002); retry = none, and none is possible — a same-process call has
no transient failure mode to retry through; timeout = not applicable for the same reason;
idempotency = the call is read-only and naturally idempotent.
Blocks        : nothing is blocked and nothing is DEFERRED. SEC v1 is gated (pass-1 APPROVE) and
SEC_MODULE_REG exists, so this row is ACTIVE from the moment MDL v1 is created. No `DBF-*` and no
`API-*` waits on it: the column DBF-MDL-003 is stored either way, and only the create path
(API-MDL-002) consults SEC at all. Unblock condition: none outstanding.
<!-- XM:XM-MDL-001:END -->

**Inbound dependencies** — recorded for the cascade, never assigned here, because the consuming
module owns its own row: XM-FIN-001 (FIN → MDL, SOFT-READ, ACTIVE) reads this module's values
through API-MDL-011's interface for every lookup-backed code FIN owns. Every module from PRC
onward reaches the same surface the same way. The notation for a consumer not yet assigned is
`XM-INBOUND-STUB-<n>`, never TODO; none is outstanding for this version, since the one live
consumer already carries its own id.
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START traces=REQ-MDL-001,REQ-MDL-002 -->
## PHASE 6 — INT-R

One status row, for the one contract INT-C places. This phase consumes that contract and does
not redefine it.

| XM | status | workaround / mock strategy |
|---|---|---|
| XM-MDL-001 | READY — the target is gated and the interface is injected in the same deployable | none needed: nothing is DEFERRED, MOCKED, SIMULATED, BLOCKED or in EXTERNAL_WAIT |

Nothing in this module is blocked on another module's delivery. Every endpoint of PHASE 3 can be
built, deployed and called with SEC present in the same deployment, and ten of the eleven do not
touch SEC at all — only the create path asks it a question.

What a test must do about it, stated here rather than discovered later: because the call is an
injected in-process interface and not a network hop, there is no HTTP-level way to simulate "SEC
unreachable", and no test should try to. The failure that exists is a registry row that is
absent, and it is exercised by submitting an unregistered code (AC-MDL-002).
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START traces=REQ-MDL-001,REQ-MDL-005,REQ-MDL-011,REQ-MDL-013 -->
## PHASE 7 — SEC-BE

**Two screens, two page codes.** SCR-REQ-MDL-001 (اللوكبات العامة / Generic Lookups) is
composite — master and detail, search and entry on both levels are ONE screen — so it registers
exactly one page code, `MDL_LOOKUPS`, and mints four permission names from it. SCR-REQ-MDL-002
(سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner) is a read-only browse and
registers `MDL_TYPE_REGISTRY` with the gateway action alone. Two rows in `SEC_PAGES`, no more.

Every endpoint of PHASE 3 verifies its permission through the platform authorisation layer
before any processing, on the page code and the action the operation carries. No role name is
compared anywhere in this module's code: services see permissions, never roles. `VIEW` is the
gateway — a requester without it reaches no endpoint behind that page code, whatever else they
hold. The finer grain the SRS explicitly excludes is not invented here: there is no permission
per lookup type, because granting the screen is the whole of the precision v1 offers.

**Registration rows** — written at deployment into the platform registries, with the column
names taken from SEC's own script and never from here:
- one page row in `SEC_PAGES` with the page code `MDL_LOOKUPS`, its ar/en name
  (اللوكبات العامة / Generic Lookups) and its menu parent (ENT-SEC-005);
- one page row in `SEC_PAGES` with the page code `MDL_TYPE_REGISTRY`, its ar/en name
  (سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner) and the same menu parent;
- four action rows on the first page — VIEW, CREATE, UPDATE, DELETE — and one on the second
  (ENT-SEC-006), each named `PERM_<PAGE_CODE>_<ACTION>`;
- the module's own row in the registry this plan reads at create time (ENT-SEC-004): MDL
  registers itself there, exactly as it requires every other module to.

**Forbidden responses** map through `LocalizedException → {code, messageAr, messageEn}` with the
catalog row ACCESS_DENIED — the shared handler's row, not a module code, for the reason the
ERROR CATALOG states.

**Permission matrix** — one row per composite screen. A cell is marked only where the row also
names the endpoint that serves it and the permission for that action; where an action is not
granted, the cell is left as a dash rather than marked hopefully:

| Screen | ENT-* | API-* serving it | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|---|---|
| MDL_LOOKUPS | ENT-MDL-001, ENT-MDL-002 | API-MDL-001, API-MDL-005, API-MDL-011 (VIEW) · API-MDL-002, API-MDL-006 (CREATE) · API-MDL-003, API-MDL-007, API-MDL-009 (UPDATE) · API-MDL-004, API-MDL-008 (DELETE) | ✓ PERM_MDL_LOOKUPS_VIEW | ✓ PERM_MDL_LOOKUPS_CREATE | ✓ PERM_MDL_LOOKUPS_UPDATE | ✓ PERM_MDL_LOOKUPS_DELETE |
| MDL_TYPE_REGISTRY | ENT-MDL-001 | API-MDL-010 | ✓ PERM_MDL_TYPE_REGISTRY_VIEW | — | — | — |

The `DELETE` column of the first row is the soft deactivation of REQ-MDL-004 and REQ-MDL-009,
and it is marked because two endpoints really serve it; no hard-delete endpoint exists in this
module and none is implied by the word. The three dashes on the second row are the same fact
SRS §B3 and §B4 state twice: that screen creates, updates and deletes nothing.

### BOOTSTRAP DATA — MDL v1

The rows that must exist before any endpoint of this module can succeed. Every row names who
produces it, not only that it is needed.

| item | value | producer |
|---|---|---|
| LOOKUP KEY | none — SRS A6: this module owns no lookup key and consumes none, so no endpoint above reads a value of any module's lookup table. It is the store the other modules' keys live in, and each of those is registered and seeded by its owning module through API-MDL-002 and API-MDL-006 | seed source: not applicable — there is no key of this module's to seed, here or in another module's tables |
| PERMISSION | PERM_MDL_LOOKUPS_VIEW | grant target: role منسّق المنصة / Platform administrator, role مدير قوائم الوحدة المالكة / Owning-module lookup manager, and role الحساب الخدمي للوحدة المستهلكة / consuming-module service account — granted by the SEC role seed shipped with this module's deployment. This is the gateway: without the grant the name exists and authorises nobody, and every endpoint of PHASE 3 answers ACCESS_DENIED to every caller, the administrator included |
| PERMISSION | PERM_MDL_LOOKUPS_CREATE | grant target: role منسّق المنصة / Platform administrator and role مدير قوائم الوحدة المالكة / Owning-module lookup manager, granted in the same SEC role seed |
| PERMISSION | PERM_MDL_LOOKUPS_UPDATE | grant target: role منسّق المنصة / Platform administrator and role مدير قوائم الوحدة المالكة / Owning-module lookup manager, granted in the same SEC role seed |
| PERMISSION | PERM_MDL_LOOKUPS_DELETE | grant target: role منسّق المنصة / Platform administrator and role مدير قوائم الوحدة المالكة / Owning-module lookup manager, granted in the same SEC role seed |
| PERMISSION | PERM_MDL_TYPE_REGISTRY_VIEW | grant target: role منسّق المنصة / Platform administrator alone, granted in the same SEC role seed — SRS §B4 grants this screen to no other role |
| REGISTRATION | the two `SEC_PAGES` rows, the five action rows and this module's own registry row | seed source: this module's deployment seed, written into the platform registries (ENT-SEC-004, ENT-SEC-005, ENT-SEC-006). Without them the screens are absent from the menu and the module's own create path would reject its own code |

No required column of either table is seeded: every one of them is written by an endpoint of
PHASE 3 or by the platform — the keys from the two sequences, the audit columns by the
interceptor — so both tables start empty and the first create call fills them. That is the whole
reason this module has no data bootstrap of its own while carrying the platform's largest one:
every other module's lists live in these two tables, and each of those modules seeds its own.

The frontend stage references these permission names and never redeclares them.
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START traces=REQ-MDL-001,REQ-MDL-011,REQ-MDL-013 -->
## PHASE 8 — ALIGN-BE

Every row below names the mechanical check that backs it, and there are no other rows. Each mark
is the analyze report's result for that check, copied — not an independent judgement.

```
ALIGN — MDL v1
row               backing check        mark                assertion
TRACEABILITY      traces               ✓                   every PHASE/SUB/atom block carries traces=, and every API traces to its REQ and its DBF
COVERED           orphans              ✓                   every REQ is covered by ≥1 API or DBF
BINDING (§2A)     value-agreement      ✓                   every DBF names the same physical column here as the db-script declares for it
MANIFEST (§4)     count-agrees         ✓                   every total this plan states equals the rows it heads
WRITERS           required-writer      ✓                   every required column is written by an endpoint, or the row states why not
QRC (§5)          orphans              ✓                   every catalogued query is reached by ≥1 API
API (R3)          code-format          ✓                   every catalog code is an instance of the declared format and carries a status the platform can emit
RULE INPUTS       data-source          ✓                   every RULE enforced at runtime names where the data it READS comes from, or is deferred
CROSS-MODULE      registry-agree       ✓                   every registered XM is placed here, and every XM minted here is back-registered
FOREIGN IDS       xref-resolve         ✓                   every id of another module cited here is defined in that module's own registry
BOOTSTRAP DATA    bootstrap-complete   ✓                   every lookup key and every permission has a row naming who produces it
SECURITY (R7)     operation-resolves   ✓                   every declared entity operation resolves to an API, and every marked matrix cell names its API and its permission
DEMAND (SRS)      operation-resolves   ✓                   every operation an SRS screen names is built by an API, or the plan states why it is not
DECISIONS         refs-exist           ✓                   every ADR this plan cites exists on disk in analysis/decisions/MDL/
PATHS             paths-resolve        ✓                   every path the generated manifest and execution state emit resolves to something that exists
COVERAGE          (the report)         none                the clauses the analyze report lists as having examined nothing
RESULT            PASSED ✓ — 0 findings
```

**What this revision corrected, so a reader of the previous one is not misled.** Four statements
in the gated revision of this plan described a system that was not built, and none of them was a
new decision — each was the plan lagging behind an input it had itself been derived from:

- **PK generation.** Both BINDINGS lines read `GENERATED ALWAYS AS IDENTITY`. The profile
  declares `sequence`, the db-script builds two named sequences and no identity clause, and the
  deployed migrations agree with the db-script. Corrected in PHASE 1 and in both entity blocks,
  and made explicit as QR-MDL-016 and QR-MDL-017 so the allocation has an address rather than
  living in a sentence — db-script §4.2 asked this stage for exactly this.
- **Three read verbs.** The contract table carried `GET` collection reads for API-MDL-001,
  API-MDL-005 and API-MDL-010. The SRS §B5 and the published surface both say
  `POST …/search`; the plan was the only artifact that disagreed (ADR-MDL-002).
- **The cross-module mechanism.** XM-MDL-001's interface line named a REST call to a path on
  SEC's own base path, a shape the platform has nowhere to put:
  `profile.conventions.module_interface` is `in_process` and the whole platform is one
  deployable, so no HTTP call to another module exists or could. The old line is not reproduced
  here, because a path written out reads as an address even when the sentence around it says the
  opposite. Corrected in INT-C.
- **A catalog row nothing could raise.** The 503 row described "SEC unreachable during
  XM-MDL-001's validation call" — a network failure that cannot occur in one deployable, on a
  status `profile.stack.backend.api.http_statuses` does not declare. Struck, with the reason
  kept where the row was, so the deletion is legible.
<!-- PHASE:ALIGN-BE:END -->

## Coverage tables

**ENT / DBF → phases → QR → XM**

| ENT | DBF | phases | QR | XM |
|---|---|---|---|---|
| ENT-MDL-001 | DBF-MDL-001 | DATA-DOM, SVC-API | QR-MDL-016, QR-MDL-002, QR-MDL-003, QR-MDL-004 | — |
| ENT-MDL-001 | DBF-MDL-002 | DATA-DOM, SVC-API | QR-MDL-001, QR-MDL-002, QR-MDL-010, QR-MDL-013, QR-MDL-015 | — |
| ENT-MDL-001 | DBF-MDL-003 | DATA-DOM, SVC-API, INT-C | QR-MDL-001, QR-MDL-002, QR-MDL-010, QR-MDL-012 | XM-MDL-001 |
| ENT-MDL-001 | DBF-MDL-004 | DATA-DOM, SVC-API | QR-MDL-001, QR-MDL-002, QR-MDL-003, QR-MDL-010 | — |
| ENT-MDL-001 | DBF-MDL-005 | DATA-DOM, SVC-API | QR-MDL-001, QR-MDL-002, QR-MDL-003, QR-MDL-010 | — |
| ENT-MDL-001 | DBF-MDL-006 | DATA-DOM, SVC-API | QR-MDL-001, QR-MDL-002, QR-MDL-004, QR-MDL-010, QR-MDL-011, QR-MDL-015 | — |
| ENT-MDL-001 | DBF-MDL-007 | DATA-DOM, SVC-API | QR-MDL-002 | — |
| ENT-MDL-001 | DBF-MDL-008 | DATA-DOM, SVC-API | QR-MDL-002 | — |
| ENT-MDL-001 | DBF-MDL-009 | DATA-DOM, SVC-API | QR-MDL-003, QR-MDL-004 | — |
| ENT-MDL-001 | DBF-MDL-010 | DATA-DOM, SVC-API | QR-MDL-003, QR-MDL-004 | — |
| ENT-MDL-002 | DBF-MDL-011 | DATA-DOM, SVC-API | QR-MDL-017, QR-MDL-006, QR-MDL-007, QR-MDL-008, QR-MDL-009 | — |
| ENT-MDL-002 | DBF-MDL-012 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-009, QR-MDL-011, QR-MDL-014 | — |
| ENT-MDL-002 | DBF-MDL-013 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-011, QR-MDL-014 | — |
| ENT-MDL-002 | DBF-MDL-014 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-007, QR-MDL-011 | — |
| ENT-MDL-002 | DBF-MDL-015 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-007, QR-MDL-011 | — |
| ENT-MDL-002 | DBF-MDL-016 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-007, QR-MDL-009, QR-MDL-011 | — |
| ENT-MDL-002 | DBF-MDL-017 | DATA-DOM, SVC-API | QR-MDL-005, QR-MDL-006, QR-MDL-008, QR-MDL-011 | — |
| ENT-MDL-002 | DBF-MDL-018 | DATA-DOM, SVC-API | QR-MDL-006 | — |
| ENT-MDL-002 | DBF-MDL-019 | DATA-DOM, SVC-API | QR-MDL-006 | — |
| ENT-MDL-002 | DBF-MDL-020 | DATA-DOM, SVC-API | QR-MDL-007, QR-MDL-008, QR-MDL-009 | — |
| ENT-MDL-002 | DBF-MDL-021 | DATA-DOM, SVC-API | QR-MDL-007, QR-MDL-008, QR-MDL-009 | — |

**RULE → API → catalog code**

| RULE | API | catalog code |
|---|---|---|
| RULE-MDL-001 | API-MDL-002 | MDL-409-MODULE-NOT-REGISTERED |
| RULE-MDL-002 | API-MDL-006, API-MDL-007 | MDL-409-VALUE-DUP |
| RULE-MDL-003 | API-MDL-003 | none — enforced by DTO shape; no code path can raise one |
| RULE-MDL-004 | API-MDL-011 | MDL-404-TYPE-KEY |

**XM → status → blocks → workaround**

| XM | status | blocks | workaround |
|---|---|---|---|
| XM-MDL-001 | ACTIVE / READY | none — no DBF and no API waits on it | none needed |

## Decisions applied

| ADR | What | Status |
|---|---|---|
| ADR-MDL-002 (P3.2) | The three composite reads are `POST …/search`, and this plan's contract table was the artifact that lagged — corrected in PHASE 3 and PHASE 4 | ACCEPTED (non-breaking) — analysis/decisions/MDL/ADR-MDL-002.md |
| ADR-MDL-007 (P3.2) | API-MDL-011 is bound and called by no screen: a consuming module's backend calls it, and its authorisation is the VIEW grant on MDL_LOOKUPS | ACCEPTED (non-breaking) — analysis/decisions/MDL/ADR-MDL-007.md |
| ADR-MDL-009 (P2) | Index strategy for the two tables — which filter columns are indexed and why neither flag is | ACCEPTED (non-breaking), applied here |
| ADR-MDL-010 (P2) | String precisions for the six unsized text columns — the maxima every Request line above states | ACCEPTED (non-breaking), applied here |
| ADR-SEC-002 (SEC) | Structural error-catalog rows (duplicate, not found) and infrastructure rows sit under one PLATFORM-STD umbrella instead of a business rule each — cited, not re-derived, exactly as that decision's own consequence allows | ACCEPTED, applied here |
| ADR-FIN-001 (FIN) | A dependency on another module is an application read, never a cross-module foreign key — applied to XM-MDL-001 | ACCEPTED, applied here |

No new ADR was raised at this stage: every point that could have gone two ways was settled by an
input already in hand — the profile, the db-script, the SRS, or a decision already on the
platform's record. No BLOCKED ADR — the pass was not stopped. No question was raised.
══════════════════════════════════════════════════════════════════
