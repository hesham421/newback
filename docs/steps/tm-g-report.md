# TM-G report — revoke a single screen or action grant (closes D7)

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §0 D7, §8b, §9 (ADR-SEC-041 row), §10 |
| Branch | `tm/g-sec-grant-revoke` (from `main` `2274f86`) |
| Version | erp-core 1.3.0-SNAPSHOT (additive: two endpoints, two DTOs; no migration) |

## Summary

A role's single screen grant or single action grant can now be revoked. Before this package the API could only
revoke a whole module (`DELETE /roles/{id}/modules/{moduleId}`), although ENT-SEC-008/009 and the audit catalogue
(`SCREEN_REVOKED`, `ACTION_REVOKED`) had always promised it.

- `DELETE /api/v1/sec/roles/{id}/screens/{screenId}` removes the screen grant and the role's action grants on that
  screen (RULE-SEC-054). It answers `ScreenGrantRevokeResponse { revokedActionGrants }`.
- `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` removes one action grant. Revoking the screen's `VIEW` also
  removes the role's other action grants on that screen; the screen grant stays (RULE-SEC-055, ADR-SEC-062). It
  answers `ActionGrantRevokeResponse { revokedActionGrants }`, where the count includes the requested grant.
- Both need `PERM_SEC_ROLES_UPDATE`. They answer 404 `SEC-404-ROLE` when the role is not in the caller's tenant,
  then 404 `SEC-404-GRANT` when the role does not hold the grant. Each runs in one transaction and writes SEC audit
  rows the same way `revokeModule` does. No new error code.
- The cascade sets are decided by `RoleScreenGrantDomain.cascadeOnRevoke` and `RoleActionGrantDomain.cascadeOnRevoke`,
  not by the service.
- Sessions are not ended: `JwtAuthenticationFilter` reads authorities again on every request. A super role keeps
  every authority, and a revoke changes only its menu. Both facts are verified in code and documented in the
  addendum (§6).

## Analysis entries written

| File | Section | Ids / content |
|---|---|---|
| `governance/analysis/modules/SEC/P1/srs-sec.md` | `## Implementation Addendum — erp-core 1.3.0` (after the 1.2.0 one) | REQ-SEC-080, AC-SEC-086, REQ-SEC-081, AC-SEC-087, RULE-SEC-054, RULE-SEC-055; SCR-REQ-SEC-005 B1/B3/B5 CHANGED (two B5 rows); endpoint table (method, path, permission, response DTO, errors with HTTP status, order of checks); audit (§5, catalogue cited: §A6, `module-registry-sec.md` AUTO-DECISIONS, `CHK_SEC_AUDIT_LOG_EVENT_TYPE` in V4 §5c); behaviour notes (§6: sessions, super role, Domain ownership, all cited to code); decisions (§7); frontend impact (§8); "no schema change, no migration" |
| `governance/analysis/modules/SEC/P1/registry-srs-sec.md` | same heading | registry deltas, counts REQ 37 · AC 37 · RULE 9, last sequence per atom |
| `governance/analysis/modules/SEC/P0/business-policies-sec.md` | same heading | POL-SEC-002 extended to revocation (CHANGED); sessions-not-ended and super-role notes |
| `governance/analysis/decisions/SEC/ADR-SEC-062.md` | new | "Revoking VIEW cascades vs refusing" (plan name ADR-SEC-041) — cascade with a counted response |
| `governance/README.md` | decisions line | SEC ADR count 13 → 14 |
| P2 | — | not touched: no schema change, and the SEC P2 file pattern needs no entry for it |

## Files changed

Code (erp-core, `com.erp.sec`):
- `domain/RoleScreenGrantDomain.java`: `cascadeOnRevoke(List<RoleActionGrant>)` (RULE-SEC-054).
- `domain/RoleActionGrantDomain.java`: `cascadeOnRevoke(List<RoleActionGrant>)` (RULE-SEC-055).
- `repository/RoleScreenGrantRepository.java`: `findByRoleAndScreen`.
- `repository/RoleActionGrantRepository.java`: `findByRoleAndAction`, `findAllByRoleAndScreen`.
- `dto/ScreenGrantRevokeResponse.java` (new), `dto/ActionGrantRevokeResponse.java` (new).
- `mapper/RoleScreenGrantMapper.java`, `mapper/RoleActionGrantMapper.java`: `toRevokeResponse(int)`.
- `service/RoleGrantService.java`: `revokeScreen`, `revokeAction`, private `loadRole`.
- `controller/RoleGrantController.java`: the two `@DeleteMapping`s.
- `exception/SecErrorCodes.java`: Javadoc of `SEC_404_ROLE` / `SEC_404_GRANT` names the new endpoints.

