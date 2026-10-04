package com.erp.audit.crossmodule;

import com.erp.audit.service.AuditRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link AuditApi} over the audit module's internal {@link AuditRecordingService}. No
 * {@code @PreAuthorize}: every module records its own facts here, also from anonymous paths (a login
 * is recorded before the caller is authenticated); it is reachable in-process only — no controller
 * writes audit rows.
 */
@Component
@RequiredArgsConstructor
public class AuditApiImpl implements AuditApi {

    private final AuditRecordingService recordingService;

    @Override
    public void record(AuditEntry entry) {
        recordingService.record(entry);
    }
}
