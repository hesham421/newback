# BRIEF — stage `domain-profile` (Domain Profile) · module SEC · v1 · profile `erp`

Lane `analysis` · implementer claude:opus · effort high · round 1

## Rules that bind this run
- Questions: **allowed**. Close every open point inside this dialogue with a researched, recommended answer; never write an external open-questions file.
- Owns IDs: none — ID grammar `{prefix}-{MOD}-{seq}` (seq width 3); never re-number, never restart a sequence.
- Read only what this brief contains (generated current state); never open version folders yourself.
- Write exactly these files (complete files; in a delta version only what changed, plus `change-manifest.md`):
- `erp/domain-profile.md`
- Respond with one `<<<FILE: <repo-relative path>>>> … <<<END FILE>>>` block per file when running through a command runner; when running as the operator, write the files directly.

## Dialogue protocol (converging, in-brief)
Implementers claude:opus alternate for at most 4 rounds; converge on **mutually-acceptable**.
Round 1 drafts the artifacts and, for every open point, a `PROPOSAL:` block (options, researched recommendation, sources).
Each later round answers every open PROPOSAL (accept / amend with reason), refines the artifacts, and appends `<!-- CONVERGED -->` at the end of the response when nothing material remains open. The last response is final.

## Contracts checked by `gov.py analyze` after this stage
- **C1** domain profile → registry bootstrap: C1.1 exists {'artifact': 'domain-profile'} [CRITICAL]; C1.2 no-questions {'artifact': 'domain-profile'} [CRITICAL]; C1.3 languages {'artifact': 'domain-profile'} [MAJOR]

---
# ENGINE
# Domain Profile — ENGINE

```
Engine        : Domain Profile
Stage id      : domain-profile
Pass          : pre   (once per platform)
Questions     : allowed — resolved in-dialogue, confirmed by the user
Dialogue      : yes — lane analysis (claude:opus; ≤ 4 rounds; converge on "mutually-acceptable"; output: resolved-decisions)
Research      : web
Inputs        : raw-idea, platform-brief?
Produces      : domain/domain-profile.md
Owns IDs      : — (no pipeline IDs; it defines the vocabulary they will use)
Next          : P-1
Profile       : erp — ERP Platform
```

This engine runs as a **conversational project**: the user brings a raw idea, the
engine researches, proposes, and pressure-tests, and the two of them converge on a
`domain-profile.md` that every later stage reads first. Its `questions: allowed`
means it may ask the user what research and dialogue could not settle. The
**saved file is the entry gate** of `P-1`: nothing runs before it
exists, and nothing before it may require a registry (the registry is built by
`P-1` *from* this file).

Completion (write → registry → analyze → commit → gate) is owned by the orchestrator
— see `shared/GOVERNANCE-CORE.md`. Versions of the profile follow `shared/VERSIONING.md`.

---

## 0 — Identity and boundaries

**What this engine IS**
- A thinking partner: it reflects the idea back, names what is strong vs thin,
  proposes structure and real options with trade-offs.
- A researcher: it looks at how established systems in this domain structure similar
  products before it proposes anything (§2).
- A closer of open points: every open point receives a researched, recommended answer
  and is settled in the dialogue (§3) — there is no external open-questions file.
- The author of the domain's **ubiquitous language** (§4 STEERING block): the words,
  module codes and bounded contexts that all later stages use verbatim.

**What it is NOT**
- It never turns one of its own proposals into a written fact without the user's
  explicit "yes, that one". Proposing is its job; deciding silently is a violation.
- It does not assign any pipeline ID (`POL, US, REQ, AC, ENT, RULE, DBF, XM, API, QR, UXD, SCR, SCR-REQ, TC, ADR, CS`) — those
  belong to their owning stages (`factory.ids.atoms.*.owner`).
- It does not validate the domain against a registry (there is none yet) and it does
  not edit profile or knowledge files — the profile is data, changed by the user.

---

## 1 — Intake of the raw idea

Inputs: `raw-idea`, `platform-brief?`. Any form is accepted — a paragraph, notes,
a brief, an existing system description.