Tests: `erp-core/src/test/java/com/erp/sec/RoleGrantRevokeIntegrationTest.java` (7 tests, new),
`erp-core/src/test/java/com/erp/sec/domain/GrantRevokeDomainRulesTest.java` (2 tests, new).

Docs: the analysis files above; `docs/api-docs/**` (regenerated, 29 files); `docs/test-api/core-test-plan.md`,
`docs/test-api/core_api_verify.py`, `docs/test-api/core_verify_report.py`,
`docs/test-api/results/20261008T000213-P-LIVE.json` + `…-P-LIVE-report.md` (new; they replace the first run's files); `docs/CHANGELOG.md`;
`docs/DEVIATIONS.md`; this report.

Migrations: none.

## Decisions & deviations

`docs/DEVIATIONS.md` → `## [TM-G] tenant-maturity G — revoke a single screen or action grant`:
1. Plan ADR-SEC-041 = **ADR-SEC-062**. ADR-SEC numbers up to 061 were issued historically.
2. The two revokes answer 200 with a count DTO rather than 204 (skill rule A.6.5), the same as the existing module
   revoke.
3. The api-docs: the first regeneration lost 29 business-error rows (shared-helper refactor `6b01816`); the
   generator was fixed and 28 rows restored, the 29th is a genuine change (Review round 1). The `check` FAILs are
   the known generator limitations.
5. Review round 1: G's SEC ids renumbered past the highest ever issued (REQ-SEC-080/081, AC-SEC-086/087,
   RULE-SEC-054/055).
4. The HTTP-run archive layout for tenant-maturity packages: the run JSON plus a per-run report in
   `docs/test-api/results/`.

Other decisions, all in the addendum:
- The role is checked before the grant: `SEC-404-ROLE` comes before `SEC-404-GRANT`. The module revoke is unchanged
  and keeps going straight to `SEC-404-GRANT`.
- `revokedActionGrants` on the action revoke counts every action grant removed, the requested one included.
- Revoking from an inactive role is allowed.
- The HTTP cases run in tenant B. REPORT-003 and TENANT-014 assert tenant A's exact user set, so adding a user in A
  would break them.

## Acceptance checklist

| # | Item (plan §8b + §10 DoD) | Evidence |
|---|---|---|
| 1 | Analysis entries (REQ-SEC-080/081, AC, RULE-SEC-054/055, B5, ADR) before the first code commit | `900a8fc` precedes `d298019` |
| 2 | `DELETE /roles/{id}/screens/{screenId}`, `PERM_SEC_ROLES_UPDATE`, `ScreenGrantRevokeResponse { revokedActionGrants }`, 404 role/grant, one transaction, audit `SCREEN_REVOKED` + one `ACTION_REVOKED` per cascaded action | `RoleGrantService.revokeScreen`; JUnit `revokeScreen_cascades…`; TC-CORE-SEC-035, SEC-038 PASS |
| 3 | `DELETE /roles/{id}/actions/{actionId}`: VIEW cascades (RULE-SEC-055), otherwise one row | `RoleGrantService.revokeAction`; JUnit `revokeAction_ofView…`, `revokeAction_ofANonViewAction…`; TC-CORE-SEC-036, SEC-037 PASS |
| 4 | Module revoke unchanged | JUnit `moduleRevoke_stillCascades…`; TC-CORE-SEC-040 PASS |
| 5 | Cascade decisions on the grant Domain objects | `RoleScreenGrantDomain.cascadeOnRevoke`, `RoleActionGrantDomain.cascadeOnRevoke`; `GrantRevokeDomainRulesTest` |
| 6 | Sessions not terminated; super role keeps every authority (documented) | addendum §6; JUnit `revoke_takesEffectOnTheNextAuthorityRead_butASuperRoleKeepsEveryAuthority`; TC-CORE-SEC-037 (same token refused next request, menu 200) |
| 7 | Tests: screen cascade N + audit N+1, non-VIEW one, VIEW cascades, unknown grant 404, other tenant's role 404, module revoke | JUnit 7 + 2; TC-CORE-SEC-035..040 |
| 8 | DoD: code matches the entry; deviations in addendum + DEVIATIONS | table below; `[TM-G]` block |
| 9 | DoD: `mvn -q verify` green (ArchUnit, MigrationNamingTest, JaCoCo) | below |
| 10 | DoD: `docs/api-docs/sec/` regenerated from the running app; `check_completeness.py` clean; both endpoints present | regenerated again in review round 1 with the generator fix; 107/107 PASS |
| 11 | DoD: `core-test-plan.md` extended; api-verify run archived | `ec2542f`; run `261008000254` 156/156 (review round 1) |
| 12 | DoD: CHANGELOG `[Unreleased]` line | `[TM-G]` under Added |
| 13 | DoD: frontend items | not this package (F4); the addendum §8 lists what F4 needs |

