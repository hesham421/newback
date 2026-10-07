# PROJECT REGISTRY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile            : erp
Registry Version   : 1.3.0
Domain Profile     : erp/domain-profile.md v1
Last Updated       : 2026-09-10 by P3.1 (FIN v1 pass-1 completion — batch complete)
Modules registered : 9   Entity candidates : 29 (13 SEC + 2 MDL + 14 FIN)   Open items : 0
══════════════════════════════════════════════════════════════════

## SCHEMA COMPLIANCE MAP
| Section of this registry | Category (shared/REGISTRY-SCHEMA.md) |
|---|---|
| IDENTITY & VERSIONING | CAT-1 identity & conventions |
| CONVENTIONS & STEERING | CAT-1 identity & conventions |
| MODULE / COMPONENT INDEX | CAT-2 module index |
| ENTITY OWNERSHIP | CAT-3 entity ownership |
| SHARED ENTITY DECLARATIONS | CAT-4 shared declarations |
| STRUCTURAL / IMPLEMENTATION REGISTRY | CAT-5 structural registry |
| CROSS-MODULE DEPENDENCY INDEX | CAT-6 dependency indexes |
| DECISION INDEX | CAT-7 decision index |
| PIPELINE / PROGRESS STATUS | CAT-8 pipeline status |
| CHANGE / EVENT HISTORY | CAT-9 event history |
| PLATFORM FINDINGS | CAT-10 platform findings |
Uncovered: none

## IDENTITY & VERSIONING
| Field | Value |
|---|---|
| Profile | erp — ERP Platform |
| Registry version | 1.0.0 |
| Domain profile source | erp/domain-profile.md v1 |

### Version history
| Version | Date | Change |
|---|---|---|
| 1.0.0 | 2026-09-10 | Initial bootstrap from erp/domain-profile.md v1 (BOOTSTRAP event, see CHANGE/EVENT HISTORY) |
| 1.1.0 | 2026-09-10 | SEC v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |
| 1.2.0 | 2026-09-10 | MDL v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2 |
| 1.3.0 | 2026-09-10 | FIN v1 registered a new module (pass-1 complete, gate APPROVE) — minor bump per RULE-2; this batch (SEC→MDL→FIN) is now complete |
| 1.3.1 | 2026-09-18 | v7 repo layout migration — paths only, no content change (see CHANGE / EVENT HISTORY) |

## CONVENTIONS & STEERING
(copied verbatim from `erp/domain-profile.md` §7 — the authoritative source; this section
mirrors it for engines that read only the registry)

### Ubiquitous language
See `erp/domain-profile.md` §7.1 for the full bilingual (ar/en) term table — copied verbatim,
not restated here to avoid drift; cite as `[domain-profile §7.1]`.

### Bounded contexts
| Context | Owns module codes | Boundary statement |
|---|---|---|
| organization | ORG, SEC, MDL | Foundational capabilities (Tier 0) every other context depends on |
| supply | PRC, INV | Supply and inventory flows (out of scope this batch) |
| finance | FIN | General ledger; consumes organization, produces nothing back to it |
| people | HR | Human resources (out of scope this batch) |
| commercial | SLS, CTR | Sales and contracts (out of scope this batch) |

### Module prefixes
| Code | Display | Status |
|---|---|---|
| ORG | Organization | IN PROFILE |
| SEC | Security | IN PROFILE |
| MDL | Master Data Lookup | IN PROFILE |
| PRC | Procurement | IN PROFILE |
| FIN | Finance | IN PROFILE |
| HR | Human Resources | IN PROFILE |
| INV | Inventory | IN PROFILE |
| SLS | Sales | IN PROFILE |
| CTR | Contracts | IN PROFILE |

### Identifier rules
`{prefix}-{MOD}-{seq}` — seq width 3 (`factory.ids.pattern`, `factory.ids.seq_width`).
Entity kinds: master, transactional, lookup, config, security.

### ENFORCEMENT NOTES
- **E1** Every later artifact uses the terms of `domain-profile.md §7.1` verbatim; a synonym
  listed under "do not say" is a consistency finding at the pass gate (`gov.py analyze`
  checks registry ↔ artifact agreement).
