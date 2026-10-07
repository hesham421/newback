-- ============================================================
-- V16 — Staff user profile and password facts — tenant-maturity plan package D (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V2..V15 | additive only (README: no rename, drop or type change)
--
-- Written from governance/analysis/modules/SEC/P2/db-script-sec.md → "Implementation Addendum —
-- erp-core 1.3.0" (DBF-SEC-117..123, CHK_SEC_USER_LOCALE). The plan expected V19; the number follows
-- the execution order (docs/DEVIATIONS.md [TM-D]).
--
-- SEC_USER gains:
--   PHONE, JOB_TITLE_AR/EN, PREFERRED_LOCALE ('ar' | 'en')        — the staff profile (GET/PATCH /sec/me)
--   PHOTO_FILE_ID                                                 — soft reference to FILE_DOCUMENT.ID, no FK
--                                                                   (XM-SEC-006, the ATTACHMENT_FILE_ID convention)
--   PASSWORD_CHANGE_REQUIRED_FL (DEFAULT FALSE: the upgrade forces no account into a change)
--   PASSWORD_CHANGED_AT                                           — when a person last set a usable password
-- ============================================================

ALTER TABLE SEC_USER ADD COLUMN PHONE                       VARCHAR(30);
ALTER TABLE SEC_USER ADD COLUMN JOB_TITLE_AR                VARCHAR(150);
ALTER TABLE SEC_USER ADD COLUMN JOB_TITLE_EN                VARCHAR(150);
ALTER TABLE SEC_USER ADD COLUMN PREFERRED_LOCALE            VARCHAR(5);
ALTER TABLE SEC_USER ADD COLUMN PHOTO_FILE_ID               BIGINT;
ALTER TABLE SEC_USER ADD COLUMN PASSWORD_CHANGE_REQUIRED_FL BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE SEC_USER ADD COLUMN PASSWORD_CHANGED_AT         TIMESTAMPTZ;

ALTER TABLE SEC_USER ADD CONSTRAINT CHK_SEC_USER_LOCALE
    CHECK (PREFERRED_LOCALE IS NULL OR PREFERRED_LOCALE IN ('ar', 'en'));

COMMENT ON COLUMN SEC_USER.PHONE IS 'Staff phone number, E.164-ish (optional +, digits, spaces, hyphens); DBF-SEC-117.';
COMMENT ON COLUMN SEC_USER.JOB_TITLE_AR IS 'Job title (Arabic); DBF-SEC-118.';
COMMENT ON COLUMN SEC_USER.JOB_TITLE_EN IS 'Job title (English); DBF-SEC-119.';
COMMENT ON COLUMN SEC_USER.PREFERRED_LOCALE IS 'Preferred UI language, ar or en (CHK_SEC_USER_LOCALE); DBF-SEC-120.';
COMMENT ON COLUMN SEC_USER.PHOTO_FILE_ID IS 'Soft reference to FILE_DOCUMENT.ID of the PUBLIC profile photo (no FK, XM-SEC-006); DBF-SEC-121.';
COMMENT ON COLUMN SEC_USER.PASSWORD_CHANGE_REQUIRED_FL IS 'TRUE while an administrator-chosen password awaits the owner''s change (RULE-SEC-058/059); DBF-SEC-122.';
COMMENT ON COLUMN SEC_USER.PASSWORD_CHANGED_AT IS 'When a person last set a usable password; DBF-SEC-123.';
