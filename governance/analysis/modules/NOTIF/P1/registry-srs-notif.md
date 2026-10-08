# REGISTRY EXTRACT — registry-srs-NOTIF
══════════════════════════════════════════════════════════════════
Module          : Notification Service (NOTIF)
Source artifact : srs-NOTIF.md (v1.2)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Notification Service (خدمة الإشعارات)
Module Prefix : NOTIF
OQ count : 1 (OQ-NOTIF-001 — RESOLVED)

## ENTITIES (PART A — A3)
| ENTITY-ID | Entity Name | Type |
|---|---|---|
| ENTITY-NOTIF-001 | NotificationLog | PRIVATE |
| ENTITY-NOTIF-002 | NotificationTemplate | PRIVATE |
| ENTITY-NOTIF-003 | NotificationChannelConfig | PRIVATE |

## RULES (PART A — A4)
| RULE-ID | Short Title | Test-Hint |
|---|---|---|
| RULE-NOTIF-001 | Fan out one log per channel | — |
| RULE-NOTIF-002 | Retry ≤5 then FAILED | — |
| RULE-NOTIF-003 | Disabled channel, no retry | — |
| RULE-NOTIF-004 | Bilingual templates required | — |
| RULE-NOTIF-005 | Delegate auth to Security filter | — |
| RULE-NOTIF-006 | Unique template/channel codes | — |
| RULE-NOTIF-007 | No dispatch to inactive recipient | — |

## LOVs (PART A — A5)
| LOV-ID | LOV Name |
|---|---|
| LOV-NOTIF-001 | NotificationChannel (NOTIF_CHANNEL) |
| LOV-NOTIF-002 | NotificationStatus (NOTIF_STATUS) |

## LIFECYCLE STATES (PART A — A6)
NotificationLog: PENDING → SENT | PENDING → FAILED (after retries) | PENDING → CHANNEL_DISABLED

## DEPENDENCIES (PART A — A7)
| Type | Target ENTITY-ID | Target Module | XM candidate |
|---|---|---|---|
| SOFT-READ | ENTITY-SEC-001 (UserAccount) | SEC | Yes |
| SOFT/service | FileDocument (via FileService, file_id) | FILE | Yes |
Note: CU (Events/config/exceptions) is USES (library) — not a governed dependency type.

## SCREENS (PART B)
| SCR-ID | page_code | Screen Name | Pattern |
|---|---|---|---|
| SCR-NOTIF-001 | NOTIF_TEMPLATES | Notification Templates | PATTERN-2 (SIDE_DRAWER) |
| SCR-NOTIF-002 | NOTIF_CHANNELS | Channel Configuration | PATTERN-2 (SIDE_DRAWER) |
| SCR-NOTIF-003 | NOTIF_LOG | Notification Log (read-only) | PATTERN-2 (SIDE_DRAWER) |

## APIs (PART B — B5)
| API-ID | Method | Endpoint | Owning SCR-ID |
|---|---|---|---|
| API-NOTIF-001 | POST | /api/v1/notifications/dispatch | — (event/service endpoint) |
| API-NOTIF-002 | GET | /api/v1/notifications/logs | SCR-NOTIF-003 |
| API-NOTIF-003 | GET | /api/v1/notifications/logs/{id} | SCR-NOTIF-003 |
| API-NOTIF-004 | POST/GET/PUT/DELETE | /api/v1/notifications/templates | SCR-NOTIF-001 |
| API-NOTIF-005 | POST/GET/PUT/DELETE | /api/v1/notifications/channels | SCR-NOTIF-002 |
| API-NOTIF-006 | GET | /api/v1/notifications/lookups/{lookupKey} | (cross-screen) |

## PERMISSIONS (Permissions Summary)
| PERM Name | Linked SCR-ID(s) |
|---|---|
| PERM_NOTIF_TEMPLATES_{VIEW,CREATE,UPDATE,DELETE} | SCR-NOTIF-001 |
| PERM_NOTIF_CHANNELS_{VIEW,CREATE,UPDATE,DELETE} | SCR-NOTIF-002 |
| PERM_NOTIF_LOG_VIEW (VIEW only) | SCR-NOTIF-003 |
All granted to NOTIF_ADMIN per srs-NOTIF Permissions Summary.

## OQ LOG STATUS
| OQ-ID | Status | One-line topic | Escalation |
|---|---|---|---|
| OQ-NOTIF-001 | RESOLVED | Actual provider per channel (SMS/WhatsApp/Push) | P3-TECH |

---
*End of registry-srs-NOTIF.md*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 02, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text and sources in `srs.md` → "Implementation Addendum — erp-core 1.2.0". The RULE ids
below are the ones minted there (RULE-NOTIF-008..022); no other id is assigned.

