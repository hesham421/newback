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
Steps          : 04, 05, 06, 08 (migrations V6, V9, V10, V11, V13)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script.md` → "Implementation Addendum — erp-core 1.2.0". No DBS / DBF /
XM ids are assigned here.

### TABLES — delta
| Table | Delta | Migration |
|---|---|---|
| NOTIF_INBOX | NEW (15 columns, tenant-scoped) | V13__notif_async_inbox.sql |
| NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG | + `TENANT_ID` (FK `CORE_TENANT`), + `VERSION` | V10__tenant_schema.sql |

### COLUMNS — delta
| Column | DB Type | Table | Migration |
|---|---|---|---|
| ATTEMPTS | INT NOT NULL DEFAULT 0 | NOTIF_LOG | V13 |
| NEXT_ATTEMPT_AT | TIMESTAMP | NOTIF_LOG | V13 (lease since 1.2.0) |
| LAST_ERROR | TEXT | NOTIF_LOG | V13 |
| VARIABLES_JSON | TEXT | NOTIF_LOG | V13 |
| TENANT_ID | BIGINT NOT NULL | all three tables | V10 |
| VERSION | BIGINT NOT NULL DEFAULT 0 | all three tables | V10 |

### CONSTRAINTS / INDEXES — delta
`UQ_NOTIF_TEMPLATE_CODE (TENANT_ID, TEMPLATE_CODE)`, `UQ_NOTIF_CHANNEL_CONFIG_TYPE (TENANT_ID, CHANNEL_TYPE_ID)`
(V10); `IDX_NOTIF_LOG_STATUS_NEXT_ATTEMPT`, `IDX_NOTIF_INBOX_RECIPIENT`, `IDX_<TABLE>_TENANT` (V10, V13).

### LOV DDL REGISTER — delta
| LOV | Added code values | Migration |
|---|---|---|
| NOTIF_CHANNEL | IN_APP | V13 §3a |
| NOTIF_STATUS | QUEUED, SKIPPED_NO_PROVIDER | V13 §3a |

### XM REGISTER — delta
| Type | Target Table | Target Module | Note |
|---|---|---|---|
| HARD-FK | CORE_TENANT | tenant (no analysis folder) | every `TENANT_ID` |
| SOFT-READ | SEC_USER | SEC | `NOTIF_INBOX.RECIPIENT_USER_ID` (no FK), same pattern as XM-NOTIF-001; the physical table is `SEC_USER` |

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
