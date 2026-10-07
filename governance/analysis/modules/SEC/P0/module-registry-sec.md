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
Steps          : 01, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No ENT/XM ids are minted here.

ENTITIES — deltas
| Entity | Delta | Source |
|---|---|---|
| CustomerVerifyToken (`SEC_CUSTOMER_VERIFY_TOKEN`) | NEW, PRIVATE, tenant-scoped: hashed single-use e-mail verification token of a CUSTOMER account | V11__sec_realms.sql; docs/steps/06-report.md |
| User | CHANGED: carries `realm` (STAFF / CUSTOMER, immutable) and `tenantId`; unique per (tenant, realm) | V10, V11; DEVIATIONS [06] |
| Role | CHANGED: carries `isSuper` (super role) and `tenantId` | V11 |
| All other SEC entities except the three registries | CHANGED: tenant-scoped (extend `AuditableEntity`, Hibernate `@TenantId`) | docs/steps/05-report.md |
| ModuleRegistry, ScreenRegistry, ActionRegistry | CHANGED: GLOBAL (no tenant; extend `GlobalAuditableEntity`): one code-defined catalog shared by all tenants | V10 §3; DEVIATIONS [05], [12] (rule 2 allow-list) |

LOOKUPS — deltas
| Value set | Delta | Source |
|---|---|---|
| USER_STATUS | + `PENDING_VERIFICATION` (customer before e-mail verification); enforced by `CHK_SEC_USER_STATUS` | V11 §3 |
| REALM (new) | `STAFF`, `CUSTOMER`; enforced by `CHK_SEC_USER_REALM` (a column CHECK, not an MDL lookup) | V11 §1 |

DEPENDENCIES — deltas (all through the other module's root SPI or `crossmodule` package; ArchUnit
`sec_depends_on_other_core_modules_only_through_their_crossmodule_packages`, DEVIATIONS [06])
| Module | Kind | What is consumed | Source |
|---|---|---|---|
| tenant | HARD (FK `TENANT_ID` → `CORE_TENANT`) + SPI | `TenantContext`; `TenantProvisioningContributor` (SEC copies PLATFORM's catalog roles and grants, minus the `PLATFORM` module, and creates the tenant's first administrator) | DEVIATIONS [05] (provisioning entry) |
| NOTIF | SOFT (in-core API) | `NotificationDispatchApi` for the customer verification and password-reset mails (previously "external, optional") | DEVIATIONS [06] |
| audit | SOFT (in-core API) | `AuditApi` (LOGIN / LOGOUT / PASSWORD_RESET) and the `@Audited` listener on User and Role | DEVIATIONS [10] |
| events | publishes | `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent` | DEVIATIONS [08] (where the core events live) |
| report | SPI | `SecUserListReport` (`ReportProvider`) | DEVIATIONS [11] |

EXPOSED SURFACE — deltas
| Surface | Delta | Source |
|---|---|---|
| `com.erp.sec.crossmodule.SecUserDirectoryApi.findCurrentUserId()` | NEW — SEC_USER id of the authenticated caller (either realm); consumed by NOTIF's in-app inbox | DEVIATIONS [08] (inbox caller entry) |
| `SecUserDirectoryApi.findUserIdsHoldingPermission` (REQ-SEC-035) | kept; no consumer since `fin` was removed | 01-STEP; source javadoc |
| `com.erp.sec.permission` — `PermissionContributor`, `PermissionDef`, `PermissionModule`, `PermissionScreen` | NEW public SPI: every module (core or application) declares its own modules, screens and permissions | docs/steps/06-report.md; docs/RELEASE.md (public API) |

PERMISSION CATALOG — now code-defined
- `PermissionConstants` was deleted; each module has one contributor holding its own constants:
  `SecPermissions`, `FilePermissions`, `NotifPermissions`, `MdlPermissions`, `CuPermissions`,
  `TenantPermissions`, `SequencePermissions`, `AuditPermissions`, `ReportPermissions` (registry-fed).
  `PermissionCatalogSynchronizer` upserts the catalog into `SEC_MODULE_REG` / `SEC_SCREEN_REG` /
  `SEC_ACTION_REG` at startup (runs first, as PLATFORM; never deletes, never toggles `IS_ACTIVE_FL`).
- Authority format: the step file asked for `module:screen:action`; the existing registry format
  `PERM_<SCREEN>_<ACTION>` was kept (the step's own "adopt the existing format" rule). A permission may
  carry an explicit authority instead: CU `CONFIG_*`, `PLATFORM_TENANT_MANAGE`,
  `PLATFORM_SETTINGS_MANAGE`, `FILE:DOCUMENT:PUBLISH`, `AUDIT:EVENT:READ`, report authorities
  `<MODULE>:REPORT:<CODE>`. — DEVIATIONS [06], [07], [09], [10], [11].
- RULE-SEC-007 still holds: every screen needs a `VIEW` action, and non-VIEW actions are effective only
  together with it.
- REMOVED: the 30 `PERM_FIN_*` constants and the 18 FIN migrations with their seeds (`fin` deleted) — docs/steps/01-report.md.

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
