# PASS 1 — module SEC v1 — bundled session (5 stages, one commit per stage)

- `P0` Platform Inception — questions allowed
- `P0.5` PRD — questions allowed
- `P1` SRS — questions forbidden
- `P2` Database — questions forbidden
- `P3.1` Backend Execution Plan — questions forbidden

==============================================================================
# BRIEF — stage `P0` (Platform Inception) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **allowed**. Close every open point inside this dialogue with a researched, recommended answer; never write an external open-questions file.
- Owns IDs: POL — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/SEC/P0/platform-summary.md`
- `erp/modules/SEC/P0/module-registry-sec.md`
- `erp/modules/SEC/P0/business-policies-sec.md`
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
Module        : SEC   Version: 1
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
Registry Version   : 1.3.0
Domain Profile     : erp/domain-profile.md v1
Last Updated       : 2026-09-10 by P3.1 (FIN v1 pass-1 completion — batch complete)
Modules registered : 9   Entity candidates : 29 (13 SEC + 2 MDL + 14 FIN)   Open items : 0
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
| 1.2.0 | 2026-09-10 | MDL v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |
| 1.3.0 | 2026-09-10 | FIN v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2; this batch (SEC→MDL→FIN) is now complete |

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
| 2 | MDL | Master Data Lookup | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 2 |
| 3 | FIN | Finance (General Ledger) | finance | Business — Tier 1 | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 3 |
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
| ENT-MDL-001 | LookupType | MDL | master | SHARED (owner) | REGISTERED |
| ENT-MDL-002 | LookupValue | MDL | lookup | SHARED (owner) | REGISTERED |
| ENT-FIN-001 | Account | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-002 | Dimension | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-003 | DimensionValue | FIN | lookup | PRIVATE | REGISTERED |
| ENT-FIN-004 | JournalEntry | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-005 | JournalLine | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-006 | JournalLineDimension | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-007 | FiscalYear | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-008 | FiscalPeriod | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-009 | EventTypeRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-010 | RuleLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-011 | RecurringTemplate | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-012 | RecurringTemplateLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-013 | AllocationRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-014 | AllocationTarget | FIN | config | PRIVATE | REGISTERED |
(Source: erp/modules/SEC/P1/registry-srs-sec.md, erp/modules/MDL/P1/registry-srs-mdl.md,
erp/modules/FIN/P1/registry-srs-fin.md)

## SHARED ENTITY DECLARATIONS
| Entity | Owner ENT id | Owner module | Consumers so far |
|---|---|---|---|
| User | ENT-SEC-001 | SEC | every future module, for its own audit fields (createdBy/updatedBy reference a SEC principal string, not a physical FK — see SEC db-script §3 AUDIT COLUMNS rule; declared here as the canonical identity source, not as a live FK target) |
| ModuleRegistry | ENT-SEC-004 | SEC | every future module registers one row of itself here (API-SEC-018) |
| ScreenRegistry | ENT-SEC-005 | SEC | every future module registers its screens here (API-SEC-019) |
| ActionRegistry | ENT-SEC-006 | SEC | every future module registers its actions here (API-SEC-020) |
| LookupType | ENT-MDL-001 | MDL | every future module registers its own lookup types here (API-MDL-002) |
| LookupValue | ENT-MDL-002 | MDL | every future module reads active values by key here (API-MDL-011) |

## STRUCTURAL / IMPLEMENTATION REGISTRY
| Module | Version | Tables | DBF range | API range | XM range |
|---|---|---|---|---|---|
| SEC | v1 | 13 (SEC_USER … SEC_SIGNUP_REQUEST) | DBF-SEC-001 … DBF-SEC-104 | API-SEC-001 … API-SEC-027 (QR-SEC-001…038) | none (ROOT) |
| MDL | v1 | 2 (MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE) | DBF-MDL-001 … DBF-MDL-021 | API-MDL-001 … API-MDL-011 (QR-MDL-001…015) | XM-MDL-001 (SOFT-READ → SEC, ACTIVE) |
| FIN | v1 | 14 (FIN_ACCOUNT … FIN_ALLOCATION_TARGET) | DBF-FIN-001 … DBF-FIN-146 | API-FIN-001 … API-FIN-032 (QR-FIN-001…044) | XM-FIN-001 (SOFT-READ → MDL, ACTIVE) |

## CROSS-MODULE DEPENDENCY INDEX
| Candidate ref | Kind | From module | To module | Consumes | Status | Evidence |
|---|---|---|---|---|---|---|
| XM-CAND-001 | — | FIN | SEC | RESOLVED — identity/authorization + SoD are enforced via the platform-standard interceptor (narrated in FIN's P3.1 Phase 1/Phase 7), not a formal XM row; see ADR-FIN-001 | RESOLVED — not an XM (no physical cross-module FK) | domain-profile §6 row "FIN \| SEC \| HARD-FK" (superseded) |
| XM-CAND-002 | — | FIN | MDL | RESOLVED — see XM-FIN-001 below; the platform-summary/module-registry candidate assumed HARD-FK, P2 correctly reclassified to SOFT-READ (ADR-FIN-001) | RESOLVED | domain-profile §6 row "FIN \| MDL \| HARD-FK" (superseded) |
| XM-MDL-001 | SOFT-READ | MDL | SEC | ModuleRegistry (ENT-SEC-004) — validates a lookup type's owner module code | ACTIVE (assigned, not a candidate) | erp/modules/MDL/P2/db-script-mdl.md §2 |
| XM-FIN-001 | SOFT-READ | FIN | MDL | LookupValue — validates every FIN lookup-backed column's code (13 keys) | ACTIVE (assigned, not a candidate) | erp/modules/FIN/P2/db-script-fin.md §2; erp/decisions/FIN/ADR-FIN-001.md |
FIN's SEC dependency (identity/authorization, self-registration) is not an XM row — see
ADR-FIN-001: no physical cross-module FK exists anywhere in this pipeline.
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
| 9 | ADR-FIN-001 — FIN's SEC dependencies (identity/authorization, self-registration) are not modeled as XM rows; only XM-FIN-001 (SOFT-READ → MDL, lookup validation) is assigned, since no physical cross-module FK exists anywhere in this pipeline | ACCEPTED (non-breaking) | erp/decisions/FIN/ADR-FIN-001.md |

## OPEN QUESTION INDEX
none — `domain-profile.md` §10 records no open item.

## PIPELINE / PROGRESS STATUS
| Module | Version | Last committed stage | Last gate verdict | Delivered tracks | Tag |
|---|---|---|---|---|---|
| SEC | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| MDL | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| FIN | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
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
| 2026-09-10 | P0 | MDL | v1 | P0 completed: MDL (platform-summary, module-registry-mdl, business-policies-mdl — 6 POL) |
| 2026-09-10 | P0.5 | MDL | v1 | P0.5 completed: MDL — 5 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | MDL | v1 | P1 completed: MDL — 2 entities, 13 requirements, 13 AC, 4 rules, 2 screen requirements, 0 ADR |
| 2026-09-10 | P2 | MDL | v1 | P2 completed: MDL — 2 tables, 21 DBF, 1 XM (XM-MDL-001, SOFT-READ → SEC) |
| 2026-09-10 | P3.1 | MDL | v1 | P3.1 completed: MDL — 11 API, 15 QR, ALIGN PASSED, 0 new ADR |
| 2026-09-10 | gate:pass-1 | MDL | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 3) |
| 2026-09-10 | split | MDL | v1 | backend/exec split: 11 files, verify ok (21 checked) |
| 2026-09-10 | P0 | FIN | v1 | P0 completed: FIN (platform-summary, module-registry-fin, business-policies-fin — 20 POL, incl. all 14 §12 must-honor points) |
| 2026-09-10 | P0.5 | FIN | v1 | P0.5 completed: FIN — 19 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | FIN | v1 | P1 completed: FIN — 14 entities, 46 requirements, 46 AC, 16 rules, 12 screen requirements, 0 ADR |
| 2026-09-10 | P2 | FIN | v1 | P2 completed: FIN — 14 tables, 146 DBF, 1 XM (XM-FIN-001, SOFT-READ → MDL); ADR-FIN-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | FIN | v1 | P3.1 completed: FIN — 32 API, 44 QR, ALIGN PASSED (14-point §12 coverage confirmed), 0 new ADR |
| 2026-09-10 | gate:pass-1 | FIN | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | FIN | v1 | backend/exec split: 15 files, verify ok (45 checked) |
| 2026-09-10 | BATCH | (platform) | — | GENERATION-INSTRUCTIONS.md batch complete: SEC v1 → MDL v1 → FIN v1, all pass-1 APPROVE, in mandated dependency order |
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
# BRIEF — stage `P0.5` (PRD) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **allowed**. Close every open point inside this dialogue with a researched, recommended answer; never write an external open-questions file.
- Owns IDs: US — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/SEC/P0_5/prd-sec.md`
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
Module        : SEC   Version: 1
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
  Module             : SEC
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
PRD EXTRACTION REPORT — SEC — [date]
══════════════════════════════════════════════════════════════════
STORIES DRAFTED
  + US-SEC-001 — [one line] — Traces: [ids] — Source: [ref]
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
# PRD — [Module display] (SEC)
══════════════════════════════════════════════════════════════════
Module          : SEC     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : [N]   Policies covered : [N]/[N]   Deferred : [N]
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES
US-SEC-001
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
event history    : "P0.5 completed: SEC v1 — [N] stories, [N] decisions"
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
# PLATFORM SUMMARY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile : erp   Domain profile : v1   Registry : v1.0.0
══════════════════════════════════════════════════════════════════

## OVERVIEW
منصة ERP متعددة الوحدات، قائمة على مبدأ "كل ما يمكن أن يتغير = بيانات، لا شيفرة"،
تُبنى بـ Spring (خلفية) وReact (واجهة) على PostgreSQL كهدف بناء وحيد؛ النظام القديم
Oracle/ADF يبقى مصدر أحداث فقط. هذه الدفعة تُنشئ ثلاث وحدات تأسيسية بالترتيب الصارم
SEC ← MDL ← FIN؛ باقي وحدات المنصة (ORG, PRC, HR, INV, SLS, CTR) معروفة الرمز
والسياق من `profiles/erp.yaml` لكنها خارج نطاق هذه الدفعة. [domain-profile §1-§2]

## MODULES
| #   | Code | Module (ar/en) | Bounded context | Layer | Type | Depends on | Status |
|-----|------|--------|-----------------|-------|------|------------|--------|
| 1.1 | ORG | الهيكل التنظيمي / Organization | organization | L1 | master data | ROOT | NEW (not this batch) |
| 1.2 | SEC | الأمان / Security | organization | L1 | security engine | ROOT | NEW — this batch, first |
| 1.3 | MDL | البيانات المرجعية / Master Data Lookup | organization | L1 | reference | ROOT | NEW (not this batch) |
| 2.1 | PRC | المشتريات / Procurement | supply | L3 | transactional | SEC, MDL (SOFT/HARD, not yet detailed) | NEW (not this batch) |
| 2.2 | FIN | الحسابات العامة / Finance (GL) | finance | L3 | transactional/reporting | SEC (HARD), MDL (HARD) | NEW — this batch, third |
| 2.3 | INV | المخزون / Inventory | supply | L3 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.1 | SLS | المبيعات / Sales | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.2 | CTR | العقود / Contracts | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.3 | HR | الموارد البشرية / Human Resources | people | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
Status: NEW (Phase 2 produces) · EXISTING (Phase 2 extends) · EXCEPTION (read as-is)
Numbering: [tier].[sequence within tier] — the user requests Phase 2 by this number.
This run's Phase 2 request: **1.2 SEC** (module convergence below).

## DEPENDENCY MAP
Build order: Tier 1 [ORG, SEC, MDL] → Tier 2 [PRC, FIN, INV] → Tier 3 [SLS, CTR, HR] → Tier 4 (reporting, none yet)
Key dependencies (one line each):
  FIN → HARD → SEC : identity + module/screen/action grants + SoD (entry-creator ≠ period-close approver)
  FIN → HARD → MDL : payment methods, accounting event types, account types, period states, journal types
  SEC → SOFT → NOTIF (external, ready) : password-reset message, optional only
  FIN → SOFT → NOTIF (external, ready) : period-close-awaiting notice, optional only
  FIN → SOFT → FILESVC (external, ready) : statement export, optional only
  (host business system) → EVENT → FIN : canonical accounting event only, no table read/write

## DEFERRED (not in scope for this version)
| Item | Reason / activation trigger |
| ORG, PRC, HR, INV, SLS, CTR detailed analysis | user-scoped this batch to SEC → MDL → FIN only (GENERATION-INSTRUCTIONS.md §3) |
| Workflow engine | profile: `forbidden` |
| Notifications / File Service redesign | ready external modules; consumed only on real need, never re-specified [security-module-plan-en.md §8; lookup-module-plan-en.md §5; general-accounting-system-plan-en.md §2.3] |
| Multi-currency, multi-ledger/entity, statistical accounts, multi-pattern calendar, attachments (FIN) | excluded by explicit decision [general-accounting-system-plan-en.md §15] |

## RESOLVED DECISIONS (this phase)
| # | Point | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Numbering/tiering of the 9 platform modules | Foundation=1.x, Core business=2.x, Extended business=3.x, per KB §1 tiers | Yes — no conflicting statement in the plans | [KB:erp-domain-standards §1] |
| 2 | Phase 2 request for this P0 run | Module 1.2 SEC (first of the batch order SEC→MDL→FIN) | Yes — GENERATION-INSTRUCTIONS.md §3 | GENERATION-INSTRUCTIONS.md §3 |

## OPEN ITEMS
None — platform scope fully determined for this batch (SEC, MDL, FIN); the other six
modules are out of scope, not ambiguous — no registry ↔ vision conflict exists.

## NEXT STEP
Module 1.2 SEC converges below (module-registry-sec.md, business-policies-sec.md).
Reply with a plain instruction to adjust, or request module 1.3 (MDL) next per the
mandated order.

<<<END INPUT>>>

<<<INPUT: module-registry>>>
## MODULE REGISTRY — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module Code    : SEC   (profile.vocabulary.module_prefixes)
Bounded context: organization
Layer / Type   : L1 / security engine     Execution tier : 1.2
Source         : NEW
Knowledge      : new project/security-module-plan-en.md; profiles/erp/knowledge/erp-domain-standards.md §4
Readiness      : READY
══════════════════════════════════════════════════════════════════

