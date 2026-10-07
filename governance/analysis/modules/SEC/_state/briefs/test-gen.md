# BRIEF — stage `test-gen` (Test Generation) · module SEC · v1 · profile `erp`

Lane `test-gen` · implementer claude:opus · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `analysis/decisions/SEC/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: TC — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `governance-shared/analysis/modules/SEC/test_gen/backend-test-plan-sec.md`
- `governance-shared/analysis/modules/SEC/test_gen/frontend-test-plan-sec.md`
- `governance-shared/analysis/modules/SEC/test_gen/test-execution-manifest-sec.md` (optional)
- `governance-shared/analysis/platform/system-test-index-erp.md` (optional)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C10** acceptance criteria → test generation (standalone): C10.1 traces {'from': 'TC', 'to': ['AC', 'XM', 'UXD'], 'min': 1, 'mode': 'any'} [CRITICAL]; C10.2 orphans {'kind': 'AC', 'referenced_by': ['TC'], 'min': 1} [MAJOR]; C10.3 markers {'artifact': 'backend-test-plan', 'track': 'backend', 'plan': 'test'} [CRITICAL]; C10.4 markers {'artifact': 'frontend-test-plan', 'track': 'frontend', 'plan': 'test'} [CRITICAL]; C10.5 ids-owned {'stage': 'test-gen'} [CRITICAL]; C10.6 exists {'artifact': 'test-execution-manifest', 'when': 'profile.stack.testing.manifest'} [MINOR]

---
# ENGINE
```
ENGINE        : test-gen — Test Generation   (STANDALONE — outside the line, on demand)
LANE          : test-gen · questions forbidden · derives from `AC-*` (module) + `XM-*`/`UXD-*` (integration)
SCOPE         : module · modules SEC
MODULE        : SEC · v1 · profile erp (ERP Platform)
READS         : srs · backend-execution-plan? · frontend-execution-plan? · registry-srs · registry-db? · registry-exec-fe?   (from _state/ — "?" = optional — read for EACH module in scope)
PRODUCES      : backend-test-plan-sec.md · frontend-test-plan-sec.md · test-execution-manifest-sec.md (optional)
OWNS IDS      : TC
FRAMEWORK     : backend `agnostic` · frontend `agnostic` · manifest ON   (profile.stack.testing)
BOUNDARY      : analysis-only — test PLANS, never test code; never a gate for the core
```

# Test Generation — engine reference

## 0. Position

This engine runs **outside the governed line** (`factory.yaml → standalone`). No pass gates
on it, it gates nothing, and the pass gates never wait for it. It is invoked on demand
(`/test-gen`) once the
artifacts it consumes exist in `_state/`. It **invents nothing**: no rule, error,
endpoint, field, screen or cross-module flow — it only adds test cases and the indexes
derived from them.

Questions are `forbidden`; ambiguity → `factory.yaml → ambiguity` (ADR, then
`continue`; breaking → `BLOCKED`,
`stop`) — shared/GOVERNANCE-CORE.md.

Delta versions: read `_state/` as the baseline, emit only ADDED / MODIFIED / REMOVED
test cases, continue the `TC` sequence — shared/VERSIONING.md.

## 1. Inputs

| Input | Read from | Use |
|---|---|---|
| `srs` | `_state/current-srs.md` | **the derivation source**: every `REQ-*` with its `AC-*` (Given / When / Then), `RULE-*` messages, screens, permissions |
| `backend-execution-plan` (optional) | `_state/current-backend-execution-plan.md` | `API-*` (verb, path, request/response, catalog codes) to bind backend steps to endpoints; its `XM-*` blocks (target module, type HARD-FK/SOFT-READ, traces) are the **integration derivation source** on the backend track |
| `frontend-execution-plan` (optional) | `_state/current-frontend-execution-plan.md` | `SCR-*`, routes, F-blocks to bind frontend steps to screens; its `UXD-*` references (screen, foreign field, owner module) are the **integration derivation source** on the frontend track |
| `registry-srs` | `_state/current-registry-srs.md` | ID ranges, coverage of REQ by API/SCR |
| `registry-db` (optional) | `_state/current-registry-db.md` | the module's `XM-*` register (target module, type) — cross-checked against the `XM` blocks above, never restated |
| `registry-exec-fe` (optional) | `_state/current-registry-exec-fe.md` | the module's `UXD-*` register (screen, field, owner module, API used) — cross-checked against the `UXD-*` references above, never restated |

A missing optional plan → the corresponding test plan is written in **REDUCED** mode
(steps reference the AC only, no endpoint/screen binding) and says so in its header. A
missing `registry-db`/`registry-exec-fe` never blocks module-scope derivation — it only
narrows what integration-scope cross-checking can do.

## 2. Scope — a command flag, never prose

```
gov.py run-standalone test-gen --module MOD                 # this run: scope=module, mods=[MOD]
gov.py run-standalone test-gen --modules MOD-A,MOD-B,…       # this run: scope=modules, mods=[MOD-A, MOD-B, …]
gov.py run-standalone test-gen --scope project               # this run: scope=project, mods=every module with a committed version
```

This run: **scope = `module`**, modules = `SEC`.

| Scope | TC sources | Phases populated |
|---|---|---|
| `module` | `AC-*` of the one module (§3) | the module-scope phases only (`TEST-PLAN-BE` on backend; the equivalent on frontend) — **identical in shape to a single-module run today** |
| `modules` | `AC-*` per module (§3) **+** `XM-*` (§4) and `UXD-*` (§5) between the *selected* modules only | module-scope phases for every selected module, plus the integration phase(s) (`profile…phases[*].integration: true`) wherever a real linking atom pairs two selected modules |
| `project` | everything `modules` does, across **every** module the factory has ever produced a version for, **plus** a derived coverage rollup | the same as `modules`, plus `system-test-index-{profile}.md` (§9) — no new atom, no new gate |

Rules that hold at every scope:
1. a module never gets an integration TC for a module that is **not** in the current
   selection — running `--module SEC` alone never touches another module's `XM`/`UXD`;
2. an integration phase with no real linking atom among the selected modules stays **absent**
   from the output — never an empty `PHASE` block, never a guessed pairing;
3. `--module` output is produced exactly as it always was — no integration phase key, no
   `system-test-index`, no extra section — so a single-module run stays byte-identical in
   shape to before this scope model existed.

## 3. Derivation (module scope) — every `TC-*` comes from an `AC-*`

`TC-*` (`TC-SEC-{seq}`, 3-digit seq,
one continuous sequence across the module — both plans share it, so no TC id repeats) traces → AC + XM + UXD. The
module-scope derivation is **mechanical**:

| AC part | becomes |
|---|---|
| **Given** | preconditions — data state, role/permission, system state (bound to real entities/screens from the plans) |
| **When** | the step list — for backend: the endpoint call (`API-*`, verb, path, payload from the AC); for frontend: navigation + user actions on `SCR-*` |
| **Then** | expected result — status/response (per `LocalizedException → {code, messageAr, messageEn}` when a RULE fires) or UI state; message asserted in every language (ar, en) |

Rules:
1. one `TC-*` per `AC-*`, always — an AC without a TC is a coverage gap (✗), never skipped;
2. an AC whose Then names a `RULE-*` violation yields the **violation** TC; its happy-path
   twin exists only if another AC states it — do not fabricate happy paths;
3. a **boundary** TC is added only when the AC (or the RULE it cites) states a numeric limit;
4. every TC cites, besides its AC: the `REQ-*`, and the `API-*` (backend) or `SCR-*`
   (frontend) it exercises, plus the `RULE-*` / catalog code when a violation is expected;
5. never reword a rule, message or endpoint — reference by ID/code; message text is copied
   character-perfect from the SRS in every language;
6. over-engineering guard: if a track's TC count exceeds ~2× its AC count, review — the
   extra TCs are almost always fabricated variants; remove them.

Scenario tags (one per TC): `HAPPY | VIOLATION | BOUNDARY | PERMISSION | STATE | INTEGRATION`;
data class: `VALID | INVALID | BOUNDARY | EDGE | ATTACK`.
Permission ACs (no `VIEW` / no action permission) produce `PERMISSION` TCs on both tracks.

## 4. XM → TC derivation (integration scope, backend)

Runs only at `scope: modules|project`, and only for an `XM-*` whose **target module** is
also in `SEC` — an `XM-*` targeting a module outside the selection is
left alone (not this run's concern, not a gap either).

Source: the `XM-*` blocks of the **declaring** (consuming) module's `backend-execution-plan`
(cross-checked against its `registry-db`, never restated — [XM-PROTOCOL.md](../../../shared/XM-PROTOCOL.md)).
The **declaring module owns the resulting TC** — one continuous `TC-{MOD}-<seq>` sequence,
same rule as module scope (design decision: integration TC ownership follows declaration,
not the target).

| XM type | TC scenario |
|---|---|
| `HARD-FK` | one `EXISTS` TC (the referenced row is present — request/flow succeeds) **and** one `MISSING` TC (the referenced row is absent — the physical constraint is honoured: the documented rejection, never a silent pass) |
| `SOFT-READ` | one `GRACEFUL-DEGRADATION` TC — the target read fails or returns empty and the declaring module's flow still returns a defined result (never a 500 / unhandled state) |

Rules:
1. never invent the target entity's shape — bind by `ENT`/`DBF` ID only, as the `XM` block
   already does; the TC exercises the declaring module's own `API-*`, not the target's;
2. tag every such TC `INTEGRATION`, data class per the row above;
3. `traces=` carries the `XM-*` id plus the `REQ-*` the XM itself traces to (and the `API-*`
   exercised, when the plan binds one) — never an `AC-*` that does not exist for it;
4. one `XM-*` never yields more than the two/one TC(s) in the table above — no fabricated
   extra scenarios ("over-engineering guard" of §3 applies here too).

## 5. UXD → TC derivation (integration scope, frontend)

Runs only at `scope: modules|project`, and only for a `UXD-*` whose **owner module** (the
module that owns the displayed data) is also in `SEC`.

Source: the `UXD-*` references of the **displaying** module's `frontend-execution-plan`
F4 blocks (cross-checked against its `registry-exec-fe`, never restated). The **displaying
module owns the resulting TC** (it is the one whose screen renders the foreign field).

| UXD case | TC scenario |
|---|---|
| foreign field rendered | one TC: navigate to the `SCR-*`, the foreign-owned field renders the value the owner module's API returns |
| foreign field empty / owner API failure | one TC: the screen shows its declared empty/error state (§A.3 `States` of the ui-ux-spec) — never a blank crash, never invented copy |

Rules:
1. never invent the owner module's field shape or a new permission — the UXD block and the
   owner's real `API-*` (from api-docs, when present in state) are the only sources;
2. tag every such TC `INTEGRATION`; `traces=` carries the `UXD-*` id plus its `REQ-*`/`AC-*`
   and the `SCR-*` it renders on;
3. one `UXD-*` never yields more than the two TCs in the table above.

## 6. TC block — framework-agnostic form

```
<!-- TC:TC-SEC-<seq>:START traces=AC-SEC-<seq>,REQ-SEC-<seq>[,API-SEC-<seq>|SCR-SEC-<seq>|XM-SEC-<seq>|UXD-SEC-<seq>] -->
### TC-SEC-<seq> — <title>
Derived from : AC-SEC-<seq>  (REQ-SEC-<seq>)   |   XM-SEC-<seq> (REQ-SEC-<seq>)   |   UXD-SEC-<seq> (REQ-SEC-<seq>, AC-SEC-<seq>)
Exercises    : API-SEC-<seq> <verb path>   |   SCR-SEC-<seq> <route>
Rule / code  : RULE-SEC-<seq> → <catalog code> | —
Scenario     : <tag> · data class <class> · language <ar|en|ALL>
Preconditions: <from Given — concrete entities, role, state | for XM/UXD: the target/owner entity present or absent>
Steps        : 1. … 2. … (from When — one observable action per step)
Expected     : <from Then — status / body shape / message per language / UI state>
Test data    : <values named in the AC; placeholders marked, never invented business data>
<!-- TC:TC-SEC-<seq>:END -->
```
Framework: `profile.stack.testing` is **agnostic** on both tracks — the block above is the whole
contract; the consumer repo chooses its tool and turns each TC into a test. No framework
name, annotation or file layout is mentioned anywhere in the plan.

## 7. Test plans — organised by the profile's test phases

Each track with a `test` plan in the profile gets one file per module, wrapped in the
profile's test phases with `TC` atoms (kind `TC`, level 3, parents PHASE/SUB,
plans test). Test-plan SUB ids are **bare** labels
(`factory.markers.rules.sub_unqualified_exempt_plans` = test).
A phase flagged `integration: true` in the profile is populated **only** at `scope:
modules|project`, per §2/§4/§5 — at `scope: module` it is skipped entirely, so single-module
output carries the module-scope phases only, exactly as before this scope model existed.

### Track `backend` — one file per selected module: `backend-test-plan-sec.md`

| Phase key | Split rule | SUB labels |
|---|---|---|
| `TEST-PLAN-BE` | SUB when TC count > 12 — grouped RULE-SCENARIOS / API-SCENARIOS | `RULE-SCENARIOS`, `API-SCENARIOS` |
| `INT-XM` _(integration — `scope: modules|project` only)_ | SUB when TC count > 8 — grouped per target module | — |
Layout (per selected module):
```
<header>   sources (_state files + versions) · scope `module` · framework note (§3) · REDUCED? · open ADRs
<!-- PHASE:TEST-PLAN-BE:START traces=<union of the TCs' REQ/AC> -->
  <!-- SUB:RULE-SCENARIOS:START traces=… -->  …TC blocks…  <!-- SUB:RULE-SCENARIOS:END -->
  <!-- SUB:API-SCENARIOS:START traces=… -->  …TC blocks…  <!-- SUB:API-SCENARIOS:END -->
  (SUBs only when the threshold is met — decide WHILE writing, from the TC count)
<!-- PHASE:TEST-PLAN-BE:END -->
TC TRACEABILITY INDEX   AC → TC · REQ → TC · API → TC · RULE/code → TC
COVERAGE                AC covered <n>/<total> (a gap is ✗ and blocks the run) · REQ covered · API covered
```
Backend grouping hint: rule-driven ACs (violations, state transitions) vs endpoint-driven ACs
(happy paths, permission, paging/empty-result per `Page<T>`); integration TCs (§4) group by target module inside `INT-XM`.

### Track `frontend` — one file per selected module: `frontend-test-plan-sec.md`

| Phase key | Split rule | SUB labels |
|---|---|---|
| `TEST-PLAN-FE` | SUB when TC count > 8 — grouped UI-FLOWS / INT-FLOW | `UI-FLOWS`, `INT-FLOW` |
| `INT-UXD` _(integration — `scope: modules|project` only)_ | SUB when TC count > 8 — grouped per source module | — |
Layout (per selected module):
```
<header>   sources (_state files + versions) · scope `module` · framework note (§3) · REDUCED? · open ADRs
<!-- PHASE:TEST-PLAN-FE:START traces=<union of the TCs' REQ/AC> -->
  <!-- SUB:UI-FLOWS:START traces=… -->  …TC blocks…  <!-- SUB:UI-FLOWS:END -->
  <!-- SUB:INT-FLOW:START traces=… -->  …TC blocks…  <!-- SUB:INT-FLOW:END -->
  (SUBs only when the threshold is met — decide WHILE writing, from the TC count)
<!-- PHASE:TEST-PLAN-FE:END -->
TC TRACEABILITY INDEX   AC → TC · REQ → TC · SCR → TC · RULE/code → TC
COVERAGE                AC covered <n>/<total> (a gap is ✗ and blocks the run) · REQ covered · SCR covered
```
Frontend grouping hint: per-screen flows (search, create/edit, violation shown on screen,
permission-hidden affordance) vs the single module lifecycle flow (create → search → update →
deactivate → gone from active results); the composite-screen invariant (entry never rendered open by default) is one TC when an AC states it; integration TCs (§5) group by source (owner) module inside `INT-UXD`.

## 8. Test-execution manifest (ON — `profile.stack.testing.manifest`)

Emitted in the same run, after the backend test plan, as
`test-execution-manifest-sec.md` —
a **derived view** for the `api-verify` standalone (it introduces no ID), one per module:
```
DEPENDENCY ORDER      topological entity build order (from FK/XM relations in the backend plan and "must belong to" RULEs)
RULE → CODE → TC      RULE-* │ catalog code (runtime format per LocalizedException → {code, messageAr, messageEn}) │ TC-* │ HTTP │ API-*   (informational-only RULEs excluded)
ENTITY CRUD CHECKLIST ENT-* │ create │ read │ search │ update │ deactivate (soft) │ activate (each ✓ / —, from the API-* set)
```
Regenerate whenever the backend plan or test plan changes — a stale manifest is a defect.

## 9. System test index (`scope: project` only)

Emitted once per run, after every selected module's test plans, as
`system-test-index-erp.md` — a platform-level **derived view**
(introduces no ID, never a gate, `paths.platform` — same tier as `project-registry.md`):
```
SYSTEM TEST INDEX — ERP Platform — generated {scope: project}
AC → TC       per module: AC covered <n>/<total>, list of uncovered AC (✗)
XM → TC       every XM-* between two modules that both have a committed version: covered ✓/✗ (✗ = gap, never silently dropped)
UXD → TC      every UXD-* whose owner module also has a committed version: covered ✓/✗
COVERAGE %    per module: AC%, and — where the module both declares and is targeted by XM/UXD — integration%
CROSS-MODULE  matrix of module → module, one row per XM/UXD pair, TC id(s) covering it (— if none: gap ✗)
```
Never restates TC content — every row is an ID reference. A module the platform has never
produced a version for is out of scope (not a gap): the index only rolls up modules that
exist.

## 10. Split

The same toolkit splits test plans, with plan key `test`
(`factory.tracks.<track>.packages.test` → `backend-test`, `frontend-test`), **per module**:
```
gov.py split --track <track> --module <MOD> --version <v> --plan test --dry-run   # validate, non-zero exit = fix first
gov.py split --track <track> --module <MOD> --version <v> --plan test
```
Every TC atom is verified by content hash (`factory.markers.rules.verify` = sha256) after the split.
`system-test-index-erp.md` is a platform-level file — it is never split, never packaged, never delivered to a consumer repo.

## 11. Self-check before finishing

```
[ ] every AC-* in the SRS (of every selected module) has ≥1 TC-* (coverage ✗ = not done)
[ ] every TC-* carries traces= with its AC-*/XM-*/UXD-* source (+ the upstream ids the plan names) and the atom marker pair
[ ] every TC names one of AC/XM/UXD as its source; no reworded rule/message/endpoint; test data never invented
[ ] phases = the profile's test phases, in order; SUB labels bare; thresholds checked while writing
[ ] framework wording matches §6; manifest emitted iff profile.stack.testing.manifest (per module)
[ ] at scope module: no integration phase, no system-test-index — output unchanged from before
[ ] at scope modules|project: every XM-*/UXD-* between two SELECTED modules has ≥1 TC or is
    recorded as a gap (✗) — never silently dropped, never fabricated when absent
[ ] at scope project: system-test-index-erp.md rolls up every module with
    a committed version; every AC/XM/UXD gap listed is ✗, exactly like a module-scope AC gap
