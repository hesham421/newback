> Vendored verbatim on 2026-10-07 from `erp-app-reference/governance/project-artifacts/sec-implementation-notes.md` (newback `b8ce7fa`), because eight erp-core classes cite it.
> Paths inside are historical (`governance/shared/…`, `governance/modules/SEC/…`, a root `src/`); the decisions recorded are still current as of erp-core 1.2.0.
> The current references are `governance/analysis/modules/SEC/` (P1 SRS, P2 DB script) and `docs/api-docs/sec/`.

# SEC — backend implementation notes

Record of the non-obvious implementation decisions behind `src/main/java/com/erp/sec/`.
Extracted from the class Javadoc on 2026-09-11, when the SEC sources were trimmed to the
GOVERNANCE-RULES.md Javadoc limit (~5 lines, one non-obvious fact per block). The SEC classes
now link here instead of restating the reasoning in every file.

Sources referenced throughout (governance paths as mounted today — `governance/shared` is the
shared governance submodule; older sections below were written when these lived under
`governance/modules/SEC/`):
`governance/shared/analysis/modules/SEC/P2/db-script-sec.md`,
`governance/shared/backend/modules/SEC/packages/backend-execution/{CORE,DATA-DOM,SVC-API}*.md`,
`governance/shared/analysis/modules/SEC/P1/srs-sec.md`,
`governance/shared/backend/modules/SEC/execution-state.json`,
`src/main/resources/db/migration/V16__sec_schema.sql`.

Sections are dated decision records. Where a later change superseded a statement, the statement
has been corrected in place and the change is marked **(updated 2026-10-01)**. For how another
module integrates with SEC today, read [platform-integration-sec.md](platform-integration-sec.md).

---

## 1. AuditableEntity exemptions

`build-create-entity` A.1.1 requires every entity to extend `com.erp.common.domain.AuditableEntity`.
SH.1 exempts a table that has no `created_by / created_at / updated_by / updated_at` columns.
Six of the thirteen SEC tables are in that position; extending `AuditableEntity` there would make
Hibernate write four columns that do not exist.

| Entity | Table | db-script evidence | Own lifecycle / system-set fields |
|---|---|---|---|
| `UserRoleAssignment` | `SEC_USER_ROLE` | §3 CREATE TABLE — five columns only | `assignedBy` / `assignedAt` |
| `RoleModuleGrant` | `SEC_ROLE_MODULE_GRANT` | §3 CREATE TABLE — five columns only | `grantedBy` / `grantedAt` |
| `RoleScreenGrant` | `SEC_ROLE_SCREEN_GRANT` | §3 CREATE TABLE — five columns only | `grantedBy` / `grantedAt` |
| `RoleActionGrant` | `SEC_ROLE_ACTION_GRANT` | §3 CREATE TABLE — five columns only | `grantedBy` / `grantedAt` |
| `ActiveSession` | `SEC_ACTIVE_SESSION` | §3 CREATE TABLE — eight columns only | `startedAt`, `lastActivityAt`, `terminatedAt` / `terminatedBy` |
| `AuditLogEntry` | `SEC_AUDIT_LOG` | §3 CREATE TABLE — no audit columns | `actorUserId`, `occurredAt` |
| `PasswordResetToken` | `SEC_PWD_RESET_TOKEN` | §3 CREATE TABLE — six columns only | `requestedAt`, `expiresAt`, `usedAt` |
| `SignupRequest` | `SEC_SIGNUP_REQUEST` | §3 CREATE TABLE — eight columns only | `submittedAt`, `reviewedBy` / `reviewedAt` |

CORE.md "Audit fields" declares each of those `*By` / `*At` pairs system-set (Field Registry
read-only = Yes), i.e. the per-table equivalent of the audit columns.

Two further consequences:

- **`SignupRequest.submittedAt`** has no authenticated principal to record: API-SEC-002 is a
  pre-authentication, self-service endpoint.
- **`AuditLogEntry`** is the strongest case — SRS A3 ENT-SEC-011 states it outright: "this entity
  has no createdBy/updatedBy — it IS the audit record; actorUserId + occurredAt serve that purpose".

### Tables with no `is_active_fl`

`build-create-entity` A.1.18's `activate()` / `deactivate()` pair applies only to a table that has
an `IS_ACTIVE_FL` column. It therefore does **not** apply to `UserRoleAssignment`, the three grant
tables, `ActiveSession`, `AuditLogEntry`, `PasswordResetToken` or `SignupRequest`. Those rows are
created and deleted, or carry a different lifecycle column; inventing an `isActiveFl` field there
would contradict the schema. The equivalents actually implemented are:

- `ActiveSession.terminate(..)` — lifecycle is `terminatedAt IS NULL` (active) / `IS NOT NULL`
  (terminated).
- `PasswordResetToken.markUsed()` — lifecycle is `requestedAt` / `expiresAt` / `usedAt`.
- `SignupRequest.approve(..)` / `reject(..)` — lifecycle is `statusCode` over SIGNUP_STATUS.

All three are pure field mutation; the guard is always a Domain decision taken first.

---

## 2. Append-only enforcement for `AuditLogEntry` (POL-SEC-009)

DATA-DOM-TRANSACTIONAL.md ENT-SEC-011 says the immutability "is enforced by omission — no
UPDATE/DELETE mapping exists on this repository at all, not by a DB trigger". What that means in
code:

- `AuditLogEntry` declares **no Lombok `@Setter`** (unlike every other SEC entity), no mutator, and
  no state transition. A row is built once through `@SuperBuilder` and never touched again.
- It has **no Domain companion**: an append-only record makes no "is this operation allowed?"
  decision (A.0.1 Decision Test).
- `AuditLogEntryRepository` declares no update and no delete method. **Java cannot remove an
  inherited method**, so `JpaRepository`'s `delete`, `deleteById`, `deleteAll*` and the update half
  of `save` remain visible on the type — *no SEC service may call any of them*. This is a
  convention, not a compiler-enforced constraint, and is the one place where POL-SEC-009 could be
  violated without a build failure.
  **Forward action — deliberately deferred, not forgotten:** the sanctioned fix is a build-time
  ArchUnit guard failing if any SEC service calls a `delete*` method on `AuditLogEntryRepository`
  — the same pattern `build-create-service` prescribes for its internal trusted-caller gate. It is
  NOT written here because `governance/GOVERNANCE-RULES.md` forbids creating any file under
  `src/test/java/` outside an explicit request or the gated `execute-backend-test` phase. Add it
  when that phase runs; until then this bullet is the only thing standing between POL-SEC-009 and
  a silent violation.
- The only sanctioned write is an insert of a fresh instance (`save(newEntity)`), performed inline
  by each other service's orchestration step, with shape
  `{eventTypeCode, actorUserId, occurredAt=now(), targetRef, detailsAr, detailsEn, ipAddress}`.
- Search (QR-SEC-023, API-SEC-023) and export (QR-SEC-024, API-SEC-024) are both FIND_BY_CRITERIA
  and use the inherited `findAll(Specification, Pageable)` / `findAll(Specification, Sort)` driven
  by the shared `SpecBuilder` / `PageableBuilder` — no bespoke query is declared.

---

## 3. RULE-SEC-005 — the conflicting-pair finding (ABSENT, resolved without inventing a contract)

