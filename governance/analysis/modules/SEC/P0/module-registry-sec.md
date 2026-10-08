## MODULE REGISTRY — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module Code    : SEC   (profile.vocabulary.module_prefixes)
Bounded context: organization
Layer / Type   : L1 / security engine     Execution tier : 1.2
Source         : NEW
Knowledge      : new project/security-module-plan-en.md; profiles/erp/knowledge/erp-domain-standards.md §4
Readiness      : READY
══════════════════════════════════════════════════════════════════

ENTITIES OWNED   (names only — entity IDs are assigned by P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| المستخدم / User | security | SHARED — every module's audit fields (createdBy/updatedBy) FK to it | security-module-plan-en.md §3, §4.4; profiles/erp.yaml conventions.entity_defaults |
| الدور / Role | security | PRIVATE | security-module-plan-en.md §4.4 |
| ربط المستخدم بالدور / UserRoleAssignment | security | PRIVATE | security-module-plan-en.md §4.4 |
| سجل الوحدات / ModuleRegistry | security | SHARED — every consuming module registers itself here | security-module-plan-en.md §4.3, §7 |
| سجل الشاشات / ScreenRegistry (SEC_PAGES) | security | SHARED — every consuming module registers its screens here | security-module-plan-en.md §4.3, §4.5; profiles/erp.yaml conventions.security_model.page_registry |
| سجل الإجراءات / ActionRegistry (permission catalog, PERM_*) | security | SHARED — every consuming module registers its actions here | security-module-plan-en.md §4.1, §4.3; profiles/erp.yaml conventions.security_model.permission_pattern |
| منح الوحدة للدور / RoleModuleGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| منح الشاشة للدور / RoleScreenGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| منح الإجراء للدور / RoleActionGrant | security | PRIVATE | security-module-plan-en.md §4.1-§4.2 |
| الجلسة النشطة / ActiveSession | security | PRIVATE | security-module-plan-en.md §5.1, §5.3 |
| سجل التدقيق / AuditLogEntry | security | PRIVATE | security-module-plan-en.md §5.2-§5.3 |
| رمز إعادة تعيين كلمة المرور / PasswordResetToken | security | PRIVATE | security-module-plan-en.md §3 |
| طلب تسجيل معلّق / SignupRequest | security | PRIVATE | security-module-plan-en.md §3 |

LOOKUPS OWNED    (value lists this module masters — registered into MDL, not stored locally)
| Lookup key | Description | Initial values (only those the user named) | Source |
|---|---|---|---|
| USER_STATUS | حالة حساب المستخدم / user account status | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
| AUDIT_EVENT_TYPE | نوع حدث التدقيق / audit-log event type | None — no specific values named by the user | AUTO (see AUTO-DECISIONS) |
Rule (profile): all LOV values runtime-loaded from the lookup module; no hardcoded enums in APIs or field specs

LOOKUPS CONSUMED (from other modules)
| Lookup key | Owner code | READ-ONLY |
None — SEC is a Tier-0 foundation module; it consumes no other in-scope module's lookups this batch.

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |
None — SEC is ROOT; it depends on no other in-scope module.

DEPENDENCIES
| Module code | HARD / SOFT / LOOKUP | What is consumed |
None (within the platform's module registry).
External (not a registry module): Notifications (ready, SOFT, optional — e.g. password-reset email) [security-module-plan-en.md §8].
ROOT: YES

AUTO-DECISIONS
AUTO: registered USER_STATUS and AUDIT_EVENT_TYPE as SEC-owned lookup types (values: USER_STATUS = PENDING/ACTIVE/DISABLED/LOCKED; AUDIT_EVENT_TYPE = LOGIN_SUCCESS/LOGIN_FAILED/LOGOUT/PASSWORD_RESET_REQUESTED/PASSWORD_RESET_COMPLETED/ROLE_ASSIGNED/ROLE_REVOKED/MODULE_GRANTED/MODULE_REVOKED/SCREEN_GRANTED/SCREEN_REVOKED/ACTION_GRANTED/ACTION_REVOKED/SESSION_TERMINATED — these initial values are AUTO, not user-named, so they are listed here rather than in the Lookups Owned "Initial values" column)
  FROM: profiles/erp.yaml conventions.lookups ("no hardcoded enums in APIs or field specs") + lookup-module-plan-en.md §3
  IF WRONG: fold these states into a plain internal enum on the User/AuditLogEntry entities instead of a shared lookup type — revise this module registry and business-policies-sec.md's CUSTOM LOOKUP VALUES accordingly.
AUTO: classified User as SHARED
  FROM: profiles/erp.yaml conventions.entity_defaults (every entity carries createdBy/updatedBy, which must reference a real User row)
  IF WRONG: none recommended — dropping this would break audit traceability platform-wide.
AUTO: tier/numbering 1.2 (Foundation, Tier 0)
  FROM: [KB:erp-domain-standards §1]
  IF WRONG: renumber if the platform later reprioritizes; number stability rule applies once confirmed.
AUTO: Session and AuditLogEntry kept PRIVATE, not SHARED
  FROM: security-module-plan-en.md §5 scopes the audit feed to SEC's own security events (logins, resets, role/permission changes) only, not other modules' business events
  IF WRONG: promote to SHARED if a later module needs to append platform-wide audit events through SEC — would need its own ADR at that time.

RESOLVED DECISIONS (dialogue, this module)
| # | Point | Recommended | Confirmed by user | Sources |
None — security-module-plan-en.md fully settles this module's P0 scope; no point required dialogue.

POLICIES OWNED (full text in business-policies-sec.md)
POL-SEC-001, POL-SEC-002, POL-SEC-003, POL-SEC-004, POL-SEC-005, POL-SEC-006,
POL-SEC-007, POL-SEC-008, POL-SEC-009, POL-SEC-010, POL-SEC-011
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 04, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Revised        : 2026-10-08 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No ENT/XM ids are minted here.

ENTITIES — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| NEW | CustomerVerifyToken (`SEC_CUSTOMER_VERIFY_TOKEN`) | PRIVATE, tenant-scoped: hashed single-use e-mail verification token of a CUSTOMER account | V11__sec_realms.sql; docs/steps/06-report.md |
| CHANGED | every SEC entity | primary key `BIGINT` fed by a named `SEQ_SEC_*` sequence (14 sequences), never IDENTITY — ADR-SEC-068 | V4__sec_schema.sql:17-31; V11:64 |
| CHANGED | User | carries `realm` (STAFF / CUSTOMER, immutable) and `tenantId`; unique per (tenant, realm); `username` immutable after creation | V10, V11; DEVIATIONS [06] |
| CHANGED | Role | carries `isSuper` (super role) and `tenantId`; `code` upper-cased and immutable after creation | V11; sec/entity/Role.java |
| CHANGED | All other SEC entities except the three registries | tenant-scoped (extend `AuditableEntity`, Hibernate `@TenantId`); the 8 that had no audit columns gained nullable ones (V10 §4) | docs/steps/05-report.md |
| CHANGED | ModuleRegistry, ScreenRegistry, ActionRegistry | GLOBAL (no tenant; extend `GlobalAuditableEntity`): one code-defined catalog shared by all tenants; codes upper-cased; `IS_ACTIVE_FL` never toggled by any API (registry-row deactivate NOT IMPLEMENTED) | V10 §3; DEVIATIONS [05], [12] (rule 2 allow-list); ADR-SEC-038 |
| CHANGED | ActiveSession | one row per login of either realm, keyed by the token's `jti` (`TOKEN_REF`); terminated by logout (new endpoint, REQ-SEC-036), administrator termination, deactivation or password reset; `lastActivityAt` is the login time | sec/service/AuthService.java; ADR-SEC-066 |

LOOKUPS — deltas
| Kind | Value set | Delta | Source |
|---|---|---|---|
| CHANGED | USER_STATUS | + `PENDING_VERIFICATION` (customer before e-mail verification); enforced by `CHK_SEC_USER_STATUS` | V11 §3 |
| NEW | REALM | `STAFF`, `CUSTOMER`; enforced by `CHK_SEC_USER_REALM` (a column CHECK, not an MDL lookup) | V11 §1 |
| CHANGED | AUDIT_EVENT_TYPE | the 14 AUTO values above are exactly the `CHK_SEC_AUDIT_LOG_EVENT_TYPE` set and every one has a writer; `LOGOUT` is written by `POST /api/v1/sec/auth/logout`, `ROLE_REVOKED` by the role assignment that replaces a user's set. Not written for: user creation / update / status change, sign-up decisions, role create / update, registry registrations, customer-realm events (srs addendum §2, REQ-SEC-024) | V4 §5c; sec/service/*.java |

DEPENDENCIES — deltas (all through the other module's root SPI or `crossmodule` package; ArchUnit
`sec_depends_on_other_core_modules_only_through_their_crossmodule_packages`, DEVIATIONS [06])
| Kind | Module | Kind of link | What is consumed | Source |
|---|---|---|---|---|
| NEW | tenant | HARD (FK `TENANT_ID` → `CORE_TENANT`) + SPI | `TenantContext`; `TenantProvisioningContributor` (SEC, order 0: copies PLATFORM's four catalog roles `SYS_ADMIN`, `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` and their grants, minus the `PLATFORM` module, and creates the tenant's first administrator; aborts with `INTERNAL_ERROR` when PLATFORM has no `SYS_ADMIN`) | DEVIATIONS [05] (provisioning entry); sec/tenant/SecTenantProvisioningContributor.java |
| CHANGED | NOTIF | SOFT (in-core API) | `NotificationDispatchApi.dispatchIndependently` for the staff reset mail (`PASSWORD_RESET`, V9) and the customer verification and password-reset mails (`CUSTOMER_VERIFY_EMAIL`, `CUSTOMER_PASSWORD_RESET`, V11 §6) — previously "external, optional" | DEVIATIONS [06] |
| NEW | audit | SOFT (in-core API) | `AuditApi` (LOGIN / LOGOUT / PASSWORD_RESET) and the `@Audited` listener on User and Role | DEVIATIONS [10] |
| NEW | events | publishes | `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent` (verify endpoint only), `PasswordResetRequestedEvent` (staff only); SEC consumes no event | DEVIATIONS [08] (where the core events live) |
| NEW | report | SPI | `SecUserListReport` (`ReportProvider`) | DEVIATIONS [11] |

EXPOSED SURFACE — deltas
| Kind | Surface | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.sec.crossmodule.SecUserDirectoryApi.findCurrentUserId()` | SEC_USER id of the authenticated caller (either realm); consumed by NOTIF's in-app inbox | DEVIATIONS [08] (inbox caller entry) |
| CHANGED | `SecUserDirectoryApi.findUserIdsHoldingPermission` (REQ-SEC-035) | kept; no consumer since `fin` was removed | 01-STEP; source javadoc |
| NEW | `com.erp.sec.crossmodule.SecModuleRegistryApi.isModuleActive(moduleCode)` | existence check of an active `SEC_MODULE_REG` row; consumed by MDL's `LookupTypeService` (RULE-MDL-001, XM-MDL-001) — the second inbound read surface beside `SecUserDirectoryApi` | sec/crossmodule/SecModuleRegistryApi.java; mdl/service/LookupTypeService.java:80 |
| NEW | `com.erp.sec.permission` — `PermissionContributor`, `PermissionDef`, `PermissionModule`, `PermissionScreen` | public SPI: every module (core or application) declares its own modules, screens and permissions | docs/steps/06-report.md; docs/RELEASE.md (public API) |
| NEW | `com.erp.sec.security.InternalCallerContext` | synthetic in-process principal (`INTERNAL_TRUSTED_CALLER`) used by the anonymous reset request to call NOTIF; never obtainable by a request | sec/security/InternalCallerContext.java |

PERMISSION CATALOG — now code-defined
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | catalog source | `PermissionConstants` was deleted; each module has one contributor holding its own constants: `SecPermissions`, `FilePermissions`, `NotifPermissions`, `MdlPermissions`, `CuPermissions`, `TenantPermissions`, `SequencePermissions`, `AuditPermissions`, `ReportPermissions` (registry-fed). `PermissionCatalogSynchronizer` upserts the catalog into `SEC_MODULE_REG` / `SEC_SCREEN_REG` / `SEC_ACTION_REG` at startup (runs first, as PLATFORM; inserts, renames to the contributed names, never deletes, never toggles `IS_ACTIVE_FL`; warns for a screen without a `VIEW` gateway). | docs/steps/06-report.md; sec/service/PermissionCatalogSynchronizer.java |
| CHANGED | registry write endpoints | `POST /api/v1/sec/registry/{modules,screens,actions}` exist and are gated by `PERM_SEC_MODULE_REGISTRY_UPDATE` (the registry above gives UPDATE "deactivate only" and treats registration as the module's own call) — ADR-SEC-067 | sec/service/RegistryService.java:72-125 |
| CHANGED | authority format | the step file asked for `module:screen:action`; the existing registry format `PERM_<SCREEN>_<ACTION>` was kept (the step's own "adopt the existing format" rule). A permission may carry an explicit authority instead: CU `CONFIG_*`, `PLATFORM_TENANT_MANAGE`, `PLATFORM_SETTINGS_MANAGE`, `FILE:DOCUMENT:PUBLISH`, `AUDIT:EVENT:READ`, report authorities `<MODULE>:REPORT:<CODE>` | DEVIATIONS [06], [07], [09], [10], [11] |
| CHANGED | RULE-SEC-007 | still holds, and is blocking: every screen needs a `VIEW` action, a non-VIEW action grant is refused without the role's VIEW (`SEC-409-NO-VIEW-GRANT`), and non-VIEW actions are effective only together with VIEW | sec/domain/RoleActionGrantDomain.java |
| CHANGED | `PERM_SEC_ROLES_DELETE` | registered and granted to `SYS_ADMIN`, consumed by no gate (role deactivate NOT IMPLEMENTED — ADR-SEC-038) | sec/permission/SecPermissions.java:31-32 |
| NEW | seeded roles | `SYS_ADMIN` (super), `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` in the PLATFORM tenant (V7: 8 module, 20 screen, 59 action grants), copied to every tenant by provisioning; the registry seed also holds the MDL, CU, NOTIF, FILE and PLATFORM rows (srs addendum §4) | V7__sec_seed.sql:141-200; V10 §6; V12 §3 |
| REMOVED | the 30 `PERM_FIN_*` constants and the 18 FIN migrations with their seeds (`fin` deleted) | — | docs/steps/01-report.md |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D — passwords, profile, photo, staff `/me` (package G changed nothing here)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `P1/srs-sec.md` → "Implementation Addendum — erp-core 1.3.0" §9.

Screens, actions, permissions: unchanged (set password and photo are `PERM_SEC_USERS_UPDATE` of `SEC_USERS`;
`/api/v1/sec/me/**` is authentication-only, STAFF chain).

Consumed modules
| Kind | Module | Kind of link | What | Source |
|---|---|---|---|---|
| NEW | FILE | SOFT (in-core API) | `FileImageStoreApi` (store / discard a public image), `FileDocumentLookupApi.publicUrl(s)` — `SEC_USER.PHOTO_FILE_ID` (XM-SEC-006) | srs-sec.md 1.3.0 §9.7 |
| CHANGED | tenant | in-core API | `TenantLookupApi.summaryOf` for `/me.tenant` | same |
| NEW (consumer of SEC) | NOTIF | event bus | `UserPasswordChangedEvent` → `STAFF_PASSWORD_CHANGED` mail | NOTIF srs.md 1.3.0 addendum |

Lookups owned: unchanged. `preferredLocale` (`ar`, `en`) is a CHECK-constrained value set on the column
(`CHK_SEC_USER_LOCALE`), not a lookup type.

Package B (tenant-maturity plan §4 B.4) — exposed surface deltas; full text in `P1/srs-sec.md` 1.3.0 §10.
| Kind | Surface | Delta | Consumer |
|---|---|---|---|
| CHANGED | `com.erp.sec.crossmodule.SecUserDirectoryApi` | + `countStaff()`, `countCustomers()`, `countActiveSessions()` (current tenant) | TENANT usage (REQ-SEC-090) |
| NEW | `com.erp.sec.crossmodule.SecAdminRecoveryApi` (+ `RecoveryTarget`) | `findRecoveryTarget(String)`, `resetSuperUserPassword(String, String, Boolean)`; gate `PLATFORM_TENANT_MANAGE` | TENANT admin-reset (REQ-SEC-091) |

Package C12 (tenant-maturity plan §5 C.1, C.2) — dependency and exposed surface deltas; full text in `P1/srs-sec.md` 1.3.0 §12.
| Kind | Module / surface | Kind of link | Delta | Source |
|---|---|---|---|---|
| NEW | events (TENANT publishes) | event bus (XM-SEC-007) | consumes `TenantSuspendedEvent` → ends the tenant's sessions | srs-sec.md 1.3.0 §12 |
| CHANGED | tenant | root-package API | `JwtAuthenticationFilter` writes `TenantTokenFacts` (request attribute) for the tenant filter's cut-off check | same |
| CHANGED | `com.erp.sec.crossmodule.SecAdminRecoveryApi` (exposed) | in-core API | + `terminateAllSessions()`; gate `PLATFORM_TENANT_MANAGE`; consumer TENANT revoke-tokens | same |