[ ] ADRs written for every derivation choice that was not mechanical
```

## 12. Boundaries

| Owns | References (never redefines) | Never |
|---|---|---|
| `TC-*`, the test plans, the manifest | `REQ/AC/RULE` (P1), `API` (P3.1), `SCR/UXD` (P3.2 — `UXD` is `P3.2`'s, cited never redefined), `DBF/XM` (P2 — `XM` is `P2`'s, cited never redefined), catalog codes | test code, framework scaffolding, any edit to a line artifact, any gate or verdict, a cross-module TC for a module outside the current selection |


---
# INPUTS (generated current state)

<<<INPUT: srs>>>
# SRS — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved 2026-09-10)
Counts : ENT 13 · REQ 33 · AC 33 · RULE 7 · SCR-REQ 10 · ADR 0
══════════════════════════════════════════════════════════════════

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | SEC — الأمان / Security |
| Feature code | SEC |
| Version | v1 |
| Date | 2026-09-10 |
| Status | DRAFT (P1) |
| Prepared by | governance-factory (analysis lane) |
| Decisions applied count | 0 (no ADR was needed — no ambiguity §9 was reached) |

## A2 — Functional context

**In scope:** المصادقة (تسجيل الدخول، التسجيل الذاتي، إعادة تعيين كلمة المرور)، RBAC هرمي
بثلاث مستويات (وحدة→شاشة→إجراء)، تسجيل الوحدات/الشاشات/الإجراءات كبيانات لأي وحدة مستهلكة،
فصل المهام (SoD) على مستوى المستخدم، القائمة الديناميكية ثنائية المستوى، لوحة تحكم الأمان،
سجل التدقيق غير القابل للتعديل، إدارة الجلسات النشطة، تكامل اختياري مع خدمة الإشعارات.

**Out of scope:** المصادقة متعددة العوامل (MFA)، تسجيل الدخول الموحد (SSO)/موفرو هوية
خارجيون — غير مذكورين في `security-module-plan-en.md` [business-policies-sec.md →
SCOPE EXCEPTIONS]؛ أي منطق عمل خاص بوحدة مستهلكة.

**Module function (one paragraph):** وحدة SEC هي نظام الأمان الوحيد للمنصة بأكملها: تُصدر
الهوية (المصادقة) وتُقرّر الصلاحيات الفعلية (RBAC هرمي)، بحيث لا تملك أي وحدة أخرى مستخدمين
أو أدوارًا أو تسجيل دخول خاصًا بها؛ كل وحدة تستهلك SEC عبر تسجيل نفسها كبيانات ثم فحص
المنح الصادرة عنها.

**Detailed description (workflow narrative, roles):** مستخدم يُسجّل ذاتيًا فيبقى معلّقًا بلا
صلاحيات → يوافق مسؤول أمان عليه فيصبح نشطًا → يُسنَد له دور واحد أو أكثر → عند كل طلب،
يُفحص منح الوحدة أولاً (بوابة)، ثم منح الشاشة، ثم منح الإجراء. مسؤول الأمان يدير الأدوار
والمستخدمين والجلسات النشطة ويراقب سجل التدقيق ولوحة التحكم. أي وحدة مستهلكة (مثل FIN
لاحقًا) تُسجّل نفسها وشاشاتها وإجراءاتها هنا كبيانات فقط، دون أي تعديل على شيفرة SEC.

**Current situation:** لا يوجد نظام أمان سابق ضمن هذه الدفعة — هذه أول وحدة تُبنى (Tier 0)؛
لا "وضع حالي" يُستبدل داخل هذه المنصة الجديدة.

**Current difficulties:** لا ينطبق (وحدة جديدة بالكامل).

**Proposed system and benefits:** نظام أمان مركزي واحد يمنع ازدواج/تضارب الصلاحيات بين
الوحدات، يضمن بوابة وحدة صارمة (لا شاشة يتيمة)، ويوفر أثرًا تدقيقيًا كاملاً غير قابل للتعديل.

**General notes (constraints, deferred items):** محرك سير العمل ممنوع منصّيًا
(`profiles/erp.yaml → conventions.workflow_engine: forbidden`)؛ لا آلية قفل تلقائي بعد محاولات
دخول فاشلة متكررة — لم يذكرها `security-module-plan-en.md`، فلم تُخترع (تُعرض فقط أعداد
الدخول الفاشل في لوحة التحكم، REQ-SEC-002/022).

## A3 — Entities and fields

Standard fields per kind (profile.conventions.entity_defaults): the `security` entity
kind carries no fixed default-field set in the profile (only master/transactional/
lookup/config do); every SEC entity below still carries the platform's audit fields
(`createdBy, createdAt, updatedBy, updatedAt`, `profile.stack.db.naming.audit_fields`)
except pure append-only log/session rows where a "who created it" field is redundant
with the row's own actor field (documented per entity).

### ENT-SEC-001 — المستخدم / User
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) — every module's audit fields (createdBy/updatedBy) reference it | No — login identity (email/username) is the natural key, not a generated number [§3.3 NUMBERING test] | create, read, search, update, activate, deactivate | consumed read-only by every future consumer module for its own audit fields | security-module-plan-en.md §3, §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| userPk | number | yes (system) | — | primary key | معرّف المستخدم | User id |
| username | text | yes | unique | login identity | اسم المستخدم | Username |
| email | text | yes | unique, valid email | used for password-reset delivery | البريد الإلكتروني | Email |
| passwordHash | text | yes (system) | never exposed to any client [POL-SEC-004] | write-only | تجزئة كلمة المرور | Password hash |
| fullNameAr | text | yes | — | — | الاسم الكامل (عربي) | Full name (Arabic) |
| fullNameEn | text | yes | — | — | الاسم الكامل (إنجليزي) | Full name (English) |
| statusCode | lookup | yes | lookup key `USER_STATUS` (A6) | drives A7 lifecycle | الحالة | Status |
| lastLoginAt | date-time | no | — | informational | آخر دخول | Last login |
| failedLoginCount24h | number | no | derived, not stored per ERP-…(see POL-SEC-010) — displayed from AuditLogEntry, not persisted on User | dashboard-only figure; kept here only as a documentation note, not a real column | عدد محاولات الدخول الفاشلة (٢٤س) | Failed logins (24h) |
| isActiveFl | flag | yes | true/false | mirrors statusCode ≠ DISABLED, kept for the platform's standard flag convention | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields | — | — |

### ENT-SEC-002 — الدور / Role
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create, read, search, update, deactivate | none | security-module-plan-en.md §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| rolePk | number | yes (system) | — | primary key | معرّف الدور | Role id |
| code | text | yes | unique | stable machine reference | رمز الدور | Role code |
| nameAr, nameEn | text | yes | — | — | اسم الدور | Role name |
| descriptionAr, descriptionEn | text | no | — | — | الوصف | Description |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-003 — ربط المستخدم بالدور / UserRoleAssignment
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create, read (list), delete (revoke) | none | security-module-plan-en.md §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| userRoleAssignmentPk | number | yes (system) | — | primary key | معرّف الإسناد | Assignment id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| assignedBy, assignedAt | system | yes | — | who/when granted | — | — |

### ENT-SEC-004 — سجل الوحدات / ModuleRegistry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) — every consuming module registers one row of itself here | No — the module code is the platform's own `{prefix}` (factory.ids), not a SEC-generated number | create (by a registering module), read, search, deactivate | consumed by every module registering itself (e.g. FIN in a later pass) | security-module-plan-en.md §4.3, §7 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| moduleRegistryPk | number | yes (system) | — | primary key | معرّف الوحدة المسجّلة | Registered module id |
| code | text | yes | unique — the platform module code (e.g. FIN, SEC) | matches `profile.vocabulary.module_prefixes` | رمز الوحدة | Module code |
| nameAr, nameEn | text | yes | — | — | اسم الوحدة | Module name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-005 — سجل الشاشات / ScreenRegistry (SEC_PAGES)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) | No | create (by a registering module), read, search, deactivate | consumed by every module registering its own screens | security-module-plan-en.md §4.3, §4.5; profiles/erp.yaml conventions.security_model.page_registry |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| screenRegistryPk | number | yes (system) | — | primary key | معرّف الشاشة المسجّلة | Registered screen id |
| pageCode | text | yes | unique | `SEC_PAGES` row per §7.1 | رمز الصفحة | Page code |
| moduleId | reference | yes | ENT-SEC-004; must already be registered [RULE-SEC-004] | — | الوحدة | Module |
| nameAr, nameEn | text | yes | — | — | اسم الشاشة | Screen name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-006 — سجل الإجراءات / ActionRegistry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) | No | create (by a registering module), read, search, deactivate | consumed by every module registering its own actions | security-module-plan-en.md §4.1, §4.3; profiles/erp.yaml conventions.security_model.permission_pattern |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| actionRegistryPk | number | yes (system) | — | primary key | معرّف الإجراء المسجّل | Registered action id |
| permissionCode | text | yes | unique, pattern `PERM_<PAGE_CODE>_<ACTION>` (profile.conventions.security_model.permission_pattern) — derived, never entered a second time as seed data | derivation, not duplication | رمز الصلاحية | Permission code |
| screenId | reference | yes | ENT-SEC-005; must already be registered [RULE-SEC-002] | — | الشاشة | Screen |
| actionCode | text | yes | one of the platform-standard set VIEW/CREATE/UPDATE/DELETE (profile.conventions.security_model.actions) or a module-declared custom code (e.g. "REVERSE_ENTRY") | free beyond the standard four, per plan §4.1 "custom actions" | الإجراء | Action |
| nameAr, nameEn | text | yes | — | — | اسم الإجراء | Action name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-007 — منح الوحدة للدور / RoleModuleGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant), read (list), delete (revoke — cascades per RULE-SEC-003) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleModuleGrantPk | number | yes (system) | — | primary key | معرّف منح الوحدة | Module grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| moduleId | reference | yes | ENT-SEC-004 | — | الوحدة | Module |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-008 — منح الشاشة للدور / RoleScreenGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant, blocked without matching module grant — RULE-SEC-001), read (list), delete (revoke) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleScreenGrantPk | number | yes (system) | — | primary key | معرّف منح الشاشة | Screen grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| screenId | reference | yes | ENT-SEC-005; role must hold its module [RULE-SEC-001] | — | الشاشة | Screen |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-009 — منح الإجراء للدور / RoleActionGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant, blocked without matching screen grant — RULE-SEC-002, and without VIEW on that screen unless the action itself is VIEW — RULE-SEC-007), read (list), delete (revoke) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleActionGrantPk | number | yes (system) | — | primary key | معرّف منح الإجراء | Action grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| actionId | reference | yes | ENT-SEC-006; role must hold the action's screen [RULE-SEC-002] and, unless the action itself is VIEW, must also hold VIEW on that screen [RULE-SEC-007] | — | الإجراء | Action |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-010 — الجلسة النشطة / ActiveSession
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, on login), read (list), terminate | none | security-module-plan-en.md §5.1, §5.3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| activeSessionPk | number | yes (system) | — | primary key | معرّف الجلسة | Session id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| tokenRef | text | yes | opaque reference — never the raw token/hash | — | مرجع الرمز | Token reference |
| startedAt | date-time | yes (system) | — | — | بدأت في | Started at |
| lastActivityAt | date-time | yes (system) | — | — | آخر نشاط | Last activity |
| ipAddress | text | no | — | — | عنوان IP | IP address |
| terminatedAt, terminatedBy | date-time / reference | no | set on logout or forced termination | null while active | أُنهيت في / بواسطة | Terminated at / by |

### ENT-SEC-011 — سجل التدقيق / AuditLogEntry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, append-only), read, search | none | security-module-plan-en.md §5.2-§5.3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| auditLogEntryPk | number | yes (system) | — | primary key | معرّف قيد التدقيق | Audit entry id |
| eventTypeCode | lookup | yes | lookup key `AUDIT_EVENT_TYPE` (A6) | — | نوع الحدث | Event type |
| actorUserId | reference | no | ENT-SEC-001 — null for an unauthenticated failed-login attempt | — | المستخدم الفاعل | Actor user |
| occurredAt | date-time | yes (system) | — | immutable once written [POL-SEC-009] | وقت الحدث | Occurred at |
| targetRef | text | no | free text (e.g. affected user/role id) | — | الهدف | Target |
| detailsAr, detailsEn | text | no | — | — | التفاصيل | Details |
| ipAddress | text | no | — | — | عنوان IP | IP address |
Note: this entity has no `createdBy`/`updatedBy` — it IS the audit record; `actorUserId` +
`occurredAt` serve that purpose, and it is never updated after insert (immutability, POL-SEC-009).

### ENT-SEC-012 — رمز إعادة تعيين كلمة المرور / PasswordResetToken
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, on request), read (validate), invalidate (on use or expiry) | none | security-module-plan-en.md §3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| passwordResetTokenPk | number | yes (system) | — | primary key | معرّف الرمز | Token id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| tokenHash | text | yes (system) | never exposed in full after issuance | write-once | تجزئة الرمز | Token hash |
| requestedAt, expiresAt | date-time | yes (system) | expiresAt = requestedAt + DEFAULT window (see A7 note) | — | — | — |
| usedAt | date-time | no | set once consumed [RULE-SEC-006] | — | استُخدم في | Used at |

### ENT-SEC-013 — طلب تسجيل معلّق / SignupRequest
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (self-service), read, search, approve, reject | none | security-module-plan-en.md §3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| signupRequestPk | number | yes (system) | — | primary key | معرّف طلب التسجيل | Signup request id |
| email | text | yes | valid email | becomes the user's login on approval | البريد الإلكتروني | Email |
| fullNameAr, fullNameEn | text | yes | — | — | الاسم الكامل | Full name |
| submittedAt | date-time | yes (system) | — | — | تاريخ التقديم | Submitted at |
| statusCode | lookup | yes | lookup key `SIGNUP_STATUS` (A6) | — | الحالة | Status |
| reviewedBy, reviewedAt | reference / date-time | no | set on approve/reject | — | — | — |

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-SEC-001 — تسجيل دخول ناجح / Successful login
Pattern    : event
Statement  : When a registered, active user submits valid credentials, the system shall issue a session/access token representing that user's identity.
Traces     : US-SEC-001
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : entry point every consuming module trusts
Source     : security-module-plan-en.md §3
Priority   : HIGH
#### AC-SEC-001 — [REQ-SEC-001]
Given an active user with a known username and password
When the user submits those correct credentials to the login screen
Then the system creates an active session for the user and grants access to the platform

### REQ-SEC-002 — رفض بيانات دخول غير صحيحة / Reject invalid credentials
Pattern    : unwanted
Statement  : If submitted credentials do not match an active user, then the system shall reject the login attempt and record a failed-login audit event.
Traces     : US-SEC-001
Entities   : ENT-SEC-001, ENT-SEC-011
Rationale  : POL-SEC-004 discipline; feeds the dashboard's failed-logins widget
Source     : security-module-plan-en.md §3, §5.1
Priority   : HIGH
#### AC-SEC-002 — [REQ-SEC-002]
Given a login attempt with a wrong password or an unknown/disabled username
When the user submits it
Then the system rejects the attempt with message ar: "بيانات الدخول غير صحيحة" · en: "Invalid credentials", issues no session, and appends one `LOGIN_FAILED` audit entry

### REQ-SEC-003 — تقديم طلب تسجيل / Submit sign-up
Pattern    : event
Statement  : When a prospective user submits a sign-up request, the system shall create a pending sign-up request with no permission granted to anyone.
Traces     : US-SEC-002
Entities   : ENT-SEC-013
Rationale  : self-registration without premature access
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-003 — [REQ-SEC-003]
Given a prospective user fills the sign-up form with a valid, not-already-registered email
When they submit it
Then the system creates a `SignupRequest` with status PENDING and creates no user account yet

### REQ-SEC-004 — الموافقة على طلب التسجيل / Approve a sign-up request
Pattern    : event
Statement  : When an administrator approves a pending sign-up request, the system shall create an active user account from it.
Traces     : US-SEC-002
Entities   : ENT-SEC-013, ENT-SEC-001
Rationale  : conversion from pending to real, permission-bearing identity
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
Note       : the credential of the created account is unspecified here and in AC-SEC-004, yet DBF-SEC-004 `password_hash` is NOT NULL. Operative behaviour: the account is created with an unusable random secret (never logged, returned or transmitted), so it is ACTIVE but cannot be authenticated against; the owner's route to a real credential is the existing REQ-SEC-006 → REQ-SEC-007 password-reset pair (RULE-SEC-006). OPEN for a human (not decided here): no SEC artifact says the approved owner is notified or that a reset token is issued at approval — REQ-SEC-029 triggers on token issuance, not approval, and is itself `optional` — and the state is undiscoverable from outside, since login answers the deliberately uniform "Invalid credentials" error (POL-SEC-004) and the reset request answers the same generic 200 whether or not the email exists, making "approved", "still pending" and "rejected" indistinguishable. The two candidate answers are (a) SEC issues a reset token at approval time — which introduces a NOTIF dependency into a module declared ROOT — or (b) the spec states plainly that the owner is informed out of band.
#### AC-SEC-004 — [REQ-SEC-004]
Given a SignupRequest with status PENDING
When an administrator approves it
Then the system creates a User with status ACTIVE from its email/name, and marks the SignupRequest APPROVED

### REQ-SEC-005 — رفض طلب التسجيل / Reject a sign-up request
Pattern    : unwanted
Statement  : If an administrator rejects a pending sign-up request, then the system shall mark the request rejected and create no user account.
Traces     : US-SEC-002
Entities   : ENT-SEC-013
Rationale  : symmetric negative path to REQ-SEC-004
Source     : security-module-plan-en.md §3
Priority   : LOW
#### AC-SEC-005 — [REQ-SEC-005]
Given a SignupRequest with status PENDING
When an administrator rejects it
Then the system marks it REJECTED and creates no User

### REQ-SEC-006 — إصدار رمز إعادة تعيين / Issue a password-reset token
Pattern    : event
Statement  : When a user requests a password reset, the system shall issue a single-use, time-limited reset token for that user.
Traces     : US-SEC-003
Entities   : ENT-SEC-012
Rationale  : secure self-service reset
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-006 — [REQ-SEC-006]
Given a user identifies themselves by a registered email
When they request a password reset
Then the system creates one PasswordResetToken with an expiry, and no password is changed yet

### REQ-SEC-007 — إتمام إعادة التعيين بنجاح / Complete a password reset
Pattern    : event
Statement  : When a user submits a valid, unexpired reset token with a new password, the system shall update that user's password and invalidate the token.
Traces     : US-SEC-003
Entities   : ENT-SEC-012, ENT-SEC-001
Rationale  : one-time use enforced
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-007 — [REQ-SEC-007]
Given an unexpired, unused PasswordResetToken and a new password meeting the platform's password rules
When the user submits them
Then the system updates the user's password hash, sets the token's usedAt, and appends a `PASSWORD_RESET_COMPLETED` audit entry

### REQ-SEC-008 — رفض رمز منتهٍ أو مُستخدَم / Reject an expired or used reset token
Pattern    : unwanted
Statement  : If a submitted reset token is expired or already used, then the system shall reject the password reset.
Traces     : US-SEC-003
Entities   : ENT-SEC-012
Rationale  : RULE-SEC-006
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-008 — [REQ-SEC-008]
Given a PasswordResetToken that is expired or already has a usedAt value
When it is submitted with a new password
Then the system rejects the request with message ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired" and changes nothing

### REQ-SEC-009 — إنشاء مستخدم / Create a user
Pattern    : event
Statement  : When an administrator creates a user, the system shall record that user's bilingual name, login identity and status.
Traces     : US-SEC-004
Entities   : ENT-SEC-001
Rationale  : direct administrative provisioning (distinct from self sign-up)
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-009 — [REQ-SEC-009]
Given an administrator fills the user form with a unique username/email and both name labels
When they save it
Then the system creates the User with status ACTIVE (or as chosen)

### REQ-SEC-010 — إسناد أدوار متعددة / Assign one or more roles to a user
Pattern    : event
Statement  : When an administrator assigns one or more roles to a user, the system shall record each assignment individually.
Traces     : US-SEC-004
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003
Rationale  : a user may hold several roles; effective permission is their union [POL-SEC-008]
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-010 — [REQ-SEC-010]
Given an administrator selects one or more active roles for a user
When they save the assignment
Then the system creates one UserRoleAssignment row per selected role

### REQ-SEC-011 — تعطيل مستخدم / Deactivate a user
Pattern    : event
Statement  : When an administrator deactivates a user, the system shall immediately end that user's active sessions and prevent new logins for that user.
Traces     : US-SEC-004
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : revoking access must be immediate, not just cosmetic
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-011 — [REQ-SEC-011]
Given an active user with one active session
When an administrator deactivates the user
Then the system sets status DISABLED, terminates every active session of that user, and a subsequent login attempt is rejected per REQ-SEC-002

### REQ-SEC-031 — إعادة تفعيل مستخدم / Reactivate a disabled user
Pattern    : event
Statement  : When an administrator reactivates a disabled user, the system shall restore that user's ability to sign in.
Traces     : US-SEC-004
Entities   : ENT-SEC-001
Rationale  : symmetric to REQ-SEC-011; "activate" listed alongside deactivate in the story
Source     : security-module-plan-en.md §4.4
Priority   : MEDIUM
#### AC-SEC-031 — [REQ-SEC-031]
Given a user with status DISABLED
When an administrator reactivates them
Then the system sets status ACTIVE and a subsequent login with correct credentials succeeds

### REQ-SEC-012 — منح وحدة لدور / Grant a module to a role
Pattern    : event
Statement  : When an administrator grants a module to a role, the system shall record that grant as the role's module-level access.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-004, ENT-SEC-007
Rationale  : the module gate, evaluated first [POL-SEC-001]
Source     : security-module-plan-en.md §4.1-§4.2
Priority   : HIGH
#### AC-SEC-012 — [REQ-SEC-012]
Given an active role and an active registered module
When an administrator grants that module to that role
Then the system creates one RoleModuleGrant row

### REQ-SEC-013 — رفض منح شاشة دون منح وحدة / Reject a screen grant without its module grant
Pattern    : unwanted
Statement  : If an administrator attempts to grant a screen of a module the target role does not hold, then the system shall reject the grant.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Rationale  : RULE-SEC-001; structural integrity, no orphaned grant [POL-SEC-002]
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-013 — [REQ-SEC-013]
Given a role with no RoleModuleGrant for module FIN
When an administrator attempts to grant that role a FIN screen
Then the system rejects the grant with message ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module" and creates no RoleScreenGrant

### REQ-SEC-014 — رفض منح إجراء دون منح شاشة / Reject an action grant without its screen grant
Pattern    : unwanted
Statement  : If an administrator attempts to grant an action of a screen the target role does not hold, then the system shall reject the grant.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-006, ENT-SEC-008, ENT-SEC-009
Rationale  : RULE-SEC-002; same structural-integrity principle one level down
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-014 — [REQ-SEC-014]
Given a role with no RoleScreenGrant for a given screen
When an administrator attempts to grant that role an action on that screen
Then the system rejects the grant with message ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen" and creates no RoleActionGrant

### REQ-SEC-015 — إلغاء المنح المتسلسل عند سحب الوحدة / Cascade-revoke on module-grant removal
Pattern    : event
Statement  : When an administrator revokes a role's module grant, the system shall also remove every screen and action grant that module covered for that role.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : RULE-SEC-003; prevents an orphaned screen/action grant from surviving its module grant
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-015 — [REQ-SEC-015]
Given a role holding a module grant plus two screen grants and three action grants under that module
When an administrator revokes the module grant
Then the system deletes the module grant and every screen/action grant it covered, leaving none behind

### REQ-SEC-016 — تسجيل وحدة جديدة / Register a new module
Pattern    : event
Statement  : When a consuming module registers itself, the system shall record its code and bilingual name in the module registry.
Traces     : US-SEC-006
Entities   : ENT-SEC-004
Rationale  : "no change to security code" onboarding
Source     : security-module-plan-en.md §4.3, §7
Priority   : HIGH
#### AC-SEC-016 — [REQ-SEC-016]
Given a module code not yet registered
When it is registered with its bilingual name
Then the system creates one active ModuleRegistry row

### REQ-SEC-017 — تسجيل شاشة لوحدة مسجّلة / Register a screen under a registered module
Pattern    : event
Statement  : When a consuming module registers a screen, the system shall record it under that module's already-registered code.
Traces     : US-SEC-006
Entities   : ENT-SEC-004, ENT-SEC-005
Rationale  : SEC_PAGES per §7.1
Source     : security-module-plan-en.md §4.3, §4.5
Priority   : HIGH
#### AC-SEC-017 — [REQ-SEC-017]
Given a registered, active module
When it registers a screen with a unique page code and bilingual name
Then the system creates one active ScreenRegistry row under that module

### REQ-SEC-018 — رفض تسجيل شاشة لوحدة غير مسجّلة / Reject a screen registered under an unregistered module
Pattern    : unwanted
Statement  : If a screen registration names a module that is not registered, then the system shall reject the screen registration.
Traces     : US-SEC-006
Entities   : ENT-SEC-004, ENT-SEC-005
Rationale  : RULE-SEC-004
Source     : security-module-plan-en.md §4.3
Priority   : MEDIUM
#### AC-SEC-018 — [REQ-SEC-018]
Given a module code with no ModuleRegistry row
When a screen registration names that code
Then the system rejects it with message ar: "الوحدة غير مسجّلة" · en: "Module is not registered" and creates no ScreenRegistry row

### REQ-SEC-019 — تسجيل إجراء لشاشة مسجّلة / Register an action under a registered screen
Pattern    : event
Statement  : When a consuming module registers an action on one of its screens, the system shall record it under that screen.
Traces     : US-SEC-006
Entities   : ENT-SEC-005, ENT-SEC-006
Rationale  : permission catalog per §4.1
Source     : security-module-plan-en.md §4.1, §4.3
Priority   : HIGH
#### AC-SEC-019 — [REQ-SEC-019]
Given a registered, active screen
When it registers an action code with bilingual name
Then the system creates one active ActionRegistry row with permission code `PERM_<pageCode>_<actionCode>`

### REQ-SEC-020 — منع تضارب الإجراءات لدى مستخدم واحد / Prevent one user from holding two conflicting actions
Pattern    : optional
Statement  : Where a consumer module declares two of its actions as conflicting, the system shall prevent a single user from holding both action grants at the same time, whether obtained through one role or several.
Traces     : US-SEC-007
Entities   : ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-005; SoD moved to the shared RBAC layer per general-accounting-system-plan-en.md §8.2/§10.3
Source     : security-module-plan-en.md §4.4
Priority   : MEDIUM
Note       : DEFERRED in SEC v1 — nothing in SEC v1 declares a conflicting pair, so this `optional` pattern's precondition is never established: there is no ENT, DBF, table, API field or screen element for such a declaration. The guards (RULE-SEC-005, QR-SEC-031) are implemented and inert — the conflicting-counterpart set is empty in v1. The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.
#### AC-SEC-020 — [REQ-SEC-020]
Given two actions declared conflicting by their owning module, and a user who already holds one of them (via any role)
When an administrator attempts to assign a role that would give that same user the other conflicting action
Then the system rejects the assignment with message ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"

### REQ-SEC-021 — القائمة تعرض الممنوح فقط / Menu shows only effective grants
Pattern    : event
Statement  : When a user's menu is rendered, the system shall include only the modules that user's effective grants hold, each showing only that user's effective granted screens beneath it.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Rationale  : POL-SEC-006
Source     : security-module-plan-en.md §6
Priority   : HIGH
#### AC-SEC-021 — [REQ-SEC-021]
Given a user whose roles' union grants exactly module FIN with screens "Journal Entries" and "Trial Balance"
When their menu renders
Then the system shows only FIN as a top-level entry with exactly those two screens beneath it

### REQ-SEC-032 — إخفاء الوحدة غير الممنوحة من القائمة / Hide an ungranted module from the menu
Pattern    : unwanted
Statement  : If a user's effective grants do not include a module, then the system shall omit that module entirely from that user's rendered menu.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-007
Rationale  : POL-SEC-007 (first half)
Source     : security-module-plan-en.md §4.2, §6
Priority   : HIGH
#### AC-SEC-032 — [REQ-SEC-032]
Given a user whose roles hold no grant for module FIN
When their menu renders
Then FIN does not appear anywhere in the menu

### REQ-SEC-033 — بوابة الوحدة تُفحص على كل طلب / Module gate enforced on every request
Pattern    : ubiquitous
Statement  : The system shall verify a user's effective module grant before allowing any request to a screen or action of that module, independent of menu visibility.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-007
Rationale  : POL-SEC-007 (second half) — "not merely hidden... blocked up front, not merely hidden"
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-033 — [REQ-SEC-033]
Given a user whose roles hold no grant for module FIN
When that user directly calls a FIN endpoint or navigates to a FIN screen URL
Then the system denies the request with an authorization error, regardless of how the request was reached

### REQ-SEC-022 — حساب أرقام لوحة التحكم حيًا / Compute dashboard figures live
Pattern    : event
Statement  : When an authorized administrator opens the admin dashboard, the system shall compute every widget figure from live data at that moment.
Traces     : US-SEC-009
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Rationale  : POL-SEC-010
Source     : security-module-plan-en.md §5.1-§5.2
Priority   : MEDIUM
Note       : three of the widget figures API-SEC-022 returns are named but never defined here or in AC-SEC-022 — `recentActivity`'s N, the age at which a PENDING sign-up counts as stalled, and what makes a role `privileged` (SCR-REQ-SEC-007 B1-B5, prd-sec.md and all 104 DBF are silent too). Operative definitions, chosen at implementation to fill that silence and subject to a human's confirmation: N = 10 most recent AuditLogEntry rows; stalled = a SignupRequest still PENDING more than 7 days; privileged role = a role holding at least one action grant whose action code is not the VIEW gateway (RULE-SEC-007 / REQ-SEC-030's own VIEW-vs-other distinction). Whether SEC v1 should state these three, or drop the figures from the contract, is an open P1/P2 decision.
#### AC-SEC-022 — [REQ-SEC-022]
Given the dashboard is opened
When it renders
Then every widget (users overview, failed logins 24h, active sessions, recent activity, roles/permissions summary, onboarding funnel) is computed from current data, never from a stored counter

### REQ-SEC-023 — إخفاء عنصر لوحة التحكم غير الممنوح / Hide an ungranted dashboard widget
Pattern    : unwanted
Statement  : If a user's role does not grant a dashboard widget's underlying permission, then the system shall omit that widget for that user.
Traces     : US-SEC-009
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : POL-SEC-011
Source     : security-module-plan-en.md §5.1
Priority   : MEDIUM
#### AC-SEC-023 — [REQ-SEC-023]
Given an administrator role without the "active sessions" widget's permission
When that role's user opens the dashboard
Then the active-sessions widget does not appear for that user

### REQ-SEC-024 — تسجيل حدث تدقيقي / Append an audit-log entry
Pattern    : event
Statement  : When any security-relevant event occurs (login, failed login, password reset, role or permission change, session termination), the system shall append one immutable audit-log entry recording it.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : POL-SEC-009
Source     : security-module-plan-en.md §5.2-§5.3
Priority   : MEDIUM
#### AC-SEC-024 — [REQ-SEC-024]
Given any of the listed events occurs
When it completes
Then the system appends one AuditLogEntry with the correct eventTypeCode, actor and timestamp, and no existing entry is altered

### REQ-SEC-025 — بحث/تصفية سجل التدقيق / Search and filter the audit log
Pattern    : event
Statement  : When an administrator searches or filters the audit log, the system shall return matching entries without altering any of them.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : usable audit trail
Source     : security-module-plan-en.md §5.3
Priority   : MEDIUM
#### AC-SEC-025 — [REQ-SEC-025]
Given audit entries exist across several event types and dates
When an administrator filters by event type and date range
Then the system returns exactly the matching entries, unmodified

### REQ-SEC-026 — تصدير سجل التدقيق / Export the audit log
Pattern    : event
Statement  : When an administrator exports the audit log, the system shall produce a CSV file of the currently filtered entries.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : plan §5.3 deliverable "CSV export"
Source     : security-module-plan-en.md §5.3
Priority   : LOW
#### AC-SEC-026 — [REQ-SEC-026]
Given a filtered audit-log view
When the administrator exports it
Then the system produces a CSV file containing exactly the filtered entries' fields

### REQ-SEC-027 — عرض الجلسات النشطة / List active sessions
Pattern    : event
Statement  : When an administrator views the active-sessions screen, the system shall list every session that has not been terminated, with its user and last-activity time.
Traces     : US-SEC-011
Entities   : ENT-SEC-010
Rationale  : plan §5.1, §5.3
Source     : security-module-plan-en.md §5.1, §5.3
Priority   : MEDIUM
#### AC-SEC-027 — [REQ-SEC-027]
Given several sessions exist, some terminated and some not
When an administrator opens the active-sessions screen
Then only the non-terminated sessions are listed, each with its user and last-activity time

### REQ-SEC-028 — إنهاء جلسة قسريًا / Force-terminate a session
Pattern    : event
Statement  : When an authorized administrator force-terminates a session, the system shall immediately end that session and require the affected user to sign in again.
Traces     : US-SEC-011
Entities   : ENT-SEC-010
Rationale  : plan §5.1 "force-terminate a session"
Source     : security-module-plan-en.md §5.1
Priority   : MEDIUM
#### AC-SEC-028 — [REQ-SEC-028]
Given an active session
When an authorized administrator force-terminates it
Then the system sets terminatedAt/terminatedBy and the associated token is no longer accepted for any subsequent request

### REQ-SEC-029 — إشعار اختياري عند إعادة التعيين / Optional notification on password reset
Pattern    : optional
Statement  : Where the Notifications integration is enabled, the system shall dispatch a password-reset message through it when a reset token is issued.
Traces     : US-SEC-012
Entities   : ENT-SEC-012
Rationale  : "only on real need", never a hard dependency
Source     : security-module-plan-en.md §8; new project/integration-notifications-fileservice.md §1
Priority   : LOW
#### AC-SEC-029 — [REQ-SEC-029]
Given the Notifications integration is enabled and a reset token is issued
When the token is created
Then the system dispatches one notification with templateCode identifying the password-reset message, and REQ-SEC-006 succeeds unchanged if the integration is disabled or unavailable

### REQ-SEC-030 — اشتراط VIEW لبقية الإجراءات / VIEW required for any other action on a screen
Pattern    : unwanted
Statement  : If a role does not hold the VIEW action grant for a screen, then the system shall deny every other action on that screen for that role.
Traces     : US-SEC-005
Entities   : ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-007; profile.conventions.security_model.gateway_action = VIEW
Source     : profiles/erp.yaml conventions.security_model
Priority   : HIGH
#### AC-SEC-030 — [REQ-SEC-030]
Given a role holds CREATE on a screen but not VIEW on that same screen
When that role's user attempts the CREATE action
Then the system denies it until VIEW is also granted on that screen

### REQ-SEC-034 — قراءة بيانات الاتصال بالمستخدم عبر الوحدات / Cross-module read of a user's contact details
Pattern    : optional
Statement  : Where another module must reach a user out of band, the system shall expose that user's email address, bilingual display names and active state — and nothing else — through a read-only cross-module interface.
Traces     : US-SEC-012
Entities   : ENT-SEC-001
Rationale  : REQ-SEC-029's delivery half needs an address for a bare user id; NOTIF's XM-NOTIF-001 recipient-active check needs the same row's state
Source     : AMENDMENT 2026-09-11 — build-create-service "Exposing this module to others"; execution-state.json api_doc_gaps #9
Priority   : LOW
Note       : The surface is `com.erp.sec.crossmodule.SecUserDirectoryApi`, an injected Spring interface — not an HTTP endpoint: `build-create-service` requires cross-module reads to go through "direct Spring interface injection, not loopback HTTP". See §A8's third table.
#### AC-SEC-034 — [REQ-SEC-034]
Given a consumer module that holds only a user id
When it asks SEC for that user's contact details
Then the system returns the email address, both display names and the active flag, and never the password hash (POL-SEC-004) or any role, grant or session data

### REQ-SEC-035 — قراءة حاملي صلاحية معيّنة عبر الوحدات / Cross-module read of the holders of a permission code
Pattern    : optional
Statement  : Where another module enforces a separation-of-duties rule over its own permission codes, the system shall expose to that module the set of user ids currently holding a given permission code through their roles.
Traces     : US-SEC-007
Entities   : ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-005 is inert in SEC v1 (REQ-SEC-020 Note); the platform's only real conflicting pair is FIN-owned and FIN-enforced (RULE-FIN-015), and FIN needs the holder set to enforce it
Source     : AMENDMENT 2026-09-11 — build-create-service "Exposing this module to others"; execution-state.json api_doc_gaps #8
Priority   : MEDIUM
Note       : Same surface as REQ-SEC-034 (`SecUserDirectoryApi`, QR-SEC-039), not an endpoint. It answers with user ids only — the consumer decides; SEC neither learns nor evaluates the consumer's conflicting pair, so REQ-SEC-020's Note stands unchanged.
#### AC-SEC-035 — [REQ-SEC-035]
Given two permission codes that a consumer module declares as conflicting
When that module asks SEC which users hold each of them
Then the system returns, for each code, the set of user ids computed across every active role assignment, and returns nothing else about those users

## A5 — Business rules

### RULE-SEC-001 — منع منح شاشة دون منح الوحدة / No screen grant without its module grant
Scope      : ENT-SEC-008
Trigger    : on create (screen grant)
Statement  : The system shall prevent a screen grant for a role that does not hold the screen's module grant.
Data source: ENT-SEC-007 (the role's module grants) · ENT-SEC-005 (the screen's owning module)
Message    : ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module"
Traces     : REQ-SEC-013
Source     : security-module-plan-en.md §4.2

### RULE-SEC-002 — منع منح إجراء دون منح الشاشة / No action grant without its screen grant
Scope      : ENT-SEC-009
Trigger    : on create (action grant)
Statement  : The system shall prevent an action grant for a role that does not hold the action's screen grant.
Data source: ENT-SEC-008 (the role's screen grants) · ENT-SEC-006 (the action's owning screen)
Message    : ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen"
Traces     : REQ-SEC-014
Source     : security-module-plan-en.md §4.2

### RULE-SEC-003 — الإلغاء المتسلسل عند سحب منح الوحدة / Cascade revoke on module-grant removal
Scope      : ENT-SEC-007
Trigger    : on delete (module grant)
Statement  : The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked.
Data source: ENT-SEC-008 and ENT-SEC-009 (the grants to remove) · ENT-SEC-005 and ENT-SEC-006 (which screens and actions that module covers)
Message    : ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" · en: "Every screen and action grant under this module for this role will be revoked"
Traces     : REQ-SEC-015
Source     : security-module-plan-en.md §4.2

### RULE-SEC-004 — رفض تسجيل شاشة لوحدة غير مسجّلة / No screen under an unregistered module
Scope      : ENT-SEC-005
Trigger    : on create (screen registration)
Statement  : The system shall reject a screen registration whose module code has no ModuleRegistry row.
Data source: ENT-SEC-004 (the ModuleRegistry row the submitted module code must match)
Message    : ar: "الوحدة غير مسجّلة" · en: "Module is not registered"
Traces     : REQ-SEC-018
Source     : security-module-plan-en.md §4.3

### RULE-SEC-005 — منع تضارب الإجراءات لمستخدم واحد / Prevent conflicting actions on one user
Scope      : ENT-SEC-003, ENT-SEC-009
Trigger    : on create (role assignment or action grant)
Statement  : The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair.
Data source: ENT-SEC-003 (every role the user holds) · ENT-SEC-009 (the action grants those roles carry) · ENT-SEC-006 (the owning module's action declarations, which carry the conflicting pair)
Message    : ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"
Traces     : REQ-SEC-020
Source     : security-module-plan-en.md §4.4; general-accounting-system-plan-en.md §8.2
Note       : DEFERRED in SEC v1 — nothing in SEC v1 declares a conflicting pair, so this `optional` pattern's precondition is never established: there is no ENT, DBF, table, API field or screen element for such a declaration. The guards (RULE-SEC-005, QR-SEC-031) are implemented and inert — the conflicting-counterpart set is empty in v1. The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.

### RULE-SEC-006 — رفض رمز إعادة تعيين منتهٍ أو مُستخدَم / Reject expired or used reset token
Scope      : ENT-SEC-012
Trigger    : on submit (password reset completion)
Statement  : The system shall reject a password-reset submission whose token is expired or already used.
Data source: ENT-SEC-012 (the token's expiry and used state)
Message    : ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired"
Traces     : REQ-SEC-008
Source     : security-module-plan-en.md §3

### RULE-SEC-007 — اشتراط VIEW كبوابة على مستوى الشاشة / VIEW as the screen-level gateway action
Scope      : ENT-SEC-009
Trigger    : on evaluate (any action check) and on create (action grant, informational)
Statement  : The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it.
Data source: ENT-SEC-009 (the role's action grants on that screen) · ENT-SEC-006 (which registered action is VIEW, and the screen it belongs to)
Message    : ar: "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" · en: "The VIEW action must be granted on this screen first"
Traces     : REQ-SEC-030
Source     : profiles/erp.yaml conventions.security_model.gateway_action

## A6 — Lookups

**USER_STATUS** — owned by SEC — used by ENT-SEC-001.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| ACTIVE | نشط | Active |
| DISABLED | معطّل | Disabled |
Source: AUTO (module-registry-sec.md → AUTO-DECISIONS), values fixed by the A7 lifecycle below.

**SIGNUP_STATUS** — owned by SEC — used by ENT-SEC-013.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| APPROVED | مقبول | Approved |
| REJECTED | مرفوض | Rejected |
DEFAULT: a distinct lookup from USER_STATUS because a rejected sign-up never becomes a
User row (no DISABLED-equivalent needed) — Source: this stage, applying profile.conventions.lookups
("no hardcoded enums") to the SignupRequest lifecycle (REQ-SEC-003/004/005) — Override: merge
into USER_STATUS if the client prefers one shared status set.

**AUDIT_EVENT_TYPE** — owned by SEC — used by ENT-SEC-011.eventTypeCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| LOGIN_SUCCESS | دخول ناجح | Login success |
| LOGIN_FAILED | دخول فاشل | Login failed |
| LOGOUT | خروج | Logout |
| PASSWORD_RESET_REQUESTED | طلب إعادة تعيين | Password reset requested |
| PASSWORD_RESET_COMPLETED | إتمام إعادة التعيين | Password reset completed |
| ROLE_ASSIGNED | إسناد دور | Role assigned |
| ROLE_REVOKED | سحب دور | Role revoked |
| MODULE_GRANTED | منح وحدة | Module granted |
| MODULE_REVOKED | سحب منح وحدة | Module revoked |
| SCREEN_GRANTED | منح شاشة | Screen granted |
| SCREEN_REVOKED | سحب منح شاشة | Screen revoked |
| ACTION_GRANTED | منح إجراء | Action granted |
| ACTION_REVOKED | سحب منح إجراء | Action revoked |
| SESSION_TERMINATED | إنهاء جلسة | Session terminated |
Source: module-registry-sec.md → AUTO-DECISIONS; profiles/erp.yaml conventions.lookups.

Consumed lookups: none — SEC is a Tier-0 foundation module.

## A7 — Status lifecycle

**ENT-SEC-001 User.statusCode (USER_STATUS, 3 states, >2 transitions — diagram required)**
```
PENDING --(REQ-SEC-004, admin approves sign-up)--> ACTIVE
ACTIVE  --(REQ-SEC-011, admin deactivates)--------> DISABLED
DISABLED--(REQ-SEC-031, admin reactivates)--------> ACTIVE
```
(A User created directly by an administrator — REQ-SEC-009 — starts at ACTIVE, bypassing PENDING.)

**ENT-SEC-013 SignupRequest.statusCode (SIGNUP_STATUS, 3 states)**
```
PENDING --(REQ-SEC-004, approve)--> APPROVED
PENDING --(REQ-SEC-005, reject)---> REJECTED
```
APPROVED and REJECTED are terminal — no further transition.

All other statuses in this module (grant rows, sessions) are a binary
active/terminated flag, not a multi-state lifecycle — not applicable for a diagram.

**DEFAULT — reset-token expiry window**: not stated by the plan; this stage applies a
DEFAULT of 30 minutes from `requestedAt` to `expiresAt` (industry-standard short-lived
reset window) — Source: domain best practice (no conflicting statement in
security-module-plan-en.md or the knowledge file) — Override: configurable value if the
client states a different window; non-breaking, no ADR required (a pure numeric default
with no story/policy it could contradict).

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
|---|---|---|---|---|
None — SEC is ROOT; it consumes no entity owned by another in-scope module.

| External service | Purpose | Integration kind |
|---|---|---|
| Notifications (ready, external) | password-reset message (REQ-SEC-029) | SOFT / optional, per new project/integration-notifications-fileservice.md §1 |

**AMENDMENT 2026-09-11 — the EXPOSED direction.** The two tables above describe only what SEC
*consumes*; both remain true. SEC additionally exposes one read-only inbound surface. It registers
no entity, table or column, so it carries no XM row here — formal `XM-*` ids for this direction are
assigned by the *consuming* module's own P2, not by SEC.

| Exposed entity | Owner ENT id | Consumer | Read-model | Surface |
|---|---|---|---|---|
| User (contact details only: email + both display names + active) | ENT-SEC-001 | NOTIF — XM-NOTIF-001, and REQ-SEC-029's delivery half | `UserContact` | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-034) |
| Holders of a permission code (spans User → UserRoleAssignment → RoleActionGrant → ActionRegistry) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | FIN — RULE-FIN-015 separation of duties | `List<Long>` (user ids only) | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-035, QR-SEC-039) |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-SEC-001 — تسجيل الدخول / Login
### B1 — Definition
Purpose      : السماح لمستخدم مسجَّل بالدخول الآمن إلى المنصة.
Entities     : ENT-SEC-001, ENT-SEC-010
Operations   : create (session, via REQ-SEC-001/002)
Users        : أي مستخدم مسجَّل (غير مصادَق بعد)
Navigation   : (public) → Login; to: post-login landing / dashboard per the user's own menu
Content shape: flat record (username/password form)
Traces       : REQ-SEC-001, REQ-SEC-002
Composite    : single screen (no search/detail split)
### B2 — Search / list
Not applicable — no search on this screen.
### B3 — Input
Fields: username (ENT-SEC-001.username), password (write-only, not persisted as entered).
Buttons: "Sign in" → REQ-SEC-001/REQ-SEC-002; "Forgot password?" → navigates to SCR-REQ-SEC-003; "Sign up" → navigates to SCR-REQ-SEC-002.
### B4 — Access
Page code: SEC_LOGIN. Public (pre-authentication) — no role/permission gate applies to reaching this screen itself.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| login | POST | /api/v1/sec/auth/login | username, password | session/access token | — | REQ-SEC-001, REQ-SEC-002 |

## SCR-REQ-SEC-002 — التسجيل الذاتي / Sign-up
### B1 — Definition
Purpose      : السماح لمستخدم محتمل بتقديم طلب تسجيل ذاتي.
Entities     : ENT-SEC-013
Operations   : create
Users        : زائر غير مصادَق
Navigation   : (public) → Sign-up; from: SCR-REQ-SEC-001
Content shape: flat record
Traces       : REQ-SEC-003
Composite    : single screen
### B2 — Search / list
Not applicable.
### B3 — Input
Fields: email, fullNameAr, fullNameEn (ENT-SEC-013). Button "Submit" → REQ-SEC-003.
### B4 — Access
Page code: SEC_SIGNUP. Public (pre-authentication).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| submit sign-up | POST | /api/v1/sec/auth/signup | email, fullNameAr, fullNameEn | signup request confirmation | — | REQ-SEC-003 |

## SCR-REQ-SEC-003 — نسيت / إعادة تعيين كلمة المرور / Forgot / reset password
### B1 — Definition
Purpose      : السماح بإعادة تعيين ذاتية آمنة لكلمة المرور.
Entities     : ENT-SEC-012, ENT-SEC-001
Operations   : create (token), update (password)
Users        : أي مستخدم يعرف بريده الإلكتروني المسجَّل
Navigation   : (public) → Forgot password; from: SCR-REQ-SEC-001
Content shape: flat record (two steps: request → token+new password)
Traces       : REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-029
Composite    : Wizard (request step + reset step) = ONE screen requirement
### B2 — Search / list
Not applicable.
### B3 — Input
Step 1: email. Step 2: token (from the reset link), new password, confirm password.
### B4 — Access
Page code: SEC_PWD_RESET. Public (pre-authentication).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| request reset | POST | /api/v1/sec/auth/password-reset/request | email | confirmation (always generic, never reveals whether the email exists) | — | REQ-SEC-006, REQ-SEC-029 |
| complete reset | POST | /api/v1/sec/auth/password-reset/complete | token, newPassword | confirmation | RULE-SEC-006 | REQ-SEC-007, REQ-SEC-008 |

## SCR-REQ-SEC-004 — المستخدمون / Users
### B1 — Definition
Purpose      : إدارة المستخدمين وإسناد الأدوار لهم.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-013
Operations   : search, create, read, update, activate, deactivate; approve/reject a SignupRequest
Users        : مسؤول الأمان
Navigation   : SEC → Authorization → Users; to: user detail (roles tab)
Content shape: flat record (search list + entry form; roles shown as a repeating sub-list on the detail)
Traces       : REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031, REQ-SEC-004, REQ-SEC-005
Composite    : Search + Entry = ONE screen requirement
### B2 — Search / list
Filters: username/email, fullName, statusCode — each corresponds to a result column.
### B3 — Input
Fields: username, email, fullNameAr, fullNameEn, statusCode (ENT-SEC-001); roles multi-select (ENT-SEC-003). Buttons: Activate/Deactivate → REQ-SEC-031/REQ-SEC-011; a separate "Pending sign-ups" tab lists SignupRequest rows with Approve/Reject → REQ-SEC-004/REQ-SEC-005.
### B4 — Access
Page code: SEC_USERS. Actions: VIEW (list/search), CREATE, UPDATE (incl. activate/deactivate/approve/reject), per §7.1 (RULE-SEC-007 gateway).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search users | POST | /api/v1/sec/users/search | filters, paging | Page\<User\> | — | REQ-SEC-009 |
| create user | POST | /api/v1/sec/users | user fields | User | — | REQ-SEC-009 |
| update user | PUT | /api/v1/sec/users/{id} | user fields | User | — | REQ-SEC-009 |
| assign roles | PUT | /api/v1/sec/users/{id}/roles | role ids | User with roles | — | REQ-SEC-010 |
| deactivate user | DELETE | /api/v1/sec/users/{id} | id | confirmation | RULE (session termination, REQ-SEC-011) | REQ-SEC-011 |
| reactivate user | PATCH | /api/v1/sec/users/{id} | status=ACTIVE | User | — | REQ-SEC-031 |
| approve/reject signup | PATCH | /api/v1/sec/signup-requests/{id} | decision | User (on approve) / SignupRequest (on reject) | — | REQ-SEC-004, REQ-SEC-005 |

## SCR-REQ-SEC-005 — الأدوار والصلاحيات (محرر المنح الثلاثي) / Roles & permissions (3-level grant editor)
### B1 — Definition
Purpose      : إدارة الأدوار ومنحها الوحدات ثم الشاشات ثم الإجراءات، بالترتيب الهرمي.
Entities     : ENT-SEC-002, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Operations   : search, create, read, update, deactivate (role); create/delete (module/screen/action grants)
Users        : مسؤول الأمان
Navigation   : SEC → Authorization → Roles & permissions; from: SEC_USERS (role reference)
Content shape: true hierarchy (parent/child) — module → screen → action tree per role
Traces       : REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-020, REQ-SEC-030
Composite    : Master (role list) + Detail (3-level grant tree) = ONE screen requirement
### B2 — Search / list
Filters: role code/name, active flag — correspond to result columns.
### B3 — Input
Role fields: code, nameAr, nameEn, description. Grant tree: check a module (→ REQ-SEC-012), then screens within it (→ REQ-SEC-013/RULE-SEC-001 blocks illegal ones), then actions within a checked screen (→ REQ-SEC-014/RULE-SEC-002, RULE-SEC-007 VIEW gateway); unchecking a module cascades (REQ-SEC-015/RULE-SEC-003). A conflicting-action pair (RULE-SEC-005) blocks the second grant with its message.
### B4 — Access
Page code: SEC_ROLES. Actions: VIEW, CREATE, UPDATE, DELETE (deactivate role), plus grant-tree edits under UPDATE.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search roles | POST | /api/v1/sec/roles/search | filters, paging | Page\<Role\> | — | REQ-SEC-012 |
| create role | POST | /api/v1/sec/roles | role fields | Role | — | REQ-SEC-012 |
| grant module | POST | /api/v1/sec/roles/{id}/modules | moduleId | RoleModuleGrant | — | REQ-SEC-012 |
| revoke module | DELETE | /api/v1/sec/roles/{id}/modules/{moduleId} | — | confirmation | RULE-SEC-003 | REQ-SEC-015 |
| grant screen | POST | /api/v1/sec/roles/{id}/screens | screenId | RoleScreenGrant | RULE-SEC-001 | REQ-SEC-013 |
| grant action | POST | /api/v1/sec/roles/{id}/actions | actionId | RoleActionGrant | RULE-SEC-002, RULE-SEC-005, RULE-SEC-007 | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030 |

## SCR-REQ-SEC-006 — سجل الوحدة/الشاشة/الإجراء / Module / screen / action registry
### B1 — Definition
Purpose      : عرض وإدارة ما سجّلته كل وحدة مستهلكة من وحدات/شاشات/إجراءات كبيانات.
Entities     : ENT-SEC-004, ENT-SEC-005, ENT-SEC-006
Operations   : search, read, deactivate (modules/screens/actions arrive via each module's own registration call, not typed here by hand)
Users        : مسؤول الأمان / مطوّر الوحدة المستهلكة
Navigation   : SEC → Authorization → Module/screen/action registry
Content shape: true hierarchy (module → screen → action)
Traces       : REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019
Composite    : Master-detail tree = ONE screen requirement
### B2 — Search / list
Filters: module code, screen page code — correspond to result columns.
### B3 — Input
Read-mostly: registration itself happens via the registering module's own onboarding call (REQ-SEC-016/017/019); this screen's own edit surface is limited to deactivating a stale row.
### B4 — Access
Page code: SEC_MODULE_REGISTRY. Actions: VIEW, UPDATE (deactivate only).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| register module | POST | /api/v1/sec/registry/modules | code, nameAr, nameEn | ModuleRegistry | — | REQ-SEC-016 |
| register screen | POST | /api/v1/sec/registry/screens | moduleCode, pageCode, nameAr, nameEn | ScreenRegistry | RULE-SEC-004 | REQ-SEC-017, REQ-SEC-018 |
| register action | POST | /api/v1/sec/registry/actions | pageCode, actionCode, nameAr, nameEn | ActionRegistry | — | REQ-SEC-019 |
| search registry | POST | /api/v1/sec/registry/search | filters, paging | Page\<registry rows\> | — | REQ-SEC-016 |

## SCR-REQ-SEC-007 — لوحة تحكم الأمان / Admin dashboard
### B1 — Definition
Purpose      : عرض حالة الأمان العامة للمنصة بشكل حي.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Operations   : read (aggregate figures)
Users        : مسؤول الأمان (لكل عنصر لوحة تحكم بمقتضى صلاحيته الخاصة)
Navigation   : SEC → Monitoring → Dashboard; to: SCR-REQ-SEC-008 (audit), SCR-REQ-SEC-009 (sessions)
Content shape: other (dashboard — a grid of independent widgets, not a record/list)
Traces       : REQ-SEC-022, REQ-SEC-023
Composite    : single screen (widgets are not separate screen requirements)
### B2 — Search / list
Not applicable — aggregate widgets, not a browsable list.
### B3 — Input
Read-only; no data entry.
### B4 — Access
Page code: SEC_DASHBOARD. Action: VIEW; each widget additionally requires the VIEW permission of the screen it summarizes (REQ-SEC-023) — e.g. the active-sessions widget requires SEC_SESSIONS VIEW.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| dashboard summary | GET | /api/v1/sec/dashboard | — | live aggregate figures per widget the caller may see | — | REQ-SEC-022, REQ-SEC-023 |

## SCR-REQ-SEC-008 — سجل التدقيق / Audit log
### B1 — Definition
Purpose      : تصفح وتصدير سجل الأحداث الأمنية غير القابل للتعديل.
Entities     : ENT-SEC-011
Operations   : search, export
Users        : مسؤول الأمان
Navigation   : SEC → Monitoring → Audit log; from: SCR-REQ-SEC-007
Content shape: flat record (append-only list)
Traces       : REQ-SEC-024, REQ-SEC-025, REQ-SEC-026
Composite    : single screen (search list; no entry — audit rows are never hand-created)
### B2 — Search / list
Filters: eventTypeCode, actorUserId, date range — each corresponds to a result column.
### B3 — Input
Not applicable — no create/update; rows are system-appended only (REQ-SEC-024).
### B4 — Access
Page code: SEC_AUDIT_LOG. Action: VIEW (search + export share the same permission — export is not a separate mutation).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search audit log | POST | /api/v1/sec/audit-log/search | filters, paging | Page\<AuditLogEntry\> | — | REQ-SEC-025 |
| export audit log | GET | /api/v1/sec/audit-log/export | filters | CSV file | — | REQ-SEC-026 |

## SCR-REQ-SEC-009 — إدارة الجلسات النشطة / Active sessions management
### B1 — Definition
Purpose      : عرض الجلسات الحية وإنهاء أي منها قسريًا عند الحاجة.
Entities     : ENT-SEC-010
Operations   : search, terminate
Users        : مسؤول الأمان
Navigation   : SEC → Monitoring → Active sessions; from: SCR-REQ-SEC-007
Content shape: flat record (live list)
Traces       : REQ-SEC-027, REQ-SEC-028
Composite    : single screen (search list + a terminate action; no separate entry form)
### B2 — Search / list
Filters: user, IP address — correspond to result columns.
### B3 — Input
Not applicable for create/update; the only mutation is "Terminate" per row → REQ-SEC-028.
### B4 — Access
Page code: SEC_SESSIONS. Actions: VIEW, DELETE (terminate, RULE-SEC-007 gateway applies).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| list active sessions | POST | /api/v1/sec/sessions/search | filters, paging | Page\<ActiveSession\> | — | REQ-SEC-027 |
| terminate session | DELETE | /api/v1/sec/sessions/{id} | id | confirmation | — | REQ-SEC-028 |

## SCR-REQ-SEC-010 — القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu
### B1 — Definition
Purpose      : عرض قائمة تنقّل لكل مستخدم مبنية من منحه الفعلية فقط.
Entities     : ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Operations   : read (rendered on every authenticated page load)
Users        : أي مستخدم مصادَق
Navigation   : rendered globally; not itself a destination
Content shape: other (navigation component, not a record/list)
Traces       : REQ-SEC-021, REQ-SEC-032, REQ-SEC-033
Composite    : single global component = ONE screen requirement (not per-module)
### B2 — Search / list
Not applicable.
### B3 — Input
Not applicable — read-only, derived.
### B4 — Access
No page code of its own (it is not a securable destination); its content is filtered
per-user by the same module/screen grants each target page already enforces (REQ-SEC-033).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| effective menu | GET | /api/v1/sec/menu | — | modules → screens tree, effective grants only | — | REQ-SEC-021, REQ-SEC-032 |

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-SEC-001 | REQ-SEC-001, REQ-SEC-002 | AC-SEC-001, AC-SEC-002 | — | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-001 |
| US-SEC-002 | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005 | AC-SEC-003…005 | — | ENT-SEC-013, ENT-SEC-001 | SCR-REQ-SEC-002, SCR-REQ-SEC-004 |
| US-SEC-003 | REQ-SEC-006, REQ-SEC-007, REQ-SEC-008 | AC-SEC-006…008 | RULE-SEC-006 | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |
| US-SEC-004 | REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031 | AC-SEC-009…011, AC-SEC-031 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-010 | SCR-REQ-SEC-004 |
| US-SEC-005 | REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-030 | AC-SEC-012…015, AC-SEC-030 | RULE-SEC-001, RULE-SEC-002, RULE-SEC-003, RULE-SEC-007 | ENT-SEC-002, ENT-SEC-004…009 | SCR-REQ-SEC-005 |
| US-SEC-006 | REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019 | AC-SEC-016…019 | RULE-SEC-004 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | SCR-REQ-SEC-006 |
| US-SEC-007 | REQ-SEC-020, REQ-SEC-035 | AC-SEC-020, AC-SEC-035 | RULE-SEC-005 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | SCR-REQ-SEC-005 |
| US-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 | AC-SEC-021, AC-SEC-032, AC-SEC-033 | — | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | SCR-REQ-SEC-010 |
| US-SEC-009 | REQ-SEC-022, REQ-SEC-023 | AC-SEC-022, AC-SEC-023 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-007 |
| US-SEC-010 | REQ-SEC-024, REQ-SEC-025, REQ-SEC-026 | AC-SEC-024…026 | — | ENT-SEC-011 | SCR-REQ-SEC-008 |
| US-SEC-011 | REQ-SEC-027, REQ-SEC-028 | AC-SEC-027, AC-SEC-028 | — | ENT-SEC-010 | SCR-REQ-SEC-009 |
| US-SEC-012 | REQ-SEC-029, REQ-SEC-034 | AC-SEC-029, AC-SEC-034 | — | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |

Every story traces to ≥1 REQ; every REQ traces to ≥1 AC; every RULE traces to a REQ;
every SCR-REQ traces to ≥1 REQ (rows above). No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| DEFAULT | Password-reset token expiry = 30 minutes | domain best practice (A7 note) | configurable; non-breaking |
| DEFAULT | USER_STATUS/AUDIT_EVENT_TYPE/SIGNUP_STATUS initial lookup values | profiles/erp.yaml conventions.lookups + lookup-module-plan-en.md §3 | extend the value set as new events/states are needed; non-breaking |
No ADR was raised — no ambiguity reached the breaking/non-breaking fork of §9; every
point was settled by business policies, PRD stories, the registries or the knowledge
source, or documented above as a plain DEFAULT.

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| SEC_LOGIN | Login | public | — | — | — | — |
| SEC_SIGNUP | Sign-up | public | — | — | — | — |
| SEC_PWD_RESET | Forgot/reset password | public | — | — | — | — |
| SEC_USERS | Users | role-granted | role-granted | role-granted (incl. activate/deactivate, approve/reject signup) | — | — |
| SEC_ROLES | Roles & permissions | role-granted | role-granted | role-granted (grant tree edits) | role-granted (deactivate role) | — |
| SEC_MODULE_REGISTRY | Module/screen/action registry | role-granted | (via registering module's own call) | role-granted (deactivate row) | — | — |
| SEC_DASHBOARD | Admin dashboard | role-granted (+ per-widget VIEW of its source screen) | — | — | — | — |
| SEC_AUDIT_LOG | Audit log | role-granted | — | — | — | export (shares VIEW) |
| SEC_SESSIONS | Active sessions | role-granted | — | — | role-granted (terminate) | — |
| (menu) | Dynamic menu | derived from the above — no page code of its own | — | — | — | — |
Every action beyond VIEW additionally requires VIEW on the same screen (RULE-SEC-007).
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: backend-execution-plan>>>
# BACKEND EXECUTION PLAN — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Dialect : postgresql16
Framework : spring-boot-java (profile.stack.backend.framework)
Inputs : srs (v1, PRD-approved), db-script (v1), registry-srs (v1), registry-db (v1)
Open ADRs : 1 — erp/decisions/SEC/ (ADR-SEC-001, carried from P2; no new ADR this stage)
══════════════════════════════════════════════════════════════════

## PRE-GENERATION EXTRACTION — SEC v1 (working set; not part of the plan proper)

```
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      13 — ENT-SEC-001..013, all kind=security (SRS A3)
REQUIREMENTS  35 — REQ-SEC-001..035 (SRS A4), each with ≥1 AC-SEC-* (034/035 added by the 2026-09-11 amendment)
RULES         7 — RULE-SEC-001..007 (SRS A5), full text reused verbatim below
SCREENS       10 — SCR-REQ-SEC-001..010 (SRS Part B), each composite per profile.conventions.composite_screen
PERMISSIONS   SEC_PAGES page codes + PERM_<PAGE_CODE>_<ACTION>, gateway VIEW (SRS §7.1 / Access summary)
LOOKUPS       USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (SRS A6) — CHECK-constrained in v1 per ADR-SEC-001
BUSINESS CODE none — no SEC entity has a platform-numbered business code (SRS §3.3 NUMBERING test: all "No")
── FROM db-script ────────────────────────────────────────────────────────
TABLES        13 tables, exact names SEC_USER … SEC_SIGNUP_REQUEST (db-script-sec.md §3 BLOCK 2/3)
PK GENERATION every table: `GENERATED ALWAYS AS IDENTITY` (postgresql16 identity clause, db-script §3 header)
COLUMNS       104 DBF-SEC-001..104, exact names/types/null/default per db-script §1 and §6
CONSTRAINTS   PK_*, UQ_*, CHK_*, FK_* per db-script §3 BLOCK 5; INDEXES IDX_* per BLOCK 7
XM            none CONSUMED — SEC is ROOT (db-script §2 XM REGISTER: empty in that direction); 1 EXPOSED crossmodule surface, 0 XM rows (db-script §2 amendment 2026-09-11)
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES CONSUMED   none (SEC is ROOT)
EXISTING LOOKUP KEYS        USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (registry-db-sec.md — reused, not recreated)
ID RANGES already used      API: none yet (this is the first use) · QR: none yet (this is the first use)
──────────────────────────────────────────────────────────────────────────
No row required §2A.3 extraction-failure handling — every field bound cleanly.
```

## EXECUTION PLAN INDEX — SEC v1 — backend-execution-plan-sec.md
Profile: erp · dialect: postgresql16 · framework: spring-boot-java
Open ADRs: 1 — decisions/SEC/ (ADR-SEC-001, non-breaking, carried from P2)

**ENTITY REGISTRY**
| ENT | Name | Table | Business code | Operations |
|---|---|---|---|---|
| ENT-SEC-001 | User | SEC_USER | none | create, read, search, update, activate, deactivate |
| ENT-SEC-002 | Role | SEC_ROLE | none | create, read, search, update, deactivate |
| ENT-SEC-003 | UserRoleAssignment | SEC_USER_ROLE | none | create, read (list), delete |
| ENT-SEC-004 | ModuleRegistry | SEC_MODULE_REG | none | create, read, search, deactivate |
| ENT-SEC-005 | ScreenRegistry | SEC_SCREEN_REG | none | create, read, search, deactivate |
| ENT-SEC-006 | ActionRegistry | SEC_ACTION_REG | none | create, read, search, deactivate |
| ENT-SEC-007 | RoleModuleGrant | SEC_ROLE_MODULE_GRANT | none | create, read (list), delete |
| ENT-SEC-008 | RoleScreenGrant | SEC_ROLE_SCREEN_GRANT | none | create, read (list), delete |
| ENT-SEC-009 | RoleActionGrant | SEC_ROLE_ACTION_GRANT | none | create, read (list), delete |
| ENT-SEC-010 | ActiveSession | SEC_ACTIVE_SESSION | none | create (system), read (list), terminate |
| ENT-SEC-011 | AuditLogEntry | SEC_AUDIT_LOG | none | create (system, append-only), read, search |
| ENT-SEC-012 | PasswordResetToken | SEC_PWD_RESET_TOKEN | none | create (system), read (validate), invalidate |
| ENT-SEC-013 | SignupRequest | SEC_SIGNUP_REQUEST | none | create, read, search, approve, reject |

**FIELD REGISTRY** (full binding detail: DB Alignment Manifest §below)
| DBF | Property | Read-only | ENT |
|---|---|---|---|
| DBF-SEC-001 | userPk | Yes | ENT-SEC-001 |
| DBF-SEC-002 | username | No | ENT-SEC-001 |
| DBF-SEC-003 | email | No | ENT-SEC-001 |
| DBF-SEC-004 | passwordHash | Yes | ENT-SEC-001 |
| DBF-SEC-005 | fullNameAr | No | ENT-SEC-001 |
| DBF-SEC-006 | fullNameEn | No | ENT-SEC-001 |
| DBF-SEC-007 | statusCode | Yes | ENT-SEC-001 |
| DBF-SEC-008 | lastLoginAt | Yes | ENT-SEC-001 |
| DBF-SEC-009 | isActiveFl | Yes | ENT-SEC-001 |
| DBF-SEC-010 | createdBy | Yes | ENT-SEC-001 |
| DBF-SEC-011 | createdAt | Yes | ENT-SEC-001 |
| DBF-SEC-012 | updatedBy | Yes | ENT-SEC-001 |
| DBF-SEC-013 | updatedAt | Yes | ENT-SEC-001 |
| DBF-SEC-014 | rolePk | Yes | ENT-SEC-002 |
| DBF-SEC-015 | code | No | ENT-SEC-002 |
| DBF-SEC-016 | nameAr | No | ENT-SEC-002 |
| DBF-SEC-017 | nameEn | No | ENT-SEC-002 |
| DBF-SEC-018 | descriptionAr | No | ENT-SEC-002 |
| DBF-SEC-019 | descriptionEn | No | ENT-SEC-002 |
| DBF-SEC-020 | isActiveFl | Yes | ENT-SEC-002 |
| DBF-SEC-021 | createdBy | Yes | ENT-SEC-002 |
| DBF-SEC-022 | createdAt | Yes | ENT-SEC-002 |
| DBF-SEC-023 | updatedBy | Yes | ENT-SEC-002 |
| DBF-SEC-024 | updatedAt | Yes | ENT-SEC-002 |
| DBF-SEC-025 | userRolePk | Yes | ENT-SEC-003 |
| DBF-SEC-026 | userId | No | ENT-SEC-003 |
| DBF-SEC-027 | roleId | No | ENT-SEC-003 |
| DBF-SEC-028 | assignedBy | Yes | ENT-SEC-003 |
| DBF-SEC-029 | assignedAt | Yes | ENT-SEC-003 |
| DBF-SEC-030 | moduleRegPk | Yes | ENT-SEC-004 |
| DBF-SEC-031 | code | No | ENT-SEC-004 |
| DBF-SEC-032 | nameAr | No | ENT-SEC-004 |
| DBF-SEC-033 | nameEn | No | ENT-SEC-004 |
| DBF-SEC-034 | isActiveFl | Yes | ENT-SEC-004 |
| DBF-SEC-035 | createdBy | Yes | ENT-SEC-004 |
| DBF-SEC-036 | createdAt | Yes | ENT-SEC-004 |
| DBF-SEC-037 | updatedBy | Yes | ENT-SEC-004 |
| DBF-SEC-038 | updatedAt | Yes | ENT-SEC-004 |
| DBF-SEC-039 | screenRegPk | Yes | ENT-SEC-005 |
| DBF-SEC-040 | pageCode | No | ENT-SEC-005 |
| DBF-SEC-041 | moduleId | No | ENT-SEC-005 |
| DBF-SEC-042 | nameAr | No | ENT-SEC-005 |
| DBF-SEC-043 | nameEn | No | ENT-SEC-005 |
| DBF-SEC-044 | isActiveFl | Yes | ENT-SEC-005 |
| DBF-SEC-045 | createdBy | Yes | ENT-SEC-005 |
| DBF-SEC-046 | createdAt | Yes | ENT-SEC-005 |
| DBF-SEC-047 | updatedBy | Yes | ENT-SEC-005 |
| DBF-SEC-048 | updatedAt | Yes | ENT-SEC-005 |
| DBF-SEC-049 | actionRegPk | Yes | ENT-SEC-006 |
| DBF-SEC-050 | permissionCode | Yes | ENT-SEC-006 |
| DBF-SEC-051 | screenId | No | ENT-SEC-006 |
| DBF-SEC-052 | actionCode | No | ENT-SEC-006 |
| DBF-SEC-053 | nameAr | No | ENT-SEC-006 |
| DBF-SEC-054 | nameEn | No | ENT-SEC-006 |
| DBF-SEC-055 | isActiveFl | Yes | ENT-SEC-006 |
| DBF-SEC-056 | createdBy | Yes | ENT-SEC-006 |
| DBF-SEC-057 | createdAt | Yes | ENT-SEC-006 |
| DBF-SEC-058 | updatedBy | Yes | ENT-SEC-006 |
| DBF-SEC-059 | updatedAt | Yes | ENT-SEC-006 |
| DBF-SEC-060 | roleModuleGrantPk | Yes | ENT-SEC-007 |
| DBF-SEC-061 | roleId | No | ENT-SEC-007 |
| DBF-SEC-062 | moduleId | No | ENT-SEC-007 |
| DBF-SEC-063 | grantedBy | Yes | ENT-SEC-007 |
| DBF-SEC-064 | grantedAt | Yes | ENT-SEC-007 |
| DBF-SEC-065 | roleScreenGrantPk | Yes | ENT-SEC-008 |
| DBF-SEC-066 | roleId | No | ENT-SEC-008 |
| DBF-SEC-067 | screenId | No | ENT-SEC-008 |
| DBF-SEC-068 | grantedBy | Yes | ENT-SEC-008 |
| DBF-SEC-069 | grantedAt | Yes | ENT-SEC-008 |
| DBF-SEC-070 | roleActionGrantPk | Yes | ENT-SEC-009 |
| DBF-SEC-071 | roleId | No | ENT-SEC-009 |
| DBF-SEC-072 | actionId | No | ENT-SEC-009 |
| DBF-SEC-073 | grantedBy | Yes | ENT-SEC-009 |
| DBF-SEC-074 | grantedAt | Yes | ENT-SEC-009 |
| DBF-SEC-075 | activeSessionPk | Yes | ENT-SEC-010 |
| DBF-SEC-076 | userId | Yes | ENT-SEC-010 |
| DBF-SEC-077 | tokenRef | Yes | ENT-SEC-010 |
| DBF-SEC-078 | startedAt | Yes | ENT-SEC-010 |
| DBF-SEC-079 | lastActivityAt | Yes | ENT-SEC-010 |
| DBF-SEC-080 | ipAddress | Yes | ENT-SEC-010 |
| DBF-SEC-081 | terminatedAt | Yes | ENT-SEC-010 |
| DBF-SEC-082 | terminatedBy | Yes | ENT-SEC-010 |
| DBF-SEC-083 | auditLogPk | Yes | ENT-SEC-011 |
| DBF-SEC-084 | eventTypeCode | Yes | ENT-SEC-011 |
| DBF-SEC-085 | actorUserId | Yes | ENT-SEC-011 |
| DBF-SEC-086 | occurredAt | Yes | ENT-SEC-011 |
| DBF-SEC-087 | targetRef | Yes | ENT-SEC-011 |
| DBF-SEC-088 | detailsAr | Yes | ENT-SEC-011 |
| DBF-SEC-089 | detailsEn | Yes | ENT-SEC-011 |
| DBF-SEC-090 | ipAddress | Yes | ENT-SEC-011 |
| DBF-SEC-091 | pwdResetTokenPk | Yes | ENT-SEC-012 |
| DBF-SEC-092 | userId | Yes | ENT-SEC-012 |
| DBF-SEC-093 | tokenHash | Yes | ENT-SEC-012 |
| DBF-SEC-094 | requestedAt | Yes | ENT-SEC-012 |
| DBF-SEC-095 | expiresAt | Yes | ENT-SEC-012 |
| DBF-SEC-096 | usedAt | Yes | ENT-SEC-012 |
| DBF-SEC-097 | signupRequestPk | Yes | ENT-SEC-013 |
| DBF-SEC-098 | email | No | ENT-SEC-013 |
| DBF-SEC-099 | fullNameAr | No | ENT-SEC-013 |
| DBF-SEC-100 | fullNameEn | No | ENT-SEC-013 |
| DBF-SEC-101 | submittedAt | Yes | ENT-SEC-013 |
| DBF-SEC-102 | statusCode | Yes | ENT-SEC-013 |
| DBF-SEC-103 | reviewedBy | Yes | ENT-SEC-013 |
| DBF-SEC-104 | reviewedAt | Yes | ENT-SEC-013 |

**API REGISTRY** (full blocks: §Phase 3 SVC-API below)
| API | Operation | Verb | Path | Traces (REQ, DBF) |
|---|---|---|---|---|
| API-SEC-001 | login | POST | /api/v1/sec/auth/login | REQ-SEC-001,REQ-SEC-002 · DBF-SEC-002,DBF-SEC-004,DBF-SEC-007,DBF-SEC-075..078 |
| API-SEC-002 | submit sign-up | POST | /api/v1/sec/auth/signup | REQ-SEC-003 · DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 |
| API-SEC-003 | request password reset | POST | /api/v1/sec/auth/password-reset/request | REQ-SEC-006,REQ-SEC-029 · DBF-SEC-092,DBF-SEC-093,DBF-SEC-094,DBF-SEC-095 |
| API-SEC-004 | complete password reset | POST | /api/v1/sec/auth/password-reset/complete | REQ-SEC-007,REQ-SEC-008 · DBF-SEC-093,DBF-SEC-095,DBF-SEC-096,DBF-SEC-004 |
| API-SEC-005 | search users | POST | /api/v1/sec/users/search | REQ-SEC-009 · DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 |
| API-SEC-006 | create user | POST | /api/v1/sec/users | REQ-SEC-009 · DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 |
| API-SEC-007 | update user | PUT | /api/v1/sec/users/{id} | REQ-SEC-009 · DBF-SEC-003,DBF-SEC-005,DBF-SEC-006 |
| API-SEC-008 | assign roles to user | PUT | /api/v1/sec/users/{id}/roles | REQ-SEC-010 · DBF-SEC-026,DBF-SEC-027,DBF-SEC-028,DBF-SEC-029 |
| API-SEC-009 | deactivate user | DELETE | /api/v1/sec/users/{id} | REQ-SEC-011 · DBF-SEC-007,DBF-SEC-009,DBF-SEC-081,DBF-SEC-082 |
| API-SEC-010 | reactivate user | PATCH | /api/v1/sec/users/{id} | REQ-SEC-031 · DBF-SEC-007,DBF-SEC-009 |
| API-SEC-011 | approve/reject signup | PATCH | /api/v1/sec/signup-requests/{id} | REQ-SEC-004,REQ-SEC-005 · DBF-SEC-102,DBF-SEC-103,DBF-SEC-104,DBF-SEC-001 |
| API-SEC-012 | search roles | POST | /api/v1/sec/roles/search | REQ-SEC-012 · DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-020 |
| API-SEC-013 | create role | POST | /api/v1/sec/roles | REQ-SEC-012 · DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019 |
| API-SEC-014 | grant module to role | POST | /api/v1/sec/roles/{id}/modules | REQ-SEC-012 · DBF-SEC-061,DBF-SEC-062,DBF-SEC-063,DBF-SEC-064 |
| API-SEC-015 | revoke module grant | DELETE | /api/v1/sec/roles/{id}/modules/{moduleId} | REQ-SEC-015 · DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-071 |
| API-SEC-016 | grant screen to role | POST | /api/v1/sec/roles/{id}/screens | REQ-SEC-013 · DBF-SEC-066,DBF-SEC-067,DBF-SEC-068,DBF-SEC-069 |
| API-SEC-017 | grant action to role | POST | /api/v1/sec/roles/{id}/actions | REQ-SEC-014,REQ-SEC-020,REQ-SEC-030 · DBF-SEC-071,DBF-SEC-072,DBF-SEC-073,DBF-SEC-074 |
| API-SEC-018 | register module | POST | /api/v1/sec/registry/modules | REQ-SEC-016 · DBF-SEC-031,DBF-SEC-032,DBF-SEC-033 |
| API-SEC-019 | register screen | POST | /api/v1/sec/registry/screens | REQ-SEC-017,REQ-SEC-018 · DBF-SEC-040,DBF-SEC-041,DBF-SEC-042,DBF-SEC-043 |
| API-SEC-020 | register action | POST | /api/v1/sec/registry/actions | REQ-SEC-019 · DBF-SEC-050,DBF-SEC-051,DBF-SEC-052,DBF-SEC-053,DBF-SEC-054 |
| API-SEC-021 | search registry | POST | /api/v1/sec/registry/search | REQ-SEC-016 · DBF-SEC-031,DBF-SEC-040,DBF-SEC-050 |
| API-SEC-022 | dashboard summary | GET | /api/v1/sec/dashboard | REQ-SEC-022,REQ-SEC-023 · DBF-SEC-007,DBF-SEC-076,DBF-SEC-081,DBF-SEC-084,DBF-SEC-086 |
| API-SEC-023 | search audit log | POST | /api/v1/sec/audit-log/search | REQ-SEC-025 · DBF-SEC-084,DBF-SEC-085,DBF-SEC-086 |
| API-SEC-024 | export audit log | GET | /api/v1/sec/audit-log/export | REQ-SEC-026 · DBF-SEC-084,DBF-SEC-085,DBF-SEC-086,DBF-SEC-087,DBF-SEC-088,DBF-SEC-089 |
| API-SEC-025 | list active sessions | POST | /api/v1/sec/sessions/search | REQ-SEC-027 · DBF-SEC-076,DBF-SEC-079,DBF-SEC-081 |
| API-SEC-026 | terminate session | DELETE | /api/v1/sec/sessions/{id} | REQ-SEC-028 · DBF-SEC-081,DBF-SEC-082 |
| API-SEC-027 | effective menu | GET | /api/v1/sec/menu | REQ-SEC-021,REQ-SEC-032,REQ-SEC-033 · DBF-SEC-031,DBF-SEC-040,DBF-SEC-061,DBF-SEC-066 |

**RULE REGISTRY**
| RULE | Name | Scope (ENT) | Message ar/en ✓ |
|---|---|---|---|
| RULE-SEC-001 | No screen grant without its module grant | ENT-SEC-008 | ✓ |
| RULE-SEC-002 | No action grant without its screen grant | ENT-SEC-009 | ✓ |
| RULE-SEC-003 | Cascade revoke on module-grant removal | ENT-SEC-007 | ✓ |
| RULE-SEC-004 | No screen under an unregistered module | ENT-SEC-005 | ✓ |
| RULE-SEC-005 | Prevent conflicting actions on one user | ENT-SEC-003, ENT-SEC-009 | ✓ |
| RULE-SEC-006 | Reject expired or used reset token | ENT-SEC-012 | ✓ |
| RULE-SEC-007 | VIEW as the screen-level gateway action | ENT-SEC-009 | ✓ |

**SCREEN REGISTRY**
| Screen | Type | ENT | Permission names |
|---|---|---|---|
| SEC_LOGIN | public | ENT-SEC-001 | (none — public) |
| SEC_SIGNUP | public | ENT-SEC-013 | (none — public) |
| SEC_PWD_RESET | public | ENT-SEC-012 | (none — public) |
| SEC_USERS | secured | ENT-SEC-001 | PERM_SEC_USERS_VIEW, PERM_SEC_USERS_CREATE, PERM_SEC_USERS_UPDATE |
| SEC_ROLES | secured | ENT-SEC-002 | PERM_SEC_ROLES_VIEW, PERM_SEC_ROLES_CREATE, PERM_SEC_ROLES_UPDATE, PERM_SEC_ROLES_DELETE |
| SEC_MODULE_REGISTRY | secured | ENT-SEC-004 | PERM_SEC_MODULE_REGISTRY_VIEW, PERM_SEC_MODULE_REGISTRY_UPDATE |
| SEC_DASHBOARD | secured | (aggregate) | PERM_SEC_DASHBOARD_VIEW (+ per-widget source-screen VIEW) |
| SEC_AUDIT_LOG | secured | ENT-SEC-011 | PERM_SEC_AUDIT_LOG_VIEW |
| SEC_SESSIONS | secured | ENT-SEC-010 | PERM_SEC_SESSIONS_VIEW, PERM_SEC_SESSIONS_DELETE |
| (menu) | derived | — | none of its own — filtered per target page's own permissions |

**LOOKUP REGISTRY**
| Key | Used in field | ENT |
|---|---|---|
| USER_STATUS | statusCode | ENT-SEC-001 |
| SIGNUP_STATUS | statusCode | ENT-SEC-013 |
| AUDIT_EVENT_TYPE | eventTypeCode | ENT-SEC-011 |

**QRC SUMMARY** (agent reference only — full catalog below)
39 QR ids, QR-SEC-001..039 — see §Query Reference Catalog (QR-SEC-039 added 2026-09-11 with REQ-SEC-035).

**DB ALIGNMENT** — see manifest below — ALIGNED ✓ / issues: 0
**XM STATUS** — 0 CONSUMED (SEC is ROOT) · 1 EXPOSED crossmodule surface
(`com.erp.sec.crossmodule.SecUserDirectoryApi`, REQ-SEC-034/035) carrying 0 XM rows — it
registers no entity, table or column, and ids for that direction are the consuming module's
own P2 to assign, not SEC's
**SECURITY** — 7 secured screens × role-driven grants (no fixed role count — data-driven per RBAC)

## DB Alignment Manifest — SEC v1
DBF-* │ ENT-* │ plan property │ plan type │ XM-* │ status — sourced by lookup from db-script-sec.md §1; not reproduced here beyond the four bound columns.

All 104 rows: **status ✓ (aligned), XM — (none)** for every row; property/type below (Java types, `profile.stack.backend.framework` = spring-boot-java; TIMESTAMPTZ→Instant, VARCHAR/TEXT→String, BOOLEAN→Boolean, BIGINT identity→Long).

| DBF | ENT | property | type |
|---|---|---|---|
| DBF-SEC-001 | ENT-SEC-001 | userPk | Long |
| DBF-SEC-002 | ENT-SEC-001 | username | String |
| DBF-SEC-003 | ENT-SEC-001 | email | String |
| DBF-SEC-004 | ENT-SEC-001 | passwordHash | String |
| DBF-SEC-005 | ENT-SEC-001 | fullNameAr | String |
| DBF-SEC-006 | ENT-SEC-001 | fullNameEn | String |
| DBF-SEC-007 | ENT-SEC-001 | statusCode | String |
| DBF-SEC-008 | ENT-SEC-001 | lastLoginAt | Instant |
| DBF-SEC-009 | ENT-SEC-001 | isActiveFl | Boolean |
| DBF-SEC-010 | ENT-SEC-001 | createdBy | String |
| DBF-SEC-011 | ENT-SEC-001 | createdAt | Instant |
| DBF-SEC-012 | ENT-SEC-001 | updatedBy | String |
| DBF-SEC-013 | ENT-SEC-001 | updatedAt | Instant |
| DBF-SEC-014 | ENT-SEC-002 | rolePk | Long |
| DBF-SEC-015 | ENT-SEC-002 | code | String |
| DBF-SEC-016 | ENT-SEC-002 | nameAr | String |
| DBF-SEC-017 | ENT-SEC-002 | nameEn | String |
| DBF-SEC-018 | ENT-SEC-002 | descriptionAr | String |
| DBF-SEC-019 | ENT-SEC-002 | descriptionEn | String |
| DBF-SEC-020 | ENT-SEC-002 | isActiveFl | Boolean |
| DBF-SEC-021 | ENT-SEC-002 | createdBy | String |
| DBF-SEC-022 | ENT-SEC-002 | createdAt | Instant |
| DBF-SEC-023 | ENT-SEC-002 | updatedBy | String |
| DBF-SEC-024 | ENT-SEC-002 | updatedAt | Instant |
| DBF-SEC-025 | ENT-SEC-003 | userRolePk | Long |
| DBF-SEC-026 | ENT-SEC-003 | userId | Long |
| DBF-SEC-027 | ENT-SEC-003 | roleId | Long |
| DBF-SEC-028 | ENT-SEC-003 | assignedBy | String |
| DBF-SEC-029 | ENT-SEC-003 | assignedAt | Instant |
| DBF-SEC-030 | ENT-SEC-004 | moduleRegPk | Long |
| DBF-SEC-031 | ENT-SEC-004 | code | String |
| DBF-SEC-032 | ENT-SEC-004 | nameAr | String |
| DBF-SEC-033 | ENT-SEC-004 | nameEn | String |
| DBF-SEC-034 | ENT-SEC-004 | isActiveFl | Boolean |
| DBF-SEC-035 | ENT-SEC-004 | createdBy | String |
| DBF-SEC-036 | ENT-SEC-004 | createdAt | Instant |
| DBF-SEC-037 | ENT-SEC-004 | updatedBy | String |
| DBF-SEC-038 | ENT-SEC-004 | updatedAt | Instant |
| DBF-SEC-039 | ENT-SEC-005 | screenRegPk | Long |
| DBF-SEC-040 | ENT-SEC-005 | pageCode | String |
| DBF-SEC-041 | ENT-SEC-005 | moduleId | Long |
| DBF-SEC-042 | ENT-SEC-005 | nameAr | String |
| DBF-SEC-043 | ENT-SEC-005 | nameEn | String |
| DBF-SEC-044 | ENT-SEC-005 | isActiveFl | Boolean |
| DBF-SEC-045 | ENT-SEC-005 | createdBy | String |
| DBF-SEC-046 | ENT-SEC-005 | createdAt | Instant |
| DBF-SEC-047 | ENT-SEC-005 | updatedBy | String |
| DBF-SEC-048 | ENT-SEC-005 | updatedAt | Instant |
| DBF-SEC-049 | ENT-SEC-006 | actionRegPk | Long |
| DBF-SEC-050 | ENT-SEC-006 | permissionCode | String |
| DBF-SEC-051 | ENT-SEC-006 | screenId | Long |
| DBF-SEC-052 | ENT-SEC-006 | actionCode | String |
| DBF-SEC-053 | ENT-SEC-006 | nameAr | String |
| DBF-SEC-054 | ENT-SEC-006 | nameEn | String |
| DBF-SEC-055 | ENT-SEC-006 | isActiveFl | Boolean |
| DBF-SEC-056 | ENT-SEC-006 | createdBy | String |
| DBF-SEC-057 | ENT-SEC-006 | createdAt | Instant |
| DBF-SEC-058 | ENT-SEC-006 | updatedBy | String |
| DBF-SEC-059 | ENT-SEC-006 | updatedAt | Instant |
| DBF-SEC-060 | ENT-SEC-007 | roleModuleGrantPk | Long |
| DBF-SEC-061 | ENT-SEC-007 | roleId | Long |
| DBF-SEC-062 | ENT-SEC-007 | moduleId | Long |
| DBF-SEC-063 | ENT-SEC-007 | grantedBy | String |
| DBF-SEC-064 | ENT-SEC-007 | grantedAt | Instant |
| DBF-SEC-065 | ENT-SEC-008 | roleScreenGrantPk | Long |
| DBF-SEC-066 | ENT-SEC-008 | roleId | Long |
| DBF-SEC-067 | ENT-SEC-008 | screenId | Long |
| DBF-SEC-068 | ENT-SEC-008 | grantedBy | String |
| DBF-SEC-069 | ENT-SEC-008 | grantedAt | Instant |
| DBF-SEC-070 | ENT-SEC-009 | roleActionGrantPk | Long |
| DBF-SEC-071 | ENT-SEC-009 | roleId | Long |
| DBF-SEC-072 | ENT-SEC-009 | actionId | Long |
| DBF-SEC-073 | ENT-SEC-009 | grantedBy | String |
| DBF-SEC-074 | ENT-SEC-009 | grantedAt | Instant |
| DBF-SEC-075 | ENT-SEC-010 | activeSessionPk | Long |
| DBF-SEC-076 | ENT-SEC-010 | userId | Long |
| DBF-SEC-077 | ENT-SEC-010 | tokenRef | String |
| DBF-SEC-078 | ENT-SEC-010 | startedAt | Instant |
| DBF-SEC-079 | ENT-SEC-010 | lastActivityAt | Instant |
| DBF-SEC-080 | ENT-SEC-010 | ipAddress | String |
| DBF-SEC-081 | ENT-SEC-010 | terminatedAt | Instant |
| DBF-SEC-082 | ENT-SEC-010 | terminatedBy | String |
| DBF-SEC-083 | ENT-SEC-011 | auditLogPk | Long |
| DBF-SEC-084 | ENT-SEC-011 | eventTypeCode | String |
| DBF-SEC-085 | ENT-SEC-011 | actorUserId | Long |
| DBF-SEC-086 | ENT-SEC-011 | occurredAt | Instant |
| DBF-SEC-087 | ENT-SEC-011 | targetRef | String |
| DBF-SEC-088 | ENT-SEC-011 | detailsAr | String |
| DBF-SEC-089 | ENT-SEC-011 | detailsEn | String |
| DBF-SEC-090 | ENT-SEC-011 | ipAddress | String |
| DBF-SEC-091 | ENT-SEC-012 | pwdResetTokenPk | Long |
| DBF-SEC-092 | ENT-SEC-012 | userId | Long |
| DBF-SEC-093 | ENT-SEC-012 | tokenHash | String |
| DBF-SEC-094 | ENT-SEC-012 | requestedAt | Instant |
| DBF-SEC-095 | ENT-SEC-012 | expiresAt | Instant |
| DBF-SEC-096 | ENT-SEC-012 | usedAt | Instant |
| DBF-SEC-097 | ENT-SEC-013 | signupRequestPk | Long |
| DBF-SEC-098 | ENT-SEC-013 | email | String |
| DBF-SEC-099 | ENT-SEC-013 | fullNameAr | String |
| DBF-SEC-100 | ENT-SEC-013 | fullNameEn | String |
| DBF-SEC-101 | ENT-SEC-013 | submittedAt | Instant |
| DBF-SEC-102 | ENT-SEC-013 | statusCode | String |
| DBF-SEC-103 | ENT-SEC-013 | reviewedBy | String |
| DBF-SEC-104 | ENT-SEC-013 | reviewedAt | Instant |

## Query Reference Catalog (QR-SEC-*)

> Logical specification only — never executable code; the implementer rewrites every
> entry with real entity classes and the project's query strategy (§5 warning, engine).

| QR | Operation | Phase | API | Entity | Kind | Intent |
|---|---|---|---|---|---|---|
| QR-SEC-001 | FIND_ONE | SVC-API | API-SEC-001 | ENT-SEC-001 | FIND_ONE by username | resolve login identity |
| QR-SEC-002 | SAVE | SVC-API | API-SEC-002 | ENT-SEC-013 | SAVE | create pending sign-up |
| QR-SEC-003 | SAVE | SVC-API | API-SEC-003 | ENT-SEC-012 | SAVE | issue reset token |
| QR-SEC-004 | UPDATE | SVC-API | API-SEC-004 | ENT-SEC-001, ENT-SEC-012 | UPDATE | set new password hash + mark token used |
| QR-SEC-005 | FIND_BY_CRITERIA | SVC-API | API-SEC-005 | ENT-SEC-001 | FIND_BY_CRITERIA | search users |
| QR-SEC-006 | SAVE | SVC-API | API-SEC-006 | ENT-SEC-001 | SAVE | create user |
| QR-SEC-007 | UPDATE | SVC-API | API-SEC-007 | ENT-SEC-001 | UPDATE | update user profile fields |
| QR-SEC-008 | SAVE | SVC-API | API-SEC-008 | ENT-SEC-003 | SAVE (batch) | assign roles to user |
| QR-SEC-009 | UPDATE | SVC-API | API-SEC-009 | ENT-SEC-001, ENT-SEC-010 | UPDATE | deactivate user + terminate its sessions |
| QR-SEC-010 | UPDATE | SVC-API | API-SEC-010 | ENT-SEC-001 | UPDATE | reactivate user |
| QR-SEC-011 | UPDATE | SVC-API | API-SEC-011 | ENT-SEC-013, ENT-SEC-001 | UPDATE / SAVE | approve (creates user) / reject sign-up |
| QR-SEC-012 | FIND_BY_CRITERIA | SVC-API | API-SEC-012 | ENT-SEC-002 | FIND_BY_CRITERIA | search roles |
| QR-SEC-013 | SAVE | SVC-API | API-SEC-013 | ENT-SEC-002 | SAVE | create role |
| QR-SEC-014 | SAVE | SVC-API | API-SEC-014 | ENT-SEC-007 | SAVE | grant module |
| QR-SEC-015 | DELETE | SVC-API | API-SEC-015 | ENT-SEC-007 | DELETE | revoke module grant |
| QR-SEC-016 | SAVE | SVC-API | API-SEC-016 | ENT-SEC-008 | SAVE | grant screen |
| QR-SEC-017 | SAVE | SVC-API | API-SEC-017 | ENT-SEC-009 | SAVE | grant action |
| QR-SEC-018 | SAVE | SVC-API | API-SEC-018 | ENT-SEC-004 | SAVE | register module |
| QR-SEC-019 | SAVE | SVC-API | API-SEC-019 | ENT-SEC-005 | SAVE | register screen |
| QR-SEC-020 | SAVE | SVC-API | API-SEC-020 | ENT-SEC-006 | SAVE | register action |
| QR-SEC-021 | FIND_BY_CRITERIA | SVC-API | API-SEC-021 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | FIND_BY_CRITERIA | browse registry tree |
| QR-SEC-022 | AGGREGATE | SVC-API | API-SEC-022 | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-002 | AGGREGATE | dashboard figures (6 widget sub-counts) |
| QR-SEC-023 | FIND_BY_CRITERIA | SVC-API | API-SEC-023 | ENT-SEC-011 | FIND_BY_CRITERIA | search audit log |
| QR-SEC-024 | FIND_BY_CRITERIA | SVC-API | API-SEC-024 | ENT-SEC-011 | FIND_BY_CRITERIA | audit log rows for export |
| QR-SEC-025 | FIND_BY_CRITERIA | SVC-API | API-SEC-025 | ENT-SEC-010 | FIND_BY_CRITERIA | non-terminated sessions |
| QR-SEC-026 | UPDATE | SVC-API | API-SEC-026 | ENT-SEC-010 | UPDATE | terminate session |
| QR-SEC-027 | FIND_BY_CRITERIA | SVC-API | API-SEC-027 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | FIND_BY_CRITERIA | effective menu tree for caller |
| QR-SEC-028 | EXISTS | SVC-API | API-SEC-016 | ENT-SEC-007 | EXISTS | RULE-SEC-001: role holds the screen's module grant? |
| QR-SEC-029 | EXISTS | SVC-API | API-SEC-017 | ENT-SEC-008 | EXISTS | RULE-SEC-002: role holds the action's screen grant? |
| QR-SEC-030 | EXISTS | SVC-API | API-SEC-017 | ENT-SEC-009 | EXISTS | RULE-SEC-007: role holds VIEW on the action's screen? |
| QR-SEC-031 | EXISTS | SVC-API | API-SEC-008, API-SEC-017 | ENT-SEC-009 | EXISTS | RULE-SEC-005: user already holds the conflicting action? |
| QR-SEC-032 | FIND_ALL | SVC-API | API-SEC-015 | ENT-SEC-008, ENT-SEC-009 | FIND_ALL | RULE-SEC-003: dependent screen/action grants for cascade |
| QR-SEC-033 | EXISTS | SVC-API | API-SEC-006 | ENT-SEC-001 | EXISTS | uniqueness: username / email |
| QR-SEC-034 | EXISTS | SVC-API | API-SEC-013 | ENT-SEC-002 | EXISTS | uniqueness: role code |
| QR-SEC-035 | EXISTS | SVC-API | API-SEC-018 | ENT-SEC-004 | EXISTS | uniqueness: module code |
| QR-SEC-036 | EXISTS | SVC-API | API-SEC-019 | ENT-SEC-004, ENT-SEC-005 | EXISTS | RULE-SEC-004: module registered? + uniqueness: page code |
| QR-SEC-037 | EXISTS | SVC-API | API-SEC-020 | ENT-SEC-005, ENT-SEC-006 | EXISTS | screen exists? + uniqueness: permission code |
| QR-SEC-038 | EXISTS | SVC-API | API-SEC-004 | ENT-SEC-012 | EXISTS | RULE-SEC-006: token unexpired and unused? |
| QR-SEC-039 | FIND_ALL | INT-R | — (no API — `SecUserDirectoryApi`) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | FIND_ALL | REQ-SEC-035: user ids holding a permission code through an active role |

Standard operation defaults (engine §5) apply to every QR above unless noted; no QR overrides
paging/filter/transaction defaults except where its row states otherwise. Join governance:
every QR above is single-table or a documented FK join within SEC only (no cross-module join
— SEC has zero XM); no QR joins to resolve a lookup label (`statusCode`/`eventTypeCode` are
returned as codes; the frontend resolves the display label).

---

<!-- PHASE:CORE:START traces=REQ-SEC-033 -->
## PHASE 1 — CORE

**Layers** (`profile.stack.backend.layers`): controller → service → mapper → domain → repository.
- **controller**: HTTP binding, request validation shape (types/required), maps DTO ↔ command; never queries the repository directly; never contains a RULE-* check.
- **service**: orchestration — loads, validates every RULE-*, integrates (for SEC only the optional NOTIF password-reset dispatch — REQ-SEC-029, §PHASE 5 INT-C; still zero XM), persists via repository; the sole place RULE-* logic runs; the sole place PERM_* is asserted before any mutation proceeds.
- **mapper**: entity ↔ DTO conversion only; no business logic, no query.
- **domain**: entity classes; domain-behaviour placement = **in entity methods** for single-entity invariants (e.g. `User.deactivate()` flips `isActiveFl`/`statusCode`), and in the service layer for any rule spanning more than one entity (e.g. RULE-SEC-001/002/003/005/007, which read another table).
- **repository**: Spring Data JPA repositories, one per entity/table; every non-trivial query is a named method backed by a `QR-SEC-*` spec (§Query Reference Catalog); no business logic.

**Error signalling**: `LocalizedException → {code, messageAr, messageEn}`. Runtime `code` format:
`SEC-<HTTP-status>-<SCENARIO>` (module-scoped, stated once here so `api-verify` can assert on it —
e.g. `SEC-409-USER-DUP`). EVERY `SecErrorCodes` value carries that full three-segment shape; no
scenario-less `SEC-<HTTP-status>` code exists, because the 500 path is not module-scoped at all.
Every module-owned catalog row (§Error Catalog) is registered as a static constant in
`SecErrorCodes` that the shared exception→envelope mapping resolves; the single PLATFORM-STD 500
row (`INTERNAL_ERROR`) is owned and emitted by the shared `GlobalExceptionHandler` in
`com.erp.common.web`, so SEC neither declares nor throws it. `messageAr`/`messageEn`
are copied character-perfect from the SRS RULE message or from this plan where PLATFORM-STD.

**Transaction scope defaults**: `READ_ONLY` for every `FIND_*`/`EXISTS`/`AGGREGATE` QR;
`READ_WRITE` for every `SAVE`/`UPDATE`/`DELETE` QR; no `REQUIRES_NEW` anywhere in SEC v1
(no QR overrides this).

**Search contract**: request shape `{filters: {...}, page, size, sort}`; allowed sort fields =
exactly the columns listed as filters in each screen's SRS B2; paging `Page<T>`
(`profile.stack.backend.api.paging`); an empty result is success with empty content, never
"not found" (§5 FIND_BY_CRITERIA default).

**Audit fields**: `createdBy, createdAt, updatedBy, updatedAt` are framework-filled (from the
authenticated principal + server clock) — never present in a create/update request DTO, never
set by a mapper or service method explicitly; the same applies to `grantedBy/grantedAt`,
`assignedBy/assignedAt`, `reviewedBy/reviewedAt`, `terminatedBy/terminatedAt`,
`occurredAt/actorUserId` (all system-set for the same reason, per Field Registry read-only=Yes).

**Type mapping** (`profile.stack.db.syntax_map` postgresql16 → Java, spring-boot-java):
| postgresql16 | Java |
|---|---|
| GENERATED ALWAYS AS IDENTITY | Long |
| VARCHAR(n) | String |
| BOOLEAN | Boolean |
| TIMESTAMPTZ | Instant |
| TEXT | String |
No `NUMERIC` column exists in SEC v1 (no money/decimal field) — not applicable this module; a
future decimal field would map to `BigDecimal` per the same syntax_map row, stated here for
completeness, no deviation ADR needed since none is used.

**Lookup values**: `statusCode` (User, SignupRequest) and `eventTypeCode` (AuditLogEntry) are
returned and accepted as plain strings (the lookup CODE) everywhere in every API below — never
as an enum type in a DTO, never as a numeric id. Per ADR-SEC-001 they are CHECK-constrained in
v1, not FK-backed; the service layer still validates the incoming code against the same closed
set the CHECK constraint enforces, so an invalid code is rejected with a catalog error before it
ever reaches the database.

**Numbering**: not applicable — SEC has no entity with a platform-numbered business code
(Pre-generation extraction: "BUSINESS CODE: none").

**Workflow engine**: forbidden (`profiles/erp.yaml → conventions.workflow_engine`) — every
status transition below (User, SignupRequest) is a plain field update guarded by a RULE or a
dedicated action endpoint, never a workflow definition.

**Languages**: every name field (`nameAr`/`nameEn`, `fullNameAr`/`fullNameEn`,
`descriptionAr`/`descriptionEn`, `detailsAr`/`detailsEn`) and every catalog message is present
in both `ar` and `en` in every DTO and every response — a single-language value anywhere is
incomplete per §Error Catalog / §6.1 rule.

**Cross-module contract placement**: no XM contract to place this version — SEC has zero XM (it
is ROOT). The one interface SEC injects is NOTIF-owned, not SEC-owned: `NotificationDispatchApi`
for the REQ-SEC-029 password-reset dispatch (§PHASE 5 INT-C). SEC itself is consumed by every
future module through its own REST surface (§SVC-API below), not through an injected interface.

**Cross-cutting authorization (REQ-SEC-033)**: a single servlet filter / method-level
interceptor runs before every secured controller method (i.e. every API below except
API-SEC-001..004, which are pre-authentication): it resolves the caller's effective module,
screen and action grants (via the same read path as API-SEC-027's effective-menu query) and
denies with a catalog error (§Error Catalog, `SEC-403`) before the controller method body runs
if the module grant, the screen's VIEW grant (RULE-SEC-007), or the specific action grant is
missing. This single mechanism is what §SEC-BE (Phase 7) and every API's "Security" line below
refer to — it is declared once here, never re-implemented per endpoint.
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START traces=REQ-SEC-001,REQ-SEC-003,REQ-SEC-006,REQ-SEC-009,REQ-SEC-010,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-024 -->
## PHASE 2 — DATA-DOM

Entity count is 13 (≥ the engine's self-check threshold for a split) — grouped below under
`SUB:DATA-DOM-MASTER` (reference/master-like registries), `SUB:DATA-DOM-TRANSACTIONAL` (grant
and session/audit rows with a lifecycle or an occurrence timestamp), and
`SUB:DATA-DOM-LOOKUP` (none — SEC owns no `kind: lookup` entity in its own SRS; its lookup
*values* live in USER_STATUS/SIGNUP_STATUS/AUDIT_EVENT_TYPE, which are CHECK constraints on
other entities, not their own ENT — this SUB is intentionally empty and stated so, not omitted).

<!-- SUB:DATA-DOM-MASTER:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019 -->
### SUB — DATA-DOM-MASTER

#### ENT-SEC-001 — User      kind: security
BINDINGS: table `SEC_USER` · PK `userPk` (DBF-SEC-001) · PK generation `GENERATED ALWAYS AS IDENTITY` · db-script v1
BUSINESS CODE: none (§3.3 test: no)
DEFAULT FIELDS: `security` kind carries no profile-fixed default set (SRS A3 note); every field below is explicit
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-001 | userPk | user_pk | Long | NOT NULL | Yes | PK_SEC_USER | معرّف المستخدم / User id |
| DBF-SEC-002 | username | username | String | NOT NULL | No | UQ_SEC_USER_USERNAME | اسم المستخدم / Username |
| DBF-SEC-003 | email | email | String | NOT NULL | No | UQ_SEC_USER_EMAIL | البريد الإلكتروني / Email |
| DBF-SEC-004 | passwordHash | password_hash | String | NOT NULL | Yes | — | تجزئة كلمة المرور / Password hash |
| DBF-SEC-005 | fullNameAr | full_name_ar | String | NOT NULL | No | — | الاسم الكامل (عربي) / Full name (Arabic) |
| DBF-SEC-006 | fullNameEn | full_name_en | String | NOT NULL | No | — | الاسم الكامل (إنجليزي) / Full name (English) |
| DBF-SEC-007 | statusCode | status_code | String | NOT NULL | Yes | CHK_SEC_USER_STATUS | الحالة / Status |
| DBF-SEC-008 | lastLoginAt | last_login_at | Instant | NULL | Yes | — | آخر دخول / Last login |
| DBF-SEC-009 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-SEC-010 | createdBy | created_by | String | NOT NULL | Yes | — | أُنشئ بواسطة / Created by |
| DBF-SEC-011 | createdAt | created_at | Instant | NOT NULL | Yes | — | تاريخ الإنشاء / Created at |
| DBF-SEC-012 | updatedBy | updated_by | String | NULL | Yes | — | حُدّث بواسطة / Updated by |
| DBF-SEC-013 | updatedAt | updated_at | Instant | NULL | Yes | — | تاريخ التحديث / Updated at |
DTO MEMBERSHIP: create-request excludes {userPk, passwordHash(raw password field instead, hashed server-side), statusCode, lastLoginAt, isActiveFl, audit}; update-request excludes {userPk, username, passwordHash, statusCode, isActiveFl, audit} (username immutable after create; password changes only via API-SEC-004); response includes all except passwordHash (never serialized).
LOOKUP FIELDS: `statusCode` → key `USER_STATUS` → GET /api/v1/mdl/lookups?type=USER_STATUS (API-MDL-011 — MDL's consumer lookup API; v1 validates server-side against the closed set per ADR-SEC-001) — stores the code, never a numeric FK.
DOMAIN RULES: none scoped to User alone (RULE-SEC-005 scopes ENT-SEC-003/009, cited there).
STATE MACHINE: `statusCode` (USER_STATUS) — values PENDING/ACTIVE/DISABLED; initial ACTIVE (direct create, API-SEC-006) or PENDING (via sign-up approval, API-SEC-011); transitions PENDING→ACTIVE (API-SEC-011, actor: administrator), ACTIVE→DISABLED (API-SEC-009, actor: administrator), DISABLED→ACTIVE (API-SEC-010, actor: administrator); no terminal state; no invalid-transition RULE beyond "the four listed transitions are the only ones exposed" (enforced by which endpoint exists, not a DB CHECK on the transition itself).
CROSS-MODULE: none CONSUMED (SEC is ROOT). EXPOSED since 2026-09-11: the contact projection
(email + both display names + active, DBF-SEC-003/005/006/007) and this entity's pk as the
permission-holder result, both read-only via `com.erp.sec.crossmodule.SecUserDirectoryApi`
(REQ-SEC-034, REQ-SEC-035) — no table, column or FK registered, so still no XM row (§PHASE 6).
REPOSITORY OPS → QR-SEC-001 (FIND_ONE by username), QR-SEC-005 (FIND_BY_CRITERIA), QR-SEC-006 (SAVE), QR-SEC-007 (UPDATE), QR-SEC-009 (UPDATE, deactivate), QR-SEC-010 (UPDATE, reactivate), QR-SEC-033 (EXISTS, uniqueness).

#### ENT-SEC-002 — Role      kind: security
BINDINGS: table `SEC_ROLE` · PK `rolePk` (DBF-SEC-014) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-014 | rolePk | role_pk | Long | NOT NULL | Yes | PK_SEC_ROLE | معرّف الدور / Role id |
| DBF-SEC-015 | code | code | String | NOT NULL | No | UQ_SEC_ROLE_CODE | رمز الدور / Role code |
| DBF-SEC-016 | nameAr | name_ar | String | NOT NULL | No | — | اسم الدور (عربي) / Role name (Arabic) |
| DBF-SEC-017 | nameEn | name_en | String | NOT NULL | No | — | اسم الدور (إنجليزي) / Role name (English) |
| DBF-SEC-018 | descriptionAr | description_ar | String | NULL | No | — | الوصف (عربي) / Description (Arabic) |
| DBF-SEC-019 | descriptionEn | description_en | String | NULL | No | — | الوصف (إنجليزي) / Description (English) |
| DBF-SEC-020 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-SEC-021..024 | createdBy/createdAt/updatedBy/updatedAt | created_by/created_at/updated_by/updated_at | String/Instant | see db-script | Yes | — | audit / audit |
DTO MEMBERSHIP: create-request excludes {rolePk, isActiveFl, audit}; update-request excludes {rolePk, code, isActiveFl, audit} (code immutable); response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: none scoped to Role alone.
STATE MACHINE: `isActiveFl` binary only — not applicable for a diagram (SRS A7).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-012 (FIND_BY_CRITERIA), QR-SEC-013 (SAVE), QR-SEC-034 (EXISTS, uniqueness).

#### ENT-SEC-004 — ModuleRegistry      kind: security
BINDINGS: table `SEC_MODULE_REG` · PK `moduleRegPk` (DBF-SEC-030) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `code` is the platform's own module prefix (factory.ids), supplied by the registering module, not system-generated.
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-030 | moduleRegPk | module_reg_pk | Long | NOT NULL | Yes | PK_SEC_MODULE_REG | معرّف الوحدة المسجّلة / Registered module id |
| DBF-SEC-031 | code | code | String | NOT NULL | No | UQ_SEC_MODULE_REG_CODE | رمز الوحدة / Module code |
| DBF-SEC-032 | nameAr | name_ar | String | NOT NULL | No | — | اسم الوحدة (عربي) / Module name (Arabic) |
| DBF-SEC-033 | nameEn | name_en | String | NOT NULL | No | — | اسم الوحدة (إنجليزي) / Module name (English) |
| DBF-SEC-034..038 | isActiveFl/audit | is_active_fl/… | Boolean/String/Instant | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {moduleRegPk, isActiveFl, audit}; update: deactivate only, no field-level update endpoint; response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: none scoped alone (RULE-SEC-004 scopes ENT-SEC-005, cited there).
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none — this table IS the shared registry every future module writes into; no XM row exists because a registration is a plain REST call to SEC (API-SEC-018), not a physical FK from another module's schema.
REPOSITORY OPS → QR-SEC-018 (SAVE), QR-SEC-021 (FIND_BY_CRITERIA), QR-SEC-035 (EXISTS, uniqueness).

#### ENT-SEC-005 — ScreenRegistry      kind: security
BINDINGS: table `SEC_SCREEN_REG` · PK `screenRegPk` (DBF-SEC-039) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `pageCode` is caller-supplied per SEC_PAGES convention, not system-generated.
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-039 | screenRegPk | screen_reg_pk | Long | NOT NULL | Yes | PK_SEC_SCREEN_REG | معرّف الشاشة المسجّلة / Registered screen id |
| DBF-SEC-040 | pageCode | page_code | String | NOT NULL | No | UQ_SEC_SCREEN_REG_PAGE | رمز الصفحة / Page code |
| DBF-SEC-041 | moduleId | module_id | Long | NOT NULL | No | FK_SCREEN_REG_MODULE | الوحدة / Module |
| DBF-SEC-042 | nameAr | name_ar | String | NOT NULL | No | — | اسم الشاشة (عربي) / Screen name (Arabic) |
| DBF-SEC-043 | nameEn | name_en | String | NOT NULL | No | — | اسم الشاشة (إنجليزي) / Screen name (English) |
| DBF-SEC-044..048 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {screenRegPk, isActiveFl, audit}; response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: **RULE-SEC-004** — Scope ENT-SEC-005 · Trigger: on create (screen registration) · Statement: "The system shall reject a screen registration whose module code has no ModuleRegistry row." · Message ar: "الوحدة غير مسجّلة" / en: "Module is not registered" · DB enforcement: `FK_SCREEN_REG_MODULE` (structural — the FK constraint itself makes an unregistered module impossible; the service layer pre-checks with QR-SEC-036 to raise the friendly catalog error before the DB would reject it) · owner layer: service (pre-check) + database (hard guarantee).
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-019 (SAVE), QR-SEC-036 (EXISTS: module registered + page code unique).

#### ENT-SEC-006 — ActionRegistry      kind: security
BINDINGS: table `SEC_ACTION_REG` · PK `actionRegPk` (DBF-SEC-049) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `permissionCode` is derived (`PERM_<pageCode>_<actionCode>`), never entered a second time as seed data (single source of truth).
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-049 | actionRegPk | action_reg_pk | Long | NOT NULL | Yes | PK_SEC_ACTION_REG | معرّف الإجراء المسجّل / Registered action id |
| DBF-SEC-050 | permissionCode | permission_code | String | NOT NULL | Yes (derived) | UQ_SEC_ACTION_REG_PERM | رمز الصلاحية / Permission code |
| DBF-SEC-051 | screenId | screen_id | Long | NOT NULL | No | FK_ACTION_REG_SCREEN | الشاشة / Screen |
| DBF-SEC-052 | actionCode | action_code | String | NOT NULL | No | — | الإجراء / Action |
| DBF-SEC-053 | nameAr | name_ar | String | NOT NULL | No | — | اسم الإجراء (عربي) / Action name (Arabic) |
| DBF-SEC-054 | nameEn | name_en | String | NOT NULL | No | — | اسم الإجراء (إنجليزي) / Action name (English) |
| DBF-SEC-055..059 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {actionRegPk, permissionCode (server-derived), isActiveFl, audit}; response includes all, including the derived `permissionCode`.
LOOKUP FIELDS: none — `actionCode` is not lookup-backed (open set: VIEW/CREATE/UPDATE/DELETE + module-declared custom codes, SRS A3 note).
DOMAIN RULES: none scoped alone.
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-020 (SAVE), QR-SEC-037 (EXISTS: screen exists + permission code unique).
<!-- SUB:DATA-DOM-MASTER:END -->

<!-- SUB:DATA-DOM-TRANSACTIONAL:START traces=REQ-SEC-001,REQ-SEC-010,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-024 -->
### SUB — DATA-DOM-TRANSACTIONAL

#### ENT-SEC-003 — UserRoleAssignment      kind: security
BINDINGS: table `SEC_USER_ROLE` · PK `userRolePk` (DBF-SEC-025) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-025 | userRolePk | user_role_pk | Long | NOT NULL | Yes | PK_SEC_USER_ROLE |
| DBF-SEC-026 | userId | user_id | Long | NOT NULL | No | FK_USER_ROLE_USER, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-027 | roleId | role_id | Long | NOT NULL | No | FK_USER_ROLE_ROLE, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-028 | assignedBy | assigned_by | String | NOT NULL | Yes | — |
| DBF-SEC-029 | assignedAt | assigned_at | Instant | NOT NULL | Yes | — |
DTO MEMBERSHIP: request = `{roleIds: [Long]}` (batch); response = list of `{roleId, code, nameAr, nameEn}`.
DOMAIN RULES: **RULE-SEC-005** applies here too (assigning a role that carries an action already held via another role, for a conflicting pair, is rejected — same QR-SEC-031 check as API-SEC-017, cited fully under ENT-SEC-009 below to avoid duplication).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-008 (SAVE, batch), QR-SEC-031 (EXISTS, conflict check, shared with API-SEC-017).

#### ENT-SEC-007 — RoleModuleGrant      kind: security
BINDINGS: table `SEC_ROLE_MODULE_GRANT` · PK `roleModuleGrantPk` (DBF-SEC-060) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-060 | roleModuleGrantPk | role_module_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_MODULE_GRANT |
| DBF-SEC-061 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_MODULE_GRANT_ROLE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-062 | moduleId | module_id | Long | NOT NULL | No | FK_ROLE_MODULE_GRANT_MODULE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-063 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-064 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES: **RULE-SEC-003** — Scope ENT-SEC-007 · Trigger: on delete (module grant) · Statement: "The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked." · Message ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" / en: "Every screen and action grant under this module for this role will be revoked" · DB enforcement: application layer (service, transactional) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-014 (SAVE), QR-SEC-015 (DELETE), QR-SEC-032 (FIND_ALL, cascade targets).

#### ENT-SEC-008 — RoleScreenGrant      kind: security
BINDINGS: table `SEC_ROLE_SCREEN_GRANT` · PK `roleScreenGrantPk` (DBF-SEC-065) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-065 | roleScreenGrantPk | role_screen_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_SCREEN_GRANT |
| DBF-SEC-066 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_SCREEN_GRANT_ROLE, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-067 | screenId | screen_id | Long | NOT NULL | No | FK_ROLE_SCREEN_GRANT_SCREEN, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-068 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-069 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES: **RULE-SEC-001** — Scope ENT-SEC-008 · Trigger: on create (screen grant) · Statement: "The system shall prevent a screen grant for a role that does not hold the screen's module grant." · Message ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" / en: "Cannot grant a screen without first granting its module" · DB enforcement: application layer (service, via QR-SEC-028 pre-check) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-016 (SAVE), QR-SEC-028 (EXISTS, module-grant pre-check).

#### ENT-SEC-009 — RoleActionGrant      kind: security
BINDINGS: table `SEC_ROLE_ACTION_GRANT` · PK `roleActionGrantPk` (DBF-SEC-070) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-070 | roleActionGrantPk | role_action_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_ACTION_GRANT |
| DBF-SEC-071 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_ACTION_GRANT_ROLE, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-072 | actionId | action_id | Long | NOT NULL | No | FK_ROLE_ACTION_GRANT_ACTION, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-073 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-074 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES:
**RULE-SEC-002** — Scope ENT-SEC-009 · Trigger: on create (action grant) · Statement: "The system shall prevent an action grant for a role that does not hold the action's screen grant." · Message ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" / en: "Cannot grant an action without first granting its screen" · DB enforcement: application layer (service, via QR-SEC-029) · owner layer: service.
**RULE-SEC-005** — Scope ENT-SEC-003, ENT-SEC-009 · Trigger: on create (role assignment or action grant) · Statement: "The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair." · Message ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" / en: "This user already holds a conflicting action" · DB enforcement: application layer (service, via QR-SEC-031, checked across all of a user's roles) · owner layer: service. · **DEFERRED (SEC v1)**: nothing in SEC v1 declares a conflicting pair — there is no ENT, DBF, table, API field or screen element for such a declaration — so REQ-SEC-020's `optional` precondition is never established and this guard, though implemented, is inert (the conflicting-counterpart set is empty in v1). The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.
**RULE-SEC-007** — Scope ENT-SEC-009 · Trigger: on evaluate (any action check) and on create (action grant, informational) · Statement: "The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it." · Message ar: "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" / en: "The VIEW action must be granted on this screen first" · DB enforcement: application layer (service, via QR-SEC-030 at grant time + the CORE interceptor at request time) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-017 (SAVE), QR-SEC-029 (EXISTS), QR-SEC-030 (EXISTS), QR-SEC-031 (EXISTS).

#### ENT-SEC-010 — ActiveSession      kind: security
BINDINGS: table `SEC_ACTIVE_SESSION` · PK `activeSessionPk` (DBF-SEC-075) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-075..082 — see DB Alignment Manifest; all `read-only: Yes` (system lifecycle, no client-editable field).
DTO MEMBERSHIP: no create/update request (system-created at login, API-SEC-001); response includes all except `tokenRef` (never serialized back).
DOMAIN RULES: none scoped alone.
STATE MACHINE: `terminatedAt IS NULL` = active, `terminatedAt IS NOT NULL` = terminated — binary, not applicable for a diagram.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-025 (FIND_BY_CRITERIA, non-terminated), QR-SEC-026 (UPDATE, terminate) — also written to by API-SEC-001 (create) and API-SEC-009 (bulk-terminate on deactivate), both via the same repository, no separate QR (plain SAVE / batch UPDATE, standard defaults apply).

#### ENT-SEC-011 — AuditLogEntry      kind: security
BINDINGS: table `SEC_AUDIT_LOG` · PK `auditLogPk` (DBF-SEC-083) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-083..090 — see DB Alignment Manifest; all `read-only: Yes` (append-only, no update endpoint exists at all — immutability, POL-SEC-009).
DTO MEMBERSHIP: no create/update request exposed to any client — rows are written internally by every other API's orchestration step, never by a dedicated "create audit entry" endpoint; response (search/export) includes all fields.
DOMAIN RULES: none (the immutability itself is enforced by omission — no UPDATE/DELETE mapping exists on this repository at all, not by a DB trigger).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-023 (FIND_BY_CRITERIA, search), QR-SEC-024 (FIND_BY_CRITERIA, export) — writes happen inline from every other service method (SAVE, standard defaults, no dedicated QR per writer to avoid 20+ near-identical entries; the write shape is always `{eventTypeCode, actorUserId, occurredAt=now(), targetRef, detailsAr, detailsEn, ipAddress}`).

#### ENT-SEC-012 — PasswordResetToken      kind: security
BINDINGS: table `SEC_PWD_RESET_TOKEN` · PK `pwdResetTokenPk` (DBF-SEC-091) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-091..096 — see DB Alignment Manifest; all `read-only: Yes` (system-managed lifecycle).
DTO MEMBERSHIP: no direct create/read/update DTO — entirely internal to API-SEC-003/004's orchestration; the client only ever sees the opaque token string delivered out-of-band (§API-SEC-003 Response).
DOMAIN RULES: **RULE-SEC-006** — Scope ENT-SEC-012 · Trigger: on submit (password reset completion) · Statement: "The system shall reject a password-reset submission whose token is expired or already used." · Message ar: "رابط إعادة التعيين غير صالح أو منتهي" / en: "This reset link is invalid or has expired" · DB enforcement: application layer (service, via QR-SEC-038) · owner layer: service. `expiresAt` DEFAULT = `requestedAt + 30 minutes` (SRS A7 DEFAULT, non-breaking).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-003 (SAVE), QR-SEC-004 (UPDATE, on completion), QR-SEC-038 (EXISTS, validity check).

#### ENT-SEC-013 — SignupRequest      kind: security
BINDINGS: table `SEC_SIGNUP_REQUEST` · PK `signupRequestPk` (DBF-SEC-097) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-097..104 — see DB Alignment Manifest.
DTO MEMBERSHIP: create-request = `{email, fullNameAr, fullNameEn}`; no update endpoint (only approve/reject via API-SEC-011); response includes all.
LOOKUP FIELDS: `statusCode` → key `SIGNUP_STATUS` (ADR-SEC-001, same treatment as USER_STATUS).
DOMAIN RULES: none scoped alone (RULE-SEC-004/007 do not apply here).
STATE MACHINE: `statusCode` (SIGNUP_STATUS) — values PENDING/APPROVED/REJECTED; initial PENDING; transitions PENDING→APPROVED (API-SEC-011 approve, actor: administrator), PENDING→REJECTED (API-SEC-011 reject, actor: administrator); both terminal; no invalid-transition RULE beyond "only PENDING may transition."
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-002 (SAVE), QR-SEC-011 (UPDATE, decision).
<!-- SUB:DATA-DOM-TRANSACTIONAL:END -->

<!-- SUB:DATA-DOM-LOOKUP:START traces=REQ-SEC-004 -->
### SUB — DATA-DOM-LOOKUP
Not applicable — SEC owns no `kind: lookup` `ENT` of its own this version; `USER_STATUS`,
`SIGNUP_STATUS`, `AUDIT_EVENT_TYPE` are CHECK-constrained value sets on other entities'
`statusCode`/`eventTypeCode` columns (ADR-SEC-001), not separate lookup tables/entities. This
SUB exists to satisfy the engine's grouping convention and is intentionally empty.
<!-- SUB:DATA-DOM-LOOKUP:END -->
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START traces=REQ-SEC-001,REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-022,REQ-SEC-025 -->
## PHASE 3 — SVC-API

API count = 27 ≥ 8 → split by threshold, grouped CRUD / SEARCH / INT.

<!-- SUB:SVC-API-SEARCH:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-022,REQ-SEC-025,REQ-SEC-027 -->
### SUB — SVC-API-SEARCH (read-only)

<!-- API:API-SEC-005:START traces=REQ-SEC-009,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 -->
### API-SEC-005 — search users
Endpoint     : POST /api/v1/sec/users/search   verb: POST
Layers       : controller → `UserController.search` ; service → `UserService.search`
Request      : body `UserSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `username`, `email`, `statusCode`, plus `fullName` (LIKE, matches fullNameAr or fullNameEn), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<UserResponse>` (userPk, username, email, fullNameAr, fullNameEn, statusCode, lastLoginAt) · envelope `ApiResponse<Page<UserResponse>>`
Validations  : none (read-only)
Errors       : none beyond platform-standard (§Error Catalog INTERNAL_ERROR)
Orchestration: load (QR-SEC-005 FIND_BY_CRITERIA) → map → return
Repository   : QR-SEC-005 · join NONE · transaction READ_ONLY
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_VIEW`
Localization : fullNameAr/fullNameEn both returned
<!-- API:API-SEC-005:END -->

