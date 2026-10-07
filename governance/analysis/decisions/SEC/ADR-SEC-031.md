# ADR-SEC-031 — The decided consumer role (one action grant, no VIEW) cannot exist under the inherited VIEW gateway
Status      : SUPERSEDED — by ADR-SEC-034 (option A, human decision 2026-09-23)
Stage       : P1        Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
Decided     : 2026-09-23 — resolved as option A; see ADR-SEC-034
traces      : US-SEC-014, POL-SEC-012, POL-SEC-013, REQ-SEC-030, RULE-SEC-007, REQ-SEC-044, REQ-SEC-045, ADR-SEC-018, ADR-SEC-030

## Context
Three closed upstream statements fix the Oracle consumer's role as **FIN module grant + journal-entry
screen grant + create-journal-entry action grant, and no VIEW action grant**:

- business-policies-sec.md → RESOLVED DECISIONS #4: "منح إجراء واحد (إنشاء قيد يومية)؛ منح وحدة FIN وشاشة
  القيد مسار بنيوي … **لا منح VIEW**";
- ADR-SEC-018 → Consequences: "The consumer's role is: FIN module grant + journal-entry screen grant +
  create-journal-entry action grant, and nothing else (**no VIEW grant**). P0.5 acceptance criteria check
  this exact shape";
- US-SEC-014 (approved at prd-approval) → Success metric, as fixed by ADR-SEC-030: one action grant is
  counted, and the module and screen grants are the structural path to it. The story itself says
  "create a journal entry in FIN **and nothing else**".

Four inherited statements make that role impossible:

- REQ-SEC-030 / RULE-SEC-007 (SEC v1): "If a role does not hold the VIEW action grant for a screen, then
  the system shall deny every other action on that screen for that role." The profile states the same rule
  as `profiles/erp.yaml → conventions.security_model.gateway_action: VIEW`, and engine §7.1 reads
  "Gateway: VIEW — without it no other action applies".
- SEC v1 as built enforces the rule **twice**. At grant time, API-SEC-017 runs QR-SEC-030 and returns
  `SEC-409-NO-VIEW-GRANT` for "non-VIEW action grant attempted without VIEW". At request time, the CORE
  interceptor returns `SEC-403-FORBIDDEN` (current backend-execution-plan-sec.md, RULE-SEC-007 block and
  error catalog). An administrator therefore cannot create the decided role. If it existed, its CREATE
  call would be denied anyway.
- FIN v2 relies on the same rule. Its SRS access summary says: "Every action beyond VIEW additionally
  requires VIEW on the same screen (platform gateway …)".
- The change manifest's acceptance 2 and POL-SEC-012 require every SEC v1 invariant to hold for a machine
  principal: "inherited, not reopened".

The P0 and P0.5 dialogues checked "exactly one permission" against the module-first hierarchy (POL-SEC-001,
POL-SEC-002, ADR-SEC-030) but never against the VIEW gateway. As decided, the consumer's first journal-entry
call would fail. That is the silent integration break the change set was raised to prevent.

## Options
| # | Option | What it contradicts | Blast radius |
|---|---|---|---|
| A | Count VIEW as part of the structural path, like the module gate. The consumer's role holds the FIN module grant, the journal-entry screen grant, VIEW and CREATE. "Exactly one permission" counts the single business capability (create a journal entry). | Decision #4 and ADR-SEC-018 ("no VIEW grant"), and the wording of ADR-SEC-030. The daemon can also search and read FIN journal entries, which weakens "nothing else". | P0/P0.5 wording only. No v1 REQ, DBF, API or FIN artifact changes. |
| B | Exempt SERVICE principals from the VIEW gateway. | Acceptance 2, POL-SEC-012, the profile's `gateway_action`, and REQ-SEC-030 / RULE-SEC-007, which SEC P2, P3.1 and the test plans and FIN v2 all rely on. | BREAKING change type: MODIFIED REQ-SEC-030, RULE-SEC-007, QR-SEC-030 and the interceptor. |
| C | Change the platform's gateway convention. | The profile, which is platform-owned and outside this module's say. | Every module. |
| D | FIN registers a dedicated event-intake page whose VIEW exposes no read endpoint. The consumer holds that page's VIEW and CREATE only. | The change manifest scope ("FIN-side deltas … tracked separately"). The FIN v2 change set did not provide this. | A new FIN change set. SEC stays as is. |

## Decision
**None — BLOCKED.** Every option contradicts a closed decision, an approved story, a profile convention or a
requirement other artifacts already rely on. Under §9 of the SRS engine, that makes this a breaking
ambiguity, so this pass stops here.

Recommendation for the human: **A**, unless least privilege on FIN reads matters for this integration, in
which case choose **D**. A keeps every v1 invariant and the profile convention. It amends only the wording of
one dialogue decision, and "one permission" still names the one business capability the manifest asked for.
D is the only option that keeps "nothing else" literally true, but it needs a FIN change set.

## Consequences
- The SRS delta of this pass is written, but the pass is not committed. `gov.py` stops on this ADR
  (BLOCKED check after analyze).
- The delta states service-account authorization only through POL-SEC-012: the same checks as a human
  user (REQ-SEC-044, REQ-SEC-045). No acceptance criterion in it asserts the consumer's exact grant set.
  Neither side of the conflict is decided in the SRS.
- When a human resolves this ADR (new ADR, this one SUPERSEDED):
  - **A** → amend decision #4 and ADR-SEC-018, and add one AC under REQ-SEC-044 stating the role shape
    (module + screen + VIEW + CREATE). The rest of the delta stands.
  - **B** → reissue CS-SEC-001 as BREAKING, then re-run P1 with REQ-SEC-030 and RULE-SEC-007 MODIFIED.
  - **C** → raise a platform finding. P1 waits on the profile.
  - **D** → open a FIN change set. SEC P1 adds one AC naming the intake page's VIEW + CREATE.
