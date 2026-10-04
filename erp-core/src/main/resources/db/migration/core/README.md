# Flyway migrations — `classpath:db/migration`

Spring Boot's `FlywayAutoConfiguration` runs every `V{n}__{description}.sql`
file in this folder, in order, against the configured datasource at
application startup (`spring.flyway.locations=classpath:db/migration`).

## The chain (cleaned in erp-core step 01)

The chain is contiguous, `V1..V20`, and contains only the core modules
(`cu`, `sec`, `notif`, `file`, `mdl`). The former `fin` module's migrations
were deleted outright (not reverted by a later migration) and the remaining
files were renumbered in place, preserving their relative order. This was
possible because erp-core is a new project with no deployed database: every
environment is created from scratch, so Flyway checksums of the old numbers
are irrelevant.

| Version | File | Module |
|---|---|---|
| V1  | `V1__cu_app_configuration_schema.sql`        | cu |
| V2  | `V2__sec_security_schema.sql`                | sec (legacy schema, dropped by V12) |
| V3  | `V3__sec_security_seed.sql`                  | sec (legacy) |
| V4  | `V4__notif_schema.sql`                       | notif |
| V5  | `V5__notif_security_seed.sql`                | notif (legacy sec rows) |
| V6  | `V6__file_schema.sql`                        | file |
| V7  | `V7__file_security_seed.sql`                 | file (legacy sec rows) |
| V8  | `V8__sec_bootstrap_admin_user.sql`           | sec (legacy) |
| V9  | `V9__notif_email_channel_seed.sql`           | notif |
| V10 | `V10__notif_email_templates_action_link.sql` | notif |
| V11 | `V11__cu_security_seed.sql`                  | cu (legacy sec rows) |
| V12 | `V12__drop_legacy_security_schema.sql`       | sec |
| V13 | `V13__sec_schema.sql`                        | sec (current schema) |
| V14 | `V14__sec_security_seed.sql`                 | sec |
| V15 | `V15__mdl_sequences.sql`                     | mdl |
| V16 | `V16__mdl_security_seed.sql`                 | mdl |
| V17 | `V17__notif_file_lookup_data_migration.sql`  | notif / file → mdl lookups |
| V18 | `V18__mdl_role_grants.sql`                   | mdl |
| V19 | `V19__cu_notif_file_security_seed.sql`       | cu / notif / file |
| V20 | `V20__notif_password_reset_copy.sql`         | notif |

Comments inside the files predate the renumbering and may cite the old
version numbers (e.g. "V16 schema" now means `V13__sec_schema.sql`). Full
file-name references were updated; bare `V<n>` mentions were left as written.
Old → new mapping: V1–V3 unchanged; V6→V4, V7→V5, V8→V6, V9→V7, V10→V8,
V11→V9, V12→V10, V13→V11, V14→V12, V16→V13, V17→V14, V18→V15, V19→V16,
V20→V17, V21→V18, V31→V19, V32→V20 (V4, V5, V15 never existed; V22–V30 and
V33–V41 were `fin` and are deleted).

## Rules

- One forward-only `V{n}__{module}_{summary}.sql` per logical change; derive
  `n` as the current highest version + 1. Never edit a migration after it has
  run anywhere (Flyway checksums them).
- Do not add a manual baseline; `spring.flyway.baseline-on-migrate=true`
  already lets Flyway adopt a non-empty database.
