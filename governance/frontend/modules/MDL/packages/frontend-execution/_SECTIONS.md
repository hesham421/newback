<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# FRONTEND EXECUTION PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module    : MDL   Version : v1   Profile : erp   Track : frontend   Plan : exec
Framework : react-ts-vite · routing react-router · server-state tanstack-query ·
            forms react-hook-form · validation zod · state zustand ·
            one lazily-loaded chunk per composite screen (profile.stack.frontend)
Inputs    : srs (v1) · prd (v1) · api-docs (fetched from the backend repo, digest `bac95437…`,
            4 files) · registry-srs (v1) · registry-exec-be (v1)
Screens   : 2 — SCR-MDL-001, SCR-MDL-002 · UXD : 1 — UXD-MDL-001 · API published : 11, bound 11
Decisions : ADR-MDL-002 · ADR-MDL-003 · ADR-MDL-004 · ADR-MDL-005 · ADR-MDL-006 · ADR-MDL-007 ·
            ADR-MDL-011 · ADR-MDL-012 · ADR-MDL-013 · ADR-MDL-014 · ADR-MDL-015 · ADR-MDL-016 —
            all ACCEPTED, all non-breaking
            (superseded, cited by them and not applied here: ADR-MDL-001, ADR-MDL-008)
══════════════════════════════════════════════════════════════════

## API SURFACE — MDL v1   (shapes: `_inputs/api-docs-mdl.md` — cited by API id, never restated)

The published document now carries a `Contract ID` line per endpoint and an API column in its
catalog — 11 of 11 served endpoints — so every block below cites its endpoint by the id the
api-docs themselves publish (ADR-MDL-011). Verb, path, request and response DTOs, the paging
envelope and the error catalog are read there and are not copied here.

```
BINDING   REQ → API — the binding only
  REQ-MDL-001, REQ-MDL-002  → API-MDL-002   create a lookup type
  REQ-MDL-001, REQ-MDL-003  → API-MDL-001   search lookup types (the list the registrar returns to)
  REQ-MDL-003               → API-MDL-003   update a lookup type
  REQ-MDL-004               → API-MDL-004   deactivate a lookup type
  REQ-MDL-005               → API-MDL-005   search the selected type's values
  REQ-MDL-006, REQ-MDL-007  → API-MDL-006   create a lookup value
  REQ-MDL-008               → API-MDL-007   update a lookup value
  REQ-MDL-009               → API-MDL-008   deactivate a lookup value
  REQ-MDL-010               → API-MDL-009   reorder a type's values
  REQ-MDL-013               → API-MDL-010   browse the type registry by owner
  REQ-MDL-011, REQ-MDL-012  → API-MDL-011   read active values by key — a consuming module's
                                            backend call, bound here and called by no screen
                                            (ADR-MDL-007)
UNMAPPED  REQ needing an endpoint that has none : none — all 13 requirements bind.
          Documented endpoint mapping to no REQ : none — all 11 endpoints bind.
          Operations the SRS names for which nothing is published: read one type by id · read
          one value by id. Neither is required by a REQ; both are omitted rather than faked,
          and the entry sub-views hydrate from the row the search query already holds where
          that row is in cache, with a stated cold-load fallback where it is not
          (ADR-MDL-005, ADR-MDL-014). This SRS version names no `activate` action at either
          level (§B3: "لا إجراء تفعيل ولا محو نهائي"), so the half-toggle ADR-MDL-005 also
          covers is not a gap in this version — there is nothing to omit.
CODES     runtime error code → the RULE it carries, the link neither source states:
            MDL-409-MODULE-NOT-REGISTERED → RULE-MDL-001   (the owner module is registered in SEC)
            MDL-409-VALUE-DUP             → RULE-MDL-002   (no duplicate code within one type)
            MDL-404-TYPE-KEY              → RULE-MDL-004   (an inactive type hides its values)
            MDL-409-TYPE-DUP · MDL-404-TYPE · MDL-404-VALUE · MDL-400-REORDER-MISMATCH → no
            RULE of their own: platform-standard duplicate / not-found / validation rows under
            the ADR-SEC-002 umbrella the SRS §A5 names.
            RULE-MDL-003 carries no code at all: `key` is absent from the update request, so no
            code path can raise one. The form states the rule instead of waiting for a message.
          Platform rows the shared handler owns and this module does not mint:
            VALIDATION_ERROR (400) · ACCESS_DENIED (403) · METHOD_NOT_ALLOWED (405) ·
            DATA_INTEGRITY_VIOLATION (409) · INTERNAL_ERROR (500)
ENVELOPE  every response is wrapped in the published `ApiResponse<T>` — `success`, `data`,
          `error { code, message, fieldErrors[] { field, message } }`, `timestamp`. The
          published error object carries ONE message, not a bilingual pair: the client keys the
          text it displays on `error.code` against the module's catalog, which carries the ar
          and the en wording, so the language follows the user's locale; `error.message` is the
          fallback when a code is not in the catalog. `error.fieldErrors[].field` is what routes
          a validation message to a control.
PAGING    the published pagination envelope is `PageLookup<T>` (index.md), constraints from
          `PageableBuilder`: default page 0 · default size 20 · maximum size 200 — the same two
          numbers the SRS §B2 states. THREE response shapes travel in this module and the
          difference is load-bearing in every F2 block:
            · paginated  — API-MDL-001 only
            · bare array — API-MDL-005, API-MDL-009, API-MDL-010, API-MDL-011
            · one object — API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-006, API-MDL-007,
                           API-MDL-008
          **Correction applied a prior pass (G2):** an earlier revision modelled API-MDL-005 as
          paginated, on no published source. `_inputs/api-docs-mdl.md`, backend-execution-plan
          API-MDL-005 ("No paging: the result is bounded by one type and returned whole,
          ordered") and QR-MDL-005 (`Pagination: NO`) all agree the detail read is unpaged; SRS
          §B2 states the same. There is no divergence to record as an ADR here — every source
          agrees, and the frontend plan was the one artifact that had assumed otherwise. F1, F2
          and this table are corrected together.
SEARCH    the three reads of a collection are `POST …/search` carrying a
          `filters[] {field, operator, value}` envelope (operators EQUALS, NOT_EQUALS, LIKE,
          GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN) — which is
          what the SRS §B5 tables state as well. Only the backend plan's contract summary still
          predicts GET for them, and that summary is never read as an API source (ADR-MDL-002).
LOOKUPS   none. MDL owns no lookup key and consumes none (SRS §A6): no field on either screen
          is backed by a list of values, no lookup hook exists anywhere in this plan, and no
          enum is modelled. The one constrained field, `ownerModuleCode`, binds to
          UXD-MDL-001's runtime-loaded list, which is another module's registry — not lookup
          data, and not read through API-MDL-011.
PERMS     declared by the backend and cited, never redeclared: PERM_MDL_LOOKUPS_VIEW (gateway) ·
          PERM_MDL_LOOKUPS_CREATE · PERM_MDL_LOOKUPS_UPDATE · PERM_MDL_LOOKUPS_DELETE ·
          PERM_MDL_TYPE_REGISTRY_VIEW. The api-docs state each endpoint's requirement, and the
          two deactivate endpoints require UPDATE, not DELETE — read there, not assumed here,
          and filed as PF-MDL-001 below (G5) rather than settled in prose.
```

