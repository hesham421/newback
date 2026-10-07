# SYSTEM TEST — FULL CROSS-MODULE FLOW — ERP Platform (SEC → MDL → FIN)
══════════════════════════════════════════════════════════════════
Status  : convenience document, NOT a governed test-gen artifact — it mints no `TC-*`
          id, owns no atom, is not split/verified/delivered, and is not read by any
          gate. Every scenario below is assembled from `TC-*` ids that already exist in
          the three committed backend test plans; nothing here is invented.
Purpose : `system-test-index-erp.md` (the governed platform rollup) deliberately "never
          restates TC content — every row is an ID reference" (engine §9). This document
          is the missing narrative: what actually has to work, end to end, for the
          platform to be usable, walking real user/system journeys across SEC, MDL and FIN.
Sources : backend-test-plan-{sec,mdl,fin}.md v1 · system-test-index-erp.md
══════════════════════════════════════════════════════════════════

## Flow 1 — Platform bootstrap (module & lookup onboarding)
The order these three modules were actually analyzed and must be deployed in.

1. SEC deploys first — it has no dependency on anything (ROOT).
2. MDL deploys second and, at its own first write, validates its owner-module references
   against SEC — **TC-MDL-014** (graceful degradation if SEC is unreachable at that
   moment; the happy path is implicit in every other MDL TC that names a real owner
   module, e.g. **TC-MDL-001**).
3. FIN deploys third:
   - registers its module + 12 screens + every action into SEC — **TC-FIN-044**.
   - registers its 13 lookup types into MDL — **TC-FIN-045**.
   - from this point on, every FIN write that touches a lookup-backed field validates
     against MDL — **TC-FIN-047** covers the degraded case (MDL unreachable).

**Pass/fail for this flow**: all of TC-MDL-014, TC-FIN-044, TC-FIN-045, TC-FIN-047 pass,
in this order, against a freshly provisioned environment (SEC empty except its own
schema, then MDL, then FIN).

## Flow 2 — Identity, RBAC and first FIN access
Ties SEC's whole authorization model to FIN actually becoming reachable.

