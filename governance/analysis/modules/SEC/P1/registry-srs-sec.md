## REGISTRY — P1 — SEC v1
══════════════════════════════════════════════════════════════════

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|
| ENT-SEC-001 | المستخدم / User | security | SHARED (owner) | REGISTERED |
| ENT-SEC-002 | الدور / Role | security | PRIVATE | REGISTERED |
| ENT-SEC-003 | ربط المستخدم بالدور / UserRoleAssignment | security | PRIVATE | REGISTERED |
| ENT-SEC-004 | سجل الوحدات / ModuleRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-005 | سجل الشاشات / ScreenRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-006 | سجل الإجراءات / ActionRegistry | security | SHARED (owner) | REGISTERED |
| ENT-SEC-007 | منح الوحدة للدور / RoleModuleGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-008 | منح الشاشة للدور / RoleScreenGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-009 | منح الإجراء للدور / RoleActionGrant | security | PRIVATE | REGISTERED |
| ENT-SEC-010 | الجلسة النشطة / ActiveSession | security | PRIVATE | REGISTERED |
| ENT-SEC-011 | سجل التدقيق / AuditLogEntry | security | PRIVATE | REGISTERED |
| ENT-SEC-012 | رمز إعادة تعيين كلمة المرور / PasswordResetToken | security | PRIVATE | REGISTERED |
| ENT-SEC-013 | طلب تسجيل معلّق / SignupRequest | security | PRIVATE | REGISTERED |

Consumed
none — SEC is ROOT (→ dependency index: no row this module).

Lookups owned
| Key | ENT | Values count |
|---|---|---|
| USER_STATUS | ENT-SEC-001 | 3 |
| SIGNUP_STATUS | ENT-SEC-013 | 3 |
| AUDIT_EVENT_TYPE | ENT-SEC-011 | 14 |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code |
|---|---|---|
| SCR-REQ-SEC-001 | تسجيل الدخول / Login | SEC_LOGIN |
| SCR-REQ-SEC-002 | التسجيل الذاتي / Sign-up | SEC_SIGNUP |
| SCR-REQ-SEC-003 | نسيت/إعادة تعيين كلمة المرور / Forgot/reset password | SEC_PWD_RESET |
| SCR-REQ-SEC-004 | المستخدمون / Users | SEC_USERS |
| SCR-REQ-SEC-005 | الأدوار والصلاحيات / Roles & permissions | SEC_ROLES |
| SCR-REQ-SEC-006 | سجل الوحدة/الشاشة/الإجراء / Module/screen/action registry | SEC_MODULE_REGISTRY |
| SCR-REQ-SEC-007 | لوحة تحكم الأمان / Admin dashboard | SEC_DASHBOARD |
| SCR-REQ-SEC-008 | سجل التدقيق / Audit log | SEC_AUDIT_LOG |
| SCR-REQ-SEC-009 | إدارة الجلسات النشطة / Active sessions management | SEC_SESSIONS |
| SCR-REQ-SEC-010 | القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu | (no page code — global component) |

Requirements
REQ count: 33 · AC count: 33 · RULE count: 7 · ENT count: 13 · SCR-REQ count: 10
Last sequence per atom: REQ: 033 · AC: 033 · ENT: 013 · RULE: 007 · SCR-REQ: 010

REQ ids (full text in srs-sec.md → A4): REQ-SEC-001, REQ-SEC-002, REQ-SEC-003,
REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-009,
REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015,
REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019, REQ-SEC-020, REQ-SEC-021,
REQ-SEC-022, REQ-SEC-023, REQ-SEC-024, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027,
REQ-SEC-028, REQ-SEC-029, REQ-SEC-030, REQ-SEC-031, REQ-SEC-032, REQ-SEC-033

AC ids (full text in srs-sec.md → A4, one per REQ above): AC-SEC-001, AC-SEC-002,
AC-SEC-003, AC-SEC-004, AC-SEC-005, AC-SEC-006, AC-SEC-007, AC-SEC-008, AC-SEC-009,
AC-SEC-010, AC-SEC-011, AC-SEC-012, AC-SEC-013, AC-SEC-014, AC-SEC-015, AC-SEC-016,
AC-SEC-017, AC-SEC-018, AC-SEC-019, AC-SEC-020, AC-SEC-021, AC-SEC-022, AC-SEC-023,
AC-SEC-024, AC-SEC-025, AC-SEC-026, AC-SEC-027, AC-SEC-028, AC-SEC-029, AC-SEC-030,
AC-SEC-031, AC-SEC-032, AC-SEC-033

