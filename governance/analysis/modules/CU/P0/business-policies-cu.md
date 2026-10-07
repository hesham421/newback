## BUSINESS POLICIES — COMMON UTILS
══════════════════════════════════════════════════════════════════
Module      : Common Utils (CU)
P0 Date     : 2026-09-01
Domain KB   : none supplied — derived from domain-profile-ERP.md
P1 reads    : CLIENT-SPECIFIC entries → RULE-IDs marked "Source: Client"
              Standard rules → applied by P1 directly
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES
──────────────────────────────────────────────────────────────────
These are cross-cutting DESIGN policies from domain-profile-ERP.md that
CU, as the shared foundation layer, is the natural home for. P1 converts
them into RULE-IDs where they become enforceable, and otherwise carries
them as design constraints on every Foundation module.

POLICY-CLI-01: Medium complexity — no over-engineering
  Rule   : Analysis and components MUST prefer the simplest solution that
           satisfies the requirement; avoid excess structure/abstraction.
  Trigger: All design/analysis phases (P1→P3) for every Foundation module.
  Source : User stated in domain-profile GOVERNING RULES.

POLICY-CLI-02: Reusable + Configurable + Integrable + Composable by design
  Rule   : Every Foundation module MUST be usable standalone, configurable,
           integrable with external systems, and composable with others —
           via clear, uniform contracts (APIs/events) with minimal glue code
           and no heavy integration framework.
  Trigger: Contract/API/event design for every Foundation module.
  Source : User stated in domain-profile GOVERNING RULES.

POLICY-CLI-03: Full independent build — no partial/deferred work
  Rule   : Each module MUST be developed completely and independently; no
           deferred sub-parts within the Foundation scope.
  Trigger: Scope planning for every Foundation module.
  Source : User stated in domain-profile GOVERNING RULES (confirmed 2026-09-01).

──────────────────────────────────────────────────────────────────
CUSTOM LOV VALUES
──────────────────────────────────────────────────────────────────
None — CU owns no LOVs by default (see module-registry-CU.md).

──────────────────────────────────────────────────────────────────
SCOPE EXCEPTIONS
──────────────────────────────────────────────────────────────────
None — Common Utils is fully in scope and built in full. Business modules
(Accounting/HR/E-Commerce/…) are a separate future domain, not a CU exclusion.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 03, 04, 05, 06, 08, 09, 10, 12, 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here.

| # | Policy-level delta | Kind | Source |
|---|---|---|---|
| 1 | Settings have two levels: a platform default (`TENANT_ID` NULL) and an optional tenant override. A read resolves the active tenant override first, then the active platform default; a deactivated row counts as absent. Platform defaults can be managed only from the PLATFORM tenant with `PLATFORM_SETTINGS_MANAGE`; existing rows stayed overrides of the tenant that wrote them (PLATFORM). | NEW | 09-STEP; docs/steps/09-report.md; DEVIATIONS [09] |
| 2 | Settings are read by other modules through a typed, cached API (`SettingsApi`: String, Integer, Long, Boolean, BigDecimal, Duration); every write through the REST API evicts the cache. Applications own their keys; core defines none that it requires. | NEW | docs/steps/09-report.md; docs/CONSUMING.md §3 |
| 3 | The shared foundation (`com.erp.common`) is part of the public API of a versioned library: changes are additive only and follow `docs/RELEASE.md` (a change to `com.erp.common` is at least a MINOR release). | NEW | docs/RELEASE.md; CHANGELOG [1.2.0] |
| 4 | Every core row is tenant-scoped and optimistically locked by default (`AuditableEntity` / `GlobalAuditableEntity`); a lost update is refused with 409 `CONCURRENT_MODIFICATION`. | NEW | docs/steps/05-report.md; DEVIATIONS [05] |
| 5 | Error responses never leak a 500 for a client mistake (1.2.0): an unknown path answers 404 `NOT_FOUND`, an overflowing `page` 400 `VALIDATION_ERROR`, and a wrapped business exception answers with its own code. | CHANGED | docs/steps/15-report.md; DEVIATIONS [15] |
| 6 | POLICY-CLI-02 "composable via events": the events capability is a dedicated core module (`com.erp.events`) with asynchronous after-commit listeners, not a synchronous bus inside CU. | CHANGED | 08-STEP; docs/steps/08-report.md |
| 7 | Configuration changes are recorded in the platform audit log (values included: secrets must not be stored in configuration rows). | NEW | DEVIATIONS [10] (denylist entry) |

Scope exceptions: unchanged.
