# ADR-SEC-033 — Each service-account credential records its last successful use
Status      : ACCEPTED (non-breaking)
Stage       : P1        Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : ENT-SEC-014, REQ-SEC-056, REQ-SEC-057, US-SEC-016, POL-SEC-021

## Context
POL-SEC-021 and ADR-SEC-015 make rotation administrator-driven. The administrator issues a second
credential, switches the daemon to it, then revokes the first. Nothing upstream tells the administrator when
the switch has actually happened. If the first credential is revoked while the daemon still uses it, the
integration breaks, which is the failure CS-SEC-001 exists to prevent. The audit log records every
successful authentication (POL-SEC-019). The SERVICE_AUTH_SUCCESS entry names the account, not the
credential.

## Decision
Every credential carries a `lastUsedAt` time. It is set on each successful machine authentication with that
credential (REQ-SEC-057) and shown in the credentials list (REQ-SEC-056). Google Cloud service-account keys
and GitHub tokens surface the same "last used" value for the same reason: safe rotation.

## Consequences
- ENT-SEC-014 declares `lastUsedAt`. P2 binds it to a column.
- The value is informational. No rule reads it to allow or refuse anything.
- Override: drop the field and REQ-SEC-057, and let administrators rely on the audit log alone.