```
STEP 1.1 — Mode
  erp/domain-profile.md exists?  → CONTINUATION: read it, show it back, work only
                                       on what is new or revised.
                                     → FRESH: everything below, field by field.

STEP 1.2 — Framing (before any field question)
  Reflect the idea back in two or three sentences.
  Name what is strong and what is thin (scope, positioning, boundaries).
  Propose a candidate MAIN COMPONENTS structure to react to — a proposal, not a fact.
  Cross-check candidate components against the profile's module codes
  (`profile.vocabulary.module_prefixes`) and bounded contexts
  (`profile.vocabulary.bounded_contexts`) — these are the starting vocabulary,
  not a closed list: a component the profile does not yet name is proposed
  freely (§4 block 7.3) and flows forward as RESERVED, never blocked on it.

STEP 1.3 — Open-point inventory
  List every point the intake did not settle (scope edge, component ownership,
  a rule, a relation to another domain). Each becomes an item for §2 research
  and §3 resolution. Nothing is guessed at this step.
```

Interview cadence for every field of §4: engage (sharpen / surface the gap / offer
2–3 researched options with trade-offs) → the user answers or picks → write THAT
answer → next field. Never batch fields; never write what the user did not state or
select.

---

## 2 — Research step (`research: web`)

Before proposing answers, research how established systems in this domain structure
similar products. This absorbs the former idea-draft step.

```
RESEARCH TARGETS (per open point and for the structure as a whole)
  R1  Decomposition  : how mature products split this domain into modules /
                       bounded contexts; what is core vs extension
  R2  Vocabulary     : the terms practitioners actually use (ubiquitous language);
                       synonyms to avoid
  R3  Governing rules: standards, regulations, widely adopted conventions that
                       constrain the domain
  R4  Relations      : which other domains such products integrate with, and how
                       (owner / consumer, hard vs soft dependency)
  R5  Pitfalls       : known anti-patterns in this domain's products

PRIMARY SOURCES FIRST
  The profile declares knowledge files — cite them before anything external:
    - profiles/erp/knowledge/erp-domain-standards.md
  Then real web research: vendor documentation, standards bodies, reference
  architectures, practitioner literature. Prefer primary and recent sources.

CITATION RULE
  Every researched claim that reaches a proposal carries a source: title, URL (or
  knowledge-file path), date accessed. Unsourced claims are opinions and are
  labelled as such.

RESEARCH LOG (kept in the profile, §4 block 9)
  | # | Point | What established systems do | Source(s) | Used in |
```

Research informs proposals; it never becomes a written fact by itself. Only what the
user confirms (§5) is written.

---

## 3 — Resolving open points in dialogue

For every open point the engine emits one PROPOSAL block, the dialogue converges,
then the user confirms.

```
PROPOSAL — [point]
  Question      : [one sentence]
  Options       : A) … (trade-off)   B) … (trade-off)   C) … (trade-off)
  Researched    : [what established systems do — with sources from §2]
  Recommended   : [option] — because [rationale]
  Consequence   : [what this fixes for later stages: scope / vocabulary / relations]
```

Dialogue protocol (lane `analysis`): the implementers (claude:opus)
take turns on each PROPOSAL — the second challenges the first's recommendation with
evidence, the first answers — for at most 4 rounds, until the answer is
**mutually-acceptable** (not perfect). The converged block is shown to the user as
the recommended answer; the user confirms, adjusts, or picks another option.

```
RESOLUTION RULES
  - Output of the dialogue = resolved decisions recorded in the profile (§4 block 8).
    There is no separate open-questions file.
  - "I don't know / decide for me" → present the recommended answer again with its
    sources; if the user still will not choose, the point stays OPEN (§4 block 10)
    and is named in the exit summary — it is never guessed.
  - A point that only a later stage can settle (e.g. a field-level rule) is not
    kept open here: record the steering fact that lets that stage decide it, and
    hand it forward.
```

---

## 4 — Output template — `domain/domain-profile.md`

Language policy: narrative in `ar`; every name, label and
term in the STEERING block carries all of `ar, en`.

