package com.erp.sec.mapper;

import com.erp.sec.dto.PasswordChangeResponse;
import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.StaffProfileResponse;
import com.erp.sec.dto.StaffProfileTenantResponse;
import com.erp.sec.dto.StaffProfileUpdateRequest;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserStatusResponse;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.entity.SignupRequest;
import com.erp.sec.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-001 (User). The already-hashed password is an explicit
 * parameter so a caller cannot build a User without one, and no raw secret ever reaches this class.
 */
@Component
public class UserMapper {

    /** USER_STATUS code a directly created or approved user starts at (A7 state machine). */
    private static final String STATUS_ACTIVE = "ACTIVE";

    /**
     * tenant-maturity D: {@code passwordChangeRequired} is decided by {@code UserDomain} (RULE-SEC-058);
     * empty profile values are stored as null.
     */
    public User toEntity(UserCreateRequest request, String passwordHash, boolean passwordChangeRequired,
                         Instant passwordChangedAt) {
        if (request == null) {
            return null;
        }
        return User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .fullNameAr(request.getFullNameAr())
            .fullNameEn(request.getFullNameEn())
            .passwordHash(passwordHash)
            .statusCode(STATUS_ACTIVE)
            .realm(User.REALM_STAFF)
            .isActiveFl(Boolean.TRUE)
            .phone(emptyToNull(request.getPhone()))
            .jobTitleAr(emptyToNull(request.getJobTitleAr()))
            .jobTitleEn(emptyToNull(request.getJobTitleEn()))
            .preferredLocale(emptyToNull(request.getPreferredLocale()))
            .passwordChangeRequiredFl(passwordChangeRequired)
            .passwordChangedAt(passwordChangedAt)
            .build();
    }

    /**
     * API-SEC-011 approve — the sign-up's email becomes the new user's login (DBF-SEC-098). The
     * admin-approved sign-up is staff onboarding: the account is explicitly {@code REALM='STAFF'}
     * (erp-core step 06, task 7).
     */
    public User toEntity(SignupRequest signupRequest, String passwordHash) {
        if (signupRequest == null) {
            return null;
        }
        return User.builder()
            .username(signupRequest.getEmail())
            .email(signupRequest.getEmail())
            .fullNameAr(signupRequest.getFullNameAr())
            .fullNameEn(signupRequest.getFullNameEn())
            .passwordHash(passwordHash)
            .statusCode(STATUS_ACTIVE)
            .realm(User.REALM_STAFF)
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /**
     * Mutates in place. Skips username, passwordHash, statusCode and isActiveFl — all immutable here.
     * tenant-maturity D: a null profile field keeps its value, an empty one clears it.
     */
    public void updateEntityFromRequest(User entity, UserUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setEmail(request.getEmail());
        entity.setFullNameAr(request.getFullNameAr());
        entity.setFullNameEn(request.getFullNameEn());
        applyProfile(entity, request.getPhone(), request.getJobTitleAr(), request.getJobTitleEn(),
            request.getPreferredLocale());
    }

    /** tenant-maturity D (REQ-SEC-086) — PATCH /me: only the supplied fields; empty clears the optional ones. */
    public void updateEntityFromProfileRequest(User entity, StaffProfileUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getFullNameAr() != null) {
            entity.setFullNameAr(request.getFullNameAr());
        }
        if (request.getFullNameEn() != null) {
            entity.setFullNameEn(request.getFullNameEn());
        }
        applyProfile(entity, request.getPhone(), request.getJobTitleAr(), request.getJobTitleEn(),
            request.getPreferredLocale());
    }

    /**
     * {@code roles} is a required argument, and there is deliberately no one-argument overload: a
     * caller that has not looked the roles up cannot accidentally emit a user without them. Pass
     * {@link List#of()} where the user provably holds none.
     */
    public UserResponse toResponse(User entity, List<RoleSummaryResponse> roles, String photoUrl) {
        if (entity == null) {
            return null;
        }
        return UserResponse.builder()
            .userPk(entity.getUserPk())
            .username(entity.getUsername())
            .email(entity.getEmail())
            .fullNameAr(entity.getFullNameAr())
            .fullNameEn(entity.getFullNameEn())
            .statusCode(entity.getStatusCode())
            .realm(entity.getRealm())
            .lastLoginAt(entity.getLastLoginAt())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .phone(entity.getPhone())
            .jobTitleAr(entity.getJobTitleAr())
            .jobTitleEn(entity.getJobTitleEn())
            .preferredLocale(entity.getPreferredLocale())
            .photoUrl(photoUrl)
            .passwordChangeRequired(Boolean.TRUE.equals(entity.getPasswordChangeRequiredFl()))
            .passwordChangedAt(entity.getPasswordChangedAt())
            .roles(roles == null ? List.of() : roles)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /** tenant-maturity D (REQ-SEC-086) — the caller's own profile; no roles (ADR-SEC-064). */
    public StaffProfileResponse toProfileResponse(User entity, String photoUrl, StaffProfileTenantResponse tenant) {
        if (entity == null) {
            return null;
        }
        return StaffProfileResponse.builder()
            .userPk(entity.getUserPk())
            .username(entity.getUsername())
            .email(entity.getEmail())
            .fullNameAr(entity.getFullNameAr())
            .fullNameEn(entity.getFullNameEn())
            .phone(entity.getPhone())
            .jobTitleAr(entity.getJobTitleAr())
            .jobTitleEn(entity.getJobTitleEn())
            .preferredLocale(entity.getPreferredLocale())
            .photoUrl(photoUrl)
            .passwordChangeRequired(Boolean.TRUE.equals(entity.getPasswordChangeRequiredFl()))
            .lastLoginAt(entity.getLastLoginAt())
            .tenant(tenant)
            .build();
    }

    /** tenant-maturity D — the outcome of an admin-set or self-change; never a secret. */
    public PasswordChangeResponse toPasswordChangeResponse(User entity, int sessionsTerminated) {
        if (entity == null) {
            return null;
        }
        return PasswordChangeResponse.builder()
            .userPk(entity.getUserPk())
            .passwordChangeRequired(Boolean.TRUE.equals(entity.getPasswordChangeRequiredFl()))
            .passwordChangedAt(entity.getPasswordChangedAt())
            .sessionsTerminated(sessionsTerminated)
            .build();
    }

    public UserStatusResponse toStatusResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return UserStatusResponse.builder()
            .userPk(entity.getUserPk())
            .statusCode(entity.getStatusCode())
            .build();
    }

    /** Null keeps, empty clears, anything else is set (validated by the DTO patterns). */
    private static void applyProfile(User entity, String phone, String jobTitleAr, String jobTitleEn,
                                     String preferredLocale) {
        if (phone != null) {
            entity.setPhone(emptyToNull(phone));
        }
        if (jobTitleAr != null) {
            entity.setJobTitleAr(emptyToNull(jobTitleAr));
        }
        if (jobTitleEn != null) {
            entity.setJobTitleEn(emptyToNull(jobTitleEn));
        }
        if (preferredLocale != null) {
            entity.setPreferredLocale(emptyToNull(preferredLocale));
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
