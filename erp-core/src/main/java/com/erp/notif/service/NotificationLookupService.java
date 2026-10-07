package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.lookup.LookupOptionResponse;
import com.erp.common.lookup.OwnedLookups;
import com.erp.mdl.crossmodule.MdlLookupApi;
import com.erp.mdl.crossmodule.LookupOptionView;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-NOTIF-006 — runtime resolution of the NOTIF-local LOVs (LOV-NOTIF-001 NOTIF_CHANNEL,
 * LOV-NOTIF-002 NOTIF_STATUS). These options and their bilingual labels (SRS A5) are now owned by
 * MDL (seeded by V8__mdl_seed.sql) and read live via {@link MdlLookupApi} — this service only guards which keys
 * it is responsible for and translates MDL's own not-found into this module's ERR-0004 NOT_FOUND,
 * per the cross-module rule (never let {@code MDL_404_TYPE_KEY} leak out of this API).
 *
 * <p>{@code @PreAuthorize("isAuthenticated()")} — deliberate, spec-mandated form (API-NOTIF-006
 * SECURITY = "Security filter"): any authenticated caller may read these platform lookups; they are
 * not gated by SCR-NOTIF-* permissions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationLookupService {

    public static final String LOOKUP_NOTIF_CHANNEL = "NOTIF_CHANNEL";
    public static final String LOOKUP_NOTIF_STATUS = "NOTIF_STATUS";

    /** The two LOVs this module fronts; any other key is refused before MDL is called. */
    private static final Set<String> OWNED_KEYS = Set.of(LOOKUP_NOTIF_CHANNEL, LOOKUP_NOTIF_STATUS);

    private final MdlLookupApi mdlLookupApi;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<LookupOptionResponse>> get(String lookupKey) {
        log.debug("Resolving NOTIF lookup for key: {}", lookupKey);
        // MDL already orders by sortOrder (QR-MDL-011); LookupOptionResponse has no sortOrder
        // field, so it is intentionally dropped here.
        return ServiceResult.success(OwnedLookups.read(lookupKey, OWNED_KEYS, NotifErrorCodes.NOTIF_LOOKUP_KEY_UNKNOWN,
            key -> mdlLookupApi.readActiveValuesByKey(key).stream().map(NotificationLookupService::toResponse).toList()));
    }

    private static LookupOptionResponse toResponse(LookupOptionView view) {
        return LookupOptionResponse.builder()
            .code(view.code())
            .labelAr(view.labelAr())
            .labelEn(view.labelEn())
            .build();
    }
}
