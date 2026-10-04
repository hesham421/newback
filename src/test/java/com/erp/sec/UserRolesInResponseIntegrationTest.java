package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.main.ErpMainApplication;
import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserSearchRequest;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.RoleRepository;
import com.erp.sec.repository.UserRepository;
import com.erp.sec.repository.UserRoleAssignmentRepository;
import com.erp.sec.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-SEC-005/006/007 must return {@code roles} — their api-docs always documented the member, but
 * only API-SEC-008 ever populated it, so the only place in the platform where a user's roles were
 * visible was the response to the write that set them. These tests pin the read paths, the batch
 * (non-N+1) load behind the search page, and API-SEC-006's new optional {@code roleIds}.
 *
 * <p>Runs against the real dev Postgres/Redis the same way {@code SecCoverageIntegrationTest} does;
 * the class-level {@link Transactional} rolls every write back. The two rollback/denial cases opt
 * out of it with {@link Propagation#NOT_SUPPORTED}, because "the user was never created" is only
 * observable once the service owns its own transaction — they leave nothing behind by construction.
 */
@SpringBootTest(
    classes = ErpMainApplication.class,
    properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@Transactional
class UserRolesInResponseIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Autowired
    private AuditLogEntryRepository auditLogEntryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -----------------------------------------------------------------------------------------
    // API-SEC-005 — search returns roles, in one query for the whole page
    // -----------------------------------------------------------------------------------------

    @Test
    void search_returnsRolesPerRow_withoutAQueryPerRow() {
        String batch = uniqueSuffix();
        Role roleA = persistRole("A");
        Role roleB = persistRole("B");

        User twoRoles = persistUser(batch + "-two");
        assign(twoRoles, roleA);
        assign(twoRoles, roleB);
        User oneRole = persistUser(batch + "-one");
        assign(oneRole, roleA);
        User noRoles = persistUser(batch + "-none");

        // The assignments above are still only in the persistence context; the search below runs
        // its own selects, so they must be on the wire first.
        entityManager.flush();
        entityManager.clear();

        setAuthenticatedPrincipal("searcher", PermissionConstants.PERM_SEC_USERS_VIEW);

        Statistics statistics = statistics();
        statistics.clear();

        ServiceResult<Page<UserResponse>> result = userService.search(UserSearchRequest.builder()
            .filters(List.of(filter("username", SearchOperator.LIKE, batch)))
            .page(0)
            .size(10)
            .build());

        long queriesForThePage = statistics.getPrepareStatementCount();

        List<UserResponse> rows = result.getData().getContent();
        assertThat(rows).hasSize(3);

        assertThat(roleCodesOf(rows, twoRoles.getUserPk()))
            .containsExactlyInAnyOrder(roleA.getCode(), roleB.getCode());
        assertThat(roleCodesOf(rows, oneRole.getUserPk())).containsExactly(roleA.getCode());

        // The point of the whole change: "holds no roles" is an empty array, never a missing key.
        UserResponse noRolesRow = rowFor(rows, noRoles.getUserPk());
        assertThat(noRolesRow.getRoles()).isNotNull().isEmpty();

        // count + page + ONE batched role select. A per-row load would add one statement per user,
        // so this stays a real regression guard as the fixture grows.
        assertThat(queriesForThePage)
            .as("statements issued by a 3-row search page — must not grow with the row count")
            .isLessThanOrEqualTo(3);
    }

    @Test
    void search_emptyPage_issuesNoRoleQueryAtAll() {
        setAuthenticatedPrincipal("searcher", PermissionConstants.PERM_SEC_USERS_VIEW);

        // An IN () against an empty page is a Postgres syntax error — the empty page must short out.
        ServiceResult<Page<UserResponse>> result = userService.search(UserSearchRequest.builder()
            .filters(List.of(filter("username", SearchOperator.EQUALS, "absent-" + uniqueSuffix())))
            .page(0)
            .size(10)
            .build());

        assertThat(result.getData().getContent()).isEmpty();
    }

    // -----------------------------------------------------------------------------------------
    // API-SEC-007 — update returns the roles the user already holds
    // -----------------------------------------------------------------------------------------

    @Test
    void update_returnsTheUsersExistingRoles() {
        Role role = persistRole("upd");
        User user = persistUser("update");
        assign(user, role);
        entityManager.flush();

        setAuthenticatedPrincipal("updater", PermissionConstants.PERM_SEC_USERS_UPDATE);

        ServiceResult<UserResponse> result = userService.update(user.getUserPk(),
            UserUpdateRequest.builder()
                .email("renamed-" + uniqueSuffix() + "@example.com")
                .fullNameAr("اسم محدَّث")
                .fullNameEn("Updated name")
                .build());

        assertThat(result.getData().getRoles())
            .extracting(RoleSummaryResponse::getCode)
            .containsExactly(role.getCode());
    }

    // -----------------------------------------------------------------------------------------
    // API-SEC-006 — create, with and without roleIds
    // -----------------------------------------------------------------------------------------

    @Test
    void create_withRoleIds_assignsThemAndAudits() {
        Role role = persistRole("create");
        entityManager.flush();

        setAuthenticatedPrincipal("creator",
            PermissionConstants.PERM_SEC_USERS_CREATE, PermissionConstants.PERM_SEC_USERS_UPDATE);

        ServiceResult<UserResponse> result = userService.create(createRequest("withroles")
            .roleIds(List.of(role.getRolePk()))
            .build());

        UserResponse created = result.getData();
        assertThat(created.getRoles())
            .extracting(RoleSummaryResponse::getCode)
            .containsExactly(role.getCode());
        assertThat(created.getRoles().getFirst().getRoleId()).isEqualTo(role.getRolePk());

        assertThat(userRoleAssignmentRepository.findByUser(created.getUserPk())).hasSize(1);

        // Assigning at creation is an audit event like any other assignment.
        Specification<AuditLogEntry> assignedForUser = (root, query, cb) -> cb.and(
            cb.equal(root.get("eventTypeCode"), "ROLE_ASSIGNED"),
            cb.equal(root.get("targetRef"), created.getUserPk() + "/" + role.getRolePk()));
        assertThat(auditLogEntryRepository.findAll(assignedForUser)).hasSize(1);
    }

    @Test
    void create_withoutRoleIds_stillWorksAndReturnsAnEmptyArray() {
        setAuthenticatedPrincipal("creator", PermissionConstants.PERM_SEC_USERS_CREATE);

        // The pre-change client sends no roleIds at all — that request must stay valid.
        ServiceResult<UserResponse> result =
            userService.create(createRequest("noroles").build());

        assertThat(result.getData().getRoles()).isNotNull().isEmpty();
        assertThat(userRoleAssignmentRepository.findByUser(result.getData().getUserPk())).isEmpty();
    }

    @Test
    void create_withEmptyRoleIds_isTreatedAsNoRoles() {
        setAuthenticatedPrincipal("creator", PermissionConstants.PERM_SEC_USERS_CREATE);

        // [] means "no roles", so it must not demand the assignment permission either.
        ServiceResult<UserResponse> result =
            userService.create(createRequest("emptyroles").roleIds(List.of()).build());

        assertThat(result.getData().getRoles()).isNotNull().isEmpty();
    }

    // -----------------------------------------------------------------------------------------
    // API-SEC-006 — the failure paths must leave no user behind
    // -----------------------------------------------------------------------------------------

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_withUnknownRoleId_rollsTheUserBackToo() {
        setAuthenticatedPrincipal("creator",
            PermissionConstants.PERM_SEC_USERS_CREATE, PermissionConstants.PERM_SEC_USERS_UPDATE);

        UserCreateRequest request = createRequest("badrole")
            .roleIds(List.of(Long.MAX_VALUE))
            .build();

        assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(SecErrorCodes.SEC_404_ROLE);

        // Nothing half-built survives: the insert shared the transaction the assignment failed in.
        assertThat(userRepository.existsByUsername(request.getUsername())).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_withRoleIdsButWithoutTheAssignPermission_isDeniedAndCreatesNothing() {
        setAuthenticatedPrincipal("creator", PermissionConstants.PERM_SEC_USERS_CREATE);

        UserCreateRequest request = createRequest("nogrant")
            .roleIds(List.of(Long.MAX_VALUE))
            .build();

        // CREATE alone must not become a side door around API-SEC-008's UPDATE gate. The role id is
        // deliberately one that does not exist: answering 403 rather than 404 proves the gate fires
        // before the request body is looked at at all, so nothing is written on the denied path.
        assertThatThrownBy(() -> userService.create(request))
            .isInstanceOf(LocalizedException.class)
            .extracting(e -> ((LocalizedException) e).getErrorCode())
            .isEqualTo(SecErrorCodes.SEC_403_FORBIDDEN);

        assertThat(userRepository.existsByUsername(request.getUsername())).isFalse();
    }

    // -----------------------------------------------------------------------------------------
    // Fixture helpers
    // -----------------------------------------------------------------------------------------

    private UserCreateRequest.UserCreateRequestBuilder createRequest(String tag) {
        String unique = tag + "-" + uniqueSuffix();
        return UserCreateRequest.builder()
            .username("tc-" + unique)
            .email("tc-" + unique + "@example.com")
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Test user " + tag)
            .password("N3wP@ssw0rd!");
    }

    private User persistUser(String tag) {
        String unique = tag + "-" + uniqueSuffix();
        return userRepository.save(User.builder()
            .username("tc-" + unique)
            .email("tc-" + unique + "@example.com")
            .passwordHash(passwordEncoder.encode("Def@ultPass1"))
            .fullNameAr("مستخدم اختبار " + tag)
            .fullNameEn("Test user " + tag)
            .statusCode(User.STATUS_ACTIVE)
            .build());
    }

    private Role persistRole(String tag) {
        return roleRepository.save(Role.builder()
            .code("TROLE" + uniqueSuffix())
            .nameAr("دور اختبار " + tag)
            .nameEn("Test role " + tag)
            .build());
    }

    private void assign(User user, Role role) {
        userRoleAssignmentRepository.save(UserRoleAssignment.builder()
            .user(user)
            .role(role)
            .assignedBy("UserRolesInResponseIntegrationTest")
            .build());
    }

    private List<String> roleCodesOf(List<UserResponse> rows, Long userPk) {
        return rowFor(rows, userPk).getRoles().stream()
            .map(RoleSummaryResponse::getCode)
            .toList();
    }

    private UserResponse rowFor(List<UserResponse> rows, Long userPk) {
        return rows.stream()
            .filter(row -> userPk.equals(row.getUserPk()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No search row for user " + userPk));
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private SearchFilter filter(String field, SearchOperator operator, Object value) {
        return SearchFilter.builder().field(field).operator(operator).value(value).build();
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
