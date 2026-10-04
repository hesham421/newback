package com.erp.sec.report;

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
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.repository.AuditLogEntryRepository;
import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Core reference report {@code AUDIT_EVENT_LIST} (erp-core step 11): the tenant's audit events, newest
 * first, filtered by event type and time window.
 *
 * <p><b>Interim source.</b> The step file assigns this report to the audit module (step 10's
 * {@code CORE_AUDIT_EVENT}), which does not exist on this branch; until it does, the report reads the
 * only audit trail the core has, SEC's {@code SEC_AUDIT_LOG} ({@link AuditLogEntryRepository},
 * {@link SpecBuilder}, tenant-filtered by Hibernate). Its module code is already {@code AUDIT}, so its
 * permission {@code AUDIT:REPORT:AUDIT_EVENT_LIST} stays the same when the provider moves to
 * {@code com.erp.audit} and switches to the generic event store.
 */
@Component
@RequiredArgsConstructor
public class AuditEventListReport implements ReportProvider {

    public static final String CODE = "AUDIT_EVENT_LIST";
    public static final String MODULE = "AUDIT";
    /** {@code AUDIT:REPORT:AUDIT_EVENT_LIST}. */
    public static final String AUTHORITY = ReportAuthorities.of(MODULE, CODE);

    private static final Set<String> FILTER_FIELDS = Set.of("eventTypeCode", "occurredAt");

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("occurredAt", ColumnType.DATETIME, "وقت الحدث", "Occurred at"),
        new ReportColumn("eventType", ColumnType.STRING, "نوع الحدث", "Event type"),
        new ReportColumn("actor", ColumnType.STRING, "المستخدم", "Actor"),
        new ReportColumn("target", ColumnType.STRING, "الهدف", "Target"),
        new ReportColumn("detailsAr", ColumnType.STRING, "التفاصيل بالعربية", "Details (Arabic)"),
        new ReportColumn("detailsEn", ColumnType.STRING, "التفاصيل بالإنجليزية", "Details (English)"),
        new ReportColumn("ipAddress", ColumnType.STRING, "عنوان IP", "IP address"));

    private final AuditLogEntryRepository auditLogEntryRepository;

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
            ReportParam.of("eventType", ParamType.STRING, false, "نوع الحدث", "Event type"),
            ReportParam.of("occurredFrom", ParamType.DATETIME, false, "من وقت", "Occurred from"),
            ReportParam.of("occurredTo", ParamType.DATETIME, false, "حتى وقت", "Occurred to"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.report.AuditEventListReport).AUTHORITY)")
    public ReportResult run(Map<String, Object> params, Pageable page) {
        List<SearchFilter> filters = new ArrayList<>();
        if (params.get("eventType") != null) {
            filters.add(filter("eventTypeCode", SearchOperator.EQUALS,
                params.get("eventType").toString().toUpperCase(java.util.Locale.ROOT)));
        }
        if (params.get("occurredFrom") instanceof Instant from) {
            filters.add(filter("occurredAt", SearchOperator.GREATER_THAN_OR_EQUAL, from));
        }
        if (params.get("occurredTo") instanceof Instant to) {
            filters.add(filter("occurredAt", SearchOperator.LESS_THAN, to));
        }
        Specification<AuditLogEntry> spec = SpecBuilder.<AuditLogEntry>build(
                SearchRequest.builder().filters(filters).build(), new SetAllowedFields(FILTER_FIELDS),
                DefaultFieldValueConverter.INSTANCE)
            .and(fetchActor());

        Page<AuditLogEntry> events = auditLogEntryRepository.findAll(spec, PageRequest.of(page.getPageNumber(),
            page.getPageSize(), Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "auditLogPk"))));
        return new ReportResult(COLUMNS, events.getContent().stream().map(AuditEventListReport::toRow).toList(),
            Map.of("events", events.getTotalElements()), events.getTotalElements());
    }

    /** Fetches the actor with the events (one query instead of one per row); not on the count query. */
    private static Specification<AuditLogEntry> fetchActor() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("actor", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    private static Map<String, Object> toRow(AuditLogEntry event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("occurredAt", event.getOccurredAt());
        row.put("eventType", event.getEventTypeCode());
        row.put("actor", event.getActor() == null ? null : event.getActor().getUsername());
        row.put("target", event.getTargetRef());
        row.put("detailsAr", event.getDetailsAr());
        row.put("detailsEn", event.getDetailsEn());
        row.put("ipAddress", event.getIpAddress());
        return row;
    }

    private static SearchFilter filter(String field, SearchOperator operator, Object value) {
        return SearchFilter.builder().field(field).operator(operator).value(value).build();
    }
}
