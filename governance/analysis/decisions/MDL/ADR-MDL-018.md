# ADR-MDL-018 — G1's fix needs two things round 1 omitted.** `MDL-400-REORDER-MISMATCH`'s trigger reads "a submitted id does not belong to the type in the path" — reusing it for an incomplete set requires broadening that row, or the code is raised by a condition its own catalog row doesn't describe. And a size check alone misses **duplicate ids**: `[A,A,B]` on a 3-value type passes it and writes A twice. The check must be over the *distinct* submitted set.
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.2        Module: MDL        Version: v1
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-19T12:22:46+00:00
Dialogue-key: cd344b24d697
traces      : —

## Decision
(the dialogue recorded the title alone)

Source      : governance-shared/analysis/modules/MDL/_state/briefs/gate-pass-2-round2.response2.md
