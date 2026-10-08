## REGISTRY — P2 — TENANT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Read from the core migrations at main @ 2274f86 (`erp-core/src/main/resources/db/migration/core/`;
`Vnn:line` = that script and line). Full detail: `db-script-tenant.md`.

Tables
| Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|
| CORE_TENANT | ENT-TENANT-001 | platform registry (global, no `TENANT_ID`) | DBF-TENANT-001 … DBF-TENANT-010 | V10:31-42 |
| (discriminator columns on 22 tables of other modules) | — (FK target ENT-TENANT-001) | `TENANT_ID BIGINT` → `CORE_TENANT(ID)` | DBF-TENANT-011 … DBF-TENANT-032 | V10:63-80 (18), V11:68, V13:40, V14:33, V15:18 |

DBF ids → code location
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-TENANT-001 | CORE_TENANT.ID | BIGINT | NOT NULL | `PK_CORE_TENANT`; `SEQ_CORE_TENANT` | (PK) | V10:32, :48, :29 |
| DBF-TENANT-002 | CORE_TENANT.CODE | VARCHAR(32) | NOT NULL | `UQ_CORE_TENANT_CODE`, `CHK_CORE_TENANT_CODE` | (unique) | V10:33, :49, :51 |
| DBF-TENANT-003 | CORE_TENANT.NAME_AR | VARCHAR(200) | NOT NULL | — | — | V10:34 |
| DBF-TENANT-004 | CORE_TENANT.NAME_EN | VARCHAR(200) | NOT NULL | — | — | V10:35 |
| DBF-TENANT-005 | CORE_TENANT.STATUS_CODE | VARCHAR(20) DEFAULT 'ACTIVE' | NOT NULL | `CHK_CORE_TENANT_STATUS` | — | V10:36, :50 |
| DBF-TENANT-006 | CORE_TENANT.CREATED_BY | VARCHAR(100) | NOT NULL | — | — | V10:37 |
| DBF-TENANT-007 | CORE_TENANT.CREATED_AT | TIMESTAMPTZ DEFAULT now() | NOT NULL | — | — | V10:38 |
| DBF-TENANT-008 | CORE_TENANT.UPDATED_BY | VARCHAR(100) | NULL | — | — | V10:39 |
| DBF-TENANT-009 | CORE_TENANT.UPDATED_AT | TIMESTAMPTZ | NULL | — | — | V10:40 |
| DBF-TENANT-010 | CORE_TENANT.VERSION | BIGINT DEFAULT 0 | NOT NULL | — | — | V10:41 |
| DBF-TENANT-011 | CU_APP_CONFIGURATION.TENANT_ID | BIGINT | **NULL** (V14:66) | `FK_CU_APP_CONFIGURATION_TENANT` | `IDX_CU_APP_CONFIGURATION_TENANT` | V10:63, :102, :121 |
| DBF-TENANT-012 | MDL_LOOKUP_TYPE.TENANT_ID | BIGINT | NOT NULL | `FK_MDL_LOOKUP_TYPE_TENANT` | `IDX_MDL_LOOKUP_TYPE_TENANT` | V10:64, :103, :122 |
| DBF-TENANT-013 | MDL_LOOKUP_VALUE.TENANT_ID | BIGINT | NOT NULL | `FK_MDL_LOOKUP_VALUE_TENANT` | `IDX_MDL_LOOKUP_VALUE_TENANT` | V10:65, :104, :123 |
| DBF-TENANT-014 | SEC_USER.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_USER_TENANT` | `IDX_SEC_USER_TENANT` | V10:66, :105, :124 |
| DBF-TENANT-015 | SEC_ROLE.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_ROLE_TENANT` | `IDX_SEC_ROLE_TENANT` | V10:67, :106, :125 |
| DBF-TENANT-016 | SEC_USER_ROLE.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_USER_ROLE_TENANT` | `IDX_SEC_USER_ROLE_TENANT` | V10:68, :107, :126 |
| DBF-TENANT-017 | SEC_ROLE_MODULE_GRANT.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_ROLE_MODULE_GRANT_TENANT` | `IDX_SEC_ROLE_MODULE_GRANT_TENANT` | V10:69, :108, :127 |
| DBF-TENANT-018 | SEC_ROLE_SCREEN_GRANT.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_ROLE_SCREEN_GRANT_TENANT` | `IDX_SEC_ROLE_SCREEN_GRANT_TENANT` | V10:70, :109, :128 |
| DBF-TENANT-019 | SEC_ROLE_ACTION_GRANT.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_ROLE_ACTION_GRANT_TENANT` | `IDX_SEC_ROLE_ACTION_GRANT_TENANT` | V10:71, :110, :129 |
| DBF-TENANT-020 | SEC_ACTIVE_SESSION.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_ACTIVE_SESSION_TENANT` | `IDX_SEC_ACTIVE_SESSION_TENANT` | V10:72, :111, :130 |
| DBF-TENANT-021 | SEC_AUDIT_LOG.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_AUDIT_LOG_TENANT` | `IDX_SEC_AUDIT_LOG_TENANT` | V10:73, :112, :131 |
| DBF-TENANT-022 | SEC_PWD_RESET_TOKEN.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_PWD_RESET_TOKEN_TENANT` | `IDX_SEC_PWD_RESET_TOKEN_TENANT` | V10:74, :113, :132 |
| DBF-TENANT-023 | SEC_SIGNUP_REQUEST.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_SIGNUP_REQUEST_TENANT` | `IDX_SEC_SIGNUP_REQUEST_TENANT` | V10:75, :114, :133 |
| DBF-TENANT-024 | FILE_CATEGORY.TENANT_ID | BIGINT | NOT NULL | `FK_FILE_CATEGORY_TENANT` | `IDX_FILE_CATEGORY_TENANT` | V10:76, :115, :134 |
| DBF-TENANT-025 | FILE_DOCUMENT.TENANT_ID | BIGINT | NOT NULL | `FK_FILE_DOCUMENT_TENANT` | `IDX_FILE_DOCUMENT_TENANT` | V10:77, :116, :135 |
| DBF-TENANT-026 | NOTIF_TEMPLATE.TENANT_ID | BIGINT | NOT NULL | `FK_NOTIF_TEMPLATE_TENANT` | `IDX_NOTIF_TEMPLATE_TENANT` | V10:78, :117, :136 |
| DBF-TENANT-027 | NOTIF_CHANNEL_CONFIG.TENANT_ID | BIGINT | NOT NULL | `FK_NOTIF_CHANNEL_CONFIG_TENANT` | `IDX_NOTIF_CHANNEL_CONFIG_TENANT` | V10:79, :118, :137 |
| DBF-TENANT-028 | NOTIF_LOG.TENANT_ID | BIGINT | NOT NULL | `FK_NOTIF_LOG_TENANT` | `IDX_NOTIF_LOG_TENANT` | V10:80, :119, :138 |
| DBF-TENANT-029 | SEC_CUSTOMER_VERIFY_TOKEN.TENANT_ID | BIGINT | NOT NULL | `FK_SEC_CUSTOMER_VERIFY_TOKEN_TENANT` | `IDX_SEC_CUSTOMER_VERIFY_TOKEN_TENANT` | V11:68, :84, :86 |
| DBF-TENANT-030 | NOTIF_INBOX.TENANT_ID | BIGINT | NOT NULL | `FK_NOTIF_INBOX_TENANT` | `IDX_NOTIF_INBOX_TENANT` | V13:40, :61, :62 |
| DBF-TENANT-031 | CORE_NUMBER_SERIES.TENANT_ID | BIGINT | NOT NULL | `FK_CORE_NUMBER_SERIES_TENANT` | `IDX_CORE_NUMBER_SERIES_TENANT` | V14:33, :58, :61 |
| DBF-TENANT-032 | CORE_AUDIT_EVENT.TENANT_ID | BIGINT | NOT NULL | `FK_CORE_AUDIT_EVENT_TENANT` | `IDX_CORE_AUDIT_EVENT_TENANT` | V15:18, :40, :44 |