<!-- API:API-SEC-012:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-020 -->
### API-SEC-012 — search roles
Endpoint     : POST /api/v1/sec/roles/search
Layers       : controller → `RoleController.search` ; service → `RoleService.search`
Request      : body `RoleSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `code`, `isActiveFl`, plus `name` (LIKE, nameAr or nameEn), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<RoleResponse>` (rolePk, code, nameAr, nameEn, descriptionAr, descriptionEn, isActiveFl) · `ApiResponse<Page<RoleResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-012) → map → return
Repository   : QR-SEC-012 · join NONE · transaction READ_ONLY
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_VIEW`
Localization : nameAr/nameEn/descriptionAr/descriptionEn returned
<!-- API:API-SEC-012:END -->

<!-- API:API-SEC-021:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-040,DBF-SEC-050 -->
### API-SEC-021 — search registry
Endpoint     : POST /api/v1/sec/registry/search
Layers       : controller → `RegistryController.search` ; service → `RegistryService.search`
Request      : body `RegistrySearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `code` (the module code), plus `pageCode` (LIKE, on the child screen), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<RegistryRowResponse>` (a module row with its nested active screens and, per screen, its active actions) · `ApiResponse<Page<RegistryRowResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-021, joins SEC_MODULE_REG → SEC_SCREEN_REG → SEC_ACTION_REG, all intra-module) → assemble tree → return
Repository   : QR-SEC-021 · join intra-module (module/screen/action — same table family, not cross-module) · transaction READ_ONLY
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_VIEW`
Localization : nameAr/nameEn at every level
<!-- API:API-SEC-021:END -->

