-- ============================================================
-- V13 — Notification Service (NOTIF): asynchronous, event-driven delivery + the IN_APP inbox
--       (erp-core step 08). Additive only (README: no rename, drop or type change).
-- Target: POSTGRESQL_16
--
-- 1. NOTIF_LOG gains the delivery-queue columns: ATTEMPTS, NEXT_ATTEMPT_AT, LAST_ERROR, and
--    VARIABLES_JSON (the dispatch variables the asynchronous worker renders the message from; the
--    worker clears it as soon as the row reaches a final status, so link tokens do not stay at rest).
--    New NOTIFICATION_STATUS_ID values (no CHECK constraint exists on the column): QUEUED (waiting for
--    the asynchronous worker) and SKIPPED_NO_PROVIDER (no ChannelProvider handles the channel).
-- 2. NOTIF_INBOX — the IN_APP channel's real implementation (tenant-scoped, README tenant rules).
-- 3. Seeds, for EVERY existing tenant (new tenants copy PLATFORM through the MDL/NOTIF provisioning
--    contributors): NOTIF_STATUS values QUEUED and SKIPPED_NO_PROVIDER, NOTIF_CHANNEL value IN_APP,
--    and an enabled IN_APP channel configuration.
-- ============================================================

-- ------------------------------------------------------------
-- 1. NOTIF_LOG — delivery-queue columns
-- ------------------------------------------------------------
ALTER TABLE NOTIF_LOG ADD COLUMN ATTEMPTS        INT       DEFAULT 0 NOT NULL;
ALTER TABLE NOTIF_LOG ADD COLUMN NEXT_ATTEMPT_AT TIMESTAMP;
ALTER TABLE NOTIF_LOG ADD COLUMN LAST_ERROR      TEXT;
ALTER TABLE NOTIF_LOG ADD COLUMN VARIABLES_JSON  TEXT;

COMMENT ON COLUMN NOTIF_LOG.ATTEMPTS IS 'Delivery attempts made by the asynchronous worker (<= erp.core.notif.retry.max-attempts, default 5).';
COMMENT ON COLUMN NOTIF_LOG.NEXT_ATTEMPT_AT IS 'When the next retry is due while QUEUED; NULL otherwise. Also the staleness clock of NotificationRequeueJob.';
COMMENT ON COLUMN NOTIF_LOG.LAST_ERROR IS 'Error of the most recent failed attempt.';
COMMENT ON COLUMN NOTIF_LOG.VARIABLES_JSON IS 'Dispatch variables (JSON object) kept only while QUEUED; cleared on SENT/FAILED/SKIPPED_NO_PROVIDER.';

-- QUEUED rows older than N minutes are what the requeue job looks for.
CREATE INDEX IDX_NOTIF_LOG_STATUS_NEXT_ATTEMPT ON NOTIF_LOG (NOTIFICATION_STATUS_ID, NEXT_ATTEMPT_AT);

-- ------------------------------------------------------------
-- 2. NOTIF_INBOX — in-app notifications of one recipient (SEC_USER id, either realm)
-- ------------------------------------------------------------
CREATE SEQUENCE SEQ_NOTIF_INBOX START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE NOTIF_INBOX (
  ID                BIGINT        NOT NULL,
  TENANT_ID         BIGINT        NOT NULL,
  RECIPIENT_USER_ID BIGINT        NOT NULL,
  TITLE_AR          VARCHAR(300)  NOT NULL,
  TITLE_EN          VARCHAR(300)  NOT NULL,
  BODY_AR           TEXT          NOT NULL,
  BODY_EN           TEXT          NOT NULL,
  READ_AT           TIMESTAMP,
  REFERENCE_TYPE    VARCHAR(100),
  REFERENCE_ID      BIGINT,
  CREATED_BY        VARCHAR(255),
  CREATED_AT        TIMESTAMP,
  UPDATED_BY        VARCHAR(255),
  UPDATED_AT        TIMESTAMP,
  VERSION           BIGINT        DEFAULT 0 NOT NULL
);

COMMENT ON TABLE NOTIF_INBOX IS 'IN_APP channel inbox (erp-core step 08): one row per in-app notification delivered to a recipient.';
COMMENT ON COLUMN NOTIF_INBOX.RECIPIENT_USER_ID IS 'Recipient SEC_USER id (staff or customer realm) — SOFT-READ to SEC, no FK (as NOTIF_LOG.RECIPIENT_ID).';
COMMENT ON COLUMN NOTIF_INBOX.READ_AT IS 'When the recipient marked the item read; NULL while unread.';

ALTER TABLE NOTIF_INBOX ADD CONSTRAINT PK_NOTIF_INBOX PRIMARY KEY (ID);
ALTER TABLE NOTIF_INBOX ADD CONSTRAINT FK_NOTIF_INBOX_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
CREATE INDEX IDX_NOTIF_INBOX_TENANT    ON NOTIF_INBOX (TENANT_ID);
CREATE INDEX IDX_NOTIF_INBOX_RECIPIENT ON NOTIF_INBOX (TENANT_ID, RECIPIENT_USER_ID, READ_AT);

-- ------------------------------------------------------------
-- 3a. MDL lookup values (every tenant that has the NOTIF lookup types)
-- ------------------------------------------------------------
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, TENANT_ID, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER,
                              IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), t.TENANT_ID, t.LOOKUP_TYPE_PK, v.code, v.name_ar, v.name_en, v.sort_order,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    (1, 'NOTIF_STATUS',  'QUEUED',              'في قائمة الانتظار',     'Queued',              5),
    (2, 'NOTIF_STATUS',  'SKIPPED_NO_PROVIDER', 'تم التخطي — لا يوجد مزوّد', 'Skipped (no provider)', 6),
    (3, 'NOTIF_CHANNEL', 'IN_APP',              'داخل التطبيق',          'In-app',              6)
) AS v(ord, type_key, code, name_ar, name_en, sort_order)
JOIN MDL_LOOKUP_TYPE t ON t.KEY = v.type_key
WHERE NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_VALUE x
                  WHERE x.TENANT_ID = t.TENANT_ID AND x.LOOKUP_TYPE_ID = t.LOOKUP_TYPE_PK AND x.CODE = v.code)
ORDER BY t.TENANT_ID, v.ord;

-- ------------------------------------------------------------
-- 3b. NOTIF_CHANNEL_CONFIG — IN_APP enabled for every tenant
-- ------------------------------------------------------------
INSERT INTO NOTIF_CHANNEL_CONFIG (ID, TENANT_ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CONFIG_JSON, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_NOTIF_CHANNEL_CONFIG'), ct.ID, 'IN_APP', 1, NULL, 'SYSTEM', CURRENT_TIMESTAMP
FROM CORE_TENANT ct
WHERE NOT EXISTS (SELECT 1 FROM NOTIF_CHANNEL_CONFIG c WHERE c.TENANT_ID = ct.ID AND c.CHANNEL_TYPE_ID = 'IN_APP')
ORDER BY ct.ID;
