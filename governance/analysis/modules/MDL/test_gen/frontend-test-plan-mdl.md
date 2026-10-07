# FRONTEND TEST PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Track : frontend   Plan : test
Scope  : **module** — modules MDL (`gov.py run-standalone test-gen --module MDL`)
Sources: `_state/current-srs.md` (v1) · `_state/current-frontend-execution-plan.md` (v1) ·
         `_state/current-registry-srs.md` (v1) · `_state/current-registry-exec-fe.md` (v1)
Framework : **agnostic** (`profile.stack.testing`). Every block below is the framework-agnostic
            TC form; no tool, selector, annotation or file layout is named anywhere in this plan.
            The consumer repo chooses its framework and turns each TC into a test.
REDUCED   : no — the frontend-execution-plan is present, so every step binds to a real `SCR-*`
            and a real route.
TC ids    : TC-MDL-015 … TC-MDL-025 — the continuation of the one module sequence
            `backend-test-plan-mdl.md` opened at TC-MDL-001 and closed at TC-MDL-014. The
            sequence does not restart here and no id is reused across the two files.
Open ADRs : 2 raised by this run — ADR-MDL-023 (this file's AC denominator), ADR-MDL-024.
            Applied and not re-derived: ADR-MDL-002, ADR-MDL-005, ADR-MDL-006, ADR-MDL-007,
            ADR-MDL-012, ADR-MDL-013, ADR-MDL-014, ADR-MDL-015.
══════════════════════════════════════════════════════════════════

**Two ACs have no screen, and that is not a gap (ADR-MDL-023).** AC-MDL-011 and AC-MDL-012 are
the consumer read by key: `API-MDL-011` is bound by the frontend plan and **called by no screen**
— a consuming module's backend performs it over the platform's in-process interface (ADR-MDL-007),
and registry-exec-fe-mdl.md records the same 11-of-13 ratio for exactly this reason. Both are
covered on the backend track (TC-MDL-011, TC-MDL-012). This plan therefore states its AC coverage
against the **screen-bearing** AC set of eleven, and lists the two out-of-track ACs by id rather
than marking them ✗ — a ✗ would assert a missing test for a screen that does not exist.
Module-level AC coverage across both tracks is 13/13.

**No PERMISSION TC appears here**, and none is owed: no `AC-*` in the SRS states a denied VIEW or
a denied action. Action-level grants are readable from no published surface, so the screens render
their affordances and the server's `ACCESS_DENIED` is the authority (ADR-MDL-012, PF-MDL-001) —
there is no acceptance criterion describing either half, and inventing one is out of this engine's
boundary.

**No BOUNDARY TC appears here.** No AC and no RULE this module carries states a numeric limit;
the column widths are field constraints stated in F1/F3, not an AC's assertion.

<!-- PHASE:TEST-PLAN-FE:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-009,AC-MDL-010,AC-MDL-013 -->
## PHASE — TEST-PLAN-FE

TC count is 11, above the split threshold of 8, so this phase is split into the two groups the
engine names: the per-screen flows, and the flows whose assertion spans a state change across
both levels of the composite screen.

Every TC below runs in both languages where a message is asserted: the locale resolves
session → browser → `ar` (`profile.languages.primary`), and the client keys its displayed text on
`error.code` against the module's catalog, which carries the ar and the en wording.

<!-- SUB:UI-FLOWS:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-010,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-010,AC-MDL-013 -->
### SUB — UI-FLOWS

Search, entry, violation-on-screen and grouped-browse flows, one per AC, on the screen each AC's
requirement is traced to.

<!-- TC:TC-MDL-015:START traces=AC-MDL-001,REQ-MDL-001,SCR-MDL-001,API-MDL-002 -->
### TC-MDL-015 — register a lookup type from the generic lookups screen
Derived from : AC-MDL-001  (REQ-MDL-001)
Exercises    : SCR-MDL-001 `/reference-data/lookups/new`  (submits API-MDL-002)
Rule / code  : RULE-MDL-001 (satisfied — `FIN` is in the owner-module list) → no error expected
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a signed-in registrar whose effective menu carries `MDL_LOOKUPS`; no lookup type
               carries the key `PAYMENT_METHOD`; the owner-module select has loaded its options
               through UXD-MDL-001 and offers `FIN`
Steps        : 1. navigate to `/reference-data/lookups`
               2. open the type entry — route `/reference-data/lookups/new`
               3. enter key `PAYMENT_METHOD`, choose `FIN` in the owner-module select, enter
                  nameAr «طريقة الدفع» and nameEn "Payment method"
               4. save — one submit, one call
Expected     : the type is saved active and the success message renders —
               ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved." ·
               the master list shows the new type with its key, both names and owner `FIN`
Test data    : key `PAYMENT_METHOD`, owner `FIN`, names «طريقة الدفع» / "Payment method"
<!-- TC:TC-MDL-015:END -->

<!-- TC:TC-MDL-016:START traces=AC-MDL-002,REQ-MDL-002,SCR-MDL-001,API-MDL-002 -->
### TC-MDL-016 — the unregistered owner module is refused on the form
Derived from : AC-MDL-002  (REQ-MDL-002)
Exercises    : SCR-MDL-001 `/reference-data/lookups/new`  (submits API-MDL-002)
Rule / code  : RULE-MDL-001 → MDL-409-MODULE-NOT-REGISTERED (409)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: the registrar is on the type entry; the submitted owner module code has no
               ModuleRegistry row in SEC at the moment of submit — the case the F3 validator
               leaves to the server, since the select is filled from a list loaded earlier
               (a module deregistered between load and submit)
Steps        : 1. enter key `SHIPPING_MODE` and both names
               2. submit with the owner module code `XYZ`
Expected     : the save is refused and the localized message renders **inline on the
               owner-module field**, routed by the catalog code MDL-409-MODULE-NOT-REGISTERED —
               ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
               en: "The owning module is not registered in the Security module" ·
               no row is added to the master list (AC-MDL-002: «ولا يُخزَّن صفّ نوع البتة»)
Test data    : key `SHIPPING_MODE`, owner module code `XYZ`
<!-- TC:TC-MDL-016:END -->

<!-- TC:TC-MDL-017:START traces=AC-MDL-003,REQ-MDL-003,SCR-MDL-001,API-MDL-003 -->
### TC-MDL-017 — the rename form carries no key field at all
Derived from : AC-MDL-003  (REQ-MDL-003)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId/edit`  (submits API-MDL-003)
Rule / code  : RULE-MDL-003 → no code and no refusal: the rule is expressed by the field's absence
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an existing type with key `PAYMENT_METHOD` and names «طريقة الدفع» /
               "Payment method", reached by selecting it in the master list (the edit route
               hydrates from the row the search query already holds — ADR-MDL-005)
Steps        : 1. open the type editor from the list
               2. observe the key and the owner module code
               3. change the two names to «وسيلة الدفع» and "Payment means" and save
Expected     : 2. `key` and `ownerModuleCode` render read-only — neither is an input — and the
                  rule's own text is stated beside the key: ar: «لا يمكن تعديل مفتاح نوع اللوكب
                  بعد إنشائه» · en: "A lookup type's key cannot be changed after creation"
               3. the save succeeds; the list row shows the two new names and the same key
                  `PAYMENT_METHOD`
Test data    : key `PAYMENT_METHOD`, revised names «وسيلة الدفع» / "Payment means"
<!-- TC:TC-MDL-017:END -->

<!-- TC:TC-MDL-018:START traces=AC-MDL-005,REQ-MDL-005,SCR-MDL-001,API-MDL-005 -->
### TC-MDL-018 — selecting a type confines the detail pane to its values
Derived from : AC-MDL-005  (REQ-MDL-005)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId`  (reads API-MDL-005)
Rule / code  : — · the manager's pane shows what the consumer read hides
Scenario     : HAPPY · data class VALID · language —
Preconditions: the type `PAYMENT_METHOD` holds four values at `sortOrder` 1, 2, 3 and 4, one of
               them deactivated; a second type holds two values of its own
Steps        : 1. on `/reference-data/lookups`, select `PAYMENT_METHOD` in the master list
               2. select the second type
Expected     : 1. the route becomes `/reference-data/lookups/:typeId` and the detail pane lists
                  all four values of `PAYMENT_METHOD` — the active and the deactivated alike,
                  the deactivated one marked inactive — ordered ascending by `sortOrder`, with no
                  value of the second type shown
               2. the pane shows that type's two values and none of `PAYMENT_METHOD`'s
Test data    : `PAYMENT_METHOD` with four values (one deactivated); a second type with two
<!-- TC:TC-MDL-018:END -->

<!-- TC:TC-MDL-019:START traces=AC-MDL-006,REQ-MDL-006,SCR-MDL-001,API-MDL-006 -->
### TC-MDL-019 — add a value from the detail level
Derived from : AC-MDL-006  (REQ-MDL-006)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId/values/new`  (submits API-MDL-006)
Rule / code  : RULE-MDL-002 (satisfied — no value under this type carries `CASH`) → no error
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: `PAYMENT_METHOD` is the selected type and holds no value whose code is `CASH`
Steps        : 1. open the value entry — route `/reference-data/lookups/:typeId/values/new`
               2. enter code `CASH`, nameAr «نقدًا», nameEn "Cash", sort order 1
               3. save — one submit, one call; the parent type is the route's id, never typed
Expected     : the value is saved active under `PAYMENT_METHOD` and the success message renders —
               ar: «تم حفظ القيمة.» · en: "The value has been saved." ·
               the detail pane shows the new row at rank 1
Test data    : code `CASH`, names «نقدًا» / "Cash", sort order 1
<!-- TC:TC-MDL-019:END -->

<!-- TC:TC-MDL-020:START traces=AC-MDL-007,REQ-MDL-007,SCR-MDL-001,API-MDL-006 -->
### TC-MDL-020 — a duplicate code is refused inline on the code field
Derived from : AC-MDL-007  (REQ-MDL-007)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId/values/new`  (submits API-MDL-006)
Rule / code  : RULE-MDL-002 → MDL-409-VALUE-DUP (409)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: the selected type `PAYMENT_METHOD` already holds a value whose code is `CASH`
Steps        : 1. open the value entry under the same type
               2. enter code `CASH` and leave the field (the uniqueness check runs on blur,
                  scoped to the selected parent type)
               3. complete the labels and the rank and submit
Expected     : the save is refused and the localized message renders **inline on `code`** —
               ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
               en: "This code is already used within this type" ·
               the detail pane holds exactly the rows it held before the attempt (AC-MDL-007:
               «ويبقى عدد قيم النوع كما كان قبل الطلب»)
Test data    : type `PAYMENT_METHOD`, code `CASH` (already held)
<!-- TC:TC-MDL-020:END -->

<!-- TC:TC-MDL-021:START traces=AC-MDL-008,REQ-MDL-008,SCR-MDL-001,API-MDL-007 -->
### TC-MDL-021 — edit a value's labels and rank, its code read-only
Derived from : AC-MDL-008  (REQ-MDL-008)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId/values/:valueId/edit`
               (submits API-MDL-007)
Rule / code  : RULE-MDL-002 → nothing can raise it here: `code` is not an input on edit
Scenario     : HAPPY · data class VALID · language —
Preconditions: a value under `PAYMENT_METHOD` whose code is `CASH` and whose rank is 1, reached
               from the detail pane (the editor hydrates from the row the pane's query holds)
Steps        : 1. open the value editor from its row
               2. observe the code field
               3. change the labels to «نقد» / "Cash payment", set the sort order to 2, and save
Expected     : 2. `code` renders read-only and is not an input
               3. the save succeeds; the row shows the two new labels at rank 2 and the same
                  code `CASH`, and the pane re-renders in `sortOrder` order
Test data    : value `CASH`, revised labels «نقد» / "Cash payment", sort order 2
<!-- TC:TC-MDL-021:END -->

<!-- TC:TC-MDL-022:START traces=AC-MDL-010,REQ-MDL-010,SCR-MDL-001,API-MDL-009 -->
### TC-MDL-022 — dragging a value submits the type's whole ordered set
Derived from : AC-MDL-010  (REQ-MDL-010)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId`  (submits API-MDL-009)
Rule / code  : — · no `RULE-*` applies to a reorder
Scenario     : HAPPY · data class VALID · language —
Preconditions: the selected type holds exactly three values — `CASH`, `CHEQUE`, `TRANSFER` at
               ranks 1, 2 and 3 — and the detail pane's `code` filter is empty, which is what
               makes the drag handle enabled at all: the pane must show the whole type before a
               reorder may be submitted
Steps        : 1. drag `TRANSFER` above `CASH` and `CHEQUE`
               2. let the reorder settle
Expected     : the ordered set `TRANSFER`, `CASH`, `CHEQUE` is submitted once, as ids without
               rank numbers; the persisted ranks become 1 for `TRANSFER`, 2 for `CASH` and 3 for
               `CHEQUE`, and the pane renders the persisted order the response carries
Test data    : `CASH`, `CHEQUE`, `TRANSFER` at ranks 1–3, reordered to `TRANSFER`, `CASH`,
               `CHEQUE` (AC-MDL-010's own values)
<!-- TC:TC-MDL-022:END -->

<!-- TC:TC-MDL-023:START traces=AC-MDL-013,REQ-MDL-013,SCR-MDL-002,API-MDL-010,UXD-MDL-001 -->
### TC-MDL-023 — the registry browses the active types grouped by owner
Derived from : AC-MDL-013  (REQ-MDL-013)
Exercises    : SCR-MDL-002 `/reference-data/type-registry`  (reads API-MDL-010)
Rule / code  : — · the active-only narrowing is REQ-MDL-013's own text
Scenario     : HAPPY · data class VALID · language —
Preconditions: a platform administrator whose effective menu carries `MDL_TYPE_REGISTRY`; three
               active lookup types — two owned by `FIN`, one owned by `SEC`
Steps        : 1. navigate to `/reference-data/type-registry`
               2. apply no filter
Expected     : two owner group sections render — `FIN` carrying its two types and `SEC` carrying
               its one — each type row carrying `key`, both names and the owner module code; the
               group headings are labelled through UXD-MDL-001, falling back to the bare code the
               browse returns when that read is refused, never to free text
Test data    : three active types, owners `FIN` (×2) and `SEC` (AC-MDL-013's own precondition)
<!-- TC:TC-MDL-023:END -->
<!-- SUB:UI-FLOWS:END -->

<!-- SUB:INT-FLOW:START traces=REQ-MDL-004,REQ-MDL-009,AC-MDL-004,AC-MDL-009 -->
### SUB — INT-FLOW

The two module lifecycle flows: a deactivation at either level, and what it changes across the
two screens. Each of the two ACs also has a half that no screen can observe — the consumer read —
which is asserted on the backend track and is named here rather than silently dropped.

<!-- TC:TC-MDL-024:START traces=AC-MDL-004,REQ-MDL-004,SCR-MDL-001,SCR-MDL-002,API-MDL-004 -->
### TC-MDL-024 — deactivating a type leaves its values on the screen and removes it from the registry
Derived from : AC-MDL-004  (REQ-MDL-004)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId` (submits API-MDL-004) ·
               SCR-MDL-002 `/reference-data/type-registry`
Rule / code  : RULE-MDL-004 → the rule's own text is what the confirmation and the state label say
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active type `PAYMENT_METHOD` holding three active values; the caller's menu
               carries both `MDL_LOOKUPS` and `MDL_TYPE_REGISTRY`; the registry currently lists
               the type under `FIN`
Steps        : 1. with the type selected, invoke Deactivate and read the confirmation
               2. confirm
               3. navigate to `/reference-data/type-registry`
Expected     : 1. the confirmation states the consequence before the act — consuming modules stop
                  receiving this type's values, and the key stays reserved because no activate
                  action exists at either level
               2. the type row shows inactive, labelled with the rule's own text —
                  ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive" —
                  and its three values are still listed in the detail pane, unchanged
               3. the type is no longer among the registry's groups: that browse admits active
                  types only
Test data    : type `PAYMENT_METHOD` with three active values, owner `FIN`
               (the consumer-read half of AC-MDL-004's Then has no screen surface and is
               asserted by TC-MDL-004 on the backend track)
<!-- TC:TC-MDL-024:END -->

<!-- TC:TC-MDL-025:START traces=AC-MDL-009,REQ-MDL-009,SCR-MDL-001,API-MDL-008 -->
### TC-MDL-025 — a deactivated value stays visible in the management pane
Derived from : AC-MDL-009  (REQ-MDL-009)
Exercises    : SCR-MDL-001 `/reference-data/lookups/:typeId`  (submits API-MDL-008)
Rule / code  : — · the value's disappearance from the consumer read is RULE-MDL-004's effect
Scenario     : STATE · data class VALID · language —
Preconditions: an active value whose code is `CHEQUE`, under the active type `PAYMENT_METHOD`,
               shown in the detail pane
Steps        : 1. invoke Deactivate on the `CHEQUE` row and read the confirmation
               2. confirm
               3. look for an Activate affordance on the row and at the type level
Expected     : 1. the confirmation states that the code stays reserved under this type and that
                  the act is not reversible from this screen
               2. the row remains in the detail pane, marked inactive (AC-MDL-009: «وتبقى معروضة
                  في الجزء التفصيلي للشاشة العامة»)
               3. no Activate affordance exists at either level — no endpoint is published for it
Test data    : value `CHEQUE` under type `PAYMENT_METHOD`
               (the consumer-read half of AC-MDL-009's Then is asserted by TC-MDL-009 on the
               backend track)
<!-- TC:TC-MDL-025:END -->
<!-- SUB:INT-FLOW:END -->
<!-- PHASE:TEST-PLAN-FE:END -->

## TC TRACEABILITY INDEX

**AC → TC**

| AC | TC | AC | TC |
|---|---|---|---|
| AC-MDL-001 | TC-MDL-015 | AC-MDL-008 | TC-MDL-021 |
| AC-MDL-002 | TC-MDL-016 | AC-MDL-009 | TC-MDL-025 |
| AC-MDL-003 | TC-MDL-017 | AC-MDL-010 | TC-MDL-022 |
| AC-MDL-004 | TC-MDL-024 | AC-MDL-011 | — no screen (ADR-MDL-007) → TC-MDL-011, backend |
| AC-MDL-005 | TC-MDL-018 | AC-MDL-012 | — no screen (ADR-MDL-007) → TC-MDL-012, backend |
| AC-MDL-006 | TC-MDL-019 | AC-MDL-013 | TC-MDL-023 |
| AC-MDL-007 | TC-MDL-020 | — | — |

**REQ → TC**

| REQ | TC | REQ | TC |
|---|---|---|---|
| REQ-MDL-001 | TC-MDL-015 | REQ-MDL-008 | TC-MDL-021 |
| REQ-MDL-002 | TC-MDL-016 | REQ-MDL-009 | TC-MDL-025 |
| REQ-MDL-003 | TC-MDL-017 | REQ-MDL-010 | TC-MDL-022 |
| REQ-MDL-004 | TC-MDL-024 | REQ-MDL-011 | — no screen → backend track |
| REQ-MDL-005 | TC-MDL-018 | REQ-MDL-012 | — no screen → backend track |
| REQ-MDL-006 | TC-MDL-019 | REQ-MDL-013 | TC-MDL-023 |
| REQ-MDL-007 | TC-MDL-020 | — | — |

**SCR → TC**

| SCR | route(s) exercised | TC |
|---|---|---|
| SCR-MDL-001 | `/reference-data/lookups` · `/new` · `/:typeId` · `/:typeId/edit` · `/:typeId/values/new` · `/:typeId/values/:valueId/edit` | TC-MDL-015, TC-MDL-016, TC-MDL-017, TC-MDL-018, TC-MDL-019, TC-MDL-020, TC-MDL-021, TC-MDL-022, TC-MDL-024, TC-MDL-025 |
| SCR-MDL-002 | `/reference-data/type-registry` | TC-MDL-023, TC-MDL-024 (the registry assertion) |

**RULE / catalog code → TC**

| RULE | code | TC |
|---|---|---|
| RULE-MDL-001 | MDL-409-MODULE-NOT-REGISTERED (409) | TC-MDL-016 |
| RULE-MDL-002 | MDL-409-VALUE-DUP (409) | TC-MDL-020 · TC-MDL-021 (not raisable on edit, asserted as such) |
| RULE-MDL-003 | — (expressed by the field's absence) | TC-MDL-017 |
| RULE-MDL-004 | — on this track (the code is answered to a calling module, not to a screen) | TC-MDL-024 — the rule's text as the confirmation and the state label |

**UXD → TC** — UXD-MDL-001 (`ownerModuleCode`, owned by SEC) is cited by TC-MDL-015 (the select
that must offer `FIN`), TC-MDL-016 (the field the refusal routes to) and TC-MDL-023 (the group
headings and their fallback). No **integration** TC is derived from it: at `scope: module` the
`INT-UXD` phase is skipped entirely (§2 rule 3), and SEC — the owner module — is not in this
selection. Nothing about UXD-MDL-001 is recorded as a gap here; it is not this run's concern.

## COVERAGE

```
AC  covered   11/11   ✓ 0 gaps   of the screen-bearing AC set (ADR-MDL-023)
                      AC-MDL-011, AC-MDL-012 — out of track, no screen implements them
                      (ADR-MDL-007); both covered by TC-MDL-011 / TC-MDL-012 on the backend
AC  (module)  13/13   ✓ 0 gaps   across both tracks
REQ covered   11/13   REQ-MDL-011, REQ-MDL-012 have no screen — the same two, the same reason
SCR covered    2/2    ✓ each of SCR-MDL-001 and SCR-MDL-002 carries ≥1 TC
TC count       11     ✓ 1.0× the screen-bearing AC count — under the ~2× over-engineering guard
Integration    n/a    scope = module: no INT-UXD phase is emitted and none is owed (§2 rule 3)
```
══════════════════════════════════════════════════════════════════
