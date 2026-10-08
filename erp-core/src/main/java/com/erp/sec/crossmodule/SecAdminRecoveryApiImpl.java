package com.erp.sec.crossmodule;

import com.erp.sec.service.SessionService;
import com.erp.sec.service.UserPasswordService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The small dedicated implementation build-create-service requires ("Exposing this module to others"): it
 * delegates to {@link UserPasswordService} and (tenant-maturity C12) {@link SessionService}, whose methods carry the gate
 * and the transaction, and injects no repository.
 */
@Component
@RequiredArgsConstructor
public class SecAdminRecoveryApiImpl implements SecAdminRecoveryApi {

    private final UserPasswordService userPasswordService;
    private final SessionService sessionService;

    @Override
    public Optional<RecoveryTarget> findRecoveryTarget(String username) {
        return userPasswordService.findRecoveryTarget(username).getData();
    }

    @Override
    public int resetSuperUserPassword(String username, String rawPassword, Boolean requireChangeAtNextLogin) {
        return userPasswordService.resetSuperUserPassword(username, rawPassword, requireChangeAtNextLogin).getData();
    }

    @Override
    public int terminateAllSessions() {
        return sessionService.terminateAllSessionsForPlatform().getData();
    }
}
