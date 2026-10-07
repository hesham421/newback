# G1 (US-SEC-016 matrix row) confirmed, downgraded to MINOR, fix corrected

Lane   : review-pass · round 2 · claude:opus
Stage  : P3.1        Module: SEC        Version: v2
Written: 2026-09-23T17:41:06+00:00

The row really does omit REQ-SEC-078 / AC-SEC-084. It is MINOR, not MAJOR: REQ-SEC-078's own `Traces:` line names US-SEC-016, and RULE-SEC-013 traces back to it, so no id is actually orphaned. What's wrong is the STANDALONE matrix, a derived view that didn't get the update. Round 1's fix also had an error: the row's RULE column isn't blank, it already holds RULE-SEC-008. RULE-SEC-013 has to be added next to it.
