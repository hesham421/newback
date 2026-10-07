# ADR-SEC-046 — The AC-block checker accepts both AC forms rather than rewriting 35 v1 blocks
Status      : ACCEPTED (non-breaking)
Stage       : P1        Module: SEC        Version: v2
Lane        : merge-review-notes (pass-1 revise, finding G4)
Decided     : 2026-09-23
traces      : AC-SEC-001, AC-SEC-035, REQ-SEC-001

## Decision
C5.14's acceptance-criterion check should accept both forms in use: the labelled
`Given :` / `When :` / `Then :` form every v2 AC writes, and the unlabelled prose form every AC carried
from v1 uses. The 35 v1 AC blocks are **not** rewritten to match the newer form.

## Why
The checker recognises only the labelled form, so every v1-carried AC is reported as having no stated
outcome — 51 of this pass's 51 MINOR findings. That noise is not a defect in the module; it hides any
real missing-`Then` defect inside a wall of false positives, which is the opposite of what the clause
is for.

Rewriting 35 committed v1 blocks to satisfy a checker would be a large, behaviour-free edit to frozen
content, and would make the diff of this change set mostly unrelated churn. The checker is the thing
that is wrong.

## Consequence
- The 51 MINOR findings stand in this pass's reports and are understood, not fixed here.
- The checker change belongs to the factory, not to SEC. It is recorded here because this is where the
  cost of it shows, and the module states the finding rather than absorbing it.

Source : pass-1 gate review finding G4 / the C5.14 pattern reported at every SEC stage