<!-- API:API-SEC-022:START traces=REQ-SEC-022,REQ-SEC-023,DBF-SEC-007,DBF-SEC-076,DBF-SEC-081,DBF-SEC-084,DBF-SEC-086 -->
### API-SEC-022 — dashboard summary
Endpoint     : GET /api/v1/sec/dashboard
Layers       : controller → `DashboardController.summary` ; service → `DashboardService.summary`
Request      : none
Response     : 200 · `DashboardResponse` — six sub-figures, each present only if the caller holds that widget's source-screen VIEW permission (omitted field, not a zeroed one, when absent): `usersOverview{total,active,disabled,pendingSignups}`, `failedLogins24h{count}`, `activeSessions{count}`, `recentActivity{list of last N AuditLogEntry}`, `rolesPermissionsSummary{roleCount,privilegedRoleCount,usersPerRole}`, `onboardingFunnel{pendingSignups,stalledCount}` · `ApiResponse<DashboardResponse>`
Validations  : none — this endpoint filters its OWN output by REQ-SEC-023 (unwanted pattern) rather than rejecting the call
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: resolve caller's effective permissions (same read path as API-SEC-027) → for each widget whose source-screen VIEW the caller holds, compute it live (QR-SEC-022 sub-queries) → assemble → return (REQ-SEC-022: every figure computed at that moment, never cached)
Repository   : QR-SEC-022 (6 independent COUNT/aggregate sub-queries against SEC_USER, SEC_ACTIVE_SESSION, SEC_AUDIT_LOG, SEC_ROLE/SEC_USER_ROLE) · join NONE (each sub-query is single-table) · transaction READ_ONLY
Security     : screen SEC_DASHBOARD · permission `PERM_SEC_DASHBOARD_VIEW` (gateway) + per-widget the widget's own source-screen VIEW (SEC_USERS, SEC_SESSIONS, SEC_AUDIT_LOG, SEC_ROLES)
Localization : recentActivity entries carry detailsAr/detailsEn
Notes        : three figures in the Response line above are named but never defined upstream — `recentActivity`'s N, `onboardingFunnel.stalledCount`'s "stalled" window, and what makes a role `privileged` (REQ-SEC-022, AC-SEC-022, SCR-REQ-SEC-007 B1-B5, prd-sec.md and all 104 DBF are silent). Operative definitions, chosen at implementation to fill that silence and subject to a human's confirmation: N = 10 most recent AuditLogEntry rows; stalled = a SignupRequest still PENDING more than 7 days; privileged role = a role holding at least one action grant whose `actionCode` is not the VIEW gateway (RULE-SEC-007 / REQ-SEC-030's own VIEW-vs-other distinction, QR-SEC-022 via `countPrivilegedRoles`). All three are named constants in one class, not config keys, so a decision can change them in one place; nothing upstream authorises them yet.
<!-- API:API-SEC-022:END -->