- **E2** IDs follow `{prefix}-{MOD}-{seq}` (seq width 3) with the module codes of this section only.
- **E3** Entities are classified with the kinds: master, transactional, lookup, config, security.
- **E4** Sources to cite when a stage resolves an ambiguity: `profiles/erp/knowledge/erp-domain-standards.md`,
  then `erp/domain-profile.md` itself, then the three module plans (`new project/*-plan-en.md`)
  named in `domain-profile.md §7.5`.
- **E5** Pipeline status (below) is maintained by the orchestrator from commits; seeded here as NOT STARTED.

## MODULE / COMPONENT INDEX
| # | Code | Module | Bounded context | Category | Core/ext | Status | Source |
|---|---|---|---|---|---|---|---|
| 1 | SEC | Security | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 1 |
| 2 | MDL | Master Data Lookup | organization | Foundation | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 2 |
| 3 | FIN | Finance (General Ledger) | finance | Business — Tier 1 | Core | pass-1 COMPLETE (v1, gate APPROVE) | domain-profile §4 row 3 |
| 4 | ORG | Organization | organization | Foundation | — | RESERVED — not this batch | domain-profile §4 row 4; profile |
| 5 | PRC | Procurement | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 5; profile |
| 6 | HR | Human Resources | people | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 6; profile |
| 7 | INV | Inventory | supply | Business — Tier 1 | — | RESERVED — not this batch | domain-profile §4 row 7; profile |
| 8 | SLS | Sales | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 8; profile |
| 9 | CTR | Contracts | commercial | Business — Tier 2 | — | RESERVED — not this batch | domain-profile §4 row 9; profile |

## ENTITY OWNERSHIP
| ENT id | Name | Owner module | Kind | PRIVATE/SHARED | Status |
|---|---|---|---|---|---|
| ENT-SEC-001 | User | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-002 | Role | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-003 | UserRoleAssignment | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-004 | ModuleRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-005 | ScreenRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-006 | ActionRegistry | SEC | security | SHARED (owner) | REGISTERED |
| ENT-SEC-007 | RoleModuleGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-008 | RoleScreenGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-009 | RoleActionGrant | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-010 | ActiveSession | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-011 | AuditLogEntry | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-012 | PasswordResetToken | SEC | security | PRIVATE | REGISTERED |
| ENT-SEC-013 | SignupRequest | SEC | security | PRIVATE | REGISTERED |
| ENT-MDL-001 | LookupType | MDL | master | SHARED (owner) | REGISTERED |
| ENT-MDL-002 | LookupValue | MDL | lookup | SHARED (owner) | REGISTERED |
| ENT-FIN-001 | Account | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-002 | Dimension | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-003 | DimensionValue | FIN | lookup | PRIVATE | REGISTERED |
| ENT-FIN-004 | JournalEntry | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-005 | JournalLine | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-006 | JournalLineDimension | FIN | transactional | PRIVATE | REGISTERED |
| ENT-FIN-007 | FiscalYear | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-008 | FiscalPeriod | FIN | master | PRIVATE | REGISTERED |
| ENT-FIN-009 | EventTypeRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-010 | RuleLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-011 | RecurringTemplate | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-012 | RecurringTemplateLine | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-013 | AllocationRule | FIN | config | PRIVATE | REGISTERED |
| ENT-FIN-014 | AllocationTarget | FIN | config | PRIVATE | REGISTERED |
(Source: erp/modules/SEC/P1/registry-srs-sec.md, erp/modules/MDL/P1/registry-srs-mdl.md,
erp/modules/FIN/P1/registry-srs-fin.md)

