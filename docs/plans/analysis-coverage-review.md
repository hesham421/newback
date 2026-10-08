# Analysis coverage review — does the analysis describe the implemented erp-core?

| | |
|---|---|
| Date | 2026-10-07 |
| Baseline | erp-core `v1.2.0` (the documented version); HEAD `1.3.0-SNAPSHOT` differs only by behaviour-neutral refactors (`DomainRules`, `StatusTransitions`, `OwnedLookups`, `TokenHasher`, `common.lookup.LookupOptionResponse`) |
| Method | six read-only audits, one per analysed module (SEC, MDL, CU, FILE, NOTIF) and one inventory of the packages with no analysis folder; each compared `docs/api-docs/<module>/`, the entities, migrations, permission classes, error-code classes, domain/service rules and cross-module surfaces against `governance/analysis/modules/<MOD>/` (P0–P2 bodies + the "Implementation Addendum — erp-core 1.2.0" sections), the ADRs and `docs/DEVIATIONS.md` |
| Scope | analysis and design only — no code or analysis file was changed by this review |
| Status | FINDINGS, awaiting the owner's decisions in §8 |

## 1. Verdict

**The analysis-first rule is not currently met for the implemented platform.** Every P0–P2 file of the five analysed modules carries an addendum with the required header lines — the *form* is right — but the *content* falls short in the same six ways in every module, and six implemented packages (TENANT, SEQUENCE, AUDIT, REPORT, EVENTS, COMMON) have no analysis folder at all.

| Module | Analysis folder | Addendum header on every P0–P2 file | Endpoints covered / mismatch | Error codes catalogued | ADRs | P2_5 addendum | Severity |
|---|---|---|---|---|---|---|---|
| SEC | yes | 8 / 8 | 29 / 11 of 40 | 11 of 32 (20 SEC-* codes nowhere) | 13 (2 cite ids that do not exist) | — (no P2_5) | **high** |
| MDL | yes | 8 / 8 | 5 / 6 of 11 (0 listed in the addendum) | 0 of 7 | 12 (ADR-MDL-009/010 still cited, files dropped) | none (stale: assumes DELETE permission) | **high** |
| CU | yes | 8 / 8 | 2 / 2 + 5 partial of 5 | 3 of 7 | 0 | — (P2_5 empty) | medium |
| FILE | yes | 8 / 8 | 6 / 9 of 14 (+2 in-process) | 2 of 12 | **0** (folder has `.gitkeep` only) | none (stale: no publish/visibility) | **high** |
| NOTIF | yes | 8 / 8 | 10 / 2 + 6 partial of 18 | 2 of 13 | **0** (folder empty) | none (stale: 4 statuses, 5 channels, no inbox) | **high** |
| TENANT | **no** | — | 5 endpoints, 7 codes, 13 rules undocumented | — | 0 | — | **high** (platform-wide) |
| SEQUENCE | **no** | — | 6 endpoints, 4 codes, 13 rules | — | 0 | — | medium |
| AUDIT | **no** | — | 1 endpoint, 1 code, 13 rules, 10 audited entities | — | 0 | — | medium |
| REPORT | **no** | — | 4 endpoints, 3 codes, 12 rules, dynamic permissions | — | 0 | — | medium |
| EVENTS | **no** | — | 10-event catalogue, executor semantics | — | 0 | — | medium |
| COMMON | **no** | — | 9 common codes, `Status` enum, 10 shared rules | — | 0 | — | low |

## 2. The six recurring failure modes (every analysed module)