**Where the business codes are stated, and where they are not.** The api-docs publish this
module's business codes once, in the index's Known Error Codes table, and state per endpoint
only the structurally guaranteed answers (`ACCESS_DENIED`, and `VALIDATION_ERROR` where a body
is accepted). The per-block routing below therefore maps each code to the endpoint whose rule
scope can raise it — the mapping is this plan's reading, not a published per-endpoint list, and
no code appears below that the catalog does not publish.

**Field precisions (G1, a prior pass).** Every `maxLength` in this plan is sourced from the
deployed column widths — db-script §1 and ADR-MDL-010 — and not restated from
`_inputs/api-docs-mdl.md` where the two disagree: `key` and `code` are `VARCHAR(50)`, every
`nameAr`/`nameEn` is `VARCHAR(200)`. If the api-docs genuinely publish 80/150, that is a defect
of the MDL api-doc generator on the backend repo, not a frontend choice: it is filed as
PF-MDL-002 below, and this plan binds to the db-script meanwhile so a value the database accepts
is never refused by the client and vice versa.

### Platform findings — MDL v1 frontend track

| id | status | owner | evidence | impact |
|---|---|---|---|---|
| PF-MDL-001 | OPEN | MDL backend track (P3.1) + backend repo method-level security annotations | backend-execution-plan-mdl.md PHASE 7 permission matrix and BOOTSTRAP DATA mark the DELETE column for API-MDL-004 and API-MDL-008 and seed `PERM_MDL_LOOKUPS_DELETE` with grant targets; `_inputs/api-docs-mdl.md` puts `PERM_MDL_LOOKUPS_UPDATE` on both deactivate endpoints | as built, a caller holding UPDATE alone can deactivate a type or a value, and a caller granted DELETE alone can deactivate neither; SRS §B4's DELETE row is unenforceable as built |
| PF-MDL-002 | OPEN (conditional) | MDL backend track — the api-docs generator | if `_inputs/api-docs-mdl.md` publishes `key`/`code` maxLength 80 and `nameAr`/`nameEn` maxLength 150, that contradicts db-script §1 (`VARCHAR(50)`, `VARCHAR(200)`) and ADR-MDL-010 | a 51–80 character key passes client validation and is refused by the database; a 151–200 character label the platform accepts is refused by the client |
| PF-MDL-003 | OPEN | platform — the search-filter contract for every module that omits a by-id read | no published filter set for API-MDL-001 or API-MDL-005 carries the record's own id (key, ownerModuleCode, name, isActiveFl on the first; lookupTypeId, code on the second) | a cold-load deep link into a type or value editor cannot resolve its target through any search, one-row or otherwise; see ADR-MDL-014 |
| PF-MDL-004 | OPEN | MDL backend track (P3.1) | QR-MDL-009's statement and Result-shape lines (backend-execution-plan-mdl.md) enforce only `lookup_type_id = :typeId` per submitted id, against this plan's F2-QUERY VALUE REORDER submission rule, which states the submitted set must always be the type's complete, unfiltered value set | a submitted set that is incomplete or carries a duplicate re-ranks 1..n and collides with the ranks of every value left out — nothing server-side rejects it; required backend change: reject in the API-MDL-009 orchestration, before the UPDATE loop and inside the same transaction, when the DISTINCT submitted id count does not equal the count of rows under `lookup_type_id = :typeId` (active and inactive both, matching QR-MDL-005's scope), and broaden MDL-400-REORDER-MISMATCH's catalog trigger — today it reads only "a submitted id does not belong to the type in the path" — to cover an incomplete or duplicated set |
| PF-MDL-005 | RESOLVED | MDL backend track (P3.1) | answered by the test-gen stage: TC-MDL-014 keeps its id and now exercises RULE-MDL-001's own Test-Hint — a module deregistered from SEC after its types were accepted does not invalidate them (ADR-MDL-024) | closed; the test is constructible and cites no foreign endpoint. The evidence this row previously quoted is text the artifact no longer carries |
| PF-MDL-006 | OPEN | SEC / P3.1 track | registry-exec-be-mdl.md XM STATUS and backend-execution-plan-mdl.md INT-C XM-MDL-001's Interface paragraph both state that SEC's own P3.1 artifacts register only `SecUserDirectoryApi`, so the module-registry read XM-MDL-001 consumes is written down nowhere on SEC's side, disclosed only as prose and deferred to "SEC's own re-run" while the XM is carried ACTIVE with "Unblock condition: none outstanding" | SEC v1's P3.1 artifacts must register `SecModuleRegistryApi` as an exposed cross-module surface alongside `SecUserDirectoryApi` — distinct from the published HTTP read UXD-MDL-001 uses (named in `ui-ux-spec-mdl.md`); the gap is specifically the in-process backend contract |
| PF-MDL-007 | OPEN | MDL backend track (P3.1) | API-MDL-006's orchestration confirms only that the parent type exists; QR-MDL-006 carries no state predicate, so a value can be written under a deactivated type. RULE-MDL-004 scopes only to reads, and no activate endpoint exists (ADR-MDL-005), so such a row is unreachable for the life of the platform and reserves its code permanently (db-script BLOCK 5b) | the orchestration must confirm the parent is active before QR-MDL-014 runs and refuse with a catalog row of its own — broadening MDL-404-TYPE is wrong, the type exists. The SRS owes RULE-MDL-004 its write-side half. The client guard (ADR-MDL-042) is half the fix: a direct call still bypasses it |
| PF-MDL-008 | OPEN | the factory — dialogue-to-ADR writer (dispatch.persist_decisions) | thirteen files of this module's decision stream carry no decision: a full paragraph of review prose as the title line, status `RESOLVED-IN-DIALOGUE` (none of ACCEPTED, BLOCKED, SUPERSEDED), and a Decision body reading "(the dialogue recorded the title alone)" — ADR-MDL-017..022, 025..031 | the writer should take the first sentence as the title, the remainder as the Decision body, and stamp a status from the accepted set, so the stream stops depending on the reviewer's formatting discipline. The two substantive ones are restated as ADR-MDL-043 and ADR-MDL-044; the generated files are outside this pass's writable set |

