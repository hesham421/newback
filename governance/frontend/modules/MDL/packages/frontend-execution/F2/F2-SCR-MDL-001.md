<!-- source: PHASE:F2 / SUB:F2-SCR-MDL-001 -->
<!-- context: F2-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-001, AC-MDL-002, AC-MDL-004, AC-MDL-005, AC-MDL-007, AC-MDL-009, AC-MDL-010, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, SCR-MDL-001, UXD-MDL-001 -->
<!-- SUB:F2-SCR-MDL-001:START traces=SCR-MDL-001,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,AC-MDL-001,AC-MDL-002,AC-MDL-004,AC-MDL-005,AC-MDL-007,AC-MDL-009,AC-MDL-010,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009 -->
### F2 · SCR-MDL-001 — اللوكبات العامة / Generic Lookups

#### F2-QUERY — TYPE SEARCH — API-MDL-001      traces=API-MDL-001,REQ-MDL-001,REQ-MDL-003
Kind         : read query — a POST that mutates nothing (ADR-MDL-002); paginated response
Cache key    : `[lookup-types, filters]`, `filters` being the whole request object — key,
               ownerModuleCode, isActiveFl, sortDirection **and page, size**. Every one of them
               changes the response, so every one is in the key. No `sortField` is in the key or
               the request: the backend offers no free-form sort parameter (G7, F1-SCREEN above).
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — the SRS states nothing about this call being slow, so no global indicator
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-002, API-MDL-003 and API-MDL-004
Note (G1)    : this query's response is also the fallback source for the master-list
               `ownerModuleCode` filter's own degraded state (see F2-SCREEN-INIT, ADR-MDL-016) —
               its rows are read for their `ownerModuleCode` values, not through a second call.

#### F2-QUERY — TYPE CREATE — API-MDL-002      traces=API-MDL-002,REQ-MDL-001,REQ-MDL-002,AC-MDL-001,AC-MDL-002
Kind         : mutation
Errors       : `MDL-409-MODULE-NOT-REGISTERED` → the RULE-MDL-001 message, routed to the
               owner-module field — ar: «الوحدة المالكة غير مسجّلة في وحدة الأمان» ·
               en: "The owning module is not registered in the Security module" ·
               `MDL-409-TYPE-DUP` → inline on `key` (the platform duplicate row) ·
               `VALIDATION_ERROR` → inline per `error.fieldErrors[].field` ·
               `ACCESS_DENIED` → the localized forbidden message (ADR-MDL-012)
Loading      : LOCAL — on the submitting form
Invalidation : `[lookup-types, *]`, and `[lookup-types-by-owner, *]` — a new type changes what
               SCR-MDL-002's registry shows
Success      : ar: «تم حفظ نوع اللوكب.» · en: "The lookup type has been saved." (AC-MDL-001)

