package com.erp.sec.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.Role;
import com.erp.sec.entity.RoleActionGrant;
import com.erp.sec.entity.RoleScreenGrant;
import com.erp.sec.entity.ScreenRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

/** erp-core 1.3.0 (TM-G) — the cascade sets RULE-SEC-054 and RULE-SEC-055 decide on revoke. */
class GrantRevokeDomainRulesTest {

    private static final Role ROLE = Role.builder().rolePk(7L).build();
    private static final ScreenRegistry ROLES = ScreenRegistry.builder().screenRegPk(10L).build();
    private static final ScreenRegistry USERS = ScreenRegistry.builder().screenRegPk(20L).build();

    private final RoleActionGrant rolesView = grant(101L, "VIEW", ROLES);
    private final RoleActionGrant rolesCreate = grant(102L, "CREATE", ROLES);
    private final RoleActionGrant rolesUpdate = grant(103L, "UPDATE", ROLES);
    private final RoleActionGrant usersView = grant(201L, "VIEW", USERS);

    @Test
    void screenRevoke_cascadesExactlyTheRolesActionGrantsOnThatScreen() {
        RoleScreenGrantDomain screenGrant = RoleScreenGrantDomain.from(
            RoleScreenGrant.builder().role(ROLE).screen(ROLES).build());

        assertThat(screenGrant.cascadeOnRevoke(List.of(rolesView, usersView, rolesCreate, rolesUpdate)))
            .containsExactly(rolesView, rolesCreate, rolesUpdate);
        assertThat(screenGrant.cascadeOnRevoke(List.of(usersView))).isEmpty();
    }

    @Test
    void viewRevoke_cascadesTheScreensOtherActionGrants_whileAnyOtherActionCascadesNothing() {
        List<RoleActionGrant> onScreen = List.of(rolesView, rolesCreate, rolesUpdate);

        assertThat(RoleActionGrantDomain.from(rolesView).cascadeOnRevoke(onScreen))
            .containsExactly(rolesCreate, rolesUpdate);
        assertThat(RoleActionGrantDomain.from(rolesCreate).cascadeOnRevoke(onScreen)).isEmpty();
        assertThat(RoleActionGrantDomain.from(rolesView).cascadeOnRevoke(List.of(rolesView)))
            .as("VIEW alone on its screen cascades nothing").isEmpty();
    }

    private static RoleActionGrant grant(Long actionId, String actionCode, ScreenRegistry screen) {
        ActionRegistry action = ActionRegistry.builder()
            .actionRegPk(actionId).actionCode(actionCode).screen(screen).build();
        return RoleActionGrant.builder().role(ROLE).action(action).build();
    }
}
