# PASS 2 — module MDL v1 — bundled session (1 stages, one commit per stage)

- `P3.2` Frontend — UX Design + Execution Plan — questions forbidden

==============================================================================
# BRIEF — stage `P3.2` (Frontend — UX Design + Execution Plan) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/MDL/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: UXD, SCR — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `governance-shared/erp/modules/MDL/P3_2/flow-diagram-mdl.md`
- `governance-shared/erp/modules/MDL/P3_2/ui-ux-spec-mdl.md`
- `governance-shared/erp/modules/MDL/P3_2/frontend-execution-plan-mdl.md`
- `governance-shared/erp/modules/MDL/P3_2/registry-exec-fe-mdl.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C8** real API docs (consumer repo input) → frontend: C8.1 exists {'input': 'api-docs'} [CRITICAL]; C8.2 registry-agree {'artifact': 'api-docs', 'registry': 'registry-exec-be', 'kinds': ['API'], 'direction': 'artifact→registry'} [MAJOR]; C8.3 registry-agree {'artifact': 'api-docs', 'registry': 'registry-exec-be', 'kinds': ['API'], 'direction': 'registry→artifact'} [MAJOR]; C8.4 endpoint-agrees {'artifact': 'backend-execution-plan', 'source': 'api-docs', 'kind': 'API'} [MAJOR]
- **C9** frontend design + execution plan → split: C9.1 markers {'artifact': 'frontend-execution-plan', 'track': 'frontend', 'plan': 'exec'} [CRITICAL]; C9.2 traces {'from': 'frontend-execution-plan', 'blocks': ['PHASE', 'SUB'], 'min': 1} [MAJOR]; C9.3 traces {'from': 'UXD', 'to': ['REQ', 'AC'], 'min': 1} [MAJOR]; C9.4 traces {'from': 'SCR', 'to': ['REQ', 'UXD'], 'min': 1, 'mode': 'any'} [MAJOR]; C9.5 traces {'from': 'frontend-execution-plan', 'to': ['API'], 'defined_in': 'api-docs'} [CRITICAL]; C9.6 orphans {'kind': 'UXD', 'referenced_by': ['frontend-execution-plan'], 'min': 1} [MAJOR]; C9.7 orphans {'kind': 'SCR', 'referenced_by': ['frontend-execution-plan'], 'min': 1} [MAJOR]; C9.8 registry-agree {'artifact': ['ui-ux-spec', 'frontend-execution-plan'], 'registry': 'registry-exec-fe', 'kinds': ['UXD', 'SCR']} [MAJOR]; C9.9 ids-owned {'stage': 'P3.2'} [CRITICAL]; C9.10 no-questions {'stage': 'P3.2'} [CRITICAL]; C9.11 ids-continue {'stage': 'P3.2'} [CRITICAL]; C9.13 xref-surface {'artifact': ['frontend-execution-plan'], 'locator': 'stack.backend.api.base_path', 'kinds': ['API']} [MAJOR]; C9.14 languages {'stage': 'P3.2'} [MAJOR]; C9.12 verdict-agrees {'artifact': ['frontend-execution-plan'], 'spec': 'self_check', 'when': 'profile.self_check'} [CRITICAL]

---
# ENGINE
```
ENGINE        : P3.2 — Frontend — UX Design + Execution Plan
PASS / TRACK  : pass 2 · track frontend · lane analysis · questions forbidden
MODULE        : MDL · v1 · profile erp (ERP Platform)
READS         : srs · prd · api-docs · registry-srs · registry-exec-be
                (_state/ for current state; _inputs/api-docs-mdl.md for the real API surface)
