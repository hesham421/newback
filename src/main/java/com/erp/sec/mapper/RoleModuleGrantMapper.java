package com.erp.sec.mapper;

import com.erp.sec.dto.ModuleGrantRevokeResponse;
import com.erp.sec.dto.RoleModuleGrantNodeResponse;
import com.erp.sec.dto.RoleModuleGrantResponse;
import com.erp.sec.dto.RoleScreenGrantNodeResponse;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleModuleGrant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-007 (RoleModuleGrant). Both parents are parameters so the
 * caller cannot leave an FK unset; {@code grantedBy} is system-set by the service.
 */
@Component
public class RoleModuleGrantMapper {

    public RoleModuleGrant toEntity(Role role, ModuleRegistry module) {
        if (role == null || module == null) {
            return null;
        }
        return RoleModuleGrant.builder()
            .role(role)
            .module(module)
            .build();
    }

    public RoleModuleGrantResponse toResponse(RoleModuleGrant entity) {
        if (entity == null) {
            return null;
        }
        return RoleModuleGrantResponse.builder()
            .roleModuleGrantPk(entity.getRoleModuleGrantPk())
            .roleId(entity.getRole() == null ? null : entity.getRole().getRolePk())
            .moduleId(entity.getModule() == null ? null : entity.getModule().getModuleRegPk())
            .grantedBy(entity.getGrantedBy())
            .grantedAt(entity.getGrantedAt())
            .build();
    }

    /** API-SEC-015 confirmation — the counts come from the cascade the service actually performed. */
    public ModuleGrantRevokeResponse toRevokeResponse(int revokedScreenGrants, int revokedActionGrants) {
        return ModuleGrantRevokeResponse.builder()
            .revokedScreenGrants(revokedScreenGrants)
            .revokedActionGrants(revokedActionGrants)
            .build();
    }

    /**
     * Grant-tree module node. Same reason as the screen node for taking the registry row as a
     * parameter: a module reached only through a screen or action grant has no module grant to
     * read it from, and is emitted with {@code granted = false}.
     */
    public RoleModuleGrantNodeResponse toNodeResponse(ModuleRegistry module,
                                                      RoleModuleGrant grant,
                                                      List<RoleScreenGrantNodeResponse> screens) {
        if (module == null) {
            return null;
        }
        return RoleModuleGrantNodeResponse.builder()
            .moduleRegPk(module.getModuleRegPk())
            .code(module.getCode())
            .nameAr(module.getNameAr())
            .nameEn(module.getNameEn())
            .granted(grant != null)
            .grantedAt(grant == null ? null : grant.getGrantedAt())
            .screens(screens == null ? List.of() : screens)
            .build();
    }
}
