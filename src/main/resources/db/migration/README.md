# Flyway migrations — `classpath:db/migration`

Spring Boot's `FlywayAutoConfiguration` runs every `V{n}__{description}.sql`
file in this folder, in order, against the configured datasource at
application startup (`spring.flyway.locations=classpath:db/migration`).

This is a clean start: there are no migrations yet. Each governed module adds
its own versioned migration(s) here when its `db-script.md` (P2) is
implemented — one forward-only `V{n}__{module}_{summary}.sql` per change, never
edited after it has run anywhere (Flyway checksums them). Do not add a manual
baseline; `spring.flyway.baseline-on-migrate=true` already lets Flyway adopt a
non-empty database, and an empty folder simply means "nothing to migrate yet".
