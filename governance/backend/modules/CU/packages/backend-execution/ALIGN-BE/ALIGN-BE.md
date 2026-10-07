<!-- Source: PHASE:ALIGN-BE -->

## PHASE ALIGN-BE — Backend Internal Self-Consistency Gate (auto-run)
─────────────────────────────────────────────────────────────────

## ALIGN-BE GATE — CU — PLAN-ID: PLAN-CU-001
TRACEABILITY CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
All FIELD-IDs used in phases appear in Plan Index            │ ✓
All API-IDs used in phases appear in Plan Index              │ ✓
All RULE-IDs used in phases appear in Plan Index             │ ✓
All ERR-IDs used in Error Catalog appear correctly           │ ✓
All QR-IDs in QRC appear in Plan Index QRC Summary           │ ✓
Derivation Log complete — no undocumented inferences         │ ✓
DB Structural Alignment confirms field coverage              │ ✓
BUSINESS CODE CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
Business Code excluded from POST/PUT bodies                  │ ✓ (N/A — no BC)
Business Code always present in GET/response DTOs            │ ✓ (N/A — no BC)
LOCALIZATION CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
All RULE-IDs have Message-AR defined                         │ ✓
All API error responses: messageAr + messageEn               │ ✓
SECURITY CHECKS                                              │ Status
─────────────────────────────────────────────────────────────┼────────
Every API-ID has authorization declared                      │ ✓
SCR-ID / SEC-BE coverage                                     │ ✓ (N/A — no screens)
QUERY REFERENCE CATALOG CHECKS                               │ Status
─────────────────────────────────────────────────────────────┼────────
Every API-ID with DB op has QR-ID in QRC                     │ ✓
Every QR-ID has agent-reference warning label                 │ ✓
No QR entry references ENUM for LOV fields                    │ ✓ (no LOV)
No QR entry joins to lookups table                            │ ✓
Every QR-ID states exact sequence name (SAVE)                │ ✓ SEQ_CU_APP_CONFIGURATION
TEST-BE COVERAGE CHECKS                                       │ Status
─────────────────────────────────────────────────────────────┼────────
TC Coverage Matrix Summary present in SECTION D              │ ✓
No GAP ✗ without DEFERRED                                     │ ✓
ARTIFACT BINDING CHECKS (Section 2A compliance)              │ Status
─────────────────────────────────────────────────────────────┼────────
No placeholder [TABLE_NAME]/[LOOKUP_CODE]/[SEQ_NAME]         │ ✓
No RULE block shows "see SRS" — all text inline              │ ✓
Every column name traces to a DBF-ID                          │ ✓
Every Message-AR is exact text                               │ ✓
DB Alignment Manifest: 5 columns only (CONTRACT-1)           │ ✓
PLAN COMPLETENESS CHECKS                                      │ Status
─────────────────────────────────────────────────────────────┼────────
Canonical architecture declared in PHASE CORE                │ ✓
Domain behavior placement declared                           │ ✓
No orgUnitId in any DTO                                       │ ✓
No audit fields in CreateRequest/UpdateRequest               │ ✓
Error signaling strategy declared (LocalizedException)       │ ✓
All ERR-IDs have 4-registration points declared              │ ✓
All search operations declare ALLOWED_SORT_FIELDS            │ ✓
Empty search → HTTP 200 declared                             │ ✓
Pre-deactivation existence check declared                    │ ✓
═══════════════════════════════════════════════════════════════════
ALIGN-BE GATE RESULT: PASSED ✓
Auto-correction applied: None
═══════════════════════════════════════════════════════════════════

Table 3 — XM Dependency Gate:
XM-ID │ Type │ Status │ Blocks │ Workaround
(none)
─────────────────────────────────────────────────────────────────

## ALIGN-BE — Verified against real built code (2026-09-04)
Gate re-confirmed against the implemented `com.erp.cu` code (not re-derived from
the plan). All checkable-against-code rows hold:
- Traceability / DB: AppConfiguration = 5 columns (ID, CONFIG_KEY, CONFIG_VALUE,
  NOTES, IS_ACTIVE_FL), sequence SEQ_CU_APP_CONFIGURATION, audit inherited from
  AuditableEntity (not declared locally). ✓
- DTO hygiene: CreateRequest & UpdateRequest exclude id + audit fields;
  UpdateRequest also excludes configKey (RULE-CU-003 immutability); no orgUnitId
  in any DTO; Response carries audit fields (createdAt/By, updatedAt/By). ✓
- Security: every ConfigurationService method is @PreAuthorize-gated
  (CONFIG_CREATE/VIEW/UPDATE/DEACTIVATE). ✓
- Localization: all four CU codes (APP_CONFIGURATION_KEY_DUPLICATE,
  _FIELDS_REQUIRED, _KEY_IMMUTABLE, _NOT_FOUND) exist as CuErrorCodes constants
  and in BOTH messages.properties (EN) and messages_ar.properties (AR). ✓
- Search/deactivate behavior: empty search returns a normal Page (HTTP 200,
  never 404); deactivate does a find-first existence check then soft-deactivate;
  all errors raised via LocalizedException. Controller exposes 5 endpoints with
  search as POST /search. ✓
- Final compile: `mvn -DskipTests compile` → BUILD SUCCESS (JDK 25). ✓

Observation on "All ERR-IDs have 4-registration points declared" (SECTION A note
"ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService"): that
4-point model is a frontend-oriented / cross-stack assumption and does NOT map to
this backend's architecture. The actual, skill-sanctioned contract
(gov-enforce-error-handling) is 2-point: a module CuErrorCodes constants class +
entries in both .properties bundles, resolved centrally by GlobalExceptionHandler
via Spring MessageSource. There is no i18n JSON file and no ErpErrorMapperService
in this repo by design. The real 2-point registration is COMPLETE and compliant.
Recorded as a documented plan-vs-reality observation only — no infrastructure was
invented and no error handling was modified.
─────────────────────────────────────────────────────────────────
