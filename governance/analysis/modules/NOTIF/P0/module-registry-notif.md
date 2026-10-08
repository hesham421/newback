## MODULE REGISTRY — NOTIFICATION SERVICE
══════════════════════════════════════════════════════════════════
Module Name    : Notification Service
Module Code    : NOTIF
Layer          : L1
Type           : Service (Foundation — multi-channel notification dispatch)
Execution Tier : T1 (built last of the Foundation set — needs CU + SEC + FILE)
P0 Date        : 2026-09-01
Readiness      : READY
Domain KB      : none supplied — derived from domain-profile-ERP.md + ARCH-REF-1.8 (idea source)
Source         : NEW  (fresh — ARCH-REF-1.8-NOTIFICATION-SERVICE.md used as IDEA reference only)
══════════════════════════════════════════════════════════════════

SCOPE NOTE
──────────────────────────────────────────────────────────────────
Reusable multi-channel notification foundation. Five channels, all built
and enabled in full: EMAIL, SMS, WHATSAPP, PUSH, INTERNAL. Unified table
design (one log, one template, one channel-config table) with a channel_type
discriminator — no per-channel tables. The SENDING module chooses channels
via a channelHint on the event; Notification stays business-logic-neutral
and fans out one log row per requested channel. Adapted from the HEAC
reference for Modular Monolith + medium complexity: RabbitMQ → in-process
Spring events (via Common Utils); Apache Camel email routing → plain Spring
JavaMailSender (no heavy integration framework); provider creds live in
channel config JSON, not in code.

ENTITIES OWNED
──────────────────────────────────────────────────────────────────
NotificationLog           │ Transactional │ PRIVATE  (one row per channel per event)
NotificationTemplate      │ Config/Master │ PRIVATE  (inline body_ar/en; optional file_id attachment)
NotificationChannelConfig │ Config        │ PRIVATE  (per-channel enable flag + provider config_json)
──────────────────────────────────────────────────────────────────
Note: names only — ENTITY-IDs assigned by P1. Fields (recipient_id,
notification_type, template_code, status, retry_count, module_code,
reference_id/type, etc.) detailed at P1, not here.

LOVs OWNED
──────────────────────────────────────────────────────────────────
NotificationChannel │ EMAIL / SMS / WHATSAPP / PUSH / INTERNAL          │ channel discriminator
NotificationStatus  │ PENDING / SENT / FAILED / CHANNEL_DISABLED        │ log status lifecycle
──────────────────────────────────────────────────────────────────
Note: LOV-IDs assigned by P1. Owned locally (no MasterData module in
this domain).

LOVs CONSUMED (from other modules)
──────────────────────────────────────────────────────────────────
(none)
──────────────────────────────────────────────────────────────────

SHARED ENTITIES CONSUMED
──────────────────────────────────────────────────────────────────
UserAccount │ Owner: SEC │ SOFT-READ  (recipient identity)
──────────────────────────────────────────────────────────────────
Note: attachments reference File Service via its API (file_id, nullable) —
a service call, not a governed shared-table read.

DEPENDENCIES
──────────────────────────────────────────────────────────────────
Common Utils │ USES (library) │ Events (NotificationEvent), config, exceptions
Security     │ SOFT           │ recipient identity (UserAccount)
File Service │ SOFT / service │ optional attachments via File Service API (file_id)
──────────────────────────────────────────────────────────────────
ROOT: NO — depends on CU (lib) + SEC (SOFT) + FILE (SOFT/service).
Top of the Foundation build order.

AUTO-DECISIONS
──────────────────────────────────────────────────────────────────
AUTO: Five channels (EMAIL/SMS/WHATSAPP/PUSH/INTERNAL) all built and
      enabled (is_enabled_fl=1) from the start.
FROM: ARCH-REF RESOLUTION-01 + domain rule "no partial/deferred work".
IF WRONG: an operator can disable a channel at runtime via config (data, not code).

AUTO: Unified tables (Log/Template/ChannelConfig) with channel_type
      discriminator — no per-channel tables.
FROM: ARCH-REF RESOLUTION-01.
IF WRONG: n/a — splitting per channel is the rejected design.

AUTO: In-process Spring events (via Common Utils Events) — NOT RabbitMQ.
FROM: Modular Monolith (ADAPT-NOTIF-02) + domain "no heavy framework".
IF WRONG: durable async broker opened as a new decision only if needed.

AUTO: Email via Spring JavaMailSender directly — NOT Apache Camel.
FROM: domain "medium complexity, no heavy integration framework"
      (fresh adaptation — reference used Camel from heac-mailservice).
IF WRONG: revisit only if complex routing/enterprise integration is required.

AUTO: Sending module chooses channels via channelHint (single | list | ALL);
      fan-out = one NotificationLog row per channel.
FROM: ARCH-REF AD-NOTIF-10 (business logic stays out of Notification).
IF WRONG: n/a — core neutrality principle.

AUTO: Templates stored inline (body_ar/en); file_id references File Service
      for optional ATTACHMENTS only (not template text).
FROM: ARCH-REF AD-NOTIF-05 + File Service now in-scope.
IF WRONG: move template text to File Service if runtime file-managed templates wanted (P1).

AUTO: Push via Firebase Admin SDK; SMS/WhatsApp via adapter pattern with
      provider creds in NotificationChannelConfig.config_json.
FROM: ARCH-REF ADAPT-NOTIF-06 + AD-NOTIF-02.
IF WRONG: n/a for shape. Concrete SMS/WhatsApp provider = P3 technical
          decision (non-blocking; table shape is provider-independent).

