I made round-3 fixes to three of the four files. `platform-summary.md` is unchanged, so its round-2 version stays final. I didn't run `gov.py analyze`; the orchestrator runs it after this stage. I checked that none of my new text contains the markers the no-questions check looks for (`[QUESTION]`, `<!-- OPEN -->`).

I reviewed all seven round-2 decisions again (ADR-SEC-012…018) and accepted each one unchanged. The fixes below are wording gaps I found in the policies. None of them reopens a decision.

**What changed**
- **`business-policies-sec.md`:** four policies fixed (POL-SEC-016, 017, 021 amended; the sign-up rule moved out of 016). Two new policies: POL-SEC-022 and POL-SEC-023. New row #5 in the resolved-decisions table.
- **`module-registry-sec.md`:** POLICIES OWNED now runs to POL-SEC-023. Resolved-decision #3 and one AUTO-decision reference updated to match.
- **`change-manifest.md`:** ADDED list for P0 policies extended to POL-SEC-023. Resolved-decision rows now read 1–5, plus a round-3 note under Dialogue.

---

DECISION: A service account rejects interactive login and password reset requested on its behalf (POL-SEC-016 reworded)
The round-2 text said "if a service account attempts … a self sign-up". An account that already exists can't sign itself up, so that part could never be tested. The policy now covers only requests made *for* an existing service account: interactive login and password reset. The intent of ADR-SEC-016 is unchanged; only the wording is fixed.
traces: POL-SEC-016, ADR-SEC-016

DECISION: A service account is created only through security administration, never through self sign-up (POL-SEC-022 added)
This is the sign-up half moved out of POL-SEC-016 as its own ubiquitous policy. In v1, self sign-up only produces a pending human user (POL-SEC-003). A self-service way to create a machine principal would bypass the administrator. That conflicts with the change set's requirement that the account be administered in the existing screens, and with POL-SEC-013 (no implicit grants).
traces: POL-SEC-022, POL-SEC-013, POL-SEC-003, ADR-SEC-016

DECISION: A service-account credential secret is kept only in a non-reversible form (POL-SEC-023 added)
Resolved-decision #2 and the title of ADR-SEC-016 both say the secret is "stored irreversibly". No policy carried that, so P0.5 would have no id to trace it to. POL-SEC-020 (revealed once) only covers what the system discloses, not what is stored. It is a separate policy because adding it to 020 would give one statement two separate obligations. Source: OWASP Secrets Management Cheat Sheet. It is also the counterpart of v1's secure password storage (security-module-plan-en.md §3).
traces: POL-SEC-023, POL-SEC-020, POL-SEC-004, ADR-SEC-016

DECISION: Deactivating a service account rejects requests from every one of its credentials and every token issued from them (POL-SEC-017 made explicit)
The round-2 text said "that credential", which only works for single-credential revocation. With rotation (POL-SEC-021), an account can hold two live credentials. A literal reading of "deactivate" could then leave the second credential, and tokens issued from it, still accepted. The statement now names the revoked credential, every credential of a deactivated account, and every token issued from any of them. This follows ADR-SEC-014 (no trust window after revocation).
traces: POL-SEC-017, POL-SEC-021, ADR-SEC-014

DECISION: POL-SEC-021 (rotation) is a state-pattern policy, not an optional one
In EARS, "Where" introduces an optional feature and "While" introduces a system state (`factory.yaml → ids.ears.patterns`). "Holds more than one active credential" is a runtime state, so the statement now starts with "While" and the pattern is `state`. The meaning is unchanged.
traces: POL-SEC-021, ADR-SEC-015

---

The dialogue is converged, and no decision is left open for P0.5. The next step is P0.5 for SEC v2, which must cite POL-SEC-012…023. The P0 contracts don't include the `ears` check. Even if a later stage runs it, these policies would only match through an unlabeled `Statement :` line, and the v1 file doesn't have one either.

<!-- CONVERGED -->