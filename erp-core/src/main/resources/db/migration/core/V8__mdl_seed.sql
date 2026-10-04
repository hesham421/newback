-- ============================================================
-- V8 — Master Data Lookup (MDL) seed: the four lookup types the NOTIF and FILE modules read
--      through MDL, with their 17 values.
-- Target: POSTGRESQL_16 | schema: V3__mdl_schema.sql
-- Squashed in erp-core step 04 from old V20__notif_file_lookup_data_migration.sql [post-step-01:
--   V17], its final state (no later script touched these rows). Its other half, registering
--   NOTIF and FILE in SEC_MODULE_REG, is in V7__sec_seed.sql, which runs first: MDL's
--   RULE-MDL-001 (owner_module_code must be a registered module) is checked application-side
--   (XM-MDL-001, SOFT-READ, no FK), so a raw INSERT must keep the data consistent with it.
--   (Old V18__mdl_sequences.sql [V15] created the MDL tables only; it seeded nothing.)
--
--   NOTIF_CHANNEL     (owner NOTIF) — 5 values: EMAIL, SMS, WHATSAPP, PUSH, INTERNAL
--   NOTIF_STATUS      (owner NOTIF) — 4 values: PENDING, SENT, FAILED, CHANNEL_DISABLED
--   FILE_FILE_STATUS  (owner FILE)  — 3 values: ACTIVE, ARCHIVED, DELETED
--   FILE_FILE_TYPE    (owner FILE)  — 5 values: IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER
-- Codes, bilingual labels and sort orders are exactly the value sets the NOTIF and FILE Java code
-- uses. They are admin-editable MDL data (a recorded human decision accepting the drift risk
-- against the Java code that still switches on them).
-- Surrogate PKs from SEQ_MDL_LOOKUP_TYPE / SEQ_MDL_LOOKUP_VALUE; LOOKUP_TYPE_ID resolved by KEY.
-- ============================================================

-- ------------------------------------------------------------
-- 1. MDL_LOOKUP_TYPE
-- ------------------------------------------------------------
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_MDL_LOOKUP_TYPE'), v.key, v.owner_module_code, v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    (1, 'NOTIF_CHANNEL',    'NOTIF', 'قناة الإشعار', 'Notification Channel'),
    (2, 'NOTIF_STATUS',     'NOTIF', 'حالة الإشعار', 'Notification Status'),
    (3, 'FILE_FILE_STATUS', 'FILE',  'حالة الملف',   'File Status'),
    (4, 'FILE_FILE_TYPE',   'FILE',  'نوع الملف',    'File Type')
) AS v(ord, key, owner_module_code, name_ar, name_en)
ORDER BY v.ord;

-- ------------------------------------------------------------
-- 2. MDL_LOOKUP_VALUE — 17 values (5 + 4 + 3 + 5)
-- ------------------------------------------------------------
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), t.LOOKUP_TYPE_PK, v.code, v.name_ar, v.name_en, v.sort_order,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    -- NOTIF_CHANNEL
    ( 1, 'NOTIF_CHANNEL',    'EMAIL',            'بريد إلكتروني',  'Email',            1),
    ( 2, 'NOTIF_CHANNEL',    'SMS',              'رسالة نصية',     'SMS',              2),
    ( 3, 'NOTIF_CHANNEL',    'WHATSAPP',         'واتساب',         'WhatsApp',         3),
    ( 4, 'NOTIF_CHANNEL',    'PUSH',             'إشعار فوري',     'Push',             4),
    ( 5, 'NOTIF_CHANNEL',    'INTERNAL',         'داخلي',          'Internal',         5),
    -- NOTIF_STATUS
    ( 6, 'NOTIF_STATUS',     'PENDING',          'قيد الانتظار',   'Pending',          1),
    ( 7, 'NOTIF_STATUS',     'SENT',             'أُرسل',          'Sent',             2),
    ( 8, 'NOTIF_STATUS',     'FAILED',           'فشل',            'Failed',           3),
    ( 9, 'NOTIF_STATUS',     'CHANNEL_DISABLED', 'القناة معطّلة',  'Channel Disabled', 4),
    -- FILE_FILE_STATUS
    (10, 'FILE_FILE_STATUS', 'ACTIVE',           'نشط',            'Active',           1),
    (11, 'FILE_FILE_STATUS', 'ARCHIVED',         'مؤرشف',          'Archived',         2),
    (12, 'FILE_FILE_STATUS', 'DELETED',          'محذوف',          'Deleted',          3),
    -- FILE_FILE_TYPE
    (13, 'FILE_FILE_TYPE',   'IMAGE',            'صورة',           'Image',            1),
    (14, 'FILE_FILE_TYPE',   'DOCUMENT',         'مستند',          'Document',         2),
    (15, 'FILE_FILE_TYPE',   'SPREADSHEET',      'جدول',           'Spreadsheet',      3),
    (16, 'FILE_FILE_TYPE',   'ARCHIVE',          'أرشيف',          'Archive',          4),
    (17, 'FILE_FILE_TYPE',   'OTHER',            'أخرى',           'Other',            5)
) AS v(ord, type_key, code, name_ar, name_en, sort_order)
JOIN MDL_LOOKUP_TYPE t ON t.KEY = v.type_key
ORDER BY v.ord;
