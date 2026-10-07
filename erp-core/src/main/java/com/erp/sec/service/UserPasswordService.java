package com.erp.sec.service;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.events.DomainEventPublisher;
import com.erp.events.UserPasswordChangedEvent;
import com.erp.sec.crossmodule.RecoveryTarget;
import com.erp.sec.domain.PasswordPolicy;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.dto.AdminPasswordSetRequest;
import com.erp.sec.dto.PasswordChangeRequest;
import com.erp.sec.dto.PasswordChangeResponse;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.UserMapper;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * tenant-maturity D — the two STAFF password changes: an administrator sets another user's password
 * (REQ-SEC-083) and a user changes their own (REQ-SEC-085); tenant-maturity B adds the platform's recovery of a
 * tenant's super user (REQ-SEC-091). Each applies the password policy (RULE-SEC-056), ends sessions, records a
 * generic-audit row and publishes {@link UserPasswordChangedEvent}. Raw passwords are never logged, stored,
 * returned, audited or put on the event. No caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPasswordService {

    static final String ACTION_PASSWORD_SET_BY_ADMIN = "PASSWORD_SET_BY_ADMIN";
    static final String ACTION_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    static final String ACTION_ADMIN_PASSWORD_RESET = "ADMIN_PASSWORD_RESET";

    private static final String FIELD_NEW_PASSWORD = "newPassword";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyProvider passwordPolicyProvider;
    private final UserSessionTerminator sessionTerminator;
    private final JwtTokenValidator jwtTokenValidator;
    private final DomainEventPublisher eventPublisher;
    private final AuditApi auditApi;

    /**
     * REQ-SEC-083 — {@code PUT /api/v1/sec/users/{id}/password}: STAFF target only (404 otherwise), never
     * the caller (RULE-SEC-057), policy, flag per RULE-SEC-058, every session of the user ended.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<PasswordChangeResponse> setPassword(Long id, AdminPasswordSetRequest request) {
        log.info("Setting the password of User ID: {} by an administrator", id);

        User user = repository.findStaffById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, id));
        Optional<User> caller = repository.findByUsername(SecurityContextHelper.getCurrentUsername());
        UserDomain.assertNotSelfForAdminPasswordSet(caller.map(c -> c.getUserPk().equals(id)).orElse(false));
        PasswordPolicy policy = passwordPolicyProvider.current();
        policy.assertAcceptable(FIELD_NEW_PASSWORD, request.getNewPassword());

        user.changePassword(passwordEncoder.encode(request.getNewPassword()),
            UserDomain.passwordChangeRequiredFor(request.getRequireChangeAtNextLogin()), Instant.now());
        User saved = repository.save(user);
        int terminated = sessionTerminator.terminateOpenSessions(saved, null,
            "إنهاء الجلسة لأن المسؤول عيّن كلمة مرور جديدة", "Session terminated because an administrator set a new password");

        auditApi.record(AuditEntry.builder()
            .action(ACTION_PASSWORD_SET_BY_ADMIN)
            .actorUserId(caller.map(User::getUserPk).orElse(null))
            .entityType(SecAuditEntries.ENTITY_TYPE_USER)
            .entityId(String.valueOf(saved.getUserPk()))
            .summaryAr("تعيين كلمة مرور المستخدم " + saved.getUsername() + " من المسؤول")
            .summaryEn("Password of user " + saved.getUsername() + " set by an administrator")
            .build());
        eventPublisher.publish(new UserPasswordChangedEvent(saved.getUserPk(), true));
        log.info("Password of User ID: {} set by an administrator; change required: {}; sessions terminated: {}",
            saved.getUserPk(), saved.getPasswordChangeRequiredFl(), terminated);

        return ServiceResult.success(mapper.toPasswordChangeResponse(saved, terminated), Status.UPDATED);
    }

    /**
     * REQ-SEC-085 — {@code PUT /api/v1/sec/me/password}: current password (RULE-SEC-060), policy, flag
     * cleared, the user's other sessions ended (the caller's own, the token's {@code jti}, stays).
     */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<PasswordChangeResponse> changeOwnPassword(PasswordChangeRequest request,
                                                                   String authorizationHeader) {
        User user = repository.findByUsername(SecurityContextHelper.getCurrentUsername())
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecErrorCodes.SEC_404_USER,
                SecurityContextHelper.getCurrentUsername()));
        log.info("User ID: {} is changing their own password", user.getUserPk());

        UserDomain.assertCurrentPasswordMatches(
            passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash()));
        PasswordPolicy policy = passwordPolicyProvider.current();
        policy.assertAcceptable(FIELD_NEW_PASSWORD, request.getNewPassword());

        user.changePassword(passwordEncoder.encode(request.getNewPassword()), false, Instant.now());
        User saved = repository.save(user);
        int terminated = sessionTerminator.terminateOpenSessions(saved, callerTokenRef(authorizationHeader),
            "إنهاء الجلسة لأن المستخدم غيّر كلمة المرور", "Session terminated because the user changed the password");

        auditApi.record(SecAuditEntries.accountEvent(ACTION_PASSWORD_CHANGED, saved,
            "تغيير كلمة المرور من المستخدم", "Password changed by the user", null));
        eventPublisher.publish(new UserPasswordChangedEvent(saved.getUserPk(), false));
        log.info("User ID: {} changed their own password; other sessions terminated: {}", saved.getUserPk(), terminated);

        return ServiceResult.success(mapper.toPasswordChangeResponse(saved, terminated), Status.UPDATED);
    }

    /**
     * REQ-SEC-091 (tenant-maturity B), reached only through {@code SecAdminRecoveryApi.findRecoveryTarget}: the
     * STAFF user {@code username} of the current tenant and whether it holds an active super role. The gate is
     * the platform authority ({@code SecPermissions.PLATFORM_TENANT_MANAGE}, mirrored from the tenant module).
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<Optional<RecoveryTarget>> findRecoveryTarget(String username) {
        log.debug("Resolving the recovery target {} in the current tenant", username);

        return ServiceResult.success(repository.findByUsername(username)
            .map(user -> new RecoveryTarget(user.getUserPk(), user.getUsername(),
                roleRepository.holdsActiveSuperRole(user.getUserPk()))));
    }

    /**
     * REQ-SEC-091 — {@code SecAdminRecoveryApi.resetSuperUserPassword}: the platform operator sets the password
     * of a super user of the current tenant (RULE-SEC-056 policy, RULE-SEC-058 flag: null = TRUE), every session
     * of the user ends, {@code ADMIN_PASSWORD_RESET} is recorded in this tenant with the operator as actor.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).PLATFORM_TENANT_MANAGE)")
    public ServiceResult<Integer> resetSuperUserPassword(String username, String rawPassword,
                                                         Boolean requireChangeAtNextLogin) {
        User user = repository.findByUsername(username).orElse(null);
        UserDomain.assertRecoverableSuperUser(
            user != null && roleRepository.holdsActiveSuperRole(user.getUserPk()), username);
        log.info("Resetting the password of super user ID: {} for the platform", user.getUserPk());

        PasswordPolicy policy = passwordPolicyProvider.current();
        policy.assertAcceptable(FIELD_NEW_PASSWORD, rawPassword);

        user.changePassword(passwordEncoder.encode(rawPassword),
            UserDomain.passwordChangeRequiredFor(requireChangeAtNextLogin), Instant.now());
        User saved = repository.save(user);
        // the operator is not a user of this tenant: no acting user on the SEC audit rows
        int terminated = sessionTerminator.terminateOpenSessions(saved, null, null,
            "إنهاء الجلسة لأن مشغّل المنصة أعاد تعيين كلمة المرور",
            "Session terminated because the platform operator reset the password");

        auditApi.record(AuditEntry.builder()
            .action(ACTION_ADMIN_PASSWORD_RESET)
            .entityType(SecAuditEntries.ENTITY_TYPE_USER)
            .entityId(String.valueOf(saved.getUserPk()))
            .summaryAr("إعادة تعيين كلمة مرور المستخدم " + saved.getUsername() + " من مشغّل المنصة")
            .summaryEn("Password of user " + saved.getUsername() + " reset by the platform operator")
            .build());
        eventPublisher.publish(new UserPasswordChangedEvent(saved.getUserPk(), true));
        log.info("Password of super user ID: {} reset for the platform; change required: {}; sessions terminated: {}",
            saved.getUserPk(), saved.getPasswordChangeRequiredFl(), terminated);

        return ServiceResult.success(terminated, Status.UPDATED);
    }

    /** The caller's session is the token's {@code jti} ({@code tokenRef}, DBF-SEC-077); null when unreadable. */
    private String callerTokenRef(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return jwtTokenValidator.parse(authorizationHeader.substring(BEARER_PREFIX.length()))
            .map(Claims::getId)
            .orElse(null);
    }
}
