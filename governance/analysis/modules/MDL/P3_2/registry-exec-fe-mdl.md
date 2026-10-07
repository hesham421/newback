## REGISTRY — P3.2 — MDL v1
══════════════════════════════════════════════════════════════════
Module : MDL (البيانات المرجعية / Master Data Lookup)   Version : v1   Profile : erp
Source : analysis/modules/MDL/P3_2/ui-ux-spec-mdl.md · frontend-execution-plan-mdl.md
══════════════════════════════════════════════════════════════════

ID RANGES
UXD-MDL-001 .. UXD-MDL-001 · SCR-MDL-001 .. SCR-MDL-002

SCR ids : SCR-MDL-001, SCR-MDL-002
UXD ids : UXD-MDL-001
Last sequence per atom : SCR 002 · UXD 001

Neither sequence restarts and nothing is renumbered: both are the assignment this module version
already carries, cited unchanged by the decisions on disk (ADR-MDL-003, ADR-MDL-004, ADR-MDL-007)
and by every downstream artifact that names them. This run mints no new `SCR-*` or `UXD-*` id —
the two screens the SRS declares and the one cross-module display dependency are the same two and
the same one; it mints one new ADR id (ADR-MDL-016) against this same unchanged set.

SCREENS
| SCR | الاسم / Name | Owning ENT | Container pattern | Permissions |
|---|---|---|---|---|
| SCR-MDL-001 | اللوكبات العامة / Generic Lookups | ENT-MDL-001 (+ ENT-MDL-002) | TREE_MASTER_DETAIL | PERM_MDL_LOOKUPS_VIEW (gateway), PERM_MDL_LOOKUPS_CREATE, PERM_MDL_LOOKUPS_UPDATE, PERM_MDL_LOOKUPS_DELETE |
| SCR-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | ENT-MDL-001 | FULL_PAGE — no entry sub-view (ADR-MDL-003) | PERM_MDL_TYPE_REGISTRY_VIEW |

Two composite screens, two page codes (MDL_LOOKUPS, MDL_TYPE_REGISTRY), and one `SUB` per screen
in each of the four sub-bearing phases — eight per-screen blocks, plus a level-1 block per screen
in SEC-FE. The permission names are the backend registry's; this stage cites them and declares
none. **PF-MDL-001** (frontend-execution-plan-mdl.md, API SURFACE) records that the api-docs
require `PERM_MDL_LOOKUPS_UPDATE`, not `PERM_MDL_LOOKUPS_DELETE`, on both deactivate endpoints —
filed against the backend track, not corrected in this table, which states the SRS §B4 grant as
written.

COMPOSITION
| SCR | Secondary detail | Placement | Submits |
|---|---|---|---|
| SCR-MDL-001 | the selected type's values — records of a second entity, absent from every type write DTO | none inline · a summary row per value, its editor a second level opened from the route (with a cold-load redirect when the hydrating row is not in cache, ADR-MDL-014), rendered as a sibling and carrying no scroll region of its own | one per open surface, one surface at a time; no action of this screen owns two calls; the reorder submits only the type's complete, unfiltered value set (G3) |
| SCR-MDL-002 | none — a read-only browse | none | none — the screen writes nothing |

UXD INDEX
| UXD | Screen(s) | Field | Owner module · API used |
|---|---|---|---|
| UXD-MDL-001 | SCR-MDL-001, SCR-MDL-002 | `ownerModuleCode` — the type's owning module | SEC · `ModuleRegistry` (ENT-SEC-004), read through the security module's registry search, named in ui-ux-spec-mdl.md and cited by no id inside the frontend plan (ADR-MDL-004, ADR-MDL-011) |

One `UXD-*` for the whole module, and the same shared, long-lived hook serves both screens and
every control on them. It is **not** a lookup dependency: SRS §A6 records that MDL introduces no
coded list of its own, and the owner-module field's valid set is another module's registry data.
It is not the backend's cross-module record either: that one is the server-side existence check
behind RULE-MDL-001 and appears in no frontend artifact.
Grant that travels with it: every role granted `PERM_MDL_LOOKUPS_CREATE` must also hold
`PERM_SEC_MODULE_REGISTRY_VIEW`, or the owner-module select stays empty and disabled and creation
is blocked behind it. The grant is the security module's to make (ADR-MDL-013).
Degraded source, one per control, not one per screen: the create-form select on SCR-MDL-001
degrades to empty-and-disabled (ADR-MDL-013) — the only one of the three controls where a
submitted value could reach the server unvalidated. Both read-only filters degrade instead to the
distinct `ownerModuleCode` values already present in their own screen's current search response —
SCR-MDL-001's master-list filter falls back to the current API-MDL-001 response (ADR-MDL-016, new
this run, G1), and SCR-MDL-002's registry filter falls back to the current API-MDL-010 response
(ADR-MDL-015, G6) — never to free text, and never blocking browsing.