<!-- API:API-SEC-023:START traces=REQ-SEC-025,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086 -->
### API-SEC-023 — search audit log
Endpoint     : POST /api/v1/sec/audit-log/search
Layers       : controller → `AuditLogController.search` ; service → `AuditLogService.search`
Request      : body `AuditLogEntrySearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `eventTypeCode` and `occurredAt` (any range operator), plus `actorUserId` (EXACT), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<AuditLogEntryResponse>` (all fields, unmodified) · `ApiResponse<Page<AuditLogEntryResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-023) → return unmodified (REQ-SEC-025: "without altering any of them")
Repository   : QR-SEC-023 · join NONE · transaction READ_ONLY
Security     : screen SEC_AUDIT_LOG · permission `PERM_SEC_AUDIT_LOG_VIEW`
Localization : detailsAr/detailsEn returned
<!-- API:API-SEC-023:END -->

<!-- API:API-SEC-025:START traces=REQ-SEC-027,DBF-SEC-076,DBF-SEC-079,DBF-SEC-081 -->
### API-SEC-025 — list active sessions
Endpoint     : POST /api/v1/sec/sessions/search
Layers       : controller → `SessionController.search` ; service → `SessionService.search`
Request      : body `ActiveSessionSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `ipAddress`, plus `userId` (EXACT), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<ActiveSessionResponse>` (activeSessionPk, userId, username, startedAt, lastActivityAt, ipAddress) — `tokenRef` never returned · `ApiResponse<Page<ActiveSessionResponse>>`
Validations  : filter `terminatedAt IS NULL` always applied server-side (REQ-SEC-027: "every session that has not been terminated") — not a client-supplied filter
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-025, filter terminatedAt IS NULL) → map → return
Repository   : QR-SEC-025 · join NONE · transaction READ_ONLY
Security     : screen SEC_SESSIONS · permission `PERM_SEC_SESSIONS_VIEW`
Localization : username shown (via userId join within SEC_USER, intra-module)
<!-- API:API-SEC-025:END -->

<!-- API:API-SEC-027:START traces=REQ-SEC-021,REQ-SEC-032,REQ-SEC-033,DBF-SEC-031,DBF-SEC-040,DBF-SEC-061,DBF-SEC-066 -->
### API-SEC-027 — effective menu
Endpoint     : GET /api/v1/sec/menu
Layers       : controller → `MenuController.effective` ; service → `MenuService.effective`
Request      : none (caller resolved from the authenticated session)
Response     : 200 · `List<ModuleMenuResponse>` — only modules the caller's effective grants hold (REQ-SEC-021/032), each with only that caller's effective granted screens beneath it · `ApiResponse<List<ModuleMenuResponse>>`
Validations  : none — the endpoint's entire behaviour IS the filter (REQ-SEC-021, REQ-SEC-032)
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: resolve caller's roles (QR-SEC-027, join SEC_ROLE_MODULE_GRANT/SEC_ROLE_SCREEN_GRANT to SEC_MODULE_REG/SEC_SCREEN_REG, all intra-module) → union across the caller's roles → assemble modules→screens tree → return
Repository   : QR-SEC-027 · join intra-module (grant tables to registry tables) · transaction READ_ONLY
Security     : no page code of its own (SRS B4) — every authenticated caller may call it; its content is the security boundary, not a permission on itself
Localization : nameAr/nameEn at module and screen level
<!-- API:API-SEC-027:END -->
<!-- SUB:SVC-API-SEARCH:END -->

<!-- SUB:SVC-API-CRUD:START traces=REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-020,REQ-SEC-028,REQ-SEC-030,REQ-SEC-031 -->
### SUB — SVC-API-CRUD (single-entity mutations)

<!-- API:API-SEC-006:START traces=REQ-SEC-009,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 -->
### API-SEC-006 — create user
Endpoint     : POST /api/v1/sec/users
Layers       : controller → `UserController.create` ; service → `UserService.create`
Request      : body `{username, email, fullNameAr, fullNameEn, password, statusCode?}` — statusCode optional, defaults ACTIVE; excludes {userPk, passwordHash, lastLoginAt, isActiveFl, audit}
Response     : 201 · `UserResponse` (no passwordHash)
Validations  : uniqueness of username and email (QR-SEC-033) — violation → catalog `SEC-409-USER-DUP`
Errors       : `SEC-409-USER-DUP` (409)
Orchestration: validate uniqueness (QR-SEC-033) → hash password → persist (QR-SEC-006) → append AuditLogEntry `MODULE_GRANTED`? no — no audit event type is defined for plain user creation in SRS A6, so none is appended here (only the 14 named AUDIT_EVENT_TYPE codes apply; user-creation itself is not one of them, per SRS A6 — no invention) → return
Repository   : QR-SEC-006, QR-SEC-033 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_CREATE`
Localization : fullNameAr and fullNameEn both required
<!-- API:API-SEC-006:END -->

