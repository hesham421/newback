<!-- source: PHASE:TEST-PLAN-FE / SUB:INT-FLOW -->
<!-- context: TEST-PLAN-FE-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-004, AC-MDL-009, API-MDL-004, API-MDL-008, REQ-MDL-004, REQ-MDL-009, SCR-MDL-001, SCR-MDL-002 -->
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
