package com.erp.sec.mapper;

import com.erp.sec.dto.SignupRequestResponse;
import com.erp.sec.dto.SignupSubmitRequest;
import com.erp.sec.entity.SignupRequest;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENT-SEC-013 (SignupRequest). */
@Component
public class SignupRequestMapper {

    /** API-SEC-002 — {@code submittedAt} and the PENDING {@code statusCode} are @PrePersist's. */
    public SignupRequest toEntity(SignupSubmitRequest request) {
        if (request == null) {
            return null;
        }
        return SignupRequest.builder()
            .email(request.getEmail())
            .fullNameAr(request.getFullNameAr())
            .fullNameEn(request.getFullNameEn())
            .build();
    }

    public SignupRequestResponse toResponse(SignupRequest entity) {
        if (entity == null) {
            return null;
        }
        return SignupRequestResponse.builder()
            .signupRequestPk(entity.getSignupRequestPk())
            .email(entity.getEmail())
            .fullNameAr(entity.getFullNameAr())
            .fullNameEn(entity.getFullNameEn())
            .submittedAt(entity.getSubmittedAt())
            .statusCode(entity.getStatusCode())
            .reviewedBy(entity.getReviewedBy())
            .reviewedAt(entity.getReviewedAt())
            .build();
    }
}