API COVERAGE
| Status | Count | API ids |
|---|---|---|
| used by this frontend | 10 | API-MDL-001 … API-MDL-010 |
| documented, deliberately uncalled | 1 | API-MDL-011 — the consumer read a consuming module's backend performs over the platform's in-process interface; bound in F2 and drawn on no screen (ADR-MDL-007) |
| used but undocumented | 0 | no endpoint is called that the api-docs do not publish |
| documented but unbound | 0 | all 11 published endpoints are bound, each cited by the `Contract ID` the api-docs now publish for it (ADR-MDL-011, superseding ADR-MDL-008) |

RESPONSE SHAPES — the difference is load-bearing and is stated per block in F2 — corrected in a
prior run (G2): API-MDL-005 moved from paginated to bare array. An earlier revision modelled it as
paginated on no published source; `_inputs/api-docs-mdl.md`, backend-execution-plan API-MDL-005
and QR-MDL-005 (`Pagination: NO`) and SRS §B2 all agree it is unpaged.

| Shape | API ids |
|---|---|
| paginated (`PageLookup<T>`) | API-MDL-001 |
| a bare array | API-MDL-005, API-MDL-009, API-MDL-010, API-MDL-011 |
| a single object | API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-006, API-MDL-007, API-MDL-008 |
Only API-MDL-001 carries page and size in its cache key. API-MDL-005 and API-MDL-010 accept
`filters` alone — no sort and no paging — so neither belongs in either one's key.

SHAPE DIFFS AGAINST THE BACKEND PLAN
Three reads are `POST …/search` where `backend-execution-plan-mdl.md`'s contract summary still
predicts `GET`: API-MDL-001, API-MDL-005, API-MDL-010. The api-docs and the SRS §B5 tables agree
with each other, so the backend plan is the one artifact that lags; `gov.py analyze` reports it
there (C8.4). Correcting it is three rows in a P3.1 artifact, outside this stage's boundary —
recorded here and in ADR-MDL-002, not silently corrected.

FIELD PRECISION DIFF (G1, a prior run) — `key`/`code` and `nameAr`/`nameEn` maxLength is bound to
db-script §1 and ADR-MDL-010 (50 / 200) rather than to any conflicting number
`_inputs/api-docs-mdl.md` may publish (80 / 150); filed as PF-MDL-002 in
frontend-execution-plan-mdl.md against the MDL api-doc generator, with the db-script column
widths as evidence.

OPERATIONS WITHOUT AN ENDPOINT
read one lookup type by id · read one lookup value by id — named by SRS Part B, required by no
`REQ-*`, and omitted from the frontend rather than faked; both entry sub-views hydrate from the
row the search query already holds when it is present, and redirect to the parent surface on a
cold load when it is not (ADR-MDL-005, ADR-MDL-014). The value-create route (`/values/new`) is a
special case of that redirect, not a third kind of hydration: it creates a record and has no row
to hydrate at all, so its cold-load redirect protects the parent type's display context, not a
value record (ADR-MDL-014, corrected this run — G2). This SRS version names no `activate` action
at either level, so deactivation being one-way is the SRS's own statement, not an omission.

LOOKUPS
None. MDL introduces no domain-specific coded list of its own — it is the generic mechanism every
other module's lookup types run on. No lookup hook exists anywhere in the plan, no validator binds
an option set, and no enum is modelled. The one constrained field, `ownerModuleCode`, binds to the
runtime-loaded list of UXD-MDL-001.

ALIGN
Verdict as stamped in frontend-execution-plan-mdl.md → ALIGN-FE → the `RESULT` line (written by
the orchestrator from the analyze report).

