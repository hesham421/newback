package com.erp.fin.service;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.mdl.crossmodule.LookupOptionView;
import com.erp.mdl.crossmodule.MdlLookupApi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * XM-FIN-001 — FIN's single consumption point for MDL's lookup values. Every FIN lookup-backed
 * column is a plain {@code String} holding the code, validated here against MDL before any write
 * (CORE.md "Lookup values"); an invalid code is rejected with {@code FIN-400-INVALID-LOOKUP}
 * before it reaches the database.
 *
 * <p><b>In-process, never HTTP.</b> MDL's published cross-module interface
 * {@link MdlLookupApi} is injected directly, exactly as CORE.md's "Cross-module contract
 * placement" requires and as the delivered {@code NotificationLookupService} and
 * {@code FileLookupService} already do — never a loopback call to API-MDL-011. Only the narrow
 * read-model {@link LookupOptionView} is consumed; MDL's {@code LookupValue} entity and internal
 * DTOs are never touched. {@code readActiveValuesByKey} offers no "does this code exist" method,
 * so membership is checked here against the returned list.
 *
 * <p><b>Translation of MDL's not-found.</b> An unknown or deactivated {@code typeKey} makes MDL
 * throw {@code LocalizedException(Status.NOT_FOUND, MDL_404_TYPE_KEY, ...)}. That code must never
 * leak out of FIN's API, so it is caught at this call site and re-thrown as FIN's own
 * {@code FIN-400-INVALID-LOOKUP} — the same posture {@code NotificationLookupService} documents,
 * and the behaviour the struck {@code FIN-503} catalog row's resolution prescribes. Any other
 * {@code LocalizedException} is re-thrown untouched (it falls through to {@code FIN-500}); nothing
 * is swallowed.
 *
 * <p><b>Why this is not a Domain delegation (A.5.18).</b> Lookup-code membership is a referential
 * input-shape check against another module's reference data, not an "is this operation allowed?"
 * decision about a FIN entity, and it has no SRS RULE — the Error Catalog files
 * {@code FIN-400-INVALID-LOOKUP} as PLATFORM-STD / HTTP 400. Same reasoning
 * {@code LookupValueService.reorder} records for its own membership guard. No FIN domain class
 * could own it in any case: A.0.6 forbids a Domain object calling another module.
 *
 * <p>{@code @PreAuthorize("isAuthenticated()")} — this service fronts no endpoint of its own; it
 * is only ever reached from another FIN service whose own {@code @PreAuthorize} has already
 * enforced the screen permission. The gate is the same spec-sanctioned form
 * {@code NotificationLookupService} uses, never an unguarded public method (A.5.2).
 *
 * <p>No caching annotations anywhere: FIN's approved cache register is empty, and
 * gov-enforce-caching-rules independently bars caching for a financial module.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinLookupValidationService {

    /** LOV keys owned by FIN and registered into MDL (SRS A6 — 13 keys, all FIN-owned). */
    public static final String KEY_ACCOUNT_TYPE = "ACCOUNT_TYPE";

    public static final String KEY_DEBIT_CREDIT = "DEBIT_CREDIT";

    public static final String KEY_PERIOD_STATE = "PERIOD_STATE";

    public static final String KEY_FISCAL_YEAR_STATUS = "FISCAL_YEAR_STATUS";

    public static final String KEY_JOURNAL_TYPE = "JOURNAL_TYPE";

    public static final String KEY_JOURNAL_STATUS = "JOURNAL_STATUS";

    public static final String KEY_ACCOUNTING_EVENT_TYPE = "ACCOUNTING_EVENT_TYPE";

    public static final String KEY_PAYMENT_METHOD = "PAYMENT_METHOD";

    public static final String KEY_ACCOUNT_DERIVATION_TYPE = "ACCOUNT_DERIVATION_TYPE";

    public static final String KEY_AMOUNT_SOURCE_TYPE = "AMOUNT_SOURCE_TYPE";

    public static final String KEY_DISTRIBUTION_TYPE = "DISTRIBUTION_TYPE";

    public static final String KEY_RECURRING_SCHEDULE_TYPE = "RECURRING_SCHEDULE_TYPE";

    public static final String KEY_RECURRING_FREQUENCY = "RECURRING_FREQUENCY";

    /** SRS A6, ADDED v2 — the business field a MAPPING line or a BUSINESS_FIELD tag names. */
    public static final String KEY_FIN_EVENT_BUSINESS_FIELD = "FIN_EVENT_BUSINESS_FIELD";

    /** SRS A6, ADDED v2 — a dimension tag's value source: CONSTANT or BUSINESS_FIELD. */
    public static final String KEY_FIN_DIMENSION_VALUE_SOURCE = "FIN_DIMENSION_VALUE_SOURCE";

    /**
     * MDL's own error code for "unknown or inactive {@code typeKey}" — the ONE MDL failure
     * XM-FIN-001's contract tells FIN to translate. Held as a local literal, deliberately: the
     * constant itself lives in {@code com.erp.mdl.exception.MdlErrorCodes}, an MDL <em>internal</em>
     * package, and importing it from FIN would be a cross-module boundary violation that
     * {@code CrossModuleBoundaryArchTest} fails (only {@code com.erp.mdl.crossmodule} is
     * consumable, and it publishes no error-code surface). This is not a FIN error code and is
     * never thrown from here — it is only compared against, so it is intentionally not registered
     * in {@code FinErrorCodes} or either message bundle; FIN's own thrown code stays
     * {@code FIN-400-INVALID-LOOKUP}.
     */
    private static final String MDL_404_TYPE_KEY = "MDL-404-TYPE-KEY";

    private final MdlLookupApi mdlLookupApi;

    /**
     * Rejects a submitted code that is not an active value of {@code typeKey}. Transaction
     * intent, here and in the two readers below: default REQUIRED propagation with
     * {@code readOnly = true}, so the in-process MDL read joins the calling FIN write
     * transaction and commits nothing of its own (MDL's side is itself a pure read).
     *
     * @param typeKey       one of the 13 FIN-owned keys declared above
     * @param submittedCode the code carried by the request
     * @throws LocalizedException {@code FIN-400-INVALID-LOOKUP} ({@code Status.VALIDATION_ERROR})
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public void assertValidCode(String typeKey, String submittedCode) {
        log.debug("Validating FIN lookup code for key: {}", typeKey);

        List<LookupOptionView> activeValues;
        try {
            activeValues = mdlLookupApi.readActiveValuesByKey(typeKey);
        } catch (LocalizedException ex) {
            // Match on MDL's specific code, not merely on Status.NOT_FOUND: only the documented
            // unknown/inactive-typeKey failure may be relabelled as FIN-400-INVALID-LOOKUP. Any
            // other NOT_FOUND escaping readActiveValuesByKey is an upstream fault, not invalid
            // client input, and is re-thrown untouched rather than silently mis-reported.
            if (ex.getStatus() == Status.NOT_FOUND
                && MDL_404_TYPE_KEY.equals(ex.getErrorCode())) {
                throw new LocalizedException(Status.VALIDATION_ERROR,
                    FinErrorCodes.FIN_400_INVALID_LOOKUP, typeKey);
            }
            throw ex;
        }

        boolean known = submittedCode != null && activeValues.stream()
            .anyMatch(option -> submittedCode.equals(option.code()));
        if (!known) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                FinErrorCodes.FIN_400_INVALID_LOOKUP, submittedCode);
        }
    }

    /**
     * The optional-column variant: a {@code null} or blank code is left alone (the column is
     * nullable in db-script-fin.md), anything present is validated exactly as above.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public void assertValidCodeIfPresent(String typeKey, String submittedCode) {
        if (submittedCode == null || submittedCode.isBlank()) {
            return;
        }
        assertValidCode(typeKey, submittedCode);
    }

    /**
     * Fact reader for the two rules that own their own rejection instead of the generic lookup
     * check (RULE-FIN-018, RULE-FIN-024; CORE.md "Lookup values"): is {@code code} an active
     * value of {@code typeKey}? Throws no FIN code itself — the Domain decides; an unknown or
     * inactive type key simply means "not an active value".
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public boolean isActiveCode(String typeKey, String code) {
        log.debug("Reading FIN lookup membership for key: {}", typeKey);

        List<LookupOptionView> activeValues;
        try {
            activeValues = mdlLookupApi.readActiveValuesByKey(typeKey);
        } catch (LocalizedException ex) {
            if (ex.getStatus() == Status.NOT_FOUND
                && MDL_404_TYPE_KEY.equals(ex.getErrorCode())) {
                return false;
            }
            throw ex;
        }
        return code != null && activeValues.stream()
            .anyMatch(option -> code.equals(option.code()));
    }
}
