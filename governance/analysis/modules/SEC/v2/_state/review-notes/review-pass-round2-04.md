# Analyze report confirmations kept, with one refinement

Lane   : review-pass · round 2 · claude:opus
Stage  : P3.1        Module: SEC        Version: v2
Written: 2026-09-23T17:41:06+00:00

The 35 C5.14 findings are false positives (ADR-SEC-046). Of the C5.15 findings, ENT-SEC-002 "readd" is partly a false positive: REQ-SEC-073 now states the read. The checker apparently doesn't match "return … fields", and "readd" is a typo in the checker's own output. That belongs to the factory, not SEC. The remaining C5.15 and C5.16 findings are confirmed as v1-carried MINORs. The rejections behind them sit under the PLATFORM-STD umbrella (ADR-SEC-002).
