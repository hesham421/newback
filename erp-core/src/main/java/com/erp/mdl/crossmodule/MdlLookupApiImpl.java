package com.erp.mdl.crossmodule;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.entity.LookupValue;
import com.erp.mdl.exception.MdlErrorCodes;
import com.erp.mdl.repository.LookupTypeRepository;
import com.erp.mdl.repository.LookupValueRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The small dedicated implementation build-create-service's "Exposing this module to others"
 * section requires. Replicates API-MDL-011's own resolution logic
 * ({@code LookupConsumerService.readByKey}: resolve {@code LookupType} by key, confirm
 * {@code isActiveFl}, else {@code MDL-404-TYPE-KEY}; otherwise load the type's active values
 * ordered by {@code sortOrder}) but reached via direct repository access — {@link
 * LookupTypeRepository} / {@link LookupValueRepository}, not {@code LookupConsumerService} or its
 * {@code LookupValueMapper} — because that internal service is gated
 * {@code @PreAuthorize(...PERM_MDL_LOOKUPS_VIEW)}, a permission NOTIF/FILE's own calling services
 * do not hold (they run under {@code @PreAuthorize("isAuthenticated()")} only). This mirrors how
 * {@code SecModuleRegistryApiImpl} deliberately bypasses {@code RegistryService} for the same
 * reason, rather than reusing its internal DTO/mapper.
 *
 * <p>{@code @Transactional(readOnly = true)}: a pure read on the producing side, never joining or
 * altering the caller's own write transaction.
 *
 * <p>No {@code @PreAuthorize}. Confirmed by grepping this codebase for an existing
 * internal-trusted-caller utility before concluding this: {@code
 * com.erp.sec.security.InternalCallerContext} exists, but it exists specifically for a
 * <em>principal-less</em> in-process caller (its own precedent use is NOTIF's dispatch reached
 * from SEC's unauthenticated password-reset flow — see {@code PasswordResetService}). That is not
 * this case: NOTIF/FILE's own calling services are reached from a real, authenticated HTTP
 * request (they require {@code isAuthenticated()}), so a genuine security context already travels
 * with the synchronous call into this method — re-gating here would be redundant, exactly the
 * reasoning {@code SecModuleRegistryApiImpl} documents for its own un-gated
 * {@code isModuleActive}. No internal-trusted-caller utility fits this shape (an
 * already-authenticated caller lacking only an MDL-specific permission), so none is used.
 */
@Component
@RequiredArgsConstructor
public class MdlLookupApiImpl implements MdlLookupApi {

    private final LookupTypeRepository lookupTypeRepository;
    private final LookupValueRepository lookupValueRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LookupOptionView> readActiveValuesByKey(String typeKey) {
        Optional<LookupType> found = lookupTypeRepository.findByKey(typeKey);
        if (found.isEmpty() || !Boolean.TRUE.equals(found.get().getIsActiveFl())) {
            throw new LocalizedException(Status.NOT_FOUND, MdlErrorCodes.MDL_404_TYPE_KEY, typeKey);
        }
        LookupType type = found.get();

        List<LookupValue> values = lookupValueRepository.findActiveValuesOfActiveType(
            type.getLookupTypePk(), Boolean.TRUE);

        return values.stream()
            .map(v -> new LookupOptionView(v.getCode(), v.getNameAr(), v.getNameEn(), v.getSortOrder()))
            .toList();
    }
}
