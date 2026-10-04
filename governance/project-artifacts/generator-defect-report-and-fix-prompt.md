# Prompt — fix the governance plan generator at source

> Hand this file to a session working **inside the generator repository** (`gov.py`,
> `factory.yaml`, `profiles/erp.yaml`, the engine). It is self-contained: the generator
> session has no access to the consuming backend repo, so every claim below carries its own
> evidence and its own reproduction.
>
> Compiled 2026-09-11 from a real build of module SEC (13 entities, 27 APIs) in the
> `backend` repo, cross-checked against the FIN and MDL outputs of the same generator.

---

## Copy everything below this line into the generator session

---

You are working on the governance plan generator (`gov.py render`, driven by `factory.yaml`
plus a profile such as `profiles/erp.yaml`). It emits, per module, a P1 SRS, a P2 db-script, a
P3.1 backend execution plan, a `manifest.json`, an `execution-state.json`, and a split
`packages/backend-execution/` tree (`_SECTIONS.md`, per-phase `*-HEADER.md` and `SUB` files).

Downstream, an AI agent implements a Spring Boot / PostgreSQL 16 backend from those artifacts,
bound by a separate set of skill files it treats as authoritative. A real end-to-end build of
module **SEC** was just completed through the DATA-DOM phase. It surfaced defects that are
**systemic** — reproduced identically in the SEC, FIN and MDL outputs — and that cost the
implementing agent a correction round on almost every sub.

Your job is to fix them **in the generator**, not in the generated files. The generated files
in the consuming repo will be regenerated and are not to be patched.

Work through the priorities in order. P0 is the multiplier — fix it first, because it is the
reason the rest shipped undetected.

---

### P0 — The ALIGN self-check reports PASSED while every defect below is present

Every generated plan ends with an `## Alignment self-check (ALIGN)` block. In all three
modules it reads:

```
RESULT            PASSED ✓ — 0 findings
```

It asserts, among other things:

```
BINDING (§2A)  ✓ no placeholder table/column/key/generation object; every column cites a DBF
TRACEABILITY   ✓ every traces target exists upstream
MANIFEST (§4)  ✓ all 104 DBF of every bound table listed
QRC (§5)       ✓ every API with a DB operation has ≥1 QR
```

Every one of those sentences is true as literally checked, and every defect in P1–P4 below
still got through. The self-check validates that references are *shaped* correctly; it does not
validate that they *resolve*, that two generated tables *agree*, or that a generated rule has
an input. A self-check that always prints PASSED is worse than no self-check, because it
transferred false confidence to the consuming agent.

**Add these cross-checks, and make any failure print as a finding rather than being omitted:**

1. **Cross-table column agreement.** For every `DBF-*` id, the column name in the P2 db-script
   §1 field registry, the column in its §3 `CREATE TABLE`, the column in the `COMMENT ON` line,
   and the column in the P3.1 `SUB` file's FIELDS table must all be the same string. See P3.1
   below — exactly one of 55 SEC columns diverges, and nothing caught it. You already hold both
   tables in memory at render time; this is a dictionary comparison.
2. **Path resolution.** Every path emitted into `manifest.json` and `execution-state.json` must
   resolve against the repo the artifacts are written into. See P2.
3. **Declared-format vs emitted-value agreement.** Where a phase declares a format (error-code
   shape, id shape, naming pattern), assert the values actually emitted elsewhere in the same
   plan match it. See P3.
4. **Rule input sourcing.** Every `RULE-*` that the plan turns into a runtime check must have a
   traceable source for the *data the check reads*, not just an error-catalog row and an API.
   See P4.
5. **Cross-module contract existence.** When module A's plan states it will call module B's
   API, assert that B's own API registry actually defines such an endpoint. See P4.

Also: consider emitting the self-check as machine-readable JSON alongside the prose, so the
consuming pipeline can gate on it instead of reading a paragraph.

---

### P1 — Primary-key generation strategy is wrong for this profile

**What is emitted** (SEC, FIN, MDL — the newer generator output):

```sql
-- BLOCK 1 — SEQUENCES
-- none: every PK uses GENERATED ALWAYS AS IDENTITY (postgresql16 syntax_map)

CREATE TABLE SEC_USER (
  user_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  ...
```

