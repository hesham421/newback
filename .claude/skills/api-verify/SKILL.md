---
name: api-verify
description: "API VERIFICATION (post-implementation). Generates one runnable script that exercises a module's real API in dependency order, plus a problems report — from that module's own api-docs (+ test-execution-manifest when present). Standalone, on demand, never a gate. Use after a backend module's endpoints are implemented and its api-docs are current."
---
`$GOV` below is the governance root the factory publishes — read it, never type it:
`governance/shared/$(jq -r '.paths.modules' governance/shared/platform/profile-summary.json | cut -d/ -f1)`.


# Skill: api-verify

## Description

**API VERIFICATION.** Turns an already-governed module's api-docs (and, when present, its
test-execution-manifest) into one runnable script that calls the real API end-to-end in
dependency order, plus a report of what it found. It designs no test and invents no rule,
message, dependency, or data value of its own — anything not traceable to an input document
is skipped and named as skipped.

This skill is **translation, not derivation**, and it is **module-agnostic** — it runs
identically for any `<MOD>` (SEC, FIN, MDL, FILE, CU, NOTIF, or any module registered later).
Nothing about a specific module is hard-coded here; every fact that can change — base path,
envelope shape, error format, languages, permission pattern, output location — is read from
[`governance/shared/platform/rules/api-verify-config.md`](../../../governance/shared/platform/rules/api-verify-config.md), never typed
into this file. If that file and this one ever disagree, `api-verify-config.md` wins — update
it, not this skill.

## When to Use

- A backend module's endpoints are implemented and its api-docs are current
  (`$GOV/modules/<MOD>/api-docs/`)
- Post-implementation verification against the real running API — not a code-review, not a
  substitute for `gov-validate-backend-feature`
- On demand, invoked explicitly for a given `<MOD>` — never automatically, never as a gate

## When NOT to Use

- Before the module's api-docs exist or are stale — regenerate them first
  (`governance/governance-tools/api-doc-generator`); a stale doc produces a script that tests
  the wrong contract
- As a substitute for `gov-enforce-backend-contract` / `gov-validate-backend-feature` — those
  review code and architecture; this skill exercises the live HTTP surface
- To design new test cases from scratch — that is `test-gen`'s job (test-execution-manifest);
  this skill only consumes what `test-gen` and the api-docs already established

## Responsibilities

- State the tier (Full vs Minimal, see §2) at the start of every run
- Produce one script that creates, reads, updates, and deactivates every documented entity in
  dependency order, tears itself down, and reports failures — nothing else
- Produce a problems report bucketed into *likely real bug* / *test assumption mismatch* /
  *infrastructure*

## Constraints

- MUST NOT invent a business rule, error code, dependency edge, or payload value not present
  in the api-docs or manifest
- MUST NOT ask questions — an unresolvable ambiguity (a dependency cycle, an ambiguous
  payload) is an ADR-worthy blocker: state it and stop, do not guess
- MUST NOT change any line artifact, mint any governance ID, or render a gate verdict — this
  runs entirely outside the governed pipeline
- MUST NOT perform a hard delete unless the module's own api-docs document one
- MUST NOT write to the database outside the opt-in, transaction-scoped, run-created-ids-only
  path described in `api-verify-config.md` §4. "Write to the database" here means a *direct*
  write — a SQL statement the script issues itself. State changes made by calling the platform's
  own documented endpoints are not that: they are the thing being verified, and they are governed
  by stages F and I instead
- MUST NOT embed a literal credential in a generated script — credentials are run arguments or
  clearly-marked placeholders (`api-verify-config.md` §4), never typed into an artifact that
  gets committed
- MUST NOT mutate a pre-existing security record (role, grant, user) outside the single
  bounded exception in §3-I — and never at all against a non-Dev/Test target
- MUST NOT report a run as clean while records it created survive: every surviving id is
  listed in the report (§3-F), never summarised as "cleaned up"

## Output

- `$GOV/modules/<MOD>/backend/test-api/test_<mod>_apis.py` (or the language the run targets)
- `$GOV/modules/<MOD>/backend/test-api/<mod>_problems_report.md`

---

## 1. Inputs and tiers

Read `governance/shared/platform/rules/api-verify-config.md` §1 for exact paths. Two tiers, decided by what
`<MOD>`'s own governance folder actually contains — state which one at the start of the run:

| Tier | Present | Generates |
|---|---|---|
| **Full** | api-docs + test-execution-manifest | happy-path CRUD per entity **and** negative (rule-violation) tests; dependency order and RULE→code→TC triples read verbatim from the manifest — zero self-derivation, even with other artifacts also attached |
| **Minimal** | api-docs only | happy-path CRUD only; FK order inferred from FK-typed fields and "parent …" wording in the request tables; the report states negatives were skipped and why |

Multi-batch api-docs (a large module documented across several endpoint files): reconcile
received endpoints against the manifest's ENTITY CRUD CHECKLIST (or the api-docs' own
catalogue), report entity-level coverage briefly, never ask — list any entity without a
documented create endpoint as *out of scope for this run*. One function per entity lets a
later batch append without a rewrite.

## 2. Stack conventions

