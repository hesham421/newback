-- ============================================================
-- V11 — Auth realms (STAFF / CUSTOMER) and the super role — erp-core step 06
-- Target: POSTGRESQL_16 | after V2..V10
--
-- What this script does:
--   1. SEC_USER.REALM VARCHAR(16) NOT NULL ∈ {STAFF, CUSTOMER}: added with DEFAULT 'STAFF' (backfills
--      every existing account into the staff realm), then the default is dropped (the step file's
--      design: the application always names the realm).
--   2. User uniqueness becomes (TENANT_ID, REALM, USERNAME) and (TENANT_ID, REALM, EMAIL): the same
--      person may hold one staff and one customer account per tenant. Constraint names are kept.
--   3. CHK_SEC_USER_STATUS also admits PENDING_VERIFICATION (a self-registered customer before the
--      e-mail verification). A widened CHECK: every existing row still satisfies it.
--   4. SEC_ROLE.IS_SUPER BOOLEAN NOT NULL DEFAULT FALSE; every tenant's SYS_ADMIN (the bootstrap role,
--      and its copies made by tenant provisioning) becomes a super role: it holds every catalog
--      permission without per-permission grants (MenuService.effectiveAuthorityCodes).
--   5. SEC_CUSTOMER_VERIFY_TOKEN (tenant-scoped): the hashed one-time e-mail verification token.
--   6. NOTIF_TEMPLATE rows CUSTOMER_VERIFY_EMAIL / CUSTOMER_PASSWORD_RESET (AR/EN) for EVERY tenant that
--      exists when this script runs; tenants provisioned later get them from PLATFORM through
--      NotifTenantProvisioningContributor, which copies all of PLATFORM's templates.
--
-- The permission catalog rows seeded by V7/V10 are NOT touched: since step 06 the
-- PermissionCatalogSynchronizer upserts the code-defined catalog on every start and reproduces those
-- rows identically (V7 is an applied core script and is never edited; README "Additive only").
--
-- Additive-only rule (README.md): REALM is added with a default (dropped afterwards, as the step file
-- prescribes); the two user unique constraints are re-created in their wider (TENANT_ID, REALM, ...)
-- form under the same names (the same kind of change V10 made, now including REALM); the status CHECK
-- is widened. Recorded in docs/DEVIATIONS.md ([06]).
-- ============================================================

-- ============================================================
-- 1. SEC_USER.REALM
-- ============================================================
ALTER TABLE SEC_USER ADD COLUMN REALM VARCHAR(16) NOT NULL DEFAULT 'STAFF';
ALTER TABLE SEC_USER ALTER COLUMN REALM DROP DEFAULT;
ALTER TABLE SEC_USER ADD CONSTRAINT CHK_SEC_USER_REALM CHECK (REALM IN ('STAFF','CUSTOMER'));
COMMENT ON COLUMN SEC_USER.REALM IS 'Auth realm: STAFF (back office, RBAC) | CUSTOMER (self-registered, ROLE_CUSTOMER only). Immutable.';

-- ============================================================
-- 2. Uniqueness per (tenant, realm)
-- ============================================================
ALTER TABLE SEC_USER DROP CONSTRAINT UQ_SEC_USER_USERNAME;
ALTER TABLE SEC_USER ADD  CONSTRAINT UQ_SEC_USER_USERNAME UNIQUE (TENANT_ID, REALM, USERNAME);
ALTER TABLE SEC_USER DROP CONSTRAINT UQ_SEC_USER_EMAIL;
ALTER TABLE SEC_USER ADD  CONSTRAINT UQ_SEC_USER_EMAIL UNIQUE (TENANT_ID, REALM, EMAIL);

-- ============================================================
-- 3. USER_STATUS: + PENDING_VERIFICATION (widened CHECK)
-- ============================================================
ALTER TABLE SEC_USER DROP CONSTRAINT CHK_SEC_USER_STATUS;
ALTER TABLE SEC_USER ADD  CONSTRAINT CHK_SEC_USER_STATUS
  CHECK (STATUS_CODE IN ('PENDING','ACTIVE','DISABLED','PENDING_VERIFICATION'));

-- ============================================================
-- 4. SEC_ROLE.IS_SUPER
-- ============================================================
ALTER TABLE SEC_ROLE ADD COLUMN IS_SUPER BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN SEC_ROLE.IS_SUPER IS 'Super role: holds every catalog permission without grants (PLATFORM-module permissions only inside the PLATFORM tenant).';
UPDATE SEC_ROLE SET IS_SUPER = TRUE WHERE CODE = 'SYS_ADMIN';