### Reconciliation against the SRS — run once, before any F-content

- **Every REQ that needs an endpoint has one**, and every documented endpoint maps to a REQ —
  the BINDING block above is the whole mapping, with no gap in either direction.
- **Three reads are `POST …/search`** where the backend plan's contract summary predicts `GET`.
  The api-docs and the SRS §B5 tables agree with each other, so the backend plan is the one
  artifact that lags; `gov.py analyze` reports it there (C8.4). Recorded, not silently corrected
  (ADR-MDL-002).
- **The published ids are now the api-docs' own** — the generator emits the `Contract ID` line
  it emits for the other modules, so this plan cites `API-MDL-*` rather than the plan-local
  labels its previous revision used (ADR-MDL-011, superseding ADR-MDL-008).
- **Two by-id reads the SRS names are published nowhere**; both entry sub-views hydrate from the
  search cache where the row is present, and redirect on a cold load where it is not
  (ADR-MDL-005, ADR-MDL-014).
- **`isActiveFl` is read-only at both levels**, which the SRS §B3 Editable column states as well;
  no published write DTO carries it, and the deactivate endpoints are what change it
  (ADR-MDL-006).
- **`ownerModuleCode` is a select over another module's registry**, cited as UXD-MDL-001 and
  never as a foreign path or a foreign API id (ADR-MDL-004); the foreign endpoint is named in
  `ui-ux-spec-mdl.md`, and the grant that must travel with the screen is ADR-MDL-013. Its
  degraded-source fallback on SCR-MDL-002 is ADR-MDL-015, and on SCR-MDL-001's own master-list
  search filter is ADR-MDL-016 (G1) — distinct from the create-form select's fallback,
  ADR-MDL-013, because the filter is a read over rows already on screen and the select is the
  one place a value could reach the server unvalidated.
