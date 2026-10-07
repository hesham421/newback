# ADR-SEC-017 — SEC's audit log covers a service account's own security events; business actions are attributed via the owning module's audit fields

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
CS-SEC-001 requires that a service account's "activity shows up in the audit log like any
other principal's." Two readings: (A) SEC's audit log records the service account's
authentications, credential lifecycle and grant changes; its business actions (e.g. creating
a journal entry in FIN) are attributed through the platform-standard audit fields in the
module that owns that action; (B) SEC logs every API call the service account makes,
including business calls to other modules.

## Decision
**Option A.** SEC v1 already scoped AuditLogEntry to SEC's own security events and kept it
PRIVATE (module-registry-sec.md v1 AUTO-DECISIONS) — not a platform-wide event feed. Option B
would make SEC a second, competing audit trail for every module's business actions, duplicating
the four audit fields every table already carries [KB:erp-domain-standards §6].

## Consequences
- POL-SEC-019 lists exactly what SEC's audit log records for a service account:
  authentication attempts, credential issuance/revocation, and role/grant changes.
- FIN's own createdBy/updatedBy fields on JournalEntry (or later, its own audit trail) are the
  record of what the service account actually did in FIN — SEC does not duplicate it.
- business-policies-sec.md lists "SEC-side logging of every business call" under SCOPE
  EXCEPTIONS, reopened only by an explicit platform-wide audit decision.

## Traces
POL-SEC-019
