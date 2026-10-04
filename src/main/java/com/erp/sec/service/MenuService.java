package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.util.SecurityContextHelper;
import com.erp.sec.domain.RoleActionGrantDomain;
import com.erp.sec.dto.ModuleMenuResponse;
import com.erp.sec.dto.ScreenMenuResponse;
import com.erp.sec.entity.User;
import com.erp.sec.mapper.ModuleRegistryMapper;
import com.erp.sec.mapper.ScreenRegistryMapper;
import com.erp.sec.repository.EffectiveGrantProjection;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.sec.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-SEC-027 and the single effective-grant read path CORE.md's REQ-SEC-033 paragraph names.
 * API-SEC-022's per-widget filter consumes {@link #effectivePermissionCodes()} rather than
 * repeating the query. No caching: SEC is absent from the approved register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final UserRepository userRepository;
    private final ModuleRegistryRepository moduleRepository;
    private final ScreenRegistryRepository screenRepository;
    private final RoleActionGrantRepository roleActionGrantRepository;
    private final ModuleRegistryMapper moduleMapper;
    private final ScreenRegistryMapper screenMapper;

    /**
     * API-SEC-027. Gated on authentication alone: SRS B4 gives this component no page code, so its
     * content — not a permission on itself — is the security boundary.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<ModuleMenuResponse>> effective() {
        log.debug("Resolving the effective menu of the authenticated caller");

        Optional<User> caller = resolveCaller();
        if (caller.isEmpty()) {
            return ServiceResult.success(List.of());
        }
        Long userPk = caller.get().getUserPk();

        Map<Long, List<ScreenMenuResponse>> screensByModule =
            screenRepository.findEffectiveScreensForUser(userPk).stream()
                .collect(Collectors.groupingBy(screen -> screen.getModule().getModuleRegPk(),
                    Collectors.mapping(screenMapper::toMenuResponse, Collectors.toList())));

        List<ModuleMenuResponse> menu = moduleRepository.findEffectiveModulesForUser(userPk).stream()
            .map(module -> moduleMapper.toMenuResponse(module, screensByModule.getOrDefault(
                module.getModuleRegPk(), List.of())))
            .toList();

        return ServiceResult.success(menu);
    }

    /** The permission-code shape of the same read path, consumed by API-SEC-022 (REQ-SEC-023). */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Set<String>> effectivePermissionCodes() {
        log.debug("Resolving the effective permission codes of the authenticated caller");

        Set<String> codes = resolveCaller()
            .map(caller -> Set.copyOf(
                roleActionGrantRepository.findEffectivePermissionCodesForUser(caller.getUserPk())))
            .orElseGet(Set::of);

        return ServiceResult.success(codes);
    }

    /**
     * The same read path with RULE-SEC-007 applied: a non-VIEW permission survives only while its
     * screen's VIEW is also held. REQ-SEC-033's {@code JwtAuthenticationFilter} installs exactly
     * this set as the caller's authorities.
     *
     * <p>The screen of each held code is the registry's {@code SEC_SCREEN_REG} row, read in the
     * very same query as the codes themselves (QR-SEC-027's registry shape) — never derived from
     * the permission code's text. A code's action may be any number of words
     * ({@code PERM_FIN_PERIODS_CLOSE_APPROVE} on screen {@code FIN_PERIODS}, gated by
     * {@code PERM_FIN_PERIODS_VIEW}) and still resolve to the right screen, which a split on the
     * code's last underscore cannot do. One query per call, no per-code lookup.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Set<String>> effectiveAuthorityCodes() {
        log.debug("Resolving the gateway-applied permission codes of the authenticated caller");

        List<EffectiveGrantProjection> grants = resolveCaller()
            .map(caller -> roleActionGrantRepository.findEffectiveGrantsForUser(caller.getUserPk()))
            .orElseGet(List::of);

        Set<Long> screensWithGateway = grants.stream()
            .filter(grant -> RoleActionGrantDomain.isGatewayAction(grant.actionCode()))
            .map(EffectiveGrantProjection::screenRegPk)
            .collect(Collectors.toUnmodifiableSet());

        Set<String> codes = grants.stream()
            .filter(grant -> RoleActionGrantDomain.isGatewayAction(grant.actionCode())
                || screensWithGateway.contains(grant.screenRegPk()))
            .map(EffectiveGrantProjection::permissionCode)
            .collect(Collectors.toUnmodifiableSet());

        return ServiceResult.success(codes);
    }

    /**
     * Resolves the SEC_USER row behind the authenticated caller. Both callers are gated on
     * {@code isAuthenticated()}, and since SEC-BE {@code JwtAuthenticationFilter} publishes the
     * access token's subject — the username — as the principal, so this lookup normally hits the
     * very row the filter already resolved; {@code getCurrentUsername()}'s {@code "system"}
     * fallback is unreachable from here. An unmatched principal still yields an empty Optional
     * rather than a throw, so both callers degrade to an empty result instead of a 404 — neither
     * API contracts a not-found error (both list the platform-standard INTERNAL_ERROR only).
     */
    private Optional<User> resolveCaller() {
        return userRepository.findByUsername(SecurityContextHelper.getCurrentUsername());
    }
}
