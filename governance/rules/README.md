# `platform/rules/` — what every runtime on this platform reads

Governance content that is **not** scoped to one module and **not** scoped to
one repository. It lived in `backend/governance/` and the frontend reached
across the repo boundary to read it — a "sanctioned cross-repo read" its own
README had to name and justify. That justification is what this directory
removes: there is no boundary to cross now.

| File | What it is | Who reads it |
|---|---|---|
| `GOVERNANCE-RULES.md` | skill routing, convention precedence, the rules every AI runtime follows | every repo |
| `api-verify-config.md` | the project's own conventions, which the `api-verify` skill states the procedure for | backend · factory |
| `AMEND-P3-O.md` | the amendment record for the pre-factory backend toolset | historical |
| `WORKSPACE.md` | what does and does not exist above the three repos | every repo |

History for all four stays in the backend repository, where they were written:
`git -C backend log -- governance/<file>`.
