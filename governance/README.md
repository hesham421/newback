# `governance/` — this project's own governance, self-contained

Everything the ERP platform's AI-assisted work needs — for the backend in this repository
and for the frontend that consumes it — lives in this folder. There is **no governance
submodule and no governance factory**: nothing here is pulled from, pinned to or published
back to another repository, and nothing here describes anything but the current logic
(erp-core 1.2.0 as implemented, plus the analysis it was implemented from).

Origin: the shared governance repository `governance-shared` at `main` @
`1087165c607ee8bac8c1c9be2598dd9415a2244d` was imported raw into this repository on
2026-10-07 (commit `b8ce7fa`, 879 files), then pruned to the documents below. That repository
is kept untouched as a **read-only historical reference**; nothing in this repository reads
it. `docs/governance-vendoring-report.md` lists every file kept, rewritten and dropped.

## Layout

```
governance/
  README.md                 this file
  rules/                    GOVERNANCE-RULES.md (skill routing, execution order, convention
                            precedence) · api-verify-config.md (api-verify run conventions)
  analysis/
    platform/               PROJECT-OVERVIEW.md (the platform as implemented) ·
                            project-registry.md (live modules, screens, contract, test suites)
    domain/                 domain-profile.md (the original domain analysis, verbatim)
    decisions/<MOD>/        the ADRs that still describe the current code (SEC 13, MDL 12,
                            TENANT 1)
    modules/<MOD>/          P0 (policies, module registry, platform summary) · P0_5 (PRD) ·
                            P1 (SRS + registry) · P2 (DB script + registry) · P2_5 (UI/UX spec,
                            flow diagram — FILE, NOTIF, MDL) — verbatim, for SEC, MDL, CU, FILE, NOTIF;
                            each P0–P2 artifact ends with "Implementation Addendum — erp-core 1.2.0";
                            TENANT (P0–P2) is not verbatim: an as-built baseline written from the
                            erp-core 1.2.0 code on 2026-10-07 (no 1.2.0 addendum; later changes
                            are "Implementation Addendum — erp-core 1.3.0" sections on it);
                            SEC also carries implementation-notes.md (cited by erp-core Javadoc)
  backend/modules/<MOD>/test-api/   the adapted legacy API suites (test_<mod>_apis.py +
                                    <mod>_problems_report.md) for SEC, MDL, CU, FILE, NOTIF
  frontend/modules/<MOD>/tests/     the frontend's E2E spec archives (specs, page objects, run
                                    reports) for AUDIT, CU, FILE, MDL, NOTIF, PLATFORM, REPORT,
                                    SEC, SEQUENCE
                                    (the frontend repo's write set here is this folder plus the
                                    append-only "Implementation Addendum — frontend <version>"
                                    sections of analysis/modules/<MOD>/P2_5/ — see the root
                                    CLAUDE.md "Analysis first")
  tools/api-doc-generator/  the generator behind docs/api-docs (driven by docs/api-docs/_tools/)
```

Module codes: analysis folders are upper-case (`SEC`); the generated API contract uses the
lower-case package names of the running app (`sec`, `tenant`, `file`, `notif`, `mdl`, `cu`,
`sequence`, `audit`, `report`, `app`).

## Documentation map — which document is the current reference for what

| Question | Current reference |
|---|---|
| The API contract (endpoints, DTOs, envelopes, error codes, messages) | `docs/api-docs/<module>/` — generated from the running reference app; never hand-edited; the only copy |
| Behaviour and its rationale per module | the "Implementation Addendum — erp-core 1.2.0" sections of `governance/analysis/modules/<MOD>/{P0,P0_5,P1,P2}` (what was built on top of the analysis; for TENANT the files themselves, an as-built baseline), the ADRs under `governance/analysis/decisions/<MOD>/`, and `docs/DEVIATIONS.md` (every deviation from the plan, by step) |
| The platform as a whole (packages, tenancy, realms, versions) | `governance/analysis/platform/PROJECT-OVERVIEW.md` |
| Which modules, screens, page codes and test suites exist | `governance/analysis/platform/project-registry.md` |
| How to consume and configure erp-core in an application | `docs/CONSUMING.md` |
| Release and compatibility policy, what changed per version | `docs/RELEASE.md`, `docs/CHANGELOG.md` |
| Implementation history, step by step | `docs/steps/NN-report.md` and `erp-core-plan/` (completed; records, not instructions) |
| Coding standards, skill routing, convention precedence | `governance/rules/GOVERNANCE-RULES.md` + `.claude/skills/` |
| Verifying the API over HTTP | `governance/rules/api-verify-config.md`; the core suite and its results in `docs/test-api/`; the legacy adapted suites under `governance/backend/modules/<MOD>/test-api/`; the frontend's E2E archive under `governance/frontend/modules/<MOD>/tests/` |
| The original analysis (before implementation) | `governance/analysis/modules/<MOD>/` bodies above their addenda, `governance/analysis/domain/domain-profile.md` |

