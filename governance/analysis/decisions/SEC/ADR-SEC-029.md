# ADR-SEC-029 — Deactivating a service account belongs to US-SEC-017 alone; US-SEC-013 covers creation and distinguishable visibility
Status      : RESOLVED-IN-DIALOGUE
Stage       : P0.5        Module: SEC        Version: v2
Lane        : analysis-dialogue · round 3 · claude:opus
Decided     : 2026-09-23T12:54:46+00:00
Dialogue-key: d595601c368d
traces      : POL-SEC-017, POL-SEC-018, POL-SEC-022, US-SEC-013, US-SEC-017

## Decision
New in round 3. Before this, US-SEC-013 ("create and deactivate") and US-SEC-017 ("revoke or deactivate the whole account") both named deactivation, which could produce two requirements for one need in P1. Deactivation's effect (POL-SEC-017, ACTIVE/DISABLED only per the registry AUTO-DECISION) now lives in one place. No trace changes.
traces: US-SEC-013, US-SEC-017, POL-SEC-017, POL-SEC-018, POL-SEC-022

Source      : governance-shared/analysis/modules/SEC/v2/_state/briefs/P0.5-round3.response3.md