RULE-SEC-005 ("the system shall prevent assigning a user, by any combination of roles, both
actions of a module-declared conflicting pair") and QR-SEC-031 both hinge on two actions being
*declared* a conflicting pair by their owning module. **No SEC v1 artifact says where that
declaration lives.** Checked and empty: srs-sec.md A3 entities, A6 lookups, A7 defaults/state
machines, A8 cross-module, Part B screens B3/B5, §7.1 permissions, db-script-sec.md's 104 DBF
across 13 tables, and API-SEC-020's action-registration request contract. There is no ENT, no DBF,
no table and no API for a conflict register.

Supporting facts:

- REQ-SEC-020 is `Pattern : optional` — "Where a consumer module declares two of its actions as
  conflicting" — an EARS precondition that SEC v1 never establishes.
- The platform's only real conflicting pair is **FIN-owned and FIN-enforced**, not SEC-enforced:
  `governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066` separates
  `PERM_FIN_PERIODS_CLOSE_APPROVE` from `PERM_FIN_JOURNAL_ENTRIES_CREATE` "by platform convention
  (an administrative guideline enforced by role design, not a database constraint)", with FIN's own
  service layer checking at hard-close / year-end-close time that no single user holds both, per
  role union — reading the two roles' user sets through SEC's role/grant read APIs (:383-386).
- RULE-FIN-015 (`srs-fin.md:1012`) is scoped to ENT-FIN-008 and carries FIN's own error code
  `FIN-403-SOD-VIOLATION` (403). No FIN-owned P1/P2/P3_1 artifact ever routes through
  `SEC-409-SOD-CONFLICT`.

**Consequence for the implementation.** API-SEC-008 and API-SEC-017 are implemented exactly as
their Orchestration lines read, with the RULE-SEC-005 step driven by a conflicting-counterpart set
that is **empty in SEC v1** (a private service-side resolver returning empty). The Domain guards
(`UserRoleAssignmentDomain`, `RoleActionGrantDomain`) and both QR-SEC-031 repository methods
therefore keep a real caller, and A.2.9 still holds at ALIGN-BE. No table, column, route, error
code or rule was invented.

**Open, human-only question** (a P1/P2 amendment, not an execution decision): whether SEC v1 should
own a conflicting-pair register at all, rather than leaving REQ-SEC-020's precondition permanently
unestablished. Also recorded in `governance/modules/SEC/execution-state.json` → `api_doc_gaps[]`.

---

## 4. Convention deviations

### 4.1 SEQUENCE primary keys, not `GENERATED ALWAYS AS IDENTITY`

`db-script-sec.md` §3 declares every PK as `GENERATED ALWAYS AS IDENTITY` and its BLOCK 1
("sequences") as "none". The repo's entity contract mandates the opposite: `build-create-entity`
A.1.3 / A.1.4 require `GenerationType.SEQUENCE` + `@SequenceGenerator`, and
`GenerationType.IDENTITY` is an automatic rejection trigger for `gov-enforce-backend-contract`.
Every other module here (CU / NOTIF / FILE, and the old SEC `V2`) uses explicit `SEQ_<TABLE>`
sequences.

Decided at DATA-DOM-MASTER and recorded in the `V16__sec_schema.sql` header. PK columns are plain
`BIGINT NOT NULL`, fed by the 13 `SEQ_SEC_*` sequences created in that migration's BLOCK 1
(`allocationSize = 1`, matching `CACHE 1` on the DB side). Every table / column / constraint /
index name is otherwise verbatim from the db-script.

### 4.2 Native `BOOLEAN` columns mapped to plain `Boolean`, no converter

`db-script-sec.md` gives `IS_ACTIVE_FL` as a native Postgres `BOOLEAN NOT NULL DEFAULT TRUE`
(DBF-SEC-009 User, -020 Role, -034 ModuleRegistry, -044 ScreenRegistry, -055 ActionRegistry). The
shared converters `com.erp.common.converter.BooleanNumberConverter` and `BooleanCharYNConverter`
exist for **numeric** and **CHAR(1)** flag columns respectively and would break a native `BOOLEAN`
column. The five SEC flags are therefore mapped as a plain `Boolean` field with no
`@Convert`. The rationale is stated once on `User.isActiveFl`; the other four carry
"no converter (see User)".

### 4.3 Unique-constraint naming

The `@UniqueConstraint` names in the SEC entities are verbatim from `db-script-sec.md` §5b and
match `build-create-entity` A.1.13's `UQ_<TABLE>_<DESC>` form — `UQ_SEC_USER_USERNAME`,
`UQ_SEC_ROLE_CODE`, `UQ_SEC_USER_ROLE_USER_ROLE`, `UQ_SEC_ROLE_{MODULE,SCREEN,ACTION}_GRANT_*`,
and so on. The only liberty taken, inherited from the db-script rather than introduced here, is an
abbreviated `<DESC>` on two of them: `UQ_SEC_ACTION_REG_PERM` covers `permission_code` and
`UQ_SEC_SCREEN_REG_PAGE` covers `page_code`. Entity, db-script and `V16__sec_schema.sql` all agree;
do not "normalise" these names — the constraint identifier is what a Postgres integrity-violation
message reports and what the SVC-API duplication mapping keys on.

### 4.4 `SecErrorCodes` constant naming

Unlike `NotifErrorCodes` / `CuErrorCodes`, where the constant name equals its value, the SEC
catalog codes contain hyphens — legal on the wire, illegal in a Java identifier. The constant name
is therefore the code with `-` replaced by `_` (`SEC_409_USER_DUP` = `"SEC-409-USER-DUP"`). The
literal hyphenated strings are what `governance/modules/SEC/test_gen/backend-test-plan-sec.md`
asserts on, so they must not be normalised. One format is used consistently across the whole module
(gov-enforce-error-handling CHECK 3), never mixed with the descriptive `<ENTITY>_<SCENARIO>` form.
Every PLATFORM-STD row is covered by ADR-SEC-002 (a single umbrella decision, no per-row ADR).

---

## 5. Layer-placement decisions worth remembering

- **`RoleModuleGrantDomain` does not own RULE-SEC-003.** The cascade revoke of dependent screen and
  action grants is an *action* taken on delete, not a permit/deny decision;
  DATA-DOM-TRANSACTIONAL.md ENT-SEC-007 puts its "owner layer: service". It is SVC-API
  orchestration in API-SEC-015. What the Domain object does own is API-SEC-014's duplication guard
  (`UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE` → `SEC-409-GRANT-DUP`). API-SEC-014's other half — "role
  and module must be active" — has no catalog code of its own; its Errors line offers only
  `SEC-404-ROLE` / `SEC-404-MODULE`, so an inactive role or module is resolved by the service as a
  load-time not-found, and no new error code was invented for it.

- **`ActionRegistryDomain` does not derive `permissionCode`.** `PERM_<pageCode>_<actionCode>` is a
  pure string derivation, not a decision about whether an operation is permitted, so it fails the
  A.0.1 Decision Test. API-SEC-020's Orchestration line places it in the service ("resolve screen
  by pageCode → derive permissionCode → validate uniqueness"), which is also the only layer holding
  the request's `pageCode`. The service derives it and passes the finished code in; the entity's
  `@PrePersist` still owns case normalization (A.1.17).

- **`UserDomain` and `SignupRequestDomain` exist despite "DOMAIN RULES: none scoped alone".**
  That line in DATA-DOM-MASTER.md / DATA-DOM-TRANSACTIONAL.md refers to the SRS §A5 `RULE-SEC-*`
  register only. `gov-enforce-backend-contract` LAYER 0 is unconditional and names state-transition
  checks explicitly (A.0.1), so the reactivation guard (API-SEC-010) and the PENDING-only signup
  transition (API-SEC-011) each require a Domain object. `ActiveSessionDomain` exists for the same
  reason (API-SEC-026's `terminatedAt IS NULL` guard).

- **QR-SEC-038 is a fetch, not an EXISTS.** The Query Reference Catalog states it as
  "EXISTS — token unexpired and unused", but that predicate *is* RULE-SEC-006's verdict, and
  `build-create-repository` forbids a query predicate that decides a business outcome. The row is
  fetched by `PasswordResetTokenRepository.findByTokenHash` and judged by
  `PasswordResetTokenDomain.assertUsable(now)` — which is what API-SEC-004 needs anyway, since it
  must load the token to reach its user and stamp `usedAt`. The comparison clock is passed in
  rather than read from `Instant.now()` inside the Domain object, keeping the decision
  deterministic and testable. An unknown token and an expired one both report
  `SEC-409-RESET-TOKEN-INVALID`, so a caller cannot distinguish them.

- **`ActiveSessionDomain` has `from(..)` but no `create(..)`.** A session row is minted by the login
  flow itself (API-SEC-001) with no permit/deny decision attached.

- **Both QR-SEC-031 shapes live on `RoleActionGrantRepository`.** The catalog entry is shared
  between API-SEC-008 (does this *user* already hold the action, through any assigned role?) and
  API-SEC-017 (does *any user holding this role* already hold it?), and is catalogued against
  ENT-SEC-009, so it is declared once there rather than duplicated on
  `UserRoleAssignmentRepository`.

- **`RoleActionGrantDomain.GATEWAY_ACTION_CODE = "VIEW"`** is passed into the QR-SEC-030 query as a
  parameter rather than hard-coded in JPQL, so the query carries no business constant. Its source
  is `profile.conventions.security_model.gateway_action`, cited by RULE-SEC-007.

---

## 6. Plan-vs-schema discrepancies resolved during execution

All three are also recorded in `governance/modules/SEC/execution-state.json` → `api_doc_gaps[]`.

1. **`RoleModuleGrant.grantedAt` (DBF-SEC-064).** DATA-DOM-TRANSACTIONAL.md's ENT-SEC-007 FIELDS
   table spells the column `grant_at`; `db-script-sec.md` says `granted_at` in all three places
   (§1 registry row, §3 CREATE TABLE, BLOCK 4 COMMENT ON), matching the sibling grant tables.
   Resolved against the schema as ground truth — entity and `V16__sec_schema.sql` both use
   `granted_at`; the plan's `grant_at` is a typo.
2. **SEC error-code format.** CORE.md declares the runtime code format as `SEC-<3-digit-sequence>`
   (e.g. `SEC-001`), but no §Error Catalog row uses that shape — every row is
   `SEC-<HTTP>-<SCENARIO>`, and `test_gen/backend-test-plan-sec.md` asserts on those literal
   strings. The catalog's literal codes are authoritative and are the constant values in
   `SecErrorCodes`; CORE.md's prose is a stale generalisation, not a competing contract.
3. **RULE-SEC-005's conflicting-pair source** — see §3 above.

---

## 7. SVC-API-SEARCH — the GET-transport deviation and its consequences

`build-create-controller` A.6.6 mandates `POST /search` with a `@RequestBody`, and A.3.9–A.3.11
mandate a `<Entity>SearchRequest extends BaseSearchContractRequest`. **SEC's seven read-only APIs
deviate deliberately**: every `Endpoint :` line in `SVC-API-SEARCH.md` specifies `GET`, and
`test_gen/backend-test-plan-sec.md` asserts the GET form literally (TC-SEC-021 `GET /api/v1/sec/menu`,
TC-SEC-022/023 `GET /api/v1/sec/dashboard`, TC-SEC-025 `GET /api/v1/sec/audit-log`, TC-SEC-027
`GET /api/v1/sec/sessions`). A `POST /search` implementation would fail those tests. CU/NOTIF/FILE
keep the `POST /search` form and were not touched.

Consequences, applied consistently:

- The seven endpoints are `@GetMapping` with `@RequestParam(required = false)` values. No
  `*SearchRequest` body DTO exists anywhere in SEC, so A.3.9/A.3.10/A.3.11 are **not applicable**
  rather than violated.
- The shared search layer is still mandatory. Each service builds a
  `com.erp.common.search.SearchRequest` programmatically and drives `SpecBuilder.build(..)` +
  `PageableBuilder.from(..)` with a `SetAllowedFields` whitelist, exactly as a body-driven search
  would. `SecSearchSupport` (package-private, `com.erp.sec.service`) is the stand-in for
  `BaseSearchContractRequest.toCommonSearchRequest()` that the missing DTO would have provided.

### `SEC-400-INVALID-SORT` is raised by SEC, because the shared layer does not signal

`PageableBuilder.from(..)` **silently drops** a sort field that is not in the whitelist (it returns
an unsorted `PageRequest`), and `SpecBuilder.build(..)` silently skips a filter on a non-whitelisted
field. Neither throws. `SEC-400-INVALID-SORT` already exists in `SecErrorCodes` and both bundles, so
`SecSearchSupport.commonRequest(..)` checks the incoming sort field against the same
`ALLOWED_SORT_FIELDS` set and throws `LocalizedException(VALIDATION_ERROR, SEC_400_INVALID_SORT, field)`
before `PageableBuilder` is reached. No shared class was modified and no error code was added. This
is structural input validation, not a business rule (`build-create-dto` scope note), so it is not an
A.5.18 Domain-delegation case.

### `ALLOWED_SORT_FIELDS` per API, from each screen's SRS B2

CORE.md's Search contract fixes the whitelist as "exactly the columns listed as filters in each
screen's SRS B2":

| API | Screen | B2 filter line | `ALLOWED_SORT_FIELDS` |
|---|---|---|---|
| API-SEC-005 | SEC_USERS | username/email, fullName, statusCode | `username, email, fullNameAr, fullNameEn, statusCode` |
| API-SEC-012 | SEC_ROLES | role code/name, active flag | `code, nameAr, nameEn, isActiveFl` |
| API-SEC-021 | SEC_MODULE_REGISTRY | module code, screen page code | `code` only — see below |
| API-SEC-023 | SEC_AUDIT_LOG | eventTypeCode, actorUserId, date range | `eventTypeCode, actor, occurredAt` |
| API-SEC-025 | SEC_SESSIONS | user, IP address | `user, ipAddress` |

Two B2 entries expand to two columns each (`fullName` → `fullNameAr`/`fullNameEn`, role `name` →
`nameAr`/`nameEn`) because the module stores both languages; the request parameter stays singular
and matches either column. API-SEC-021's paged root is the **module**, so `pageCode` — a column of
the child screen — cannot be a sort field there; it stays a filter only.

### The three predicates the shared layer cannot express

`SearchOperator` has no `OR` and no `IS NULL`, so three predicates are written directly and
`.and(..)`-ed onto the `SpecBuilder` result rather than replacing it:

- `UserService.fullNameMatches` / `RoleService.nameMatches` — the bilingual OR above.
- `SessionService.notTerminated` — `terminatedAt IS NULL`, applied unconditionally as a
  server-side invariant (API-SEC-025 Validations); a client cannot switch it off or reach a
  terminated session through the endpoint.
- `RegistryService.hasScreenWithPageCode` — an `EXISTS` subquery on the child screen (A.5.17's
  explicit-join case).

### One effective-grant read path, two consumers

`MenuService` owns the single read path CORE.md's REQ-SEC-033 paragraph names: `effective()` is
API-SEC-027, and `effectivePermissionCodes()` is the same traversal projected to
`ActionRegistry.permissionCode`, consumed by API-SEC-022's per-widget filter (REQ-SEC-023, whose
Entities line includes ENT-SEC-009 — so a widget's "screen VIEW" is an **action** grant, not a
screen grant). Both are gated `@PreAuthorize("isAuthenticated()")` as a plain string literal:
SRS B4 gives SCR-REQ-SEC-010 no page code, so there is no constant to reference — its content, not
a permission on itself, is the boundary.

Both queries exclude an **inactive role** (`SCR-REQ-SEC-005 B4` makes deactivation the role's only
DELETE, so an inactive role is a withdrawn one) and inactive registry rows (`SCR-REQ-SEC-006 B3`
calls deactivation "deactivating a stale row"). API-SEC-021 filters screens and actions to active
per its own Response line, but not modules, which that line does not scope.

**Ordering dependency for SEC-BE.** `com.erp.main.config.SecurityConfig` is still `permitAll` with
method security disabled, and `SecurityContextHelper.getCurrentUsername()` falls back to the literal
`"system"`. With no principal, `GET /api/v1/sec/menu` answers `200` with `data: []` and
`GET /api/v1/sec/dashboard` answers `200` with every widget omitted, because `"system"` matches no
`SEC_USER` row. Both become correct the moment SEC-BE installs the JWT validating filter and the
REQ-SEC-033 gateway; neither was worked around here, and `SecurityConfig` was not modified.

### REQ-SEC-023 is implemented by omission, not by zeroing

`DashboardResponse` carries `@JsonInclude(JsonInclude.Include.NON_NULL)` and every sub-figure is a
nullable object. A widget the caller has no source-screen VIEW for is simply never set, so it
disappears from the JSON rather than serializing as `null` or `0` — which is what AC-SEC-023 and
TC-SEC-023 assert. The six widgets map to four source screens: `usersOverview` and
`onboardingFunnel` → SEC_USERS (its B3 "Pending sign-ups" tab owns the SignupRequest rows),
`failedLogins24h` and `recentActivity` → SEC_AUDIT_LOG, `activeSessions` → SEC_SESSIONS,
`rolesPermissionsSummary` → SEC_ROLES. Nothing is cached (`gov-enforce-caching-rules`' approved
register is empty, and REQ-SEC-022 requires every figure computed at that moment).

The three figures API-SEC-022 names but never defines — `recentActivity`'s N,
`privilegedRoleCount` and `stalledCount` — are recorded in `execution-state.json` → `api_doc_gaps[]`
and implemented as named constants in `DashboardService`, not as configuration properties.

---

## 8. SVC-API-SEARCH — the GET transport reversed to `POST /search` (2026-09-11)

§7's deviation was accepted too quickly and has been reversed for the five criteria-driven
searches. It rested on the claim that the governed test plan pins the GET form; in fact
`test_gen/backend-test-plan-sec.md` contains no test at all for API-SEC-005, API-SEC-012 or
API-SEC-021, and the only two it does cover (TC-SEC-025 → API-SEC-023, TC-SEC-027 → API-SEC-025)
assert nothing about the verb beyond their own `Exercises` line.

### The split: criteria-driven searches move, parameterless resources stay

| Now `POST` + `<Entity>SearchRequest` | Still `GET` |
|---|---|
| API-SEC-005 `POST /api/v1/sec/users/search` | API-SEC-022 `GET /api/v1/sec/dashboard` — no criteria |
| API-SEC-012 `POST /api/v1/sec/roles/search` | API-SEC-027 `GET /api/v1/sec/menu` — no criteria |
| API-SEC-021 `POST /api/v1/sec/registry/search` | API-SEC-024 `GET /api/v1/sec/audit-log/export` — file download |
| API-SEC-023 `POST /api/v1/sec/audit-log/search` | |
| API-SEC-025 `POST /api/v1/sec/sessions/search` | |

Rationale: the GET form bypassed `BaseSearchContractRequest`, the platform's uniform search
contract. Each filter's field *and* operator were fixed in the service signature, so a client could
not express `statusCode != DISABLED`, adding a filter meant changing controller, service and plan
together, and Arabic filter text went through URL encoding rather than a JSON body. CU, NOTIF and
FILE already used `POST /search`; SEC was the only module out of line. A.3.9, A.3.10, A.3.11 and
A.6.6 now pass instead of being recorded deviations.

### The three behaviours the move had to preserve

- **Bilingual OR** — `fullName` (API-SEC-005) and `name` (API-SEC-012) still match either language
  column. The client sends them as ordinary filters; the DTO lifts them out of the generic set
  (`toCommonSearchRequest(Set.of(..))`) and the service keeps the `.and(fullNameMatches/nameMatches)`
  predicate, because `SearchOperator` still has no OR.
- **`terminatedAt IS NULL`** (API-SEC-025) is still applied unconditionally, outside the
  client-supplied filter set, and `terminatedAt` is in neither whitelist — so no request body can
  switch it off or reach a terminated session.
- **`pageCode`** (API-SEC-021) is still an EXISTS subquery on the child screen row, and
  `ALLOWED_SORT_FIELDS` is still `{code}`.

### Consequences inside the services

`SecSearchSupport` survives with a narrower job: the SEC-400-INVALID-SORT guard (`PageableBuilder`
still silently drops an unknown sort field, so the check must precede it), the `addFilter` helper
that API-SEC-024's export still needs, and an `Instant` value converter, because a JSON body carries
`occurredAt` as a string where a query parameter arrived already typed. An association filter
(`ActiveSession.user`, `AuditLogEntry.actor`) cannot take a raw id through `SpecBuilder`, so those
two searches keep the whole filter set as `ALLOWED_SORT_FIELDS` but expose a narrower
`ALLOWED_FILTER_FIELDS` to client-supplied filters and resolve `userId` / `actorUserId` to a
reference themselves — the A.3.10/A.3.11 parent-id-extractor shape.

One behaviour did change: API-SEC-021's `moduleCode` was uppercased server-side before comparison
and no longer is, since the client now names the field and operator itself. An `EQUALS` filter on
`code` matches the stored (uppercase) form exactly; `LIKE` is case-insensitive in `SpecBuilder` and
covers the lenient case.

### Documents realigned the same day

`P3_1/backend-execution-plan-sec.md` (base plan — API table rows, `Endpoint :` and `Request :`
lines, API contract summary), `packages/backend-execution/SVC-API/SVC-API-SEARCH.md` (the same five
`Endpoint :` and `Request :` lines), `packages/backend-execution/_SECTIONS.md` (the EXECUTION PLAN
INDEX API table's five rows — missed on the day and brought into line only on 2026-09-11, see §13)
and `test_gen/backend-test-plan-sec.md` (TC-SEC-025 and TC-SEC-027 verb/endpoint only — no status
code, error code or ar/en message text touched). The P1 SRS B5 tables were realigned too: all five
rows (`srs-sec.md:905, 932, 961, 1003, 1025`) now read `POST .../search`, changed in commit
`d916c62`, so requirements text and implementation contract agree on the verb and path.

---

## 9. SEC-BE — authentication and authorization switched ON (2026-09-11)

Until this phase `com.erp.main.config.SecurityConfig` carried `anyRequest().permitAll()` and no
`@EnableMethodSecurity`, so all 27 `@PreAuthorize` annotations in `com.erp.sec` were inert and every
request was permitted. That is no longer true. SEC-BE was executed ahead of DOC / INT-C / INT-R at
the user's explicit instruction; those three remain PENDING and `current_phase` was not advanced.

### What the app now enforces

| Situation | Result |
|---|---|
| No bearer token on any path but the four public ones | `401` + `SEC-401-INVALID-CREDENTIALS` |
| Token whose signature or expiry fails | same `401` (the context stays anonymous) |
| Token whose session row is missing or `terminatedAt IS NOT NULL` | same `401` (REQ-SEC-028) |
| Token of a user who is not ACTIVE / not `isActiveFl` | same `401` |
| Authenticated caller missing the endpoint's permission | `403` + `SEC-403-FORBIDDEN`, both catalog messages |

`SEC-401-INVALID-CREDENTIALS` is reused for the missing-token case because the §Error Catalog has no
"no credential presented" row and `GOVERNANCE-RULES.md` forbids inventing one. Its PLATFORM-STD
scope (ADR-SEC-002, "wrong/unknown credentials") covers a request that presents no usable credential.

### The authority model

An authority string **is** an `ActionRegistry.permissionCode` — `PERM_<PAGE_CODE>_<ACTION_CODE>`,
the same value `RegistryService` derives and `PermissionConstants` declares. `JwtAuthenticationFilter`
resolves them through `MenuService.effectiveAuthorityCodes()` — the effective-grant read path
CORE.md's REQ-SEC-033 paragraph names, with RULE-SEC-007 applied **(updated 2026-10-01)**;
`effectivePermissionCodes()` is the same read without the gateway, used by API-SEC-022. The filter installs a
provisional zero-authority authentication first, purely so that method's own
`@PreAuthorize("isAuthenticated()")` can be satisfied while the real authority set is being built; it
is replaced before the chain proceeds and no request is ever handled under it.

That query joins role → action grant only. It does not re-check the module and screen grants
REQ-SEC-033 also names, and does not need to: RULE-SEC-001 and RULE-SEC-002 are enforced at grant
time (API-SEC-016 / API-SEC-017), so an action grant already implies its screen grant and its module
grant. The seed satisfies the same chain by construction.

### RULE-SEC-007's placement

The gateway is applied **once, while the authority set is built**, not per endpoint: a non-VIEW
permission is dropped unless the same screen's `PERM_<PAGE_CODE>_VIEW` is also held. The screen of
each held code is its `SEC_SCREEN_REG` row, read in the same query as the codes — never parsed from
the code's text — so a multi-word action such as `PERM_FIN_PERIODS_CLOSE_APPROVE` is gated by
`PERM_FIN_PERIODS_VIEW` correctly **(updated 2026-10-01; the first implementation split the code on
its last underscore, which could not gate a multi-word action)**.

Proven at runtime on a throwaway database: with `PERM_SEC_USERS_VIEW` and `PERM_SEC_USERS_UPDATE`
both granted, `PATCH /api/v1/sec/users/1` reached the business layer (`409`
`SEC-409-INVALID-TRANSITION`); after revoking **only** the VIEW grant, the identical call answered
`403` `SEC-403-FORBIDDEN` while `PERM_SEC_USERS_UPDATE` was still present in `SEC_ROLE_ACTION_GRANT`.

### Why a denial carries the catalog code

`GlobalExceptionHandler`'s `AccessDeniedException` handler emits the generic `ACCESS_DENIED`, and it
is shared infrastructure this phase must not modify; a per-module `@ControllerAdvice` is forbidden
(CU.7). `SecForbiddenAdvisor` closes the gap instead: one `DefaultPointcutAdvisor`, bean-role
`ROLE_INFRASTRUCTURE`, ordered `HIGHEST_PRECEDENCE` so it sits outside the `@PreAuthorize`
interceptor, matching only `com.erp.sec.service.*`. It catches the denial and re-raises
`LocalizedException(Status.FORBIDDEN, SecErrorCodes.SEC_403_FORBIDDEN)`, which the shared handler
already maps correctly. This is CORE.md's "single method-level interceptor … declared once here,
never re-implemented per endpoint" — nothing was added per endpoint. Denials raised inside the filter
chain never reach the dispatcher, so `SecSecurityErrorHandler` writes the same envelope by hand for
those (Jackson's converters are not available that early).

### The seed and the bootstrap credential

`V17__sec_security_seed.sql` — V16 creates the 13 SEC tables and zero rows, so switching security on
without it would lock every caller out. It seeds 1 module, 9 screens, 13 actions, 1 role
(`SYS_ADMIN`), 1 module grant, 6 screen grants, 13 action grants, 1 user and 1 role assignment.

- The three public screens (`SEC_LOGIN`, `SEC_SIGNUP`, `SEC_PWD_RESET`) are registered but carry no
  action row and no grant — the matrix marks them public with no permission.
- Both cells the matrix marks "reserved" are seeded, because the matrix names a permission code for
  each. `PERM_SEC_ROLES_DELETE` is genuinely dormant (no v1 delete-role endpoint; deactivation is
  UPDATE) and is the one seeded code with no `PermissionConstants` constant.
  `PERM_SEC_MODULE_REGISTRY_UPDATE` is only "reserved" for row deactivation — `RegistryService`
  gates API-SEC-018/019/020 on it today, so omitting it would have made those three unreachable.
- `V10__sec_bootstrap_admin_user.sql` seeds into `SEC_USER_ACCOUNT`, which `V14` dropped; it is dead
  for this schema and is never edited (Flyway checksums). V17 is the forward fix, and reuses V10's
  BCrypt hash verbatim so the documented **dev-only `admin` / `admin`** credential keeps working.
  It is a published, well-known secret — change or disable it before any real environment.

### The internal-caller pattern (api_doc_gaps #5)

`com.erp.sec.security.InternalCallerContext` installs a synthetic authentication carrying one
authority, `INTERNAL_TRUSTED_CALLER`, for the duration of one in-process call and restores the
previous context in a `finally`. `PasswordResetService` wraps only the
`NotificationDispatchApi.dispatchIndependently(..)` call site in it, so the anonymous API-SEC-003 can satisfy
NOTIF's `@PreAuthorize("isAuthenticated()")` without NOTIF being touched. No request can obtain the
authority: `JwtAuthenticationFilter` is the only authentication entry point and strips that exact
string from every authority set it builds, and the chain configures no other mechanism that grants
authorities. It lives in `com.erp.sec.security` rather than `com.erp.common.util` because SEC is its
only consumer and `com.erp.common.**` is shared foundation this phase was scoped not to change.
Consequence **(updated 2026-10-01)**: because it is outside `com.erp.sec.crossmodule`,
`CrossModuleBoundaryArchTest` forbids any other module from importing it, so a principal-less caller
in another module (a scheduled job) has no sanctioned way to pass an `isAuthenticated()` gate yet.
Moving it to `sec.crossmodule` is the proposed fix and awaits a human decision.

The prescribed build-time ArchUnit guard (no `@RestController`/`@Controller` reaches the gated
method) is still owed. `src/test` now exists (`CrossModuleBoundaryArchTest`, the SEC integration
tests), and the append-only half of §2 is covered by `SecCoverageIntegrationTest`; this guard is
not yet written **(updated 2026-10-01)**.

The transaction half of gap #5 is **closed (updated 2026-10-01)**: NOTIF added
`NotificationDispatchApi.dispatchIndependently`, which sends in a REQUIRES_NEW transaction, and
`PasswordResetService` calls it. A dispatch failure can no longer mark SEC's transaction
rollback-only, so REQ-SEC-006's generic 200 holds.

### Blast radius outside SEC

`@EnableMethodSecurity` activates every `@PreAuthorize` in the deployable. When SEC-BE ran, only
four gates outside `com.erp.sec` were live (all `isAuthenticated()`); the rest of CU / NOTIF / FILE
sat behind `// TODO: SEC-PENDING` comments. **(updated 2026-10-01)** Those gates are now live:
every CU / NOTIF / FILE / MDL / FIN service method carries
`hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_…)` (or `isAuthenticated()` where
no screen exists), seeded by `V19`/`V21` (MDL), `V24`/`V25` (FIN) and `V31` (CU, NOTIF, FILE). No
`SEC-PENDING` marker remains. The chain's `anyRequest().authenticated()` answers `401` to any
request without a token.

---

## 10. Analysis artifacts realigned to what was built (2026-09-11)

Five of the six `api_doc_gaps[]` entries in `governance/modules/SEC/execution-state.json` were
decided during execution but left the upstream analysis documents still saying the old thing. This
pass corrected the **base** (unsplit) files as well as the split package files, so the two agree
line for line; each gap's `resolution` field was extended with the files it touched. Gap #5 (the
internal-caller pattern, §9 above) was already documented and was not reopened. `P2/db-script-sec.md`
was correct in every one of these and was not touched; `test_gen/backend-test-plan-sec.md` was not
touched either — nothing here changes a test.

Nothing was invented to close a gap. Where the specification is genuinely silent (gaps 3, 4, 6) the
documents now say so plainly and name the open decision, rather than reading as though the answer had
always been there.

| gap | what was untrue | what it now says | files touched |
|---|---|---|---|
| 1 — error-code format | CORE declared the runtime `code` format as `SEC-<3-digit-sequence>`, e.g. `SEC-001`; no Error Catalog row has that shape | format is `SEC-<HTTP-status>-<SCENARIO>`, e.g. `SEC-409-USER-DUP`, with the scenario segment omitted only for the generic `SEC-500` row | `P3_1/backend-execution-plan-sec.md` (Error-signalling paragraph, Error Catalog header, ALIGN `CORE (R1)` row), `packages/backend-execution/CORE/CORE.md`, `packages/backend-execution/_SECTIONS.md` |
| 2 — `grant_at` | ENT-SEC-007's FIELDS table gave DBF-SEC-064's column as `grant_at` | `granted_at`, matching db-script, the sibling grant tables, the entity and `V16__sec_schema.sql` | `P3_1/backend-execution-plan-sec.md`, `packages/backend-execution/DATA-DOM/DATA-DOM-TRANSACTIONAL.md` |
| 3 — RULE-SEC-005 has no pair source | read as an enforced rule, though SEC v1 declares no conflicting pair anywhere | DEFERRED in v1: no declaration surface exists, the guards are implemented and inert, the platform's only real pair is FIN-owned and FIN-enforced (`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066`), and a SEC-side register is an open P1/P2 decision | `P1/srs-sec.md` (REQ-SEC-020, RULE-SEC-005), `P3_1/backend-execution-plan-sec.md`, `packages/backend-execution/DATA-DOM/DATA-DOM-TRANSACTIONAL.md` |
| 4 — the approved sign-up's credential | APPROVE named no credential although `password_hash` is NOT NULL, and nothing said the owner is ever told the account exists | the account is created with an unusable random secret and the owner's route in is the existing API-SEC-003 → API-SEC-004 reset pair (RULE-SEC-006); the notification silence is recorded as an open product decision with its two candidate answers, neither chosen | `P1/srs-sec.md` (REQ-SEC-004), `P3_1/backend-execution-plan-sec.md` (API-SEC-011), `packages/backend-execution/SVC-API/SVC-API-CRUD.md` |
| 6 — three undefined dashboard figures | `recentActivity`'s N, the stalled window and "privileged" were named but defined nowhere | the operative values, marked as implementation-chosen defaults awaiting confirmation: N = 10, stalled = PENDING older than 7 days, privileged = holds ≥ 1 action grant whose `actionCode` is not the VIEW gateway | `P1/srs-sec.md` (REQ-SEC-022), `P3_1/backend-execution-plan-sec.md` (API-SEC-022), `packages/backend-execution/SVC-API/SVC-API-SEARCH.md` |

The three gap-6 values are read straight out of `DashboardService` (`RECENT_ACTIVITY_LIMIT`,
`STALLED_SIGNUP_AGE`, and `RoleActionGrantDomain.GATEWAY_ACTION_CODE` passed to
`RoleActionGrantRepository.countPrivilegedRoles`), so the documents quote the code rather than a
remembered value.

---

## 11. INT-C / INT-R — the one sanctioned cross-module call, and two inbound gaps (2026-09-11)

Both phases are documentation-and-verification only; no `.java` file was written, and NOTIF was
read but never changed.

### INT-C — "SEC consumes no other module's API" was false, and the fix was not to delete the call

`PasswordResetService.dispatchResetNotification` injects NOTIF's
`com.erp.notif.crossmodule.NotificationDispatchApi` and calls `dispatch(..)` on the API-SEC-003
path. That is a real in-process consumption of another module's API, and it is legitimate:
`srs-sec.md` §A8's *External service* table declares exactly one integration — Notifications, for
the password-reset message (REQ-SEC-029), `SOFT / optional` — and `build-create-service`'s
Cross-Module Calls section permits consuming a producing module's designated `crossmodule`
interface, which is precisely what `NotificationDispatchApi` is.

It is still not an `XM-*` row. The db-script §2 XM REGISTER records **consumed entities, tables
and FKs**; this call consumes none, so "0 XM · SEC is ROOT" stays true exactly as written. The two
statements only looked contradictory because INT-C had generalised a schema fact into a blanket
claim about APIs.

Compliance re-checked against the skill, by grep rather than assumption: SEC imports only
`com.erp.notif.crossmodule.{DispatchCommand, NotificationDispatchApi}` and nothing from
`notif.service` / `repository` / `entity` / `dto`; `PasswordResetService` is the only file in
`com.erp.sec` that names either type, so no Domain, mapper or controller holds the reference; the
argument is `DispatchCommand`, NOTIF's own record, not a JPA entity or internal DTO;
propagation intent is stated in the method's javadoc at the call site; and the call is wrapped in
`try/catch (RuntimeException)` that logs at WARN with the stack trace, so it neither surfaces as a
500 nor is silently swallowed. The one thing the catch cannot do — clear a rollback-only flag set
inside NOTIF's joined transaction — was `api_doc_gaps` #5, since closed by
`dispatchIndependently` (see §9, updated 2026-10-01).

Two further statements were made untrue by the same call and were corrected in PHASE 1 CORE (base
plan and `CORE/CORE.md`, kept byte-identical): the layer list said the service "integrates (none
for SEC — zero XM)", and *Cross-module contract placement* said "no inversion-of-control interface
is consumed by SEC". Every other `SEC is ROOT` / `zero XM` statement in the module is schema- or
entity-scoped and was left alone.

### INT-R — two inbound-contract gaps recorded, neither closed

`XM-INBOUND-GAP-1`: FIN's plan (`backend-execution-plan-fin.md:383-386`) runs RULE-FIN-015's SoD
check by "reading the two roles' user sets through SEC's role/grant read APIs". No such API
exists. Across the 27 implemented endpoints, `RoleGrantController` is write-only, role search
returns role attributes with no member list, user search has no role predicate, there is no
`GET /users/{id}/roles`, and API-SEC-027 (`GET /api/v1/sec/menu`) takes no input at all — it
resolves the caller from the session and returns modules→screens. Adding an endpoint is a P1/P2
decision, so none was added.

`XM-INBOUND-GAP-2`: NOTIF cannot resolve a SEC recipient's address.
`DefaultChannelProvider.sendEmail` reads it from `variables.get("email")` — its javadoc says NOTIF
has no crossmodule contact-lookup for a bare `recipientId` — while SEC's `DispatchCommand` carries
only `token` and `expiresAt`. Hence the SEC-BE run's `NOTIF_LOG` row `FAILED — missing recipient
email address`: REQ-SEC-029's inbound half now works (gap #5) but the outbound half does not
deliver. The same absence keeps NOTIF's own `XM-NOTIF-001` stubbed —
`DefaultRecipientStatusReader` short-circuits RULE-NOTIF-007 to `true` because
`com.erp.sec.crossmodule` does not exist. Either SEC supplies `email` among the dispatch
variables, or SEC exposes a `crossmodule` contact/status reader (which would also close
`XM-NOTIF-001`); the second is a new cross-module surface and therefore a human decision.
**Both gaps were later closed — see §14** (and, for `XM-NOTIF-001`, the 2026-10-01 note there).

---

## 12. ALIGN-BE — the self-check re-run honestly, and what it found (2026-09-11)

Phase 8 is verification and reporting only; no `.java` file, migration or i18n key was written.

### The generated verdict was false

`packages/backend-execution/ALIGN-BE/ALIGN-BE.md` defines its own content as the
`## Alignment self-check (ALIGN)` block in `packages/backend-execution/_SECTIONS.md` (mirrored
in `P3_1/backend-execution-plan-sec.md`). That block closed with `RESULT  PASSED ✓ — 0 findings`
while nine `api_doc_gaps[]` entries stood recorded against the same module. Re-checking its ten
assertion rows one at a time against the current artifacts and the implemented sources: **two
hold as written** (TRACEABILITY, MANIFEST), **one holds only because a gap corrected it**
(CORE R1 — as generated it certified a `SEC-<3-digit-sequence>` format that matched no catalog
row and no emitted code, `api_doc_gaps` #1), **four are partly true** (BINDING, QRC, API R3,
CROSS-MODULE, SECURITY R7 — five, counting SECURITY) and **one is outright false** (DECISIONS).

