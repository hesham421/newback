# REGISTRY EXTRACT — registry-db-FILE
══════════════════════════════════════════════════════════════════
Module          : File Service (FILE)
Source artifact : db-script-FILE.md
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : File Service
Module Prefix : FILE

## TABLES (DBS-ID register)
| DBS-ID | Table Name | Source ENTITY-ID |
|---|---|---|
| DBS-FILE-001 | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBS-FILE-001 | FILE_CATEGORY | ENTITY-FILE-002 |

## DB FIELD TRACEABILITY (compact)
| DBF-ID | Column Name | DB Type | Table | SRS Source |
|---|---|---|---|---|
| DBF-0001 | ID | BIGINT | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0002 | OWNER_ID | BIGINT | FILE_DOCUMENT | ENTITY-FILE-001 (polymorphic) |
| DBF-0003 | OWNER_TYPE | VARCHAR(100) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0004 | MODULE_CODE | VARCHAR(50) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0005 | FILE_NAME | VARCHAR(255) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0006 | CONTENT_TYPE | VARCHAR(150) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0007 | FILE_SIZE | BIGINT | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0008 | FILE_CONTENT | BYTEA | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0009 | FILE_TYPE_ID | VARCHAR(50) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0010 | FILE_STATUS_ID | VARCHAR(50) | FILE_DOCUMENT | ENTITY-FILE-001 |
| DBF-0011 | FILE_CATEGORY_FK | BIGINT | FILE_DOCUMENT | ENTITY-FILE-001 → ENTITY-FILE-002 |
| DBF-0012 | ID | BIGINT | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0013 | CATEGORY_CODE | VARCHAR(50) | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0014 | NAME_AR | VARCHAR(200) | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0015 | NAME_EN | VARCHAR(100) | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0016 | MAX_SIZE_BYTES | BIGINT | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0017 | ALLOWED_CONTENT_TYPES | TEXT | FILE_CATEGORY | ENTITY-FILE-002 |
| DBF-0018 | IS_ACTIVE_FL | SMALLINT | FILE_CATEGORY | ENTITY-FILE-002 |
Total: 18 DBF-IDs across 2 tables.

## LOV DDL REGISTER
| LOV-ID | Table/Type name | Code values |
|---|---|---|
| LOV-FILE-001 | FILE_FILE_TYPE (fileTypeId, runtime-loaded, no DDL table) | IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER |
| LOV-FILE-002 | FILE_FILE_STATUS (fileStatusId, runtime-loaded, no DDL table) | ACTIVE, ARCHIVED, DELETED |

## XM REGISTER
| XM-ID | Type | Target Table | Target Module | Initial Status |
|---|---|---|---|---|
| XM-FILE-001 | SOFT-READ | SEC_USER_ACCOUNT | Security | ACTIVE |

