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
Steps          : 05, 06, 08, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Registry deltas only; full text in `srs.md` → "Implementation Addendum — erp-core 1.2.0". No new ids are
assigned here.

### ENTITIES — delta
| Entity | Type | Delta |
|---|---|---|
| NotificationInboxItem (`NOTIF_INBOX`) | PRIVATE | NEW (tenant-scoped) |
| ENTITY-NOTIF-001..003 | PRIVATE | CHANGED: tenant-scoped + version; ENTITY-NOTIF-001 gains delivery-queue fields |

### RULES — delta
| Rule | Delta |
|---|---|
| RULE-NOTIF-002 | CHANGED: 5 attempts, 2 s ×2 (cap 32 s), async worker |
| RULE-NOTIF-007 | CHANGED: eligible = ACTIVE or PENDING_VERIFICATION; either realm |
| (new) | async after-commit delivery; claim/lease + bounded attempts (1.2.0); provider resolution; EMAIL address fallback + REJECTED (1.1.0); variables cleared at final status; requeue job; inbox ownership; tenant confinement |

### LOVs — delta
| LOV | Delta |
|---|---|
| LOV-NOTIF-001 | + `IN_APP` |
| LOV-NOTIF-002 | + `QUEUED`, `SKIPPED_NO_PROVIDER` |

### LIFECYCLE STATES — implemented
NotificationLog: PENDING (transient) → QUEUED | CHANNEL_DISABLED; QUEUED → SENT | FAILED | SKIPPED_NO_PROVIDER

### DEPENDENCIES — delta
| Type | Target | Module |
|---|---|---|
| HARD-FK + SPI | `CORE_TENANT`; `TenantProvisioningContributor` | tenant |
| USES | `com.erp.events` bus (replaces CU Events) | events |
| SOFT (in-core API) | `SecUserDirectoryApi` via `RecipientDirectory` (+ `emailOf`, `currentRecipientId`) | SEC |

### APIs — delta
| Kind | Method | Endpoint |
|---|---|---|
| NEW | GET | /api/v1/notif/inbox |
| NEW | PATCH | /api/v1/notif/inbox/{id}/read |
| NEW | GET | /api/v1/customers/me/inbox |
| NEW | PATCH | /api/v1/customers/me/inbox/{id}/read |
| NEW (report) | POST | /api/v1/report/NOTIF_LOG_SUMMARY/run, /export |
| CHANGED | POST | /api/v1/notifications/dispatch (async; rows QUEUED) |

### PERMISSIONS — delta
| PERM Name | Note |
|---|---|
| `NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_NOTIF_REPORTS_VIEW` | NEW (report contributor) |
| analysed `PERM_NOTIF_*` | unchanged, now code-defined (`NotifPermissions`) |

### ERROR CODES — delta
NEW: `NOTIF_CHANNEL_UNAVAILABLE` (403), `INBOX_ITEM_NOT_FOUND` (404). Source: docs/api-docs/notif/index.md.

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
