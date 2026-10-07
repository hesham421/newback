# PASS 1 — module MDL v1 — bundled session (5 stages, one commit per stage)

- `P0` Platform Inception — questions allowed
- `P0.5` PRD — questions allowed
- `P1` SRS — questions forbidden — **blocked until human approval of `prd-approval`**
- `P2` Database — questions forbidden
- `P3.1` Backend Execution Plan — questions forbidden

==============================================================================
# BRIEF — stage `P0` (Platform Inception) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **allowed**. Close every open point inside this dialogue with a researched, recommended answer; never write an external open-questions file.
- Owns IDs: POL — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/MDL/P0/platform-summary.md`
- `erp/modules/MDL/P0/module-registry-mdl.md`
- `erp/modules/MDL/P0/business-policies-mdl.md`
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Dialogue protocol (converging, in-brief)
Implementers claude:opus alternate for at most 4 rounds; converge on **mutually-acceptable**.
Round 1 drafts the artifacts and, for every open point, a `PROPOSAL:` block (options, researched recommendation, sources).
Each later round answers every open PROPOSAL (accept / amend with reason), refines the artifacts, and appends `<!-- CONVERGED -->` at the end of the response when nothing material remains open. The last response is final.

## Contracts checked by `gov.py analyze` after this stage
- **C2** project registry → inception: C2.1 exists {'artifact': 'project-registry'} [CRITICAL]; C2.2 registry-agree {'registry': 'project-registry', 'categories': 'all'} [MAJOR]; C2.3 ids-owned {'artifact': 'project-registry', 'defines': []} [MAJOR]
- **C3** inception → PRD: C3.1 exists {'artifact': 'platform-summary'} [CRITICAL]; C3.2 exists {'artifact': 'module-registry'} [CRITICAL]; C3.3 exists {'artifact': 'business-policies'} [CRITICAL]; C3.4 ids-owned {'stage': 'P0'} [CRITICAL]; C3.5 no-questions {'stage': 'P0'} [CRITICAL]; C3.6 languages {'stage': 'P0'} [MAJOR]; C3.7 registry-agree {'artifact': 'business-policies', 'registry': 'module-registry', 'kinds': ['POL']} [MAJOR]

---
# ENGINE
# Platform Inception — ENGINE

```
Engine        : Platform Inception
Stage id      : P0
Pass          : 1
Questions     : allowed — resolved in-dialogue with recommended answers; user confirms
Dialogue      : yes — lane analysis (claude:opus; ≤ 4 rounds; converge on "mutually-acceptable"; output: resolved-decisions)
Inputs        : domain-profile, project-registry
Produces      : platform-summary.md · module-registry-{mod}.md · business-policies-{mod}.md
Owns IDs      : POL   → `{prefix}-{MOD}-{seq}` (seq width 3)
Next          : P0.5
Module        : MDL   Version: 1
Profile       : erp — ERP Platform
```

This engine turns free-form vision text into a closed architectural context: first a
**platform summary** (tiered module table, dependency map), then — per module — a
**module registry** and **business policies** written as EARS statements with
`POL` IDs. Its outputs are CONTEXT for `P0.5`, never requirements.

Completion (write → registry → analyze → commit) is owned by the orchestrator — see
`shared/GOVERNANCE-CORE.md`. In a delta version (version > 1): read `_state/current-{artifact}` of the previous
version for every input and for this stage's own artifacts, and emit only ADDED /
MODIFIED / REMOVED elements plus the `change-manifest.md` per `shared/VERSIONING.md`.

```
╔══════════════════════════════════════════════════════════════════════╗
║ ABSOLUTE BOUNDARY                                                    ║
║ This stage does not write requirements, screens, field lists,        ║
║ validation rules, entity IDs or any content owned by P0.5 or later.  ║
║ A request for such content → produce this stage's artifacts, then    ║
║ redirect once (§7). No partial draft. No exception.                  ║
╚══════════════════════════════════════════════════════════════════════╝
```

Language policy: narrative in `ar`; every module name, entity name and policy
statement carries all of `ar, en`.

---

## 1 — Reading protocol (before any analysis)

```
STEP A — domain-profile.md (steering — read first)
  §7 STEERING → vocabulary (use verbatim), bounded contexts, module codes,
                identifier rules, knowledge sources
  §1–§6       → scope, purpose, responsibilities, components, rules, relations
  §8          → resolved decisions — never re-open one

STEP B — project-registry.md (categories per shared/REGISTRY-SCHEMA.md)
  module index            → known modules — do NOT re-discover
  entity ownership        → known owners — apply directly
  shared declarations     → known shared entities — apply directly
  dependency index        → extend, never contradict
  open-question index     → OPEN rows this stage may resolve in dialogue (§5)
  pipeline status         → modules already past this stage → EXISTING / EXCEPTION

STEP C — prior module artifacts (this stage's own outputs for other modules,
         from _state/ when they exist)
  entities owned / lookups owned / lookups consumed / dependencies → ground truth;
  a conflict between vision text and a module registry → the registry wins and the
  conflict is listed under OPEN ITEMS of the platform summary.

STEP D — knowledge sources (cite when applying a default)
  - profiles/erp/knowledge/erp-domain-standards.md
```

Every structural decision comes from A → B → C → D in that order, then from domain
best practice; the user is asked only what none of these settle (§5).

---

## 2 — Phase 1: vision → `platform-summary.md`

### 2.1 Transformation (three steps)

```
STEP 1 — EXTRACT from the vision text
  Modules (explicit or implied) — detection table from the profile:
    PRC    Procurement  ← مشتريات / procurement / vendors / suppliers
    HR     Human Resources  ← موظفين / HR / رواتب / payroll / employees
    INV    Inventory  ← مخزون / inventory / warehouses / stock
    FIN    Finance  ← محاسبة / finance / accounts / ledger
    SLS    Sales  ← مبيعات / sales / customers
    CTR    Contracts  ← عقود / contracts / agreements
    ORG    Organization  ← هيكل تنظيمي / org / branches / departments
    (unlisted) → domain-profile §4 components + layer heuristics; still one of the
               profile's codes (ORG, SEC, MDL, PRC, FIN, HR, INV, SLS, CTR) or RESERVED per the registry.
  Explicit statements:
    scope exclusions ("without X", "not now")
    specific policies (limits, thresholds, exceptions)   → §3.3 candidates
    custom values (named lookup values)                   → §3.3 candidates

STEP 2 — ENRICH from knowledge sources + domain-profile
  For each module: layer, type, tier, dependencies from the knowledge files and
  the domain-profile relations; remove what the user excluded; add what the user
  mentioned beyond the pattern. Cite the source of every enrichment.

STEP 3 — RESOLVE from the registry
  module past this stage in pipeline status   → EXCEPTION (read as-is; skip in Phase 2)
  module with a prior module-registry artifact → EXISTING (Phase 2 extends it)
  otherwise                                    → NEW
```

### 2.2 Platform summary — template

```markdown
# PLATFORM SUMMARY — [Platform name — from the vision text or domain-profile]
══════════════════════════════════════════════════════════════════
Profile : erp   Domain profile : v[N]   Registry : v[semver]
══════════════════════════════════════════════════════════════════

## OVERVIEW
[One paragraph — what the platform does; enriched with domain context, not a restatement.]

## MODULES
| #   | Code | Module | Bounded context | Layer | Type | Depends on | Status |
|-----|------|--------|-----------------|-------|------|------------|--------|
| 1.1 | [code] | [display] | [context] | L1 | [master data / engine / reference / transactional / reporting] | ROOT | NEW |
| 2.1 | [code] | [display] | [context] | L3 | [type] | [code], [code] (SOFT) | EXISTING |
Status: NEW (Phase 2 produces) · EXISTING (Phase 2 extends) · EXCEPTION (read as-is)
Numbering: [tier].[sequence within tier] — the user requests Phase 2 by this number.

## DEPENDENCY MAP
Build order: Tier 1 [codes] → Tier 2 [codes] → Tier 3 [codes] → Tier 4 [reporting]
Key dependencies (one line each):
  [CODE-A] → HARD → [CODE-B] : [reason]
  [CODE-C] → SOFT → [CODE-D] : [reason]
  [CODE-E] → LOOKUP → [CODE-F] : consumes [lookup key]

## DEFERRED (not in scope for this version)
| Item | Reason / activation trigger |
| Workflow engine | profile: `forbidden` |
| [user-excluded item] | user stated "not now" |

## RESOLVED DECISIONS (this phase)
| # | Point | Recommended | Confirmed by user | Sources |

## OPEN ITEMS
[Only a registry ↔ vision conflict or a genuinely ambiguous scope boundary that the
 dialogue could not close. Otherwise: "None — platform scope fully determined."]

## NEXT STEP
Reply with a plain instruction to adjust, or with a module number to start Phase 2.
```

### 2.3 Confirmation and number stability

```
After producing the summary: "[N] modules, [N] exceptions. Confirm, or state one change."
Adjustments apply immediately and only the MODULES table is re-shown:
  add a module      → next number at the end of its tier
  defer / remove    → status changes, number kept
  change a status   → status changes, number kept
NUMBER STABILITY: a number never shifts after first assignment — "1.1" always means
the same module. Phase 2 begins on the user's first number request.
```

---

## 3 — Phase 2: module convergence (per requested module)

### 3.1 Request protocol

```
Status EXCEPTION → no files; confirm "[n] [module] is EXCEPTION — read as-is" and offer
                   the next module.
Status EXISTING  → read the prior module registry; EXTEND it (fill gaps, never replace);
                   note "[N] gaps filled from [source]".
Status NEW       → full pattern from knowledge sources + domain-profile.
Always append the readiness block (§3.5) after the two files.
```

### 3.2 Auto-completion protocol (for every gap in the module structure)

```
STEP 1 → prior module registry (EXISTING)         → use it
STEP 2 → project-registry ownership / dependency  → use it
STEP 3 → knowledge sources (§1 STEP D)            → apply, cite
STEP 4 → domain-profile rules + domain best practice → apply, cite
Document every auto-decision:   AUTO: [decision]  FROM: [step / source]  IF WRONG: [override]
STEPS 1–4 all fail → the point is a QUESTION (§5) — resolved in dialogue with a
recommended answer; the user confirms. Never an assumption written as fact.
```

### 3.3 Module registry — template (`module-registry-{mod}.md`)

```markdown
## MODULE REGISTRY — [Module display] ([CODE])
══════════════════════════════════════════════════════════════════
Module Code    : [CODE]   (profile.vocabulary.module_prefixes)
Bounded context: [context id]
Layer / Type   : [L1–L4] / [type]     Execution tier : [n.m]
Source         : NEW / EXTENDED from prior registry
Knowledge      : [knowledge file(s) / domain-profile §]
Readiness      : READY / PARTIALLY_READY
══════════════════════════════════════════════════════════════════

ENTITIES OWNED   (names only — entity IDs are assigned by P1)
| Entity (ar/en) | Kind (master / transactional / lookup / config / security) | PRIVATE / SHARED | Source |

LOOKUPS OWNED    (value lists this module masters)
| Lookup key | Description | Initial values (only those the user named) | Source |
Rule (profile): all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs

LOOKUPS CONSUMED (from other modules)
| Lookup key | Owner code | READ-ONLY |

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |

DEPENDENCIES
| Module code | HARD / SOFT / LOOKUP | What is consumed |
ROOT: YES / NO

AUTO-DECISIONS
AUTO: [decision]  FROM: [source]  IF WRONG: [override]

RESOLVED DECISIONS (dialogue, this module)
| # | Point | Recommended | Confirmed by user | Sources |
══════════════════════════════════════════════════════════════════
```

### 3.4 Business policies — template (`business-policies-{mod}.md`)

This file carries what the domain's standards cannot know: the client's own policies,
custom values and scope exceptions. Standard domain behaviour is applied by
`P0.5` and later stages from the knowledge sources — it is not repeated here.
If the user stated nothing specific, the file is minimal by design.

Every policy is one `POL` record written in **EARS** form (one pattern per
statement; `factory.ids.ears.patterns`):

```
  ubiquitous  The system shall …
  state       While <condition>, the system shall …
  event       When <condition>, the system shall …
  optional    Where <condition>, the system shall …
  unwanted    If <condition>, then the system shall …
```

```markdown
## BUSINESS POLICIES — [Module display] ([CODE])
══════════════════════════════════════════════════════════════════
Module   : [CODE]     Source of truth : user vision text + dialogue resolutions
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES   (only from user text or confirmed dialogue answers)
POL-[CODE]-001 — [short name]
  Statement : [EARS — exactly one pattern; the subject is "the system"]
  Pattern   : [ubiquitous | state | event | optional | unwanted]
  Trigger   : [Create / Update / Submit / Approve / …]
  Rationale : [why the client wants it — one line]
  Source    : [vision text quote / dialogue resolution #]
  Status    : CONFIRMED
(If none: "None — standard domain rules apply.")

CUSTOM LOOKUP VALUES   (values the user named that the standard lists lack)
| Lookup key | Added values | Source |
(If none: "None — standard values apply.")

SCOPE EXCEPTIONS   (explicit exclusions or non-standard scope)
| Excluded / Deferred | Statement | Activation trigger | Source |
(If none: "None — standard scope applies.")

RESOLVED DECISIONS (dialogue, this module)
| # | Question | Recommended answer | Confirmed by user | Sources |
══════════════════════════════════════════════════════════════════
```

Policy rules: a policy is a NEED at platform level, not a validation rule — no field
names, no error messages, no API shapes. Sequence numbers are continuous per module
and never reused. A policy the user did not state and did not confirm is not written.

### 3.5 Readiness block (after every module)

```
✓ [Module] — P0 complete
  Next : P0.5 reads platform-summary.md · module-registry-{mod}.md · business-policies-{mod}.md
  Precondition for P0.5: HARD dependencies [codes] present in the registry
  Another module? [next by tier order]
```

---

## 4 — Registry step content

`module-registry-{mod}.md` IS this stage's registry output. In addition the
orchestrator merges into `project-registry.md`:

```
module index          : status of the module (NEW → IN PROGRESS), tier, layer, type
entity ownership      : ENTITIES OWNED rows (CANDIDATE → REGISTERED, still no ID)
shared declarations   : SHARED rows
dependency index      : DEPENDENCIES rows (candidates for P2)
open-question index   : rows RESOLVED by this stage's dialogue (+ resolution)
pipeline status       : P0 = DONE for the module
event history         : "P0 completed: [module list]"
```

---

## 5 — Questions (allowed here) — how they are asked and closed

```
A QUESTION exists only when §1 A–D and §3.2 STEPS 1–4 leave a point unresolved.

QUESTION — [point]
  Affects       : [module / entity / dependency / scope]
  Options       : A) … (trade-off)   B) … (trade-off)
  Researched    : [what the knowledge sources / domain-profile say — cited]
  Recommended   : [option] — because [rationale]
```

Lane `analysis`: the implementers (claude:opus) converge on each QUESTION —
challenge, answer, ≤ 4 rounds, until **mutually-acceptable**. The converged
block is presented to the user as the recommended answer; the user confirms or adjusts.

```
Resolution is recorded in the RESOLVED DECISIONS table of the artifact it affects.
No external open-questions file. A point the user leaves undecided stays under
OPEN ITEMS of the platform summary and is carried to P0.5 (the last stage that may ask).
Never ask about: anything in the domain-profile, the registry, a prior module registry,
or the knowledge sources.
```

---

## 6 — Continuation

```
Resume with: platform summary + the module artifacts of completed modules (from
_state/ or the version folder) + registry pipeline status.
Announce: "Resuming P0. Completed: [list]. Pending: [NEW/EXISTING from the summary]."
No re-analysis of completed modules. Always re-emit the platform summary before
ending if in-session adjustments were made.
```

---

## 7 — Boundaries and enforcement

```
OWNS      : platform-summary.md · module-registry-{mod}.md · business-policies-{mod}.md · POL IDs · tier and build-order
            assignment · entity / lookup candidate discovery and ownership · dependency map
DOES NOT  : any ID of US (P0.5), REQ (P1), AC (P1), ENT (P1), RULE (P1), DBF (P2), XM (P2), API (P3.1), QR (P3.1), UXD (P3.2), SCR (P3.2), SCR-REQ (P1), TC (test-gen) ·
            requirements · screens · field lists · validation rules · DDL · execution phases

