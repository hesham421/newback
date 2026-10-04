package com.erp.notif.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationTemplate;
import com.erp.notif.exception.NotifErrorCodes;

/**
 * Domain companion for ENTITY-NOTIF-002 (NotificationTemplate). Owns every "is this operation
 * allowed?" decision — RULE-NOTIF-004 (bilingual body required) and RULE-NOTIF-006 (unique
 * templateCode). No Spring/JPA annotations, no repository access; constructed only via the static
 * factories.
 */
public final class NotificationTemplateDomain {

    private final String templateCode;
    private final boolean active;

    private NotificationTemplateDomain(String templateCode, boolean active) {
        this.templateCode = templateCode;
        this.active = active;
    }

    /**
     * Construction-time validation for create: required templateCode/nameAr/nameEn, RULE-NOTIF-004
     * (both bodyAr and bodyEn required), then RULE-NOTIF-006 (unique templateCode, pre-checked by
     * the service).
     */
    public static NotificationTemplateDomain create(String templateCode, String nameAr, String nameEn,
                                                    String bodyAr, String bodyEn,
                                                    boolean templateCodeAlreadyTaken) {
        if (isBlank(templateCode) || isBlank(nameAr) || isBlank(nameEn)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                NotifErrorCodes.NOTIF_TEMPLATE_BILINGUAL_REQUIRED);
        }
        if (isBlank(bodyAr) || isBlank(bodyEn)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                NotifErrorCodes.NOTIF_TEMPLATE_BILINGUAL_REQUIRED);
        }
        if (templateCodeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                NotifErrorCodes.NOTIF_TEMPLATE_CODE_DUPLICATE, templateCode);
        }
        return new NotificationTemplateDomain(templateCode, true);
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    public static NotificationTemplateDomain from(NotificationTemplate entity) {
        return new NotificationTemplateDomain(entity.getTemplateCode(),
            Boolean.TRUE.equals(entity.getIsActive()));
    }

    /**
     * Dispatch guard — a deactivated template must not be used to send new notifications; the DELETE
     * soft-deactivate retires the template from the one operation that consumes it (RULE-NOTIF-007, A6).
     */
    public void assertDispatchable() {
        if (!active) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                NotifErrorCodes.NOTIF_TEMPLATE_INACTIVE, templateCode);
        }
    }

    /** RULE-NOTIF-004 for UPDATE — both bodyAr and bodyEn required; called before the service mutates. */
    public void assertBilingualBody(String bodyAr, String bodyEn) {
        if (isBlank(bodyAr) || isBlank(bodyEn)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                NotifErrorCodes.NOTIF_TEMPLATE_BILINGUAL_REQUIRED);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public boolean isActive() {
        return active;
    }
}
