# CHANGE MANIFEST — CS-SEC-001
Module       : SEC      Version: v2      Baseline: v1
Change type  : ADDITIVE
Summary      : Give SEC a way for a machine caller to authenticate unattended. A daemon that consumes business events from the legacy Oracle system and calls FIN continuously is being built, and SEC today has no concept of a non-human caller: its access token expires after an hour and every request is validated against a live session row, so the daemon would have to run on a human user's credentials and any routine session cleanup would silently take the integration down. One delta on v1; SEC's own analysis chooses the mechanism.

## Scope detail (the raised request, restated for this change set)

### 1. Service account for machine callers

The gap: the consumer is a daemon. It runs unattended and calls FIN continuously. SEC has no concept
of a non-human caller. Its access token expires after an hour, and every request is validated against
a live session row. So the consumer would run on a human user's credentials, and any routine session
cleanup would silently take the integration down.

Requested: a way for a machine caller to authenticate that does not depend on a human login session —
a service account, or a documented and supported refresh path for a long-running client. SEC's own
analysis chooses the mechanism; the requirement is only that:

- it can hold permissions like any other principal, and the consumer needs exactly one — permission
  to create journal entries in FIN, and nothing else;
- it survives unattended for as long as the service runs, without a person re-authenticating;
- it is visible and revocable from SEC's existing administration screens, and its activity shows up
  in the audit log like any other principal's.

This is small in itself but it will break the integration if it is skipped.

## Acceptance for this change set

1. A machine caller can authenticate to the platform unattended, with exactly one permission, and is
   administered and audited like any other principal.
2. Every SEC v1 invariant still holds for a machine principal; they are inherited, not reopened.

## Out of scope for this change set

- The in-database queue, the consumer service, and everything inside the legacy Oracle system.
- The FIN-side deltas the same investigation raised — account mapping, dimension tags on rule-driven
  lines, the duplicate-event error and the closed-period code — tracked separately as a FIN change
  set.

## Per artifact

business-policies:
  ADDED    : POL-SEC-012, POL-SEC-013, POL-SEC-014, POL-SEC-015, POL-SEC-016, POL-SEC-017, POL-SEC-018, POL-SEC-019, POL-SEC-020, POL-SEC-021, POL-SEC-022, POL-SEC-023, POL-SEC-024
  MODIFIED : none
  REMOVED  : none
  UNCHANGED: POL-SEC-001, POL-SEC-002, POL-SEC-003, POL-SEC-004, POL-SEC-005, POL-SEC-006, POL-SEC-007, POL-SEC-008, POL-SEC-009, POL-SEC-010, POL-SEC-011 (carried from v1 by gov.py state)
module-registry:
  MODIFIED : all
prd:
  ADDED    : US-SEC-013, US-SEC-014, US-SEC-015, US-SEC-016, US-SEC-017, US-SEC-018, US-SEC-019
  MODIFIED : US-SEC-006, US-SEC-011, US-SEC-012
  REMOVED  : none
  UNCHANGED: US-SEC-001, US-SEC-002, US-SEC-003, US-SEC-004, US-SEC-005, US-SEC-007, US-SEC-008, US-SEC-009, US-SEC-010 (carried from v1 by gov.py state)
platform-summary:
  MODIFIED : all
