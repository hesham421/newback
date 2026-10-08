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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here; the rule ids
are those of `../P1/srs.md`.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Every template, channel configuration, log row and inbox item belongs to one tenant. A new tenant receives copies of PLATFORM's channel configurations (without `CONFIG_JSON`) and templates (without attachment) at provisioning. | NEW | docs/steps/05-report.md; DEVIATIONS [05] (provisioning entry); RULE-NOTIF-021 |
| 2 | Dispatch is asynchronous: the dispatch call only records one row per requested channel (`QUEUED`, or `CHANNEL_DISABLED`) and returns 200; delivery runs after commit on a worker, at-least-once. Nothing is sent synchronously. | CHANGED (POLICY-CLI-02 fan-out unchanged) | docs/steps/08-report.md; RULE-NOTIF-008; ADR-NOTIF-002 |
| 3 | POLICY-CLI-03 "retry then fail": 5 attempts with waits 2, 4, 8, 16 s (`erp.core.notif.retry.max-attempts` 5, `initial-delay-ms` 2000, `multiplier` 2.0, `max-delay-ms` 32000), then `FAILED`. Analysis said ×1.5 backoff; implemented ×2 because the step-08 plan prescribes "exponential backoff 2s→32s". Attempts never exceed the maximum, also across crash-recovery requeues (1.2.0). | CHANGED | DEVIATIONS [08] (retry entries), [15]; RULE-NOTIF-002, -015; ADR-NOTIF-003 |
| 4 | POLICY-CLI-01 "five channels, all active": channel delivery is a provider SPI and a channel is active only where a `NOTIF_CHANNEL_CONFIG` row exists. erp-core seeds rows for EMAIL (V9) and the new IN_APP channel (V13) only and ships real providers for exactly those two (EMAIL when a mail sender is configured). SMS, WHATSAPP, PUSH and INTERNAL have no configuration row, so a dispatch to them ends `CHANNEL_DISABLED`; once an operator creates their row they end `SKIPPED_NO_PROVIDER` until an application contributes a `ChannelProvider` (they no longer fake success). | CHANGED | erp-core/src/main/java/com/erp/notif/service/DispatchService.java:133-141; V9__notif_seed.sql:19-20; V13__notif_async_inbox.sql:85-89; DEVIATIONS [08]; RULE-NOTIF-003, -009; ADR-NOTIF-004 |
| 5 | Delivery is at-least-once; a row is claimed (lease in `NEXT_ATTEMPT_AT`, ownership by `VERSION`) before it is sent so that a duplicate run or the requeue job never sends it twice (1.2.0); a node-local tracker drops duplicate events on the same node. Crash recovery (requeue of stale `QUEUED` rows) is opt-in (`erp.core.notif.requeue.enabled`, default false; `interval-ms` 60000; `stale-after-minutes` 10) and should be enabled in production. | NEW | DEVIATIONS [08], [15]; docs/CONSUMING.md §7; RULE-NOTIF-016, -017, -018; ADR-NOTIF-002 |
| 6 | An EMAIL goes to the recipient's account e-mail unless the request overrides it (`variables.email`); an EMAIL with no address at all is a permanent failure (`FAILED` after one attempt, never retried). A mail is single-language (`variables.lang` = `AR` → Arabic, else English), HTML-escaped, with `actionLink` rendered as a button; `From` = `spring.mail.username`. | NEW | DEVIATIONS [14]; CHANGELOG [1.1.0]; RULE-NOTIF-011 |
| 7 | RULE-NOTIF-007 (no dispatch to an inactive recipient): "active" now means ACTIVE or `PENDING_VERIFICATION`, so a newly registered customer receives the verification mail. Recipients may be STAFF or CUSTOMER accounts; an unknown id counts as inactive. | CHANGED | DEVIATIONS [06], [08]; RULE-NOTIF-019; ADR-NOTIF-007 |
| 8 | Dispatch variables (which can carry single-use link tokens) are kept only while a row is `QUEUED` and cleared at any final status; they are never exposed by the API. | NEW | DEVIATIONS [08] (`VARIABLES_JSON` entry); RULE-NOTIF-020 |
| 9 | Template changes are recorded in the platform audit log. | NEW | DEVIATIONS [10] |
| 10 | SCOPE EXCEPTIONS "creds live in `NotificationChannelConfig.config_json`": the column is stored and returned but no provider reads it; a vendor pick is realised as an application-supplied `ChannelProvider` bean configured through the application's own properties (the EMAIL provider through `spring.mail.*`). No Camel, no external broker (the bus is the in-process `com.erp.events`). | CHANGED | erp-core/src/main/java/com/erp/notif/channel/EmailChannelProvider.java:38-41; ADR-NOTIF-004 |
| 11 | POLICY-CLI-02 `channelHint`: a list of channel codes only (trimmed, upper-cased); the `ALL` wildcard is not supported and duplicates are not removed (one row per element). | CHANGED | RULE-NOTIF-013, -014 |
| 12 | Template attachments: the `file_id` is validated at write time (an unavailable FILE document is refused) but no channel sends the file. | CHANGED | RULE-NOTIF-004 |
| 13 | CUSTOM LOV VALUES: NotificationChannel + `IN_APP`; NotificationStatus + `QUEUED`, `SKIPPED_NO_PROVIDER` (`PENDING` never stored); both lists live in MDL (`NOTIF_CHANNEL`, `NOTIF_STATUS`, owner NOTIF) and are read through `MdlLookupApi`. | CHANGED | V8__mdl_seed.sql; V13 §3a; ADR-NOTIF-006 |
| 14 | Dispatch is open to every authenticated staff principal (no permission; `isAuthenticated()`), including the `variables.email` override — accepted for 1.2.0 with the risk and mitigation candidates recorded in ADR-NOTIF-005. | NEW (decision) | erp-core/src/main/java/com/erp/notif/service/DispatchService.java:69-73; ADR-NOTIF-005 |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | A staff user is told by e-mail when their password is set by an administrator or changed by themselves: SEC publishes `UserPasswordChangedEvent(userId, byAdmin)` and NOTIF dispatches `STAFF_PASSWORD_CHANGED`, seeded in every tenant and copied to new ones. This is NOTIF's first consumption of an event it does not own (from the shared `com.erp.events` bus). | NEW | srs.md 1.3.0 (RULE-NOTIF-023, XM-NOTIF-003) |
| 2 | Package C12. A suspended tenant's queued notifications are not delivered while it is suspended: no attempt claims them and the requeue job skips them; they keep their status (`QUEUED`, no new status) and are sent once the tenant is activated again. | NEW | srs.md 1.3.0 §5 (RULE-NOTIF-024, XM-NOTIF-004/005) |

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no NOTIF behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 15 | API-NOTIF-006 keeps its JSON shape while its response type moves to `com.erp.common.lookup.LookupOptionResponse` (shared with FILE). | CHANGED | CHANGELOG [Unreleased] |
