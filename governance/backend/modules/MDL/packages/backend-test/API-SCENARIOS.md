<!-- source: PHASE:TEST-PLAN-BE / SUB:API-SCENARIOS -->
<!-- traces: AC-MDL-001, AC-MDL-005, AC-MDL-006, AC-MDL-008, AC-MDL-009, AC-MDL-010, AC-MDL-013, API-MDL-002, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009, API-MDL-010, REQ-MDL-001, REQ-MDL-005, REQ-MDL-006, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-013 -->
<!-- SUB:API-SCENARIOS:START traces=REQ-MDL-001,REQ-MDL-005,REQ-MDL-006,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-013 -->
  ### SUB — API-SCENARIOS

  <!-- TC:TC-MDL-001:START traces=AC-MDL-001,REQ-MDL-001,API-MDL-002 -->
  ### TC-MDL-001 — create a lookup type
  Derived from : AC-MDL-001 (REQ-MDL-001)
  Exercises    : API-MDL-002 POST /api/v1/mdl/lookup-types
  Rule / code  : — (happy path)
  Scenario     : HAPPY · data class VALID · language ALL
  Preconditions: unique key; registered owner module code
  Steps        : 1. POST {key: "TEST_TYPE", ownerModuleCode: "MDL", nameAr, nameEn}
  Expected     : 201; active LookupType created
  Test data    : key "TEST_TYPE", ownerModuleCode "MDL" (MDL owning its own test lookup type is a legitimate self-registration case — a module owning its own reference data is a real scenario, not a contrived one)
  <!-- TC:TC-MDL-001:END -->

<!-- TC:TC-MDL-005:START traces=AC-MDL-005,REQ-MDL-005,API-MDL-005 -->
### TC-MDL-005 — select a type and list its values
Derived from : AC-MDL-005 (REQ-MDL-005)
Exercises    : API-MDL-005 POST /api/v1/mdl/lookup-types/values/search
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a type with 3 values
Steps        : 1. POST /search filtered by the type's lookupTypeId
Expected     : 200; exactly those 3 values, ordered by sortOrder
Test data    : type with 3 values
<!-- TC:TC-MDL-005:END -->

  <!-- TC:TC-MDL-006:START traces=AC-MDL-006,REQ-MDL-006,API-MDL-006 -->
### TC-MDL-006 — create a lookup value
Derived from : AC-MDL-006 (REQ-MDL-006)
Exercises    : API-MDL-006 POST /api/v1/mdl/lookup-types/{id}/values
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a code not yet used within the type
Steps        : 1. POST {code, nameAr, nameEn, sortOrder}
Expected     : 201; active LookupValue created under the type
Test data    : new code "NEW_VAL" under an existing type
<!-- TC:TC-MDL-006:END -->

<!-- TC:TC-MDL-008:START traces=AC-MDL-008,REQ-MDL-008,API-MDL-007 -->
### TC-MDL-008 — edit a lookup value
Derived from : AC-MDL-008 (REQ-MDL-008)
Exercises    : API-MDL-007 PUT /api/v1/mdl/lookup-values/{id}
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an existing LookupValue
Steps        : 1. PUT {nameAr, nameEn, sortOrder}
Expected     : 200; names/sortOrder updated; code and lookupTypeId unchanged
Test data    : existing value, new nameEn
<!-- TC:TC-MDL-008:END -->

<!-- TC:TC-MDL-009:START traces=AC-MDL-009,REQ-MDL-009,API-MDL-008 -->
### TC-MDL-009 — deactivate a lookup value
Derived from : AC-MDL-009 (REQ-MDL-009)
Exercises    : API-MDL-008 DELETE /api/v1/mdl/lookup-values/{id}
Rule / code  : — (happy path)
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active LookupValue
Steps        : 1. DELETE (deactivate) — 2. call API-MDL-011 for its type
Expected     : 1. 200, isActiveFl=false — 2. the value no longer returned
Test data    : a test-only value
<!-- TC:TC-MDL-009:END -->

<!-- TC:TC-MDL-010:START traces=AC-MDL-010,REQ-MDL-010,API-MDL-009 -->
### TC-MDL-010 — reorder lookup values
Derived from : AC-MDL-010 (REQ-MDL-010)
Exercises    : API-MDL-009 PATCH /api/v1/mdl/lookup-types/{id}/values/reorder
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: 3 values with sortOrder 1,2,3
Steps        : 1. PATCH {orderedValueIds: [v3, v1, v2]}
Expected     : 200; sortOrder persisted as 3,1,2 respectively; a subsequent API-MDL-011 call returns them in that order
Test data    : 3 values under one type
<!-- TC:TC-MDL-010:END -->

<!-- TC:TC-MDL-013:START traces=AC-MDL-013,REQ-MDL-013,API-MDL-010 -->
### TC-MDL-013 — browse the type registry grouped by owner
Derived from : AC-MDL-013 (REQ-MDL-013)
Exercises    : API-MDL-010 POST /api/v1/mdl/lookup-types/by-owner/search
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: types owned by SEC and by FIN
Steps        : 1. POST /search for the registry
Expected     : 200; types grouped under their respective owner-module headings
Test data    : existing SEC-owned and MDL-owned types
<!-- TC:TC-MDL-013:END -->
<!-- SUB:API-SCENARIOS:END -->