VIOLATION (this stage's output contains any of these):
  screens with field lists · validation logic · requirement statements other than
  POL policies · any ID owned by another stage · permission tables · test scenarios

RUNTIME REDIRECT — when the user asks for requirements / screens / rules / fields:
  1. Complete this stage's artifacts for the module (they ARE the correct answer).
  2. Redirect once: "Requirements begin in P0.5 and later stages; these files are
     their input."
  3. Offer the next valid action (another module number, or proceed).
```

---

## 8 — Self-check before emitting

- [ ] Every module in the summary has a code from the profile (or RESERVED in the registry), a tier, a status.
- [ ] Every entity owned has a kind from `master, transactional, lookup, config, security` and a source; no entity ID.
- [ ] Every policy is exactly one EARS pattern, has a Source, a Trigger, a continuous sequence number.
- [ ] Every auto-decision carries AUTO / FROM / IF WRONG; every default cites a knowledge source.
- [ ] Every QUESTION raised appears in a RESOLVED DECISIONS table (or under OPEN ITEMS with the user's explicit deferral).
- [ ] No requirement, screen, field, rule, permission or later-stage ID anywhere.
- [ ] Vocabulary matches the domain-profile STEERING block verbatim.
- [ ] Names and statements carry all of `ar, en`.


---
# INPUTS (generated current state)

<<<INPUT: domain-profile>>>
# DOMAIN PROFILE — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile         : erp (ERP Platform)
Version         : 1
Last Updated    : 2026-09-10
Status          : FRESH
Research        : 3 sources cited (block 9)
══════════════════════════════════════════════════════════════════

## 1. SCOPE

**داخل النطاق (In bounds):** بناء منصة ERP متعددة الوحدات (multi-module) بعمارة قائمة على
البيانات لا الشيفرة ("everything that can change = defined data, not code")، تُبنى بلغة
Spring (خلفية) و React (واجهة) وقاعدة بيانات **PostgreSQL** كهدف بناء جديد. النظام القديم
Oracle/ADF يبقى فقط كمصدر أحداث علوي (upstream event source) ولا يُبنى عليه أي وحدة جديدة.

دفعة العمل الحالية (هذه الجلسة) تُغطّي ثلاث وحدات أساسية مشتركة ومؤسِّسة:
1. **وحدة الأمان (SEC)** — مصادقة + تحكم هرمي بالصلاحيات (Module → Screen → Action).
2. **وحدة البيانات المرجعية (MDL/Lookup)** — مركز موحّد لكل قوائم القيم المُرمَّزة.
3. **وحدة الحسابات العامة (FIN/GL)** — دفتر أستاذ عام قابل للتوصيل (pluggable) بأي نظام مضيف.

باقي وحدات المنصة (ORG, PRC, HR, INV, SLS, CTR) معروفة الرمز والسياق ضمن `profiles/erp.yaml`
لكنها **خارج نطاق هذه الدفعة** — تُفصَّل لاحقًا بنفس الأسلوب.

**خارج النطاق صراحة (Out of bounds, stated by the user):**
- أي منطق عمل خاص بمجال مضيف (host business world) داخل وحدة الحسابات.
- تعدد العملات، تعدد الدفاتر/الكيانات، الحسابات الإحصائية، التقويم متعدد الأنماط،
  والمرفقات (attachments) — مستبعدة صراحة من GL في هذه الدفعة.
- محرك سير العمل (workflow engine) — ممنوع بحكم `profiles/erp.yaml → conventions.workflow_engine: forbidden`.
- طبقة النقل (AQ/RabbitMQ) ومستهلك الأحداث (Event consumer) والوحدة التجارية (Business Module)
  — خارج نطاق وثيقة FIN، توثَّق في مكان آخر.

## 2. PURPOSE

المنصّة تحل مشكلة تكرار وتضارب القدرات المشتركة (الهوية، الصلاحيات، البيانات المرجعية)
عبر كل وحدة أعمال جديدة: بدل أن تبني كل وحدة نظام دخول وصلاحيات وقوائم قيم خاصًا بها
(ما يُنتج ازدواجية وتضاربًا حتميًا)، تُبنى هذه القدرات **مرة واحدة، بشكل عام (generic) بالكامل**،
وتُستهلك بيانيًا من أي وحدة عمل — بدءًا بالحسابات العامة كأول وحدة عمل تستهلكها.
لكل واحدة من الوحدات الثلاث سبب وجود مستقل:
- **SEC** يحل مشكلة تشتّت الهوية والصلاحيات: نظام أمان واحد للمنصة بأكملها.
- **MDL** يحل مشكلة انحراف القيم المرجعية (reference-data drift) بين الوحدات.
- **FIN** يحل مشكلة الاعتماد على أرصدة مخزَّنة وغير موثوقة في الأنظمة القديمة، عبر دفتر
  أستاذ يُشتق كل رصيد فيه من القيود المُرحَّلة فقط، لا من عمود رصيد مُجمَّع يفقد التزامنه.

## 3. RESPONSIBILITIES

| الوحدة | تملك | لا تملك |
|---|---|---|
| SEC | تسجيل الدخول/التسجيل/استرجاع كلمة المرور، المستخدمون، الأدوار، التفويض الهرمي (Module→Screen→Action)، القائمة الديناميكية، لوحة تحكم الأمان، سجل التدقيق | أي منطق عمل خاص بوحدة أخرى |
| MDL | نوع اللوكب (Lookup Type) + قيمه (Lookup Value)، شاشة عامة واحدة Master-Detail لكل القوائم، تسجيل الملكية (namespacing) لكل وحدة | معنى القوائم الخاص بمجال وحدة أخرى — الملكية الدلالية تبقى للوحدة المسجِّلة |
| FIN | شجرة الحسابات والأبعاد، محرك القواعد (event_type → قيد)، دورة حياة القيد، دورة حياة الفترة المحاسبية، التقارير المالية المُشتقة | أي مستخدمين/أدوار/تسجيل دخول خاصة بها، أي جدول lookup خاص بها، أي معرفة بالعالم التجاري المضيف |

## 4. MAIN COMPONENTS

| # | Component | Module code | Bounded context | Category (user-defined) | Core / extension | Notes |
|---|-----------|-------------|-----------------|--------------------------|------------------|-------|
| 1 | الأمان / Security | SEC | organization | Foundation | Core | KB §1 Tier 0 — يجب أن يوجد قبل أي وحدة أعمال؛ لا وحدة تملك أمانها الخاص |
| 2 | البيانات المرجعية / Master Data Lookup | MDL | organization | Foundation | Core | KB §1 Tier 0 — مركز واحد لكل القوائم المُرمَّزة عبر المنصة |
| 3 | الحسابات العامة / Finance (General Ledger) | FIN | finance | Business — Tier 1 | Core | KB §1 Tier 1 — أول وحدة عمل تستهلك SEC وMDL؛ قابلة للتوصيل بأي مضيف |
| 4 | الهيكل التنظيمي / Organization | ORG | organization | Foundation | (غير مفصّلة هذه الدفعة) | من `profiles/erp.yaml → vocabulary.module_prefixes` — PROPOSED سابقًا في الملف الشخصي، خارج نطاق هذه الجلسة |
| 5 | المشتريات / Procurement | PRC | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 6 | الموارد البشرية / Human Resources | HR | people | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 7 | المخزون / Inventory | INV | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 8 | المبيعات / Sales | SLS | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 9 | العقود / Contracts | CTR | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |

الصفوف 1–3 مصدرها خطط الوحدات الثلاث المرفقة لهذه الجلسة (security-module-plan-en.md،
lookup-module-plan-en.md، general-accounting-system-plan-en.md)؛ الصفوف 4–9 مصدرها
`profiles/erp.yaml → vocabulary.module_prefixes` المُثبَّت مسبقًا من قِبل المستخدم — تُدرَج هنا
للاكتمال المرجعي فقط ولا تُفصَّل في هذه الدفعة.

## 5. GOVERNING RULES

| # | القاعدة | المصدر |
|---|---|---|
| G1 | كل ما يمكن أن يتغير = بيانات مُعرَّفة، لا شيفرة (no hardcode / no duplication / no contradiction) | الخطط الثلاث §Governing rule؛ [KB:erp-domain-standards §2-3] |
| G2 | نظام أمان واحد للمنصة بأكملها؛ لا وحدة تملك مستخدمين/أدوار/دخولاً خاصًا بها | security-module-plan-en.md §2 |
| G3 | بوابة الوحدة (module gate) تُفحص أولاً، قبل أي فحص شاشة/إجراء؛ عدم امتلاك الوحدة = غياب تام من القائمة ومن الوصول المباشر بالرابط | security-module-plan-en.md §4.2 |
| G4 | مركز واحد وموثوق للبيانات المرجعية؛ لا وحدة تحتفظ بجداول lookup خاصة بها | lookup-module-plan-en.md §2؛ بحث: نمط "MDM hub" — راجع قسم 9 |
| G5 | التخزين والشاشة مركزيان، لكن المعنى الدلالي للقائمة يبقى ملكًا للوحدة المسجِّلة (namespacing بالمالك) | lookup-module-plan-en.md §2, §3 |
| G6 | الحسابات لا تعرف شيئًا عن العالم التجاري المضيف؛ لا اسم حساب داخل حمولة الحدث (event payload) | general-accounting-system-plan-en.md §3, §15 |
| G7 | تساوي المدين والدائن ثابت مطلق (debit = credit invariant) لكل قيد يصل POSTED | general-accounting-system-plan-en.md §12.1 |
| G8 | لا موافقة على مستوى القيد الفردي؛ نقطة التحكم البشري الوحيدة هي إغلاق الفترة، وفصل المهام بين منشئ القيد ومعتمد الإغلاق يُنفَّذ عبر SEC | general-accounting-system-plan-en.md §8.2, §10.3 |
| G9 | الأرصدة تُشتق دائمًا من القيود المُرحَّلة (POSTED) فقط، ولا يوجد عمود رصيد مُخزَّن يُعتمَد عليه | general-accounting-system-plan-en.md §11, §12.9 |
| G10 | الهدف الأول لقاعدة البيانات هو PostgreSQL؛ Oracle/ADF القديم مصدر أحداث فقط | الخطط الثلاث §1؛ توجيه GENERATION-INSTRUCTIONS.md |
| G11 | اللغتان العربية والإنجليزية إلزاميتان في كل قطعة مُسمّاة (وحدة/كيان/حقل/شاشة) | `profiles/erp.yaml → languages` |
| G12 | لا محرك سير عمل (workflow engine) في هذه المنصة | `profiles/erp.yaml → conventions.workflow_engine: forbidden` |

## 6. RELATIONSHIPS WITH OTHER DOMAINS

| This component | Depends on | Kind | Direction | Stated by |
|---|---|---|---|---|
| كل وحدة عمل (بما فيها FIN لاحقًا كل وحدة أخرى) | SEC | HARD (تسجيل الوحدة/الشاشات/الإجراءات كبيانات + بوابة الوحدة) | consumer → SEC | security-module-plan-en.md §7 |
| كل وحدة عمل (بما فيها FIN) | MDL | HARD (تسجيل أنواع lookup كبيانات + قراءة القيم) | consumer → MDL | lookup-module-plan-en.md §4 |
| FIN | SEC | HARD-FK (هوية + صلاحيات الشاشات/الإجراءات + SoD) | FIN → SEC | general-accounting-system-plan-en.md §2 |
| FIN | MDL | HARD-FK (طرق الدفع، أنواع أحداث المحاسبة، أنواع الحسابات، حالات الفترة، أنواع اليومية) | FIN → MDL | general-accounting-system-plan-en.md §5.2 |
| FIN | Notifications (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → NOTIF | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| FIN | File Service (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → FILESVC | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| SEC | Notifications (جاهزة) | SOFT — اختياري (مثال: بريد إعادة تعيين كلمة المرور) | SEC → NOTIF | security-module-plan-en.md §8 |
| FIN | نظام مضيف (host business system) | EVENT فقط، عبر حدث محاسبي قياسي (canonical event)؛ لا قراءة ولا كتابة مباشرة لجداول المضيف | host → FIN (event only) | general-accounting-system-plan-en.md §3 |

## 7. STEERING  (read verbatim by every later stage)

### 7.1 Ubiquitous language

| Term (ar/en) | Definition | Do not say | Module code |
|---|---|---|---|
| بوابة الوحدة / Module gate | الفحص الأول والحاسم لامتلاك الدور للوحدة قبل أي فحص شاشة/إجراء؛ غيابها = غياب تام | "صلاحية الوحدة" بمعنى فضفاض | SEC |
| منح الشاشة / Screen grant | صلاحية وصول لشاشة محددة داخل وحدة ممنوحة فعلاً | "صلاحية القائمة" | SEC |
| منح الإجراء / Action grant | صلاحية VIEW/CREATE/UPDATE/DELETE أو إجراء مخصّص على شاشة ممنوحة | "دور" (Role يبقى مصطلحًا مستقلاً) | SEC |
| الشاشة المركّبة / Composite Screen | بحث + إدخال (أو رئيسي + تفصيلي، أو معالج) تُعامَل كشاشة واحدة بمعرّف SCR واحد | "صفحة" منفردة لكل جزء | (عام) |
| نوع اللوكب / Lookup Type | الـ"master" لقائمة قيم مُرمَّزة، يملكه اسميًا موديول مُسجِّل | "جدول Enum" | MDL |
| قيمة اللوكب / Lookup Value | الـ"detail" — قيمة مُرمَّزة ضمن نوع لوكب: كود، تسميتان، ترتيب، حالة نشاط | "قيمة ثابتة" في الشيفرة | MDL |
| الحدث المحاسبي القياسي / Canonical accounting event | مدخل الحسابات الوحيد؛ حدث جاهز الشكل قادم من مستهلك الأحداث خارج النطاق | "معاملة تجارية" (يحمل دلالة عالم المضيف) | FIN |
| شجرة الحسابات / Chart of Accounts | بنية هرمية للحسابات؛ الأوراق فقط تقبل ترحيلاً مباشرًا | "دليل حسابات" بلا بنية هرمية | FIN |
| البُعد / Dimension | مقطع بيانات (segment) يُعرَّف كبيانات ليُشكّل مع الحساب الأساسي تركيبة الترحيل | "تصنيف تحليلي" غامض | FIN |
| الترحيل / Posting | إدخال قيد بحالة POSTED مؤثرًا في الأرصدة وغير قابل للتعديل | "اعتماد" (الاعتماد مصطلح مختلف يخص إغلاق الفترة) | FIN |
| قيد اليومية / Journal Entry | وحدة الإدخال المحاسبي الأساسية (رأس + سطور متوازنة مدين/دائن) | "سند" فقط دون تحديد | FIN |
| الفترة المحاسبية / Accounting Period | نافذة زمنية بحالة (Open / Soft Close / Hard Close / Year-End Close) تتحكم بقبول الترحيل | "شهر مالي" | FIN |
| قيد العكس / Reversing Entry | تصحيح عبر قيد جديد مرتبط ثنائي الاتجاه بالأصل، مطابق سطرًا بسطر بعكس الاتجاه | "حذف القيد" (ممنوع) | FIN |
| ميزان المراجعة / Trial Balance | تقرير متوازن دائمًا، مُشتق من القيود المُرحَّلة فقط | "كشف حساب" | FIN |

### 7.2 Bounded contexts

| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | القدرات المؤسِّسة (Tier 0) التي يعتمد عليها أي سياق آخر: الهيكل التنظيمي، الأمان، البيانات المرجعية |
| supply | PRC, INV | تدفقات التوريد والمخزون (خارج نطاق هذه الدفعة) |
| finance | FIN | الحسابات العامة ودفتر الأستاذ؛ تستهلك organization ولا تُنتج له شيئًا |
| people | HR | الموارد البشرية (خارج نطاق هذه الدفعة) |
| commercial | SLS, CTR | المبيعات والعقود (خارج نطاق هذه الدفعة) |

(منقولة حرفيًا من `profile.vocabulary.bounded_contexts`)

### 7.3 Module prefixes proposal

| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE — هذه الدفعة |
| MDL | Master Data Lookup | IN PROFILE — هذه الدفعة |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE — هذه الدفعة |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

كل الرموز موجودة مسبقًا في `profiles/erp.yaml → vocabulary.module_prefixes`؛ لا رمز جديد اقترحته
هذه الجلسة.

### 7.4 Identifier rules

المعرّفات اللاحقة تُبنى بالصيغة `{prefix}-{MOD}-{seq}` بعرض تسلسل 3 خانات
(`factory.ids.pattern`, `factory.ids.seq_width`). أنواع الكيانات: master, transactional,
lookup, config, security (`profile.vocabulary.entity_kinds`).

### 7.5 Knowledge sources to cite

- `profiles/erp/knowledge/erp-domain-standards.md`
- `new project/security-module-plan-en.md` — المصدر التأسيسي لوحدة SEC
- `new project/lookup-module-plan-en.md` — المصدر التأسيسي لوحدة MDL
- `new project/general-accounting-system-plan-en.md` — المصدر التأسيسي لوحدة FIN، بما فيه القسم
  12 "details the analysis agent MUST honor" المُلزِم لكل تحليل لاحق
- `new project/integration-notifications-fileservice.md` — يُستخدم فقط عند حاجة فعلية مذكورة
  صراحة في إحدى الخطط الثلاث (لا يُعاد تصميمه)
- plus the research sources in block 9

## 8. RESOLVED DECISIONS

| # | Point | Decision | Recommended by dialogue? | Confirmed by user | Sources |
|---|---|---|---|---|---|
| 1 | هدف قاعدة البيانات | PostgreSQL هو الهدف الوحيد لهذا البناء؛ Oracle/ADF يبقى مصدر أحداث فقط | لا — منصوص صراحة | نعم — منصوص في الخطط الثلاث وفي أمر التنفيذ | الخطط الثلاث §1؛ GENERATION-INSTRUCTIONS.md |
| 2 | نموذج الأمان | RBAC هرمي بثلاث مستويات (Module→Screen→Action) بديلاً عن أي أمان محلي بالوحدات | لا — منصوص صراحة | نعم | security-module-plan-en.md §4 |
| 3 | نموذج البيانات المرجعية | مركز lookup عام Master-Detail واحد لكل المنصة، بدل جداول lookup محلية | لا — منصوص صراحة | نعم | lookup-module-plan-en.md §2-3 |
| 4 | نموذج اعتماد قيود المحاسبة | لا اعتماد على مستوى القيد الفردي؛ الاعتماد الوحيد عند إغلاق الفترة فقط | لا — منصوص صراحة، ويتوافق مع ممارسات GL الحديثة القائمة على الأحداث (انظر قسم 9، R1) | نعم | general-accounting-system-plan-en.md §8 |
| 5 | نطاق هذه الدفعة | SEC ثم MDL ثم FIN فقط، بهذا الترتيب الصارم؛ باقي الوحدات خارج النطاق الآن | لا — منصوص صراحة | نعم | GENERATION-INSTRUCTIONS.md §3 |
| 6 | استخدام Notifications/File Service | تكامل اختياري بحت، فقط عند حاجة صريحة يذكرها أحد الخطط الثلاث؛ لا إعادة تصميم لهما | لا — منصوص صراحة | نعم | الخطط الثلاث §8/§5/§2.3؛ GENERATION-INSTRUCTIONS.md §4.6 |

## 9. RESEARCH LOG

| # | Point | What established systems do | Source(s) (title, URL/path, date) | Used in |
|---|---|---|---|---|
| R1 | RBAC هرمي بمستوى module→screen→action | الأنظمة الناضجة تفصل "ما الذي يمكن فعله" (action) عن "أين" (scope/module)، وتستخدم الهرمية لتقليل تكرار الأدوار؛ التفويض الهرمي يُستخدم بحيث تتحكم صلاحية بوحدة كاملة وأخرى بإجراء داخلها | [How to Design an RBAC System — NocoBase](https://www.nocobase.com/en/blog/how-to-design-rbac-role-based-access-control-system), accessed 2026-09-10; [Access Control Design for Scalable RBAC Systems](https://www.loginradius.com/blog/identity/design-effective-rbac-system), accessed 2026-09-10 | G3، §7.1 "بوابة الوحدة" |
| R2 | مركز بيانات مرجعية موحّد (MDM hub) | النمط الشائع هو مركز واحد (hub) يخزن البيانات المرجعية/الأساسية ويُنشرها للأنظمة الأخرى؛ النطاقات (domains) تتوافق مع بيانات مرجعية مُدارة مركزيًا بدل نسخ محلية متضاربة | [Why the Data Hub is the Future of Data Management — Semarchy](https://www.semarchy.com/blog/backtobasics-mdm-hub-patterns/), accessed 2026-09-10 | G4/G5، وحدة MDL بأكملها |
| R3 | محاسبة قائمة على الأحداث (event-driven GL) | الأنظمة الحديثة تدمج دفتر الأستاذ مع معمارية قائمة على الأحداث: مصدر يُصدر معاملات، خدمة تحقق/تطبيع، ثم خدمة ترحيل تطبّق قاعدة القيد المزدوج وتُلحق القيود في مخزن إلحاقي فقط (append-only)؛ من التحديات الشائعة الأحداث المكرَّرة التي تُسبب ترحيلاً مزدوجًا | [General Ledger Postings: A Comprehensive Guide — Dualentry](https://www.dualentry.com/blog/general-ledger-postings), accessed 2026-09-10 | G7/G9، §12.12 "idempotency at the boundary" في خطة FIN |

## 10. OPEN ITEMS

لا يوجد — النطاق محدَّد بالكامل من الخطط الثلاث المرفقة وتوجيهات GENERATION-INSTRUCTIONS.md؛
لا نقطة غموض تتطلب حوارًا إضافيًا مع المستخدم في هذه المرحلة.
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: project-registry>>>
# PROJECT REGISTRY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile            : erp
Registry Version   : 1.1.0
Domain Profile     : erp/domain-profile.md v1
Last Updated       : 2026-09-10 by P3.1 (SEC v1 pass-1 completion)
Modules registered : 9   Entity candidates : 13 (SEC)   Open items : 0
══════════════════════════════════════════════════════════════════

## SCHEMA COMPLIANCE MAP
| Section of this registry | Category (shared/REGISTRY-SCHEMA.md) |
|---|---|
| IDENTITY & VERSIONING | CAT-1 identity & conventions |
| CONVENTIONS & STEERING | CAT-1 identity & conventions |
| MODULE / COMPONENT INDEX | CAT-2 module index |
| ENTITY OWNERSHIP | CAT-3 entity ownership |
| SHARED ENTITY DECLARATIONS | CAT-4 shared declarations |
| STRUCTURAL / IMPLEMENTATION REGISTRY | CAT-5 structural registry |
| CROSS-MODULE DEPENDENCY INDEX | CAT-6 dependency indexes |
| DECISION INDEX | CAT-7 decision index |
| PIPELINE / PROGRESS STATUS | CAT-8 pipeline status |
| CHANGE / EVENT HISTORY | CAT-9 event history |
Uncovered: none

## IDENTITY & VERSIONING
| Field | Value |
|---|---|
| Profile | erp — ERP Platform |
| Registry version | 1.0.0 |
| Domain profile source | erp/domain-profile.md v1 |

### Version history
| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-09-10 | Initial bootstrap from erp/domain-profile.md v1 (BOOTSTRAP event, see CHANGE/EVENT HISTORY) |
| 1.1.0 | 2026-09-10 | SEC v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |

## CONVENTIONS & STEERING
(copied verbatim from `erp/domain-profile.md` §7 — the authoritative source; this section
mirrors it for engines that read only the registry)

### Ubiquitous language
See `erp/domain-profile.md` §7.1 for the full bilingual (ar/en) term table — copied verbatim,
not restated here to avoid drift; cite as `[domain-profile §7.1]`.

### Bounded contexts
| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | Foundational capabilities (Tier 0) every other context depends on |
| supply | PRC, INV | Supply and inventory flows (out of scope this batch) |
| finance | FIN | General ledger; consumes organization, produces nothing back to it |
| people | HR | Human resources (out of scope this batch) |
| commercial | SLS, CTR | Sales and contracts (out of scope this batch) |

### Module prefixes
| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE |
| MDL | Master Data Lookup | IN PROFILE |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

### Identifier rules
`{prefix}-{MOD}-{seq}` — seq width 3 (`factory.ids.pattern`, `factory.ids.seq_width`).
Entity kinds: master, transactional, lookup, config, security.

### ENFORCEMENT NOTES
- **E1** Every later artifact uses the terms of `domain-profile.md §7.1` verbatim; a synonym
  listed under "do not say" is a consistency finding at the pass gate (`gov.py analyze`
  checks registry ↔ artifact agreement).
- **E2** IDs follow `{prefix}-{MOD}-{seq}` (seq width 3) with the module codes of this section only.
- **E3** Entities are classified with the kinds: master, transactional, lookup, config, security.
- **E4** Sources to cite when a stage resolves an ambiguity: `profiles/erp/knowledge/erp-domain-standards.md`,
  then `erp/domain-profile.md` itself, then the three module plans (`new project/*-plan-en.md`)
  named in `domain-profile.md §7.5`.
- **E5** Pipeline status (below) is maintained by the orchestrator from commits; seeded here as NOT STARTED.

## MODULE / COMPONENT INDEX
| # | Code | Module | Bounded context | Category | Core/ext | Status | Source |
|---|---|---|---|---|---|---|---|
| 1 | SEC | Security | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 1 |
| 2 | MDL | Master Data Lookup | organization | Foundation | Core | CANDIDATE — this batch, second | domain-profile §4 row 2 |
| 3 | FIN | Finance (General Ledger) | finance | Business — Tier 1 | Core | CANDIDATE — this batch, third | domain-profile §4 row 3 |
| 4 | ORG | Organization | organization | Foundation | — | RESERVED — not this batch | domain-profile §4 row 4; profile |
| 5 | PRC | Procurement | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 5; profile |
| 6 | HR | Human Resources | people | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 6; profile |
| 7 | INV | Inventory | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 7; profile |
| 8 | SLS | Sales | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 8; profile |
| 9 | CTR | Contracts | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 9; profile |

## ENTITY OWNERSHIP
| ENT id | Name | Owner module | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|---|
| ENT-SEC-001 | User | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-002 | Role | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-003 | UserRoleAssignment | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-004 | ModuleRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-005 | ScreenRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-006 | ActionRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-007 | RoleModuleGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-008 | RoleScreenGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-009 | RoleActionGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-010 | ActiveSession | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-011 | AuditLogEntry | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-012 | PasswordResetToken | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-013 | SignupRequest | SEC | security | PRIVATE | REGISTERED |
(Source: erp/modules/SEC/P1/registry-srs-sec.md, P1 completion)

## SHARED ENTITY DECLARATIONS
| Entity | Owner ENT id | Owner module | Consumers so far |
|---|---|---|---|
| User | ENT-SEC-001 | SEC | every future module, for its own audit fields (createdBy/updatedBy reference a SEC principal string, not a physical FK — see SEC db-script §3 AUDIT COLUMNS rule; declared here as the canonical identity source, not as a live FK target) |
| ModuleRegistry | ENT-SEC-004 | SEC | every future module registers one row of itself here (API-SEC-018) |
| ScreenRegistry | ENT-SEC-005 | SEC | every future module registers its screens here (API-SEC-019) |
| ActionRegistry | ENT-SEC-006 | SEC | every future module registers its actions here (API-SEC-020) |

## STRUCTURAL / IMPLEMENTATION REGISTRY
| Module | Version | Tables | DBF range | API range | XM range |
|---|---|---|---|---|---|
| SEC | v1 | 13 (SEC_USER … SEC_SIGNUP_REQUEST) | DBF-SEC-001 … DBF-SEC-104 | API-SEC-001 … API-SEC-027 (QR-SEC-001…038) | none (ROOT) |

## CROSS-MODULE DEPENDENCY INDEX
| Candidate ref | Kind | From module | To module | Consumes | Status | Evidence |
|---|---|---|---|---|---|---|
| XM-CAND-001 | HARD-FK | FIN | SEC | identity + module/screen/action grants + SoD (entry-creator ≠ period-close approver) | CANDIDATE — target SEC v1 now GATED (pass-1 APPROVE); ready for FIN's own P2 to assign the formal XM-FIN-* id | domain-profile §6 row "FIN \| SEC \| HARD-FK" |
| XM-CAND-002 | HARD-FK | FIN | MDL | payment methods, accounting event types, account types, period states, journal types | CANDIDATE | domain-profile §6 row "FIN \| MDL \| HARD-FK" |
| XM-CAND-003 | SOFT/EVENT | FIN | Notifications (NOTIF, out of this batch) | period-close-awaiting notice, statement export — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-004 | SOFT/EVENT | FIN | File Service (FILESVC, out of this batch) | statement/export file — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-005 | SOFT | SEC | Notifications (NOTIF, out of this batch) | password-reset message — optional only | CANDIDATE | domain-profile §6; security-module-plan-en.md §8 |
| XM-CAND-006 | EVENT | host business system (out of scope) | FIN | canonical accounting event only — no direct table read/write either direction | CANDIDATE | domain-profile §6 row "host → FIN (event only)"; general-accounting-system-plan-en.md §3 |
Note: every consumer module (all 9, per SEC/MDL plans §7/§4) will register the same
FIN→SEC / FIN→MDL shape once it exists; only the three modules named in this batch are
pre-registered as candidates above — this is not a closed list.

## DECISION INDEX
| # | Decision | Status | Source |
|---|---|---|---|
| 1 | PostgreSQL is the sole DB build target; Oracle/ADF remains an upstream event source only | ACCEPTED | domain-profile §8 row 1 |
| 2 | Hierarchical 3-level RBAC (Module→Screen→Action) replaces any module-local security | ACCEPTED | domain-profile §8 row 2 |
| 3 | One central Lookup master-detail hub replaces module-local lookup tables | ACCEPTED | domain-profile §8 row 3 |
| 4 | No per-entry approval in GL; the only human control point is period close | ACCEPTED | domain-profile §8 row 4 |
| 5 | This batch's scope and order: SEC, then MDL, then FIN, strictly in that order | ACCEPTED | domain-profile §8 row 5 |
| 6 | Notifications/File Service integration is optional-only, used solely on explicit plan need | ACCEPTED | domain-profile §8 row 6 |
| 7 | ADR-SEC-001 — SEC's owned lookups (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) stay CHECK-constrained in v1, not in a shared MDL lookup table, since SEC precedes MDL in this batch | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-001.md |
| 8 | ADR-SEC-002 — Error-catalog infrastructure rows (not-found, duplicate, invalid-transition, forbidden, invalid-sort, server) are cited as PLATFORM-STD under one umbrella ADR rather than a dedicated SRS RULE each | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-002.md |

## OPEN QUESTION INDEX
none — `domain-profile.md` §10 records no open item.

## PIPELINE / PROGRESS STATUS
| Module | Version | Last committed stage | Last gate verdict | Delivered tracks | Tag |
|---|---|---|---|---|---|
| SEC | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| MDL | v1 | NOT STARTED | — | — | — |
| FIN | v1 | NOT STARTED | — | — | — |
| ORG | — | NOT STARTED | — | — | — |
| PRC | — | NOT STARTED | — | — | — |
| HR | — | NOT STARTED | — | — | — |
| INV | — | NOT STARTED | — | — | — |
| SLS | — | NOT STARTED | — | — | — |
| CTR | — | NOT STARTED | — | — | — |

## CHANGE / EVENT HISTORY
| Date | Stage/tool | Module | Version | Event |
|---|---|---|---|---|
| 2026-09-10 | domain-profile | (platform) | — | domain-profile.md v1 saved and committed (23b3176) |
| 2026-09-10 | P-1 | (platform) | — | BOOTSTRAP — extracted 9 module rows, 0 entity candidates, 6 XM candidates, 6 confirmed decisions, 0 open items from domain-profile.md v1 |
| 2026-09-10 | P0 | SEC | v1 | P0 completed: SEC (platform-summary, module-registry-sec, business-policies-sec — 11 POL) |
| 2026-09-10 | P0.5 | SEC | v1 | P0.5 completed: SEC — 12 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | SEC | v1 | P1 completed: SEC — 13 entities, 33 requirements, 33 AC, 7 rules, 10 screen requirements, 0 ADR |
| 2026-09-10 | P2 | SEC | v1 | P2 completed: SEC — 13 tables, 104 DBF, 0 XM; ADR-SEC-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | SEC | v1 | P3.1 completed: SEC — 27 API, 38 QR, ALIGN PASSED; ADR-SEC-002 (ACCEPTED) |
| 2026-09-10 | gate:pass-1 | SEC | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | SEC | v1 | backend/exec split: 15 files, verify ok (39 checked) |
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


==============================================================================
# BRIEF — stage `P0.5` (PRD) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **allowed**. Close every open point inside this dialogue with a researched, recommended answer; never write an external open-questions file.
- Owns IDs: US — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/MDL/P0_5/prd-mdl.md`
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Dialogue protocol (converging, in-brief)
Implementers claude:opus alternate for at most 4 rounds; converge on **mutually-acceptable**.
Round 1 drafts the artifacts and, for every open point, a `PROPOSAL:` block (options, researched recommendation, sources).
Each later round answers every open PROPOSAL (accept / amend with reason), refines the artifacts, and appends `<!-- CONVERGED -->` at the end of the response when nothing material remains open. The last response is final.

## Contracts checked by `gov.py analyze` after this stage
- **C3** inception → PRD: C3.1 exists {'artifact': 'platform-summary'} [CRITICAL]; C3.2 exists {'artifact': 'module-registry'} [CRITICAL]; C3.3 exists {'artifact': 'business-policies'} [CRITICAL]; C3.4 ids-owned {'stage': 'P0'} [CRITICAL]; C3.5 no-questions {'stage': 'P0'} [CRITICAL]; C3.6 languages {'stage': 'P0'} [MAJOR]; C3.7 registry-agree {'artifact': 'business-policies', 'registry': 'module-registry', 'kinds': ['POL']} [MAJOR]
- **C4** PRD → SRS (human PRD approval in between): C4.1 exists {'artifact': 'prd'} [CRITICAL]; C4.2 gate-approved {'gate': 'prd-approval'} [CRITICAL]; C4.3 ids-owned {'stage': 'P0.5'} [CRITICAL]; C4.4 traces {'from': 'US', 'to': ['POL'], 'min': 1} [MAJOR]; C4.5 no-questions {'stage': 'P0.5'} [CRITICAL]; C4.6 languages {'stage': 'P0.5'} [MAJOR]

---
# ENGINE
# PRD — ENGINE

```
Engine        : PRD
Stage id      : P0.5
Pass          : 1
Questions     : allowed — the LAST stage that may ask; resolved in-dialogue, user confirms
Dialogue      : yes — lane analysis (claude:opus; ≤ 4 rounds; converge on "mutually-acceptable"; output: resolved-decisions)
Inputs        : platform-summary, module-registry, business-policies
Produces      : prd-{mod}.md
Owns IDs      : US   → `{prefix}-{MOD}-{seq}` (seq width 3); each traces to POL
Gate after    : prd-approval (human-approval) — blocks P1
Next          : P1
Module        : MDL   Version: 1
Profile       : erp — ERP Platform
```

This engine restates what the previous stage established about a module — its scope,
its policies, its priorities — as a set of clear, traceable **user stories**. A user
story is a NEED, never a RULE. If a story reads like an enforceable rule ("the system
shall reject any submission where X"), soften it back to intent ("the requester needs
to know the submission will not go through if X") and let `P1` decide the rule.

Completion (write → registry → analyze → commit → gate) is owned by the orchestrator —
see `shared/GOVERNANCE-CORE.md`. In a delta version (version > 1): read
`_state/current-{artifact}` of the previous version for every input and for this
stage's own artifact, and emit only ADDED / MODIFIED / REMOVED stories plus the
`change-manifest.md` per `shared/VERSIONING.md`.

Language policy: narrative in `ar`; story titles carry all of `ar, en`.

---

## 1 — Inputs and entry check

```
  platform-summary  : ✓ present / ✗ MISSING
  module-registry   : ✓ present / ✗ MISSING
  business-policies : ✓ present / ✗ MISSING
  Module             : MDL
```
A missing input is a pipeline error (the orchestrator does not start this stage
without it) — not a question for the user. The domain-profile STEERING block and the
project-registry are read for vocabulary and ownership; the knowledge sources are read
for recommended answers (§4).

---

## 2 — User story record (`US` — mandatory format)

```
US-[MOD]-[SEQ]
  Title          : [short name — in each of ar/en]
  Story          : As a [role], I need [capability], so that [outcome]   — a NEED
  Priority       : HIGH / MEDIUM / LOW — only if stated or clearly implied; otherwise "—"
  Success metric : only if stated; otherwise "—"
  Traces         : POL-[MOD]-[SEQ] [, …]   — the policies this story serves
  Source         : [document § / quote]   — MANDATORY, no exceptions
  Status         : DRAFT → APPROVED (by the PRD approval gate)
```

```
MANDATORY SOURCING RULE
  A story with no Source is a contract violation. Do not write one "to be thorough":
  an untraceable story looks governed when it is not.
TRACES RULE
  A story that serves no policy carries "Traces: — (scope only)" and cites the
  platform summary or module registry line that motivates it. `gov.py analyze`
  reports dangling POL references.
SEQUENCE RULE
  Continuous per module, never reused; a re-run continues from the highest existing
  sequence; an APPROVED story is never edited in place — amend via a new story.
```

---

## 3 — Extraction rules

```
FROM the business policies
  every policy → at least one story that expresses the NEED behind it (Traces: that policy)
  stated priorities and success criteria → Priority / Success metric
FROM the module registry
  each entity owned → "the [role] needs to manage [entity]" is a legitimate seed
  lookups owned / consumed, dependencies → integration needs — only when explicit
FROM the platform summary
  tier / dependency classification → cross-module needs — only if stated, never inferred
FROM the domain-profile
  scope statements → what NOT to write; vocabulary → use verbatim

DO NOT EXTRACT
  validation rules, data constraints, API shapes, permission matrices — those are
  P1 outputs. If a policy already states a mechanism, restate it at NEED level.
  Stories for entities or capabilities the inputs never mention.
```

---

## 4 — Questions (allowed — for the last time in the pipeline)

```
A QUESTION exists only when the inputs leave a story's scope, priority or role
genuinely ambiguous. It is never a request for a rule or a mechanism.

QUESTION — [point]
  Affects       : US candidates [list] / scope
  Options       : A) … (trade-off)   B) … (trade-off)
  Researched    : [what the knowledge sources say — cited: profiles/erp/knowledge/erp-domain-standards.md]
  Recommended   : [option] — because [rationale]
```

Lane `analysis`: the implementers (claude:opus) converge on each QUESTION —
challenge, answer, ≤ 4 rounds, until **mutually-acceptable** — and the converged
recommendation is presented to the user, who confirms or adjusts.

```
Resolutions are recorded in the RESOLVED DECISIONS table of the PRD. No external
open-questions file. Because no later stage may ask, every question MUST be closed
before the approval gate: an item the user will not decide is written as a story
with Status DEFERRED and an explicit "out of scope for v1" note — never left open.
```

---

## 5 — Extraction report (emitted before the PRD)

```
══════════════════════════════════════════════════════════════════
PRD EXTRACTION REPORT — MDL — [date]
══════════════════════════════════════════════════════════════════
STORIES DRAFTED
  + US-MDL-001 — [one line] — Traces: [ids] — Source: [ref]
STORIES SKIPPED (no traceable source)
  — [what was considered and why it was not written]
QUESTIONS RAISED → RESOLVED IN DIALOGUE
  ? [point] → [resolution] (confirmed by user: yes/no)
POLICIES WITHOUT A STORY (must be empty)
  — [policy id] — [why]
══════════════════════════════════════════════════════════════════
```
The report precedes the file in the run output; its counts become the registry
event row (§7).

---

## 6 — Output template — `prd-{mod}.md`

```markdown
# PRD — [Module display] (MDL)
══════════════════════════════════════════════════════════════════
Module          : MDL     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : [N]   Policies covered : [N]/[N]   Deferred : [N]
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES
US-MDL-001
  Title / Story / Priority / Success metric / Traces / Source / Status   (record §2)
(repeat per story, in sequence order)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
(every policy of the module appears in at least one row; a policy with no story is a
 completeness finding)

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |

## DEFERRED
| US | Reason | Activation trigger |

## APPROVAL
Approved by : [user]   Date : [date]
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════
```

---

## 7 — Registry step content

```
project-registry : module row → "PRD v1: [N] stories"; open-question rows resolved
                   by this dialogue → RESOLVED (+ resolution); pipeline status P0.5 = DONE
event history    : "P0.5 completed: MDL v1 — [N] stories, [N] decisions"
```
(There is no separate registry artifact for this stage; the story index lives in the
PRD's traceability table.)

---

## 8 — Never produce

```
✗ any ID owned by another stage (POL, REQ, AC, ENT, RULE, DBF, XM, API, QR, UXD, SCR, SCR-REQ, TC)
✗ enforceable validation logic, field-level constraints, API shapes, permissions
✗ a story with no Source, or a story invented to "fill out" the PRD
✗ an open question left unresolved at the gate
✗ padding — produce the PRD as fast as the sourcing discipline allows; the gate is
  about the file's existence and approval, not its volume
```

---

## 9 — PRD approval gate

```
Gate `prd-approval` (type human-approval) follows this stage and blocks P1.
The user approves THIS FILE. After the user approves it:
  - no stage may raise a question — P1 and every later stage resolve
    ambiguity themselves: non-breaking → ADR + continue; breaking → ADR BLOCKED + stop
    (`factory.yaml → ambiguity`, shared/GOVERNANCE-CORE.md);
  - the approved stories are frozen for v1; a change is a new story in a
    new version (shared/VERSIONING.md).
```

---

## 10 — Self-check before emitting

- [ ] Every story has Title, Story (As a / I need / So that), Traces, Source, Status.
- [ ] Every policy of the module is traced by at least one story; the traceability table is complete.
- [ ] No story states a mechanism (rule, constraint, API, permission).
- [ ] Every question raised is in RESOLVED DECISIONS or DEFERRED with the user's explicit decision.
- [ ] Sequence numbers continuous; no APPROVED story edited in place.
- [ ] Vocabulary matches the domain-profile STEERING block verbatim.
- [ ] Titles carry all of `ar, en`.


---
# INPUTS (generated current state)

<<<INPUT: platform-summary>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: module-registry>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: business-policies>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
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


==============================================================================
# BRIEF — stage `P1` (SRS) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/MDL/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: REQ, AC, ENT, RULE, SCR-REQ — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/MDL/P1/srs-mdl.md`
- `erp/modules/MDL/P1/registry-srs-mdl.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C4** PRD → SRS (human PRD approval in between): C4.1 exists {'artifact': 'prd'} [CRITICAL]; C4.2 gate-approved {'gate': 'prd-approval'} [CRITICAL]; C4.3 ids-owned {'stage': 'P0.5'} [CRITICAL]; C4.4 traces {'from': 'US', 'to': ['POL'], 'min': 1} [MAJOR]; C4.5 no-questions {'stage': 'P0.5'} [CRITICAL]; C4.6 languages {'stage': 'P0.5'} [MAJOR]
- **C5** SRS → database: C5.1 exists {'artifact': 'srs'} [CRITICAL]; C5.2 ears {'kind': 'REQ', 'patterns': 'factory.ids.ears.patterns'} [CRITICAL]; C5.3 traces {'from': 'REQ', 'to': ['US'], 'min': 1} [MAJOR]; C5.4 orphans {'kind': 'REQ', 'referenced_by': ['AC'], 'min': 1} [CRITICAL]; C5.5 traces {'from': 'AC', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.6 traces {'from': 'RULE', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.7 ids-owned {'stage': 'P1'} [CRITICAL]; C5.8 registry-agree {'artifact': 'srs', 'registry': 'registry-srs', 'kinds': ['REQ', 'AC', 'ENT', 'RULE']} [MAJOR]; C5.9 no-questions {'stage': 'P1'} [CRITICAL]; C5.10 languages {'stage': 'P1'} [MAJOR]; C5.11 ids-continue {'stage': 'P1'} [CRITICAL]
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]
- **C10** acceptance criteria → test generation (standalone): C10.1 traces {'from': 'TC', 'to': ['AC'], 'min': 1} [CRITICAL]; C10.2 orphans {'kind': 'AC', 'referenced_by': ['TC'], 'min': 1} [MAJOR]; C10.3 markers {'artifact': 'backend-test-plan', 'track': 'backend', 'plan': 'test'} [CRITICAL]; C10.4 markers {'artifact': 'frontend-test-plan', 'track': 'frontend', 'plan': 'test'} [CRITICAL]; C10.5 ids-owned {'stage': 'test-gen'} [CRITICAL]; C10.6 exists {'artifact': 'test-execution-manifest', 'when': 'profile.stack.testing.manifest'} [MINOR]

---
# ENGINE
# SRS — ENGINE

```
Engine        : SRS
Stage id      : P1
Pass          : 1
Questions     : forbidden — ambiguity is self-resolved (§9)
Lane          : analysis
Inputs        : prd, domain-profile, project-registry   (PRD must be APPROVED — gate prd-approval)
Produces      : srs-{mod}.md · registry-srs-{mod}.md (registry)
Owns IDs      : REQ, AC, ENT, RULE, SCR-REQ   → `{prefix}-{MOD}-{seq}` (seq width 3)
Format        : EARS for every functional requirement (§4)
Next          : P2
Module        : MDL   Version: 1
Profile       : erp — ERP Platform
```

This engine produces the module's **functional truth**: the SRS. Everything downstream
(database, execution plans, UX, tests) derives from it; when a downstream artifact
disagrees with the SRS, the SRS governs and the other artifact is corrected. The SRS
never contains DDL, execution phases, component names or any ID owned by a later stage.

Completion (write → registry → analyze → commit) is owned by the orchestrator — see
`shared/GOVERNANCE-CORE.md`. In a delta version (version > 1): read
`_state/current-{artifact}` of the previous version for every input and for this
stage's own artifacts, and emit only ADDED / MODIFIED / REMOVED elements plus the
`change-manifest.md` per `shared/VERSIONING.md`. ID sequences continue from the current state.

Language policy: narrative in `ar`; every label, screen name and message carries all
of `ar, en` — a single-language message is INCOMPLETE; identifiers, IDs and field names in
the domain-profile's identifier language.

### IDs this stage assigns

| Atom | Meaning | Traces to | Requires |
|---|---|---|---|
| `REQ` | requirement (EARS) | US | AC |
| `AC` | acceptance criterion (Given/When/Then) | REQ | — |
| `ENT` | entity | — | — |
| `RULE` | business rule | REQ | — |
| `SCR-REQ` | screen requirement | REQ | — |

Sequences are continuous per module and per atom, never reused.

---

## 1 — Inputs and reading protocol

```
STEP A — PRD (approved): every P0.5 story with its Traces → the demand this SRS must cover
STEP B — domain-profile.md §7 STEERING: vocabulary (verbatim), bounded contexts, codes,
         identifier rules, knowledge sources; §8 resolved decisions — never re-opened
STEP C — project-registry.md (shared/REGISTRY-SCHEMA.md categories):
         entity ownership + shared declarations → reuse, never re-create
         dependency index                        → XM candidates to carry into A8
         structural registry                      → names already fixed by other modules
         open-question index                      → must be empty for this module
STEP D — this stage's upstream module artifacts (module registry, business policies):
         entities owned / lookups / dependencies → names as given
         policies                                → the rules and requirements they imply
STEP E — knowledge sources (cite when applying a default)
         - profiles/erp/knowledge/erp-domain-standards.md
```

### 1.1 Registry pre-check (before any generation)

```
Does this entity already exist (any module)?      → reuse its ID; do not re-create
Does an equivalent lookup exist?                    → reuse its key
Does the module exist in the registry?              → extend, continue sequences
Naming conflict with a registered element?          → §9: ADR (non-breaking) or ADR BLOCKED
A closed decision / resolved open item affects it?  → apply as-is, no deviation
```

### 1.2 Feature type (auto-determined, documented, not asked)

```
Entity kinds (profile.vocabulary.entity_kinds): master / transactional / lookup / config / security
For every entity: kind + reason (one line), recorded in A3. The user is not asked.
```

---

## 2 — Resolution order (zero questions)

Before writing any section, answer every needed fact from, in order:

```
1. business policies (P0)   → apply directly; cite the policy id
2. PRD stories (P0.5)            → the need; scope and priority
3. registries (project + module)         → ownership, names, dependencies
4. knowledge sources (§1 STEP E)         → domain defaults — apply and document as DEFAULT
5. domain-profile STEERING + rules       → vocabulary, constraints
6. domain best practice                  → choose, document as ADR (§9)

DEFAULT documentation (inline, where applied):
  DEFAULT : [what was decided]
  Source  : [knowledge file § / domain-profile §]
  Override: [what to change if the client wants otherwise]
```

Nothing is asked. A fact that 1–6 cannot settle is an ambiguity → §9.

---

## 3 — Entities (`ENT`)

### 3.1 Ownership classification — first step, before any ID

| Classification | Rule | Declaration |
|---|---|---|
| PRIVATE | fully owned by this module | `ENT-MDL-[SEQ]` — [name] — PRIVATE |
| SHARED (owner) | other modules consume read-only | `ENT-MDL-[SEQ]` — [name] — SHARED (owner) |
| SHARED (consumer) | mastered by another module — NO new ID | consumes `ENT-[OWNER]-[SEQ]` — HARD-FK / SOFT-READ → XM candidate for P2 |

Entities already registered by another module are consumed, never re-created.

### 3.2 Defaults per entity kind (profile.conventions.entity_defaults)

Every entity of a kind below carries these fields automatically (in A3 they are
listed once under "standard fields — per profile", not re-typed per entity):

| Kind | Default fields |
|---|---|
| master | nameAr, nameEn, code, isActiveFl, createdBy, createdAt, updatedBy, updatedAt |
| transactional | docNo, docDate, statusCode, fiscalYearId, periodId, createdBy, createdAt, updatedBy, updatedAt |
| lookup | code, nameAr, nameEn, sortOrder, isActiveFl |
| config | key, valueAr, valueEn, isActiveFl |

Naming (profile.stack.db.naming): primary key `{entity}Pk`; flag fields end with `Fl`; audit fields `createdBy, createdAt, updatedBy, updatedAt` are system-filled and never accepted from a client. Field names are
taken from the module registry, the policies and the knowledge sources — never invented
from generic templates. Physical types belong to P2; the SRS states the
logical type only (text / number / decimal / flag / date-time / lookup / reference).

### 3.3 Structural rules

```
ARCH-1  Reuse before create — registry first.
ARCH-2  Master data has ONE owner; others consume via XM (never duplicated).
ARCH-3  Every referenced entity is defined (own ID) or consumed (owner's ID); no
        undefined reference anywhere.
ARCH-4  Names match the registry exactly.
ARCH-5  A SHARED (owner) entity that can be deactivated/deleted → a RULE stating the
        effect on SOFT-READ consumers (prevent / notify / cascade), traced to the REQ
        that introduces the operation. Undecidable → ADR (§9), never an open question.
LOOKUPS Profile rule: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs
        Lookup-backed fields reference a lookup key declared in A6; the SRS decides
        control type (fixed short list → lookup; growing/large set → reference entity
        with its own ENT). Same field → same lookup key in every screen.
NUMBERING Profile rule: document numbers come from the platform numbering engine; never generated in a module
        Business/document numbers: decided PER ENTITY, never module-wide. An entity gets
        a business number only if (a) its identifier is used outside the system, (b) a
        policy/story asks for a human-readable reference, or (c) it is the numbered
        transactional document itself. If yes → system-generated on first save,
        read-only after, unique per entity type; the RULE cites the numbering rule above.
WORKFLOW Profile: workflow engine `forbidden`. Status lifecycles (a status field +
        allowed transitions) are always documented (A7). A module-specific approval flow
        is written only when a story explicitly asks for it and is custom to the module —
        never a generic engine.
```

---

## 4 — Requirements (`REQ`) — EARS is mandatory

Every functional requirement is **exactly one** EARS pattern
(`factory.ids.ears.patterns`); free prose is a contract violation (`gov.py analyze`:
CRITICAL).

```
  ubiquitous  The system shall <response>
  state       While <condition / trigger / feature>, the system shall <response>
  event       When <condition / trigger / feature>, the system shall <response>
  optional    Where <condition / trigger / feature>, the system shall <response>
  unwanted    If <condition / trigger / feature>, then the system shall <response>
  complex     a legitimate composition of the above (e.g. While … , when … , the system shall …)
```

```
REQ-MDL-[SEQ] — [short name]
  Pattern    : [ubiquitous | state | event | optional | unwanted | complex]
  Statement  : [one EARS sentence — one behaviour, one subject "the system"]
  Traces     : P0.5-MDL-[SEQ] [, …]      (≥ 1, mandatory)
  Entities   : ENT-MDL-[SEQ] [, …]
  Rationale  : [one line — why]
  Source     : [PRD story / policy / knowledge file / ADR]
  Priority   : [from the story]
```

Rules: singular (one requirement per REQ — "and" between behaviours means two REQs);
verifiable (a test can pass/fail it); no design (no table, endpoint, component);
every P0.5 story is covered by ≥ 1 REQ; every REQ traces to ≥ 1 story
(a REQ with no story = invented scope → remove or raise an ADR).

### 4.1 Acceptance criteria (`AC`) — ≥ 1 per REQ

```
AC-MDL-[SEQ] — [REQ-MDL-[SEQ]]
  Given  : [precondition / state]
  When   : [action / event]
  Then   : [observable outcome — with the exact message when one is shown, in each of ar/en]
```

Rules: each AC tests one path of one REQ (happy path first, then each unwanted/edge
path); an AC that cannot be phrased Given/When/Then means the REQ is not verifiable —
rewrite the REQ. ACs are the mechanical source of test cases for the standalone
test-gen stage (`TC` traces to `AC`).

---

## 5 — Business rules (`RULE`)

```
RULE-MDL-[SEQ] — [short name]
  Scope      : ENT-MDL-[SEQ]
  Trigger    : [when evaluated — on create / update / submit / transition …]
  Statement  : The system shall [prevent / require / validate …] when [condition]
  Message    : ar: [text] · en: [text]   (business language, not a literal translation)
  Traces     : REQ-MDL-[SEQ] [, …]                     (≥ 1, mandatory)
  Source     : [policy id / story / DEFAULT / ADR]
  Test-Hint  : [optional, one line of business intent for test-gen; omit if obvious]
```

Rules: a RULE formalises a constraint that a REQ needs; it never introduces behaviour
absent from every REQ. Database errors never reach users — every constraint that can
fail has a RULE with a message. Rules are defined once (A5) and referenced by ID from
every screen block.

---

## 6 — Lookups and status lifecycle

```
A6 LOOKUPS — one block per lookup key this module OWNS
  Key · used by field(s) · ENT · control type · owner (this module / consumed from [code])
  · values (code + label per ar/en) · source (policy custom values / knowledge default)
  Consumed lookups are listed by key + owner only (never redefined).

A7 STATUS LIFECYCLE — for every entity with a status field and > 2 transitions
  Diagram of states and allowed transitions ONLY — no roles, no approval steps.
  Each transition that carries a constraint → RULE id.
  Module-specific approval flow (only if a story explicitly asks and the profile allows):
  documented as its own block with the story it traces to.
```

---

## 7 — Screens for `P3.2` (`SCR-REQ`)

This stage lists what screens the module needs — functional scope, not design.
`P3.2` turns each entry into `SCR` / `UXD` decisions and owns pattern,
container, layout and component choices. This stage never decides those.

```
SCR-REQ-MDL-[SEQ] — [screen name in each of ar/en]
  Purpose      : [what the user achieves]
  Entities     : ENT-MDL-[SEQ] [, …]
  Operations   : [search / list / create / read / update / deactivate / custom …]
  Users        : [roles]
  Navigation   : [module] → [menu] → [screen]; from: [screens]; to: [screens]
  Content shape: [flat record | header + repeating lines with totals | true hierarchy
                  (parent/child) | other (journal, calendar …)] — a hint, not a design
  Traces       : REQ-MDL-[SEQ] [, …]
  Composite    : Search + Entry (or Master + Detail, Wizard) = ONE screen requirement
                 (profile.conventions.composite_screen) — never one per sub-screen
```

Screen rules: search filters correspond to result columns; the same field uses the same
lookup key in search and entry; every screen declares its navigation position; every
operation on a screen is backed by a REQ.

### 7.1 Access (profile.conventions.security_model)

```
Page registry : SEC_PAGES   — one row per screen requirement
Permission    : PERM_<PAGE_CODE>_<ACTION>  with actions VIEW / CREATE / UPDATE / DELETE
Gateway       : VIEW — without it no other action applies
```
The SRS declares, per screen requirement, its page code and which roles hold which
action. It does NOT enumerate permission names as seed data — the security module
derives them from the page code (a second source of truth is a DUPLICATE finding).
No hardcoded authorisation logic is specified anywhere; checks go through the platform
authorisation layer. Implementation (annotations, guards) belongs to later stages.

---

## 8 — API expectations (stack-neutral)

The SRS states what operations the backend must expose; `P3.1` assigns the
`API` ids and designs them. Expectations follow the profile's conventions
(profile.stack.backend.api) and are referenced from screens by REQ, never by an API id.

```
Base path      : /api/v1/{module}/{resource}
Verbs          : POST = create · GET = read · PUT = update · DELETE = deactivate (soft) · PATCH = partial
Response       : ApiResponse<T>
Paging         : Page<T>
Errors         : LocalizedException → {code, messageAr, messageEn}

| Operation | Verb | Path (per base path) | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| create [entity] | [verb] | [base path with module/resource] | [fields] | [entity] | RULE-… | REQ-… |
| search [entity] | [verb] | … | [filters, paging] | [page of entity] | — | REQ-… |
| update [entity] | [verb] | …/{id} | [fields] | [entity] | RULE-… (immutability of number/code if any; uniqueness of names) | REQ-… |
| deactivate [entity] | [verb] | …/{id} | id | confirmation | RULE-… | REQ-… |
| read [entity] | [verb] | …/{id} | id | [entity] | — | REQ-… |
```
Lookup values are runtime-loaded (never enumerated in an API definition). Mobile or
channel-specific behaviour is stated as a REQ (`Where` pattern), not as widget design.

---

## 9 — Ambiguity rule (questions are forbidden here)

```
When steps 1–6 of §2 leave a genuine choice:
  NON-BREAKING (does not contradict an approved story, a policy, a registry fact or
  a closed decision) → write an ADR and CONTINUE with the chosen best practice.
      action: adr · then: continue
  BREAKING (contradicts an approved story / policy / registry fact / closed decision,
  or would change a REQ another artifact already relies on) → write the ADR with
  status BLOCKED and STOP the pass; the orchestrator surfaces it at the next
  human point.
      action: adr · status: BLOCKED · then: stop

ADR file : erp/decisions/MDL/ADR-{MOD}-{seq:03d}.md
Content  : Context · Decision · Consequences · traces (REQ / ENT / story ids) · status
Every ADR is referenced from the SRS "Decisions applied" section (§10 STANDALONE).
Details: shared/GOVERNANCE-CORE.md.
```

---

## 10 — `srs-{mod}.md` — canonical template

Structure: **PART A** (module foundation — defined once) → **PART B** (one block per
screen requirement — references PART A by ID, never redefines) → **STANDALONE**.
No section is omitted; a section that does not apply says so in one line.

```markdown
# SRS — [Module display] (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved [date])
Counts : ENT [N] · REQ [N] · AC [N] · RULE [N] · SCR-REQ [N] · ADR [N]
══════════════════════════════════════════════════════════════════

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |   (module, feature code, version, date, status, prepared by, decisions applied count)

## A2 — Functional context
In scope · Out of scope · Module function (one paragraph) · Detailed description
(workflow narrative, roles) · Current situation (steps / party / notes) ·
Current difficulties · Proposed system and benefits · General notes (constraints,
deferred items) — delete a sub-section only if it has no content and say so.

## A3 — Entities and fields
Standard fields per kind (profile) — listed once here.
### ENT-MDL-001 — [name]
| Kind | Ownership | Business number (yes/no — per §3.3 test) | Operations | Cross-module | Source |
| Field | Logical type | Required | Values / source (lookup key, ENT ref) | Notes | Label-ar | Label-en |
(repeat per entity)

## A4 — Functional requirements (EARS) and acceptance criteria
### REQ-MDL-001 — [name]        (record §4)
#### AC-MDL-001 … (record §4.1, ≥ 1 per REQ)
(repeat per requirement)

## A5 — Business rules
### RULE-MDL-001 — [name]       (record §5)
(repeat per rule — the ONLY place rule text appears)

## A6 — Lookups                      (§6 — the ONLY place lookup values appear)

## A7 — Status lifecycle             (§6 — diagram only; "not applicable" if ≤ 2 states)

## A8 — Module dependencies
| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
| External service | Purpose | Integration kind |

# PART B — SCREEN REQUIREMENTS   (one block per SCR-REQ; references PART A by ID only)

## SCR-REQ-MDL-001 — [name]
### B1 — Definition        (record §7: purpose, entities, operations, users, navigation, content shape, traces)
### B2 — Search / list     (filters = result columns; lookup keys by reference; RULEs applied by id) — "not applicable" if no search
### B3 — Input             (fields by ENT reference; buttons/actions → operation + RULE ids)
### B4 — Access            (page code + roles per action per §7.1)
### B5 — API expectations  (table §8, scoped to this screen)
(repeat per screen requirement)

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
(every story → ≥ 1 REQ; every REQ → ≥ 1 AC; every RULE → REQ; every SCR-REQ → REQ.
 Orphans and dangling references are gate failures — `gov.py analyze`.)

## Decisions applied
| DEFAULT / ADR | What | Source | Override / status |

## Access summary            (aggregate of B4 — B4 is the source)
══════════════════════════════════════════════════════════════════
```

Single-source rule: PART A defines; PART B references by ID ("applies RULE-MDL-003").
Restating rule, lookup or entity text in PART B is a DUPLICATE finding (MAJOR).

---

## 11 — `registry-srs-{mod}.md` — registry content

```
## REGISTRY — P1 — MDL v1
Entities      : ENT id · name · kind · PRIVATE / SHARED(owner) · status REGISTERED
Consumed      : owner ENT id · owner module · HARD-FK / SOFT-READ   (→ dependency index)
Lookups owned : key · ENT · values count          Lookups consumed : key · owner
Screens       : SCR-REQ id · name · page code
Requirements  : REQ count · AC count · RULE count · last sequence per atom
                (REQ: [n], AC: [n], ENT: [n], RULE: [n], SCR-REQ: [n])
Decisions     : ADR ids (+ BLOCKED, if any)
Event         : "P1 completed: MDL v1 — [counts]"
```
The orchestrator merges these rows into `project-registry.md` (entity ownership,
shared declarations, dependency index, structural registry, pipeline status, events).

---

## 12 — Boundaries

```
OWNS      : REQ, AC, ENT, RULE, SCR-REQ · functional truth · the traceability matrix from stories down
DOES NOT  : POL (P0) · US (P0.5) · DBF (P2) · XM (P2) · API (P3.1) · QR (P3.1) · UXD (P3.2) · SCR (P3.2) · TC (test-gen)
            · DDL / physical types · execution phases · UX patterns, containers, components
            · endpoint design · permission seed data · test cases
```

---

## 13 — Self-check before emitting (ISO/IEC/IEEE 29148 attributes + structure)

Quality attributes scored at the pass gate (`factory.review.rubric`):
- [ ] **unambiguous** — one reading per REQ / AC / RULE; no "etc.", "as appropriate", "fast"
- [ ] **verifiable** — every REQ has ≥ 1 Given/When/Then AC; every RULE has a message
- [ ] **complete** — every story covered; A1–A8, every B1–B5, STANDALONE present; no placeholder left
- [ ] **consistent** — vocabulary = STEERING block; names = registry; no REQ contradicts a policy or another REQ
- [ ] **singular** — one behaviour per REQ; one path per AC
- [ ] **feasible** — no requirement depends on an undefined entity, unavailable module or forbidden mechanism
- [ ] **traceable** — REQ→P0.5, AC→REQ, RULE→REQ, SCR-REQ→REQ all present; no orphan, no dangling id

Structural checks:
- [ ] Every REQ statement matches exactly one EARS pattern.
- [ ] Every entity has a kind from `master, transactional, lookup, config, security` and carries its kind's default fields.
- [ ] Every consumed entity references the owner's ENT id; none re-created.
- [ ] Rules, lookups, entities defined in PART A only; PART B references by ID.
- [ ] Every DEFAULT has Source + Override; every ADR is listed under Decisions applied; no BLOCKED ADR unless the pass stopped.
- [ ] No question raised anywhere; no open-questions section exists.
- [ ] Sequences continuous per atom.
- [ ] Every label, screen name and message carries all of `ar, en`.
- [ ] Profile check `ERP-1` (MAJOR): every master entity carries the master entity_defaults.
- [ ] Profile check `ERP-2` (MAJOR): every screen maps to exactly one composite SCR and one SEC_PAGES row.


---
# INPUTS (generated current state)

<<<INPUT: prd>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: domain-profile>>>
# DOMAIN PROFILE — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile         : erp (ERP Platform)
Version         : 1
Last Updated    : 2026-09-10
Status          : FRESH
Research        : 3 sources cited (block 9)
══════════════════════════════════════════════════════════════════

## 1. SCOPE

**داخل النطاق (In bounds):** بناء منصة ERP متعددة الوحدات (multi-module) بعمارة قائمة على
البيانات لا الشيفرة ("everything that can change = defined data, not code")، تُبنى بلغة
Spring (خلفية) و React (واجهة) وقاعدة بيانات **PostgreSQL** كهدف بناء جديد. النظام القديم
Oracle/ADF يبقى فقط كمصدر أحداث علوي (upstream event source) ولا يُبنى عليه أي وحدة جديدة.

دفعة العمل الحالية (هذه الجلسة) تُغطّي ثلاث وحدات أساسية مشتركة ومؤسِّسة:
1. **وحدة الأمان (SEC)** — مصادقة + تحكم هرمي بالصلاحيات (Module → Screen → Action).
2. **وحدة البيانات المرجعية (MDL/Lookup)** — مركز موحّد لكل قوائم القيم المُرمَّزة.
3. **وحدة الحسابات العامة (FIN/GL)** — دفتر أستاذ عام قابل للتوصيل (pluggable) بأي نظام مضيف.

باقي وحدات المنصة (ORG, PRC, HR, INV, SLS, CTR) معروفة الرمز والسياق ضمن `profiles/erp.yaml`
لكنها **خارج نطاق هذه الدفعة** — تُفصَّل لاحقًا بنفس الأسلوب.

**خارج النطاق صراحة (Out of bounds, stated by the user):**
- أي منطق عمل خاص بمجال مضيف (host business world) داخل وحدة الحسابات.
- تعدد العملات، تعدد الدفاتر/الكيانات، الحسابات الإحصائية، التقويم متعدد الأنماط،
  والمرفقات (attachments) — مستبعدة صراحة من GL في هذه الدفعة.
- محرك سير العمل (workflow engine) — ممنوع بحكم `profiles/erp.yaml → conventions.workflow_engine: forbidden`.
- طبقة النقل (AQ/RabbitMQ) ومستهلك الأحداث (Event consumer) والوحدة التجارية (Business Module)
  — خارج نطاق وثيقة FIN، توثَّق في مكان آخر.

## 2. PURPOSE

المنصّة تحل مشكلة تكرار وتضارب القدرات المشتركة (الهوية، الصلاحيات، البيانات المرجعية)
عبر كل وحدة أعمال جديدة: بدل أن تبني كل وحدة نظام دخول وصلاحيات وقوائم قيم خاصًا بها
(ما يُنتج ازدواجية وتضاربًا حتميًا)، تُبنى هذه القدرات **مرة واحدة، بشكل عام (generic) بالكامل**،
وتُستهلك بيانيًا من أي وحدة عمل — بدءًا بالحسابات العامة كأول وحدة عمل تستهلكها.
لكل واحدة من الوحدات الثلاث سبب وجود مستقل:
- **SEC** يحل مشكلة تشتّت الهوية والصلاحيات: نظام أمان واحد للمنصة بأكملها.
- **MDL** يحل مشكلة انحراف القيم المرجعية (reference-data drift) بين الوحدات.
- **FIN** يحل مشكلة الاعتماد على أرصدة مخزَّنة وغير موثوقة في الأنظمة القديمة، عبر دفتر
  أستاذ يُشتق كل رصيد فيه من القيود المُرحَّلة فقط، لا من عمود رصيد مُجمَّع يفقد التزامنه.

## 3. RESPONSIBILITIES

| الوحدة | تملك | لا تملك |
|---|---|---|
| SEC | تسجيل الدخول/التسجيل/استرجاع كلمة المرور، المستخدمون، الأدوار، التفويض الهرمي (Module→Screen→Action)، القائمة الديناميكية، لوحة تحكم الأمان، سجل التدقيق | أي منطق عمل خاص بوحدة أخرى |
| MDL | نوع اللوكب (Lookup Type) + قيمه (Lookup Value)، شاشة عامة واحدة Master-Detail لكل القوائم، تسجيل الملكية (namespacing) لكل وحدة | معنى القوائم الخاص بمجال وحدة أخرى — الملكية الدلالية تبقى للوحدة المسجِّلة |
| FIN | شجرة الحسابات والأبعاد، محرك القواعد (event_type → قيد)، دورة حياة القيد، دورة حياة الفترة المحاسبية، التقارير المالية المُشتقة | أي مستخدمين/أدوار/تسجيل دخول خاصة بها، أي جدول lookup خاص بها، أي معرفة بالعالم التجاري المضيف |

## 4. MAIN COMPONENTS

| # | Component | Module code | Bounded context | Category (user-defined) | Core / extension | Notes |
|---|-----------|-------------|-----------------|--------------------------|------------------|-------|
| 1 | الأمان / Security | SEC | organization | Foundation | Core | KB §1 Tier 0 — يجب أن يوجد قبل أي وحدة أعمال؛ لا وحدة تملك أمانها الخاص |
| 2 | البيانات المرجعية / Master Data Lookup | MDL | organization | Foundation | Core | KB §1 Tier 0 — مركز واحد لكل القوائم المُرمَّزة عبر المنصة |
| 3 | الحسابات العامة / Finance (General Ledger) | FIN | finance | Business — Tier 1 | Core | KB §1 Tier 1 — أول وحدة عمل تستهلك SEC وMDL؛ قابلة للتوصيل بأي مضيف |
| 4 | الهيكل التنظيمي / Organization | ORG | organization | Foundation | (غير مفصّلة هذه الدفعة) | من `profiles/erp.yaml → vocabulary.module_prefixes` — PROPOSED سابقًا في الملف الشخصي، خارج نطاق هذه الجلسة |
| 5 | المشتريات / Procurement | PRC | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 6 | الموارد البشرية / Human Resources | HR | people | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 7 | المخزون / Inventory | INV | supply | Business — Tier 1 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 8 | المبيعات / Sales | SLS | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |
| 9 | العقود / Contracts | CTR | commercial | Business — Tier 2 | (غير مفصّلة هذه الدفعة) | من الملف الشخصي؛ خارج النطاق |

الصفوف 1–3 مصدرها خطط الوحدات الثلاث المرفقة لهذه الجلسة (security-module-plan-en.md،
lookup-module-plan-en.md، general-accounting-system-plan-en.md)؛ الصفوف 4–9 مصدرها
`profiles/erp.yaml → vocabulary.module_prefixes` المُثبَّت مسبقًا من قِبل المستخدم — تُدرَج هنا
للاكتمال المرجعي فقط ولا تُفصَّل في هذه الدفعة.

## 5. GOVERNING RULES

| # | القاعدة | المصدر |
|---|---|---|
| G1 | كل ما يمكن أن يتغير = بيانات مُعرَّفة، لا شيفرة (no hardcode / no duplication / no contradiction) | الخطط الثلاث §Governing rule؛ [KB:erp-domain-standards §2-3] |
| G2 | نظام أمان واحد للمنصة بأكملها؛ لا وحدة تملك مستخدمين/أدوار/دخولاً خاصًا بها | security-module-plan-en.md §2 |
| G3 | بوابة الوحدة (module gate) تُفحص أولاً، قبل أي فحص شاشة/إجراء؛ عدم امتلاك الوحدة = غياب تام من القائمة ومن الوصول المباشر بالرابط | security-module-plan-en.md §4.2 |
| G4 | مركز واحد وموثوق للبيانات المرجعية؛ لا وحدة تحتفظ بجداول lookup خاصة بها | lookup-module-plan-en.md §2؛ بحث: نمط "MDM hub" — راجع قسم 9 |
| G5 | التخزين والشاشة مركزيان، لكن المعنى الدلالي للقائمة يبقى ملكًا للوحدة المسجِّلة (namespacing بالمالك) | lookup-module-plan-en.md §2, §3 |
| G6 | الحسابات لا تعرف شيئًا عن العالم التجاري المضيف؛ لا اسم حساب داخل حمولة الحدث (event payload) | general-accounting-system-plan-en.md §3, §15 |
| G7 | تساوي المدين والدائن ثابت مطلق (debit = credit invariant) لكل قيد يصل POSTED | general-accounting-system-plan-en.md §12.1 |
| G8 | لا موافقة على مستوى القيد الفردي؛ نقطة التحكم البشري الوحيدة هي إغلاق الفترة، وفصل المهام بين منشئ القيد ومعتمد الإغلاق يُنفَّذ عبر SEC | general-accounting-system-plan-en.md §8.2, §10.3 |
| G9 | الأرصدة تُشتق دائمًا من القيود المُرحَّلة (POSTED) فقط، ولا يوجد عمود رصيد مُخزَّن يُعتمَد عليه | general-accounting-system-plan-en.md §11, §12.9 |
| G10 | الهدف الأول لقاعدة البيانات هو PostgreSQL؛ Oracle/ADF القديم مصدر أحداث فقط | الخطط الثلاث §1؛ توجيه GENERATION-INSTRUCTIONS.md |
| G11 | اللغتان العربية والإنجليزية إلزاميتان في كل قطعة مُسمّاة (وحدة/كيان/حقل/شاشة) | `profiles/erp.yaml → languages` |
| G12 | لا محرك سير عمل (workflow engine) في هذه المنصة | `profiles/erp.yaml → conventions.workflow_engine: forbidden` |

## 6. RELATIONSHIPS WITH OTHER DOMAINS

| This component | Depends on | Kind | Direction | Stated by |
|---|---|---|---|---|
| كل وحدة عمل (بما فيها FIN لاحقًا كل وحدة أخرى) | SEC | HARD (تسجيل الوحدة/الشاشات/الإجراءات كبيانات + بوابة الوحدة) | consumer → SEC | security-module-plan-en.md §7 |
| كل وحدة عمل (بما فيها FIN) | MDL | HARD (تسجيل أنواع lookup كبيانات + قراءة القيم) | consumer → MDL | lookup-module-plan-en.md §4 |
| FIN | SEC | HARD-FK (هوية + صلاحيات الشاشات/الإجراءات + SoD) | FIN → SEC | general-accounting-system-plan-en.md §2 |
| FIN | MDL | HARD-FK (طرق الدفع، أنواع أحداث المحاسبة، أنواع الحسابات، حالات الفترة، أنواع اليومية) | FIN → MDL | general-accounting-system-plan-en.md §5.2 |
| FIN | Notifications (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → NOTIF | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| FIN | File Service (جاهزة، خارج هذه الجلسة) | SOFT/EVENT — اختياري فقط | FIN → FILESVC | general-accounting-system-plan-en.md §2.3؛ `new project/integration-notifications-fileservice.md` |
| SEC | Notifications (جاهزة) | SOFT — اختياري (مثال: بريد إعادة تعيين كلمة المرور) | SEC → NOTIF | security-module-plan-en.md §8 |
| FIN | نظام مضيف (host business system) | EVENT فقط، عبر حدث محاسبي قياسي (canonical event)؛ لا قراءة ولا كتابة مباشرة لجداول المضيف | host → FIN (event only) | general-accounting-system-plan-en.md §3 |

## 7. STEERING  (read verbatim by every later stage)

### 7.1 Ubiquitous language

| Term (ar/en) | Definition | Do not say | Module code |
|---|---|---|---|
| بوابة الوحدة / Module gate | الفحص الأول والحاسم لامتلاك الدور للوحدة قبل أي فحص شاشة/إجراء؛ غيابها = غياب تام | "صلاحية الوحدة" بمعنى فضفاض | SEC |
| منح الشاشة / Screen grant | صلاحية وصول لشاشة محددة داخل وحدة ممنوحة فعلاً | "صلاحية القائمة" | SEC |
| منح الإجراء / Action grant | صلاحية VIEW/CREATE/UPDATE/DELETE أو إجراء مخصّص على شاشة ممنوحة | "دور" (Role يبقى مصطلحًا مستقلاً) | SEC |
| الشاشة المركّبة / Composite Screen | بحث + إدخال (أو رئيسي + تفصيلي، أو معالج) تُعامَل كشاشة واحدة بمعرّف SCR واحد | "صفحة" منفردة لكل جزء | (عام) |
| نوع اللوكب / Lookup Type | الـ"master" لقائمة قيم مُرمَّزة، يملكه اسميًا موديول مُسجِّل | "جدول Enum" | MDL |
| قيمة اللوكب / Lookup Value | الـ"detail" — قيمة مُرمَّزة ضمن نوع لوكب: كود، تسميتان، ترتيب، حالة نشاط | "قيمة ثابتة" في الشيفرة | MDL |
| الحدث المحاسبي القياسي / Canonical accounting event | مدخل الحسابات الوحيد؛ حدث جاهز الشكل قادم من مستهلك الأحداث خارج النطاق | "معاملة تجارية" (يحمل دلالة عالم المضيف) | FIN |
| شجرة الحسابات / Chart of Accounts | بنية هرمية للحسابات؛ الأوراق فقط تقبل ترحيلاً مباشرًا | "دليل حسابات" بلا بنية هرمية | FIN |
| البُعد / Dimension | مقطع بيانات (segment) يُعرَّف كبيانات ليُشكّل مع الحساب الأساسي تركيبة الترحيل | "تصنيف تحليلي" غامض | FIN |
| الترحيل / Posting | إدخال قيد بحالة POSTED مؤثرًا في الأرصدة وغير قابل للتعديل | "اعتماد" (الاعتماد مصطلح مختلف يخص إغلاق الفترة) | FIN |
| قيد اليومية / Journal Entry | وحدة الإدخال المحاسبي الأساسية (رأس + سطور متوازنة مدين/دائن) | "سند" فقط دون تحديد | FIN |
| الفترة المحاسبية / Accounting Period | نافذة زمنية بحالة (Open / Soft Close / Hard Close / Year-End Close) تتحكم بقبول الترحيل | "شهر مالي" | FIN |
| قيد العكس / Reversing Entry | تصحيح عبر قيد جديد مرتبط ثنائي الاتجاه بالأصل، مطابق سطرًا بسطر بعكس الاتجاه | "حذف القيد" (ممنوع) | FIN |
| ميزان المراجعة / Trial Balance | تقرير متوازن دائمًا، مُشتق من القيود المُرحَّلة فقط | "كشف حساب" | FIN |

### 7.2 Bounded contexts

| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | القدرات المؤسِّسة (Tier 0) التي يعتمد عليها أي سياق آخر: الهيكل التنظيمي، الأمان، البيانات المرجعية |
| supply | PRC, INV | تدفقات التوريد والمخزون (خارج نطاق هذه الدفعة) |
| finance | FIN | الحسابات العامة ودفتر الأستاذ؛ تستهلك organization ولا تُنتج له شيئًا |
| people | HR | الموارد البشرية (خارج نطاق هذه الدفعة) |
| commercial | SLS, CTR | المبيعات والعقود (خارج نطاق هذه الدفعة) |

(منقولة حرفيًا من `profile.vocabulary.bounded_contexts`)

### 7.3 Module prefixes proposal

| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE — هذه الدفعة |
| MDL | Master Data Lookup | IN PROFILE — هذه الدفعة |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE — هذه الدفعة |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

كل الرموز موجودة مسبقًا في `profiles/erp.yaml → vocabulary.module_prefixes`؛ لا رمز جديد اقترحته
هذه الجلسة.

### 7.4 Identifier rules

المعرّفات اللاحقة تُبنى بالصيغة `{prefix}-{MOD}-{seq}` بعرض تسلسل 3 خانات
(`factory.ids.pattern`, `factory.ids.seq_width`). أنواع الكيانات: master, transactional,
lookup, config, security (`profile.vocabulary.entity_kinds`).

### 7.5 Knowledge sources to cite

- `profiles/erp/knowledge/erp-domain-standards.md`
- `new project/security-module-plan-en.md` — المصدر التأسيسي لوحدة SEC
- `new project/lookup-module-plan-en.md` — المصدر التأسيسي لوحدة MDL
- `new project/general-accounting-system-plan-en.md` — المصدر التأسيسي لوحدة FIN، بما فيه القسم
  12 "details the analysis agent MUST honor" المُلزِم لكل تحليل لاحق
- `new project/integration-notifications-fileservice.md` — يُستخدم فقط عند حاجة فعلية مذكورة
  صراحة في إحدى الخطط الثلاث (لا يُعاد تصميمه)
- plus the research sources in block 9

## 8. RESOLVED DECISIONS

| # | Point | Decision | Recommended by dialogue? | Confirmed by user | Sources |
|---|---|---|---|---|---|
| 1 | هدف قاعدة البيانات | PostgreSQL هو الهدف الوحيد لهذا البناء؛ Oracle/ADF يبقى مصدر أحداث فقط | لا — منصوص صراحة | نعم — منصوص في الخطط الثلاث وفي أمر التنفيذ | الخطط الثلاث §1؛ GENERATION-INSTRUCTIONS.md |
| 2 | نموذج الأمان | RBAC هرمي بثلاث مستويات (Module→Screen→Action) بديلاً عن أي أمان محلي بالوحدات | لا — منصوص صراحة | نعم | security-module-plan-en.md §4 |
| 3 | نموذج البيانات المرجعية | مركز lookup عام Master-Detail واحد لكل المنصة، بدل جداول lookup محلية | لا — منصوص صراحة | نعم | lookup-module-plan-en.md §2-3 |
| 4 | نموذج اعتماد قيود المحاسبة | لا اعتماد على مستوى القيد الفردي؛ الاعتماد الوحيد عند إغلاق الفترة فقط | لا — منصوص صراحة، ويتوافق مع ممارسات GL الحديثة القائمة على الأحداث (انظر قسم 9، R1) | نعم | general-accounting-system-plan-en.md §8 |
| 5 | نطاق هذه الدفعة | SEC ثم MDL ثم FIN فقط، بهذا الترتيب الصارم؛ باقي الوحدات خارج النطاق الآن | لا — منصوص صراحة | نعم | GENERATION-INSTRUCTIONS.md §3 |
| 6 | استخدام Notifications/File Service | تكامل اختياري بحت، فقط عند حاجة صريحة يذكرها أحد الخطط الثلاث؛ لا إعادة تصميم لهما | لا — منصوص صراحة | نعم | الخطط الثلاث §8/§5/§2.3؛ GENERATION-INSTRUCTIONS.md §4.6 |

## 9. RESEARCH LOG

| # | Point | What established systems do | Source(s) (title, URL/path, date) | Used in |
|---|---|---|---|---|
| R1 | RBAC هرمي بمستوى module→screen→action | الأنظمة الناضجة تفصل "ما الذي يمكن فعله" (action) عن "أين" (scope/module)، وتستخدم الهرمية لتقليل تكرار الأدوار؛ التفويض الهرمي يُستخدم بحيث تتحكم صلاحية بوحدة كاملة وأخرى بإجراء داخلها | [How to Design an RBAC System — NocoBase](https://www.nocobase.com/en/blog/how-to-design-rbac-role-based-access-control-system), accessed 2026-09-10; [Access Control Design for Scalable RBAC Systems](https://www.loginradius.com/blog/identity/design-effective-rbac-system), accessed 2026-09-10 | G3، §7.1 "بوابة الوحدة" |
| R2 | مركز بيانات مرجعية موحّد (MDM hub) | النمط الشائع هو مركز واحد (hub) يخزن البيانات المرجعية/الأساسية ويُنشرها للأنظمة الأخرى؛ النطاقات (domains) تتوافق مع بيانات مرجعية مُدارة مركزيًا بدل نسخ محلية متضاربة | [Why the Data Hub is the Future of Data Management — Semarchy](https://www.semarchy.com/blog/backtobasics-mdm-hub-patterns/), accessed 2026-09-10 | G4/G5، وحدة MDL بأكملها |
| R3 | محاسبة قائمة على الأحداث (event-driven GL) | الأنظمة الحديثة تدمج دفتر الأستاذ مع معمارية قائمة على الأحداث: مصدر يُصدر معاملات، خدمة تحقق/تطبيع، ثم خدمة ترحيل تطبّق قاعدة القيد المزدوج وتُلحق القيود في مخزن إلحاقي فقط (append-only)؛ من التحديات الشائعة الأحداث المكرَّرة التي تُسبب ترحيلاً مزدوجًا | [General Ledger Postings: A Comprehensive Guide — Dualentry](https://www.dualentry.com/blog/general-ledger-postings), accessed 2026-09-10 | G7/G9، §12.12 "idempotency at the boundary" في خطة FIN |

## 10. OPEN ITEMS

لا يوجد — النطاق محدَّد بالكامل من الخطط الثلاث المرفقة وتوجيهات GENERATION-INSTRUCTIONS.md؛
لا نقطة غموض تتطلب حوارًا إضافيًا مع المستخدم في هذه المرحلة.
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: project-registry>>>
# PROJECT REGISTRY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile            : erp
Registry Version   : 1.1.0
Domain Profile     : erp/domain-profile.md v1
Last Updated       : 2026-09-10 by P3.1 (SEC v1 pass-1 completion)
Modules registered : 9   Entity candidates : 13 (SEC)   Open items : 0
══════════════════════════════════════════════════════════════════

## SCHEMA COMPLIANCE MAP
| Section of this registry | Category (shared/REGISTRY-SCHEMA.md) |
|---|---|
| IDENTITY & VERSIONING | CAT-1 identity & conventions |
| CONVENTIONS & STEERING | CAT-1 identity & conventions |
| MODULE / COMPONENT INDEX | CAT-2 module index |
| ENTITY OWNERSHIP | CAT-3 entity ownership |
| SHARED ENTITY DECLARATIONS | CAT-4 shared declarations |
| STRUCTURAL / IMPLEMENTATION REGISTRY | CAT-5 structural registry |
| CROSS-MODULE DEPENDENCY INDEX | CAT-6 dependency indexes |
| DECISION INDEX | CAT-7 decision index |
| PIPELINE / PROGRESS STATUS | CAT-8 pipeline status |
| CHANGE / EVENT HISTORY | CAT-9 event history |
Uncovered: none

## IDENTITY & VERSIONING
| Field | Value |
|---|---|
| Profile | erp — ERP Platform |
| Registry version | 1.0.0 |
| Domain profile source | erp/domain-profile.md v1 |

### Version history
| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-09-10 | Initial bootstrap from erp/domain-profile.md v1 (BOOTSTRAP event, see CHANGE/EVENT HISTORY) |
| 1.1.0 | 2026-09-10 | SEC v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |

## CONVENTIONS & STEERING
(copied verbatim from `erp/domain-profile.md` §7 — the authoritative source; this section
mirrors it for engines that read only the registry)

### Ubiquitous language
See `erp/domain-profile.md` §7.1 for the full bilingual (ar/en) term table — copied verbatim,
not restated here to avoid drift; cite as `[domain-profile §7.1]`.

### Bounded contexts
| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | Foundational capabilities (Tier 0) every other context depends on |
| supply | PRC, INV | Supply and inventory flows (out of scope this batch) |
| finance | FIN | General ledger; consumes organization, produces nothing back to it |
| people | HR | Human resources (out of scope this batch) |
| commercial | SLS, CTR | Sales and contracts (out of scope this batch) |

### Module prefixes
| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE |
| MDL | Master Data Lookup | IN PROFILE |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

### Identifier rules
`{prefix}-{MOD}-{seq}` — seq width 3 (`factory.ids.pattern`, `factory.ids.seq_width`).
Entity kinds: master, transactional, lookup, config, security.

### ENFORCEMENT NOTES
- **E1** Every later artifact uses the terms of `domain-profile.md §7.1` verbatim; a synonym
  listed under "do not say" is a consistency finding at the pass gate (`gov.py analyze`
  checks registry ↔ artifact agreement).
- **E2** IDs follow `{prefix}-{MOD}-{seq}` (seq width 3) with the module codes of this section only.
- **E3** Entities are classified with the kinds: master, transactional, lookup, config, security.
- **E4** Sources to cite when a stage resolves an ambiguity: `profiles/erp/knowledge/erp-domain-standards.md`,
  then `erp/domain-profile.md` itself, then the three module plans (`new project/*-plan-en.md`)
  named in `domain-profile.md §7.5`.
- **E5** Pipeline status (below) is maintained by the orchestrator from commits; seeded here as NOT STARTED.

## MODULE / COMPONENT INDEX
| # | Code | Module | Bounded context | Category | Core/ext | Status | Source |
|---|---|---|---|---|---|---|---|
| 1 | SEC | Security | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 1 |
| 2 | MDL | Master Data Lookup | organization | Foundation | Core | CANDIDATE — this batch, second | domain-profile §4 row 2 |
| 3 | FIN | Finance (General Ledger) | finance | Business — Tier 1 | Core | CANDIDATE — this batch, third | domain-profile §4 row 3 |
| 4 | ORG | Organization | organization | Foundation | — | RESERVED — not this batch | domain-profile §4 row 4; profile |
| 5 | PRC | Procurement | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 5; profile |
| 6 | HR | Human Resources | people | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 6; profile |
| 7 | INV | Inventory | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 7; profile |
| 8 | SLS | Sales | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 8; profile |
| 9 | CTR | Contracts | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 9; profile |

## ENTITY OWNERSHIP
| ENT id | Name | Owner module | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|---|
| ENT-SEC-001 | User | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-002 | Role | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-003 | UserRoleAssignment | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-004 | ModuleRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-005 | ScreenRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-006 | ActionRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-007 | RoleModuleGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-008 | RoleScreenGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-009 | RoleActionGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-010 | ActiveSession | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-011 | AuditLogEntry | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-012 | PasswordResetToken | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-013 | SignupRequest | SEC | security | PRIVATE | REGISTERED |
(Source: erp/modules/SEC/P1/registry-srs-sec.md, P1 completion)

## SHARED ENTITY DECLARATIONS
| Entity | Owner ENT id | Owner module | Consumers so far |
|---|---|---|---|
| User | ENT-SEC-001 | SEC | every future module, for its own audit fields (createdBy/updatedBy reference a SEC principal string, not a physical FK — see SEC db-script §3 AUDIT COLUMNS rule; declared here as the canonical identity source, not as a live FK target) |
| ModuleRegistry | ENT-SEC-004 | SEC | every future module registers one row of itself here (API-SEC-018) |
| ScreenRegistry | ENT-SEC-005 | SEC | every future module registers its screens here (API-SEC-019) |
| ActionRegistry | ENT-SEC-006 | SEC | every future module registers its actions here (API-SEC-020) |

## STRUCTURAL / IMPLEMENTATION REGISTRY
| Module | Version | Tables | DBF range | API range | XM range |
|---|---|---|---|---|---|
| SEC | v1 | 13 (SEC_USER … SEC_SIGNUP_REQUEST) | DBF-SEC-001 … DBF-SEC-104 | API-SEC-001 … API-SEC-027 (QR-SEC-001…038) | none (ROOT) |

## CROSS-MODULE DEPENDENCY INDEX
| Candidate ref | Kind | From module | To module | Consumes | Status | Evidence |
|---|---|---|---|---|---|---|
| XM-CAND-001 | HARD-FK | FIN | SEC | identity + module/screen/action grants + SoD (entry-creator ≠ period-close approver) | CANDIDATE — target SEC v1 now GATED (pass-1 APPROVE); ready for FIN's own P2 to assign the formal XM-FIN-* id | domain-profile §6 row "FIN \| SEC \| HARD-FK" |
| XM-CAND-002 | HARD-FK | FIN | MDL | payment methods, accounting event types, account types, period states, journal types | CANDIDATE | domain-profile §6 row "FIN \| MDL \| HARD-FK" |
| XM-CAND-003 | SOFT/EVENT | FIN | Notifications (NOTIF, out of this batch) | period-close-awaiting notice, statement export — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-004 | SOFT/EVENT | FIN | File Service (FILESVC, out of this batch) | statement/export file — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-005 | SOFT | SEC | Notifications (NOTIF, out of this batch) | password-reset message — optional only | CANDIDATE | domain-profile §6; security-module-plan-en.md §8 |
| XM-CAND-006 | EVENT | host business system (out of scope) | FIN | canonical accounting event only — no direct table read/write either direction | CANDIDATE | domain-profile §6 row "host → FIN (event only)"; general-accounting-system-plan-en.md §3 |
Note: every consumer module (all 9, per SEC/MDL plans §7/§4) will register the same
FIN→SEC / FIN→MDL shape once it exists; only the three modules named in this batch are
pre-registered as candidates above — this is not a closed list.

## DECISION INDEX
| # | Decision | Status | Source |
|---|---|---|---|
| 1 | PostgreSQL is the sole DB build target; Oracle/ADF remains an upstream event source only | ACCEPTED | domain-profile §8 row 1 |
| 2 | Hierarchical 3-level RBAC (Module→Screen→Action) replaces any module-local security | ACCEPTED | domain-profile §8 row 2 |
| 3 | One central Lookup master-detail hub replaces module-local lookup tables | ACCEPTED | domain-profile §8 row 3 |
| 4 | No per-entry approval in GL; the only human control point is period close | ACCEPTED | domain-profile §8 row 4 |
| 5 | This batch's scope and order: SEC, then MDL, then FIN, strictly in that order | ACCEPTED | domain-profile §8 row 5 |
| 6 | Notifications/File Service integration is optional-only, used solely on explicit plan need | ACCEPTED | domain-profile §8 row 6 |
| 7 | ADR-SEC-001 — SEC's owned lookups (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) stay CHECK-constrained in v1, not in a shared MDL lookup table, since SEC precedes MDL in this batch | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-001.md |
| 8 | ADR-SEC-002 — Error-catalog infrastructure rows (not-found, duplicate, invalid-transition, forbidden, invalid-sort, server) are cited as PLATFORM-STD under one umbrella ADR rather than a dedicated SRS RULE each | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-002.md |

## OPEN QUESTION INDEX
none — `domain-profile.md` §10 records no open item.

## PIPELINE / PROGRESS STATUS
| Module | Version | Last committed stage | Last gate verdict | Delivered tracks | Tag |
|---|---|---|---|---|---|
| SEC | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| MDL | v1 | NOT STARTED | — | — | — |
| FIN | v1 | NOT STARTED | — | — | — |
| ORG | — | NOT STARTED | — | — | — |
| PRC | — | NOT STARTED | — | — | — |
| HR | — | NOT STARTED | — | — | — |
| INV | — | NOT STARTED | — | — | — |
| SLS | — | NOT STARTED | — | — | — |
| CTR | — | NOT STARTED | — | — | — |

## CHANGE / EVENT HISTORY
| Date | Stage/tool | Module | Version | Event |
|---|---|---|---|---|
| 2026-09-10 | domain-profile | (platform) | — | domain-profile.md v1 saved and committed (23b3176) |
| 2026-09-10 | P-1 | (platform) | — | BOOTSTRAP — extracted 9 module rows, 0 entity candidates, 6 XM candidates, 6 confirmed decisions, 0 open items from domain-profile.md v1 |
| 2026-09-10 | P0 | SEC | v1 | P0 completed: SEC (platform-summary, module-registry-sec, business-policies-sec — 11 POL) |
| 2026-09-10 | P0.5 | SEC | v1 | P0.5 completed: SEC — 12 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | SEC | v1 | P1 completed: SEC — 13 entities, 33 requirements, 33 AC, 7 rules, 10 screen requirements, 0 ADR |
| 2026-09-10 | P2 | SEC | v1 | P2 completed: SEC — 13 tables, 104 DBF, 0 XM; ADR-SEC-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | SEC | v1 | P3.1 completed: SEC — 27 API, 38 QR, ALIGN PASSED; ADR-SEC-002 (ACCEPTED) |
| 2026-09-10 | gate:pass-1 | SEC | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | SEC | v1 | backend/exec split: 15 files, verify ok (39 checked) |
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


==============================================================================
# BRIEF — stage `P2` (Database) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/MDL/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: DBF, XM — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/MDL/P2/db-script-mdl.md`
- `erp/modules/MDL/P2/registry-db-mdl.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C5** SRS → database: C5.1 exists {'artifact': 'srs'} [CRITICAL]; C5.2 ears {'kind': 'REQ', 'patterns': 'factory.ids.ears.patterns'} [CRITICAL]; C5.3 traces {'from': 'REQ', 'to': ['US'], 'min': 1} [MAJOR]; C5.4 orphans {'kind': 'REQ', 'referenced_by': ['AC'], 'min': 1} [CRITICAL]; C5.5 traces {'from': 'AC', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.6 traces {'from': 'RULE', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.7 ids-owned {'stage': 'P1'} [CRITICAL]; C5.8 registry-agree {'artifact': 'srs', 'registry': 'registry-srs', 'kinds': ['REQ', 'AC', 'ENT', 'RULE']} [MAJOR]; C5.9 no-questions {'stage': 'P1'} [CRITICAL]; C5.10 languages {'stage': 'P1'} [MAJOR]; C5.11 ids-continue {'stage': 'P1'} [CRITICAL]
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]

---
# ENGINE
# Database — ENGINE

```
Engine        : Database
Stage id      : P2
Pass          : 1
Questions     : forbidden — ambiguity is self-resolved (§9)
Lane          : analysis
Inputs        : srs, registry-srs
Produces      : db-script-{mod}.md · registry-db-{mod}.md (registry)
Owns IDs      : DBF, XM   → `{prefix}-{MOD}-{seq}` (seq width 3)
Dialect       : postgresql16   (profile.stack.db.dialects[0]; syntax from profile.stack.db.syntax_map)
Next          : P3.1
Module        : MDL   Version: 1
Profile       : erp — ERP Platform
```

This engine produces the module's **structural truth**: an executable database script
derived from the SRS, a field-level traceability matrix and the cross-module dependency
register. It invents no business logic and never redesigns SRS meaning; when the script
and the SRS disagree, the SRS governs and the script is corrected. It produces no
execution phases and no implementation sequencing.

Completion (write → registry → analyze → commit) is owned by the orchestrator — see
`shared/GOVERNANCE-CORE.md`. In a delta version (version > 1): read
`_state/current-{artifact}` of the previous version for every input and for this
stage's own artifacts, and emit only ADDED / MODIFIED / REMOVED elements plus the
`change-manifest.md` per `shared/VERSIONING.md` — the script of a delta version is a
migration (ALTER / CREATE / DROP for the changed objects only); sequences continue.

### IDs this stage assigns

| Atom | Meaning | Traces to |
|---|---|---|
| `DBF` | db field | REQ, ENT |
| `XM` | cross-module dependency | REQ |

---

## 1 — Inputs and entry check

```
  srs             : ✓ present / ✗ MISSING (pipeline error — not a question)
  registry-srs    : ✓ present / ✗ MISSING (pipeline error — not a question)
  domain-profile STEERING : vocabulary verbatim; identifier rules
  project-registry        : structural registry (names fixed by other modules),
                            dependency index, shared declarations
  Extracted               : [N] entities → [N] tables · [N] intra-module FKs ·
                            [N] XM candidates (SRS A8) · [N] lookups (SRS A6)
```

Reading protocol for the SRS: PART A entirely — A3 (entities, fields, logical types),
A5 (rules → constraints), A6 (lookups → seed data), A7 (status → check constraints),
A8 (consumed entities → XM). PART B is not read for structure (screens are not tables).

---

## 2 — DB field traceability matrix (`DBF`)

The single canonical source of `DBF` → column → type → SRS origin. Downstream
artifacts (the backend plan's alignment manifest) reference columns **by DBF id only**
and never restate column names, types or SRS references.

```
## DB FIELD TRACEABILITY MATRIX — MDL v1
| DBF id            | Table | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
| DBF-MDL-001 | …     | …      | …                    | ENT-MDL-001.[field] | REQ-MDL-… | NOT NULL | — |
Total: [N] DBF ids across [N] tables
```

```
ASSIGNMENT RULES
  - Sequence continuous across the module (not per table); never reused, even for a
    removed column.
  - Per table: PK first, then the entity's own columns in SRS order, then FK columns,
    then standard columns (audit fields last).
  - Every column traces to an ENT.field of the SRS AND to ≥ 1 REQ (via the entity's
    requirements); a standard column traces to the profile default that mandates it
    ("profile: entity_defaults.<kind>") and to the ENT.
  - A column with no SRS origin does not exist (NO-COLUMN-INVENTION, §3).
```

---

## 3 — Naming and column rules

```
IDENTIFIER TRANSFORMATION (stated once in the script header, applied everywhere)
  logical field name (SRS)  →  physical column name: one deterministic transformation
  (case + word separator) declared for postgresql16 — never two spellings of one field.
  Respect the dialect's identifier length limit and reserved words.

TABLE NAMES     : [module code]_[entity abbreviation] — module code from the domain-profile
PRIMARY KEY     : the SRS field named by `{entity}Pk` (profile.stack.db.naming.pk_pattern)
FOREIGN KEYS    : the SRS reference field; constraint FK_[LOCAL]_[REF] (FK_[LOCAL]_[REF]_[n] when several)
AUDIT COLUMNS   : createdBy, createdAt, updatedBy, updatedAt on every table that carries them per its entity kind —
                  filled by the platform, never by a client; user columns hold a
                  principal string, not a numeric FK
FLAG COLUMNS    : end with `Fl`; type BOOLEAN; soft-deactivate flag defaults to active —
                  there is no "deleted" column: deactivation, not deletion
INDEXES         : IDX_[TABLE]_[COLUMN] (composite: IDX_[TABLE]_[ABBR1]_[ABBR2])
CONSTRAINTS     : PK_[TABLE] · UQ_[TABLE]_[COL] · CHK_[TABLE]_[COL]
SEQUENCES       : SEQ_[TABLE] (only when the dialect syntax uses sequences — §4)

NO-COLUMN-INVENTION (CRITICAL)
  Every column is (1) an SRS field, or (2) a profile default for the entity's kind,
  or (3) derived from an FK / XM. Nothing from generic templates or prior examples.
  Modules declared EXCEPTION in the registry keep their real names as-is.
```

---

## 4 — Table definition rules

```
For every ENT in SRS A3 → one table (consumed SHARED entities are NOT re-created).
Each table block, in order:
  CREATE TABLE (all columns, inline NOT NULL, inline CHECK)
  COMMENT ON TABLE + COMMENT ON COLUMN for every column (the comment cites the DBF id)
  PRIMARY KEY · UNIQUE (from RULEs) · CHECK (from RULEs / status values)
  FK constraints — inline only when intra-module; XM FKs per §6/§7
PK GENERATION
  identity syntax for postgresql16: GENERATED ALWAYS AS IDENTITY
  — when the syntax_map declares `identity`, use it; otherwise create SEQ_[TABLE] and
  let the application populate the key. NEVER a trigger for PK population; NEVER a
  default that calls a sequence on the PK column.
```

### 4.1 Datatype governance (profile.stack.db.syntax_map → postgresql16)

| Logical type (SRS) | postgresql16 syntax |
|---|---|
| identity | GENERATED ALWAYS AS IDENTITY |
| string | VARCHAR(n) |
| boolean | BOOLEAN |
| timestamp | TIMESTAMPTZ |
| decimal | NUMERIC(p,s) |
| text | TEXT |

Rules: only the syntaxes above (or one stated once in the script header for a logical
type the map lacks); `n` / `p,s` are filled from the SRS field definition; a
deviation carries a governance note citing the SRS field that requires it; the other
declared dialects (oracle19c) are not emitted — one dialect per script.

### 4.2 Lookup and reference data

Profile rule: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs
```
Lookup-backed field (SRS A6, control = lookup) → the stored value is the lookup CODE
  (never a numeric key); seed rows for every value the SRS lists, each block citing
  the SRS lookup key; the shared lookup tables (if the platform uses them) are created
  once by the first module that needs them and only seeded afterwards — never
  re-created in a later module's script.
Reference entity (SRS decided: its own ENT) → an ordinary table per §4; consumers
  hold an FK.
```

### 4.3 Indexes

```
Mandatory : every FK column; every column used in SRS search / list filters (PART B B2);
            every UNIQUE business key. PK indexes are implicit — never duplicated.
```

---

## 5 — XM register (`XM`) — cross-module dependencies

`XM` is the single identifier for every cross-module dependency in the pipeline
(lifecycle and states: `shared/XM-PROTOCOL.md`). Assigned here; extended by
`P3.1` (status, blocks, workaround, unblock condition) — never re-assigned;
never touched by the frontend stage. The factory's lifecycle ends at DELIVERED; CLOSED
belongs to the consumer repository.

```
## XM REGISTER — MDL v1
| XM id            | Type      | This table | Column / access | Target table | Target module | Traces (REQ) | Status |
| XM-MDL-001 | HARD-FK   | …          | [FK column]     | …            | [code]        | REQ-MDL-… | READY / DEFERRED |
| XM-MDL-002 | SOFT-READ | (application) | [join / read pattern] | … | [code]  | REQ-MDL-… | ACTIVE / CONDITIONAL |

TYPES
  HARD-FK    physical FK constraint; target table must exist → DEFERRED until the
             target module's script is gated
  SOFT-READ  application-level read of another module's table, no FK column; the
             "column / access" cell describes the access pattern
STATUS (at this stage)
  READY        target script gated — constraint applied
  DEFERRED     HARD-FK whose target is not yet gated — column created, constraint in
               the deferred patch block (§6)
  CONDITIONAL  SOFT-READ whose target table exists in no gated script yet
  ACTIVE       SOFT-READ whose target is gated (never "closed")
SOURCES
  SRS A8 consumed entities (HARD-FK / SOFT-READ as classified there); RULEs that
  join by code to another module's table; APIs that read another module's data.
  An XM with no SRS A8 origin is an ORPHAN finding. Audit columns are never XMs.
```

### 5.1 Deferred FK handling

```
For every DEFERRED XM: create the column (with its DBF id); comment it
  '[XM id]: FK to [target] — DEFERRED pending [module] script';
do NOT create the constraint in the main DDL; emit a commented patch block:
  -- DEFERRED FK — XM-MDL-[n]
  -- Target module : [code]   Apply when : target script gated and deployed
  -- Unblock       : [condition, extended by P3.1]
  -- ALTER TABLE [table] ADD CONSTRAINT FK_[local]_[ref] FOREIGN KEY ([col]) REFERENCES [target] ([pk]);
```

### 5.2 SOFT-READ handling

```
For every SOFT-READ XM: register it (Type SOFT-READ); add a commentary block:
  -- XM-MDL-[n] SOFT-READ — this module's [service/query] reads [TARGET].[COLUMN]
  -- from [module] without an FK. Rationale: [from SRS]. Risk: changes to [TARGET]
  -- require impact assessment on [affected requirements].
```

---

## 6 — FK classification (every FK is exactly one of these)

```
INTRA-MODULE FK    both tables in this script → constraint in main DDL; DBF on the column; no XM
READY HARD-FK      target in another module's gated script → constraint applied; XM READY
DEFERRED HARD-FK   target not yet gated → no constraint; XM DEFERRED; patch block §5.1
SOFT-READ          application read → no constraint by design; XM SOFT-READ (§5.2)
```

---

## 7 — `db-script-{mod}.md` — output structure

```
1. HEADER          module · version · dialect postgresql16 · schema prefix (or "none") ·
                   identifier transformation (§3) · date · counts
2. DB FIELD TRACEABILITY MATRIX   (§2 — governance documentation, not SQL)
3. XM REGISTER                    (§5)
4. FULL_DATABASE_SCRIPT           (§7.1 — the ONLY place SQL appears)
5. DECISIONS APPLIED              (DEFAULTs + ADR ids, §9)
6. REGISTRY CONTENT               (§10)
```

### 7.1 FULL_DATABASE_SCRIPT — one consolidated executable

Copy-and-run against a clean schema of postgresql16 without editing. Not documentation:
a deployable. Mandatory block order (guarantees zero dependency errors):

```
BLOCK 1   SEQUENCES (only if §4 PK generation or the SRS needs them)
BLOCK 2   PARENT TABLES (no FK dependencies; lookup/reference tables DDL only)
BLOCK 3   CHILD TABLES (intra-module FK targets already created; chain A → B → C)
BLOCK 4   COMMENTS (table + every column; each column comment cites its DBF id)
BLOCK 5   CONSTRAINTS  5a PK · 5b UNIQUE · 5c CHECK · 5d intra-module FK (parent PK first)
BLOCK 6   TRIGGERS — audit triggers only when an SRS RULE requires them; NEVER PK triggers
BLOCK 7   INDEXES (non-PK)
BLOCK 8   LOOKUP SEED DATA (INSERT with column lists; COMMIT at the end of the block)
BLOCK 9   VIEWS (CREATE OR REPLACE)
BLOCK 10  FUNCTIONS / PROCEDURES (dialect terminator syntax; only if the SRS needs them)
BLOCK 11  DEFERRED FK PATCH BLOCKS — commented out, one per DEFERRED XM, labelled
```

```
SYNTAX RULES (dialect-conditional — the dialect's own syntax comes from
profile.stack.db.syntax_map; the rules below hold for any dialect)
  S-1  Every statement ends with the dialect's terminator; no trailing comma before a
       closing parenthesis; every referenced object has its CREATE in this script.
  S-2  Types: only §4.1 syntaxes; never a type from another dialect.
  S-3  Constraints in ALTER TABLE form (PK / FK / UQ / CHK names per §3); FK declared
       after the parent PK exists.
  S-4  No PK-population trigger; no sequence default on a PK column.
  S-5  Seed INSERTs carry a column list; NULL is never the string 'NULL'; COMMIT after DML.
  S-6  Deferred FK blocks are fully commented and carry their XM id and target module.
  S-7  Schema prefix: all objects qualified, or none — never mixed.
  S-8  No placeholders: no "...", no "[...]" inside SQL — real names and values only.
```

### 7.2 Self-verification before emitting the script

```
SYNTAX        □ S-1 … S-8 hold for every statement
              □ every type appears in §4.1 (or is declared once in the header)
ORDER         □ sequences first (if any) · parents before children · PK before FK ·
                lookup DDL before lookup INSERTs · COMMIT after the last INSERT of a block
COMPLETENESS  □ every SRS entity has a table · every SRS field a column (DBF) ·
                every lookup its seed rows · every index present ·
                every DEFERRED XM a commented block in BLOCK 11 ·
                shared lookup tables not re-created
DEFERRED FK   □ every deferred block commented · labelled with its XM id ·
                no live FK references another module's table
TRACE         □ every DBF traces to ENT.field + REQ · every XM traces to REQ and to an
                SRS A8 row · no orphan, no dangling id
```

---

## 8 — Governance recovery

```
A module whose script was produced from an incomplete or corrected SRS, or whose
script arrives after downstream artifacts exist:
  1. Re-run this stage on the current SRS (_state/ current state).
  2. Re-run `gov.py analyze` on the affected artifacts (this script, the backend plan's
     alignment manifest, the registries). Findings are resolved at the next pass gate.
  3. XM rows whose target became gated → status update per shared/XM-PROTOCOL.md.
No separate audit stage exists; `analyze` is the recovery check.
```

---

## 9 — Ambiguity rule (questions are forbidden here)

```
Structural choices the SRS does not settle (normalisation of a repeating group, a
composite vs surrogate key, an index strategy, a precision):
  NON-BREAKING → ADR, CONTINUE with the chosen best practice
      action: adr · then: continue
  BREAKING (contradicts an SRS REQ / ENT, a registered name, or a gated module's
  structure) → ADR status BLOCKED, STOP the pass
      action: adr · status: BLOCKED · then: stop
ADR file : erp/decisions/MDL/ADR-{MOD}-{seq:03d}.md  (Context · Decision · Consequences · traces · status)
Details  : shared/GOVERNANCE-CORE.md
```

---

## 10 — `registry-db-{mod}.md` — registry content

```
## REGISTRY — P2 — MDL v1
Tables        : table · ENT id · kind · DBF range              (→ structural registry)
XM index      : XM id · type · from MDL · to [code] · status    (→ dependency index)
Lookups       : key · seeded values count · owner
Sequences     : last DBF · last XM
Decisions     : ADR ids (+ BLOCKED, if any)
Event         : "P2 completed: MDL v1 — [N] tables, [N] DBF, [N] XM"
Cascade       : for every registry XM row targeting MDL with status DEFERRED, note
                that this script now exists → resolution per shared/XM-PROTOCOL.md
```

---

## 11 — Boundaries

```
OWNS      : DBF, XM · the traceability matrix · the XM register · DDL structure,
            naming and datatype governance for this module
DOES NOT  : POL (P0) · US (P0.5) · REQ (P1) · AC (P1) · ENT (P1) · RULE (P1) · API (P3.1) · QR (P3.1) · UXD (P3.2) · SCR (P3.2) · SCR-REQ (P1) · TC (test-gen)
            · business logic · execution phases · frontend structure (the frontend stage
            never reads this script; it consumes API docs)
```

---

## 12 — Self-check before emitting

- [ ] Every SRS entity → one table; every consumed SHARED entity → FK / XM, not a table.
- [ ] Every column has a DBF id, a type from §4.1, a comment, and traces (ENT.field + REQ).
- [ ] Every cross-module reference is exactly one FK class (§6) and, unless intra-module, an XM row traced to REQ + SRS A8.
- [ ] Every DEFERRED XM has its column, its comment and its commented patch block; no live cross-module FK.
- [ ] Script block order 1–11 respected; §7.2 checklist passed; script is copy-and-run for postgresql16.
- [ ] Every RULE that maps to a constraint is present (UNIQUE / CHECK) and named per §3.
- [ ] Every DEFAULT / ADR listed under Decisions applied; no BLOCKED ADR unless the pass stopped.
- [ ] No question raised; sequences continuous.
- [ ] Profile check `ERP-3` (MINOR): every flag column ends with the flag_suffix.


---
# INPUTS (generated current state)

<<<INPUT: srs>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: registry-srs>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
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


==============================================================================
# BRIEF — stage `P3.1` (Backend Execution Plan) · module MDL · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/MDL/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: API, QR — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/MDL/P3_1/backend-execution-plan-mdl.md`
- `erp/modules/MDL/P3_1/registry-exec-be-mdl.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]
- **C7** backend execution plan → split / deliver: C7.1 markers {'artifact': 'backend-execution-plan', 'track': 'backend', 'plan': 'exec'} [CRITICAL]; C7.2 traces {'from': 'backend-execution-plan', 'blocks': ['PHASE', 'SUB', 'API', 'XM'], 'min': 1} [MAJOR]; C7.3 traces {'from': 'API', 'to': ['REQ', 'DBF'], 'min': 1} [MAJOR]; C7.4 registry-agree {'artifact': 'backend-execution-plan', 'registry': 'registry-exec-be', 'kinds': ['API', 'QR']} [MAJOR]; C7.5 registry-agree {'artifact': 'backend-execution-plan', 'registry': 'registry-db', 'kinds': ['XM']} [MAJOR]; C7.6 orphans {'kind': 'REQ', 'referenced_by': ['API', 'DBF'], 'min': 1} [MAJOR]; C7.7 ids-owned {'stage': 'P3.1'} [CRITICAL]; C7.8 no-questions {'stage': 'P3.1'} [CRITICAL]; C7.9 ids-continue {'stage': 'P3.1'} [CRITICAL]

---
# ENGINE
```
ENGINE        : P3.1 — Backend Execution Plan
PASS / TRACK  : pass 1 · track backend · lane analysis · questions forbidden
MODULE        : MDL · v1 · profile erp (ERP Platform)
READS         : srs · db-script · registry-srs · registry-db   (all from _state/ — generated current state)
PRODUCES      : backend-execution-plan-mdl.md · registry-exec-be-mdl.md
OWNS IDS      : API, QR
NEXT          : gate:pass-1   (the orchestrator owns the completion protocol — shared/GOVERNANCE-CORE.md)
BOUNDARY      : analysis-only — this engine writes specifications, never code
```

# Backend Execution Plan — engine reference

## 0. Position and authority

This engine turns the module's **functional truth** (the SRS) and **structural truth** (the
db-script) into one agent-ready backend execution plan. It reads its inputs from
`_state/` only (the generated current state — never a raw `v{N}/` folder), and it
never invents business meaning, tables, columns, rules or IDs.

- Upstream artifacts govern. A conflict between this plan and the SRS or db-script is a
  **finding**, never a silent resolution.
- Questions are `forbidden` at this stage. Ambiguity is resolved by the rule in
  `factory.yaml → ambiguity` (see §12): non-breaking → ADR (`ADR`) and
  `continue`; breaking → ADR with status
  `BLOCKED` and `stop`.
- Every block in the plan carries `traces=` to the upstream IDs it implements (§6.0). The
  traceability matrix built by `gov.py analyze` must be CLEAN before the pass gate opens.
- The plan is the **sole backend input** of the implementation agent. After implementation
  the consumer repo publishes `governance/api-docs/api-docs-{mod}.md`; the
  frontend stage reads that file — never this plan's contract draft.

**Delta versions** (v2+) emit only ADDED / MODIFIED / REMOVED blocks plus
`change-manifest.md` against the baseline in `_state/`; IDs
continue their sequence and are never renumbered. Rules: shared/VERSIONING.md.

## 1. Inputs

| Input | Read from | Use |
|---|---|---|
| `srs` | `_state/current-srs.md` | authoritative functional truth — REQ/AC/ENT/RULE, screens, permissions, lookup keys |
| `db-script` | `_state/current-db-script.md` | authoritative structural truth — tables, columns (DBF), constraints, XM register |
| `registry-srs` | `_state/current-registry-srs.md` | ID ranges already assigned, shared entities, existing lookups, module prefix |
| `registry-db` | `_state/current-registry-db.md` | ID ranges already assigned, shared entities, existing lookups, module prefix |
| `erp/` steering + `profile.knowledge.files` | `profiles/erp/knowledge/erp-domain-standards.md` | primary sources cited when a best-practice choice must be made (§12) |

Business policies are not read directly: client policies are embedded in `RULE-*` inside
the SRS. A RULE sourced from a client policy is never resolved unilaterally — a conflict is a
breaking ambiguity (§12).

If the db-script is absent the run is **GOVERNANCE REDUCED**: declare it in the plan header,
produce a functional-only plan, mark every DB binding `PENDING`, and record an ADR. Never
downgrade silently.

## 2. Mandatory extraction and binding (§2A)

### 2A.0 The fundamental rule

Before writing any phase content, extract and **bind** every concrete value from the inputs.
A plan containing a placeholder (`[TABLE_NAME]`, `[LOOKUP_KEY]`, "uses a sequence",
"see SRS") is incomplete and fails the alignment self-check (§9).

```
NO-INVENTION RULE
  Every table, column, constraint, index and PK-generation object used anywhere in the plan
  MUST exist in the db-script and be cited by its DBF-* (or the exact object name the
  db-script declares). Base fields (audit, flag, PK) come from the db-script — not from
  memory, not from templates. Naming conventions are read from the profile:
    flag suffix   : Fl
    audit fields  : createdBy, createdAt, updatedBy, updatedAt
    PK pattern    : {entity}Pk
    target dialect: postgresql16 (also kept: oracle19c)
```

### 2A.1 Pre-generation extraction table

Emit this table first in the run (it is not part of the plan file; it is the working set
every phase binds from):

```
PRE-GENERATION EXTRACTION — MDL v1
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      ENT-MDL-<seq> │ exact name │ kind ∈ master | transactional | lookup | config | security
REQUIREMENTS  REQ-MDL-<seq> │ EARS text  │ its AC-MDL-<seq> list (Given/When/Then)
RULES         RULE-MDL-<seq> │ scope ENT │ trigger │ statement │ message per language (ar, en) │ source
SCREENS       every screen entry the SRS declares │ type │ owning ENT │ composite (Search + Entry = ONE screen)
PERMISSIONS   the SRS permission matrix (roles × screens × actions) — actions VIEW/CREATE/UPDATE/DELETE, gateway VIEW
LOOKUPS       every lookup key the SRS names, exactly as written — rule: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs
BUSINESS CODE format per master entity — rule: document numbers come from the platform numbering engine; never generated in a module
── FROM db-script ────────────────────────────────────────────────────────
TABLES        ENT → exact table name
PK GENERATION exact object the db-script declares per table (identity clause / sequence / trigger — as written for postgresql16)
COLUMNS       exact column name │ DBF-MDL-<seq> │ declared type │ null │ default
CONSTRAINTS   exact FK / UNIQUE / CHECK constraint names ; INDEXES exact names
XM            XM-MDL-<seq> │ kind (HARD-FK | SOFT-READ …) │ local column │ target module.table │ status
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES consumed (owner module, reached via which XM) — never redeclared
EXISTING LOOKUP KEYS (reuse — never create a duplicate)
ID RANGES already used for API, QR (continue the sequence)
──────────────────────────────────────────────────────────────────────────
Any row that cannot be filled → §2A.3.
```

### 2A.2 Binding rules

| Binding | Rule | Forbidden → Required |
|---|---|---|
| PK generation | every PK reference names the exact object from the db-script | "auto-generated" → the exact identity/sequence clause as declared |
| Column names | every field reference cites the exact column + `DBF-*`; the implementer maps property → column through the DB Alignment Manifest (§4) | a camelCase invention → `DBF-*` lookup |
| Rule text | every RULE cited in a phase carries its full statement, trigger and message in every language (ar, en) — the plan is self-contained | "applies RULE-… see SRS" → full text inline |
| Lookup keys | the exact key string from the SRS, confirmed against the db-script column that stores it; endpoint per the base path `/api/v1/{module}/{resource}` | a parameter placeholder → the literal key |
| Business code | format stated explicitly (exact pattern from the SRS, column, uniqueness constraint name) | "auto-generated, read-only" → format + column + constraint + generation source |
| Endpoints | every path is an instance of `/api/v1/{module}/{resource}`; verbs mean `POST`=create, `GET`=read, `PUT`=update, `DELETE`=deactivate (soft), `PATCH`=partial | an ad-hoc path → the base-path pattern |

### 2A.3 Extraction failure

A value that cannot be confirmed from the inputs is never invented:

| Case | Action |
|---|---|
| SRS entity has no table in the db-script | mark the entity `PENDING DB` in the plan (GOVERNANCE REDUCED for that entity) + ADR |
| Lookup key / message text / business-code format missing | mark the field `PENDING` with the ADR id; the Error Catalog row carries the ADR id instead of text |
| PK generation object missing for a table | flag in the data phase; QR entry notes `generation: not confirmed`; ADR |
| Two upstream sources contradict | breaking ambiguity → ADR `BLOCKED`, run stops (§12) |

## 3. Plan Index

The plan opens with an index — one table per element family, every row bound from §2A.1:

```
EXECUTION PLAN INDEX — MDL v1 — backend-execution-plan-mdl.md
Profile: erp · dialect: postgresql16 · framework: profile.stack.backend.framework
Open ADRs: <n> — decisions/MDL/

ENTITY REGISTRY   ENT-*  │ name │ table │ business code (if any) │ operations
FIELD REGISTRY    DBF-*  │ property │ read-only? │ ENT-*
API REGISTRY      API-*  │ operation │ verb │ path │ traces (REQ-*, DBF-*)
RULE REGISTRY     RULE-* │ name │ scope │ ENT-* │ message in every language ✓/✗
SCREEN REGISTRY   screen │ type │ ENT-* │ permission names
LOOKUP REGISTRY   key    │ used in field │ ENT-*
QRC SUMMARY       QR-*   │ operation │ phase │ ENT-*         (agent reference only — §5)
DB ALIGNMENT      see manifest (§4) — ALIGNED ✓ / issues: <n>
XM STATUS         <n> deferred — see the cross-module phases
SECURITY          <n> screens × <n> roles
```

## 4. DB Alignment Manifest

The manifest is the canonical binding between plan fields and db-script fields. It contains
**only** these columns — column names, DB types and SRS references are *sourced by lookup*
from the db-script, never reproduced here (duplicating them is a contract violation —
shared/ARTIFACT-CONTRACTS.md):

```
DB ALIGNMENT MANIFEST — MDL v1
DBF-*            │ ENT-*          │ plan property │ plan type │ XM-* (if FK crosses modules) │ status
DBF-MDL-001 │ ENT-MDL-001 │ <property>    │ <type>    │ —                            │ ✓
DBF-MDL-007 │ ENT-MDL-001 │ <property>    │ <type>    │ XM-MDL-001 ⏸           │ ⏸
Legend  ✓ aligned · ✗ type mismatch (finding) · ⏸ deferred XM
Derived / computed properties (no DBF) are listed with DBF = "— (derived)" and an ADR id.
```

## 5. Query Reference Catalog (QR-*)

The QRC expresses the **retrieval and persistence intent** of every repository operation as
pseudo-SQL. It is a logical specification, never executable code: the implementer rewrites
every entry with the real entity classes, mapped property names and the project's query
strategy. Copy-pasting a QR entry into production code is a violation.

- Format: `QR-MDL-{seq}` (3-digit sequence, continuous across the module).
- Assigned while writing the data and service phases; every API with a DB operation cites its QR.
- Ordering / paging use the profile's envelope: `Page<T>`; responses are wrapped in `ApiResponse<T>`.

```
QR-MDL-<seq> — <operation name>
Phase        : <p.key of the phase that defines it>
API          : API-MDL-<seq> | repository-only
Entity       : ENT-MDL-<seq>
Operation    : FIND_ONE | FIND_ALL | FIND_BY_CRITERIA | SAVE | UPDATE | DELETE | COUNT | EXISTS | NATIVE | AGGREGATE
Intent       : <what business question this answers / what it must return or change>
Logical spec : SELECT … FROM <exact table> [JOIN <table> ON …] WHERE <conditions from RULE-*> [ORDER BY …] [page/size]
Join         : NONE | required — ADR-<id> (why)
Transaction  : READ_ONLY (reads) | READ_WRITE (writes) | REQUIRES_NEW — ADR-<id> if non-default
Pagination   : YES (Page<T>) | NO
Filters      : <field: EXACT | LIKE | DATE_RANGE | SET>
Result shape : full entity | projection <fields> | count
Null handling: <per optional field>
```

Standard operation defaults (apply unless a QR entry overrides them):

| Operation | Default |
|---|---|
| FIND_ONE by PK | read-only; not found → error per `LocalizedException → {code, messageAr, messageEn}` with the catalog row for "not found" |
| FIND_BY_CRITERIA | read-only; filters + allowed sort fields declared per search; empty result → success with empty content, **never** "not found" |
| SAVE | read-write; PK and audit fields system-set; business code from the numbering rule (document numbers come from the platform numbering engine; never generated in a module) |
| UPDATE | read-write; immutable fields (PK, business code, audit) excluded from the request |
| `deactivate (soft)` | usage check first (can-delete / can-deactivate); blocked → catalog error; allowed → flip the active flag (suffix `Fl`); hard delete only where the SRS mandates it |
| EXISTS | read-only uniqueness check; excludes the current PK on update |

Join governance: single-table responses never join; display names of lookup values are **never** joined — the backend returns the stored code and the frontend resolves the label (all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs); parent data or cross-entity filters require a join **and** an ADR; cross-entity aggregation may need a native query — say why.

## 6. Phase content

### 6.0 Markers, thresholds, traces — read before writing any phase

Marker grammar (`factory.markers`, schema v2, syntax `html-comment`):
`<!-- KIND:ID:START [traces=…] -->` … `<!-- KIND:ID:END -->`. Kinds that may appear in a
`backend` execution plan:

| Kind | Level | Allowed parents | Notes |
|---|---|---|---|
| `PHASE` | 1 | — (top level) | keys from `profile.tracks.<track>.plans.<plan>.phases` |
| `SUB` | 2 | PHASE | id = `{PHASE-KEY}-{LABEL}` — always phase-qualified |
| `API` | 3 | PHASE, SUB | one atom `API-*` = one dedicated block |
| `XM` | 3 | PHASE, SUB | one atom `XM-*` = one dedicated block |

Rules:
- The first line you write for a phase **is** its `PHASE` START marker; the last line is its
  END marker. Content and markers are one action — never "write, then wrap".
- `traces=`: **every** PHASE, SUB and atom block carries
  `traces=` listing the upstream IDs it implements (comma-separated, grammar
  `{prefix}-{MOD}-{seq}`, 3-digit seq). Obligations from `factory.ids`:
  `API` → REQ + DBF; `XM` → REQ (XM is minted upstream and only placed here). A PHASE block traces to the union of its children.
- Split unit is `SUB or PHASE` — never an atom. Check the
  threshold **while** writing: if the count is already at threshold from §2A.1, open the first
  SUB before its first block. Never write flat and split later.
- Unknown phase key → the toolkit **refuses** (`refuse`). The key
  is `p.key`, never the display name (the
  autofix normalises `+ _ space --` to `-` only when unambiguous — do not rely on it).
- Any heading containing the word PHASE uses exactly one profile key. Index, manifest, catalog
  and self-check sections are not phases: distinct headings, no marker, placed before the
  first PHASE or after the last END.
- Full protocol: shared/MARKER-PROTOCOL.md.

Phase table for `profile.tracks.backend.plans.exec` (the plan is organised in exactly this order):

| # | Key | Display | Split rule | Atoms carried |
|---|---|---|---|---|
| 1 | `CORE` | CORE | never split | none |
| 2 | `DATA-DOM` | DATA+DOM | SUB by engine self-check; labels `DATA-DOM-MASTER`, `DATA-DOM-TRANSACTIONAL`, `DATA-DOM-LOOKUP` | none |
| 3 | `SVC-API` | SVC+API | SUB when API count >= 8 — grouped CRUD / SEARCH / INT; labels `SVC-API-CRUD`, `SVC-API-SEARCH`, `SVC-API-INT` | `API-*` blocks |
| 4 | `DOC` | DOC | never split | none |
| 5 | `INT-C` | INT-C | SUB when XM count >= 5 — grouped per target module | `XM-*` blocks |
| 6 | `INT-R` | INT-R | SUB when XM count >= 5 — grouped per target module | `XM-*` blocks |
| 7 | `SEC-BE` | SEC-BE | never split | none |
| 8 | `ALIGN-BE` | ALIGN-BE | never split | none |


### 6.1 Content roles

The profile names the phases; this engine supplies the content **by role**. Match each
phase to the roles its display name declares (a display such as "SVC+API" declares the
service and API roles; "INT-C" declares cross-module consume). A phase whose display matches
no role is filled as the profile describes it. Atom placement is data-driven: `API-*` blocks
go in the phase whose `split_threshold.kind` is `API`, `XM-*` blocks in the phases whose
kind is `XM`.

**R1 — Core / configuration (architecture policies).** Declared once, applies to the module:
- Layers and responsibilities: controller → service → mapper → domain → repository — each layer's "does / never does" stated; boundary violations are review findings.
- Domain-behaviour placement (in entity methods | separate domain classes) — one choice.
- Error signalling: `LocalizedException → {code, messageAr, messageEn}`; every catalog row is registered in every place the framework needs (declare the list once here).
- Transaction scope defaults; search contract (request shape, allowed sort fields, paging `Page<T>`).
- Audit fields (`createdBy`, `createdAt`, `updatedBy`, `updatedAt`) are framework-filled — never in create/update requests, never set by mappers or services.
- Type mapping postgresql16 → language types, stated once as a table (from `profile.stack.db.syntax_map` rows) — a deviation needs an ADR.
- Lookup values: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs.
- Numbering: document numbers come from the platform numbering engine; never generated in a module.
- Workflow engine: **forbidden**.
- Languages: every named entity carries a name per language (ar, en); a single-language artifact is incomplete.
- Cross-module contract placement: inversion-of-control interfaces consumed by other modules live in the service layer; a domain class may depend on another module's service interface (module boundary, not a layer violation).
If nothing module-specific applies, write "Standard configuration — no module-specific abstractions".

**R2 — Data + domain.** One entity block per `ENT-*`, every value bound (§2A):
```
### ENT-MDL-<seq> — <exact name>      kind: <master|transactional|lookup|config|security>
BINDINGS   table <exact> · PK <column, DBF> · PK generation <exact object> · db-script version
BUSINESS CODE property · column (DBF) · format <exact> · uniqueness constraint <exact name> · generation source
DEFAULT FIELDS per kind (profile.conventions.entity_defaults): master → nameAr, nameEn, code, isActiveFl, createdBy, createdAt, updatedBy, updatedAt; transactional → docNo, docDate, statusCode, fiscalYearId, periodId, createdBy, createdAt, updatedBy, updatedAt; lookup → code, nameAr, nameEn, sortOrder, isActiveFl; config → key, valueAr, valueEn, isActiveFl
FIELDS     DBF-* │ property │ column (exact) │ type (postgresql16) │ null │ read-only │ constraint │ label per language (ar/en)
DTO MEMBERSHIP  create-request excludes / update-request excludes / response includes (PK, business code, audit, flag stated explicitly)
LOOKUP FIELDS  property │ column (DBF) │ exact lookup key │ endpoint (base path /api/v1/{module}/{resource}) — stores the code, never a numeric FK
DOMAIN RULES   RULE-* full text: trigger · statement · message per language · scope (CREATE|UPDATE|DELETE|ALL) · DB enforcement (constraint name | app-level) · owner layer
STATE MACHINE  (if status-bearing) status column (DBF) · values · initial · transitions (trigger, actor) · terminal · invalid-transition RULE
CROSS-MODULE   XM-* rows touching this entity (kind, local column, target, status)
REPOSITORY OPS → QR-* list (FIND_ONE, FIND_BY_CRITERIA, SAVE, UPDATE, EXISTS, …)
```
Grouping for `DATA-DOM`: when the entity count justifies a split (engine self-check — not
marker-countable), group under `SUB:DATA-DOM-MASTER / SUB:DATA-DOM-TRANSACTIONAL / SUB:DATA-DOM-LOOKUP`.

**R3 — Service + API.** One `API-*` block per endpoint, each its own atom marker:
```
<!-- API:API-MDL-<seq>:START traces=REQ-MDL-<seq>,DBF-MDL-<seq> -->
### API-MDL-<seq> — <operation>
Endpoint     : <instance of /api/v1/{module}/{resource}>   verb: <POST|GET|PUT|DELETE|PATCH>
Layers       : <entry layer → method> ; <service layer → method>        (names per R1)
Request      : path params · query params (filter names = properties from R2) · body DTO fields (type, required, constraint) · excluded system fields
Response     : status · DTO fields · paginated? (Page<T>) · envelope ApiResponse<T>
Validations  : RULE-* full text (statement, trigger, message per language) — every RULE listed here has a catalog row (§7)
Errors       : catalog rows this endpoint can raise (code, HTTP, RULE-*)
Orchestration: load → validate (RULE-*) → integrate (XM-*) → persist (QR-*, table, generation object)   — WHAT in sequence, layer placement per R1
Repository   : QR-* · operation · join (NONE | ADR) · transaction
Security     : screen · permission name (`PERM_<PAGE_CODE>_<ACTION>`) — enforced before processing
Localization : every message in ar + en; every name field per language
<!-- API:API-MDL-<seq>:END -->
```
Completeness rules: every RULE in Validations ↔ a catalog row (RULE-ERR-CARRY); infrastructure
errors (not found, forbidden, server) are catalog rows with RULE = `PLATFORM-STD` and an ADR;
repository deviations (eager fetch, compound update, native query) need an ADR. Business code
(if any) is excluded from create/update bodies and always present in responses. No hard-coded
role checks in services — permission names only.

**R4 — Contract documentation (internal).** API contract summary (API │ path │ verb │ request
DTO │ response DTO │ stability), DTO typing constraints (lookup fields are
strings holding the code, never enums; business code never in create/update), and the
pagination + filter standard (request shape, sort validation, empty result = success). This
section is a **backend self-check only** — the frontend stage binds to the real
`api-docs-mdl.md` published after implementation, never to this summary.

**R5 — Cross-module consume (contracts).** The plan never mints `XM-*`; it places every XM
from the db-script register:
```
<!-- XM:XM-MDL-<seq>:START traces=REQ-MDL-<seq> -->
### XM-MDL-<seq> — <dependency>
Target        : module · entity (ENT of the owner) · classification (HARD-FK | SOFT-READ | EVENT | READ-ONLY)
Interface     : DB foreign key | REST call (<instance of /api/v1/{module}/{resource} on the target>) | message
Contract      : data required · fallback if absent · retry / timeout / idempotency
Blocks        : DBF-* / API-* blocked while DEFERRED · unblock condition · deferred strategy
<!-- XM:XM-MDL-<seq>:END -->
```
Summary table first (XM │ classification │ target │ interface │ status). Inbound
dependencies from future consumers use `XM-INBOUND-STUB-<n>` notation (consumer, entity
exposed, "assigned by the consumer"), never `TODO`. Lifecycle and RXE handling:
shared/XM-PROTOCOL.md — the factory ends at DELIVERED; CLOSED belongs to the consumer repo.

**R6 — Cross-module resolve (runtime activation).** One status row per XM (READY │ DEFERRED
│ MOCKED │ SIMULATED │ BLOCKED │ EXTERNAL_WAIT) with the workaround / mock strategy for every
non-READY row; consumes R5 contracts, never redefines them. Same XM atom-marker form when the
phase carries XM atoms.

**R7 — Security (backend half).** Enforced by the profile's security model:
- one block per screen the SRS declares: every API serving it verifies its permission before processing;
- seed data: one row per composite screen in `SEC_PAGES` (page code, name, parent) and one permission row per action `VIEW/CREATE/UPDATE/DELETE` following `PERM_<PAGE_CODE>_<ACTION>`, `VIEW` being the gateway (without it no other permission applies); column names come from the db-script, not from here;
- forbidden responses map through `LocalizedException → {code, messageAr, messageEn}` with a catalog row.
The frontend stage references these permission names — it never redeclares them.

**R8 — Alignment (self-check).** The ALIGN table of §9, written as the phase content of the
alignment-role phase (never split). If the profile declares no alignment-role phase, the
table is trailing content after the last PHASE END.

### 6.2 Phase-by-phase instructions

#### PHASE 1 — `CORE` (CORE)
- Open with `<!-- PHASE:CORE:START traces=… -->`, close with `<!-- PHASE:CORE:END -->`.
- Content: the roles in §6.1 whose words appear in "CORE"; otherwise as the profile describes this phase.
- Split: never — level-1 only, no SUB.
- Atoms: none — entity/rule blocks carry no marker of their own.

#### PHASE 2 — `DATA-DOM` (DATA+DOM)
- Open with `<!-- PHASE:DATA-DOM:START traces=… -->`, close with `<!-- PHASE:DATA-DOM:END -->`.
- Content: the roles in §6.1 whose words appear in "DATA+DOM"; otherwise as the profile describes this phase.
- Split: by engine self-check, labels `DATA-DOM-MASTER`, `DATA-DOM-TRANSACTIONAL`, `DATA-DOM-LOOKUP`.
- Atoms: none — entity/rule blocks carry no marker of their own.

#### PHASE 3 — `SVC-API` (SVC+API)
- Open with `<!-- PHASE:SVC-API:START traces=… -->`, close with `<!-- PHASE:SVC-API:END -->`.
- Content: the roles in §6.1 whose words appear in "SVC+API"; otherwise as the profile describes this phase.
- Split: open `<!-- SUB:SVC-API-<LABEL>:START traces=… -->` groups when the `API` count is >= 8, grouped CRUD / SEARCH / INT; labels `SVC-API-CRUD`, `SVC-API-SEARCH`, `SVC-API-INT`. Every atom then sits inside a SUB — no orphan atoms beside SUBs.
- Atoms: one `API-*` marker pair per atom, `traces=` on each.

#### PHASE 4 — `DOC` (DOC)
- Open with `<!-- PHASE:DOC:START traces=… -->`, close with `<!-- PHASE:DOC:END -->`.
- Content: the roles in §6.1 whose words appear in "DOC"; otherwise as the profile describes this phase.
- Split: never — level-1 only, no SUB.
- Atoms: none — entity/rule blocks carry no marker of their own.

#### PHASE 5 — `INT-C` (INT-C)
- Open with `<!-- PHASE:INT-C:START traces=… -->`, close with `<!-- PHASE:INT-C:END -->`.
- Content: the roles in §6.1 whose words appear in "INT-C"; otherwise as the profile describes this phase.
- Split: open `<!-- SUB:INT-C-<LABEL>:START traces=… -->` groups when the `XM` count is >= 5, grouped per target module. Every atom then sits inside a SUB — no orphan atoms beside SUBs.
- Atoms: one `XM-*` marker pair per atom, `traces=` on each.

#### PHASE 6 — `INT-R` (INT-R)
- Open with `<!-- PHASE:INT-R:START traces=… -->`, close with `<!-- PHASE:INT-R:END -->`.
- Content: the roles in §6.1 whose words appear in "INT-R"; otherwise as the profile describes this phase.
- Split: open `<!-- SUB:INT-R-<LABEL>:START traces=… -->` groups when the `XM` count is >= 5, grouped per target module. Every atom then sits inside a SUB — no orphan atoms beside SUBs.
- Atoms: one `XM-*` marker pair per atom, `traces=` on each.

#### PHASE 7 — `SEC-BE` (SEC-BE)
- Open with `<!-- PHASE:SEC-BE:START traces=… -->`, close with `<!-- PHASE:SEC-BE:END -->`.
- Content: the roles in §6.1 whose words appear in "SEC-BE"; otherwise as the profile describes this phase.
- Split: never — level-1 only, no SUB.
- Atoms: none — entity/rule blocks carry no marker of their own.

#### PHASE 8 — `ALIGN-BE` (ALIGN-BE)
- Open with `<!-- PHASE:ALIGN-BE:START traces=… -->`, close with `<!-- PHASE:ALIGN-BE:END -->`.
- Content: the roles in §6.1 whose words appear in "ALIGN-BE"; otherwise as the profile describes this phase.
- Split: never — level-1 only, no SUB.
- Atoms: none — entity/rule blocks carry no marker of their own.

## 7. Error Catalog

Canonical, produced with the service/API role, kept in **one** location (a pointer elsewhere
is fine; a second table is a duplicate). Envelope: `LocalizedException → {code, messageAr, messageEn}`.

```
ERROR CATALOG — MDL v1
code (runtime value per envelope) │ RULE-* (or PLATFORM-STD + ADR) │ API-* │ HTTP │ trigger │ message-AR │ message-EN
```
- Every RULE that produces a user-facing message has a row; message text is copied
  character-perfect from the SRS in every language (ar, en); a missing
  language → `PENDING ADR-<id>`, never invented.
- Downstream consumers (frontend plan, test-gen, api-verify) cite the **code**; they never
  reproduce message text.
- The runtime code format (as the framework serialises it) is stated once in R1 so that
  api-verify can assert on it.

## 8. Security

Covered by R7 (§6.1) — permission names follow
`PERM_<PAGE_CODE>_<ACTION>`, minted only from the SRS permission matrix;
a permission name that appears in the plan but not in the matrix is a finding.
Review check: `profile.review.extra_checks` rows whose stage is `P3.1`:
- `ERP-4` (MAJOR): every mutation endpoint declares its PERM_* requirement

## 9. Alignment self-check (ALIGN)

Validates the plan **against itself and its bindings** — the cross-artifact check is
`gov.py analyze` at the gate. Runs automatically after the last content phase; a ✗ is fixed
in the plan before the run ends (the fix is an ADR if it was a choice).

```
ALIGN — MDL v1
TRACEABILITY      every API-*/QR-*/RULE-*/DBF-* used in a phase appears in the Plan Index │ every block carries traces= │ every traces target exists upstream
BINDING (§2A)     no placeholder table/column/key/generation object │ no "see SRS" │ every column cites a DBF │ every message present in ar + en │ business code format explicit
MANIFEST (§4)     only the manifest's columns │ every DBF of every bound table listed │ ⏸ rows have an XM
QRC (§5)          every API with a DB operation has a QR │ every QR carries the agent-reference warning │ no join for lookup labels │ exact generation object named
API (R3)          every RULE in Validations has a catalog row │ platform errors have RULE = PLATFORM-STD + ADR │ create/update exclude system fields │ business code in responses
CROSS-MODULE      every XM from the db-script placed exactly once │ every DEFERRED has strategy + unblock │ inbound stubs use XM-INBOUND-STUB
SECURITY (R7)     every API serving a screen declares its permission │ every screen has a seed row in SEC_PAGES │ no permission outside the matrix
CORE (R1)         layers declared │ domain placement declared │ error signalling declared │ type mapping declared
DECISIONS         every non-obvious inference is an ADR in decisions/MDL/ │ no BLOCKED ADR left unsurfaced
RESULT            PASSED ✓ / list of ✗ (each with the fix applied)
```
Coverage tables (ENT/DBF → phases → QR → XM; RULE → API → catalog code; XM → status → blocks
→ workaround) close the section.

## 10. Registry update — `registry-exec-be-mdl.md`

Written in the same run, after ALIGN ✓ (categories: shared/REGISTRY-SCHEMA.md):

```
REGISTRY — P3.1 — MDL v1
ID RANGES        API-MDL-<first>..<last> · QR-MDL-<first>..<last>
ENTITIES / TABLES bound   · lookups reused / new (keys)
XM STATUS        open / deferred list
CATALOG          code count · rules without message → ADR ids
ALIGN            PASSED ✓ · findings fixed
ADRs             decisions/MDL/ADR-MDL-<seq> … (status)
TRACEABILITY     REQ covered by ≥1 API/DBF: <n>/<total> · orphan REQ: <list — a gate blocker>
```

## 11. Structural self-check (toolkit)

Before the run ends:
```
[ ] every profile key in §6.0 has exactly one PHASE START/END pair, in profile order
[ ] every API-*/XM-* mentioned anywhere has exactly ONE dedicated marker pair
[ ] every SUB id is {PHASE-KEY}-{LABEL}; identical labels under different phases stay distinct
[ ] every PHASE/SUB/atom carries traces=
[ ] no heading label repeats; trailing content (§9–§10 when not a phase) sits after the last PHASE END
[ ] thresholds were checked while writing, not retrofitted
```
Then run the toolkit validation — a non-zero exit is blocking:
```
gov.py split --track backend --module MDL --version 1 --dry-run
```
`gov.py analyze` (traceability matrix, EARS, marker validity, registry ↔ artifact agreement)
runs before the gate `gate:pass-1`; CRITICAL findings keep the gate closed.

## 12. Ambiguity rule (no questions here)

`factory.yaml → ambiguity`, stated once in shared/GOVERNANCE-CORE.md:
- **non-breaking** (does not contradict a locked decision or a REQ) → choose the best-practice
  answer using `profile.knowledge.files` + `erp/` steering, write
  `erp/decisions/MDL/ADR-MDL-{seq:03d}.md` (Context / Decision /
  Consequences / traces) and **continue**;
- **breaking** (contradicts a locked decision or a REQ) → ADR with status
  `BLOCKED`, then **stop**; the
  orchestrator surfaces it at the next human point.
Every "STOP and ask" of earlier engine generations is replaced by this rule.

## 13. Boundaries and hand-off

| Owns (mints) | References (read-only) | Never touches |
|---|---|---|
| `API-*`, `QR-*`; DB Alignment Manifest; Error Catalog; QRC; ALIGN result; ADRs it raises | `POL-*` (P0), `US-*` (P0.5), `REQ-*` (P1), `AC-*` (P1), `ENT-*` (P1), `RULE-*` (P1), `DBF-*` (P2), `XM-*` (P2), `SCR-REQ-*` (P1) | frontend/UX atoms (`UXD`, `SCR` — P3.2), `TC-*` (test-gen), any code, framework annotations, executable queries, test artifacts |

Hand-off (the orchestrator prints it): the plan + registry are split by the toolkit into
`packages/backend-execution/` and delivered on
`gov/{mod}-v{version}-{track}` after the `gate:pass-1` verdict. The implementer reads
the plan in order (index → manifest → ADRs → phases in profile order → QRC → catalog), rewrites
every QR, implements security per R7, and publishes the api-docs file the frontend stage
requires (`factory.passes.2.required_inputs`).


---
# INPUTS (generated current state)

<<<INPUT: srs>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: db-script>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: registry-srs>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
<<<END INPUT>>>

<<<INPUT: registry-db>>>
(MISSING — the orchestrator refuses to run this stage until it exists)
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

