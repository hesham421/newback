package com.erp.file.domain;

import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageRejection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RULE-FILE-008 / RULE-FILE-009 (tenant-maturity D.4) — the image store's decisions: the content type is
 * detected from the bytes only (PNG / JPEG / WebP magic numbers, SVG text), must be one the caller allows,
 * the size must be 1..maxBytes, and an SVG must carry no script, event handler, external reference,
 * DOCTYPE / ENTITY or {@code foreignObject} (rejected, never rewritten). Pure decisions, no I/O.
 */
public final class ImageValidationDomainService {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private static final Pattern EVENT_HANDLER = Pattern.compile("[\\s/\"']on[a-z0-9_.:-]*\\s*=");
    private static final Pattern HREF = Pattern.compile("(?:xlink:)?href\\s*=\\s*(\"|')?\\s*([^\"'\\s>]*)");
    private static final Pattern CSS_URL = Pattern.compile("url\\(\\s*(\"|')?\\s*([^\"')\\s]*)");
    private static final Pattern SVG_ROOT = Pattern.compile("^<svg[\\s>/]");

    private ImageValidationDomainService() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** The verdict on one image: the detected content type when accepted, else the rejection. */
    public record Verdict(String contentType, ImageRejection rejection) {

        public boolean accepted() {
            return rejection == null;
        }
    }

    /** RULE-FILE-008/009, in this order: empty, too large, type (detected and allowed), SVG safety. */
    public static Verdict check(byte[] content, long maxBytes, Set<String> allowedTypes) {
        if (content == null || content.length == 0) {
            return new Verdict(null, ImageRejection.EMPTY);
        }
        if (content.length > maxBytes) {
            return new Verdict(null, ImageRejection.TOO_LARGE);
        }
        String type = detectType(content);
        if (type == null || allowedTypes == null || !allowedTypes.contains(type)) {
            return new Verdict(null, ImageRejection.TYPE_NOT_ALLOWED);
        }
        if (FileImageStoreApi.TYPE_SVG.equals(type) && !isSafeSvg(new String(content, StandardCharsets.UTF_8))) {
            return new Verdict(null, ImageRejection.UNSAFE_SVG);
        }
        return new Verdict(type, null);
    }

    /** RULE-FILE-008 — PNG, JPEG, WebP by magic bytes, SVG by its root element; {@code null} otherwise. */
    public static String detectType(byte[] content) {
        if (startsWith(content, PNG_MAGIC)) {
            return FileImageStoreApi.TYPE_PNG;
        }
        if (content.length >= 3 && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF) {
            return FileImageStoreApi.TYPE_JPEG;
        }
        if (content.length >= 12 && ascii(content, 0, "RIFF") && ascii(content, 8, "WEBP")) {
            return FileImageStoreApi.TYPE_WEBP;
        }
        return isSvgText(new String(content, StandardCharsets.UTF_8)) ? FileImageStoreApi.TYPE_SVG : null;
    }

    /**
     * RULE-FILE-009 — false when the SVG text holds a {@code <script}, an {@code on…=} attribute, an
     * {@code href} / CSS {@code url(…)} that is not a same-document fragment, {@code javascript:},
     * {@code <!DOCTYPE} / {@code <!ENTITY} or {@code <foreignObject}.
     */
    public static boolean isSafeSvg(String svg) {
        String text = svg.toLowerCase(Locale.ROOT);
        if (text.contains("<script") || text.contains("javascript:") || text.contains("<!doctype")
            || text.contains("<!entity") || text.contains("<foreignobject") || text.contains("@import")) {
            return false;
        }
        if (EVENT_HANDLER.matcher(text).find()) {
            return false;
        }
        return onlyFragments(HREF.matcher(text)) && onlyFragments(CSS_URL.matcher(text));
    }

    private static boolean onlyFragments(Matcher matcher) {
        while (matcher.find()) {
            if (!matcher.group(2).startsWith("#")) {
                return false;
            }
        }
        return true;
    }

    /** SVG text: after an optional BOM, XML declaration, comments, DOCTYPE and whitespace, the root is {@code <svg}. */
    private static boolean isSvgText(String decoded) {
        String text = decoded.startsWith("\uFEFF") ? decoded.substring(1) : decoded;
        int guard = 0;
        while (guard++ < 64) {
            text = text.stripLeading();
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.startsWith("<?xml") || lower.startsWith("<!doctype")) {
                int end = text.indexOf('>');
                if (end < 0) {
                    return false;
                }
                text = text.substring(end + 1);
            } else if (lower.startsWith("<!--")) {
                int end = text.indexOf("-->");
                if (end < 0) {
                    return false;
                }
                text = text.substring(end + 3);
            } else {
                return SVG_ROOT.matcher(lower).find();
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] content, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (content[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
