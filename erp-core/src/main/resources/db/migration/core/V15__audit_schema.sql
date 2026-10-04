-- ============================================================
-- V15 — Generic audit log (erp-core step 10). Additive only (README: no rename, drop or type change).
-- Target: POSTGRESQL_16
--
-- CORE_AUDIT_EVENT — one cross-module, tenant-scoped "who changed what, when" store, written by
-- com.erp.audit.crossmodule.AuditApi (explicit calls) and by the opt-in @Audited Hibernate listener
-- (field-level CHANGES). SEC_AUDIT_LOG stays as it is (security-specific events); SEC additionally
-- writes LOGIN / LOGOUT / PASSWORD_RESET here, so one timeline exists. No data migration.
--
-- The read permission AUDIT:EVENT:READ is NOT seeded here: com.erp.audit.permission.AuditPermissions
-- contributes it and PermissionCatalogSynchronizer upserts it at startup (every super role holds it).
-- ============================================================

CREATE SEQUENCE SEQ_CORE_AUDIT_EVENT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE CORE_AUDIT_EVENT (
  ID             BIGINT         NOT NULL,
  TENANT_ID      BIGINT         NOT NULL,
  OCCURRED_AT    TIMESTAMP      NOT NULL,
  ACTOR          VARCHAR(100)   NOT NULL,
  ACTOR_REALM    VARCHAR(16)    NOT NULL,
  ACTOR_USER_ID  BIGINT,
  ACTION         VARCHAR(64)    NOT NULL,
  ENTITY_TYPE    VARCHAR(128),
  ENTITY_ID      VARCHAR(64),
  SUMMARY_AR     VARCHAR(1000),
  SUMMARY_EN     VARCHAR(1000),
  CHANGES        JSONB,
  IP             VARCHAR(64),
  USER_AGENT     VARCHAR(256),
  REFERENCE      VARCHAR(100),
  CREATED_BY     VARCHAR(100),
  CREATED_AT     TIMESTAMP,
  UPDATED_BY     VARCHAR(100),
  UPDATED_AT     TIMESTAMP,
  VERSION        BIGINT         DEFAULT 0 NOT NULL
);

ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT PK_CORE_AUDIT_EVENT PRIMARY KEY (ID);
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT FK_CORE_AUDIT_EVENT_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT CHK_CORE_AUDIT_EVENT_ACTION CHECK (ACTION ~ '^[A-Z_]{3,64}$');
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT CHK_CORE_AUDIT_EVENT_REALM CHECK (ACTOR_REALM IN ('STAFF', 'CUSTOMER', 'SYSTEM'));

CREATE INDEX IDX_CORE_AUDIT_EVENT_TENANT   ON CORE_AUDIT_EVENT (TENANT_ID);
CREATE INDEX IDX_CORE_AUDIT_EVENT_ENTITY   ON CORE_AUDIT_EVENT (TENANT_ID, ENTITY_TYPE, ENTITY_ID);
CREATE INDEX IDX_CORE_AUDIT_EVENT_OCCURRED ON CORE_AUDIT_EVENT (TENANT_ID, OCCURRED_AT);

COMMENT ON TABLE CORE_AUDIT_EVENT IS 'Generic tenant-scoped audit trail (erp-core step 10): explicit AuditApi.record calls and @Audited entity changes.';
COMMENT ON COLUMN CORE_AUDIT_EVENT.ACTION IS 'CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN, LOGOUT, PASSWORD_RESET, ... (free, validated ^[A-Z_]{3,64}$).';
COMMENT ON COLUMN CORE_AUDIT_EVENT.CHANGES IS 'JSON array of {field, old, new}; sensitive fields (password, secret, token, hash, ...) are never written.';
COMMENT ON COLUMN CORE_AUDIT_EVENT.REFERENCE IS 'Optional correlation id chosen by the caller.';
