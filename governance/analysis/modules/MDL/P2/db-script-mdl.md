# DATABASE — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Dialect : postgresql16   Schema prefix : none
Identifier transformation : SRS logical field name (camelCase) → physical column name
  (snake_case); table, sequence, constraint and index names are written in upper case.
  PostgreSQL folds every unquoted identifier to lower case and this script quotes no
  identifier, so exactly one spelling of each object exists in the catalog. `key` is a
  NON-RESERVED keyword in PostgreSQL 16 and is kept verbatim as the column name the SRS
  gives it (A3); no identifier reaches the 63-byte limit.
Type map addition : logical type `number` → `INTEGER` — declared once here (engine §4.1),
  since `profile.stack.db.syntax_map` carries no integer row and `sortOrder` is a whole
  number, not a decimal. Every other type is taken from the map unchanged.
PK generation : `sequence` (profile.stack.db.pk_generation) — one `SEQ_{TABLE}` per table
  in BLOCK 1, PK columns declared plain `BIGINT NOT NULL` with no identity clause and no
  sequence default; the application draws the key from the named sequence. No PK trigger.
Date : 2026-09-19
Counts : 2 tables · 21 DBF · 1 XM (XM-MDL-001 SOFT-READ → SEC)
══════════════════════════════════════════════════════════════════

## 1. DB FIELD TRACEABILITY MATRIX — MDL v1

This section is the canonical source of `DBF` → column → type → SRS origin. Downstream
artifacts reference these columns by DBF id only and never restate name, type or origin.

### Table MDL_LOOKUP_TYPE (ENT-MDL-001, kind master)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-MDL-001 | lookup_type_pk | BIGINT | ENT-MDL-001.lookupTypePk | REQ-MDL-001 | NOT NULL | — (drawn from SEQ_MDL_LOOKUP_TYPE by the application) |
| DBF-MDL-002 | key | VARCHAR(50) | ENT-MDL-001.key | REQ-MDL-001, REQ-MDL-003, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013 | NOT NULL | — |
| DBF-MDL-003 | owner_module_code | VARCHAR(10) | ENT-MDL-001.ownerModuleCode | REQ-MDL-001, REQ-MDL-002, REQ-MDL-013 | NOT NULL | — |
| DBF-MDL-004 | name_ar | VARCHAR(200) | ENT-MDL-001.nameAr | REQ-MDL-001, REQ-MDL-003, REQ-MDL-013 | NOT NULL | — |
| DBF-MDL-005 | name_en | VARCHAR(200) | ENT-MDL-001.nameEn | REQ-MDL-001, REQ-MDL-003, REQ-MDL-013 | NOT NULL | — |
| DBF-MDL-006 | is_active_fl | BOOLEAN | ENT-MDL-001.isActiveFl | REQ-MDL-001, REQ-MDL-004, REQ-MDL-011, REQ-MDL-013 | NOT NULL | TRUE |
| DBF-MDL-007 | created_by | VARCHAR(100) | ENT-MDL-001.createdBy (profile: entity_defaults.master) | REQ-MDL-001 | NOT NULL | — |
| DBF-MDL-008 | created_at | TIMESTAMPTZ | ENT-MDL-001.createdAt (profile: entity_defaults.master) | REQ-MDL-001 | NOT NULL | — |
| DBF-MDL-009 | updated_by | VARCHAR(100) | ENT-MDL-001.updatedBy (profile: entity_defaults.master) | REQ-MDL-003, REQ-MDL-004 | NULL | — |
| DBF-MDL-010 | updated_at | TIMESTAMPTZ | ENT-MDL-001.updatedAt (profile: entity_defaults.master) | REQ-MDL-003, REQ-MDL-004 | NULL | — |

### Table MDL_LOOKUP_VALUE (ENT-MDL-002, kind lookup)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-MDL-011 | lookup_value_pk | BIGINT | ENT-MDL-002.lookupValuePk | REQ-MDL-006 | NOT NULL | — (drawn from SEQ_MDL_LOOKUP_VALUE by the application) |
| DBF-MDL-012 | lookup_type_id | BIGINT | ENT-MDL-002.lookupTypeId → ENT-MDL-001 (intra-module FK) | REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-011 | NOT NULL | — |
| DBF-MDL-013 | code | VARCHAR(50) | ENT-MDL-002.code | REQ-MDL-006, REQ-MDL-007, REQ-MDL-011 | NOT NULL | — |
| DBF-MDL-014 | name_ar | VARCHAR(200) | ENT-MDL-002.nameAr | REQ-MDL-006, REQ-MDL-008, REQ-MDL-011 | NOT NULL | — |
| DBF-MDL-015 | name_en | VARCHAR(200) | ENT-MDL-002.nameEn | REQ-MDL-006, REQ-MDL-008, REQ-MDL-011 | NOT NULL | — |
| DBF-MDL-016 | sort_order | INTEGER | ENT-MDL-002.sortOrder | REQ-MDL-005, REQ-MDL-006, REQ-MDL-008, REQ-MDL-010, REQ-MDL-011 | NOT NULL | 0 |
| DBF-MDL-017 | is_active_fl | BOOLEAN | ENT-MDL-002.isActiveFl | REQ-MDL-006, REQ-MDL-009, REQ-MDL-011 | NOT NULL | TRUE |
| DBF-MDL-018 | created_by | VARCHAR(100) | ENT-MDL-002.createdBy (SRS A3 addition beyond entity_defaults.lookup) | REQ-MDL-006 | NOT NULL | — |
| DBF-MDL-019 | created_at | TIMESTAMPTZ | ENT-MDL-002.createdAt (SRS A3 addition beyond entity_defaults.lookup) | REQ-MDL-006 | NOT NULL | — |
| DBF-MDL-020 | updated_by | VARCHAR(100) | ENT-MDL-002.updatedBy (SRS A3 addition beyond entity_defaults.lookup) | REQ-MDL-008, REQ-MDL-009, REQ-MDL-010 | NULL | — |
| DBF-MDL-021 | updated_at | TIMESTAMPTZ | ENT-MDL-002.updatedAt (SRS A3 addition beyond entity_defaults.lookup) | REQ-MDL-008, REQ-MDL-009, REQ-MDL-010 | NULL | — |

