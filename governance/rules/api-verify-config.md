# api-verify — run requirements

Single source of truth for the facts `.claude/skills/api-verify/SKILL.md` needs at run
time. The skill states the *procedure*; this file states the *project's own conventions* —
edit this file when a convention changes, never the skill. Mirrors the separation
`GOVERNANCE-RULES.md` §"Convention Precedence" already uses for the `build-*`/`gov-*` skills.

---

## 1. Inputs — where to read from, per module

| Input | Path | Role |
|---|---|---|
| api-docs | `docs/api-docs/<module>/` (`index.md` + `endpoints/*.md`; `<module>` is the lower-case package name: `sec`, `tenant`, `file`, `notif`, `mdl`, `cu`, `sequence`, `audit`, `report`, `app`) | **mandatory** — endpoints, verbs, paths, request/response field tables, examples, error codes. Generated from the running reference app by `docs/api-docs/_tools/generate_all.py` (the driver of `governance/tools/api-doc-generator`) — do not hand-edit, do not treat a stale copy as current; regenerate first if in doubt |
| test-execution-manifest | supplied with the run, when one exists | optional — pre-computed DEPENDENCY ORDER, RULE → code → TC triples, ENTITY CRUD CHECKLIST. None is vendored in this repository (the generated manifests were dropped with the plan generator), so a run without one is at **minimal tier** (§3) |
| base URL · credentials · auth endpoint | given by whoever invokes the skill for this run | optional — placeholders / `http://localhost:<port>` otherwise |

Module list: the table in `docs/api-docs/README.md` (one row per module folder). Read it to
resolve which modules exist — never hard-code a module list in the skill.

Reference material that is NOT an input: the legacy adapted suites under
`governance/backend/modules/<MOD>/test-api/` (`test_<mod>_apis.py` + `<mod>_problems_report.md`)
and the core suite under `docs/test-api/` (`core_api_verify.py`, `core-test-plan.md`,
`core-verify-report.md`) show what earlier runs produced and how they adapted to erp-core
1.2.0; a new run reads the api-docs, not those scripts.

## 2. Outputs — where to write

`docs/test-api/`:
- `test_<mod>_apis.py` (or the extension matching whatever language the run targets — see §3)
- `<mod>_problems_report.md`
- `<mod>_grant_journal.md` — only when a run needed a permission grant (§4.2); append-only, and
  the one file here that is **not** regenerate-freely scratch: it is the audit trail of privilege
  an interrupted run may have left standing
- an HTML run report may also be dropped here

`<mod>` is the module code lower-cased. Generated scripts and reports are regenerated freely,
never hand-edited. The grant journal is the single exception: it is appended to, never
regenerated, because its value is precisely the history a regeneration would erase.
`docs/test-api/results/` holds dated JSON results of the core suite.

## 3. Stack conventions

