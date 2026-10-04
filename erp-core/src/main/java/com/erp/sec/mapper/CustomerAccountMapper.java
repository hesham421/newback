package com.erp.sec.mapper;

import com.erp.sec.dto.CustomerProfileResponse;
import com.erp.sec.dto.CustomerProfileUpdateRequest;
import com.erp.sec.dto.CustomerRegisterRequest;
import com.erp.sec.entity.User;
import org.springframework.stereotype.Component;

/**
 * erp-core step 06 — maps CUSTOMER-realm {@link User} rows to and from the customer DTOs. Kept apart
 * from {@link UserMapper} (staff management) so the two realms' shapes never mix.
 */
@Component
public class CustomerAccountMapper {

    /** A new, unverified customer account: the e-mail is the login, the one name fills both columns. */
    public User toEntity(CustomerRegisterRequest request, String passwordHash) {
        if (request == null) {
            return null;
        }
        return User.builder()
            .username(request.getEmail())
            .email(request.getEmail())
            .fullNameAr(request.getFullName())
            .fullNameEn(request.getFullName())
            .passwordHash(passwordHash)
            .statusCode(User.STATUS_PENDING_VERIFICATION)
            .realm(User.REALM_CUSTOMER)
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** Partial update in place: only the names, only when given. */
    public void updateEntityFromRequest(User entity, CustomerProfileUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getFullNameAr() != null) {
            entity.setFullNameAr(request.getFullNameAr());
        }
        if (request.getFullNameEn() != null) {
            entity.setFullNameEn(request.getFullNameEn());
        }
    }

    public CustomerProfileResponse toResponse(User entity) {
        if (entity == null) {
            return null;
        }
        return CustomerProfileResponse.builder()
            .id(entity.getUserPk())
            .email(entity.getEmail())
            .fullNameAr(entity.getFullNameAr())
            .fullNameEn(entity.getFullNameEn())
            .statusCode(entity.getStatusCode())
            .realm(entity.getRealm())
            .lastLoginAt(entity.getLastLoginAt())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