RULE ids (full text in srs-sec.md → A5): RULE-SEC-001, RULE-SEC-002, RULE-SEC-003,
RULE-SEC-004, RULE-SEC-005, RULE-SEC-006, RULE-SEC-007

Decisions
ADR ids: none (no ADR raised — §9 ambiguity fork was never reached this stage).

Event
"P1 completed: SEC v1 — 13 entities, 33 requirements, 33 acceptance criteria, 7 rules, 10 screen requirements, 0 ADRs"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 04, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-sec.md` → "Implementation Addendum — erp-core 1.2.0". No new ids
are assigned here (the factory assigns ENT / REQ / AC / RULE ids when it absorbs these items).

Entities
| Entity | Kind | PRIVATE/SHARED | Delta | Source |
|---|---|---|---|---|
| CustomerVerifyToken | security | PRIVATE | NEW (tenant-scoped) | V11__sec_realms.sql |
| ENT-SEC-001 User | security | SHARED (owner) | CHANGED: + realm, tenant-scoped, unique per (tenant, realm) | V10, V11 |
| ENT-SEC-002 Role | security | PRIVATE | CHANGED: + isSuper | V11 |
| ENT-SEC-004/005/006 registries | security | SHARED (owner) | CHANGED: global, code-defined catalog (synchronized at startup) | docs/steps/06-report.md |
| all other ENT-SEC-* | security | PRIVATE | CHANGED: tenant-scoped, + version | V10 |

Consumed (was "none — SEC is ROOT")
| Owner | What | Kind | Source |
|---|---|---|---|
| tenant (`CORE_TENANT`) | FK of every `TENANT_ID`; `TenantContext`; provisioning SPI | HARD-FK + SPI | V10; docs/steps/05-report.md |
| NOTIF | `NotificationDispatchApi` | SOFT (in-core API) | DEVIATIONS [06] |
| audit | `AuditApi`, `@Audited` | SOFT (in-core API) | DEVIATIONS [10] |

Lookups owned
| Key | Delta | Source |
|---|---|---|
| USER_STATUS | 3 → 4 values (+ `PENDING_VERIFICATION`) | V11 |
| REALM | NEW value set `STAFF`, `CUSTOMER` (column CHECK) | V11 |
| SIGNUP_STATUS, AUDIT_EVENT_TYPE | unchanged | — |

Screens
No new SEC frontend screen. Customer endpoints have no page code (customer chain, `ROLE_CUSTOMER`).
Registry-only screen `SEC_REPORTS` (gateway `PERM_SEC_REPORTS_VIEW`) is synchronized for the users report.

Requirements — new / changed items (counted from the srs addendum)
| Group | NEW | CHANGED | REMOVED |
|---|---|---|---|
| Endpoints | 7 customer endpoints + users report | login, logout, reset completion, staff user / session / dashboard endpoints (STAFF-only) | — |
| Rules | tenant confinement, realm, realm mismatch, customer verification, rate limit, super role, bootstrap admin, events | RULE-SEC-006 (realm-aware), REQ-SEC-024 (audit also in `CORE_AUDIT_EVENT`), notify-eligibility | — |
| Error codes | `REALM_MISMATCH`, `CUSTOMER_EMAIL_TAKEN`, `CUSTOMER_NOT_VERIFIED`, `VERIFY_TOKEN_INVALID`, `CUSTOMER_LOGIN_RATE_LIMITED` | — | — |
| Permissions | `ROLE_CUSTOMER`, `SEC:REPORT:SEC_USER_LIST`, `PERM_SEC_REPORTS_VIEW` | catalog code-defined (`SecPermissions`) | 30 `PERM_FIN_*` |

Decisions
No ADR raised by the implementation; deviations are recorded in the erp-core repository's
`docs/DEVIATIONS.md` ([01], [04], [05], [06], [08], [10], [11], [14]).

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package G — revoke a single screen or action grant
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs-sec.md` → "Implementation Addendum — erp-core 1.3.0". Unlike
the 1.2.0 addendum, this one mints ids, continuing from the highest number ever issued for SEC (the
pre-vendoring analysis reached REQ-SEC-079, AC-SEC-085, RULE-SEC-053; see `srs-sec.md` 1.3.0 addendum).

