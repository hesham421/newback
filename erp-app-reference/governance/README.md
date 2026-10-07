# `erp-app-reference/governance/` — the backend's tools and reports

This directory holds the backend's own tooling and reports. It holds **no
governance artifacts**: those live in the project's governance tree,
`governance/` at the repository root (see `governance/README.md`).

```
erp-app-reference/governance/
  governance-tools/  ← this repo's own tools (api-doc-generator)
  mcp-servers/       ← reference copies of MCP servers wired in .mcp.json
  project-artifacts/ ← this repo's reports and notes (not governance)
  testsprite/        ← TestSprite mechanism, prompts and dated run bundles
```

## Where to read, where to write

- **read** `governance/rules/` and `governance/analysis/modules/<MOD>/` (the
  analysis), `governance/backend/modules/<MOD>/packages/` (the backend packages)
- **write** only `governance/backend/modules/<MOD>/` — `api-docs/` (generated
  from the running app, never hand-edited), `execution-state.json`, `test-api/`
  — never its `packages/`

`governance/frontend/modules/<MOD>/` is written by the frontend repo
(`newfront`); `governance/analysis/` and `governance/rules/` belong to the
project owner.

## What used to be here

Until plan v4 (2026-10-07) `shared/` here was a git submodule of the external
shared-governance repository, driven by a separate governance factory. Both
dependencies were removed: the tree was imported once, cleaned, into the
repo-root `governance/` folder. Older reports under `project-artifacts/` still
describe the submodule and the factory as they were.
