<!-- source: PHASE:SVC-API / SUB:SVC-API-SEARCH -->
<!-- context: SVC-API-HEADER.md — phase-level preamble -->
<!-- traces: DBF-SEC-002, DBF-SEC-003, DBF-SEC-005, DBF-SEC-006, DBF-SEC-007, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017, DBF-SEC-020, DBF-SEC-031, DBF-SEC-040, DBF-SEC-050, DBF-SEC-061, DBF-SEC-066, DBF-SEC-076, DBF-SEC-079, DBF-SEC-081, DBF-SEC-084, DBF-SEC-085, DBF-SEC-086, REQ-SEC-009, REQ-SEC-012, REQ-SEC-016, REQ-SEC-021, REQ-SEC-022, REQ-SEC-023, REQ-SEC-025, REQ-SEC-027, REQ-SEC-032, REQ-SEC-033 -->
<!-- SUB:SVC-API-SEARCH:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-022,REQ-SEC-025,REQ-SEC-027 -->
### SUB — SVC-API-SEARCH (read-only)

<!-- API:API-SEC-005:START traces=REQ-SEC-009,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 -->
### API-SEC-005 — search users
Endpoint     : POST /api/v1/sec/users/search   verb: POST
Layers       : controller → `UserController.search` ; service → `UserService.search`
Request      : body `UserSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `username`, `email`, `statusCode`, plus `fullName` (LIKE, matches fullNameAr or fullNameEn), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<UserResponse>` (userPk, username, email, fullNameAr, fullNameEn, statusCode, lastLoginAt) · envelope `ApiResponse<Page<UserResponse>>`
Validations  : none (read-only)
Errors       : none beyond platform-standard (§Error Catalog INTERNAL_ERROR)
Orchestration: load (QR-SEC-005 FIND_BY_CRITERIA) → map → return
Repository   : QR-SEC-005 · join NONE · transaction READ_ONLY
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_VIEW`
Localization : fullNameAr/fullNameEn both returned
<!-- API:API-SEC-005:END -->

<!-- API:API-SEC-012:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-020 -->
### API-SEC-012 — search roles
Endpoint     : POST /api/v1/sec/roles/search
Layers       : controller → `RoleController.search` ; service → `RoleService.search`
Request      : body `RoleSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `code`, `isActiveFl`, plus `name` (LIKE, nameAr or nameEn), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<RoleResponse>` (rolePk, code, nameAr, nameEn, descriptionAr, descriptionEn, isActiveFl) · `ApiResponse<Page<RoleResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-012) → map → return
Repository   : QR-SEC-012 · join NONE · transaction READ_ONLY
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_VIEW`
Localization : nameAr/nameEn/descriptionAr/descriptionEn returned
<!-- API:API-SEC-012:END -->

<!-- API:API-SEC-021:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-040,DBF-SEC-050 -->
### API-SEC-021 — search registry
Endpoint     : POST /api/v1/sec/registry/search
Layers       : controller → `RegistryController.search` ; service → `RegistryService.search`
Request      : body `RegistrySearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `code` (the module code), plus `pageCode` (LIKE, on the child screen), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<RegistryRowResponse>` (a module row with its nested active screens and, per screen, its active actions) · `ApiResponse<Page<RegistryRowResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-021, joins SEC_MODULE_REG → SEC_SCREEN_REG → SEC_ACTION_REG, all intra-module) → assemble tree → return
Repository   : QR-SEC-021 · join intra-module (module/screen/action — same table family, not cross-module) · transaction READ_ONLY
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_VIEW`
Localization : nameAr/nameEn at every level
<!-- API:API-SEC-021:END -->

<!-- API:API-SEC-022:START traces=REQ-SEC-022,REQ-SEC-023,DBF-SEC-007,DBF-SEC-076,DBF-SEC-081,DBF-SEC-084,DBF-SEC-086 -->
### API-SEC-022 — dashboard summary
Endpoint     : GET /api/v1/sec/dashboard
Layers       : controller → `DashboardController.summary` ; service → `DashboardService.summary`
Request      : none
Response     : 200 · `DashboardResponse` — six sub-figures, each present only if the caller holds that widget's source-screen VIEW permission (omitted field, not a zeroed one, when absent): `usersOverview{total,active,disabled,pendingSignups}`, `failedLogins24h{count}`, `activeSessions{count}`, `recentActivity{list of last N AuditLogEntry}`, `rolesPermissionsSummary{roleCount,privilegedRoleCount,usersPerRole}`, `onboardingFunnel{pendingSignups,stalledCount}` · `ApiResponse<DashboardResponse>`
Validations  : none — this endpoint filters its OWN output by REQ-SEC-023 (unwanted pattern) rather than rejecting the call
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: resolve caller's effective permissions (same read path as API-SEC-027) → for each widget whose source-screen VIEW the caller holds, compute it live (QR-SEC-022 sub-queries) → assemble → return (REQ-SEC-022: every figure computed at that moment, never cached)
Repository   : QR-SEC-022 (6 independent COUNT/aggregate sub-queries against SEC_USER, SEC_ACTIVE_SESSION, SEC_AUDIT_LOG, SEC_ROLE/SEC_USER_ROLE) · join NONE (each sub-query is single-table) · transaction READ_ONLY
Security     : screen SEC_DASHBOARD · permission `PERM_SEC_DASHBOARD_VIEW` (gateway) + per-widget the widget's own source-screen VIEW (SEC_USERS, SEC_SESSIONS, SEC_AUDIT_LOG, SEC_ROLES)
Localization : recentActivity entries carry detailsAr/detailsEn
Notes        : three figures in the Response line above are named but never defined upstream — `recentActivity`'s N, `onboardingFunnel.stalledCount`'s "stalled" window, and what makes a role `privileged` (REQ-SEC-022, AC-SEC-022, SCR-REQ-SEC-007 B1-B5, prd-sec.md and all 104 DBF are silent). Operative definitions, chosen at implementation to fill that silence and subject to a human's confirmation: N = 10 most recent AuditLogEntry rows; stalled = a SignupRequest still PENDING more than 7 days; privileged role = a role holding at least one action grant whose `actionCode` is not the VIEW gateway (RULE-SEC-007 / REQ-SEC-030's own VIEW-vs-other distinction, QR-SEC-022 via `countPrivilegedRoles`). All three are named constants in one class, not config keys, so a decision can change them in one place; nothing upstream authorises them yet.
<!-- API:API-SEC-022:END -->

<!-- API:API-SEC-023:START traces=REQ-SEC-025,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086 -->
### API-SEC-023 — search audit log
Endpoint     : POST /api/v1/sec/audit-log/search
Layers       : controller → `AuditLogController.search` ; service → `AuditLogService.search`
Request      : body `AuditLogEntrySearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `eventTypeCode` and `occurredAt` (any range operator), plus `actorUserId` (EXACT), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<AuditLogEntryResponse>` (all fields, unmodified) · `ApiResponse<Page<AuditLogEntryResponse>>`
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-023) → return unmodified (REQ-SEC-025: "without altering any of them")
Repository   : QR-SEC-023 · join NONE · transaction READ_ONLY
Security     : screen SEC_AUDIT_LOG · permission `PERM_SEC_AUDIT_LOG_VIEW`
Localization : detailsAr/detailsEn returned
<!-- API:API-SEC-023:END -->

<!-- API:API-SEC-025:START traces=REQ-SEC-027,DBF-SEC-076,DBF-SEC-079,DBF-SEC-081 -->
### API-SEC-025 — list active sessions
Endpoint     : POST /api/v1/sec/sessions/search
Layers       : controller → `SessionController.search` ; service → `SessionService.search`
Request      : body `ActiveSessionSearchRequest` (BaseSearchContractRequest) — `filters[]` of (field, operator, value) over `ipAddress`, plus `userId` (EXACT), and `page`, `size`, `sortField`, `sortDirection`
Response     : 200 · `Page<ActiveSessionResponse>` (activeSessionPk, userId, username, startedAt, lastActivityAt, ipAddress) — `tokenRef` never returned · `ApiResponse<Page<ActiveSessionResponse>>`
Validations  : filter `terminatedAt IS NULL` always applied server-side (REQ-SEC-027: "every session that has not been terminated") — not a client-supplied filter
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load (QR-SEC-025, filter terminatedAt IS NULL) → map → return
Repository   : QR-SEC-025 · join NONE · transaction READ_ONLY
Security     : screen SEC_SESSIONS · permission `PERM_SEC_SESSIONS_VIEW`
Localization : username shown (via userId join within SEC_USER, intra-module)
<!-- API:API-SEC-025:END -->

<!-- API:API-SEC-027:START traces=REQ-SEC-021,REQ-SEC-032,REQ-SEC-033,DBF-SEC-031,DBF-SEC-040,DBF-SEC-061,DBF-SEC-066 -->
### API-SEC-027 — effective menu
Endpoint     : GET /api/v1/sec/menu
Layers       : controller → `MenuController.effective` ; service → `MenuService.effective`
Request      : none (caller resolved from the authenticated session)
Response     : 200 · `List<ModuleMenuResponse>` — only modules the caller's effective grants hold (REQ-SEC-021/032), each with only that caller's effective granted screens beneath it · `ApiResponse<List<ModuleMenuResponse>>`
Validations  : none — the endpoint's entire behaviour IS the filter (REQ-SEC-021, REQ-SEC-032)
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: resolve caller's roles (QR-SEC-027, join SEC_ROLE_MODULE_GRANT/SEC_ROLE_SCREEN_GRANT to SEC_MODULE_REG/SEC_SCREEN_REG, all intra-module) → union across the caller's roles → assemble modules→screens tree → return
Repository   : QR-SEC-027 · join intra-module (grant tables to registry tables) · transaction READ_ONLY
Security     : no page code of its own (SRS B4) — every authenticated caller may call it; its content is the security boundary, not a permission on itself
Localization : nameAr/nameEn at module and screen level
<!-- API:API-SEC-027:END -->
<!-- SUB:SVC-API-SEARCH:END -->