The single hardest fact: **`ADR-SEC-001` and `ADR-SEC-002` do not exist.** They are cited as
`erp/decisions/SEC/ADR-SEC-00N.md` throughout P2, P3.1 and `modules/project-registry.md`, and
`find . -iname 'ADR-SEC-*'` returns nothing — there is no `decisions/` directory anywhere in this
repository. Both decisions are stated inline in the artifacts that cite them and nowhere else, so
their "ACCEPTED" status has no artifact behind it. (Independently noted at
`governance/project-artifacts/generator-defect-report-and-fix-prompt.md:155-156`.)

Two more rows deserve naming here because they certify the opposite of what was built:

- **BINDING and QRC both name `GENERATED ALWAYS AS IDENTITY` as the generation object.** The
  module ships 13 `SEQ_SEC_*` sequences and `GenerationType.SEQUENCE`, per §4.1 and
  `GOVERNANCE-RULES.md` §Convention Precedence 1. The decision is settled and correct; the plan
  text was simply never brought along, so the self-check certified a fiction.
- **SECURITY (R7)'s ERP-4 sub-claim — "every POST/PUT/PATCH/DELETE API above states one" — is
  false.** API-SEC-002, API-SEC-003 and API-SEC-004 are POST mutations that write
  `SEC_SIGNUP_REQUEST`, `SEC_PWD_RESET_TOKEN` and `SEC_USER` rows and state "public — no
  permission required". The *seed* half of the same row, by contrast, is now genuinely checkable
  and genuinely true: all 9 Phase 7 page codes and all 13 permission codes are in
  `V17__sec_security_seed.sql`.