| # | Pattern | Evidence (one example per module) | Why it matters |
|---|---|---|---|
| F1 | **"Unchanged / as before" rows that are false.** The addendum asserts no change where the code changed. | SEC: `POST /auth/logout` "as before" (there was no logout; ADR-SEC-008 says none exists) · MDL: "No endpoint added, changed or removed" (deactivate moved to `UPDATE`, reorder is 0-based, values list is paged) · CU: `scope=PLATFORM` refusal named as `SETTING_PLATFORM_SCOPE_FORBIDDEN` (it is `ACCESS_DENIED`) · FILE: `PERM_FILE_BROWSER_CREATE` "unchanged" (the analysis had no CREATE) · NOTIF: SMS/WHATSAPP/PUSH/INTERNAL "end `SKIPPED_NO_PROVIDER`" (they end `CHANGED_DISABLED`: no config rows) | The frontend builds from these rows. A false "unchanged" is worse than a missing row. |
| F2 | **No error-code catalogue.** Each addendum says "all analysed `<MOD>-*` codes are unchanged", but no P0–P2 file ever listed the codes (they lived in the dropped P3_1). | SEC 20 of 32 absent · MDL 7 of 7 · CU 4 of 7 · FILE 10 of 12 · NOTIF 11 of 13 | `CLAUDE.md` "Analysis first" step 4 requires error codes per endpoint. Nothing to check the api-docs against. |
| F3 | **Search / list contract never recorded.** Every module moved list to `POST …/search` with a filter envelope, allow-listed fields/sorts, page ≤ 200 and 400 `UNSUPPORTED_FILTER_FIELD`. | CU registry-srs still says `GET /configurations` · FILE/NOTIF/MDL say "unchanged" or "sub-paths" | Precedent ADR-SEC-003 exists but is cited nowhere else. |
| F4 | **Permission and seed drift.** Deactivate gated by `UPDATE` with no `DELETE` authority (MDL, FILE categories); `SYS_ADMIN` holds every module's grants (V7) though each SRS names only the module-admin role; `PLATFORM_SETTINGS`, SEQUENCE, AUDIT, REPORT catalog rows come from the startup synchroniser, not a migration; screen labels differ from the SRS (SEC ×5, MDL ×2). | `MdlPermissions.java:23-27`, `V7__sec_seed.sql:79-80,171-176`, `PermissionCatalogSynchronizer` | The registry the frontend reads (`project-registry.md`, module-registry files) describes authorities that do not exist and omits ones that do. |
| F5 | **Cross-module surfaces undocumented.** The real in-process APIs are missing or wrong in every A8 / DEPENDENCIES section. | SEC `SecModuleRegistryApi.isModuleActive` (consumed by MDL) · MDL `MdlLookupApi` + its FILE/NOTIF consumers · FILE `FileDocumentLookupApi.isAvailable` (SRS says `FileService` is injected) · NOTIF `dispatchIndependently`, `NotificationChannelAdminApi`, `NotificationLogQueryApi`; inbound changed from event to direct call · CU `ConfigurationService.resolve` | ArchUnit enforces the `crossmodule` boundary; the analysis does not describe it. |
| F6 | **Format: the `Kind` column is missing** in most addendum tables (module-registry, prd, registry-srs, db-script, registry-db in every module); non-standard kinds (`AS-BUILT (v2 G5)`, `unchanged (…)`); no `REMOVED` rows anywhere although removals exist (FILE upload token, NOTIF `channelHint=ALL`, NOTIF event listener, MDL DELETE permission, MDL name indexes, XM-FIN-001). | 7/8 CU files, 6/8 MDL, 8/8 FILE, 6/8 NOTIF, 5/8 SEC | The pattern `CLAUDE.md` mandates (NEW / CHANGED / REMOVED rows) is only followed in `srs-*.md` and `business-policies-*.md`. |

Two further cross-cutting facts:
- **ADRs**: CU, FILE and NOTIF have **no** decision records although each carries decisions that reversed the P0 AUTO-DECISIONs (FILE storage SPI vs "BYTEA only"; NOTIF `ChannelProvider` SPI vs `CONFIG_JSON` adapters; FILE/NOTIF LOVs moved to MDL). MDL cites ADR-MDL-009/010 as ACCEPTED in three places but both files were dropped in the vendoring. SEC ADR-035/038 cite `ENT-SEC-014`, `DBF-SEC-106`, `API-SEC-032..036`, `CS-SEC-001` — ids that exist in no vendored file.
- **P2_5 (UI/UX specs)** of MDL, FILE and NOTIF are stale against 1.2.0 and carry no addendum. By `CLAUDE.md` they are the frontend's write set, so they become **gap rows handed to the frontend**, not backend edits.

## 3. Per-module findings (top items; full tables are in the audit transcripts of this session)