## SHARED ENTITY DECLARATIONS
| Entity | Owner ENT id | Owner module | Consumers so far |
|---|---|---|---|
| User | ENT-SEC-001 | SEC | every future module, for its own audit fields (createdBy/updatedBy reference a SEC principal string, not a physical FK — see SEC db-script §3 AUDIT COLUMNS rule; declared here as the canonical identity source, not as a live FK target) |
| ModuleRegistry | ENT-SEC-004 | SEC | every future module registers one row of itself here (API-SEC-018) |
| ScreenRegistry | ENT-SEC-005 | SEC | every future module registers its screens here (API-SEC-019) |
| ActionRegistry | ENT-SEC-006 | SEC | every future module registers its actions here (API-SEC-020) |
| LookupType | ENT-MDL-001 | MDL | every future module registers its own lookup types here (API-MDL-002) |
| LookupValue | ENT-MDL-002 | MDL | every future module reads active values by key here (API-MDL-011) |

## STRUCTURAL / IMPLEMENTATION REGISTRY
| Module | Version | Tables | DBF range | API range | XM range |
|---|---|---|---|---|---|
| SEC | v1 | 13 (SEC_USER … SEC_SIGNUP_REQUEST) | DBF-SEC-001 … DBF-SEC-104 | API-SEC-001 … API-SEC-027 (QR-SEC-001…038) | none (ROOT) |
| MDL | v1 | 2 (MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE) | DBF-MDL-001 … DBF-MDL-021 | API-MDL-001 … API-MDL-011 (QR-MDL-001…015) | XM-MDL-001 (SOFT-READ → SEC, ACTIVE) |
| FIN | v1 | 14 (FIN_ACCOUNT … FIN_ALLOCATION_TARGET) | DBF-FIN-001 … DBF-FIN-146 | API-FIN-001 … API-FIN-032 (QR-FIN-001…044) | XM-FIN-001 (SOFT-READ → MDL, ACTIVE) |