ENTITIES OWNED   (names only — entity IDs are assigned by P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| المستخدم / User | security | SHARED — every module's audit fields (createdBy/updatedBy) FK to it | security-module-plan-en.md §3, §4.4; profiles/erp.yaml conventions.entity_defaults |
| الدور / Role | security | PRIVATE | security-module-plan-en.md §4.4 |
| ربط المستخدم بالدور / UserRoleAssignment | security | PRIVATE | security-module-plan-en.md §4.4 |
| سجل الوحدات / ModuleRegistry | security | SHARED — every consuming module registers itself here | security-module-plan-en.md §4.3, §7 |
| سجل الشاشات / ScreenRegistry (SEC_PAGES) | security | SHARED — every consuming module registers its screens here | security-module-plan-en.md §4.3, §4.5; profiles/erp.yaml conventions.security_model.page_registry |
| سجل الإجراءات / ActionRegistry (permission catalog, PERM_*) | security | SHARED — every consuming module registers its actions here | security-module-plan-en.md §4.1, §4.3; profiles/erp.yaml conventions.security_model.permission_pattern |
| منح الوحدة للدور / RoleModuleGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| منح الشاشة للدور / RoleScreenGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| منح الإجراء للدور / RoleActionGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| الجلسة النشطة / ActiveSession | security | PRIVATE | security-module-plan-en.md §5.1, §5.3 |
| سجل التدقيق / AuditLogEntry | security | PRIVATE | security-module-plan-en.md §5.2-§5.3 |
| رمز إعادة تعيين كلمة المرور / PasswordResetToken | security | PRIVATE | security-module-plan-en.md §3 |
| طلب تسجيل معلّق / SignupRequest | security | PRIVATE | security-module-plan-en.md §3 |

LOOKUPS OWNED    (value lists this module masters — registered into MDL, not stored locally)
| Lookup key | Description | Initial values (only those the user named) | Source |
|---|---|---|---|
| USER_STATUS | حالة حساب المستخدم / user account status | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
| AUDIT_EVENT_TYPE | نوع حدث التدقيق / audit-log event type | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
Rule (profile): all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs

LOOKUPS CONSUMED (from other modules)
| Lookup key | Owner code | READ-ONLY |
None — SEC is a Tier-0 foundation module; it consumes no other in-scope module's lookups this batch.

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |
None — SEC is ROOT; it depends on no other in-scope module.

DEPENDENCIES
| Module code | HARD / SOFT / LOOKUP | What is consumed |
None (within the platform's module registry).
External (not a registry module): Notifications (ready, SOFT, optional — e.g. password-reset email) [security-module-plan-en.md §8].
ROOT: YES

AUTO-DECISIONS
AUTO: registered USER_STATUS and AUDIT_EVENT_TYPE as SEC-owned lookup types (values: USER_STATUS = PENDING/ACTIVE/DISABLED/LOCKED; AUDIT_EVENT_TYPE = LOGIN_SUCCESS/LOGIN_FAILED/LOGOUT/PASSWORD_RESET_REQUESTED/PASSWORD_RESET_COMPLETED/ROLE_ASSIGNED/ROLE_REVOKED/MODULE_GRANTED/MODULE_REVOKED/SCREEN_GRANTED/SCREEN_REVOKED/ACTION_GRANTED/ACTION_REVOKED/SESSION_TERMINATED — these initial values are AUTO, not user-named, so they are listed here rather than in the Lookups Owned "Initial values" column)
  FROM: profiles/erp.yaml conventions.lookups ("no hardcoded enums in APIs or field specs") + lookup-module-plan-en.md §3
  IF WRONG: fold these states into a plain internal enum on the User/AuditLogEntry entities instead of a shared lookup type — revise this module registry and business-policies-sec.md's CUSTOM LOOKUP VALUES accordingly.
AUTO: classified User as SHARED
  FROM: profiles/erp.yaml conventions.entity_defaults (every entity carries createdBy/updatedBy, which must reference a real User row)
  IF WRONG: none recommended — dropping this would break audit traceability platform-wide.
AUTO: tier/numbering 1.2 (Foundation, Tier 0)
  FROM: [KB:erp-domain-standards §1]
  IF WRONG: renumber if the platform later reprioritizes; number stability rule applies once confirmed.
AUTO: Session and AuditLogEntry kept PRIVATE, not SHARED
  FROM: security-module-plan-en.md §5 scopes the audit feed to SEC's own security events (logins, resets, role/permission changes) only, not other modules' business events
  IF WRONG: promote to SHARED if a later module needs to append platform-wide audit events through SEC — would need its own ADR at that time.

RESOLVED DECISIONS (dialogue, this module)
| # | Point | Recommended | Confirmed by user | Sources |
None — security-module-plan-en.md fully settles this module's P0 scope; no point required dialogue.

POLICIES OWNED (full text in business-policies-sec.md)
POL-SEC-001, POL-SEC-002, POL-SEC-003, POL-SEC-004, POL-SEC-005, POL-SEC-006,
POL-SEC-007, POL-SEC-008, POL-SEC-009, POL-SEC-010, POL-SEC-011
══════════════════════════════════════════════════════════════════

<<<END INPUT>>>

<<<INPUT: business-policies>>>
## BUSINESS POLICIES — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module   : SEC     Source of truth : new project/security-module-plan-en.md
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES   (only from user text or confirmed dialogue answers)

POL-SEC-001 — أسبقية بوابة الوحدة / Module-gate precedence
  Statement (ar) : يجب على النظام تقييم منح الوحدة للدور قبل تقييم أي منح شاشة أو إجراء ضمن تلك الوحدة.
  Statement (en) : The system shall evaluate a role's module grant before evaluating any screen or action grant within that module.
  Pattern   : ubiquitous
  Trigger   : Any authorization check
  Rationale : الوصول الخشن (coarse) يُرفض ابتداءً، لا أن يُخفى فقط
  Source    : security-module-plan-en.md §4.2
  Status    : CONFIRMED

POL-SEC-002 — منع منح الشاشة/الإجراء اليتيم / No orphaned screen/action grant
  Statement (ar) : إذا لم يملك الدور منح وحدة الطلب، فيجب على النظام رفض أي منح شاشة أو إجراء ضمن تلك الوحدة لذلك الدور.
  Statement (en) : If a role does not hold the module grant of a requested screen or action, then the system shall reject that screen or action grant for the role.
  Pattern   : unwanted
  Trigger   : Grant assignment (Role editor)
  Rationale : سلامة بنيوية — استحالة وجود منح يتيم بالبناء
  Source    : security-module-plan-en.md §4.2
  Status    : CONFIRMED

POL-SEC-003 — المستخدم المعلّق بلا صلاحيات / Pending sign-up holds no permission
  Statement (ar) : أثناء انتظار تفعيل تسجيل مستخدم ذاتي، يجب على النظام ألا يمنح ذلك المستخدم أي صلاحية.
  Statement (en) : While a self-registered user's sign-up is pending activation, the system shall grant that user no permission.
  Pattern   : state
  Trigger   : Sign-up submitted
  Rationale : المستخدم المعلّق لا يملك صلاحيات حتى المنح الصريح
  Source    : security-module-plan-en.md §3
  Status    : CONFIRMED

POL-SEC-004 — عدم إرسال تجزئة كلمة المرور للعميل / Password hash never reaches the client
  Statement (ar) : يجب على النظام ألا يرسل تجزئة (hash) كلمة المرور إلى العميل مطلقًا.
  Statement (en) : The system shall never transmit a password hash to the client.
  Pattern   : ubiquitous
  Trigger   : Any authentication-related response
  Rationale : تخزين آمن لكلمة المرور
  Source    : security-module-plan-en.md §3
  Status    : CONFIRMED

POL-SEC-005 — دعم فصل المهام للمستهلكين / Segregation of duties support for consumers
  Statement (ar) : حيثما تُعلن وحدة مستهلكة إجراءين متعارضين، يجب على النظام دعم اشتراط أن يُسنَدا لدورين/مستخدمين متمايزين.
  Statement (en) : Where a consumer module declares two conflicting actions, the system shall support requiring those actions be held by distinct roles or users.
  Pattern   : optional
  Trigger   : Consumer module registration (e.g. FIN's entry-creator vs period-close-approver)
  Rationale : فصل المهام عند المُستهلِك يُنفَّذ عبر RBAC المشترك
  Source    : security-module-plan-en.md §4.4; general-accounting-system-plan-en.md §8.2/§10.3
  Status    : CONFIRMED

POL-SEC-006 — القائمة من المنح الفعلية فقط / Menu built from effective grants only
  Statement (ar) : يجب على النظام بناء قائمة كل مستخدم من منحه الفعلية للوحدات والشاشات فقط.
  Statement (en) : The system shall build each user's menu from only that user's effective module and screen grants.
  Pattern   : ubiquitous
  Trigger   : Menu render
  Rationale : قائمة ديناميكية بمستويين مبنية بيانيًا
  Source    : security-module-plan-en.md §6
  Status    : CONFIRMED

POL-SEC-007 — إخفاء الوحدة غير الممنوحة كليًا / Absent module entirely hidden
  Statement (ar) : إذا كانت أدوار المستخدم لا تمنح وحدة ما، فيجب على النظام حذف تلك الوحدة كليًا من قائمته ومنع الوصول إليها بالرابط المباشر.
  Statement (en) : If a user's roles do not grant a module, then the system shall omit that module entirely from the user's menu and block direct URL access to it.
  Pattern   : unwanted
  Trigger   : Menu render; direct navigation attempt
  Rationale : غياب الوحدة = غياب تام، لا إخفاء بصري فقط
  Source    : security-module-plan-en.md §4.2, §6
  Status    : CONFIRMED

POL-SEC-008 — الصلاحية الفعلية = اتحاد الأدوار / Effective permission = union across roles
  Statement (ar) : يجب على النظام حساب الصلاحيات الفعلية للمستخدم كاتحاد منح كل الأدوار المسندة إليه.
  Statement (en) : The system shall compute a user's effective permissions as the union of the grants of all roles assigned to that user.
  Pattern   : ubiquitous
  Trigger   : Any authorization check
  Rationale : مستخدم قد يحمل أكثر من دور
  Source    : security-module-plan-en.md §4.4
  Status    : CONFIRMED

POL-SEC-009 — سجل تدقيق غير قابل للتعديل / Immutable audit trail
  Statement (ar) : يجب على النظام الاحتفاظ بكل قيد في سجل التدقيق (دخول، دخول فاشل، إعادة تعيين كلمة مرور، تغيير دور/صلاحية) دون تعديل أو حذف.
  Statement (en) : The system shall retain every audit-log entry (login, failed login, password reset, role/permission change) without modification or deletion.
  Pattern   : ubiquitous
  Trigger   : Any security-relevant event
  Rationale : الأثر التدقيقي متطلب أمني لا رفاهية
  Source    : security-module-plan-en.md §5.2-§5.3
  Status    : CONFIRMED

POL-SEC-010 — أرقام لوحة التحكم مُشتقة دائمًا / Dashboard figures are always derived
  Statement (ar) : يجب على النظام حساب كل رقم في لوحة تحكم الأمان وقت الاستعلام من بيانات حية، لا من عدّاد مُخزَّن.
  Statement (en) : The system shall compute every admin-dashboard figure at query time from live data, never from a stored counter.
  Pattern   : ubiquitous
  Trigger   : Dashboard render
  Rationale : انضباط مصدر الحقيقة الواحد المُتَّبع عبر المنصة
  Source    : security-module-plan-en.md §5.2
  Status    : CONFIRMED

POL-SEC-011 — كل زر/شاشة تحكمها الصلاحية / Every widget/screen permission-gated
  Statement (ar) : يجب على النظام إظهار عنصر لوحة تحكم الأمان للمستخدم فقط إذا كان دوره يمنحه صلاحيته.
  Statement (en) : The system shall show an admin-dashboard widget to a user only if that user's role grants the widget's underlying permission.
  Pattern   : ubiquitous
  Trigger   : Dashboard render
  Rationale : كل عنصر محكوم بالصلاحية، بما فيها بوابة الوحدة
  Source    : security-module-plan-en.md §5.1
  Status    : CONFIRMED

CUSTOM LOOKUP VALUES   (values the user named that the standard lists lack)
| Lookup key | Added values | Source |
|---|---|---|
None named directly by the user; AUTO-added initial value sets are recorded in
module-registry-sec.md → AUTO-DECISIONS (USER_STATUS, AUDIT_EVENT_TYPE), not here —
they are not user-stated custom values.

SCOPE EXCEPTIONS   (explicit exclusions or non-standard scope)
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Multi-factor authentication | Not mentioned by the plan; not built this batch | explicit future request | security-module-plan-en.md §3 (lists only login/sign-up/reset) |
| SSO / external identity providers | Not mentioned by the plan; not built this batch | explicit future request | security-module-plan-en.md §3 |

RESOLVED DECISIONS (dialogue, this module)
| # | Question | Recommended answer | Confirmed by user | Sources |
None — no open question was raised for SEC; the plan is fully prescriptive for this stage's scope.
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
# BRIEF — stage `P1` (SRS) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/SEC/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: REQ, AC, ENT, RULE, SCR-REQ — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/SEC/P1/srs-sec.md`
- `erp/modules/SEC/P1/registry-srs-sec.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C4** PRD → SRS (human PRD approval in between): C4.1 exists {'artifact': 'prd'} [CRITICAL]; C4.2 gate-approved {'gate': 'prd-approval'} [CRITICAL]; C4.3 ids-owned {'stage': 'P0.5'} [CRITICAL]; C4.4 traces {'from': 'US', 'to': ['POL'], 'min': 1} [MAJOR]; C4.5 no-questions {'stage': 'P0.5'} [CRITICAL]; C4.6 languages {'stage': 'P0.5'} [MAJOR]
- **C5** SRS → database: C5.1 exists {'artifact': 'srs'} [CRITICAL]; C5.2 ears {'kind': 'REQ', 'patterns': 'factory.ids.ears.patterns'} [CRITICAL]; C5.3 traces {'from': 'REQ', 'to': ['US'], 'min': 1} [MAJOR]; C5.4 orphans {'kind': 'REQ', 'referenced_by': ['AC'], 'min': 1} [CRITICAL]; C5.5 traces {'from': 'AC', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.6 traces {'from': 'RULE', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.7 ids-owned {'stage': 'P1'} [CRITICAL]; C5.8 registry-agree {'artifact': 'srs', 'registry': 'registry-srs', 'kinds': ['REQ', 'AC', 'ENT', 'RULE']} [MAJOR]; C5.9 no-questions {'stage': 'P1'} [CRITICAL]; C5.10 languages {'stage': 'P1'} [MAJOR]; C5.11 ids-continue {'stage': 'P1'} [CRITICAL]; C5.12 data-source {'kind': 'RULE', 'label': 'Data source', 'resolves_to': ['ENT'], 'deferral': 'DEFERRED'} [CRITICAL]
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]; C6.9 data-source {'kind': 'RULE', 'label': 'Data source', 'resolves_to': ['ENT'], 'deferral': 'DEFERRED', 'bound_in': 'db-script'} [CRITICAL]
- **C10** acceptance criteria → test generation (standalone): C10.1 traces {'from': 'TC', 'to': ['AC', 'XM', 'UXD'], 'min': 1, 'mode': 'any'} [CRITICAL]; C10.2 orphans {'kind': 'AC', 'referenced_by': ['TC'], 'min': 1} [MAJOR]; C10.3 markers {'artifact': 'backend-test-plan', 'track': 'backend', 'plan': 'test'} [CRITICAL]; C10.4 markers {'artifact': 'frontend-test-plan', 'track': 'frontend', 'plan': 'test'} [CRITICAL]; C10.5 ids-owned {'stage': 'test-gen'} [CRITICAL]; C10.6 exists {'artifact': 'test-execution-manifest', 'when': 'profile.stack.testing.manifest'} [MINOR]

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
Module        : SEC   Version: 1
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
| PRIVATE | fully owned by this module | `ENT-SEC-[SEQ]` — [name] — PRIVATE |
| SHARED (owner) | other modules consume read-only | `ENT-SEC-[SEQ]` — [name] — SHARED (owner) |
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
REQ-SEC-[SEQ] — [short name]
  Pattern    : [ubiquitous | state | event | optional | unwanted | complex]
  Statement  : [one EARS sentence — one behaviour, one subject "the system"]
  Traces     : P0.5-SEC-[SEQ] [, …]      (≥ 1, mandatory)
  Entities   : ENT-SEC-[SEQ] [, …]
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
AC-SEC-[SEQ] — [REQ-SEC-[SEQ]]
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
RULE-SEC-[SEQ] — [short name]
  Scope      : ENT-SEC-[SEQ]
  Trigger    : [when evaluated — on create / update / submit / transition …]
  Statement  : The system shall [prevent / require / validate …] when [condition]
  Message    : ar: [text] · en: [text]   (business language, not a literal translation)
  Traces     : REQ-SEC-[SEQ] [, …]                     (≥ 1, mandatory)
  Data source: ENT-SEC-[SEQ].[field] [, …]             (mandatory — see below)
  Source     : [policy id / story / DEFAULT / ADR]
  Test-Hint  : [optional, one line of business intent for test-gen; omit if obvious]
```

Rules: a RULE formalises a constraint that a REQ needs; it never introduces behaviour
absent from every REQ. Database errors never reach users — every constraint that can
fail has a RULE with a message. Rules are defined once (A5) and referenced by ID from
every screen block.

**`Data source` — where the data the check READS comes from (mandatory).** `Scope` says
which entities the rule *guards*; `Data source` says which declared fields the rule
*reads to decide*. Every field named here is an `ENT-SEC-[SEQ].[field]` that A3
actually declares (the next stage binds each one to a column, so a field A3 never
declares can never be read at runtime).

A rule whose statement leans on data this module does not declare — "a
{module|admin|externally}-declared X", "a configured Y", "a registered pair" — has two honest
outcomes, and no third:

```
  Data source: ENT-SEC-[SEQ].[field]        ← the declaration surface exists: name it,
                                                    and A3 carries the field / A6 the lookup
  Data source: DEFERRED — no declaration surface in this version ([what would be needed])
```

A `DEFERRED` rule is still written, still traced and still counted, but it is marked
unenforceable *here* — the next stages do not mint an error code, an enforcement query or
an enforcing endpoint for it, and test-gen does not derive a test case whose precondition
no API of this version can establish. Emitting such a rule as if it were enforceable
produces a guard that compiles, an error code that is registered, and a check that can
never fire. `gov.py analyze` resolves this mechanically (`data-source`): a rule with
neither a resolvable `Data source` nor the deferral marker is a finding, not a pass.

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
SCR-REQ-SEC-[SEQ] — [screen name in each of ar/en]
  Purpose      : [what the user achieves]
  Entities     : ENT-SEC-[SEQ] [, …]
  Operations   : [search / list / create / read / update / deactivate / custom …]
  Users        : [roles]
  Navigation   : [module] → [menu] → [screen]; from: [screens]; to: [screens]
  Content shape: [flat record | header + repeating lines with totals | true hierarchy
                  (parent/child) | other (journal, calendar …)] — a hint, not a design
  Traces       : REQ-SEC-[SEQ] [, …]
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

ADR file : erp/decisions/SEC/ADR-{MOD}-{seq:03d}.md
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
# SRS — [Module display] (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved [date])
Counts : REQ [N] · AC [N] · ENT [N] · RULE [N] · SCR-REQ [N] · ADR [N]
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
### ENT-SEC-001 — [name]
| Kind | Ownership | Business number (yes/no — per §3.3 test) | Operations | Cross-module | Source |
| Field | Logical type | Required | Values / source (lookup key, ENT ref) | Notes | Label-ar | Label-en |
(repeat per entity)

## A4 — Functional requirements (EARS) and acceptance criteria
### REQ-SEC-001 — [name]        (record §4)
#### AC-SEC-001 … (record §4.1, ≥ 1 per REQ)
(repeat per requirement)

## A5 — Business rules
### RULE-SEC-001 — [name]       (record §5)
(repeat per rule — the ONLY place rule text appears)

## A6 — Lookups                      (§6 — the ONLY place lookup values appear)

## A7 — Status lifecycle             (§6 — diagram only; "not applicable" if ≤ 2 states)

## A8 — Module dependencies
| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
| External service | Purpose | Integration kind |

# PART B — SCREEN REQUIREMENTS   (one block per SCR-REQ; references PART A by ID only)

## SCR-REQ-SEC-001 — [name]
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

Single-source rule: PART A defines; PART B references by ID ("applies RULE-SEC-003").
Restating rule, lookup or entity text in PART B is a DUPLICATE finding (MAJOR).

---

## 11 — `registry-srs-{mod}.md` — registry content

```
## REGISTRY — P1 — SEC v1
Entities      : ENT id · name · kind · PRIVATE / SHARED(owner) · status REGISTERED
Consumed      : owner ENT id · owner module · HARD-FK / SOFT-READ   (→ dependency index)
Lookups owned : key · ENT · values count          Lookups consumed : key · owner
Screens       : SCR-REQ id · name · page code
Requirements  : REQ count · AC count · RULE count · last sequence per atom
                (REQ: [n], AC: [n], ENT: [n], RULE: [n], SCR-REQ: [n])
Decisions     : ADR ids (+ BLOCKED, if any)
Event         : "P1 completed: SEC v1 — [counts]"
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
- [ ] Every RULE carries a `Data source` that either names `ENT-SEC-[SEQ].[field]` values A3 declares, or is the explicit `DEFERRED — no declaration surface in this version` marker (§5).
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
# PRD — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module          : SEC     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : 12   Policies covered : 11/11   Deferred : 0
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES

US-SEC-001
  Title          : تسجيل الدخول / Login
  Story          : As a registered platform user, I need to sign in securely, so that I receive an identity/session every consuming module trusts.
  Priority       : HIGH — implied ("secure sign-in issuing a session/token consuming modules trust", the entry point of the whole module)
  Success metric : —
  Traces         : POL-SEC-004
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-002
  Title          : التسجيل الذاتي / Sign-up
  Story          : As a prospective user, I need to self-register, so that an administrator can later grant me access without creating my account manually.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-003
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-003
  Title          : نسيت / إعادة تعيين كلمة المرور / Forgot / reset password
  Story          : As a user who lost access to my password, I need a secure self-service reset, so that I can regain access without exposing my credentials.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-004
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-004
  Title          : إدارة المستخدمين / Manage users
  Story          : As a security administrator, I need to add, edit, activate, deactivate a user and assign one or more roles, so that access reflects who currently should have it.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-SEC-008
  Source         : security-module-plan-en.md §4.4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-005
  Title          : إدارة الأدوار والمنح الثلاثية / Manage roles and the three-level grant editor
  Story          : As a security administrator, I need to create/edit/deactivate roles and grant them modules, then screens within a granted module, then actions on a granted screen, so that access always follows the module-first hierarchy.
  Priority       : HIGH — plan names this "the core improvement" (§4.1)
  Success metric : —
  Traces         : POL-SEC-001, POL-SEC-002
  Source         : security-module-plan-en.md §4.1, §4.2, §4.4, §4.5
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-006
  Title          : سجل الوحدة/الشاشة/الإجراء / Module / screen / action registry
  Story          : As a consuming module's integrator, I need to register my module, its screens and its actions as data, so that my module can be granted to roles without any change to security code.
  Priority       : HIGH — foundational to every other module's onboarding
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §4.3, §4.5, §7; module-registry-sec.md → ENTITIES OWNED (ModuleRegistry, ScreenRegistry, ActionRegistry)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-007
  Title          : فصل المهام للمستهلكين / Segregation of duties for consumers
  Story          : As a consuming module (e.g. Accounting), I need to require two conflicting actions be held by distinct roles/users, so that no single user can both perform and approve a sensitive transition.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-005
  Source         : security-module-plan-en.md §4.4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-008
  Title          : القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu
  Story          : As a signed-in user, I need my menu to show only the modules and screens my roles actually grant, so that I never see or reach something I am not authorized for.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-SEC-006, POL-SEC-007
  Source         : security-module-plan-en.md §6
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-009
  Title          : لوحة تحكم الأمان / Admin dashboard
  Story          : As a security administrator, I need a landing dashboard (users overview, failed logins, active sessions, recent activity, roles/permissions summary, onboarding funnel), so that I can spot risk and over-privileged accounts at a glance.
  Priority       : MEDIUM — plan names this a "recommended baseline" (§5)
  Success metric : —
  Traces         : POL-SEC-010, POL-SEC-011
  Source         : security-module-plan-en.md §5.1, §5.2
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-010
  Title          : سجل التدقيق / Audit log
  Story          : As a security administrator, I need a searchable, filterable, exportable (CSV) audit log of logins, failed logins, resets and role/permission changes, so that I have a complete and trustworthy record of every security-relevant event.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-009
  Source         : security-module-plan-en.md §5.3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-011
  Title          : إدارة الجلسات النشطة / Active sessions management
  Story          : As a security administrator, I need to see currently signed-in users and force-terminate a session when authorized, so that I can respond to a compromised or abandoned session.
  Priority       : MEDIUM
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §5.1, §5.3; module-registry-sec.md → ENTITIES OWNED (ActiveSession)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-012
  Title          : إشعار اختياري عبر خدمة الإشعارات / Optional notification on password reset
  Story          : As a user requesting a password reset, I need to optionally receive that reset through the platform's ready Notifications service, so that I am not blocked if this integration is skipped.
  Priority       : LOW — explicitly "only on real need", never a hard dependency
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §8; new project/integration-notifications-fileservice.md §1
  Status         : DRAFT → APPROVED (by the PRD approval gate)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-SEC-001 | POL-SEC-004 | security-module-plan-en.md §3 |
| US-SEC-002 | POL-SEC-003 | security-module-plan-en.md §3 |
| US-SEC-003 | POL-SEC-004 | security-module-plan-en.md §3 |
| US-SEC-004 | POL-SEC-008 | security-module-plan-en.md §4.4 |
| US-SEC-005 | POL-SEC-001, POL-SEC-002 | security-module-plan-en.md §4.1-§4.2 |
| US-SEC-006 | — (scope only) | security-module-plan-en.md §4.3, §7 |
| US-SEC-007 | POL-SEC-005 | security-module-plan-en.md §4.4 |
| US-SEC-008 | POL-SEC-006, POL-SEC-007 | security-module-plan-en.md §6 |
| US-SEC-009 | POL-SEC-010, POL-SEC-011 | security-module-plan-en.md §5.1-§5.2 |
| US-SEC-010 | POL-SEC-009 | security-module-plan-en.md §5.3 |
| US-SEC-011 | — (scope only) | security-module-plan-en.md §5.1, §5.3 |
| US-SEC-012 | — (scope only) | security-module-plan-en.md §8 |
Every policy POL-SEC-001 … POL-SEC-011 appears in at least one row above (001,002 →
US-005; 003 → US-002; 004 → US-001/US-003; 005 → US-007; 006,007 → US-008;
008 → US-004; 009 → US-010; 010,011 → US-009).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
None — security-module-plan-en.md and business-policies-sec.md left no story's scope,
priority or role genuinely ambiguous; no dialogue question was required.

## DEFERRED
| US | Reason | Activation trigger |
None — every capability named in security-module-plan-en.md is represented by a story
in this v1 PRD; nothing was pushed out.

## APPROVAL
Approved by : PENDING   Date : PENDING
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════

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
Registry Version   : 1.3.0
Domain Profile     : erp/domain-profile.md v1
Last Updated       : 2026-09-10 by P3.1 (FIN v1 pass-1 completion — batch complete)
Modules registered : 9   Entity candidates : 29 (13 SEC + 2 MDL + 14 FIN)   Open items : 0
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
| 1.2.0 | 2026-09-10 | MDL v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |
| 1.3.0 | 2026-09-10 | FIN v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2; this batch (SEC→MDL→FIN) is now complete |

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
| 2 | MDL | Master Data Lookup | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 2 |
| 3 | FIN | Finance (General Ledger) | finance | Business — Tier 1 | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 3 |
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
| ENT-MDL-001 | LookupType | MDL | master | SHARED (owner) | REGISTERED |
| ENT-MDL-002 | LookupValue | MDL | lookup | SHARED (owner) | REGISTERED |
| ENT-FIN-001 | Account | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-002 | Dimension | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-003 | DimensionValue | FIN | lookup | PRIVATE | REGISTERED |
| ENT-FIN-004 | JournalEntry | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-005 | JournalLine | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-006 | JournalLineDimension | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-007 | FiscalYear | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-008 | FiscalPeriod | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-009 | EventTypeRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-010 | RuleLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-011 | RecurringTemplate | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-012 | RecurringTemplateLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-013 | AllocationRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-014 | AllocationTarget | FIN | config | PRIVATE | REGISTERED |
(Source: erp/modules/SEC/P1/registry-srs-sec.md, erp/modules/MDL/P1/registry-srs-mdl.md,
erp/modules/FIN/P1/registry-srs-fin.md)

## SHARED ENTITY DECLARATIONS
| Entity | Owner ENT id | Owner module | Consumers so far |
|---|---|---|---|
| User | ENT-SEC-001 | SEC | every future module, for its own audit fields (createdBy/updatedBy reference a SEC principal string, not a physical FK — see SEC db-script §3 AUDIT COLUMNS rule; declared here as the canonical identity source, not as a live FK target) |
| ModuleRegistry | ENT-SEC-004 | SEC | every future module registers one row of itself here (API-SEC-018) |
| ScreenRegistry | ENT-SEC-005 | SEC | every future module registers its screens here (API-SEC-019) |
| ActionRegistry | ENT-SEC-006 | SEC | every future module registers its actions here (API-SEC-020) |
| LookupType | ENT-MDL-001 | MDL | every future module registers its own lookup types here (API-MDL-002) |
| LookupValue | ENT-MDL-002 | MDL | every future module reads active values by key here (API-MDL-011) |

## STRUCTURAL / IMPLEMENTATION REGISTRY
| Module | Version | Tables | DBF range | API range | XM range |
|---|---|---|---|---|---|
| SEC | v1 | 13 (SEC_USER … SEC_SIGNUP_REQUEST) | DBF-SEC-001 … DBF-SEC-104 | API-SEC-001 … API-SEC-027 (QR-SEC-001…038) | none (ROOT) |
| MDL | v1 | 2 (MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE) | DBF-MDL-001 … DBF-MDL-021 | API-MDL-001 … API-MDL-011 (QR-MDL-001…015) | XM-MDL-001 (SOFT-READ → SEC, ACTIVE) |
| FIN | v1 | 14 (FIN_ACCOUNT … FIN_ALLOCATION_TARGET) | DBF-FIN-001 … DBF-FIN-146 | API-FIN-001 … API-FIN-032 (QR-FIN-001…044) | XM-FIN-001 (SOFT-READ → MDL, ACTIVE) |

## CROSS-MODULE DEPENDENCY INDEX
| Candidate ref | Kind | From module | To module | Consumes | Status | Evidence |
|---|---|---|---|---|---|---|
| XM-CAND-001 | — | FIN | SEC | RESOLVED — identity/authorization + SoD are enforced via the platform-standard interceptor (narrated in FIN's P3.1 Phase 1/Phase 7), not a formal XM row; see ADR-FIN-001 | RESOLVED — not an XM (no physical cross-module FK) | domain-profile §6 row "FIN \| SEC \| HARD-FK" (superseded) |
| XM-CAND-002 | — | FIN | MDL | RESOLVED — see XM-FIN-001 below; the platform-summary/module-registry candidate assumed HARD-FK, P2 correctly reclassified to SOFT-READ (ADR-FIN-001) | RESOLVED | domain-profile §6 row "FIN \| MDL \| HARD-FK" (superseded) |
| XM-MDL-001 | SOFT-READ | MDL | SEC | ModuleRegistry (ENT-SEC-004) — validates a lookup type's owner module code | ACTIVE (assigned, not a candidate) | erp/modules/MDL/P2/db-script-mdl.md §2 |
| XM-FIN-001 | SOFT-READ | FIN | MDL | LookupValue — validates every FIN lookup-backed column's code (13 keys) | ACTIVE (assigned, not a candidate) | erp/modules/FIN/P2/db-script-fin.md §2; erp/decisions/FIN/ADR-FIN-001.md |
FIN's SEC dependency (identity/authorization, self-registration) is not an XM row — see
ADR-FIN-001: no physical cross-module FK exists anywhere in this pipeline.
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
| 9 | ADR-FIN-001 — FIN's SEC dependencies (identity/authorization, self-registration) are not modeled as XM rows; only XM-FIN-001 (SOFT-READ → MDL, lookup validation) is assigned, since no physical cross-module FK exists anywhere in this pipeline | ACCEPTED (non-breaking) | erp/decisions/FIN/ADR-FIN-001.md |

## OPEN QUESTION INDEX
none — `domain-profile.md` §10 records no open item.

## PIPELINE / PROGRESS STATUS
| Module | Version | Last committed stage | Last gate verdict | Delivered tracks | Tag |
|---|---|---|---|---|---|
| SEC | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| MDL | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| FIN | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
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
| 2026-09-10 | P0 | MDL | v1 | P0 completed: MDL (platform-summary, module-registry-mdl, business-policies-mdl — 6 POL) |
| 2026-09-10 | P0.5 | MDL | v1 | P0.5 completed: MDL — 5 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | MDL | v1 | P1 completed: MDL — 2 entities, 13 requirements, 13 AC, 4 rules, 2 screen requirements, 0 ADR |
| 2026-09-10 | P2 | MDL | v1 | P2 completed: MDL — 2 tables, 21 DBF, 1 XM (XM-MDL-001, SOFT-READ → SEC) |
| 2026-09-10 | P3.1 | MDL | v1 | P3.1 completed: MDL — 11 API, 15 QR, ALIGN PASSED, 0 new ADR |
| 2026-09-10 | gate:pass-1 | MDL | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 3) |
| 2026-09-10 | split | MDL | v1 | backend/exec split: 11 files, verify ok (21 checked) |
| 2026-09-10 | P0 | FIN | v1 | P0 completed: FIN (platform-summary, module-registry-fin, business-policies-fin — 20 POL, incl. all 14 §12 must-honor points) |
| 2026-09-10 | P0.5 | FIN | v1 | P0.5 completed: FIN — 19 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | FIN | v1 | P1 completed: FIN — 14 entities, 46 requirements, 46 AC, 16 rules, 12 screen requirements, 0 ADR |
| 2026-09-10 | P2 | FIN | v1 | P2 completed: FIN — 14 tables, 146 DBF, 1 XM (XM-FIN-001, SOFT-READ → MDL); ADR-FIN-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | FIN | v1 | P3.1 completed: FIN — 32 API, 44 QR, ALIGN PASSED (14-point §12 coverage confirmed), 0 new ADR |
| 2026-09-10 | gate:pass-1 | FIN | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | FIN | v1 | backend/exec split: 15 files, verify ok (45 checked) |
| 2026-09-10 | BATCH | (platform) | — | GENERATION-INSTRUCTIONS.md batch complete: SEC v1 → MDL v1 → FIN v1, all pass-1 APPROVE, in mandated dependency order |
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
# BRIEF — stage `P2` (Database) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/SEC/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: DBF, XM — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/SEC/P2/db-script-sec.md`
- `erp/modules/SEC/P2/registry-db-sec.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C5** SRS → database: C5.1 exists {'artifact': 'srs'} [CRITICAL]; C5.2 ears {'kind': 'REQ', 'patterns': 'factory.ids.ears.patterns'} [CRITICAL]; C5.3 traces {'from': 'REQ', 'to': ['US'], 'min': 1} [MAJOR]; C5.4 orphans {'kind': 'REQ', 'referenced_by': ['AC'], 'min': 1} [CRITICAL]; C5.5 traces {'from': 'AC', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.6 traces {'from': 'RULE', 'to': ['REQ'], 'min': 1} [MAJOR]; C5.7 ids-owned {'stage': 'P1'} [CRITICAL]; C5.8 registry-agree {'artifact': 'srs', 'registry': 'registry-srs', 'kinds': ['REQ', 'AC', 'ENT', 'RULE']} [MAJOR]; C5.9 no-questions {'stage': 'P1'} [CRITICAL]; C5.10 languages {'stage': 'P1'} [MAJOR]; C5.11 ids-continue {'stage': 'P1'} [CRITICAL]; C5.12 data-source {'kind': 'RULE', 'label': 'Data source', 'resolves_to': ['ENT'], 'deferral': 'DEFERRED'} [CRITICAL]
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]; C6.9 data-source {'kind': 'RULE', 'label': 'Data source', 'resolves_to': ['ENT'], 'deferral': 'DEFERRED', 'bound_in': 'db-script'} [CRITICAL]

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
Dialect       : postgresql16   (profile.stack.db.target_dialect; syntax from profile.stack.db.syntax_map)
Next          : P3.1
Module        : SEC   Version: 1
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
## DB FIELD TRACEABILITY MATRIX — SEC v1
| DBF id            | Table | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
| DBF-SEC-001 | …     | …      | …                    | ENT-SEC-001.[field] | REQ-SEC-… | NOT NULL | — |
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
SEQUENCES       : `SEQ_{TABLE}` (profile.stack.db.naming.sequence_pattern) — emitted only when
                  profile.stack.db.pk_generation is `sequence` (§4)

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
PK GENERATION — profile.stack.db.pk_generation = `sequence` (a PROFILE decision, never a
  dialect default; the same database may not carry two PK strategies)
  Strategy `sequence`: BLOCK 1 carries ONE sequence per table, named by
  `SEQ_{TABLE}`, emitted with the postgresql16 syntax from
  profile.stack.db.syntax_map.sequence:
      CREATE SEQUENCE {name} START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
  ({name} = the sequence name; use this row verbatim — never another dialect's spelling
  of the cache/cycle clauses.)
  The PK column is declared as a plain `BIGINT NOT NULL` column (syntax_map.pk) and
  carries NO identity clause and NO sequence DEFAULT. The application populates the key
  from the named sequence; the sequence name is carried into the plan by P3.1.
  `GENERATED ALWAYS AS IDENTITY` must not appear anywhere in the script.
  NEVER a trigger for PK population; NEVER a default that calls a sequence on the PK column.
```

### 4.1 Datatype governance (profile.stack.db.syntax_map → postgresql16)

| Logical type (SRS) | postgresql16 syntax |
|---|---|
| pk | BIGINT |
| string | VARCHAR(n) |
| boolean | BOOLEAN |
| timestamp | TIMESTAMPTZ |
| decimal | NUMERIC(p,s) |
| text | TEXT |

(`identity` and `sequence` are not column types — they are the PK-generation clauses of
§4, selected by `profile.stack.db.pk_generation`, and are listed there only.)

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
## XM REGISTER — SEC v1
| XM id            | Type      | This table | Column / access | Target table | Target module | Traces (REQ) | Status |
| XM-SEC-001 | HARD-FK   | …          | [FK column]     | …            | [code]        | REQ-SEC-… | READY / DEFERRED |
| XM-SEC-002 | SOFT-READ | (application) | [join / read pattern] | … | [code]  | REQ-SEC-… | ACTIVE / CONDITIONAL |

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
  -- DEFERRED FK — XM-SEC-[n]
  -- Target module : [code]   Apply when : target script gated and deployed
  -- Unblock       : [condition, extended by P3.1]
  -- ALTER TABLE [table] ADD CONSTRAINT FK_[local]_[ref] FOREIGN KEY ([col]) REFERENCES [target] ([pk]);
```

### 5.2 SOFT-READ handling

```
For every SOFT-READ XM: register it (Type SOFT-READ); add a commentary block:
  -- XM-SEC-[n] SOFT-READ — this module's [service/query] reads [TARGET].[COLUMN]
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
BLOCK 1   SEQUENCES — MANDATORY: one per table (§4), count == table count
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
PK STRATEGY   □ every PK follows profile.stack.db.pk_generation (`sequence`) — one
                `SEQ_{TABLE}` per table in BLOCK 1, PK columns plain `BIGINT NOT NULL`,
                and `GENERATED ALWAYS AS IDENTITY` nowhere in the script
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
ADR file : erp/decisions/SEC/ADR-{MOD}-{seq:03d}.md  (Context · Decision · Consequences · traces · status)
Details  : shared/GOVERNANCE-CORE.md
```

---

## 10 — `registry-db-{mod}.md` — registry content

```
## REGISTRY — P2 — SEC v1
Tables        : table · ENT id · kind · DBF range              (→ structural registry)
XM index      : XM id · type · from SEC · to [code] · status    (→ dependency index)
Lookups       : key · seeded values count · owner
Sequences     : last DBF · last XM
Decisions     : ADR ids (+ BLOCKED, if any)
Event         : "P2 completed: SEC v1 — [N] tables, [N] DBF, [N] XM"
Cascade       : for every registry XM row targeting SEC with status DEFERRED, note
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
- [ ] PK generation matches `profile.stack.db.pk_generation` = `sequence` for **every** table (§4) — no second strategy anywhere.
- [ ] Every RULE that maps to a constraint is present (UNIQUE / CHECK) and named per §3.
- [ ] Every DEFAULT / ADR listed under Decisions applied; no BLOCKED ADR unless the pass stopped.
- [ ] No question raised; sequences continuous.
- [ ] Profile check `ERP-3` (MINOR): every flag column ends with the flag_suffix.


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

## A5 — Business rules

### RULE-SEC-001 — منع منح شاشة دون منح الوحدة / No screen grant without its module grant
Scope      : ENT-SEC-008
Trigger    : on create (screen grant)
Statement  : The system shall prevent a screen grant for a role that does not hold the screen's module grant.
Message    : ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module"
Traces     : REQ-SEC-013
Source     : security-module-plan-en.md §4.2

### RULE-SEC-002 — منع منح إجراء دون منح الشاشة / No action grant without its screen grant
Scope      : ENT-SEC-009
Trigger    : on create (action grant)
Statement  : The system shall prevent an action grant for a role that does not hold the action's screen grant.
Message    : ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen"
Traces     : REQ-SEC-014
Source     : security-module-plan-en.md §4.2

### RULE-SEC-003 — الإلغاء المتسلسل عند سحب منح الوحدة / Cascade revoke on module-grant removal
Scope      : ENT-SEC-007
Trigger    : on delete (module grant)
Statement  : The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked.
Message    : ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" · en: "Every screen and action grant under this module for this role will be revoked"
Traces     : REQ-SEC-015
Source     : security-module-plan-en.md §4.2

### RULE-SEC-004 — رفض تسجيل شاشة لوحدة غير مسجّلة / No screen under an unregistered module
Scope      : ENT-SEC-005
Trigger    : on create (screen registration)
Statement  : The system shall reject a screen registration whose module code has no ModuleRegistry row.
Message    : ar: "الوحدة غير مسجّلة" · en: "Module is not registered"
Traces     : REQ-SEC-018
Source     : security-module-plan-en.md §4.3

### RULE-SEC-005 — منع تضارب الإجراءات لمستخدم واحد / Prevent conflicting actions on one user
Scope      : ENT-SEC-003, ENT-SEC-009
Trigger    : on create (role assignment or action grant)
Statement  : The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair.
Message    : ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"
Traces     : REQ-SEC-020
Source     : security-module-plan-en.md §4.4; general-accounting-system-plan-en.md §8.2

### RULE-SEC-006 — رفض رمز إعادة تعيين منتهٍ أو مُستخدَم / Reject expired or used reset token
Scope      : ENT-SEC-012
Trigger    : on submit (password reset completion)
Statement  : The system shall reject a password-reset submission whose token is expired or already used.
Message    : ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired"
Traces     : REQ-SEC-008
Source     : security-module-plan-en.md §3

### RULE-SEC-007 — اشتراط VIEW كبوابة على مستوى الشاشة / VIEW as the screen-level gateway action
Scope      : ENT-SEC-009
Trigger    : on evaluate (any action check) and on create (action grant, informational)
Statement  : The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it.
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
| search users | GET | /api/v1/sec/users | filters, paging | Page\<User\> | — | REQ-SEC-009 |
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
| search roles | GET | /api/v1/sec/roles | filters, paging | Page\<Role\> | — | REQ-SEC-012 |
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
| search registry | GET | /api/v1/sec/registry | filters, paging | Page\<registry rows\> | — | REQ-SEC-016 |

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
| search audit log | GET | /api/v1/sec/audit-log | filters, paging | Page\<AuditLogEntry\> | — | REQ-SEC-025 |
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
| list active sessions | GET | /api/v1/sec/sessions | filters, paging | Page\<ActiveSession\> | — | REQ-SEC-027 |
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
| US-SEC-007 | REQ-SEC-020 | AC-SEC-020 | RULE-SEC-005 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | SCR-REQ-SEC-005 |
| US-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 | AC-SEC-021, AC-SEC-032, AC-SEC-033 | — | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | SCR-REQ-SEC-010 |
| US-SEC-009 | REQ-SEC-022, REQ-SEC-023 | AC-SEC-022, AC-SEC-023 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-007 |
| US-SEC-010 | REQ-SEC-024, REQ-SEC-025, REQ-SEC-026 | AC-SEC-024…026 | — | ENT-SEC-011 | SCR-REQ-SEC-008 |
| US-SEC-011 | REQ-SEC-027, REQ-SEC-028 | AC-SEC-027, AC-SEC-028 | — | ENT-SEC-010 | SCR-REQ-SEC-009 |
| US-SEC-012 | REQ-SEC-029 | AC-SEC-029 | — | ENT-SEC-012 | SCR-REQ-SEC-003 |

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
# BRIEF — stage `P3.1` (Backend Execution Plan) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer ['claude:opus'] · effort high · round 1

## Rules that bind this run
- Questions: **forbidden**. A `[QUESTION]` block is refused. Ambiguity → ADR in `erp/decisions/SEC/` (`ADR-{MOD}-{seq:03d}.md`): non-breaking → continue; breaking → status BLOCKED and stop.
- Owns IDs: API, QR — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/modules/SEC/P3_1/backend-execution-plan-sec.md`
- `erp/modules/SEC/P3_1/registry-exec-be-sec.md` (registry)
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Contracts checked by `gov.py analyze` after this stage
- **C6** SRS + database → backend execution plan: C6.1 exists {'artifact': 'db-script'} [CRITICAL]; C6.2 traces {'from': 'DBF', 'to': ['REQ', 'ENT'], 'min': 1} [MAJOR]; C6.3 traces {'from': 'XM', 'to': ['REQ'], 'min': 1} [MAJOR]; C6.4 ids-owned {'stage': 'P2'} [CRITICAL]; C6.5 registry-agree {'artifact': 'db-script', 'registry': 'registry-db', 'kinds': ['DBF', 'XM']} [MAJOR]; C6.6 orphans {'kind': 'ENT', 'referenced_by': ['DBF'], 'min': 1} [MAJOR]; C6.7 no-questions {'stage': 'P2'} [CRITICAL]; C6.8 ids-continue {'stage': 'P2'} [CRITICAL]; C6.9 data-source {'kind': 'RULE', 'label': 'Data source', 'resolves_to': ['ENT'], 'deferral': 'DEFERRED', 'bound_in': 'db-script'} [CRITICAL]
- **C7** backend execution plan → split / deliver: C7.1 markers {'artifact': 'backend-execution-plan', 'track': 'backend', 'plan': 'exec'} [CRITICAL]; C7.2 traces {'from': 'backend-execution-plan', 'blocks': ['PHASE', 'SUB', 'API', 'XM'], 'min': 1} [MAJOR]; C7.3 traces {'from': 'API', 'to': ['REQ', 'DBF'], 'min': 1} [MAJOR]; C7.4 registry-agree {'artifact': 'backend-execution-plan', 'registry': 'registry-exec-be', 'kinds': ['API', 'QR']} [MAJOR]; C7.5 registry-agree {'artifact': 'backend-execution-plan', 'registry': 'registry-db', 'kinds': ['XM']} [MAJOR]; C7.6 orphans {'kind': 'REQ', 'referenced_by': ['API', 'DBF'], 'min': 1} [MAJOR]; C7.7 ids-owned {'stage': 'P3.1'} [CRITICAL]; C7.8 no-questions {'stage': 'P3.1'} [CRITICAL]; C7.9 ids-continue {'stage': 'P3.1'} [CRITICAL]; C7.10 value-agreement {'kind': 'DBF', 'binding': 'db-script', 'against': ['backend-execution-plan']} [CRITICAL]; C7.11 code-format {'artifact': ['backend-execution-plan'], 'format': 'stack.backend.api.error_code_format'} [MAJOR]; C7.12 xref-resolve {'artifact': ['backend-execution-plan', 'registry-exec-be']} [CRITICAL]; C7.13 refs-exist {'kind': 'ADR', 'dir': 'decisions', 'file_pattern': 'adr_file'} [CRITICAL]; C7.14 paths-resolve {'files': ['manifest_file']} [CRITICAL]; C7.17 xref-surface {'artifact': ['backend-execution-plan'], 'locator': 'stack.backend.api.base_path', 'kinds': ['API']} [MAJOR]; C7.16 forward-refs {'spec': 'forward_columns', 'when': 'profile.forward_columns'} [MAJOR]; C7.15 verdict-agrees {'artifact': ['backend-execution-plan'], 'spec': 'self_check', 'when': 'profile.self_check'} [CRITICAL]

---
# ENGINE
```
ENGINE        : P3.1 — Backend Execution Plan
PASS / TRACK  : pass 1 · track backend · lane analysis · questions forbidden
MODULE        : SEC · v1 · profile erp (ERP Platform)
READS         : srs · db-script · registry-srs · registry-db   (all from _state/ — generated current state)
PRODUCES      : backend-execution-plan-sec.md · registry-exec-be-sec.md
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
    PK generation : `sequence` (profile.stack.db.pk_generation) — one named sequence per table,
                    pattern `SEQ_{TABLE}`; the PK column is a plain
                    `BIGINT` column with no identity clause
    target dialect: postgresql16 (profile.stack.db.target_dialect) — syntax rows also kept for oracle19c
```

### 2A.1 Pre-generation extraction table

Emit this table first in the run (it is not part of the plan file; it is the working set
every phase binds from):

```
PRE-GENERATION EXTRACTION — SEC v1
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      ENT-SEC-<seq> │ exact name │ kind ∈ master | transactional | lookup | config | security
REQUIREMENTS  REQ-SEC-<seq> │ EARS text  │ its AC-SEC-<seq> list (Given/When/Then)
RULES         RULE-SEC-<seq> │ scope ENT │ trigger │ statement │ message per language (ar, en) │ source
SCREENS       every screen entry the SRS declares │ type │ owning ENT │ composite (Search + Entry = ONE screen)
PERMISSIONS   the SRS permission matrix (roles × screens × actions) — actions VIEW/CREATE/UPDATE/DELETE, gateway VIEW
LOOKUPS       every lookup key the SRS names, exactly as written — rule: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs
BUSINESS CODE format per master entity — rule: document numbers come from the platform numbering engine; never generated in a module
── FROM db-script ────────────────────────────────────────────────────────
TABLES        ENT → exact table name
PK GENERATION exact object the db-script declares per table — strategy `sequence`: the sequence
              NAME as the db-script's BLOCK 1 spells it (one per table)
COLUMNS       exact column name │ DBF-SEC-<seq> │ declared type │ null │ default
CONSTRAINTS   exact FK / UNIQUE / CHECK constraint names ; INDEXES exact names
XM            XM-SEC-<seq> │ kind (HARD-FK | SOFT-READ …) │ local column │ target module.table │ status
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
| PK generation | every PK reference names the exact object from the db-script — strategy `sequence`, so the BINDINGS line of R2 carries the **sequence name** (the implementer reads it, never derives it) | "auto-generated" → the exact sequence name per table |
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
EXECUTION PLAN INDEX — SEC v1 — backend-execution-plan-sec.md
Profile: erp · dialect: postgresql16 · framework: profile.stack.backend.framework
Open ADRs: <n> — decisions/SEC/

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
DB ALIGNMENT MANIFEST — SEC v1
DBF-*            │ ENT-*          │ plan property │ plan type │ XM-* (if FK crosses modules) │ status
DBF-SEC-001 │ ENT-SEC-001 │ <property>    │ <type>    │ —                            │ ✓
DBF-SEC-007 │ ENT-SEC-001 │ <property>    │ <type>    │ XM-SEC-001 ⏸           │ ⏸
Legend  ✓ aligned · ✗ type mismatch (finding) · ⏸ deferred XM
Derived / computed properties (no DBF) are listed with DBF = "— (derived)" and an ADR id.
```

## 5. Query Reference Catalog (QR-*)

The QRC expresses the **retrieval and persistence intent** of every repository operation as
pseudo-SQL. It is a logical specification, never executable code: the implementer rewrites
every entry with the real entity classes, mapped property names and the project's query
strategy. Copy-pasting a QR entry into production code is a violation.

- Format: `QR-SEC-{seq}` (3-digit sequence, continuous across the module).
- Assigned while writing the data and service phases; every API with a DB operation cites its QR.
- Ordering / paging use the profile's envelope: `Page<T>`; responses are wrapped in `ApiResponse<T>`.

```
QR-SEC-<seq> — <operation name>
Phase        : <p.key of the phase that defines it>
API          : API-SEC-<seq> | repository-only
Entity       : ENT-SEC-<seq>
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
| `deactivate (soft)` | usage check first (can-delete / can-deactivate); blocked → catalog error; allowed → `soft` per profile.stack.db.delete_semantics — flip the active flag (suffix `Fl`); the other semantics only where the SRS mandates it |
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
- Type mapping postgresql16 → language types, stated once as a table (from `profile.stack.db.syntax_map` rows, PK column type included) — a deviation needs an ADR. The table states column types only; the PK-generation clause is not a type (§2A).
- Runtime error-code format: `{MOD}-{http}[-{SLUG}]` (profile.stack.backend.api.error_code_format) — state this string verbatim and make **every** Error Catalog row (§7) an instance of it; the declared format and the emitted codes come from this one profile value, never from free text.
- Lookup values: all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs.
- Numbering: document numbers come from the platform numbering engine; never generated in a module.
- Workflow engine: **forbidden**.
- Languages: every named entity carries a name per language (ar, en); a single-language artifact is incomplete.
- Cross-module contract placement: inversion-of-control interfaces consumed by other modules live in the service layer; a domain class may depend on another module's service interface (module boundary, not a layer violation).
If nothing module-specific applies, write "Standard configuration — no module-specific abstractions".

**R2 — Data + domain.** One entity block per `ENT-*`, every value bound (§2A):
```
### ENT-SEC-<seq> — <exact name>      kind: <master|transactional|lookup|config|security>
BINDINGS   table <exact> · PK <column, DBF> · PK generation `sequence` → sequence <exact name from the db-script BLOCK 1> · db-script version
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
<!-- API:API-SEC-<seq>:START traces=REQ-SEC-<seq>,DBF-SEC-<seq> -->
### API-SEC-<seq> — <operation>
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
<!-- API:API-SEC-<seq>:END -->
```
Completeness rules: a RULE whose SRS `Data source` is DEFERRED is **not** enforced here — it is
listed once in the entity block as `DEFERRED (no declaration surface in v1)` with no catalog
row, no QR and no enforcing endpoint, because the data its check would read has no column to read
from; every other RULE in Validations ↔ a catalog row (RULE-ERR-CARRY); infrastructure
errors (not found, forbidden, server) are catalog rows with RULE = `PLATFORM-STD` and an ADR;
repository deviations (eager fetch, compound update, native query) need an ADR. Business code
(if any) is excluded from create/update bodies and always present in responses. No hard-coded
role checks in services — permission names only.

**R4 — Contract documentation (internal).** API contract summary (API │ path │ verb │ request
DTO │ response DTO │ stability), DTO typing constraints (lookup fields are
strings holding the code, never enums; business code never in create/update), and the
pagination + filter standard (request shape, sort validation, empty result = success). This
section is a **backend self-check only** — the frontend stage binds to the real
`api-docs-sec.md` published after implementation, never to this summary.

This stage runs before any implementation exists, so `Request DTO` and `Response DTO`
name something that does not exist yet. Every cell of those columns
carries `(proposed)` unless the value is already resolvable in `api-docs` —
a guess printed beside facts is read downstream as a decision, and the implementer has no way to
tell which columns were derived and which were imagined. The stage that CAN resolve them fills
them in from the built artifact. `gov.py analyze` → `forward-refs`.


**R5 — Cross-module consume (contracts).** The plan never mints `XM-*`; it places every XM
from the db-script register:
```
<!-- XM:XM-SEC-<seq>:START traces=REQ-SEC-<seq> -->
### XM-SEC-<seq> — <dependency>
Target        : module · entity (ENT of the owner) · classification (HARD-FK | SOFT-READ | EVENT | READ-ONLY)
Interface     : DB foreign key | REST call — cite the TARGET module's own `API-<TARGET>-<seq>` id **and**
                its path (an instance of /api/v1/{module}/{resource} on the target); an endpoint that the target
                module's API registry does not define may not be named here (`gov.py analyze` →
                `xref-resolve` resolves every foreign id against that module's registry). If the
                endpoint does not exist yet, the row is DEFERRED with the unblock condition — never a
                prose promise such as "through the target's read APIs" | message
Contract      : data required · fallback if absent · retry / timeout / idempotency
Blocks        : DBF-* / API-* blocked while DEFERRED · unblock condition · deferred strategy
<!-- XM:XM-SEC-<seq>:END -->
```
Summary table first (XM │ classification │ target │ interface │ target `API-*` (REST rows) │ status). Inbound
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
ERROR CATALOG — SEC v1
code (runtime value per envelope) │ RULE-* (or PLATFORM-STD + ADR) │ API-* │ HTTP │ trigger │ message-AR │ message-EN
```
- Every RULE that produces a user-facing message has a row; message text is copied
  character-perfect from the SRS in every language (ar, en); a missing
  language → `PENDING ADR-<id>`, never invented.
- Downstream consumers (frontend plan, test-gen, api-verify) cite the **code**; they never
  reproduce message text.
- The runtime code format (as the framework serialises it) is stated once in R1 as
  `{MOD}-{http}[-{SLUG}]` so that api-verify can assert on it. The declaration and the rows are the
  same fact: a code that is not an instance of the declared format, or a declared format
  no row obeys, is a finding (`gov.py analyze` → `code-format`), never a convention to
  inherit into the next module.

## 8. Security

Covered by R7 (§6.1) — permission names follow
`PERM_<PAGE_CODE>_<ACTION>`, minted only from the SRS permission matrix;
a permission name that appears in the plan but not in the matrix is a finding.
Review check: `profile.review.extra_checks` rows whose stage is `P3.1`:
- `ERP-4` (MAJOR): every mutation endpoint declares its PERM_* requirement

## 9. Alignment self-check (ALIGN)

Validates the plan **against itself and its bindings**. Runs automatically after the last
content phase; a ✗ is fixed in the plan before the run ends (the fix is an ADR if it was a
choice).

ALIGN is the *prose* half of the check and it is not the authority. The mechanical half is
`gov.py analyze`, which resolves — across artifacts, and across modules — exactly the things
a prose pass reads past:

| Mechanical check | What it resolves |
|---|---|
| `value-agreement` | the physical column a `DBF-*` names in this plan is character-identical to the one the db-script declares for it (registry row, `CREATE TABLE`, `COMMENT ON`) |
| `code-format` | every Error Catalog code is an instance of the format R1 declares (`{MOD}-{http}[-{SLUG}]`) |
| `data-source` | every `RULE-*` this plan turns into a runtime check has a declared source for the data the check *reads* — or an explicit deferral |
| `xref-resolve` | every id of another module cited here is defined in that module's own registry |
| `refs-exist` | every `ADR-*` file this plan cites by path exists on disk in `erp/decisions/SEC/` |
| `paths-resolve` | every path the generated manifest and execution state emit resolves to something that exists |
| `verdict-agrees` | the `RESULT` line does not claim fewer findings than `analyze` produced for this plan |

Write ALIGN so that a ✗ is **stated**, with the fix that was applied. When a row below
cannot be confirmed from the inputs, that row is a finding, not a silent ✓.

**Do not author the `RESULT` line.** Write the label and leave the rest as it
stands; the orchestrator overwrites it from the analyze report after the stage completes
(`gov.py` → `_stamp_verdict`), and `verdict-agrees` refuses any hand-written verdict that
claims fewer findings than the machine produced. A self-check that always prints
`PASSED ✓` transfers false confidence downstream and is worse than no self-check —
so the count is no longer a thing a model is asked to be honest about.

```
ALIGN — SEC v1
TRACEABILITY      every API-*/QR-*/RULE-*/DBF-* used in a phase appears in the Plan Index │ every block carries traces= │ every traces target exists upstream
BINDING (§2A)     no placeholder table/column/key/generation object │ no "see SRS" │ every column cites a DBF AND spells the same column string the db-script declares for it │ PK generation named per `sequence` │ every message present in ar + en │ business code format explicit
MANIFEST (§4)     only the manifest's columns │ every DBF of every bound table listed │ ⏸ rows have an XM
QRC (§5)          every API with a DB operation has a QR │ every QR carries the agent-reference warning │ no join for lookup labels │ exact generation object named
API (R3)          every RULE in Validations has a catalog row │ every catalog code is an instance of the format R1 declares │ platform errors have RULE = PLATFORM-STD + ADR │ create/update exclude system fields │ business code in responses
RULE INPUTS       every RULE enforced at runtime names where the data it READS comes from (an ENT/DBF, or an explicit deferral) — a rule whose input has no declaration surface is DEFERRED, never silently emitted
CROSS-MODULE      every XM from the db-script placed exactly once │ every DEFERRED has strategy + unblock │ inbound stubs use XM-INBOUND-STUB │ every REST row names a target-module `API-*` id that the target module's registry actually defines
SECURITY (R7)     every API serving a screen declares its permission │ every screen has a seed row in SEC_PAGES │ no permission outside the matrix
CORE (R1)         layers declared │ domain placement declared │ error signalling declared │ type mapping declared
DECISIONS         every non-obvious inference is an ADR in decisions/SEC/ │ no BLOCKED ADR left unsurfaced
RESULT            (written by the orchestrator from the analyze report — leave it alone)
```
Coverage tables (ENT/DBF → phases → QR → XM; RULE → API → catalog code; XM → status → blocks
→ workaround) close the section.

## 10. Registry update — `registry-exec-be-sec.md`

Written in the same run, after ALIGN (categories: shared/REGISTRY-SCHEMA.md):

```
REGISTRY — P3.1 — SEC v1
ID RANGES        API-SEC-<first>..<last> · QR-SEC-<first>..<last>
ENTITIES / TABLES bound   · lookups reused / new (keys)
XM STATUS        open / deferred list
CATALOG          code count · rules without message → ADR ids
ALIGN            verdict as stamped · findings fixed
ADRs             decisions/SEC/ADR-SEC-<seq> … (status)
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
gov.py split --track backend --module SEC --version 1 --dry-run
```
`gov.py analyze` (traceability matrix, EARS, marker validity, registry ↔ artifact agreement)
runs before the gate `gate:pass-1`; CRITICAL findings keep the gate closed.

## 12. Ambiguity rule (no questions here)

`factory.yaml → ambiguity`, stated once in shared/GOVERNANCE-CORE.md:
- **non-breaking** (does not contradict a locked decision or a REQ) → choose the best-practice
  answer using `profile.knowledge.files` + `erp/` steering, write
  `erp/decisions/SEC/ADR-SEC-{seq:03d}.md` (Context / Decision /
  Consequences / traces) and **continue**;
- **breaking** (contradicts a locked decision or a REQ) → ADR with status
  `BLOCKED`, then **stop**; the
  orchestrator surfaces it at the next human point.
Every "STOP and ask" of earlier engine generations is replaced by this rule.

## 13. Boundaries and hand-off

| Owns (mints) | References (read-only) | Never touches |
|---|---|---|
| `API-*`, `QR-*`; DB Alignment Manifest; Error Catalog; QRC; ALIGN rows; ADRs it raises | `POL-*` (P0), `US-*` (P0.5), `REQ-*` (P1), `AC-*` (P1), `ENT-*` (P1), `RULE-*` (P1), `DBF-*` (P2), `XM-*` (P2), `SCR-REQ-*` (P1) | frontend/UX atoms (`UXD`, `SCR` — P3.2), `TC-*` (test-gen), any code, framework annotations, executable queries, test artifacts |

Hand-off (the orchestrator prints it): the plan + registry are split by the toolkit into
`packages/backend-execution/` and delivered on
`gov/{mod}-v{version}-{track}` after the `gate:pass-1` verdict. The implementer reads
the plan in order (index → manifest → ADRs → phases in profile order → QRC → catalog), rewrites
every QR, implements security per R7, and publishes the api-docs file the frontend stage
requires (`factory.passes.2.required_inputs`).


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

## A5 — Business rules

### RULE-SEC-001 — منع منح شاشة دون منح الوحدة / No screen grant without its module grant
Scope      : ENT-SEC-008
Trigger    : on create (screen grant)
Statement  : The system shall prevent a screen grant for a role that does not hold the screen's module grant.
Message    : ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module"
Traces     : REQ-SEC-013
Source     : security-module-plan-en.md §4.2

### RULE-SEC-002 — منع منح إجراء دون منح الشاشة / No action grant without its screen grant
Scope      : ENT-SEC-009
Trigger    : on create (action grant)
Statement  : The system shall prevent an action grant for a role that does not hold the action's screen grant.
Message    : ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen"
Traces     : REQ-SEC-014
Source     : security-module-plan-en.md §4.2

### RULE-SEC-003 — الإلغاء المتسلسل عند سحب منح الوحدة / Cascade revoke on module-grant removal
Scope      : ENT-SEC-007
Trigger    : on delete (module grant)
Statement  : The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked.
Message    : ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" · en: "Every screen and action grant under this module for this role will be revoked"
Traces     : REQ-SEC-015
Source     : security-module-plan-en.md §4.2

### RULE-SEC-004 — رفض تسجيل شاشة لوحدة غير مسجّلة / No screen under an unregistered module
Scope      : ENT-SEC-005
Trigger    : on create (screen registration)
Statement  : The system shall reject a screen registration whose module code has no ModuleRegistry row.
Message    : ar: "الوحدة غير مسجّلة" · en: "Module is not registered"
Traces     : REQ-SEC-018
Source     : security-module-plan-en.md §4.3

### RULE-SEC-005 — منع تضارب الإجراءات لمستخدم واحد / Prevent conflicting actions on one user
Scope      : ENT-SEC-003, ENT-SEC-009
Trigger    : on create (role assignment or action grant)
Statement  : The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair.
Message    : ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"
Traces     : REQ-SEC-020
Source     : security-module-plan-en.md §4.4; general-accounting-system-plan-en.md §8.2

### RULE-SEC-006 — رفض رمز إعادة تعيين منتهٍ أو مُستخدَم / Reject expired or used reset token
Scope      : ENT-SEC-012
Trigger    : on submit (password reset completion)
Statement  : The system shall reject a password-reset submission whose token is expired or already used.
Message    : ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired"
Traces     : REQ-SEC-008
Source     : security-module-plan-en.md §3

### RULE-SEC-007 — اشتراط VIEW كبوابة على مستوى الشاشة / VIEW as the screen-level gateway action
Scope      : ENT-SEC-009
Trigger    : on evaluate (any action check) and on create (action grant, informational)
Statement  : The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it.
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
| search users | GET | /api/v1/sec/users | filters, paging | Page\<User\> | — | REQ-SEC-009 |
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
| search roles | GET | /api/v1/sec/roles | filters, paging | Page\<Role\> | — | REQ-SEC-012 |
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
| search registry | GET | /api/v1/sec/registry | filters, paging | Page\<registry rows\> | — | REQ-SEC-016 |

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
| search audit log | GET | /api/v1/sec/audit-log | filters, paging | Page\<AuditLogEntry\> | — | REQ-SEC-025 |
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
| list active sessions | GET | /api/v1/sec/sessions | filters, paging | Page\<ActiveSession\> | — | REQ-SEC-027 |
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
| US-SEC-007 | REQ-SEC-020 | AC-SEC-020 | RULE-SEC-005 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | SCR-REQ-SEC-005 |
| US-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 | AC-SEC-021, AC-SEC-032, AC-SEC-033 | — | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | SCR-REQ-SEC-010 |
| US-SEC-009 | REQ-SEC-022, REQ-SEC-023 | AC-SEC-022, AC-SEC-023 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-007 |
| US-SEC-010 | REQ-SEC-024, REQ-SEC-025, REQ-SEC-026 | AC-SEC-024…026 | — | ENT-SEC-011 | SCR-REQ-SEC-008 |
| US-SEC-011 | REQ-SEC-027, REQ-SEC-028 | AC-SEC-027, AC-SEC-028 | — | ENT-SEC-010 | SCR-REQ-SEC-009 |
| US-SEC-012 | REQ-SEC-029 | AC-SEC-029 | — | ENT-SEC-012 | SCR-REQ-SEC-003 |

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

<<<INPUT: db-script>>>
# DATABASE — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Dialect : postgresql16   Schema prefix : none
Identifier transformation : SRS logical field name (camelCase) → physical column
  name (snake_case) — e.g. `userPk` → `user_pk`, `nameAr` → `name_ar`. Applied to
  every identifier in this script; no other spelling of any field exists.
Date : 2026-09-10
Counts : 13 tables · 104 DBF · 0 XM (SEC is ROOT) · 2 shared infrastructure tables (see ADR-SEC-001)
══════════════════════════════════════════════════════════════════

## 1. DB FIELD TRACEABILITY MATRIX — SEC v1

### Table SEC_USER (ENT-SEC-001)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-001 | user_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-001 (PK, profile.stack.db.naming.pk_pattern) | REQ-SEC-009 | NOT NULL | identity |
| DBF-SEC-002 | username | VARCHAR(100) | ENT-SEC-001.username | REQ-SEC-001, REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-003 | email | VARCHAR(255) | ENT-SEC-001.email | REQ-SEC-006, REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-004 | password_hash | TEXT | ENT-SEC-001.passwordHash | REQ-SEC-001, REQ-SEC-007 | NOT NULL | — |
| DBF-SEC-005 | full_name_ar | VARCHAR(200) | ENT-SEC-001.fullNameAr | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-006 | full_name_en | VARCHAR(200) | ENT-SEC-001.fullNameEn | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-007 | status_code | VARCHAR(20) | ENT-SEC-001.statusCode (A6 lookup USER_STATUS) | REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031 | NOT NULL | 'ACTIVE' |
| DBF-SEC-008 | last_login_at | TIMESTAMPTZ | ENT-SEC-001.lastLoginAt | REQ-SEC-001 | NULL | — |
| DBF-SEC-009 | is_active_fl | BOOLEAN | ENT-SEC-001.isActiveFl | REQ-SEC-011, REQ-SEC-031 | NOT NULL | TRUE |
| DBF-SEC-010 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-011 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | now() |
| DBF-SEC-012 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — |
| DBF-SEC-013 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — |

### Table SEC_ROLE (ENT-SEC-002)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-014 | role_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-002 (PK) | REQ-SEC-012 | NOT NULL | identity |
| DBF-SEC-015 | code | VARCHAR(50) | ENT-SEC-002.code | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-016 | name_ar | VARCHAR(150) | ENT-SEC-002.nameAr | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-017 | name_en | VARCHAR(150) | ENT-SEC-002.nameEn | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-018 | description_ar | VARCHAR(500) | ENT-SEC-002.descriptionAr | REQ-SEC-012 | NULL | — |
| DBF-SEC-019 | description_en | VARCHAR(500) | ENT-SEC-002.descriptionEn | REQ-SEC-012 | NULL | — |
| DBF-SEC-020 | is_active_fl | BOOLEAN | ENT-SEC-002.isActiveFl | REQ-SEC-012 | NOT NULL | TRUE |
| DBF-SEC-021 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-022 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | now() |
| DBF-SEC-023 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — |
| DBF-SEC-024 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — |

### Table SEC_USER_ROLE (ENT-SEC-003 UserRoleAssignment)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-025 | user_role_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-003 (PK) | REQ-SEC-010 | NOT NULL | identity |
| DBF-SEC-026 | user_id | NUMERIC | ENT-SEC-003.userId → FK ENT-SEC-001 | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-027 | role_id | NUMERIC | ENT-SEC-003.roleId → FK ENT-SEC-002 | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-028 | assigned_by | VARCHAR(100) | ENT-SEC-003.assignedBy | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-029 | assigned_at | TIMESTAMPTZ | ENT-SEC-003.assignedAt | REQ-SEC-010 | NOT NULL | now() |

### Table SEC_MODULE_REG (ENT-SEC-004 ModuleRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-030 | module_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-004 (PK) | REQ-SEC-016 | NOT NULL | identity |
| DBF-SEC-031 | code | VARCHAR(10) | ENT-SEC-004.code | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-032 | name_ar | VARCHAR(150) | ENT-SEC-004.nameAr | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-033 | name_en | VARCHAR(150) | ENT-SEC-004.nameEn | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-034 | is_active_fl | BOOLEAN | ENT-SEC-004.isActiveFl | REQ-SEC-016 | NOT NULL | TRUE |
| DBF-SEC-035 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-036 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | now() |
| DBF-SEC-037 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — |
| DBF-SEC-038 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — |

### Table SEC_SCREEN_REG (ENT-SEC-005 ScreenRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-039 | screen_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-005 (PK) | REQ-SEC-017 | NOT NULL | identity |
| DBF-SEC-040 | page_code | VARCHAR(50) | ENT-SEC-005.pageCode | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-041 | module_id | NUMERIC | ENT-SEC-005.moduleId → FK ENT-SEC-004 [RULE-SEC-004] | REQ-SEC-017, REQ-SEC-018 | NOT NULL | — |
| DBF-SEC-042 | name_ar | VARCHAR(150) | ENT-SEC-005.nameAr | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-043 | name_en | VARCHAR(150) | ENT-SEC-005.nameEn | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-044 | is_active_fl | BOOLEAN | ENT-SEC-005.isActiveFl | REQ-SEC-017 | NOT NULL | TRUE |
| DBF-SEC-045 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-046 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | now() |
| DBF-SEC-047 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — |
| DBF-SEC-048 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — |

### Table SEC_ACTION_REG (ENT-SEC-006 ActionRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-049 | action_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-006 (PK) | REQ-SEC-019 | NOT NULL | identity |
| DBF-SEC-050 | permission_code | VARCHAR(100) | ENT-SEC-006.permissionCode | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-051 | screen_id | NUMERIC | ENT-SEC-006.screenId → FK ENT-SEC-005 | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-052 | action_code | VARCHAR(40) | ENT-SEC-006.actionCode | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-053 | name_ar | VARCHAR(150) | ENT-SEC-006.nameAr | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-054 | name_en | VARCHAR(150) | ENT-SEC-006.nameEn | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-055 | is_active_fl | BOOLEAN | ENT-SEC-006.isActiveFl | REQ-SEC-019 | NOT NULL | TRUE |
| DBF-SEC-056 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-057 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | now() |
| DBF-SEC-058 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — |
| DBF-SEC-059 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — |

### Table SEC_ROLE_MODULE_GRANT (ENT-SEC-007)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-060 | role_module_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-007 (PK) | REQ-SEC-012 | NOT NULL | identity |
| DBF-SEC-061 | role_id | NUMERIC | ENT-SEC-007.roleId → FK ENT-SEC-002 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-062 | module_id | NUMERIC | ENT-SEC-007.moduleId → FK ENT-SEC-004 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-063 | granted_by | VARCHAR(100) | ENT-SEC-007.grantedBy | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-064 | granted_at | TIMESTAMPTZ | ENT-SEC-007.grantedAt | REQ-SEC-012 | NOT NULL | now() |

### Table SEC_ROLE_SCREEN_GRANT (ENT-SEC-008)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-065 | role_screen_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-008 (PK) | REQ-SEC-013 | NOT NULL | identity |
| DBF-SEC-066 | role_id | NUMERIC | ENT-SEC-008.roleId → FK ENT-SEC-002 [RULE-SEC-001, app-layer] | REQ-SEC-013, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-067 | screen_id | NUMERIC | ENT-SEC-008.screenId → FK ENT-SEC-005 | REQ-SEC-013 | NOT NULL | — |
| DBF-SEC-068 | granted_by | VARCHAR(100) | ENT-SEC-008.grantedBy | REQ-SEC-013 | NOT NULL | — |
| DBF-SEC-069 | granted_at | TIMESTAMPTZ | ENT-SEC-008.grantedAt | REQ-SEC-013 | NOT NULL | now() |

### Table SEC_ROLE_ACTION_GRANT (ENT-SEC-009)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-070 | role_action_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-009 (PK) | REQ-SEC-014 | NOT NULL | identity |
| DBF-SEC-071 | role_id | NUMERIC | ENT-SEC-009.roleId → FK ENT-SEC-002 [RULE-SEC-002, RULE-SEC-005, RULE-SEC-007 — app-layer] | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030 | NOT NULL | — |
| DBF-SEC-072 | action_id | NUMERIC | ENT-SEC-009.actionId → FK ENT-SEC-006 | REQ-SEC-014 | NOT NULL | — |
| DBF-SEC-073 | granted_by | VARCHAR(100) | ENT-SEC-009.grantedBy | REQ-SEC-014 | NOT NULL | — |
| DBF-SEC-074 | granted_at | TIMESTAMPTZ | ENT-SEC-009.grantedAt | REQ-SEC-014 | NOT NULL | now() |

### Table SEC_ACTIVE_SESSION (ENT-SEC-010)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-075 | active_session_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-010 (PK) | REQ-SEC-001 | NOT NULL | identity |
| DBF-SEC-076 | user_id | NUMERIC | ENT-SEC-010.userId → FK ENT-SEC-001 | REQ-SEC-001, REQ-SEC-011, REQ-SEC-027 | NOT NULL | — |
| DBF-SEC-077 | token_ref | VARCHAR(200) | ENT-SEC-010.tokenRef | REQ-SEC-001 | NOT NULL | — |
| DBF-SEC-078 | started_at | TIMESTAMPTZ | ENT-SEC-010.startedAt | REQ-SEC-001 | NOT NULL | now() |
| DBF-SEC-079 | last_activity_at | TIMESTAMPTZ | ENT-SEC-010.lastActivityAt | REQ-SEC-027 | NOT NULL | now() |
| DBF-SEC-080 | ip_address | VARCHAR(64) | ENT-SEC-010.ipAddress | REQ-SEC-027 | NULL | — |
| DBF-SEC-081 | terminated_at | TIMESTAMPTZ | ENT-SEC-010.terminatedAt | REQ-SEC-011, REQ-SEC-028 | NULL | — |
| DBF-SEC-082 | terminated_by | VARCHAR(100) | ENT-SEC-010.terminatedBy | REQ-SEC-011, REQ-SEC-028 | NULL | — |

### Table SEC_AUDIT_LOG (ENT-SEC-011 AuditLogEntry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-083 | audit_log_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-011 (PK) | REQ-SEC-024 | NOT NULL | identity |
| DBF-SEC-084 | event_type_code | VARCHAR(40) | ENT-SEC-011.eventTypeCode (A6 lookup AUDIT_EVENT_TYPE) | REQ-SEC-024 | NOT NULL | — |
| DBF-SEC-085 | actor_user_id | NUMERIC | ENT-SEC-011.actorUserId → FK ENT-SEC-001 (nullable) | REQ-SEC-002, REQ-SEC-024 | NULL | — |
| DBF-SEC-086 | occurred_at | TIMESTAMPTZ | ENT-SEC-011.occurredAt | REQ-SEC-024 | NOT NULL | now() |
| DBF-SEC-087 | target_ref | VARCHAR(200) | ENT-SEC-011.targetRef | REQ-SEC-024 | NULL | — |
| DBF-SEC-088 | details_ar | TEXT | ENT-SEC-011.detailsAr | REQ-SEC-024 | NULL | — |
| DBF-SEC-089 | details_en | TEXT | ENT-SEC-011.detailsEn | REQ-SEC-024 | NULL | — |
| DBF-SEC-090 | ip_address | VARCHAR(64) | ENT-SEC-011.ipAddress | REQ-SEC-024 | NULL | — |

### Table SEC_PWD_RESET_TOKEN (ENT-SEC-012 PasswordResetToken)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-091 | pwd_reset_token_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-012 (PK) | REQ-SEC-006 | NOT NULL | identity |
| DBF-SEC-092 | user_id | NUMERIC | ENT-SEC-012.userId → FK ENT-SEC-001 | REQ-SEC-006 | NOT NULL | — |
| DBF-SEC-093 | token_hash | TEXT | ENT-SEC-012.tokenHash | REQ-SEC-006, REQ-SEC-007 | NOT NULL | — |
| DBF-SEC-094 | requested_at | TIMESTAMPTZ | ENT-SEC-012.requestedAt | REQ-SEC-006 | NOT NULL | now() |
| DBF-SEC-095 | expires_at | TIMESTAMPTZ | ENT-SEC-012.expiresAt (A7 DEFAULT: requestedAt + 30 min) | REQ-SEC-006, REQ-SEC-008 | NOT NULL | — |
| DBF-SEC-096 | used_at | TIMESTAMPTZ | ENT-SEC-012.usedAt [RULE-SEC-006, app-layer] | REQ-SEC-007, REQ-SEC-008 | NULL | — |

### Table SEC_SIGNUP_REQUEST (ENT-SEC-013)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-097 | signup_request_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-013 (PK) | REQ-SEC-003 | NOT NULL | identity |
| DBF-SEC-098 | email | VARCHAR(255) | ENT-SEC-013.email | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-099 | full_name_ar | VARCHAR(200) | ENT-SEC-013.fullNameAr | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-100 | full_name_en | VARCHAR(200) | ENT-SEC-013.fullNameEn | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-101 | submitted_at | TIMESTAMPTZ | ENT-SEC-013.submittedAt | REQ-SEC-003 | NOT NULL | now() |
| DBF-SEC-102 | status_code | VARCHAR(20) | ENT-SEC-013.statusCode (A6 lookup SIGNUP_STATUS) | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005 | NOT NULL | 'PENDING' |
| DBF-SEC-103 | reviewed_by | VARCHAR(100) | ENT-SEC-013.reviewedBy | REQ-SEC-004, REQ-SEC-005 | NULL | — |
| DBF-SEC-104 | reviewed_at | TIMESTAMPTZ | ENT-SEC-013.reviewedAt | REQ-SEC-004, REQ-SEC-005 | NULL | — |

Total: 104 DBF ids across 13 tables (plus 2 shared-infrastructure tables under ADR-SEC-001, not SEC ENT-traced — see §2 and §6).

## 2. XM REGISTER — SEC v1

None — SEC is ROOT; SRS A8 lists no consumed entity owned by another module (the
Notifications integration is an external service, not a registered platform module,
and carries no FK — see SRS A8 second table).

## 3. FULL_DATABASE_SCRIPT

```sql
-- ════════════════════════════════════════════════════════════════
-- SEC v1 — Security module — PostgreSQL 16
-- Identifier transformation: camelCase (SRS) -> snake_case (DB)
-- ════════════════════════════════════════════════════════════════

-- BLOCK 1 — SEQUENCES
-- none: every PK uses GENERATED ALWAYS AS IDENTITY (postgresql16 syntax_map)

-- BLOCK 2 — PARENT TABLES (no FK dependencies)

-- Shared platform lookup infrastructure (see ADR-SEC-001): NOT created here in v1.
-- SEC's own lookup-backed columns (status_code, event_type_code) are CHECK-constrained
-- in this version; centralization into a shared MDL_LOOKUP_TYPE/MDL_LOOKUP_VALUE pair
-- is deferred to when the MDL module's own P2 stage runs (ADR-SEC-001, non-breaking).

CREATE TABLE SEC_USER (
  user_pk        BIGINT GENERATED ALWAYS AS IDENTITY,
  username       VARCHAR(100)  NOT NULL,
  email          VARCHAR(255)  NOT NULL,
  password_hash  TEXT          NOT NULL,
  full_name_ar   VARCHAR(200)  NOT NULL,
  full_name_en   VARCHAR(200)  NOT NULL,
  status_code    VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  last_login_at  TIMESTAMPTZ,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_ROLE (
  role_pk          BIGINT GENERATED ALWAYS AS IDENTITY,
  code             VARCHAR(50)   NOT NULL,
  name_ar          VARCHAR(150)  NOT NULL,
  name_en          VARCHAR(150)  NOT NULL,
  description_ar   VARCHAR(500),
  description_en   VARCHAR(500),
  is_active_fl     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by       VARCHAR(100)  NOT NULL,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by       VARCHAR(100),
  updated_at       TIMESTAMPTZ
);

CREATE TABLE SEC_MODULE_REG (
  module_reg_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  code           VARCHAR(10)   NOT NULL,
  name_ar        VARCHAR(150)  NOT NULL,
  name_en        VARCHAR(150)  NOT NULL,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_SIGNUP_REQUEST (
  signup_request_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  email              VARCHAR(255)  NOT NULL,
  full_name_ar       VARCHAR(200)  NOT NULL,
  full_name_en       VARCHAR(200)  NOT NULL,
  submitted_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  status_code        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  reviewed_by        VARCHAR(100),
  reviewed_at        TIMESTAMPTZ
);

-- BLOCK 3 — CHILD TABLES (parents already created above; chain respected)

CREATE TABLE SEC_USER_ROLE (
  user_role_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id       BIGINT        NOT NULL,
  role_id       BIGINT        NOT NULL,
  assigned_by   VARCHAR(100)  NOT NULL,
  assigned_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_SCREEN_REG (
  screen_reg_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  page_code      VARCHAR(50)   NOT NULL,
  module_id      BIGINT        NOT NULL,
  name_ar        VARCHAR(150)  NOT NULL,
  name_en        VARCHAR(150)  NOT NULL,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_ACTION_REG (
  action_reg_pk    BIGINT GENERATED ALWAYS AS IDENTITY,
  permission_code  VARCHAR(100)  NOT NULL,
  screen_id        BIGINT        NOT NULL,
  action_code      VARCHAR(40)   NOT NULL,
  name_ar          VARCHAR(150)  NOT NULL,
  name_en          VARCHAR(150)  NOT NULL,
  is_active_fl     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by       VARCHAR(100)  NOT NULL,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by       VARCHAR(100),
  updated_at       TIMESTAMPTZ
);

CREATE TABLE SEC_ROLE_MODULE_GRANT (
  role_module_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  module_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ROLE_SCREEN_GRANT (
  role_screen_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  screen_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ROLE_ACTION_GRANT (
  role_action_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  action_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ACTIVE_SESSION (
  active_session_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id            BIGINT        NOT NULL,
  token_ref          VARCHAR(200)  NOT NULL,
  started_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  last_activity_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  ip_address         VARCHAR(64),
  terminated_at      TIMESTAMPTZ,
  terminated_by      VARCHAR(100)
);

CREATE TABLE SEC_AUDIT_LOG (
  audit_log_pk      BIGINT GENERATED ALWAYS AS IDENTITY,
  event_type_code   VARCHAR(40)   NOT NULL,
  actor_user_id     BIGINT,
  occurred_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  target_ref        VARCHAR(200),
  details_ar        TEXT,
  details_en        TEXT,
  ip_address        VARCHAR(64)
);

CREATE TABLE SEC_PWD_RESET_TOKEN (
  pwd_reset_token_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id             BIGINT       NOT NULL,
  token_hash          TEXT         NOT NULL,
  requested_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at          TIMESTAMPTZ  NOT NULL,
  used_at             TIMESTAMPTZ
);

-- BLOCK 4 — COMMENTS (table + every column; each column comment cites its DBF id)

COMMENT ON TABLE SEC_USER IS 'ENT-SEC-001 User — SHARED(owner); [DBF-SEC-001..013]';
COMMENT ON COLUMN SEC_USER.user_pk IS 'DBF-SEC-001';
COMMENT ON COLUMN SEC_USER.username IS 'DBF-SEC-002';
COMMENT ON COLUMN SEC_USER.email IS 'DBF-SEC-003';
COMMENT ON COLUMN SEC_USER.password_hash IS 'DBF-SEC-004 — never returned to any client (POL-SEC-004)';
COMMENT ON COLUMN SEC_USER.full_name_ar IS 'DBF-SEC-005';
COMMENT ON COLUMN SEC_USER.full_name_en IS 'DBF-SEC-006';
COMMENT ON COLUMN SEC_USER.status_code IS 'DBF-SEC-007 — lookup USER_STATUS (ADR-SEC-001)';
COMMENT ON COLUMN SEC_USER.last_login_at IS 'DBF-SEC-008';
COMMENT ON COLUMN SEC_USER.is_active_fl IS 'DBF-SEC-009';
COMMENT ON COLUMN SEC_USER.created_by IS 'DBF-SEC-010';
COMMENT ON COLUMN SEC_USER.created_at IS 'DBF-SEC-011';
COMMENT ON COLUMN SEC_USER.updated_by IS 'DBF-SEC-012';
COMMENT ON COLUMN SEC_USER.updated_at IS 'DBF-SEC-013';

COMMENT ON TABLE SEC_ROLE IS 'ENT-SEC-002 Role — PRIVATE; [DBF-SEC-014..024]';
COMMENT ON COLUMN SEC_ROLE.role_pk IS 'DBF-SEC-014';
COMMENT ON COLUMN SEC_ROLE.code IS 'DBF-SEC-015';
COMMENT ON COLUMN SEC_ROLE.name_ar IS 'DBF-SEC-016';
COMMENT ON COLUMN SEC_ROLE.name_en IS 'DBF-SEC-017';
COMMENT ON COLUMN SEC_ROLE.description_ar IS 'DBF-SEC-018';
COMMENT ON COLUMN SEC_ROLE.description_en IS 'DBF-SEC-019';
COMMENT ON COLUMN SEC_ROLE.is_active_fl IS 'DBF-SEC-020';
COMMENT ON COLUMN SEC_ROLE.created_by IS 'DBF-SEC-021';
COMMENT ON COLUMN SEC_ROLE.created_at IS 'DBF-SEC-022';
COMMENT ON COLUMN SEC_ROLE.updated_by IS 'DBF-SEC-023';
COMMENT ON COLUMN SEC_ROLE.updated_at IS 'DBF-SEC-024';

COMMENT ON TABLE SEC_USER_ROLE IS 'ENT-SEC-003 UserRoleAssignment — PRIVATE; [DBF-SEC-025..029]';
COMMENT ON COLUMN SEC_USER_ROLE.user_role_pk IS 'DBF-SEC-025';
COMMENT ON COLUMN SEC_USER_ROLE.user_id IS 'DBF-SEC-026';
COMMENT ON COLUMN SEC_USER_ROLE.role_id IS 'DBF-SEC-027';
COMMENT ON COLUMN SEC_USER_ROLE.assigned_by IS 'DBF-SEC-028';
COMMENT ON COLUMN SEC_USER_ROLE.assigned_at IS 'DBF-SEC-029';

COMMENT ON TABLE SEC_MODULE_REG IS 'ENT-SEC-004 ModuleRegistry — SHARED(owner); [DBF-SEC-030..038]';
COMMENT ON COLUMN SEC_MODULE_REG.module_reg_pk IS 'DBF-SEC-030';
COMMENT ON COLUMN SEC_MODULE_REG.code IS 'DBF-SEC-031';
COMMENT ON COLUMN SEC_MODULE_REG.name_ar IS 'DBF-SEC-032';
COMMENT ON COLUMN SEC_MODULE_REG.name_en IS 'DBF-SEC-033';
COMMENT ON COLUMN SEC_MODULE_REG.is_active_fl IS 'DBF-SEC-034';
COMMENT ON COLUMN SEC_MODULE_REG.created_by IS 'DBF-SEC-035';
COMMENT ON COLUMN SEC_MODULE_REG.created_at IS 'DBF-SEC-036';
COMMENT ON COLUMN SEC_MODULE_REG.updated_by IS 'DBF-SEC-037';
COMMENT ON COLUMN SEC_MODULE_REG.updated_at IS 'DBF-SEC-038';

COMMENT ON TABLE SEC_SCREEN_REG IS 'ENT-SEC-005 ScreenRegistry (SEC_PAGES) — SHARED(owner); [DBF-SEC-039..048]';
COMMENT ON COLUMN SEC_SCREEN_REG.screen_reg_pk IS 'DBF-SEC-039';
COMMENT ON COLUMN SEC_SCREEN_REG.page_code IS 'DBF-SEC-040';
COMMENT ON COLUMN SEC_SCREEN_REG.module_id IS 'DBF-SEC-041 — RULE-SEC-004 enforced by FK_SCREEN_REG_MODULE';
COMMENT ON COLUMN SEC_SCREEN_REG.name_ar IS 'DBF-SEC-042';
COMMENT ON COLUMN SEC_SCREEN_REG.name_en IS 'DBF-SEC-043';
COMMENT ON COLUMN SEC_SCREEN_REG.is_active_fl IS 'DBF-SEC-044';
COMMENT ON COLUMN SEC_SCREEN_REG.created_by IS 'DBF-SEC-045';
COMMENT ON COLUMN SEC_SCREEN_REG.created_at IS 'DBF-SEC-046';
COMMENT ON COLUMN SEC_SCREEN_REG.updated_by IS 'DBF-SEC-047';
COMMENT ON COLUMN SEC_SCREEN_REG.updated_at IS 'DBF-SEC-048';

COMMENT ON TABLE SEC_ACTION_REG IS 'ENT-SEC-006 ActionRegistry — SHARED(owner); [DBF-SEC-049..059]';
COMMENT ON COLUMN SEC_ACTION_REG.action_reg_pk IS 'DBF-SEC-049';
COMMENT ON COLUMN SEC_ACTION_REG.permission_code IS 'DBF-SEC-050';
COMMENT ON COLUMN SEC_ACTION_REG.screen_id IS 'DBF-SEC-051';
COMMENT ON COLUMN SEC_ACTION_REG.action_code IS 'DBF-SEC-052';
COMMENT ON COLUMN SEC_ACTION_REG.name_ar IS 'DBF-SEC-053';
COMMENT ON COLUMN SEC_ACTION_REG.name_en IS 'DBF-SEC-054';
COMMENT ON COLUMN SEC_ACTION_REG.is_active_fl IS 'DBF-SEC-055';
COMMENT ON COLUMN SEC_ACTION_REG.created_by IS 'DBF-SEC-056';
COMMENT ON COLUMN SEC_ACTION_REG.created_at IS 'DBF-SEC-057';
COMMENT ON COLUMN SEC_ACTION_REG.updated_by IS 'DBF-SEC-058';
COMMENT ON COLUMN SEC_ACTION_REG.updated_at IS 'DBF-SEC-059';

COMMENT ON TABLE SEC_ROLE_MODULE_GRANT IS 'ENT-SEC-007 RoleModuleGrant — PRIVATE; [DBF-SEC-060..064]';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.role_module_grant_pk IS 'DBF-SEC-060';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.role_id IS 'DBF-SEC-061';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.module_id IS 'DBF-SEC-062';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.granted_by IS 'DBF-SEC-063';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.granted_at IS 'DBF-SEC-064';

COMMENT ON TABLE SEC_ROLE_SCREEN_GRANT IS 'ENT-SEC-008 RoleScreenGrant — PRIVATE; RULE-SEC-001 enforced at application layer (P3.1); [DBF-SEC-065..069]';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk IS 'DBF-SEC-065';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.role_id IS 'DBF-SEC-066';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.screen_id IS 'DBF-SEC-067';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.granted_by IS 'DBF-SEC-068';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.granted_at IS 'DBF-SEC-069';

COMMENT ON TABLE SEC_ROLE_ACTION_GRANT IS 'ENT-SEC-009 RoleActionGrant — PRIVATE; RULE-SEC-002/005/007 enforced at application layer (P3.1); [DBF-SEC-070..074]';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.role_action_grant_pk IS 'DBF-SEC-070';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.role_id IS 'DBF-SEC-071';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.action_id IS 'DBF-SEC-072';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.granted_by IS 'DBF-SEC-073';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.granted_at IS 'DBF-SEC-074';

COMMENT ON TABLE SEC_ACTIVE_SESSION IS 'ENT-SEC-010 ActiveSession — PRIVATE; [DBF-SEC-075..082]';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.active_session_pk IS 'DBF-SEC-075';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.user_id IS 'DBF-SEC-076';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.token_ref IS 'DBF-SEC-077';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.started_at IS 'DBF-SEC-078';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.last_activity_at IS 'DBF-SEC-079';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.ip_address IS 'DBF-SEC-080';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.terminated_at IS 'DBF-SEC-081';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.terminated_by IS 'DBF-SEC-082';

COMMENT ON TABLE SEC_AUDIT_LOG IS 'ENT-SEC-011 AuditLogEntry — PRIVATE, append-only, immutable (POL-SEC-009); [DBF-SEC-083..090]';
COMMENT ON COLUMN SEC_AUDIT_LOG.audit_log_pk IS 'DBF-SEC-083';
COMMENT ON COLUMN SEC_AUDIT_LOG.event_type_code IS 'DBF-SEC-084 — lookup AUDIT_EVENT_TYPE (ADR-SEC-001)';
COMMENT ON COLUMN SEC_AUDIT_LOG.actor_user_id IS 'DBF-SEC-085';
COMMENT ON COLUMN SEC_AUDIT_LOG.occurred_at IS 'DBF-SEC-086';
COMMENT ON COLUMN SEC_AUDIT_LOG.target_ref IS 'DBF-SEC-087';
COMMENT ON COLUMN SEC_AUDIT_LOG.details_ar IS 'DBF-SEC-088';
COMMENT ON COLUMN SEC_AUDIT_LOG.details_en IS 'DBF-SEC-089';
COMMENT ON COLUMN SEC_AUDIT_LOG.ip_address IS 'DBF-SEC-090';

COMMENT ON TABLE SEC_PWD_RESET_TOKEN IS 'ENT-SEC-012 PasswordResetToken — PRIVATE; [DBF-SEC-091..096]';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.pwd_reset_token_pk IS 'DBF-SEC-091';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.user_id IS 'DBF-SEC-092';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.token_hash IS 'DBF-SEC-093';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.requested_at IS 'DBF-SEC-094';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.expires_at IS 'DBF-SEC-095';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.used_at IS 'DBF-SEC-096 — RULE-SEC-006 enforced at application layer (P3.1)';

COMMENT ON TABLE SEC_SIGNUP_REQUEST IS 'ENT-SEC-013 SignupRequest — PRIVATE; [DBF-SEC-097..104]';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.signup_request_pk IS 'DBF-SEC-097';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.email IS 'DBF-SEC-098';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.full_name_ar IS 'DBF-SEC-099';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.full_name_en IS 'DBF-SEC-100';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.submitted_at IS 'DBF-SEC-101';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.status_code IS 'DBF-SEC-102 — lookup SIGNUP_STATUS (ADR-SEC-001)';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.reviewed_by IS 'DBF-SEC-103';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.reviewed_at IS 'DBF-SEC-104';

-- BLOCK 5 — CONSTRAINTS

-- 5a PK
ALTER TABLE SEC_USER               ADD CONSTRAINT PK_SEC_USER               PRIMARY KEY (user_pk);
ALTER TABLE SEC_ROLE               ADD CONSTRAINT PK_SEC_ROLE               PRIMARY KEY (role_pk);
ALTER TABLE SEC_USER_ROLE          ADD CONSTRAINT PK_SEC_USER_ROLE          PRIMARY KEY (user_role_pk);
ALTER TABLE SEC_MODULE_REG         ADD CONSTRAINT PK_SEC_MODULE_REG         PRIMARY KEY (module_reg_pk);
ALTER TABLE SEC_SCREEN_REG         ADD CONSTRAINT PK_SEC_SCREEN_REG         PRIMARY KEY (screen_reg_pk);
ALTER TABLE SEC_ACTION_REG         ADD CONSTRAINT PK_SEC_ACTION_REG         PRIMARY KEY (action_reg_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT  ADD CONSTRAINT PK_SEC_ROLE_MODULE_GRANT  PRIMARY KEY (role_module_grant_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT  ADD CONSTRAINT PK_SEC_ROLE_SCREEN_GRANT  PRIMARY KEY (role_screen_grant_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT  ADD CONSTRAINT PK_SEC_ROLE_ACTION_GRANT  PRIMARY KEY (role_action_grant_pk);
ALTER TABLE SEC_ACTIVE_SESSION     ADD CONSTRAINT PK_SEC_ACTIVE_SESSION     PRIMARY KEY (active_session_pk);
ALTER TABLE SEC_AUDIT_LOG          ADD CONSTRAINT PK_SEC_AUDIT_LOG          PRIMARY KEY (audit_log_pk);
ALTER TABLE SEC_PWD_RESET_TOKEN    ADD CONSTRAINT PK_SEC_PWD_RESET_TOKEN    PRIMARY KEY (pwd_reset_token_pk);
ALTER TABLE SEC_SIGNUP_REQUEST     ADD CONSTRAINT PK_SEC_SIGNUP_REQUEST     PRIMARY KEY (signup_request_pk);

-- 5b UNIQUE
ALTER TABLE SEC_USER               ADD CONSTRAINT UQ_SEC_USER_USERNAME     UNIQUE (username);
ALTER TABLE SEC_USER               ADD CONSTRAINT UQ_SEC_USER_EMAIL        UNIQUE (email);
ALTER TABLE SEC_ROLE               ADD CONSTRAINT UQ_SEC_ROLE_CODE         UNIQUE (code);
ALTER TABLE SEC_USER_ROLE          ADD CONSTRAINT UQ_SEC_USER_ROLE_USER_ROLE UNIQUE (user_id, role_id);
ALTER TABLE SEC_MODULE_REG         ADD CONSTRAINT UQ_SEC_MODULE_REG_CODE   UNIQUE (code);
ALTER TABLE SEC_SCREEN_REG         ADD CONSTRAINT UQ_SEC_SCREEN_REG_PAGE   UNIQUE (page_code);
ALTER TABLE SEC_ACTION_REG         ADD CONSTRAINT UQ_SEC_ACTION_REG_PERM   UNIQUE (permission_code);
ALTER TABLE SEC_ROLE_MODULE_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE UNIQUE (role_id, module_id);
ALTER TABLE SEC_ROLE_SCREEN_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN UNIQUE (role_id, screen_id);
ALTER TABLE SEC_ROLE_ACTION_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION UNIQUE (role_id, action_id);

-- 5c CHECK (RULE-SEC-004 handled as an FK below; these two are the closed A6 lookup
-- value sets per ADR-SEC-001 — CHECK today, migrated to an FK on the shared lookup
-- infrastructure once MDL's own P2 stage creates it)
ALTER TABLE SEC_USER            ADD CONSTRAINT CHK_SEC_USER_STATUS          CHECK (status_code IN ('PENDING','ACTIVE','DISABLED'));
ALTER TABLE SEC_SIGNUP_REQUEST  ADD CONSTRAINT CHK_SEC_SIGNUP_REQUEST_STATUS CHECK (status_code IN ('PENDING','APPROVED','REJECTED'));
ALTER TABLE SEC_AUDIT_LOG       ADD CONSTRAINT CHK_SEC_AUDIT_LOG_EVENT_TYPE CHECK (event_type_code IN (
  'LOGIN_SUCCESS','LOGIN_FAILED','LOGOUT','PASSWORD_RESET_REQUESTED','PASSWORD_RESET_COMPLETED',
  'ROLE_ASSIGNED','ROLE_REVOKED','MODULE_GRANTED','MODULE_REVOKED','SCREEN_GRANTED','SCREEN_REVOKED',
  'ACTION_GRANTED','ACTION_REVOKED','SESSION_TERMINATED'));

-- 5d intra-module FK (parent PK first)
ALTER TABLE SEC_USER_ROLE         ADD CONSTRAINT FK_USER_ROLE_USER    FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_USER_ROLE         ADD CONSTRAINT FK_USER_ROLE_ROLE    FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_SCREEN_REG        ADD CONSTRAINT FK_SCREEN_REG_MODULE FOREIGN KEY (module_id) REFERENCES SEC_MODULE_REG (module_reg_pk);   -- RULE-SEC-004
ALTER TABLE SEC_ACTION_REG        ADD CONSTRAINT FK_ACTION_REG_SCREEN FOREIGN KEY (screen_id) REFERENCES SEC_SCREEN_REG (screen_reg_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT ADD CONSTRAINT FK_ROLE_MODULE_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT ADD CONSTRAINT FK_ROLE_MODULE_GRANT_MODULE FOREIGN KEY (module_id) REFERENCES SEC_MODULE_REG (module_reg_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT ADD CONSTRAINT FK_ROLE_SCREEN_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT ADD CONSTRAINT FK_ROLE_SCREEN_GRANT_SCREEN FOREIGN KEY (screen_id) REFERENCES SEC_SCREEN_REG (screen_reg_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT ADD CONSTRAINT FK_ROLE_ACTION_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT ADD CONSTRAINT FK_ROLE_ACTION_GRANT_ACTION FOREIGN KEY (action_id) REFERENCES SEC_ACTION_REG (action_reg_pk);
ALTER TABLE SEC_ACTIVE_SESSION    ADD CONSTRAINT FK_ACTIVE_SESSION_USER      FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_AUDIT_LOG         ADD CONSTRAINT FK_AUDIT_LOG_USER           FOREIGN KEY (actor_user_id) REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_PWD_RESET_TOKEN   ADD CONSTRAINT FK_PWD_RESET_TOKEN_USER     FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);

-- BLOCK 6 — TRIGGERS
-- none: no SRS RULE requires a DB-level trigger. RULE-SEC-001/002/003/005/006/007 are
-- multi-row / time-based business rules enforced at the application layer (P3.1) per
-- this stage's boundary (P2 owns structure, not business logic); RULE-SEC-004 is
-- already enforced structurally by FK_SCREEN_REG_MODULE above — no trigger needed.

-- BLOCK 7 — INDEXES (non-PK; every FK column + every SRS search/list filter column)
CREATE INDEX IDX_SEC_USER_STATUS               ON SEC_USER (status_code);
CREATE INDEX IDX_SEC_USER_ROLE_USER             ON SEC_USER_ROLE (user_id);
CREATE INDEX IDX_SEC_USER_ROLE_ROLE             ON SEC_USER_ROLE (role_id);
CREATE INDEX IDX_SEC_SCREEN_REG_MODULE          ON SEC_SCREEN_REG (module_id);
CREATE INDEX IDX_SEC_ACTION_REG_SCREEN          ON SEC_ACTION_REG (screen_id);
CREATE INDEX IDX_SEC_ROLE_MODULE_GRANT_ROLE     ON SEC_ROLE_MODULE_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_MODULE_GRANT_MODULE   ON SEC_ROLE_MODULE_GRANT (module_id);
CREATE INDEX IDX_SEC_ROLE_SCREEN_GRANT_ROLE     ON SEC_ROLE_SCREEN_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_SCREEN_GRANT_SCREEN   ON SEC_ROLE_SCREEN_GRANT (screen_id);
CREATE INDEX IDX_SEC_ROLE_ACTION_GRANT_ROLE     ON SEC_ROLE_ACTION_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_ACTION_GRANT_ACTION   ON SEC_ROLE_ACTION_GRANT (action_id);
CREATE INDEX IDX_SEC_ACTIVE_SESSION_USER        ON SEC_ACTIVE_SESSION (user_id);
CREATE INDEX IDX_SEC_ACTIVE_SESSION_TERMINATED  ON SEC_ACTIVE_SESSION (terminated_at);
CREATE INDEX IDX_SEC_AUDIT_LOG_EVENT_TYPE       ON SEC_AUDIT_LOG (event_type_code);
CREATE INDEX IDX_SEC_AUDIT_LOG_ACTOR            ON SEC_AUDIT_LOG (actor_user_id);
CREATE INDEX IDX_SEC_AUDIT_LOG_OCCURRED_AT      ON SEC_AUDIT_LOG (occurred_at);
CREATE INDEX IDX_SEC_PWD_RESET_TOKEN_USER       ON SEC_PWD_RESET_TOKEN (user_id);
CREATE INDEX IDX_SEC_SIGNUP_REQUEST_STATUS      ON SEC_SIGNUP_REQUEST (status_code);
CREATE INDEX IDX_SEC_SIGNUP_REQUEST_EMAIL       ON SEC_SIGNUP_REQUEST (email);

-- BLOCK 8 — LOOKUP SEED DATA
-- No shared lookup table exists yet in this version (ADR-SEC-001); the A6 value sets
-- (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) live only as the CHECK constraints
-- above in v1 — there is no separate seed-data table to populate.
COMMIT;

-- BLOCK 9 — VIEWS
-- none required by this SRS.

-- BLOCK 10 — FUNCTIONS / PROCEDURES
-- none required by this SRS.

-- BLOCK 11 — DEFERRED FK PATCH BLOCKS
-- none: SEC is ROOT, no XM row exists this version (see §2 XM REGISTER).
```

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| ADR-SEC-001 | SEC's three owned lookup types (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) are CHECK-constrained VARCHAR columns in v1, not rows in a shared MDL_LOOKUP_TYPE/MDL_LOOKUP_VALUE pair, because MDL has not yet been analyzed (SEC precedes it in this batch's mandated order — GENERATION-INSTRUCTIONS.md §3) | erp/decisions/SEC/ADR-SEC-001.md | ACCEPTED (non-breaking) — superseded by a migration once MDL's own P2 creates the shared tables |
| DEFAULT | password_hash / token_hash use TEXT (algorithm-agnostic width); no column length assumes a specific hash algorithm | domain best practice; no SRS-stated algorithm | non-breaking |
| DEFAULT | RULE-SEC-001/002/003/005/006/007 enforced at the application layer (P3.1), not as DB triggers/CHECKs, per this stage's boundary (structure, not business logic) | this stage §11 boundaries | non-breaking |

No BLOCKED ADR — the pass was not stopped.

## 5. REGISTRY CONTENT
See `registry-db-sec.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-SEC-001** — SEC_USER.user_pk [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-002** — SEC_USER.username [ENT-SEC-001, REQ-SEC-001, REQ-SEC-009]
**DBF-SEC-003** — SEC_USER.email [ENT-SEC-001, REQ-SEC-006, REQ-SEC-009]
**DBF-SEC-004** — SEC_USER.password_hash [ENT-SEC-001, REQ-SEC-001, REQ-SEC-007]
**DBF-SEC-005** — SEC_USER.full_name_ar [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-006** — SEC_USER.full_name_en [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-007** — SEC_USER.status_code [ENT-SEC-001, REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031]
**DBF-SEC-008** — SEC_USER.last_login_at [ENT-SEC-001, REQ-SEC-001]
**DBF-SEC-009** — SEC_USER.is_active_fl [ENT-SEC-001, REQ-SEC-011, REQ-SEC-031]
**DBF-SEC-010** — SEC_USER.created_by [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-011** — SEC_USER.created_at [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-012** — SEC_USER.updated_by [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-013** — SEC_USER.updated_at [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-014** — SEC_ROLE.role_pk [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-015** — SEC_ROLE.code [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-016** — SEC_ROLE.name_ar [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-017** — SEC_ROLE.name_en [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-018** — SEC_ROLE.description_ar [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-019** — SEC_ROLE.description_en [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-020** — SEC_ROLE.is_active_fl [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-021** — SEC_ROLE.created_by [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-022** — SEC_ROLE.created_at [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-023** — SEC_ROLE.updated_by [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-024** — SEC_ROLE.updated_at [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-025** — SEC_USER_ROLE.user_role_pk [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-026** — SEC_USER_ROLE.user_id [ENT-SEC-003, ENT-SEC-001, REQ-SEC-010]
**DBF-SEC-027** — SEC_USER_ROLE.role_id [ENT-SEC-003, ENT-SEC-002, REQ-SEC-010]
**DBF-SEC-028** — SEC_USER_ROLE.assigned_by [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-029** — SEC_USER_ROLE.assigned_at [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-030** — SEC_MODULE_REG.module_reg_pk [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-031** — SEC_MODULE_REG.code [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-032** — SEC_MODULE_REG.name_ar [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-033** — SEC_MODULE_REG.name_en [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-034** — SEC_MODULE_REG.is_active_fl [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-035** — SEC_MODULE_REG.created_by [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-036** — SEC_MODULE_REG.created_at [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-037** — SEC_MODULE_REG.updated_by [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-038** — SEC_MODULE_REG.updated_at [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-039** — SEC_SCREEN_REG.screen_reg_pk [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-040** — SEC_SCREEN_REG.page_code [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-041** — SEC_SCREEN_REG.module_id [ENT-SEC-005, ENT-SEC-004, REQ-SEC-017, REQ-SEC-018]
**DBF-SEC-042** — SEC_SCREEN_REG.name_ar [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-043** — SEC_SCREEN_REG.name_en [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-044** — SEC_SCREEN_REG.is_active_fl [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-045** — SEC_SCREEN_REG.created_by [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-046** — SEC_SCREEN_REG.created_at [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-047** — SEC_SCREEN_REG.updated_by [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-048** — SEC_SCREEN_REG.updated_at [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-049** — SEC_ACTION_REG.action_reg_pk [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-050** — SEC_ACTION_REG.permission_code [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-051** — SEC_ACTION_REG.screen_id [ENT-SEC-006, ENT-SEC-005, REQ-SEC-019]
**DBF-SEC-052** — SEC_ACTION_REG.action_code [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-053** — SEC_ACTION_REG.name_ar [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-054** — SEC_ACTION_REG.name_en [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-055** — SEC_ACTION_REG.is_active_fl [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-056** — SEC_ACTION_REG.created_by [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-057** — SEC_ACTION_REG.created_at [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-058** — SEC_ACTION_REG.updated_by [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-059** — SEC_ACTION_REG.updated_at [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-060** — SEC_ROLE_MODULE_GRANT.role_module_grant_pk [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-061** — SEC_ROLE_MODULE_GRANT.role_id [ENT-SEC-007, ENT-SEC-002, REQ-SEC-012, REQ-SEC-015]
**DBF-SEC-062** — SEC_ROLE_MODULE_GRANT.module_id [ENT-SEC-007, ENT-SEC-004, REQ-SEC-012, REQ-SEC-015]
**DBF-SEC-063** — SEC_ROLE_MODULE_GRANT.granted_by [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-064** — SEC_ROLE_MODULE_GRANT.granted_at [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-065** — SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-066** — SEC_ROLE_SCREEN_GRANT.role_id [ENT-SEC-008, ENT-SEC-002, REQ-SEC-013, REQ-SEC-015]
**DBF-SEC-067** — SEC_ROLE_SCREEN_GRANT.screen_id [ENT-SEC-008, ENT-SEC-005, REQ-SEC-013]
**DBF-SEC-068** — SEC_ROLE_SCREEN_GRANT.granted_by [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-069** — SEC_ROLE_SCREEN_GRANT.granted_at [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-070** — SEC_ROLE_ACTION_GRANT.role_action_grant_pk [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-071** — SEC_ROLE_ACTION_GRANT.role_id [ENT-SEC-009, ENT-SEC-002, REQ-SEC-014, REQ-SEC-020, REQ-SEC-030]
**DBF-SEC-072** — SEC_ROLE_ACTION_GRANT.action_id [ENT-SEC-009, ENT-SEC-006, REQ-SEC-014]
**DBF-SEC-073** — SEC_ROLE_ACTION_GRANT.granted_by [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-074** — SEC_ROLE_ACTION_GRANT.granted_at [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-075** — SEC_ACTIVE_SESSION.active_session_pk [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-076** — SEC_ACTIVE_SESSION.user_id [ENT-SEC-010, ENT-SEC-001, REQ-SEC-001, REQ-SEC-011, REQ-SEC-027]
**DBF-SEC-077** — SEC_ACTIVE_SESSION.token_ref [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-078** — SEC_ACTIVE_SESSION.started_at [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-079** — SEC_ACTIVE_SESSION.last_activity_at [ENT-SEC-010, REQ-SEC-027]
**DBF-SEC-080** — SEC_ACTIVE_SESSION.ip_address [ENT-SEC-010, REQ-SEC-027]
**DBF-SEC-081** — SEC_ACTIVE_SESSION.terminated_at [ENT-SEC-010, REQ-SEC-011, REQ-SEC-028]
**DBF-SEC-082** — SEC_ACTIVE_SESSION.terminated_by [ENT-SEC-010, REQ-SEC-011, REQ-SEC-028]
**DBF-SEC-083** — SEC_AUDIT_LOG.audit_log_pk [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-084** — SEC_AUDIT_LOG.event_type_code [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-085** — SEC_AUDIT_LOG.actor_user_id [ENT-SEC-011, ENT-SEC-001, REQ-SEC-002, REQ-SEC-024]
**DBF-SEC-086** — SEC_AUDIT_LOG.occurred_at [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-087** — SEC_AUDIT_LOG.target_ref [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-088** — SEC_AUDIT_LOG.details_ar [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-089** — SEC_AUDIT_LOG.details_en [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-090** — SEC_AUDIT_LOG.ip_address [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-091** — SEC_PWD_RESET_TOKEN.pwd_reset_token_pk [ENT-SEC-012, REQ-SEC-006]
**DBF-SEC-092** — SEC_PWD_RESET_TOKEN.user_id [ENT-SEC-012, ENT-SEC-001, REQ-SEC-006]
**DBF-SEC-093** — SEC_PWD_RESET_TOKEN.token_hash [ENT-SEC-012, REQ-SEC-006, REQ-SEC-007]
**DBF-SEC-094** — SEC_PWD_RESET_TOKEN.requested_at [ENT-SEC-012, REQ-SEC-006]
**DBF-SEC-095** — SEC_PWD_RESET_TOKEN.expires_at [ENT-SEC-012, REQ-SEC-006, REQ-SEC-008]
**DBF-SEC-096** — SEC_PWD_RESET_TOKEN.used_at [ENT-SEC-012, REQ-SEC-007, REQ-SEC-008]
**DBF-SEC-097** — SEC_SIGNUP_REQUEST.signup_request_pk [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-098** — SEC_SIGNUP_REQUEST.email [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-099** — SEC_SIGNUP_REQUEST.full_name_ar [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-100** — SEC_SIGNUP_REQUEST.full_name_en [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-101** — SEC_SIGNUP_REQUEST.submitted_at [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-102** — SEC_SIGNUP_REQUEST.status_code [ENT-SEC-013, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005]
**DBF-SEC-103** — SEC_SIGNUP_REQUEST.reviewed_by [ENT-SEC-013, REQ-SEC-004, REQ-SEC-005]
**DBF-SEC-104** — SEC_SIGNUP_REQUEST.reviewed_at [ENT-SEC-013, REQ-SEC-004, REQ-SEC-005]
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

