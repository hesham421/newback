package com.erp.sec.crossmodule;

import com.erp.sec.repository.ModuleRegistryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The small dedicated implementation build-create-service's "Exposing this module to others"
 * section requires. RULE-MDL-001 / QR-MDL-012 need only an existence check, not a full read-model
 * or the registration workflow {@code RegistryService} owns, so this delegates straight to
 * {@link ModuleRegistryRepository} rather than to that service — the exposed surface (one
 * primitive-returning method) stays narrower than either.
 *
 * <p>{@code @Transactional(readOnly = true)}: a pure read on the producing side, never joining or
 * altering the caller's own write transaction.
 *
 * <p>No {@code @PreAuthorize}. This method is reached only from {@code LookupTypeService.create()}
 * (MDL), itself already {@code @PreAuthorize}-gated behind {@code PERM_MDL_LOOKUPS_CREATE} on a
 * real, authenticated HTTP request — the security context already travels with that synchronous
 * call, so this is not the "principal-less in-process caller" case build-create-service's
 * "Internal trusted-caller calls" section addresses (that case — e.g. NOTIF's dispatch reached
 * from SEC's unauthenticated password-reset flow — uses {@code InternalCallerContext} to install
 * a synthetic principal; there is no such gap here to paper over). This matches the actual,
 * un-gated posture of this module's only other inbound cross-module surface,
 * {@link SecUserDirectoryApi} / {@link SecUserDirectoryApiImpl} — the honest, consistent
 * baseline, not a new pattern invented for this method.
 */
@Component
@RequiredArgsConstructor
public class SecModuleRegistryApiImpl implements SecModuleRegistryApi {

    private final ModuleRegistryRepository moduleRegistryRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isModuleActive(String moduleCode) {
        return moduleRegistryRepository.existsByCodeAndIsActiveFlTrue(moduleCode);
    }
}
