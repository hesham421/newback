# BACKEND TEST PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Track : backend   Plan : test
Scope  : **module** — modules MDL (`gov.py run-standalone test-gen --module MDL`)
Sources: `_state/current-srs.md` (v1) · `_state/current-backend-execution-plan.md` (v1) ·
         `_state/current-registry-srs.md` (v1) · `_state/current-registry-db.md` (v1)
Framework : **agnostic** (`profile.stack.testing`). Every block below is the framework-agnostic
            TC form; no tool, annotation, fixture or file layout is named anywhere in this plan.
            The consumer repo chooses its framework and turns each TC into a test.
REDUCED   : no — the backend-execution-plan is present, so every step binds to a real `API-*`.
TC ids    : TC-MDL-001 … TC-MDL-014 — one continuous module sequence **shared** with
            `frontend-test-plan-mdl.md`, which continues at TC-MDL-015. No id is reused and no
            id is reassigned to a different subject than the one it already carried.
Open ADRs : 2 raised by this run — ADR-MDL-023, ADR-MDL-024 (both ACCEPTED, non-breaking).
            Applied and not re-derived: ADR-MDL-002, ADR-MDL-005, ADR-MDL-007, ADR-SEC-002,
            ADR-FIN-001.
══════════════════════════════════════════════════════════════════

**Id order is AC order, not document order.** `TC-MDL-<seq>` maps 1:1 onto `AC-MDL-<seq>` for
the thirteen acceptance criteria, and TC-MDL-014 is the fourteenth. The two SUBs group by
scenario kind, so the ids are not monotonic inside a SUB — that is the price of never
renumbering, and the traceability index below is the address. No id changed its subject in this
revision.

**What this revision changed, so a reader of the previous one is not misled.**

- **Scope narrowed from `project` to `module`.** The previous revision was produced at
  `scope: project` (modules MDL, SEC) and carried a `PHASE:INT-XM` block. This run is
  `--module MDL`, so the integration phase is **absent** by the engine's own rule (§2 rule 3):
  a module never gets an integration TC for a module outside the selection, and SEC is not in
  this selection. Nothing about XM-MDL-001 is asserted or denied here; it is simply not this
  run's concern.
- **TC-MDL-014 is retargeted, keeping its id** (ADR-MDL-024). It described "SEC unreachable
  during owner-module validation" and cited a foreign endpoint id. `profile.conventions.module_interface`
  is `in_process`, so there is no hop to make unreachable — backend-execution-plan PHASE 6
  (INT-R) says so in as many words: "there is no HTTP-level way to simulate 'SEC unreachable',
  and no test should try to". The test is retargeted to the real untested edge RULE-MDL-001's
  own Test-Hint names. This is **PF-MDL-005** (frontend-execution-plan, API SURFACE) applied,
  not a decision taken here.
- **Test data is the AC's own.** Every value below is one the SRS names — `PAYMENT_METHOD`,
  `FIN`, `SEC`, `XYZ`, `SHIPPING_MODE`, `CASH`, `CHEQUE`, `TRANSFER`, `NO_SUCH_KEY` and the
  labels beside them. The previous revision invented `ZZZ`, `TEST_TYPE`, `ACCOUNT_TYPE`,
  `USER_STATUS`, `NEW_VAL` and `NOT_A_REAL_KEY`; invented business data is forbidden (§3 rule 5)
  and none survives here.
- **Every asserted message is quoted in both languages**, character-perfect from the SRS, where
  the previous revision cited only the catalog code.
- **TC-MDL-003 no longer submits a `key`.** Its step 2 ("attempt to also pass a key value")
  described a branch that cannot exist: `key` is absent from `LookupTypeUpdateRequest` and from
  QR-MDL-003's statement, so a submitted key is ignored, never rejected, and RULE-MDL-003 raises
  no catalog row (backend-execution-plan, RULE-MDL-003 block).

<!-- PHASE:TEST-PLAN-BE:START traces=REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013 -->
## PHASE — TEST-PLAN-BE

TC count is 14, above the split threshold of 12, so this phase is split into the two groups the
engine names: rule-driven scenarios (violations and state transitions) and endpoint-driven
scenarios (happy paths, listing, ordering, grouping). Every TC sits inside one of them.

