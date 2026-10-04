package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.main.ErpMainApplication;
import com.erp.sec.dto.ActiveSessionResponse;
import com.erp.sec.dto.ActiveSessionSearchRequest;
import com.erp.sec.dto.RoleResponse;
import com.erp.sec.dto.RoleSearchRequest;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.service.RoleService;
import com.erp.sec.service.SessionService;
import com.erp.sec.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the three defects the frontend E2E run of 2026-09-19 reported
 * ({@code sec-frontend-e2e-run-2026-09-19.md} §2.3/§2.4/§2.5), all three of them a filter or an
 * error detail the server accepted and then said nothing about:
 *
 * <ol>
 *   <li>API-SEC-025 discarded the {@code username} filter;</li>
 *   <li>API-SEC-012 discarded the documented top-level {@code name} field;</li>
 *   <li>{@code SEC-409-USER-DUP} named neither of the two fields it covers.</li>
 * </ol>
 *
 * <p>Plus the shared guard that stops (1) and (2) recurring anywhere: a filter field the search
 * does not support is now a 400 naming it, not a silently unfiltered page.
 *
 * <p>Same posture as {@link SecFrontendGapIntegrationTest}: real dev Postgres/Redis, {@code dev}
 * profile, every write rolled back by the class-level {@link Transactional}.
 */
@SpringBootTest(classes = ErpMainApplication.class)
@ActiveProfiles("dev")
@Transactional
class SecSearchFilterIntegrationTest {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private ActiveSessionRepository activeSessionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // Defect 1 — API-SEC-025 silently ignored the username filter
    // -----------------------------------------------------------------------------------------

    @Test
    void sessionSearch_filtersByUsername_ratherThanReturningEveryRow() {
        User owner = persistUser("owner");
        User other = persistUser("other");
        persistSession(owner);
        persistSession(other);

        setAuthenticatedPrincipal("session-admin", PermissionConstants.PERM_SEC_SESSIONS_VIEW);

        List<ActiveSessionResponse> rows = searchSessions(SearchOperator.LIKE, owner.getUsername());

        assertThat(rows)
            .as("a username filter must narrow the page, not be discarded")
            .isNotEmpty()
            .allSatisfy(row -> assertThat(row.getUsername()).isEqualTo(owner.getUsername()));
    }

    @Test
    void sessionSearch_honoursEqualsOnUsername_caseInsensitively() {
        User owner = persistUser("equals");
        persistSession(owner);

        setAuthenticatedPrincipal("session-admin", PermissionConstants.PERM_SEC_SESSIONS_VIEW);

        assertThat(searchSessions(SearchOperator.EQUALS, owner.getUsername().toUpperCase()))
            .as("EQUALS must match the login the same way LIKE does")
            .isNotEmpty()
            .allSatisfy(row -> assertThat(row.getUsername()).isEqualTo(owner.getUsername()));
    }

    @Test
    void sessionSearch_returnsNothingForAnUnknownUsername() {
        persistSession(persistUser("present"));

        setAuthenticatedPrincipal("session-admin", PermissionConstants.PERM_SEC_SESSIONS_VIEW);

        assertThat(searchSessions(SearchOperator.LIKE, "no-such-login-" + uniqueSuffix()))
            .as("an unmatched filter must yield an empty page, never the full set")
            .isEmpty();
    }

