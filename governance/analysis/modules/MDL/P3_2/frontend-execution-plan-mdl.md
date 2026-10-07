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

<!-- PHASE:F1:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,AC-MDL-011,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 1 — F1 — Models & Types

Per entity and per screen: the source DTO with each property's type and its read-only /
system-only status, then the screen's search model, form model and container. Names are carried
per language (ar, en) wherever a label is modelled. Nothing is modelled that the api-docs do not
return, no internal identifier is invented, and **no enum and no lookup-backed field appears
anywhere in this phase** — MDL owns no coded list of its own (SRS §A6).

Field and DTO binding: see `_inputs/api-docs-mdl.md` — the published request and response shapes
for this module are the source and are not restated here. What is stated here is the shape's
consequence for the client: which properties a form may write, and which it may only display.
Every `maxLength` below is the deployed db-script column width (db-script §1, ADR-MDL-010), per
the correction in API SURFACE above (G1, a prior pass); a divergence from the api-docs is
PF-MDL-002, not a number restated from them.

<!-- SUB:F1-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F1 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F1-MODEL — ENT-MDL-001 — نوع اللوكب / LookupType
Source DTOs  : `LookupTypeResponse` (read) · `LookupTypeCreateRequest` · `LookupTypeUpdateRequest`
  lookupTypePk    : number · read-only (PK) · system-only · never shown as a business reference
  key             : string · maxLength 50 (db-script §1, ADR-MDL-010) · required on create,
                    **read-only on edit** — RULE-MDL-003, and the update request does not carry it
  ownerModuleCode : string · maxLength 10 · required on create, **read-only on edit** — a plain
                    string holding the code, never an enum and never a union of literals; its
                    valid set is the security module's registry (UXD-MDL-001)
  nameAr          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn          : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  isActiveFl      : boolean · read-only — flipped by API-MDL-004 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only (audit)

#### F1-MODEL — ENT-MDL-002 — قيمة اللوكب / LookupValue
Source DTOs  : `LookupValueResponse` (read) · `LookupValueCreateRequest` ·
               `LookupValueUpdateRequest` · `LookupValueReorderRequest`
  lookupValuePk : number · read-only (PK) · system-only
  lookupTypeId  : number · read-only — the path id of API-MDL-006, taken from the selected
                  parent, never typed
  code          : string · maxLength 50 · required on create, **read-only on edit** — the update
                  request does not carry it; unique within its type (RULE-MDL-002)
  nameAr        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  nameEn        : string · required · maxLength 200 (db-script §1, ADR-MDL-010)
  sortOrder     : number · required on create **and** on update — and the same field the reorder
                  writes through `{ orderedValueIds[] }`; two paths, one field
  isActiveFl    : boolean · read-only — flipped by API-MDL-008 alone (ADR-MDL-006)
  createdBy, createdAt, updatedBy, updatedAt : read-only · system-only

