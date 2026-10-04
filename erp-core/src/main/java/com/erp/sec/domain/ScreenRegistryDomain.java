package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.exception.SecErrorCodes;

/**
 * Domain companion for ENT-SEC-005 (ScreenRegistry): RULE-SEC-004 — a screen registration whose
 * module code has no ModuleRegistry row is rejected ({@code SEC-409-MODULE-NOT-REGISTERED}) —
 * plus the page-code uniqueness pre-check API-SEC-019 runs alongside it (QR-SEC-036 →
 * {@code SEC-409-SCREEN-DUP}).
 */
public final class ScreenRegistryDomain {

    private final String pageCode;
    private final boolean active;

    private ScreenRegistryDomain(String pageCode, boolean active) {
        this.pageCode = pageCode;
        this.active = active;
    }

    /**
     * API-SEC-019 (register screen): RULE-SEC-004 first, then page-code uniqueness. The caller
     * resolves both facts with QR-SEC-036 before calling.
     *
     * @throws LocalizedException {@code SEC-409-MODULE-NOT-REGISTERED} / {@code SEC-409-SCREEN-DUP}
     */
    public static ScreenRegistryDomain create(String pageCode,
                                              boolean moduleRegistered,
                                              boolean pageCodeAlreadyTaken) {
        if (!moduleRegistered) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_MODULE_NOT_REGISTERED);
        }
        if (pageCodeAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                SecErrorCodes.SEC_409_SCREEN_DUP, pageCode);
        }
        return new ScreenRegistryDomain(pageCode, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static ScreenRegistryDomain from(ScreenRegistry entity) {
        return new ScreenRegistryDomain(entity.getPageCode(),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    public String getPageCode() {
        return pageCode;
    }

    public boolean isActive() {
        return active;
    }
}
