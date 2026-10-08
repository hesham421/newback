# REGISTRY EXTRACT — registry-db-NOTIF
══════════════════════════════════════════════════════════════════
Module          : Notification Service (NOTIF)
Source artifact : db-script-NOTIF.md
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Notification Service
Module Prefix : NOTIF

## TABLES (DBS-ID register)
| DBS-ID | Table Name | Source ENTITY-ID |
|---|---|---|
| DBS-NOTIF-001 | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBS-NOTIF-001 | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBS-NOTIF-001 | NOTIF_CHANNEL_CONFIG | ENTITY-NOTIF-003 |

## DB FIELD TRACEABILITY (compact)
| DBF-ID | Column Name | DB Type | Table | SRS Source |
|---|---|---|---|---|
| DBF-0001 | ID | BIGINT | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0002 | RECIPIENT_ID | BIGINT | NOTIF_LOG | ENTITY-NOTIF-001 (SOFT→SEC) |
| DBF-0003 | CHANNEL_TYPE_ID | VARCHAR(20) | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0004 | NOTIFICATION_STATUS_ID | VARCHAR(30) | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0005 | MODULE_CODE | VARCHAR(50) | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0006 | REFERENCE_ID | BIGINT | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0007 | REFERENCE_TYPE | VARCHAR(100) | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0008 | RETRY_COUNT | SMALLINT | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0009 | ERROR_MESSAGE | TEXT | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0010 | SENT_AT | TIMESTAMP | NOTIF_LOG | ENTITY-NOTIF-001 |
| DBF-0011 | TEMPLATE_FK | BIGINT | NOTIF_LOG | ENTITY-NOTIF-001 → ENTITY-NOTIF-002 |
| DBF-0012 | ID | BIGINT | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0013 | TEMPLATE_CODE | VARCHAR(80) | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0014 | NAME_AR | VARCHAR(200) | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0015 | NAME_EN | VARCHAR(100) | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0016 | SUBJECT_AR | VARCHAR(300) | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0017 | SUBJECT_EN | VARCHAR(300) | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0018 | BODY_AR | TEXT | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0019 | BODY_EN | TEXT | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0020 | ATTACHMENT_FILE_ID | BIGINT | NOTIF_TEMPLATE | ENTITY-NOTIF-002 (SOFT→FILE) |
| DBF-0021 | IS_ACTIVE_FL | SMALLINT | NOTIF_TEMPLATE | ENTITY-NOTIF-002 |
| DBF-0022 | ID | BIGINT | NOTIF_CHANNEL_CONFIG | ENTITY-NOTIF-003 |
| DBF-0023 | CHANNEL_TYPE_ID | VARCHAR(20) | NOTIF_CHANNEL_CONFIG | ENTITY-NOTIF-003 |
| DBF-0024 | IS_ENABLED_FL | SMALLINT | NOTIF_CHANNEL_CONFIG | ENTITY-NOTIF-003 |
| DBF-0025 | CONFIG_JSON | TEXT | NOTIF_CHANNEL_CONFIG | ENTITY-NOTIF-003 |
Total: 25 DBF-IDs across 3 tables.

## LOV DDL REGISTER
| LOV-ID | Table/Type name | Code values |
|---|---|---|
| LOV-NOTIF-001 | NOTIF_CHANNEL (channelTypeId, runtime-loaded, no DDL table) | EMAIL, SMS, WHATSAPP, PUSH, INTERNAL |
| LOV-NOTIF-002 | NOTIF_STATUS (notificationStatusId, runtime-loaded, no DDL table) | PENDING, SENT, FAILED, CHANNEL_DISABLED |

## XM REGISTER
| XM-ID | Type | Target Table | Target Module | Initial Status |
|---|---|---|---|---|
| XM-NOTIF-001 | SOFT-READ | SEC_USER_ACCOUNT | Security | ACTIVE |
| XM-NOTIF-002 | SOFT-READ | FILE_DOCUMENT | File Service | ACTIVE |

---
*End of registry-db-NOTIF.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08 (migrations V6, V9, V10, V11, V13), 15 (shipped in 1.2.0 — no migration)
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here.

### TABLES — delta
| Kind | Table | Delta | Migration |
|---|---|---|---|
| NEW | NOTIF_INBOX (ENTITY-NOTIF-004) | 15 columns, tenant-scoped; `PK_NOTIF_INBOX`, `FK_NOTIF_INBOX_TENANT`; no link to NOTIF_LOG | V13__notif_async_inbox.sql |
| CHANGED | NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG | + `TENANT_ID` (FK `CORE_TENANT`), + `VERSION`; unique keys per tenant | V10__tenant_schema.sql |
| CHANGED | NOTIF_LOG | delivery-queue columns (below); stored status values QUEUED / SENT / FAILED / CHANNEL_DISABLED / SKIPPED_NO_PROVIDER | V13 |