### 3.1 SEC
1. 20 of 27 `SEC-<HTTP>-<SCENARIO>` codes appear in no P0–P2 file; `SEC-401-INVALID-CREDENTIALS` is also reused for a missing/invalid bearer token (undocumented).
2. The three registry write endpoints are gated by `PERM_SEC_MODULE_REGISTRY_UPDATE` (`RegistryService.java:73,88,110`); the SRS access summary says UPDATE = "deactivate only" and registration is "the module's own call". Addendum: "unchanged".
3. `POST /auth/logout` is NEW (idempotent, `isAuthenticated()`, LOGOUT in `SEC_AUDIT_LOG` **and** `CORE_AUDIT_EVENT`); ADR-SEC-008 is stale on this.
4. 13 `SEQ_SEC_*` sequences + BIGINT PKs replace the db-script's IDENTITY; neither P2 addendum records it (only `implementation-notes.md` §4.1 and the v2 ADR-SEC-035).
5. Staff and customer password-reset completion terminate every open session (+ `SESSION_TERMINATED` rows for staff) — not in the analysis.
6. RULE-SEC-007 is enforced as a blocking 409 `SEC-409-NO-VIEW-GRANT` at grant time; the SRS calls the create-time check "informational".
7. `POST /users` accepts `roleIds` and then additionally requires `PERM_SEC_USERS_UPDATE` (403 `SEC-403-FORBIDDEN`) — nowhere.
8. **Not implemented and not recorded**: role deactivate, registry-row deactivate, per-screen / per-action grant revoke (→ `PERM_SEC_ROLES_DELETE` registered but unused). This is the gap already confirmed as package **G** of `tenant-maturity-plan.md`.
9. `SecModuleRegistryApi.isModuleActive` (consumed by MDL, XM-MDL-001) is absent from SEC A8 and the addendum.
10. V7 seed (5 modules / 17 screens / 38 actions / roles `SYS_ADMIN`, `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` with 8/20/59 grants) and V11 §6 (two NOTIF templates seeded from a SEC migration) are undocumented; `SEC_ACTIVE_SESSION.LAST_ACTIVITY_AT` is written only at login, contradicting REQ-SEC-027; `PUT /users/{id}/roles` **replaces** the set (removed roles audited `ROLE_REVOKED`) while REQ-SEC-010 describes add-only; `PATCH /users/{id}` takes no body (B5 says `status=ACTIVE`); staff passwords have no minimum length (AC-SEC-007 says "platform rules") while customers have 8.

### 3.2 MDL
1. RULE-MDL-004 changed: an inactive type answers 404 `MDL-404-TYPE-KEY` (HTTP and `MdlLookupApi`) instead of "values excluded".
2. No `PERM_MDL_LOOKUPS_DELETE`; deactivation of types and values is gated by `PERM_MDL_LOOKUPS_UPDATE`. SRS §4 and module-registry say "unchanged"; `ui-ux-spec-mdl.md:20` still assumes DELETE.
3. 0 of 7 MDL codes catalogued; "No new MDL code" hides it.
4. Endpoint deltas missing: values search is **paged** (SRS B2 says "shown in full"); reorder positions are **0-based** (AC-MDL-010 expects 1,2,3) and partial id lists are accepted; deactivate returns 200 + body; `sortOrder` mandatory on create (A3 says default 0).
5. `MdlLookupApi` (contract, `LookupOptionView`, **no `@PreAuthorize`** — contradicts ADR-MDL-007 and SRS B5) and its FILE / NOTIF consumers are undocumented.
6. V8 seeds 4 types / 17 values by raw INSERT, contradicting DBS BLOCK 8 ("no seed, never by INSERT"); absent from the RDB / MR / SRS seed deltas.
7. Index deltas: `IDX_MDL_LOOKUP_TYPE_ACTIVE` and `IDX_MDL_LOOKUP_VALUE_TYPE` added, `_VALUE_SORT` renamed, `NAME_AR/EN` indexes never built; ADR-MDL-009 cited as ACCEPTED (`db-script-mdl.md:204,240`, `registry-db-mdl.md:40`) but the file is gone.
8. RULE-MDL-001 now requires an **active** module row; XM-MDL-001 reads `SEC_MODULE_REG.CODE` (analysis: `module_code`).
9. **No protected/system-value rule**: the seeded NOTIF/FILE types can be renamed or deactivated through the API, which would make NOTIF/FILE lookup reads answer 404 and break report LOOKUP parameters. No registration SPI exists (DBS claims modules register through the API).
10. `created_at DEFAULT now()` (DBS:234 says no default); `VERSION` not on the wire (the "optimistic lock 409" row overstates client protection); stale inbound XM-FIN-001.

