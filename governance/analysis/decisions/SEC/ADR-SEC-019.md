# ADR-SEC-019 — A service account rejects interactive login and password reset requested on its behalf (POL-SEC-016 reworded)
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:48:15+00:00
Dialogue-key: 1946b1ce610a
traces      : ADR-SEC-016, POL-SEC-016

## Decision
The round-2 text said "if a service account attempts … a self sign-up". An account that already exists can't sign itself up, so that part could never be tested. The policy now covers only requests made *for* an existing service account: interactive login and password reset. The intent of ADR-SEC-016 is unchanged; only the wording is fixed.
traces: POL-SEC-016, ADR-SEC-016

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
