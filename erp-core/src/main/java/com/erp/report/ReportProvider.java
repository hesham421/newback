package com.erp.report;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

/**
 * The reporting SPI (erp-core step 11). A module or an application registers a report by exposing one
 * Spring bean implementing this interface; {@code ReportRegistry} collects every such bean at startup
 * (a duplicate {@link #code()} fails the startup) and the core endpoints under
 * {@code /api/v1/report/**} list, validate, run and export it — permission, tenant and export handling
 * are done once, by the core.
 *
 * <p><b>Permission.</b> Every report gets the permission {@code <MODULE>:REPORT:<CODE>}
 * ({@link ReportAuthorities#of(String, String)}), contributed to the RBAC catalog automatically; a
 * caller sees and runs only reports whose permission it holds (super roles hold all).
 *
 * <p><b>Tenant.</b> {@link #run} executes inside the caller's request, with the tenant set: query
 * through JPA repositories / {@code SpecBuilder} so Hibernate's tenant filter applies; never use native
 * SQL or JDBC without a {@code TENANT_ID} predicate.
 *
 * <p><b>Parameters.</b> {@link #run} receives only declared parameters, already validated and converted
 * to the Java types listed in {@link ParamType}; an absent optional parameter is missing from the map.
 */
public interface ReportProvider {

    /** Unique report code, upper snake case, at most 40 characters, e.g. {@code "SEC_USER_LIST"}. */
    String code();

    /** Owning module code (at most 10 characters); the permission is {@code <MODULE>:REPORT:<CODE>}. */
    String moduleCode();

    /** Arabic title. */
    String titleAr();

    /** English title. */
    String titleEn();

    /** Declared parameters, in display order. */
    List<ReportParam> params();

    /**
     * Runs the report.
     *
     * @param params validated, converted parameters
     * @param page   the page to return; the export endpoint passes one page as large as its row cap
     *               plus one, so a provider must honour the page size
     * @return the columns, the page's rows, optional totals and (when known) the total row count
     */
    ReportResult run(Map<String, Object> params, Pageable page);
}
