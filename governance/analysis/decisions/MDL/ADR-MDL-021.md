# ADR-MDL-021 — NEW — G6.** F3's UNIQUE_CHECK sends an **EQUALS** filter on `key` and `code`, but QR-MDL-001 binds `WHERE [key LIKE :key]` and QR-MDL-005 binds `[code LIKE :code]` unconditionally; PHASE 1 and SRS §B2 agree both are LIKE. Typing `PAYMENT` returns `PAYMENT_METHOD` and the blur check falsely reports the key as taken — on the one field that cannot be changed after creation. Inside this pass's writable set.
Status      : RESOLVED-IN-DIALOGUE
Stage       : P3.2        Module: MDL        Version: v1
Lane        : review-pass · round 2 · claude:opus
Decided     : 2026-09-19T12:22:46+00:00
Dialogue-key: 3eb85ad65eaf
traces      : QR-MDL-001, QR-MDL-005

## Decision
(the dialogue recorded the title alone)

Source      : governance-shared/analysis/modules/MDL/_state/briefs/gate-pass-2-round2.response2.md
