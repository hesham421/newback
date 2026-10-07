# `governance/` — the ERP project's governance

The analysis, plans, packages, API documentation and execution records of the
ERP platform, owned by this project (backend `newback` + frontend `newfront`).
It is a plain folder in this repository: no submodule, no external governance
repository, no plan generator. Everything a session needs to read or write
about a module's governance is here.

## Layout

```
README.md                  this file
modules-registry.json      the live modules (SEC, MDL, CU, FILE, NOTIF), their versions and package paths
rules/                     rules every runtime follows
  GOVERNANCE-RULES.md        skill routing, execution order, convention precedence
  api-verify-config.md       the project's conventions for the api-verify skill
  WORKSPACE.md               what does and does not exist around the repos
  AMEND-P3-O.md              historical amendment record (pre-v6 backend toolset)
  README.md                  index of the above
analysis/
  domain/                  domain-profile.md
  platform/                PROJECT-OVERVIEW.md, project-registry.md, system tests
  modules/<MOD>/           the module's stage artifacts (P0 … P3_2, test_gen, api_verify),
                           _state/, _inputs/, manifest.json; vN/ for a later version
  decisions/<MOD>/         the module's ADRs
backend/modules/<MOD>/     backend track: api-docs/, execution-state.json, packages/, test-api/, testsprite/
frontend/modules/<MOD>/    frontend track: execution-state.json, packages/, tests/
```

Modules: analysis and decisions for SEC, MDL, CU, FILE, NOTIF; backend partitions
for those plus TENANT, AUDIT, SEQUENCE, REPORT (api-docs of the erp-core modules);
frontend partitions for SEC, MDL, CU, FILE, NOTIF, PLATFORM, AUDIT, SEQUENCE, REPORT.

## Who writes what

| Path | Written by | Notes |
|---|---|---|
| `analysis/**` (modules, decisions, domain, platform) | the project owner | Implementation never edits it. A wrong plan is recorded as a gap (below). |
| `rules/**`, `modules-registry.json` | the project owner | |
| `backend/modules/<MOD>/api-docs/` | backend (`newback`) | Generated from the running app (`/generate-api-docs`, or `python docs/api-docs/_tools/generate_all.py` for the erp-core modules). Never hand-edited: an error is a defect in the code, fixed there and regenerated. One copy only: the frontend reads this one. Not version-suffixed. |
| `backend/modules/<MOD>/**` (execution-state.json, test-api/, testsprite/) | backend (`newback`) | `packages/` is read-only during implementation. |
| `frontend/modules/<MOD>/**` (execution-state.json, tests/) | frontend (`newfront`) | The frontend reads this folder from its sibling checkout (`GOV_ROOT`, default `../newback/governance`) and commits its writes here, in this repository. `packages/` is read-only during implementation. |

Reading is shared: either track may read any path here. Writing is by the
table. Nothing mechanical enforces it, so review does.

## Recording a gap

When the analysis or a package is wrong or missing something, the implementing
track does not patch it. It records the finding in its own
`execution-state.json` (`backend/modules/<MOD>/` or `frontend/modules/<MOD>/`):

```jsonc
"api_doc_gaps": [{
  "type": "ABSENT",              // or NAMING_MISMATCH, …
  "phase": "F2", "sub": "F2-SCR-SEC-003",
  "endpoint": "what the plan names that is not there",
  "detail": "what you found, and what you did instead",
  "resolution": "OPEN"           // first word: OPEN / DEFERRED / PENDING · RESOLVED / CLOSED / IMPLEMENTED · HUMAN / ADR
}]
```

The project owner reviews open gaps and answers them in `analysis/`, then the
track marks the entry resolved.

## Origin and what was removed on import

Imported on 2026-10-07 (plan v4, step v4-01) from the former shared-governance
repository at commit `1087165`. Every kept file is byte-identical to that
commit except `modules-registry.json` (FIN and NOTE entries removed). Paths were
re-rooted: `platform/rules/*` → `rules/`, `platform/modules-registry.json` →
`modules-registry.json`.

| Removed | Reason |
|---|---|
| `analysis/modules/{FIN,NOTE}`, `analysis/decisions/{FIN,NOTE}`, `backend/modules/{FIN,NOTE}`, `frontend/modules/{FIN,NOTE}` | FIN is out of the project; NOTE was the factory's sample module. 654 files. |
| FIN and NOTE entries of `modules-registry.json` | Same. |
| `_archive-v5/` | Pre-v6 factory artefacts, loaded by nothing. 43 files. |
| `history/` | Factory history and reports (blueprint, coverage map, pytest evidence). 6 files. |
| `profiles/` | The factory's domain-profile inputs (`erp.yaml`); the rendered result is kept as `analysis/domain/domain-profile.md`. 2 files. |
| `project.yaml` | The factory's project descriptor (profile id, consumer repos). |
| `platform/profile-summary.json` | The factory's consumer contract (paths, tracks, phase lists). Commands now name the fixed paths and phase lists directly. |
| `CODEOWNERS` | Enforced the shared repository's partitions; replaced by the table above. |
| `PROJECT-INSTRUCTIONS.md` | Instructions for running the factory itself; nothing project-specific. |
| the old `README.md` | Described the factory-driven, submodule-mounted repository. Its project content (one api-docs copy, generated never edited, track partitions) is folded into this file. |

Historical files — anything under `analysis/`, `rules/AMEND-P3-O.md`, and
other imported documents — may still mention the factory, `gov.py`,
`profile-summary.json`, a `governance/shared` path or FIN/NOTE. Those are
records of how they were produced, kept verbatim; a `governance/shared/<x>`
path in them now means `governance/<x>` (with `platform/rules/` → `rules/`).
