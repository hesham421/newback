# ADR-SEC-068 — Sequences (`SEQ_SEC_*`) instead of IDENTITY for every SEC primary key

Module  : SEC     Version : erp-core 1.2.0 (as-built)     Stage raised : implementation record (analysis-coverage review, 2026-10-08)
Status  : ACCEPTED (non-breaking) — replaces the citation of ADR-SEC-035 (v2) for this decision

## Context
`P2/db-script-sec.md` §3 declares "BLOCK 1 — SEQUENCES: none: every PK uses GENERATED ALWAYS AS
IDENTITY" and writes all 13 primary keys that way. The repository's entity contract
(`build-create-entity` A.1.3 / A.1.4) mandates `GenerationType.SEQUENCE` with a named
`@SequenceGenerator`, `gov-enforce-backend-contract` rejects `GenerationType.IDENTITY`, and every
other core module (CU, NOTIF, FILE, MDL, tenant, audit, sequence) creates one `SEQ_<TABLE>` per
table. ADR-SEC-035 (SEC v2, never implemented) decided the same migration for the v2 service-account
change set, but it cites ENT-SEC-014 and DBF-SEC-106, ids that exist nowhere in the vendored
analysis, and describes a `DROP IDENTITY` realignment migration that was never needed. The choice:
- Follow the gated v1 script literally (identity columns, no sequences) and carry two key strategies
  in one database.
- Follow the entity contract and the platform's other modules: a named sequence per table from the
  first SEC schema script on.

## Decision
The second, as built from the first core SEC schema script. `V4__sec_schema.sql` BLOCK 1
(`erp-core/src/main/resources/db/migration/core/V4__sec_schema.sql:17-31`) creates 13 sequences —
`SEQ_SEC_USER`, `SEQ_SEC_ROLE`, `SEQ_SEC_USER_ROLE`, `SEQ_SEC_MODULE_REG`, `SEQ_SEC_SCREEN_REG`,
`SEQ_SEC_ACTION_REG`, `SEQ_SEC_ROLE_MODULE_GRANT`, `SEQ_SEC_ROLE_SCREEN_GRANT`,
`SEQ_SEC_ROLE_ACTION_GRANT`, `SEQ_SEC_ACTIVE_SESSION`, `SEQ_SEC_AUDIT_LOG`, `SEQ_SEC_PWD_RESET_TOKEN`,
`SEQ_SEC_SIGNUP_REQUEST` — each `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE`; every `*_PK` column is
plain `BIGINT NOT NULL` under its `PK_SEC_*` constraint (V4:321-334), and no `GENERATED … AS IDENTITY`
appears in any SEC script. `V11__sec_realms.sql:64` adds the 14th, `SEQ_SEC_CUSTOMER_VERIFY_TOKEN`.
Every SEC entity maps its key with `@GeneratedValue(strategy = SEQUENCE)` and a
`@SequenceGenerator(sequenceName = "SEQ_SEC_<TABLE>", allocationSize = 1)`
(`erp-core/src/main/java/com/erp/sec/entity/*.java`). Seeds and the raw-SQL provisioning contributor
take keys with `nextval('SEQ_SEC_*')` (`V7__sec_seed.sql:19`, `SecTenantProvisioningContributor`).
The squash of step 04 kept this shape and verified that every sequence ends at the same value as the
old chain (`docs/steps/04-report.md`, "Decisions & deviations" 4); `implementation-notes.md` §4.1 is the
original record of the deviation from the db-script.

## Consequences
- `db-script-sec.md` §3's BLOCK 1 and its `GENERATED ALWAYS AS IDENTITY` key definitions describe a
  script that was never shipped; the P2 addenda carry the as-built sequences (no DBF is minted — the
  13 PK DBFs keep their ids, their physical type `BIGINT` is unchanged).
- `allocationSize = 1` with `CACHE 1` means one round trip per insert and gap-free keys in the
  common case; the trade-off was accepted platform-wide (plan README §4 naming row).
- ADR-SEC-035's v1 half (create sequences, `setval`, `DROP IDENTITY`) is moot — there were never
  identity columns to migrate — and its v2 table never existed; it stays as a historical record with
  a note.
- Non-breaking: no name, type or constraint differs from the analysed tables apart from the
  generation strategy, which no REQ, ENT or API shape exposes.

## Traces
ENT-SEC-001 … ENT-SEC-013 · DBF-SEC-001, DBF-SEC-014, DBF-SEC-025, DBF-SEC-030, DBF-SEC-039,
DBF-SEC-049, DBF-SEC-060, DBF-SEC-065, DBF-SEC-070, DBF-SEC-075, DBF-SEC-083, DBF-SEC-091, DBF-SEC-097 ·
ADR-SEC-035 (historical) · docs/steps/04-report.md · docs/DEVIATIONS.md [04], [05] ·
implementation-notes.md §4.1
