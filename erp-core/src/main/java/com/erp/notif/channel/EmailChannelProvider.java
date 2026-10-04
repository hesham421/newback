package com.erp.notif.channel;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * The core EMAIL {@link ChannelProvider}: a real SMTP send through {@link JavaMailSender} (the former
 * {@code DefaultChannelProvider} mail code, now behind the channel SPI — erp-core step 08).
 *
 * <p>Registered by {@code ErpCoreNotifAutoConfiguration} only when a {@code JavaMailSender} bean exists
 * (Spring Mail on the classpath and {@code spring.mail.host} set); otherwise EMAIL is served by
 * {@link LoggingChannelProvider} and ends {@code SKIPPED_NO_PROVIDER}.
 *
 * <p>Sends a single-language HTML e-mail (with a plain-text alternative) chosen from the bilingual
 * template by the {@code lang} variable (falls back to EN). The address comes from the {@code email}
 * variable the dispatching module supplies (e.g. from SEC's {@code SecUserDirectoryApi.findContact}).
 * Template text and every substituted value are HTML-escaped in the HTML part; {@code actionLink}
 * becomes a button.
 */
@Slf4j
public class EmailChannelProvider implements ChannelProvider {

    /** Failure reason when the message carries no {@code email} variable (retrying cannot help, but it is a failure). */
    public static final String MISSING_EMAIL = "missing recipient email address";

    private static final String ARABIC_LANG = "AR";

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailChannelProvider(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.fromAddress = fromAddress == null ? "" : fromAddress;
    }

    @Override
    public String channel() {
        return NotifChannels.EMAIL;
    }

    @Override
    public DeliveryResult send(OutboundMessage message) {
        Map<String, String> variables = message.variables();
        String to = variables.get("email");
        if (to == null || to.isBlank()) {
            log.warn("EMAIL notification {} has no 'email' variable — cannot send", message.notificationLogId());
            return DeliveryResult.failed(MISSING_EMAIL);
        }

        boolean rtl = ARABIC_LANG.equalsIgnoreCase(variables.get("lang"));
        String subjectTemplate = rtl ? message.titleAr() : message.titleEn();
        String bodyTemplate = rtl ? message.bodyAr() : message.bodyEn();

        String subject = TemplateText.render(subjectTemplate, variables);
        String plainBody = TemplateText.render(bodyTemplate, variables);
        String htmlBody = renderHtml(escape(subject),
            TemplateText.render(escape(bodyTemplate), htmlVariables(variables, rtl)), rtl);

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            if (!fromAddress.isBlank()) {
                // without spring.mail.username JavaMail uses the session's default sender (mail.from);
                // an empty From would be rejected as an illegal address
                helper.setFrom(fromAddress);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainBody, htmlBody);
            mailSender.send(mime);
            log.info("Sent EMAIL notification {} to recipient {} using template {}",
                message.notificationLogId(), message.recipientId(), message.templateCode());
            return DeliveryResult.sent();
        } catch (Exception ex) {
            log.warn("EMAIL send failed for notification {} (recipient {}): {}",
                message.notificationLogId(), message.recipientId(), ex.getMessage());
            return DeliveryResult.failed(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
        }
    }

    /** HTML-escapes every substituted value, then renders {@code actionLink} as a clickable CTA
     * button for the HTML body only — the recipient never has to see or manually enter a token. The
     * plain-text alternative keeps the raw values and URL. */
    private static Map<String, String> htmlVariables(Map<String, String> variables, boolean rtl) {
        Map<String, String> htmlVars = new HashMap<>();
        variables.forEach((key, value) -> htmlVars.put(key, escape(value)));
        if (!variables.containsKey("actionLink")) {
            return htmlVars;
        }
        String url = variables.get("actionLink");
        String safeUrl = escape(url);
        String label = escape(variables.getOrDefault(rtl ? "ctaLabelAr" : "ctaLabelEn", url));
        String fallbackIntro = rtl
            ? "أو انسخ هذا الرابط إلى متصفحك:"
            : "Or copy and paste this link into your browser:";
        htmlVars.put("actionLink", "</p><div style=\"text-align:center;margin:28px 0\">"
            + "<a href=\"" + safeUrl + "\" style=\"display:inline-block;padding:14px 36px;background:#1a1a2e;"
            + "color:#ffffff;text-decoration:none;border-radius:6px;font-weight:600;font-size:15px\">"
            + label + "</a></div>"
            + "<p style=\"font-size:12px;color:#8a8a98;margin:0 0 4px\">" + fallbackIntro + "</p>"
            + "<p style=\"font-size:12px;color:#8a8a98;word-break:break-all;margin:0 0 20px\">"
            + "<a href=\"" + safeUrl + "\" style=\"color:#8a8a98\">" + safeUrl + "</a></p><p>");
        return htmlVars;
    }

    /** Template bodies and substituted values are data, not markup — never let either close a tag. */
    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String renderHtml(String subject, String bodyWithHighlight, boolean rtl) {
        String dir = rtl ? "rtl" : "ltr";
        String align = rtl ? "right" : "left";
        // The button block closes and reopens the surrounding <p>; the template's own newlines around
        // {actionLink} would otherwise survive as <br>s hanging off an empty paragraph.
        String paragraphs = ("<p>" + bodyWithHighlight.replace("\n", "<br>") + "</p>")
            .replaceAll("(?:<br>)+(</p>)", "$1")
            .replaceAll("(<p[^>]*>)(?:<br>)+", "$1")
            .replaceAll("<p>\\s*</p>", "");
        return "<!DOCTYPE html><html dir=\"" + dir + "\" lang=\"" + (rtl ? "ar" : "en") + "\">"
            + "<body style=\"margin:0;padding:0;background:#f4f4f7;font-family:Arial,Helvetica,sans-serif\">"
            + "<div style=\"max-width:480px;margin:24px auto;background:#ffffff;border-radius:8px;"
            + "overflow:hidden;border:1px solid #e2e2ea\">"
            + "<div style=\"background:#1a1a2e;color:#ffffff;padding:16px 24px;font-size:16px;"
            + "font-weight:600;text-align:" + align + "\">ERP System</div>"
            + "<div style=\"padding:24px;color:#333333;font-size:14px;line-height:1.6;text-align:" + align + "\">"
            + "<h2 style=\"font-size:16px;margin:0 0 12px\">" + subject + "</h2>"
            + paragraphs
            + "</div>"
            + "<div style=\"padding:16px 24px;background:#f4f4f7;color:#8a8a98;font-size:12px;"
            + "text-align:" + align + "\">"
            + (rtl ? "هذه رسالة تلقائية، الرجاء عدم الرد عليها." : "This is an automated message, please do not reply.")
            + "</div></div></body></html>";
    }
}