PRODUCES      : flow-diagram-mdl.md · ui-ux-spec-mdl.md · frontend-execution-plan-mdl.md · registry-exec-fe-mdl.md   — ONE run, ONE input set
OWNS IDS      : UXD, SCR
NEXT          : gate:pass-2   (the orchestrator owns the completion protocol — shared/GOVERNANCE-CORE.md)
BOUNDARY      : analysis-only — design artifacts and specifications, never a build
```

# Frontend — UX Design + Execution Plan — engine reference

## 0. Position and authority

One engine, one run, two internal parts that share the same input set:

- **Part A — UX design** (§2): from the SRS (functional ceiling) and the PRD (priority and
  intent) it produces the flow diagram and the ui-ux-spec, minting `SCR-*` and `UXD-*`.
- **Part B — frontend execution plan** (§3): bound to the **real** API surface in
  `_inputs/api-docs-mdl.md` (published by the backend repo after implementation —
  `factory.repos.backend.publishes`), organised by the profile's frontend phases, plus the
  registry.

Authority order: SRS `REQ/AC` are the functional ceiling; the api-docs are the only source
for endpoint shape; the PRD informs sequencing and priority; Part A's spec is strong design
intent for Part B — never a licence to add a field, rule or permission the SRS does not
have. A conflict is a finding (ADR — §7), never a silent resolution. The backend execution
plan's contract summary is **never** read as an API source (it may be opened for `DBF`/catalog
code lookup only).

Questions are `forbidden`. Ambiguity → `factory.yaml → ambiguity` (§7). No human
approval sits inside this engine: the human decision is the `gate:pass-2` gate.

**Delta versions** (v2+) emit only ADDED / MODIFIED / REMOVED blocks +
`change-manifest.md` against `_state/`; `SCR/UXD` sequences continue,
never renumbered. Rules: shared/VERSIONING.md.

## 1. Inputs and entry check

| Input | Read from | Use |
|---|---|---|
| `srs` | `_state/current-srs.md` | REQ/AC (EARS + Given/When/Then), ENT, RULE with messages, screen entries, permission matrix, lookup keys |
| `prd` | `_state/current-prd.md` | US-* priority, intent, navigation expectations |
| `api-docs` | `_inputs/api-docs-mdl.md` | real endpoints, DTOs, paging + error envelope, runtime error codes |
| `registry-srs` | `_state/current-registry-srs.md` | ID ranges, screen entries, shared entities |
| `registry-exec-be` | `_state/current-registry-exec-be.md` | API-* ranges, catalog codes, XM status, permission names declared by the backend plan |

Entry: the orchestrator's `fetch-inputs` already refused the pass when
`factory.passes.2.required_inputs` are missing — this engine does not re-gate. It does
run one **reconciliation of api-docs against the SRS** (§3.0) before any F-content.

## 2. Part A — UX design (flow diagram + ui-ux-spec)

### A.0 Role

Translate the approved functional truth into navigation and component **intent**. Never
invent scope, business rules or permissions; never fix an SRS↔PRD contradiction by choosing
an interpretation (§7 decides). Final component names, code and routing are Part B's.

### A.1 Screens — `SCR-*`

Mint one `SCR-*` per screen the SRS declares (`SCR-MDL-{seq}`,
traces → REQ + UXD).
**Composite-screen rule** (`profile.conventions.composite_screen`): Search + Entry (or Master +
Detail, Wizard) are ONE screen with ONE `SCR-*`; the sub-views are UX sub-screens under it.
Each additional independent composite gets its own `SCR-*`.

### A.2 Flow diagram — `flow-diagram-mdl.md`

One flow block per navigation path (identified by its starting `SCR-*` + a short name; flows
carry no atom of their own):
```
FLOW — <name>                                   traces=US-MDL-<seq>,REQ-MDL-<seq>,SCR-MDL-<seq>
Screens   : SCR-MDL-<seq> [, SCR-MDL-<seq> …]
Sequence  : <entry> → <screen A> → <screen B> → <exit>
Trigger   : <what gets the user here>
Priority  : <from the PRD, if stated>
```
Every flow cites a `US-*` **and** an `SCR-*`. A flow with no SRS-backed screen is inventing
navigation → ADR, not silently included.

### A.3 UI/UX spec — `ui-ux-spec-mdl.md`

One block per `SCR-*`, fields and permissions copied from the SRS (no additions, no omissions):
```
## SCR-MDL-<seq> — <name>                 traces=REQ-MDL-<seq>,AC-MDL-<seq>[,UXD-MDL-<seq>]
UI pattern        : <from the SRS screen entry — do not change>
Sub-views         : Search · Entry (· Detail · Wizard) under this ONE SCR
Fields shown      : <every SRS field of the owning ENT — label per language (ar/en), read-only flags>
Permissions       : <SRS matrix rows for this screen — reference only> — names follow `PERM_<PAGE_CODE>_<ACTION>`, gateway `VIEW`
Cross-module data : <field → UXD-MDL-<seq> (owner module)> | none
States            : empty · loading · error (generic — catalog codes are Part B's) · offline (if the SRS says so)
```

### A.5 Cross-module display dependencies — `UXD-*`

`UXD-*` (`UXD-MDL-{seq}`, traces → REQ + AC)
names an application-layer need: a screen owned by **this** module displays data whose
authoritative source is another module's real API. It is minted the moment such a field is
drafted, keyed by the module owning the **screen**, recorded in the spec block and in the
registry. It is not a DB constraint, shares nothing with `XM-*`, and never appears in
backend artifacts.

Lifecycle: minted here → cited (never reassigned) by the F-blocks of Part B → verified at the
`gate:pass-2` gate by `gov.py analyze`: every `UXD-*` must be referenced by an F-block and
every referenced one must exist (unreferenced or dangling = MAJOR).

### A.6 Reconciliation self-check (SRS B1–B4 ↔ draft) — no human gate

Run before Part B, on the whole draft:
```
RECONCILIATION — MDL v1
B1 every US-* used in a flow has an SRS counterpart (REQ/AC/screen)   → none: ADR (no invented screen), flow excluded
B2 no RULE-* contradicts a flow/spec outcome                           → contradiction: both texts verbatim in an ADR (breaking → BLOCKED)
B3 every field/permission on a screen exists in the SRS               → extra: removed; missing: added
B4 every screen entry of the SRS has exactly one SCR-* block          → gap: block added
RESULT  reconciled <n> · reworked <n> (bounded to flagged blocks) · ADRs <list>
```

## 3. Part B — frontend execution plan

### 3.0 Binding to the real API surface

Before writing any phase, bind this plan to `_inputs/api-docs-mdl.md`. Record only
what exists in neither source alone — the shapes stay where they are published:
```
API SURFACE — MDL v1   (shapes: api-docs-mdl.md — cited by API-* id, never restated)
BINDING     REQ-MDL-<seq> → API-MDL-<seq> — the binding only; verb, path, request/response
            DTO, paging and envelope are read there, not copied here
UNMAPPED    REQ needing an endpoint that has none · documented endpoint mapping to no REQ → ADR
CODES       runtime error code → RULE-MDL-<seq> — the link neither the api-docs nor the SRS carries
```
Reconcile once against the SRS: every REQ that needs an endpoint has one (missing/renamed
→ ADR — naming diffs continue, a missing core operation is breaking); every documented
endpoint maps to a REQ (unknown → ADR, never silently used). A value not in the api-docs is
never invented — mark `PENDING ADR-<id>`.

### 3.1 Markers, thresholds, traces

Grammar: `factory.markers` (schema v2, syntax `html-comment`) —
`<!-- KIND:ID:START [traces=…] -->` … `<!-- KIND:ID:END -->`. Kinds allowed in a `frontend` execution plan:

| Kind | Level | Allowed parents | Notes |
|---|---|---|---|
| `PHASE` | 1 | — (top level) | keys from `profile.tracks.<track>.plans.<plan>.phases` |
| `SUB` | 2 | PHASE | id = `{PHASE-KEY}-{SCR-ID}` — always phase-qualified |

- No atom kind is carried by this track: `API-*` and `XM-*` are backend-owned and only cited.
  The unit of addressing here is the **SUB per screen** in `sub_bearing` phases.
- `traces=` on **every** PHASE and SUB block: the
  `REQ/AC/API/UXD/SCR` IDs the block implements (grammar `{prefix}-{MOD}-{seq}`,
  3-digit seq). A PHASE traces to the union of its SUBs.
- The same screen legitimately appears under several phases; without the `{PHASE-KEY}-`
  prefix the SUB ids would collide — the prefix is mandatory, always.
- First line of a phase = its START marker; last = END. Threshold checked **while** writing.
  Unknown key → the toolkit refuses (`refuse`).
- Headings with the word PHASE use a profile key only; index, ALIGN table (unless a phase),
  registry and hand-off are trailing content after the last END. Protocol: shared/MARKER-PROTOCOL.md.

Phase table for `profile.tracks.frontend.plans.exec` (plan order):

| # | Key | Display | Split rule | Per-screen SUB |
|---|---|---|---|---|
| 1 | `F1` | F1 — Models & Types | always — one SUB per screen | yes — `SUB:F1-SCR-MDL-<seq>` |
| 2 | `F2` | F2 — Data Hooks | always — one SUB per screen | yes — `SUB:F2-SCR-MDL-<seq>` |
| 3 | `F3` | F3 — Forms & Validators | always — one SUB per screen | yes — `SUB:F3-SCR-MDL-<seq>` |
| 4 | `F4` | F4 — Screens & Routes | always — one SUB per screen | yes — `SUB:F4-SCR-MDL-<seq>` |
| 5 | `SEC-FE` | SEC-FE | never split | no |
| 6 | `ALIGN-FE` | ALIGN-FE | never split | no |


### 3.2 Content roles

The profile names the phases; the engine supplies content **by role**, matched on the words
in the phase display ("Models & Types", "Data Hooks", "Screens & Routes",
security, alignment). A phase matching no role is filled as the profile describes it. Stack
facts come from `profile.stack.frontend`: framework `react-ts-vite`; libraries — routing: `react-router`, server-state: `tanstack-query`, forms: `react-hook-form`, validation: `zod`, state: `zustand`; lazy chunk per `composite-screen`.

**RF1 — Models & types.**
Field/DTO binding : see _inputs/api-docs-mdl.md — the published request/response
                     shapes for this module are the source, not restated here.

**RF2 — Data hooks.** Declares WHAT each screen needs from the API — not hook code:
```
### <role>-QUERY — API-MDL-<seq>            traces=API-…,REQ-…
Kind (read query | mutation) — verb, path and request/response shape are cited by the API-* id above, never restated
Cache key    : [resource, filters] — every filter that changes the response is in the key
Errors       : catalog code → routing (field validation → inline · business rule → user message · unauthenticated → login · forbidden → unauthorized · server → generic)
Loading      : NONE | LOCAL | GLOBAL (GLOBAL only when the SRS says the call is slow → ADR)
Cache policy : defaults | <stale/gc values> (deviation → ADR)
Invalidation : keys refreshed on success (mutations MUST declare this)
### <role>-LOOKUP — <lookup key>     endpoint · key · options shape (code + label per language) · ONE hook per key, shared across screens · long-lived cache
### <role>-SCREEN-INIT — SCR-MDL-<seq>   permission read for the screen (VIEW/CREATE/UPDATE/DELETE) · lookups used · entity-by-id when editing
### <role>-FACADE — SCR-MDL-<seq>         composes the queries above · state it owns (list from query data, selection, filters incl. page/size, derived loading) · imperative operations (create/update/deactivate with usage check first)
```
State rule: page and page size live **inside** the filter object that forms the cache key —
never as independent state. Components use the facade only; the facade uses the declared
queries only (server-state library: `tanstack-query`).

**RF4 — Screens & routes.** One block per `SCR-*`:
```
### <role>-SCREEN — SCR-MDL-<seq>            traces=REQ-…,UXD-…,API-…
Guard        : every route element guarded by its permission (`PERM_<PAGE_CODE>_<ACTION>` from the SRS matrix — never invented here)
Facade       : the RF2 facade of this screen · pages never call queries directly
Cross-module : UXD-* cited for every foreign-data field (missing → ADR, never minted here)
```

**RF5 — Security (frontend half).** Per `SCR-*`: navigation guard (no `VIEW` → unauthorized redirect) and UI behaviour per action (no VIEW → its affordance hidden / read-only; no CREATE → its affordance hidden / read-only; no UPDATE → its affordance hidden / read-only; no DELETE → its affordance hidden / read-only); forbidden responses shown as the localized catalog message. Permission names are the backend registry's — never redeclared.

**RF6 — Alignment.** The ALIGN table (§4) as the alignment-role phase content (never
split); trailing content if the profile has no such phase.

### 3.3 Phase-by-phase

#### PHASE 1 — `F1` (F1 — Models & Types)
- `<!-- PHASE:F1:START traces=… -->` … `<!-- PHASE:F1:END -->`; content = the roles whose words appear in "F1 — Models & Types", else as the profile describes.
- Per-screen SUB: `<!-- SUB:F1-SCR-MDL-<seq>:START traces=… -->` for **every** `SCR-*`.

#### PHASE 2 — `F2` (F2 — Data Hooks)
- `<!-- PHASE:F2:START traces=… -->` … `<!-- PHASE:F2:END -->`; content = the roles whose words appear in "F2 — Data Hooks", else as the profile describes.
- Per-screen SUB: `<!-- SUB:F2-SCR-MDL-<seq>:START traces=… -->` for **every** `SCR-*`.

#### PHASE 3 — `F3` (F3 — Forms & Validators)
- `<!-- PHASE:F3:START traces=… -->` … `<!-- PHASE:F3:END -->`; content = the roles whose words appear in "F3 — Forms & Validators", else as the profile describes.
- Per-screen SUB: `<!-- SUB:F3-SCR-MDL-<seq>:START traces=… -->` for **every** `SCR-*`.

#### PHASE 4 — `F4` (F4 — Screens & Routes)
- `<!-- PHASE:F4:START traces=… -->` … `<!-- PHASE:F4:END -->`; content = the roles whose words appear in "F4 — Screens & Routes", else as the profile describes.
- Per-screen SUB: `<!-- SUB:F4-SCR-MDL-<seq>:START traces=… -->` for **every** `SCR-*`.

#### PHASE 5 — `SEC-FE` (SEC-FE)
- `<!-- PHASE:SEC-FE:START traces=… -->` … `<!-- PHASE:SEC-FE:END -->`; content = the roles whose words appear in "SEC-FE", else as the profile describes.
- Never split — level-1 only.

#### PHASE 6 — `ALIGN-FE` (ALIGN-FE)
- `<!-- PHASE:ALIGN-FE:START traces=… -->` … `<!-- PHASE:ALIGN-FE:END -->`; content = the roles whose words appear in "ALIGN-FE", else as the profile describes.
- Never split — level-1 only.

### 3.4 Mutual consistency rule

Every `SCR-*` in the ui-ux-spec has an F-block in **each** `sub_bearing` phase
(`F1`, `F2`, `F3`, `F4`) and every F-block names an
`SCR-*` that exists in the spec; every `UXD-*` in the spec is cited by an F-block. `gov.py
analyze` checks this at the gate — a mismatch is MAJOR.

## 4. ALIGN self-check

Against the plan itself, the api-docs and the SRS ceiling (cross-artifact = `gov.py analyze`).

**Every row names the check that backs it, and there are no other rows.** The block used to
assert screen coverage, validation, routing and security in prose no check could falsify, and a
sibling plan shipped four such rows false under a verdict that read `PASSED ✓`. A row
nothing can falsify manufactures confidence and is worse than no row, so every unbacked row was
**deleted** rather than softened — if a dimension matters and no check covers it, the fix is a
clause in `shared/ARTIFACT-CONTRACTS.md`, not a sentence here. Each mark is the analyze report's
result for that check, copied; a clause the report says examined nothing is written
`— examined nothing`, never ✓.

```
ALIGN — MDL v1
row           backing check   assertion
SCREENS       orphans         every SCR is referenced by a plan block
UXD           orphans         every UXD is cited by a plan block — this is where a UX decision closes
TRACES        traces          every PHASE/SUB carries traces=, every UXD traces to its REQ/AC, every SCR to its REQ/UXD
API           traces          every API this plan cites is defined in the fetched api-docs — never in the backend plan's contract draft
FOREIGN       xref-surface    every reference to another module's surface resolves in that module's own artifacts
REGISTRY      registry-agree  every UXD and SCR defined here is in the stage registry, and nothing else is
LANGUAGES     languages       labels and messages in ar + en
MARKERS       markers         the parser reports no structural or semantic error for this track and plan
DECISIONS     refs-exist      every ADR this plan cites exists on disk in erp/decisions/MDL/
COVERAGE      (the report)    the clauses the analyze report lists as having examined nothing — verbatim, or `none`
RESULT    (written by the orchestrator from the analyze report — leave it alone)
```
Operations coverage table (operation │ API │ SCR action │ route │ status) closes the section —
a row with an empty route is a ✗.

## 5. Registry update — `registry-exec-fe-mdl.md`

```
REGISTRY — P3.2 — MDL v1
ID RANGES     UXD-MDL-<first>..<last> · SCR-MDL-<first>..<last>
SCREENS       SCR │ name │ owning ENT │ permissions
UXD INDEX     UXD │ screen │ field │ owner module · API used
API COVERAGE  documented endpoints used / unused (with ADR)
ALIGN      verdict as stamped · findings fixed
ADRs          decisions/MDL/ADR-MDL-<seq> … (status)
TRACEABILITY  REQ covered by ≥1 SCR/F-block: <n>/<total> · orphan REQ: <list — a gate blocker>
```

## 6. Structural self-check (toolkit)

```
[ ] every profile key has exactly one PHASE START/END pair, in profile order
[ ] every SUB id is {PHASE-KEY}-SCR-…; the same screen under different phases carries different prefixes
[ ] every PHASE/SUB carries traces=
[ ] no heading repeats; trailing content sits after the last PHASE END
[ ] §3.4 mutual consistency holds
```
Then (non-zero exit is blocking):
```
gov.py split --track frontend --module MDL --version 1 --dry-run
```
`gov.py analyze` runs before `gate:pass-2`; CRITICAL keeps the gate closed.

## 7. Ambiguity rule

`factory.yaml → ambiguity` (shared/GOVERNANCE-CORE.md): non-breaking → ADR
`erp/decisions/MDL/ADR-MDL-{seq:03d}.md` and
**continue**; breaking (contradicts a locked decision or a
REQ, including an SRS↔PRD contradiction) → ADR `BLOCKED`,
**stop**. Use `profile.knowledge.files` and `erp/` steering
for the best-practice choice. No question is raised at this stage.

## 8. Boundaries and hand-off

| Owns (mints) | References (read-only) | Never touches |
|---|---|---|
| `UXD-*`, `SCR-*`; flow diagram; ui-ux-spec; F-blocks; ALIGN; ADRs it raises | `REQ/AC/ENT/RULE` (P1), `API` (P3.1 — shape from the api-docs), catalog codes, permission names, `US` (P0.5) | `DBF/XM` (P2 — backend-only), `QR`, `TC` (test-gen), any code, any build |

Hand-off (the orchestrator prints it): plan + registry split by the toolkit into
`packages/frontend-execution/` inside the
shared repo after the `gate:pass-2` verdict, then tagged `{mod}-v{version}`. Nothing is
copied anywhere: the implementer reads it where it was written, at the commit its own repo pins. The implementer reads the plan in profile-phase order, the spec for
intent, the api-docs for shapes, and never invents a route, component, permission or field
not traceable to an F-block (a gap → ADR, not an invention).


---
# INPUTS (generated current state)

<<<INPUT: srs>>>
# SRS — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved 2026-09-10)
Counts : ENT 2 · REQ 13 · AC 13 · RULE 4 · SCR-REQ 2 · ADR 0
══════════════════════════════════════════════════════════════════

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | MDL — البيانات المرجعية / Master Data Lookup |
| Feature code | MDL |
| Version | v1 |
| Date | 2026-09-10 |
| Status | DRAFT (P1) |
| Prepared by | governance-factory (analysis lane) |
| Decisions applied count | 0 |

## A2 — Functional context

**In scope:** نوع اللوكب (Master) وقيمه (Detail)، شاشة عامة واحدة لإدارة كل القوائم، تسجيل
نوع جديد كبيانات من أي وحدة مستهلكة، قراءة القيم النشطة بالمفتاح لأي وحدة مستهلكة، سجل
الأنواع مجمّعًا حسب المالك، التحقق من صحة الوحدة المالكة عبر SEC.

**Out of scope:** تسلسل هرمي لقيم اللوكب (شجرة)، صلاحيات دقيقة لكل نوع لوكب على حدة (تبقى
الصلاحية على مستوى الشاشة فقط)؛ غير مذكورة في `lookup-module-plan-en.md`
[business-policies-mdl.md → SCOPE EXCEPTIONS].

**Module function (one paragraph):** MDL هو المركز الموحّد الوحيد لكل قوائم القيم
المُرمَّزة في المنصة؛ لا وحدة أخرى — بما فيها SEC وFIN لاحقًا — تحتفظ بجدول قيم مرجعية
خاص بها؛ كل وحدة تسجّل أنواعها هنا كبيانات وتقرأ قيمها من هنا وقت التشغيل.

**Detailed description (workflow narrative, roles):** وحدة مستهلكة (مثل FIN لاحقًا) تُسجّل
نوع لوكب جديد كبيانات، مسمّية نفسها مالكًا → MDL يتحقق من أن رمز الوحدة مسجَّل فعليًا في
SEC (قراءة فقط، بلا مفتاح خارجي فعلي) → مسؤول مخوَّل يدير قيم ذلك النوع عبر الشاشة العامة
الواحدة (رئيسي-تفصيلي) → أي وحدة تقرأ القيم النشطة بالمفتاح وقت التشغيل، مرتبة حسب الترتيب.

**Current situation:** لا يوجد نظام سابق ضمن هذه الدفعة؛ SEC هي الوحدة الوحيدة المكتملة
حتى الآن ولا تملك آلية قوائم مرجعية مركزية — قيمها الثلاث (USER_STATUS, SIGNUP_STATUS,
AUDIT_EVENT_TYPE) مُقيَّدة بـ CHECK محليًا (ADR-SEC-001)، على أن تُهاجَر لاحقًا إلى هنا.

**Current difficulties:** بلا مركزية، كل وحدة تخترع قوائمها الخاصة فتتضارب القيم — بالضبط
المشكلة التي يحلّها MDL (KB:erp-domain-standards §2 rule 3).

**Proposed system and benefits:** مركز واحد موثوق يمنع الازدواج والتضارب، ويجعل إضافة قائمة
أو قيمة جديدة عملية بيانات بحتة، لا تغييرًا في الشيفرة.

**General notes (constraints, deferred items):** لا محرك سير عمل؛ هجرة قوائم SEC الثلاث
إلى هذا المركز مؤجّلة لإصدار v2 من SEC (خارج نطاق هذه الدفعة، موثّقة في ADR-SEC-001).

## A3 — Entities and fields

Standard fields per kind (profile.conventions.entity_defaults):
master → nameAr, nameEn, code, isActiveFl, createdBy, createdAt, updatedBy, updatedAt;
lookup → code, nameAr, nameEn, sortOrder, isActiveFl.

### ENT-MDL-001 — نوع اللوكب / LookupType
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| master | SHARED (owner) — every consuming module registers and reads its own types here | No — `key` is a client-chosen stable string, not a platform-numbered sequence [§3.3 test] | create, read, search, update (name only), deactivate | consumed (registered into) by every future module | lookup-module-plan-en.md §2-§3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| lookupTypePk | number | yes (system) | — | primary key | معرّف نوع اللوكب | LookupType id |
| key | text | yes | unique, immutable after create [RULE-MDL-003] | the string every consumer cites (e.g. `USER_STATUS`) | المفتاح | Key |
| ownerModuleCode | text | yes | must exist in SEC's ModuleRegistry [RULE-MDL-001] | namespacing | رمز الوحدة المالكة | Owner module code |
| nameAr | text | yes | — | — | الاسم (عربي) | Name (Arabic) |
| nameEn | text | yes | — | — | الاسم (إنجليزي) | Name (English) |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields | — | — |

### ENT-MDL-002 — قيمة اللوكب / LookupValue
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| lookup | SHARED (owner) — every consuming module reads/manages its own values here | No | create, read, search, update, deactivate, reorder | consumed (read) by every future module | lookup-module-plan-en.md §3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| lookupValuePk | number | yes (system) | — | primary key | معرّف قيمة اللوكب | LookupValue id |
| lookupTypeId | reference | yes | ENT-MDL-001 | — | نوع اللوكب | Lookup type |
| code | text | yes | unique within its lookupTypeId [RULE-MDL-002] | the value a consumer stores | الرمز | Code |
| nameAr | text | yes | — | — | الاسم (عربي) | Name (Arabic) |
| nameEn | text | yes | — | — | الاسم (إنجليزي) | Name (English) |
| sortOrder | number | yes | — | display order | ترتيب العرض | Sort order |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields | — | — |

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-MDL-001 — إنشاء نوع لوكب / Create a lookup type
Pattern    : event
Statement  : When an administrator creates a lookup type naming its owner module, the system shall record it as an active lookup type.
Traces     : US-MDL-001, US-MDL-004
Entities   : ENT-MDL-001
Rationale  : registration as data, per module's own integration
Source     : lookup-module-plan-en.md §2-§3
Priority   : HIGH
#### AC-MDL-001 — [REQ-MDL-001]
Given a unique key and a registered owner module code
When an administrator submits the lookup type form
Then the system creates an active LookupType

### REQ-MDL-002 — رفض نوع لوحدة غير مسجّلة / Reject a type for an unregistered module
Pattern    : unwanted
Statement  : If a lookup type registration names a module that is not registered in the Security module, then the system shall reject the registration.
Traces     : US-MDL-001, US-MDL-004
Entities   : ENT-MDL-001
Rationale  : RULE-MDL-001; POL-MDL-003
Source     : lookup-module-plan-en.md §3
Priority   : HIGH
#### AC-MDL-002 — [REQ-MDL-002]
Given an owner module code with no ModuleRegistry row in SEC
When a lookup type registration names that code
Then the system rejects it and creates no LookupType

### REQ-MDL-003 — تعديل اسم نوع اللوكب / Edit a lookup type's name
Pattern    : event
Statement  : When an administrator edits a lookup type's name, the system shall update it without changing its key.
Traces     : US-MDL-001
Entities   : ENT-MDL-001
Rationale  : RULE-MDL-003 — key immutability
Source     : lookup-module-plan-en.md §3
Priority   : MEDIUM
#### AC-MDL-003 — [REQ-MDL-003]
Given an existing LookupType
When an administrator updates its nameAr/nameEn
Then the system saves the new names and leaves the key unchanged

### REQ-MDL-004 — تعطيل نوع لوكب / Deactivate a lookup type
Pattern    : event
Statement  : When an administrator deactivates a lookup type, the system shall exclude its values from every future consumer read.
Traces     : US-MDL-001
Entities   : ENT-MDL-001
Rationale  : RULE-MDL-004
Source     : lookup-module-plan-en.md §3
Priority   : MEDIUM
#### AC-MDL-004 — [REQ-MDL-004]
Given an active LookupType
When an administrator deactivates it
Then the system sets isActiveFl=false and REQ-MDL-011 no longer returns its values

### REQ-MDL-005 — اختيار النوع وعرض قيمه / Select a type and list its values
Pattern    : event
Statement  : When a user selects a lookup type in the master, the system shall list its values in the detail.
Traces     : US-MDL-002
Entities   : ENT-MDL-001, ENT-MDL-002
Rationale  : the generic master-detail mechanism, POL-MDL-004
Source     : lookup-module-plan-en.md §3
Priority   : HIGH
#### AC-MDL-005 — [REQ-MDL-005]
Given a lookup type with three values
When a user selects it in the master list
Then the system lists exactly those three values in the detail, ordered by sortOrder

### REQ-MDL-006 — إنشاء قيمة لوكب / Create a lookup value
Pattern    : event
Statement  : When an authorized user creates a value under a selected lookup type, the system shall record its code, bilingual labels, sort order and active flag.
Traces     : US-MDL-002
Entities   : ENT-MDL-002
Rationale  : POL-MDL-005
Source     : lookup-module-plan-en.md §3
Priority   : HIGH
#### AC-MDL-006 — [REQ-MDL-006]
Given a selected lookup type and a code not yet used within it
When the user submits the value form
Then the system creates an active LookupValue under that type

### REQ-MDL-007 — رفض تكرار الرمز ضمن النوع / Reject a duplicate code within a type
Pattern    : unwanted
Statement  : If two lookup values under the same lookup type share the same code, then the system shall reject the second.
Traces     : US-MDL-002
Entities   : ENT-MDL-002
Rationale  : RULE-MDL-002; POL-MDL-006
Source     : lookup-module-plan-en.md §6
Priority   : HIGH
#### AC-MDL-007 — [REQ-MDL-007]
Given a lookup type already holding a value with code "ACTIVE"
When a user attempts to create another value with code "ACTIVE" under the same type
Then the system rejects it and creates no second row

### REQ-MDL-008 — تعديل قيمة لوكب / Edit a lookup value
Pattern    : event
Statement  : When an authorized user edits a lookup value's labels or sort order, the system shall update it.
Traces     : US-MDL-002
Entities   : ENT-MDL-002
Rationale  : POL-MDL-005
Source     : lookup-module-plan-en.md §3
Priority   : MEDIUM
#### AC-MDL-008 — [REQ-MDL-008]
Given an existing LookupValue
When the user updates its nameAr/nameEn/sortOrder
Then the system saves the new values (code and lookupTypeId remain unchanged)

### REQ-MDL-009 — تعطيل قيمة لوكب / Deactivate a lookup value
Pattern    : event
Statement  : When an authorized user deactivates a lookup value, the system shall exclude it from future consumer reads.
Traces     : US-MDL-002
Entities   : ENT-MDL-002
Rationale  : POL-MDL-005
Source     : lookup-module-plan-en.md §3
Priority   : MEDIUM
#### AC-MDL-009 — [REQ-MDL-009]
Given an active LookupValue
When the user deactivates it
Then the system sets isActiveFl=false and REQ-MDL-011 no longer returns it

### REQ-MDL-010 — إعادة ترتيب قيم اللوكب / Reorder lookup values
Pattern    : event
Statement  : When an authorized user reorders the values of a lookup type, the system shall persist the new sort order for each affected value.
Traces     : US-MDL-002
Entities   : ENT-MDL-002
Rationale  : sortOrder is a stored, consumer-facing field (A3)
Source     : lookup-module-plan-en.md §3
Priority   : LOW
#### AC-MDL-010 — [REQ-MDL-010]
Given three values with sortOrder 1,2,3
When the user reorders them to 3,1,2
Then the system persists sortOrder=3,1,2 respectively and REQ-MDL-011 returns them in that order

### REQ-MDL-011 — قراءة القيم النشطة بالمفتاح / Read active values by key
Pattern    : event
Statement  : When a consuming module requests the active values of a lookup type by key, the system shall return them ordered by sort order.
Traces     : US-MDL-003
Entities   : ENT-MDL-001, ENT-MDL-002
Rationale  : POL-MDL-001 — the module's entire reason to exist
Source     : lookup-module-plan-en.md §2, §4
Priority   : HIGH
#### AC-MDL-011 — [REQ-MDL-011]
Given a lookup type "PAYMENT_METHOD" with two active and one inactive value
When a consuming module requests its values by key
Then the system returns exactly the two active values, ordered by sortOrder

### REQ-MDL-012 — رفض مفتاح نوع غير موجود / Reject an unknown type key
Pattern    : unwanted
Statement  : If a consuming module requests values for a lookup type key that does not exist, then the system shall return a not-found error.
Traces     : US-MDL-003
Entities   : ENT-MDL-001
Rationale  : distinguishes "type exists, zero values" (success, empty) from "type never registered" (integration defect)
Source     : lookup-module-plan-en.md §4
Priority   : MEDIUM
#### AC-MDL-012 — [REQ-MDL-012]
Given no LookupType with key "NOT_A_REAL_KEY"
When a consuming module requests its values
Then the system returns a not-found error, not an empty success

### REQ-MDL-013 — تصفّح سجل الأنواع حسب المالك / Browse the type registry by owner
Pattern    : event
Statement  : When an administrator browses the lookup-type registry, the system shall group active lookup types by their owner module.
Traces     : US-MDL-005
Entities   : ENT-MDL-001
Rationale  : POL-MDL-002
Source     : lookup-module-plan-en.md §7
Priority   : MEDIUM
#### AC-MDL-013 — [REQ-MDL-013]
Given lookup types owned by SEC and by MDL... (example: two types owned by SEC, one by MDL's own future host)
When an administrator opens the registry screen
Then the system shows the types grouped under their respective owner module headings

## A5 — Business rules

### RULE-MDL-001 — رفض نوع لوحدة غير مسجّلة / Reject a type for an unregistered owner module
Scope      : ENT-MDL-001
Trigger    : on create (lookup type)
Statement  : The system shall reject a lookup type registration whose owner module code has no ModuleRegistry row in SEC.
Data source: ENT-MDL-001.ownerModuleCode — validated against SEC's module registry through XM-MDL-001 (SOFT-READ, application layer)
Message    : ar: "الوحدة المالكة غير مسجّلة في وحدة الأمان" · en: "The owning module is not registered in the Security module"
Traces     : REQ-MDL-002
Source     : lookup-module-plan-en.md §3

### RULE-MDL-002 — رفض رمز مكرر ضمن النوع / Reject a duplicate code within a type
Scope      : ENT-MDL-002
Trigger    : on create (lookup value)
Statement  : The system shall reject a lookup value whose code already exists under the same lookup type.
Data source: ENT-MDL-002.code, ENT-MDL-002.lookupTypeId
Message    : ar: "هذا الرمز مستخدم بالفعل ضمن هذا النوع" · en: "This code is already used within this type"
Traces     : REQ-MDL-007
Source     : lookup-module-plan-en.md §6

### RULE-MDL-003 — عدم تغيير مفتاح النوع بعد إنشائه / Key immutability after creation
Scope      : ENT-MDL-001
Trigger    : on update (lookup type)
Statement  : The system shall prevent editing a lookup type's key after creation.
Data source: ENT-MDL-001.key
Message    : ar: "لا يمكن تعديل مفتاح نوع اللوكب بعد إنشائه" · en: "A lookup type's key cannot be changed after creation"
Traces     : REQ-MDL-003
Source     : lookup-module-plan-en.md §3 (the key is what every consumer already cites)

### RULE-MDL-004 — استبعاد قيم النوع المعطّل / Exclude an inactive type's values from reads
Scope      : ENT-MDL-001
Trigger    : on evaluate (consumer read, REQ-MDL-011)
Statement  : While a lookup type is inactive, the system shall exclude its values from consumer reads.
Data source: ENT-MDL-001.isActiveFl
Message    : ar: "هذا النوع معطّل حاليًا" · en: "This lookup type is currently inactive"
Traces     : REQ-MDL-004, REQ-MDL-011
Source     : lookup-module-plan-en.md §3

## A6 — Lookups
None — MDL introduces no domain-specific coded list of its own (it is the generic
mechanism every other module's lookup types run on top of; see module-registry-mdl.md →
LOOKUPS OWNED).

## A7 — Status lifecycle
`LookupType.isActiveFl` and `LookupValue.isActiveFl` are each a plain binary active flag
(≤2 states) — not applicable for a diagram.

## A8 — Module dependencies
| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
|---|---|---|---|---|
| ModuleRegistry | ENT-SEC-004 | SEC | SOFT-READ | assigned by P2 (this module) |

| External service | Purpose | Integration kind |
|---|---|---|
None — lookup-module-plan-en.md §5 names Notifications/File Service as available but MDL's
PRD (US-MDL-001..005) states no need that would use either; not built this v1.

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-MDL-001 — اللوكبات العامة / Generic Lookups (master-detail, all lists)
### B1 — Definition
Purpose      : إدارة كل قوائم القيم المُرمَّزة عبر شاشة واحدة.
Entities     : ENT-MDL-001, ENT-MDL-002
Operations   : search, create, read, update, deactivate (type); create, read, search, update, deactivate, reorder (value)
Users        : مسؤول مخوَّل بصلاحية الشاشة (أي وحدة، حسب منحها من SEC)
Navigation   : MDL → Reference data → Generic Lookups
Content shape: header + repeating lines with totals — رئيسي (أنواع) + تفصيلي (قيم) قابل لإعادة الترتيب
Traces       : REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010
Composite    : Master (types) + Detail (values) = ONE screen requirement
### B2 — Search / list
Master filters: key(LIKE), ownerModuleCode(EXACT), isActiveFl(EXACT) — correspond to result columns.
Detail filters: code(LIKE) — corresponds to result column.
### B3 — Input
Master fields: key, ownerModuleCode, nameAr, nameEn (ENT-MDL-001). Detail fields: code,
nameAr, nameEn, sortOrder (drag-to-reorder), isActiveFl (ENT-MDL-002). Buttons: activate/
deactivate per row at both levels.
### B4 — Access
Page code: MDL_LOOKUPS. Actions: VIEW, CREATE, UPDATE, DELETE (deactivate), per §7.1
(gateway VIEW).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search types | POST | /api/v1/mdl/lookup-types/search | filters, paging | Page\<LookupType\> | — | REQ-MDL-001 |
| create type | POST | /api/v1/mdl/lookup-types | key, ownerModuleCode, nameAr, nameEn | LookupType | RULE-MDL-001 | REQ-MDL-001, REQ-MDL-002 |
| update type | PUT | /api/v1/mdl/lookup-types/{id} | nameAr, nameEn | LookupType | RULE-MDL-003 | REQ-MDL-003 |
| deactivate type | DELETE | /api/v1/mdl/lookup-types/{id} | id | confirmation | — | REQ-MDL-004 |
| search values | POST | /api/v1/mdl/lookup-types/values/search | filters, paging (parent lookupTypeId in filters) | Page\<LookupValue\> | — | REQ-MDL-005 |
| create value | POST | /api/v1/mdl/lookup-types/{id}/values | code, nameAr, nameEn, sortOrder | LookupValue | RULE-MDL-002 | REQ-MDL-006, REQ-MDL-007 |
| update value | PUT | /api/v1/mdl/lookup-values/{id} | nameAr, nameEn, sortOrder | LookupValue | — | REQ-MDL-008 |
| deactivate value | DELETE | /api/v1/mdl/lookup-values/{id} | id | confirmation | — | REQ-MDL-009 |
| reorder values | PATCH | /api/v1/mdl/lookup-types/{id}/values/reorder | ordered list of value ids | Page\<LookupValue\> | — | REQ-MDL-010 |

## SCR-REQ-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
### B1 — Definition
Purpose      : تصفّح أنواع اللوكب مجمّعة حسب الوحدة المالكة.
Entities     : ENT-MDL-001
Operations   : search, read
Users        : مسؤول مخوَّل
Navigation   : MDL → Reference data → Type registry; from: SCR-REQ-MDL-001
Content shape: true hierarchy (parent/child) — وحدة مالكة → أنواعها
Traces       : REQ-MDL-013
Composite    : single screen (grouped list) = ONE screen requirement
### B2 — Search / list
Filters: ownerModuleCode(EXACT), key(LIKE) — correspond to result columns.
### B3 — Input
Read-only browse; no create/update here (management happens on SCR-REQ-MDL-001).
### B4 — Access
Page code: MDL_TYPE_REGISTRY. Action: VIEW.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| browse registry | POST | /api/v1/mdl/lookup-types/by-owner/search | filters | grouped List\<OwnerGroupResponse\> | — | REQ-MDL-013 |
| read values by key (consumer API) | GET | /api/v1/mdl/lookups | type=key | List\<LookupValue\> (active only) | RULE-MDL-004 | REQ-MDL-011, REQ-MDL-012 |

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-MDL-001 | REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004 | AC-MDL-001…004 | RULE-MDL-001, RULE-MDL-003, RULE-MDL-004 | ENT-MDL-001 | SCR-REQ-MDL-001 |
| US-MDL-002 | REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010 | AC-MDL-005…010 | RULE-MDL-002 | ENT-MDL-001, ENT-MDL-002 | SCR-REQ-MDL-001 |
| US-MDL-003 | REQ-MDL-011, REQ-MDL-012 | AC-MDL-011, AC-MDL-012 | RULE-MDL-004 | ENT-MDL-001, ENT-MDL-002 | SCR-REQ-MDL-002 |
| US-MDL-004 | REQ-MDL-001, REQ-MDL-002 | AC-MDL-001, AC-MDL-002 | RULE-MDL-001 | ENT-MDL-001 | SCR-REQ-MDL-001 |
| US-MDL-005 | REQ-MDL-013 | AC-MDL-013 | — | ENT-MDL-001 | SCR-REQ-MDL-002 |

Every story traces to ≥1 REQ; every REQ traces to ≥1 AC; every RULE traces to a REQ; every
SCR-REQ traces to ≥1 REQ. No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
No ADR was raised — no ambiguity reached the breaking/non-breaking fork of §9; the MDL→SEC
SOFT-READ classification was settled at P0 (module-registry-mdl.md → AUTO-DECISIONS), not
here, and every other point was settled by the plan directly.

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|---|
| MDL_LOOKUPS | Generic Lookups | role-granted | role-granted | role-granted | role-granted (deactivate) |
| MDL_TYPE_REGISTRY | Type registry by owner | role-granted | — | — | — |
Every action beyond VIEW additionally requires VIEW on the same screen (platform gateway
convention, `profiles/erp.yaml → conventions.security_model.gateway_action`, applied the
same way SEC's own RULE-SEC-007 documents it — not restated as an MDL-owned RULE since it
is a platform-wide convention, not an MDL-specific constraint).
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: prd>>>
# PRD — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module          : MDL     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : 5   Policies covered : 6/6   Deferred : 0
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES

US-MDL-001
  Title          : إدارة أنواع اللوكب / Manage lookup types
  Story          : As a platform administrator, I need to create, edit and deactivate a lookup type naming its owning module, so that every coded list has one authoritative, namespaced home.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-MDL-002, POL-MDL-003
  Source         : lookup-module-plan-en.md §2-§3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-002
  Title          : إدارة قيم اللوكب عبر الشاشة العامة / Manage lookup values through the generic screen
  Story          : As a user authorized for a lookup type, I need one generic master-detail screen to pick the type and manage its values (code, bilingual labels, sort order, active flag), so that I never need a screen per list.
  Priority       : HIGH — plan names this the module's core mechanism (§3)
  Success metric : —
  Traces         : POL-MDL-004, POL-MDL-005, POL-MDL-006
  Source         : lookup-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-003
  Title          : قراءة القيم من أي وحدة مستهلكة / Read values from any consuming module
  Story          : As a consuming module's backend, I need to read a lookup type's active values by key, so that I never hardcode a coded value list of my own.
  Priority       : HIGH — this is the module's entire reason to exist (§2)
  Success metric : —
  Traces         : POL-MDL-001
  Source         : lookup-module-plan-en.md §2, §4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-004
  Title          : تسجيل نوع لوكب جديد كبيانات / Register a new lookup type as data
  Story          : As a consuming module's integrator, I need to register my own lookup type here as data (naming my module as owner), so that adding a new list never requires touching MDL's code.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-MDL-002, POL-MDL-003
  Source         : lookup-module-plan-en.md §4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-MDL-005
  Title          : سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
  Story          : As a platform administrator, I need to browse lookup types grouped by their owning module, so that I can find and audit a module's reference lists at a glance.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-MDL-002
  Source         : lookup-module-plan-en.md §7 (screen inventory row 2)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-MDL-001 | POL-MDL-002, POL-MDL-003 | lookup-module-plan-en.md §2-§3 |
| US-MDL-002 | POL-MDL-004, POL-MDL-005, POL-MDL-006 | lookup-module-plan-en.md §3 |
| US-MDL-003 | POL-MDL-001 | lookup-module-plan-en.md §2, §4 |
| US-MDL-004 | POL-MDL-002, POL-MDL-003 | lookup-module-plan-en.md §4 |
| US-MDL-005 | POL-MDL-002 | lookup-module-plan-en.md §7 |
Every policy POL-MDL-001 … POL-MDL-006 appears in at least one row above.

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
None — lookup-module-plan-en.md left no story's scope, priority or role genuinely
ambiguous; no dialogue question was required.

## DEFERRED
| US | Reason | Activation trigger |
None — every capability named in lookup-module-plan-en.md is represented by a story in
this v1 PRD.

## APPROVAL
Approved by : PENDING   Date : PENDING
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: api-docs>>>
<!-- GENERATED by `gov.py fetch-inputs` — do not edit.
     source : /Users/ezzat/my project/factory/governance-shared/erp/modules/MDL/api-docs
     files  : 4
     digest : 2c13821bde2477282f9b76401318884dd35539220e8d91332cb43b5e0f6bb547
     Edits belong in the source; anything this factory adds belongs in
     the companion file, which this command never touches. -->

<!-- fetched-from: index.md -->
<!-- AUTO-GENERATED by api-doc-generator — do not edit manually -->
# MDL API Documentation

_OpenAPI definition_

API version: `v0`

## Servers

- http://localhost:7272


## Common Response Envelope

Schema: `ApiResponse<T>`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| success | boolean | No |  |  |
| data | object | No |  | Endpoint-specific payload — see each endpoint's Response section |
| error | ApiError | No |  | Present only when success=false |
| error.code | string | No |  |  |
| error.message | string | No |  |  |
| error.fieldErrors | array<FieldErrorItem> | No |  |  |
| error.fieldErrors[].field | string | No |  |  |
| error.fieldErrors[].message | string | No |  |  |
| timestamp | string (date-time) | No |  |  |

## Pagination Envelope

Schema: `PageLookup<T>`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| totalPages | integer (int32) | No |  |  |
| totalElements | integer (int64) | No |  |  |
| first | boolean | No |  |  |
| last | boolean | No |  |  |
| numberOfElements | integer (int32) | No |  |  |
| pageable | Pageable | No |  |  |
| sort | Sort | No |  |  |
| size | integer (int32) | No |  |  |
| number | integer (int32) | No |  |  |
| empty | boolean | No |  |  |

## Pagination Constraints

Source: `com/erp/common/search/PageableBuilder.java`

| Constraint | Value |
|---|---|
| Default page | 0 |
| Default size | 20 |
| Maximum size | 200 |

## Known Error Codes

| Code | Value | Source | Status | HTTP Status |
|---|---|---|---|---|
| MDL_409_MODULE_NOT_REGISTERED | `MDL-409-MODULE-NOT-REGISTERED` | exception/MdlErrorCodes.java | CONFLICT | 409 CONFLICT |
| MDL_409_TYPE_DUP | `MDL-409-TYPE-DUP` | exception/MdlErrorCodes.java | ALREADY_EXISTS | 409 CONFLICT |
| MDL_409_VALUE_DUP | `MDL-409-VALUE-DUP` | exception/MdlErrorCodes.java | ALREADY_EXISTS | 409 CONFLICT |
| MDL_404_TYPE | `MDL-404-TYPE` | exception/MdlErrorCodes.java | NOT_FOUND | 404 NOT_FOUND |
| MDL_404_VALUE | `MDL-404-VALUE` | exception/MdlErrorCodes.java | NOT_FOUND | 404 NOT_FOUND |
| MDL_400_REORDER_MISMATCH | `MDL-400-REORDER-MISMATCH` | exception/MdlErrorCodes.java | VALIDATION_ERROR | 400 BAD_REQUEST |
| MDL_404_TYPE_KEY | `MDL-404-TYPE-KEY` | exception/MdlErrorCodes.java | NOT_FOUND | 404 NOT_FOUND |
| VALIDATION_ERROR | `VALIDATION_ERROR` | com/erp/common/web/GlobalExceptionHandler.java |  | 400 BAD_REQUEST |
| DATA_INTEGRITY_VIOLATION | `DATA_INTEGRITY_VIOLATION` | com/erp/common/web/GlobalExceptionHandler.java |  | 409 CONFLICT |
| ACCESS_DENIED | `ACCESS_DENIED` | com/erp/common/web/GlobalExceptionHandler.java |  | 403 FORBIDDEN |
| INTERNAL_ERROR | `INTERNAL_ERROR` | com/erp/common/web/GlobalExceptionHandler.java |  | 500 INTERNAL_SERVER_ERROR |

## Status -> HTTP Status Reference

Shared, module-independent mapping every business error code's `Status` resolves through (see each error code's own Status column above, when known).

| Status | HTTP Status |
|---|---|
| ALREADY_EXISTS | 409 CONFLICT |
| BUSINESS_RULE_VIOLATION | 422 UNPROCESSABLE_CONTENT |
| CONFLICT | 409 CONFLICT |
| CREATED | 201 CREATED |
| FORBIDDEN | 403 FORBIDDEN |
| INTERNAL_ERROR | 500 INTERNAL_SERVER_ERROR |
| NOT_FOUND | 404 NOT_FOUND |
| PAYLOAD_TOO_LARGE | 413 CONTENT_TOO_LARGE |
| SUCCESS | 200 OK |
| UNAUTHORIZED | 401 UNAUTHORIZED |
| UNSUPPORTED_MEDIA_TYPE | 415 UNSUPPORTED_MEDIA_TYPE |
| UPDATED | 200 OK |
| VALIDATION_ERROR | 400 BAD_REQUEST |

## API Catalog

### Lookup Value Management

| Method | Path | Summary | Doc |
|---|---|---|---|
| PUT | `/api/v1/mdl/lookup-values/{id}` | Update lookup value | [update](endpoints/lookup-value-management.md#put-apiv1mdllookup-valuesid) |
| DELETE | `/api/v1/mdl/lookup-values/{id}` | Deactivate lookup value | [deactivate](endpoints/lookup-value-management.md#delete-apiv1mdllookup-valuesid) |
| POST | `/api/v1/mdl/lookup-types/values/search` | Search lookup values of a type | [search_1](endpoints/lookup-value-management.md#post-apiv1mdllookup-typesvaluessearch) |
| POST | `/api/v1/mdl/lookup-types/{id}/values` | Create lookup value | [create_1](endpoints/lookup-value-management.md#post-apiv1mdllookup-typesidvalues) |
| PATCH | `/api/v1/mdl/lookup-types/{id}/values/reorder` | Reorder lookup values | [reorder](endpoints/lookup-value-management.md#patch-apiv1mdllookup-typesidvaluesreorder) |

### Lookup Type Management

| Method | Path | Summary | Doc |
|---|---|---|---|
| PUT | `/api/v1/mdl/lookup-types/{id}` | Update lookup type | [update_1](endpoints/lookup-type-management.md#put-apiv1mdllookup-typesid) |
| DELETE | `/api/v1/mdl/lookup-types/{id}` | Deactivate lookup type | [deactivate_1](endpoints/lookup-type-management.md#delete-apiv1mdllookup-typesid) |
| POST | `/api/v1/mdl/lookup-types/search` | Search lookup types | [search](endpoints/lookup-type-management.md#post-apiv1mdllookup-typessearch) |
| POST | `/api/v1/mdl/lookup-types` | Create lookup type | [create](endpoints/lookup-type-management.md#post-apiv1mdllookup-types) |
| POST | `/api/v1/mdl/lookup-types/by-owner/search` | Browse lookup type registry by owner | [browseByOwner](endpoints/lookup-type-management.md#post-apiv1mdllookup-typesby-ownersearch) |

### Lookup Consumer API

| Method | Path | Summary | Doc |
|---|---|---|---|
| GET | `/api/v1/mdl/lookups` | Read lookup values by type key | [readByKey](endpoints/lookup-consumer-api.md#get-apiv1mdllookups) |

<!-- fetched-from: endpoints/lookup-consumer-api.md -->
<!-- AUTO-GENERATED by api-doc-generator — do not edit manually -->
# Lookup Consumer API

## GET /api/v1/mdl/lookups

**Read lookup values by type key**

قراءة قيم اللوكب حسب مفتاح النوع

Operation ID: `readByKey`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_VIEW (found on service:LookupConsumerService)

### Query Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| type | string | Yes |  |

### Response `200` — OK

Shape: `array of LookupValueResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |

<!-- fetched-from: endpoints/lookup-type-management.md -->
<!-- AUTO-GENERATED by api-doc-generator — do not edit manually -->
# Lookup Type Management

**Endpoints in this file:**

- [PUT /api/v1/mdl/lookup-types/{id}](#put-apiv1mdllookup-typesid)
- [DELETE /api/v1/mdl/lookup-types/{id}](#delete-apiv1mdllookup-typesid)
- [POST /api/v1/mdl/lookup-types/search](#post-apiv1mdllookup-typessearch)
- [POST /api/v1/mdl/lookup-types](#post-apiv1mdllookup-types)
- [POST /api/v1/mdl/lookup-types/by-owner/search](#post-apiv1mdllookup-typesby-ownersearch)

## PUT /api/v1/mdl/lookup-types/{id}

**Update lookup type**

تعديل أسماء نوع اللوكب

Operation ID: `update_1`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_UPDATE (found on service:LookupTypeService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Request Body

Schema: `LookupTypeUpdateRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| nameAr | string | Yes | maxLength: 150 | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | Yes | maxLength: 150 | Name (English) - الاسم بالإنجليزية | Order Status |

**Request Example**

```json
{
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status"
}
```

### Response `200` — OK

Shape: `LookupTypeResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupTypePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| key | string | No |  | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Order Status |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupTypePk": 1,
  "key": "ORDER_STATUS",
  "ownerModuleCode": "FIN",
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status",
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## DELETE /api/v1/mdl/lookup-types/{id}

**Deactivate lookup type**

إلغاء تفعيل نوع اللوكب

Operation ID: `deactivate_1`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_UPDATE (found on service:LookupTypeService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Response `200` — OK

Shape: `LookupTypeResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupTypePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| key | string | No |  | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Order Status |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupTypePk": 1,
  "key": "ORDER_STATUS",
  "ownerModuleCode": "FIN",
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status",
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |

## POST /api/v1/mdl/lookup-types/search

**Search lookup types**

بحث في أنواع اللوكب

Operation ID: `search`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_VIEW (found on service:LookupTypeService)

### Request Body

Schema: `LookupTypeSearchRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| filters | array<SearchFilter> | No |  | Filter criteria - معايير التصفية |  |
| filters[].field | string | No |  |  |  |
| filters[].operator | string | No | enum: EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN |  |  |
| filters[].value | object | No |  |  |  |
| sortField | string | No |  | Sort field - حقل الترتيب |  |
| sortDirection | string | No | enum: ASC, DESC | Sort direction - اتجاه الترتيب |  |
| page | integer (int32) | No |  | Page number, zero-based - رقم الصفحة | 0 |
| size | integer (int32) | No |  | Page size - حجم الصفحة | 20 |

**Request Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "page": 0,
  "size": 20,
  "filters": [{"field": "key", "operator": "LIKE", "value": "ORDER_STATUS"}]
}
```

### Response `200` — OK

Shape: `paginated list of LookupTypeResponse (see Pagination Envelope in index.md)`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupTypePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| key | string | No |  | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Order Status |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupTypePk": 1,
  "key": "ORDER_STATUS",
  "ownerModuleCode": "FIN",
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status",
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## POST /api/v1/mdl/lookup-types

**Create lookup type**

إنشاء نوع لوكب جديد

Operation ID: `create`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_CREATE (found on service:LookupTypeService)

### Request Body

Schema: `LookupTypeCreateRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| key | string | Yes | maxLength: 80 | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| ownerModuleCode | string | Yes | maxLength: 10 | Owner module code, validated against SEC's module registry - رمز الوحدة المالكة | FIN |
| nameAr | string | Yes | maxLength: 150 | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | Yes | maxLength: 150 | Name (English) - الاسم بالإنجليزية | Order Status |

**Request Example**

```json
{
  "key": "ORDER_STATUS",
  "ownerModuleCode": "FIN",
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status"
}
```

### Response `200` — OK

Shape: `LookupTypeResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupTypePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| key | string | No |  | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Order Status |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupTypePk": 1,
  "key": "ORDER_STATUS",
  "ownerModuleCode": "FIN",
  "nameAr": "حالة الطلب",
  "nameEn": "Order Status",
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## POST /api/v1/mdl/lookup-types/by-owner/search

**Browse lookup type registry by owner**

استعراض سجل أنواع اللوكب حسب المالك

Operation ID: `browseByOwner`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_TYPE_REGISTRY_VIEW (found on service:LookupTypeService)

### Request Body

Schema: `LookupTypeByOwnerSearchRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| filters | array<SearchFilter> | No |  | Filter criteria - معايير التصفية (`isActiveFl` is not a valid client filter here — the service always restricts to active types) |  |
| filters[].field | string | No |  |  |  |
| filters[].operator | string | No | enum: EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN |  |  |
| filters[].value | object | No |  |  |  |

**Request Example**

_(partial — only fields with a documented example are shown; `page`/`size`/`sortField` are inherited but unused — this response is not paginated)_

```json
{
  "filters": [{"field": "ownerModuleCode", "operator": "EQUALS", "value": "SEC"}]
}
```

### Response `200` — OK

Shape: `array of OwnerGroupResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| types | array<LookupTypeResponse> | No |  | Active lookup types owned by this module - أنواع اللوكب النشطة التابعة لهذه الوحدة |  |
| types[].lookupTypePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| types[].key | string | No |  | Unique lookup type key - مفتاح نوع اللوكب الفريد | ORDER_STATUS |
| types[].ownerModuleCode | string | No |  | Owner module code - رمز الوحدة المالكة | FIN |
| types[].nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | حالة الطلب |
| types[].nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Order Status |
| types[].isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| types[].createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| types[].createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| types[].updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| types[].updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "ownerModuleCode": "FIN",
  "types": [
    {
      "lookupTypePk": 1,
      "key": "ORDER_STATUS",
      "ownerModuleCode": "FIN",
      "nameAr": "حالة الطلب",
      "nameEn": "Order Status",
      "isActiveFl": true
    }
  ]
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

<!-- fetched-from: endpoints/lookup-value-management.md -->
<!-- AUTO-GENERATED by api-doc-generator — do not edit manually -->
# Lookup Value Management

**Endpoints in this file:**

- [PUT /api/v1/mdl/lookup-values/{id}](#put-apiv1mdllookup-valuesid)
- [DELETE /api/v1/mdl/lookup-values/{id}](#delete-apiv1mdllookup-valuesid)
- [POST /api/v1/mdl/lookup-types/values/search](#post-apiv1mdllookup-typesvaluessearch)
- [POST /api/v1/mdl/lookup-types/{id}/values](#post-apiv1mdllookup-typesidvalues)
- [PATCH /api/v1/mdl/lookup-types/{id}/values/reorder](#patch-apiv1mdllookup-typesidvaluesreorder)

## PUT /api/v1/mdl/lookup-values/{id}

**Update lookup value**

تعديل قيمة لوكب

Operation ID: `update`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_UPDATE (found on service:LookupValueService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Request Body

Schema: `LookupValueUpdateRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| nameAr | string | Yes | maxLength: 150 | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | Yes | maxLength: 150 | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | Yes |  | Display sort order - ترتيب العرض | 10 |

**Request Example**

```json
{
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10
}
```

### Response `200` — OK

Shape: `LookupValueResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## DELETE /api/v1/mdl/lookup-values/{id}

**Deactivate lookup value**

إلغاء تفعيل قيمة لوكب

Operation ID: `deactivate`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_UPDATE (found on service:LookupValueService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Response `200` — OK

Shape: `LookupValueResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |

## POST /api/v1/mdl/lookup-types/values/search

**Search lookup values of a type**

بحث في قيم نوع لوكب

Operation ID: `search_1`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_VIEW (found on service:LookupValueService)

### Request Body

Schema: `LookupValueSearchRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| filters | array<SearchFilter> | No |  | Filter criteria - معايير التصفية (the parent `lookupTypeId` travels here too, e.g. `{"field":"lookupTypeId","operator":"EQUALS","value":1}` — never a path variable) |  |
| filters[].field | string | No |  |  |  |
| filters[].operator | string | No | enum: EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN |  |  |
| filters[].value | object | No |  |  |  |
| sortField | string | No |  | Sort field - حقل الترتيب (defaults to `sortOrder` server-side when omitted) |  |
| sortDirection | string | No | enum: ASC, DESC | Sort direction - اتجاه الترتيب |  |
| page | integer (int32) | No |  | Page number, zero-based - رقم الصفحة | 0 |
| size | integer (int32) | No |  | Page size - حجم الصفحة | 20 |

**Request Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "page": 0,
  "size": 20,
  "filters": [
    {"field": "lookupTypeId", "operator": "EQUALS", "value": 1},
    {"field": "code", "operator": "LIKE", "value": "PEND"}
  ]
}
```

### Response `200` — OK

Shape: `paginated list of LookupValueResponse (see Pagination Envelope in index.md)`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## POST /api/v1/mdl/lookup-types/{id}/values

**Create lookup value**

إنشاء قيمة لوكب جديدة ضمن نوع

Operation ID: `create_1`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_CREATE (found on service:LookupValueService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Request Body

Schema: `LookupValueCreateRequest` (application/json)

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| code | string | Yes | maxLength: 50 | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | Yes | maxLength: 150 | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | Yes | maxLength: 150 | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | Yes |  | Display sort order - ترتيب العرض | 10 |

**Request Example**

```json
{
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10
}
```

### Response `200` — OK

Shape: `LookupValueResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

## PATCH /api/v1/mdl/lookup-types/{id}/values/reorder

**Reorder lookup values**

إعادة ترتيب قيم نوع اللوكب

Operation ID: `reorder`

**Authentication**

Not determined from the OpenAPI document.

**Required permission(s)**: PERM_MDL_LOOKUPS_UPDATE (found on service:LookupValueService)

### Path Parameters

| Name | Type | Required | Description |
|---|---|---|---|
| id | integer | Yes |  |

### Request Body

Schema: `LookupValueReorderRequest` (application/json)

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| orderedValueIds | array<integer> | Yes |  | Lookup value ids in their new display order - معرّفات القيم بترتيبها الجديد |

### Response `200` — OK

Shape: `array of LookupValueResponse`

| Field | Type | Required | Constraints | Description | Example |
|---|---|---|---|---|---|
| lookupValuePk | integer (int64) | No |  | Unique identifier - المعرف الفريد | 1 |
| lookupTypeId | integer (int64) | No |  | Parent lookup type id - معرّف نوع اللوكب الأب | 1 |
| code | string | No |  | Unique code within the parent lookup type - الرمز الفريد ضمن النوع | PENDING |
| nameAr | string | No |  | Name (Arabic) - الاسم بالعربية | قيد الانتظار |
| nameEn | string | No |  | Name (English) - الاسم بالإنجليزية | Pending |
| sortOrder | integer (int32) | No |  | Display sort order - ترتيب العرض | 10 |
| isActiveFl | boolean | No |  | Active status - حالة التفعيل | true |
| createdAt | string (date-time) | No |  | Created timestamp - تاريخ الإنشاء |  |
| createdBy | string | No |  | Created by - أنشئ بواسطة |  |
| updatedAt | string (date-time) | No |  | Updated timestamp - تاريخ التحديث |  |
| updatedBy | string | No |  | Updated by - حُدّث بواسطة |  |

**Response Example**

_(partial — only fields with a documented example are shown)_

```json
{
  "lookupValuePk": 1,
  "lookupTypeId": 1,
  "code": "PENDING",
  "nameAr": "قيد الانتظار",
  "nameEn": "Pending",
  "sortOrder": 10,
  "isActiveFl": true
}
```

### Other Possible Responses

Structurally guaranteed by this endpoint's own shape (auth requirement, permission check, request body) combined with the shared framework's exception handling — not specific business errors.

| HTTP Status | Code | Why |
|---|---|---|
| 403 FORBIDDEN | ACCESS_DENIED | An authorization check was found for this endpoint (@PreAuthorize/@Secured); GlobalExceptionHandler maps AccessDeniedException to this status. |
| 400 BAD_REQUEST | VALIDATION_ERROR | Endpoint accepts a JSON request body; GlobalExceptionHandler maps a malformed or invalid body (HttpMessageNotReadableException / MethodArgumentNotValidException) to this status. |

<<<END INPUT>>>

<<<INPUT: registry-srs>>>
## REGISTRY — P1 — MDL v1
══════════════════════════════════════════════════════════════════

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|
| ENT-MDL-001 | نوع اللوكب / LookupType | master | SHARED (owner) | REGISTERED |
| ENT-MDL-002 | قيمة اللوكب / LookupValue | lookup | SHARED (owner) | REGISTERED |

Consumed
| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ |
|---|---|---|---|
| ModuleRegistry | ENT-SEC-004 | SEC | SOFT-READ |

Lookups owned
None (see SRS A6).

Lookups consumed
None.

Screens
| SCR-REQ id | Name (ar/en) | Page code |
|---|---|---|
| SCR-REQ-MDL-001 | اللوكبات العامة / Generic Lookups | MDL_LOOKUPS |
| SCR-REQ-MDL-002 | سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner | MDL_TYPE_REGISTRY |

Requirements
REQ count: 13 · AC count: 13 · RULE count: 4 · ENT count: 2 · SCR-REQ count: 2
Last sequence per atom: REQ: 013 · AC: 013 · ENT: 002 · RULE: 004 · SCR-REQ: 002

REQ ids (full text in srs-mdl.md → A4): REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004,
REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-011,
REQ-MDL-012, REQ-MDL-013

AC ids (full text in srs-mdl.md → A4, one per REQ above): AC-MDL-001, AC-MDL-002,
AC-MDL-003, AC-MDL-004, AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-009,
AC-MDL-010, AC-MDL-011, AC-MDL-012, AC-MDL-013

RULE ids (full text in srs-mdl.md → A5): RULE-MDL-001, RULE-MDL-002, RULE-MDL-003,
RULE-MDL-004

Decisions
ADR ids: none.

Event
"P1 completed: MDL v1 — 2 entities, 13 requirements, 13 acceptance criteria, 4 rules, 2 screen requirements, 0 ADRs"
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: registry-exec-be>>>
## REGISTRY — P3.1 — MDL v1
══════════════════════════════════════════════════════════════════

ID RANGES
API-MDL-001 .. API-MDL-011 · QR-MDL-001 .. QR-MDL-015

API ids: API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006,
API-MDL-007, API-MDL-008, API-MDL-009, API-MDL-010, API-MDL-011

QR ids: QR-MDL-001, QR-MDL-002, QR-MDL-003, QR-MDL-004, QR-MDL-005, QR-MDL-006, QR-MDL-007,
QR-MDL-008, QR-MDL-009, QR-MDL-010, QR-MDL-011, QR-MDL-012, QR-MDL-013, QR-MDL-014,
QR-MDL-015

ENTITIES / TABLES bound
ENT-MDL-001 → MDL_LOOKUP_TYPE, ENT-MDL-002 → MDL_LOOKUP_VALUE; lookups owned: none (MDL
is the mechanism, not a consumer of its own kind).

XM STATUS
open: none · deferred: none · active: XM-MDL-001 (SOFT-READ → SEC, target already gated).

CATALOG
10 error-catalog rows (9 module-specific/PLATFORM-STD + the shared 403 convention).
Rules without a message: none — every RULE-MDL-001..004 has a full ar/en message pair.

ALIGN
PASSED ✓ · 0 findings.

ADRs
none this stage (0 new; SEC's ADR-SEC-002 convention cited, not re-derived).

TRACEABILITY
REQ covered by ≥1 API/DBF: 13/13 (every REQ-MDL-001..013 appears in ≥1 API block's traces=
or ≥1 DBF record's traces in db-script-mdl.md). Orphan REQ: none.

Event
"P3.1 completed: MDL v1 — 11 API, 15 QR, ALIGN PASSED, 0 new ADRs"
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

---
# KNOWLEDGE (profile primary sources — cite as [KB:<file> §n])

<<<KB: profiles/erp/knowledge/erp-domain-standards.md>>>
# ERP Domain Standards — knowledge base for the `erp` profile

```
Profile   : erp            (profiles/erp.yaml → knowledge.files)
Role      : PRIMARY SOURCE the engines may cite (domain-profile, P0, P1, P2, P3.x)
            when they resolve an ambiguity themselves (factory.yaml → ambiguity).
Replaces  : the former "platform-standards.md Section M" that engines referenced
            but that never existed in the factory.
Rule      : a citation to this file is written as [KB:erp-domain-standards §n].
```

## §1 Module tiers
| Tier | Purpose | Typical modules |
|---|---|---|
| Tier 0 — Foundation | must exist before any business module | Organization (ORG), Security (SEC), Master Data Lookup (MDL) |
| Tier 1 — Core business | first revenue/cost flows | Procurement (PRC), Finance (FIN), Inventory (INV) |
| Tier 2 — Extended business | depends on Tier 1 | Sales (SLS), Contracts (CTR), Human Resources (HR) |

A module may only declare a HARD-FK XM towards a module of the same or a lower tier.

## §2 Entity kinds and defaults
Entity kinds and their default fields are declared in `profiles/erp.yaml → conventions.entity_defaults`.
Rules the engines apply on top:
1. Every master entity is bilingual (`nameAr`, `nameEn`) and soft-deletable (`isActiveFl`).
2. Transactional documents are period-bound (`fiscalYearId`, `periodId`) and status-driven (`statusCode` from a lookup).
3. Lookups are owned by MDL; a module never stores a lookup's display text, only its code.
4. No entity generates its own document numbers — the platform numbering engine does.

## §3 Business-policy conventions (P0 → POL-*)
- A policy is a single, testable sentence in EARS form (see factory.yaml → ids.ears).
- Policies that cross modules are declared once, in the owning (lower-tier) module, and referenced by code elsewhere.
- Fiscal policies (period locking, posting rules) belong to FIN; approval-limit policies belong to the module that owns the document.

## §4 Screens, security and permissions
- Composite screens: Search + Entry (or Master + Detail, Wizard) = ONE `SCR-*` and ONE `SEC_PAGES` row.
- Permission pattern and gateway action: `profiles/erp.yaml → conventions.security_model`.
- Backend: one controller per composite screen; authorization per method (gateway action on reads; CREATE/UPDATE/DELETE on mutations).
- Frontend: one lazily-loaded chunk per composite screen; Search↔Entry via route params.

## §5 Cross-module dependencies (XM)
- `HARD-FK`: a physical foreign key to another module's table — allowed only downward in tier.
- `SOFT-READ`: a read-only lookup by code — allowed in any direction.
- Every XM cites the `REQ-*` that needs it; the consuming module owns the XM record.

## §6 Defaults an engine may assume without asking (after PRD approval)
| Question | Default |
|---|---|
| Soft delete vs hard delete | soft (`isActiveFl`) |
| Audit trail | the four audit fields on every table |
| Paging | server-side, page size 20, max 200 |
| Search | server-side filter on code/name (both languages) |
| Money | `NUMERIC(18,4)`, currency code from MDL |
| Dates | `TIMESTAMPTZ`, stored UTC, displayed in tenant timezone |

Anything not covered here becomes an ADR (`decisions/<MOD>/`) per the ambiguity rule.

<<<END KB>>>

