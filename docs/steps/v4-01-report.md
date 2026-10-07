# v4-01 — governance inside newback

## Summary

The project's governance now lives in this repository as a plain folder, `governance/`. It was imported once
from the former shared-governance repository at `1087165` and cleaned of FIN, NOTE and the factory machinery.
The `erp-app-reference/governance/shared` submodule, `.gitmodules`, `.governance-scope`,
`erp-app-reference/scripts/governance` and the `governance-shared.yml` workflow are removed. Every live
reference in `.claude/**`, `erp-app-reference/CLAUDE.md`, `INSTALL.md`, `.mcp.json`, `docs/api-docs/**` and
the api-doc-generator now points at `governance/`. The source repository was not modified or pushed.

## Files changed

| Area | Count |
|---|---|
| `governance/` added | 879 files: 877 byte-identical to `1087165`, `modules-registry.json` (FIN and NOTE removed), new `README.md` |
| Not imported from `1087165` | 710 files (table below) |
| `.claude/commands/FIN/**` deleted | 4 |
| `.claude/commands/**` edited | 8 |
| `.claude/skills/**` edited | 3 |
| Removed submodule/factory plumbing | 5 (`.gitmodules`, `.governance-scope`, `.github/workflows/governance-shared.yml`, `erp-app-reference/scripts/governance`, gitlink `erp-app-reference/governance/shared`) |
| Docs and config edited | 5 (`INSTALL.md`, `.mcp.json`, `erp-app-reference/CLAUDE.md`, `erp-app-reference/governance/README.md`, `docs/api-docs/README.md`) |
| Tooling edited | 4 (`docs/api-docs/_tools/generate_all.py`, api-doc-generator `discovery.py`, `checks.py`, `README.md`) |
| This report | 1 |

Kept by area: `analysis/modules` 334, `analysis/decisions` 96, `analysis/platform` 4, `analysis/domain` 1,
`backend/modules` 265, `frontend/modules` 170, `rules` 6 (5 `.md` + `.gitkeep`), `.gitignore` 1.

Not imported, by area: `analysis/modules` 232 (FIN 179, NOTE 53), `analysis/decisions` 77 (FIN 70, NOTE 7),
`backend/modules` 113 (FIN 103, NOTE 10), `frontend/modules` 232 (FIN 224, NOTE 8), `_archive-v5` 43,
`history` 6, `profiles` 2, `project.yaml`, `CODEOWNERS`, `PROJECT-INSTRUCTIONS.md`, `README.md`,
`platform/profile-summary.json`.

## Decisions

- **`PROJECT-INSTRUCTIONS.md` dropped.** It contains only instructions for operating the factory. Nothing in it
  is about the project.
- **Old `README.md` dropped and folded in.** The project content (one api-docs copy, generated and never
  edited, track partitions) is in the new `governance/README.md`. The factory, submodule and pinning content is
  not carried over.
- **`governance/.gitignore` kept**, unchanged. It ignores derived `_state` caches and receipts.
- **`rules/` kept verbatim**, including `AMEND-P3-O.md` (historical). The README notes that a
  `governance/shared/<x>` path in imported files now means `governance/<x>`.
- **`rules/WORKSPACE.md` is stored with CRLF.** That is how the source blob stores it. It was added with
  `core.autocrlf=false` so the index blob matches `1087165` (`0779822`).
- **FIN commands deleted:** `.claude/commands/FIN/{execute-backend,execute-backend-test}.md` and
  `FIN/v2/{execute-backend,execute-backend-test}.md`.
- **Factory steps rewritten, none deleted outright:**
  - `generate-module-setup.md`: the precondition (submodule plus `profile-summary.json`) now checks for
    `governance/README.md`. Step 0 names the fixed paths and the phase lists. The lists are
    `EXEC_PHASES="CORE DATA-DOM SVC-API DOC INT-C INT-R SEC-BE ALIGN-BE"` and a test-phase table
    (`TEST-PLAN-BE`: `RULE-SCENARIOS`, `API-SCENARIOS`; `INT-XM` integration), copied from
    `profile-summary.json@1087165`. The `.governance-scope`/`CODEOWNERS` paragraph and the submodule push
    in 0.3 are replaced with a plain commit in this repo.
  - `orchestrate-module.md`: Step 0 uses fixed paths. `EXEC_PHASES` is a fixed list, with the
    module's `execution-state.json` as the per-module authority.
  - `generate-api-docs.md`: Step 0 uses fixed paths. STEP 4 "publish to the submodule" became a plain commit
    in this repo. The FAIL-table owner "factory" became "analysis owner". A note was added that erp-core
    modules are generated through `docs/api-docs/_tools/generate_all.py`.
  - `erp-core/orchestrate-plan.md`: step 13 (COMPLETE) is marked not re-runnable. Its
    `ERP_FACTORY_DIR`/`ERP_GOV_SHARED_DIR` requirement is gone. `INSTALL.md` matches.
    `erp-core/generate-plan-setup.md` is reworded so it no longer names the submodule.
- **Path mapping applied to live files:**
  - `governance/shared/platform/rules/` → `governance/rules/`
  - `governance/shared/platform/modules-registry.json` → `governance/modules-registry.json`
  - `governance/shared/` → `governance/`
  - `$GOV/modules/<MOD>/backend/…` → `governance/backend/modules/<MOD>/…`
  - `$GOV/modules/<MOD>/` (analysis) → `governance/analysis/modules/<MOD>/`
