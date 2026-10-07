# ADR-SEC-035 — Every SEC primary key is generated from a named sequence; v1's identity columns are migrated
Status      : ACCEPTED (non-breaking)
Stage       : P2        Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009, ENT-SEC-010, ENT-SEC-011, ENT-SEC-012, ENT-SEC-013, ENT-SEC-014, DBF-SEC-001, DBF-SEC-014, DBF-SEC-025, DBF-SEC-030, DBF-SEC-039, DBF-SEC-049, DBF-SEC-060, DBF-SEC-065, DBF-SEC-070, DBF-SEC-075, DBF-SEC-083, DBF-SEC-091, DBF-SEC-097, DBF-SEC-106

## Context
The profile sets `stack.db.pk_generation: sequence`. The target database's applied migrations build
every PK on a named sequence, and "a second strategy cannot be introduced beside them". P2's engine
requires one `SEQ_{TABLE}` per table, PK columns as plain `BIGINT NOT NULL`, and no
`GENERATED ALWAYS AS IDENTITY` anywhere, for **every** table.

The gated SEC v1 script built all 13 of its PKs as `GENERATED ALWAYS AS IDENTITY` and had no
sequences. v2 adds a 14th table, `SEC_SVC_ACCOUNT_CRED` (ENT-SEC-014). There are three options:
- Create the new table on a sequence and leave v1 alone. The database then carries two PK strategies.
- Create the new table on identity. That is a second violation of the profile.
- Migrate v1's 13 tables as well.

The SRS does not settle this. It is a structural choice.

## Decision
The v2 migration puts every SEC table on a named sequence:
- `SEQ_SEC_SVC_ACCOUNT_CRED` for the new table, whose PK is `BIGINT NOT NULL`.
- For each v1 table: `CREATE SEQUENCE SEQ_{TABLE}` using the profile's postgresql16 syntax row verbatim,
  then `setval` to `MAX(pk) + 1` (or 1 if the table is empty) so no issued key repeats, then
  `ALTER COLUMN … DROP IDENTITY`. The column stays `BIGINT NOT NULL` under its v1 PK constraint.

The migration runs in one transaction. Table, column and constraint names do not change, and no key
value changes.

## Consequences
- 13 PK DBFs are MODIFIED (identity → sequence): DBF-SEC-001, 014, 025, 030, 039, 049, 060, 065, 070,
  075, 083, 091, 097. DBF-SEC-106 is ADDED on its sequence.
- The database no longer generates keys. The backend must take each new key from its sequence
  (P3.1 v2 carries the 14 sequence names; the JPA mapping moves from `IDENTITY` to a named
  `SEQUENCE` generator). A v1 backend that inserts without a key fails after this migration. The
  v2 backend and this migration must be deployed together.
- Not a breaking change. The SRS has no REQ or ENT about key generation. Every registered name is
  kept. The only gated structure that changes is SEC's own, inside SEC's own delta, and it moves
  toward the profile.
- Override: if the target database already carries these `SEQ_SEC_*` objects (the profile says its
  applied migrations use named sequences), skip the 13 v1 `CREATE SEQUENCE` lines and keep the
  realignment and `DROP IDENTITY` lines. If the client would rather accept two strategies, keep
  v1's identity columns and delete this migration's v1 half. Only the new table then follows the
  profile.
