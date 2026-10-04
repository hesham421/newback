package com.erp.sec.mapper;

import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.UserCreateRequest;
import com.erp.sec.dto.UserResponse;
import com.erp.sec.dto.UserStatusResponse;
import com.erp.sec.dto.UserUpdateRequest;
import com.erp.sec.entity.SignupRequest;
import com.erp.sec.entity.User;
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

    public User toEntity(UserCreateRequest request, String passwordHash) {
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
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** API-SEC-011 approve — the sign-up's email becomes the new user's login (DBF-SEC-098). */
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
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** Mutates in place. Skips username, passwordHash, statusCode and isActiveFl — all immutable here. */
    public void updateEntityFromRequest(User entity, UserUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setEmail(request.getEmail());
        entity.setFullNameAr(request.getFullNameAr());
        entity.setFullNameEn(request.getFullNameEn());
    }

    /**
     * {@code roles} is a required argument, and there is deliberately no one-argument overload: a
     * caller that has not looked the roles up cannot accidentally emit a user without them. Pass
     * {@link List#of()} where the user provably holds none.
     */
    public UserResponse toResponse(User entity, List<RoleSummaryResponse> roles) {
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
            .lastLoginAt(entity.getLastLoginAt())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .roles(roles == null ? List.of() : roles)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
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
}