Findings fixed in this revision (gate `pass-2`, third round): a shared degraded-source fallback
that left SCR-MDL-001's own master-list `ownerModuleCode` filter undocumented and defaulting, by
the nearest stated text, to the create-form select's empty-and-disabled treatment — even though
that select's failure grant (`PERM_SEC_MODULE_REGISTRY_VIEW`) is tied to CREATE alone, so a
VIEW-only caller was denied a read-only filter their own permissions never gated (this round's
G1, ADR-MDL-016, new); and an overstated cold-load hydration rule that grouped the value-create
route's redirect with the two true edit routes' record-hydration rule, when the create route has
no record to hydrate at all and its redirect protects the parent type's display context instead
(this round's G2, documentation-only, no ADR). The corresponding line in `ADR-MDL-014.md` needs
the same G2 correction and remains open against that file, which this stage's writable set does
not include — carried forward in the same posture G8/G9 already record below.

Findings fixed in earlier revisions of this same gate, kept for the record: the six-way
field-precision divergence from the deployed schema (G1, round 1), the detail read modelled as
paginated on no published source (G2, round 1), a reorder submittable from a partial pane (G3,
round 1), an unbuildable cold-load deep link (G4, round 1, ADR-MDL-014), a cross-artifact
permission divergence settled in prose instead of filed (G5, round 1, PF-MDL-001), a silent
degraded-source gap on the SCR-MDL-002 owner filter (G6, round 1, ADR-MDL-015), the unstated
permanence of a deactivated code/key (G7, round 1), a stale ADR-MDL-008 status header and an
ADR-MDL-005 Context-table citation of a superseded SRS line (G8, G9, round 1 — filed against the
ADR files themselves, outside that pass's writable set), an unstated optimistic-reorder failure
path (G10, round 1), an unconsumed F1 model for the consumer read (G11, round 1), a server-side
reorder invariant gap settled client-side instead of filed (G1, round 2, PF-MDL-004), a backend
test naming the wrong surface and an unconstructible failure mode (G2, round 2, PF-MDL-005), an
undisclosed cross-module registration gap on SEC's side carried as ACTIVE with no outstanding
condition (G3, round 2, PF-MDL-006), the UNIQUE_CHECK line requesting an EQUALS filter the
backend never honours (G6, round 2), and a `sortField` modelled for an ordering the backend
cannot produce (G7, round 2). G4 and G5 of round 2 land outside this stage's writable files
(srs-mdl.md and ADR-MDL-008.md respectively) and remain carried, not applied — the same posture
G8/G9 and this round's ADR-MDL-014 correction already state. No new ADR was raised in round 2;
this round raises exactly one (ADR-MDL-016), because G1 of this round needed a choice among two
live, already-accepted patterns (empty-and-disabled vs. current-response fallback) rather than a
restatement of an existing decision.

ADRs
analysis/decisions/MDL/ADR-MDL-002.md (ACCEPTED — three reads are POST `…/search`; the backend
contract summary is what lags) ·
analysis/decisions/MDL/ADR-MDL-003.md (ACCEPTED — container pattern for the read-only browse) ·
analysis/decisions/MDL/ADR-MDL-004.md (ACCEPTED — the owner-module field reads the security
module's registry; UXD-MDL-001) ·
analysis/decisions/MDL/ADR-MDL-005.md (ACCEPTED — operations with no published endpoint are
omitted, not faked) ·
analysis/decisions/MDL/ADR-MDL-006.md (ACCEPTED — `isActiveFl` read-only at both levels) ·
analysis/decisions/MDL/ADR-MDL-007.md (ACCEPTED — API-MDL-011 bound and called by no screen) ·
analysis/decisions/MDL/ADR-MDL-011.md (ACCEPTED — the re-fetched api-docs publishes a Contract ID
per endpoint, so the plan cites API ids; supersedes ADR-MDL-008) ·
analysis/decisions/MDL/ADR-MDL-012.md (ACCEPTED — no published surface tells a screen which
actions its caller holds) ·
analysis/decisions/MDL/ADR-MDL-013.md (ACCEPTED — the grant that travels with UXD-MDL-001's
create-form select, and that select's own degraded-source behaviour) ·
analysis/decisions/MDL/ADR-MDL-014.md (ACCEPTED — cold-load redirect for the deep-linked entry
routes; its own text still owes the G2 correction this round applies in
frontend-execution-plan-mdl.md, carried forward against the file itself) ·
analysis/decisions/MDL/ADR-MDL-015.md (ACCEPTED — the SCR-MDL-002 degraded-source fallback for
UXD-MDL-001's registry filter, G6) ·
analysis/decisions/MDL/ADR-MDL-016.md (ACCEPTED, raised this run — SCR-MDL-001's own master-list
owner-module filter falls back to the current search response's distinct values rather than
sharing the create-form select's empty-and-disabled behaviour, G1)
Superseded, kept on disk and cited only by the decisions that replaced them: ADR-MDL-001,
ADR-MDL-008 — the latter's own status header remains to be corrected on disk (G8, outside this
pass's writable files). Carried from earlier stages: ADR-MDL-009 and ADR-MDL-010 (P2) touch no
frontend artifact and are not applied here. No BLOCKED ADR — the pass was not stopped, and no
question was raised.

TRACEABILITY
REQ reached by ≥1 F-block: 13/13 — REQ-MDL-001 … REQ-MDL-010 appear in the `traces=` of
SCR-MDL-001's blocks, and REQ-MDL-011, REQ-MDL-012, REQ-MDL-013 in SCR-MDL-002's. Orphan REQ:
none.
REQ traced by an `SCR-*` or `UXD-*` record: **11/13**, and the two numbers are not in conflict.
REQ-MDL-011 and REQ-MDL-012 are the consumer's read by key — a server-to-server call with no
screen (ADR-MDL-007) — so they are reached by the F2 block that binds API-MDL-011 (and by that
block alone — no F1 model duplicates it, G11) and by the values and active flags managed on
SCR-MDL-001 that decide what that read returns, and they are traced by no screen, because no
screen implements them. The analyze report's `req-ux` ratio says 84.6% for exactly this reason;
inflating it would mean asserting a screen that does not exist.
AC covered: 13/13 — each AC accompanies its REQ in the same traces.
SCR covered: 2/2 — each carries a SUB in F1, F2, F3 and F4 and a block in SEC-FE.
UXD cited by an F-block: 1 of 1 — none unreferenced, none dangling.
Screen operations the SRS names, resolved to a route: 8/8 on SCR-MDL-001 (search, create, read,
update, deactivate, reorder across both levels) and 2/2 on SCR-MDL-002 (search, browse).

PLATFORM FINDINGS (frontend track, this run)
| id | status | subject |
|---|---|---|
| PF-MDL-001 | OPEN | deactivate-endpoint permission (UPDATE vs DELETE) divergence — G5, round 1 |
| PF-MDL-002 | OPEN (conditional) | api-docs field-precision divergence from the db-script — G1, round 1 |
| PF-MDL-003 | OPEN | no by-id read published for either entity, blocking cold-load hydration — G4, round 1 |
| PF-MDL-004 | OPEN | QR-MDL-009 checks only type membership per id, never that the submitted set is the type's complete, non-duplicated value set — G1, round 2, owner MDL backend track (P3.1) |
| PF-MDL-005 | RESOLVED | TC-MDL-014 named the wrong surface for an unconstructible in-process failure mode; retargeted by the test-gen stage to RULE-MDL-001's own Test-Hint, keeping its id (ADR-MDL-024) |
| PF-MDL-006 | OPEN | SEC's own P3.1 artifacts register only `SecUserDirectoryApi`, leaving XM-MDL-001's module-registry read unregistered on SEC's side while the XM is carried ACTIVE — G3, round 2, owner SEC / P3.1 track |
| PF-MDL-007 | OPEN | a value can be written under a deactivated type: API-MDL-006's orchestration checks existence, not state, and RULE-MDL-004 scopes only to reads. The row is then unreachable for good and reserves its code. Server-side fix owed by P3.1; the client half is ADR-MDL-042 |
| PF-MDL-008 | OPEN | thirteen ADR files (ADR-MDL-017..022, 025..031) carry a paragraph as title, status RESOLVED-IN-DIALOGUE and no decision body. Owner: the factory's dialogue-to-ADR writer. The two substantive ones are restated as ADR-MDL-043, ADR-MDL-044 |
No new platform-finding row is filed this round: G1 and G2 of this round are both corrected
within this stage's own writable files (frontend-execution-plan-mdl.md, ui-ux-spec-mdl.md), with
G2's mirror correction in `ADR-MDL-014.md` carried forward rather than filed as a platform
finding, since it names no other track as owner — it is this same track's own file, just outside
this pass's writable set.
Full text of each in frontend-execution-plan-mdl.md, API SURFACE.

Event
"P3.2 revised (gate pass-2, third round): MDL v1 — 2 screens, 1 UXD, 11/11 API bound (10
called), 1 new ADR (ADR-MDL-016), 0 new platform-findings rows filed, 2 findings (G1, G2 of this
round) applied — G2's ADR-MDL-014 mirror correction lands outside this stage's writable files and
is carried forward, alongside G4/G5 of round 2 and G8/G9 of round 1"
══════════════════════════════════════════════════════════════════