Entities, lookups, screens, consumed modules: unchanged (no ENT, DBF, lookup value, page code or
permission added; no migration).

Requirements — new / changed items
| Kind | Id | Title | Traces |
|---|---|---|---|
| NEW | REQ-SEC-080 / AC-SEC-086 | Revoke a screen grant from a role (its action grants cascade) | US-SEC-005; RULE-SEC-054; SCR-REQ-SEC-005 |
| NEW | REQ-SEC-081 / AC-SEC-087 | Revoke an action grant from a role (revoking VIEW cascades the screen's other action grants) | US-SEC-005; RULE-SEC-055; SCR-REQ-SEC-005 |
| NEW | RULE-SEC-054 | Cascade revoke on screen-grant removal (ENT-SEC-008) | REQ-SEC-080 |
| NEW | RULE-SEC-055 | Revoking VIEW cascades the screen's other action grants (ENT-SEC-009) | REQ-SEC-081 |
| CHANGED | SCR-REQ-SEC-005 | B5 + `DELETE /api/v1/sec/roles/{id}/screens/{screenId}`, `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` | REQ-SEC-080, REQ-SEC-081 |

Counts in the current analysis after this addendum (base 33 REQ/AC + REQ/AC-SEC-034/035 of the 2026-09-11
amendment + these two): REQ 37 · AC 37 · RULE 9 · ENT 13 · SCR-REQ 10 (ids are not contiguous).
Last sequence per atom (highest ever issued, incl. the pre-vendoring analysis): REQ: 081 · AC: 087 · ENT: 014 ·
RULE: 055 · SCR-REQ: 010 · DBF: 116 · XM: 005 · QR: 054 · API: 050 · ADR: 062

Decisions
| Kind | ADR | Subject |
|---|---|---|
| NEW | ADR-SEC-062 | Revoking VIEW cascades the screen's other action grants (plan name ADR-SEC-041; renumbered because ADR-SEC numbers up to 061 were issued historically) |

Package D (tenant-maturity plan §6 D.1–D.3) — registry deltas; full text in `srs-sec.md` 1.3.0 addendum §9.

Entities (package D)
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-SEC-001 User | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale`, `photoFileId` (soft ref, XM-SEC-006), `passwordChangeRequired`, `passwordChangedAt` | V16__sec_user_profile.sql (DBF-SEC-117..123) |

Consumed (package D)
| Kind | Owner | What | Kind of link |
|---|---|---|---|
| NEW | FILE | `FileImageStoreApi` (XM-FILE-002), `FileDocumentLookupApi.publicUrl` / `publicUrls` — XM-SEC-006 | SOFT (in-core API, no FK) |
| CHANGED | tenant | `TenantLookupApi.summaryOf` (XM-TENANT-001) | in-core API |
| NEW (consumer) | NOTIF | reacts to `UserPasswordChangedEvent` (RULE-NOTIF-023) | event bus |

Requirements — new / changed items (package D)
| Kind | Id | Title | Traces |
|---|---|---|---|
| NEW | REQ-SEC-082 / AC-SEC-088 | Password policy | US-SEC-001, US-SEC-002; RULE-SEC-056 |
| NEW | REQ-SEC-083 / AC-SEC-089 | Administrator sets a staff user's password | US-SEC-002; RULE-SEC-057, RULE-SEC-058; SCR-REQ-SEC-004 |
| NEW | REQ-SEC-084 / AC-SEC-090 | Forced password change (403 gate) | US-SEC-002; RULE-SEC-059 |
| NEW | REQ-SEC-085 / AC-SEC-091 | A staff user changes their own password | US-SEC-001; RULE-SEC-060 |
| NEW | REQ-SEC-086 / AC-SEC-092 | Staff profile `/me` (read, patch; no roles) | US-SEC-001; RULE-SEC-062; ADR-SEC-064 |
| NEW | REQ-SEC-087 / AC-SEC-093 | Profile photo (own and another staff user's) | US-SEC-001, US-SEC-002; RULE-SEC-061; SCR-REQ-SEC-004 |
| NEW | REQ-SEC-088 / AC-SEC-094 | Profile fields in user management, `passwordChangeRequired` at login | US-SEC-002; RULE-SEC-058, RULE-SEC-062; SCR-REQ-SEC-004 |
| NEW | REQ-SEC-089 / AC-SEC-095 | Password-change event (NOTIF e-mail) | US-SEC-001 |
| NEW | RULE-SEC-056 | Password policy (`PasswordPolicy`): 8..72 characters, ≤ 72 UTF-8 bytes (BCrypt), letter + digit; customers get the byte limit only | REQ-SEC-082 |
| NEW | RULE-SEC-057 | No admin-set on oneself (`SEC-422-PASSWORD-SELF`) | REQ-SEC-083 |
| NEW | RULE-SEC-058 | An administrator-chosen password must be changed (default TRUE) | REQ-SEC-083, REQ-SEC-088 |
| NEW | RULE-SEC-059 | Forced-change gate (`SEC-403-PASSWORD-CHANGE-REQUIRED`) | REQ-SEC-084 |
| NEW | RULE-SEC-060 | Self-change needs the current password (`SEC-403-PASSWORD-CURRENT-INVALID`) | REQ-SEC-085 |
| NEW | RULE-SEC-061 | Profile photo: PNG/JPEG/WebP ≤ 1 MB, one per user (`SEC-400-PHOTO-INVALID`) | REQ-SEC-087 |
| NEW | RULE-SEC-062 | Preferred locale `ar` / `en` (`CHK_SEC_USER_LOCALE`) | REQ-SEC-086, REQ-SEC-088 |
| CHANGED | SCR-REQ-SEC-004 | B5 + set password, set / remove photo | REQ-SEC-083, REQ-SEC-087, REQ-SEC-088 |
| CHANGED | REQ-SEC-007 (reset completion), REQ-SEC-009 (create user) | + RULE-SEC-056; reset completion clears the forced-change flag | — |

Screens: no new SEC screen, page code or permission (the profile pages are authentication-only frontend routes).

Counts in the current analysis after package D: REQ 45 · AC 45 · RULE 16 · ENT 13 · SCR-REQ 10 (ids are not contiguous).
Last sequence per atom (highest ever issued): REQ: 089 · AC: 095 · ENT: 014 · RULE: 062 · SCR-REQ: 010 ·
DBF: 123 · XM: 006 · QR: 054 · API: 050 · ADR: 064 (065 held spare for this run; the as-built SEC ADRs of the
analysis-coverage session start at 066)

Decisions (package D)
| Kind | ADR | Subject |
|---|---|---|
| NEW | ADR-SEC-063 | An administrator-chosen password forces a change at next login (plan name ADR-SEC-039) |
| NEW | ADR-SEC-064 | The staff `/me` payload carries no roles or permissions (plan name ADR-SEC-040) |

Package B (tenant-maturity plan §4 B.4) — registry deltas; full text in `srs-sec.md` 1.3.0 addendum §10.

Requirements — new items (package B)
| Kind | Id | Title | Traces |
|---|---|---|---|
| NEW | REQ-SEC-090 / AC-SEC-096 | Directory counts of the current tenant (`SecUserDirectoryApi.countStaff / countCustomers / countActiveSessions`) | US-SEC-002; consumer TENANT REQ-TENANT-028 |
| NEW | REQ-SEC-091 / AC-SEC-097 | Platform recovery of a super user's password (`SecAdminRecoveryApi.findRecoveryTarget / resetSuperUserPassword`) | US-SEC-002; RULE-SEC-056, RULE-SEC-058; consumer TENANT REQ-TENANT-027, RULE-TENANT-017 |

Exposed surface (package B): `SecUserDirectoryApi` + three counts (CHANGED); `SecAdminRecoveryApi` +
`RecoveryTarget` (NEW, gate `PLATFORM_TENANT_MANAGE`). Entities, screens, permissions, error codes, schema:
unchanged. Audit action + `ADMIN_PASSWORD_RESET` (generic audit log).

Counts in the current analysis after package B: REQ 47 · AC 47 · RULE 16 · ENT 13 · SCR-REQ 10 (ids are not contiguous).
Last sequence per atom (highest ever issued): REQ: 091 · AC: 097 · ENT: 014 · RULE: 062 · SCR-REQ: 010 ·
DBF: 123 · XM: 006 · QR: 054 · API: 050 · ADR: 064 (065 held spare; 066 … 068 the analysis-coverage work's)
