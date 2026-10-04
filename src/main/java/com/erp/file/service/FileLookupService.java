package com.erp.file.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.dto.LookupOptionResponse;
import com.erp.file.exception.FileErrorCodes;
import com.erp.mdl.crossmodule.MdlLookupApi;
import com.erp.mdl.crossmodule.LookupOptionView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-FILE-008 — runtime resolution of the FILE-local LOVs (LOV-FILE-001 FILE_FILE_TYPE,
 * LOV-FILE-002 FILE_FILE_STATUS). These options and their bilingual labels (SRS A5) are now owned
 * by MDL (seeded by V20) and read live via {@link MdlLookupApi} — this service only guards which
 * keys it is responsible for and translates MDL's own not-found into this module's ERR-0006
 * NOT_FOUND, per the cross-module rule (never let {@code MDL_404_TYPE_KEY} leak out of this API).
 *
 * <p>{@code @PreAuthorize("isAuthenticated()")} — spec-mandated form (API-FILE-008 SECURITY =
 * "Security filter"): any authenticated caller may read these platform lookups; they are not gated
 * by a FILE permission.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileLookupService {

    public static final String LOOKUP_FILE_TYPE = "FILE_FILE_TYPE";
    public static final String LOOKUP_FILE_STATUS = "FILE_FILE_STATUS";

    // LOV-FILE-001 type-bucket codes — single source of truth, also consumed by
    // FileService.deriveFileType() so the classifier and this LOV can never drift. Left
    // untouched: they still drive the classifier; only the DISPLAY LIST below now comes from MDL.
    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_DOCUMENT = "DOCUMENT";
    public static final String TYPE_SPREADSHEET = "SPREADSHEET";
    public static final String TYPE_ARCHIVE = "ARCHIVE";
    public static final String TYPE_OTHER = "OTHER";

    private final MdlLookupApi mdlLookupApi;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<LookupOptionResponse>> get(String lookupKey) {
        log.debug("Resolving FILE lookup for key: {}", lookupKey);

        String normalized = lookupKey == null ? null : lookupKey.trim().toUpperCase();
        if (!LOOKUP_FILE_TYPE.equals(normalized) && !LOOKUP_FILE_STATUS.equals(normalized)) {
            // FILE only fronts its own two LOVs — never a generic pass-through for arbitrary
            // MDL keys it doesn't own, so reject before ever calling MDL.
            throw new LocalizedException(
                Status.NOT_FOUND, FileErrorCodes.FILE_LOOKUP_KEY_UNKNOWN, lookupKey);
        }

        List<LookupOptionView> values;
        try {
            values = mdlLookupApi.readActiveValuesByKey(normalized);
        } catch (LocalizedException ex) {
            if (ex.getStatus() == Status.NOT_FOUND) {
                // Translate MDL's own not-found (unseeded/deactivated type) into FILE's own
                // error code — MDL_404_TYPE_KEY must never leak out of this module's API.
                throw new LocalizedException(
                    Status.NOT_FOUND, FileErrorCodes.FILE_LOOKUP_KEY_UNKNOWN, lookupKey);
            }
            throw ex;
        }

        // MDL already orders by sortOrder (QR-MDL-011); LookupOptionResponse has no sortOrder
        // field, so it is intentionally dropped here.
        List<LookupOptionResponse> options = values.stream().map(FileLookupService::toResponse).toList();

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
