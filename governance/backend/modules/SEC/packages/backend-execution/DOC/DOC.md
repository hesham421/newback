<!-- source: PHASE:DOC -->
<!-- traces: REQ-SEC-016 -->
<!-- PHASE:DOC:START traces=REQ-SEC-016 -->
## PHASE 4 — DOC

**API contract summary** (R4 — backend self-check only; the frontend stage binds to the
real generated api-docs published under `governance/modules/SEC/api-docs/` after
implementation — `index.md` plus `endpoints/<group-slug>.md` — never to this table):

| API | Path | Verb | Request DTO | Response DTO | Stability |
|---|---|---|---|---|---|
| API-SEC-001 | /auth/login | POST | LoginRequest | LoginResponse | v1 |
| API-SEC-002 | /auth/signup | POST | SignupSubmitRequest | SignupRequestResponse | v1 |
| API-SEC-003 | /auth/password-reset/request | POST | PasswordResetRequest | ConfirmationResponse | v1 |
| API-SEC-004 | /auth/password-reset/complete | POST | PasswordResetCompleteRequest | ConfirmationResponse | v1 |
| API-SEC-005 | /users/search | POST | UserSearchRequest | Page\<UserResponse\> | v1 |
| API-SEC-006 | /users | POST | UserCreateRequest | UserResponse | v1 |
| API-SEC-007 | /users/{id} | PUT | UserUpdateRequest | UserResponse | v1 |
| API-SEC-008 | /users/{id}/roles | PUT | UserRoleAssignmentRequest | UserResponse | v1 |
| API-SEC-009 | /users/{id} | DELETE | — | UserStatusResponse | v1 |
| API-SEC-010 | /users/{id} | PATCH | — | UserStatusResponse | v1 |
| API-SEC-011 | /signup-requests/{id} | PATCH | SignupDecisionRequest | UserResponse \| SignupRequestResponse | v1 |
| API-SEC-012 | /roles/search | POST | RoleSearchRequest | Page\<RoleResponse\> | v1 |
| API-SEC-013 | /roles | POST | RoleCreateRequest | RoleResponse | v1 |
| API-SEC-014 | /roles/{id}/modules | POST | RoleModuleGrantRequest | RoleModuleGrantResponse | v1 |
| API-SEC-015 | /roles/{id}/modules/{moduleId} | DELETE | — | ModuleGrantRevokeResponse | v1 |
| API-SEC-016 | /roles/{id}/screens | POST | RoleScreenGrantRequest | RoleScreenGrantResponse | v1 |
| API-SEC-017 | /roles/{id}/actions | POST | RoleActionGrantRequest | RoleActionGrantResponse | v1 |
| API-SEC-018 | /registry/modules | POST | ModuleRegistryCreateRequest | ModuleRegistryResponse | v1 |
| API-SEC-019 | /registry/screens | POST | ScreenRegistryCreateRequest | ScreenRegistryResponse | v1 |
| API-SEC-020 | /registry/actions | POST | ActionRegistryCreateRequest | ActionRegistryResponse | v1 |
| API-SEC-021 | /registry/search | POST | RegistrySearchRequest | Page\<RegistryRowResponse\> | v1 |
| API-SEC-022 | /dashboard | GET | — | DashboardResponse | v1 |
| API-SEC-023 | /audit-log/search | POST | AuditLogEntrySearchRequest | Page\<AuditLogEntryResponse\> | v1 |
| API-SEC-024 | /audit-log/export | GET | (query params) | text/csv | v1 |
| API-SEC-025 | /sessions/search | POST | ActiveSessionSearchRequest | Page\<ActiveSessionResponse\> | v1 |
| API-SEC-026 | /sessions/{id} | DELETE | — | SessionTerminationResponse | v1 |
| API-SEC-027 | /menu | GET | — | List\<ModuleMenuResponse\> | v1 |
(paths relative to `/api/v1/sec`)

**DTO typing constraints**: `statusCode`/`eventTypeCode`/`actionCode` are `String` holding
the coded value, never a Java enum (profile lookup rule); business code fields — not
applicable, no SEC entity has one; PK fields never appear in a create-request body.

**Pagination + filter standard**: request shape `{page, size, sortField, sortDirection, filters}`
— the shared `BaseSearchContractRequest` body every `*SearchRequest` extends — with the filter
fields named per screen (§Phase 1 CORE "Search contract"); an unrecognized `sortField` is rejected
(`SEC-400-INVALID-SORT`); an empty filtered result is `200` with empty `content`, never `404`.
<!-- PHASE:DOC:END -->
