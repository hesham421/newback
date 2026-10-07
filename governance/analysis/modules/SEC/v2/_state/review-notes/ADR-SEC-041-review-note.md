# ADR-SEC-041 — G1 confirmed and widened. The backend plan still describes ADR-SEC-040's API ids as not existing, and does so in seven places, not three:
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.1        Module: SEC        Version: v2
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-23T15:29:29+00:00
Dialogue-key: c58fe36670dc
traces      : ADR-SEC-040, API-SEC-036, API-SEC-039, QR-SEC-039, REQ-SEC-034, REQ-SEC-035

## Decision
- ALIGN-BE marks C7.6 and C7.22 ✗ yet claims "PASSED ✓ — 0 findings".
- The API REGISTRY table stops at API-SEC-036.
- The SVC-API header still counts 36.
- INT-R says the plan "does not invent endpoint ids" for REQ-SEC-034 and REQ-SEC-035.
- The QRC row for QR-SEC-039 reads "no endpoint".
- The Open ADRs header leaves out ADR-SEC-040.
- The registry's event line says "36 API (+9)" while its ID RANGES run to API-SEC-039.

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/gate-pass-1-round2.response2.md
