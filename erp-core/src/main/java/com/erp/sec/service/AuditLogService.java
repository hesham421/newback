package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.sec.dto.AuditLogEntryResponse;
import com.erp.sec.dto.AuditLogEntrySearchRequest;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.User;
import com.erp.sec.mapper.AuditLogEntryMapper;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only orchestration for ENT-SEC-011 — API-SEC-023 (search) and API-SEC-024 (export), which
 * share one filter set and one permission. The export is unpaged by contract, so it swaps the
 * shared {@code PageableBuilder} for a plain {@code Sort} on the same specification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    /** The API-SEC-023 filter set this export reuses; also the only sortable fields (A.5.6). */
    private static final Set<String> ALLOWED_SORT_FIELDS =
        Set.of("eventTypeCode", "actor", "occurredAt");

    /** {@code actor} is an association, so a client-supplied filter may only target the scalars. */
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of("eventTypeCode", "occurredAt");

    private static final Set<String> INSTANT_FILTER_FIELDS = Set.of("occurredAt");

    private static final String CSV_HEADER = "auditLogPk,eventTypeCode,actorUserId,occurredAt,"
        + "targetRef,detailsAr,detailsEn,ipAddress";

    /**
     * UTF-8 byte-order mark. Excel reads a {@code .csv} with no BOM as ANSI regardless of the
     * HTTP {@code charset}, which renders the Arabic {@code detailsAr} column as mojibake; the
     * bilingual mandate covers exported artifacts, so the document carries it. It is part of the
     * rendered document rather than the controller's headers, so every consumer of
     * {@link #export} gets it.
     */
    private static final String UTF8_BOM = "\uFEFF";

    /** Leading characters a spreadsheet treats as the start of a formula — see {@link #cell}. */
    private static final String FORMULA_TRIGGERS = "=+-@\t\r";

    private final AuditLogEntryRepository repository;
    private final UserRepository userRepository;
    private final AuditLogEntryMapper mapper;

    /** API-SEC-023 — rows are returned exactly as stored (REQ-SEC-025, "without altering any of them"). */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_AUDIT_LOG_VIEW)")
    public ServiceResult<Page<AuditLogEntryResponse>> search(AuditLogEntrySearchRequest searchRequest) {
        log.debug("Searching audit log");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SecSearchSupport.assertSortAllowed(commonRequest.getSortField(), ALLOWED_SORT_FIELDS);

        Specification<AuditLogEntry> spec = SpecBuilder.build(commonRequest,
            new SetAllowedFields(ALLOWED_FILTER_FIELDS),
            SecSearchSupport.instantFieldConverter(INSTANT_FILTER_FIELDS));
        Long actorUserId = searchRequest.getActorUserId();
        if (actorUserId != null) {
            spec = spec.and(actedBy(actorUserId));
        }

        Page<AuditLogEntry> result =
            repository.findAll(spec, PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS));

        return ServiceResult.success(result.map(mapper::toResponse));
    }

    /**
     * API-SEC-024. Returns the rendered CSV document itself — the controller streams it as
     * {@code text/csv} rather than wrapping it in the JSON envelope (Response line).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_AUDIT_LOG_VIEW)")
    public ServiceResult<String> export(String eventTypeCode,
                                        Long actorUserId,
                                        Instant occurredFrom,
                                        Instant occurredTo) {
        log.debug("Exporting audit log: eventTypeCode={}, actorUserId={}", eventTypeCode, actorUserId);

        SearchRequest commonRequest = SearchRequest.builder()
            .filters(buildFilters(eventTypeCode, actorUserId, occurredFrom, occurredTo))
            .build();
        Specification<AuditLogEntry> spec = SpecBuilder.build(commonRequest,
            new SetAllowedFields(ALLOWED_SORT_FIELDS), DefaultFieldValueConverter.INSTANCE);

        List<AuditLogEntry> rows = repository.findAll(spec, Sort.by(Sort.Direction.ASC, "occurredAt"));

        return ServiceResult.success(toCsv(rows));
    }

    /** The one filter set API-SEC-023 and API-SEC-024 share (SCR-REQ-SEC-008 B2). */
    private List<SearchFilter> buildFilters(String eventTypeCode, Long actorUserId,
                                            Instant occurredFrom, Instant occurredTo) {
        List<SearchFilter> filters = new ArrayList<>();
        SecSearchSupport.addFilter(filters, "eventTypeCode", SearchOperator.EQUALS, eventTypeCode);
        SecSearchSupport.addFilter(filters, "actor", SearchOperator.EQUALS, actorReference(actorUserId));
        SecSearchSupport.addFilter(filters, "occurredAt", SearchOperator.GREATER_THAN_OR_EQUAL, occurredFrom);
        SecSearchSupport.addFilter(filters, "occurredAt", SearchOperator.LESS_THAN_OR_EQUAL, occurredTo);
        return filters;
    }

    /**
     * DBF-SEC-085 is an association, so the filter value must be a {@code User} reference rather
     * than a raw id; the proxy is never initialized — the criteria comparison reads its id only.
     */
    private User actorReference(Long actorUserId) {
        return actorUserId == null ? null : userRepository.getReferenceById(actorUserId);
    }

    /** API-SEC-023's actor filter, for the same reason, as a predicate on the client-supplied id. */
    private Specification<AuditLogEntry> actedBy(Long actorUserId) {
        User reference = actorReference(actorUserId);
        return (root, query, cb) -> cb.equal(root.get("actor"), reference);
    }

    private String toCsv(List<AuditLogEntry> rows) {
        StringBuilder csv = new StringBuilder(UTF8_BOM).append(CSV_HEADER);
        for (AuditLogEntry row : rows) {
            User actor = row.getActor();
            csv.append('\n')
                .append(cell(row.getAuditLogPk())).append(',')
                .append(cell(row.getEventTypeCode())).append(',')
                .append(cell(actor == null ? null : actor.getUserPk())).append(',')
                .append(cell(row.getOccurredAt())).append(',')
                .append(cell(row.getTargetRef())).append(',')
                .append(cell(row.getDetailsAr())).append(',')
                .append(cell(row.getDetailsEn())).append(',')
                .append(cell(row.getIpAddress()));
        }
        return csv.toString();
    }

    /**
     * RFC 4180 quoting, plus formula-injection neutralization: a value whose first character is
     * one a spreadsheet reads as the start of a formula ({@code = + - @}, TAB, CR) is prefixed
     * with an apostrophe so the sheet renders it as text. Audit rows carry unauthenticated input
     * verbatim — {@code AuthService.login} stores the submitted username as the {@code targetRef}
     * of every {@code LOGIN_FAILED} row — so the export must not hand an admin's spreadsheet a
     * formula an anonymous caller chose.
     */
    private String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (!text.isEmpty() && FORMULA_TRIGGERS.indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
