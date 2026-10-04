package com.erp.report;

/**
 * One input parameter of a report (erp-core step 11).
 *
 * @param name      parameter name as sent in the request's {@code params} object
 * @param type      declared type; the value the provider receives is converted (see {@link ParamType})
 * @param required  whether the parameter must be present (a blank string counts as absent)
 * @param lookupKey the MDL lookup type key for {@link ParamType#LOOKUP}; {@code null} otherwise
 * @param labelAr   Arabic label
 * @param labelEn   English label
 */
public record ReportParam(String name, ParamType type, boolean required, String lookupKey,
                          String labelAr, String labelEn) {

    /** A non-lookup parameter. */
    public static ReportParam of(String name, ParamType type, boolean required, String labelAr, String labelEn) {
        return new ReportParam(name, type, required, null, labelAr, labelEn);
    }

    /** A {@link ParamType#LOOKUP} parameter validated against the active values of {@code lookupKey}. */
    public static ReportParam lookup(String name, String lookupKey, boolean required, String labelAr, String labelEn) {
        return new ReportParam(name, ParamType.LOOKUP, required, lookupKey, labelAr, labelEn);
    }
}