AUTO: Notification does NOT validate JWT — trusts Security's filter.
FROM: ARCH-REF ADAPT-NOTIF-03.
IF WRONG: n/a — single JWT authority = Security.

INF-IDs
──────────────────────────────────────────────────────────────────
(none — all decisions traced to ARCH-REF + domain adaptation via
 AUTO-DECISIONS above; no unresolved gap)
──────────────────────────────────────────────────────────────────
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No ENTITY / LOV / XM ids are minted.

ENTITIES OWNED — deltas
| Entity | Delta | Source |
|---|---|---|
| NotificationInboxItem (`NOTIF_INBOX`) | NEW, PRIVATE, tenant-scoped: one in-app notification of one recipient (SEC_USER id of either realm), written only by the IN_APP channel provider | V13__notif_async_inbox.sql; docs/steps/08-report.md |
| NotificationLog | CHANGED: becomes the delivery queue (attempts, next attempt / lease, last error, transient variables) | V13; DEVIATIONS [08], [15] |
| all three analysed entities | CHANGED: tenant-scoped (`TENANT_ID`) + optimistic lock (`VERSION`) | V10__tenant_schema.sql |

LOVs OWNED — deltas
| LOV | Delta | Source |
|---|---|---|
| NotificationChannel (`NOTIF_CHANNEL`) | + `IN_APP` | V13 §3a |
| NotificationStatus (`NOTIF_STATUS`) | + `QUEUED`, + `SKIPPED_NO_PROVIDER`; `PENDING` is now only the transient in-memory state of a row being built (never persisted) | V13 §3a; DEVIATIONS [08] (state machine) |

DEPENDENCIES — deltas
| Module | Kind | What | Source |
|---|---|---|---|
| tenant | HARD FK + SPI | `CORE_TENANT` FK; `TenantProvisioningContributor` (`NotifTenantProvisioningContributor` copies channel configs without `CONFIG_JSON` and templates without attachment) | DEVIATIONS [05] |
| events (replaces "CU Events") | USES | `NotificationRequestedEvent` (listened to), `NotificationDispatchedEvent`, `NotificationFailedEvent` (published) on the shared `com.erp.events` bus; async executor `erpCoreEventExecutor` | DEVIATIONS [08] (where the core events live) |
| SEC | SOFT (in-core API) | `SecUserDirectoryApi.findContact` (active check, account e-mail) and `findCurrentUserId()` (inbox caller), through NOTIF's own `RecipientDirectory` port | DEVIATIONS [08], [14] |
| audit | SOFT | `@Audited` on `NotificationTemplate` | DEVIATIONS [10] |
| report | SPI | `NotifLogSummaryReport` (`NOTIF_LOG_SUMMARY`) | DEVIATIONS [11] |

EXPOSED SURFACE — deltas
| Surface | Delta | Source |
|---|---|---|
| `com.erp.notif.channel.ChannelProvider` (`channel()`, `send(OutboundMessage)` → `DeliveryResult`) | NEW public SPI; core providers EMAIL (only with a `JavaMailSender`) and IN_APP; any other channel falls back to a logging stand-in → `SKIPPED_NO_PROVIDER`; an application provider wins over a core one | docs/steps/08-report.md; DEVIATIONS [08] |
| `DeliveryStatus.REJECTED` / `DeliveryResult.rejected(...)` | NEW (1.1.0): permanent failure, never retried | CHANGELOG [1.1.0] |
| `RecipientDirectory.emailOf(Long)` | NEW (1.1.0), no default implementation (an implementation outside core must add it) | CHANGELOG [1.1.0] |
| `NotificationDispatchApi` | signature unchanged; now asynchronous | docs/steps/08-report.md |

PERMISSIONS
Now code-defined by `NotifPermissions` (same authorities as before:
`PERM_NOTIF_TEMPLATES_*`, `PERM_NOTIF_CHANNELS_*`, `PERM_NOTIF_LOG_VIEW`). The inbox needs no permission
(`isAuthenticated()`, own items only). — docs/steps/06-report.md; DEVIATIONS [08] (inbox entry)

AUTO-DECISION revisited
"In-process Spring events (via Common Utils Events) — NOT RabbitMQ" holds; the bus is the dedicated
`com.erp.events` module and delivery is asynchronous after commit (step 08).

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — a usage count for the platform (package D.3 changed nothing here)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

EXPOSED SURFACE — delta
| Surface | Delta | Consumer | Source |
|---|---|---|---|
| `com.erp.notif.crossmodule.NotificationLogQueryApi` | NEW method `countDispatchedSince(Instant)` | TENANT (`GET /api/v1/platform/tenants/{id}/usage`) | `P1/srs.md` 1.3.0 §4 |
Entities owned, lookups, permissions, dependencies: unchanged.

Package C12 (tenant-maturity plan §5 C.1) — dependency deltas; full text in `P1/srs.md` 1.3.0 §5.
| Kind | Module | Kind of link | What | Source |
|---|---|---|---|---|
| NEW | tenant | crossmodule (XM-NOTIF-004) | `TenantLookupApi.isActive(Long)` — read at every delivery claim and by the requeue job (RULE-NOTIF-024) | `P1/srs.md` 1.3.0 §5 |
| NEW | events (TENANT publishes) | event bus (XM-NOTIF-005) | consumes `TenantActivatedEvent` → re-dispatches the tenant's held `QUEUED` rows; `TenantSuspendedEvent` exists but NOTIF registers no listener for it | same |