#### F2-QUERY — TYPE UPDATE — API-MDL-003      traces=API-MDL-003,REQ-MDL-003,AC-MDL-003
Kind         : mutation
Errors       : `MDL-404-TYPE` → user message · `VALIDATION_ERROR` → inline ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]`
The request carries the two names and nothing else — RULE-MDL-003 expressed in the surface
itself: there is no key field to send, and none is sent.

#### F2-QUERY — TYPE DEACTIVATE — API-MDL-004  traces=API-MDL-004,REQ-MDL-004,AC-MDL-004
Kind         : mutation · no request body
Errors       : `MDL-404-TYPE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-types, *]`, `[lookup-types-by-owner, *]` **and** `[lookup-values, *]` —
               RULE-MDL-004 makes an inactive type's values invisible to consumers, so a cached
               value list of that type is stale in meaning even though its rows did not change
Requires `PERM_MDL_LOOKUPS_UPDATE` per the api-docs, not DELETE — read there, not assumed, and
filed as PF-MDL-001 rather than settled here (G5).

#### F2-QUERY — VALUE SEARCH — API-MDL-005     traces=API-MDL-005,REQ-MDL-005,AC-MDL-005
Kind         : read query (ADR-MDL-002); **not paginated** — a bare array, ordered by
               `sortOrder` then `code`, returned whole because it is bounded by one type
               (backend-execution-plan API-MDL-005, QR-MDL-005, SRS §B2). A prior revision of
               this block modelled this call as paginated; corrected then (G2).
Cache key    : `[lookup-values, filters]` — lookupTypeId, code. No page and no size: the endpoint
               accepts neither, and keying a variation the server cannot produce would fragment
               the cache for nothing. The parent id is part of the key, so selecting another type
               is a different cache entry, not a refetch of the same one.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL — on the values pane alone; the master list does not blank while it loads
Cache policy : defaults
Invalidation : n/a (a read); refreshed by API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009
This read returns inactive values beside active ones — it is the manager's view, not the
consumer's, and the screen shows what API-MDL-011 hides (AC-MDL-005, AC-MDL-009).

#### F2-QUERY — VALUE CREATE — API-MDL-006     traces=API-MDL-006,REQ-MDL-006,REQ-MDL-007,AC-MDL-006,AC-MDL-007
Kind         : mutation · the parent type is the path id, taken from the selection
Inactive parent : the call is **not issued when the selected parent type is inactive**
               (ADR-MDL-042). `isActiveFl` is on the row the pane already holds, so the
               state is known without a read. A value created under a deactivated type is
               unreachable for the life of the platform — v1 publishes no activate endpoint
               (ADR-MDL-005) and RULE-MDL-004 excludes an inactive type's values from every
               consumer read — and it reserves its code under that type permanently. UPDATE
               and REORDER stay legal: they maintain rows AC-MDL-004 already guarantees
               survive deactivation. The guard is the client half only; the server half is
               PF-MDL-007
Errors       : `MDL-409-VALUE-DUP` → the RULE-MDL-002 message, routed inline to `code` —
               ar: «هذا الرمز مستخدم بالفعل ضمن هذا النوع» ·
               en: "This code is already used within this type" — a deactivated value of the
               same code under this type may be the one holding it (RULE-MDL-002 permits no
               reactivation, G7); the list's own inactive row, still shown in this pane
               (AC-MDL-005), is the evidence the user can check ·
               `MDL-404-TYPE` → user message (the parent type is gone) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]`
Success      : ar: «تم حفظ القيمة.» · en: "The value has been saved." (AC-MDL-006)

#### F2-QUERY — VALUE UPDATE — API-MDL-007     traces=API-MDL-007,REQ-MDL-008,AC-MDL-008
Kind         : mutation
Errors       : `MDL-404-VALUE` → user message · `MDL-409-VALUE-DUP` → inline on `code` if the
               server ever raises it here (RULE-MDL-002 is scoped to create and update alike) ·
               `VALIDATION_ERROR` → inline · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL
Invalidation : `[lookup-values, *]` — the submitted `sortOrder` may have moved the row

