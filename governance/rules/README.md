# `governance/rules/` — what every runtime reads

Governance content that is not scoped to one module: the rules every AI runtime follows
when it works on this platform, whether from this repository or from the frontend's.

| File | What it is | Who reads it |
|---|---|---|
| `GOVERNANCE-RULES.md` | skill routing, execution order, convention precedence, the rules every AI runtime follows | every runtime, this repo and the frontend |
| `api-verify-config.md` | the project's own conventions, which the `api-verify` skill states the procedure for | the `api-verify` skill |

Both were rewritten on 2026-10-07 to the current conventions only: every path points inside
this repository (`governance/…`, `docs/api-docs/<module>/`, `docs/test-api/`) and nothing
refers to any external governance repository or plan generator. The historical versions are in
`governance-shared` @ `1087165` (`platform/rules/`), a read-only reference.
