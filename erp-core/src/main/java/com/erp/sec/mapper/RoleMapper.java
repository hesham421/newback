package com.erp.sec.mapper;

import com.erp.sec.dto.RoleCreateRequest;
import com.erp.sec.dto.RoleGrantTreeResponse;
import com.erp.sec.dto.RoleModuleGrantNodeResponse;
import com.erp.sec.dto.RoleResponse;
import com.erp.sec.dto.RoleSummaryResponse;
import com.erp.sec.dto.RoleUpdateRequest;
import com.erp.sec.dto.RoleUserCountResponse;
import com.erp.sec.entity.Role;
import com.erp.sec.repository.RoleUserCountProjection;
import java.util.List;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENT-SEC-002 (Role). */
@Component
public class RoleMapper {

    public Role toEntity(RoleCreateRequest request) {
        if (request == null) {
            return null;
        }
        return Role.builder()
            .code(request.getCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .descriptionAr(request.getDescriptionAr())
            .descriptionEn(request.getDescriptionEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** {@code code} is immutable and therefore never written here — see {@link RoleUpdateRequest}. */
    public void updateEntityFromRequest(Role entity, RoleUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setDescriptionAr(request.getDescriptionAr());
        entity.setDescriptionEn(request.getDescriptionEn());
    }

    public RoleResponse toResponse(Role entity) {
        if (entity == null) {
            return null;
        }
        return RoleResponse.builder()
            .rolePk(entity.getRolePk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .descriptionAr(entity.getDescriptionAr())
            .descriptionEn(entity.getDescriptionEn())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /** QR-SEC-022's grouped projection, not an entity — the widget's {@code usersPerRole} entry. */
    public RoleUserCountResponse toUserCountResponse(RoleUserCountProjection projection) {
        if (projection == null) {
            return null;
        }
        return RoleUserCountResponse.builder()
            .roleId(projection.getRolePk())
            .code(projection.getCode())
            .nameAr(projection.getNameAr())
            .nameEn(projection.getNameEn())
            .userCount(projection.getUserCount())
            .build();
    }

    /** The nested {@code {roleId, code, nameAr, nameEn}} view API-SEC-008 returns. */
    public RoleSummaryResponse toSummaryResponse(Role entity) {
        if (entity == null) {
            return null;
        }
        return RoleSummaryResponse.builder()
            .roleId(entity.getRolePk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .build();
    }

    /** Grant-tree root — the role's identity; the modules are assembled by the service. */
    public RoleGrantTreeResponse toGrantTreeResponse(Role entity,
                                                     List<RoleModuleGrantNodeResponse> modules) {
        if (entity == null) {
            return null;
        }
        return RoleGrantTreeResponse.builder()
            .rolePk(entity.getRolePk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .modules(modules == null ? List.of() : modules)
            .build();
    }
}
