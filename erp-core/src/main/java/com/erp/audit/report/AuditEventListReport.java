package com.erp.audit.report;

import com.erp.audit.entity.AuditEvent;
import com.erp.audit.repository.AuditEventRepository;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.report.ColumnType;
import com.erp.report.ParamType;
import com.erp.report.ReportAuthorities;
import com.erp.report.ReportColumn;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core reference report {@code AUDIT_EVENT_LIST} (erp-core step 11, audit module): the tenant's generic
 * audit events ({@code CORE_AUDIT_EVENT}, step 10), newest first, filtered by action, entity, actor and
 * time window. Read-only, through {@link AuditEventRepository} and {@link SpecBuilder} — tenant-filtered
 * by Hibernate, no native SQL. The {@code CHANGES} JSON is not a column (the per-event detail stays in
 * {@code GET /api/v1/audit/events}).
 *
 * <p>Permission {@code AUDIT:REPORT:AUDIT_EVENT_LIST} (screen {@code AUDIT_REPORTS} of the module
 * {@code AUDIT}, whose row and names {@code AuditPermissions} declares). It is independent of
 * {@code AUDIT:EVENT:READ}: a role may export the report without browsing the event API, and vice versa.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListReport implements ReportProvider {

    public static final String CODE = "AUDIT_EVENT_LIST";
    public static final String MODULE = "AUDIT";
    /** {@code AUDIT:REPORT:AUDIT_EVENT_LIST}. */
    public static final String AUTHORITY = ReportAuthorities.of(MODULE, CODE);

    private static final Set<String> FILTER_FIELDS = Set.of("action", "entityType", "entityId", "actor", "occurredAt");

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("occurredAt", ColumnType.DATETIME, "وقت الحدث", "Occurred at"),
        new ReportColumn("action", ColumnType.STRING, "الإجراء", "Action"),
        new ReportColumn("actor", ColumnType.STRING, "المستخدم", "Actor"),
        new ReportColumn("actorRealm", ColumnType.STRING, "نطاق المستخدم", "Actor realm"),
        new ReportColumn("entityType", ColumnType.STRING, "نوع الكيان", "Entity type"),
        new ReportColumn("entityId", ColumnType.STRING, "معرف الكيان", "Entity id"),
        new ReportColumn("summaryAr", ColumnType.STRING, "الملخص بالعربية", "Summary (Arabic)"),
        new ReportColumn("summaryEn", ColumnType.STRING, "الملخص بالإنجليزية", "Summary (English)"),
        new ReportColumn("ip", ColumnType.STRING, "عنوان IP", "IP address"),
        new ReportColumn("reference", ColumnType.STRING, "المرجع", "Reference"));

    private final AuditEventRepository auditEventRepository;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String moduleCode() {
        return MODULE;
    }

    @Override
    public String titleAr() {
        return "سجل أحداث التدقيق";
    }

    @Override
    public String titleEn() {
        return "Audit event list";
    }

    @Override
    public List<ReportParam> params() {
        return List.of(
            ReportParam.of("action", ParamType.STRING, false, "الإجراء", "Action"),
            ReportParam.of("entityType", ParamType.STRING, false, "نوع الكيان", "Entity type"),
            ReportParam.of("entityId", ParamType.STRING, false, "معرف الكيان", "Entity id"),
            ReportParam.of("actor", ParamType.STRING, false, "المستخدم", "Actor"),
            ReportParam.of("occurredFrom", ParamType.DATETIME, false, "من وقت", "Occurred from"),
            ReportParam.of("occurredTo", ParamType.DATETIME, false, "حتى وقت", "Occurred to"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.audit.report.AuditEventListReport).AUTHORITY)")
    public ReportResult run(Map<String, Object> params, Pageable page) {
        List<SearchFilter> filters = new ArrayList<>();
        if (params.get("action") != null) {
            filters.add(filter("action", SearchOperator.EQUALS, params.get("action").toString().toUpperCase(Locale.ROOT)));
        }
        if (params.get("entityType") != null) {
            filters.add(filter("entityType", SearchOperator.EQUALS, params.get("entityType").toString()));
        }
        if (params.get("entityId") != null) {
            filters.add(filter("entityId", SearchOperator.EQUALS, params.get("entityId").toString()));
        }
        if (params.get("actor") != null) {
            filters.add(filter("actor", SearchOperator.EQUALS, params.get("actor").toString()));
        }
        if (params.get("occurredFrom") instanceof Instant from) {
            filters.add(filter("occurredAt", SearchOperator.GREATER_THAN_OR_EQUAL, from));
        }
        if (params.get("occurredTo") instanceof Instant to) {
            filters.add(filter("occurredAt", SearchOperator.LESS_THAN, to));
        }
        Specification<AuditEvent> spec = SpecBuilder.build(SearchRequest.builder().filters(filters).build(),
            new SetAllowedFields(FILTER_FIELDS), DefaultFieldValueConverter.INSTANCE);

        Page<AuditEvent> events = auditEventRepository.findAll(spec, PageRequest.of(page.getPageNumber(),
            page.getPageSize(), Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new ReportResult(COLUMNS, events.getContent().stream().map(AuditEventListReport::toRow).toList(),
            Map.of("events", events.getTotalElements()), events.getTotalElements());
    }

    private static Map<String, Object> toRow(AuditEvent event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("occurredAt", event.getOccurredAt());
        row.put("action", event.getAction());
        row.put("actor", event.getActor());
        row.put("actorRealm", event.getActorRealm());
        row.put("entityType", event.getEntityType());
        row.put("entityId", event.getEntityId());
        row.put("summaryAr", event.getSummaryAr());
        row.put("summaryEn", event.getSummaryEn());
        row.put("ip", event.getIp());
        row.put("reference", event.getReference());
        return row;
    }

    private static SearchFilter filter(String field, SearchOperator operator, Object value) {
        return SearchFilter.builder().field(field).operator(operator).value(value).build();
    }
}
