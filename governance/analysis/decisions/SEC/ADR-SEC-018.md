# ADR-SEC-018 — "Exactly one permission" means one action grant; the module/screen grants the three-level gate requires are not counted as extra

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
CS-SEC-001 requires the consumer to hold "exactly one [permission] — permission to create
journal entries in FIN, and nothing else." SEC's model is hierarchical: a role needs a module
grant before a screen grant, and a screen grant before an action grant (POL-SEC-001,
POL-SEC-002). Two readings: (A) the consumer's role holds one action grant (create journal
entry); the FIN module grant and the journal-entry screen grant it also needs are the
structural path v1's own rules require, not additional permissions — it gets no VIEW action
grant, and a newly created service account starts with none of these until an administrator
assigns them; (B) hard-code a limit of one permission per service account.

## Decision
**Option A.** [KB:erp-domain-standards §4] authorizes each controller method separately, so a
bare action grant without the module/screen path beneath it is unreachable regardless — the
structural grants are a precondition the gate already imposes on every principal, not an
extra permission being carved out for this one. Option B would put a configuration rule in
code, against G1 [domain-profile §5].

## Consequences
- POL-SEC-013 requires a newly created service account to hold no role and no grant until an
  administrator assigns them explicitly.
- The consumer's role is: FIN module grant + journal-entry screen grant + create-journal-entry
  action grant, and nothing else (no VIEW grant). P0.5 acceptance criteria check this exact
  shape when the FIN change set's role is provisioned.
- Nothing here changes POL-SEC-001/POL-SEC-002 (module-gate precedence, no orphaned grants);
  they apply to a service account's role exactly as to a human's.

## Traces
POL-SEC-012, POL-SEC-013
