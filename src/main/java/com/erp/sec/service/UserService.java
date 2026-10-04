package com.erp.sec.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.sec.crossmodule.UserContact;
import com.erp.sec.domain.ActiveSessionDomain;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserSearchRequest;
import com.erp.sec.dto.UserStatusResponse;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.UserMapper;
import com.erp.sec.permission.PermissionConstants;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.RoleActionGrantRepository;
import com.erp.sec.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration for ENT-SEC-001 (User) — API-SEC-005/006/007/009/010. No caching: SEC is absent from
 * the gov-enforce-caching-rules approved register. The raw password is hashed once here and never
 * logged, stored or returned.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /** AUDIT_EVENT_TYPE code (CHK_SEC_AUDIT_LOG_EVENT_TYPE) written by the deactivate cascade. */
    private static final String EVENT_SESSION_TERMINATED = "SESSION_TERMINATED";

    /** SCR-REQ-SEC-004 B2 filters (username/email, fullName, statusCode) as entity columns (A.5.6). */
    private static final Set<String> ALLOWED_SORT_FIELDS =
        Set.of("username", "email", "fullNameAr", "fullNameEn", "statusCode");

    private final UserRepository repository;
    private final ActiveSessionRepository activeSessionRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final RoleActionGrantRepository roleActionGrantRepository;
    private final UserMapper mapper;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;

    /**
     * API-SEC-006. SRS A6 defines no AUDIT_EVENT_TYPE for plain user creation, so no audit row is
     * appended here — the role assignment {@code roleIds} may carry does audit itself (ROLE_ASSIGNED),
     * through the same path API-SEC-008 uses.
     *
     * <p>Authorization is split: PERM_SEC_USERS_CREATE always (the {@code @PreAuthorize} below), and
     * PERM_SEC_USERS_UPDATE additionally when {@code roleIds} is non-empty, because that is what
     * API-SEC-008 demands of the identical write. Without the second check, creating a user would be
     * a side door around the assignment gate. The check is in the body, not in SpEL, because it
     * depends on the request body.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_CREATE)")
    public ServiceResult<UserResponse> create(UserCreateRequest request) {
        log.info("Creating User with username: {}", request.getUsername());

        List<Long> roleIds = request.getRoleIds();
        boolean assigningRoles = roleIds != null && !roleIds.isEmpty();
        if (assigningRoles) {
            assertMayAssignRoles();      // before the insert — a denial must write nothing at all
        }

        boolean usernameTaken = repository.existsByUsername(request.getUsername());
        boolean emailTaken = repository.existsByEmail(request.getEmail());

        UserDomain.create(request.getUsername(), request.getEmail(), usernameTaken, emailTaken);

        User saved = repository.save(
            mapper.toEntity(request, passwordEncoder.encode(request.getPassword())));
        log.info("Created User ID: {}", saved.getUserPk());

        // Same transaction as the insert above: an unknown role id rolls the user back too,
        // so a half-configured account cannot survive a failed assignment.
        List<RoleSummaryResponse> roles =
            assigningRoles ? userRoleService.replaceAssignments(saved, roleIds) : List.of();

        return ServiceResult.success(mapper.toResponse(saved, roles), Status.CREATED);
    }

    /** API-SEC-007 — username is immutable, so only email uniqueness is re-checked (QR-SEC-033). */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<UserResponse> update(Long id, UserUpdateRequest request) {
        log.info("Updating User ID: {}", id);

        User entity = loadUser(id);

        boolean emailTaken = repository.existsByEmailAndUserPkNot(request.getEmail(), id);
        UserDomain.from(entity).assertEmailAvailable(emailTaken);

        mapper.updateEntityFromRequest(entity, request);
        User saved = repository.save(entity);
        log.info("Updated User ID: {}", saved.getUserPk());

        return ServiceResult.success(
            mapper.toResponse(saved, userRoleService.rolesOf(id)), Status.UPDATED);
    }

    /**
     * API-SEC-009 — deactivate, then end every still-open session of that user (REQ-SEC-011's
     * second half), one SESSION_TERMINATED audit row per session, all in this transaction.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<UserStatusResponse> deactivate(Long id) {
        log.info("Deactivating User ID: {}", id);

        User entity = loadUser(id);
        entity.deactivate();
        User saved = repository.save(entity);

        String principal = SecurityContextHelper.getCurrentUsername();
        List<ActiveSession> openSessions = activeSessionRepository.findNonTerminatedByUser(id);
        for (ActiveSession session : openSessions) {
            ActiveSessionDomain.from(session).assertCanTerminate();
            session.terminate(principal);
            auditLogEntryRepository.save(AuditLogEntry.builder()
                .eventTypeCode(EVENT_SESSION_TERMINATED)
                .actor(currentActor(principal))
                .occurredAt(Instant.now())
                .targetRef(String.valueOf(session.getActiveSessionPk()))
                .detailsAr("إنهاء الجلسة بسبب تعطيل المستخدم")
                .detailsEn("Session terminated because the user was deactivated")
                .build());
        }
        activeSessionRepository.saveAll(openSessions);
        log.info("Deactivated User ID: {}, terminated sessions: {}", saved.getUserPk(), openSessions.size());

        return ServiceResult.success(mapper.toStatusResponse(saved), Status.UPDATED);
    }

    /** API-SEC-010 — only a DISABLED user may be reactivated (A7 lifecycle, UserDomain decides). */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<UserStatusResponse> reactivate(Long id) {
        log.info("Reactivating User ID: {}", id);

        User entity = loadUser(id);
        UserDomain.from(entity).assertCanReactivate();

        entity.activate();
        User saved = repository.save(entity);
        log.info("Reactivated User ID: {}", saved.getUserPk());

        return ServiceResult.success(mapper.toStatusResponse(saved), Status.UPDATED);
    }

    /**
     * The ordinary single-resource read behind {@code GET /api/v1/sec/users/{id}}. Returns exactly
     * the {@code UserResponse} the API-SEC-005 search returns for the same row, {@code roles}
     * included, so a client can resolve a user it holds only an id for without having listed it.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_VIEW)")
    public ServiceResult<UserResponse> getById(Long id) {
        log.debug("Fetching User ID: {}", id);

        User entity = loadUser(id);

        return ServiceResult.success(mapper.toResponse(entity, userRoleService.rolesOf(id)));
    }

    /** API-SEC-005 — an empty match is success with empty content, never a 404 (CORE search contract). */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants).PERM_SEC_USERS_VIEW)")
    public ServiceResult<Page<UserResponse>> search(UserSearchRequest searchRequest) {
        log.debug("Searching User");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SecSearchSupport.assertSortAllowed(commonRequest.getSortField(), ALLOWED_SORT_FIELDS);

        Specification<User> spec = SpecBuilder.build(commonRequest,
            new SetAllowedFields(ALLOWED_SORT_FIELDS), DefaultFieldValueConverter.INSTANCE);
        String fullName = searchRequest.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            spec = spec.and(fullNameMatches(fullName));
        }

        Page<User> result =
            repository.findAll(spec, PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS));

        // One query for the whole page — never one per row (A.2.6).
        Map<Long, List<RoleSummaryResponse>> rolesByUser = userRoleService.rolesByUser(
            result.getContent().stream().map(User::getUserPk).toList());

        return ServiceResult.success(result.map(user -> mapper.toResponse(
            user, rolesByUser.getOrDefault(user.getUserPk(), List.of()))));
    }

    /**
     * REQ-SEC-034, reached only through {@code SecUserDirectoryApi.findContact}. Served by the
     * inherited {@code findById}, so it carries no QR id of its own (this repository's own
     * convention). An unknown id is an empty Optional, not a 404: the caller holds only an id and
     * absence is a legitimate cross-module answer.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<Optional<UserContact>> findContact(Long userPk) {
        log.debug("Resolving the cross-module contact of User ID: {}", userPk);

        return ServiceResult.success(repository.findById(userPk).map(UserService::toContact));
    }

    /**
     * REQ-SEC-035 / QR-SEC-039, reached only through
     * {@code SecUserDirectoryApi.findUserIdsHoldingPermission}. Gated on authentication alone: no
     * PERM_* of its own can exist, because DBF-SEC-051 makes SEC_ACTION_REG.screen_id NOT NULL, so
     * a screenless permission cannot be seeded — the API-SEC-027 exception the ALIGN block records.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<List<Long>> findUserIdsHoldingPermission(String permissionCode) {
        log.debug("Resolving the holders of permission code: {}", permissionCode);

        return ServiceResult.success(
            roleActionGrantRepository.findUserIdsHoldingPermission(permissionCode));
    }

    /**
     * {@code fullName} matches either language column (API-SEC-005 Request line); the shared
     * {@code SearchOperator} set has no OR, so this one predicate is expressed directly.
     */
    private Specification<User> fullNameMatches(String fullName) {
        String pattern = "%" + fullName.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("fullNameAr")), pattern),
            cb.like(cb.lower(root.get("fullNameEn")), pattern));
    }

    /** The body-dependent half of API-SEC-006's gate; SEC-403-FORBIDDEN is the module's denial. */
    private void assertMayAssignRoles() {
        if (!SecurityContextHelper.hasAuthority(PermissionConstants.PERM_SEC_USERS_UPDATE)) {
            throw new LocalizedException(Status.FORBIDDEN, SecErrorCodes.SEC_403_FORBIDDEN);
        }
    }

    private User loadUser(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, id));
    }

    /** DBF-SEC-001/003/005/006 only; {@code active} derived from DBF-SEC-007 (never the hash). */
    private static UserContact toContact(User user) {
        return new UserContact(user.getUserPk(), user.getEmail(), user.getFullNameAr(),
            user.getFullNameEn(), User.STATUS_ACTIVE.equals(user.getStatusCode()));
    }

    /** DBF-SEC-085 is nullable — an actor the token cannot resolve is recorded as null. */
    private User currentActor(String principal) {
        return repository.findByUsername(principal).orElse(null);
    }
}
