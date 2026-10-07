<!-- source: PHASE:TEST-PLAN-BE / SUB:RULE-SCENARIOS -->
<!-- traces: AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-007, AC-MDL-011, AC-MDL-012, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-006, API-MDL-011, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-007, REQ-MDL-011, REQ-MDL-012 -->
<!-- SUB:RULE-SCENARIOS:START traces=REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-007,REQ-MDL-011,REQ-MDL-012 -->
### SUB — RULE-SCENARIOS

<!-- TC:TC-MDL-002:START traces=AC-MDL-002,REQ-MDL-002,API-MDL-002 -->
### TC-MDL-002 — reject a type for an unregistered owner module
Derived from : AC-MDL-002 (REQ-MDL-002)
Exercises    : API-MDL-002 POST /api/v1/mdl/lookup-types
Rule / code  : RULE-MDL-001 → MDL-409-MODULE-NOT-REGISTERED
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: an owner module code with no ModuleRegistry row in SEC
Steps        : 1. POST {key, ownerModuleCode: "ZZZ", nameAr, nameEn}
Expected     : 409 MDL-409-MODULE-NOT-REGISTERED; no LookupType created
Test data    : ownerModuleCode "ZZZ" (never registered in SEC)
<!-- TC:TC-MDL-002:END -->

<!-- TC:TC-MDL-003:START traces=AC-MDL-003,REQ-MDL-003,API-MDL-003 -->
### TC-MDL-003 — key is immutable after creation
Derived from : AC-MDL-003 (REQ-MDL-003)
Exercises    : API-MDL-003 PUT /api/v1/mdl/lookup-types/{id}
Rule / code  : RULE-MDL-003 → (enforced by DTO shape — no error code, `key` simply absent from the request schema)
Scenario     : STATE · data class VALID · language ALL
Preconditions: an existing LookupType
Steps        : 1. PUT {nameAr, nameEn} (no `key` field in the request DTO at all) — 2. attempt to also pass a `key` value and confirm it is ignored / rejected by schema validation
Expected     : 200; names updated, key unchanged; step 2 either fails schema validation (400) or is silently ignored depending on the consumer's DTO strictness — either way key never changes
Test data    : existing type key "ACCOUNT_TYPE"
<!-- TC:TC-MDL-003:END -->

<!-- TC:TC-MDL-004:START traces=AC-MDL-004,REQ-MDL-004,API-MDL-004 -->
### TC-MDL-004 — deactivating a type excludes its values from reads
Derived from : AC-MDL-004 (REQ-MDL-004)
Exercises    : API-MDL-004 DELETE /api/v1/mdl/lookup-types/{id}
Rule / code  : RULE-MDL-004 → (no error code — enforced on the read side, see TC-MDL-011)
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active LookupType with active values
Steps        : 1. DELETE (deactivate) the type — 2. call API-MDL-011 for its key
Expected     : 1. 200, isActiveFl=false — 2. step 2 returns no values (REQ-MDL-011 no longer returns them)
Test data    : a test-only type with 2 active values
<!-- TC:TC-MDL-004:END -->

<!-- TC:TC-MDL-007:START traces=AC-MDL-007,REQ-MDL-007,API-MDL-006 -->
### TC-MDL-007 — reject a duplicate code within a type
Derived from : AC-MDL-007 (REQ-MDL-007)
Exercises    : API-MDL-006 POST /api/v1/mdl/lookup-types/{id}/values
Rule / code  : RULE-MDL-002 → MDL-409-VALUE-DUP
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a type already holding a value with code "ACTIVE"
Steps        : 1. POST a second value with code "ACTIVE" under the same type
Expected     : 409 MDL-409-VALUE-DUP; no second row created
Test data    : type USER_STATUS, code "ACTIVE" (already exists)
<!-- TC:TC-MDL-007:END -->

<!-- TC:TC-MDL-011:START traces=AC-MDL-011,REQ-MDL-011,API-MDL-011 -->
### TC-MDL-011 — read active values by key, ordered
Derived from : AC-MDL-011 (REQ-MDL-011)
Exercises    : API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-004 (positive path — active type, active values only)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a type "PAYMENT_METHOD" with 2 active and 1 inactive value
Steps        : 1. GET ?type=PAYMENT_METHOD
Expected     : 200; exactly the 2 active values, ordered by sortOrder
Test data    : PAYMENT_METHOD with 3 values (2 active, 1 inactive)
<!-- TC:TC-MDL-011:END -->

<!-- TC:TC-MDL-012:START traces=AC-MDL-012,REQ-MDL-012,API-MDL-011 -->
### TC-MDL-012 — reject an unknown type key
Derived from : AC-MDL-012 (REQ-MDL-012)
Exercises    : API-MDL-011 GET /api/v1/mdl/lookups
Rule / code  : RULE-MDL-004 → MDL-404-TYPE-KEY
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: no LookupType with key "NOT_A_REAL_KEY"
Steps        : 1. GET ?type=NOT_A_REAL_KEY
Expected     : 404 MDL-404-TYPE-KEY (never an empty-success)
Test data    : type "NOT_A_REAL_KEY"
<!-- TC:TC-MDL-012:END -->
<!-- SUB:RULE-SCENARIOS:END -->
