package com.erp.notif.channel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NOTIF's template placeholder engine, unchanged by erp-core step 08: {@code {name}} is replaced by the
 * dispatch variable {@code name}; an unknown placeholder is left as written.
 */
public final class TemplateText {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");

    private TemplateText() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code text} with every known {@code {placeholder}} substituted; {@code null} stays {@code null}. */
    public static String render(String text, Map<String, String> variables) {
        if (text == null || variables == null || variables.isEmpty()) {
            return text;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = variables.getOrDefault(matcher.group(1), matcher.group(0));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
