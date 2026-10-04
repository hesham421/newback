-- V32 — Rewrite the PASSWORD_RESET / ACCOUNT_ACTIVATION bodies now that {actionLink} is actually
-- supplied. V12 switched the templates to {actionLink} on the strength of a "SecurityAuthEventListener"
-- that was never written, so the mail shipped with the literal text "{actionLink}" where the button
-- should have been and a raw Instant ("2026-09-18T15:36:19.829117Z") as the expiry. PasswordResetService
-- now builds the link from app.frontend-url + app.password-reset-path and formats {expiresAt}; this
-- migration brings the copy up to the same standard — states what happened, what to do, how long there
-- is to do it, and what to do if it wasn't you.
-- V12 already applied and must not be edited (Flyway checksum) — this is a forward-fixing UPDATE.

UPDATE NOTIF_TEMPLATE
SET SUBJECT_EN = 'Reset your ERP System password',
    SUBJECT_AR = 'إعادة تعيين كلمة مرور نظام ERP',
    BODY_EN = 'We received a request to reset the password for your ERP System account.' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'For your security this link can be used only once, and it expires on {expiresAt}.' || chr(10) ||
              'If you did not request this, you can safely ignore this email — your password will not change.',
    BODY_AR = 'تلقّينا طلبًا لإعادة تعيين كلمة المرور الخاصة بحسابك في نظام ERP.' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'لأسباب أمنية يمكن استخدام هذا الرابط مرة واحدة فقط، وتنتهي صلاحيته في {expiresAt}.' || chr(10) ||
              'إذا لم تطلب ذلك، يمكنك تجاهل هذه الرسالة بأمان — لن تتغير كلمة المرور الخاصة بك.'
WHERE TEMPLATE_CODE = 'PASSWORD_RESET';

UPDATE NOTIF_TEMPLATE
SET BODY_EN = 'An ERP System account has been created for you. Activate it to choose your password and sign in.' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'This link expires on {expiresAt}.',
    BODY_AR = 'تم إنشاء حساب لك في نظام ERP. قم بتفعيله لاختيار كلمة المرور وتسجيل الدخول.' || chr(10) ||
              '{actionLink}' || chr(10) ||
              'تنتهي صلاحية هذا الرابط في {expiresAt}.'
WHERE TEMPLATE_CODE = 'ACCOUNT_ACTIVATION';
