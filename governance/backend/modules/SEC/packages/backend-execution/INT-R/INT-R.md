<!-- source: PHASE:INT-R -->
<!-- traces: REQ-SEC-016 -->
<!-- PHASE:INT-R:START traces=REQ-SEC-016 -->
## PHASE 6 — INT-R (cross-module resolve)

No `XM-*` row to resolve — same basis as Phase 5. No SUB opened (0 < 5). Since 2026-09-11 this
phase also carries SEC's one EXPOSED inbound surface, `com.erp.sec.crossmodule` — both former
inbound gaps close here, and this is the inbound phase, so the surface is documented here rather
than in Phase 5 (which stays the consume direction: NOTIF's `NotificationDispatchApi`). The
surface registers no entity, table or column, so it still adds no `XM-*` row to db-script §2
(amended there to say so), and SEC still assigns no XM id of its own.

Inbound dependency stub (future consumers, not `TODO`): `XM-INBOUND-STUB-1` — any future
module (first expected: MDL, then FIN per GENERATION-INSTRUCTIONS.md §3) will register
itself via API-SEC-018/019/020 and consume identity/authorization via API-SEC-001/027 and
the CORE interceptor; the entity it reaches is `ENT-SEC-004` (ModuleRegistry) /
`ENT-SEC-005` (ScreenRegistry) / `ENT-SEC-006` (ActionRegistry); formal `XM-*` ids for that
direction are assigned by the *consuming* module's own P2, not by SEC.

Two inbound-contract gaps were found during execution and recorded here. **Both are now CLOSED
(2026-09-11)**, by the human-authorized amendment that gave SEC its first cross-module read
surface — a P1/P2 decision, taken deliberately and reflected back into P1 (REQ-SEC-034,
REQ-SEC-035, SRS §A8's third table) and P2 (§2 XM REGISTER), not an execution-time invention.

`XM-INBOUND-GAP-1` — **CLOSED. A consumer can now read the user ids holding a permission code.**
`SecUserDirectoryApi.findUserIdsHoldingPermission(String)` (REQ-SEC-035, QR-SEC-039) returns the
DISTINCT user ids reaching that permission code through an active role — the inverse of
QR-SEC-027, with the same active-flag predicates. The original finding stands as written: no SEC
*endpoint* returns a role's user set, and none was added. FIN's plan
(`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:383-386`) says the SoD check reads the
sets "through SEC's role/grant read APIs"; that wording is **superseded**, because
`build-create-service` requires cross-module reads to go through "direct Spring interface
injection, not loopback HTTP" — an injected interface, never an HTTP call between two modules of
one deployable. Correcting FIN's wording is FIN's own pass; `governance/modules/FIN/**` was not
modified here. The surface answers with user ids only: SEC neither learns nor evaluates FIN's
conflicting pair, so REQ-SEC-020's "inert in v1" Note is unaffected.

`XM-INBOUND-GAP-2` — **CLOSED on both halves.** *Delivery half*, closed earlier on 2026-09-11:
`PasswordResetService.dispatchResetNotification` now carries the resolved user's `email` among the
`DispatchCommand` variables, which is exactly the contract `DefaultChannelProvider` publishes
("NOTIF has no crossmodule contact-lookup for a bare recipientId"), so REQ-SEC-029's outbound half
delivers instead of writing `NOTIF_LOG FAILED — missing recipient email address`. *Durable half*,
closed now: `SecUserDirectoryApi.findContact(Long)` (REQ-SEC-034) returns a `UserContact` —
email, both display names, active — for a bare user id, so NOTIF can resolve a recipient without
SEC pushing the address, and can discharge its own `XM-NOTIF-001` whenever it chooses.
NOTIF's `DefaultRecipientStatusReader` stub was **deliberately left in place**: replacing it is
NOTIF's call in NOTIF's own pass, and nothing under `src/main/java/com/erp/notif/` or
`governance/modules/NOTIF/**` was touched from this SEC-driven pass.
<!-- PHASE:INT-R:END -->