and in the P3.1 plan: `PK generation `GENERATED ALWAYS AS IDENTITY`` on every entity, plus a
CORE type-mapping row `GENERATED ALWAYS AS IDENTITY | Long`.

**Why this is wrong for this target.** The consuming repo's binding skills mandate
`GenerationType.SEQUENCE` with an explicit `@SequenceGenerator(..., allocationSize = 1)`, and
list `GenerationType.IDENTITY` as an **automatic rejection trigger** in the master validation
skill. More importantly, the same database already holds four modules (CU, NOTIF, FILE, and a
prior SEC schema) built on explicit `SEQ_<TABLE>` sequences through applied Flyway migrations
V1–V15. Those migrations cannot be rewritten — Flyway checksums applied migrations. Adopting
IDENTITY for newer modules would put two PK strategies in one database permanently.

Note the older generator output was already correct: `CU`, `NOTIF` and `FILE` db-scripts emit
`CREATE SEQUENCE`. The convention changed between generations and nothing reconciled it.

**Reproduce:**

```bash
for m in CU NOTIF FILE SEC FIN MDL; do
  f=$(ls governance/modules/$m/P2/db-script*.md | head -1)
  echo "$m  sequences=$(grep -c 'CREATE SEQUENCE' $f)  identity=$(grep -c 'GENERATED ALWAYS AS IDENTITY' $f)"
done
# CU/NOTIF/FILE → sequences>0, identity=0
# SEC/FIN/MDL   → sequences=0, identity=27/29/5
```

**Fix.** Make PK generation a profile setting rather than a hard-coded dialect default, and set
it to sequences for the `erp` profile. The relevant config keys, named in the generated output
itself, are `profile.stack.db.syntax_map` and `profile.stack.db.naming.pk_pattern`.

When the profile selects sequences, the emitted db-script must:
- populate `BLOCK 1 — SEQUENCES` with one `CREATE SEQUENCE SEQ_<TABLE> START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;` per table (this is the exact form the four working modules use and it is valid PostgreSQL — do **not** emit Oracle-style `NO CACHE`, which fails on PostgreSQL);
- declare PK columns as plain `BIGINT NOT NULL`;
- carry the sequence name into the P3.1 per-entity `BINDINGS` line, so the implementing agent
  reads it rather than deriving it;
- keep the CORE type-mapping row consistent (`BIGINT` → `Long`).

**Do not** simply hard-code sequences either — the point is that the strategy is a profile
decision, and a profile targeting a greenfield database may legitimately prefer identity.

---

### P2 — Emitted paths do not resolve in the consuming repository

**What is emitted** — `governance/modules/SEC/manifest.json`:

```json
"root": "erp/modules/SEC",
"stages": { "P0": "erp/modules/SEC/P0", "P1": "erp/modules/SEC/P1", ... },
"decisions_dir": "erp/decisions/SEC",
"state_dir": "erp/modules/SEC/_state"
```

The artifacts actually live at `governance/modules/SEC/...`. The `erp/` prefix resolves to
nothing in the consuming repo — there is no `erp/` directory. Same in FIN and MDL; the older
CU and NOTIF manifests emit an empty `root` instead, which is a different bug with the same
cause.

Consequences observed in the real build:

- **`decisions_dir` is dangling.** `ADR-SEC-001` and `ADR-SEC-002` are referenced 30+ times
  across the P2 and P3.1 artifacts, always as `erp/decisions/SEC/ADR-SEC-00N.md`. Neither file
  exists anywhere in the consuming repo. The implementing agent could not read either ADR and
  had to work from the restatements embedded in the db-script. **Two problems in one:** the
  path prefix is wrong, *and* the generator never emits the ADR files it cites.
- **`api_docs_path` is dangling.** `execution-state.json` sets
  `"api_docs_path": "governance/modules/SEC/api-docs/"` — correct prefix this time, but the
  directory is never created, and the DOC phase is specified to write into it.
- **`packages/backend-test/` is declared but absent.** `manifest.json` lists it under
  `packages`, `status.split["backend/test"]` is `false`, and the directory does not exist —
  while `execution-state.json`'s `test_phases[0].header_file` points somewhere else entirely
  (`test_gen/backend-test-plan-sec.md`). Two generated files disagree about where the test plan
  lives.

