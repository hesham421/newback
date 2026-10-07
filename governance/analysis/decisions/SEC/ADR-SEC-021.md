# ADR-SEC-021 — A service-account credential secret is kept only in a non-reversible form (POL-SEC-023 added)
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:48:15+00:00
Dialogue-key: 2ceb43ab5804
traces      : ADR-SEC-016, POL-SEC-004, POL-SEC-020, POL-SEC-023

## Decision
Resolved-decision #2 and the title of ADR-SEC-016 both say the secret is "stored irreversibly". No policy carried that, so P0.5 would have no id to trace it to. POL-SEC-020 (revealed once) only covers what the system discloses, not what is stored. It is a separate policy because adding it to 020 would give one statement two separate obligations. Source: OWASP Secrets Management Cheat Sheet. It is also the counterpart of v1's secure password storage (security-module-plan-en.md §3).
traces: POL-SEC-023, POL-SEC-020, POL-SEC-004, ADR-SEC-016

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
