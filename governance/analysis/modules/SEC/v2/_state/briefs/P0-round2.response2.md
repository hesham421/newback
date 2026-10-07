All artifacts are written. Summary:

- **`v2/P0/platform-summary.md`**, **`module-registry-sec.md`**, **`business-policies-sec.md`** — round-2 final versions, all 10 new POL-SEC-012…021 records, SERVICE principal type, ServiceAccountCredential entity.
- **`v2/change-manifest.md`** — `## Per artifact` filled in, plus a dialogue note on the round-2 amendment.
- **`ADR-SEC-012` through `ADR-SEC-018`** — the seven dialogue decisions persisted as RESOLVED-IN-DIALOGUE ADRs.

One substantive change from round 1: POL-SEC-017 (revocation) now explicitly covers requests authenticated by a previously issued access token, not just the raw credential secret — closing a gap where a token issued moments before revocation would otherwise keep working until its own expiry under the OAuth client-credentials flow (ADR-SEC-014). The other six round-1 proposals were accepted unchanged. Dialogue is converged; next step per the brief is P0.5 for SEC v2.