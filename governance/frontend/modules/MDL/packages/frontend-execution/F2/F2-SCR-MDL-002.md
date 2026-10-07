<!-- source: PHASE:F2 / SUB:F2-SCR-MDL-002 -->
<!-- context: F2-HEADER.md — phase-level preamble -->
<!-- traces: AC-MDL-011, AC-MDL-012, AC-MDL-013, API-MDL-010, API-MDL-011, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013, SCR-MDL-002, UXD-MDL-001 -->
<!-- SUB:F2-SCR-MDL-002:START traces=SCR-MDL-002,UXD-MDL-001,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-010,API-MDL-011 -->
### F2 · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner

#### F2-QUERY — REGISTRY BROWSE — API-MDL-010  traces=API-MDL-010,REQ-MDL-013,AC-MDL-013
Kind         : read query (ADR-MDL-002) · request carries `filters` alone — no sort, no paging ·
               response a bare array of owner groups
Cache key    : `[lookup-types-by-owner, filters]` — ownerModuleCode and key, and nothing more.
               No page or size belongs in this key because the endpoint accepts neither; adding
               them would key a variation the server cannot produce.
Errors       : `VALIDATION_ERROR` → inline on the offending filter · `ACCESS_DENIED` → the
               localized forbidden message · `INTERNAL_ERROR` → generic
Loading      : LOCAL
Cache policy : defaults
Invalidation : n/a (a read). It is refreshed by SCR-MDL-001's type mutations, which name this
               key family in their own invalidation lines — a type created, renamed or
               deactivated there changes what this registry shows.

#### F2-QUERY — CONSUMER READ BY KEY — API-MDL-011  traces=API-MDL-011,REQ-MDL-011,REQ-MDL-012,AC-MDL-011,AC-MDL-012
Kind         : read query · **bound, and called by no screen of this module** (ADR-MDL-007).
               This block is the complete accounting for API-MDL-011's response shape — no
               second model of it exists in F1 (G11).
Caller       : a consuming module's backend, over the platform's in-process module interface —
               not a user at a screen. Stated here so the published surface is completely
               accounted for.
Errors       : `MDL-404-TYPE-KEY` [REQ-MDL-012, RULE-MDL-004] → answered to the calling module —
               ar: «لا يوجد نوع لوكب بهذا المفتاح» · en: "No lookup type exists with this key" —
               an unknown key is a not-found, never an empty success ·
               `ACCESS_DENIED` → answered to the calling module
Cache key    : n/a — no client of this plan calls it
Invalidation : n/a. What a user can observe of this endpoint is indirect: deactivating a value,
               or its type, on SCR-MDL-001 is what makes it stop being returned (RULE-MDL-004).

#### F2-LOOKUP — none
Same as the other screen, and for the same reason: MDL owns no lookup key and consumes none.

#### F2-SCREEN-INIT — SCR-MDL-002
Permission read : `MDL_TYPE_REGISTRY` present in the caller's effective menu → VIEW. This screen
                  has no other action to gate (SRS §B4).
Lookups used    : none
Foreign data    : the owner-module filter and the group headings resolve through UXD-MDL-001 —
                  the same shared hook SCR-MDL-001 uses, not a second call. When that read is
                  refused or fails, the owner select falls back to the **distinct
                  `ownerModuleCode` values present in the current API-MDL-010 response**, which
                  the browse already returns on every group — never to free text, and never to a
                  code the registry does not currently hold. Group headings themselves fall back
                  to the bare code the browse returns, unchanged from before; browsing is never
                  blocked by the degraded source (ADR-MDL-013, ADR-MDL-015, G6)
Entity by id    : none — each group carries its full type rows

#### F2-FACADE — SCR-MDL-002
Composes     : API-MDL-010 · the UXD-MDL-001 hook
State it owns: the groups derived from the query's data, and the filter object (owner module,
               key) mirrored from the route's search params, so a filtered registry view is
               shareable by address (ADR-MDL-002). No grouping and no count is composed here —
               both arrive in the response.
Operations   : none — this screen writes nothing. Opening a type in SCR-MDL-001 is a route
               change, not an operation.
Ordered pairs: none — there is no call to order.
<!-- SUB:F2-SCR-MDL-002:END -->
