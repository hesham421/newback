# ADR-AUDIT-002 — Redaction model: name-substring denylist + technical fields + @Audited(ignore)

Module  : AUDIT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (listener diff rules) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
The `@Audited` listener writes the persistent properties of any annotated entity into `CHANGES`; the
step-10 file required that "no sensitive field name appears in any `CHANGES` JSON" and named a global
denylist `password, secret, token, hash` plus a per-entity `ignore` list, but left the diff rules open:
which technical properties to skip, how to match the denylist, what to do with associations, collections,
binary content, long values, and an update that changes nothing recordable (docs/DEVIATIONS.md [10]
"Diff rules (unspecified)", "Global denylist"). The same `CHANGES` array can also be supplied by an
explicit `AuditApi.record` caller.

Alternatives for the sensitive-field rule:
- **Exact-name denylist** (`password`, `passwordHash` …) — misses `tokenHash`, `contentHash`, `apiKey`,
  `clientSecret` the moment a module names a field differently.
- **Allowlist per entity** — every entity would have to enumerate its recordable fields; a new field would
  be silently unaudited.
- **Case-insensitive substring denylist, applied to every change whoever supplies it, plus an explicit
  per-entity `ignore`** — catches every spelling, needs no per-field registration, and lets an entity hide
  a field that is noisy rather than secret.

## Decision
A change is written to `CHANGES` only if its field passes all three filters
(`erp-core/src/main/java/com/erp/audit/domain/AuditEventDomain.java:50-73`):
1. **Global denylist, substring, case-insensitive** — `AuditApi.SENSITIVE_FIELD_WORDS` = `password`,
   `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey`, `salt` (the step's four, extended by four)
   (`audit/crossmodule/AuditApi.java:32-38`; `AuditEventDomain.isSensitive`, `:50-57`). Applied to the
   listener's diffs **and** to the changes of an explicit entry
   (`AuditEventDomain.recordableChanges`, `:68-73`; `audit/service/AuditRecordingService.java:75`).
   Consequence accepted: `FILE_DOCUMENT.contentHash` is never recorded; `CU_APP_CONFIGURATION.configValue`
   is recorded, because its name is not sensitive — an application must not store secrets in
   configuration rows (docs/DEVIATIONS.md [10] denylist entry).
2. **Technical fields** — `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `version`, `tenantId`
   (`AuditEventDomain.TECHNICAL_FIELDS`, `:28-29`): noise on every row, and the row carries actor, time
   and tenant itself.
3. **The entity's own `@Audited(ignore = {...})`** — Java property names never written
   (`audit/crossmodule/Audited.java:43-44`): `SEC_USER` ignores `passwordHash` (step) and `lastLoginAt`
   (each login would add an `UPDATE` row next to its `LOGIN` row); `CORE_NUMBER_SERIES` ignores `nextValue`
   (one row per allocation otherwise) (`sec/entity/User.java:32`; `sequence/entity/NumberSeries.java:41`).

Diff rules of the listener (`audit/listener/AuditedEntityListener.java:63-118`, `:157-183`):
- collections are skipped (`Type.isCollectionType()`); binary values (`byte[]`, `Byte[]`, `char[]`, `Blob`,
  `Clob`) become `null` and are left out; an association is recorded as the referenced entity's id;
- `CREATE` lists every non-null recordable property (`old` = null); `DELETE` every non-null deleted
  property (`new` = null); `UPDATE` only the properties whose value differs from the loaded snapshot
  (the dirty set when Hibernate has no snapshot); **an `UPDATE` with no recordable difference writes no
  row** (e.g. a password-only change — `SecAuditIntegrationTest.passwordOnlyUpdate_writesNoRow_andAnotherFieldDoes`);
- values become JSON scalars: strings, numbers, booleans; enums by name; temporals ISO-8601 (`toString`);
  text is cut at 2000 characters with a trailing `…` (`AuditRecordingService.scalar`, `:90-99`).

Verification: `AuditRows.assertNoSensitiveFieldAnywhere` scans every `CHANGES` array of every tenant after
each HTTP test against `password, passwordhash, token, tokenhash, secret, hash, contenthash, credential,
apikey, privatekey, salt, configjson` (docs/steps/10-report.md "Acceptance checklist"); TC-CORE-AUDIT-016.

## Consequences
- A field whose name contains a denylisted word is invisible to the audit log even when it is not a secret
  (`hash` matches `contentHash`); the trade-off is accepted — a false negative on a secret is worse.
- A secret stored under an innocent name (`configValue`, `notes`) **is** recorded; redaction is by name,
  not by content. Applications own their naming (POL-AUDIT-003; CU addendum policy 7).
- Explicit callers cannot force a sensitive field into `CHANGES`; the summaries (`SUMMARY_AR` / `SUMMARY_EN`)
  and `REFERENCE` are free text that the caller must keep clean.
- Adding a word to the denylist is a core change of a `List` constant on the public `AuditApi` (additive,
  MINOR); removing one is never done.
- Because `tenantId` is technical, a settings row's switch between platform default and tenant override
  is not visible in `CHANGES` — the row's own `TENANT_ID` identifies the acting tenant
  (docs/DEVIATIONS.md [10] rebase entry on `AppConfiguration`).

## Traces
ENT-AUDIT-001 · REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-008, REQ-AUDIT-011, REQ-AUDIT-017 ·
RULE-AUDIT-004, RULE-AUDIT-006, RULE-AUDIT-008 · POL-AUDIT-003, POL-AUDIT-009, POL-AUDIT-011 ·
DBF-AUDIT-012 · XM-AUDIT-002, XM-AUDIT-003 · docs/DEVIATIONS.md [10] (diff rules; denylist; `@Audited`
placement and options); docs/steps/10-report.md "Decisions & deviations" 6, 7, 8
