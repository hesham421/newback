# ADR-SEC-020 — A service account is created only through security administration, never through self sign-up (POL-SEC-022 added)
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:48:15+00:00
Dialogue-key: 2fc36faf54bc
traces      : ADR-SEC-016, POL-SEC-003, POL-SEC-013, POL-SEC-016, POL-SEC-022

## Decision
This is the sign-up half moved out of POL-SEC-016 as its own ubiquitous policy. In v1, self sign-up only produces a pending human user (POL-SEC-003). A self-service way to create a machine principal would bypass the administrator. That conflicts with the change set's requirement that the account be administered in the existing screens, and with POL-SEC-013 (no implicit grants).
traces: POL-SEC-022, POL-SEC-013, POL-SEC-003, ADR-SEC-016

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
