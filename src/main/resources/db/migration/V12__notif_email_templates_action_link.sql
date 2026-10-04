-- V12 — Switch PASSWORD_RESET / ACCOUNT_ACTIVATION bodies from a raw {token} the user must copy into
-- a one-click {actionLink} (SecurityAuthEventListener now builds it from app.frontend-url + the raw
-- token as a query param), per the "no manual token entry" review — {token} is still substituted if
-- present for any other consumer, but EMAIL's own template no longer asks for it.
-- V11 already applied and must not be edited (Flyway checksum) — this is a forward-fixing UPDATE.

UPDATE NOTIF_TEMPLATE
SET BODY_EN = 'A password reset was requested for your account. Click the button below to choose a new password:' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'This link expires at: {expiresAt}' || chr(10) ||
              'If you did not request this, you can safely ignore this email.',
    BODY_AR = 'تم استلام طلب لإعادة تعيين كلمة المرور الخاصة بك. اضغط على الزر أدناه لاختيار كلمة مرور جديدة:' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'ينتهي صلاحية هذا الرابط في: {expiresAt}' || chr(10) ||
              'إذا لم تطلب ذلك، تجاهل هذه الرسالة.'
WHERE TEMPLATE_CODE = 'PASSWORD_RESET';

UPDATE NOTIF_TEMPLATE
SET BODY_EN = 'An account was created for you. Click the button below to activate it:' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'This link expires at: {expiresAt}',
    BODY_AR = 'تم إنشاء حساب لك. اضغط على الزر أدناه لتفعيله:' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'ينتهي صلاحية هذا الرابط في: {expiresAt}'
WHERE TEMPLATE_CODE = 'ACCOUNT_ACTIVATION';
