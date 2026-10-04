package com.erp.mdl.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.mdl.dto.LookupValueResponse;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.entity.LookupValue;
import com.erp.mdl.exception.MdlErrorCodes;
import com.erp.mdl.mapper.LookupValueMapper;
import com.erp.mdl.repository.LookupTypeRepository;
import com.erp.mdl.repository.LookupValueRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-MDL-011 — the consumer-facing read (SVC-API-SEARCH.md). A dedicated service, not a method
 * on {@link LookupTypeService}/{@link LookupValueService}: the spec's own "controller →
 * LookupConsumerController ; service → LookupConsumerService" line, and this is a cross-entity
 * orchestration (resolve type by key, then load the type's values) that belongs to neither
 * entity's own CRUD service.
 *
 * <p>No caching — same reasoning as {@link LookupTypeService}/{@link LookupValueService}: MDL
 * entities are absent from the cache-eligibility register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupConsumerService {

    private final LookupTypeRepository lookupTypeRepository;
    private final LookupValueRepository lookupValueRepository;
    private final LookupValueMapper mapper;

    /**
     * RULE-MDL-004 — resolve the type by key (QR-MDL-015); unknown key OR a key resolving to an
     * inactive type both surface as {@code MDL-404-TYPE-KEY} (API-MDL-011's Errors line does not
     * distinguish the two cases). Otherwise load the type's active values ordered by sortOrder
     * (QR-MDL-011) — an empty list is a valid success when the type is active but has zero
     * active values.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_MDL_LOOKUPS_VIEW)")
    public ServiceResult<List<LookupValueResponse>> readByKey(String key) {
        log.debug("Resolving LookupType by key: {}", key);

        Optional<LookupType> found = lookupTypeRepository.findByKey(key);
        if (found.isEmpty() || !Boolean.TRUE.equals(found.get().getIsActiveFl())) {
            throw new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE_KEY, key);
        }
        LookupType type = found.get();

        List<LookupValue> values =
            lookupValueRepository.findActiveValuesOfActiveType(type.getLookupTypePk(), Boolean.TRUE);

        return ServiceResult.success(values.stream().map(mapper::toResponse).toList());
    }
}