srs:
  ADDED    : ENT-SEC-014, REQ-SEC-036, REQ-SEC-037, REQ-SEC-038, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-042, REQ-SEC-043, REQ-SEC-044, REQ-SEC-045, REQ-SEC-046, REQ-SEC-047, REQ-SEC-048, REQ-SEC-049, REQ-SEC-050, REQ-SEC-051, REQ-SEC-052, REQ-SEC-053, REQ-SEC-054, REQ-SEC-055, REQ-SEC-056, REQ-SEC-057, REQ-SEC-058, REQ-SEC-059, REQ-SEC-060, REQ-SEC-061, REQ-SEC-062, REQ-SEC-063, REQ-SEC-064, REQ-SEC-065, REQ-SEC-066, REQ-SEC-067, REQ-SEC-068, REQ-SEC-069, REQ-SEC-070, REQ-SEC-071, AC-SEC-036, AC-SEC-037, AC-SEC-038, AC-SEC-039, AC-SEC-040, AC-SEC-041, AC-SEC-042, AC-SEC-043, AC-SEC-044, AC-SEC-045, AC-SEC-046, AC-SEC-047, AC-SEC-048, AC-SEC-049, AC-SEC-050, AC-SEC-051, AC-SEC-052, AC-SEC-053, AC-SEC-054, AC-SEC-055, AC-SEC-056, AC-SEC-057, AC-SEC-058, AC-SEC-059, AC-SEC-060, AC-SEC-061, AC-SEC-062, AC-SEC-063, AC-SEC-064, AC-SEC-065, AC-SEC-066, AC-SEC-067, AC-SEC-068, AC-SEC-069, AC-SEC-070, AC-SEC-071, AC-SEC-072, AC-SEC-073, AC-SEC-074, AC-SEC-075, AC-SEC-076, RULE-SEC-008, RULE-SEC-009, RULE-SEC-010, RULE-SEC-011, RULE-SEC-012
  MODIFIED : ENT-SEC-001, SCR-REQ-SEC-004, SCR-REQ-SEC-007
  REMOVED  : none
  UNCHANGED: ENT-SEC-002, ENT-SEC-003, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009, ENT-SEC-010, ENT-SEC-011, ENT-SEC-012, ENT-SEC-013, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019, REQ-SEC-020, REQ-SEC-021, REQ-SEC-022, REQ-SEC-023, REQ-SEC-024, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027, REQ-SEC-028, REQ-SEC-029, REQ-SEC-030, REQ-SEC-031, REQ-SEC-032, REQ-SEC-033, REQ-SEC-034, REQ-SEC-035, AC-SEC-001, AC-SEC-002, AC-SEC-003, AC-SEC-004, AC-SEC-005, AC-SEC-006, AC-SEC-007, AC-SEC-008, AC-SEC-009, AC-SEC-010, AC-SEC-011, AC-SEC-012, AC-SEC-013, AC-SEC-014, AC-SEC-015, AC-SEC-016, AC-SEC-017, AC-SEC-018, AC-SEC-019, AC-SEC-020, AC-SEC-021, AC-SEC-022, AC-SEC-023, AC-SEC-024, AC-SEC-025, AC-SEC-026, AC-SEC-027, AC-SEC-028, AC-SEC-029, AC-SEC-030, AC-SEC-031, AC-SEC-032, AC-SEC-033, AC-SEC-034, AC-SEC-035, RULE-SEC-001, RULE-SEC-002, RULE-SEC-003, RULE-SEC-004, RULE-SEC-005, RULE-SEC-006, RULE-SEC-007, SCR-REQ-SEC-001, SCR-REQ-SEC-002, SCR-REQ-SEC-003, SCR-REQ-SEC-005, SCR-REQ-SEC-006, SCR-REQ-SEC-008, SCR-REQ-SEC-009, SCR-REQ-SEC-010 (carried from v1 by gov.py state); A1, A2, A3 preamble, A6, A7, A8 and STANDALONE are restated in full (they carry no id to fold by)
registry-srs:
  MODIFIED : all