All read from `governance/shared/platform/rules/api-verify-config.md` §3 — base path, verb→operation mapping,
response/paging/error envelopes, error-code format, `DELETE` semantics, permission pattern,
languages. Do not restate or override those values here or in the generated script; if a
module's actual behavior disagrees with that file, that is either a documentation gap (flag
it) or a convention that changed (fix `api-verify-config.md`, not this run).

## 3. Processing pipeline

**A0 — Preconditions (before any suite runs).** Every payload value that references a row the
run does not itself create — a foreign/registry code, an owner-module code, a lookup key, a
parent id taken from an example rather than threaded — is *verified to exist and be active*
first, through a documented read endpoint. A missing one is reported once, up front, as a
precondition failure naming the exact value and the document that supplied it, and its
dependent suites are marked blocked-on-precondition. Never let it surface instead as a wall of
downstream assertion failures: an example value in a governed document is a claim about the
environment, and a claim that is false is a finding about the document or the seed data, not
about the endpoint under test.

A0 runs **after** stage I, not before it, whenever the run needs a grant at all: the read
endpoints A0 checks through are themselves permission-gated, so a run that verifies
preconditions before granting itself access reports a wall of 403s as "missing preconditions"
and hides the real state of the data. Order is grants → preconditions → suites.

**A — Inventory.** Parse every documented endpoint: id (`API-*` when the docs carry it), verb,
path, entity (path segment), operation type (from verb + path suffix), request field table
(name, type, required, constraints, example), response shape.

**B — Dependency order.** Full tier: read the manifest's DEPENDENCY ORDER as-is. Minimal tier:
infer edges from FK-typed fields whose description names a parent, then sort topologically;
roots first. A cycle or an unresolvable edge → stop and report it as a blocker, do not guess.

**C — Negative mapping.** Full tier only: for each manifest RULE → code → TC triple, set up the
precondition the RULE describes, call the endpoint that must be blocked, assert the HTTP
status and the runtime error code (format from `api-verify-config.md` §3 — never the
hyphenated governance RULE id), tag the function with the RULE/TC ids. Minimal tier: none —
say so. Informational-only RULEs are excluded (the manifest already does this).

**D — Assembly.** One fixed structure (§4), one `test_<entity>()` per entity in stage-B order,
FK ids threaded as parameters from the parent's own return value — never hard-coded.

**E — Exploratory scenarios (bounded).** Only for fields with `required = yes` or a stated
constraint (length, numeric range): at-limit / one-over, omission, type mismatch, an
undocumented lookup value, CRUD idempotency (deactivate twice, activate an already-active
record), invalid FK reference. **Assert vs observe:** an outcome governed by a RULE + code is
stage C's; any other expected status is undocumented → executed as an *observation* (recorded,
never pass/fail, listed apart in the report). No documented source value → skip the case.

**F — Teardown.** Every created id is tracked per entity; cleanup runs in `finally`, in reverse
dependency order; a hard-delete endpoint is used only when the api-docs document one, otherwise
the deactivate endpoint is called and the report states that records remain (deactivated) —
never a silent "cleaned up". That statement is a **list, not a sentence**: a `Surviving records`
section naming every id/natural key left behind and why (no documented hard delete, or a failed
cleanup call), so a human can purge them. Rows the run adds to a *registry or reference table
another module's rules read* (a module registry, a status/type catalogue) are called out
separately as **permanent residue**, with suggested cleanup SQL per stage H — such a row keeps
influencing other modules' behaviour long after the run ends, which an ordinary deactivated
fixture does not. Where that table exposes *any* documented way to retire a row (deactivate,
archive, soft-delete), teardown uses it rather than leaving the row active: an abandoned active
registry entry is not inert test data, it is a value another module's rule may now match on.
Codes written into such a table carry the run's own namespace so residue is always attributable.

**G — Problems report.** `<mod>_problems_report.md` lists only failures, bucketed: *likely real
bug* (a documented rejection did not happen, or an unexplained status), *test assumption
mismatch* (rejected, but with a status the manifest did not state — fix the expectation, not
the backend), *infrastructure* (connection / timeout — rerun). Ambiguous → *likely real bug*.

**H — Log correlation and database access (opt-in).** Log excerpts around a failing call are
attached as *approximate* unless a trace id exists. Database writes are two-tier: deleting rows
this run created (tracked ids only) is allowed for teardown; any other data fix is emitted as
*suggested SQL* for a human — never executed. All DB writes sit behind an explicit opt-in flag
(default off = print only), in a transaction, with separate credentials — see
`api-verify-config.md` §4.

