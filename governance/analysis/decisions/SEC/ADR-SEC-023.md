# ADR-SEC-023 — POL-SEC-021 (rotation) is a state-pattern policy, not an optional one
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:48:15+00:00
Dialogue-key: 823011b8e526
traces      : ADR-SEC-015, POL-SEC-021

## Decision
In EARS, "Where" introduces an optional feature and "While" introduces a system state (`factory.yaml → ids.ears.patterns`). "Holds more than one active credential" is a runtime state, so the statement now starts with "While" and the pattern is `state`. The meaning is unchanged.
traces: POL-SEC-021, ADR-SEC-015

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
