<!-- source: PHASE:CORE -->
<!-- traces: REQ-SEC-033 -->
<!-- PHASE:CORE:START traces=REQ-SEC-033 -->
## PHASE 1 — CORE

**Layers** (`profile.stack.backend.layers`): controller → service → mapper → domain → repository.
- **controller**: HTTP binding, request validation shape (types/required), maps DTO ↔ command; never queries the repository directly; never contains a RULE-* check.
- **service**: orchestration — loads, validates every RULE-*, integrates (for SEC only the optional NOTIF password-reset dispatch — REQ-SEC-029, §PHASE 5 INT-C; still zero XM), persists via repository; the sole place RULE-* logic runs; the sole place PERM_* is asserted before any mutation proceeds.
- **mapper**: entity ↔ DTO conversion only; no business logic, no query.
- **domain**: entity classes; domain-behaviour placement = **in entity methods** for single-entity invariants (e.g. `User.deactivate()` flips `isActiveFl`/`statusCode`), and in the service layer for any rule spanning more than one entity (e.g. RULE-SEC-001/002/003/005/007, which read another table).
- **repository**: Spring Data JPA repositories, one per entity/table; every non-trivial query is a named method backed by a `QR-SEC-*` spec (§Query Reference Catalog); no business logic.

**Error signalling**: `LocalizedException → {code, messageAr, messageEn}`. Runtime `code` format:
`SEC-<HTTP-status>-<SCENARIO>` (module-scoped, stated once here so `api-verify` can assert on it —
e.g. `SEC-409-USER-DUP`). EVERY `SecErrorCodes` value carries that full three-segment shape; no
scenario-less `SEC-<HTTP-status>` code exists, because the 500 path is not module-scoped at all.
Every module-owned catalog row (§Error Catalog) is registered as a static constant in
`SecErrorCodes` that the shared exception→envelope mapping resolves; the single PLATFORM-STD 500
row (`INTERNAL_ERROR`) is owned and emitted by the shared `GlobalExceptionHandler` in
`com.erp.common.web`, so SEC neither declares nor throws it. `messageAr`/`messageEn`
are copied character-perfect from the SRS RULE message or from this plan where PLATFORM-STD.

**Transaction scope defaults**: `READ_ONLY` for every `FIND_*`/`EXISTS`/`AGGREGATE` QR;
`READ_WRITE` for every `SAVE`/`UPDATE`/`DELETE` QR; no `REQUIRES_NEW` anywhere in SEC v1
(no QR overrides this).

**Search contract**: request shape `{filters: {...}, page, size, sort}`; allowed sort fields =
exactly the columns listed as filters in each screen's SRS B2; paging `Page<T>`
(`profile.stack.backend.api.paging`); an empty result is success with empty content, never
"not found" (§5 FIND_BY_CRITERIA default).

**Audit fields**: `createdBy, createdAt, updatedBy, updatedAt` are framework-filled (from the
authenticated principal + server clock) — never present in a create/update request DTO, never
set by a mapper or service method explicitly; the same applies to `grantedBy/grantedAt`,
`assignedBy/assignedAt`, `reviewedBy/reviewedAt`, `terminatedBy/terminatedAt`,
`occurredAt/actorUserId` (all system-set for the same reason, per Field Registry read-only=Yes).

**Type mapping** (`profile.stack.db.syntax_map` postgresql16 → Java, spring-boot-java):
| postgresql16 | Java |
|---|---|
| GENERATED ALWAYS AS IDENTITY | Long |
| VARCHAR(n) | String |
| BOOLEAN | Boolean |
| TIMESTAMPTZ | Instant |
| TEXT | String |
No `NUMERIC` column exists in SEC v1 (no money/decimal field) — not applicable this module; a
future decimal field would map to `BigDecimal` per the same syntax_map row, stated here for
completeness, no deviation ADR needed since none is used.

**Lookup values**: `statusCode` (User, SignupRequest) and `eventTypeCode` (AuditLogEntry) are
returned and accepted as plain strings (the lookup CODE) everywhere in every API below — never
as an enum type in a DTO, never as a numeric id. Per ADR-SEC-001 they are CHECK-constrained in
v1, not FK-backed; the service layer still validates the incoming code against the same closed
set the CHECK constraint enforces, so an invalid code is rejected with a catalog error before it
ever reaches the database.

**Numbering**: not applicable — SEC has no entity with a platform-numbered business code
(Pre-generation extraction: "BUSINESS CODE: none").

**Workflow engine**: forbidden (`profiles/erp.yaml → conventions.workflow_engine`) — every
status transition below (User, SignupRequest) is a plain field update guarded by a RULE or a
dedicated action endpoint, never a workflow definition.

**Languages**: every name field (`nameAr`/`nameEn`, `fullNameAr`/`fullNameEn`,
`descriptionAr`/`descriptionEn`, `detailsAr`/`detailsEn`) and every catalog message is present
in both `ar` and `en` in every DTO and every response — a single-language value anywhere is
incomplete per §Error Catalog / §6.1 rule.

**Cross-module contract placement**: no XM contract to place this version — SEC has zero XM (it
is ROOT). The one interface SEC injects is NOTIF-owned, not SEC-owned: `NotificationDispatchApi`
for the REQ-SEC-029 password-reset dispatch (§PHASE 5 INT-C). SEC itself is consumed by every
future module through its own REST surface (§SVC-API below), not through an injected interface.

**Cross-cutting authorization (REQ-SEC-033)**: a single servlet filter / method-level
interceptor runs before every secured controller method (i.e. every API below except
API-SEC-001..004, which are pre-authentication): it resolves the caller's effective module,
screen and action grants (via the same read path as API-SEC-027's effective-menu query) and
denies with a catalog error (§Error Catalog, `SEC-403`) before the controller method body runs
if the module grant, the screen's VIEW grant (RULE-SEC-007), or the specific action grant is
missing. This single mechanism is what §SEC-BE (Phase 7) and every API's "Security" line below
refer to — it is declared once here, never re-implemented per endpoint.
<!-- PHASE:CORE:END -->
