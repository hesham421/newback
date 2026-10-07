# ADR-SEC-010 — SCR-SEC-004's form differs from SRS B3's input list, because the published write DTOs do

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
SRS `SCR-REQ-SEC-004` B3 lists the Users screen's inputs as "username, email, fullNameAr,
fullNameEn, statusCode (ENT-SEC-001); roles multi-select (ENT-SEC-003)". The published write
DTOs do not match that list in two places:

| Field | SRS B3 | Published API | Consequence |
|---|---|---|---|
| `password` | **not listed** as an input | `UserCreateRequest` declares it **required** (maxLength 200, "Raw password, hashed server-side") | a create form without it cannot call API-SEC-006 at all |
| `statusCode` | listed as an input | accepted by **no** write DTO — neither `UserCreateRequest` nor `UserUpdateRequest` carries it; it is changed only by API-SEC-009 (deactivate), API-SEC-010 (reactivate) and API-SEC-011 (sign-up approval), each returning `UserStatusResponse` | a status field the user could type would post a value no endpoint reads |

The engine's B3 rule is "every field/permission on a screen exists in the SRS — extra:
removed; missing: added". Applied literally to the B3 list alone it would delete the password
field and add a writable status field, and both changes would break the screen against the real
API. Applied to the SRS as a whole it resolves cleanly, because both fields are in SRS A3.

## Decision
The form binds the published DTOs, and each divergence is resolved against SRS A3 rather than
against B3's prose:

1. **`password` is an input on create only**, write-only, never shown and never returned. Its
   SRS basis is `ENT-SEC-001.passwordHash` — "never exposed to any client [POL-SEC-004],
   write-only". The client sends the raw value once and holds nothing; the hash is the server's.
   It is not an input on edit: `UserUpdateRequest` does not carry it, and no password-change
   endpoint is published for an administrator (a user changes their own through SCR-SEC-003).
2. **`statusCode` is read-only on the form**, rendered from `UserResponse` and changed only by
   the activate / deactivate affordances and by sign-up approval — exactly the transitions
   SRS A7 draws (PENDING → ACTIVE → DISABLED → ACTIVE), each with its own endpoint and its own
   REQ (REQ-SEC-004, REQ-SEC-011, REQ-SEC-031).

## Consequences
- The screen can actually create a user, and cannot post a status value nothing reads.
- SRS A7's lifecycle stays the only way a status changes, which is stronger than B3's prose
  would have been: a typed status field could express a transition A7 does not allow, and the
  affordances cannot.
- The flow diagram's B3 reconciliation records both divergences by name rather than reporting
  "extra removed: none, missing added: none", which is what this ADR corrects.
- Non-breaking: no `REQ-*` changes, and no other screen diverges from its B3 list.

## Traces
REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031 · AC-SEC-004, AC-SEC-009, AC-SEC-011,
AC-SEC-031 · POL-SEC-004 · ENT-SEC-001 · API-SEC-006, API-SEC-007, API-SEC-009, API-SEC-010,
API-SEC-011 · SCR-SEC-004