## The API contract is not here

The API documentation of every module is generated from the running reference app and
lives in **`../docs/api-docs/<module>/`** — the single source, read by the frontend and by
the `api-verify` skill. No copy is kept under `governance/`. The raw import carried
`backend/modules/<MOD>/api-docs/` for nine modules; `diff -r` against `docs/api-docs/<module>/`
found them identical, so they were removed rather than kept in step by hand.

## Which SEC analysis is current

`analysis/modules/SEC/` (v1) is the current analysis: its P0–P2 artifacts carry the
"Implementation Addendum — erp-core 1.2.0" sections recording what erp-core actually
implemented (tag `v1.2.0`), as every other module's do. The service-account change set
(CS-SEC-001, analysed as SEC v2) was never implemented; its five G5 endpoint declarations
(`GET /sec/users/{id}`, `GET /sec/roles/{id}`, `PUT /sec/roles/{id}`, `GET /sec/roles/{id}/grants`,
`POST /sec/signup-requests/search`) are the as-built endpoints recorded in ADR-SEC-038 and
`docs/api-docs/sec/`, and are listed in the SEC P1 addendum. The rest of v2 and its ADRs are not
vendored (`governance-shared` @ `1087165`, `analysis/modules/SEC/v2/`,
`analysis/decisions/SEC/ADR-SEC-012…034`).
`analysis/modules/SEC/implementation-notes.md` is the SEC implementation-notes record ten
erp-core classes cite (historical paths inside; decisions current as of 1.2.0).

MDL's UI/UX specification (`ui-ux-spec-mdl.md`, `flow-diagram-mdl.md`) was produced under
`P3_2/` instead of `P2_5/`; it is kept under `analysis/modules/MDL/P2_5/` so the three modules
that have a UI/UX spec (FILE, NOTIF, MDL) keep it in the same place.

## What was deliberately left out (reference: governance-shared @1087165)

- **FIN and NOTE everywhere** — FIN was removed from erp-core in plan step 01; NOTE was the
  generator's sample module.
- **Every plan-generator artifact** — per-module `P3_1`, `P3_2`, `P3_5_BE`, `_inputs`, `_state`,
  `api_verify`, `test_gen`, `frontend-test`, `manifest.json`; `backend/modules/*/{packages,
  testsprite, execution-state.json, api-docs}`; `frontend/modules/*/{packages, frontend-test,
  execution-state.json}`; `modules-registry.json`; `rules/{AMEND-P3-O.md, WORKSPACE.md}`; the
  generator's own repository files.
- **Pre-erp-core platform documents** — the former `PROJECT-OVERVIEW.md` and
  `project-registry.md` (rewritten from the current code), the system-test rollups (old
  monolith flows), the whole of `erp-app-reference/governance/project-artifacts/` (handovers,
  test reports, generator fix prompts and integration notes written against the monolith; FIN
  and Oracle items), `testsprite/`, `mcp-servers/`.
- **ADRs contradicted by or unrelated to the current code** — the SEC v2 service-account
  decisions, the generator's id-binding and review-round records, and the decisions the code contradicts
  (see the report §3).

## Historical wording inside verbatim files

The analysis, ADRs, legacy suites and E2E archives are kept verbatim, so a few of them still
mention the plan generator, the former submodule path, FIN permission codes or the legacy
Oracle system in their prose. Those mentions are history, not dependencies: nothing in this
repository resolves them. The live files — `CLAUDE.md`, `.claude/`, `.mcp.json`, `rules/`,
`tools/`, `docs/api-docs/_tools/`, the build and CI files — carry none.
