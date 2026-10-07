# Verdict is ESCALATE, not REVISE

Lane   : review-pass · round 2 · claude:opus
Stage  : P3.1        Module: SEC        Version: v2
Written: 2026-09-23T17:41:06+00:00

The brief allows REVISE only while the merge-review-notes lane still has its budget of at most 1 application. The artifacts show that pass-1 has already used it: "revise pass (G1–G7) applied" (registry-exec-be), and "pass-1 REVISE review (RG4, RG6)" (SRS A1, registry-srs, ADR-SEC-046/047). A MAJOR remains, and it was introduced by that revise, so it can't go through REVISE again. If the orchestrator's revise counter shows budget left, every finding below is mechanical and REVISE would apply instead.