#### F1-SCREEN — SCR-MDL-001
Search model : master — key : string · LIKE · ownerModuleCode : string · EXACT (options from the
               UXD-MDL-001 hook) · isActiveFl : boolean · EXACT · plus page, size, sortDirection,
               all inside the one request object of API-MDL-001 (the only paged read on this
               screen). No `sortField` is modelled: QR-MDL-001 declares a single ordering
               (`ORDER BY key`) and PHASE 1 states the module offers no free-form sort parameter,
               so `sortDirection` is available on `key` alone and keying a variation the server
               cannot produce would fragment the cache for nothing (G7).
               detail — code : string · LIKE · lookupTypeId : number · EXACT (from the selected
               parent, not typed) — **no page, no size, no sort**: API-MDL-005 is unpaged
               (backend-execution-plan API-MDL-005, QR-MDL-005 `Pagination: NO`, SRS §B2), so
               none of the three is modelled and none belongs in this screen's detail cache key
               (a prior pass's correction — G2)
Form model   : type · create — key, ownerModuleCode, nameAr, nameEn (all required)
               type · edit   — nameAr, nameEn (required); key and ownerModuleCode read-only
               value · create — code, nameAr, nameEn, sortOrder (all required)
               value · edit   — nameAr, nameEn, sortOrder (required); code read-only
               reorder        — the ordered list of value ids, and only ever the type's complete
               value set (G3, see F2-QUERY VALUE REORDER); not a per-row edit and not a form
               excluded system fields : both PKs · lookupTypeId · both isActiveFl · the audit four
Container    : TREE_MASTER_DETAIL — a master list of types and, beside it, the selected type's
               values; each of the two entry surfaces models its own record and nothing else
The master read returns the published pagination envelope; the detail read returns a bare array,
returned whole because it is confined to one type. Both write models drop every property no
published write DTO carries, so no form offers a field the server would ignore (ADR-MDL-006).
<!-- SUB:F1-SCR-MDL-001:END -->

<!-- SUB:F1-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-011,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F1 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F1-MODEL — OwnerGroupResponse — المجموعة حسب المالك / Owner group
Source DTO   : `OwnerGroupResponse[]` — a bare array (API-MDL-010); this screen writes nothing
  ownerModuleCode : string · read-only — the group key, labelled through UXD-MDL-001
  types[]         : the same type projection as above, **every property read-only here** —
                    lookupTypePk, key, ownerModuleCode, nameAr, nameEn, isActiveFl and the audit
                    four. The type model of `F1-SCR-MDL-001` is reused, not re-declared; only its
                    writability differs, and on this screen there is none.

**No F1-MODEL for the consumer read (G11, a prior pass).** The bare array
`LookupValueResponse[]` API-MDL-011 returns is accounted for once, in the F2 block that binds it
(`F2 · SCR-MDL-002` below, per ADR-MDL-007): that block already carries its own caller, its own
error answers and `Cache key : n/a`. Modelling it a second time here, in a screen this frontend
renders, instructed an implementer to write a client type nothing in the delivered frontend
consumes; removed rather than kept as documentation, since the F2 block is the complete and
correct place for a foreign contract this plan binds but never emits.

#### F1-SCREEN — SCR-MDL-002
Search model : ownerModuleCode : string · EXACT · key : string · LIKE. **No page, no size, no
               sort** — the request object of API-MDL-010 carries `filters` alone, so none of the
               three is modelled and none belongs in this screen's cache key
Form model   : none — a read-only browse (SRS §B3); no property of the response is writable
Container    : FULL_PAGE, no entry sub-view (ADR-MDL-003) — the owner → types hierarchy is the
               grouped list the endpoint returns, not a second pane with a form in it
The response is a bare array of groups and is modelled as one: reading it through a pagination
envelope would invent fields the endpoint does not send.
<!-- SUB:F1-SCR-MDL-002:END -->
<!-- PHASE:F1:END -->

<!-- PHASE:F2:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-004,AC-MDL-005,AC-MDL-007,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 2 — F2 — Data Hooks

What each screen needs from the API — not hook code. Verb, path and request/response shape are
cited by the API id at each block's head and are read in the api-docs, never restated. Every read
query's cache key carries every filter that changes the response, page and size included **where
the endpoint is paged**; page and page size live inside the filter object and are never
independent state, and are absent from the key of an unpaged read entirely. Every mutation
declares its invalidation. Components use the facade only; the facade uses the declared queries
only (server-state library: `tanstack-query`).

Error routing is uniform and is stated per block only where a business code makes it specific:
field validation → inline on the field `error.fieldErrors[].field` names · business rule → the
user message for that rule · unauthenticated → the platform's sign-in destination, discarding the
server-state cache · forbidden → the localized catalog message on the surface that attempted the
call · server → the generic message.

<!-- SUB:F2-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-004,AC-MDL-005,AC-MDL-007,AC-MDL-009,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F2 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F2-QUERY — TYPE SEARCH — API-MDL-001      traces=API-MDL-001,REQ-MDL-001,REQ-MDL-003
Kind         : read query — a POST that mutates nothing (ADR-MDL-002); paginated response
Cache key    : `[lookup-types, filters]`, `filters` being the whole request object — key,
               ownerModuleCode, isActiveFl, sortDirection **and page, size**. Every one of them
               changes the response, so every one is in the key. No `sortField` is in the key or
               the request: the backend offers no free-form sort parameter (G7, F1-SCREEN above).
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — the SRS states nothing about this call being slow, so no global indicator
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-002, API-MDL-003 and API-MDL-004
Note (G1)    : this query's response is also the fallback source for the master-list
               `ownerModuleCode` filter's own degraded state (see F2-SCREEN-INIT, ADR-MDL-016) —
               its rows are read for their `ownerModuleCode` values, not through a second call.

#### F2-QUERY — TYPE CREATE — API-MDL-002      traces=API-MDL-002,REQ-MDL-001,REQ-MDL-002,AC-MDL-001,AC-MDL-002
Kind         : mutation
Errors       : `MDL-409-MODULE-NOT-REGISTERED` → the RULE-MDL-001 message, routed to the
               owner-module field — ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
               en: "The owning module is not registered in the Security module" ·
               `MDL-409-TYPE-DUP` → inline on `key` (the platform duplicate row) ·
               `VALIDATION_ERROR` → inline per `error.fieldErrors[].field` ·
               `ACCESS_DENIED` → the localized forbidden message (ADR-MDL-012)
Loading      : LOCAL — on the submitting form
Invalidation : `[lookup-types, *]`, and `[lookup-types-by-owner, *]` — a new type changes what
               SCR-MDL-002's registry shows
Success      : ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved." (AC-MDL-001)

#### F2-QUERY — TYPE UPDATE — API-MDL-003      traces=API-MDL-003,REQ-MDL-003,AC-MDL-003
Kind         : mutation
Errors       : `MDL-404-TYPE` → user message · `VALIDATION_ERROR` → inline ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]`
The request carries the two names and nothing else — RULE-MDL-003 expressed in the surface
itself: there is no key field to send, and none is sent.

#### F2-QUERY — TYPE DEACTIVATE — API-MDL-004  traces=API-MDL-004,REQ-MDL-004,AC-MDL-004
Kind         : mutation · no request body
Errors       : `MDL-404-TYPE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]` **and** `[lookup-values, *]` —
               RULE-MDL-004 makes an inactive type's values invisible to consumers, so a cached
               value list of that type is stale in meaning even though its rows did not change
Requires `PERM_MDL_LOOKUPS_UPDATE` per the api-docs, not DELETE — read there, not assumed, and
filed as PF-MDL-001 rather than settled here (G5).

#### F2-QUERY — VALUE SEARCH — API-MDL-005     traces=API-MDL-005,REQ-MDL-005,AC-MDL-005
Kind         : read query (ADR-MDL-002); **not paginated** — a bare array, ordered by
               `sortOrder` then `code`, returned whole because it is bounded by one type
               (backend-execution-plan API-MDL-005, QR-MDL-005, SRS §B2). A prior revision of
               this block modelled this call as paginated; corrected then (G2).
Cache key    : `[lookup-values, filters]` — lookupTypeId, code. No page and no size: the endpoint
               accepts neither, and keying a variation the server cannot produce would fragment
               the cache for nothing. The parent id is part of the key, so selecting another type
               is a different cache entry, not a refetch of the same one.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — on the values pane alone; the master list does not blank while it loads
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009
This read returns inactive values beside active ones — it is the manager's view, not the
consumer's, and the screen shows what API-MDL-011 hides (AC-MDL-005, AC-MDL-009).

#### F2-QUERY — VALUE CREATE — API-MDL-006     traces=API-MDL-006,REQ-MDL-006,REQ-MDL-007,AC-MDL-006,AC-MDL-007
Kind         : mutation · the parent type is the path id, taken from the selection
Inactive parent : the call is **not issued when the selected parent type is inactive**
               (ADR-MDL-042). `isActiveFl` is on the row the pane already holds, so the
               state is known without a read. A value created under a deactivated type is
               unreachable for the life of the platform — v1 publishes no activate endpoint
               (ADR-MDL-005) and RULE-MDL-004 excludes an inactive type's values from every
               consumer read — and it reserves its code under that type permanently. UPDATE
               and REORDER stay legal: they maintain rows AC-MDL-004 already guarantees
               survive deactivation. The guard is the client half only; the server half is
               PF-MDL-007
Errors       : `MDL-409-VALUE-DUP` → the RULE-MDL-002 message, routed inline to `code` —
               ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
               en: "This code is already used within this type" — a deactivated value of the
               same code under this type may be the one holding it (RULE-MDL-002 permits no
               reactivation, G7); the list's own inactive row, still shown in this pane
               (AC-MDL-005), is the evidence the user can check ·
               `MDL-404-TYPE` → user message (the parent type is gone) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]`
