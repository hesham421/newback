package com.erp.sec.service;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.common.util.TokenHasher;
import com.erp.events.CustomerRegisteredEvent;
import com.erp.events.CustomerVerifiedEvent;
import com.erp.events.DomainEventPublisher;
import com.erp.notif.crossmodule.DispatchCommand;
import com.erp.notif.crossmodule.NotificationDispatchApi;
import com.erp.sec.domain.CustomerVerifyTokenDomain;
import com.erp.sec.domain.PasswordResetTokenDomain;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.dto.ConfirmationResponse;
import com.erp.sec.dto.CustomerLoginRequest;
import com.erp.sec.dto.CustomerProfileResponse;
import com.erp.sec.dto.CustomerProfileUpdateRequest;
import com.erp.sec.dto.CustomerRegisterRequest;
import com.erp.sec.dto.CustomerVerifyRequest;
import com.erp.sec.dto.LoginResponse;
import com.erp.sec.dto.PasswordResetCompleteRequest;
import com.erp.sec.dto.PasswordResetRequest;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.CustomerVerifyToken;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.CustomerAccountMapper;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.CustomerVerifyTokenRepository;
import com.erp.sec.repository.PasswordResetTokenRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.InternalCallerContext;
import com.erp.sec.security.JwtTokenIssuer;
import com.erp.sec.security.LoginRateLimiter;
import com.erp.tenant.TenantContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * erp-core step 06 — the CUSTOMER realm: self-registered storefront accounts of a tenant (the tenant
 * comes from {@code X-Tenant-Code} on the public endpoints and from the token's {@code tid} afterwards).
 * <ul>
 *   <li>{@link #register} creates a {@code PENDING_VERIFICATION} account ({@code REALM='CUSTOMER'}, no
 *       roles) and mails a one-time verification link ({@code CUSTOMER_VERIFY_EMAIL});</li>
 *   <li>{@link #verify} consumes the token and activates the account;</li>
 *   <li>{@link #login} issues an access token carrying {@code realm=CUSTOMER} and {@code tid}; the
 *       customer's only authority is {@code ROLE_CUSTOMER} (JwtAuthenticationFilter);</li>
 *   <li>{@link #requestPasswordReset} / {@link #completePasswordReset} mirror the staff reset on the
 *       customer realm ({@code CUSTOMER_PASSWORD_RESET});</li>
 *   <li>{@link #me} / {@link #updateMe} read and edit the caller's own profile.</li>
 * </ul>
 * The five public operations are pre-authentication (the same precedent as {@code AuthService.login}
 * and {@code PasswordResetService}) and carry no {@code @PreAuthorize}; the two {@code me} operations
 * require {@value com.erp.sec.permission.SecPermissions#ROLE_CUSTOMER}. Raw tokens leave this class only
 * through the notification variables; only their SHA-256 hashes are stored.
 *
 * <p>Notifications go through NOTIF's cross-module {@link NotificationDispatchApi}: the
 * {@code dispatchIndependently} entry point (REQUIRES_NEW) inside {@link InternalCallerContext} (the
 * callers are anonymous), so a failed mail never rolls back the account or token, exactly as the staff
 * password reset does.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAccountService {

    /** Seeded by {@code V11__sec_realms.sql} for every tenant. */
    static final String TEMPLATE_VERIFY_EMAIL = "CUSTOMER_VERIFY_EMAIL";
    static final String TEMPLATE_PASSWORD_RESET = "CUSTOMER_PASSWORD_RESET";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String MODULE_CODE = "SEC";
    static final String REFERENCE_VERIFY = "SEC_CUSTOMER_VERIFY_TOKEN";
    static final String REFERENCE_RESET = "SEC_PWD_RESET_TOKEN";

    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private static final DateTimeFormatter EXPIRY_FORMAT =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private static final String VERIFY_CTA_EN = "Verify e-mail";
    private static final String VERIFY_CTA_AR = "تأكيد البريد";
    private static final String RESET_CTA_EN = "Reset Password";
    private static final String RESET_CTA_AR = "إعادة تعيين كلمة المرور";

    private static final String RESET_REQUEST_AR = "إذا كان البريد الإلكتروني مسجلًا فسيتم إرسال رابط إعادة التعيين";
    private static final String RESET_REQUEST_EN = "If that email is registered, a reset link has been sent";
    private static final String RESET_COMPLETE_AR = "تم تحديث كلمة المرور";
    private static final String RESET_COMPLETE_EN = "Your password has been updated";

    private final UserRepository userRepository;
    private final CustomerVerifyTokenRepository verifyTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final CustomerAccountMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final LoginRateLimiter loginRateLimiter;
    private final NotificationDispatchApi notificationDispatchApi;
    private final ErpCoreProperties properties;
    private final DomainEventPublisher eventPublisher;
    // erp-core step 10 — customer LOGIN / PASSWORD_RESET go to the generic audit log (realm CUSTOMER)
    private final AuditApi auditApi;

    /** {@code POST /api/v1/public/customers/register} — 201 with the new, unverified account. */
    @Transactional
    public ServiceResult<CustomerProfileResponse> register(CustomerRegisterRequest request) {
        log.info("Customer registration requested");

        boolean emailTaken = userRepository.existsByEmailAndRealm(request.getEmail(), User.REALM_CUSTOMER);
        UserDomain.createCustomer(request.getEmail(), emailTaken);

        User saved = userRepository.save(mapper.toEntity(request, passwordEncoder.encode(request.getPassword())));

        String rawToken = UUID.randomUUID().toString();
        CustomerVerifyToken token = verifyTokenRepository.save(CustomerVerifyToken.builder()
            .user(saved)
            .tokenHash(TokenHasher.sha256Hex(rawToken))
            .build());
        dispatch(saved, TEMPLATE_VERIFY_EMAIL, token.getId(), REFERENCE_VERIFY, rawToken, token.getExpiresAt(),
            properties.getFrontend().getCustomerVerifyPath(), VERIFY_CTA_EN, VERIFY_CTA_AR);

        log.info("Registered customer User ID: {}", saved.getUserPk());
        // erp-core step 08 — anonymous endpoint: the customer itself is the actor
        eventPublisher.publish(new CustomerRegisteredEvent(TenantContext.current(), saved.getUsername(),
            saved.getUserPk(), saved.getEmail()));
        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /** {@code POST /api/v1/public/customers/verify} — consumes the token and activates the account. */
    @Transactional
    public ServiceResult<CustomerProfileResponse> verify(CustomerVerifyRequest request) {
        log.info("Customer e-mail verification submitted");

        CustomerVerifyToken token = verifyTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.getToken()))
            .orElseThrow(CustomerVerifyTokenDomain::invalid);
        CustomerVerifyTokenDomain.from(token).assertUsable(Instant.now());

        token.markUsed();
        verifyTokenRepository.save(token);

        User user = token.getUser();
        if (UserDomain.from(user).awaitsVerification()) {
            user.markVerified();
            user = userRepository.save(user);
        }

        log.info("Verified customer User ID: {}", user.getUserPk());
        eventPublisher.publish(new CustomerVerifiedEvent(TenantContext.current(), user.getUsername(), user.getUserPk()));
        return ServiceResult.success(mapper.toResponse(user), Status.UPDATED);
    }

    /**
     * {@code POST /api/v1/public/customers/login}. Unknown e-mail, wrong password and a disabled
     * account answer the same 401; only a correct password on an unverified account reveals
     * {@code CUSTOMER_NOT_VERIFIED} (403). Rate limited per {@code tenant:realm:username}.
     */
    @Transactional(noRollbackFor = LocalizedException.class)
    public ServiceResult<LoginResponse> login(CustomerLoginRequest request, String ipAddress) {
        log.info("Customer login attempt");

        if (!loginRateLimiter.tryAcquire(
            LoginRateLimiter.key(TenantContext.require(), User.REALM_CUSTOMER, request.getEmail()))) {
            throw new LocalizedException(Status.TOO_MANY_REQUESTS, SecErrorCodes.CUSTOMER_LOGIN_RATE_LIMITED);
        }

        User user = userRepository.findByUsernameAndRealm(request.getEmail(), User.REALM_CUSTOMER).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActiveFl())
            || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new LocalizedException(Status.UNAUTHORIZED, SecErrorCodes.SEC_401_INVALID_CREDENTIALS);
        }
        UserDomain account = UserDomain.from(user);
        account.assertCustomerVerified();
        if (!User.STATUS_ACTIVE.equals(account.getStatusCode())) {
            throw new LocalizedException(Status.UNAUTHORIZED, SecErrorCodes.SEC_401_INVALID_CREDENTIALS);
        }

        Instant now = Instant.now();
        String tokenRef = UUID.randomUUID().toString();
        activeSessionRepository.save(ActiveSession.builder()
            .user(user)
            .tokenRef(tokenRef)
            .startedAt(now)
            .lastActivityAt(now)
            .ipAddress(ipAddress)
            .build());
        user.setLastLoginAt(now);
        userRepository.save(user);
        auditApi.record(SecAuditEntries.accountEvent(AuditApi.ACTION_LOGIN, user,
            "تسجيل دخول ناجح", "Successful login", ipAddress));

        log.info("Customer login succeeded for User ID: {}", user.getUserPk());
        return ServiceResult.success(LoginResponse.builder()
            .accessToken(jwtTokenIssuer.issue(user, tokenRef, now))
            .tokenType(TOKEN_TYPE_BEARER)
            .expiresIn(jwtTokenIssuer.getExpiresInSeconds())
            .passwordChangeRequired(Boolean.FALSE) // tenant-maturity D: customers are never flagged
            .build());
    }

    /** {@code POST /api/v1/public/customers/password-reset/request} — same answer whether or not the e-mail exists. */
    @Transactional
    public ServiceResult<ConfirmationResponse> requestPasswordReset(PasswordResetRequest request) {
        log.info("Customer password reset requested");

        userRepository.findByEmailAndRealm(request.getEmail(), User.REALM_CUSTOMER)
            .filter(user -> Boolean.TRUE.equals(user.getIsActiveFl()))
            .ifPresent(user -> {
                String rawToken = UUID.randomUUID().toString();
                PasswordResetToken token = resetTokenRepository.save(PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.sha256Hex(rawToken))
                    .build());
                dispatch(user, TEMPLATE_PASSWORD_RESET, token.getPwdResetTokenPk(), REFERENCE_RESET, rawToken,
                    token.getExpiresAt(), properties.getFrontend().getCustomerPasswordResetPath(),
                    RESET_CTA_EN, RESET_CTA_AR);
            });

        return ServiceResult.success(ConfirmationResponse.builder()
            .messageAr(RESET_REQUEST_AR).messageEn(RESET_REQUEST_EN).build());
    }

    /**
     * {@code POST /api/v1/public/customers/password-reset/complete}. A staff token is refused like an
     * unknown one. Completing a reset proves the e-mail address, so an unverified account becomes
     * verified; every open session of the account is terminated.
     */
    @Transactional
    public ServiceResult<ConfirmationResponse> completePasswordReset(PasswordResetCompleteRequest request) {
        log.info("Customer password reset completion submitted");

        PasswordResetToken token = resetTokenRepository.findByTokenHash(TokenHasher.sha256Hex(request.getToken()))
            .orElseThrow(() -> new LocalizedException(Status.CONFLICT, SecErrorCodes.SEC_409_RESET_TOKEN_INVALID));
        Instant now = Instant.now();
        PasswordResetTokenDomain.from(token).assertUsable(now, User.REALM_CUSTOMER, token.getUser().getRealm());

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        if (UserDomain.from(user).awaitsVerification()) {
            user.markVerified();
        }
        userRepository.save(user);
        token.markUsed();
        resetTokenRepository.save(token);

        String principal = SecurityContextHelper.getCurrentUsername();
        List<ActiveSession> open = activeSessionRepository.findNonTerminatedByUser(user.getUserPk());
        open.forEach(session -> session.terminate(principal));
        activeSessionRepository.saveAll(open);
        auditApi.record(SecAuditEntries.accountEvent(AuditApi.ACTION_PASSWORD_RESET, user,
            "تم إتمام إعادة تعيين كلمة المرور", "Password reset completed", null));

        log.info("Customer password reset completed for User ID: {} ({} sessions terminated)",
            user.getUserPk(), open.size());
        return ServiceResult.success(ConfirmationResponse.builder()
            .messageAr(RESET_COMPLETE_AR).messageEn(RESET_COMPLETE_EN).build());
    }

    /** {@code GET /api/v1/customers/me}. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).ROLE_CUSTOMER)")
    public ServiceResult<CustomerProfileResponse> me() {
        log.debug("Reading the caller's customer profile");
        return ServiceResult.success(mapper.toResponse(currentCustomer()));
    }

    /** {@code PATCH /api/v1/customers/me}. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).ROLE_CUSTOMER)")
    public ServiceResult<CustomerProfileResponse> updateMe(CustomerProfileUpdateRequest request) {
        User user = currentCustomer();
        log.info("Updating customer profile of User ID: {}", user.getUserPk());

        mapper.updateEntityFromRequest(user, request);
        return ServiceResult.success(mapper.toResponse(userRepository.save(user)), Status.UPDATED);
    }

    private User currentCustomer() {
        String username = SecurityContextHelper.getCurrentUsername();
        return userRepository.findByUsernameAndRealm(username, User.REALM_CUSTOMER)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, username));
    }

    /**
     * Best effort, like the staff password reset: a failed dispatch is logged and never fails the
     * caller (REQUIRES_NEW on NOTIF's side, so this transaction is never marked rollback-only). The
     * recipient's e-mail travels in the variables (NOTIF's published contract, see
     * {@code PasswordResetService}).
     */
    private void dispatch(User user, String templateCode, Long referenceId, String referenceType, String rawToken,
                          Instant expiresAt, String frontendPath, String ctaEn, String ctaAr) {
        try {
            InternalCallerContext.call(() -> notificationDispatchApi.dispatchIndependently(new DispatchCommand(
                user.getUserPk(),
                templateCode,
                List.of(CHANNEL_EMAIL),
                MODULE_CODE,
                referenceId,
                referenceType,
                Map.of("token", rawToken,
                    "expiresAt", EXPIRY_FORMAT.format(expiresAt),
                    "email", user.getEmail(),
                    "actionLink", actionLink(frontendPath, rawToken),
                    "ctaLabelEn", ctaEn,
                    "ctaLabelAr", ctaAr))));
        } catch (RuntimeException e) {
            log.warn("Customer notification {} could not be dispatched for User ID {}; the operation itself "
                + "still succeeds", templateCode, user.getUserPk(), e);
        }
    }

    private String actionLink(String path, String rawToken) {
        String base = properties.getFrontend().getBaseUrl() == null ? "" : properties.getFrontend().getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String route = path == null || path.isBlank() ? "/" : path.trim();
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        return base + route + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
