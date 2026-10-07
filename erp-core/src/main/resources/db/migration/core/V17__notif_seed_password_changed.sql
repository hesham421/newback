-- ============================================================
-- V17 — NOTIF template STAFF_PASSWORD_CHANGED — tenant-maturity plan package D (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V16 | seed only
--
-- Written from governance/analysis/modules/NOTIF/P2/db-script.md → "Implementation Addendum —
-- erp-core 1.3.0". The e-mail NOTIF sends when a staff password is set by an administrator or changed by
-- its owner (RULE-NOTIF-009, UserPasswordChangedEvent). Placeholders: {changedAt}, {changedBy}.
-- One row for EVERY tenant that exists when this script runs (the V11 §6 pattern), skipped where the tenant
-- already has a template of that code (UQ_NOTIF_TEMPLATE_CODE); tenants provisioned later copy it from
-- PLATFORM through NotifTenantProvisioningContributor. The plan expected V21 (docs/DEVIATIONS.md [TM-D]).
-- ============================================================

INSERT INTO NOTIF_TEMPLATE (ID, TENANT_ID, TEMPLATE_CODE, NAME_AR, NAME_EN, SUBJECT_AR, SUBJECT_EN,
                            BODY_AR, BODY_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_NOTIF_TEMPLATE'), t.ID, v.code, v.name_ar, v.name_en, v.subject_ar, v.subject_en,
       v.body_ar, v.body_en, 1, 'SYSTEM', CURRENT_TIMESTAMP
FROM CORE_TENANT t
CROSS JOIN (VALUES
    ('STAFF_PASSWORD_CHANGED', 'إشعار تغيير كلمة مرور الموظف', 'Staff password changed',
        'تم تغيير كلمة المرور', 'Your password was changed',
        'تم تغيير كلمة مرور حسابك في {changedAt} بواسطة {changedBy}.' || chr(10) ||
        'إذا لم تكن تتوقع هذا التغيير، فتواصل مع مسؤول النظام فورًا.',
        'The password of your account was changed on {changedAt} by {changedBy}.' || chr(10) ||
        'If you did not expect this change, contact your administrator immediately.')
) AS v(code, name_ar, name_en, subject_ar, subject_en, body_ar, body_en)
WHERE NOT EXISTS (
    SELECT 1 FROM NOTIF_TEMPLATE x WHERE x.TENANT_ID = t.ID AND x.TEMPLATE_CODE = v.code)
ORDER BY t.ID;