Success      : ar: «تم حفظ القيمة.» · en: "The value has been saved." (AC-MDL-006)

#### F2-QUERY — VALUE UPDATE — API-MDL-007     traces=API-MDL-007,REQ-MDL-008,AC-MDL-008
Kind         : mutation
Errors       : `MDL-404-VALUE` → user message · `MDL-409-VALUE-DUP` → inline on `code` if the
               server ever raises it here (RULE-MDL-002 is scoped to create and update alike) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]` — the submitted `sortOrder` may have moved the row

#### F2-QUERY — VALUE DEACTIVATE — API-MDL-008 traces=API-MDL-008,REQ-MDL-009,AC-MDL-009
Kind         : mutation · no request body
Errors       : `MDL-404-VALUE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-values, *]`
The row stays in this screen's list afterwards, marked inactive: it leaves the consumer's read,
not the manager's (AC-MDL-009). No endpoint exists to reverse it (ADR-MDL-005), so its `code`
stays reserved under this type for the life of the type — the confirmation states this (G7,
ui-ux-spec-mdl.md).

#### F2-QUERY — VALUE REORDER — API-MDL-009    traces=API-MDL-009,REQ-MDL-010,AC-MDL-010
Kind         : mutation · request `{ orderedValueIds[] }` · response a bare array in the
               persisted order — not a paginated envelope
Errors       : `MDL-400-REORDER-MISMATCH` → user message on the value list (the submitted set is
               not exactly that type's values) · `MDL-404-TYPE` → user message ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the list, with the dragged order held until the call settles
Invalidation : on success only — `[lookup-values, *]`, since every row's `sortOrder` may have
               changed and the response carries the persisted order the list then renders from.
               On any non-2xx answer the pending order is discarded and the list re-renders from
               the last persisted response; no invalidation runs (G10). MDL-400-REORDER-MISMATCH
               therefore always leaves the list showing the order the server actually holds,
               never the rejected drag.
Submission rule (G3) : **the submitted list is always the type's complete, unfiltered value set.**
               `lookup_type_id = :typeId` is the only predicate QR-MDL-009 checks — it does not
               (and cannot) verify that every value of the type was submitted — so a subset
               re-ranks 1..n and collides with the ranks of every value left out. The values
               pane can produce exactly that subset (a non-empty `code` filter, F1/F3), so the
               drag affordance is **disabled** whenever the pane shows less than the whole type:
               a non-empty `code` filter is active. A shown reason accompanies the disabled
               state — ar: «امسح مرشّح الرمز لإعادة الترتيب» · en: "Clear the code filter to
               reorder" — because the API-MDL-005 read this pane is built on carries no paging to
               also guard against (G2). Reordering from a filtered pane is refused by the client
               before the call is made, not answered by the server, because the server cannot
               distinguish a partial submission from a deliberate whole-set one.
<!-- moved from a bare inline note: the endpoint takes `orderedValueIds[]`; the mismatch code
     exists for an id that does not belong to the type, never for a merely incomplete list. -->

#### F2-LOOKUP — none
MDL owns no lookup key and consumes none (SRS §A6). The one option list on this screen is the
owner-module select, which is not lookup data: it is UXD-MDL-001, declared below.

#### F2-SCREEN-INIT — SCR-MDL-001
Permission read : `MDL_LOOKUPS` present in the caller's effective menu → VIEW, the gateway.
                  CREATE, UPDATE and DELETE are readable from no published surface; the server
                  answers them per call (ADR-MDL-012, PF-MDL-001).
Lookups used    : none
Foreign data    : the owner-module select (create form) and the owner-module filter (master
                  list) both resolve through UXD-MDL-001 — ONE shared hook, long-lived cache,
                  shared with SCR-MDL-002 — but the two controls degrade differently, because a
                  select that must accept an unpicked value cannot degrade the way a read-only
                  filter over an already-known result set can:
                    · create-form select — a refused read leaves the select empty and disabled,
                      and the create action disabled behind it (ADR-MDL-013)
                    · master-list filter — a refused read leaves the filter falling back to the
                      distinct `ownerModuleCode` values already present in the current
                      API-MDL-001 response (see F2-QUERY TYPE SEARCH above) — never empty, never
                      disabled, and browsing the type list is never blocked by the degraded
                      source (ADR-MDL-016, G1)
Entity by id    : none published at either level; both entry sub-views hydrate from the row their
                  own search query already holds when that row is in cache; on a cold load the
                  hydrating row is absent and the route redirects to `/reference-data/lookups`
                  (type editor) or `/reference-data/lookups/:typeId` (value editor) rather than
                  render empty (ADR-MDL-014)

#### F2-FACADE — SCR-MDL-001
Composes     : API-MDL-001, API-MDL-005 (the two lists) · API-MDL-002, API-MDL-003, API-MDL-004,
               API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009 (the mutations) · the
               UXD-MDL-001 hook
State it owns: the type list and the selected type's value list, both derived from their queries'
               data and never copied into state; the selected type id, read from the route param;
               two filter objects (the master's carrying page and size, the detail's carrying
               `code` alone); the pending drag order while a reorder is in flight, and whether the
               detail's `code` filter is empty (the reorder-eligibility flag of G3); the open
               editor's identity, held in navigation state rather than in a boolean; and a
               derived loading flag over the calls in flight
Operations   : createType · updateType · deactivateType (confirmed, and the confirmation says
               consumer reads will stop returning this type's values, and that `key` stays
               reserved and unreusable afterwards — RULE-MDL-004, G7) · createValue · updateValue
               · deactivateValue (confirmed, same reserved-code statement, G7) · reorderValues
               (the whole ordered list, submitted once, disabled while the detail pane is
               filtered — G3)
Ordered pairs: none. **No operation of this screen owns two calls** — a type's write carries no
               values, so nothing here updates a record and then replaces its child set, and no
               ordering or skip-if-unchanged rule is needed. Each operation is exactly one call.
There is no activateType and no activateValue: the SRS names no such action and no endpoint
exists for one (ADR-MDL-005). Components use the facade only; the facade uses the declared
queries only.
<!-- SUB:F2-SCR-MDL-001:END -->

<!-- SUB:F2-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F2 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F2-QUERY — REGISTRY BROWSE — API-MDL-010  traces=API-MDL-010,REQ-MDL-013,AC-MDL-013
Kind         : read query (ADR-MDL-002) · request carries `filters` alone — no sort, no paging ·
               response a bare array of owner groups
Cache key    : `[lookup-types-by-owner, filters]` — ownerModuleCode and key, and nothing more.
               No page or size belongs in this key because the endpoint accepts neither; adding
               them would key a variation the server cannot produce.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL
Cache policy : defaults
Invalidation : n/a (a read). It is refreshed by SCR-MDL-001's type mutations, which name this
               key family in their own invalidation lines — a type created, renamed or
               deactivated there changes what this registry shows.

#### F2-QUERY — CONSUMER READ BY KEY — API-MDL-011  traces=API-MDL-011,REQ-MDL-011,REQ-MDL-012,AC-MDL-011,AC-MDL-012
Kind         : read query · **bound, and called by no screen of this module** (ADR-MDL-007).
               This block is the complete accounting for API-MDL-011's response shape — no
               second model of it exists in F1 (G11).
Caller       : a consuming module's backend, over the platform's in-process module interface —
               not a user at a screen. Stated here so the published surface is completely
               accounted for.
Errors       : `MDL-404-TYPE-KEY` [REQ-MDL-012, RULE-MDL-004] → answered to the calling module —
               ar: «لا يوجد نوع لوكب بهذا المفتاح» · en: "No lookup type exists with this key" —
               an unknown key is a not-found, never an empty success ·
               `ACCESS_DENIED` → answered to the calling module
Cache key    : n/a — no client of this plan calls it
Invalidation : n/a. What a user can observe of this endpoint is indirect: deactivating a value,
               or its type, on SCR-MDL-001 is what makes it stop being returned (RULE-MDL-004).

#### F2-LOOKUP — none
Same as the other screen, and for the same reason: MDL owns no lookup key and consumes none.

#### F2-SCREEN-INIT — SCR-MDL-002
Permission read : `MDL_TYPE_REGISTRY` present in the caller's effective menu → VIEW. This screen
                  has no other action to gate (SRS §B4).
Lookups used    : none
Foreign data    : the owner-module filter and the group headings resolve through UXD-MDL-001 —
                  the same shared hook SCR-MDL-001 uses, not a second call. When that read is
                  refused or fails, the owner select falls back to the **distinct
                  `ownerModuleCode` values present in the current API-MDL-010 response**, which
                  the browse already returns on every group — never to free text, and never to a
                  code the registry does not currently hold. Group headings themselves fall back
                  to the bare code the browse returns, unchanged from before; browsing is never
                  blocked by the degraded source (ADR-MDL-013, ADR-MDL-015, G6)
Entity by id    : none — each group carries its full type rows

#### F2-FACADE — SCR-MDL-002
Composes     : API-MDL-010 · the UXD-MDL-001 hook
State it owns: the groups derived from the query's data, and the filter object (owner module,
               key) mirrored from the route's search params, so a filtered registry view is
               shareable by address (ADR-MDL-002). No grouping and no count is composed here —
               both arrive in the response.
Operations   : none — this screen writes nothing. Opening a type in SCR-MDL-001 is a route
               change, not an operation.
Ordered pairs: none — there is no call to order.
<!-- SUB:F2-SCR-MDL-002:END -->
<!-- PHASE:F2:END -->

<!-- PHASE:F3:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,REQ-MDL-011,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-010 -->
## PHASE 3 — F3 — Forms & Validators

One block per `RULE-*` a form enforces, plus the field constraints the published DTOs state. No
frontend-only validation the SRS does not state; every message is read from its catalog code
rather than hard-coded; the locale resolves session → browser → `ar` (`profile.languages.primary`);
and a caller without the write permission is answered by the server, not by a pre-emptively
disabled field (ADR-MDL-012). Schemas are written with `zod` and bound with `react-hook-form`.
Every `maxLength` below is the deployed column width (db-script §1, ADR-MDL-010), per the
correction in API SURFACE (G1, a prior pass).

**No option-set validator exists anywhere in this module.** MDL owns no coded list (SRS §A6), so
no field binds to a set of lookup values. The one field with a constrained set is
`ownerModuleCode`, whose set is another module's registry: its validator binds to the
runtime-loaded list of UXD-MDL-001, never to a static list of module codes.

<!-- SUB:F3-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-006,AC-MDL-007,AC-MDL-008,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-005,API-MDL-006,API-MDL-007 -->
### F3 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

Validation timing for this screen, declared once and holding for both entry surfaces: **on blur
for the unique key and the unique code, on submit for everything else.**

#### F3-FIELD — SCR-MDL-001 (type · create)
key             · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK · on blur
ownerModuleCode · REQUIRED · LENGTH (maxLength 10) · MEMBER_OF the UXD-MDL-001 list ·
                  BUSINESS_RULE (RULE-MDL-001) · on submit
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (type · edit)
key, ownerModuleCode · read-only — not inputs at all; the update request carries neither
nameAr, nameEn       · REQUIRED · LENGTH (maxLength 200) · on submit

#### F3-FIELD — SCR-MDL-001 (value · create)
code            · REQUIRED · LENGTH (maxLength 50) · UNIQUE_CHECK within the selected type
                  (RULE-MDL-002) · on blur
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

#### F3-FIELD — SCR-MDL-001 (value · edit)
code            · read-only — the update request does not carry it
nameAr, nameEn  · REQUIRED · LENGTH (maxLength 200) · on submit
sortOrder       · REQUIRED · integer · on submit

UNIQUE_CHECK    : async, on blur — both backend filters are LIKE, never EQUALS (QR-MDL-013's
                  `key LIKE :key`, QR-MDL-005's `code LIKE :code`; PHASE 1's Search contract and
                  SRS §B2 agree), so the check requests the LIKE filter the backend actually
                  declares and then asserts exact string equality client-side over the returned
                  rows before showing the inline message: the type's `key` through API-MDL-001
                  (LIKE `key`, then filter the response for an exact match) — the value's `code`
                  through API-MDL-005 scoped to `lookupTypeId` (LIKE `code`, then filter the
                  response for an exact match), so the check's scope is the rule's scope. This
                  uses only the published surface and is correct whether or not the service ever
                  honours EQUALS (G6). Neither blocks submit on its own: `MDL-409-TYPE-DUP` and
                  `MDL-409-VALUE-DUP` from the server are the authority, routed inline to the
                  same field. On edit neither field is an input, so neither check runs.

#### F3-VALIDATION — RULE-MDL-001   traces=REQ-MDL-002,AC-MDL-002
Statement : The system shall reject a lookup type registration whose owner module code has no
            ModuleRegistry row in the security module.
Message   : catalog code `MDL-409-MODULE-NOT-REGISTERED` —
            ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
            en: "The owning module is not registered in the Security module"
Scope     : CREATE · Field : ownerModuleCode · kind BUSINESS_RULE · when submit
Shape     : the control is a select over the registered module codes loaded through UXD-MDL-001,
            so the common case cannot be typed wrong at all; the validator asserts that the
            submitted value is one the runtime-loaded list contains, never that it is one of a
            static set. The server stays the authority — a module deregistered between load and
            submit is caught there — and the catalog message routes to this field. When the
            foreign read is refused the select is empty and disabled and create is disabled
            behind it, rather than falling back to free text (ADR-MDL-013). The read-only
            master-list filter beside this control degrades differently — to the current search
            response's own values, never disabled — because nothing is submitted through a
            filter (ADR-MDL-016, G1).

#### F3-VALIDATION — RULE-MDL-002   traces=REQ-MDL-007,AC-MDL-007
Statement : The system shall reject a lookup value whose code already exists under the same
            lookup type.
Message   : catalog code `MDL-409-VALUE-DUP` — ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
            en: "This code is already used within this type"
Scope     : CREATE (and UPDATE, per the rule's trigger) · Field : the value's code ·
            kind UNIQUE_CHECK · when blur, and again on submit by the server
Shape     : uniqueness is scoped to the parent type, never globally — the same code under
            another type is legitimate, and a global check would reject a value the server
            accepts. The async check is bound to the selected parent id. On edit the field is
            read-only, so the client check cannot fire and the rule is the server's alone. A
            deactivated value under the same type can be the one holding a submitted code — no
            activate endpoint exists (ADR-MDL-005), so the code stays reserved — and the inline
            message names that possibility (G7, F2-QUERY VALUE CREATE).

#### F3-VALIDATION — RULE-MDL-003   traces=REQ-MDL-003,AC-MDL-003
Statement : The system shall prevent editing a lookup type's key after creation.
Message   : the rule's own text — ar: «لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه» ·
            en: "A lookup type's key cannot be changed after creation"
Scope     : UPDATE · Field : key · kind BUSINESS_RULE · not a form check at all
Shape     : there is **nothing to validate** — `key` is not an input on edit, because the update
            request does not carry it. The rule is expressed by the absence of the field rather
            than by a message on a control that would refuse. The form still states the rule
            beside the read-only key, so an editor learns why it cannot be changed instead of
            meeting a disabled control with no explanation.

#### F3-VALIDATION — RULE-MDL-004   traces=REQ-MDL-004,AC-MDL-004
Statement : While a lookup type is inactive, the system shall exclude its values from consumer
            reads.
Message   : the rule's own text — ar: «هذا النوع معطّل حاليًا» ·
            en: "This lookup type is currently inactive"
Scope     : the deactivate action (API-MDL-004) · Field : none — a row action ·
            kind BUSINESS_RULE · when submit
Shape     : **not a validation this form performs** — it is a consequence the deactivate
            confirmation names before the act: every consuming module stops receiving this
            type's values, and — because no activate endpoint exists — the type's `key` stays
            reserved under the platform and cannot be reused by a later registration (G7). The
            rule's text is also the state label on an inactive type row, so the same words
            explain the row and the warning. Nothing on this screen is hidden by it: the
            manager's value list still shows the values, which is the difference between this
            screen and a consumer.

Business-code fields: `key` and `code` are client-chosen strings, not platform-numbered, and both
are read-only after create per the two update DTOs. Neither is generated or predicted on the
client (SRS §3.3 numbering).
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without CREATE, UPDATE or DELETE receives `ACCESS_DENIED`
on submit and the form shows the localized message; fields are not pre-emptively disabled,
because no published surface tells this screen which actions its caller holds (ADR-MDL-012,
PF-MDL-001).
<!-- SUB:F3-SCR-MDL-001:END -->

<!-- SUB:F3-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-013,AC-MDL-013,API-MDL-010 -->
### F3 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

This screen has **no form**: SRS §B3 reads "read-only browse; no create/update here", and every
property of the response is read-only.

#### F3-FIELD — SCR-MDL-002 (filters — not a form)
ownerModuleCode · optional · the select offers the UXD-MDL-001 list; when that read is refused
                  or fails the select falls back to the distinct `ownerModuleCode` values in the
                  current API-MDL-010 response, never to free text (ADR-MDL-013, ADR-MDL-015, G6)
                  — a code that is no longer registered but still owns types is still shown in
                  the results either way, because the grouping is the server's
key             · optional · LENGTH (maxLength 50) · a LIKE filter

#### F3-VALIDATION — none on this screen
No `RULE-*` is enforced here, because nothing is written. RULE-MDL-004's effect is visible — a
deactivated type leaves this registry's active set — but the rule fires on the consumer read
(API-MDL-011), not on this screen.
Shape  : filter validation only, written with `zod` over the route's search params, so an address
         someone shared is validated exactly as a typed filter is.
Locale : session → browser → `ar`.
Permission-driven behaviour: a caller without VIEW never reaches this screen — the navigation
guard of SEC-FE stops the route before any of this runs.
<!-- SUB:F3-SCR-MDL-002:END -->
<!-- PHASE:F3:END -->

<!-- PHASE:F4:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-005,AC-MDL-009,AC-MDL-010,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 4 — F4 — Screens & Routes

One block per `SCR-*`: routes, chunk, guard, facade, components, composition and cross-module
citations. Routes are named by the container pattern — `TREE_MASTER_DETAIL` → one page hosting
the master list and the detail beside it, with the list route registered **before** any `:id`
route and every static segment registered before the parameterised ones; `FULL_PAGE` with no
entry sub-view → a single page and no entry route. One lazily-loaded chunk per composite screen.
Every `PERM_*` name below is the backend registry's, cited and never invented, and every route
sits under the module segment `/reference-data`.

<!-- SUB:F4-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-005,AC-MDL-009,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F4 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F4-SCREEN — SCR-MDL-001
Routes       : base slug `lookups`, under `/reference-data` —
               `/reference-data/lookups` — the type list, registered **before** any `:id` route ·
               `/reference-data/lookups/new` — the type entry, a **static** segment registered
               before the parameterised ones ·
               `/reference-data/lookups/:typeId` — that type's values beside the list ·
               `/reference-data/lookups/:typeId/edit` — the type entry, edit ·
               `/reference-data/lookups/:typeId/values/new` — the value entry, create ·
               `/reference-data/lookups/:typeId/values/:valueId/edit` — the value entry, edit
Chunk        : one lazy chunk for this composite screen — both panes and both entry surfaces
               share it; neither entry is a chunk of its own
Guard        : every route element guarded by `PERM_MDL_LOOKUPS_VIEW`, evaluated as "`MDL_LOOKUPS`
               is in the caller's effective menu". The write routes carry the same guard, because
               CREATE, UPDATE and DELETE are readable from no published surface and the server's
               403 is the authority on the write itself (ADR-MDL-012, PF-MDL-001)
Facade       : the SCR-MDL-001 facade of F2 — the page never calls a query directly
Components   : `LookupsPage` (route-level, TREE_MASTER_DETAIL) · `LookupTypeList`,
               `LookupTypeFilters`, `LookupTypeForm`, `LookupValuePane`, `LookupValueRow`,
               `LookupValueFilters`, `LookupValueForm`, `ValueReorderHandle`, `DeactivateConfirm`
               (presentational)
Mode         : CREATE | EDIT | VIEW resolved from the route match — `/new` and `/values/new` →
               CREATE, `/edit` → EDIT, `/:typeId` → VIEW — never from a parent's prop
Cold-load hydration (ADR-MDL-014) : the two **true edit routes** —
               `/reference-data/lookups/:typeId/edit` and
               `/reference-data/lookups/:typeId/values/:valueId/edit` — hydrate their form from
               the row their own search query already holds (ADR-MDL-005). No by-id read is
               published for either entity (PF-MDL-003: the search filter sets of API-MDL-001 and
               API-MDL-005 carry no id operator), so on a cold load — the hydrating row absent
               from cache, as when a link is opened directly rather than navigated to — each of
               these two routes redirects instead of rendering an empty form.
               `/reference-data/lookups/:typeId/values/new` is different in kind, not degree: it
               **creates** a record, so it has no row of its own to hydrate at all — nothing
               about it can be "empty" the way an edit form can. Its redirect on a cold load
               protects the **parent type's display context**, not a value record: the values
               pane it opens over renders from API-MDL-005, whose cache key is
               `[lookup-values, {lookupTypeId, code}]` and needs no cached type row to answer —
               so what a missing type in cache actually costs this route is the surrounding page
               (the type's own name and state around the pane), not the create form's own fields,
               which start blank regardless (G2).
               All three routes redirect to `/reference-data/lookups/:typeId`; if the type itself
               is not in cache either, to `/reference-data/lookups`. The destination shows the
               localized message — ar: «افتح السجل من القائمة» · en: "Open the record from the
               list" — rather than a blank editor. This needs no unpublished endpoint and no
               invented filter, and it keeps every route addressable for the in-session case it
               was written for.
               **Note (G2):** the corresponding line of `ADR-MDL-014.md` still groups all three
               routes under one "hydrate a row from cache or redirect" rule, which overstates
               `/values/new`'s own constraint. That file is outside this stage's writable set;
               the correction above is carried forward against it, in the same posture G8/G9
               already record (registry-exec-fe-mdl.md).
Composition  : the spec's line resolved to components. Nothing is inline: `LookupValuePane` is not
               a control inside `LookupTypeForm`, and the type's write carries no values, so the
               type form has nothing of the values in it. Each value is a **summary row**
               (`LookupValueRow` — code, both names, order, state) and its editor is the **second
               level**: `LookupValueForm` is a SIBLING of `LookupValuePane`, never rendered inside
               its element; it is opened from the route (`/values/new`, `/values/:valueId/edit`),
               so back closes it, dismissing closes only it and a deep link opens it, subject to
               the cold-load redirect above; it carries no scroll region of its own — the pane
               scrolls with the body it sits in
Saves        : ONE per open surface, and one surface at a time. `LookupTypeForm` submits once
               (API-MDL-002 or API-MDL-003); `LookupValueForm` submits once (API-MDL-006 or
               API-MDL-007) for its own record. The two are never open together: a value route is
               a state of the detail level, not a panel beside the open type form. **No action of
               this screen owns two calls**, so no ordered pair and no skip-if-unchanged rule
               arises. Deactivate and reorder are direct actions over a row and over the list;
               neither is a form and neither carries a second submit. The reorder handle is
               disabled whenever the detail pane's `code` filter is non-empty (G3, F2-QUERY VALUE
               REORDER). The **add-value affordance is disabled whenever the selected parent type
               is inactive** (ADR-MDL-042, G1), and `/reference-data/lookups/:typeId/values/new`
               redirects to that type's value pane rather than opening an editor that cannot
               save; both state the reason in RULE-MDL-004's own wording —
               ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive".
               Editing and reordering the type's existing values stay available (AC-MDL-004)
Cross-module : UXD-MDL-001 — the owner-module select on the type entry (fallback: empty and
               disabled, ADR-MDL-013), the owner-module filter on the master list (fallback: the
               distinct values already in the current search response, ADR-MDL-016, G1), and the
               owner column of the list. The one field on this screen whose authoritative source
               is another module
The selected type is a route param, so a type's values are a linkable address and the browser's
back gesture returns to the list. Both levels offer Deactivate and neither offers an Activate: no
endpoint exists for the second half, and the confirmation says the act is not reversible from
this screen and that the code/key stays reserved (ADR-MDL-005, G7). The drag handle submits the
whole ordered set through API-MDL-009 rather than writing one row's `sortOrder`.
<!-- SUB:F4-SCR-MDL-001:END -->

<!-- SUB:F4-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F4 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F4-SCREEN — SCR-MDL-002
Routes       : base slug `type-registry`, under `/reference-data` —
               `/reference-data/type-registry` — the only route. No `new`, no `:id`, no
               `:id/edit`: this screen addresses no record it could edit. The owner module and
               the key filter live in the route's search params, so the browse IS its address
Chunk        : one lazy chunk for this screen
Guard        : the route element guarded by `PERM_MDL_TYPE_REGISTRY_VIEW`, evaluated as
               "`MDL_TYPE_REGISTRY` is in the caller's effective menu"
Facade       : the SCR-MDL-002 facade of F2
Components   : `TypeRegistryPage` (route-level, FULL_PAGE) · `RegistryFilters`,
               `OwnerGroupSection`, `RegistryTypeTable` (presentational)
Mode         : not applicable — no CREATE, EDIT or VIEW mode to resolve; this screen writes
               nothing
Composition  : `none`, resolved: no picker, no child-row editor, no second level and no component
               opened over this page. `OwnerGroupSection` renders the groups the endpoint
               returns, in the endpoint's own grouping
Saves        : none — the screen submits nothing at all. Its one affordance is a link into
               SCR-MDL-001, which is a navigation, not a save
Cross-module : UXD-MDL-001 — here it is the grouping itself and the label on each group heading,
               not a field of a form; its degraded-source fallback is ADR-MDL-015 (G6)
Each type row links to `/reference-data/lookups/:typeId`, which is SCR-MDL-001's own route and
carries its own guard: reviewing and managing are two steps of one task, and this screen does
neither half of the second. API-MDL-011 has no component and no route here (ADR-MDL-007).
<!-- SUB:F4-SCR-MDL-002:END -->
<!-- PHASE:F4:END -->

<!-- PHASE:SEC-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-004,REQ-MDL-005,REQ-MDL-009,REQ-MDL-013 -->
## PHASE 5 — SEC-FE

The frontend half of the security model, per `SCR-*`: the navigation guard and the UI behaviour
per action. Permission names are the backend registry's and the SRS Access summary's, cited and
never redeclared. One mechanism gates both screens — the **menu gate**: the screen's page code is
present in the effective menu the security module serves this caller. Action-level grants are
published nowhere, so an action's affordance renders for a caller who holds the screen and the
server's `ACCESS_DENIED` is the authority, shown as its localized message (ADR-MDL-012). Never
split — level-1 only.

### SEC-FE · SCR-MDL-001 — اللوكبات العامة / Generic Lookups
Permissions      : `PERM_MDL_LOOKUPS_VIEW` (gateway) · `PERM_MDL_LOOKUPS_CREATE` ·
                   `PERM_MDL_LOOKUPS_UPDATE` · `PERM_MDL_LOOKUPS_DELETE`
Navigation guard : `MDL_LOOKUPS` must be in the caller's effective menu. A caller without it is
                   sent to the unauthorized destination, and every route of this screen — the
                   list, `new`, `:typeId`, `:typeId/edit` and both value routes — carries the
                   same guard.
Per action       : **VIEW** → the gate above, exact, and it covers both panes: a caller who holds
                   the screen sees types and values alike; without it no route of this screen
                   renders at all. **CREATE** (a type, and a value under it) → not readable
                   before the call; the affordance renders and a 403 is shown as the localized
                   message on the form that attempted it. **UPDATE** (edit at either level, and
                   the reorder) → the same. **DELETE** (which here means deactivate and nothing
                   else — no hard delete exists at either level) → the same.

**Filed as a platform finding, not settled here (G5).** The api-docs put `PERM_MDL_LOOKUPS_UPDATE`
on **both** deactivate endpoints, while backend-execution-plan-mdl.md PHASE 7's permission matrix
and BOOTSTRAP DATA mark the DELETE column for API-MDL-004 and API-MDL-008 and seed
`PERM_MDL_LOOKUPS_DELETE` with grant targets — a real divergence, not a naming detail: as built,
UPDATE alone lets a caller deactivate, and DELETE alone lets a caller deactivate nothing, so SRS
§B4's DELETE row is unenforceable as built. This is filed as **PF-MDL-001** (API SURFACE, above),
owner the MDL backend track, evidence the two artifacts just named. The frontend neither checks
`PERM_MDL_LOOKUPS_DELETE` nor depends on it: the guard it can evaluate is the page gate, and the
authority on a write is the server's answer to the call actually made.

The screen-level grant is the whole granularity available. Per-lookup-type permissions — letting
a role manage one type but not another — are an explicit SRS scope exception (§A2), so no
per-type gate is drawn, attempted or hinted at in the UI.
Foreign grant    : a role granted `PERM_MDL_LOOKUPS_CREATE` needs `PERM_SEC_MODULE_REGISTRY_VIEW`
                   as well, or the owner-module select it must fill stays empty and disabled. The
                   grant is the security module's to make; this plan names it and mints nothing
                   (UXD-MDL-001, ADR-MDL-013). The master-list owner-module filter needs no such
                   grant of its own: it degrades to data the search response already carries
                   (ADR-MDL-016, G1), never to a blocked control.

### SEC-FE · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
Permissions      : `PERM_MDL_TYPE_REGISTRY_VIEW`
Navigation guard : `MDL_TYPE_REGISTRY` must be in the caller's effective menu.
Per action       : **VIEW** → the gate above. There is no CREATE, UPDATE or DELETE on this
                   screen: the SRS Access summary gives it VIEW alone and the screen writes
                   nothing, so there is no affordance to hide and no submit to refuse. Its links
                   into SCR-MDL-001 render unconditionally; the target route's own guard stops a
                   caller who does not hold that screen, so a reviewer who may browse but not
                   manage sees the registry and is refused at the door of the editor rather than
                   shown a dead link.

**Across both screens.** A forbidden response is shown as its localized catalog message, never as
a silent no-op and never as a generic failure. An unauthenticated response returns the caller to
the platform's sign-in destination and discards the server-state cache, so no data of the
previous identity survives into the next. No screen composes a permission name and no screen
keeps a local copy of the caller's grants: the menu response is the single source, and a failure
to load it renders no entry of this module and grants no route of it — access narrows, never
widens.
<!-- PHASE:SEC-FE:END -->

<!-- PHASE:ALIGN-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 6 — ALIGN-FE

The alignment self-check is this phase's content. Every row names the check that backs it, and
there are no other rows: a row nothing can falsify manufactures confidence, so a dimension with
no check behind it is not asserted here at all. The `RESULT` row is written by the orchestrator
from the analyze report. Never split — level-1 only.

```
ALIGN-FE — MDL v1
row           backing check   assertion
SCREENS       orphans         every SCR is referenced by a plan block — SCR-MDL-001 and
                              SCR-MDL-002 each carry a SUB in F1, F2, F3 and F4 and a block in
                              SEC-FE
