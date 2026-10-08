# ADR-TENANT-003 — Idempotency keys stored in `CORE_IDEMPOTENCY_KEY`; first consumer is tenant create

Module  : TENANT (mechanism owned by `com.erp.common`)     Version : erp-core 1.3.0 (tenant-maturity plan, package C.4)     Stage raised : P2 (Database) — before the code
Status  : ACCEPTED (erp-core 1.3.0, package C4; written PROPOSED-before-code in the analysis commit df7e4e7, accepted
          after the code check)

## Context
`POST /api/v1/platform/tenants` is the platform's most expensive and least repeatable call: it inserts the
`CORE_TENANT` row and runs every `TenantProvisioningContributor` in one transaction
(`erp-core/src/main/java/com/erp/tenant/service/TenantService.java`, `create`; POL-TENANT-004). A client that
times out cannot tell whether the tenant was created; retrying today either answers 409 `TENANT_CODE_DUPLICATE`
(the first call committed) or creates the tenant twice under different codes (the operator changed the code to get
past the 409). The plan (§5 C.4, item 13) asks for an idempotent create: a retry with the same key replays the first
answer. Three places were available for the stored answers:
- **In memory** (per node) — lost on restart, wrong across nodes; the download-token store's in-memory default
  exists only because a token is short-lived and single-use.
- **Redis** (optional, like the download-token store) — a second code path when Redis is absent, and the platform's
  only mandatory store is PostgreSQL; a Redis entry cannot commit with the tenant it describes.
- **A core table** — one row per (tenant, key, endpoint), purged by a job, visible to every node, written inside
  the same transaction as the work it protects.
The reference analysis (`reference-snapshot.md`, package C4) drafted this ADR and left one point open: the plan's
nine-column table versus the `AuditableEntity` convention.