db-script:
  ADDED    : DBF-SEC-105, DBF-SEC-106, DBF-SEC-107, DBF-SEC-108, DBF-SEC-109, DBF-SEC-110, DBF-SEC-111, DBF-SEC-112, DBF-SEC-113, DBF-SEC-114, DBF-SEC-115, DBF-SEC-116
  MODIFIED : DBF-SEC-001, DBF-SEC-002, DBF-SEC-003, DBF-SEC-004, DBF-SEC-007, DBF-SEC-014, DBF-SEC-025, DBF-SEC-026, DBF-SEC-027, DBF-SEC-030, DBF-SEC-039, DBF-SEC-049, DBF-SEC-060, DBF-SEC-065, DBF-SEC-070, DBF-SEC-075, DBF-SEC-083, DBF-SEC-084, DBF-SEC-091, DBF-SEC-097, DBF-SEC-072
  REMOVED  : none
  UNCHANGED: DBF-SEC-005, DBF-SEC-006, DBF-SEC-008, DBF-SEC-009, DBF-SEC-010, DBF-SEC-011, DBF-SEC-012, DBF-SEC-013, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017, DBF-SEC-018, DBF-SEC-019, DBF-SEC-020, DBF-SEC-021, DBF-SEC-022, DBF-SEC-023, DBF-SEC-024, DBF-SEC-028, DBF-SEC-029, DBF-SEC-031, DBF-SEC-032, DBF-SEC-033, DBF-SEC-034, DBF-SEC-035, DBF-SEC-036, DBF-SEC-037, DBF-SEC-038, DBF-SEC-040, DBF-SEC-041, DBF-SEC-042, DBF-SEC-043, DBF-SEC-044, DBF-SEC-045, DBF-SEC-046, DBF-SEC-047, DBF-SEC-048, DBF-SEC-050, DBF-SEC-051, DBF-SEC-052, DBF-SEC-053, DBF-SEC-054, DBF-SEC-055, DBF-SEC-056, DBF-SEC-057, DBF-SEC-058, DBF-SEC-059, DBF-SEC-061, DBF-SEC-062, DBF-SEC-063, DBF-SEC-064, DBF-SEC-066, DBF-SEC-067, DBF-SEC-068, DBF-SEC-069, DBF-SEC-071, DBF-SEC-073, DBF-SEC-074, DBF-SEC-076, DBF-SEC-077, DBF-SEC-078, DBF-SEC-079, DBF-SEC-080, DBF-SEC-081, DBF-SEC-082, DBF-SEC-085, DBF-SEC-086, DBF-SEC-087, DBF-SEC-088, DBF-SEC-089, DBF-SEC-090, DBF-SEC-092, DBF-SEC-093, DBF-SEC-094, DBF-SEC-095, DBF-SEC-096, DBF-SEC-098, DBF-SEC-099, DBF-SEC-100, DBF-SEC-101, DBF-SEC-102, DBF-SEC-103, DBF-SEC-104 (carried from v1 by gov.py state); header, matrix (§1), XM register (§2) and decisions (§4) are restated in full (they carry no id to fold by); the script (§3) is a migration on v1
registry-db:
  MODIFIED : all
backend-execution-plan:
  ADDED    : API-SEC-028, API-SEC-029, API-SEC-030, API-SEC-031, API-SEC-032, API-SEC-033, API-SEC-034, API-SEC-035, API-SEC-036, API-SEC-037, API-SEC-038, API-SEC-039, QR-SEC-040, QR-SEC-041, QR-SEC-042, QR-SEC-043, QR-SEC-044, QR-SEC-045, QR-SEC-046, QR-SEC-047, QR-SEC-048, QR-SEC-049, QR-SEC-050, QR-SEC-051, QR-SEC-052, QR-SEC-053, QR-SEC-054
  MODIFIED : API-SEC-001, API-SEC-002, API-SEC-003, API-SEC-004, API-SEC-005, API-SEC-006, API-SEC-007, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-011, API-SEC-012, API-SEC-013, API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017, API-SEC-018, API-SEC-019, API-SEC-020, API-SEC-021, API-SEC-022, API-SEC-023, API-SEC-024, API-SEC-025, API-SEC-026, API-SEC-027, QR-SEC-001, QR-SEC-004, QR-SEC-005, QR-SEC-006, QR-SEC-011, QR-SEC-022, QR-SEC-026, QR-SEC-039, ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009, ENT-SEC-010, ENT-SEC-011, ENT-SEC-012, ENT-SEC-013
  REMOVED  : none
  UNCHANGED: QR-SEC-002, QR-SEC-003, QR-SEC-007, QR-SEC-008, QR-SEC-009, QR-SEC-010, QR-SEC-012, QR-SEC-013, QR-SEC-014, QR-SEC-015, QR-SEC-016, QR-SEC-017, QR-SEC-018, QR-SEC-019, QR-SEC-020, QR-SEC-021, QR-SEC-023, QR-SEC-024, QR-SEC-025, QR-SEC-027, QR-SEC-028, QR-SEC-029, QR-SEC-030, QR-SEC-031, QR-SEC-032, QR-SEC-033, QR-SEC-034, QR-SEC-035, QR-SEC-036, QR-SEC-037, QR-SEC-038 (definition lines restated byte-identical); the plan is re-emitted in full — every phase, the index, the DB Alignment Manifest, both catalogs and the self-check (ADR-SEC-036)
