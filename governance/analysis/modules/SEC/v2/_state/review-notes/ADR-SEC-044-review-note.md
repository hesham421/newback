# ADR-SEC-044 — G3 is confirmed as a false positive, but I'm changing the fix. Normalising 35 frozen v1 ACs would make them all MODIFIED under C12. The right place for the fix is the `ac-measurable` rule: accept `Then` both with and without a colon. That is a platform finding this pass should have recorded (P4: one rule, 35 symptoms).
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.1        Module: SEC        Version: v2
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-23T15:29:29+00:00
Dialogue-key: 480c9deebb67
traces      : —

## Decision
(the dialogue recorded the title alone)

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/gate-pass-1-round2.response2.md