### 3.3 CU
1. `scope=PLATFORM` from a non-PLATFORM tenant → 403 `ACCESS_DENIED` (`@PreAuthorize`), not `SETTING_PLATFORM_SCOPE_FORBIDDEN` (srs L173/L188); the integration test and `TC-CORE-SETTINGS-004` already expect `ACCESS_DENIED`.
2. Key normalisation undocumented — and a **code bug**: persist upper-cases but does not trim, lookup trims and upper-cases (`AppConfiguration` hooks vs `ConfigurationService.normalize`), so `" key"` is stored as `" KEY"` and can never be read.
3. `APP_CONFIGURATION_*` codes never listed: `NOT_FOUND` (404) has no rule, `KEY_IMMUTABLE` is never raised, `FIELDS_REQUIRED` is shadowed by bean validation.
4. `PLATFORM_SETTINGS` screen + `PLATFORM_SETTINGS_MANAGE` are inserted at startup by the catalog synchroniser (no migration); the authority reaches PLATFORM super roles via `MenuService.withSuperRole` — neither recorded.
5. Cache operations missing: provider chosen by the app, no TTL, not cluster-coherent, out-of-band DB changes never evicted, no settings-changed event, `ErpCoreCacheAutoConfiguration`.
6. `registry-srs-cu.md` still lists API-CU-002 as `GET /configurations`; search allow-lists, PUT reactivation via `isActive`, DELETE 204 repeatable, `configKey` silently ignored in PUT — all unrecorded.
7. `SettingsApi` requires a bound `TenantContext` (else 500 `TENANT_CONTEXT_MISSING`); CU has no `TenantProvisioningContributor` (new tenants inherit defaults); `SEQ_CU_APP_CONFIGURATION` created `CACHE 1` while P2 says `NO CACHE` ("DDL verbatim" claim hides it).

### 3.4 FILE
1. Download token: TTL **10 min** (RULE-FILE-003 says ~100), bound to the issuing user, `isAuthenticated()` only; the POLICY-CLI-03 **upload token was never implemented** (upload = JWT + permission) and is not marked REMOVED.
2. `PERM_FILE_BROWSER_CREATE` gates upload; the analysis explicitly had no CREATE; addendum says "unchanged". V7 grants every FILE action to `SYS_ADMIN` (SRS L137 says no seed).
3. `DELETE /api/v1/files/{id}?action=ARCHIVE|DELETE` selects `PERM_FILE_BROWSER_UPDATE` vs `_DELETE`, 400 unknown action, 422 illegal transition, 200 + body — undocumented.
4. FILE → MDL dependency (LOVs served from MDL, seeded V8) contradicts SRS A5 and db-script "no lookup table".
5. SRS L192 says `FileService` is injected into NOTIF; the real surface is `FileDocumentLookupApi.isAvailable` (unrecorded).
6. 10 FILE codes never named; `FILE_CATEGORY_INACTIVE` (422) has no rule; `FILE_DOCUMENT_INVALID_TRANSITION` is 400 **or** 422.
7. No platform-wide type list: only the category allow-list against the JDK magic-byte sniff (`URLConnection.guessContentTypeFromStream`), which does not recognise PDF/ZIP/Office — a PDF-only category likely rejects PDFs (**needs a test**).
8. Soft-delete / visibility interplay: DELETED rows still appear in metadata and list; ARCHIVED documents can be published (event fires, URL answers 404); archive does not unpublish; an inactive category still serves public files; `ALLOW_PUBLIC` is native BOOLEAN against the `IS_*_FL SMALLINT` convention.
9. RULE-FILE-005 "ownership-bounded" is a filter, not an authorisation check.
10. Required configuration unrecorded (`access-token-secret`, `s3.*`, `tenant.path-tenant-paths`, Spring multipart ≥ `erp.core.files.*`); `StorageProvider` keys are closed by a CHECK (DB/LOCAL/S3) while the addendum says "an application may override a provider by key".

