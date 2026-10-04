package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.notif.dto.NotificationLogResponse;
import com.erp.notif.dto.NotificationLogSearchRequest;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.mapper.NotificationLogMapper;
import com.erp.notif.repository.NotificationLogRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only orchestration for ENTITY-NOTIF-001 (NotificationLog) — API-NOTIF-002 (query) and
 * API-NOTIF-003 (by id), SCR-NOTIF-003 (VIEW only). The log is a system record written by
 * {@link DispatchService}; there is no create/update/delete surface here. Search returns an empty
 * page (200) when nothing matches — never 404.
 *
 * <p>No caching annotations — NOTIF is absent from the caching approved-register, so per
 * gov-enforce-caching-rules this service carries zero {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLogService {

    private final NotificationLogRepository repository;
    private final NotificationLogMapper mapper;

    /** Sort whitelist (PageableBuilder) per SVC-API — entity property names. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "createdAt", "sentAt", "notificationStatusId"
    );

    /** Filter whitelist (SpecBuilder) — the API-NOTIF-002 search criteria; entity property names. */
    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
        "recipientId", "moduleCode", "channelTypeId", "notificationStatusId", "referenceType", "sentAt"
    );

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_LOG_VIEW)")
    public ServiceResult<Page<NotificationLogResponse>> search(NotificationLogSearchRequest searchRequest) {
        log.debug("Searching NotificationLog");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_FILTER_FIELDS);
        Specification<NotificationLog> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<NotificationLog> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_LOG_VIEW)")
    public ServiceResult<NotificationLogResponse> getById(Long id) {
        log.debug("Fetching NotificationLog ID: {}", id);

        NotificationLog entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_LOG_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity));
    }
}