registry-exec-be:
  MODIFIED : all

The three MODIFIED stories are re-emitted for their `Traces` line only, with no change of behaviour:
C4.4 requires every story to trace to at least one policy, and v1 wrote all three as "— (scope only)"
under a contract that did not yet ask for one. POL-SEC-024 is the policy v1 omitted — US-SEC-012
stated the rule in its own prose and nothing carried it.

## Detail (prose)

### P0 — business-policies-sec.md
- ADDED: POL-SEC-012, POL-SEC-013, POL-SEC-014, POL-SEC-015, POL-SEC-016, POL-SEC-017, POL-SEC-018, POL-SEC-019, POL-SEC-020, POL-SEC-021, POL-SEC-022, POL-SEC-023
- MODIFIED: none
- REMOVED: none
- UNCHANGED: every v1 policy (POL-SEC-001 through POL-SEC-011) — inherited by service accounts per acceptance 2
- Also ADDED (no ids): scope exceptions (forced credential expiry, workload identity / mTLS, SEC-side logging of business calls, queue/consumer/Oracle internals); resolved-decisions rows 1–5

### P0 — module-registry-sec.md
- ADDED: POLICIES OWNED entries POL-SEC-012 through POL-SEC-023; entity candidate ServiceAccountCredential (security, PRIVATE); lookup type PRINCIPAL_TYPE; external consumer note under DEPENDENCIES; five AUTO-DECISIONS; resolved-decisions rows 1–3
- MODIFIED: entity candidate User (now covers HUMAN and SERVICE principals); lookup type AUDIT_EVENT_TYPE (AUTO initial values extended for service-account events)
- REMOVED: none
- UNCHANGED: every other v1 entity candidate, USER_STATUS, lookups consumed, shared entities consumed, ROOT status, v1 AUTO-DECISIONS

### P0 — platform-summary.md
- ADDED: overview paragraph for v2; dependency lines for the external Oracle event consumer (authenticates as a SEC service account; holds only the create-journal-entry action grant in FIN); four DEFERRED rows; resolved-decisions rows 3–5
- MODIFIED: MODULES rows 1.2 SEC (status EXISTING), 1.3 MDL and 2.2 FIN (status EXCEPTION, per registry pipeline status); FIN dependency lines aligned to the registry (MDL SOFT-READ; SEC via platform-standard interceptor, no physical FK)
- REMOVED: none
- UNCHANGED: module numbering, build order, MODULES rows 1.1, 2.1, 2.3, 3.1, 3.2, 3.3, v1 DEFERRED rows, v1 resolved-decisions rows 1–2