### 3.5 NOTIF
1. Channel enablement wrong in two addenda: no config rows for SMS/WHATSAPP/PUSH/INTERNAL → `CHANNEL_DISABLED`, not `SKIPPED_NO_PROVIDER`.
2. 11 of 13 codes unrecorded; `NOTIF_TEMPLATE_INACTIVE` (422) cites RULE-NOTIF-007 wrongly.
3. Undocumented surfaces: `dispatchIndependently` (REQUIRES_NEW, caller-side recipient check), `NotificationChannelAdminApi`, `NotificationLogQueryApi`, `DispatchCommand`.
4. Inbound changed from `NotificationEvent` listener to a direct API call from SEC; `module-registry-notif.md:156-158` says the event decision "holds"; SRS L202 listener has no REMOVED row; `project-registry.md:56` repeats the error.
5. Attachments are validated on write but **never sent**; `CONFIG_JSON` is stored but **never read** (providers use Spring properties); `channelHint=ALL` unsupported — no CHANGED/REMOVED rows.
6. Rendering and e-mail composition undocumented: `{var}` syntax, unknown placeholders kept verbatim, single-language mail by `variables.lang` (default EN), HTML-escaped + CTA, IN_APP titles truncated at 300.
7. **Security decision unrecorded**: dispatch is `isAuthenticated()` only — any staff user can send any active template to any recipient or to an arbitrary `variables.email` (open-relay / phishing risk with `PASSWORD_RESET` + caller-chosen `actionLink`).
8. Templates/channels search are POST `/search` but "unchanged"; DELETE = soft disable/deactivate (204); PUT is full replace (omitted `configJson` / `attachmentFileId` clears it).
9. XM-NOTIF-001 still names `SEC_USER_ACCOUNT` (table is `SEC_USER`); MDL dependency and FILE write-time validation missing from dependency deltas.
10. Delivery details missing: `NotificationDeliveryTracker` (per-node de-dup), `requeue.interval-ms`, `NotificationFailedEvent` also fires on `REJECTED`; stale V6/V13 column comments; `ACCOUNT_ACTIVATION` template seeded but never dispatched.

### 3.6 Packages with no analysis folder (as-built inventory)

