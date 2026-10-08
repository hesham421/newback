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
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to the erp-core repository at that tag. Policy ids are not minted here; rule ids are
the ones minted in `../P1/srs-cu.md`.

| # | Kind | Policy-level delta | Source |
|---|---|---|---|
| 1 | NEW | Settings have two levels: a platform default (`TENANT_ID` NULL) and an optional tenant override. A read resolves the active tenant override first, then the active platform default; a deactivated row counts as absent. Platform defaults can be managed only from the PLATFORM tenant with `PLATFORM_SETTINGS_MANAGE` (held implicitly by its super roles; any other caller is refused with 403 `ACCESS_DENIED`); existing rows stayed overrides of the tenant that wrote them (PLATFORM). | 09-STEP; docs/steps/09-report.md; docs/DEVIATIONS.md [09]; srs-cu.md RULE-CU-004, 005 |
| 2 | NEW | Settings are read by other modules through a typed, cached API (`SettingsApi`: String, Integer, Long, Boolean, BigDecimal, Duration); every write through the REST API evicts the cache. Applications own their keys; core defines none that it requires and no core module reads one. | docs/steps/09-report.md; docs/CONSUMING.md §3; srs-cu.md RULE-CU-006, 007 |
| 3 | NEW | Settings are changed only through the configuration API (or `ConfigurationService`), never by direct database edits: the cache has no TTL and is evicted by API writes only, so an out-of-band change stays invisible until the next API write; a locally cached (`simple`) deployment is not cluster-coherent. | srs-cu.md RULE-CU-008; ADR-CU-001 |
| 4 | NEW | Configuration keys are case-insensitive: stored upper-case, matched upper-case on every lookup (one key spelling per owner). | srs-cu.md RULE-CU-009; ADR-CU-003 |
| 5 | NEW | A new tenant is provisioned with no configuration rows; it inherits the platform defaults until it writes its own overrides. CU raises no event when a setting changes. | srs-cu.md RULE-CU-013, 014 |
| 6 | NEW | Every core row is tenant-scoped and optimistically locked by default (`AuditableEntity` / `GlobalAuditableEntity`); a lost update is refused with 409 `CONCURRENT_MODIFICATION` (for settings only between two server-side writes: `version` is not on the wire). | docs/steps/05-report.md; docs/DEVIATIONS.md [05]; srs-cu.md RULE-CU-011 |
| 7 | CHANGED | Error responses never leak a 500 for a client mistake (1.2.0): an unknown path answers 404 `NOT_FOUND`, an overflowing `page` 400 `VALIDATION_ERROR`, and a wrapped business exception answers with its own code. | docs/steps/15-report.md; docs/DEVIATIONS.md [15] |
| 8 | CHANGED | POLICY-CLI-02 "composable via events": the events capability is a dedicated core module (`com.erp.events`) with asynchronous after-commit listeners, not a synchronous bus inside CU. | 08-STEP; docs/steps/08-report.md |
| 9 | NEW | Configuration changes are recorded in the platform audit log (values included: secrets must not be stored in configuration rows). | docs/DEVIATIONS.md [10] (denylist entry); srs-cu.md RULE-CU-012 |

Note: the release policy of the shared foundation (`com.erp.common` is public API of the versioned library;
additive changes only, at least a MINOR release) is a platform policy recorded in `docs/RELEASE.md` and the SEC
platform summary addendum, not a CU business policy.

Scope exceptions: unchanged.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (commit 6b01816; CHANGELOG [Unreleased]); no CU behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | uses the shared `com.erp.common.domain.DomainRules`; no policy effect | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` | uses `TenantContext.isPlatform()`; no policy effect | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |

No policy delta.
