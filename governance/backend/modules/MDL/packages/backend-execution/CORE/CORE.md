<!-- source: PHASE:CORE -->
<!-- traces: REQ-MDL-002 -->
<!-- PHASE:CORE:START traces=REQ-MDL-002 -->
## PHASE 1 — CORE

**Layers**: controller → service → mapper → domain → repository (same as every module,
`profile.stack.backend.layers`). Domain-behaviour placement: entity methods for
single-entity invariants (e.g. `LookupType.deactivate()`); service layer for anything
spanning more than one entity or a cross-module read (RULE-MDL-001 via XM-MDL-001,
RULE-MDL-004's join, RULE-MDL-002's pre-check even though it is also DB-enforced).

**Error signalling**: `LocalizedException → {code, messageAr, messageEn}`; runtime code
format `MDL-{http}[-{SLUG}]` (`profile.stack.backend.api.error_code_format`; {http} = the row's HTTP status, {SLUG} = SCREAMING-KEBAB, the [ ] half optional).

**Transaction scope**: `READ_ONLY` for every `FIND_*`/`EXISTS` QR; `READ_WRITE` for every
`SAVE`/`UPDATE` QR (including the batch reorder, QR-MDL-009, in one transaction).

**Search contract**: `{filters, page, size, sort}`, `Page<T>`, empty result = success.

**Audit fields**: `createdBy/createdAt/updatedBy/updatedAt` framework-filled, never in a
request DTO.

**Type mapping** (postgresql16 → Java):
| postgresql16 | Java |
|---|---|
| GENERATED ALWAYS AS IDENTITY | Long |
| VARCHAR(n) | String |
| BOOLEAN | Boolean |
| TIMESTAMPTZ | Instant |
| NUMERIC (bare, `sort_order` only) | Integer — governance note: `sort_order` is a whole-number ordering field, not a monetary/precision decimal; the profile's `decimal` syntax-map row (`NUMERIC(p,s)`) does not fit a bare `NUMERIC` column, so this module maps it to `Integer` explicitly (deviation stated once here, per P2 engine §4.1) |

**Lookup values**: not applicable in the usual sense — MDL IS the lookup mechanism; its own
`ownerModuleCode` field is validated against SEC's `ModuleRegistry` (XM-MDL-001), not
against another lookup type.

**Numbering**: not applicable — no MDL entity has a business code.

**Workflow engine**: forbidden — not used.

**Languages**: every name field (`nameAr`/`nameEn`) and catalog message present in ar + en.

**Cross-module contract placement**: MDL's one cross-module read (XM-MDL-001) is a plain
outbound call from the service layer (`SecModuleRegistryClient` or an equivalent
inversion-of-control interface implemented against SEC's `GET /api/v1/sec/registry`
search endpoint (API-SEC-021), filtered by `code`) — not a physical join, not a shared transaction.

**Cross-cutting authorization**: the same CORE interceptor mechanism SEC's own plan
declares (SEC's backend-execution-plan-sec.md → Phase 1 CORE) applies platform-wide; MDL
does not redeclare it, only cites it — every secured MDL endpoint below is gated by it
before its controller method runs.
<!-- PHASE:CORE:END -->