The block in both files was rewritten to say all of this, its `RESULT` row replaced with the real
count, and the two copies verified byte-identical (sha256 over the whole block region).

### The master validation, re-run

`gov-validate-backend-feature` was run for real against the implemented module — STAGE 0 build
order, STAGE 1 inventory, the 85 layer checks, the 37 cross-cutting checks, a real
`mvn -DskipTests clean compile`, and the CU.1–CU.8 gate. **122 / 133 applicable checks (91.7%) →
CONDITIONAL.** Fifteen checks are N/A with a stated reason and were removed from both numerator
and denominator rather than counted as passes; nine decided deviations (the PK strategy, native
`BOOLEAN`, `UQ_` naming, the missing `UsageResponse`, the POST/GET split, the reset-token 409,
the four public endpoints, the three DELETE-verb endpoints, the CSV export) are labelled and
costed, not silently passed. The CU gate passes 8/8. The compile is clean: 241 sources, exit 0,
no warning attributable to SEC.

What the module does genuinely well, verified rather than assumed: every one of the 21
business-rule throws lives in a `com.erp.sec.domain.*` class and none in a service (A.5.18);
all 28 error codes then registered are in both bundles (27 after §13's `SEC_500` removal);
27/27 endpoints carry `@Operation`, 20/20 request bodies
carry `@Valid`; and the 22 `PERM_`-based `@PreAuthorize` annotations match the 22 plan `Security`
lines that name a permission, one for one.

### Two new findings, appended to `api_doc_gaps[]`

1. **`ActiveSessionRepository.findNonTerminated(Pageable)` is dead code** (as found; closed at
   §13) — the only one of the
   module's repository methods with no caller. It carried QR-SEC-025's declared `JOIN FETCH`
   + count query, orphaned when §8 moved API-SEC-025 onto `SpecBuilder`. A.2.9 is on the
   automatic-rejection list. **Closed 2026-09-11 — see §13.**
2. **`SEC-500` is contracted but unreachable** — eight API blocks (the finding first said nine;
   the true count is eight) name it as their only error and both bundle messages exist, but
   `SecErrorCodes.SEC_500` has zero references and the shared
   `GlobalExceptionHandler` answers `INTERNAL_ERROR`. No fix is available inside SEC:
   `GlobalExceptionHandler` is shared foundation and CU.7 forbids a per-module `@ControllerAdvice`.
   **Closed 2026-09-11 — see §13:** SEC stopped claiming the code instead.

Three further findings were reported but **not** appended, each with its reason: the seeded
`PERM_SEC_ROLES_DELETE` missing from `PermissionConstants` (the Phase 7 matrix already marks it
reserved), `/api/v1/sec/roles` being served by two controllers (a code-structure question, not a
documentation gap), and the two absent ADRs (already recorded in
`generator-defect-report-and-fix-prompt.md`).

One observation worth carrying into the test phase: `activate()`/`deactivate()` on `Role`,
`ModuleRegistry`, `ScreenRegistry` and `ActionRegistry` have no caller, because SEC v1 exposes no
endpoint that deactivates a role or a registry row — while `V17` seeds and grants
`PERM_SEC_ROLES_DELETE` for exactly that, and SRS B4 calls deactivation the role's only DELETE.

Full per-check evidence, every N/A reason and every deviation's justification:
**`governance/project-artifacts/sec-alignment-report.md`**.

---

## 13. The two ALIGN-BE gaps closed, and the index-table drift they uncovered (2026-09-11)

`api_doc_gaps[]` #10 (F-ALIGN-1) and #11 (F-ALIGN-2) were the only two findings ALIGN-BE left
open inside SEC's own boundary. Both are now closed. A second-agent debate (STEP 1.4 of the
backend orchestration protocol) converged on candidate (a) for each, the contested facts were
re-verified against the files, and the human chose the source-side disposition for #11.