**Reproduce:**

```bash
python3 -c "import json;print(json.load(open('governance/modules/SEC/manifest.json'))['root'])"
# erp/modules/SEC   ← does not exist
ls governance/modules/SEC/api-docs 2>&1   # No such file or directory
find . -name 'ADR-SEC-*'                  # nothing
```

**Fix.** Emit paths relative to the repository root the artifacts are written into, from a
single resolved base rather than a literal prefix. Create every directory a generated path
points at, or stop emitting the path. Make `manifest.json` and `execution-state.json` agree on
the test-plan location — one of them should be derived from the other, not written twice. And
either emit the ADR files or stop citing them by path.

---

### P3 — Two generated files contradict each other inside one module

**P3.1 — Error-code format.** The generated `CORE.md` declares:

> Runtime `code` format: `SEC-<3-digit-sequence>` (module-scoped, stated once here so
> `api-verify` can assert on it — e.g. `SEC-001` for the first catalog row).

The generated Error Catalog in the *same plan* then emits 28 rows, none of which has that
shape:

```
SEC-401-INVALID-CREDENTIALS · SEC-409-USER-DUP · SEC-404-USER · SEC-403-FORBIDDEN · SEC-500
```

and the generated test plan asserts on those literal strings. The declared format is a stale
generalisation that no emitted value obeys. Identical in FIN (`FIN-<3-digit>` declared,
`FIN-409-ACCOUNT-DUP` emitted) and MDL (`MDL-<3-digit>` declared, `MDL-409-TYPE-DUP` emitted).

Worse, the wrong format is propagating *as a convention*: MDL's CORE.md line 13 reads
`format `MDL-<3-digit>` (module-scoped, same convention as SEC's `SEC-<3-digit>`)` — it cites
SEC's declaration as precedent, so each new module now inherits and reinforces a format string
that no module's own catalog has ever obeyed.

This is not cosmetic: the CORE text explicitly says the format is stated "so `api-verify` can
assert on it", so a downstream verifier built from that sentence would reject every real code.

*Fix:* derive the declared format string from the catalog generator, or delete the declaration.
Do not maintain it as free text in a template.

**P3.2 — Column name transcribed wrong in one place.** `DATA-DOM-TRANSACTIONAL.md`'s
ENT-SEC-007 FIELDS table lists `DBF-SEC-064` with column **`grant_at`**. The db-script names it
**`granted_at`** in all three of its own places (§1 registry, §3 `CREATE TABLE`, `COMMENT ON`),
matching the two sibling grant tables.

**Reproduce** — this script found it, and found only it, across 55 columns:

```python
import re, glob
truth = {}
for line in open('governance/modules/SEC/P2/db-script-sec.md', encoding='utf-8'):
    m = re.match(r'\|\s*(DBF-SEC-\d+)\s*\|\s*([a-z_0-9]+)\s*\|', line)
    if m: truth.setdefault(m.group(1), m.group(2))
for f in glob.glob('governance/modules/SEC/packages/backend-execution/DATA-DOM/*.md'):
    for line in open(f, encoding='utf-8'):
        m = re.match(r'\|\s*(DBF-SEC-\d+)\s*\|\s*(\w+)\s*\|\s*([a-z_0-9]+)\s*\|', line)
        if m and m.group(1) in truth and m.group(3) != truth[m.group(1)]:
            print(m.group(1), 'plan:', m.group(3), 'db-script:', truth[m.group(1)])
# DBF-SEC-064 plan: grant_at db-script: granted_at
```

A single divergence in 55 is a transcription bug, not a naming-convention problem — but it is
exactly the class of defect P0's cross-check #1 would catch for free, and a column name is the
one thing the implementing agent is forbidden to guess at.

---

### P4 — The generator emits rules and cross-module calls with no source

These are design-level and need judgement, not a one-line fix. They are the two findings that
cost the most time in the real build.

**P4.1 — A rule was generated whose input data has no declared source.**

`RULE-SEC-005` (segregation of duties) is generated into the SRS, given an error-catalog row
(`SEC-409-SOD-CONFLICT`), an enforcement query (`QR-SEC-031`), and two enforcing APIs
(`API-SEC-008`, `API-SEC-017`). Its statement is:

> "The system shall prevent assigning a user, by any combination of roles, both actions of a
> **module-declared conflicting pair**."

Nothing in SEC v1 declares a pair. There is no entity, no table, no `DBF-*` column, no request
field on the action-registration API, and no screen element through which a conflicting pair
can ever be recorded. The rule is unenforceable by construction: the guard compiles, the error
code is registered, and the check can never fire. Verified empty across SRS A3/A6/A7/A8, Part B
screens, §7.1, all 104 `DBF-*` rows across 13 tables, and `API-SEC-020`'s request contract.

The corresponding generated test case is likewise unrunnable — its own precondition cannot be
established through any SEC v1 API, and its test-data line points at a *different module's*
document.

The ALIGN self-check passed it because it only asked "does this RULE have a catalog code and an
API?" — both true. It never asked "where does the data this rule reads come from?"

*Fix:* when a generated rule's statement references data ("module-declared X", "configured Y"),
require a resolvable source — an `ENT`/`DBF`, or an explicit deferral marker. If neither exists,
that is an ALIGN finding, not a pass. An honest `DEFERRED — no declaration surface in this
version` is far more useful downstream than a silently unenforceable rule.

**P4.2 — A cross-module call was generated against an endpoint that does not exist.**

FIN's generated plan states its service layer will enforce SoD "by reading the two roles' user
sets through SEC's role/grant read APIs". SEC's own generated API registry defines 27 endpoints
and **none** of them returns the users of a role, or a user's effective actions
(`API-SEC-027` is caller-self-scoped and returns modules→screens only).

So FIN's plan depends on a SEC endpoint SEC's plan never generates. Both modules' ALIGN checks
passed, because each validated only its own artifacts.

*Fix:* validate cross-module contracts across the module set, not per module. When module A's
plan names an API of module B, resolve it against B's API registry and fail if absent.

---

### Explicitly do NOT change these two

Both were reported as conflicts during the build. Both turned out to be defects in the
*consuming* skill files, which have since been corrected there. The generator is right:

1. **Native `BOOLEAN` columns.** SEC/FIN/MDL emit `is_active_fl BOOLEAN NOT NULL DEFAULT TRUE`.
   That is correct for PostgreSQL. The consuming skill implied every boolean needed a numeric
   converter; it now reads "native `BOOLEAN` → no converter; numeric → converter;
   `CHAR(1)` → converter". Do not switch to `SMALLINT` for uniformity with the older modules.
2. **`UQ_` unique-constraint naming.** Every db-script from this generator uses `UQ_<TABLE>_<DESC>`,
   and so does all existing built code. The consuming skill said `UK_`; the skill was wrong and
   has been corrected. Do not rename to `UK_`.

---

### Acceptance — how to know each fix landed

Regenerate SEC, FIN and MDL and assert:

| # | Assertion |
|---|---|
| P0 | The ALIGN block reports a real finding when any assertion below is violated — verify by deliberately reintroducing the `grant_at` typo and confirming it fails |
| P1 | `grep -c 'CREATE SEQUENCE'` equals the table count; `grep -c 'GENERATED ALWAYS AS IDENTITY'` is 0; no `NO CACHE` anywhere |
| P2 | Every path in `manifest.json` and `execution-state.json` resolves to an existing directory; every cited `ADR-*` file exists; `manifest.json` and `execution-state.json` name the same test-plan path |
| P3 | The declared error-code format string matches the shape of every emitted catalog code; the DBF→column cross-check script above prints nothing |
| P4 | Every `RULE-*` either resolves its input data to an `ENT`/`DBF` or carries an explicit deferral marker; every cross-module API reference resolves in the target module's registry |

---

### One question worth answering before you start

Three of these defects (P1, P3.1, and the `erp/` prefix in P2) look like the same underlying
cause: a template or profile value that was changed for a newer generation without updating
everything downstream of it, and without a test that would have noticed. If that is right, the
durable fix is a regression test over a golden module output — one that would have failed on
all three — rather than three separate patches. Consider whether that is cheaper than fixing
them individually, given a fourth and fifth module are coming.