| Convention | Value |
|---|---|
| Backend framework | Spring Boot 4 (Java 25), `erp-core` library consumed by `erp-app-reference` |
| Base path | `/api/v1/{module}/{resource}` — the module segment is the lower-case module name or its resource noun, e.g. `/api/v1/notifications/templates`, `/api/v1/sec/audit-log`; read the exact paths from the api-docs |
| Tenant header | every request carries `X-Tenant-Code` (see `docs/api-docs/<module>/index.md` → auth / common headers); the platform tenant is `PLATFORM` |
| Verb → operation | `POST`=create · `GET`=read · `PUT`=update · `DELETE`=deactivate (**soft** — see below) · `PATCH`=partial |
| Response envelope | `ApiResponse<T>` — unwrap before asserting on `data` |
| Paging envelope | `Page<T>` — fields: `totalPages`, `totalElements`, `first`, `last`, `numberOfElements`, `pageable`, `sort`, `size`, `number`, `empty`, `content` — confirm names against the module's own api-docs; an undocumented field is a documentation gap to flag, not an assertion |
| Error envelope | `LocalizedException` → `{code, messageAr, messageEn}` (plus `fieldErrors[]` for validation failures — see the module's `index.md`) |
| Error code format | `{MOD}-{http}[-{SLUG}]` — e.g. `SEC-409-USER-DUP`, `SEC-500`; `{http}` = the row's HTTP status, `{SLUG}` = SCREAMING-KEBAB (optional) — never the hyphenated governance rule/RULE id |
| `DELETE` semantics | **soft** — an "active" flag column, suffix `Fl` (e.g. `isActiveFl`); teardown calls the deactivate endpoint, never a hard delete, unless the module's own api-docs document a genuine hard-delete endpoint |
| Permissions | pattern `PERM_<PAGE_CODE>_<ACTION>`, actions `VIEW`/`CREATE`/`UPDATE`/`DELETE`, gateway action `VIEW` (without `VIEW` no other permission on that page applies) — a forbidden call asserts the forbidden status through the error envelope |
| Realms | STAFF (`/api/v1/auth/**`, staff endpoints) and CUSTOMER (`/api/v1/public/customers/**`, `/api/v1/customers/me/**`); a token of one realm is rejected on the other's endpoints — see `docs/api-docs/sec/index.md` |
| Languages | `ar` (primary), `en` — both required; message-presence assertions check both language fields the envelope carries; report language = `ar`, with `en` beside it |
| Manifest tier | when a test-execution-manifest is supplied → Full tier; otherwise → **minimal tier** (happy-path CRUD only, state why negatives were skipped) rather than treating the absence as an error |

## 4. Safety defaults

- Base URL: the run's own argument, else `http://localhost:<server.port>` — read the port from
  `erp-app-reference/src/main/resources/application*.properties` (7272 today) or the run's
  `.env`; never assume `8080`
- Credentials: placeholders unless supplied for the run (the bootstrap `admin` password is
  `ERP_BOOTSTRAP_ADMIN_PASSWORD` — there is no default)
- Database access: opt-in only, off by default; when enabled, writes are scoped to ids this
  run created, run in a transaction, under separate credentials from the app's own
- Never targets anything but Dev/Test — this generates a **Dev/Test-only** script

### 4.1 Dev/Test verification (required before any permission grant)

A target counts as Dev/Test only when its host is `localhost`/`127.0.0.1`, **or** the run was
given an explicit `--allow-nonlocal` argument naming the environment. Anything else aborts
before the skill's stage I runs — a verification run may not escalate privileges on an
environment it cannot prove is disposable.

### 4.2 Permission-grant journal (SKILL.md §3-I)

`docs/test-api/<mod>_grant_journal.md` — append-only, one line per intended grant, written
**before** the grant call and again after its revoke:

```
<ISO timestamp> | RUN_ID=<id> | GRANT   | role=<code> | target=<module|screen|action> | value=<code>
<ISO timestamp> | RUN_ID=<id> | REVOKE  | role=<code> | target=<module|screen|action> | value=<code>
```

A `GRANT` line with no matching `REVOKE` is standing privilege from an interrupted run — the
file exists precisely so that state is discoverable rather than invisible. Append only: never
regenerate it, never rewrite an existing line, and never delete lines to tidy it — an unmatched
`GRANT` is the one record that a privilege may still be standing, and erasing it destroys the
only evidence. Add a `REVOKE` line to close one out; leave the history in place.

### 4.3 Preconditions (SKILL.md §3-A0)

Externally-owned values a payload references — an owner-module code, a registry code, a lookup
key, an example-sourced parent id — are verified present and active before the suites that need
them. This project has already been bitten by the alternative: a test plan once named an owner
module that was never registered in `SEC_MODULE_REG`, and the resulting 409 turned into eleven
cascading test failures that read like application defects for three runs before the real
cause was found. The registry tables worth checking first are the ones other modules' rules
read — `SEC_MODULE_REG` above all, since MDL's `RULE-MDL-001` validates every lookup type's
owner against it.