### #10 — the dead QR-SEC-025 query, deleted

`ActiveSessionRepository.findNonTerminated(Pageable)` was the module's only repository method with
no caller (A.2.9, an automatic-rejection trigger). The decisive fact is that **no artifact ever
required it**: `P3_1/backend-execution-plan-sec.md:792` reads
`Repository : QR-SEC-025 · join NONE · transaction READ_ONLY`, and the QRC at `plan:376` already
registers QR-SEC-025 as `FIND_BY_CRITERIA`. The plan therefore already describes the
`SpecBuilder` + `PageableBuilder` path `SessionService` actually runs; the orphan `@Query` was the
divergence. The method and its two now-unused imports (`Page`, `Pageable`) were deleted and **no
governance file was touched for this gap**.

Accepted consequence, deliberately not fixed: that method was the module's only fetch join for
API-SEC-025's page, so `ActiveSessionMapper.toResponse` now dereferences the LAZY `user` once per
row inside the read-only transaction — an N+1. Correct but slower. `join NONE` mandates no fetch
join, so adding an `@EntityGraph` would be an un-mandated optimisation and was left out of scope.

### #11 — SEC stops claiming a code it does not own

The shared `com.erp.common.web.GlobalExceptionHandler`'s `@ExceptionHandler(Exception.class)`
answers `.code("INTERNAL_ERROR")`, `CommonErrorCodes.INTERNAL_ERROR` already exists, and
`gov-enforce-error-handling` both forbids a module modifying shared exception infrastructure and
assigns platform-standard codes to `CommonErrorCodes`. CU.7 forbids a per-module
`@ControllerAdvice`. SEC could not make its own claim true, so it stopped asserting it.

