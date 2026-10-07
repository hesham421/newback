## BUSINESS POLICIES — NOTIFICATION SERVICE
══════════════════════════════════════════════════════════════════
Module      : Notification Service (NOTIF)
P0 Date     : 2026-09-01
Domain KB   : none supplied — derived from domain-profile-ERP.md + ARCH-REF-1.8
P1 reads    : CLIENT-SPECIFIC entries → RULE-IDs marked "Source: Client"
              Standard rules → applied by P1 directly
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES
──────────────────────────────────────────────────────────────────
The cross-cutting design policies (POLICY-CLI-01..03 in business-policies-
CU.md) also apply.

POLICY-CLI-01: Five channels, all active from the start
  Rule   : EMAIL, SMS, WHATSAPP, PUSH, INTERNAL are all built and enabled
           (nothing deferred). Runtime enable/disable is data, per channel.
  Trigger: Dispatch.
  Source : User decision 2026-09-01 (all in scope) + ARCH-REF RESOLUTION-01.

POLICY-CLI-02: Sending module owns channel choice
  Rule   : The module raising the event decides the channel(s) via
           channelHint; Notification never embeds business rules such as
           "overdue invoices need SMS". One log row per requested channel.
  Trigger: Event publish.
  Source : ARCH-REF AD-NOTIF-10.

POLICY-CLI-03: Retry then fail
  Rule   : On send failure, retry up to 5 times (2s initial, 1.5x backoff);
           after that the channel row is marked FAILED. A disabled channel
           is logged as CHANNEL_DISABLED (not retried).
  Trigger: Dispatch failure / disabled channel.
  Source : ARCH-REF AD-NOTIF-01 (reference default — tune at P1).

──────────────────────────────────────────────────────────────────
CUSTOM LOV VALUES
──────────────────────────────────────────────────────────────────
NotificationChannel : EMAIL, SMS, WHATSAPP, PUSH, INTERNAL
NotificationStatus  : PENDING, SENT, FAILED, CHANNEL_DISABLED
(owned locally by Notification — see module-registry-NOTIF.md)

──────────────────────────────────────────────────────────────────
SCOPE EXCEPTIONS
──────────────────────────────────────────────────────────────────
Excluded : Apache Camel — email uses Spring JavaMailSender directly
           (medium complexity, no heavy integration framework).
Excluded : RabbitMQ / external message broker — dispatch is triggered by
           in-process Spring events via Common Utils.
Deferred : SMS / WhatsApp / Push concrete provider selection (Twilio /
           Unifonic / Meta Cloud API / BSP, Firebase project) — a P3
           TECHNICAL config decision, NON-BLOCKING for P0/P1. Channel table
           shape is provider-independent; creds live in
           NotificationChannelConfig.config_json. Not deferred WORK on the
           module — only the vendor pick.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Every template, channel configuration, log row and inbox item belongs to one tenant. A new tenant receives copies of PLATFORM's channel configurations (without provider credentials) and templates (without attachment) at provisioning. | NEW | docs/steps/05-report.md; DEVIATIONS [05] (provisioning entry) |
| 2 | Dispatch is asynchronous: the dispatch call only records one `QUEUED` row per enabled requested channel and returns; delivery runs after commit on a worker. Nothing is sent synchronously. | CHANGED (POLICY-CLI-02 fan-out unchanged) | 08-STEP; docs/steps/08-report.md |
| 3 | POLICY-CLI-03 "retry then fail": implemented as 5 attempts with waits 2, 4, 8, 16 s (initial 2 s, multiplier 2, cap 32 s; all `erp.core.notif.retry.*`), then `FAILED`. Analysis said ×1.5 backoff; implemented ×2 because the step-08 plan prescribes "exponential backoff 2s→32s". Attempts never exceed the maximum, also across crash-recovery requeues (1.2.0). | CHANGED | DEVIATIONS [08] (retry entries), [15] |
| 4 | POLICY-CLI-01 "five channels, all active": channel delivery is a provider SPI. erp-core ships real providers only for EMAIL (when a mail sender is configured) and the new IN_APP inbox channel. SMS, WHATSAPP, PUSH and INTERNAL end `SKIPPED_NO_PROVIDER` until an application contributes a `ChannelProvider` (they no longer fake success). | CHANGED | 08-STEP ("EMAIL impl; SMS/PUSH/IN_APP contracts"); DEVIATIONS [08] (logging provider, `EmailChannelProvider`) |
| 5 | Delivery is at-least-once; a row is claimed before it is sent so that a duplicate run or the requeue job never sends it twice (1.2.0). Crash recovery (requeue of stale `QUEUED` rows) is opt-in (`erp.core.notif.requeue.enabled`, default false) and should be enabled in production. | NEW | DEVIATIONS [08], [15]; docs/CONSUMING.md §7 |
| 6 | An EMAIL goes to the recipient's account e-mail unless the request overrides it (`variables.email`); an EMAIL with no address at all is a permanent failure (FAILED after one attempt, never retried). | NEW | DEVIATIONS [14]; CHANGELOG [1.1.0] |
| 7 | RULE-NOTIF-007 (no dispatch to an inactive recipient): "active" now means ACTIVE or `PENDING_VERIFICATION`, so a newly registered customer receives the verification mail. Recipients may be STAFF or CUSTOMER accounts. | CHANGED | DEVIATIONS [06], [08] |
| 8 | Dispatch variables (which can carry single-use link tokens) are kept only while a row is `QUEUED` and cleared at any final status; they are never exposed by the API. | NEW | DEVIATIONS [08] (`VARIABLES_JSON` entry) |
| 9 | Template changes are recorded in the platform audit log. | NEW | DEVIATIONS [10] |

Scope exceptions: unchanged — no Camel, no external broker (the events bus is in-process,
`com.erp.events`). The concrete SMS / WhatsApp / Push vendor stays an application decision, now realised
as an application-supplied `ChannelProvider`.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | A staff user is told by e-mail when their password is set by an administrator or changed by themselves: SEC publishes `UserPasswordChangedEvent(userId, byAdmin)` and NOTIF dispatches `STAFF_PASSWORD_CHANGED`, seeded in every tenant and copied to new ones. This is NOTIF's first consumption of an event it does not own (from the shared `com.erp.events` bus). | NEW | srs.md 1.3.0 (RULE-NOTIF-023, XM-NOTIF-003) |
