package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.mdl.crossmodule.MdlLookupApi;
import com.erp.mdl.crossmodule.LookupOptionView;
import com.erp.notif.dto.LookupOptionResponse;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-NOTIF-006 — runtime resolution of the NOTIF-local LOVs (LOV-NOTIF-001 NOTIF_CHANNEL,
 * LOV-NOTIF-002 NOTIF_STATUS). These options and their bilingual labels (SRS A5) are now owned by
 * MDL (seeded by V20) and read live via {@link MdlLookupApi} — this service only guards which keys
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

    private final MdlLookupApi mdlLookupApi;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<LookupOptionResponse>> get(String lookupKey) {
        log.debug("Resolving NOTIF lookup for key: {}", lookupKey);

        String normalized = lookupKey == null ? null : lookupKey.trim().toUpperCase();
        if (!LOOKUP_NOTIF_CHANNEL.equals(normalized) && !LOOKUP_NOTIF_STATUS.equals(normalized)) {
            // NOTIF only fronts its own two LOVs — never a generic pass-through for arbitrary
            // MDL keys it doesn't own, so reject before ever calling MDL.
            throw new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_LOOKUP_KEY_UNKNOWN, lookupKey);
        }

        List<LookupOptionView> values;
        try {
            values = mdlLookupApi.readActiveValuesByKey(normalized);
        } catch (LocalizedException ex) {
            if (ex.getStatus() == Status.NOT_FOUND) {
                // Translate MDL's own not-found (unseeded/deactivated type) into NOTIF's own
                // error code — MDL_404_TYPE_KEY must never leak out of this module's API.
                throw new LocalizedException(
                    Status.NOT_FOUND, NotifErrorCodes.NOTIF_LOOKUP_KEY_UNKNOWN, lookupKey);
            }
            throw ex;
        }

        // MDL already orders by sortOrder (QR-MDL-011); LookupOptionResponse has no sortOrder
        // field, so it is intentionally dropped here.
        List<LookupOptionResponse> options = values.stream().map(NotificationLookupService::toResponse).toList();

        return ServiceResult.success(options);
    }

    private static LookupOptionResponse toResponse(LookupOptionView view) {
        return LookupOptionResponse.builder()
            .code(view.code())
            .labelAr(view.labelAr())
            .labelEn(view.labelEn())
            .build();
    }
}
