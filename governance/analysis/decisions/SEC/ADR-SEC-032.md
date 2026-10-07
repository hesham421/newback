# ADR-SEC-032 — A service account's email is the responsible contact; its password hash is an unusable random value
Status      : ACCEPTED (non-breaking)
Stage       : P1        Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
Decided     : 2026-09-23
traces      : ENT-SEC-001, REQ-SEC-064, RULE-SEC-010, POL-SEC-004, POL-SEC-016, US-SEC-013

## Decision
`ENT-SEC-001` is reused for both principal types rather than split, and two of its v1 columns are
given a stated meaning for a SERVICE principal:

- **email** — the responsible team's contact address, not a login identity and never a
  password-reset target. A service account's row still satisfies v1's uniqueness and format
  constraints, so no v1 requirement changes.
- **passwordHash** — an unusable random value. It is write-only as in v1 (POL-SEC-004): never
  logged, never returned, never transmitted. A service account authenticates by its credential,
  never by a password, so nothing ever verifies against this value.

`principalTypeCode` (lookup `PRINCIPAL_TYPE`, HUMAN / SERVICE) is entered on create only and is
read-only afterwards (RULE-SEC-012).

## Why
Both columns are NOT NULL in v1 and every screen, query and audit path in the module reads them. Making
them nullable for one principal type would reopen v1 structure that acceptance 2 says is inherited, not
reopened, and would push a null check into every reader. Giving them a defined meaning costs nothing
structural and keeps one users table, one set of admin screens and one audit trail — which is what
POL-SEC-012's "like any other principal" asks for.

An unusable hash rather than an empty one matters: it makes a password login for a service account fail
by construction, not only by the policy check of POL-SEC-016.

Source : NIST SP 800-53 Rev.5 AC-2; REQ-SEC-004 Note precedent