Governance amended (plan and its `packages/backend-execution/` twins kept identical):

- The **8** `Errors :` lines — `plan:719, 733, 747, 761, 776, 790, 804, 1093`, twinned at
  `SVC-API/SVC-API-SEARCH.md:14, 28, 42, 56, 71, 85, 99` and `SVC-API/SVC-API-INT.md:112` — now
  read `INTERNAL_ERROR only (platform-standard, shared handler)` / `none beyond platform-standard
  (§Error Catalog INTERNAL_ERROR)`. The count is **eight**, not the nine §12 and the alignment
  report first recorded.
- The §Error Catalog row is now `INTERNAL_ERROR | PLATFORM-STD (infrastructure, shared
  GlobalExceptionHandler — not module-scoped)`, with both message cells copied verbatim from the
  `INTERNAL_ERROR=` entries in `messages.properties` / `messages_ar.properties`, so the catalog
  states what the wire actually carries.
- The CORE error-signalling paragraph (`plan` + `CORE/CORE.md`) no longer claims a scenario-less
  code exists: **every** `SecErrorCodes` value carries the full `SEC-<HTTP-status>-<SCENARIO>`
  shape, and the platform 500 row is owned and emitted by the shared handler, not registered by
  the module.
- `P3_1/registry-exec-be-sec.md`'s CATALOG arithmetic was wrong independently of this change
  (`27 + 2 generic = 29`); the catalog has **28** rows, so it now reads `26 + 2 generic = 28`.

