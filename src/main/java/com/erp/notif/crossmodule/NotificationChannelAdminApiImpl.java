package com.erp.notif.crossmodule;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.dto.ChannelUpdateRequest;
import com.erp.notif.entity.NotificationChannelConfig;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.repository.NotificationChannelConfigRepository;
import com.erp.notif.service.NotificationChannelConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationChannelAdminApiImpl implements NotificationChannelAdminApi {

    private final NotificationChannelConfigRepository repository;
    private final NotificationChannelConfigService service;

    @Override
    @Transactional
    public void setChannelEnabled(String channelTypeId, boolean enabled) {
        NotificationChannelConfig entity = repository.findByChannelTypeId(channelTypeId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_CHANNEL_CONFIG_NOT_FOUND, channelTypeId));

        // configJson is carried through unchanged — the mapper applies it unconditionally
        // (unlike isEnabledFl, it has no null-check), so omitting it here would wipe it.
        service.update(entity.getId(), ChannelUpdateRequest.builder()
            .isEnabledFl(enabled)
            .configJson(entity.getConfigJson())
            .build());
    }
}