<!-- SUB:RULE-SCENARIOS:START traces=REQ-MDL-002,REQ-MDL-004,REQ-MDL-007,REQ-MDL-009,REQ-MDL-012,AC-MDL-002,AC-MDL-004,AC-MDL-007,AC-MDL-009,AC-MDL-012 -->
### SUB — RULE-SCENARIOS

The ACs whose Then names a `RULE-*` violation or a state transition. Each one asserts the
catalog code and both message strings, or the stored state the rule governs.

<!-- TC:TC-MDL-002:START traces=AC-MDL-002,REQ-MDL-002,API-MDL-002 -->
### TC-MDL-002 — an unregistered owner module code is refused
Derived from : AC-MDL-002  (REQ-MDL-002)
Exercises    : API-MDL-002 POST /api/v1/mdl/lookup-types
Rule / code  : RULE-MDL-001 → MDL-409-MODULE-NOT-REGISTERED (409)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a registrar holding PERM_MDL_LOOKUPS_CREATE (gated by PERM_MDL_LOOKUPS_VIEW); the
               module code `XYZ` has no ModuleRegistry row in SEC (ENT-SEC-004)
Steps        : 1. POST `/api/v1/mdl/lookup-types` with `LookupTypeCreateRequest` carrying
                  key `SHIPPING_MODE`, ownerModuleCode `XYZ`, and both names
Expected     : 409 · `LocalizedException → {code, messageAr, messageEn}` with code
               MDL-409-MODULE-NOT-REGISTERED —
               ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
               en: "The owning module is not registered in the Security module".
               No lookup type row is stored at all — a subsequent search for key `SHIPPING_MODE`
               (API-MDL-001) returns an empty page.
