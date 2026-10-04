-- V10 — Bootstrap a local admin/admin user for a fresh environment.
-- V3 seeds the SYS_ADMIN role/permissions but never an actual SEC_USER_ACCOUNT row, so a brand
-- new database has no user that can log in to create the first user (chicken-and-egg: user
-- creation itself requires an authenticated SYS_ADMIN). This migration seeds exactly that one
-- account, ACTIVE (not PENDING_ACTIVATION) so it can log in immediately, and grants it SYS_ADMIN.
-- PASSWORD_HASH is a BCrypt hash of "admin" (dev/test bootstrap credential only).

INSERT INTO SEC_USER_ACCOUNT (ID, USERNAME, PASSWORD_HASH, EMAIL, FULL_NAME, PREFERRED_LANG_ID,
                               USER_STATUS_ID, FAILED_LOGIN_COUNT, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_USER_ACCOUNT'), 'admin',
        '$2y$10$.G36apS4.ChTaMI.YU3bxO1nPj9IREwDvDa1MEHGuB5dlRSU7ikLe',
        'admin@erp.local', 'System Administrator', 'EN', 'ACTIVE', 0, 1, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO SEC_USER_ROLE (USER_ACCOUNT_FK, ROLE_FK)
VALUES ((SELECT ID FROM SEC_USER_ACCOUNT WHERE USERNAME = 'admin'),
        (SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'SYS_ADMIN'));
