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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag (`erp-core/src/main/java/com/erp/notif/` unless
another path is given). No ENTITY / LOV / XM ids are minted; the RULE ids are those of `../P1/srs.md`.

ENTITIES OWNED — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| NEW | ENTITY-NOTIF-004 NotificationInboxItem (`NOTIF_INBOX`) | PRIVATE, tenant-scoped: one in-app notification of one recipient (`SEC_USER` id of either realm), written only by the IN_APP channel provider; no link to `NOTIF_LOG` | V13__notif_async_inbox.sql §2; channel/InAppChannelProvider.java:31-44; docs/steps/08-report.md |
| CHANGED | NotificationLog | becomes the delivery queue: `ATTEMPTS`, `NEXT_ATTEMPT_AT` (retry due time / claim lease), `LAST_ERROR`, transient `VARIABLES_JSON` | V13 §1; DEVIATIONS [08], [15] |
| CHANGED | NotificationTemplate | `@Audited`; `ATTACHMENT_FILE_ID` validated at write through FILE, never sent with a message | entity/NotificationTemplate.java:33; service/NotificationTemplateService.java:153-158 |
| CHANGED | NotificationChannelConfig | `CONFIG_JSON` is stored and returned but read by no provider; only EMAIL and IN_APP rows are seeded | V9__notif_seed.sql:19-20; V13 §3b; ADR-NOTIF-004 |
| CHANGED | all three analysed entities | tenant-scoped (`TENANT_ID`) + optimistic lock (`VERSION`); unique keys per tenant | V10__tenant_schema.sql |

LOVs OWNED — deltas
| Kind | LOV | Delta | Source |
|---|---|---|---|
| CHANGED | NotificationChannel (`NOTIF_CHANNEL`) | + `IN_APP`; the values are MDL lookup rows (type owner NOTIF), not NOTIF-local lists; `NotifChannels` carries constants for EMAIL, SMS, PUSH, IN_APP only | V8__mdl_seed.sql:28, 42-47; V13 §3a; channel/NotifChannels.java:13-16; ADR-NOTIF-006 |
| CHANGED | NotificationStatus (`NOTIF_STATUS`) | + `QUEUED`, + `SKIPPED_NO_PROVIDER`; `PENDING` is only the transient in-memory state of a row being built (never persisted); MDL rows | V8:29, 49-52; V13 §3a; DEVIATIONS [08] |
| CHANGED | "Owned locally (no MasterData module in this domain)" | MDL exists and hosts both types; NOTIF fronts them through API-NOTIF-006 and owns their seeds | ADR-NOTIF-006 |

LOVs CONSUMED — deltas
| Kind | LOV | Delta | Source |
|---|---|---|---|
| NEW | `NOTIF_CHANNEL`, `NOTIF_STATUS` (hosted by MDL) | read live through `MdlLookupApi.readActiveValuesByKey` for the lookup endpoint and the report parameters | service/NotificationLookupService.java:39-48; report/NotifLogSummaryReport.java:74-75 |

SHARED ENTITIES CONSUMED — deltas
| Kind | Entity | Delta | Source |
|---|---|---|---|
| CHANGED | UserAccount (SEC) | physical table `SEC_USER`, both realms; read through the `RecipientDirectory` port (`isActive`, `emailOf`, `currentRecipientId`) backed by `SecUserDirectoryApi` | crossmodule/SecRecipientDirectory.java |
| CHANGED | FileDocument (FILE) | the attachment id is checked at write time (`FileDocumentLookupApi.isAvailable`) — a cross-module read, still no FK | service/NotificationTemplateService.java:47, 153-158 |

