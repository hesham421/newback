# ADR-MDL-046 — In-process MdlLookupApi is ungated; VIEW gates the HTTP read only

Module  : MDL     Version : v1     Stage raised : erp-core 1.2.0 analysis revision (2026-10-07)
Status  : ACCEPTED (non-breaking) — supersedes ADR-MDL-007 in part (the authorization sentence)

## Context
`srs-mdl.md` SCR-REQ-MDL-002 §B5 and ADR-MDL-007 describe one consumer read, `GET
/api/v1/mdl/lookups?type=<key>` (API-MDL-011), "gated by `PERM_MDL_LOOKUPS_VIEW` granted to the
calling module's service principal": a consuming module was expected to call MDL over HTTP under a
service account holding VIEW on `MDL_LOOKUPS` (B4 of SCR-REQ-MDL-001 lists that account).

erp-core builds every module into one library and forbids loopback HTTP between modules
(`com.erp.sec.crossmodule.SecUserDirectoryApi` doctrine; ArchUnit `CrossModuleBoundaryArchTest`).
MDL therefore exposes a second read, `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey
(String typeKey)` → `List<LookupOptionView(code, labelAr, labelEn, sortOrder)>`, implemented by
`MdlLookupApiImpl` (`erp-core/src/main/java/com/erp/mdl/crossmodule/MdlLookupApiImpl.java`):

- `@Transactional(readOnly = true)`, direct repository access — it does **not** go through
  `LookupConsumerService`, whose `@PreAuthorize(PERM_MDL_LOOKUPS_VIEW)` gates the HTTP read;
- **no `@PreAuthorize`** of its own (`MdlLookupApiImpl.java:32-46` records the reasoning: the callers
  are reached from an already-authenticated request, and `InternalCallerContext` exists for
  principal-less callers, which these are not);
- the same resolution and the same refusal as the HTTP read: an unknown key or an inactive type throws
  `LocalizedException(NOT_FOUND, MDL-404-TYPE-KEY)`; an active type with no active value returns an
  empty list.

Its consumers at 1.2.0 are FILE (`FileLookupService`, `GET /api/v1/files/lookups/{lookupKey}`,
`isAuthenticated()`), NOTIF (`NotificationLookupService`, `GET /api/v1/notifications/lookups/
{lookupKey}`, `isAuthenticated()`) and REPORT (`ReportService`, LOOKUP parameter validation). None of
their callers holds — or needs — an MDL authority; `V7__sec_seed.sql` grants MDL to `SYS_ADMIN` only
and seeds no service-account role. Had the API been gated by `PERM_MDL_LOOKUPS_VIEW`, every FILE or
NOTIF user would have needed an MDL grant to open a dropdown, which the analysis never asked for
(business-policies SCOPE EXCEPTIONS: the gate is the screen-level grant of the *consuming* screen).

## Decision
- The in-process `MdlLookupApi` carries **no** permission gate. The authorization of a consumer read
  is the consuming module's own gate on its own endpoint or job; MDL trusts an in-process caller the
  way `SecModuleRegistryApiImpl.isModuleActive` does.
- `PERM_MDL_LOOKUPS_VIEW` gates the **HTTP** reads only: `POST /lookup-types/search`,
  `POST /lookup-types/values/search` and `GET /lookups?type=`. The HTTP consumer read stays published
  for external (non-core) callers and for verification; it is called by no erp-core module.
- The consumer contract is the same on both paths: active values of an active type, ordered by
  `sortOrder`; 404 `MDL-404-TYPE-KEY` for an unknown or inactive type (RULE-MDL-004 as built). A
  consumer that fronts MDL on its own API translates that 404 into its own code (`FILE_LOOKUP_KEY_
  UNKNOWN`, `NOTIF_LOOKUP_KEY_UNKNOWN`, `REPORT_PARAM_INVALID`), so `MDL-404-TYPE-KEY` never leaks
  out of another module's API.
- ADR-MDL-007's binding decision — API-MDL-011 is bound to SCR-MDL-002's F2 block and called by no
  screen — stands. Its authorization sentence ("gated by `PERM_MDL_LOOKUPS_VIEW` granted to the calling
  module's service principal") is superseded by this ADR, and B4's "consuming-module service account"
  role is not seeded and not required.

## Consequences
- `srs-mdl.md` §4 (1.2.0 addendum) records VIEW as gating the HTTP reads only and the API as ungated;
  `module-registry-mdl.md` EXPOSED SURFACE and `registry-srs-mdl.md` Exposed carry the API's shape.
- A new in-core consumer of lookups injects `MdlLookupApi` and declares its own gate; it never calls
  `GET /api/v1/mdl/lookups` over HTTP and never requests an MDL authority for its users.
- Should a consumer ever need caller-specific restriction on lookup reads (per-type permission is a
  recorded scope exception of business-policies-mdl.md), that is a new ADR and a new gate on the
  consumer, not on `MdlLookupApi`.
- The frontend reads this through `governance/analysis/modules/MDL/`; `ui-ux-spec-mdl.md` (P2_5,
  the frontend's write set) is not changed by this ADR.

## Traces
REQ-MDL-011, REQ-MDL-012 · RULE-MDL-004 · SCR-REQ-MDL-001 §B4, SCR-REQ-MDL-002 §B5 · ADR-MDL-007 ·
`MdlLookupApi.java`, `MdlLookupApiImpl.java:32-66`, `LookupConsumerService.java:48`,
`FileLookupService.java:48-58`, `NotificationLookupService.java:39-49`, `ReportService.java:143-150`,
`V7__sec_seed.sql:108-112,171`