Counts verified: 22 discriminator columns / FKs / indexes (`erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:51`);
14 tenant-leading unique constraints (`:98`) + 2 tenant-leading unique indexes (V12:43, V14:68). The plan's
"18 `TENANT_ID` columns" is the V10 count only (V10:60).

XM index
| XM-ID | Type | Surface | Target / implementers | Status |
|---|---|---|---|---|
| XM-TENANT-001 | crossmodule read (exposed) | `TenantLookupApi.codeOf(Long)` | FILE, SEQUENCE | ACTIVE |
| XM-TENANT-002 | SPI (exposed) | `TenantProvisioningContributor` | SEC, MDL, NOTIF, SEQUENCE | ACTIVE |

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| `CORE_TENANT.STATUS_CODE` | 2 (CHECK-constrained `CHK_CORE_TENANT_STATUS`, not seeded rows) | TENANT |

Sequences
`SEQ_CORE_TENANT` (V10:29; set to 1 by the PLATFORM seed, V10:57).
Last DBF: DBF-TENANT-032 · Last XM: XM-TENANT-002

Decisions
ADR-TENANT-001 (ACCEPTED, as built) — see `governance/analysis/decisions/TENANT/ADR-TENANT-001.md`

Event
"P2 completed: TENANT v1 (as built) — 1 table, 1 sequence, 32 DBF (10 own + 22 discriminator), 2 XM"

