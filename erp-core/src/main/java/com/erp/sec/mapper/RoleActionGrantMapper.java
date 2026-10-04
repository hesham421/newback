package com.erp.sec.mapper;

import com.erp.sec.dto.RoleActionGrantNodeResponse;
import com.erp.sec.dto.RoleActionGrantResponse;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-009 (RoleActionGrant). Both parents are parameters so the
 * caller cannot leave an FK unset; {@code grantedBy} is system-set by the service.
 */
@Component
public class RoleActionGrantMapper {

    public RoleActionGrant toEntity(Role role, ActionRegistry action) {
        if (role == null || action == null) {
            return null;
        }
        return RoleActionGrant.builder()
            .role(role)
            .action(action)
            .build();
    }

    public RoleActionGrantResponse toResponse(RoleActionGrant entity) {
        if (entity == null) {
            return null;
        }
        return RoleActionGrantResponse.builder()
            .roleActionGrantPk(entity.getRoleActionGrantPk())
            .roleId(entity.getRole() == null ? null : entity.getRole().getRolePk())
            .actionId(entity.getAction() == null ? null : entity.getAction().getActionRegPk())
            .grantedBy(entity.getGrantedBy())
            .grantedAt(entity.getGrantedAt())
            .build();
    }

    /** Grant-tree leaf — the registry action behind the grant, plus when it was granted. */
    public RoleActionGrantNodeResponse toNodeResponse(RoleActionGrant entity) {
        if (entity == null || entity.getAction() == null) {
            return null;
        }
        ActionRegistry action = entity.getAction();
        return RoleActionGrantNodeResponse.builder()
            .actionRegPk(action.getActionRegPk())
            .actionCode(action.getActionCode())
            .permissionCode(action.getPermissionCode())
            .nameAr(action.getNameAr())
            .nameEn(action.getNameEn())
            .grantedAt(entity.getGrantedAt())
            .build();
    }
}
