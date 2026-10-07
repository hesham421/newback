<!-- source: PHASE:SEC-FE -->
<!-- traces: REQ-MDL-001, REQ-MDL-004, REQ-MDL-005, REQ-MDL-009, REQ-MDL-013, SCR-MDL-001, SCR-MDL-002, UXD-MDL-001 -->
<!-- PHASE:SEC-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-004,REQ-MDL-005,REQ-MDL-009,REQ-MDL-013 -->
## PHASE 5 — SEC-FE

The frontend half of the security model, per `SCR-*`: the navigation guard and the UI behaviour
per action. Permission names are the backend registry's and the SRS Access summary's, cited and
never redeclared. One mechanism gates both screens — the **menu gate**: the screen's page code is
present in the effective menu the security module serves this caller. Action-level grants are
published nowhere, so an action's affordance renders for a caller who holds the screen and the
server's `ACCESS_DENIED` is the authority, shown as its localized message (ADR-MDL-012). Never
split — level-1 only.

### SEC-FE · SCR-MDL-001 — اللوكبات العامة / Generic Lookups
Permissions      : `PERM_MDL_LOOKUPS_VIEW` (gateway) · `PERM_MDL_LOOKUPS_CREATE` ·
                   `PERM_MDL_LOOKUPS_UPDATE` · `PERM_MDL_LOOKUPS_DELETE`
Navigation guard : `MDL_LOOKUPS` must be in the caller's effective menu. A caller without it is
                   sent to the unauthorized destination, and every route of this screen — the
                   list, `new`, `:typeId`, `:typeId/edit` and both value routes — carries the
                   same guard.
Per action       : **VIEW** → the gate above, exact, and it covers both panes: a caller who holds
                   the screen sees types and values alike; without it no route of this screen
                   renders at all. **CREATE** (a type, and a value under it) → not readable
                   before the call; the affordance renders and a 403 is shown as the localized
                   message on the form that attempted it. **UPDATE** (edit at either level, and
                   the reorder) → the same. **DELETE** (which here means deactivate and nothing
                   else — no hard delete exists at either level) → the same.

**Filed as a platform finding, not settled here (G5).** The api-docs put `PERM_MDL_LOOKUPS_UPDATE`
on **both** deactivate endpoints, while backend-execution-plan-mdl.md PHASE 7's permission matrix
and BOOTSTRAP DATA mark the DELETE column for API-MDL-004 and API-MDL-008 and seed
`PERM_MDL_LOOKUPS_DELETE` with grant targets — a real divergence, not a naming detail: as built,
UPDATE alone lets a caller deactivate, and DELETE alone lets a caller deactivate nothing, so SRS
§B4's DELETE row is unenforceable as built. This is filed as **PF-MDL-001** (API SURFACE, above),
owner the MDL backend track, evidence the two artifacts just named. The frontend neither checks
`PERM_MDL_LOOKUPS_DELETE` nor depends on it: the guard it can evaluate is the page gate, and the
authority on a write is the server's answer to the call actually made.

The screen-level grant is the whole granularity available. Per-lookup-type permissions — letting
a role manage one type but not another — are an explicit SRS scope exception (§A2), so no
per-type gate is drawn, attempted or hinted at in the UI.
Foreign grant    : a role granted `PERM_MDL_LOOKUPS_CREATE` needs `PERM_SEC_MODULE_REGISTRY_VIEW`
                   as well, or the owner-module select it must fill stays empty and disabled. The
                   grant is the security module's to make; this plan names it and mints nothing
                   (UXD-MDL-001, ADR-MDL-013). The master-list owner-module filter needs no such
                   grant of its own: it degrades to data the search response already carries
                   (ADR-MDL-016, G1), never to a blocked control.

### SEC-FE · SCR-MDL-002 — سجل أنواع اللوكب حسب المالك / Lookup-type registry by owner
Permissions      : `PERM_MDL_TYPE_REGISTRY_VIEW`
Navigation guard : `MDL_TYPE_REGISTRY` must be in the caller's effective menu.
Per action       : **VIEW** → the gate above. There is no CREATE, UPDATE or DELETE on this
                   screen: the SRS Access summary gives it VIEW alone and the screen writes
                   nothing, so there is no affordance to hide and no submit to refuse. Its links
                   into SCR-MDL-001 render unconditionally; the target route's own guard stops a
                   caller who does not hold that screen, so a reviewer who may browse but not
                   manage sees the registry and is refused at the door of the editor rather than
                   shown a dead link.

**Across both screens.** A forbidden response is shown as its localized catalog message, never as
a silent no-op and never as a generic failure. An unauthenticated response returns the caller to
the platform's sign-in destination and discards the server-state cache, so no data of the
previous identity survives into the next. No screen composes a permission name and no screen
keeps a local copy of the caller's grants: the menu response is the single source, and a failure
to load it renders no entry of this module and grants no route of it — access narrows, never
widens.
<!-- PHASE:SEC-FE:END -->
