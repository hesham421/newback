package com.erp.notif.channel;

import java.util.Map;

/**
 * One message to deliver over one channel — a plain value built by NOTIF from the queued
 * {@code NOTIF_LOG} row and its template; never a JPA entity.
 *
 * <p>The template texts are passed raw (with their {@code {placeholder}} markers) together with the
 * dispatch {@code variables}: each provider renders them the way its channel needs (HTML for e-mail,
 * plain text for the inbox) — {@link TemplateText#render} substitutes the placeholders. Conventional
 * variables: {@code email} (the e-mail address), {@code lang} ({@code AR}/{@code EN}),
 * {@code actionLink}, {@code ctaLabelEn}/{@code ctaLabelAr}.
 *
 * @param notificationLogId the {@code NOTIF_LOG} row being delivered
 * @param tenantId          the tenant the message belongs to (also the current {@code TenantContext})
 * @param channel           the channel code
 * @param recipientId       the recipient's {@code SEC_USER} id (either realm)
 * @param templateCode      the template's code
 * @param nameAr            template name (Arabic) — a fallback subject/title
 * @param nameEn            template name (English)
 * @param subjectAr         template subject (Arabic), may be {@code null}
 * @param subjectEn         template subject (English), may be {@code null}
 * @param bodyAr            template body (Arabic)
 * @param bodyEn            template body (English)
 * @param moduleCode        the dispatching module
 * @param referenceType     the source entity type, may be {@code null}
 * @param referenceId       the source entity id, may be {@code null}
 * @param variables         the dispatch variables (never {@code null})
 */
public record OutboundMessage(
    Long notificationLogId,
    Long tenantId,
    String channel,
    Long recipientId,
    String templateCode,
    String nameAr,
    String nameEn,
    String subjectAr,
    String subjectEn,
    String bodyAr,
    String bodyEn,
    String moduleCode,
    String referenceType,
    Long referenceId,
    Map<String, String> variables) {

    public OutboundMessage {
        Map<String, String> copy = new java.util.LinkedHashMap<>();
        if (variables != null) {
            variables.forEach((key, value) -> {
                if (key != null && value != null) {
                    copy.put(key, value);
                }
            });
        }
        variables = java.util.Collections.unmodifiableMap(copy);
    }

    /** The subject in Arabic, falling back to the template name. */
    public String titleAr() {
        return subjectAr != null && !subjectAr.isBlank() ? subjectAr : nameAr;
    }

    /** The subject in English, falling back to the template name. */
    public String titleEn() {
        return subjectEn != null && !subjectEn.isBlank() ? subjectEn : nameEn;
    }

    /** A dispatch variable, or {@code null}. */
    public String variable(String name) {
        return variables.get(name);
    }

    @Override
    public String toString() {
        // variables may carry links with single-use tokens: never print them
        return "OutboundMessage[log=" + notificationLogId + ", tenant=" + tenantId + ", channel=" + channel
            + ", recipient=" + recipientId + ", template=" + templateCode + "]";
    }
}
