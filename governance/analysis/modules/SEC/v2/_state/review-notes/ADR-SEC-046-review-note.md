# ADR-SEC-046 — New MINOR findings:
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.1        Module: SEC        Version: v2
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-23T15:29:29+00:00
Dialogue-key: 0c5b378b3c7c
traces      : ADR-SEC-036

## Decision
- **G6 (seam):** v1 tokens still in flight carry no principal-type claim, and the plan doesn't say how they are treated.
- **G7:** the shared DATA_INTEGRITY_VIOLATION code is used but missing from the catalog.
- **G8 (concurrency):** the sign-up race is recorded but has no guard. That is inconsistent with ADR-SEC-036, which closed four other races in v1.

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/gate-pass-1-round2.response2.md
