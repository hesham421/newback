# API ID BINDING — SEC

> Written by this factory (P3.2 reconciliation, ADR-SEC-004), NOT published by the
> backend. It lived inside the consolidated api-docs file, where the first automated
> fetch would have erased it. It sits beside that file now: `gov.py fetch-inputs`
> regenerates the api-docs and never touches this one.

## API ID BINDING — added by P3.2 reconciliation (ADR-SEC-004)

The published generator emits no governance `API-*` id. This annex binds each `API-SEC-*`
of `registry-exec-be-sec.md` to the endpoint this document actually publishes, matched on
verb + path. No endpoint is invented and no id is re-numbered; five rows carry a shape
diff (GET + query params planned, POST `…/search` + `filters[]` published) recorded in
ADR-SEC-003 and marked ⚠ below.

| API id | Operation | Published verb · path | Bound |
|---|---|---|---|
| API-SEC-001 | login | POST `/api/v1/sec/auth/login` | ✓ |
| API-SEC-002 | submit sign-up | POST `/api/v1/sec/auth/signup` | ✓ |
| API-SEC-003 | request password reset | POST `/api/v1/sec/auth/password-reset/request` | ✓ |
| API-SEC-004 | complete password reset | POST `/api/v1/sec/auth/password-reset/complete` | ✓ |
| API-SEC-005 | search users | POST `/api/v1/sec/users/search` | ⚠ shape diff |
| API-SEC-006 | create user | POST `/api/v1/sec/users` | ✓ |
| API-SEC-007 | update user | PUT `/api/v1/sec/users/{id}` | ✓ |
| API-SEC-008 | assign roles to user | PUT `/api/v1/sec/users/{id}/roles` | ✓ |
| API-SEC-009 | deactivate user | DELETE `/api/v1/sec/users/{id}` | ✓ |
| API-SEC-010 | reactivate user | PATCH `/api/v1/sec/users/{id}` | ✓ |
| API-SEC-011 | approve / reject sign-up | PATCH `/api/v1/sec/signup-requests/{id}` | ✓ |
| API-SEC-012 | search roles | POST `/api/v1/sec/roles/search` | ⚠ shape diff |
| API-SEC-013 | create role | POST `/api/v1/sec/roles` | ✓ |
| API-SEC-014 | grant module to role | POST `/api/v1/sec/roles/{id}/modules` | ✓ |
| API-SEC-015 | revoke module grant | DELETE `/api/v1/sec/roles/{id}/modules/{moduleId}` | ✓ |
| API-SEC-016 | grant screen to role | POST `/api/v1/sec/roles/{id}/screens` | ✓ |
| API-SEC-017 | grant action to role | POST `/api/v1/sec/roles/{id}/actions` | ✓ |
| API-SEC-018 | register module | POST `/api/v1/sec/registry/modules` | ✓ |
| API-SEC-019 | register screen | POST `/api/v1/sec/registry/screens` | ✓ |
| API-SEC-020 | register action | POST `/api/v1/sec/registry/actions` | ✓ |
| API-SEC-021 | search registry | POST `/api/v1/sec/registry/search` | ⚠ shape diff |
| API-SEC-022 | dashboard summary | GET `/api/v1/sec/dashboard` | ✓ |
| API-SEC-023 | search audit log | POST `/api/v1/sec/audit-log/search` | ⚠ shape diff |
| API-SEC-024 | export audit log | GET `/api/v1/sec/audit-log/export` | ✓ |
| API-SEC-025 | list active sessions | POST `/api/v1/sec/sessions/search` | ⚠ shape diff |
| API-SEC-026 | terminate session | DELETE `/api/v1/sec/sessions/{id}` | ✓ |
| API-SEC-027 | effective menu | GET `/api/v1/sec/menu` | ✓ |

Coverage: 27 registered ids ↔ 27 published endpoints — no unbound id, no undocumented
endpoint. Operations the SRS names that this document publishes NO endpoint for are listed
in ADR-SEC-008; they are omitted from the frontend, never faked.

==============================================================================