DEPENDENCIES — deltas
| Kind | Module | Type | What | Source |
|---|---|---|---|---|
| NEW | tenant | HARD FK + SPI | `CORE_TENANT` FK; `TenantProvisioningContributor` (`NotifTenantProvisioningContributor`, order 20, copies channel configs without `CONFIG_JSON` and templates without attachment) | tenant/NotifTenantProvisioningContributor.java; DEVIATIONS [05] |
| CHANGED | events (replaces "CU Events") | USES | own `NotificationRequestedEvent` (listened to), `NotificationDispatchedEvent`, `NotificationFailedEvent` (published) on the shared `com.erp.events` bus; async executor `erpCoreEventExecutor` | service/NotificationDeliveryListener.java; DEVIATIONS [08] |
| CHANGED | SEC | SOFT (in-core API) | `SecUserDirectoryApi.findContact` (active check = ACTIVE or PENDING_VERIFICATION, account e-mail) and `findCurrentUserId()` (inbox caller), through NOTIF's `RecipientDirectory` port | crossmodule/SecRecipientDirectory.java; DEVIATIONS [08], [14] |
| NEW | MDL | SOFT (in-core API) | `MdlLookupApi.readActiveValuesByKey` — the `NOTIF_CHANNEL` / `NOTIF_STATUS` values (seeded V8, V13) | service/NotificationLookupService.java:39; ADR-NOTIF-006 |
| NEW | FILE | SOFT (in-core API) | `FileDocumentLookupApi.isAvailable` — attachment validation (404 `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND`) | service/NotificationTemplateService.java:153-158 |
| NEW | audit | SOFT | `@Audited` on `NotificationTemplate` | DEVIATIONS [10] |
| NEW | report | SPI | `NotifLogSummaryReport` (`NOTIF_LOG_SUMMARY`) | DEVIATIONS [11] |
| REMOVED | Common Utils | USES | "Events (`NotificationEvent`)" — NOTIF listens to no foreign event; config / exceptions now come from `com.erp.common` | ADR-NOTIF-001 |

EXPOSED SURFACE — deltas
| Kind | Surface | Delta | Source |
|---|---|---|---|
| NEW | `com.erp.notif.channel.ChannelProvider` (`channel()`, `send(OutboundMessage)` → `DeliveryResult`) | public SPI; core providers EMAIL (only with a `JavaMailSender`) and IN_APP; any other channel falls back to a logging stand-in → `SKIPPED_NO_PROVIDER`; an application provider wins over a core one | channel/ChannelProvider.java; service/ChannelProviderRegistry.java:26-46; docs/steps/08-report.md; ADR-NOTIF-004 |
| NEW | `DeliveryResult.sent()` / `failed(error)` / `skippedNoProvider(reason)` / `rejected(reason)`; `DeliveryStatus.REJECTED` | `rejected` (1.1.0): permanent failure, never retried (omitted from docs/CONSUMING.md §7) | channel/DeliveryResult.java:12-27; CHANGELOG [1.1.0] |
| NEW | `OutboundMessage` (15 fields: log id, tenant, channel, recipient, template code, nameAr/En, subjectAr/En, bodyAr/En, moduleCode, referenceType/Id, variables) | `titleAr()` / `titleEn()` fall back to the template name; conventional variables `email`, `lang`, `actionLink`, `ctaLabelEn`, `ctaLabelAr`; `toString` hides the variables | channel/OutboundMessage.java:31-80 |
| NEW | `NotificationDispatchApi.dispatch(DispatchCommand)` | signature unchanged; asynchronous; caller's transaction; `isAuthenticated()` | crossmodule/NotificationDispatchApi.java:25 |
| NEW | `NotificationDispatchApi.dispatchIndependently(DispatchCommand)` | REQUIRES_NEW (`DispatchService.dispatchSystem`); the adapter resolves the recipient in the caller's transaction; used by SEC's password reset and customer verify / reset mails | crossmodule/NotificationDispatchApi.java:27-34; crossmodule/NotificationDispatchApiImpl.java:28-36; com/erp/sec/service/PasswordResetService.java:226; com/erp/sec/service/CustomerAccountService.java:309 |
| NEW | `DispatchCommand(recipientId, templateCode, channelHint, moduleCode, referenceId, referenceType, variables)` | the inbound read-model | crossmodule/DispatchCommand.java |
| NEW | `NotificationChannelAdminApi.setChannelEnabled(channelTypeId, enabled)` | flips `IS_ENABLED_FL` by code, keeps `CONFIG_JSON`; unknown → `NOTIF_CHANNEL_CONFIG_NOT_FOUND` | crossmodule/NotificationChannelAdminApi.java; NotificationChannelAdminApiImpl.java:21-34 |
| NEW | `NotificationLogQueryApi.findByRecipientModuleAndReference(recipientId, moduleCode, referenceType)` → `List<DispatchLogRecord(id, templateCode, notificationStatusId)>` | newest first | crossmodule/NotificationLogQueryApi.java; crossmodule/DispatchLogRecord.java; docs/CONSUMING.md §7 |
| NEW | `RecipientDirectory.emailOf(Long)` | 1.1.0, no default implementation (an implementation outside core must add it) | crossmodule/RecipientDirectory.java:23; CHANGELOG [1.1.0] |
| NEW | `NotifChannels` (`EMAIL`, `SMS`, `PUSH`, `IN_APP`), `TemplateText.render` | channel constants and the placeholder engine | channel/NotifChannels.java; channel/TemplateText.java |

