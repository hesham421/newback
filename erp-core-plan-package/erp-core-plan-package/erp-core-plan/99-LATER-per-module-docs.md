# Step 99 (later phase — NOT part of the current execution) — Per-module documentation

**When:** after step 12 is released (and ideally after step 13), as a separate run.
**Goal:** one document per core module, generated from the final code, that serves three readers: a developer integrating with the module, the Governance Factory (as a knowledge input / "given" contract), and a reviewer assessing the module's analysis.

## Output
`erp-core/docs/modules/<module>.md` for each of: `common, cu, mdl, sec, file, notif, tenant, audit, events, sequence, report, autoconfigure` + `docs/modules/README.md` index.

## Fixed template (every file, same headings, English)
1. **Purpose** — what the module is and is not (2–4 sentences).
2. **Concepts & entities** — table per entity: table name, key columns, tenant-scoped (Y/N), audited (Y/N), lifecycle/status values (from lookups).
3. **Public API (REST)** — endpoint table: method, path, realm (STAFF/CUSTOMER/PUBLIC), permission constant, request/response DTO names, error codes.
4. **Cross-module API (Java)** — interfaces in `crossmodule`, method signatures, semantics, transactional expectations.
5. **Extension points (SPI)** — interfaces an app may implement, how registration works (bean), defaults shipped by core.
6. **Events** — published events (name, when, payload fields) and events consumed.
7. **Configuration** — `erp.core.*` keys, defaults, required/optional, infra dependencies (Redis/SMTP/S3).
8. **Migrations** — core scripts touching the module and the additive-only rule; seed data the module relies on; what an app must seed (`V1000+`).
9. **Security & tenancy** — realm rules, tenant scoping exceptions, sensitive fields excluded from audit.
10. **Integration recipes** — 2–3 short code examples (e.g. "dispatch a notification from an app service", "contribute permissions", "register a report").
11. **Analysis notes** — the module's invariants, known limitations, out-of-scope items, and the decisions taken in steps 01–12 that affect it (link to `docs/steps/NN-report.md`).
12. **Test coverage** — which test classes cover it, coverage %.

## Method
- Generate from code and tests (controllers, DTOs, `*Permissions`, `ErpCoreProperties`, migrations, ArchUnit lists), not from memory. Where a fact cannot be derived from code, write `TBD` and list it in `docs/modules/OPEN-ITEMS.md`.
- After generation, copy the folder into the project repo's knowledge location (step 13 §5) and bump the dependency entry's `docs:` field.

## Acceptance
- 12 files + index; every endpoint in OpenAPI appears in exactly one module doc (script check: compare `/v3/api-docs` paths with the docs' endpoint tables).
- Every `erp.core.*` property appears in exactly one module doc.