-- ============================================================
-- 5. SEC_CUSTOMER_VERIFY_TOKEN
-- ============================================================
CREATE SEQUENCE SEQ_SEC_CUSTOMER_VERIFY_TOKEN START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE SEC_CUSTOMER_VERIFY_TOKEN (
  ID          BIGINT        NOT NULL,
  TENANT_ID   BIGINT        NOT NULL,
  USER_ID     BIGINT        NOT NULL,
  TOKEN_HASH  TEXT          NOT NULL,
  EXPIRES_AT  TIMESTAMPTZ   NOT NULL,
  USED_AT     TIMESTAMPTZ,
  CREATED_BY  VARCHAR(100)  NOT NULL,
  CREATED_AT  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UPDATED_BY  VARCHAR(100),
  UPDATED_AT  TIMESTAMPTZ,
  VERSION     BIGINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE SEC_CUSTOMER_VERIFY_TOKEN IS 'One-time e-mail verification token of a self-registered CUSTOMER account (only the SHA-256 hash is stored).';

ALTER TABLE SEC_CUSTOMER_VERIFY_TOKEN ADD CONSTRAINT PK_SEC_CUSTOMER_VERIFY_TOKEN PRIMARY KEY (ID);
ALTER TABLE SEC_CUSTOMER_VERIFY_TOKEN ADD CONSTRAINT UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH UNIQUE (TENANT_ID, TOKEN_HASH);
ALTER TABLE SEC_CUSTOMER_VERIFY_TOKEN ADD CONSTRAINT FK_SEC_CUSTOMER_VERIFY_TOKEN_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE SEC_CUSTOMER_VERIFY_TOKEN ADD CONSTRAINT FK_SEC_CUSTOMER_VERIFY_TOKEN_USER   FOREIGN KEY (USER_ID)   REFERENCES SEC_USER (USER_PK);
CREATE INDEX IDX_SEC_CUSTOMER_VERIFY_TOKEN_TENANT ON SEC_CUSTOMER_VERIFY_TOKEN (TENANT_ID);
CREATE INDEX IDX_SEC_CUSTOMER_VERIFY_TOKEN_USER   ON SEC_CUSTOMER_VERIFY_TOKEN (USER_ID);

-- ============================================================
-- 6. NOTIF templates for the customer realm, one copy per existing tenant.
--    Placeholders: {actionLink} (frontend link carrying the raw token), {expiresAt}.
-- ============================================================
INSERT INTO NOTIF_TEMPLATE (ID, TENANT_ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_NOTIF_TEMPLATE'), t.ID, v.code, v.name_ar, v.name_en, v.subject_ar, v.subject_en,
       v.body_ar, v.body_en, 1, 'SYSTEM', CURRENT_TIMESTAMP
FROM CORE_TENANT t
CROSS JOIN (VALUES
    (1, 'CUSTOMER_VERIFY_EMAIL', 'تأكيد البريد الإلكتروني', 'Customer e-mail verification',
        'أكّد بريدك الإلكتروني', 'Verify your e-mail address',
        'شكرًا لتسجيلك. أكّد بريدك الإلكتروني لتفعيل حسابك.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'تنتهي صلاحية هذا الرابط في {expiresAt}.',
        'Thank you for signing up. Verify your e-mail address to activate your account.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'This link expires on {expiresAt}.'),
    (2, 'CUSTOMER_PASSWORD_RESET', 'إعادة تعيين كلمة مرور العميل', 'Customer password reset',
        'إعادة تعيين كلمة المرور', 'Reset your password',
        'تلقّينا طلبًا لإعادة تعيين كلمة المرور الخاصة بحسابك.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'يمكن استخدام هذا الرابط مرة واحدة فقط، وتنتهي صلاحيته في {expiresAt}.' || chr(10) ||
        'إذا لم تطلب ذلك، يمكنك تجاهل هذه الرسالة بأمان.',
        'We received a request to reset the password of your account.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'This link can be used only once, and it expires on {expiresAt}.' || chr(10) ||
        'If you did not request this, you can safely ignore this e-mail.')
) AS v(ord, code, name_ar, name_en, subject_ar, subject_en, body_ar, body_en)
ORDER BY t.ID, v.ord;