## Decision
A core table `CORE_IDEMPOTENCY_KEY` (`V21__core_idempotency_key.sql`; the plan expected V20) behind the mechanism
`com.erp.common.idempotency`; v1 applies it to `POST /api/v1/platform/tenants` only.
- **Table.** The plan's columns plus the tenant-scoped convention's audit columns, because the entity
  (`IdempotencyKey`) extends `AuditableEntity`: `ID`, `TENANT_ID` (FK, `@TenantId`), `IDEMPOTENCY_KEY VARCHAR(64)`,
  `ENDPOINT VARCHAR(200)`, `REQUEST_HASH VARCHAR(64)`, `RESPONSE_STATUS INT`, `RESPONSE_BODY TEXT`,
  `CREATED_BY VARCHAR(100) NOT NULL` (the key's owner), `CREATED_AT TIMESTAMPTZ`, `UPDATED_BY`, `UPDATED_AT`,
  `VERSION`; `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)`, `SEQ_CORE_IDEMPOTENCY_KEY`,
  `IDX_CORE_IDEMPOTENCY_KEY_TENANT`, `IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT`.
- **Header.** `Idempotency-Key`, optional, `^[A-Za-z0-9._:-]{1,64}$` (else 400 `IDEMPOTENCY_KEY_INVALID`); without it,
  or with `erp.core.idempotency.enabled=false`, the 1.2.0 behaviour is unchanged.
- **One transaction.** Without a live row, the key's row is inserted first (the claim), the operation runs in the
  same transaction (the `@Transactional` service joins it), and a 2xx answer is stored in that row before the commit.
  A non-2xx answer or an exception rolls everything back: **only successful answers are stored**, and a failed create
  may be retried with the same key.
- **Replay or conflict.** A live row with the same request hash and the same `CREATED_BY` is answered with its stored
  status and envelope and `Idempotent-Replayed: true`; the operation does not run. Any other live row → 409
  `IDEMPOTENCY_KEY_CONFLICT` (another body, or another user).
- **Concurrency.** Two simultaneous first requests with one key: the second one's claim insert waits on
  `UQ_CORE_IDEMPOTENCY_KEY` until the first transaction ends, fails on the unique key if it committed, and then
  reads and replays it (or runs itself if the first rolled back). No "in progress" row or code exists.
- **Hash.** Lower-case hex HMAC-SHA256 of the canonical JSON of the bound request body (properties and map keys
  sorted), keyed by a key derived from `erp.core.security.jwt.secret`. Whitespace and property order never make a new
  request; headers are not part of it.
- **Retention.** 24 h (`erp.core.idempotency.retention`): an older row is treated as unused at lookup (deleted and
  re-claimed), and `IdempotencyKeyRetentionJob.run()` purges such rows tenant by tenant on the `AuditRetentionJob`
  pattern (`erp.core.idempotency.retention-cron`, default off; core never schedules).

Reasons:
1. **One mandatory store, one commit.** PostgreSQL is the only store every deployment has; the row commits with the
   tenant it protects, so a crash between the provisioning and the key write cannot leave a replayable answer for a
   tenant that does not exist, nor a tenant without its answer.
2. **The unique index is the lock.** Serialising same-key requests on `UQ_CORE_IDEMPOTENCY_KEY` needs no lease, no
   "in progress" status and no clean-up of a request that died half-way (its transaction rolls back).
3. **Tenant-scoped like every core row.** The key is unique per (tenant, endpoint); the row is written inside the
   operator's tenant (PLATFORM) by Hibernate's `@TenantId`, so `TenantScopedEntityTest` needs no new global entity and
   the table follows `db/migration/core/README.md`.
4. **Generic without a second consumer.** The table carries `ENDPOINT`, so a later consumer adds no column; the
   mechanism imports nothing from `com.erp.tenant` (or any module).
5. **No password verifier at rest.** The tenant-create body contains `adminPassword`; a plain SHA-256 of a body whose
   other fields are known would let anyone reading the table test guesses offline at full speed. The keyed HMAC is
   not checkable without the server secret. The stored response (`TenantResponse`) contains no password.
6. **Bounded growth.** 24 h keeps the table at about one row per create per day.

## Alternatives rejected
- **Store every answer, failures included** (the usual HTTP-API practice): here a failed create leaves nothing behind
  (one transaction), so replaying a 4xx/5xx would only stop a corrected retry from working.
- **An "in progress" row committed before the work** (answered 409 `IDEMPOTENCY_KEY_IN_PROGRESS` to a concurrent
  request): needs its own commit, a lease and a recovery for a crashed request; the unique-index wait gives the
  second request the real answer instead.
- **Hash of the raw bytes**: a client re-serialising the same object (another key order, whitespace) would get a 409.
- **One namespace for every user of the tenant** (the reference draft: "two platform operators share the
  namespace"): the unique constraint keeps that namespace, but replaying operator A's stored answer to operator B
  would hand out a response B never asked for; B gets 409 instead.

## Consequences
- The replayed `data` is the stored JSON as it was answered; a later change of `TenantResponse` is not reflected in a
  replay of an older key (24 h window, acceptable).
- A same-key request may wait for the duration of the first request's transaction (a provisioning: about a second).
- A replay is served before the service's `@PreAuthorize`, but only to the user who stored it (who held the authority
  then); a replay writes no audit row and publishes no event.
- A consumer's operation must run in the caller's transaction; work it commits in its own `REQUIRES_NEW`
  transactions would not be atomic with the key — checked when a second consumer is added.
- Rotating `erp.core.security.jwt.secret` changes the hash key: a retry within 24 h after a rotation answers 409.
- The new codes are answered through the common helper, which the api-doc generator does not walk: the create's
  `@Operation` description names them (the header itself is a documented parameter).
- `TenantSchemaIntegrationTest`'s counts move with V21: 23 discriminator columns, 15 tenant-leading unique
  constraints, 22 tenant-aware entities.

## Traces
Accepted after the code check of package C4 (the check commit follows the code commits 0a5ad05, 9b39b57).
ENT-TENANT-001 (FK target) · REQ-TENANT-001, REQ-TENANT-036 · RULE-TENANT-010, RULE-TENANT-011, RULE-TENANT-025,
RULE-TENANT-026 · POL-TENANT-004, POL-TENANT-016 · DBF-TENANT-045 · plan §5 C.4, §9, §11