### Dialogue (rounds 1–3)
- Round 2 amended one proposal on convergence: POL-SEC-017's revocation policy now explicitly covers a request authenticated by an access token issued from a credential, not only one presenting the credential's secret directly — closing a gap where a token issued just before revocation would otherwise keep working until its own expiry (RFC 6749 §4.4 client-credentials flow). module-registry-sec.md resolved-decision #3 and business-policies-sec.md POL-SEC-017 reflect this.
- All other round-1 proposals (mechanism = OAuth 2.0 client credentials; principal model = User of type SERVICE; no forced credential expiry; no interactive use + secret shown once; audit scope; "exactly one permission") were accepted unchanged in round 2. Converged — see the seven DECISION blocks recorded for ADR persistence (ADR-SEC-012 through ADR-SEC-018).
- Round 3 reviewed all seven round-2 decisions again, accepted each one, and fixed policy wording without reopening any decision. POL-SEC-016 now covers a login or password reset requested *for* a service account. Its no-self-sign-up part moved to a new policy, POL-SEC-022. Decision #2 promised irreversible secret storage, but no policy carried it until the new POL-SEC-023. POL-SEC-017 now explicitly names every credential of a deactivated account. POL-SEC-021 describes a state condition, so its EARS pattern changed from optional to state. Five round-3 DECISION blocks were recorded for ADR persistence.

### P0.5 — prd-sec.md
- ADDED: US-SEC-013, US-SEC-014, US-SEC-015, US-SEC-016, US-SEC-017, US-SEC-018, US-SEC-019 — together they trace every v2 policy (POL-SEC-012 through POL-SEC-023); US-SEC-019 also traces the inherited POL-SEC-009
- MODIFIED: none
- REMOVED: none
- UNCHANGED: every v1 story (US-SEC-001 through US-SEC-012) — service accounts are served by the added stories, not by editing v1 stories in place (ENGINE §2 SEQUENCE RULE; acceptance 2)
- Also ADDED (no ids): traceability rows for the seven stories; resolved-decisions rows 1–7

### Dialogue (P0.5, rounds 1–3)
- Round 1 drafted US-SEC-013 through US-SEC-019 and raised five proposals: (1) serve service accounts with new stories rather than editing v1 stories in place; (2) tell the unattended-authentication story from the machine caller's own role; (3) no separate dashboard/active-sessions story; (4) no DEFERRED stories for the P0 scope exceptions; (5) HIGH priority only on stories the manifest states or clearly implies (013, 014, 015, 017, 019), "—" on the two stories derived only from dialogue decisions (016, 018).
- Round 2 accepted all five proposals unchanged. It wrote no DECISION blocks, though, so none were persisted. ADR-SEC-019 through ADR-SEC-023 are the P0 round-3 decisions, not these.
- Round 3 accepted all five proposals again and wrote them as DECISION blocks. It also made two wording fixes that do not change any story id or trace. First, deactivation now lives only in US-SEC-017; before this, US-SEC-013 also listed it. Second, US-SEC-014's "exactly one permission" metric now counts one action grant and treats the FIN module and journal-screen grants as the path to that action. Without this, the metric conflicted with POL-SEC-001 and POL-SEC-002. Seven round-3 DECISION blocks were recorded for ADR persistence. Converged; nothing material remains open before the prd-approval gate.

### P1 — srs-sec.md, registry-srs-sec.md
- ADDED:
  - entity ServiceAccountCredential, ENT-SEC-014;
  - REQ-SEC-036 through REQ-SEC-071, covering US-SEC-013 through US-SEC-019;
  - AC-SEC-036 through AC-SEC-077 (AC-SEC-077 added when ADR-SEC-031 was resolved at the human stop);
  - RULE-SEC-008 through RULE-SEC-012;
  - lookup PRINCIPAL_TYPE (HUMAN / SERVICE), and six SERVICE_* values in AUDIT_EVENT_TYPE;
  - the machine-facing token operation in A8.
- MODIFIED:
  - ENT-SEC-001 gains principalTypeCode and defines what email and passwordHash hold for a service account (ADR-SEC-032);
  - SCR-REQ-SEC-004 gains a principal-type filter, service-account creation and a credentials tab;
  - SCR-REQ-SEC-007 shows the users overview per principal type.
