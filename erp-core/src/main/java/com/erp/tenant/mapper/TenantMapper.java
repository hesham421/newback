package com.erp.tenant.mapper;

import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.entity.Tenant;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for {@link Tenant}. There is no update mapping: the code is immutable and
 * the status changes only through {@code activate()}/{@code suspend()}; the administrator fields of
 * the create request are not tenant columns (they go to the provisioning contributors).
 */
@Component
public class TenantMapper {

    public Tenant toEntity(TenantCreateRequest request) {
        if (request == null) {
            return null;
        }
        return Tenant.builder()
            .code(request.getCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .build();
    }

    public TenantResponse toResponse(Tenant entity) {
        if (entity == null) {
            return null;
        }
        return TenantResponse.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .statusCode(entity.getStatusCode())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
