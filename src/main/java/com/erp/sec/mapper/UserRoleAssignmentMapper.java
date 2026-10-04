package com.erp.sec.mapper;

import com.erp.sec.entity.Role;
import com.erp.sec.entity.User;
import com.erp.sec.entity.UserRoleAssignment;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-SEC-003 (UserRoleAssignment). Both parents are parameters so the caller
 * cannot leave an FK unset; this table has no request DTO of its own beyond a list of role ids.
 */
@Component
public class UserRoleAssignmentMapper {

    public UserRoleAssignment toEntity(User user, Role role) {
        if (user == null || role == null) {
            return null;
        }
        return UserRoleAssignment.builder()
            .user(user)
            .role(role)
            .build();
    }
}
