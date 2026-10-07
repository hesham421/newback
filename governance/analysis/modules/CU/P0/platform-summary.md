# Platform Vision Summary
## Foundation

```
Platform      : Foundation
Domain        : ERP  (see _domain/domain-profile-ERP.md)
Stack         : Spring Boot / Java · PostgreSQL 16 · React(TS)+Vite   (GOVERNANCE-CONFIG.md)
Workflow      : OFF (RULE-13 / GOVERNANCE-CONFIG §8)
P0 Date       : 2026-09-01
Status        : CONFIRMED — Phase 1 complete
```

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
OVERVIEW
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

مكتبة مكوّنات أساس (Foundation) مملوكة ذاتيًا، تُبنى كأصول جاهزة:
Reusable + Configurable + Integrable + Composable. الهدف إغلاق مكوّنات
الأساس أولًا كأصول قابلة للتركيب — لا بناء تطبيق تجاري ولا المنصة
كاملةً مقدّمًا. domains الأعمال المستقبلية (Accounting / HR / E-Commerce)
تعتمد على هذا الأساس لاحقًا كـ domains منفصلة، والأساس مُعتمَد-عليه من
الجميع ولا يعتمد على أيٍّ منها. مبدأ حاكم: تعقيد متوسط مقصود — يُتجنَّب
الإفراط في الهيكلة والتجريد؛ الحل الأبسط الذي يفي بالمتطلب هو المطلوب.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
MODULES   (all Foundation · Tier 1 · Status = NEW)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

| #   | Module               | Code  | Layer | Type          | Depends On                          | Status |
|-----|----------------------|-------|-------|---------------|-------------------------------------|--------|
| 1.1 | Security             | SEC   | L1    | Engine        | Common Utils                        | NEW    |
| 1.2 | Notification Service | NOTIF | L1    | Service       | Common Utils, Security, File Service| NEW    |
| 1.3 | File Service         | FILE  | L1    | Service       | Common Utils, Security              | NEW    |
| 1.4 | Common Utils         | CU    | L1    | Cross-Cutting | ROOT                                | NEW    |

Status values:
  NEW → to be built — Phase 2 produces module-registry + business-policies
Numbering [Tier].[seq] — fixed on first assignment, never shifts.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
DEPENDENCY MAP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Build order (lower builds first):
  1. Common Utils        (ROOT — cross-cutting, depended-on by all)
  2. Security            (→ Common Utils)
  3. File Service        (→ Common Utils, + Security integration)
  4. Notification Service(→ Common Utils, Security, File Service)

Cross-module dependencies (all in-scope, built in full — nothing deferred):
  Notification → HARD → File Service : template + attachment storage/retrieval
  Notification → SOFT → Security     : recipient identity
  File Service → SOFT → Security      : trusts Security auth filter (no self JWT check)
  ALL          → USES → Common Utils  : exceptions / config / events / specification-filtering

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
OUT OF BOUNDS   (not "deferred" — a different domain entirely)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Business modules (Accounting / HR / E-Commerce / …) are separate future
domains that depend on this Foundation. They are out of this domain's
scope per domain-profile-ERP.md — not a deferred item inside it. Inside
the Foundation platform, every module and every dependency above is
in scope and built in full.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
OPEN ITEMS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

None — platform scope fully determined and confirmed by the architecture
authority (Hesham). Provider choices for Notification channels
(SMS / WhatsApp / Push) are P3-level technical decisions, non-blocking
for P0/P1, resolved inside NOTIF_CHANNEL_CONFIG rather than table shape.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
REFERENCES (idea sources only — NOT authoritative artifacts)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  ARCH-REF File Service (ex-1.10)    → informs FILE architecture (BYTEA, AES-GCM token)
  ARCH-REF Notification (ex-1.8)     → informs NOTIF architecture (5 channels)
  srs-SECURITY.md (prior, v2.10 reg) → informs SEC — re-derived fresh, not carried as-is

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
NEXT STEP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Phase 2 runs one module at a time on request by number.
Recommended order: 1.4 → 1.1 → 1.3 → 1.2.
SRS is produced in P1 (SRS Governance Engine) — never here.

*End of platform-summary.md — Foundation platform, P0 Phase 1.*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01–12 (plan), 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

The full description of the implemented platform (erp-core library + consuming apps, the new core
modules tenant / audit / events / sequence / report, conventions, release policy) is recorded once in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (decision recorded there).

CU's own platform-relevant changes:
| Change | Source |
|---|---|
| Settings: platform default + tenant override, typed cached `SettingsApi`, `?scope=TENANT` or `PLATFORM` on the configuration API | docs/steps/09-report.md |
| `com.erp.common` is public API of the versioned library (`AuditableEntity` / `GlobalAuditableEntity`, `ApiResponse`, `LocalizedException`, `SpecBuilder`, `PageableBuilder`, `GlobalExceptionHandler`) | docs/RELEASE.md |
| Events capability moved to the `com.erp.events` core module (async, after commit) | docs/steps/08-report.md |
| Error handling hardening: 404 `NOT_FOUND`, 400 for overflowing `page`, wrapped `LocalizedException` (1.2.0) | docs/steps/15-report.md |
