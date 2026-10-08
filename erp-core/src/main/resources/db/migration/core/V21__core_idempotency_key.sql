-- ============================================================
-- V21 — Idempotency keys — tenant-maturity plan package C4 (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V2..V20 | additive only (README: no rename, drop or type change)
--
-- Written from governance/analysis/modules/TENANT/P2/db-script-tenant.md → "Implementation Addendum —
-- erp-core 1.3.0" (package C4: CORE_IDEMPOTENCY_KEY, DBF-TENANT-045, ADR-TENANT-003). The plan expected V20; the
-- number follows the execution order (docs/DEVIATIONS.md [TM-C4]).
--
-- CORE_IDEMPOTENCY_KEY — the stored answers of com.erp.common.idempotency (header Idempotency-Key; first consumer
-- POST /api/v1/platform/tenants). Tenant-scoped (entity IdempotencyKey extends AuditableEntity), so besides the
-- plan's nine columns it carries the convention's audit columns; CREATED_BY is the key's owner. A row is claimed and
-- answered inside the transaction of the work it protects; only 2xx answers are ever committed. Rows older than
-- erp.core.idempotency.retention (24 h) are ignored and purged by IdempotencyKeyRetentionJob. No seed.
-- ============================================================

CREATE SEQUENCE SEQ_CORE_IDEMPOTENCY_KEY START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE CORE_IDEMPOTENCY_KEY (
  ID               BIGINT         NOT NULL,
  TENANT_ID        BIGINT         NOT NULL,
  IDEMPOTENCY_KEY  VARCHAR(64)    NOT NULL,
  ENDPOINT         VARCHAR(200)   NOT NULL,
  REQUEST_HASH     VARCHAR(64)    NOT NULL,
  RESPONSE_STATUS  INT            NOT NULL,
  RESPONSE_BODY    TEXT,
  CREATED_BY       VARCHAR(100)   NOT NULL,
  CREATED_AT       TIMESTAMPTZ    NOT NULL DEFAULT now(),
  UPDATED_BY       VARCHAR(100),
  UPDATED_AT       TIMESTAMPTZ,
  VERSION          BIGINT         NOT NULL DEFAULT 0
);

ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT PK_CORE_IDEMPOTENCY_KEY PRIMARY KEY (ID);
ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT FK_CORE_IDEMPOTENCY_KEY_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT UQ_CORE_IDEMPOTENCY_KEY UNIQUE (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT);

CREATE INDEX IDX_CORE_IDEMPOTENCY_KEY_TENANT     ON CORE_IDEMPOTENCY_KEY (TENANT_ID);
CREATE INDEX IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT ON CORE_IDEMPOTENCY_KEY (CREATED_AT);

COMMENT ON TABLE CORE_IDEMPOTENCY_KEY IS 'Stored answers of requests sent with an Idempotency-Key (com.erp.common.idempotency, tenant-maturity C4, ADR-TENANT-003); kept erp.core.idempotency.retention (24 h).';
COMMENT ON COLUMN CORE_IDEMPOTENCY_KEY.IDEMPOTENCY_KEY IS 'The client''s key, ^[A-Za-z0-9._:-]{1,64}$ (RULE-TENANT-025); unique per tenant and endpoint.';
COMMENT ON COLUMN CORE_IDEMPOTENCY_KEY.ENDPOINT IS 'The consumer''s constant endpoint id, e.g. POST /api/v1/platform/tenants.';
COMMENT ON COLUMN CORE_IDEMPOTENCY_KEY.REQUEST_HASH IS 'Lower-case hex HMAC-SHA256 of the canonical JSON request body (keyed; never the body itself).';
COMMENT ON COLUMN CORE_IDEMPOTENCY_KEY.RESPONSE_STATUS IS 'HTTP status of the stored answer (2xx once committed).';
COMMENT ON COLUMN CORE_IDEMPOTENCY_KEY.RESPONSE_BODY IS 'The JSON envelope as answered, replayed with Idempotent-Replayed: true.';