PERMISSIONS — deltas
| Kind | Authority | Delta | Source |
|---|---|---|---|
| CHANGED | `PERM_NOTIF_TEMPLATES_*`, `PERM_NOTIF_CHANNELS_*`, `PERM_NOTIF_LOG_VIEW` | seeded by V7 and code-defined (`NotifPermissions`); held by `NOTIF_ADMIN` and `SYS_ADMIN` (super role) | permission/NotifPermissions.java; V7__sec_seed.sql:119-127, 173, 176 |
| NEW | `NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_NOTIF_REPORTS_VIEW` (screen `NOTIF_REPORTS`) | no explicit role grant seeded | DEVIATIONS [11] |
| NEW | `isAuthenticated()` | dispatch (ADR-NOTIF-005), inbox (own items only), lookups | service/DispatchService.java:72; service/NotificationInboxService.java:49, 65; service/NotificationLookupService.java:42 |

AUTO-DECISIONS — revisited
| Kind | AUTO-DECISION | Delta | Source |
|---|---|---|---|
| CHANGED | "Five channels all built and enabled (`is_enabled_fl=1`) from the start" | channel delivery is a provider SPI; only EMAIL (with a mail sender) and IN_APP have core providers and seeded configurations; SMS, WHATSAPP, PUSH, INTERNAL have no configuration row (→ `CHANNEL_DISABLED`) and no provider (→ `SKIPPED_NO_PROVIDER` once a row exists) | service/DispatchService.java:133-141; V9:19-20; V13 §3b; ADR-NOTIF-004 |
| CHANGED | "In-process Spring events (via Common Utils Events) — NOT RabbitMQ" | still no broker; the bus is the dedicated `com.erp.events` module and delivery is asynchronous after commit, at-least-once, with a claim lease and an opt-in requeue job | docs/steps/08-report.md; DEVIATIONS [15]; ADR-NOTIF-002 |
| CHANGED | "Sending module chooses channels via `channelHint` (single \| list \| ALL)" | list only (each element one row); `ALL` is not supported (REMOVED); duplicates are not removed | service/DispatchService.java:129-131; dto/DispatchRequest.java:38-40 |
| CHANGED | "Templates stored inline; `file_id` references File Service for optional ATTACHMENTS" | inline bodies hold; the attachment id is validated at write time but no provider sends the file | service/NotificationTemplateService.java:153-158; channel/OutboundMessage.java |
| REMOVED | "Push via Firebase Admin SDK; SMS/WhatsApp via adapter pattern with provider creds in `NotificationChannelConfig.config_json`" | no Firebase dependency; a vendor is an application `ChannelProvider` bean configured through the application's own properties; `CONFIG_JSON` is read by nothing | ADR-NOTIF-004 |
| CHANGED | "Notification does NOT validate JWT — trusts Security's filter" | holds; the consequence is that dispatch is gated by `isAuthenticated()` alone (no page permission can be seeded for a screenless operation) — recorded as an accepted risk | service/DispatchService.java:69-73; ADR-NOTIF-005 |

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

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no NOTIF behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Register | Delta | Source |
|---|---|---|---|
| CHANGED | EXPOSED SURFACE | `notif.dto.LookupOptionResponse` removed; API-NOTIF-006 returns `com.erp.common.lookup.LookupOptionResponse` (same JSON) | CHANGELOG [Unreleased] |