**I — Permission preconditions (the one sanctioned exception to §6's "records it did not
create").** A module's endpoints are permission-gated, and the run's own account may legitimately
lack those permissions — a module registers its screens/actions without granting them to anyone,
by design. The run may therefore grant itself the module's documented permissions through the
platform's own documented grant endpoints, under **all** of these, or not at all:

- **Dev/Test target only** — verified against `api-verify-config.md` §4.1 before the first grant
  call; a target that is not demonstrably Dev/Test aborts the run rather than escalating.
- **Only what the documented endpoints under test require** — never a broader role, never a
  permission outside `<MOD>`'s own set.
- **Journal before granting, not after.** Append the intended grant to the on-disk journal named
  in `api-verify-config.md` §4.2 *before* the call that creates it. `finally` does not survive
  `SIGKILL`, a container stop, or a lost machine — the journal is what makes an interrupted run
  auditable instead of an invisible standing privilege.
- **Revoke only what this run newly created**, in `finally`; a grant that already existed is
  left untouched and reported as pre-existing. Treat a duplicate-grant rejection as "already
  present", never as a failure.
- **Disclose in the report either way** — what was granted, what was revoked, and anything the
  revoke failed to remove, named explicitly as standing privilege needing manual removal.
- **Assume you are not alone.** "Already granted" may mean a concurrent run granted it seconds
  ago and is about to revoke it underneath you. Before treating a pre-existing grant as usable,
  check the journal (§4.2) for an unmatched `GRANT` from another `RUN_ID`; if one is there, stop
  and say so rather than racing — two runs sharing one role will fail each other in ways that
  read like authorization bugs in the module under test.

Anything beyond this — creating a role, elevating a human user, touching a grant the run did not
make — is out of scope and stays out.

## 4. Script structure

```
# Intended for Dev/Test environments only.
config      : BASE_URL (argument or localhost default) · credentials (placeholders unless given)
              · auth endpoint · DB opt-in flag
client      : thin HTTP wrapper (get/post/put/patch/delete) that unwraps the response envelope
results     : TestResult / TestSuite records · run() for asserted calls · run_observation() for
              stage-E observations (separate bucket, never in totals)
helpers     : extract_token · extract_id · first id of a page (per the paging envelope)
grants()    : stage I, only if the run needs permissions it lacks — assert a Dev/Test target
              first, then journal-append → grant, recording which grants this run newly
              created (skipped entirely when unneeded)
preflight() : stage A0 — assert every externally-owned referenced value exists and is active;
              a miss blocks its dependent suites with a precondition failure, never a cascade
per entity  : test_<entity>(parent_ids…) → create → get-by-id (ok) → get-by-id (missing id →
              not found) → update → [stage C negatives] → [stage E observations] → deactivate →
              activate ; appends created ids
cleanup()   : stage F, then stage I's revoke of self-created grants (journal-append after each)
report      : Markdown (+ optional HTML) with pass/fail suites, an "observations" section, the
              problems buckets, a "Surviving records" list and a "Privileges" line →
              <mod>_problems_report.md ; never persists an auth response or a token
main()      : grants() → preflight() → suites in stage-B order, all inside try/finally; exit
              non-zero on any asserted failure (observations never affect the exit code)
```

Every test function carries a traceability comment: `Covers: API-… ; Negative: RULE-… / <code> / TC-…`.

Payload rules: use the api-docs' `example` values verbatim; a required field without an example
gets a clearly marked placeholder of the right type; lookup/enum values only as seen in the
docs; FK ids threaded, never literal.

## 5. Generation gate (silent; only failures are reported)

```
[ ] every entity in the api-docs has a test_<entity>() function
[ ] main() order = stage-B order — no forward FK reference
[ ] every payload value comes from an example or a marked placeholder
[ ] negatives only where RULE + code + TC resolve (Full tier) — none self-derived beside a
    present manifest
[ ] exploratory scenarios only on required/constrained fields; undocumented outcomes use
    run_observation()
[ ] every create appends its id; cleanup() in finally, reverse order; hard-delete used only if
    documented
[ ] runtime error codes in the format api-verify-config.md states; page fields as documented
[ ] no invented credentials, business codes, or lookup values; traceability comment on every
    function
[ ] every externally-owned value a payload references (registry/owner/lookup code, example-
    sourced parent id) is precondition-checked by preflight() before the suites that need it
[ ] the report writer emits a Surviving records list from the tracked-id set, with permanent
    registry/reference residue in its own section — not a hardcoded "cleaned up" string
[ ] if the script grants anything: preflight asserts a Dev/Test target, the journal append
    precedes every grant call, revoke is in finally and guarded on newly-created-by-this-run,
    and the report writer emits the Privileges line unconditionally
[ ] no literal credential anywhere in the script — argument or marked placeholder only
[ ] no auth response, token, or Authorization header value reaches the report or the journal
```

## 6. Boundaries

| Consumes (read-only) | Produces | Never |
|---|---|---|
| api-docs, the test-execution-manifest (when present), run arguments, `api-verify-config.md` | `test_<mod>_apis.py`, `<mod>_problems_report.md` under `$GOV/modules/<MOD>/backend/test-api/`, the stage-I grant journal | a governance ID of any kind, a change to any line artifact, a gate verdict, a data fix against records it did not create — **one exception, and only one**: the bounded, journalled, self-revoked permission grant of stage I |

## Related Skills

| Skill | Purpose |
|-------|---------|
| [`gov-validate-backend-feature`](../gov-validate-backend-feature/SKILL.md) | Static/architectural validation before this skill's live run |
| [`gov-enforce-backend-contract`](../gov-enforce-backend-contract/SKILL.md) | The 85-rule contract this skill's target code was already required to pass |