#### F2-QUERY — VALUE DEACTIVATE — API-MDL-008 traces=API-MDL-008,REQ-MDL-009,AC-MDL-009
Kind         : mutation · no request body
Errors       : `MDL-404-VALUE` → user message · `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the confirmation
Invalidation : `[lookup-values, *]`
The row stays in this screen's list afterwards, marked inactive: it leaves the consumer's read,
not the manager's (AC-MDL-009). No endpoint exists to reverse it (ADR-MDL-005), so its `code`
stays reserved under this type for the life of the type — the confirmation states this (G7,
ui-ux-spec-mdl.md).

#### F2-QUERY — VALUE REORDER — API-MDL-009    traces=API-MDL-009,REQ-MDL-010,AC-MDL-010
Kind         : mutation · request `{ orderedValueIds[] }` · response a bare array in the
               persisted order — not a paginated envelope
Errors       : `MDL-400-REORDER-MISMATCH` → user message on the value list (the submitted set is
               not exactly that type's values) · `MDL-404-TYPE` → user message ·
               `ACCESS_DENIED` → the localized forbidden message
Loading      : LOCAL — on the list, with the dragged order held until the call settles
Invalidation : on success only — `[lookup-values, *]`, since every row's `sortOrder` may have
               changed and the response carries the persisted order the list then renders from.
               On any non-2xx answer the pending order is discarded and the list re-renders from
               the last persisted response; no invalidation runs (G10). MDL-400-REORDER-MISMATCH
               therefore always leaves the list showing the order the server actually holds,
               never the rejected drag.
Submission rule (G3) : **the submitted list is always the type's complete, unfiltered value set.**
               `lookup_type_id = :typeId` is the only predicate QR-MDL-009 checks — it does not
               (and cannot) verify that every value of the type was submitted — so a subset
               re-ranks 1..n and collides with the ranks of every value left out. The values
               pane can produce exactly that subset (a non-empty `code` filter, F1/F3), so the
               drag affordance is **disabled** whenever the pane shows less than the whole type:
               a non-empty `code` filter is active. A shown reason accompanies the disabled
               state — ar: «امسح مرشّح الرمز لإعادة الترتيب» · en: "Clear the code filter to
               reorder" — because the API-MDL-005 read this pane is built on carries no paging to
               also guard against (G2). Reordering from a filtered pane is refused by the client
               before the call is made, not answered by the server, because the server cannot
               distinguish a partial submission from a deliberate whole-set one.
<!-- moved from a bare inline note: the endpoint takes `orderedValueIds[]`; the mismatch code
     exists for an id that does not belong to the type, never for a merely incomplete list. -->

#### F2-LOOKUP — none
MDL owns no lookup key and consumes none (SRS §A6). The one option list on this screen is the
owner-module select, which is not lookup data: it is UXD-MDL-001, declared below.

#### F2-SCREEN-INIT — SCR-MDL-001
Permission read : `MDL_LOOKUPS` present in the caller's effective menu → VIEW, the gateway.
                  CREATE, UPDATE and DELETE are readable from no published surface; the server
                  answers them per call (ADR-MDL-012, PF-MDL-001).
Lookups used    : none
Foreign data    : the owner-module select (create form) and the owner-module filter (master
                  list) both resolve through UXD-MDL-001 — ONE shared hook, long-lived cache,
                  shared with SCR-MDL-002 — but the two controls degrade differently, because a
                  select that must accept an unpicked value cannot degrade the way a read-only
                  filter over an already-known result set can:
                    · create-form select — a refused read leaves the select empty and disabled,
                      and the create action disabled behind it (ADR-MDL-013)
                    · master-list filter — a refused read leaves the filter falling back to the
                      distinct `ownerModuleCode` values already present in the current
                      API-MDL-001 response (see F2-QUERY TYPE SEARCH above) — never empty, never
                      disabled, and browsing the type list is never blocked by the degraded
                      source (ADR-MDL-016, G1)
Entity by id    : none published at either level; both entry sub-views hydrate from the row their
                  own search query already holds when that row is in cache; on a cold load the
                  hydrating row is absent and the route redirects to `/reference-data/lookups`
                  (type editor) or `/reference-data/lookups/:typeId` (value editor) rather than
                  render empty (ADR-MDL-014)

#### F2-FACADE — SCR-MDL-001
Composes     : API-MDL-001, API-MDL-005 (the two lists) · API-MDL-002, API-MDL-003, API-MDL-004,
               API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009 (the mutations) · the
               UXD-MDL-001 hook
State it owns: the type list and the selected type's value list, both derived from their queries'
               data and never copied into state; the selected type id, read from the route param;
               two filter objects (the master's carrying page and size, the detail's carrying
               `code` alone); the pending drag order while a reorder is in flight, and whether the
               detail's `code` filter is empty (the reorder-eligibility flag of G3); the open
               editor's identity, held in navigation state rather than in a boolean; and a
               derived loading flag over the calls in flight
Operations   : createType · updateType · deactivateType (confirmed, and the confirmation says
               consumer reads will stop returning this type's values, and that `key` stays
               reserved and unreusable afterwards — RULE-MDL-004, G7) · createValue · updateValue
               · deactivateValue (confirmed, same reserved-code statement, G7) · reorderValues
               (the whole ordered list, submitted once, disabled while the detail pane is
               filtered — G3)
Ordered pairs: none. **No operation of this screen owns two calls** — a type's write carries no
               values, so nothing here updates a record and then replaces its child set, and no
               ordering or skip-if-unchanged rule is needed. Each operation is exactly one call.
There is no activateType and no activateValue: the SRS names no such action and no endpoint
exists for one (ADR-MDL-005). Components use the facade only; the facade uses the declared
queries only.
<!-- SUB:F2-SCR-MDL-001:END -->
