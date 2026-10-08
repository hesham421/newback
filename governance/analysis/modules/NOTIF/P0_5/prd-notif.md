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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here.

NEW product capabilities
| Kind | Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|---|
| NEW | In-app inbox | any signed-in user (staff or customer) | Lists own in-app notifications (newest first, optional unread-only, ≤ 200 per page) and marks one read (idempotent, keeps the first read time). Another user's item looks like a missing one. | DEVIATIONS [08] (inbox entries); srs.md addendum §1 |
| NEW | Customer account mails | SEC (customer realm) | Templates `CUSTOMER_VERIFY_EMAIL` and `CUSTOMER_PASSWORD_RESET` (AR/EN, `{actionLink}`, `{expiresAt}`) exist in every tenant; SEC sends them through `NotificationDispatchApi.dispatchIndependently`. | V11__sec_realms.sql §6; DEVIATIONS [06]; ADR-NOTIF-001 |
| NEW | Pluggable channels | application developer | An application adds a delivery channel (e.g. an SMS gateway) by contributing a `ChannelProvider` bean and creating the channel's configuration row; a channel with a row but no provider ends `SKIPPED_NO_PROVIDER`, a channel without a row `CHANNEL_DISABLED`. | docs/steps/08-report.md; docs/CONSUMING.md §7; ADR-NOTIF-004 |
| NEW | Delivery summary report | operator with `NOTIF:REPORT:NOTIF_LOG_SUMMARY` | Counts per channel, status and day; run as JSON or exported as CSV / JSON. | DEVIATIONS [11] |

CHANGED behaviour of existing stories
| Kind | Story | Delta | Source |
|---|---|---|---|
| CHANGED | US-NOTIF-001 notify a user over one or more channels | Dispatch returns at once (200, log ids); delivery happens asynchronously after commit, retried 5 times (2 → 16 s waits), at-least-once. An EMAIL is single-language (`variables.lang`), uses the recipient's account e-mail unless `variables.email` overrides it, and fails permanently without any address. Any signed-in staff user may dispatch (no permission) — ADR-NOTIF-005. | docs/steps/08-report.md; DEVIATIONS [14]; srs.md addendum RULE-NOTIF-008, -011 |
| CHANGED | US-NOTIF-002 choose channels via `channelHint` | A list of channel codes only (trimmed, upper-cased); no `ALL` wildcard; duplicates produce duplicate rows. | srs.md addendum RULE-NOTIF-013, -014 |
| CHANGED | US-NOTIF-003 bilingual templates with optional attachment | Bilingual bodies and names are mandatory (400 otherwise); the attachment `file_id` is validated against FILE at save time (404 when unavailable) and stored, but **no channel sends the file** — the attachment is not delivered in 1.2.0. Template changes are audited. | srs.md addendum RULE-NOTIF-004 (CHANGED); DEVIATIONS [10] |
| CHANGED | US-NOTIF-004 enable / disable channels | A disabled channel still logs `CHANNEL_DISABLED`; a channel with no configuration row counts as disabled; an enabled channel without a provider logs `SKIPPED_NO_PROVIDER`. Provider settings in `config_json` are stored but read by no provider (a provider is configured in the application); `PUT` replaces the whole record (an omitted `configJson` clears it). A new tenant starts with PLATFORM's channel configs (credentials not copied). | DEVIATIONS [08], [05]; srs.md addendum RULE-NOTIF-003, §1 |
| CHANGED | US-NOTIF-005 see each notification's status | New visible statuses `QUEUED` and `SKIPPED_NO_PROVIDER`; each log shows `attempts`, `nextAttemptAt` (lease expiry while in flight, next retry otherwise) and `lastError`; the list is `POST /logs/search` with filters recipient, module, channel, status, reference type and sent-at range. | docs/api-docs/notif/endpoints/notification-logs.md; DEVIATIONS [15] |
| CHANGED | STORIES EXCLUDED "retry policy 5 attempts, backoff 1.5x" | implemented ×2 (2, 4, 8, 16 s), configurable through `erp.core.notif.retry.*` | ADR-NOTIF-003 |
| CHANGED | SCOPE EXCLUSIONS "dispatch via internal Spring events (CU)" | the trigger is a direct in-process call to `NotificationDispatchApi`; NOTIF listens to no foreign event | ADR-NOTIF-001 |
| CHANGED | OPEN ITEMS "SMS / WhatsApp / Push vendor … credentials in `config_json`" | the vendor is an application `ChannelProvider` bean with its own configuration; `config_json` plays no part; no Firebase dependency | ADR-NOTIF-004 |

Scope note
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | "five channels, all built and enabled" (STORIES / POLICY-CLI-01) | six channel codes (`IN_APP` added) in MDL; real core delivery and a seeded, enabled configuration for EMAIL and IN_APP only; SMS, WHATSAPP, PUSH, INTERNAL end `CHANNEL_DISABLED` until an operator creates their row and an application supplies a provider | V9__notif_seed.sql; V13__notif_async_inbox.sql §3; ADR-NOTIF-004 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Password-change e-mail | staff user (recipient) | When an administrator sets a staff user's password or the user changes it, the user receives `STAFF_PASSWORD_CHANGED` (when and by whom). The template exists in every tenant and can be edited like any template. | srs.md 1.3.0 (RULE-NOTIF-023); V17 |

Package C12 (tenant-maturity plan §5 C.1) — changed behaviour of existing stories; full text in `P1/srs.md` 1.3.0 §5.
| Story | Delta | Source |
|---|---|---|
| US-NOTIF-001 notify a user, US-NOTIF-005 see each notification's status — for a suspended tenant | queued notifications of a suspended tenant are not sent while it is suspended (they stay `QUEUED`, attempts unchanged) and are sent once it is activated again | srs.md 1.3.0 §5 (RULE-NOTIF-024) |