## CROSS-MODULE DEPENDENCY INDEX
| Candidate ref | Kind | From module | To module | Consumes | Status | Evidence |
|---|---|---|---|---|---|---|
| XM-CAND-001 | — | FIN | SEC | RESOLVED — identity/authorization + SoD are enforced via the platform-standard interceptor (narrated in FIN's P3.1 Phase 1/Phase 7), not a formal XM row; see ADR-FIN-001 | RESOLVED — not an XM (no physical cross-module FK) | domain-profile §6 row "FIN \| SEC \| HARD-FK" (superseded) |
| XM-CAND-002 | — | FIN | MDL | RESOLVED — see XM-FIN-001 below; the platform-summary/module-registry candidate assumed HARD-FK, P2 correctly reclassified to SOFT-READ (ADR-FIN-001) | RESOLVED | domain-profile §6 row "FIN \| MDL \| HARD-FK" (superseded) |
| XM-MDL-001 | SOFT-READ | MDL | SEC | ModuleRegistry (ENT-SEC-004) — validates a lookup type's owner module code | ACTIVE (assigned, not a candidate) | erp/modules/MDL/P2/db-script-mdl.md §2 |
| XM-FIN-001 | SOFT-READ | FIN | MDL | LookupValue — validates every FIN lookup-backed column's code (13 keys) | ACTIVE (assigned, not a candidate) | erp/modules/FIN/P2/db-script-fin.md §2; erp/decisions/FIN/ADR-FIN-001.md |
FIN's SEC dependency (identity/authorization, self-registration) is not an XM row — see
ADR-FIN-001: no physical cross-module FK exists anywhere in this pipeline.
| XM-CAND-003 | SOFT/EVENT | FIN | Notifications (NOTIF, out of this batch) | period-close-awaiting notice, statement export — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-004 | SOFT/EVENT | FIN | File Service (FILESVC, out of this batch) | statement/export file — optional only | CANDIDATE | domain-profile §6; general-accounting-system-plan-en.md §2.3 |
| XM-CAND-005 | SOFT | SEC | Notifications (NOTIF, out of this batch) | password-reset message — optional only | CANDIDATE | domain-profile §6; security-module-plan-en.md §8 |
| XM-CAND-006 | EVENT | host business system (out of scope) | FIN | canonical accounting event only — no direct table read/write either direction | CANDIDATE | domain-profile §6 row "host → FIN (event only)"; general-accounting-system-plan-en.md §3 |
Note: every consumer module (all 9, per SEC/MDL plans §7/§4) will register the same
FIN→SEC / FIN→MDL shape once it exists; only the three modules named in this batch are
pre-registered as candidates above — this is not a closed list.

## DECISION INDEX
| # | Decision | Status | Source |
|---|---|---|---|
| 1 | PostgreSQL is the sole DB build target; Oracle/ADF remains an upstream event source only | ACCEPTED | domain-profile §8 row 1 |
| 2 | Hierarchical 3-level RBAC (Module→Screen→Action) replaces any module-local security | ACCEPTED | domain-profile §8 row 2 |
| 3 | One central Lookup master-detail hub replaces module-local lookup tables | ACCEPTED | domain-profile §8 row 3 |
| 4 | No per-entry approval in GL; the only human control point is period close | ACCEPTED | domain-profile §8 row 4 |
| 5 | This batch's scope and order: SEC, then MDL, then FIN, strictly in that order | ACCEPTED | domain-profile §8 row 5 |
| 6 | Notifications/File Service integration is optional-only, used solely on explicit plan need | ACCEPTED | domain-profile §8 row 6 |
| 7 | ADR-SEC-001 — SEC's owned lookups (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) stay CHECK-constrained in v1, not in a shared MDL lookup table, since SEC precedes MDL in this batch | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-001.md |
| 8 | ADR-SEC-002 — Error-catalog infrastructure rows (not-found, duplicate, invalid-transition, forbidden, invalid-sort, server) are cited as PLATFORM-STD under one umbrella ADR rather than a dedicated SRS RULE each | ACCEPTED (non-breaking) | erp/decisions/SEC/ADR-SEC-002.md |
| 9 | ADR-FIN-001 — FIN's SEC dependencies (identity/authorization, self-registration) are not modeled as XM rows; only XM-FIN-001 (SOFT-READ → MDL, lookup validation) is assigned, since no physical cross-module FK exists anywhere in this pipeline | ACCEPTED (non-breaking) | erp/decisions/FIN/ADR-FIN-001.md |

## OPEN QUESTION INDEX
none — `domain-profile.md` §10 records no open item.

## PIPELINE / PROGRESS STATUS
| Module | Version | Last committed stage | Last gate verdict | Delivered tracks | Tag |
|---|---|---|---|---|---|
| SEC | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| MDL | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| FIN | v1 | P3.1 (pass-1 complete) | APPROVE (pass-1, 2026-09-10) | backend: split done, deliver BLOCKED (no repo linked) | — |
| ORG | — | NOT STARTED | — | — | — |
| PRC | — | NOT STARTED | — | — | — |
| HR | — | NOT STARTED | — | — | — |
| INV | — | NOT STARTED | — | — | — |
| SLS | — | NOT STARTED | — | — | — |
| CTR | — | NOT STARTED | — | — | — |

## PLATFORM FINDINGS
CAT-10 (REGISTRY-SCHEMA.md §4) — every finding a module-scoped stage recorded
that is NOT that module's to settle. A module-scoped stage RECORDS a platform
finding and never fixes one; the row is closed by whoever owns the fix, never by
the module that found it. Every gate reads the OPEN rows of this table.

| Finding | Evidence | Found by | Belongs to | Status |
|---|---|---|---|---|
| `stack.backend.api.http_statuses` declares no **429**, so no module can answer a rate limit with a code the platform can emit. A throttle must therefore sit ahead of the application, where it is invisible to every module's contract and untestable from its plan. | `profiles/erp.yaml → stack.backend.api.http_statuses` (200, 201, 400, 401, 403, 404, 409, 413, 415, 422, 500); SEC v2 P3.1 C7.11 refused `SEC-429-RATE-LIMITED` on four lines | SEC v2 · P3.1 · pass-1 gate | the profile (platform-owned) | OPEN |

Open platform findings: 1

## CHANGE / EVENT HISTORY
| Date | Stage/tool | Module | Version | Event |
|---|---|---|---|---|
| 2026-09-10 | domain-profile | (platform) | — | domain-profile.md v1 saved and committed (23b3176) |
| 2026-09-10 | P-1 | (platform) | — | BOOTSTRAP — extracted 9 module rows, 0 entity candidates, 6 XM candidates, 6 confirmed decisions, 0 open items from domain-profile.md v1 |
| 2026-09-10 | P0 | SEC | v1 | P0 completed: SEC (platform-summary, module-registry-sec, business-policies-sec — 11 POL) |
| 2026-09-10 | P0.5 | SEC | v1 | P0.5 completed: SEC — 12 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | SEC | v1 | P1 completed: SEC — 13 entities, 33 requirements, 33 AC, 7 rules, 10 screen requirements, 0 ADR |
| 2026-09-10 | P2 | SEC | v1 | P2 completed: SEC — 13 tables, 104 DBF, 0 XM; ADR-SEC-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | SEC | v1 | P3.1 completed: SEC — 27 API, 38 QR, ALIGN PASSED; ADR-SEC-002 (ACCEPTED) |
| 2026-09-10 | gate:pass-1 | SEC | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | SEC | v1 | backend/exec split: 15 files, verify ok (39 checked) |
| 2026-09-10 | P0 | MDL | v1 | P0 completed: MDL (platform-summary, module-registry-mdl, business-policies-mdl — 6 POL) |
| 2026-09-10 | P0.5 | MDL | v1 | P0.5 completed: MDL — 5 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | MDL | v1 | P1 completed: MDL — 2 entities, 13 requirements, 13 AC, 4 rules, 2 screen requirements, 0 ADR |
| 2026-09-10 | P2 | MDL | v1 | P2 completed: MDL — 2 tables, 21 DBF, 1 XM (XM-MDL-001, SOFT-READ → SEC) |
| 2026-09-10 | P3.1 | MDL | v1 | P3.1 completed: MDL — 11 API, 15 QR, ALIGN PASSED, 0 new ADR |
| 2026-09-10 | gate:pass-1 | MDL | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 3) |
| 2026-09-10 | split | MDL | v1 | backend/exec split: 11 files, verify ok (21 checked) |
| 2026-09-10 | P0 | FIN | v1 | P0 completed: FIN (platform-summary, module-registry-fin, business-policies-fin — 20 POL, incl. all 14 §12 must-honor points) |
| 2026-09-10 | P0.5 | FIN | v1 | P0.5 completed: FIN — 19 stories; prd-approval APPROVED by ahmed.alsabonabi@gmail.com |
| 2026-09-10 | P1 | FIN | v1 | P1 completed: FIN — 14 entities, 46 requirements, 46 AC, 16 rules, 12 screen requirements, 0 ADR |
| 2026-09-10 | P2 | FIN | v1 | P2 completed: FIN — 14 tables, 146 DBF, 1 XM (XM-FIN-001, SOFT-READ → MDL); ADR-FIN-001 (ACCEPTED) |
| 2026-09-10 | P3.1 | FIN | v1 | P3.1 completed: FIN — 32 API, 44 QR, ALIGN PASSED (14-point §12 coverage confirmed), 0 new ADR |
| 2026-09-10 | gate:pass-1 | FIN | v1 | GATE pass-1: APPROVE (scores unambiguous 3, verifiable 3, complete 3, consistent 3, singular 3, feasible 3, traceable 2) |
| 2026-09-10 | split | FIN | v1 | backend/exec split: 15 files, verify ok (45 checked) |
| 2026-09-10 | BATCH | (platform) | — | GENERATION-INSTRUCTIONS.md batch complete: SEC v1 → MDL v1 → FIN v1, all pass-1 APPROVE, in mandated dependency order |
| 2026-09-18 | migration | (platform) | — | v7 layout: this repo is now the ERP PROJECT repo (project.yaml, profiles/erp.yaml moved here from the factory); `erp/…` became `analysis/…`, api-docs and each track's own state moved to `backend|frontend/modules/{MOD}/`, delivered packages to `…/modules/{MOD}/packages/`; every `erp/…` path cited in earlier rows reads with that prefix replaced — the rows themselves are history and were not rewritten. DEMO: no leftover found in the profile, this registry or any artifact — nothing to retire |
══════════════════════════════════════════════════════════════════
