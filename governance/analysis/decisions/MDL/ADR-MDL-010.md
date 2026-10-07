# ADR-MDL-010 — the string precisions the SRS leaves unsized

Module  : MDL     Version : v1     Stage raised : P2 (Database)
Status  : ACCEPTED (non-breaking)

## Context
`profile.stack.db.syntax_map.string` is `VARCHAR(n)` — the map supplies the syntax, the SRS is
meant to supply `n`. It does not: `srs-mdl.md` A3 types six columns `text` with no length, and
one `number` with no range. A precision is exactly the class of structural choice §9 of the
engine assigns to this stage rather than to a question, and it is a choice with consequences —
a length is a contract with every consumer that stores the value.

The knowledge base sizes money and dates ([KB:erp-domain-standards §6]) and nothing else, so
there is no platform number to copy for identifiers and names.

## Decision

| Column(s) | Type | Why that size |
|---|---|---|
| MDL_LOOKUP_TYPE.key | VARCHAR(50) | A platform-unique machine key written by the registrar in SCREAMING_SNAKE (`PAYMENT_METHOD` in AC-MDL-001 is 14 characters). 50 leaves room for a compound key without inviting a sentence into a column every consumer hard-codes against. |
| MDL_LOOKUP_VALUE.code | VARCHAR(50) | The same class of value — a code stored on the consumer's own row (`CASH`, `CHEQUE`, `TRANSFER`) — and sized identically, so a code can never be a length the storing module cannot hold. |
| MDL_LOOKUP_TYPE.owner_module_code | VARCHAR(10) | A module code of the platform register: every code in `project-registry` MODULE / COMPONENT INDEX is three characters (SEC, MDL, FIN). 10 absorbs a longer future code without pretending the column is free text. |
| MDL_LOOKUP_TYPE.name_ar / name_en, MDL_LOOKUP_VALUE.name_ar / name_en | VARCHAR(200) | Display labels in two languages, one line each. 200 characters holds any label a list offers a user and stays short enough to be indexed (ADR-MDL-009). |
| MDL_LOOKUP_TYPE.created_by / updated_by, MDL_LOOKUP_VALUE.created_by / updated_by | VARCHAR(100) | A principal string, not a key: a username or a service-account name. 100 is the platform's audit-column width and never a numeric FK into SEC. |
| MDL_LOOKUP_VALUE.sort_order | INTEGER | A display position among the values of one type; the logical type `number` has no row in the syntax map, so `INTEGER` is declared once in the script header per engine §4.1. A four-byte integer is three orders of magnitude beyond any list a human maintains through a screen. |

No column is given `TEXT`: every one of them is a bounded, human-entered or machine-entered
identifier or label, and an unbounded column would remove the only validation the database can
offer on them.

## Consequences
- The lengths are the contract the API layer validates against: a create request carrying a
  51-character key is a 400 from the module, not a database error surfacing to a user.
- Widening any of them later is an `ALTER TABLE … ALTER COLUMN … TYPE VARCHAR(n)` in a delta
  version — non-rewriting in PostgreSQL and visible in that version's change manifest. Narrowing
  one is not: it would break rows already stored, which is why each size above is chosen with
  headroom rather than at the observed maximum.
- `key` and `code` sharing one width is deliberate and load-bearing: a consuming module sizes
  its own code column from this decision (XM-FIN-001 stores MDL codes in FIN columns), so the
  two must not drift apart.

Traces : REQ-MDL-001, REQ-MDL-006, REQ-MDL-011 · ENT-MDL-001, ENT-MDL-002 · DBF-MDL-002,
DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-007, DBF-MDL-009, DBF-MDL-013, DBF-MDL-014,
DBF-MDL-015, DBF-MDL-016, DBF-MDL-018, DBF-MDL-020
