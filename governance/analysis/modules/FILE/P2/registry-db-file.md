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
Steps          : 04, 05, 07 (migrations V5, V10, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here.

### COLUMNS — delta
| Column | DB Type | Table | Migration |
|---|---|---|---|
| TENANT_ID | BIGINT NOT NULL (FK `CORE_TENANT`) | FILE_CATEGORY, FILE_DOCUMENT | V10 |
| VERSION | BIGINT NOT NULL DEFAULT 0 | FILE_CATEGORY, FILE_DOCUMENT | V10 |
| ALLOW_PUBLIC | BOOLEAN NOT NULL DEFAULT FALSE | FILE_CATEGORY | V12 |
| STORAGE_PROVIDER | VARCHAR(8) NOT NULL DEFAULT 'DB' | FILE_DOCUMENT | V12 |
| STORAGE_REF | VARCHAR(512) | FILE_DOCUMENT | V12 |
| VISIBILITY | VARCHAR(8) NOT NULL DEFAULT 'PRIVATE' | FILE_DOCUMENT | V12 |
| PUBLIC_SLUG | VARCHAR(64) | FILE_DOCUMENT | V12 |
| CONTENT_HASH | VARCHAR(64) | FILE_DOCUMENT | V12 |
| FILE_CONTENT | BYTEA, now NULLABLE | FILE_DOCUMENT | V12 |

### CONSTRAINTS — delta
`UQ_FILE_CATEGORY_CATEGORY_CODE (TENANT_ID, CATEGORY_CODE)` (V10); `CHK_FILE_DOCUMENT_STORAGE_PROVIDER`,
`CHK_FILE_DOCUMENT_VISIBILITY`, `CHK_FILE_DOCUMENT_PUBLIC_SLUG`, partial unique index
`UQ_FILE_DOCUMENT_PUBLIC_SLUG (TENANT_ID, PUBLIC_SLUG) WHERE PUBLIC_SLUG IS NOT NULL` (V12).

### XM REGISTER — delta
| Type | Target Table | Target Module | Note |
|---|---|---|---|
| HARD-FK | CORE_TENANT | tenant (no analysis folder) | every `TENANT_ID` |

Package C5 review round 1 — COLUMNS delta: `REQUIRED_AUTHORITY` · VARCHAR(100) NULL · FILE_DOCUMENT ·
`V22__file_document_required_authority.sql` (RULE-FILE-012; detail in `db-script.md` 1.3.0 package C5).
