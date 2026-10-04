# Prompt — fix the api-docs generator (module-agnostic)

Paste everything below the line as the first message of a new session, in the **backend** repo.

---

Fix the api-docs generator at `governance/governance-tools/api-doc-generator/` in
`/Users/ezzat/my project/backend`. Seven deficiencies were found by a team that built 12 screens
and 40 governed test cases against one module's generated docs. They are analysed, ranked and
costed in `governance/project-artifacts/API-DOCS-GENERATOR-IMPROVEMENTS-2026-09-23.md`.
**Read that report in full before you touch anything** — it names the file and the reachability of
each fix, and it says which item not to do.

## The prime directive: this tool is MODULE-AGNOSTIC

It generates for **any** module from the running backend plus the Java source. Its whole value is
that it derives, never guesses. Every change you make must hold for a module nobody has written
yet.

Concretely, and these are not hypotheticals — each one is a real trap this codebase already
contains:

- **Never hardcode a module code, package, table prefix, entity or file name.** `discovery.py`
  auto-discovers all of it. Preserve that. If your change needs a new fact, discover it.
- **Never assume a permission-constant naming convention.** Most modules spell them `PERM_<MOD>_<SCREEN>_<ACTION>`, but `CU` uses **`CONFIG_CREATE`** — no `PERM_` prefix at all. A regex or filter keyed on `PERM_` silently drops that module.
- **Never assume one spelling of a Java construct.** The bug you are fixing first exists precisely because the tool assumed one. Handle the variants that exist, and write the test for both.
- **Never assume a module has any given feature.** A module may have no permissions (some use only `isAuthenticated()`), no pagination, no business error codes, no search endpoints. "Absent" must be reportable as *legitimately absent* and distinguishable from *failed to extract* — that distinction is the single most valuable thing you will add.
- **Never make the output depend on iteration order** of a dict, a directory listing, or a springdoc response.

## The verified baseline — measure against this, and do not trust it blindly

Permission rows published in api-docs vs `@PreAuthorize(` annotations in each module's source,
measured today:

| Module | endpoint files | permission rows published | `@PreAuthorize(` in source |
|---|---|---|---|
| CU | 1 | **0** | 5 |
| NOTIF | 5 | **0** | 15 |
| FILE | 3 | **0** | 11 |
| SEC | 11 | 27 | 34 |
| MDL | 3 | 11 | 11 |
| FIN | 9 | **0** | 43 |

Four of six modules publish **no permission and therefore no 403**, because
`extractors/security_extractor.py:42` is

```python
PREAUTH_RE = re.compile(r'@(?:PreAuthorize|Secured)\s*\(\s*"([^"]*)"\s*\)')
```

which requires a single quoted string followed immediately by `)`. FIN, CU and others write the
annotation as a multi-line **string concatenation**:

```java
@PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
    + ".PERM_FIN_ACCOUNT_LEDGER_VIEW)")
```

SEC and MDL use the single-line form, which is why only they work. Note that SEC is **27 of 34**,
so even the "working" module is partial — find out why before assuming your fix is complete, and
verify some of NOTIF's and FILE's zeros are legitimate `isAuthenticated()` rather than a second
unhandled spelling. **Re-measure everything yourself; do not cite my numbers as evidence.**

## Do the work in this order

The report ranks these by value over cost. Follow that order, and land each one separately so a
regression can be bisected.

1. **`@PreAuthorize` / `@Secured` parsing** — accept the concatenated form (any number of parts,
   any whitespace and newlines between them), the single-line form, and any other spelling you
   find in the six modules' source. Join the parts, then resolve the constant as the extractor
   already does. Do not special-case a module. Do not assume the holder class, the constant prefix,
   or that the expression is `hasAuthority(...)` at all — `isAuthenticated()` is a valid value that
   is not a permission, and the two must not be conflated in the output.
2. **Determinism** — the same running backend regenerated twice must produce byte-identical files.
   The known instance is springdoc emitting `Page<T>`'s properties in a different order between JVM
   runs, surfacing in `dto_extractor.find_page_envelope`. Sort at every point where you consume a
   mapping or a directory listing, not only that one. This matters more than it looks: a tool whose
   output churns trains reviewers to wave through api-docs diffs, which is how a real contract drift
   gets merged.
3. **Bind business error codes to the endpoints that raise them.** Today codes appear in the shared
   error table bound to nothing, so a client cannot learn which endpoint returns which. The
   controller→service→domain delegate walk already exists in `discovery.py`; the report identifies
   where the association is discarded. Walk the real call graph. If a code cannot be attributed with
   certainty, **leave it in the unbound table and say it is unbound** — never attribute it on a
   guess.
4. **State what `fieldErrors[].field` actually carries.** The shared handler falls back from a form
   path to the error code, so the same schema field means two different things depending on the
   path. Document the real semantics; the report cites the handler line and the Javadoc that states
   the fallback.