Source, on an explicit human decision — **no rule compelled it** (the automatic-rejection list
covers dead *repository methods*, not constants); it matches CU, NOTIF and FILE, none of which
declare a `_500` constant:

- `SecErrorCodes.SEC_500` deleted with its javadoc → **27** constants.
- `SEC-500=` deleted from `messages.properties` and `messages_ar.properties` → **27** SEC keys in
  each bundle, pairing 1:1 with the 27 constants (gov-enforce-error-handling CHECK 4).
- `MenuService.resolveCaller()`'s javadoc rewritten: it named `SEC-500`, and it also still claimed
  the JWT validating filter was not yet installed — false since §9. No executable line changed.

`com.erp.common` was not touched, and no `@ControllerAdvice` was added under `com.erp.sec`.

**Durable cross-module note.** `governance/modules/MDL/P3_1/backend-execution-plan-mdl.md:540` and
`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1118` carry the byte-identical
`<MOD>-500 | PLATFORM-STD (infrastructure) | any | 500 | unhandled server error | حدث خطأ في الخادم
| A server error occurred |` row. This is a generator-level defect, not a SEC one, and it will
recur verbatim when MDL and FIN are built. Neither file was modified.

### The drift the debate uncovered

`packages/backend-execution/_SECTIONS.md`'s EXECUTION PLAN INDEX API table still carried the
pre-§8 `GET` verbs and collection paths for API-SEC-005, 012, 021, 023 and 025, while the plan's
twin table already carried `POST .../search` — so the "kept byte-identical" claim in the alignment
report's Part 5 was untrue of the file as a whole. The five rows were copied from the plan
(authoritative, and matching the five `@PostMapping("/search")` controllers); the two tables now
`diff` clean. API-SEC-022, 024 and 027 are genuinely `GET` and were left alone. Recorded as a new
`NAMING_MISMATCH` entry in `api_doc_gaps[]`, already resolved.