---
*End of registry-db-FILE.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 07 (migrations V5, V7, V8, V10, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here.

### COLUMNS — delta
| Kind | Column | DB Type | Table | Migration |
|---|---|---|---|---|
| NEW | TENANT_ID | BIGINT NOT NULL (FK `CORE_TENANT`) | FILE_CATEGORY, FILE_DOCUMENT | V10 |
| NEW | VERSION | BIGINT NOT NULL DEFAULT 0 | FILE_CATEGORY, FILE_DOCUMENT | V10 |
| NEW | ALLOW_PUBLIC | BOOLEAN NOT NULL DEFAULT FALSE (native boolean, not SMALLINT 0/1) | FILE_CATEGORY | V12 |
| CHANGED | CATEGORY_CODE | VARCHAR(50), stored trimmed + upper-cased (application-level normalisation) | FILE_CATEGORY | V5 (type unchanged); entity `@PrePersist` / `@PreUpdate` |
| NEW | STORAGE_PROVIDER | VARCHAR(8) NOT NULL DEFAULT 'DB', CHECK IN (DB, LOCAL, S3) | FILE_DOCUMENT | V12 |
| NEW | STORAGE_REF | VARCHAR(512) | FILE_DOCUMENT | V12 |
| NEW | VISIBILITY | VARCHAR(8) NOT NULL DEFAULT 'PRIVATE', CHECK IN (PRIVATE, PUBLIC) | FILE_DOCUMENT | V12 |
| NEW | PUBLIC_SLUG | VARCHAR(64) | FILE_DOCUMENT | V12 |
| NEW | CONTENT_HASH | VARCHAR(64) | FILE_DOCUMENT | V12 |
| CHANGED | FILE_CONTENT | BYTEA, now NULLABLE | FILE_DOCUMENT | V12 |

### CONSTRAINTS — delta
| Kind | Constraint / index | Definition | Migration |
|---|---|---|---|
| CHANGED | UQ_FILE_CATEGORY_CATEGORY_CODE | UNIQUE (TENANT_ID, CATEGORY_CODE) | V10 |
| NEW | FK_FILE_CATEGORY_TENANT, FK_FILE_DOCUMENT_TENANT | FOREIGN KEY (TENANT_ID) → CORE_TENANT (ID) | V10 |
| NEW | IDX_FILE_CATEGORY_TENANT, IDX_FILE_DOCUMENT_TENANT | (TENANT_ID) | V10 |
| NEW | CHK_FILE_DOCUMENT_STORAGE_PROVIDER | STORAGE_PROVIDER IN ('DB', 'LOCAL', 'S3') — closes the storage SPI's keys | V12 |
| NEW | CHK_FILE_DOCUMENT_VISIBILITY | VISIBILITY IN ('PRIVATE', 'PUBLIC') | V12 |
| NEW | CHK_FILE_DOCUMENT_PUBLIC_SLUG | (VISIBILITY = 'PUBLIC') = (PUBLIC_SLUG IS NOT NULL) | V12 |
| NEW | UQ_FILE_DOCUMENT_PUBLIC_SLUG | partial unique index (TENANT_ID, PUBLIC_SLUG) WHERE PUBLIC_SLUG IS NOT NULL | V12 |

### LOV DDL REGISTER — delta
| Kind | LOV-ID | Table/Type name | Delta |
|---|---|---|---|
| CHANGED | LOV-FILE-001 | FILE_FILE_TYPE — row of `MDL_LOOKUP_TYPE` (owner FILE) with 5 `MDL_LOOKUP_VALUE` rows (IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER) | MDL-backed, seeded by V8 — not "runtime-loaded, no DDL table"; `FILE_TYPE_ID` remains a plain code column (no FK, no CHECK) |
| CHANGED | LOV-FILE-002 | FILE_FILE_STATUS — row of `MDL_LOOKUP_TYPE` (owner FILE) with 3 `MDL_LOOKUP_VALUE` rows (ACTIVE, ARCHIVED, DELETED) | same; `FILE_STATUS_ID` remains a plain code column |

### SEED — delta
| Kind | Rows | Migration |
|---|---|---|
| NEW | SEC registry: module FILE, screens FILE_CATEGORIES / FILE_BROWSER, 8 PERM_FILE_* actions, role FILE_ADMIN, grants to SYS_ADMIN and FILE_ADMIN | V7 |
| NEW | SEC action `FILE:DOCUMENT:PUBLISH` (screen FILE_BROWSER) + grants to PLATFORM's SYS_ADMIN and FILE_ADMIN | V12 |
| NEW | MDL lookup types and values for the two FILE LOVs | V8 |

### XM REGISTER — delta
| Kind | Type | Target Table | Target Module | Note |
|---|---|---|---|---|
| NEW | HARD-FK | CORE_TENANT | tenant | every `TENANT_ID` |
| NEW | SOFT-READ (application) | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE via `MdlLookupApi` | MDL | lookup values for API-FILE-008; no physical FK; no XM id minted here |

Package C5 review round 1 — COLUMNS delta: `REQUIRED_AUTHORITY` · VARCHAR(100) NULL · FILE_DOCUMENT ·
`V22__file_document_required_authority.sql` (RULE-FILE-012; detail in `db-script.md` 1.3.0 package C5).
