## REGISTRY — P1 — SEC v2
══════════════════════════════════════════════════════════════════
Delta : v2 on v1 (CS-SEC-001, ADDITIVE). This registry is written as the complete current registry (v1 + v2), so no v1 line needs to be carried by the fold.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|
| ENT-SEC-001 | المستخدم / User | security | SHARED (owner) | REGISTERED — v2 MODIFIED (principalTypeCode; covers HUMAN and SERVICE principals) |
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
| ENT-SEC-014 | بيانات اعتماد حساب الخدمة / ServiceAccountCredential | security | PRIVATE | REGISTERED — v2 ADDED |

Consumed
none — SEC is ROOT (→ dependency index: no row this module). (v2: unchanged; the Oracle event consumer is an
external caller that authenticates as a SEC service account, not a dependency of SEC.)

Lookups owned
| Key | ENT | Values count |
|---|---|---|
| USER_STATUS | ENT-SEC-001 | 3 |
| SIGNUP_STATUS | ENT-SEC-013 | 3 |
| AUDIT_EVENT_TYPE | ENT-SEC-011 | 21 (v2: +7 SERVICE_* values — SERVICE_ACCOUNT_REACTIVATED added by the pass-1 REVISE review, RG6) |
| PRINCIPAL_TYPE | ENT-SEC-001 | 2 (v2 ADDED: HUMAN, SERVICE) |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code |
|---|---|---|
| SCR-REQ-SEC-001 | تسجيل الدخول / Login | SEC_LOGIN |
| SCR-REQ-SEC-002 | التسجيل الذاتي / Sign-up | SEC_SIGNUP |
| SCR-REQ-SEC-003 | نسيت/إعادة تعيين كلمة المرور / Forgot/reset password | SEC_PWD_RESET |
| SCR-REQ-SEC-004 | المستخدمون / Users | SEC_USERS — v2 MODIFIED (service accounts, credentials tab) |
| SCR-REQ-SEC-005 | الأدوار والصلاحيات / Roles & permissions | SEC_ROLES |
| SCR-REQ-SEC-006 | سجل الوحدة/الشاشة/الإجراء / Module/screen/action registry | SEC_MODULE_REGISTRY |
| SCR-REQ-SEC-007 | لوحة تحكم الأمان / Admin dashboard | SEC_DASHBOARD — v2 MODIFIED (users overview per principal type) |
| SCR-REQ-SEC-008 | سجل التدقيق / Audit log | SEC_AUDIT_LOG |
| SCR-REQ-SEC-009 | إدارة الجلسات النشطة / Active sessions management | SEC_SESSIONS |
| SCR-REQ-SEC-010 | القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu | (no page code — global component) |

Requirements
REQ count: 79 · AC count: 85 · RULE count: 13 · ENT count: 14 · SCR-REQ count: 10
Last sequence per atom: REQ: 079 · AC: 085 · ENT: 014 · RULE: 013 · SCR-REQ: 010
(v1 closed at REQ 035 · AC 035 — its registry's "033" predates the 2026-09-11 amendment that added
REQ-SEC-034/035 with AC-SEC-034/035; v2 continues from 035. G5/G6 (pass-1 review) added REQ-SEC-072…077
with AC-SEC-078…083 — event-pattern REQs retracing API-SEC-032…036, and POL-SEC-024's unwanted path.
G3 (pass-1 review) added REQ-SEC-078 with AC-SEC-084 and RULE-SEC-013 — the service-account credential-count
cap, ADR-SEC-042. The pass-1 REVISE review (RG6) added REQ-SEC-079 with AC-SEC-085 and ADR-SEC-047 — the
service-account reactivation audit gap.)

REQ ids (full text in srs-sec.md → A4):
| Version | Ids |
|---|---|
| v1 | REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019, REQ-SEC-020, REQ-SEC-021, REQ-SEC-022, REQ-SEC-023, REQ-SEC-024, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027, REQ-SEC-028, REQ-SEC-029, REQ-SEC-030, REQ-SEC-031, REQ-SEC-032, REQ-SEC-033, REQ-SEC-034, REQ-SEC-035 |
| v2 ADDED | REQ-SEC-036, REQ-SEC-037, REQ-SEC-038, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-042, REQ-SEC-043, REQ-SEC-044, REQ-SEC-045, REQ-SEC-046, REQ-SEC-047, REQ-SEC-048, REQ-SEC-049, REQ-SEC-050, REQ-SEC-051, REQ-SEC-052, REQ-SEC-053, REQ-SEC-054, REQ-SEC-055, REQ-SEC-056, REQ-SEC-057, REQ-SEC-058, REQ-SEC-059, REQ-SEC-060, REQ-SEC-061, REQ-SEC-062, REQ-SEC-063, REQ-SEC-064, REQ-SEC-065, REQ-SEC-066, REQ-SEC-067, REQ-SEC-068, REQ-SEC-069, REQ-SEC-070, REQ-SEC-071, REQ-SEC-072, REQ-SEC-073, REQ-SEC-074, REQ-SEC-075, REQ-SEC-076, REQ-SEC-077, REQ-SEC-078, REQ-SEC-079 |