- **Stale tool paths fixed.** Bare `governance/{governance-tools,testsprite,project-artifacts,mcp-servers}`
  became `erp-app-reference/governance/…`. These paths were already wrong after step 03 moved the app. With a
  repo-root `governance/` they would now resolve to the wrong tree. This fixes the `.mcp.json` server paths
  too.
- **api-doc generator.**
  - `discovery.py` no longer reads `profile-summary.json`. It walks up to the first `governance/` holding
    `analysis/modules/`. Output goes to `governance/backend/modules/<MOD>/api-docs`, plans are read from
    `governance/analysis/modules/<MOD>`, and the unused `import json` is gone.
  - `generate_all.py` mirrors every module except `app/` into `governance/backend/modules/<MOD>/api-docs/`
    after `generate`/`update`. `--no-governance` skips the mirror. The two trees were identical at import
    (`diff -rq` clean for all 9 modules).
- **`erp-app-reference/CLAUDE.md`.** The governance sections and the ownership table are rewritten for the
  in-repo tree. Every path is now stated relative to the repository root. The `governance-shared/`
  placeholder rows and the `gov.py publish` text are removed. The other sections are unchanged.
- **`erp-app-reference/governance/`** (tools, MCP copies, TestSprite, reports) stays where it is. Its README
  now says it holds no governance artifacts.
- **Historical, not updated:**
  - `erp-core-plan/**`, `docs/steps/*-report.md`, `docs/DEVIATIONS.md`
  - `erp-app-reference/governance/project-artifacts/**`, `erp-app-reference/governance/testsprite/runs/**`
  - everything imported under `governance/` except `README.md` and `modules-registry.json`

## Verification

Checksum (`git ls-files -s governance` blob ids vs `git ls-tree -r 1087165`, plus a working-tree sha256
check):
```
source files @1087165 : 1588
kept byte-identical   : 877
removed               : 710 (+ modules-registry.json transformed: 1)
new                   : 1 (governance/README.md)
governance/ in index  : 879
unexpected in index   : []
missing from index    : []
problems              : 0
```

`ls governance/analysis/modules governance/backend/modules governance/frontend/modules`:
```
analysis/modules : CU FILE MDL NOTIF SEC          (analysis/decisions: CU FILE MDL NOTIF SEC)
backend/modules  : AUDIT CU FILE MDL NOTIF REPORT SEC SEQUENCE TENANT
frontend/modules : AUDIT CU FILE MDL NOTIF PLATFORM REPORT SEC SEQUENCE
```
`modules-registry.json` modules: `CU NOTIF FILE SEC MDL`. It is valid JSON.

`git grep -nE "governance/shared|governance-shared|gov-module|gov\.py|profile-summary"` outside `governance/`
matches only these historical files:
- `docs/DEVIATIONS.md` (3), `docs/steps/03-report.md` (2), `docs/steps/12-report.md` (1)
- `erp-core-plan/13-STEP-factory-registration-as-given.md` (4), `erp-core-plan/execution-state.json` (7)
- 23 files under `erp-app-reference/governance/project-artifacts/**`, all reports, handovers, prompts and
  seed scripts dated 2026-09

Inside `governance/`, the matches are in 239 imported files plus the import note in `governance/README.md`.

Build: `JAVA_HOME=…/jdk-25 mvn -q -DskipTests package` → exit 0.
api-doc-generator unit tests: 58 passed, 1 failed
(`test_security_extractor.py::CommentsAreNotCode::test_comment_with_public_between_mapping_and_method`). That
extractor is unrelated to discovery and this step did not touch it. No baseline was captured on the same
checkout.

## Notes for v4-02 (newfront)

- Root: `GOV_ROOT` defaults to `../newback/governance` (the sibling checkout of this repo).
- Read:
  - `$GOV_ROOT/rules/GOVERNANCE-RULES.md`, `$GOV_ROOT/rules/api-verify-config.md`
  - `$GOV_ROOT/modules-registry.json`; the delivery paths inside it are relative to `$GOV_ROOT`, e.g.
    `frontend/modules/SEC/packages/frontend-execution`
  - `$GOV_ROOT/analysis/modules/<MOD>/` (P3_2 frontend plan, ui-ux-spec, `test_gen/`, `manifest.json`;
    `vN/` for later versions, e.g. `SEC/v2/`)
  - `$GOV_ROOT/analysis/decisions/<MOD>/`
  - `$GOV_ROOT/backend/modules/<MOD>/api-docs/` (`index.md`, `endpoints/*.md`), which is the Prism/mock
    input
  - `$GOV_ROOT/frontend/modules/<MOD>/packages/`
- Write (as a commit in newback):
  - `$GOV_ROOT/frontend/modules/<MOD>/execution-state.json`, including `api_doc_gaps[]`
  - `$GOV_ROOT/frontend/modules/<MOD>/tests/` (Playwright specs, POMs, reports)
  - existing extras such as `frontend/modules/SEC/frontend-test/`
- The old `$GOV=governance/shared/<profile>` and `gov-module.py` lookups map directly:
  - `paths.modules` → `analysis/modules`
  - `paths.decisions` → `analysis/decisions`
  - `paths.platform` → `analysis/platform`
  - `tracks.frontend.partition` → `frontend/modules/{MOD}`
  - `platform/rules` → `rules`
  - `platform/modules-registry.json` → `modules-registry.json`
- No `profile-summary.json` exists anymore. The frontend phase lists must be named in the newfront commands.
- The FIN and NOTE trees are gone from governance. Any FIN command or skill in newfront has nothing to read.
