-- V11 — Enable the EMAIL channel and seed the two templates the SEC auth-event bridge dispatches
-- (SecurityAuthEventListener): PASSWORD_RESET and ACCOUNT_ACTIVATION. Resolves OQ-NOTIF-001 for the
-- EMAIL channel (DefaultChannelProvider now sends real SMTP mail instead of only logging).
-- {token} / {expiresAt} are placeholders substituted from the dispatch variables at send time.

INSERT INTO NOTIF_CHANNEL_CONFIG (ID, CHANNEL_TYPE_ID, IS_ENABLED_FL, CONFIG_JSON, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_CHANNEL_CONFIG'), 'EMAIL', 1, NULL, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_TEMPLATE'), 'PASSWORD_RESET', 'إعادة تعيين كلمة المرور', 'Password Reset',
        'إعادة تعيين كلمة المرور', 'Password Reset',
        'تم استلام طلب لإعادة تعيين كلمة المرور الخاصة بك. رمز إعادة التعيين: {token}' || chr(10) ||
        'ينتهي صلاحية هذا الرمز في: {expiresAt}' || chr(10) ||
        'إذا لم تطلب ذلك، تجاهل هذه الرسالة.',
        'A password reset was requested for your account. Reset token: {token}' || chr(10) ||
        'This token expires at: {expiresAt}' || chr(10) ||
        'If you did not request this, you can safely ignore this email.',
        1, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO NOTIF_TEMPLATE (ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_NOTIF_TEMPLATE'), 'ACCOUNT_ACTIVATION', 'تفعيل الحساب', 'Account Activation',
        'تفعيل الحساب', 'Activate your account',
        'تم إنشاء حساب لك. رمز التفعيل: {token}' || chr(10) ||
        'ينتهي صلاحية هذا الرمز في: {expiresAt}',
        'An account was created for you. Activation token: {token}' || chr(10) ||
        'This token expires at: {expiresAt}',
        1, 'SYSTEM', CURRENT_TIMESTAMP);