### COLUMNS — delta
| Kind | Column | DB Type | Table | Migration |
|---|---|---|---|---|
| NEW | ATTEMPTS | INT NOT NULL DEFAULT 0 | NOTIF_LOG | V13 |
| NEW | NEXT_ATTEMPT_AT | TIMESTAMP (next retry; claim lease since 1.2.0) | NOTIF_LOG | V13 |
| NEW | LAST_ERROR | TEXT | NOTIF_LOG | V13 |
| NEW | VARIABLES_JSON | TEXT (cleared at final status) | NOTIF_LOG | V13 |
| NEW | TENANT_ID | BIGINT NOT NULL | NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG | V10 |
| NEW | VERSION | BIGINT NOT NULL DEFAULT 0 | NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG | V10 |
| NEW | ID, TENANT_ID, RECIPIENT_USER_ID, TITLE_AR, TITLE_EN, BODY_AR, BODY_EN, READ_AT, REFERENCE_TYPE, REFERENCE_ID, CREATED_BY, CREATED_AT, UPDATED_BY, UPDATED_AT, VERSION | BIGINT / BIGINT NOT NULL / BIGINT NOT NULL / VARCHAR(300) NOT NULL ×2 / TEXT NOT NULL ×2 / TIMESTAMP / VARCHAR(100) / BIGINT / VARCHAR(255) / TIMESTAMP / VARCHAR(255) / TIMESTAMP / BIGINT NOT NULL DEFAULT 0 | NOTIF_INBOX | V13 |
| CHANGED | TEMPLATE_CODE, CHANNEL_TYPE_ID | stored trimmed + upper-cased (entity lifecycle callbacks) | NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG | — (code) |
| CHANGED | CONFIG_JSON | stored and returned; read by no provider | NOTIF_CHANNEL_CONFIG | — (code); ADR-NOTIF-004 |

### SEQUENCES — delta
| Kind | Sequence | Delta | Migration |
|---|---|---|---|
| CHANGED | SEQ_NOTIF_LOG, SEQ_NOTIF_TEMPLATE, SEQ_NOTIF_CHANNEL_CONFIG | `CACHE 1` (analysis: `NO CACHE`) | V6 |
| NEW | SEQ_NOTIF_INBOX | `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE` | V13 |

### CONSTRAINTS / INDEXES — delta
| Kind | Name | Definition | Migration |
|---|---|---|---|
| CHANGED | UQ_NOTIF_TEMPLATE_CODE | `(TENANT_ID, TEMPLATE_CODE)` | V10 |
| CHANGED | UQ_NOTIF_CHANNEL_CONFIG_TYPE | `(TENANT_ID, CHANNEL_TYPE_ID)` | V10 |
| NEW | FK_NOTIF_TEMPLATE_TENANT, FK_NOTIF_CHANNEL_CONFIG_TENANT, FK_NOTIF_LOG_TENANT, FK_NOTIF_INBOX_TENANT | `TENANT_ID → CORE_TENANT(ID)` | V10, V13 |
| NEW | IDX_NOTIF_TEMPLATE_TENANT, IDX_NOTIF_CHANNEL_CONFIG_TENANT, IDX_NOTIF_LOG_TENANT, IDX_NOTIF_INBOX_TENANT | `(TENANT_ID)` | V10, V13 |
| NEW | IDX_NOTIF_LOG_STATUS_NEXT_ATTEMPT | `(NOTIFICATION_STATUS_ID, NEXT_ATTEMPT_AT)` | V13 |
| NEW | PK_NOTIF_INBOX | `(ID)` | V13 |
| NEW | IDX_NOTIF_INBOX_RECIPIENT | `(TENANT_ID, RECIPIENT_USER_ID, READ_AT)` | V13 |

### SEEDS — delta
| Kind | Table | Rows | Migration |
|---|---|---|---|
| NEW | NOTIF_CHANNEL_CONFIG | EMAIL (PLATFORM); IN_APP (every tenant) — the only two configured channels | V9; V13 §3b |
| NEW | NOTIF_TEMPLATE | PASSWORD_RESET, ACCOUNT_ACTIVATION (PLATFORM; the latter dispatched by no core code); CUSTOMER_VERIFY_EMAIL, CUSTOMER_PASSWORD_RESET (every tenant) | V9; V11 §6 |
| NEW | MDL_LOOKUP_VALUE | NOTIF_CHANNEL + IN_APP; NOTIF_STATUS + QUEUED, SKIPPED_NO_PROVIDER (the analysed values come from V8) | V8; V13 §3a |

### LOV DDL REGISTER — delta
| Kind | LOV | Code values | Migration |
|---|---|---|---|
| CHANGED | NOTIF_CHANNEL | MDL lookup type (owner NOTIF): EMAIL, SMS, WHATSAPP, PUSH, INTERNAL, + IN_APP | V8; V13 §3a |
| CHANGED | NOTIF_STATUS | MDL lookup type (owner NOTIF): PENDING (never stored), SENT, FAILED, CHANNEL_DISABLED, + QUEUED, + SKIPPED_NO_PROVIDER | V8; V13 §3a |

### XM REGISTER — delta
| Kind | XM-ID | Type | Target Table | Target Module | Note |
|---|---|---|---|---|---|
| CHANGED | XM-NOTIF-001 | SOFT-READ | SEC_USER | SEC | `NOTIF_LOG.RECIPIENT_ID`; the physical table is `SEC_USER` (both realms), not `SEC_USER_ACCOUNT`; no FK |
| CHANGED | XM-NOTIF-002 | SOFT-READ | FILE_DOCUMENT | FILE | `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`; validated at write through `FileDocumentLookupApi.isAvailable`; no FK; never sent |
| NEW | — | HARD-FK | CORE_TENANT | tenant | every `TENANT_ID` |
| NEW | — | SOFT-READ | SEC_USER | SEC | `NOTIF_INBOX.RECIPIENT_USER_ID` (no FK), same pattern as XM-NOTIF-001 |
| NEW | — | SOFT-READ (data) | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE | MDL | the NOTIF lookup rows, read through `MdlLookupApi` |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.3.0".

### TABLES / COLUMNS / CONSTRAINTS — delta
None (seed only).

### SEED — delta
| Table | Rows | Migration |
|---|---|---|
| NOTIF_TEMPLATE | `STAFF_PASSWORD_CHANGED` for every existing tenant (copied to later tenants from PLATFORM) | V17__notif_seed_password_changed.sql |