- **Action-level grants are readable from no published surface**, so no affordance is hidden on
  a guess; the page gate is the readable half and the server's 403 is the other (ADR-MDL-012).
  The DELETE/UPDATE divergence itself is filed as PF-MDL-001, not resolved by this plan.
- **The detail read is unpaged**, agreeing with every backend and SRS source; a prior revision's
  paginated model was corrected (G2, that pass).
- **The whole value set is what a reorder submits**, and the screen refuses a reorder drag
  from a filtered or paged detail pane, because a partial submission collides ranks rather than
  being refused server-side (G3, a prior pass).
- **Nothing is invented.** No value absent from the api-docs appears in this plan, and no
  permission name, route, component or field is derived from anything but the SRS, the api-docs
  and the profile's own stack.

## EXECUTION PLAN INDEX — MDL v1

| # | Phase | Split | Blocks |
|---|---|---|---|
| 1 | F1 — Models & Types | per screen — always | `SUB:F1-SCR-MDL-001`, `SUB:F1-SCR-MDL-002` |
| 2 | F2 — Data Hooks | per screen — always | `SUB:F2-SCR-MDL-001`, `SUB:F2-SCR-MDL-002` |
| 3 | F3 — Forms & Validators | per screen — always | `SUB:F3-SCR-MDL-001`, `SUB:F3-SCR-MDL-002` |
| 4 | F4 — Screens & Routes | per screen — always | `SUB:F4-SCR-MDL-001`, `SUB:F4-SCR-MDL-002` |
| 5 | SEC-FE | never split | level-1 only |
| 6 | ALIGN-FE | never split | level-1 only |

One `SUB` per screen in each of the four sub-bearing phases, at any screen count: what the split
exists to give is a per-screen address — an implementer takes one screen's F2 block, not "the F2
phase of a small module" — and that is worth the same at two screens as at twelve
(`profile.tracks.frontend.plans.exec`).

**SCREEN REGISTRY**

| SCR | الاسم / Name | Page code | Container pattern | Owning ENT |
|---|---|---|---|---|
| SCR-MDL-001 | اللوكبات العامة / Generic Lookups | MDL_LOOKUPS | TREE_MASTER_DETAIL | ENT-MDL-001 (+ ENT-MDL-002) |
| SCR-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | MDL_TYPE_REGISTRY | FULL_PAGE — no entry sub-view (ADR-MDL-003) | ENT-MDL-001 |













---

## Hand-off

The implementer reads the phases in profile order — F1 models, F2 hooks, F3 forms, F4 screens and
routes, SEC-FE guards — takes design intent from `ui-ux-spec-mdl.md`, and takes every request and
response shape from `_inputs/api-docs-mdl.md` at the commit this module version pins, **except**
the two published numbers PF-MDL-002 disputes (`key`/`code` maxLength, `nameAr`/`nameEn`
maxLength), which this plan binds to the db-script instead until that finding is resolved. No
route, component, permission or field that is not traceable to an F-block above is invented: a
gap is an ADR in `analysis/decisions/MDL/`, never an invention.

Two response shapes travel in this module and the difference is load-bearing: one paginated read,
four bare arrays and six single objects. No block may be read through an envelope another block
declares.

The plan and its registry are split by the toolkit into the frontend execution package inside the
shared repo after the `gate:pass-2` verdict, and tagged `mdl-v1`. Nothing is copied anywhere: the
implementer reads it where it was written.
══════════════════════════════════════════════════════════════════