12/12 backend items met; item 13 belongs to F4.

## Code ↔ addendum check

| Item | Addendum | Code | Result |
|---|---|---|---|
| Screen revoke path / method | `DELETE /api/v1/sec/roles/{id}/screens/{screenId}` | `@DeleteMapping("/{id}/screens/{screenId}")` under `/api/v1/sec/roles` | match |
| Action revoke path / method | `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` | `@DeleteMapping("/{id}/actions/{actionId}")` | match |
| Permission | `PERM_SEC_ROLES_UPDATE` | `@PreAuthorize(… SecPermissions.PERM_SEC_ROLES_UPDATE)` on both | match |
| Response DTOs | `ScreenGrantRevokeResponse { int revokedActionGrants }`, `ActionGrantRevokeResponse { int revokedActionGrants }` | same names, one `int` field each | match |
| Count semantics | screen: cascaded action grants; action: all removed, the requested one included | `actionGrants.size()`; `cascaded.size() + 1` | match |
| Errors and order | role → `SEC-404-ROLE`; grant → `SEC-404-GRANT`; 403 `SEC-403-FORBIDDEN`; 401 | `loadRole` then `findByRole…orElseThrow(SEC_404_GRANT)`; 403/401 by the shared handlers | match (api-docs list both 404s) |
| Status | 200 with body | `Status.SUCCESS` | match |
| Audit | `ACTION_REVOKED` per removed action, `SCREEN_REVOKED` for the screen; targetRef `<roleId>/<regPk>` | `appendAudit(EVENT_…)` as specified | match |
| Rules owner | `RoleScreenGrantDomain` / `RoleActionGrantDomain` `cascadeOnRevoke` | same | match |
| VIEW revoke keeps the screen grant | yes | screen grant not touched in `revokeAction` | match |
| Schema / migration | none | none | match |
| Module revoke | unchanged | `revokeModule` untouched | match |

## Verification output

- `mvn -q verify` from the worktree root after deleting every `target/` (first at `6b0d6b8`; again in review
  round 1 with the code at `73f1828`): exit 0 both times, same totals.
  Surefire totals: **erp-core 399 tests, 0 failures, 0 errors, 0 skipped**; **erp-app-reference 10 tests, 0
  failures, 0 errors, 0 skipped**. ArchUnit, enforcer and the JaCoCo gate passed (part of `verify`).
- HTTP suite, P-LIVE, full (all existing cases + the six new ones), app on port 18102, database `erp_tm_g`:
  review round 1 RUN `261008000254` **156/156 PASS** on a fresh `erp_tm_g`, archived as
  `docs/test-api/results/20261008T000213-P-LIVE.json` and `…-P-LIVE-report.md` (the first run, `26100723344C`, also
  156/156, was replaced). No regression. The other profiles (22 cases) were not part of this package's run.
- `check_completeness.py`: `missing=0 duplicated=0 stale=0`, `RESULT: PASS`, sec=42, 107 operations.
- `generate_all.py --function check`: SEC, TENANT, MDL, SEQUENCE and REPORT pass. FILE and NOTIF fail on
  permissions (`CONTROLLER_NOT_MATCHED`), CU on unique-constraints, AUDIT on business-errors, and APP on permissions
  and business-errors. These are the documented generator limitations in `docs/api-docs/README.md`, all
  pre-existing.
- The app was stopped and `erp_tm_g` dropped.

## Skills checked