    @Test
    void sessionSearch_rejectsAnOperatorItCannotHonourOnUsername() {
        setAuthenticatedPrincipal("session-admin", PermissionConstants.PERM_SEC_SESSIONS_VIEW);

        assertThatThrownBy(() -> searchSessions(SearchOperator.GREATER_THAN, "abc"))
            .isInstanceOfSatisfying(LocalizedException.class, ex -> {
                assertThat(ex.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
                assertThat(ex.getErrors()).extracting(ErrorDetail::field).containsExactly("username");
            });
    }

    // -----------------------------------------------------------------------------------------
    // Defect 2 — API-SEC-012 silently ignored the documented top-level name field
    // -----------------------------------------------------------------------------------------

    @Test
    void roleSearch_honoursTheTopLevelNameField() {
        Role match = persistRole("Uniquely named " + uniqueSuffix());
        persistRole("Something else " + uniqueSuffix());

        setAuthenticatedPrincipal("role-admin", PermissionConstants.PERM_SEC_ROLES_VIEW);

        ServiceResult<Page<RoleResponse>> result = roleService.search(
            RoleSearchRequest.builder().name(match.getNameEn()).size(50).build());

        assertThat(result.getData().getContent())
            .as("the documented top-level name field must filter, not be dropped")
            .hasSize(1)
            .allSatisfy(row -> assertThat(row.getRolePk()).isEqualTo(match.getRolePk()));
    }

    @Test
    void roleSearch_stillHonoursTheFiltersSpellingOfName() {
        Role match = persistRole("Filters spelling " + uniqueSuffix());

        setAuthenticatedPrincipal("role-admin", PermissionConstants.PERM_SEC_ROLES_VIEW);

        ServiceResult<Page<RoleResponse>> result = roleService.search(
            RoleSearchRequest.builder()
                .filters(List.of(SearchFilter.builder()
                    .field("name").operator(SearchOperator.LIKE).value(match.getNameEn()).build()))
                .size(50)
                .build());

        assertThat(result.getData().getContent())
            .as("the pre-existing filters[] spelling must keep working")
            .hasSize(1)
            .allSatisfy(row -> assertThat(row.getRolePk()).isEqualTo(match.getRolePk()));
    }

    // -----------------------------------------------------------------------------------------
    // The shared guard — an unsupported filter field is a 400 naming it, not a silent full page
    // -----------------------------------------------------------------------------------------

    @Test
    void anySearch_rejectsAnUnsupportedFilterFieldAndNamesIt() {
        setAuthenticatedPrincipal("role-admin", PermissionConstants.PERM_SEC_ROLES_VIEW);

        assertThatThrownBy(() -> roleService.search(RoleSearchRequest.builder()
            .filters(List.of(SearchFilter.builder()
                .field("noSuchColumn").operator(SearchOperator.LIKE).value("x").build()))
            .build()))
            .isInstanceOfSatisfying(LocalizedException.class, ex -> {
                assertThat(ex.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
                assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCodes.VALIDATION_ERROR);
                assertThat(ex.getErrors())
                    .as("the client cannot detect a dropped filter, so the field must be named")
                    .extracting(ErrorDetail::field)
                    .containsExactly("noSuchColumn");
            });
    }

    // -----------------------------------------------------------------------------------------
    // Defect 3 — SEC-409-USER-DUP named no field
    // -----------------------------------------------------------------------------------------

    @Test
    void createUser_withATakenUsername_namesUsernameInFieldErrors() {
        User existing = persistUser("dup-username");

        setAuthenticatedPrincipal("user-admin", PermissionConstants.PERM_SEC_USERS_CREATE);

        assertThatThrownBy(() -> userService.create(userCreateRequest(
            existing.getUsername(), "free-" + uniqueSuffix() + "@example.com")))
            .isInstanceOfSatisfying(LocalizedException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo(SecErrorCodes.SEC_409_USER_DUP);
                assertThat(ex.getErrors()).extracting(ErrorDetail::field).containsExactly("username");
            });
    }

    @Test
    void createUser_withATakenEmail_namesEmailInFieldErrors() {
        User existing = persistUser("dup-email");

        setAuthenticatedPrincipal("user-admin", PermissionConstants.PERM_SEC_USERS_CREATE);

        assertThatThrownBy(() -> userService.create(userCreateRequest(
            "free-" + uniqueSuffix(), existing.getEmail())))
            .isInstanceOfSatisfying(LocalizedException.class, ex -> {
                assertThat(ex.getErrorCode()).isEqualTo(SecErrorCodes.SEC_409_USER_DUP);
                assertThat(ex.getErrors()).extracting(ErrorDetail::field).containsExactly("email");
            });
    }

    @Test
    void createUser_withBothTaken_namesBothFields() {
        User existing = persistUser("dup-both");

        setAuthenticatedPrincipal("user-admin", PermissionConstants.PERM_SEC_USERS_CREATE);

        assertThatThrownBy(() -> userService.create(
            userCreateRequest(existing.getUsername(), existing.getEmail())))
            .isInstanceOfSatisfying(LocalizedException.class, ex -> {
                assertThat(ex.getErrorCode())
                    .as("the top-level wire code must not change")
                    .isEqualTo(SecErrorCodes.SEC_409_USER_DUP);
                assertThat(ex.getErrors()).extracting(ErrorDetail::field)
                    .containsExactly("username", "email");
            });
    }

    // -----------------------------------------------------------------------------------------
    // Fixtures
    // -----------------------------------------------------------------------------------------

    private List<ActiveSessionResponse> searchSessions(SearchOperator operator, String value) {
        return sessionService.search(ActiveSessionSearchRequest.builder()
            .filters(List.of(SearchFilter.builder()
                .field("username").operator(operator).value(value).build()))
            .size(200)
            .build())
            .getData().getContent();
    }

    private UserCreateRequest userCreateRequest(String username, String email) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setFullNameAr("مستخدم اختبار");
        request.setFullNameEn("Test user");
        request.setPassword("Def@ultPass1");
        return request;
    }

    private User persistUser(String tag) {
        String unique = uniqueSuffix();
        return userRepository.save(User.builder()
            .username("sf-" + tag + "-" + unique)
            .email("sf-" + tag + "-" + unique + "@example.com")
            .passwordHash(passwordEncoder.encode("Def@ultPass1"))
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
            .build());
    }

    private ActiveSession persistSession(User user) {
        return activeSessionRepository.save(ActiveSession.builder()
            .user(user)
            .tokenRef(UUID.randomUUID().toString())
            .startedAt(Instant.now())
            .lastActivityAt(Instant.now())
            .ipAddress("203.0.113.7")
            .build());
    }

    private Role persistRole(String nameEn) {
        return roleRepository.save(Role.builder()
            .code("SFROLE" + uniqueSuffix())
            .nameAr("دور اختبار")
            .nameEn(nameEn)
            .build());
    }

    private void setAuthenticatedPrincipal(String username, String... authorities) {
        List<SimpleGrantedAuthority> grantedAuthorities =
            Stream.of(authorities).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, "N/A", grantedAuthorities));
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
