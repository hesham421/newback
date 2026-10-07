# PRD — Notification Service (NOTIF)
══════════════════════════════════════════════════════════════════
Module          : Notification Service (NOTIF prefix)
Source artifacts: platform-summary.md, module-registry-NOTIF.md,
                  business-policies-NOTIF.md
Status          : DRAFT — awaiting Reconciliation Gate (Project 2.5)
Open Questions  : None — see OQ Log
══════════════════════════════════════════════════════════════════

## USER STORIES

US-NOTIF-001
  Story    : كموديول مستهلِك، أحتاج إشعار المستخدم عبر قناة واحدة أو
             أكثر، ليصله ما يخصّه من أحداث.
  Priority : —
  Success metric : —
  Source   : module-registry-NOTIF.md §SCOPE NOTE (multi-channel dispatch);
             entity NotificationLog; business-policies-NOTIF.md §POLICY-CLI-01
  Status   : DRAFT

US-NOTIF-002
  Story    : كموديول مُرسِل، أحتاج اختيار القناة/القنوات لكل إشعار عبر
             channelHint، لتبقى منطقية اختيار القناة عند الموديول لا عند
             خدمة الإشعارات.
  Priority : —
  Success metric : —
  Source   : business-policies-NOTIF.md §POLICY-CLI-02 (sending module owns
             channel choice); module-registry-NOTIF.md §SCOPE NOTE (channelHint)
  Status   : DRAFT

US-NOTIF-003
  Story    : كأدمن، أحتاج قوالب رسائل ثنائية اللغة (عربي/إنجليزي) مع
             إمكانية إرفاق ملف اختياري، لتكون الإشعارات متّسقة ومترجمة.
  Priority : —
  Success metric : —
  Source   : module-registry-NOTIF.md §ENTITIES OWNED (NotificationTemplate —
             inline body_ar/en; optional file_id attachment)
  Status   : DRAFT

US-NOTIF-004
  Story    : كمشغّل، أحتاج تمكين/تعطيل كل قناة وحفظ إعدادات مزوّدها كبيانات،
             لتكون القنوات قابلة للتهيئة دون تعديل الكود.
  Priority : —
  Success metric : —
  Source   : module-registry-NOTIF.md §ENTITIES OWNED (NotificationChannelConfig
             — per-channel enable flag + provider config_json)
  Status   : DRAFT

US-NOTIF-005
  Story    : كمشغّل، أحتاج رؤية حالة كل إشعار ونتيجة تسليمه (أُرسل / فشل /
             القناة معطّلة)، لتتبّع مشاكل التسليم.
  Priority : —
  Success metric : —
  Source   : module-registry-NOTIF.md §ENTITIES OWNED (NotificationLog);
             §LOVs OWNED (NotificationStatus); business-policies-NOTIF.md
             §POLICY-CLI-03 (retry then fail)
  Status   : DRAFT

## STORIES EXCLUDED (justified)

  — NotificationChannel / NotificationStatus (LOVs) — قوائم قيم داعمة
    للقصص أعلاه، لا قصص مستقلة.
  — سياسة إعادة المحاولة الدقيقة (5 محاولات، backoff 1.5x) — تفصيل
    RULE-level يقرّره P1؛ الحاجة (رؤية النتيجة) مغطّاة في US-NOTIF-005.

## SCOPE EXCLUSIONS (خارج النطاق — من P0)

  — Apache Camel — البريد عبر Spring JavaMailSender مباشرةً.
  — RabbitMQ / وسيط رسائل خارجي — الإطلاق عبر أحداث Spring داخلية (CU).

## OPEN ITEMS (ambiguous, not yet a story)

  ? اختيار مزوّد SMS / WhatsApp / Push الملموس (Twilio / Unifonic /
    Meta Cloud API / Firebase) — مؤجّل كقرار تقني P3، غير حاجب لـ P0/P1؛
    شكل الجدول مستقل عن المزوّد والاعتمادات في config_json
    (business-policies-NOTIF §SCOPE EXCEPTIONS + platform-summary §OPEN ITEMS).

══════════════════════════════════════════════════════════════════
*End of prd-NOTIF.md*
*Next stage: Project 2.5 (UI/UX Design Engine) — requires this file
 AND srs.md together (CONTRACT-11). Does not gate Project 1.*
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 05, 06, 08, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| In-app inbox | any signed-in user (staff or customer) | Lists own in-app notifications (newest first, optional unread-only) and marks one read (idempotent, keeps the first read time). Another user's item looks like a missing one. | DEVIATIONS [08] (inbox entries) |
| Customer account mails | SEC (customer realm) | Templates `CUSTOMER_VERIFY_EMAIL` and `CUSTOMER_PASSWORD_RESET` (AR/EN, `{actionLink}`, `{expiresAt}`) exist in every tenant. | V11__sec_realms.sql; DEVIATIONS [06] |
| Pluggable channels | application developer | An application adds a delivery channel (e.g. an SMS gateway) by contributing a `ChannelProvider`; without one the channel's rows end `SKIPPED_NO_PROVIDER`. | docs/steps/08-report.md; docs/CONSUMING.md §7 |
| Delivery summary report | operator with `NOTIF:REPORT:NOTIF_LOG_SUMMARY` | Counts per channel, status and day; run as JSON or exported as CSV / JSON. | DEVIATIONS [11] |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-NOTIF-001 notify a user over one or more channels | Dispatch returns at once (log ids); delivery happens asynchronously after commit, retried 5 times (2 → 16 s waits). EMAIL uses the recipient's account e-mail unless `variables.email` overrides it. | docs/steps/08-report.md; DEVIATIONS [14] |
| US-NOTIF-004 enable / disable channels | A disabled channel still logs `CHANNEL_DISABLED`; an enabled channel without a provider logs `SKIPPED_NO_PROVIDER`. A new tenant starts with PLATFORM's channel configs (credentials not copied). | DEVIATIONS [08], [05] |
| US-NOTIF-005 see each notification's status | New visible statuses `QUEUED` and `SKIPPED_NO_PROVIDER`; each log shows `attempts`, `nextAttemptAt` (lease expiry while in flight) and `lastError`. | docs/api-docs/notif/endpoints/notification-logs.md; DEVIATIONS [15] |

Scope note: "five channels, all built and enabled" (STORIES / POLICY-CLI-01) is realised as five
channel codes plus `IN_APP`, with real core delivery for EMAIL and IN_APP only (step-08 plan).

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Password-change e-mail | staff user (recipient) | When an administrator sets a staff user's password or the user changes it, the user receives `STAFF_PASSWORD_CHANGED` (when and by whom). The template exists in every tenant and can be edited like any template. | srs.md 1.3.0 (RULE-NOTIF-023); V17 |