Cascade
No XM row of another module targets TENANT with status DEFERRED — nothing to resolve. The 1.2.0
db-script addenda of SEC, MDL, CU, FILE and NOTIF already record their own `TENANT_ID` columns; the
DBF-TENANT ids above are the tenant-side register of the same columns and do not replace those entries.
`CORE_NUMBER_SERIES` (sequence) and `CORE_AUDIT_EVENT` (audit) have no analysis folder; DBF-TENANT-031/032
are their only register entry.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant profile and lifecycle facts on `CORE_TENANT`
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script-tenant.md` → "Implementation Addendum — erp-core 1.3.0".

Tables — delta
| Kind | Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|---|
| CHANGED | CORE_TENANT | ENT-TENANT-001 | global | + DBF-TENANT-033 … 042 (10 columns) | V18__tenant_profile.sql (033 … 038), V19__tenant_lifecycle.sql (039 … 042) |

DBF ids → code location — delta
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-TENANT-033 | CORE_TENANT.CONTACT_EMAIL | VARCHAR(255) | NULL | — | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-034 | CORE_TENANT.CONTACT_PHONE | VARCHAR(30) | NULL | — | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-035 | CORE_TENANT.COUNTRY_CODE | VARCHAR(2) | NULL | — | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-036 | CORE_TENANT.DEFAULT_LOCALE | VARCHAR(5) | NULL | `CHK_CORE_TENANT_LOCALE` | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-037 | CORE_TENANT.TIMEZONE | VARCHAR(64) | NULL | — | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-038 | CORE_TENANT.NOTES | VARCHAR(1000) | NULL | — | — | V18; tenant/entity/Tenant.java |
| DBF-TENANT-039 | CORE_TENANT.SUSPENDED_AT | TIMESTAMPTZ | NULL | — | — | V19; tenant/entity/Tenant.java |
| DBF-TENANT-040 | CORE_TENANT.SUSPENDED_BY | VARCHAR(100) | NULL | — | — | V19; tenant/entity/Tenant.java |
| DBF-TENANT-041 | CORE_TENANT.SUSPENSION_REASON | VARCHAR(500) | NULL | — | — | V19; tenant/entity/Tenant.java |
| DBF-TENANT-042 | CORE_TENANT.TOKENS_INVALID_BEFORE | TIMESTAMPTZ | NULL | — | — | V19; tenant/entity/Tenant.java |

Constraints — delta: `CHK_CORE_TENANT_LOCALE CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar', 'en'))` (V18).

Lookups — delta
| Key | Seeded values count | Owner |
|---|---|---|
| `CORE_TENANT.DEFAULT_LOCALE` | 2 (CHECK-constrained `CHK_CORE_TENANT_LOCALE`, not seeded rows) | TENANT |

XM index, sequences: unchanged. Last DBF: DBF-TENANT-042 · Last XM: XM-TENANT-002

Event
"P2 1.3.0 (package B): TENANT — 1 table, 1 sequence, 42 DBF (20 own + 22 discriminator), 2 XM"

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding columns on `CORE_TENANT`
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package E).

