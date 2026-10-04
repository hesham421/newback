# Local Dev Commands — Backend

Quick reference for running, resetting, and troubleshooting the ERP backend locally.

> **Prereqs**
> - JDK 25 at `~/opt/jdk-25-temurin` → `export JAVA_HOME=/Users/ezzat/opt/jdk-25-temurin/Contents/Home`
> - Docker containers running (Postgres on 5432, Redis on 6379)
> - App listens on **port 7272**, context path `/`
> - Maven does **not** auto-load `.env` — you must export `SPRING_PROFILES_ACTIVE=dev` (or pass `-Dspring-boot.run.profiles=dev`) or the datasource driver won't resolve.

---

## Run the backend

```bash
cd "/Users/ezzat/my project/backend"
export JAVA_HOME=/Users/ezzat/opt/jdk-25-temurin/Contents/Home
set -a && source .env && set +a          # loads DB_URL, SPRING_PROFILES_ACTIVE=dev, etc.
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Success looks like:
```
Tomcat started on port 7272 (http) with context path '/'
Started ErpMainApplication in ~3s
```

Run in background & watch the log:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/boot.log 2>&1 &
tail -f /tmp/boot.log
```

---

## Reset / restore the database (Flyway-native)

Wipes all data and lets Flyway re-apply every migration on next app start.
**Does NOT delete the Docker volume** (unlike `docker compose down -v`, which is forbidden).

```bash
# 1. Drop & recreate the schema
docker exec erp-backend-postgres psql -U postgres -d erp_db -c \
  "DROP SCHEMA public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO postgres; GRANT ALL ON SCHEMA public TO public;"

# 2. Start the app — Flyway re-applies all migrations from an empty schema and reseeds
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

After restart you should see `Successfully applied N migrations to schema "public"` and it lands at the latest `vN`.

---

## Kill a stale backend (port 7272 already in use)

```bash
lsof -ti tcp:7272 | xargs -r kill -9      # kill whatever holds the port
lsof -ti tcp:7272 || echo free            # verify it's free
```

> Always check `lsof -ti tcp:7272` **before** launching — a lingering instance causes
> `APPLICATION FAILED TO START — Port 7272 was already in use`.

---

## Inspect the database

```bash
# Flyway migration history + success flags
docker exec erp-backend-postgres psql -U postgres -d erp_db -c \
  "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

# List all tables
docker exec erp-backend-postgres psql -U postgres -d erp_db -c "\dt"

# Interactive psql shell
docker exec -it erp-backend-postgres psql -U postgres -d erp_db
```

---

## Docker containers

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'   # what's running

# Start Postgres + Redis (named volumes survive `down` and reboots)
docker compose --env-file .env -f docker/docker-compose.yml up -d

docker compose -f docker/docker-compose.yml down                 # stop, KEEP data
# docker compose down -v   ← NEVER: deletes the database volume
```

---

## Migrations

```bash
# List migration files
ls src/main/resources/db/migration/

# Highest current version (next file is V<N+1>)
ls src/main/resources/db/migration/ | grep -oE '^V[0-9]+' | sort -t V -k2 -n | tail -1
```

> Never edit an already-applied migration (Flyway checksums them) — fix forward with a new `V<N+1>__*.sql`.

---

## Build / test (no run)

```bash
export JAVA_HOME=/Users/ezzat/opt/jdk-25-temurin/Contents/Home
mvn clean compile        # compile only
mvn test                 # run tests
mvn clean package -DskipTests
```

---

## Common startup failures

| Symptom | Cause | Fix |
|---|---|---|
| `Failed to determine a suitable driver class` | `dev` profile not active → no datasource URL | export `SPRING_PROFILES_ACTIVE=dev` or `-Dspring-boot.run.profiles=dev` |
| `Port 7272 was already in use` | stale backend instance | `lsof -ti tcp:7272 \| xargs -r kill -9` |
| Enforcer fails at `validate` naming a JDK | wrong JDK | point `JAVA_HOME` at JDK 25 |
