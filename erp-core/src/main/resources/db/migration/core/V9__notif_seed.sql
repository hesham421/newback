-- ============================================================
-- V9 — Notification Service (NOTIF) seed: the enabled EMAIL channel and the two templates SEC
--      dispatches (PASSWORD_RESET, ACCOUNT_ACTIVATION).
-- Target: POSTGRESQL_16 | schema: V6__notif_schema.sql
-- Squashed in erp-core step 04 from the final state of (pre-step-01 names, post-step-01 in brackets):
--   V11__notif_email_channel_seed.sql          [V9]   channel row + the two template rows
--   V12__notif_email_templates_action_link.sql [V10]  bodies switched to {actionLink}
--   V32__notif_password_reset_copy.sql         [V20]  final subjects/bodies (copy rewrite)
-- Placeholders substituted at send time from the dispatch variables:
--   {actionLink}  built by PasswordResetService from erp.core.frontend.base-url +
--                 erp.core.frontend.password-reset-path and the raw token;
--   {expiresAt}   the formatted token expiry.
-- ============================================================

-- ------------------------------------------------------------
-- 1. NOTIF_CHANNEL_CONFIG — EMAIL enabled (DefaultChannelProvider sends real SMTP mail when a
--    JavaMailSender is configured; otherwise the dispatch is recorded as FAILED).
-- ------------------------------------------------------------
INSERT INTO NOTIF_CHANNEL_CONFIG (ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CONFIG_JSON, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_CHANNEL_CONFIG'), 'EMAIL', 1, NULL, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 2. NOTIF_TEMPLATE — PASSWORD_RESET
-- ------------------------------------------------------------
INSERT INTO NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_TEMPLATE'), 'PASSWORD_RESET', 'إعادة تعيين كلمة المرور', 'Password Reset',
        'إعادة تعيين كلمة مرور نظام ERP', 'Reset your ERP System password',
        'تلقّينا طلبًا لإعادة تعيين كلمة المرور الخاصة بحسابك في نظام ERP.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'لأسباب أمنية يمكن استخدام هذا الرابط مرة واحدة فقط، وتنتهي صلاحيته في {expiresAt}.' || chr(10) ||
        'إذا لم تطلب ذلك، يمكنك تجاهل هذه الرسالة بأمان — لن تتغير كلمة المرور الخاصة بك.',
        'We received a request to reset the password for your ERP System account.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'For your security this link can be used only once, and it expires on {expiresAt}.' || chr(10) ||
        'If you did not request this, you can safely ignore this email — your password will not change.',
        1, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 3. NOTIF_TEMPLATE — ACCOUNT_ACTIVATION
-- ------------------------------------------------------------
INSERT INTO NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_TEMPLATE'), 'ACCOUNT_ACTIVATION', 'تفعيل الحساب', 'Account Activation',
        'تفعيل الحساب', 'Activate your account',
        'تم إنشاء حساب لك في نظام ERP. قم بتفعيله لاختيار كلمة المرور وتسجيل الدخول.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'تنتهي صلاحية هذا الرابط في {expiresAt}.',
        'An ERP System account has been created for you. Activate it to choose your password and sign in.' || chr(10) ||
        '{actionLink}' || chr(10) ||
        'This link expires on {expiresAt}.',
        1, 'SYSTEM', CURRENT_TIMESTAMP);