Test data    : key `SHIPPING_MODE`, ownerModuleCode `XYZ` (AC-MDL-002's own values)
<!-- TC:TC-MDL-002:END -->

<!-- TC:TC-MDL-004:START traces=AC-MDL-004,REQ-MDL-004,API-MDL-004,API-MDL-011 -->
### TC-MDL-004 — deactivating a type keeps its values and hides them from consumers
Derived from : AC-MDL-004  (REQ-MDL-004)
Exercises    : API-MDL-004 DELETE /api/v1/mdl/lookup-types/{id}  ·  API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-004 → MDL-404-TYPE-KEY (404) on the consumer read
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active lookup type with key `PAYMENT_METHOD` holding three active values; the
               caller holds PERM_MDL_LOOKUPS_DELETE, gated by PERM_MDL_LOOKUPS_VIEW
Steps        : 1. DELETE `/api/v1/mdl/lookup-types/{id}` for that type
               2. POST `/api/v1/mdl/lookup-types/values/search` filtered by its `lookupTypeId`
               3. GET `/api/v1/mdl/lookups?type=PAYMENT_METHOD`
Expected     : 1. 200 · the type's `isActiveFl` is false
               2. the three values are still stored, each with its own `isActiveFl` untouched —
                  deactivating a type writes nothing to its values
               3. no value of the type is returned: an inactive type answers MDL-404-TYPE-KEY —
                  ar: «لا يوجد نوع لوكب بهذا المفتاح» ·
                  en: "No lookup type exists with this key"
Test data    : type `PAYMENT_METHOD` with three active values (AC-MDL-004's own precondition)
<!-- TC:TC-MDL-004:END -->

<!-- TC:TC-MDL-007:START traces=AC-MDL-007,REQ-MDL-007,API-MDL-006 -->
### TC-MDL-007 — a duplicate code under the same type is refused
Derived from : AC-MDL-007  (REQ-MDL-007)
Exercises    : API-MDL-006 POST /api/v1/mdl/lookup-types/{id}/values
Rule / code  : RULE-MDL-002 → MDL-409-VALUE-DUP (409)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: the type `PAYMENT_METHOD` already holds a value whose code is `CASH`
Steps        : 1. POST `/api/v1/mdl/lookup-types/{id}/values` with a second
                  `LookupValueCreateRequest` carrying code `CASH` under the same type
Expected     : 409 · MDL-409-VALUE-DUP —
               ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
               en: "This code is already used within this type".
               The type's value count is exactly what it was before the request (AC-MDL-007:
               «ويبقى عدد قيم النوع كما كان قبل الطلب»), confirmed by re-running API-MDL-005.
Test data    : type `PAYMENT_METHOD`, code `CASH` (already held)
<!-- TC:TC-MDL-007:END -->

<!-- TC:TC-MDL-009:START traces=AC-MDL-009,REQ-MDL-009,API-MDL-008,API-MDL-011,API-MDL-005 -->
### TC-MDL-009 — a deactivated value leaves the consumer read and stays in the management read
Derived from : AC-MDL-009  (REQ-MDL-009)
Exercises    : API-MDL-008 DELETE /api/v1/mdl/lookup-values/{id}  ·  API-MDL-011 GET /api/v1/mdl/lookups
               ·  API-MDL-005 POST /api/v1/mdl/lookup-types/values/search
Rule / code  : — · RULE-MDL-004 is the read-side reason the value disappears from step 2
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active value whose code is `CHEQUE`, under the active type `PAYMENT_METHOD`
Steps        : 1. DELETE `/api/v1/mdl/lookup-values/{id}` for `CHEQUE`
               2. GET `/api/v1/mdl/lookups?type=PAYMENT_METHOD`
               3. POST `/api/v1/mdl/lookup-types/values/search` filtered by the type's `lookupTypeId`
Expected     : 1. 200 · the value's `isActiveFl` is false
               2. `CHEQUE` is absent from the consumer read
               3. `CHEQUE` is present in the management detail read — the endpoint the detail
                  pane of the generic lookups screen calls (AC-MDL-009: «وتبقى معروضة في الجزء
                  التفصيلي للشاشة العامة»)
Test data    : value `CHEQUE` under type `PAYMENT_METHOD` (AC-MDL-009's own values)
<!-- TC:TC-MDL-009:END -->

<!-- TC:TC-MDL-012:START traces=AC-MDL-012,REQ-MDL-012,API-MDL-011 -->
### TC-MDL-012 — an unknown key is a not-found, never an empty success
Derived from : AC-MDL-012  (REQ-MDL-012)
Exercises    : API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-004 → MDL-404-TYPE-KEY (404)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: no lookup type on the platform carries the key `NO_SUCH_KEY`
Steps        : 1. GET `/api/v1/mdl/lookups?type=NO_SUCH_KEY` as a consuming module's service
                  account (VIEW on MDL_LOOKUPS)
Expected     : 404 · MDL-404-TYPE-KEY —
               ar: «لا يوجد نوع لوكب بهذا المفتاح» ·
               en: "No lookup type exists with this key".
               The answer is **not** a 200 carrying an empty list — the two outcomes are
               distinguished by requirement (AC-MDL-012: «ولا يُعاد ردّ ناجح بقائمة فارغة»).
Test data    : key `NO_SUCH_KEY` (AC-MDL-012's own value)
<!-- TC:TC-MDL-012:END -->

TC-MDL-014 below is this plan's one TC that is not the 1:1 twin of an AC. It derives from
AC-MDL-002's own rule, RULE-MDL-001, whose **Test-Hint** states the edge in the SRS itself:
«الفحص عند الإنشاء وحده؛ إلغاء تسجيل وحدة في الأمان لاحقًا لا يُبطل أنواعها القائمة». Nothing
is invented for it, and it replaces — under the same id — the unconstructible
"SEC unreachable" test the previous revision carried (PF-MDL-005, ADR-MDL-024).

<!-- TC:TC-MDL-014:START traces=AC-MDL-002,REQ-MDL-002,API-MDL-001,API-MDL-003,API-MDL-011 -->
### TC-MDL-014 — deregistering the owner module later does not invalidate its existing types
Derived from : AC-MDL-002  (REQ-MDL-002) — through RULE-MDL-001's own Test-Hint
Exercises    : API-MDL-001 POST /api/v1/mdl/lookup-types/search · API-MDL-003 PUT /api/v1/mdl/lookup-types/{id}
               · API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-001 → no code is expected on any step: the rule fires `on create` alone
Scenario     : STATE · data class EDGE · language —
Preconditions: a lookup type with key `PAYMENT_METHOD` and ownerModuleCode `FIN` was accepted
               while `FIN` held a ModuleRegistry row in SEC, and holds active values; `FIN`'s
               registry row is then removed from SEC (ENT-SEC-004), so the code no longer
               resolves there
Steps        : 1. POST `/api/v1/mdl/lookup-types/search` filtered by ownerModuleCode `FIN`
               2. PUT `/api/v1/mdl/lookup-types/{id}` with revised names
               3. GET `/api/v1/mdl/lookups?type=PAYMENT_METHOD`
Expected     : 1. 200 · the type is still returned, `isActiveFl` still true, `ownerModuleCode`
                  still the stored `FIN` — nothing re-reads SEC on a search
               2. 200 · the rename succeeds; no code path on the update re-checks SEC
               3. 200 · the type's active values are returned as before — the type is usable
Test data    : type `PAYMENT_METHOD`, ownerModuleCode `FIN` (AC-MDL-001/AC-MDL-002's own values);
               the SEC-side deregistration is a precondition of the target module's data, not a
               value this module supplies
<!-- TC:TC-MDL-014:END -->
<!-- SUB:RULE-SCENARIOS:END -->

<!-- SUB:API-SCENARIOS:START traces=REQ-MDL-001,REQ-MDL-003,REQ-MDL-005,REQ-MDL-006,REQ-MDL-008,REQ-MDL-010,REQ-MDL-011,REQ-MDL-013,AC-MDL-001,AC-MDL-003,AC-MDL-005,AC-MDL-006,AC-MDL-008,AC-MDL-010,AC-MDL-011,AC-MDL-013 -->
### SUB — API-SCENARIOS

The endpoint-driven ACs: the happy paths, the two ordered reads and the grouped browse.

<!-- TC:TC-MDL-001:START traces=AC-MDL-001,REQ-MDL-001,API-MDL-002 -->
### TC-MDL-001 — register a lookup type, happy path
Derived from : AC-MDL-001  (REQ-MDL-001)
Exercises    : API-MDL-002 POST /api/v1/mdl/lookup-types
Rule / code  : RULE-MDL-001 (satisfied — `FIN` is registered in SEC) → no error expected
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a registrar holding PERM_MDL_LOOKUPS_CREATE, gated by PERM_MDL_LOOKUPS_VIEW; no
               lookup type on the platform carries the key `PAYMENT_METHOD`; `FIN` has a
               ModuleRegistry row in SEC (ENT-SEC-004)
Steps        : 1. POST `/api/v1/mdl/lookup-types` with `LookupTypeCreateRequest` carrying
                  key `PAYMENT_METHOD`, ownerModuleCode `FIN`, nameAr «طريقة الدفع»,
                  nameEn "Payment method"
Expected     : 201 · `ApiResponse<LookupTypeResponse>` — `key = PAYMENT_METHOD`,
               `ownerModuleCode = FIN`, `isActiveFl = true`, with `createdBy` and `createdAt`
               written by the system and absent from the request; message —
               ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved."
Test data    : key `PAYMENT_METHOD`, ownerModuleCode `FIN`, names «طريقة الدفع» / "Payment method"
<!-- TC:TC-MDL-001:END -->

<!-- TC:TC-MDL-003:START traces=AC-MDL-003,REQ-MDL-003,API-MDL-003 -->
### TC-MDL-003 — the names change and the key does not
Derived from : AC-MDL-003  (REQ-MDL-003)
Exercises    : API-MDL-003 PUT /api/v1/mdl/lookup-types/{id}
Rule / code  : RULE-MDL-003 → no catalog row exists and none can be raised: `key` is absent from
               `LookupTypeUpdateRequest` and from QR-MDL-003's statement
Scenario     : HAPPY · data class VALID · language —
Preconditions: an existing lookup type whose key is `PAYMENT_METHOD` and whose names are
               «طريقة الدفع» / "Payment method"; the caller holds PERM_MDL_LOOKUPS_UPDATE
Steps        : 1. PUT `/api/v1/mdl/lookup-types/{id}` with `LookupTypeUpdateRequest` carrying
                  nameAr «وسيلة الدفع» and nameEn "Payment means" — the request carries no `key`
                  field, because the schema has none
Expected     : 200 · `nameAr` and `nameEn` hold the submitted values; `key` is still
               `PAYMENT_METHOD`; `ownerModuleCode` is unchanged; `updatedBy` and `updatedAt` are
               filled by the system
Test data    : key `PAYMENT_METHOD`, revised names «وسيلة الدفع» / "Payment means"
<!-- TC:TC-MDL-003:END -->

<!-- TC:TC-MDL-005:START traces=AC-MDL-005,REQ-MDL-005,API-MDL-005 -->
### TC-MDL-005 — the detail is confined to the selected type, active and inactive alike
Derived from : AC-MDL-005  (REQ-MDL-005)
Exercises    : API-MDL-005 POST /api/v1/mdl/lookup-types/values/search
Rule / code  : — · RULE-MDL-004 governs the consumer read, not this management read
Scenario     : HAPPY · data class VALID · language —
Preconditions: the type `PAYMENT_METHOD` holds four values with `sortOrder` 1, 2, 3 and 4, one
               of them deactivated; a second lookup type holds two values of its own
Steps        : 1. POST `/api/v1/mdl/lookup-types/values/search` with `lookupTypeId` = the id of
                  `PAYMENT_METHOD` and no `code` filter
Expected     : 200 · a bare list (not a page) of all four values of `PAYMENT_METHOD` — the three
               active and the deactivated one alike — ordered ascending by `sortOrder`; no value
               of the second type appears in the result
Test data    : `PAYMENT_METHOD` with four values at ranks 1–4, one deactivated; a second type
               with two values (AC-MDL-005's own precondition)
<!-- TC:TC-MDL-005:END -->

<!-- TC:TC-MDL-006:START traces=AC-MDL-006,REQ-MDL-006,API-MDL-006 -->
### TC-MDL-006 — add a value under the selected type, happy path
Derived from : AC-MDL-006  (REQ-MDL-006)
Exercises    : API-MDL-006 POST /api/v1/mdl/lookup-types/{id}/values
Rule / code  : RULE-MDL-002 (satisfied — no value under this type carries `CASH`) → no error
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: the selected type `PAYMENT_METHOD` holds no value whose code is `CASH`; the
               caller holds PERM_MDL_LOOKUPS_CREATE
Steps        : 1. POST `/api/v1/mdl/lookup-types/{id}/values` with `LookupValueCreateRequest`
                  carrying code `CASH`, nameAr «نقدًا», nameEn "Cash", sortOrder 1
Expected     : 201 · the value is stored under `PAYMENT_METHOD` with `code = CASH`,
               `sortOrder = 1` and `isActiveFl = true`; message —
               ar: «تم حفظ القيمة.» · en: "The value has been saved."
Test data    : code `CASH`, names «نقدًا» / "Cash", sortOrder 1 (AC-MDL-006's own values)
<!-- TC:TC-MDL-006:END -->

<!-- TC:TC-MDL-008:START traces=AC-MDL-008,REQ-MDL-008,API-MDL-007 -->
### TC-MDL-008 — the labels and the rank change and the code does not
Derived from : AC-MDL-008  (REQ-MDL-008)
Exercises    : API-MDL-007 PUT /api/v1/mdl/lookup-values/{id}
Rule / code  : RULE-MDL-002 → nothing can raise it on this path: `code` is absent from
               `LookupValueUpdateRequest`
Scenario     : HAPPY · data class VALID · language —
Preconditions: an existing value under `PAYMENT_METHOD` whose code is `CASH` and whose
               `sortOrder` is 1; the caller holds PERM_MDL_LOOKUPS_UPDATE
Steps        : 1. PUT `/api/v1/mdl/lookup-values/{id}` with `LookupValueUpdateRequest` carrying
                  nameAr «نقد», nameEn "Cash payment", sortOrder 2
Expected     : 200 · `nameAr`, `nameEn` and `sortOrder = 2` hold the submitted values; `code` is
               still `CASH`; `lookupTypeId` is unchanged; `updatedBy` and `updatedAt` are filled
Test data    : value `CASH`, revised names «نقد» / "Cash payment", sortOrder 2
<!-- TC:TC-MDL-008:END -->

<!-- TC:TC-MDL-010:START traces=AC-MDL-010,REQ-MDL-010,API-MDL-009,API-MDL-011 -->
### TC-MDL-010 — the ranks follow the submitted order
Derived from : AC-MDL-010  (REQ-MDL-010)
Exercises    : API-MDL-009 PATCH /api/v1/mdl/lookup-types/{id}/values/reorder
               · API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : — · no `RULE-*` applies to a reorder
Scenario     : HAPPY · data class VALID · language —
Preconditions: a type holding exactly three active values — `CASH`, `CHEQUE` and `TRANSFER` —
               with `sortOrder` 1, 2 and 3; the caller holds PERM_MDL_LOOKUPS_UPDATE
Steps        : 1. PATCH `/api/v1/mdl/lookup-types/{id}/values/reorder` with
                  `LookupValueReorderRequest` carrying the ids of `TRANSFER`, `CASH`, `CHEQUE`
                  in that order — the type's complete value set, no rank number supplied
               2. GET `/api/v1/mdl/lookups?type=PAYMENT_METHOD`
Expected     : 1. 200 · `sortOrder` is 1 for `TRANSFER`, 2 for `CASH` and 3 for `CHEQUE` — each
                  value's rank is its position in the submitted list
               2. the values come back in that order
Test data    : `CASH`, `CHEQUE`, `TRANSFER` at ranks 1–3, resubmitted as `TRANSFER`, `CASH`,
               `CHEQUE` (AC-MDL-010's own values)
<!-- TC:TC-MDL-010:END -->

<!-- TC:TC-MDL-011:START traces=AC-MDL-011,REQ-MDL-011,API-MDL-011 -->
### TC-MDL-011 — the active values only, in order
Derived from : AC-MDL-011  (REQ-MDL-011)
Exercises    : API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-004 (satisfied — the type is active) → no error expected
Scenario     : HAPPY · data class VALID · language —
Preconditions: an active type with key `PAYMENT_METHOD` holding two active values at `sortOrder`
               1 and 2 and a third value that is deactivated; the caller is a consuming module's
               service account holding VIEW on MDL_LOOKUPS
Steps        : 1. GET `/api/v1/mdl/lookups?type=PAYMENT_METHOD`
Expected     : 200 · exactly the two active values, ordered ascending by `sortOrder`, each
               carrying `code`, `nameAr` and `nameEn`; the deactivated value is absent from the
               response
Test data    : `PAYMENT_METHOD` with two active values (ranks 1, 2) and one deactivated
<!-- TC:TC-MDL-011:END -->

<!-- TC:TC-MDL-013:START traces=AC-MDL-013,REQ-MDL-013,API-MDL-010 -->
### TC-MDL-013 — the registry groups by owner module
Derived from : AC-MDL-013  (REQ-MDL-013)
Exercises    : API-MDL-010 POST /api/v1/mdl/lookup-types/by-owner/search
Rule / code  : — · the active-only narrowing is REQ-MDL-013's own text, not a rule
Scenario     : HAPPY · data class VALID · language —
Preconditions: three active lookup types — two whose `ownerModuleCode` is `FIN`, one whose
               `ownerModuleCode` is `SEC`; the caller holds PERM_MDL_TYPE_REGISTRY_VIEW
Steps        : 1. POST `/api/v1/mdl/lookup-types/by-owner/search` with no filter
Expected     : 200 · two groups — `FIN` carrying its two types and `SEC` carrying its one —
               with every row carrying `key`, `nameAr`, `nameEn` and `ownerModuleCode`
Test data    : three active types, owners `FIN` (×2) and `SEC` (AC-MDL-013's own precondition)
<!-- TC:TC-MDL-013:END -->
<!-- SUB:API-SCENARIOS:END -->
<!-- PHASE:TEST-PLAN-BE:END -->

## TC TRACEABILITY INDEX

**AC → TC**

| AC | TC | AC | TC |
|---|---|---|---|
| AC-MDL-001 | TC-MDL-001 | AC-MDL-008 | TC-MDL-008 |
| AC-MDL-002 | TC-MDL-002, TC-MDL-014 | AC-MDL-009 | TC-MDL-009 |
| AC-MDL-003 | TC-MDL-003 | AC-MDL-010 | TC-MDL-010 |
| AC-MDL-004 | TC-MDL-004 | AC-MDL-011 | TC-MDL-011 |
| AC-MDL-005 | TC-MDL-005 | AC-MDL-012 | TC-MDL-012 |
| AC-MDL-006 | TC-MDL-006 | AC-MDL-013 | TC-MDL-013 |
| AC-MDL-007 | TC-MDL-007 | — | — |

**REQ → TC**

| REQ | TC | REQ | TC |
|---|---|---|---|
| REQ-MDL-001 | TC-MDL-001 | REQ-MDL-008 | TC-MDL-008 |
| REQ-MDL-002 | TC-MDL-002, TC-MDL-014 | REQ-MDL-009 | TC-MDL-009 |
| REQ-MDL-003 | TC-MDL-003 | REQ-MDL-010 | TC-MDL-010 |
| REQ-MDL-004 | TC-MDL-004 | REQ-MDL-011 | TC-MDL-011 |
| REQ-MDL-005 | TC-MDL-005 | REQ-MDL-012 | TC-MDL-012 |
| REQ-MDL-006 | TC-MDL-006 | REQ-MDL-013 | TC-MDL-013 |
| REQ-MDL-007 | TC-MDL-007 | — | — |

**API → TC**

| API | TC |
|---|---|
| API-MDL-001 | TC-MDL-002 (the empty-search assertion), TC-MDL-014 |
| API-MDL-002 | TC-MDL-001, TC-MDL-002 |
| API-MDL-003 | TC-MDL-003, TC-MDL-014 |
| API-MDL-004 | TC-MDL-004 |
| API-MDL-005 | TC-MDL-005, TC-MDL-007 (the count assertion), TC-MDL-009 |
| API-MDL-006 | TC-MDL-006, TC-MDL-007 |
| API-MDL-007 | TC-MDL-008 |
| API-MDL-008 | TC-MDL-009 |
| API-MDL-009 | TC-MDL-010 |
| API-MDL-010 | TC-MDL-013 |
| API-MDL-011 | TC-MDL-004, TC-MDL-009, TC-MDL-010, TC-MDL-011, TC-MDL-012, TC-MDL-014 |

**RULE / catalog code → TC**

| RULE | code | TC |
|---|---|---|
| RULE-MDL-001 | MDL-409-MODULE-NOT-REGISTERED (409) | TC-MDL-002 · TC-MDL-014 (the create-only limit, no code raised) |
| RULE-MDL-002 | MDL-409-VALUE-DUP (409) | TC-MDL-007 · TC-MDL-008 (unreachable on the update path, asserted as such) |
| RULE-MDL-003 | — (no catalog row can be raised) | TC-MDL-003 |
| RULE-MDL-004 | MDL-404-TYPE-KEY (404) | TC-MDL-004, TC-MDL-012 · TC-MDL-009, TC-MDL-011 (the read-time exclusion) |

Catalog rows **no TC exercises**, stated rather than left unsaid: MDL-409-TYPE-DUP,
MDL-404-TYPE, MDL-404-VALUE, MDL-400-REORDER-MISMATCH, and the three platform rows
(VALIDATION_ERROR, ACCESS_DENIED, INTERNAL_ERROR). No `AC-*` states any of them, and this engine
derives from the ACs alone — fabricating a TC for a code no acceptance criterion asserts is the
over-engineering the guard of §3 forbids. They are a requirements-side gap if anyone wants them
covered, not a test-generation one. PF-MDL-004 (the reorder invariant) is filed against the
backend track for the same reason: nothing in this plan can test a rejection the service does
not perform.

## COVERAGE

```
AC  covered   13/13   ✓ 0 gaps   (every AC-MDL-001…013 carries ≥1 TC)
REQ covered   13/13   ✓ 0 gaps
API covered   11/11   ✓ 0 gaps   (API-MDL-001…011, each named by ≥1 TC's Exercises line)
TC count      14      ✓ 1.08× the AC count — under the ~2× over-engineering guard
Integration   n/a     scope = module: no INT-XM phase is emitted and none is owed (§2 rule 3)
```
══════════════════════════════════════════════════════════════════
