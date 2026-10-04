package com.erp.audit.service;

import com.erp.audit.dto.AuditEventResponse;
import com.erp.audit.entity.AuditEvent;
import com.erp.audit.mapper.AuditEventMapper;
import com.erp.audit.repository.AuditEventRepository;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.FieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
 * Staff query over the generic audit log (erp-core step 10) — {@code GET /api/v1/audit/events}. Rows
 * are tenant-filtered by Hibernate, newest first; filters go through the shared {@code SpecBuilder}.
 * Read-only: the log is written through {@code AuditApi} and the {@code @Audited} listener only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditEventService {

    /** Filterable and sortable fields. */
    private static final Set<String> ALLOWED_SORT_FIELDS =
        Set.of("entityType", "entityId", "actor", "action", "occurredAt");

    private static final String OCCURRED_AT = "occurredAt";

    /** The {@code from}/{@code to} bounds arrive as ISO-8601 instants; anything else is a 400. */
    private static final FieldValueConverter INSTANT_CONVERTER = (field, rawValue) -> {
        if (rawValue == null || rawValue instanceof Instant || !OCCURRED_AT.equals(field)) {
            return rawValue;
        }
        try {
            return Instant.parse(String.valueOf(rawValue).trim());
        } catch (DateTimeParseException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    };

    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;

    /**
     * Audit rows of the current tenant matching every given filter (each optional): exact
     * {@code entityType}, {@code entityId}, {@code actor}, {@code action}, and {@code occurredAt}
     * within [{@code from}, {@code to}] (ISO-8601 instants).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.audit.permission.AuditPermissions).AUDIT_EVENT_READ)")
    public ServiceResult<Page<AuditEventResponse>> search(String entityType, String entityId, String actor,
                                                          String action, String from, String to,
                                                          int page, int size) {
        log.debug("Searching audit events: entityType={}, entityId={}, actor={}, action={}",
            entityType, entityId, actor, action);

        List<SearchFilter> filters = new ArrayList<>();
        addFilter(filters, "entityType", SearchOperator.EQUALS, entityType);
        addFilter(filters, "entityId", SearchOperator.EQUALS, entityId);
        addFilter(filters, "actor", SearchOperator.EQUALS, actor);
        addFilter(filters, "action", SearchOperator.EQUALS, action);
        addFilter(filters, OCCURRED_AT, SearchOperator.GREATER_THAN_OR_EQUAL, from);
        addFilter(filters, OCCURRED_AT, SearchOperator.LESS_THAN_OR_EQUAL, to);
        SearchRequest request = SearchRequest.builder()
            .filters(filters)
            .sortField(OCCURRED_AT)
            .sortDirection(Sort.Direction.DESC)
            .page(page)
            .size(size)
            .build();

        Specification<AuditEvent> spec =
            SpecBuilder.build(request, new SetAllowedFields(ALLOWED_SORT_FIELDS), INSTANT_CONVERTER);
        Page<AuditEvent> result = repository.findAll(spec, PageableBuilder.from(request, ALLOWED_SORT_FIELDS));
        return ServiceResult.success(result.map(mapper::toResponse));
    }

    private static void addFilter(List<SearchFilter> filters, String field, SearchOperator operator, String value) {
        if (value != null && !value.isBlank()) {
            filters.add(SearchFilter.builder().field(field).operator(operator).value(value.trim()).build());
        }
    }
}
