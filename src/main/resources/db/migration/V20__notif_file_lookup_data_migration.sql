-- ============================================================
-- V20 — NOTIF/FILE legacy lookup DATA migration into MDL
-- ============================================================
-- This is a DATA migration, not a schema change (no DDL below). It performs two things:
--
-- (a) Registers NOTIF and FILE as modules in SEC_MODULE_REG. This is a PREREQUISITE for (b):
--     MDL_LOOKUP_TYPE.owner_module_code is validated by MDL's own RULE-MDL-001 against
--     SEC_MODULE_REG, but that check runs application-side (XM-MDL-001, SOFT-READ, no live FK —
--     see V18's BLOCK 5d comment). A raw SQL INSERT below bypasses that runtime check entirely,
--     so the referenced module MUST already exist in SEC_MODULE_REG here, or the data this
--     migration inserts would be inconsistent with the rule the application enforces for every
--     other caller (mirrors V19's own module-registration step for MDL itself).
--
-- (b) Seeds 4 lookup types + their 17 values into MDL, owned by NOTIF and FILE respectively, as
--     real, admin-editable MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE rows:
--       NOTIF_CHANNEL     (owner NOTIF) — 5 values: EMAIL, SMS, WHATSAPP, PUSH, INTERNAL
--       NOTIF_STATUS      (owner NOTIF) — 4 values: PENDING, SENT, FAILED, CHANNEL_DISABLED
--       FILE_FILE_STATUS  (owner FILE)  — 3 values: ACTIVE, ARCHIVED, DELETED
--       FILE_FILE_TYPE    (owner FILE)  — 5 values: IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER
--     All codes and bilingual (AR/EN) labels below are exactly the value sets the NOTIF and FILE
--     Java code already uses — nothing retyped from memory, no value invented.
--
-- DELIBERATE ARCHITECTURE DECISION (already made by a human, not made by this migration):
--     Before this change, NOTIF_STATUS / FILE_FILE_STATUS / FILE_FILE_TYPE existed only as
--     anti-drift-locked Java constants inside NotificationLogDomain / FileDocumentDomain /
--     FileService — i.e. their value sets could only change via a code deploy. Moving them into
--     MDL data makes them externally, admin-editable at runtime instead. A human has explicitly
--     decided to accept the resulting drift-risk tradeoff (a DB edit can now desync these values
--     from the Java code that still switches on them) as part of the broader NOTIF/FILE →
--     MDL lookup migration. This migration's only job is to seed that data — it is not the one
--     deciding to accept the tradeoff.
--
-- This migration is step 1 of that broader migration only: seed data. It does NOT touch any
-- Java file, does NOT add SEC_SCREEN_REG/SEC_ACTION_REG rows for NOTIF/FILE (out of scope — the
-- only table RULE-MDL-001's check reads is SEC_MODULE_REG), and does NOT grant these new lookup
-- rows' implicit permissions to any role (they will be read via a future cross-module Spring
-- interface call, not gated HTTP endpoints, in a later dispatch). Refactoring
-- NotificationLookupService / FileLookupService to actually consume this data via a new MDL
-- cross-module read API is separate, later work.
--
-- Style matches V19: plain INSERTs (not idempotent — runs once on a fresh schema), surrogate PKs
-- from existing sequences (SEQ_SEC_MODULE_REG from V16; SEQ_MDL_LOOKUP_TYPE / SEQ_MDL_LOOKUP_VALUE
-- from V18 — none recreated here), every FK resolved by natural key (CODE / KEY), never a
-- hardcoded id.
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_MODULE_REG — register NOTIF and FILE (not yet registered anywhere in this schema —
--    live pre-check confirmed only SEC and MDL exist there today).
--    Bilingual module names taken from governance/modules/NOTIF/P1/srs.md and
--    governance/modules/FILE/P1/srs.md (§ module header), matching OpenApiConfig.java's
--    "NOTIF — Notification Service" / "FILE — File Service" display names.
-- ------------------------------------------------------------
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'NOTIF', 'خدمة الإشعارات', 'Notification Service', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'FILE', 'خدمة الملفات', 'File Service', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 2. MDL_LOOKUP_TYPE — the 4 lookup types seeded by this migration, owned by NOTIF/FILE in MDL.
--    Codes + bilingual names match the value sets the NOTIF/FILE Java code already uses.
-- ------------------------------------------------------------

-- ---- Type: NOTIF_CHANNEL (owner NOTIF) ----
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_MDL_LOOKUP_TYPE'), 'NOTIF_CHANNEL', 'NOTIF', 'قناة الإشعار', 'Notification Channel', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Type: NOTIF_STATUS (owner NOTIF) ----
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_MDL_LOOKUP_TYPE'), 'NOTIF_STATUS', 'NOTIF', 'حالة الإشعار', 'Notification Status', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Type: FILE_FILE_STATUS (owner FILE) ----
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_MDL_LOOKUP_TYPE'), 'FILE_FILE_STATUS', 'FILE', 'حالة الملف', 'File Status', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Type: FILE_FILE_TYPE (owner FILE) ----
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_MDL_LOOKUP_TYPE'), 'FILE_FILE_TYPE', 'FILE', 'نوع الملف', 'File Type', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 3. MDL_LOOKUP_VALUE — 17 values total (5 + 4 + 3 + 5), one INSERT block per type, each
--    resolving its LOOKUP_TYPE_ID via a natural-key subquery on KEY (never a hardcoded id).
--    Codes, bilingual names, and sort orders match the value sets the NOTIF/FILE Java code already uses.
-- ------------------------------------------------------------

-- ---- Values for NOTIF_CHANNEL (5) ----
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT) VALUES
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_CHANNEL'), 'EMAIL',    'بريد إلكتروني', 'Email',    1, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_CHANNEL'), 'SMS',      'رسالة نصية',    'SMS',      2, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_CHANNEL'), 'WHATSAPP', 'واتساب',        'WhatsApp', 3, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_CHANNEL'), 'PUSH',     'إشعار فوري',    'Push',     4, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_CHANNEL'), 'INTERNAL', 'داخلي',         'Internal', 5, TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Values for NOTIF_STATUS (4) ----
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT) VALUES
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_STATUS'), 'PENDING',          'قيد الانتظار',   'Pending',          1, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_STATUS'), 'SENT',             'أُرسل',          'Sent',             2, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_STATUS'), 'FAILED',           'فشل',            'Failed',           3, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'NOTIF_STATUS'), 'CHANNEL_DISABLED', 'القناة معطّلة',  'Channel Disabled', 4, TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Values for FILE_FILE_STATUS (3) ----
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT) VALUES
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_STATUS'), 'ACTIVE',   'نشط',    'Active',   1, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_STATUS'), 'ARCHIVED', 'مؤرشف',  'Archived', 2, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_STATUS'), 'DELETED',  'محذوف',  'Deleted',  3, TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ---- Values for FILE_FILE_TYPE (5) ----
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT) VALUES
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_TYPE'), 'IMAGE',       'صورة',  'Image',       1, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_TYPE'), 'DOCUMENT',    'مستند', 'Document',    2, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_TYPE'), 'SPREADSHEET', 'جدول',  'Spreadsheet', 3, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_TYPE'), 'ARCHIVE',     'أرشيف', 'Archive',     4, TRUE, 'SYSTEM', CURRENT_TIMESTAMP),
 (nextval('SEQ_MDL_LOOKUP_VALUE'), (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = 'FILE_FILE_TYPE'), 'OTHER',       'أخرى',  'Other',       5, TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