---

## 14. SEC's first cross-module read surface — gaps #8 and #9 closed together (2026-09-11)

`api_doc_gaps[]` #8 (`XM-INBOUND-GAP-1`) and #9 (`XM-INBOUND-GAP-2`) were the two findings that
pointed *outward*: FIN assuming a SEC read API that did not exist, and NOTIF unable to resolve a
bare `recipientId`. A second-agent analysis converged on one answer for both, and the human
authorized it together with the requirement that it be reflected back into the analysis
artifacts. This section supersedes §11's closing sentence that
"`com.erp.sec.crossmodule` does not exist".

### The shape, and why it is not an endpoint

`.claude/skills/build-create-service/SKILL.md`'s "Cross-Module Calls" settles it: "Modules are
package-by-feature areas inside one deployable — always deployed together. Cross-module reads go
through **direct Spring interface injection**, not loopback HTTP." So the candidate both gap
entries first listed — "SEC v1 gains a read API" — was the one shape the binding skill forbids.
The original findings still stand exactly as written: no SEC *endpoint* returns a role's user set,
and none was added. The HTTP surface was unchanged by this work, and `/v3/api-docs` gained no path.

Built per the same skill's "Exposing this module to others" — a narrow interface plus its
read-model, implemented by a small dedicated class delegating to the internal service rather than
on the service itself:

- `crossmodule/SecUserDirectoryApi.java` — two read methods, nothing else.
- `crossmodule/UserContact.java` — a `record` carrying DBF-SEC-001 `user_pk`, DBF-SEC-003 `email`,
  DBF-SEC-005/006 display names and `active`, derived from DBF-SEC-007 `status_code`. Never
  `passwordHash` (DBF-SEC-004, POL-SEC-004), never a reset or session token, an audit row, or any
  role/grant data — and never the `User` entity or an internal DTO.
- `crossmodule/SecUserDirectoryApiImpl.java` — `@Component`, injects `UserService` and **no**
  repository (A.2.3), so the exposed surface stays narrower than the internal service.

### #8 — the permission-holder read

`SecUserDirectoryApi.findUserIdsHoldingPermission(String)` → `UserService` →
`RoleActionGrantRepository.findUserIdsHoldingPermission`, registered as **QR-SEC-039**: the
DISTINCT `UserRoleAssignment` user pks whose role appears in the `RoleActionGrant` rows for that
`permission_code`, with the action and the role both active. It is the exact inverse of
`findEffectivePermissionCodesForUser` (QR-SEC-027) and mirrors its active-flag predicates. Every
column already existed — DBF-SEC-026/027, DBF-SEC-071/072, DBF-SEC-050/055, DBF-SEC-020 — so **no
migration was written**. The method answers with user ids only: SEC neither learns nor evaluates
FIN's conflicting pair, so REQ-SEC-020's "inert in SEC v1" Note is untouched.

No `PERM_` constant was invented for it. `db-script-sec.md` makes `SEC_ACTION_REG.screen_id` NOT
NULL (DBF-SEC-051, and the §3 DDL `screen_id BIGINT NOT NULL`), so a screenless permission cannot
be seeded and a dedicated code for this read cannot exist. Both new service methods are therefore
gated `@PreAuthorize("isAuthenticated()")` — the same exception the ALIGN `SECURITY (R7)` row
already records for API-SEC-027, and the same reasoning NOTIF's `DispatchService` records for its
own gate.

### #9 — the contact read

`SecUserDirectoryApi.findContact(Long)` returns `Optional<UserContact>`, served by the inherited
`findById` (no new QR id — `UserRepository`'s own convention for its inherited finders). An
unknown id answers `Optional.empty()`, not a 404: the caller holds only an id, so absence is a
legitimate cross-module answer. With the delivery half already closed by carrying `email` among
the `DispatchCommand` variables, gap #9 is now closed on both halves.

**(updated 2026-10-01)** NOTIF has since adopted it: the always-true
`DefaultRecipientStatusReader` stub is gone, replaced by the NOTIF port `RecipientDirectory` and its
`SecRecipientDirectory` implementation, which reads `UserContact.active` through
`SecUserDirectoryApi.findContact`. `XM-NOTIF-001` is resolved; an inactive or unknown recipient
receives nothing (RULE-NOTIF-007).

### One entity change, and why

`User.STATUS_ACTIVE` was widened from `private` to `public static final`. Deriving
`UserContact.active` from DBF-SEC-007 had to use the constant the entity already declares rather
than repeat the literal `"ACTIVE"`; the value, the lifecycle and every other member are unchanged.
`STATUS_DISABLED` stays private — nothing outside the entity needs it.

### The module-level cycle, stated rather than discovered

SEC consumes NOTIF's `NotificationDispatchApi`, and NOTIF now consumes `SecUserDirectoryApi`
**(updated 2026-10-01)**, so the two modules point at each other at the **module** level. That is not the circular dependency
`build-create-service` forbids — neither crossmodule interface calls the other, and the two paths
are independent (SEC→NOTIF at reset-token issuance; NOTIF→SEC at delivery, to resolve a bare
`recipientId`) — but it is real, and is now recorded in the ALIGN `CROSS-MODULE` row so it is
weighed before any further surface is added in either direction.

### Owed elsewhere, deliberately not written here

**(updated 2026-10-01)** FIN did not keep a separation-of-duties lookup: `FiscalPeriodService`
and `FiscalYearService` state that the permission gate itself is the enforcement and that no SoD
check is to be added back. `findUserIdsHoldingPermission` therefore has no consumer today; it
remains available for a future module.
