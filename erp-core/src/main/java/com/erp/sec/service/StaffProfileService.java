package com.erp.sec.service;

import com.erp.audit.crossmodule.AuditApi;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.util.SecurityContextHelper;
import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageStoreRequest;
import com.erp.file.crossmodule.ImageStoreResult;
import com.erp.sec.domain.UserDomain;
import com.erp.sec.dto.ProfilePhotoResponse;
import com.erp.sec.dto.StaffProfileResponse;
import com.erp.sec.dto.StaffProfileTenantResponse;
import com.erp.sec.dto.StaffProfileUpdateRequest;
import com.erp.sec.entity.User;
import com.erp.sec.exception.SecErrorCodes;
import com.erp.sec.mapper.UserMapper;
import com.erp.sec.repository.UserRepository;
import com.erp.tenant.TenantContext;
import com.erp.tenant.crossmodule.TenantLookupApi;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * tenant-maturity D — the STAFF caller's own profile ({@code /api/v1/sec/me}, REQ-SEC-086) and profile
 * photos, own and another staff user's (REQ-SEC-087). Photos go through FILE's image store
 * (XM-SEC-006 → XM-FILE-002): PUBLIC, random slug, the previous document discarded. The profile carries
 * no roles (ADR-SEC-064). {@code /me} is authentication-only on the STAFF chain. No caching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffProfileService {

    static final String ACTION_PROFILE_PHOTO_CHANGED = "PROFILE_PHOTO_CHANGED";

    private final UserRepository repository;
    private final UserMapper mapper;
    private final UserPhotoUrls photoUrls;
    private final FileImageStoreApi fileImageStoreApi;
    private final TenantLookupApi tenantLookupApi;
    private final AuditApi auditApi;

    /** REQ-SEC-086 — {@code GET /api/v1/sec/me}. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<StaffProfileResponse> getMyProfile() {
        User user = currentUser();
        log.debug("Reading the profile of User ID: {}", user.getUserPk());
        return ServiceResult.success(toProfile(user));
    }

    /** REQ-SEC-086 — {@code PATCH /api/v1/sec/me}: only the supplied fields (null keeps, empty clears). */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<StaffProfileResponse> updateMyProfile(StaffProfileUpdateRequest request) {
        User user = currentUser();
        log.info("Updating the profile of User ID: {}", user.getUserPk());
        mapper.updateEntityFromProfileRequest(user, request);
        return ServiceResult.success(toProfile(repository.save(user)), Status.UPDATED);
    }

    /** REQ-SEC-087 — {@code PUT /api/v1/sec/me/photo}. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<ProfilePhotoResponse> setMyPhoto(MultipartFile file) {
        return ServiceResult.success(storePhoto(currentUser(), file), Status.UPDATED);
    }

    /** REQ-SEC-087 — {@code DELETE /api/v1/sec/me/photo}; no photo is not an error. */
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void removeMyPhoto() {
        removePhoto(currentUser());
    }

    /** REQ-SEC-087 — {@code PUT /api/v1/sec/users/{id}/photo}: another STAFF user's photo. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).PERM_SEC_USERS_UPDATE)")
    public ServiceResult<ProfilePhotoResponse> setUserPhoto(Long id, MultipartFile file) {
        return ServiceResult.success(storePhoto(loadStaff(id), file), Status.UPDATED);
    }

    /** REQ-SEC-087 — {@code DELETE /api/v1/sec/users/{id}/photo}. */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.SecPermissions).PERM_SEC_USERS_UPDATE)")
    public void removeUserPhoto(Long id) {
        removePhoto(loadStaff(id));
    }

    /** Store new (RULE-SEC-061 via FILE's verdict) → point at it → discard the previous → audit. */
    private ProfilePhotoResponse storePhoto(User user, MultipartFile file) {
        log.info("Setting the photo of User ID: {}", user.getUserPk());
        ImageStoreResult result = fileImageStoreApi.storePublicImage(new ImageStoreRequest(
            UserDomain.PHOTO_OWNER_TYPE, user.getUserPk(), UserDomain.PHOTO_MODULE_CODE, readBytes(file),
            file == null ? null : file.getOriginalFilename(), UserDomain.PHOTO_MAX_BYTES, UserDomain.PHOTO_TYPES));
        UserDomain.assertPhotoAccepted(result.isStored());

        Long previous = user.getPhotoFileId();
        user.setPhotoFileId(result.image().documentId());
        repository.save(user);
        fileImageStoreApi.discard(previous);
        recordPhotoChange(user, "تعيين صورة المستخدم", "Profile photo set");
        log.info("Photo of User ID: {} is now document {}", user.getUserPk(), result.image().documentId());
        return ProfilePhotoResponse.builder().photoUrl(result.image().publicUrl()).build();
    }

    private void removePhoto(User user) {
        Long previous = user.getPhotoFileId();
        if (previous == null) {
            log.debug("User ID: {} has no photo to remove", user.getUserPk());
            return;
        }
        log.info("Removing the photo of User ID: {}", user.getUserPk());
        user.setPhotoFileId(null);
        repository.save(user);
        fileImageStoreApi.discard(previous);
        recordPhotoChange(user, "إزالة صورة المستخدم", "Profile photo removed");
    }

    private void recordPhotoChange(User user, String summaryAr, String summaryEn) {
        auditApi.record(AuditEntry.builder()
            .action(ACTION_PROFILE_PHOTO_CHANGED)
            .entityType(SecAuditEntries.ENTITY_TYPE_USER)
            .entityId(String.valueOf(user.getUserPk()))
            .summaryAr(summaryAr)
            .summaryEn(summaryEn)
            .build());
    }

    private StaffProfileResponse toProfile(User user) {
        StaffProfileTenantResponse tenant = tenantLookupApi.summaryOf(TenantContext.require())
            .map(t -> StaffProfileTenantResponse.builder().code(t.code()).nameAr(t.nameAr()).nameEn(t.nameEn()).build())
            .orElse(null);
        return mapper.toProfileResponse(user, photoUrls.of(user), tenant);
    }

    /** The authenticated STAFF caller (the chain refuses other realms with REALM_MISMATCH). */
    private User currentUser() {
        String username = SecurityContextHelper.getCurrentUsername();
        return repository.findByUsername(username)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, username));
    }

    private User loadStaff(Long id) {
        return repository.findStaffById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SecErrorCodes.SEC_404_USER, id));
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null) {
            return new byte[0];
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new LocalizedException(Status.INTERNAL_ERROR, CommonErrorCodes.INTERNAL_ERROR);
        }
    }
}