### ENTITIES — delta
| Kind | Entity | Type | Delta |
|---|---|---|---|
| NEW | ENTITY-NOTIF-004 NotificationInboxItem (`NOTIF_INBOX`) | PRIVATE | tenant-scoped in-app inbox item; no link to `NOTIF_LOG` |
| CHANGED | ENTITY-NOTIF-001 NotificationLog | PRIVATE | tenant-scoped + version; delivery-queue fields `attempts`, `nextAttemptAt` (lease), `lastError`, `variablesJson` |
| CHANGED | ENTITY-NOTIF-002 NotificationTemplate | PRIVATE | tenant-scoped + version; code unique per tenant; `@Audited`; attachment validated at write, never sent |
| CHANGED | ENTITY-NOTIF-003 NotificationChannelConfig | PRIVATE | tenant-scoped + version; channel unique per tenant; `configJson` read by no provider; only EMAIL and IN_APP rows seeded |

### RULES — delta
| Kind | RULE-ID | Short Title | Test-Hint |
|---|---|---|---|
| CHANGED | RULE-NOTIF-002 | Retry: 5 attempts, 2 s ×2 (cap 32 s), `erp.core.notif.retry.*` | FAILED after waits 2+4+8+16 s; `NotificationFailedEvent` |
| CHANGED | RULE-NOTIF-003 | No config row ≡ disabled → CHANNEL_DISABLED; SKIPPED_NO_PROVIDER only for an enabled channel without provider | dispatch to SMS without a row → CHANNEL_DISABLED |
| CHANGED | RULE-NOTIF-004 | Attachment validated on write (404), never sent | unknown `attachmentFileId` → 404 |
| CHANGED | RULE-NOTIF-007 | Eligible = ACTIVE or PENDING_VERIFICATION; either realm; unknown = inactive | PENDING_VERIFICATION customer receives the mail |
| NEW | RULE-NOTIF-008 | Asynchronous at-least-once delivery after commit (prepare / send / recordOutcome) | dispatch returns before any send |
| NEW | RULE-NOTIF-009 | Provider resolution: application bean > core > logging stand-in | channel without provider → SKIPPED_NO_PROVIDER |
| NEW | RULE-NOTIF-010 | Template rendering `{name}`; unknown placeholder kept; title = subject else name | `{missing}` stays verbatim |
| NEW | RULE-NOTIF-011 | EMAIL: one language by `lang`; address = `variables.email` else account e-mail; none → FAILED (REJECTED); HTML-escaped; CTA from `actionLink` | no address → FAILED after 1 attempt |
| NEW | RULE-NOTIF-012 | IN_APP: bilingual inbox row, title ≤ 300; own items only | other user's item → 404 INBOX_ITEM_NOT_FOUND |
| NEW | RULE-NOTIF-013 | Template / channel codes trimmed + upper-cased (persist and dispatch) | hint `email` → EMAIL row |
| NEW | RULE-NOTIF-014 | `channelHint` = list only; no ALL; duplicates not removed | `[EMAIL, EMAIL]` → two rows |
| NEW | RULE-NOTIF-015 | Exhaustion: last attempt → FAILED at once; used-up row failed without a try; `@Recover` fallback | — |
| NEW | RULE-NOTIF-016 | Claim lease in `NEXT_ATTEMPT_AT`; ownership by `VERSION`; optimistic-lock loser skipped | — |
| NEW | RULE-NOTIF-017 | Node-local tracker de-duplication; executor rejection leaves the row QUEUED | — |
| NEW | RULE-NOTIF-018 | Requeue job: opt-in, `interval-ms`, `stale-after-minutes`, cross-tenant | — |
| NEW | RULE-NOTIF-019 | Recipient resolution; unknown = inactive; `[]` and no row; caller-side for REQUIRES_NEW | unknown recipient → 200 `[]` |
| NEW | RULE-NOTIF-020 | `VARIABLES_JSON` only while QUEUED; never exposed | — |
| NEW | RULE-NOTIF-021 | Tenant confinement; provisioning copies configs (no CONFIG_JSON) and templates (no attachment) | — |
| NEW | RULE-NOTIF-022 | No dispatch rate limit; page size ≤ 200 | — |
| NEW (decision) | — | Dispatch gated by `isAuthenticated()` + `variables.email` override — ADR-NOTIF-005 | — |
| REMOVED | — | `channelHint = ALL`; the synchronous send path and `NO_MAIL_SENDER` | — |

### LOVs — delta
| Kind | LOV | Delta |
|---|---|---|
| CHANGED | LOV-NOTIF-001 | + `IN_APP`; hosted in MDL (`NOTIF_CHANNEL`, owner NOTIF) |
| CHANGED | LOV-NOTIF-002 | + `QUEUED`, `SKIPPED_NO_PROVIDER`; `PENDING` never stored; hosted in MDL (`NOTIF_STATUS`) |

