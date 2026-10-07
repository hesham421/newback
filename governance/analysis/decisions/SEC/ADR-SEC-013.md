# ADR-SEC-013 — A service account is a User of principal type SERVICE, not a separate entity

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
CS-SEC-001 requires that a service account "can hold permissions like any other principal"
and is "visible … from SEC's existing administration screens." Two shapes were considered:
(A) a service account is the existing User entity, distinguished by a new PRINCIPAL_TYPE
lookup (HUMAN / SERVICE), reusing role assignment, grants, admin views and audit attribution
as-is, with only a new credential entity added; (B) a separate ServiceAccount entity with its
own role-assignment and grant path.

## Decision
**Option A.** Option B duplicates the assignment/grant path that already exists on User,
which conflicts with G1 — no duplication [domain-profile §5] — for no requirement the change
manifest actually asks for. User is already declared SHARED as the platform's canonical
identity source (project-registry → SHARED ENTITY DECLARATIONS); every module that already
treats a User row as the audit-field identity source keeps working unchanged for a
SERVICE-typed row.

## Consequences
- The only new entity this change set introduces is the service account's credential
  (ServiceAccountCredential); User itself is MODIFIED, not replaced.
- Every existing SEC screen and API that lists/edits users, roles and grants extends to
  service accounts without a parallel screen — P0.5/P1 add a principal-type filter/badge,
  not a new administration surface.
- A service account never has PENDING status (no self sign-up path) and is not reachable
  through the interactive LOCKED path (see ADR-SEC-015) — recorded as an AUTO-DECISION in
  module-registry-sec.md rather than reopened here.

## Traces
POL-SEC-012, POL-SEC-018