```markdown
# DOMAIN PROFILE — [Domain / Platform name]
══════════════════════════════════════════════════════════════════
Profile         : erp (ERP Platform)
Version         : [N]            (per shared/VERSIONING.md)
Last Updated    : [date]
Status          : [FRESH | CONTINUATION — updated from v[N-1]]
Research        : [N] sources cited (block 9)
══════════════════════════════════════════════════════════════════

## 1. SCOPE
[In bounds / out of bounds. Stated by the user — never inferred.]

## 2. PURPOSE
[Why this domain/platform exists. The problem it solves.]

## 3. RESPONSIBILITIES
[The capabilities this domain owns. As stated.]

## 4. MAIN COMPONENTS
| # | Component | Module code | Bounded context | Category (user-defined) | Core / extension | Notes |
|---|-----------|-------------|-----------------|--------------------------|------------------|-------|
| 1 | [name]    | [code from profile.vocabulary.module_prefixes] | [context id] | [e.g. Foundation / Business] | [Core / ext-name] | [as stated] |
Every row is something the user named explicitly.

## 5. GOVERNING RULES
[Domain-level constraints — each with its source: user statement, knowledge file, or research citation.]

## 6. RELATIONSHIPS WITH OTHER DOMAINS
| This component | Depends on | Kind | Direction | Stated by |
|---|---|---|---|---|
| [component] | [other domain / component] | HARD / SOFT / EVENT | owner → consumer | [user / source] |

## 7. STEERING  (read verbatim by every later stage)
### 7.1 Ubiquitous language
| Term | Definition | Do not say | Module code |
|---|---|---|---|
| [term — in each of ar/en] | [one sentence] | [rejected synonyms] | [code] |
(starts from `profile.vocabulary.glossary`; adds only confirmed domain terms)

### 7.2 Bounded contexts
| Context | Owns module codes | Boundary statement |
|---|---|---|
(starts from `profile.vocabulary.bounded_contexts`)

### 7.3 Module prefixes proposal
| Code | Display | Status |
|---|---|---|
| [code] | [display] | IN PROFILE / PROPOSED — carried into `P-1` as RESERVED |
Codes are taken from `profile.vocabulary.module_prefixes` where they already exist.
A module the profile does not list is recorded as PROPOSED here and needs no manual
profile edit to proceed: `P-1` registers it as RESERVED and the pipeline
continues normally. Adding the code to `profiles/erp.yaml` (or the active profile) is
optional bookkeeping the user can do whenever convenient — never a gate. Engines never
invent codes; they only carry forward what the user named.

### 7.4 Identifier rules
Later stages build IDs as `{prefix}-{MOD}-{seq}` (seq width 3) with
the module codes above. Entity kinds: `master, transactional, lookup, config, security`.
Record here any domain-specific atom the profile adds (`profile.ids.atoms`).

### 7.5 Knowledge sources to cite
- `profiles/erp/knowledge/erp-domain-standards.md`
- plus the research sources in block 9 that the user accepted as references

## 8. RESOLVED DECISIONS
| # | Point | Decision | Recommended by dialogue? | Confirmed by user | Sources |
|---|---|---|---|---|---|

## 9. RESEARCH LOG
| # | Point | What established systems do | Source(s) (title, URL/path, date) | Used in |
|---|---|---|---|---|

## 10. OPEN ITEMS
[Field / Status: OPEN / Note — expected: none. Anything here is named in the exit summary.]
══════════════════════════════════════════════════════════════════
```

---

## 5 — Write rule and exit

```
WRITE RULE (unchanged from every prior edition of this engine)
  Only what the user confirmed is written. A proposal, a research finding, or a
  dialogue recommendation becomes a line in the file the moment the user says
  "yes, that one" — and not before.

EXIT
  1. Show the assembled document back with a short "where this could be sharper" note.
  2. Name every OPEN item (block 10). Expected: none.
  3. Save `domain/domain-profile.md`. The saved file is the entry gate of P-1.
     The orchestrator commits it (shared/GOVERNANCE-CORE.md).
  4. A later revision of the profile is a new version per shared/VERSIONING.md —
     never an in-place edit of a version that later stages already consumed.
```

---

## 6 — Self-check before saving

- [ ] Every field in §4 blocks 1–7 is a user-confirmed statement or is marked OPEN.
- [ ] Every governing rule and every research-derived claim carries a source.
- [ ] Every component row has a module code that is IN PROFILE or PROPOSED.
- [ ] STEERING terms are defined once, with rejected synonyms, in all of `ar, en`.
- [ ] RESOLVED DECISIONS lists every PROPOSAL that was raised; none is missing.
- [ ] No pipeline ID, no field list, no rule text that belongs to a later stage.
- [ ] No open-questions file was written anywhere; open items live only in block 10.


---
# INPUTS (generated current state)

<<<INPUT: raw-idea>>>
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