AC ids (full text in srs-sec.md → A4):
| Version | Ids |
|---|---|
| v1 | AC-SEC-001, AC-SEC-002, AC-SEC-003, AC-SEC-004, AC-SEC-005, AC-SEC-006, AC-SEC-007, AC-SEC-008, AC-SEC-009, AC-SEC-010, AC-SEC-011, AC-SEC-012, AC-SEC-013, AC-SEC-014, AC-SEC-015, AC-SEC-016, AC-SEC-017, AC-SEC-018, AC-SEC-019, AC-SEC-020, AC-SEC-021, AC-SEC-022, AC-SEC-023, AC-SEC-024, AC-SEC-025, AC-SEC-026, AC-SEC-027, AC-SEC-028, AC-SEC-029, AC-SEC-030, AC-SEC-031, AC-SEC-032, AC-SEC-033, AC-SEC-034, AC-SEC-035 |
| v2 ADDED | AC-SEC-036, AC-SEC-037, AC-SEC-038, AC-SEC-039, AC-SEC-040, AC-SEC-041, AC-SEC-042, AC-SEC-043, AC-SEC-044, AC-SEC-045, AC-SEC-046, AC-SEC-047, AC-SEC-048, AC-SEC-049, AC-SEC-050, AC-SEC-051, AC-SEC-052, AC-SEC-053, AC-SEC-054, AC-SEC-055, AC-SEC-056, AC-SEC-057, AC-SEC-058, AC-SEC-059, AC-SEC-060, AC-SEC-061, AC-SEC-062, AC-SEC-063, AC-SEC-064, AC-SEC-065, AC-SEC-066, AC-SEC-067, AC-SEC-068, AC-SEC-069, AC-SEC-070, AC-SEC-071, AC-SEC-072, AC-SEC-073, AC-SEC-074, AC-SEC-075, AC-SEC-076, AC-SEC-077, AC-SEC-078, AC-SEC-079, AC-SEC-080, AC-SEC-081, AC-SEC-082, AC-SEC-083, AC-SEC-084, AC-SEC-085 |

RULE ids (full text in srs-sec.md → A5):
| Version | Ids |
|---|---|
| v1 | RULE-SEC-001, RULE-SEC-002, RULE-SEC-003, RULE-SEC-004, RULE-SEC-005, RULE-SEC-006, RULE-SEC-007 |
| v2 ADDED | RULE-SEC-008, RULE-SEC-009, RULE-SEC-010, RULE-SEC-011, RULE-SEC-012, RULE-SEC-013 |

Decisions
ADR ids raised at P1: ADR-SEC-031 (**SUPERSEDED** by ADR-SEC-034 — the decided consumer role vs the inherited VIEW gateway,
resolved at the human stop as option A),
ADR-SEC-032 (ACCEPTED), ADR-SEC-033 (ACCEPTED), ADR-SEC-034 (ACCEPTED). Upstream v2 ADRs applied: ADR-SEC-012 … ADR-SEC-030.
v1 P1 raised none. ADR-SEC-042 (the credential-count cap, RULE-SEC-013) is raised at P3.1, not here.
Pass-1 REVISE review (this pass) additionally raised: ADR-SEC-046 (C5.14 parser accepts both AC forms, RG4),
ADR-SEC-047 (SERVICE_ACCOUNT_REACTIVATED audit code, RG6).

Event
"P1 COMPLETE: SEC v2 — 14 entities (+1), 79 requirements (+44), 85 acceptance criteria (+50), 13 rules (+6), 10 screen requirements (2 modified), 6 ADRs (ADR-SEC-031 SUPERSEDED by ADR-SEC-034, ADR-SEC-046, ADR-SEC-047); REQ-SEC-072…077 added by pass-1 review (G5, G6); REQ-SEC-078 added by pass-1 review (G3); REQ-SEC-079 added by the pass-1 REVISE review (RG6)"
══════════════════════════════════════════════════════════════════
<<<END ARTIFACT>>>
