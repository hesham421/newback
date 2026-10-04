package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.erp.common.domain.status.ServiceResult;
import com.erp.main.ErpMainApplication;
import com.erp.sec.dto.LoginRequest;
import com.erp.sec.dto.LoginResponse;
import com.erp.sec.dto.SessionTerminationResponse;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.User;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.security.JwtAuthenticationFilter;
import com.erp.sec.security.JwtTokenValidator;
import com.erp.sec.service.AuthService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-SEC-028 / REQ-SEC-036 — the signed-in user ends their own session. Covers AC-SEC-036's four
 * halves: the caller's own row is stamped, the token stops authenticating, a repeat is a no-op,
 * and one LOGOUT audit row is appended.
 *
 * <p>Driven in-process rather than over HTTP because the assertions need the token's {@code jti}
 * and the {@code SEC_ACTIVE_SESSION} row behind it — neither is ever returned to a client. Runs
 * against the real dev Postgres/Redis like {@link SecCoverageIntegrationTest}, with the same
 * class-level {@link Transactional} rollback, so nothing is left behind.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class SecLogoutIntegrationTest {

    private static final String CALLER_IP = "203.0.113.7";

    @Autowired
    private AuthService authService;
    @Autowired
    private JwtTokenValidator jwtTokenValidator;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ActiveSessionRepository activeSessionRepository;
    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // (a) logout terminates the caller's own session row  ·  (d) the LOGOUT audit row is written
    // -----------------------------------------------------------------------------------------

    @Test
    void logout_terminatesOwnSessionAndAppendsLogoutAudit() {
        // Covers: AC-SEC-036, DBF-SEC-081/082
        String rawPassword = "L0gout!Pass1";
        User user = persistUser("logout", rawPassword);
        String token = login(user, rawPassword);
        String tokenRef = tokenRefOf(token);

        ActiveSession before = activeSessionRepository.findByTokenRef(tokenRef).orElseThrow();
        assertThat(before.getTerminatedAt()).as("precondition: the session starts active").isNull();

        // A second, unrelated session of the SAME user — logout must end exactly one session, the
        // caller's own, not every session that user holds (that is API-SEC-009's bulk behaviour).
        ActiveSession otherSession = activeSessionRepository.save(ActiveSession.builder()
            .user(user)
            .tokenRef(UUID.randomUUID().toString())
            .startedAt(Instant.now())
            .lastActivityAt(Instant.now())
            .ipAddress("198.51.100.4")
            .build());

        authenticateAs(user.getUsername());
        Instant beforeLogout = Instant.now().minusSeconds(1);
        ServiceResult<SessionTerminationResponse> result = logout(token);

        SessionTerminationResponse body = result.getData();
        assertThat(body).isNotNull();
        assertThat(body.getActiveSessionPk()).isEqualTo(before.getActiveSessionPk());
        assertThat(body.getTerminatedAt()).isNotNull();

        ActiveSession after = activeSessionRepository.findByTokenRef(tokenRef).orElseThrow();
        assertThat(after.getTerminatedAt()).as("DBF-SEC-081 stamped").isNotNull();
        assertThat(after.getTerminatedBy())
            .as("DBF-SEC-082 names the caller themselves, not an administrator")
            .isEqualTo(user.getUsername());

        assertThat(activeSessionRepository.findById(otherSession.getActiveSessionPk()).orElseThrow()
            .getTerminatedAt())
            .as("the user's other session must be untouched — logout ends one session, not all")
            .isNull();

        List<AuditLogEntry> logoutRows = auditRows("LOGOUT", user);
        assertThat(logoutRows).hasSize(1);
        AuditLogEntry entry = logoutRows.get(0);
        assertThat(entry.getActor().getUserPk()).isEqualTo(user.getUserPk());
        assertThat(entry.getTargetRef())
            .as("targetRef carries the user id, exactly as AuthService records LOGIN_SUCCESS")
            .isEqualTo(String.valueOf(user.getUserPk()));
        assertThat(entry.getIpAddress()).isEqualTo(CALLER_IP);
        assertThat(entry.getOccurredAt()).isAfterOrEqualTo(beforeLogout);
    }

    // -----------------------------------------------------------------------------------------
    // (b) the token no longer authenticates afterwards
    // -----------------------------------------------------------------------------------------

    @Test
    void logout_makesTheAccessTokenUnusableForSubsequentRequests() {
        // Covers: AC-SEC-036's "no longer accepted for any subsequent request" — REQ-SEC-028's
        // request-time half, which is JwtAuthenticationFilter's job, not the service's. Asserted
        // by running the real filter over the real token before and after, so the test proves the
        // token actually stops working rather than merely that a column was set.
        String rawPassword = "L0gout!Pass2";
        User user = persistUser("tokendead", rawPassword);
        String token = login(user, rawPassword);

        assertThat(authenticatesThroughFilter(token))
            .as("control: the token authenticates while its session is live")
            .isTrue();

        authenticateAs(user.getUsername());
        logout(token);

        assertThat(authenticatesThroughFilter(token))
            .as("the same token must be rejected once its session carries terminatedAt")
            .isFalse();
    }

    // -----------------------------------------------------------------------------------------
    // (c) a second logout is idempotent
    // -----------------------------------------------------------------------------------------

    @Test
    void secondLogout_isANoOpRatherThanAConflict() {
        // Covers: AC-SEC-036 / REQ-SEC-036's Note. SEC-409-ALREADY-TERMINATED is API-SEC-026's
        // answer to an administrator closing someone else's closed session; a caller closing their
        // own gets the standing stamp back instead, and no second audit row.
        //
        // Note on reachability: over HTTP a repeat with the same token never gets this far — the
        // filter exercised above answers it 401. What this covers is the races that DO reach the
        // service: two concurrent logouts, or an API-SEC-026 termination landing between the
        // filter and this method.
        String rawPassword = "L0gout!Pass3";
        User user = persistUser("idempotent", rawPassword);
        String token = login(user, rawPassword);

        authenticateAs(user.getUsername());
        SessionTerminationResponse first = logout(token).getData();

        SessionTerminationResponse second = logout(token).getData();

        assertThat(second).isNotNull();
        assertThat(second.getActiveSessionPk()).isEqualTo(first.getActiveSessionPk());
        assertThat(second.getTerminatedAt())
            .as("the original termination stamp is returned unchanged, not refreshed")
            .isEqualTo(first.getTerminatedAt());

        assertThat(auditRows("LOGOUT", user))
            .as("a repeated logout must not append a second LOGOUT row")
            .hasSize(1);

        assertThatCode(() -> logout(token))
            .as("further repeats stay a no-op — never a 409 and never a 500")
            .doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------------------------

    private ServiceResult<SessionTerminationResponse> logout(String token) {
        return authService.logout("Bearer " + token, CALLER_IP);
    }

    private String login(User user, String rawPassword) {
        ServiceResult<LoginResponse> result = authService.login(
            LoginRequest.builder().username(user.getUsername()).password(rawPassword).build(),
            CALLER_IP);
        return result.getData().getAccessToken();
    }

    /** The access token's {@code jti} IS the session's {@code tokenRef} (DBF-SEC-077). */
    private String tokenRefOf(String token) {
        return jwtTokenValidator.parse(token).orElseThrow().getId();
    }

    /**
     * Runs the real CORE filter over a bearer token and reports whether it installed an
     * authentication. The context is cleared first because the filter only acts on an anonymous
     * one, and again afterwards so a passing control cannot leak into the next assertion.
     */
    private boolean authenticatesThroughFilter(String token) {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        try {
            jwtAuthenticationFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            return SecurityContextHolder.getContext().getAuthentication() != null;
        } catch (Exception e) {
            throw new IllegalStateException("Filter invocation failed", e);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private List<AuditLogEntry> auditRows(String eventTypeCode, User user) {
        Specification<AuditLogEntry> spec = (root, query, cb) -> cb.and(
            cb.equal(root.get("eventTypeCode"), eventTypeCode),
            cb.equal(root.get("actor").get("userPk"), user.getUserPk()));
        return auditLogEntryRepository.findAll(spec);
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "N/A", List.of()));
    }

    private User persistUser(String tag, String rawPassword) {
        String unique = tag + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userRepository.save(User.builder()
            .username("tc-" + unique)
            .email("tc-" + unique + "@example.com")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
            .build());
    }
}