- REMOVED: none.
- UNCHANGED: every other v1 record. Each applies to a service account as to any principal (acceptance 2).
- Registry: the v1 registry said REQ and AC stopped at 033. The v1 SRS already held 034 and 035 (amendment of 2026-09-11), so v2 continues from 035.
- ADRs:
  - ADR-SEC-032 and ADR-SEC-033 are ACCEPTED.
  - ADR-SEC-031 is **SUPERSEDED** by ADR-SEC-034 (option A, human decision): the VIEW gateway is structural, so the consumer's role is FIN module + journal-entry screen + VIEW + CREATE. AC-SEC-077 states that shape. Originally raised BLOCKED — The consumer role decided at P0 and P0.5 has one action grant and no VIEW grant. REQ-SEC-030 / RULE-SEC-007 and the profile's VIEW gateway make that role impossible to create or use. Acceptance 2 and POL-SEC-012 carry that gateway over to service accounts.
  - The pass stops before commit. A human chooses among the four options in ADR-SEC-031.

### P2 — db-script-sec.md, registry-db-sec.md
- The script is a migration on the gated v1 schema, run as one transaction.
- ADDED:
  - table SEC_SVC_ACCOUNT_CRED (ENT-SEC-014), DBF-SEC-106 through DBF-SEC-116, with its PK, FK to SEC_USER, revocation CHECK and user index;
  - column SEC_USER.principal_type_code, DBF-SEC-105: default HUMAN backfills v1 rows; CHECK on HUMAN / SERVICE; a SERVICE principal is never PENDING; indexed for the users-list filter.
- MODIFIED:
  - the 13 v1 PK columns move from identity to a named `SEQ_{TABLE}` sequence realigned past existing keys (ADR-SEC-035, profile `pk_generation: sequence`);
  - DBF-SEC-002, 003, 004 and 007 carry the service-account meaning of username, email, password hash and status (ADR-SEC-032);
  - DBF-SEC-084's CHECK is re-created with the six SERVICE_* audit codes (20 codes).
- REMOVED: none. XM: none (SEC is ROOT). Lookups remain CHECK-constrained under ADR-SEC-001, so there are no seed rows.
- ADRs: ADR-SEC-035 ACCEPTED (non-breaking). The backend must switch to sequence-generated keys when it deploys with this migration (P3.1 v2).

### P3.1 — backend-execution-plan-sec.md, registry-exec-be-sec.md
- The plan is the complete current plan: every phase re-emitted, v1 content restated and v2 content marked (ADR-SEC-036).
- ADDED:
  - API-SEC-028 list, API-SEC-029 issue and API-SEC-030 revoke a service account's credentials;
  - API-SEC-031, the client-credentials token (ADR-SEC-037: JWT carrying the credential id, live-state check on every request, no session, no refresh token, rejection audit in its own transaction);
  - API-SEC-032 … API-SEC-036, five endpoints the SRS screens demand that the v1 plan never declared (ADR-SEC-038);
  - QR-SEC-040 … QR-SEC-054; the ENT-SEC-014 block; the BOOTSTRAP DATA section; six catalog rows, plus the shared VALIDATION_ERROR row v1 omitted.
- MODIFIED in substance: API-SEC-001, 003, 005, 006, 007, 008, 009, 010, 011, 022 and the CORE gate's bearer check; four v1 races get a guard (API-SEC-004, 011, 014 … 017, 026). Every other listed id in annotation only: entity and concurrency lines, DBF ids on writing lines, sequence bindings (ADR-SEC-035).
- REMOVED: none. XM: none (SEC is ROOT).
- DEFERRED: deactivating a role and deactivating a registry row, SRS operations never built (ADR-SEC-038).
- Open by design: REQ-SEC-034, REQ-SEC-035 and QR-SEC-039 are served by the in-process `SecUserDirectoryApi`, not an endpoint. The orphans checks flag all three, as they did in v1 (ADR-SEC-039).