Total: 21 DBF ids across 2 tables. Sequence continuous across the module, never per table.
Column order inside each table follows the matrix; inside MDL_LOOKUP_VALUE that is the SRS
A3 field order, which itself lists `lookupTypeId` second (§4 below).

Every column above is an SRS A3 field of its entity — no column is invented, and the four
audit columns of each table are the profile default set for the entity kind, carried by the
SRS itself (A3 notes on both entities). The `master` default field `code` is not a second
column here: `key` plays its role, by the SRS A3 note and the module vocabulary.

## 2. XM REGISTER — MDL v1

| XM id | Type | This table | Column / access | Target table | Target module | Traces (REQ) | Status |
|---|---|---|---|---|---|---|---|
| XM-MDL-001 | SOFT-READ | MDL_LOOKUP_TYPE | `owner_module_code` (DBF-MDL-003) — existence of the code is read by code at create time, no FK column and no constraint | SEC_MODULE_REG | SEC | REQ-MDL-001, REQ-MDL-002 | ACTIVE (target SEC v1 gated, pass-1 APPROVE) |

**XM-MDL-001** — SOFT-READ · MDL_LOOKUP_TYPE.owner_module_code → SEC_MODULE_REG (SEC, ENT-SEC-004) [REQ-MDL-001, REQ-MDL-002] · source: SRS A8 consumed entity ModuleRegistry, required by RULE-MDL-001 · status ACTIVE.