| Package | Step | What a folder must contain | Existing prose to cite (not restate) | Three ADR candidates |
|---|---|---|---|---|
| TENANT | 05 (+07, 15) | 5 endpoints; `CORE_TENANT` + the V10 retrofit (18 tables, FKs, indexes, composite UQs); module PLATFORM / screen `PLATFORM_TENANTS` / `PERM_PLATFORM_TENANTS_VIEW` + `PLATFORM_TENANT_MANAGE`; 7 codes; 13 rules (resolution order, strict resolver before web start, atomic provisioning, contributors SEC 0 / MDL 10 / NOTIF 20 / SEQUENCE 40); `TenantLookupApi`, `TenantProvisioningContributor`; `erp.core.tenant.*` | `SEC/P0/platform-summary.md:101-108`, `project-registry.md` row 19, `PROJECT-OVERVIEW.md:32,46-49`, `05-report.md`, DEVIATIONS [05][07][15], CONSUMING §3 | row-level `@TenantId` tenancy + global-entity allow-list; resolution order and suspension cutting live tokens; JDBC provisioning SPI with PLATFORM grants never copied |
| SEQUENCE | 09 | 6 endpoints (no DELETE); `CORE_NUMBER_SERIES` (14 columns, `UQ (TENANT_ID, CODE, PERIOD_KEY)`, CHECKs); module SEQUENCE / screen `SEQUENCE_SERIES` / VIEW + MANAGE (code only); 4 codes; 13 rules (pattern grammar `{PREFIX}{YYYY}{YY}{MM}{SEQ:n}{TENANT}`, period rows, anchor pessimistic lock + REQUIRES_NEW, gaps accepted); `NumberSeriesApi` (no `@PreAuthorize`, no core consumer) | `platform-summary.md:122-126`, registry row 24, `09-report.md`, DEVIATIONS [09], CONSUMING §3 | anchor-row lock + REQUIRES_NEW, not gap-free; row-per-period with immutable reset policy; deactivate-only, counter reset to 1 on provisioning |
| AUDIT | 10 | 1 endpoint (`AUDIT:EVENT:READ`); `CORE_AUDIT_EVENT` (append-only, JSONB `CHANGES`, **TIMESTAMP** not TIMESTAMPTZ); screens `AUDIT_EVENTS`, `AUDIT_REPORTS`; 1 code; 13 rules (synchronous JDBC write on the caller's connection incl. from the Hibernate flush, redaction denylist, truncation, 10 audited entities, SEC dual-write, opt-in retention job); `AuditApi`, `@Audited` | `platform-summary.md:110-115`, registry row 25, `10-report.md`, DEVIATIONS [10], CONSUMING §9 | synchronous JDBC write vs async/outbox; redaction model; coexistence with `SEC_AUDIT_LOG` + the TIMESTAMP choice |
| REPORT | 11 | 4 endpoints (`isAuthenticated()` + per-report authority in code); no table (in-memory registry); dynamic `<MODULE>_REPORTS` / `PERM_<MODULE>_REPORTS_VIEW` / `<MODULE>:REPORT:<CODE>` (4 reports today); 3 codes; 12 rules (fail-fast startup validation, parameter typing incl. LOOKUP via MDL, cap+1 export probe, CSV BOM + formula guard, bounded `byte[]`); `ReportProvider` SPI | `platform-summary.md:128-133`, registry row 26, `11-report.md`, DEVIATIONS [11], CONSUMING §6 | reports as code + startup registry; automatic permission registration checked in code not `@PreAuthorize`; bounded export |
| EVENTS | 08 | no endpoint/table; `DomainEvent` envelope; 10-event catalogue with publishers; one core consumer (`NotificationDeliveryListener`); `erpCoreEventExecutor` (`defaultCandidate=false`) + `TenantAndSecurityContextTaskDecorator`; after-commit, at-most-once; `erp.core.events.executor.*` | `platform-summary.md:117-120`, `PROJECT-OVERVIEW.md:34`, `08-report.md`, DEVIATIONS [08], CONSUMING §5 | in-process bus, no broker/outbox; tenant/security-propagating executor; payload contract (plain values, no secrets) |
| COMMON | 01/05/15 | no endpoint/table; `GlobalAuditableEntity` / `AuditableEntity`; 9 common codes + 14-value `Status`; `SpecBuilder` (400 on unknown filter, 8 operators), `PageableBuilder` (20 / max 200), `StatusTransitions`, `DomainRules`, `OwnedLookups`, `FilterErrorResponseWriter`, converters; `ActiveFlagQueryHelper` has 0 consumers | `PROJECT-OVERVIEW.md:26`, `platform-summary.md:135-146`, CHANGELOG [Unreleased], DEVIATIONS [01][05][15] | `LocalizedException` + bilingual registered codes + envelope; strict filter rejection + page cap; base-entity hierarchy with global allow-list |
| AUTOCONFIGURE · APP | 03 · — | 9 auto-configurations, ~45 `erp.core.*` keys with defaults; OpenAPI groups exist for sec/notif/file/mdl/cu/customers only; APP: 1 dev endpoint under `/api/v1/sec/dev`, `APP_SMOKE` without tenant columns, `APP_SMOKE_REPORT` | `03-report.md`, DEVIATIONS [03][12], CONSUMING §1–2, registry row 27 | library auto-config with explicit package list; Flyway V1..V999 / V1000+ split; layered message source |

## 4. Platform documents that contradict the code

| Document | Says | Code |
|---|---|---|
| `governance/analysis/platform/project-registry.md:56` | SEC reaches NOTIF through domain events | direct `NotificationDispatchApi.dispatchIndependently` calls (`CustomerAccountService.java:309`, `PasswordResetService.java:226`); events are used only inside NOTIF |
| `PROJECT-OVERVIEW.md:46` | "Every core table carries `TENANT_ID`" | `CORE_TENANT` and the three `SEC_*_REG` tables are global; `CU_APP_CONFIGURATION.TENANT_ID` is nullable |
| `SEC/P0/platform-summary.md:79` | Java 21 | JDK 25 required (CHANGELOG [Unreleased], enforcer) |
| `SEC/implementation-notes.md` §1 | the 8 SEC tables have no audit columns and do not extend `AuditableEntity`; header says "current as of 1.2.0" | V10 §4 added the columns; all 13 entities extend `AuditableEntity` |
| ADR-SEC-008 | no logout endpoint is published | `POST /api/v1/sec/auth/logout` exists |
| ADR-SEC-035 / 038, MDL db-script / registry-db | cite ADR-MDL-009/010, ENT-SEC-014, DBF-SEC-106, API-SEC-032..036, CS-SEC-001 | none of these exists in the vendored set |

Contract-document (`docs/api-docs/`) defects found on the way — generator or annotation issues, not analysis: FILE publish permission shown as `DOCUMENT_PUBLISH` (constant name) instead of `FILE:DOCUMENT:PUBLISH`; audit permission shown as `AUDIT_EVENT_READ` instead of `AUDIT:EVENT:READ`; CU "Required permission(s)" line garbles the SpEL; `public-files.md` lists only 200; NOTIF inbox endpoints have no authorization rule or business responses (so `INBOX_ITEM_NOT_FOUND` and `NOTIF_CHANNEL_UNAVAILABLE` are "unbound"); a stale `FileService.sha256Hex` citation; no OpenAPI groups for tenant / sequence / audit / report. ArchUnit names `com.erp.events.crossmodule` and `com.erp.report.crossmodule`, which do not exist.

## 5. Code defects and risks the audit surfaced (NOT analysis work — for a separate backlog)

| # | Module | Finding | Evidence |
|---|---|---|---|
| R1 | CU | Key with surrounding spaces is stored as `" KEY"` and unreachable | `AppConfiguration` `@PrePersist` upper-cases only; `ConfigurationService.normalize` trims + upper-cases |
| R2 | MDL | Seeded NOTIF/FILE lookup types have no protection; deactivating one breaks NOTIF/FILE lookups and report LOOKUP params | no system flag; `LookupTypeService.deactivate` |
| R3 | FILE | Category allow-list is matched against the JDK sniff, which does not detect PDF/ZIP/Office | `FileService.java:130-135,446-452` — verify with a test |
| R4 | FILE | DELETED documents still returned by metadata and list; inactive category still serves public files; ARCHIVED can be published | `FileService.java:236-245`, repository L98-99, L323-342 |
| R5 | NOTIF | Dispatch open to any authenticated staff user, with caller-chosen `variables.email` and `actionLink` | `DispatchService.java:69-72` |
| R6 | NOTIF | Attachments never delivered; `CONFIG_JSON` never read; `ACCOUNT_ACTIVATION` seeded but never dispatched | `OutboundMessage`, `EmailChannelProvider`, V9 |
| R7 | SEC | `SEC_ACTIVE_SESSION.LAST_ACTIVITY_AT` never refreshed after login | writers: `AuthService.java:101`, `CustomerAccountService.java:201` only |
| R8 | SEC | Staff password has no minimum length (customers: 8) | `UserCreateRequest` vs customer register |
| R9 | SEC | `CustomerVerifiedEvent` not published when reset completion verifies; `PasswordResetRequestedEvent` staff-only | `CustomerAccountService.java:255-265` |
| R10 | COMMON | `ActiveFlagQueryHelper` has no consumer | dead code |
| R11 | CU | `CuPermissions` Javadoc maps API ids wrongly (003/004/005) | code comment only |

R2, R5 and R8 overlap with the approved `tenant-maturity-plan.md` (password policy in D.1; SEC grant revoke in G). R5 should become an ADR + a rule before 1.3.0 is tagged.

## 6. What "done" looks like (target state per module)

For each module, the addendum set must let a reader answer every item of `CLAUDE.md` "After the code is written" without opening the code:

1. **Endpoints table** — every (method, path) in `docs/api-docs/<module>/` with permission (exact authority string), request/response deltas, success status, and every error code it can raise. No "as before" / "unchanged" wording: a row is NEW, CHANGED (with the delta) or absent because the original B5 row is still exact.
2. **Error-code catalogue** — every constant of `<Mod>ErrorCodes` with HTTP status, trigger and the RULE/REQ it serves; "registered but never raised" is a valid row.
3. **Schema table** — every column/constraint/index/sequence/seed of the module's migrations, including deltas against the P2 DDL (`CACHE 1`, `DEFAULT now()`, renamed indexes, missing indexes, raw-INSERT seeds).
4. **Permissions & seeds** — every constant, catalog row (with labels), which migration or synchroniser creates it, which roles hold it (incl. `SYS_ADMIN`), and the REMOVED authorities.
5. **Rules** — every Domain decision and every service-level rule (normalisation, immutability, cascade, limits, defaults, caching), each as a `RULE-<MOD>-NNN` continuing the sequence.
6. **Cross-module & events** — every `crossmodule` interface (method, auth posture, consumers), every SPI with its implementers, every event published/consumed (with "none" stated explicitly).
7. **Kind column** on every addendum table; `REMOVED` rows for everything the analysis promised and the code dropped.
8. **ADRs** for each decision that reversed an AUTO-DECISION or the original analysis.

## 7. Proposed remediation (analysis work only, in order)

| Package | Content | Output | Size |
|---|---|---|---|
| **R0 — pattern standard** | one short section in `governance/rules/GOVERNANCE-RULES.md`: the addendum table shapes (Kind column mandatory, allowed kinds NEW / CHANGED / REMOVED / NOT IMPLEMENTED, no "unchanged" rows, mandatory sub-tables §1–§6), the header lines, how a correction to an earlier addendum is recorded | 1 file | S |
| **R1 — TENANT folder** | P0 / P0_5 / P1 / P2 as-built + ADR-TENANT-001..003 (package A of `tenant-maturity-plan.md`) | 9 files | M |
| **R2 — SEC, MDL, FILE, NOTIF, CU corrections** | per module: the false rows fixed, the missing rows added (§6 target state), REMOVED / NOT IMPLEMENTED rows, dangling ADR references resolved | ~40 files touched | L (SEC, MDL, FILE, NOTIF ≈ 1 day each; CU ½) |
| **R3 — ADRs** | CU (1–2), FILE (≈7), NOTIF (≈7), SEC (logout, registry gate, sequences-over-identity: 3), MDL (replace dropped 009/010 with one "index strategy as built") | ≈20 files | M |
| **R4 — SEQUENCE, AUDIT, REPORT, EVENTS, COMMON folders** | same pattern as R1, from §3.6 inventories; AUTOCONFIGURE and APP as sections of `PROJECT-OVERVIEW.md` | ≈35 files | L |
| **R5 — platform documents** | `project-registry.md`, `PROJECT-OVERVIEW.md`, `platform-summary.md`, `implementation-notes.md`, ADR-SEC-008 corrections (§4) | 5 files | S |
| **R6 — hand-offs** | frontend: P2_5 gap rows (MDL, FILE, NOTIF) via `newfront/docs/api-doc-gaps.md`; backend backlog: §5 R1–R11 and the api-docs generator defects | 2 lists | S |

R0 first (so R1–R4 are written to one standard), then R1 (unblocks `tenant-maturity-plan.md`), then R2 in the order NOTIF → FILE → SEC → MDL → CU (most false rows first). R3 can run alongside R2 per module. R4 last.

## 8. Decisions needed from the owner

| # | Question | Recommendation |
|---|---|---|
| Q1 | A 1.2.0 addendum row that is **wrong** (F1): correct it in place, or leave it and add a dated correction section? | **Correct in place** — the addendum is the implementation record, not the original analysis the append-only rule protects; add one line `Revised 2026-10-07 — see docs/plans/analysis-coverage-review.md` under the addendum header. Original analysis bodies above the addenda stay untouched. |
| Q2 | Operations the analysis specified and the code never built (SEC role deactivate, registry-row deactivate, grant revoke; FILE upload token; NOTIF `ALL`, attachments): record as `NOT IMPLEMENTED` rows, or as REMOVED? | `NOT IMPLEMENTED (DEFERRED → <plan ref>)` when a plan exists (e.g. package G), `REMOVED` when the decision is final (upload token). |
| Q3 | Scope of R4: all five remaining folders now, or TENANT only (R1) and the rest after 1.3.0? | All five, after R2 — they are smaller than any single existing module and their inventories are already written in §3.6. |
| Q4 | ADR numbering for modules whose folder is empty: start at `ADR-<MOD>-001`? | Yes; MDL continues after 043 (009/010 stay dropped, their replacement gets the next free number). |
| Q5 | §5 code defects: open a backlog file (`docs/plans/code-findings-backlog.md`) now, or fold into `tenant-maturity-plan.md`? | Separate backlog file; only R5 (NOTIF dispatch exposure) is pulled into 1.3.0 scope as an ADR + rule. |
