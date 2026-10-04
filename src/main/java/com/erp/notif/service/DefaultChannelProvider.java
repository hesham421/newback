package com.erp.notif.service;

import com.erp.notif.entity.NotificationTemplate;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Default {@link ChannelProvider} — resolves OQ-NOTIF-001 for the EMAIL channel with a real SMTP
 * send via {@link JavaMailSender}. Sends a single-language HTML email (plain-text alternative
 * included for clients/filters that prefer it) chosen from the bilingual {@link NotificationTemplate}
 * by the {@code variables.get("lang")} the caller supplies (falls back to EN) — a stacked EN+AR body
 * in one email is not how transactional mail is normally sent. The recipient address is read from
 * {@code variables.get("email")}: the caller supplies it (SEC's {@code SecUserDirectoryApi.findContact}
 * gives a consuming module the address for a userPk). Template text and every substituted value are
 * HTML-escaped in the HTML part. Every other channel type (SMS/WHATSAPP/PUSH/INTERNAL) remains the original
 * provider-agnostic stub — logs the send and reports success — until its own concrete provider is
 * chosen (OQ-NOTIF-001, still open for those channels).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultChannelProvider implements ChannelProvider {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");
    private static final String EMAIL_CHANNEL = "EMAIL";
    private static final String ARABIC_LANG = "AR";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Override
    public ChannelSendResult send(String channelTypeId, Long recipientId, NotificationTemplate template,
                                  String configJson, Map<String, String> variables) {
        if (!EMAIL_CHANNEL.equals(channelTypeId)) {
            log.info("Dispatching notification via channel {} to recipient {} using template {}",
                channelTypeId, recipientId, template != null ? template.getTemplateCode() : null);
            return ChannelSendResult.ok();
        }
        return sendEmail(recipientId, template, variables);
    }

    private ChannelSendResult sendEmail(Long recipientId, NotificationTemplate template,
                                        Map<String, String> variables) {
        String to = variables != null ? variables.get("email") : null;
        if (to == null || to.isBlank()) {
            log.warn("EMAIL dispatch to recipient {} has no 'email' variable — cannot send", recipientId);
            return ChannelSendResult.failure("missing recipient email address");
        }

        boolean rtl = ARABIC_LANG.equalsIgnoreCase(variables.get("lang"));
        String subjectTemplate = rtl ? template.getSubjectAr() : template.getSubjectEn();
        String bodyTemplate = rtl ? template.getBodyAr() : template.getBodyEn();
        if (subjectTemplate == null) {
            subjectTemplate = rtl ? template.getNameAr() : template.getNameEn();
        }

        String subject = substitute(subjectTemplate, variables);
        String plainBody = substitute(bodyTemplate, variables);
        String htmlBody = renderHtml(escape(subject),
            substitute(escape(bodyTemplate), htmlVariables(variables, rtl)), rtl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainBody, htmlBody);
            mailSender.send(message);
            log.info("Sent EMAIL notification to recipient {} ({}) using template {}",
                recipientId, to, template.getTemplateCode());
            return ChannelSendResult.ok();
        } catch (Exception ex) {
            log.warn("EMAIL send failed for recipient {} ({}): {}", recipientId, to, ex.getMessage());
            return ChannelSendResult.failure(ex.getMessage());
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
            ? "\u0623\u0648 \u0627\u0646\u0633\u062e \u0647\u0630\u0627 \u0627\u0644\u0631\u0627\u0628\u0637 \u0625\u0644\u0649 \u0645\u062a\u0635\u0641\u062d\u0643:"
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

    private static String substitute(String text, Map<String, String> variables) {
        if (text == null || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = variables.getOrDefault(matcher.group(1), matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
