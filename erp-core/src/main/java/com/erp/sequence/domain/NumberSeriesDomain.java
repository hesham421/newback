package com.erp.sequence.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.exception.SequenceErrorCodes;
import java.time.LocalDate;

/**
 * Domain companion of {@link NumberSeries} (erp-core step 09): every "is this allowed?" decision of a
 * number series — code uniqueness, pattern validity under the reset policy, whether numbers may be
 * drawn — and the number formatting itself. No Spring/JPA annotations, no repository access; built
 * only through {@link #create} and {@link #from}.
 */
public final class NumberSeriesDomain {

    private final String code;
    private final ResetPolicy resetPolicy;
    private final NumberPattern pattern;
    private final String prefix;
    private final boolean active;

    private NumberSeriesDomain(String code, ResetPolicy resetPolicy, NumberPattern pattern, String prefix,
                               boolean active) {
        this.code = code;
        this.resetPolicy = resetPolicy;
        this.pattern = pattern;
        this.prefix = prefix;
        this.active = active;
    }

    /**
     * Construction-time rules of a new series: the code is free in the tenant, the pattern parses and
     * keeps numbers distinct under the reset policy (save-time {@code SEQUENCE_PATTERN_INVALID}).
     */
    public static NumberSeriesDomain create(String code, String prefix, String pattern, ResetPolicy resetPolicy,
                                            boolean codeAlreadyTaken) {
        if (codeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS, SequenceErrorCodes.NUMBER_SERIES_CODE_DUPLICATE, code);
        }
        NumberPattern parsed = NumberPattern.parse(pattern);
        parsed.assertDistinctUnder(resetPolicy);
        return new NumberSeriesDomain(code, resetPolicy, parsed, prefix, true);
    }

    /** A Domain view over a persisted row (its pattern was validated when it was saved). */
    public static NumberSeriesDomain from(NumberSeries entity) {
        return new NumberSeriesDomain(entity.getCode(), entity.getResetPolicy(), NumberPattern.parse(entity.getPattern()),
            entity.getPrefix(), Boolean.TRUE.equals(entity.getIsActive()));
    }

    /** Update-time rule: the new pattern parses and fits the (immutable) reset policy. */
    public void assertCanChangePattern(String newPattern) {
        NumberPattern.parse(newPattern).assertDistinctUnder(resetPolicy);
    }

    /** Numbers are drawn only from an active series; an inactive one counts as not configured. */
    public void assertCanAllocate() {
        if (!active) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED, code);
        }
    }

    /** The period key of {@code date} for this series. */
    public String periodKey(LocalDate date) {
        return resetPolicy.periodKey(date);
    }

    /** Whether formatting needs the tenant code (so the caller looks it up only when used). */
    public boolean needsTenantCode() {
        return pattern.uses(NumberPattern.Token.TENANT);
    }

    /** Formats {@code sequence} drawn on {@code date}. */
    public String format(LocalDate date, long sequence, String tenantCode) {
        return pattern.render(prefix, date, sequence, tenantCode);
    }

    public String getCode() {
        return code;
    }

    public boolean isActive() {
        return active;
    }
}
