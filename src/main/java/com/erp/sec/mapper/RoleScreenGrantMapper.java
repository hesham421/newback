package com.erp.sec.mapper;

import com.erp.sec.dto.RoleActionGrantNodeResponse;
import com.erp.sec.dto.RoleScreenGrantNodeResponse;
import com.erp.sec.dto.RoleScreenGrantResponse;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.entity.ScreenRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-008 (RoleScreenGrant). Both parents are parameters so the
 * caller cannot leave an FK unset; {@code grantedBy} is system-set by the service.
 */
@Component
public class RoleScreenGrantMapper {

    public RoleScreenGrant toEntity(Role role, ScreenRegistry screen) {
        if (role == null || screen == null) {
            return null;
        }
        return RoleScreenGrant.builder()
            .role(role)
            .screen(screen)
            .build();
    }

    public RoleScreenGrantResponse toResponse(RoleScreenGrant entity) {
        if (entity == null) {
            return null;
        }
        return RoleScreenGrantResponse.builder()
            .roleScreenGrantPk(entity.getRoleScreenGrantPk())
            .roleId(entity.getRole() == null ? null : entity.getRole().getRolePk())
            .screenId(entity.getScreen() == null ? null : entity.getScreen().getScreenRegPk())
            .grantedBy(entity.getGrantedBy())
            .grantedAt(entity.getGrantedAt())
            .build();
    }

    /**
     * Grant-tree screen node. The screen registry row is a parameter rather than read off the
     * grant, so the service can also emit a {@code granted = false} node for a screen it reached
     * only through an action grant — there is no screen grant to read it from in that case.
     */
    public RoleScreenGrantNodeResponse toNodeResponse(ScreenRegistry screen,
                                                      RoleScreenGrant grant,
                                                      List<RoleActionGrantNodeResponse> actions) {
        if (screen == null) {
            return null;
        }
        return RoleScreenGrantNodeResponse.builder()
            .screenRegPk(screen.getScreenRegPk())
            .pageCode(screen.getPageCode())
            .nameAr(screen.getNameAr())
            .nameEn(screen.getNameEn())
            .granted(grant != null)
            .grantedAt(grant == null ? null : grant.getGrantedAt())
            .actions(actions == null ? List.of() : actions)
            .build();
    }
}