COMPOSITION   screen-composition  every SCR names where its secondary detail sits and that it
                              saves once — SCR-MDL-001: none inline, a summary row per value and
                              its editor as a second level, one submit per open surface;
                              SCR-MDL-002: none, and it submits nothing
UXD           orphans         every UXD is cited by a plan block — UXD-MDL-001 is cited by both
                              F2 SCREEN-INIT blocks, by both facades, by the F3 validator for
                              RULE-MDL-001, by both F4 Cross-module lines and by SEC-FE
TRACES        traces          every PHASE and SUB carries traces=; UXD-MDL-001 traces to its REQ
                              and its AC; every SCR traces to its REQ and its UXD
API           traces          every API this plan cites is defined in the fetched api-docs —
                              API-MDL-001..011, each carrying its own Contract ID line there
                              (ADR-MDL-011) — and never in the backend plan's contract summary.
                              No foreign module's API id is cited here at all
FOREIGN       xref-surface    every reference to another module's surface resolves in that
                              module's own artifacts. This plan writes no foreign path and no
                              foreign id: the one cross-module read is cited as UXD-MDL-001 and
                              named in ui-ux-spec-mdl.md, so this clause has no subject here
REGISTRY      registry-agree  the UXD and both SCR defined here are in registry-exec-fe-mdl.md,
                              and nothing else is
