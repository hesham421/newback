package com.erp.notif.crossmodule;

import com.erp.notif.dto.DispatchRequest;
import com.erp.notif.service.DispatchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The dedicated adapter implementing NOTIF's {@link NotificationDispatchApi} — it maps the narrow
 * cross-module {@link DispatchCommand} read-model onto the internal {@link DispatchRequest} and
 * delegates to {@link DispatchService}. Kept separate from the internal service so the exposed
 * cross-module surface stays narrow (build-create-service "Exposing this module to others").
 * Authorization is enforced on the delegate (@PreAuthorize isAuthenticated(), SEC-BE).
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatchApiImpl implements NotificationDispatchApi {

    private final DispatchService dispatchService;
    private final RecipientDirectory recipientDirectory;

    @Override
    public List<Long> dispatch(DispatchCommand command) {
        return dispatchService.dispatch(toRequest(command)).getData().getLogIds();
    }

    @Override
    public List<Long> dispatchIndependently(DispatchCommand command) {
        // The recipient is resolved here, inside the consuming module's transaction, so a user account
        // that transaction has not committed yet is still visible. The send itself then runs in the
        // REQUIRES_NEW entry point, committing or rolling back on its own without ever marking the
        // consuming module's transaction rollback-only.
        boolean recipientActive = recipientDirectory.isActive(command.recipientId());
        return dispatchService.dispatchSystem(toRequest(command), recipientActive).getData().getLogIds();
    }

    /** Maps the narrow cross-module read-model onto NOTIF's internal request DTO. */
    private DispatchRequest toRequest(DispatchCommand command) {
        return DispatchRequest.builder()
            .recipientId(command.recipientId())
            .templateCode(command.templateCode())
            .channelHint(command.channelHint())
            .moduleCode(command.moduleCode())
            .referenceId(command.referenceId())
            .referenceType(command.referenceType())
            .variables(command.variables())
            .build();
    }
}
