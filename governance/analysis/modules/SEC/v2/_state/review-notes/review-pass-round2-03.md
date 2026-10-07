# Three stale "six SERVICE_*" counts, filed as one MINOR (P4 pattern)

Lane   : review-pass · round 2 · claude:opus
Stage  : P3.1        Module: SEC        Version: v2
Written: 2026-09-23T17:41:06+00:00

Revise G6 raised the count to seven, but "six" survives in three places: API-SEC-023's Request line, the db-script BLOCK 8 comment ("the six new AUDIT_EVENT_TYPE codes") and the module-registry AUTO-DECISION, which lists six. The plan's QR id index also still says QR-SEC-039 has "no API", though ADR-SEC-040 gave it API-SEC-038. The root cause is that the revise lane didn't sweep derived counts and cross-reference lines.