5. **Unregistered contract ids** — the tool already produces a "declared vs served" drift table.
   The ids are present in the controllers' own Javadoc, so it can quote a served endpoint's
   **claimed** id and mark it unregistered. That is a signpost, not a registration: allocating an id
   belongs to the factory's API Registry, which is read-only here.
6. **Uniqueness invariants** — reachable from `@Table(uniqueConstraints=…)` and from Flyway
   `CREATE UNIQUE INDEX`, including partial indexes with a `WHERE` clause that no JPA-only reader
   can see. Lowest value of the six, and the most likely to produce a wrong statement, so do it last
   and only if the earlier items landed cleanly.

**Do NOT do item 7** (inferring how a response-only field gets its value by scanning migrations).
The report explains why: it would search a guessed column name and try to distinguish a seed from a
schema change, and a wrong citation is worse than silence. Ship at most a structural
"response-only, absent from every request schema" label, which is derived rather than inferred. The
real remedy is one sentence in the backend's own `@Schema`, which is not your change to make.

## Add the regression guard, and make it fail

Add `--function check`: everything `review` reports, plus a **non-zero exit** on

- any drift row (declared vs served),
- any rendered file differing from what is on disk,
- two `build_document()` runs rendering differently,
- a whole-module "silent empty" ratio, expressed as a ratio and never as a bare count — e.g.
  `permissions: 0/43 endpoints, 43 @PreAuthorize in source → FAIL`,
- a business-error walk that attributed nothing while the module's source throws catalog codes.

A count nobody reads is how a 0-of-43 condition survived. Ratios with an expected side fail loudly.

Waivers must be **explicit, per-module and written down** — a module that legitimately has no
permissions declares that once, in a file, with a reason. Never satisfy `check` by deleting or
loosening an assertion.

## Add tests — the tool has none today

Unit-test each extractor against fixture Java that carries **both** spellings of every construct
whose alternative is documented in a code comment as having broken something once: single-line and
concatenated `@PreAuthorize`, the two ways status mappings are registered, and a literal error code
versus a constant reference. The report lists them. A fixture that only contains the spelling the
regex already handles is worse than no test, because it certifies the bug.

## Verification protocol — this is the part that proves module-agnosticism

Before you change anything, with the backend running, capture a **baseline** by generating for
**every** module that has api-docs today — CU, NOTIF, FILE, SEC, MDL, FIN — into a scratch copy, not
over the real files. After each numbered item above, regenerate all six and diff against the
baseline. Then, for every changed line, state which of these it is:

- an **intended fix** (say which item, and why the new output is right),
- a **regression** (fix it),
- or **churn** (then item 2 is not done).

A change that improves one module and silently alters another is a failure even if the other looks
fine. Report the six diffs, not a summary of them.

Then run `--function check` on all six and report each verdict. `check` failing on a genuine
outstanding problem is a correct result — say so rather than weakening it.

## Constraints

- Read the repo's `CLAUDE.md` in full first and obey it, including its comment discipline: one line
  per file at the top, one inline line only where the code cannot say it itself, `⚠` on the
  genuinely dangerous ones. No dividers, no ALL-CAPS headings inside comments, no essays. Delete a
  false comment rather than leaving it.
- **The generator derives; it never invents.** No change may make it infer, guess, or fill a gap
  with a plausible value. If a fact is not reachable from the running application or the source,
  the honest output is to say it is not available — and to say whether that is "absent by design"
  or "could not extract".
- `governance/shared/analysis/**` is the factory's partition and is **read-only** here. `api-docs/`
  is this track's to write, but **only by regenerating** — never hand-edit generated output, and
  never hand-fix a doc to make a test pass.
- Do not change any Java source, migration or annotation as part of this task. Where the honest
  remedy is a backend change (item 7's `@Schema` sentence, a missing `SecurityScheme`, a `200`
  documented where the service returns `CREATED`), **record it** and leave it.
- Fix the human-facing instructions too: `.claude/commands/generate-api-docs.md` STEP 3 blames the
  source for what is a generator regex bug, and calls a 38-of-38 condition with a six-line fix a
  "known backend gap" — which reads as "expected" and sent a diligent reader to file the issue in
  the wrong repository. Each symptom row should name candidate causes on **both** sides and demand a
  filed item with an owner. Any symptom a human is asked to notice every run belongs in `check`, not
  in prose.
- Do not run `git commit` or `git push`. Leave everything in the working tree and report what you
  changed.

## Report back

- Per item: what you changed, the file, and the measured before/after (the ratio table above,
  re-measured).
- The six per-module diffs, each line classified as intended fix / regression / churn.
- `--function check` verdicts for all six modules, including any legitimate failure.
- The tests you added and what each would have caught.
- Anything you found that contradicts the report or this prompt — say so plainly rather than
  working around it. Both were written from one module's evidence, and a tool that serves seven
  will have cases neither of us saw.
- Every backend-side or factory-side remedy you found and deliberately did not apply.
