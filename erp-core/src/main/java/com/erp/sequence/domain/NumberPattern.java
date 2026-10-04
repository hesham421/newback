package com.erp.sequence.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sequence.exception.SequenceErrorCodes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed number-series pattern (erp-core step 09), e.g. {@code {PREFIX}-{YYYY}-{SEQ:6}}.
 *
 * <p>Tokens: {@code {PREFIX}}, {@code {YYYY}}, {@code {YY}}, {@code {MM}}, {@code {SEQ:n}} (the sequence
 * value zero-padded to {@code n} digits, 1 ≤ n ≤ 18; a value longer than {@code n} is never cut) and
 * {@code {TENANT}} (the tenant code). Everything outside braces is literal text. A pattern is invalid
 * ({@code SEQUENCE_PATTERN_INVALID}) when it has an unknown token, an unmatched brace, or not exactly one
 * {@code {SEQ:n}}. Pure: no Spring, no database.
 */
public final class NumberPattern {

    /** The tokens a pattern may use (besides literal text). */
    public enum Token { PREFIX, YYYY, YY, MM, SEQ, TENANT }

    private static final Pattern SEQ = Pattern.compile("SEQ:(\\d{1,2})");
    private static final int MAX_SEQ_WIDTH = 18;

    private final String source;
    private final List<Segment> segments;
    private final Set<Token> tokens;

    private NumberPattern(String source, List<Segment> segments, Set<Token> tokens) {
        this.source = source;
        this.segments = segments;
        this.tokens = tokens;
    }

    /** Parses {@code pattern}; throws {@code SEQUENCE_PATTERN_INVALID} when it is malformed. */
    public static NumberPattern parse(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw invalid(pattern);
        }
        List<Segment> segments = new ArrayList<>();
        Set<Token> tokens = EnumSet.noneOf(Token.class);
        int seqCount = 0;
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '}') {
                throw invalid(pattern);
            }
            if (c != '{') {
                literal.append(c);
                i++;
                continue;
            }
            int end = pattern.indexOf('}', i + 1);
            if (end < 0) {
                throw invalid(pattern);
            }
            String name = pattern.substring(i + 1, end);
            if (name.indexOf('{') >= 0) {
                throw invalid(pattern);
            }
            if (!literal.isEmpty()) {
                segments.add(Segment.literal(literal.toString()));
                literal.setLength(0);
            }
            Segment token = tokenSegment(name, pattern);
            if (token.token() == Token.SEQ) {
                seqCount++;
            }
            tokens.add(token.token());
            segments.add(token);
            i = end + 1;
        }
        if (!literal.isEmpty()) {
            segments.add(Segment.literal(literal.toString()));
        }
        if (seqCount != 1) {
            throw invalid(pattern);
        }
        return new NumberPattern(pattern, List.copyOf(segments), Collections.unmodifiableSet(tokens));
    }

    private static Segment tokenSegment(String name, String pattern) {
        Matcher seq = SEQ.matcher(name);
        if (seq.matches()) {
            int width = Integer.parseInt(seq.group(1));
            if (width < 1 || width > MAX_SEQ_WIDTH) {
                throw invalid(pattern);
            }
            return new Segment(Token.SEQ, null, width);
        }
        return switch (name) {
            case "PREFIX" -> new Segment(Token.PREFIX, null, 0);
            case "YYYY" -> new Segment(Token.YYYY, null, 0);
            case "YY" -> new Segment(Token.YY, null, 0);
            case "MM" -> new Segment(Token.MM, null, 0);
            case "TENANT" -> new Segment(Token.TENANT, null, 0);
            default -> throw invalid(pattern);
        };
    }

    /**
     * The policy check done at save time: under a reset policy the sequence starts again at 1, so the
     * rendered number must carry the period, or numbers would repeat — YEARLY needs {@code {YYYY}} or
     * {@code {YY}}, MONTHLY needs a year token and {@code {MM}}.
     */
    public void assertDistinctUnder(ResetPolicy policy) {
        boolean hasYear = tokens.contains(Token.YYYY) || tokens.contains(Token.YY);
        boolean ok = switch (policy) {
            case NEVER -> true;
            case YEARLY -> hasYear;
            case MONTHLY -> hasYear && tokens.contains(Token.MM);
        };
        if (!ok) {
            throw invalid(source);
        }
    }

    /** Whether the pattern uses {@code token}. */
    public boolean uses(Token token) {
        return tokens.contains(token);
    }

    /** Renders one number. {@code prefix} and {@code tenantCode} may be null (rendered empty). */
    public String render(String prefix, LocalDate date, long sequence, String tenantCode) {
        StringBuilder out = new StringBuilder();
        for (Segment segment : segments) {
            if (segment.token() == null) {
                out.append(segment.literal());
                continue;
            }
            switch (segment.token()) {
                case PREFIX -> out.append(prefix == null ? "" : prefix);
                case YYYY -> out.append(String.format("%04d", date.getYear()));
                case YY -> out.append(String.format("%02d", date.getYear() % 100));
                case MM -> out.append(String.format("%02d", date.getMonthValue()));
                case TENANT -> out.append(tenantCode == null ? "" : tenantCode);
                case SEQ -> {
                    String digits = Long.toString(sequence);
                    out.append("0".repeat(Math.max(0, segment.width() - digits.length()))).append(digits);
                }
            }
        }
        return out.toString();
    }

    public String source() {
        return source;
    }

    private static LocalizedException invalid(String pattern) {
        return new LocalizedException(Status.VALIDATION_ERROR, SequenceErrorCodes.SEQUENCE_PATTERN_INVALID,
            pattern == null ? "" : pattern);
    }

    private record Segment(Token token, String literal, int width) {
        static Segment literal(String text) {
            return new Segment(null, text, 0);
        }
    }
}
