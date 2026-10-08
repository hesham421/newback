package com.erp.tenant.mapper;

import com.erp.tenant.dto.TenantAdminResetResponse;
import com.erp.tenant.dto.TenantBrandingResponse;
import com.erp.tenant.dto.TenantBrandingUpdateRequest;
import com.erp.tenant.dto.TenantCreateRequest;
import com.erp.tenant.dto.TenantResponse;
import com.erp.tenant.dto.TenantUpdateRequest;
import com.erp.tenant.dto.TenantUsageResponse;
import com.erp.tenant.entity.Tenant;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for {@link Tenant}. The update mapping (tenant-maturity B) covers the names and the
 * profile only: the code is immutable, the status and its facts change only through {@code activate(..)} /
 * {@code suspend(..)}, the branding through its own mapping (E); the create request's administrator fields are not columns.
 */
@Component
public class TenantMapper {

    public Tenant toEntity(TenantCreateRequest request) {
        if (request == null) {
            return null;
        }
        return Tenant.builder()
            .code(request.getCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .build();
    }

    /** Full replacement of the editable fields; never {@code code}, {@code statusCode} or the suspension facts. */
    public void updateEntityFromRequest(Tenant entity, TenantUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setContactEmail(request.getContactEmail());
        entity.setContactPhone(request.getContactPhone());
        entity.setCountryCode(request.getCountryCode());
        entity.setDefaultLocale(request.getDefaultLocale());
        entity.setTimezone(request.getTimezone());
        entity.setNotes(request.getNotes());
    }

    /**
     * tenant-maturity E: the brand colour as sent — {@code TenantDomain} has checked it (RULE-TENANT-021); trimming,
     * blank → null and upper case are the entity's {@code @PreUpdate} normalisation (A.1.17).
     */
    public void updateBrandingFromRequest(Tenant entity, TenantBrandingUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setBrandColor(request.getBrandColor());
    }

    /** {@code logoUrl} is resolved by the service inside the tenant (FILE's lookup is tenant-filtered); null = none. */
    public TenantResponse toResponse(Tenant entity, String logoUrl) {
        if (entity == null) {
            return null;
        }
        return TenantResponse.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .statusCode(entity.getStatusCode())
            .contactEmail(entity.getContactEmail())
            .contactPhone(entity.getContactPhone())
            .countryCode(entity.getCountryCode())
            .defaultLocale(entity.getDefaultLocale())
            .timezone(entity.getTimezone())
            .notes(entity.getNotes())
            .suspendedAt(entity.getSuspendedAt())
            .suspendedBy(entity.getSuspendedBy())
            .suspensionReason(entity.getSuspensionReason())
            .logoUrl(logoUrl)
            .brandColor(entity.getBrandColor())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /** tenant-maturity E (REQ-TENANT-031/032): the six branding fields, nothing else. */
    public TenantBrandingResponse toBrandingResponse(Tenant entity, String logoUrl) {
        if (entity == null) {
            return null;
        }
        return TenantBrandingResponse.builder()
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .logoUrl(logoUrl)
            .brandColor(entity.getBrandColor())
            .defaultLocale(entity.getDefaultLocale())
            .build();
    }

    public TenantAdminResetResponse toAdminResetResponse(String username, int sessionsTerminated) {
        return TenantAdminResetResponse.builder()
            .username(username)
            .sessionsTerminated(sessionsTerminated)
            .build();
    }

    /** The figures as counted by SEC, FILE and NOTIF inside the tenant (REQ-TENANT-028). */
    public TenantUsageResponse toUsageResponse(Long tenantId, int staffUsers, int customerUsers, int activeSessions,
                                               long fileDocuments, long fileBytes, long notificationsLast30Days,
                                               Instant collectedAt) {
        return TenantUsageResponse.builder()
            .id(tenantId)
            .staffUsers(staffUsers)
            .customerUsers(customerUsers)
            .activeSessions(activeSessions)
            .fileDocuments(fileDocuments)
            .fileBytes(fileBytes)
            .notificationsLast30Days(notificationsLast30Days)
            .collectedAt(collectedAt)
            .build();
    }
}
