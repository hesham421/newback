# ADR-SEC-022 — Deactivating a service account rejects requests from every one of its credentials and every token issued from them (POL-SEC-017 made explicit)
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:48:15+00:00
Dialogue-key: b35fb03141a2
traces      : ADR-SEC-014, POL-SEC-017, POL-SEC-021

## Decision
The round-2 text said "that credential", which only works for single-credential revocation. With rotation (POL-SEC-021), an account can hold two live credentials. A literal reading of "deactivate" could then leave the second credential, and tokens issued from it, still accepted. The statement now names the revoked credential, every credential of a deactivated account, and every token issued from any of them. This follows ADR-SEC-014 (no trust window after revocation).
traces: POL-SEC-017, POL-SEC-021, ADR-SEC-014

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