### LIFECYCLE STATES — delta
| Kind | Transition |
|---|---|
| CHANGED | PENDING (transient, never persisted) → QUEUED \| CHANNEL_DISABLED |
| NEW | QUEUED → SENT \| FAILED \| SKIPPED_NO_PROVIDER |
| REMOVED | PENDING → SENT / FAILED directly (no synchronous send) |

### DEPENDENCIES — delta
| Kind | Type | Target | Module |
|---|---|---|---|
| NEW | HARD-FK + SPI | `CORE_TENANT`; `TenantProvisioningContributor` (order 20) | tenant |
| CHANGED | USES | `com.erp.events` bus (replaces "CU Events") | events |
| CHANGED | SOFT (in-core API) | `SecUserDirectoryApi` via `RecipientDirectory` (`isActive`, `emailOf`, `currentRecipientId`); physical target `SEC_USER` | SEC |
| NEW | SOFT (in-core API) | `MdlLookupApi` (`NOTIF_CHANNEL` / `NOTIF_STATUS` values) | MDL |
| NEW | SOFT (in-core API) | `FileDocumentLookupApi.isAvailable` (attachment validation; still no FK) | FILE |
| NEW | SOFT / SPI | `@Audited` (audit); `ReportProvider` (report) | audit, report |
| REMOVED | USES | CU Events `NotificationEvent` | CU |

### SCREENS — delta
| Kind | SCR-ID | page_code | Screen Name | Note |
|---|---|---|---|---|
| NEW | — | NOTIF_REPORTS | Notification reports | report gateway screen (`PERM_NOTIF_REPORTS_VIEW`) |
| NEW | — | (none) | In-app inbox | no screen row; `isAuthenticated()` / `ROLE_CUSTOMER` endpoints |

### APIs — delta
| Kind | API-ID | Method | Endpoint |
|---|---|---|---|
| CHANGED | API-NOTIF-001 | POST | /api/v1/notifications/dispatch (asynchronous; 200; rows QUEUED / CHANNEL_DISABLED; `isAuthenticated()`) |
| CHANGED | API-NOTIF-002 | POST | /api/v1/notifications/logs/search (was GET /logs; filter envelope, whitelists) |
| CHANGED | API-NOTIF-003 | GET | /api/v1/notifications/logs/{id} (+ `attempts`, `nextAttemptAt`, `lastError`) |
| CHANGED | API-NOTIF-004 | POST, POST /search, GET /{id}, PUT /{id}, DELETE /{id} | /api/v1/notifications/templates (list → POST /search; PUT full replace; DELETE = 204 soft deactivate) |
| CHANGED | API-NOTIF-005 | POST, POST /search, GET /{id}, PUT /{id}, DELETE /{id} | /api/v1/notifications/channels (list → POST /search; PUT full replace; DELETE = 204 soft disable) |
| CHANGED | API-NOTIF-006 | GET | /api/v1/notifications/lookups/{lookupKey} (values from MDL; `isAuthenticated()`) |
| NEW | — | GET, PATCH | /api/v1/notif/inbox, /api/v1/notif/inbox/{id}/read |
| NEW | — | GET, PATCH | /api/v1/customers/me/inbox, /api/v1/customers/me/inbox/{id}/read |
| NEW (report) | — | POST | /api/v1/report/NOTIF_LOG_SUMMARY/run, /export |
| REMOVED | — | — | `NotificationEvent` listener (in-process); replaced by `NotificationDispatchApi.dispatch` / `dispatchIndependently` |

### PERMISSIONS — delta
| Kind | PERM Name | Note |
|---|---|---|
| CHANGED | `PERM_NOTIF_TEMPLATES_*`, `PERM_NOTIF_CHANNELS_*`, `PERM_NOTIF_LOG_VIEW` | seeded by V7 and code-defined (`NotifPermissions`); granted to NOTIF_ADMIN and SYS_ADMIN (super role) |
| NEW | `NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_NOTIF_REPORTS_VIEW` | report; no explicit role grant seeded (SYS_ADMIN holds it through the super role) |
| NEW | `isAuthenticated()` | dispatch, inbox, lookups |
| NEW | `ROLE_CUSTOMER` | customer inbox paths |

