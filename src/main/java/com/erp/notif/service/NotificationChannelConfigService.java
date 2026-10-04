package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.notif.domain.NotificationChannelConfigDomain;
import com.erp.notif.dto.ChannelCreateRequest;
import com.erp.notif.dto.ChannelResponse;
import com.erp.notif.dto.ChannelSearchRequest;
import com.erp.notif.dto.ChannelUpdateRequest;
import com.erp.notif.entity.NotificationChannelConfig;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.mapper.NotificationChannelConfigMapper;
import com.erp.notif.repository.NotificationChannelConfigRepository;
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
 * Orchestration for ENTITY-NOTIF-003 (NotificationChannelConfig) — API-NOTIF-005 (Channels CRUD /
 * enable-disable), SCR-NOTIF-002. RULE-NOTIF-006 (unique channelTypeId) delegates to
 * NotificationChannelConfigDomain. The channel has IS_ENABLED_FL only (no IS_ACTIVE_FL), so DELETE
 * maps to a soft {@code disable()} (204) rather than a deactivate.
 *
 * <p>No caching annotations — NOTIF is absent from the caching approved-register, so per
 * gov-enforce-caching-rules this service carries zero {@code @Cacheable}/{@code @CacheEvict}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationChannelConfigService {

    private final NotificationChannelConfigRepository repository;
    private final NotificationChannelConfigMapper mapper;

    /** Entity property names (isEnabled, NOT the DTO's isEnabledFl) — filter + sort whitelist. */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "channelTypeId", "isEnabled", "createdAt"
    );

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_CHANNELS_CREATE)")
    public ServiceResult<ChannelResponse> create(ChannelCreateRequest request) {
        log.info("Creating NotificationChannelConfig with channel: {}", request.getChannelTypeId());

        // RULE-NOTIF-006 pre-check against the normalized (uppercase) code — the entity's onCreate()
        // uppercases channelTypeId before insert, so the probe must match the stored form.
        boolean channelTaken = repository.existsByChannelTypeId(normalize(request.getChannelTypeId()));

        // Delegate the decision (RULE-NOTIF-006 uniqueness) to the Domain
        NotificationChannelConfigDomain.create(request.getChannelTypeId(), channelTaken);

        NotificationChannelConfig saved = repository.save(mapper.toEntity(request));
        log.info("Created NotificationChannelConfig ID: {}, channel: {}", saved.getId(), saved.getChannelTypeId());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_CHANNELS_VIEW)")
    public ServiceResult<Page<ChannelResponse>> search(ChannelSearchRequest searchRequest) {
        log.debug("Searching NotificationChannelConfig");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();

        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<NotificationChannelConfig> spec =
            SpecBuilder.build(commonRequest, allowedFields, DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        Page<NotificationChannelConfig> page = repository.findAll(spec, pageable);

        return ServiceResult.success(page.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_CHANNELS_VIEW)")
    public ServiceResult<ChannelResponse> getById(Long id) {
        log.debug("Fetching NotificationChannelConfig ID: {}", id);

        NotificationChannelConfig entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_CHANNEL_CONFIG_NOT_FOUND, id));

        return ServiceResult.success(mapper.toResponse(entity));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_CHANNELS_UPDATE)")
    public ServiceResult<ChannelResponse> update(Long id, ChannelUpdateRequest request) {
        log.info("Updating NotificationChannelConfig ID: {}", id);

        NotificationChannelConfig entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_CHANNEL_CONFIG_NOT_FOUND, id));

        // channelTypeId immutability (RULE-NOTIF-006) needs no guard — it is absent from the request.
        mapper.updateEntityFromRequest(entity, request);
        NotificationChannelConfig saved = repository.save(entity);
        log.info("Updated NotificationChannelConfig ID: {}", saved.getId());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-NOTIF-005 DELETE — soft disable. The channel entity carries IS_ENABLED_FL only (no
     * IS_ACTIVE_FL, per SRS A3 / CORE), so DELETE maps to {@code disable()} rather than a deactivate.
     * Returns void so the controller responds 204.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_NOTIF_CHANNELS_DELETE)")
    public void disable(Long id) {
        log.info("Disabling NotificationChannelConfig ID: {}", id);

        NotificationChannelConfig entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_CHANNEL_CONFIG_NOT_FOUND, id));

        entity.disable();
        repository.save(entity);
        log.info("Disabled NotificationChannelConfig ID: {}", id);
    }

    /**
     * Normalizes a caller-supplied channelTypeId to the canonical uppercase form the entity's
     * onCreate() always applies, so the uniqueness check matches the stored value.
     */
    private static String normalize(String channelTypeId) {
        return channelTypeId == null ? null : channelTypeId.trim().toUpperCase();
    }
}