<!-- API:API-SEC-007:START traces=REQ-SEC-009,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006 -->
### API-SEC-007 — update user
Endpoint     : PUT /api/v1/sec/users/{id}
Layers       : controller → `UserController.update` ; service → `UserService.update`
Request      : body `{email, fullNameAr, fullNameEn}` — excludes {userPk, username, passwordHash, statusCode, isActiveFl, audit}
Response     : 200 · `UserResponse`
Validations  : uniqueness of email excluding the current PK (QR-SEC-033, EXISTS default "excludes the current PK on update")
Errors       : `SEC-409-USER-DUP` (409), `SEC-404-USER` (404, id not found)
Orchestration: load (QR-SEC-001-style FIND_ONE by PK) → validate → update (QR-SEC-007) → return
Repository   : QR-SEC-007 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : both name fields updatable independently
<!-- API:API-SEC-007:END -->

<!-- API:API-SEC-008:START traces=REQ-SEC-010,DBF-SEC-026,DBF-SEC-027,DBF-SEC-028,DBF-SEC-029 -->
### API-SEC-008 — assign roles to user
Endpoint     : PUT /api/v1/sec/users/{id}/roles
Layers       : controller → `UserController.assignRoles` ; service → `UserRoleService.assign`
Request      : body `{roleIds: [Long]}`
Response     : 200 · `UserResponse` with nested `roles: [{roleId, code, nameAr, nameEn}]`
Validations  : RULE-SEC-005 (full text: DATA-DOM §ENT-SEC-009) — the union of the user's existing + newly assigned roles' actions must contain no conflicting pair (QR-SEC-031) → violation `SEC-409-SOD-CONFLICT`
Errors       : `SEC-409-SOD-CONFLICT` (409), `SEC-404-USER`/`SEC-404-ROLE` (404)
Orchestration: load user + roles → check RULE-SEC-005 (QR-SEC-031) → replace the assignment set (QR-SEC-008) → append AuditLogEntry `ROLE_ASSIGNED` per added role, `ROLE_REVOKED` per removed role → return
Repository   : QR-SEC-008, QR-SEC-031 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : role name fields returned in both languages
<!-- API:API-SEC-008:END -->

<!-- API:API-SEC-009:START traces=REQ-SEC-011,DBF-SEC-007,DBF-SEC-009,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-009 — deactivate user
Endpoint     : DELETE /api/v1/sec/users/{id}
Layers       : controller → `UserController.deactivate` ; service → `UserService.deactivate`
Request      : path `id`
Response     : 200 · confirmation `{userPk, statusCode: "DISABLED"}`
Validations  : none beyond existence
Errors       : `SEC-404-USER` (404)
Orchestration: load user → `User.deactivate()` (domain method: statusCode→DISABLED, isActiveFl→false) → persist (QR-SEC-009) → terminate every active session of that user (QR-SEC-026, bulk) → append AuditLogEntry `SESSION_TERMINATED` per session — this is the "immediately end that user's active sessions" half of REQ-SEC-011
Repository   : QR-SEC-009, QR-SEC-026 (bulk) · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a (no message text beyond the confirmation)
<!-- API:API-SEC-009:END -->

<!-- API:API-SEC-010:START traces=REQ-SEC-031,DBF-SEC-007,DBF-SEC-009 -->
### API-SEC-010 — reactivate user
Endpoint     : PATCH /api/v1/sec/users/{id}
Layers       : controller → `UserController.reactivate` ; service → `UserService.reactivate`
Request      : path `id`
Response     : 200 · confirmation `{userPk, statusCode: "ACTIVE"}`
Validations  : the user must currently be DISABLED (A7 lifecycle — an ACTIVE or PENDING user cannot be "reactivated")
Errors       : `SEC-404-USER` (404), `SEC-409-INVALID-TRANSITION` (409)
Orchestration: load user → verify statusCode=DISABLED → `User.reactivate()` → persist (QR-SEC-010) → return
Repository   : QR-SEC-010 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a
<!-- API:API-SEC-010:END -->

<!-- API:API-SEC-011:START traces=REQ-SEC-004,REQ-SEC-005,DBF-SEC-102,DBF-SEC-103,DBF-SEC-104,DBF-SEC-001 -->
### API-SEC-011 — approve / reject signup
Endpoint     : PATCH /api/v1/sec/signup-requests/{id}
Layers       : controller → `SignupRequestController.decide` ; service → `SignupRequestService.decide`
Request      : body `{decision: "APPROVE"|"REJECT"}`
Response     : 200 · on APPROVE: the created `UserResponse`; on REJECT: the updated `SignupRequestResponse` (statusCode=REJECTED)
Validations  : the request must currently be PENDING (A7 lifecycle)
Errors       : `SEC-404-SIGNUP` (404), `SEC-409-INVALID-TRANSITION` (409)
Orchestration: load SignupRequest → verify PENDING → on APPROVE: create User (QR-SEC-006-style, statusCode=ACTIVE) + update SignupRequest (statusCode=APPROVED, reviewedBy/At) (QR-SEC-011); on REJECT: update SignupRequest (statusCode=REJECTED, reviewedBy/At) only, no User row (REQ-SEC-005) → return
Repository   : QR-SEC-011 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS (the "Pending sign-ups" tab, SRS B3) · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a
Notes        : credential — no upstream artifact names one for the approved account, yet DBF-SEC-004 `password_hash` is NOT NULL. Operative behaviour: the account is created with an unusable random secret (never logged, returned or transmitted), so it is ACTIVE but cannot be authenticated against; the owner's route to a real credential is the existing API-SEC-003 → API-SEC-004 reset pair (RULE-SEC-006). OPEN for a human (not decided here): no SEC artifact says the approved owner is notified or that a reset token is issued at approval — REQ-SEC-029 triggers on token issuance, not approval, and is itself `optional` — and the state is undiscoverable from outside, since login answers the deliberately uniform `SEC-401-INVALID-CREDENTIALS` (POL-SEC-004) and API-SEC-003 answers the same generic 200 whether or not the email exists, making "approved", "still pending" and "rejected" indistinguishable. The two candidate answers are (a) SEC issues a reset token at approval time — which introduces a NOTIF dependency into a module declared ROOT — or (b) the spec states plainly that the owner is informed out of band.
<!-- API:API-SEC-011:END -->

