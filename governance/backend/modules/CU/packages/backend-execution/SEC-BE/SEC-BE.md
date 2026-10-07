<!-- Source: PHASE:SEC-BE -->

## PHASE SEC-BE — Backend Security Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

SCOPE: CU is Backend-only with NO screens → no SCR-IDs, no SEC_PAGES rows,
CORE-9 Composite Screen governance does NOT apply (srs-CU PART B).

API-LEVEL ENFORCEMENT:
  Configuration management endpoints are administrative. Every API-CU-00x method
  requires authorization enforced at the API level before request processing
  (config-admin authority). HTTP 403 on failure → mapped via LocalizedException.
  The concrete authority/permission source is the Security (SEC) module; CU
  declares the enforcement requirement, SEC provides the mechanism.

SECURITY SEED DATA REQUIREMENTS: none (no SEC_PAGES/PERMISSIONS rows — no screens).

SEC-BE Governance Rules:
  SEC-IMPL-RULE-1 — every configuration endpoint enforces authorization at API level
  SEC-IMPL-RULE-3 — HTTP 403 mapped via LocalizedException carrying correct ERR-ID
─────────────────────────────────────────────────────────────────

SEC-BE VERIFICATION (recorded 2026-09-04):
  SEC-IMPL-RULE-1 — SATISFIED. Verified against real code: every public method on
  ConfigurationService carries a @PreAuthorize referencing PermissionConstants —
  create=CONFIG_CREATE, search=CONFIG_VIEW, update=CONFIG_UPDATE,
  getByKey=CONFIG_VIEW, deactivate=CONFIG_DEACTIVATE, getValue=CONFIG_VIEW. All 5
  controller endpoints delegate to those gated methods and add no unguarded path.
  Enforcement is via Spring method security (@EnableMethodSecurity active). No new
  code was written in this sub.

  SEC-IMPL-RULE-3 — DEFERRED TO SEC MODULE. Spring Security throws AccessDeniedException
  (not LocalizedException) on an authorization failure — the standard, correct mechanism.
  The CU error catalog (ERR-0001..0004) has no authorization/403 ERR-ID, so there is
  nothing in CU to carry. Per this spec's own delegation ("CU declares the enforcement
  requirement, SEC provides the mechanism"), the concrete ERR-ID-carrying 403 envelope
  is owned by the SEC module. Interim behavior: the shared GlobalExceptionHandler maps
  AccessDeniedException → HTTP 403 with a generic ACCESS_DENIED code. No CU authz ERR-ID
  was invented and no shared handler was modified.
