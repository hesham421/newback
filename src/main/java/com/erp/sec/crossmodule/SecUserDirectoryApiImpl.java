package com.erp.sec.crossmodule;

import com.erp.sec.service.UserService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The small dedicated implementation build-create-service requires ("Exposing this module to
 * others"): it delegates to {@link UserService} and injects no repository, so the exposed surface
 * stays narrower than the internal service and the boundary keeps one owner.
 */
@Component
@RequiredArgsConstructor
public class SecUserDirectoryApiImpl implements SecUserDirectoryApi {

    private final UserService userService;

    @Override
    public Optional<UserContact> findContact(Long userPk) {
        return userService.findContact(userPk).getData();
    }

    @Override
    public List<Long> findUserIdsHoldingPermission(String permissionCode) {
        return userService.findUserIdsHoldingPermission(permissionCode).getData();
    }
}