LANGUAGES     languages       labels and messages in ar + en
MARKERS       markers         the parser reports no structural or semantic error for this track
                              and plan
DECISIONS     refs-exist      every ADR this plan cites exists on disk in
                              analysis/decisions/MDL/ — ADR-MDL-002, ADR-MDL-003, ADR-MDL-004,
                              ADR-MDL-005, ADR-MDL-006, ADR-MDL-007, ADR-MDL-011, ADR-MDL-012,
                              ADR-MDL-013, ADR-MDL-014, ADR-MDL-015, ADR-MDL-016, and the two
                              superseded ones they cite
COVERAGE      (the report)    none — the analyze report lists no clause as having examined
                              nothing. Two facts sit behind that word: every clause that counts
                              its subjects counted at least one here, and `xref-surface` reports
                              no count at all, so it appears in neither the coverage map nor that
                              list. It has no subject in this plan, by the decision the API row
                              above records.
RESULT        PASSED ✓ — 0 findings
```

### Operations coverage

| Operation | API | SCR action | Route | Status |
|---|---|---|---|---|
| search lookup types | API-MDL-001 | SCR-MDL-001 · type search | `/reference-data/lookups` | ✓ |
| create lookup type | API-MDL-002 | SCR-MDL-001 · type entry, create | `/reference-data/lookups/new` | ✓ |
| update lookup type | API-MDL-003 | SCR-MDL-001 · type entry, edit | `/reference-data/lookups/:typeId/edit` | ✓ |
| deactivate lookup type | API-MDL-004 | SCR-MDL-001 · deactivate a type | `/reference-data/lookups/:typeId` | ✓ |
| search a type's values | API-MDL-005 | SCR-MDL-001 · the values pane | `/reference-data/lookups/:typeId` | ✓ |
| create lookup value | API-MDL-006 | SCR-MDL-001 · value entry, create | `/reference-data/lookups/:typeId/values/new` | ✓ |
| update lookup value | API-MDL-007 | SCR-MDL-001 · value entry, edit | `/reference-data/lookups/:typeId/values/:valueId/edit` | ✓ |
| deactivate lookup value | API-MDL-008 | SCR-MDL-001 · deactivate a value | `/reference-data/lookups/:typeId` | ✓ |
| reorder a type's values | API-MDL-009 | SCR-MDL-001 · drag to reorder (disabled while filtered, G3) | `/reference-data/lookups/:typeId` | ✓ |
| browse the registry by owner | API-MDL-010 | SCR-MDL-002 · the browse itself | `/reference-data/type-registry` | ✓ |
| read active values by key | API-MDL-011 | — a consuming module's backend call | — (ADR-MDL-007) | ✗ |
| read one type by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |
| read one value by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |

Ten of the eleven published endpoints carry a route and a ✓. Three rows carry a ✗ with the ADR
that explains it: one endpoint published for a caller that is not this frontend, and two
operations the SRS names for which nothing is published. No row is a ✗ for want of a decision,
and no row carries an empty route without one.
<!-- PHASE:ALIGN-FE:END -->

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