1. A prospective controller signs up — **TC-SEC-003**.
2. A security administrator approves them — **TC-SEC-004** — a real `User` now exists.
3. The administrator creates the two roles FIN's own SoD model requires: `FIN Accountant`
   and `FIN Controller` — **TC-SEC-016**-pattern role creation (via API-SEC-013, not its
   own TC — role creation itself is TC-SEC-012's grant, the role row is a precondition).
4. The administrator grants `FIN Accountant` the FIN module, the Journal Entries screen,
   and CREATE — **TC-SEC-012**, **TC-SEC-013** (would-be violation if attempted out of
   order), **TC-SEC-014** (would-be violation if attempted out of order), **TC-SEC-030**
   (VIEW must exist before CREATE takes effect).
5. The administrator grants `FIN Controller` the FIN module, the Periods screen, and the
   custom `PERM_FIN_PERIODS_CLOSE_APPROVE` action only — never `PERM_FIN_JOURNAL_ENTRIES_CREATE`
   — this is the precondition Flow 4 depends on.
6. The accountant logs in — **TC-SEC-001** — and their menu shows exactly FIN → Journal
   Entries, nothing else — **TC-SEC-021**, **TC-SEC-032** (every other module/screen absent).
7. The accountant tries to open the Periods screen directly by URL anyway — denied
   regardless of the menu — **TC-SEC-033**.

**Pass/fail for this flow**: TC-SEC-001 through the direct-URL denial in step 7 all pass,
and the accountant's session never sees FIN_PERIODS or FIN_ALLOCATION_RULES anywhere.

## Flow 3 — Chart setup → event-driven posting → live reporting
The actual accounting spine, from configuration to a number on a report.

1. Set up 2 accounts (one ASSET leaf, one REVENUE leaf) and 1 dimension with 2 values —
   **TC-FIN-001**, **TC-FIN-004**, **TC-FIN-005**.
2. Configure one event-type rule with a 33%/33%/remainder distribution — **TC-FIN-007**,
   **TC-FIN-008**, and the remainder-count guard — **TC-FIN-009**.
3. A canonical event of that type arrives — the engine builds the entry from the rule —
   **TC-FIN-010**, with the remainder line computed exactly — **TC-FIN-012**.
4. The same event reference is replayed (simulating an at-least-once delivery from the
   out-of-scope Event consumer) — rejected as a duplicate, not double-posted —
   **TC-FIN-011** — this is the platform's actual idempotency guarantee under test, not
   just a unit-level assertion.
5. Validation runs and the entry posts directly, no approval step — **TC-FIN-017** — and
   is immediately locked — **TC-FIN-016**.
6. The accountant opens the account ledger for the ASSET account — the new posting is
   visible immediately, computed live — **TC-FIN-039**.
7. The controller opens the trial balance — it balances exactly, by construction —
   **TC-FIN-040**.
8. The controller drills from a balance-sheet line down to this exact entry and its
   event reference — **TC-FIN-046**.

**Pass/fail for this flow**: every TC above passes in sequence against the SAME entities
created in steps 1–2 (not fresh fixtures per step) — this is what actually proves the
pieces compose, not just that each endpoint works in isolation.

## Flow 4 — Segregation of duties at period close (the platform's one real cross-cutting security invariant)
The scenario that most depends on SEC, FIN and the role setup from Flow 2 all being correct together.

1. The accountant (holds `PERM_FIN_JOURNAL_ENTRIES_CREATE` only, from Flow 2) posts
   several entries during the month via Flow 3's mechanics.
2. The accountant attempts to hard-close the period themselves — denied, they hold no
   `PERM_FIN_PERIODS_CLOSE_APPROVE` — **TC-SEC-033**-style denial at the SEC layer,
   surfacing as **TC-FIN-038**'s `FIN-403-SOD-VIOLATION` at the FIN layer.
3. The controller (holds `PERM_FIN_PERIODS_CLOSE_APPROVE` only, never the creation
   permission — this exclusivity was set up in Flow 2 step 5) hard-closes the period —
   **TC-FIN-034**, **TC-FIN-037** (approval recorded distinctly from every entry's
   `createdBy` in that period).
4. A late entry attempt against the now-closed period is rejected even though it was
   Open when the accountant started filling the form — **TC-FIN-020**.
5. The controller runs year-end close once every period of the year is hard-closed —
   **TC-FIN-036** — and immediately checks the new year's balance sheet (opening
   balances match the prior year's closing, **TC-FIN-041**) and income statement (opens
   at zero, **TC-FIN-042**).

**Pass/fail for this flow**: this is the highest-value regression to protect — if SEC's
role model, FIN's SoD check (`RULE-FIN-015`), or the year-end arithmetic (`RULE-FIN-010`
reused, `POL-FIN-010`) ever drift apart, this is the flow that will catch it before a
narrower per-module test does.

## Flow 5 — Correction without loss (reversal, not deletion)
1. Post a 3-line manual entry — **TC-FIN-014**.
2. Attempt to delete it directly — no route exists, it is rejected — **TC-FIN-016**.
3. Reverse it instead — a new, linked, exactly-mirrored entry posts — **TC-FIN-028**.
4. Attempt to reverse the (now-VOID) original a second time — rejected — **TC-FIN-030**.
5. Reverse an entry whose own period has since been hard-closed — the reversal lands in
   the current open period instead, not the closed one — **TC-FIN-029**.

## Flow 6 — Degraded-dependency resilience
Both of the platform's only two cross-module reads must fail *safely*, never opaquely.

1. With MDL made unreachable, attempt to create a FIN account (needs `ACCOUNT_TYPE`
   validation) — a defined `FIN-503`, not a stack trace — **TC-FIN-047**.
2. With SEC made unreachable, attempt to register a new MDL lookup type (needs owner-module
   validation) — a defined error, not a stack trace — **TC-MDL-014**.
3. (Documented limitation, not a test failure) — SEC itself has no upstream dependency to
   degrade against; if SEC is down, nothing in the platform authenticates, which is the
   expected, by-design single point of trust for identity — this is not a gap to test
   around, it is the architecture (domain-profile.md §6: every module depends HARD on SEC).

══════════════════════════════════════════════════════════════════
Every `TC-*` referenced above already exists, verbatim, in its module's committed
`backend-test-plan-*.md`. This document adds no new test obligation — it only shows the
order that makes the existing ones tell the platform's actual story.