<!-- API:API-SEC-013:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019 -->
### API-SEC-013 — create role
Endpoint     : POST /api/v1/sec/roles
Layers       : controller → `RoleController.create` ; service → `RoleService.create`
Request      : body `{code, nameAr, nameEn, descriptionAr?, descriptionEn?}` — excludes {rolePk, isActiveFl, audit}
Response     : 201 · `RoleResponse`
Validations  : uniqueness of code (QR-SEC-034)
Errors       : `SEC-409-ROLE-DUP` (409)
Orchestration: validate uniqueness → persist (QR-SEC-013) → return
Repository   : QR-SEC-013, QR-SEC-034 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_CREATE`
Localization : nameAr/nameEn required; descriptionAr/En optional but both-or-neither
<!-- API:API-SEC-013:END -->

<!-- API:API-SEC-014:START traces=REQ-SEC-012,DBF-SEC-061,DBF-SEC-062,DBF-SEC-063,DBF-SEC-064 -->
### API-SEC-014 — grant module to role
Endpoint     : POST /api/v1/sec/roles/{id}/modules
Layers       : controller → `RoleGrantController.grantModule` ; service → `RoleGrantService.grantModule`
Request      : body `{moduleId}`
Response     : 201 · `RoleModuleGrantResponse`
Validations  : role and module must be active; grant must not already exist (UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE)
Errors       : `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-MODULE` (404)
Orchestration: validate → persist (QR-SEC-014) → append AuditLogEntry `MODULE_GRANTED` → return
Repository   : QR-SEC-014 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-014:END -->

<!-- API:API-SEC-015:START traces=REQ-SEC-015,DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-071 -->
### API-SEC-015 — revoke module grant
Endpoint     : DELETE /api/v1/sec/roles/{id}/modules/{moduleId}
Layers       : controller → `RoleGrantController.revokeModule` ; service → `RoleGrantService.revokeModule`
Request      : path `id` (role), `moduleId`
Response     : 200 · confirmation `{revokedScreenGrants: n, revokedActionGrants: m}`
Validations  : RULE-SEC-003 (full text: DATA-DOM §ENT-SEC-007) — cascade is mandatory, not optional
Errors       : `SEC-404-GRANT` (404, grant does not exist)
Orchestration: find dependent screen/action grants for this role under this module (QR-SEC-032) → delete them, then the module grant itself (QR-SEC-015) → append AuditLogEntry `MODULE_REVOKED` (+ `SCREEN_REVOKED`/`ACTION_REVOKED` per cascaded row) → return
Repository   : QR-SEC-015, QR-SEC-032 · join NONE (sequential deletes, same transaction) · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-015:END -->

<!-- API:API-SEC-016:START traces=REQ-SEC-013,DBF-SEC-066,DBF-SEC-067,DBF-SEC-068,DBF-SEC-069 -->
### API-SEC-016 — grant screen to role
Endpoint     : POST /api/v1/sec/roles/{id}/screens
Layers       : controller → `RoleGrantController.grantScreen` ; service → `RoleGrantService.grantScreen`
Request      : body `{screenId}`
Response     : 201 · `RoleScreenGrantResponse`
Validations  : RULE-SEC-001 (full text: DATA-DOM §ENT-SEC-008) — role must already hold the screen's module grant (QR-SEC-028) → violation `SEC-409-NO-MODULE-GRANT`
Errors       : `SEC-409-NO-MODULE-GRANT` (409), `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-SCREEN` (404)
Orchestration: resolve screen's module → check RULE-SEC-001 (QR-SEC-028) → persist (QR-SEC-016) → append AuditLogEntry `SCREEN_GRANTED` → return
Repository   : QR-SEC-016, QR-SEC-028 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-016:END -->

<!-- API:API-SEC-017:START traces=REQ-SEC-014,REQ-SEC-020,REQ-SEC-030,DBF-SEC-071,DBF-SEC-072,DBF-SEC-073,DBF-SEC-074 -->
### API-SEC-017 — grant action to role
Endpoint     : POST /api/v1/sec/roles/{id}/actions
Layers       : controller → `RoleGrantController.grantAction` ; service → `RoleGrantService.grantAction`
Request      : body `{actionId}`
Response     : 201 · `RoleActionGrantResponse`
Validations  : RULE-SEC-002 (full text: DATA-DOM §ENT-SEC-009) — role must hold the action's screen grant (QR-SEC-029); RULE-SEC-007 (full text: DATA-DOM §ENT-SEC-009) — unless the action itself is VIEW, role must also hold VIEW on that screen (QR-SEC-030); RULE-SEC-005 (full text: DATA-DOM §ENT-SEC-009) — the grant must not give the underlying user a conflicting-pair action (QR-SEC-031)
Errors       : `SEC-409-NO-SCREEN-GRANT` (409), `SEC-409-NO-VIEW-GRANT` (409), `SEC-409-SOD-CONFLICT` (409), `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-ACTION` (404)
Orchestration: resolve action's screen → check RULE-SEC-002 (QR-SEC-029) → check RULE-SEC-007 unless actionCode=VIEW (QR-SEC-030) → check RULE-SEC-005 across every user holding this role (QR-SEC-031) → persist (QR-SEC-017) → append AuditLogEntry `ACTION_GRANTED` → return
Repository   : QR-SEC-017, QR-SEC-029, QR-SEC-030, QR-SEC-031 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-017:END -->

<!-- API:API-SEC-026:START traces=REQ-SEC-028,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-026 — terminate session
Endpoint     : DELETE /api/v1/sec/sessions/{id}
Layers       : controller → `SessionController.terminate` ; service → `SessionService.terminate`
Request      : path `id`
Response     : 200 · confirmation `{activeSessionPk, terminatedAt}`
Validations  : session must currently be active (`terminatedAt IS NULL`)
Errors       : `SEC-404-SESSION` (404), `SEC-409-ALREADY-TERMINATED` (409)
Orchestration: load session → verify active → set terminatedAt/terminatedBy (QR-SEC-026) → append AuditLogEntry `SESSION_TERMINATED` → the associated tokenRef is invalidated at the auth-filter cache layer (REQ-SEC-028: "no longer accepted for any subsequent request") → return
Repository   : QR-SEC-026 · join NONE · transaction READ_WRITE
Security     : screen SEC_SESSIONS · permission `PERM_SEC_SESSIONS_DELETE`
Localization : n/a
<!-- API:API-SEC-026:END -->
<!-- SUB:SVC-API-CRUD:END -->

<!-- SUB:SVC-API-INT:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-006,REQ-SEC-007,REQ-SEC-008,REQ-SEC-016,REQ-SEC-017,REQ-SEC-018,REQ-SEC-019,REQ-SEC-026,REQ-SEC-029 -->
### SUB — SVC-API-INT (auth flows, onboarding registration, export)

<!-- API:API-SEC-001:START traces=REQ-SEC-001,REQ-SEC-002,DBF-SEC-002,DBF-SEC-004,DBF-SEC-007,DBF-SEC-075,DBF-SEC-076,DBF-SEC-077,DBF-SEC-078 -->
### API-SEC-001 — login
Endpoint     : POST /api/v1/sec/auth/login   (pre-authentication)
Layers       : controller → `AuthController.login` ; service → `AuthService.login`
Request      : body `{username, password}`
Response     : 200 (success) · `{accessToken, tokenType, expiresIn}` · `ApiResponse<LoginResponse>` — 401 on failure, same envelope shape, `success:false`
Validations  : credentials must match an ACTIVE user (QR-SEC-001 + password verify)
Errors       : `SEC-401-INVALID-CREDENTIALS` (401)
Orchestration: load user by username (QR-SEC-001) → verify active + password → on success: create ActiveSession (QR-SEC-006-style SAVE on ENT-SEC-010), update `lastLoginAt`, append AuditLogEntry `LOGIN_SUCCESS`, issue token → on failure: append AuditLogEntry `LOGIN_FAILED` (actorUserId null if username unknown), return 401 (REQ-SEC-002)
Repository   : QR-SEC-001 · join NONE · transaction READ_WRITE (session + audit write even on the read-mostly path)
Security     : screen SEC_LOGIN · public — no permission required
Localization : `SEC-401-INVALID-CREDENTIALS` message ar: "بيانات الدخول غير صحيحة" / en: "Invalid credentials" (POL-SEC-004)
<!-- API:API-SEC-001:END -->

<!-- API:API-SEC-002:START traces=REQ-SEC-003,DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 -->
### API-SEC-002 — submit sign-up
Endpoint     : POST /api/v1/sec/auth/signup   (pre-authentication)
Layers       : controller → `AuthController.signup` ; service → `SignupRequestService.submit`
Request      : body `{email, fullNameAr, fullNameEn}`
Response     : 201 · `SignupRequestResponse` (statusCode=PENDING)
Validations  : email not already a User.email and not an already-PENDING SignupRequest.email
Errors       : `SEC-409-SIGNUP-DUP` (409)
Orchestration: validate → persist (QR-SEC-002, statusCode=PENDING) → return
Repository   : QR-SEC-002 · join NONE · transaction READ_WRITE
Security     : screen SEC_SIGNUP · public — no permission required
Localization : fullNameAr/fullNameEn both required
<!-- API:API-SEC-002:END -->

<!-- API:API-SEC-003:START traces=REQ-SEC-006,REQ-SEC-029,DBF-SEC-092,DBF-SEC-093,DBF-SEC-094,DBF-SEC-095 -->
### API-SEC-003 — request password reset
Endpoint     : POST /api/v1/sec/auth/password-reset/request   (pre-authentication)
Layers       : controller → `AuthController.requestReset` ; service → `PasswordResetService.request`
Request      : body `{email}`
Response     : 200 · generic confirmation `{message}` — always the same shape whether or not the email exists (never reveals which)
Validations  : none exposed to the caller (existence check is internal only, never surfaced)
Errors       : none beyond platform-standard
Orchestration: look up user by email (internal) → if found: create PasswordResetToken (QR-SEC-003, expiresAt=now+30min) → append AuditLogEntry `PASSWORD_RESET_REQUESTED` → **Where** the Notifications integration is enabled (REQ-SEC-029, optional pattern): dispatch per `new project/integration-notifications-fileservice.md` §1.2 through NOTIF's `NotificationDispatchApi.dispatchIndependently()` crossmodule entry point — not `dispatch()`: `dispatchIndependently` runs in an independent transaction (`REQUIRES_NEW`), so a dispatch failure commits or rolls back on its own and can never mark SEC's transaction rollback-only, leaving the token and audit rows intact — with `templateCode` identifying the reset message, `recipientId=userPk`, `moduleCode="SEC"`, and the recipient's `email` carried among the dispatch `variables` (alongside `token` / `expiresAt`) because NOTIF resolves the destination address from `variables.get("email")`, having no crossmodule contact-lookup for a bare recipientId → if not found: do nothing further (still returns the same generic 200) → return
Repository   : QR-SEC-003 · join NONE · transaction READ_WRITE
Security     : screen SEC_PWD_RESET · public — no permission required
Localization : generic confirmation message in ar + en
<!-- API:API-SEC-003:END -->

<!-- API:API-SEC-004:START traces=REQ-SEC-007,REQ-SEC-008,DBF-SEC-093,DBF-SEC-095,DBF-SEC-096,DBF-SEC-004 -->
### API-SEC-004 — complete password reset
Endpoint     : POST /api/v1/sec/auth/password-reset/complete   (pre-authentication)
Layers       : controller → `AuthController.completeReset` ; service → `PasswordResetService.complete`
Request      : body `{token, newPassword}`
Response     : 200 · confirmation `{message}`
Validations  : RULE-SEC-006 (full text: DATA-DOM §ENT-SEC-012) — token must be unexpired and unused (QR-SEC-038)
Errors       : `SEC-409-RESET-TOKEN-INVALID` (409)
Orchestration: validate token (QR-SEC-038) → hash newPassword → update user's passwordHash + mark token usedAt (QR-SEC-004) → append AuditLogEntry `PASSWORD_RESET_COMPLETED` → return
Repository   : QR-SEC-004, QR-SEC-038 · join NONE · transaction READ_WRITE
Security     : screen SEC_PWD_RESET · public — no permission required
Localization : `SEC-409-RESET-TOKEN-INVALID` message ar: "رابط إعادة التعيين غير صالح أو منتهي" / en: "This reset link is invalid or has expired"
<!-- API:API-SEC-004:END -->

<!-- API:API-SEC-018:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-032,DBF-SEC-033 -->
### API-SEC-018 — register module
Endpoint     : POST /api/v1/sec/registry/modules
Layers       : controller → `RegistryController.registerModule` ; service → `RegistryService.registerModule`
Request      : body `{code, nameAr, nameEn}`
Response     : 201 · `ModuleRegistryResponse`
Validations  : uniqueness of code (QR-SEC-035)
Errors       : `SEC-409-MODULE-DUP` (409)
Orchestration: validate → persist (QR-SEC-018) → return
Repository   : QR-SEC-018, QR-SEC-035 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE` — called by a registering module's own onboarding process, itself acting through an administrator-held credential; SEC mints no special "system" principal
Localization : nameAr/nameEn both required
<!-- API:API-SEC-018:END -->

<!-- API:API-SEC-019:START traces=REQ-SEC-017,REQ-SEC-018,DBF-SEC-040,DBF-SEC-041,DBF-SEC-042,DBF-SEC-043 -->
### API-SEC-019 — register screen
Endpoint     : POST /api/v1/sec/registry/screens
Layers       : controller → `RegistryController.registerScreen` ; service → `RegistryService.registerScreen`
Request      : body `{moduleCode, pageCode, nameAr, nameEn}`
Response     : 201 · `ScreenRegistryResponse`
Validations  : RULE-SEC-004 (full text: DATA-DOM §ENT-SEC-005) — module must already be registered (QR-SEC-036); uniqueness of pageCode (QR-SEC-036)
Errors       : `SEC-409-MODULE-NOT-REGISTERED` (409), `SEC-409-SCREEN-DUP` (409)
Orchestration: check RULE-SEC-004 + uniqueness (QR-SEC-036) → persist (QR-SEC-019) → return
Repository   : QR-SEC-019, QR-SEC-036 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE`
Localization : nameAr/nameEn both required
<!-- API:API-SEC-019:END -->

<!-- API:API-SEC-020:START traces=REQ-SEC-019,DBF-SEC-050,DBF-SEC-051,DBF-SEC-052,DBF-SEC-053,DBF-SEC-054 -->
### API-SEC-020 — register action
Endpoint     : POST /api/v1/sec/registry/actions
Layers       : controller → `RegistryController.registerAction` ; service → `RegistryService.registerAction`
Request      : body `{pageCode, actionCode, nameAr, nameEn}`
Response     : 201 · `ActionRegistryResponse` (includes server-derived `permissionCode`)
Validations  : screen must already be registered; uniqueness of the derived permissionCode (QR-SEC-037)
Errors       : `SEC-409-SCREEN-NOT-REGISTERED` (409), `SEC-409-ACTION-DUP` (409)
Orchestration: resolve screen by pageCode → derive `permissionCode = PERM_<pageCode>_<actionCode>` → validate uniqueness (QR-SEC-037) → persist (QR-SEC-020) → return
Repository   : QR-SEC-020, QR-SEC-037 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE`
Localization : nameAr/nameEn both required
<!-- API:API-SEC-020:END -->

<!-- API:API-SEC-024:START traces=REQ-SEC-026,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086,DBF-SEC-087,DBF-SEC-088,DBF-SEC-089 -->
### API-SEC-024 — export audit log
Endpoint     : GET /api/v1/sec/audit-log/export
Layers       : controller → `AuditLogController.export` ; service → `AuditLogService.export`
Request      : query params — same filter set as API-SEC-023 (eventTypeCode, actorUserId, occurredFrom/occurredTo), no paging (exports the full filtered set)
Response     : 200 · `Content-Type: text/csv` body, one row per matching AuditLogEntry, all fields
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load the full filtered set (QR-SEC-024, same filters as QR-SEC-023, unpaged) → serialize to CSV → return (REQ-SEC-026: "exactly the filtered entries' fields")
Repository   : QR-SEC-024 · join NONE · transaction READ_ONLY
Security     : screen SEC_AUDIT_LOG · permission `PERM_SEC_AUDIT_LOG_VIEW` (shares VIEW — export is not a separate mutation, SRS Access summary)
Localization : detailsAr/detailsEn both included as separate CSV columns
<!-- API:API-SEC-024:END -->
<!-- SUB:SVC-API-INT:END -->
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START traces=REQ-SEC-016 -->
## PHASE 4 — DOC

**API contract summary** (R4 — backend self-check only; the frontend stage binds to the
real generated api-docs published under `governance/modules/SEC/api-docs/` after
implementation — `index.md` plus `endpoints/<group-slug>.md` — never to this table):

| API | Path | Verb | Request DTO | Response DTO | Stability |
|---|---|---|---|---|---|
| API-SEC-001 | /auth/login | POST | LoginRequest | LoginResponse | v1 |
| API-SEC-002 | /auth/signup | POST | SignupSubmitRequest | SignupRequestResponse | v1 |
| API-SEC-003 | /auth/password-reset/request | POST | PasswordResetRequest | ConfirmationResponse | v1 |
| API-SEC-004 | /auth/password-reset/complete | POST | PasswordResetCompleteRequest | ConfirmationResponse | v1 |
| API-SEC-005 | /users/search | POST | UserSearchRequest | Page\<UserResponse\> | v1 |
| API-SEC-006 | /users | POST | UserCreateRequest | UserResponse | v1 |
| API-SEC-007 | /users/{id} | PUT | UserUpdateRequest | UserResponse | v1 |
| API-SEC-008 | /users/{id}/roles | PUT | UserRoleAssignmentRequest | UserResponse | v1 |
| API-SEC-009 | /users/{id} | DELETE | — | UserStatusResponse | v1 |
| API-SEC-010 | /users/{id} | PATCH | — | UserStatusResponse | v1 |
| API-SEC-011 | /signup-requests/{id} | PATCH | SignupDecisionRequest | UserResponse \| SignupRequestResponse | v1 |
| API-SEC-012 | /roles/search | POST | RoleSearchRequest | Page\<RoleResponse\> | v1 |
| API-SEC-013 | /roles | POST | RoleCreateRequest | RoleResponse | v1 |
| API-SEC-014 | /roles/{id}/modules | POST | RoleModuleGrantRequest | RoleModuleGrantResponse | v1 |
| API-SEC-015 | /roles/{id}/modules/{moduleId} | DELETE | — | ModuleGrantRevokeResponse | v1 |
| API-SEC-016 | /roles/{id}/screens | POST | RoleScreenGrantRequest | RoleScreenGrantResponse | v1 |
| API-SEC-017 | /roles/{id}/actions | POST | RoleActionGrantRequest | RoleActionGrantResponse | v1 |
| API-SEC-018 | /registry/modules | POST | ModuleRegistryCreateRequest | ModuleRegistryResponse | v1 |
| API-SEC-019 | /registry/screens | POST | ScreenRegistryCreateRequest | ScreenRegistryResponse | v1 |
| API-SEC-020 | /registry/actions | POST | ActionRegistryCreateRequest | ActionRegistryResponse | v1 |
| API-SEC-021 | /registry/search | POST | RegistrySearchRequest | Page\<RegistryRowResponse\> | v1 |
| API-SEC-022 | /dashboard | GET | — | DashboardResponse | v1 |
| API-SEC-023 | /audit-log/search | POST | AuditLogEntrySearchRequest | Page\<AuditLogEntryResponse\> | v1 |
| API-SEC-024 | /audit-log/export | GET | (query params) | text/csv | v1 |
| API-SEC-025 | /sessions/search | POST | ActiveSessionSearchRequest | Page\<ActiveSessionResponse\> | v1 |
| API-SEC-026 | /sessions/{id} | DELETE | — | SessionTerminationResponse | v1 |
| API-SEC-027 | /menu | GET | — | List\<ModuleMenuResponse\> | v1 |
(paths relative to `/api/v1/sec`)

**DTO typing constraints**: `statusCode`/`eventTypeCode`/`actionCode` are `String` holding
the coded value, never a Java enum (profile lookup rule); business code fields — not
applicable, no SEC entity has one; PK fields never appear in a create-request body.

**Pagination + filter standard**: request shape `{page, size, sortField, sortDirection, filters}`
— the shared `BaseSearchContractRequest` body every `*SearchRequest` extends — with the filter
fields named per screen (§Phase 1 CORE "Search contract"); an unrecognized `sortField` is rejected
(`SEC-400-INVALID-SORT`); an empty filtered result is `200` with empty `content`, never `404`.
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START traces=REQ-SEC-016 -->
## PHASE 5 — INT-C (cross-module consume)

No `XM-*` row exists for SEC (db-script-sec.md §2: "None — SEC is ROOT"). SEC consumes no
other module's entity, table or column and carries no cross-module FK — the XM REGISTER is a
schema-level statement and stays empty. This phase is intentionally near-empty, stated so per
engine §6.2 rather than omitted; no SUB is opened (XM count 0 < the split threshold of 5).

One in-process service call does cross the boundary, and it is sanctioned, not an omission:
`PasswordResetService.dispatchResetNotification` (API-SEC-003, `SUB:SVC-API-INT`) injects NOTIF's
`com.erp.notif.crossmodule.NotificationDispatchApi` and hands it the `DispatchCommand` read-model,
wrapped in `InternalCallerContext` because the caller is pre-authentication. That is REQ-SEC-029 —
srs-sec.md §A8's *External service* table, `SOFT / optional` — and it is not an `XM-*` row because
it registers no entity, table or column for db-script §2 to carry. Verified here against
build-create-service "Cross-Module Calls": only the `crossmodule` package is imported, the
reference is held by the service alone (never a Domain, mapper or controller), the argument is
NOTIF's own record, propagation intent is stated at the call site, and the failure is caught there
and logged — so REQ-SEC-006's generic 200 still answers when Notifications is unavailable.
Propagation is `REQUIRES_NEW`, declared on NOTIF's `dispatchIndependently` entry point: a failure
inside dispatch commits or rolls back on its own and never marks SEC's transaction rollback-only,
so the PasswordResetToken and audit rows still commit — execution-state.json gap #5 closed.

SEC's actual cross-module role runs in the opposite direction: every future module
*consumes* SEC through the plain REST surface documented in Phase 3 (registration:
API-SEC-018/019/020; authorization: the CORE interceptor + API-SEC-027) — that is standard
API consumption by another module's own P3.1, not an `XM-*` row inside SEC's own plan.
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START traces=REQ-SEC-016 -->
## PHASE 6 — INT-R (cross-module resolve)

