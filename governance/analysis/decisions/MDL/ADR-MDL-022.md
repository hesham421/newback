# ADR-MDL-022 — NEW — G7.** F1/F2 model `sortField` as a free variable and key the cache on it, but QR-MDL-001 declares one ordering (`ORDER BY key`) and PHASE 1 says "the module offers no free-form sort parameter". This is the pass's own G2 principle — "keying a variation the server cannot produce would fragment the cache for nothing" — not applied to itself. `sortDirection` survives; `sortField` does not.
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.2        Module: MDL        Version: v1
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-19T12:22:46+00:00
Dialogue-key: 57f2e22488e5
traces      : QR-MDL-001

## Decision
(the dialogue recorded the title alone)

Source      : governance-shared/analysis/modules/MDL/_state/briefs/gate-pass-2-round2.response2.md
