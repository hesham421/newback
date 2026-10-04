package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.sec.domain.RoleActionGrantDomain;
import com.erp.sec.dto.ActiveSessionsCountResponse;
import com.erp.sec.dto.AuditLogEntryResponse;
import com.erp.sec.dto.DashboardResponse;
import com.erp.sec.dto.FailedLoginsResponse;
import com.erp.sec.dto.OnboardingFunnelResponse;
import com.erp.sec.dto.RolesPermissionsSummaryResponse;
import com.erp.sec.dto.UsersOverviewResponse;
import com.erp.sec.mapper.AuditLogEntryMapper;
import com.erp.sec.mapper.RoleMapper;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.SignupRequestRepository;
import com.erp.sec.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-SEC-022. Every figure is computed on the call (REQ-SEC-022, "never cached"), and a widget the
 * caller has no source-screen VIEW for is left unset so it disappears from the JSON (REQ-SEC-023).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    /** USER_STATUS / SIGNUP_STATUS / AUDIT_EVENT_TYPE codes this summary counts (SRS A6). */
    private static final String USER_STATUS_ACTIVE = "ACTIVE";
    private static final String USER_STATUS_DISABLED = "DISABLED";
    private static final String SIGNUP_STATUS_PENDING = "PENDING";
    private static final String EVENT_LOGIN_FAILED = "LOGIN_FAILED";

    private static final Duration FAILED_LOGIN_WINDOW = Duration.ofHours(24);

    /** REQ-SEC-022 says "the last N AuditLogEntry" without fixing N; this is that unspecified N. */
    private static final int RECENT_ACTIVITY_LIMIT = 10;

    /** No SEC artifact says when a PENDING sign-up counts as stalled; this is that unspecified age. */
    private static final Duration STALLED_SIGNUP_AGE = Duration.ofDays(7);

    private final MenuService menuService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleActionGrantRepository roleActionGrantRepository;
    private final SignupRequestRepository signupRequestRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final AuditLogEntryMapper auditLogEntryMapper;
    private final RoleMapper roleMapper;

    /**
     * The per-widget conditions are output filtering driven by the caller's grants, not business
     * rules — there is no permit/deny decision here, so no Domain object is involved.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_DASHBOARD_VIEW)")
    public ServiceResult<DashboardResponse> summary() {
        log.debug("Composing the security dashboard summary");

        Set<String> permissions = menuService.effectivePermissionCodes().getData();
        Instant now = Instant.now();
        DashboardResponse.DashboardResponseBuilder summary = DashboardResponse.builder();

        if (permissions.contains(PermissionConstants.PERM_SEC_USERS_VIEW)) {
            summary.usersOverview(usersOverview());
            summary.onboardingFunnel(onboardingFunnel(now));
        }
        if (permissions.contains(PermissionConstants.PERM_SEC_AUDIT_LOG_VIEW)) {
            summary.failedLogins24h(failedLogins24h(now));
            summary.recentActivity(recentActivity());
        }
        if (permissions.contains(PermissionConstants.PERM_SEC_SESSIONS_VIEW)) {
            summary.activeSessions(activeSessions());
        }
        if (permissions.contains(PermissionConstants.PERM_SEC_ROLES_VIEW)) {
            summary.rolesPermissionsSummary(rolesPermissionsSummary());
        }

        return ServiceResult.success(summary.build());
    }

    private UsersOverviewResponse usersOverview() {
        return UsersOverviewResponse.builder()
            .total(userRepository.countAllUsers())
            .active(userRepository.countByStatus(USER_STATUS_ACTIVE))
            .disabled(userRepository.countByStatus(USER_STATUS_DISABLED))
            .pendingSignups(signupRequestRepository.countByStatus(SIGNUP_STATUS_PENDING))
            .build();
    }

    private FailedLoginsResponse failedLogins24h(Instant now) {
        return FailedLoginsResponse.builder()
            .count(auditLogEntryRepository.countByEventTypeSince(
                EVENT_LOGIN_FAILED, now.minus(FAILED_LOGIN_WINDOW)))
            .build();
    }

    private ActiveSessionsCountResponse activeSessions() {
        return ActiveSessionsCountResponse.builder()
            .count(activeSessionRepository.countNonTerminated())
            .build();
    }

    /** The repository query carries the ordering; the shared builder supplies only the page cap. */
    private List<AuditLogEntryResponse> recentActivity() {
        SearchRequest topN = SearchRequest.builder().size(RECENT_ACTIVITY_LIMIT).build();
        return auditLogEntryRepository.findRecent(PageableBuilder.from(topN, Set.of())).stream()
            .map(auditLogEntryMapper::toResponse)
            .toList();
    }

    private RolesPermissionsSummaryResponse rolesPermissionsSummary() {
        return RolesPermissionsSummaryResponse.builder()
            .roleCount(roleRepository.countAllRoles())
            .privilegedRoleCount(roleActionGrantRepository.countPrivilegedRoles(
                RoleActionGrantDomain.GATEWAY_ACTION_CODE))
            .usersPerRole(roleRepository.findUserCountsPerRole().stream()
                .map(roleMapper::toUserCountResponse)
                .toList())
            .build();
    }

    private OnboardingFunnelResponse onboardingFunnel(Instant now) {
        return OnboardingFunnelResponse.builder()
            .pendingSignups(signupRequestRepository.countByStatus(SIGNUP_STATUS_PENDING))
            .stalledCount(signupRequestRepository.countStalled(
                SIGNUP_STATUS_PENDING, now.minus(STALLED_SIGNUP_AGE)))
            .build();
    }
}