Tables — delta
| Kind | Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|---|
| CHANGED | CORE_TENANT | ENT-TENANT-001 | global | + DBF-TENANT-043 … 044 (2 columns) | V20__tenant_branding.sql |

DBF ids → code location — delta
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-TENANT-043 | CORE_TENANT.LOGO_FILE_ID | BIGINT | NULL | — (soft reference, XM-TENANT-003) | — | V20; tenant/entity/Tenant.java |
| DBF-TENANT-044 | CORE_TENANT.BRAND_COLOR | VARCHAR(7) | NULL | `CHK_CORE_TENANT_BRAND_COLOR` | — | V20; tenant/entity/Tenant.java |

Constraints — delta: `CHK_CORE_TENANT_BRAND_COLOR CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$')` (V20).

XM index — delta
| Kind | XM id | Kind | Column → target | Status |
|---|---|---|---|---|
| NEW | XM-TENANT-003 | SOFT-REF (consumed) | `CORE_TENANT.LOGO_FILE_ID` → FILE `FILE_DOCUMENT.ID` | IMPLEMENTED (1.3.0) |

Cascade: none in the schema; the logo's `FILE_DOCUMENT` row (owner `CORE_TENANT` / {id}, module `TENANT`) is data
of the tenant's own rows, discarded (DELETED + PRIVATE) on replace or remove. Sequences: unchanged.
Last DBF: DBF-TENANT-044 · Last XM: XM-TENANT-003

Event
"P2 1.3.0 (package E): TENANT — 1 table, 1 sequence, 44 DBF (22 own + 22 discriminator), 3 XM"

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — the token cut-off enforced; `TenantLookupApi.isActive`
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package C12).

Tables, columns, constraints, sequences — delta: none (no migration; `TOKENS_INVALID_BEFORE` is V19's DBF-TENANT-042,
now enforced by `TenantResolutionFilter` and written by revoke-tokens too).

XM index — delta
| Kind | XM id | Kind | Column → target | Status |
|---|---|---|---|---|
| CHANGED | XM-TENANT-001 | crossmodule read (exposed) | + `TenantLookupApi.isActive(Long)` → `CORE_TENANT.STATUS_CODE` (consumer NOTIF) | IMPLEMENTED (1.3.0) |

Last DBF: DBF-TENANT-044 · Last XM: XM-TENANT-003 (unchanged)

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C4 — the idempotency table `CORE_IDEMPOTENCY_KEY` (common; first consumer tenant create)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; detail in `db-script-tenant.md` → "Implementation Addendum — erp-core 1.3.0" (package C4).

Tables — delta
| Kind | Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|---|
| NEW (owned by common, registered here) | CORE_IDEMPOTENCY_KEY | — (`com.erp.common.idempotency.IdempotencyKey`) | tenant-scoped (`TENANT_ID` FK) | DBF-TENANT-045 (its `TENANT_ID` only) | V21__core_idempotency_key.sql |

DBF ids → code location — delta
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-TENANT-045 | CORE_IDEMPOTENCY_KEY.TENANT_ID | BIGINT | NOT NULL | `FK_CORE_IDEMPOTENCY_KEY_TENANT`; leads `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` | `IDX_CORE_IDEMPOTENCY_KEY_TENANT` | V21; common/domain/AuditableEntity.java (`@TenantId`) |

Constraints — delta: `PK_CORE_IDEMPOTENCY_KEY`, `FK_CORE_IDEMPOTENCY_KEY_TENANT`, `UQ_CORE_IDEMPOTENCY_KEY` (V21).
Indexes — delta: `IDX_CORE_IDEMPOTENCY_KEY_TENANT`, `IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT` (V21).
Sequences — delta: `SEQ_CORE_IDEMPOTENCY_KEY` (V21; owned by common).
Counts after V21: 23 discriminator columns / FKs / indexes; 15 tenant-leading unique constraints + 2 tenant-leading
unique indexes (`TenantSchemaIntegrationTest`).

XM index — delta: none (the FK is an inbound discriminator, DBF-TENANT-045).
Last DBF: DBF-TENANT-045 · Last XM: XM-TENANT-003

Event
"P2 1.3.0 (package C4): TENANT — 1 own table + 1 registered common table, 2 sequences, 45 DBF (22 own + 23 discriminator), 3 XM"
