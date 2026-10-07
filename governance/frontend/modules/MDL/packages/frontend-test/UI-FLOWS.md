<!-- source: PHASE:TEST-PLAN-FE / SUB:UI-FLOWS -->
<!-- context: TEST-PLAN-FE-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-010, AC-MDL-013, API-MDL-002, API-MDL-003, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-009, API-MDL-010, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-010, REQ-MDL-013, SCR-MDL-001, SCR-MDL-002, UXD-MDL-001 -->
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