`gov-enforce-backend-contract` (85 checks):
- Domain: new methods take plain arguments and use no repository.
- Repository: JOIN FETCH; every new method has a caller.
- DTO: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`, `@Schema` bilingual with an example.
- Mapper: pure.
- Service: `@Transactional`, `@PreAuthorize`, `ServiceResult`, `LocalizedException` with constants, `log.info` on
  writes, no inlined rule.
- Controller: thin, `@Operation`, `craftResponse`. A.6.5 is deviated deliberately (see Decisions).

Also used:
- `build-create-entity` (Domain section), `build-create-repository`, `build-create-dto`, `build-create-mapper`,
  `build-create-service`, `build-create-controller`.
- `gov-enforce-error-handling`: existing codes only, both bundles already hold them.
- `gov-validate-backend-feature`: no entity, caching or migration change. Stages 0–1 are satisfied by the existing
  grant feature. Stage 2 has only the A.6.5 note. Stage 3: no caching, cross-module or event change. Stage 4
  compiles. Verdict APPROVED WITH NOTES (A.6.5 deviation, recorded).
- `api-verify` (translation of the plan cases).
- `/generate-api-docs`.

## Notes for later steps

- Frontend F4 can build now: `docs/api-docs/sec/endpoints/role-grants.md` has both endpoints. The addendum §8 lists
  the dialog, the VIEW warning and the super-role hint.
- Shared files touched:
  - `docs/CHANGELOG.md`: one Added bullet.
  - `docs/DEVIATIONS.md`: `[TM-G]` block.
  - `governance/README.md`: line 26 "SEC 13" → "SEC 14". Package A may edit the same line for the TENANT ADR count,
    so expect a one-line merge.
  - `docs/test-api/core-test-plan.md` and `core_api_verify.py`: SEC-035..040 and the phase-6 order.
  - `docs/test-api/core_verify_report.py`: optional `out` parameter and per-run instance/code text.
  - `docs/api-docs/**`.
- The next TC-CORE-SEC number is **041**.
- Later packages that regenerate api-docs: run the fixed generator from this branch (or after it merges); with the
  old generator the 28 helper-raised rows disappear again.
- Next free SEC ids: see "Review round 1" below. ENT-SEC-014 was issued historically (SEC v2
  `SEC_SVC_ACCOUNT_CRED`), so the next ENT is 015, not 014 as the brief says.
- To archive a package's HTTP run: `--instance "…" --code-under-test "…"`, then
  `--report <json> --report-out docs/test-api/results/<timestamp>-P-LIVE-report.md`.
- Any HTTP case that adds users must not add them to tenant A: REPORT-003 and TENANT-014 assert A's exact staff set.

## Review round 1

| # | Finding | Resolution | Commit |
|---|---|---|---|
| 1 | REQ-SEC-036 / AC-SEC-036 already cited for logout (`AuthService.java:130`, `SecLogoutIntegrationTest`) | Highest ids ever issued, read from the current tree, this repository's full history and `governance-shared`'s full history (read only): REQ-SEC-079, AC-SEC-085, RULE-SEC-053. G renumbered: REQ-SEC-036 → 080, REQ-SEC-037 → 081, AC-SEC-036 → 086, AC-SEC-037 → 087, RULE-SEC-008 → 054, RULE-SEC-009 → 055, in the addendum, registry, policies, ADR-SEC-062, code Javadoc, tests, test plan, CHANGELOG, DEVIATIONS and this report. The logout comments were left as they are: the current `srs-sec.md` has no logout requirement to repoint them to (they cite the pre-vendoring v1 ids). DEVIATIONS `[TM-G]` line added. | `73f1828` |
| 2 | api-docs lost 29 business-error rows | Generator fixed at the source (`extractors/business_error_extractor.py`): helpers that throw a code their caller supplies are read from source and the code is bound at the caller's site with the helper's Status. This covers a parameter (`DomainRules.assertUnique` / `assertNotBlank`, `OwnedLookups.read`), a constructor-bound field (`StatusTransitions.assertAllowed`), and a constant of an instantiated shared class (`InstantFieldValueConverter`). The code also counts as a throw site and gets its Status in `index.md` (first site in path order, the same rule direct throws follow). Tests: `tests/test_helper_codes.py` (6), fixtures `tests/fixtures/helpers/`; the suite has 65 tests, OK. The pre-existing `test_security_extractor` UTF-8 failure on a cp1252 default was fixed too. Regenerated: **28/29 rows restored** (list below). The 29th, `FileService.sha256Hex` 500 `INTERNAL_ERROR`, is a genuine change: `TokenHasher.sha256Hex` throws `IllegalStateException`, which the shared handler answers as 500 `INTERNAL_ERROR`. Against main's api-docs, what remains differs only by: the two new endpoints plus their counts (`SEC-404-ROLE` 8→10, `SEC-404-GRANT` 1→3, `SEC-403-FORBIDDEN` 31→33 endpoints, catalog rows); the `NOT_FOUND` framework row (1.2.0, step 15); the walked-method lists of FILE/NOTIF endpoints (private `isBlank` / `sha256Hex` moved to `com.erp.common`); that sha256Hex row; and README counts. `README.md` Generated/Generator rows and DEVIATIONS line corrected. | `2e7d1b1`, `4242926` |
| 3 | After a VIEW revoke the screen stays in the menu | srs-sec 1.3.0 addendum §6 (new note) and §8 (frontend), ADR-SEC-062 Consequences: the menu is built from screen grants alone (`ScreenRegistryRepository.findEffectiveScreensForUser`), so the screen stays listed while its endpoints answer 403; **to remove the menu entry, revoke the screen**. TC-CORE-SEC-037 now asserts the menu still lists `SEC_ROLES`. | `73f1828` |
| 4 | `revokeScreen` loaded all the role's action grants | Uses `findAllByRoleAndScreen(roleId, screenId)`; `RoleScreenGrantDomain.cascadeOnRevoke` still decides the set. | `73f1828` |

### Next free ids per SEC prefix

Highest ever issued, read from the current tree, `git log -p --all` of this repository and of `governance-shared`:

| Prefix | Highest issued | Next free |
|---|---|---|
| REQ-SEC | 081 (this package; pre-vendoring 079) | **REQ-SEC-082** |
| AC-SEC | 087 (this package; pre-vendoring 085) | **AC-SEC-088** |
| RULE-SEC | 055 (this package; pre-vendoring 053) | **RULE-SEC-056** |
| ENT-SEC | 014 (v2 `SEC_SVC_ACCOUNT_CRED`, ADR-SEC-035) | **ENT-SEC-015** |
| SCR-REQ-SEC | 010 | **SCR-REQ-SEC-011** |
| DBF-SEC | 116 | **DBF-SEC-117** |
| XM-SEC | 005 | **XM-SEC-006** |
| QR-SEC | 054 | **QR-SEC-055** |
| API-SEC | 050 | **API-SEC-051** |
| ADR-SEC | 062 (this package) | **ADR-SEC-063** |
| TC-CORE-SEC | 040 | **TC-CORE-SEC-041** |

### Restored business-error rows (28)

| HTTP | Code | Constant | Site |
|---|---|---|---|
| 400 BAD_REQUEST | `APP_CONFIGURATION_FIELDS_REQUIRED` | APP_CONFIGURATION_FIELDS_REQUIRED | AppConfigurationDomain.create |
| 409 CONFLICT | `APP_CONFIGURATION_KEY_DUPLICATE` | APP_CONFIGURATION_KEY_DUPLICATE | AppConfigurationDomain.create |
| 400 BAD_REQUEST | `APP_CONFIGURATION_FIELDS_REQUIRED` | APP_CONFIGURATION_FIELDS_REQUIRED | AppConfigurationDomain.assertCanUpdate |
| 409 CONFLICT | `FILE_CATEGORY_CODE_DUPLICATE` | FILE_CATEGORY_CODE_DUPLICATE | FileCategoryDomain.create |
| 422 UNPROCESSABLE_CONTENT | `FILE_DOCUMENT_INVALID_TRANSITION` | FILE_DOCUMENT_INVALID_TRANSITION | FileDocumentDomain.assertCanTransitionTo |
| 404 NOT_FOUND | `FILE_LOOKUP_KEY_UNKNOWN` | FILE_LOOKUP_KEY_UNKNOWN | FileLookupService.get |
| 409 CONFLICT | `MDL-409-TYPE-DUP` | MDL_409_TYPE_DUP | LookupTypeDomain.create |
| 409 CONFLICT | `MDL-409-VALUE-DUP` | MDL_409_VALUE_DUP | LookupValueDomain.create |
| 400 BAD_REQUEST | `NOTIF_CHANNEL_TYPE_REQUIRED` | NOTIF_CHANNEL_TYPE_REQUIRED | NotificationChannelConfigDomain.create |
| 409 CONFLICT | `NOTIF_CHANNEL_CONFIG_DUPLICATE` | NOTIF_CHANNEL_CONFIG_DUPLICATE | NotificationChannelConfigDomain.create |
| 422 UNPROCESSABLE_CONTENT | `NOTIF_LOG_INVALID_TRANSITION` | NOTIF_LOG_INVALID_TRANSITION | NotificationLogDomain.assertCanTransitionTo |
| 404 NOT_FOUND | `NOTIF_LOOKUP_KEY_UNKNOWN` | NOTIF_LOOKUP_KEY_UNKNOWN | NotificationLookupService.get |
| 400 BAD_REQUEST | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | NOTIF_TEMPLATE_BILINGUAL_REQUIRED | NotificationTemplateDomain.create |
| 409 CONFLICT | `NOTIF_TEMPLATE_CODE_DUPLICATE` | NOTIF_TEMPLATE_CODE_DUPLICATE | NotificationTemplateDomain.create |
| 400 BAD_REQUEST | `NOTIF_TEMPLATE_BILINGUAL_REQUIRED` | NOTIF_TEMPLATE_BILINGUAL_REQUIRED | NotificationTemplateDomain.assertBilingualBody |
| 400 BAD_REQUEST | `VALIDATION_ERROR` | VALIDATION_ERROR | SecSearchSupport.instantFieldConverter |
| 409 CONFLICT | `SEC-409-SIGNUP-DUP` | SEC_409_SIGNUP_DUP | SignupRequestDomain.create |
| 409 CONFLICT | `CUSTOMER_EMAIL_TAKEN` | CUSTOMER_EMAIL_TAKEN | UserDomain.createCustomer |
| 409 CONFLICT | `SEC-409-ACTION-DUP` | SEC_409_ACTION_DUP | ActionRegistryDomain.create |
| 409 CONFLICT | `SEC-409-MODULE-DUP` | SEC_409_MODULE_DUP | ModuleRegistryDomain.create |
| 409 CONFLICT | `SEC-409-SCREEN-DUP` | SEC_409_SCREEN_DUP | ScreenRegistryDomain.create |
| 409 CONFLICT | `SEC-409-GRANT-DUP` | SEC_409_GRANT_DUP | RoleActionGrantDomain.create |
| 409 CONFLICT | `SEC-409-GRANT-DUP` | SEC_409_GRANT_DUP | RoleModuleGrantDomain.create |
| 409 CONFLICT | `SEC-409-GRANT-DUP` | SEC_409_GRANT_DUP | RoleScreenGrantDomain.create |
| 409 CONFLICT | `SEC-409-ROLE-DUP` | SEC_409_ROLE_DUP | RoleDomain.create |
| 400 BAD_REQUEST | `VALIDATION_ERROR` | VALIDATION_ERROR | SecSearchSupport.instantFieldConverter |
| 409 CONFLICT | `NUMBER_SERIES_CODE_DUPLICATE` | NUMBER_SERIES_CODE_DUPLICATE | NumberSeriesDomain.create |
| 409 CONFLICT | `TENANT_CODE_DUPLICATE` | TENANT_CODE_DUPLICATE | TenantDomain.create |

### Verification (round 1)

- `mvn -q verify` (clean `target/`, code at `73f1828`): exit 0. erp-core 399 tests, 0 failures, 0 errors,
  0 skipped; erp-app-reference 10 tests, 0 failures, 0 errors, 0 skipped.
- api-doc-generator unit tests: 65, OK.
- HTTP P-LIVE, full suite, fresh `erp_tm_g` on 18102: RUN `261008000254` 156/156 PASS, archived as
  `docs/test-api/results/20261008T000213-P-LIVE.json` + `…-P-LIVE-report.md`. The first run's files were removed.
- `check_completeness.py --base http://localhost:18102`: 107 operations, missing=0 duplicated=0 stale=0, PASS.
- `generate_all.py --function check`: SEC, TENANT, MDL, SEQUENCE and REPORT pass. FILE (permissions,
  `CONTROLLER_NOT_MATCHED`), NOTIF (same), CU (unique-constraints), AUDIT (business-errors) and APP (permissions,
  business-errors) fail; these are the five documented generator limitations in `docs/api-docs/README.md`,
  unchanged.
- App stopped, `erp_tm_g` dropped.