### ERROR CODES — delta
| Kind | Code | HTTP | Trigger |
|---|---|---|---|
| NEW | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | 400 | blank code / names / bodies (Domain; shadowed over HTTP by `VALIDATION_ERROR`) |
| NEW | `NOTIF_TEMPLATE_CODE_DUPLICATE` | 409 | duplicate template code in the tenant |
| NEW | `NOTIF_CHANNEL_CONFIG_DUPLICATE` | 409 | duplicate channel code in the tenant |
| NEW | `NOTIF_LOG_INVALID_TRANSITION` | 422 | illegal status transition (internal) |
| NEW | `NOTIF_CHANNEL_TYPE_REQUIRED` | 400 | blank channel code (Domain; shadowed over HTTP) |
| NEW | `NOTIF_TEMPLATE_INACTIVE` | 422 | dispatch against a deactivated template (no analysed rule) |
| NEW | `NOTIF_TEMPLATE_NOT_FOUND` | 404 | template id or dispatch `templateCode` unknown |
| NEW | `NOTIF_CHANNEL_CONFIG_NOT_FOUND` | 404 | channel-config id unknown; `NotificationChannelAdminApi` unknown code |
| NEW | `NOTIF_LOG_NOT_FOUND` | 404 | log id unknown |
| NEW | `NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND` | 404 | `attachmentFileId` not an available FILE document |
| NEW | `NOTIF_LOOKUP_KEY_UNKNOWN` | 404 | lookup key not NOTIF_CHANNEL / NOTIF_STATUS, or unseeded |
| NEW | `NOTIF_CHANNEL_UNAVAILABLE` | 403 | inbox used by a non-user principal; stored reason of SKIPPED_NO_PROVIDER |
| NEW | `INBOX_ITEM_NOT_FOUND` | 404 | inbox item unknown or not the caller's |
| NEW (SEC) | `REALM_MISMATCH` | 403 | token of the other realm on an inbox path |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs.md` → "Implementation Addendum — erp-core 1.3.0".

### RULES — delta
| Rule | Delta |
|---|---|
| RULE-NOTIF-023 | NEW: `UserPasswordChangedEvent` → `STAFF_PASSWORD_CHANGED` e-mail to the user (async, after commit, failures logged) |

### DEPENDENCIES — delta
| XM-ID | Type | Target | Module |
|---|---|---|---|
| XM-NOTIF-003 | EVENT-CONSUME | `UserPasswordChangedEvent` | events (SEC publishes) |

### ENTITIES / APIs / PERMISSIONS / ERROR CODES — delta
None. Template rows: + `STAFF_PASSWORD_CHANGED` (seed, V17).

Last sequence per atom (highest ever issued): RULE: 023 · XM: 003 · API: 012 · US: 008.

Package B (tenant-maturity plan §4 B.4) — registry delta; full text in `srs.md` 1.3.0 §4.
| Kind | Surface | Delta | Consumer |
|---|---|---|---|
| CHANGED | `NotificationLogQueryApi` (exposed) | + `countDispatchedSince(Instant)` (current tenant's `NOTIF_LOG` rows created since) | TENANT (usage) |
No rule, XM, entity, API, permission or error-code delta. Last sequence per atom unchanged (RULE: 023 · XM: 003 ·
API: 012 · US: 008).

Package C12 (tenant-maturity plan §5 C.1) — registry deltas; full text in `srs.md` 1.3.0 §5.
| Kind | Item | Delta |
|---|---|---|
| NEW | RULE-NOTIF-024 | the claim and the requeue job skip `QUEUED` rows of a tenant that is not ACTIVE (status unchanged); `TenantActivatedEvent` re-dispatches them |
| NEW | XM-NOTIF-004 (CROSSMODULE-READ) | `TenantLookupApi.isActive(Long)` — TENANT |
| NEW | XM-NOTIF-005 (EVENT-CONSUME) | `TenantActivatedEvent` — events (TENANT publishes) |
No entity, API, permission, status or error-code delta. Last sequence per atom: RULE: 024 · XM: 005 · API: 012 · US: 008.


Package C5 (tenant-maturity plan §5 C.5) — registry delta; full text in `srs.md` 1.3.0 §6.
| Kind | Item | Delta |
|---|---|---|
| NEW | `com.erp.notif.tenant.NotifTenantExportContributor` | implements TENANT XM-TENANT-004: `NOTIF_TEMPLATE`, `NOTIF_CHANNEL_CONFIG` (without `CONFIG_JSON`), `NOTIF_LOG` (without `VARIABLES_JSON`), `NOTIF_INBOX` of the exported tenant |
No id minted; entity, API, permission, status and error-code delta: none. Last sequence per atom unchanged.

### Refactors without behaviour change — shared helpers moved to `com.erp.common`
Change         : shared helpers moved to `com.erp.common` (`docs/CHANGELOG.md` [Unreleased]); no NOTIF behaviour change
Statement      : The sections above are unchanged; this block records the refactor deltas already on main. No id minted; endpoints, error codes, permissions, entities and migrations unchanged.

| Kind | Register | Item | Delta |
|---|---|---|---|
| CHANGED | APIs | API-NOTIF-006 response type | `com.erp.common.lookup.LookupOptionResponse` replaces `notif.dto.LookupOptionResponse` (same JSON) |
| CHANGED | internals | Domain classes | `DomainRules` / `StatusTransitions` helpers; no behaviour change |
