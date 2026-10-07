<!-- Source: PHASE:CORE -->

## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CANONICAL ARCHITECTURE (backend): controller/ service/ mapper/ domain/ repository/ entity/ dto/ exception/ config/
Domain behavior placement: separate classes in domain/ (dispatch fan-out, retry-with-backoff, provider abstraction, recipient-active check are non-trivial — e.g. DispatchDomainService, RetryPolicy, ChannelProvider strategy).
PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity on all 3 tables (audit via AuditEntityListener). ✗ orgUnitId never in any DTO.
  Error signaling  : LocalizedException — NotFoundException BANNED.
  Error catalog    : every ERR-ID registered 4× (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).
  Search contract  : SearchRequest extends BaseSearchContractRequest; ALLOWED_SORT_FIELDS per search.
  Deactivation     : NotificationTemplate isActiveFl=false. NotificationChannelConfig uses IS_ENABLED_FL only (enable/disable) — no IS_ACTIVE_FL.
  Auth             : delegate to Security filter — NOTIF never self-verifies JWT (RULE-NOTIF-005).
  Events           : consumes CU in-process ApplicationEvents (NotificationEvent). No external broker (RabbitMQ/Kafka not used).
  Providers        : ChannelProvider is an interface; concrete provider resolved from NOTIF_CHANNEL_CONFIG.configJson (provider-agnostic — OQ-NOTIF-001, DRV-004).
TYPE MAPPING (POSTGRESQL_16): BIGINT→Long · VARCHAR(N)→String · TEXT→String(+@Lob) · SMALLINT(_FL)→Boolean · TIMESTAMP→LocalDateTime.
MODULE-SPECIFIC NOTES:
  - Dispatch fans out ONE NOTIF_LOG row per requested channel (RULE-NOTIF-001); the service is business-neutral (no sender routing logic).
  - Disabled channel → row with notificationStatusId=CHANNEL_DISABLED, no retry (RULE-NOTIF-003).
  - Failure → retry ≤5 (2s, ×1.5 backoff) then FAILED (RULE-NOTIF-002).
  - Inactive recipient → skip dispatch (RULE-NOTIF-007); historical logs retained.
  - LOV values runtime codes (no ENUM, no lookup table). No Business Code. No Workflow Engine (RULE-13 = OFF).
─────────────────────────────────────────────────────────────────
