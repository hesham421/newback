# ADR-NOTIF-004 — ChannelProvider SPI with application-first precedence replaces CONFIG_JSON-driven adapters and Firebase

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — step 08)
Status  : ACCEPTED (non-breaking)

## Context
The P0 AUTO-DECISIONs said "five channels (EMAIL/SMS/WHATSAPP/PUSH/INTERNAL) all built and enabled",
"Push via Firebase Admin SDK; SMS/WhatsApp via adapter pattern with provider creds in
`NotificationChannelConfig.config_json`", and OQ-NOTIF-001 deferred the concrete vendor to P3. erp-core is a
library consumed by applications: it cannot carry a vendor SDK (Firebase, Twilio, Meta) for every consumer,
cannot know how an application stores its credentials, and must not fake a successful send on a channel it
cannot deliver (step 02's stub did exactly that for every non-EMAIL channel).

## Decision
- Delivery is a bean SPI: `com.erp.notif.channel.ChannelProvider { channel(); send(OutboundMessage) →
  DeliveryResult }`. `ChannelProviderRegistry` resolves, per channel: an application provider bean → the core
  provider → a `LoggingChannelProvider` stand-in that answers `SKIPPED_NO_PROVIDER` (reason
  `NOTIF_CHANNEL_UNAVAILABLE`, final).
- Core ships `EmailChannelProvider` (registered only when a `JavaMailSender` bean exists, configured by
  `spring.mail.*`) and `InAppChannelProvider` (the `NOTIF_INBOX` channel added in step 08). Nothing for SMS,
  WHATSAPP, PUSH or INTERNAL; no Firebase dependency.
- `NOTIF_CHANNEL_CONFIG` keeps its analysed shape (the enable flag drives RULE-NOTIF-003; `CONFIG_JSON`
  stays a nullable TEXT column, stored and returned by the API) but no provider reads `CONFIG_JSON`, and the
  message handed to a provider (`OutboundMessage`) does not carry it. Only EMAIL (V9) and IN_APP (V13) are
  seeded with a configuration row; a channel without a row counts as disabled.

## Consequences
- POLICY-CLI-01 "five channels, all active" is not what ships: SMS, WHATSAPP, PUSH and INTERNAL end
  `CHANNEL_DISABLED` (no row) or `SKIPPED_NO_PROVIDER` (row, no provider) until an application contributes
  a provider bean and an operator creates the row. The frontend must expect both statuses.
- An application provider's configuration lives in the application's own properties; `CONFIG_JSON` is
  effectively dead data in 1.2.0. Either passing the channel configuration to providers or dropping the
  column from the API is a later decision (removing it from the API would be breaking).
- `DeliveryResult.rejected` (1.1.0) lets a provider refuse a message permanently (e.g. EMAIL without an
  address) without consuming the retry budget.
- Provider precedence is deterministic (application bean wins), so an application can replace the core
  EMAIL or IN_APP provider without touching core.

## Traces
POLICY-CLI-01 · RULE-NOTIF-003, RULE-NOTIF-009, RULE-NOTIF-011, RULE-NOTIF-012 · DBF-0024, DBF-0025 ·
srs.md addendum §2, §7 · module-registry-notif.md addendum AUTO-DECISIONS · code:
`erp-core/src/main/java/com/erp/notif/channel/ChannelProvider.java`, `DeliveryResult.java`,
`EmailChannelProvider.java:38-86`, `InAppChannelProvider.java`, `LoggingChannelProvider.java`,
`service/ChannelProviderRegistry.java:26-46`, `com/erp/autoconfigure/ErpCoreNotifAutoConfiguration.java:55-65`,
`db/migration/core/V9__notif_seed.sql:19-20`, `V13__notif_async_inbox.sql:85-89` · docs/CONSUMING.md §7 ·
docs/steps/08-report.md · DEVIATIONS [08], [14], [15]