No `XM-*` row to resolve — same basis as Phase 5. No SUB opened (0 < 5). Since 2026-09-11 this
phase also carries SEC's one EXPOSED inbound surface, `com.erp.sec.crossmodule` — both former
inbound gaps close here, and this is the inbound phase, so the surface is documented here rather
than in Phase 5 (which stays the consume direction: NOTIF's `NotificationDispatchApi`). The
surface registers no entity, table or column, so it still adds no `XM-*` row to db-script §2
(amended there to say so), and SEC still assigns no XM id of its own.

Inbound dependency stub (future consumers, not `TODO`): `XM-INBOUND-STUB-1` — any future
module (first expected: MDL, then FIN per GENERATION-INSTRUCTIONS.md §3) will register
itself via API-SEC-018/019/020 and consume identity/authorization via API-SEC-001/027 and
the CORE interceptor; the entity it reaches is `ENT-SEC-004` (ModuleRegistry) /
`ENT-SEC-005` (ScreenRegistry) / `ENT-SEC-006` (ActionRegistry); formal `XM-*` ids for that
direction are assigned by the *consuming* module's own P2, not by SEC.

Two inbound-contract gaps were found during execution and recorded here. **Both are now CLOSED
(2026-09-11)**, by the human-authorized amendment that gave SEC its first cross-module read
surface — a P1/P2 decision, taken deliberately and reflected back into P1 (REQ-SEC-034,
REQ-SEC-035, SRS §A8's third table) and P2 (§2 XM REGISTER), not an execution-time invention.

`XM-INBOUND-GAP-1` — **CLOSED. A consumer can now read the user ids holding a permission code.**
`SecUserDirectoryApi.findUserIdsHoldingPermission(String)` (REQ-SEC-035, QR-SEC-039) returns the
DISTINCT user ids reaching that permission code through an active role — the inverse of
QR-SEC-027, with the same active-flag predicates. The original finding stands as written: no SEC
*endpoint* returns a role's user set, and none was added. FIN's plan
(`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:383-386`) says the SoD check reads the
sets "through SEC's role/grant read APIs"; that wording is **superseded**, because
`build-create-service` requires cross-module reads to go through "direct Spring interface
injection, not loopback HTTP" — an injected interface, never an HTTP call between two modules of
one deployable. Correcting FIN's wording is FIN's own pass; `governance/modules/FIN/**` was not
modified here. The surface answers with user ids only: SEC neither learns nor evaluates FIN's
conflicting pair, so REQ-SEC-020's "inert in v1" Note is unaffected.

`XM-INBOUND-GAP-2` — **CLOSED on both halves.** *Delivery half*, closed earlier on 2026-09-11:
`PasswordResetService.dispatchResetNotification` now carries the resolved user's `email` among the
`DispatchCommand` variables, which is exactly the contract `DefaultChannelProvider` publishes
("NOTIF has no crossmodule contact-lookup for a bare recipientId"), so REQ-SEC-029's outbound half
delivers instead of writing `NOTIF_LOG FAILED — missing recipient email address`. *Durable half*,
closed now: `SecUserDirectoryApi.findContact(Long)` (REQ-SEC-034) returns a `UserContact` —
email, both display names, active — for a bare user id, so NOTIF can resolve a recipient without
SEC pushing the address, and can discharge its own `XM-NOTIF-001` whenever it chooses.
NOTIF's `DefaultRecipientStatusReader` stub was **deliberately left in place**: replacing it is
NOTIF's call in NOTIF's own pass, and nothing under `src/main/java/com/erp/notif/` or
`governance/modules/NOTIF/**` was touched from this SEC-driven pass.
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START traces=REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-020,REQ-SEC-023,REQ-SEC-030,REQ-SEC-033 -->
## PHASE 7 — SEC-BE (security, backend half)

One block per screen (R7); every API serving a screen verifies its permission via the CORE
interceptor (Phase 1) before its controller method body runs.

| Screen (page code) | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|
| SEC_LOGIN | public | — | — | — |
| SEC_SIGNUP | public | — | — | — |
| SEC_PWD_RESET | public | — | — | — |
| SEC_USERS | PERM_SEC_USERS_VIEW (API-SEC-005) | PERM_SEC_USERS_CREATE (API-SEC-006) | PERM_SEC_USERS_UPDATE (API-SEC-007, 008, 009, 010, 011) | — |
| SEC_ROLES | PERM_SEC_ROLES_VIEW (API-SEC-012) | PERM_SEC_ROLES_CREATE (API-SEC-013) | PERM_SEC_ROLES_UPDATE (API-SEC-014..017) | PERM_SEC_ROLES_DELETE (reserved — no delete-role endpoint in v1, deactivate only, which is UPDATE) |
| SEC_MODULE_REGISTRY | PERM_SEC_MODULE_REGISTRY_VIEW (API-SEC-021) | (via registering module's own call, API-SEC-018/019/020) | PERM_SEC_MODULE_REGISTRY_UPDATE (deactivate — no v1 endpoint yet, reserved) | — |
| SEC_DASHBOARD | PERM_SEC_DASHBOARD_VIEW (API-SEC-022) + per-widget source VIEW | — | — | — |
| SEC_AUDIT_LOG | PERM_SEC_AUDIT_LOG_VIEW (API-SEC-023, 024) | — | — | — |
| SEC_SESSIONS | PERM_SEC_SESSIONS_VIEW (API-SEC-025) | — | — | PERM_SEC_SESSIONS_DELETE (API-SEC-026) |

**Seed data** (one SEC_PAGES row per composite screen, one action row per action per §7.1):
9 page rows (SEC_LOGIN, SEC_SIGNUP, SEC_PWD_RESET, SEC_USERS, SEC_ROLES,
SEC_MODULE_REGISTRY, SEC_DASHBOARD, SEC_AUDIT_LOG, SEC_SESSIONS) inserted via
API-SEC-019/registered by SEC's own bootstrap (SEC registers itself into itself — the one
module for which no external caller is needed); action rows: VIEW/CREATE/UPDATE/DELETE per
secured screen above, per PERM_<PAGE_CODE>_<ACTION> (profile.conventions.security_model).
Column names come from the db-script (SEC_SCREEN_REG, SEC_ACTION_REG), not restated here.

**Gateway**: every non-VIEW permission requires VIEW on the same screen first
(RULE-SEC-007, enforced by the CORE interceptor + at grant time by API-SEC-017/QR-SEC-030).

**Forbidden responses**: any denial from the CORE interceptor maps to
`LocalizedException{code: "SEC-403-FORBIDDEN", messageAr: "غير مصرح بهذا الإجراء",
messageEn: "You are not authorized to perform this action"}` (§Error Catalog `SEC-403`).
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START traces=REQ-SEC-033 -->
## PHASE 8 — ALIGN-BE

See §Alignment self-check (ALIGN) below — its RESULT row is this phase's content, per
engine §6.1 R8 ("written as the phase content of the alignment-role phase").
<!-- PHASE:ALIGN-BE:END -->

## Error Catalog — SEC v1

Envelope: `LocalizedException → {code, messageAr, messageEn}`. Runtime code format: `SEC-<HTTP-status>-<SCENARIO>` (Phase 1 CORE).

| code | RULE / PLATFORM-STD | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| SEC-401-INVALID-CREDENTIALS | PLATFORM-STD (ADR-SEC-002, new — see below) | API-SEC-001 | 401 | wrong/unknown credentials | بيانات الدخول غير صحيحة | Invalid credentials |
| SEC-409-USER-DUP | PLATFORM-STD (uniqueness) | API-SEC-006, 007 | 409 | duplicate username/email | اسم المستخدم أو البريد الإلكتروني مستخدم بالفعل | Username or email already in use |
| SEC-404-USER | PLATFORM-STD (not found) | API-SEC-007, 008, 009, 010 | 404 | unknown user id | المستخدم غير موجود | User not found |
| SEC-409-SOD-CONFLICT | RULE-SEC-005 | API-SEC-008, 017 | 409 | conflicting action pair on one user | هذا المستخدم يملك إجراءً متعارضًا بالفعل | This user already holds a conflicting action |
| SEC-404-ROLE | PLATFORM-STD (not found) | API-SEC-008, 014, 016, 017 | 404 | unknown role id | الدور غير موجود | Role not found |
| SEC-409-INVALID-TRANSITION | PLATFORM-STD (lifecycle) | API-SEC-010, 011 | 409 | transition not allowed from current status | لا يمكن تنفيذ هذا الانتقال من الحالة الحالية | This transition is not allowed from the current status |
| SEC-404-SIGNUP | PLATFORM-STD (not found) | API-SEC-011 | 404 | unknown signup request id | طلب التسجيل غير موجود | Signup request not found |
| SEC-409-ROLE-DUP | PLATFORM-STD (uniqueness) | API-SEC-013 | 409 | duplicate role code | رمز الدور مستخدم بالفعل | Role code already in use |
| SEC-409-GRANT-DUP | PLATFORM-STD (uniqueness) | API-SEC-014, 016, 017 | 409 | grant already exists | هذا المنح موجود بالفعل | This grant already exists |
| SEC-404-MODULE | PLATFORM-STD (not found) | API-SEC-014, 015 | 404 | unknown module id | الوحدة غير موجودة | Module not found |
| SEC-404-GRANT | PLATFORM-STD (not found) | API-SEC-015 | 404 | grant does not exist | المنح غير موجود | Grant not found |
| SEC-409-NO-MODULE-GRANT | RULE-SEC-001 | API-SEC-016 | 409 | screen grant attempted without module grant | لا يمكن منح شاشة دون منح الوحدة أولًا | Cannot grant a screen without first granting its module |
| SEC-404-SCREEN | PLATFORM-STD (not found) | API-SEC-016, 017, 019 | 404 | unknown screen id / page code | الشاشة غير موجودة | Screen not found |
| SEC-409-NO-SCREEN-GRANT | RULE-SEC-002 | API-SEC-017 | 409 | action grant attempted without screen grant | لا يمكن منح إجراء دون منح الشاشة أولًا | Cannot grant an action without first granting its screen |
| SEC-409-NO-VIEW-GRANT | RULE-SEC-007 | API-SEC-017 | 409 | non-VIEW action grant attempted without VIEW | يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة | The VIEW action must be granted on this screen first |
| SEC-404-ACTION | PLATFORM-STD (not found) | API-SEC-017 | 404 | unknown action id | الإجراء غير موجود | Action not found |
| SEC-409-MODULE-DUP | PLATFORM-STD (uniqueness) | API-SEC-018 | 409 | duplicate module code | رمز الوحدة مستخدم بالفعل | Module code already in use |
| SEC-409-MODULE-NOT-REGISTERED | RULE-SEC-004 | API-SEC-019 | 409 | screen registered under an unregistered module | الوحدة غير مسجّلة | Module is not registered |
| SEC-409-SCREEN-DUP | PLATFORM-STD (uniqueness) | API-SEC-019 | 409 | duplicate page code | رمز الصفحة مستخدم بالفعل | Page code already in use |
| SEC-409-SCREEN-NOT-REGISTERED | PLATFORM-STD (referential) | API-SEC-020 | 409 | action registered under an unregistered screen | الشاشة غير مسجّلة | Screen is not registered |
| SEC-409-ACTION-DUP | PLATFORM-STD (uniqueness) | API-SEC-020 | 409 | duplicate permission code | رمز الصلاحية مستخدم بالفعل | Permission code already in use |
| SEC-409-RESET-TOKEN-INVALID | RULE-SEC-006 | API-SEC-004 | 409 | expired or used reset token | رابط إعادة التعيين غير صالح أو منتهي | This reset link is invalid or has expired |
| SEC-409-SIGNUP-DUP | PLATFORM-STD (uniqueness) | API-SEC-002 | 409 | duplicate pending/registered email | البريد الإلكتروني مستخدم بالفعل | This email is already in use |
| SEC-404-SESSION | PLATFORM-STD (not found) | API-SEC-026 | 404 | unknown session id | الجلسة غير موجودة | Session not found |
| SEC-409-ALREADY-TERMINATED | PLATFORM-STD (lifecycle) | API-SEC-026 | 409 | session already terminated | هذه الجلسة منتهية بالفعل | This session is already terminated |
| SEC-403-FORBIDDEN | PLATFORM-STD (RULE-SEC-007 + REQ-SEC-033, CORE interceptor) | every secured API | 403 | missing module/screen/action grant | غير مصرح بهذا الإجراء | You are not authorized to perform this action |
| SEC-400-INVALID-SORT | PLATFORM-STD (search contract) | every search API | 400 | unrecognized `sort` field | حقل الترتيب غير معروف | Unrecognized sort field |
| INTERNAL_ERROR | PLATFORM-STD (infrastructure, shared GlobalExceptionHandler — not module-scoped) | any | 500 | unhandled server error | حدث خطأ غير متوقع. يرجى المحاولة لاحقاً. | An unexpected error occurred. Please try again later. |

`SEC-401-INVALID-CREDENTIALS` and every other PLATFORM-STD row is a standard infrastructure
error (not-found / conflict / server / forbidden), not sourced from a specific SRS RULE — per
engine §7 these carry `RULE = PLATFORM-STD` and are covered by a single umbrella note rather
than 20 near-identical ADRs: **ADR-SEC-002** (below) records this once for the whole catalog.

## ADR-SEC-002 (new this stage)
See `erp/decisions/SEC/ADR-SEC-002.md` — PLATFORM-STD error rows (not-found, duplicate,
invalid-transition, forbidden, server, invalid-sort) are standard REST/CRUD infrastructure
errors implied by the SRS's own conventions (unique constraints, status lifecycles, the
module/screen/action gate) rather than restated as individual RULE-* ids; non-breaking.

## Alignment self-check (ALIGN) — SEC v1

```
TRACEABILITY      ✓ every API-*/QR-*/RULE-*/DBF-* used in a phase appears in the Plan Index; every PHASE/SUB/atom carries traces=; every traces target exists upstream (REQ-SEC-001..035, DBF-SEC-001..104 all defined in srs/db-script)
BINDING (§2A)     ⚠ no placeholder table/column/key object; every column cites a DBF (Field Registry + per-entity FIELDS tables); every RULE message present in ar+en; business code: none applicable (stated, not silently skipped). Two things this row did not catch: a real column-name mismatch (ENT-SEC-007's `grant_at` vs db-script's `granted_at`, DBF-SEC-064 — api_doc_gaps #2, since corrected), and the generation object it certifies is NOT what is built — the module ships 13 `SEQ_SEC_*` sequences + `GenerationType.SEQUENCE` per GOVERNANCE-RULES.md §Convention Precedence 1, while 13 BINDINGS lines and the Phase 1 type table still name `GENERATED ALWAYS AS IDENTITY`
MANIFEST (§4)     ✓ only the 4 mandated columns beyond DBF/ENT (property, type — status/XM added per engine format); all 104 DBF of every bound table listed; 0 ⏸ rows (0 XM)
QRC (§5)          ⚠ every API with a DB operation has ≥1 QR — re-verified, all 27 API atoms cite one; every QR entry carries the "logical spec, not code" framing (catalog header); no join for a lookup label. But the exact generation object named (`GENERATED ALWAYS AS IDENTITY`, Phase 1 CORE type table) is superseded as above. Separately, QR-SEC-025's declared query was dead code: `ActiveSessionRepository.findNonTerminated(Pageable)` had no caller after API-SEC-025 moved to a SpecBuilder-driven POST /search (A.2.9). CLOSED 2026-09-11: the plan declares `join NONE` for QR-SEC-025 (plan:792) and this catalog already registers it as FIND_BY_CRITERIA (plan:376), so the plan never required the JOIN FETCH — the orphan method was deleted and A.2.9 now holds across all 37 methods the module's 13 repositories declare
API (R3)          ⚠ every RULE in a Validations line has a catalog row (28 catalog rows = 27 module-owned rows, one per SecErrorCodes constant, all present in messages.properties AND messages_ar.properties, plus the 1 platform row INTERNAL_ERROR the shared GlobalExceptionHandler owns and SEC does not declare); platform errors carry RULE=PLATFORM-STD + ADR-SEC-002; create/update requests exclude PK/audit/system fields (DTO MEMBERSHIP, Phase 2; Request lines, Phase 3) — verified in code. But the check only tests field EXCLUSION, never whether a field an API NAMES is DEFINED, which is why it passed API-SEC-011 with the approved account's credential unspecified (api_doc_gaps #4) and API-SEC-022 with three Response figures undefined (api_doc_gaps #6). Separately `SEC-500`, named by 8 API blocks (not 9) as their only error, was never emitted — the shared GlobalExceptionHandler answers `INTERNAL_ERROR` and SecErrorCodes.SEC_500 had zero references. CLOSED 2026-09-11: those 8 Errors lines and the catalog row now name `INTERNAL_ERROR`, and `SecErrorCodes.SEC_500` plus both `SEC-500=` bundle keys were removed
CROSS-MODULE      ⚠ 0 XM rows from db-script, 0 placed, 0 mismatched — still exact, but only for the CONSUME direction and only at the SCHEMA level (db-script §2 XM REGISTER, as amended 2026-09-11: "None in the CONSUME direction"; no consumed entity, table or FK); inbound stub uses XM-INBOUND-STUB-1 notation, not TODO. At the API level SEC is not isolated in EITHER direction. CONSUMES: PasswordResetService injects `com.erp.notif.crossmodule.NotificationDispatchApi` (INT-C — srs-sec.md §A8's one declared SOFT integration, the module's only non-sec/non-common import). EXPOSES, new 2026-09-11: `com.erp.sec.crossmodule.SecUserDirectoryApi` — `findContact` (REQ-SEC-034) and `findUserIdsHoldingPermission` (REQ-SEC-035, QR-SEC-039) — implemented by `SecUserDirectoryApiImpl` delegating to `UserService`, injecting no repository, returning only the `UserContact` read-model and a list of ids; it registers no entity, table or column, so the XM row count legitimately stays 0 and the CONSUMING module's own P2 assigns any XM id for that direction. Both inbound gaps INT-R recorded are therefore CLOSED (this row previously read "two inbound contracts SEC does not satisfy"): XM-INBOUND-GAP-1 by `findUserIdsHoldingPermission`, XM-INBOUND-GAP-2 on both halves (`email` among the dispatch variables, plus `findContact`). MODULE-LEVEL CYCLE — stated here rather than discovered later: SEC already consumes NOTIF's `NotificationDispatchApi`, so the moment NOTIF adopts `SecUserDirectoryApi` the two modules point at each other at the MODULE level. That is NOT the circular dependency build-create-service forbids — neither crossmodule interface calls the other, and the two paths are independent (SEC→NOTIF at reset-token issuance; NOTIF→SEC at delivery, to resolve a bare recipientId) — but it is real, and must be weighed before any further surface is added in either direction
SECURITY (R7)     ⚠ every secured screen has a Phase 7 seed row — now checkable and true: all 9 page codes and all 13 permission codes the Phase 7 matrix names are seeded by V17__sec_security_seed.sql. Every secured API declares its PERM_* with two stated exceptions: API-SEC-027 has no page code of its own (SRS B4) and is gated `isAuthenticated()`, and API-SEC-001..004 are public by contract — 22 of 27 Security lines name a PERM_*, matching exactly 22 PERM_-based @PreAuthorize in com.erp.sec.service. profile.review.extra_checks ERP-4 ("every mutation endpoint declares its PERM_*") is FALSE as written: API-SEC-002, API-SEC-003 and API-SEC-004 are POST mutations writing SEC_SIGNUP_REQUEST / SEC_PWD_RESET_TOKEN / SEC_USER rows and state "public — no permission required". One seeded code, PERM_SEC_ROLES_DELETE, has no PermissionConstants constant (Phase 7 marks it reserved)
CORE (R1)         ✓ layers declared, domain placement declared (entity methods for single-entity, service for multi-row), error signalling declared (code format `SEC-<HTTP-status>-<SCENARIO>`, verified against all 27 SecErrorCodes values), type mapping declared (postgresql16 → Java table) — holds only after api_doc_gaps #1: as generated this row certified a `SEC-<3-digit-sequence>` format matching no catalog row and no emitted code
DECISIONS         ✗ ADR-SEC-001 (lookup centralization deferral) and ADR-SEC-002 (PLATFORM-STD catalog umbrella) are cited as ACCEPTED at `erp/decisions/SEC/ADR-SEC-00N.md` throughout P2/P3.1 and in modules/project-registry.md — NEITHER FILE EXISTS anywhere in this repository and there is no decisions/ directory (`find . -iname 'ADR-SEC-*'` returns nothing). Both decisions are stated inline in the artifacts citing them and nowhere else, so ACCEPTED is unevidenced. Correct on its own terms: no BLOCKED ADR
RESULT            FAILED ✗ — 11 findings, not 0, when this block was re-run at ALIGN-BE. Current state of governance/modules/SEC/execution-state.json, re-derived 2026-09-11 by reading all 13 `api_doc_gaps[]` resolution fields: 13 entries, re-derived again 2026-09-11 after the api-docs closure — 9 CLOSED (#1 and #2 by realignment; #5 both halves — the internal-caller authorization half, and the transaction-propagation half via NOTIF's `NotificationDispatchApi.dispatchIndependently` REQUIRES_NEW crossmodule entry point; #10 the dead QR-SEC-025 query, #11 the unreachable SEC-500 code, #12 the five stale GET rows in _SECTIONS.md's EXECUTION PLAN INDEX API table; plus, on the human-authorized amendment, #8 XM-INBOUND-GAP-1 — closed by `SecUserDirectoryApi.findUserIdsHoldingPermission` (REQ-SEC-035, QR-SEC-039), a crossmodule Spring interface and deliberately NOT a REST endpoint, since build-create-service requires "direct Spring interface injection, not loopback HTTP" — and #9 XM-INBOUND-GAP-2 on both halves, the delivery half by carrying `email` among the dispatch variables and the durable half by `SecUserDirectoryApi.findContact` (REQ-SEC-034); and #7 the missing producer for the published api-docs, closed by the per-module `GroupedOpenApi` beans in `src/main/java/com/erp/main/config/OpenApiConfig.java` plus an actual generator run, which wrote `index.md` and ten `endpoints/<group-slug>.md` files under `governance/modules/SEC/api-docs/`, and by striking `api-docs-sec.md` from the DOC-phase paragraph in favour of that layout) · 3 answered by an implementation choice with a human-only P1/P2 question left over (#3 RULE-SEC-005's absent conflicting-pair source, #4 the approved sign-up's credential, #6 the three undefined dashboard figures) · 0 partially closed · 1 open and needing a decision outside SEC (#13 the reset mail's unspecified `lang` and `actionLink`). Owed elsewhere, deliberately not written from this SEC-driven pass: NOTIF's own governance artifacts still need a matching cross-module contract entry for `dispatchIndependently`, and its `DefaultRecipientStatusReader` stub is NOTIF's to replace over `SecUserDirectoryApi` (governance/modules/NOTIF/** and src/main/java/com/erp/notif/** both left untouched); FIN's plan (`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:383-386`) still describes the SoD read as going "through SEC's role/grant read APIs" — superseded wording that FIN's own pass must correct (governance/modules/FIN/** left untouched). Four of the ten rows above are only partly true and one is false. Master validation: the last full pass against the implemented module scored 122/133 applicable checks (91.7%), verdict CONDITIONAL, with 9 labelled deviations — recorded 2026-09-11 BEFORE the A.2.9 fix, and deliberately NOT re-derived at this closure. A.2.9 now passes where it then failed and its automatic-rejection trigger no longer fires; against that, API-SEC-025's page no longer uses a fetch join, so A.2.6 would have to be re-judged. No new score is asserted here — deriving one honestly means re-running the full 148-check pass. Per-check evidence, the N/A reasons and every deviation's justification: governance/project-artifacts/sec-alignment-report.md
```

**Coverage — ENT/DBF → phases → QR → XM**: every ENT-SEC-001..013 appears in exactly one
DATA-DOM entity block (SUB-MASTER or SUB-TRANSACTIONAL) with ≥1 QR cited under REPOSITORY
OPS; every DBF-SEC-001..104 appears in the DB Alignment Manifest and its owning entity's
FIELDS table; 0 XM (n/a).

**Coverage — RULE → API → catalog code**: RULE-SEC-001→API-SEC-016→SEC-409-NO-MODULE-GRANT ·
RULE-SEC-002→API-SEC-017→SEC-409-NO-SCREEN-GRANT · RULE-SEC-003→API-SEC-015→(no dedicated
code — success path with a count in the response; failure paths are SEC-404-GRANT) ·
RULE-SEC-004→API-SEC-019→SEC-409-MODULE-NOT-REGISTERED ·
RULE-SEC-005→API-SEC-008,017→SEC-409-SOD-CONFLICT · RULE-SEC-006→API-SEC-004→
SEC-409-RESET-TOKEN-INVALID · RULE-SEC-007→API-SEC-017→SEC-409-NO-VIEW-GRANT (+ the
CORE interceptor at runtime → SEC-403-FORBIDDEN).

**Coverage — XM → status → blocks → workaround**: not applicable (0 XM).

## QR id definitions (cross-reference index — full detail in Query Reference Catalog above)
**QR-SEC-001** — FIND_ONE user by username [ENT-SEC-001, API-SEC-001]
**QR-SEC-002** — SAVE signup request [ENT-SEC-013, API-SEC-002]
**QR-SEC-003** — SAVE password reset token [ENT-SEC-012, API-SEC-003]
**QR-SEC-004** — UPDATE user password + mark token used [ENT-SEC-001, ENT-SEC-012, API-SEC-004]
**QR-SEC-005** — FIND_BY_CRITERIA search users [ENT-SEC-001, API-SEC-005]
**QR-SEC-006** — SAVE create user [ENT-SEC-001, API-SEC-006]
**QR-SEC-007** — UPDATE user profile fields [ENT-SEC-001, API-SEC-007]
**QR-SEC-008** — SAVE assign roles to user [ENT-SEC-003, API-SEC-008]
**QR-SEC-009** — UPDATE deactivate user + terminate sessions [ENT-SEC-001, ENT-SEC-010, API-SEC-009]
**QR-SEC-010** — UPDATE reactivate user [ENT-SEC-001, API-SEC-010]
**QR-SEC-011** — UPDATE approve/reject signup [ENT-SEC-013, ENT-SEC-001, API-SEC-011]
**QR-SEC-012** — FIND_BY_CRITERIA search roles [ENT-SEC-002, API-SEC-012]
**QR-SEC-013** — SAVE create role [ENT-SEC-002, API-SEC-013]
**QR-SEC-014** — SAVE grant module [ENT-SEC-007, API-SEC-014]
**QR-SEC-015** — DELETE revoke module grant [ENT-SEC-007, API-SEC-015]
**QR-SEC-016** — SAVE grant screen [ENT-SEC-008, API-SEC-016]
**QR-SEC-017** — SAVE grant action [ENT-SEC-009, API-SEC-017]
**QR-SEC-018** — SAVE register module [ENT-SEC-004, API-SEC-018]
**QR-SEC-019** — SAVE register screen [ENT-SEC-005, API-SEC-019]
**QR-SEC-020** — SAVE register action [ENT-SEC-006, API-SEC-020]
**QR-SEC-021** — FIND_BY_CRITERIA browse registry tree [ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, API-SEC-021]
**QR-SEC-022** — AGGREGATE dashboard figures [ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-002, API-SEC-022]
**QR-SEC-023** — FIND_BY_CRITERIA search audit log [ENT-SEC-011, API-SEC-023]
**QR-SEC-024** — FIND_BY_CRITERIA audit log export [ENT-SEC-011, API-SEC-024]
**QR-SEC-025** — FIND_BY_CRITERIA non-terminated sessions [ENT-SEC-010, API-SEC-025]
**QR-SEC-026** — UPDATE terminate session [ENT-SEC-010, API-SEC-026]
**QR-SEC-027** — FIND_BY_CRITERIA effective menu tree [ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008, API-SEC-027]
**QR-SEC-028** — EXISTS role holds module grant (RULE-SEC-001) [ENT-SEC-007, API-SEC-016]
**QR-SEC-029** — EXISTS role holds screen grant (RULE-SEC-002) [ENT-SEC-008, API-SEC-017]
**QR-SEC-030** — EXISTS role holds VIEW on screen (RULE-SEC-007) [ENT-SEC-009, API-SEC-017]
**QR-SEC-031** — EXISTS conflicting action already held (RULE-SEC-005) [ENT-SEC-009, API-SEC-008, API-SEC-017]
**QR-SEC-032** — FIND_ALL dependent screen/action grants for cascade (RULE-SEC-003) [ENT-SEC-008, ENT-SEC-009, API-SEC-015]
**QR-SEC-033** — EXISTS uniqueness username/email [ENT-SEC-001, API-SEC-006]
**QR-SEC-034** — EXISTS uniqueness role code [ENT-SEC-002, API-SEC-013]
**QR-SEC-035** — EXISTS uniqueness module code [ENT-SEC-004, API-SEC-018]
**QR-SEC-036** — EXISTS module registered + uniqueness page code (RULE-SEC-004) [ENT-SEC-004, ENT-SEC-005, API-SEC-019]
**QR-SEC-037** — EXISTS screen exists + uniqueness permission code [ENT-SEC-005, ENT-SEC-006, API-SEC-020]
**QR-SEC-038** — EXISTS reset token unexpired and unused (RULE-SEC-006) [ENT-SEC-012, API-SEC-004]
**QR-SEC-039** — FIND_ALL DISTINCT user ids holding a permission code through an active role (REQ-SEC-035) [ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009, no API — `SecUserDirectoryApi`]

## Registry content
See `registry-exec-be-sec.md`.
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: registry-srs>>>
## REGISTRY — P1 — SEC v1
══════════════════════════════════════════════════════════════════

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|
| ENT-SEC-001 | المستخدم / User | security | SHARED (owner) | REGISTERED |
| ENT-SEC-002 | الدور / Role | security | PRIVATE | REGISTERED |
| ENT-SEC-003 | ربط المستخدم بالدور / UserRoleAssignment | security | PRIVATE | REGISTERED |
| ENT-SEC-004 | سجل الوحدات / ModuleRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-005 | سجل الشاشات / ScreenRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-006 | سجل الإجراءات / ActionRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-007 | منح الوحدة للدور / RoleModuleGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-008 | منح الشاشة للدور / RoleScreenGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-009 | منح الإجراء للدور / RoleActionGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-010 | الجلسة النشطة / ActiveSession | security | PRIVATE | REGISTERED |
| ENT-SEC-011 | سجل التدقيق / AuditLogEntry | security | PRIVATE | REGISTERED |
| ENT-SEC-012 | رمز إعادة تعيين كلمة المرور / PasswordResetToken | security | PRIVATE | REGISTERED |
| ENT-SEC-013 | طلب تسجيل معلّق / SignupRequest | security | PRIVATE | REGISTERED |

Consumed
none — SEC is ROOT (→ dependency index: no row this module).

Lookups owned
| Key | ENT | Values count |
|---|---|---|
| USER_STATUS | ENT-SEC-001 | 3 |
| SIGNUP_STATUS | ENT-SEC-013 | 3 |
| AUDIT_EVENT_TYPE | ENT-SEC-011 | 14 |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code |
|---|---|---|
| SCR-REQ-SEC-001 | تسجيل الدخول / Login | SEC_LOGIN |
| SCR-REQ-SEC-002 | التسجيل الذاتي / Sign-up | SEC_SIGNUP |
| SCR-REQ-SEC-003 | نسيت/إعادة تعيين كلمة المرور / Forgot/reset password | SEC_PWD_RESET |
| SCR-REQ-SEC-004 | المستخدمون / Users | SEC_USERS |
| SCR-REQ-SEC-005 | الأدوار والصلاحيات / Roles & permissions | SEC_ROLES |
| SCR-REQ-SEC-006 | سجل الوحدة/الشاشة/الإجراء / Module/screen/action registry | SEC_MODULE_REGISTRY |
| SCR-REQ-SEC-007 | لوحة تحكم الأمان / Admin dashboard | SEC_DASHBOARD |
| SCR-REQ-SEC-008 | سجل التدقيق / Audit log | SEC_AUDIT_LOG |
| SCR-REQ-SEC-009 | إدارة الجلسات النشطة / Active sessions management | SEC_SESSIONS |
| SCR-REQ-SEC-010 | القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu | (no page code — global component) |

Requirements
REQ count: 33 · AC count: 33 · RULE count: 7 · ENT count: 13 · SCR-REQ count: 10
Last sequence per atom: REQ: 033 · AC: 033 · ENT: 013 · RULE: 007 · SCR-REQ: 010

REQ ids (full text in srs-sec.md → A4): REQ-SEC-001, REQ-SEC-002, REQ-SEC-003,
REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-009,
REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015,
REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019, REQ-SEC-020, REQ-SEC-021,
REQ-SEC-022, REQ-SEC-023, REQ-SEC-024, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027,
REQ-SEC-028, REQ-SEC-029, REQ-SEC-030, REQ-SEC-031, REQ-SEC-032, REQ-SEC-033

AC ids (full text in srs-sec.md → A4, one per REQ above): AC-SEC-001, AC-SEC-002,
AC-SEC-003, AC-SEC-004, AC-SEC-005, AC-SEC-006, AC-SEC-007, AC-SEC-008, AC-SEC-009,
AC-SEC-010, AC-SEC-011, AC-SEC-012, AC-SEC-013, AC-SEC-014, AC-SEC-015, AC-SEC-016,
AC-SEC-017, AC-SEC-018, AC-SEC-019, AC-SEC-020, AC-SEC-021, AC-SEC-022, AC-SEC-023,
AC-SEC-024, AC-SEC-025, AC-SEC-026, AC-SEC-027, AC-SEC-028, AC-SEC-029, AC-SEC-030,
AC-SEC-031, AC-SEC-032, AC-SEC-033

RULE ids (full text in srs-sec.md → A5): RULE-SEC-001, RULE-SEC-002, RULE-SEC-003,
RULE-SEC-004, RULE-SEC-005, RULE-SEC-006, RULE-SEC-007

Decisions
ADR ids: none (no ADR raised — §9 ambiguity fork was never reached this stage).

Event
"P1 completed: SEC v1 — 13 entities, 33 requirements, 33 acceptance criteria, 7 rules, 10 screen requirements, 0 ADRs"
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: registry-db>>>
## REGISTRY — P2 — SEC v1
══════════════════════════════════════════════════════════════════

Tables
| Table | ENT id | Kind | DBF range |
|---|---|---|---|
| SEC_USER | ENT-SEC-001 | security | DBF-SEC-001 … DBF-SEC-013 |
| SEC_ROLE | ENT-SEC-002 | security | DBF-SEC-014 … DBF-SEC-024 |
| SEC_USER_ROLE | ENT-SEC-003 | security | DBF-SEC-025 … DBF-SEC-029 |
| SEC_MODULE_REG | ENT-SEC-004 | security | DBF-SEC-030 … DBF-SEC-038 |
| SEC_SCREEN_REG | ENT-SEC-005 | security | DBF-SEC-039 … DBF-SEC-048 |
| SEC_ACTION_REG | ENT-SEC-006 | security | DBF-SEC-049 … DBF-SEC-059 |
| SEC_ROLE_MODULE_GRANT | ENT-SEC-007 | security | DBF-SEC-060 … DBF-SEC-064 |
| SEC_ROLE_SCREEN_GRANT | ENT-SEC-008 | security | DBF-SEC-065 … DBF-SEC-069 |
| SEC_ROLE_ACTION_GRANT | ENT-SEC-009 | security | DBF-SEC-070 … DBF-SEC-074 |
| SEC_ACTIVE_SESSION | ENT-SEC-010 | security | DBF-SEC-075 … DBF-SEC-082 |
| SEC_AUDIT_LOG | ENT-SEC-011 | security | DBF-SEC-083 … DBF-SEC-090 |
| SEC_PWD_RESET_TOKEN | ENT-SEC-012 | security | DBF-SEC-091 … DBF-SEC-096 |
| SEC_SIGNUP_REQUEST | ENT-SEC-013 | security | DBF-SEC-097 … DBF-SEC-104 |

DBF ids (full detail in db-script-sec.md → §1): DBF-SEC-001, DBF-SEC-002, DBF-SEC-003,
DBF-SEC-004, DBF-SEC-005, DBF-SEC-006, DBF-SEC-007, DBF-SEC-008, DBF-SEC-009, DBF-SEC-010,
DBF-SEC-011, DBF-SEC-012, DBF-SEC-013, DBF-SEC-014, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017,
DBF-SEC-018, DBF-SEC-019, DBF-SEC-020, DBF-SEC-021, DBF-SEC-022, DBF-SEC-023, DBF-SEC-024,
DBF-SEC-025, DBF-SEC-026, DBF-SEC-027, DBF-SEC-028, DBF-SEC-029, DBF-SEC-030, DBF-SEC-031,
DBF-SEC-032, DBF-SEC-033, DBF-SEC-034, DBF-SEC-035, DBF-SEC-036, DBF-SEC-037, DBF-SEC-038,
DBF-SEC-039, DBF-SEC-040, DBF-SEC-041, DBF-SEC-042, DBF-SEC-043, DBF-SEC-044, DBF-SEC-045,
DBF-SEC-046, DBF-SEC-047, DBF-SEC-048, DBF-SEC-049, DBF-SEC-050, DBF-SEC-051, DBF-SEC-052,
DBF-SEC-053, DBF-SEC-054, DBF-SEC-055, DBF-SEC-056, DBF-SEC-057, DBF-SEC-058, DBF-SEC-059,
DBF-SEC-060, DBF-SEC-061, DBF-SEC-062, DBF-SEC-063, DBF-SEC-064, DBF-SEC-065, DBF-SEC-066,
DBF-SEC-067, DBF-SEC-068, DBF-SEC-069, DBF-SEC-070, DBF-SEC-071, DBF-SEC-072, DBF-SEC-073,
DBF-SEC-074, DBF-SEC-075, DBF-SEC-076, DBF-SEC-077, DBF-SEC-078, DBF-SEC-079, DBF-SEC-080,
DBF-SEC-081, DBF-SEC-082, DBF-SEC-083, DBF-SEC-084, DBF-SEC-085, DBF-SEC-086, DBF-SEC-087,
DBF-SEC-088, DBF-SEC-089, DBF-SEC-090, DBF-SEC-091, DBF-SEC-092, DBF-SEC-093, DBF-SEC-094,
DBF-SEC-095, DBF-SEC-096, DBF-SEC-097, DBF-SEC-098, DBF-SEC-099, DBF-SEC-100, DBF-SEC-101,
DBF-SEC-102, DBF-SEC-103, DBF-SEC-104

XM index
none — SEC is ROOT (→ dependency index: no row this module).

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| USER_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| SIGNUP_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| AUDIT_EVENT_TYPE | 14 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |

Sequences
Last DBF: DBF-SEC-104 · Last XM: none assigned (0 rows)

Decisions
ADR-SEC-001 (ACCEPTED, non-breaking) — see erp/decisions/SEC/ADR-SEC-001.md

Event
"P2 completed: SEC v1 — 13 tables, 104 DBF, 0 XM"

Cascade
No registry XM row anywhere in the platform currently targets SEC with status DEFERRED
(SEC is the first module through this pipeline this batch) — nothing to resolve.
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