MDL consumes exactly one entity (SRS A8) and it is read, never joined: the check is an
application read by code, per the platform precedent that no physical cross-module FK
exists anywhere in this pipeline (project-registry DECISION INDEX #9, ADR-FIN-001). SEC is
Tier 0 like MDL, so the direction is permitted either way [KB:erp-domain-standards §5].
Audit columns are never XMs: `created_by` / `updated_by` hold a principal string, not a key
into SEC (engine §3; project-registry SHARED ENTITY DECLARATIONS row `User`).

### 2.1 SOFT-READ handling
```
-- XM-MDL-001 SOFT-READ — this module's lookup-type service reads SEC_MODULE_REG.module_code
-- from SEC without an FK, to refuse a lookup type whose owner module has no registry row
-- (RULE-MDL-001, on create only). Rationale: referential sanity for the namespacing rule
-- (POL-MDL-002, POL-MDL-003) without a cross-module constraint — SRS A8, [KB:erp-domain-
-- standards §5]. Risk: a module later unregistered in SEC does not invalidate the lookup
-- types it already owns (the SRS states this outcome in RULE-MDL-001 Test-Hint); changes to
-- SEC_MODULE_REG require impact assessment on REQ-MDL-001, REQ-MDL-002 and REQ-MDL-013.
```

No DEFERRED XM and no deferred FK: the only dependency is a SOFT-READ, which never carries
a constraint (§6 of the engine), and SEC is gated regardless.

**Inbound dependencies** (recorded here for the cascade, not assigned here — the consuming
module owns its own XM row): XM-FIN-001 reads this module's tables. Consumers resolve a
value by joining MDL_LOOKUP_VALUE to MDL_LOOKUP_TYPE on `lookup_type_id` and filtering
`MDL_LOOKUP_TYPE.key` — there is no key column on the value table.

## 3. FULL_DATABASE_SCRIPT

Copy-and-run against a clean schema of postgresql16, unedited. All objects unqualified —
no schema prefix anywhere, never mixed (S-7).

```sql
-- ════════════════════════════════════════════════════════════════
-- MDL — البيانات المرجعية / Master Data Lookup — v1 — postgresql16
-- Generated by P2 from srs-mdl.md (v1). 2 tables · 21 DBF · 1 XM.
-- PK generation: named sequences (profile.stack.db.pk_generation = sequence).
-- ════════════════════════════════════════════════════════════════

-- BLOCK 1 — SEQUENCES (one per table; count == table count)
CREATE SEQUENCE SEQ_MDL_LOOKUP_TYPE  START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_MDL_LOOKUP_VALUE START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- BLOCK 2 — PARENT TABLES (no FK dependencies)
CREATE TABLE MDL_LOOKUP_TYPE (
  lookup_type_pk     BIGINT        NOT NULL,
  key                VARCHAR(50)   NOT NULL,
  owner_module_code  VARCHAR(10)   NOT NULL,
  name_ar            VARCHAR(200)  NOT NULL,
  name_en            VARCHAR(200)  NOT NULL,
  is_active_fl       BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by         VARCHAR(100)  NOT NULL,
  created_at         TIMESTAMPTZ   NOT NULL,
  updated_by         VARCHAR(100),
  updated_at         TIMESTAMPTZ
);

-- BLOCK 3 — CHILD TABLES (parent created above)
CREATE TABLE MDL_LOOKUP_VALUE (
  lookup_value_pk    BIGINT        NOT NULL,
  lookup_type_id     BIGINT        NOT NULL,
  code               VARCHAR(50)   NOT NULL,
  name_ar            VARCHAR(200)  NOT NULL,
  name_en            VARCHAR(200)  NOT NULL,
  sort_order         INTEGER       NOT NULL DEFAULT 0,
  is_active_fl       BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by         VARCHAR(100)  NOT NULL,
  created_at         TIMESTAMPTZ   NOT NULL,
  updated_by         VARCHAR(100),
  updated_at         TIMESTAMPTZ
);

-- BLOCK 4 — COMMENTS (table + every column; each column comment cites its DBF id)
COMMENT ON TABLE MDL_LOOKUP_TYPE IS 'ENT-MDL-001 — نوع اللوكب / LookupType: سجل أنواع قوائم القيم المرمزة على مستوى المنصة، لكل نوع مفتاح فريد ووحدة مالكة / the platform-wide register of coded-list types, each with a unique key and an owning module.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.lookup_type_pk    IS 'DBF-MDL-001 — معرف نوع اللوكب / LookupType id. المفتاح الأساسي، يأخذه التطبيق من SEQ_MDL_LOOKUP_TYPE ولا يعرض كمرجع عمل / primary key, drawn by the application from SEQ_MDL_LOOKUP_TYPE, never shown as a business reference.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.key               IS 'DBF-MDL-002 — المفتاح / Key. فريد على مستوى المنصة وغير قابل للتعديل بعد الإنشاء (RULE-MDL-003)، وهو عقد القراءة لكل مستهلك (REQ-MDL-011) / platform-unique, immutable after creation, the read contract of every consumer.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.owner_module_code IS 'DBF-MDL-003 — رمز الوحدة المالكة / Owner module code. يتحقق من وجوده في SEC_MODULE_REG عند الإنشاء بقراءة تطبيقية بلا مفتاح أجنبي (XM-MDL-001, RULE-MDL-001) / validated against SEC_MODULE_REG on create by an application read, with no foreign key.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.name_ar           IS 'DBF-MDL-004 — الاسم (عربي) / Name (Arabic).';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.name_en           IS 'DBF-MDL-005 — الاسم (إنجليزي) / Name (English).';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.is_active_fl      IS 'DBF-MDL-006 — نشط / Active. التعطيل ناعم ولا حذف نهائي؛ النوع المعطل يحجب قيمه عن قراءة المستهلك (RULE-MDL-004) / soft deactivation, never a delete; an inactive type hides its values from consumer reads.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.created_by        IS 'DBF-MDL-007 — أنشأه / Created by. معرف الأصل المصادق، يملؤه النظام ولا يقبل من عميل / the authenticated principal string, filled by the platform, never accepted from a client.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.created_at        IS 'DBF-MDL-008 — تاريخ الإنشاء / Created at. UTC مخزنة وتعرض بتوقيت المستأجر / stored UTC, displayed in the tenant timezone.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.updated_by        IS 'DBF-MDL-009 — عدله / Updated by. يملأ عند أول تعديل / filled by the platform on the first update.';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.updated_at        IS 'DBF-MDL-010 — تاريخ التعديل / Updated at. UTC مخزنة / stored UTC.';
COMMENT ON TABLE MDL_LOOKUP_VALUE IS 'ENT-MDL-002 — قيمة اللوكب / LookupValue: قيم كل نوع لوكب، مرتبة بالرتبة، والرمز هو ما تخزنه الوحدات المستهلكة / the values of each lookup type, ordered by sort order; the code is what consuming modules store.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.lookup_value_pk  IS 'DBF-MDL-011 — معرف القيمة / LookupValue id. المفتاح الأساسي، يأخذه التطبيق من SEQ_MDL_LOOKUP_VALUE / primary key, drawn by the application from SEQ_MDL_LOOKUP_VALUE.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.lookup_type_id   IS 'DBF-MDL-012 — نوع اللوكب / Lookup type. مفتاح أجنبي داخل الوحدة إلى MDL_LOOKUP_TYPE (FK_LOOKUP_VALUE_TYPE) / intra-module foreign key to MDL_LOOKUP_TYPE.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.code             IS 'DBF-MDL-013 — الرمز / Code. فريد ضمن النوع الواحد (RULE-MDL-002)، وهو ما تخزنه الوحدة المستهلكة عندها / unique within one type; the value a consuming module stores on its own row.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.name_ar          IS 'DBF-MDL-014 — الاسم (عربي) / Name (Arabic). التسمية المعروضة / the displayed label.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.name_en          IS 'DBF-MDL-015 — الاسم (إنجليزي) / Name (English). التسمية المعروضة / the displayed label.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.sort_order       IS 'DBF-MDL-016 — الترتيب / Sort order. ترتيب العرض تصاعديا، يضبط بالتحرير أو بإعادة الترتيب (REQ-MDL-010) / ascending display order, set by editing or by reorder.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.is_active_fl     IS 'DBF-MDL-017 — نشط / Active. القيمة المعطلة تغيب عن قراءة المستهلك وتبقى معروضة في شاشة الإدارة (REQ-MDL-009) / an inactive value is absent from consumer reads and still shown on the management screen.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.created_by       IS 'DBF-MDL-018 — أنشأها / Created by. معرف الأصل المصادق، يملؤه النظام / the authenticated principal string, filled by the platform.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.created_at       IS 'DBF-MDL-019 — تاريخ الإنشاء / Created at. UTC مخزنة / stored UTC.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.updated_by       IS 'DBF-MDL-020 — عدلها / Updated by. يملأ عند أول تعديل / filled by the platform on the first update.';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.updated_at       IS 'DBF-MDL-021 — تاريخ التعديل / Updated at. UTC مخزنة / stored UTC.';

-- BLOCK 5 — CONSTRAINTS
-- 5a PRIMARY KEY
ALTER TABLE MDL_LOOKUP_TYPE  ADD CONSTRAINT PK_MDL_LOOKUP_TYPE  PRIMARY KEY (lookup_type_pk);
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT PK_MDL_LOOKUP_VALUE PRIMARY KEY (lookup_value_pk);

-- 5b UNIQUE
-- REQ-MDL-001: the key is unique across the platform.
ALTER TABLE MDL_LOOKUP_TYPE  ADD CONSTRAINT UQ_MDL_LOOKUP_TYPE_KEY        UNIQUE (key);
-- RULE-MDL-002: a code is unique within one type; the same code under another type is legal.
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT UQ_MDL_LOOKUP_VALUE_TYPE_CODE UNIQUE (lookup_type_id, code);

-- 5c CHECK
-- none: no SRS RULE states a value domain for any column; SRS A7 declares no status column
-- (both entities are two-state and carry a BOOLEAN flag, which is its own domain), and the
-- module owns no coded list of its own (SRS A6), so there is no code set to constrain.

-- 5d FOREIGN KEYS (intra-module only; parent PK created in 5a above)
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT FK_LOOKUP_VALUE_TYPE FOREIGN KEY (lookup_type_id) REFERENCES MDL_LOOKUP_TYPE (lookup_type_pk);

-- BLOCK 6 — TRIGGERS
-- none: no SRS RULE requires one. RULE-MDL-001 is a cross-module application read
-- (XM-MDL-001), RULE-MDL-003 is enforced by omission — the key is absent from the update
-- payload entirely (its SRS Test-Hint) — and RULE-MDL-004 is a read-time filter that
-- changes no stored flag. RULE-MDL-002 is structural, in BLOCK 5b. No PK-population
-- trigger and no sequence default on any PK column.

-- BLOCK 7 — INDEXES (non-PK)
-- Search/list filters of SRS PART B B2 on MDL_LOOKUP_TYPE:
CREATE INDEX IDX_MDL_LOOKUP_TYPE_OWNER   ON MDL_LOOKUP_TYPE (owner_module_code);
CREATE INDEX IDX_MDL_LOOKUP_TYPE_NAME_AR ON MDL_LOOKUP_TYPE (name_ar);
CREATE INDEX IDX_MDL_LOOKUP_TYPE_NAME_EN ON MDL_LOOKUP_TYPE (name_en);
-- The type key (B2 filter, and the consumer read of REQ-MDL-011/012) is served by the
-- implicit index of UQ_MDL_LOOKUP_TYPE_KEY — never duplicated.
-- The ordered detail read (REQ-MDL-005, REQ-MDL-011) and the FK column itself:
CREATE INDEX IDX_MDL_LOOKUP_VALUE_TYPE_SORT ON MDL_LOOKUP_VALUE (lookup_type_id, sort_order);
-- The value-code filter of B2 and the FK column are both led by lookup_type_id in
-- UQ_MDL_LOOKUP_VALUE_TYPE_CODE and in the index above — no third index over the same
-- leading column. Neither active flag is indexed (see ADR-MDL-009).

-- BLOCK 8 — LOOKUP SEED DATA
-- none, anywhere: MDL owns no lookup key and consumes none (SRS A6) — it is the mechanism
-- coded lists run on, not the owner of one. Every key belongs to the module that registers
-- it and is seeded by that module through this module's own API (API-MDL-002 for the type,
-- API-MDL-006 for the values), never by an INSERT written in another module's script.
COMMIT;

-- BLOCK 9 — VIEWS
-- none required by this SRS: the consumer read (REQ-MDL-011) is a two-table query with a
-- filter on both active flags, expressed at the API layer, not a stored view.

-- BLOCK 10 — FUNCTIONS / PROCEDURES
-- none required by this SRS.

-- BLOCK 11 — DEFERRED FK PATCH BLOCKS
-- none: this module's single cross-module dependency (XM-MDL-001) is a SOFT-READ, which
-- never becomes a constraint, and SEC is gated regardless. No live FK in this script
-- references another module's table.
```

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| PROFILE | PK generation is `sequence`: one `SEQ_{TABLE}` per table, PK columns plain `BIGINT NOT NULL`, no identity clause, no sequence default, no trigger | profile.stack.db.pk_generation + naming.sequence_pattern + syntax_map.sequence | Binding — one strategy per database. See the alignment note below |
| DEFAULT | Soft deactivation through `is_active_fl` on both tables; no delete column, no hard delete | [KB:erp-domain-standards §6 soft delete]; SRS Decisions applied | Platform-wide decision, not a module one |
| DEFAULT | The four audit columns on both tables — including the `lookup`-kind table, whose default set omits them — because the SRS A3 note carries them there deliberately | [KB:erp-domain-standards §6 audit trail]; SRS A3 | Dropping them would lose who changed a list everyone reads |
| DEFAULT | `created_by` / `updated_by` are a principal string (`VARCHAR(100)`), never a numeric FK into SEC; audit columns are never XMs | engine §3; project-registry SHARED ENTITY DECLARATIONS row `User` | non-breaking |
| DEFAULT | No database DEFAULT on `created_at` / `created_by`: the platform fills the audit columns, so exactly one writer exists. The two flags and `sort_order` do carry the defaults the SRS states (`true`, `0`) | engine §3 AUDIT COLUMNS; SRS A3 | non-breaking |
| DEFAULT | `TIMESTAMPTZ`, stored UTC, displayed in the tenant timezone | [KB:erp-domain-standards §6 dates] | Platform-wide |
| DEFAULT | `key` is kept verbatim as a column name — non-reserved in PostgreSQL 16 — and plays the role of the `master` default field `code` (SRS A3 note); no second column is created for `code` | SRS A3; engine §3 | non-breaking |
| DEFAULT | Inside MDL_LOOKUP_VALUE the DBF order follows SRS A3, which lists `lookupTypeId` second; the engine ordering rule "FK columns after the entity's own columns" governs FK columns the SRS does not itself declare as fields | engine §2; SRS A3; project-registry STRUCTURAL REGISTRY (range pinned) | non-breaking |
| DEFAULT | No CHECK constraint anywhere: no RULE states a value domain, A7 declares no status column, A6 declares no owned coded list | SRS A5, A6, A7 | non-breaking |
| DEFAULT | No seed data in this script and none in any other module's script: MDL owns no lookup key | SRS A6; engine §4.2 | non-breaking |
| ADR-MDL-009 | Index strategy: which filter columns get an index, which are left to the implicit index of a UNIQUE constraint, and why neither BOOLEAN flag is indexed | analysis/decisions/MDL/ADR-MDL-009.md | ACCEPTED (non-breaking) |
| ADR-MDL-010 | String precisions (`VARCHAR` lengths) for the six text columns the SRS leaves unsized | analysis/decisions/MDL/ADR-MDL-010.md | ACCEPTED (non-breaking) |

No BLOCKED ADR — the pass was not stopped. No question was raised.

### 4.1 RULE → constraint mapping (SRS A5)

| RULE | Mapped to | Where |
|---|---|---|
| RULE-MDL-001 | no database object — an application read of SEC_MODULE_REG by code (XM-MDL-001), on create only | §2.1; BLOCK 6 note |
| RULE-MDL-002 | `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` UNIQUE (lookup_type_id, code) | BLOCK 5b |
| RULE-MDL-003 | no database object — the key is absent from the update payload (the RULE Test-Hint); never a trigger | BLOCK 6 note |
| RULE-MDL-004 | no database object — a read-time filter over DBF-MDL-006 and DBF-MDL-017; the rule changes no stored flag | BLOCK 6 note; BLOCK 9 note |

The platform-unique key of REQ-MDL-001 and the not-found of REQ-MDL-012 are error-catalog
rows under PLATFORM-STD, not business rules (project-registry DECISION INDEX #8,
ADR-SEC-002); the uniqueness half of that pair is nevertheless structural here, in
`UQ_MDL_LOOKUP_TYPE_KEY`.

### 4.2 Alignment note — PK generation and the existing P3.1 plan (engine §8)

`backend-execution-plan-mdl.md` (P3.1, gated) states `PK generation GENERATED ALWAYS AS
IDENTITY` in the BINDINGS line of both entities. That line predates the profile's current
`pk_generation: sequence` and does not describe what is deployed: the delivered migrations
build plain `BIGINT NOT NULL` PKs fed by named `SEQ_<TABLE>` sequences, and the repository's
entity contract mandates `GenerationType.SEQUENCE` — recorded for the sibling module in
`db-script-fin.md` §1 (migration V22). This script follows the profile and the deployed
reality; the two BINDINGS lines of the P3.1 plan are stale text to be corrected when that
stage is re-run, per engine §8. Nothing else in the plan is affected: it references columns
by DBF id, and no DBF id, column name, type or constraint name moved.

## 5. REGISTRY CONTENT
See `registry-db-mdl.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-MDL-001** — MDL_LOOKUP_TYPE.lookup_type_pk [ENT-MDL-001, REQ-MDL-001]
**DBF-MDL-002** — MDL_LOOKUP_TYPE.key [ENT-MDL-001, REQ-MDL-001, REQ-MDL-003, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013]
**DBF-MDL-003** — MDL_LOOKUP_TYPE.owner_module_code [ENT-MDL-001, REQ-MDL-001, REQ-MDL-002, REQ-MDL-013]
**DBF-MDL-004** — MDL_LOOKUP_TYPE.name_ar [ENT-MDL-001, REQ-MDL-001, REQ-MDL-003, REQ-MDL-013]
**DBF-MDL-005** — MDL_LOOKUP_TYPE.name_en [ENT-MDL-001, REQ-MDL-001, REQ-MDL-003, REQ-MDL-013]
**DBF-MDL-006** — MDL_LOOKUP_TYPE.is_active_fl [ENT-MDL-001, REQ-MDL-001, REQ-MDL-004, REQ-MDL-011, REQ-MDL-013]
**DBF-MDL-007** — MDL_LOOKUP_TYPE.created_by [ENT-MDL-001, REQ-MDL-001]
**DBF-MDL-008** — MDL_LOOKUP_TYPE.created_at [ENT-MDL-001, REQ-MDL-001]
**DBF-MDL-009** — MDL_LOOKUP_TYPE.updated_by [ENT-MDL-001, REQ-MDL-003, REQ-MDL-004]
**DBF-MDL-010** — MDL_LOOKUP_TYPE.updated_at [ENT-MDL-001, REQ-MDL-003, REQ-MDL-004]
**DBF-MDL-011** — MDL_LOOKUP_VALUE.lookup_value_pk [ENT-MDL-002, REQ-MDL-006]
**DBF-MDL-012** — MDL_LOOKUP_VALUE.lookup_type_id [ENT-MDL-002, ENT-MDL-001, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-011]
**DBF-MDL-013** — MDL_LOOKUP_VALUE.code [ENT-MDL-002, REQ-MDL-006, REQ-MDL-007, REQ-MDL-011]
**DBF-MDL-014** — MDL_LOOKUP_VALUE.name_ar [ENT-MDL-002, REQ-MDL-006, REQ-MDL-008, REQ-MDL-011]
**DBF-MDL-015** — MDL_LOOKUP_VALUE.name_en [ENT-MDL-002, REQ-MDL-006, REQ-MDL-008, REQ-MDL-011]
**DBF-MDL-016** — MDL_LOOKUP_VALUE.sort_order [ENT-MDL-002, REQ-MDL-005, REQ-MDL-006, REQ-MDL-008, REQ-MDL-010, REQ-MDL-011]
**DBF-MDL-017** — MDL_LOOKUP_VALUE.is_active_fl [ENT-MDL-002, REQ-MDL-006, REQ-MDL-009, REQ-MDL-011]
**DBF-MDL-018** — MDL_LOOKUP_VALUE.created_by [ENT-MDL-002, REQ-MDL-006]
**DBF-MDL-019** — MDL_LOOKUP_VALUE.created_at [ENT-MDL-002, REQ-MDL-006]
**DBF-MDL-020** — MDL_LOOKUP_VALUE.updated_by [ENT-MDL-002, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010]
**DBF-MDL-021** — MDL_LOOKUP_VALUE.updated_at [ENT-MDL-002, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010]
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 08 (migrations V3, V8, V10, V13)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` at that tag unless
stated otherwise; `mdl/` abbreviates `erp-core/src/main/java/com/erp/mdl/`. No DBF or XM ids are minted
here. Every row is labelled NEW / CHANGED / REMOVED / NOT IMPLEMENTED.

### Migration chain — سلسلة الترحيل
| Kind | Script | What it does to MDL | Source |
|---|---|---|---|
| CHANGED | `V3__mdl_schema.sql` | creates `SEQ_MDL_LOOKUP_TYPE`, `SEQ_MDL_LOOKUP_VALUE`, `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`, the PK / UNIQUE / FK constraints and four indexes (squashed in step 04 from the old `V18__mdl_sequences`, DDL verbatim). Its header still says the db-script declares `GENERATED ALWAYS AS IDENTITY`; this script (§4 PROFILE row) says `sequence`, and the migration itself builds sequence-fed `BIGINT NOT NULL` PKs — the header comment is stale, the DDL agrees with this script. | V3__mdl_schema.sql:8-12,18-19,86-97 |
| NEW (seed) | `V8__mdl_seed.sql` | 4 lookup types / 17 values inserted raw (from the old `V20__notif_file_lookup_data_migration`); the owners' `SEC_MODULE_REG` rows and the MDL registry / grants are in `V7__sec_seed.sql`, which runs first. | V8__mdl_seed.sql:25-65; V7__sec_seed.sql:37,64-65,108-112 |
| CHANGED | `V10__tenant_schema.sql` | tenant and version columns, FKs, indexes, composite uniques (step 05). | V10__tenant_schema.sql:64-65,84-85,103-104,122-123,177-180 |
| NEW (seed) | `V13__notif_async_inbox.sql` §3a | three NOTIF-owned values for every tenant that holds the type (step 08). | V13__notif_async_inbox.sql:66-80 |
Full old → new mapping: `docs/steps/04-report.md` → "Old → new mapping".

### Per-table deltas — فروق الجداول
| Kind | Table | Delta (exact physical names) | Migration |
|---|---|---|---|
| CHANGED | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE | + `TENANT_ID BIGINT NOT NULL` (added `DEFAULT 1` = PLATFORM for the backfill, default dropped), `FK_MDL_LOOKUP_TYPE_TENANT` / `FK_MDL_LOOKUP_VALUE_TENANT` → `CORE_TENANT (ID)`, `IDX_MDL_LOOKUP_TYPE_TENANT` / `IDX_MDL_LOOKUP_VALUE_TENANT (TENANT_ID)`; + `VERSION BIGINT NOT NULL DEFAULT 0` | V10:64-65,84-85,103-104,122-123 |
| CHANGED | MDL_LOOKUP_TYPE | `UQ_MDL_LOOKUP_TYPE_KEY` → `UNIQUE (TENANT_ID, key)` (dropped and re-created under the same name) | V10:177-178 |
| CHANGED | MDL_LOOKUP_VALUE | `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` → `UNIQUE (TENANT_ID, lookup_type_id, code)` | V10:179-180 |
| CHANGED | MDL_LOOKUP_TYPE | `key VARCHAR(80)` (DBF-MDL-002, planned 50); `name_ar` / `name_en VARCHAR(150)` (DBF-MDL-004/005, planned 200); `created_at TIMESTAMPTZ NOT NULL DEFAULT now()` (DBF-MDL-008, planned no default). `owner_module_code VARCHAR(10)`, `created_by` / `updated_by VARCHAR(100)`, `is_active_fl BOOLEAN NOT NULL DEFAULT TRUE` as planned. | V3:24-35 |
| CHANGED | MDL_LOOKUP_VALUE | `name_ar` / `name_en VARCHAR(150)` (DBF-MDL-014/015, planned 200); `sort_order NUMERIC NOT NULL DEFAULT 0` (DBF-MDL-016, planned INTEGER) — the entity maps it to `Integer`; `created_at TIMESTAMPTZ NOT NULL DEFAULT now()` (DBF-MDL-019). `code VARCHAR(50)` as planned. | V3:40-52; mdl/entity/LookupValue.java:80-87 |
| CHANGED | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE | BLOCK 4 comments reduced to the DBF id (`'DBF-MDL-0NN'`), with a rule pointer on `key`, `owner_module_code` and `code`; the bilingual text of this script's BLOCK 4 is not in the catalog. | V3:57-80 |
| NEW | MDL_LOOKUP_TYPE | `IDX_MDL_LOOKUP_TYPE_ACTIVE ON MDL_LOOKUP_TYPE (is_active_fl)` — BLOCK 7 said neither flag is indexed. | V3:113; mdl/entity/LookupType.java:41 |
| NEW | MDL_LOOKUP_VALUE | `IDX_MDL_LOOKUP_VALUE_TYPE ON MDL_LOOKUP_VALUE (lookup_type_id)` — BLOCK 7 said no third index over the leading column. | V3:114; mdl/entity/LookupValue.java:44 |
| CHANGED | MDL_LOOKUP_VALUE | `IDX_MDL_LOOKUP_VALUE_SORT ON MDL_LOOKUP_VALUE (lookup_type_id, sort_order)` — the BLOCK 7 index exists under this name, not `IDX_MDL_LOOKUP_VALUE_TYPE_SORT`. | V3:115; mdl/entity/LookupValue.java:45 |
| REMOVED | MDL_LOOKUP_TYPE | `IDX_MDL_LOOKUP_TYPE_NAME_AR`, `IDX_MDL_LOOKUP_TYPE_NAME_EN` — never created. `IDX_MDL_LOOKUP_TYPE_OWNER (owner_module_code)` exists as planned. | V3:112-115 |
| NEW (seed) | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE | BLOCK 8 ("none, anywhere … never by an INSERT") is not what shipped: `V8` inserts, for the PLATFORM tenant (`TENANT_ID` 1 after the `V10` backfill), `CREATED_BY = 'SYSTEM'`, `CREATED_AT = CURRENT_TIMESTAMP`, PKs from the sequences, values linked by `KEY`: `NOTIF_CHANNEL` (owner NOTIF — EMAIL 1, SMS 2, WHATSAPP 3, PUSH 4, INTERNAL 5), `NOTIF_STATUS` (owner NOTIF — PENDING 1, SENT 2, FAILED 3, CHANNEL_DISABLED 4), `FILE_FILE_STATUS` (owner FILE — ACTIVE 1, ARCHIVED 2, DELETED 3), `FILE_FILE_TYPE` (owner FILE — IMAGE 1, DOCUMENT 2, SPREADSHEET 3, ARCHIVE 4, OTHER 5). | V8:25-65 |
| NEW (seed) | MDL_LOOKUP_VALUE | + `NOTIF_STATUS` `QUEUED` (sort 5), `SKIPPED_NO_PROVIDER` (sort 6); + `NOTIF_CHANNEL` `IN_APP` (sort 6), for every tenant that has those types, skipping codes already present. | V13:66-80 |

### XM register deltas — فروق سجل التقاطع
| Kind | XM | Delta | Source |
|---|---|---|---|
| CHANGED | XM-MDL-001 (SOFT-READ → SEC_MODULE_REG) | the read is `SEC_MODULE_REG.CODE` **with** `IS_ACTIVE_FL = TRUE` (§2.1 says `module_code` and existence only), performed in-process through `SecModuleRegistryApi.isModuleActive(String)`; still no FK, no constraint. `SEC_MODULE_REG` stays global (no `TENANT_ID`), upserted from code at startup. | mdl/service/LookupTypeService.java:80; erp-core/src/main/java/com/erp/sec/crossmodule/SecModuleRegistryApiImpl.java:36-39; V10 §3 |
| NEW | HARD-FK → CORE_TENANT | `TENANT_ID` on both tables (no analysis folder for the tenant module; no XM id assigned). | V10:103-104 |
| REMOVED | inbound XM-FIN-001 | `fin` was removed from erp-core in step 01; no module joins MDL's tables. | docs/steps/01-report.md |
| NEW | inbound, in-process | FILE (`FileLookupService`), NOTIF (`NotificationLookupService`) and REPORT (`ReportService`) read MDL through `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey` (`LookupOptionView(code, labelAr, labelEn, sortOrder)`, no `@PreAuthorize`, 404 `MDL-404-TYPE-KEY` for an unknown or inactive type) — a Java call, never a SQL join on MDL's tables. | mdl/crossmodule/MdlLookupApiImpl.java:51-66 |
| NEW | registration | there is no registration SPI: a consuming module's types and values are seeded by its own Flyway `INSERT` (core `V8` / `V13`, application `V1000+`) or created through `POST /api/v1/mdl/lookup-types` — BLOCK 8's "seeded through this module's own API" is one of the two ways. A raw `INSERT` must itself respect RULE-MDL-001 (owner row in `SEC_MODULE_REG` first). | V8:5-9 |

### Decisions applied — deltas — القرارات
| Kind | Decision | Delta | Source |
|---|---|---|---|
| REMOVED | ADR-MDL-009 (cited ACCEPTED in BLOCK 7 and §4) | file dropped in the vendoring: the migration creates `IDX_MDL_LOOKUP_TYPE_ACTIVE` and `IDX_MDL_LOOKUP_VALUE_TYPE` and no name index, contradicting it. `V3` BLOCK 7 is the current truth; the strategy as built is ADR-MDL-045 (ACCEPTED). | docs/governance-vendoring-report.md; governance/analysis/decisions/MDL/ADR-MDL-045.md |
| REMOVED | ADR-MDL-010 (cited ACCEPTED in §4) | file dropped in the vendoring: planned `key 50`, names `200`, `sort_order INTEGER`; built `key 80`, names `150`, `sort_order NUMERIC`. The DBF tables of §1 keep the planned widths as history; `V3` is the current truth (row "Physical widths" above). | docs/governance-vendoring-report.md; V3:26-29,43-46 |
| CHANGED | §4 DEFAULT "no database DEFAULT on `created_at` / `created_by`" | `created_at` carries `DEFAULT now()` on both tables; `created_by` has none. | V3:32,49 |
| CHANGED | §4 DEFAULT "no seed data in this script and none in any other module's script" | `V8` (core) seeds four NOTIF / FILE types; `V13` adds values. | V8; V13 §3a |
| NOT IMPLEMENTED | §4.2 alignment note (P3.1 plan BINDINGS `IDENTITY`) | the P3.1 plan is not vendored; the `V3` header comment carries the same stale "IDENTITY" wording while its DDL is sequence-fed. | V3:8-12 |
| CHANGED | `*_pk` primary-key names | kept as recorded (renames are not additive): `PK_MDL_LOOKUP_TYPE (lookup_type_pk)`, `PK_MDL_LOOKUP_VALUE (lookup_value_pk)`. | DEVIATIONS [05]; V3:86-87 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (CHANGELOG [Unreleased]); no MDL behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | consumers' read model | FILE / NOTIF answer their MDL-stored keys with `com.erp.common.lookup.LookupOptionResponse` through `OwnedLookups.read` — a Java-level change on the consumer side; MDL's tables, sequences, constraints and indexes are untouched. | erp-core/src/main/java/com/erp/common/lookup/; CHANGELOG [Unreleased] |
| CHANGED | `LookupTypeDomain` / `LookupValueDomain` | uniqueness refusals raised through `com.erp.common.domain.DomainRules.assertUnique`; same codes, same 409. No migration. | mdl/domain/LookupTypeDomain.java:48; mdl/domain/LookupValueDomain.java:34 |

No migration after `V15` touches MDL (`git diff v1.2.0 -- erp-core/src/main/resources/db/migration/core` is empty).
