package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** tenant-maturity D (REQ-SEC-087) — the public URL of a user's new photo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Profile photo - صورة المستخدم")
public class ProfilePhotoResponse {

    @Schema(description = "Public URL of the photo (no token needed) - الرابط العام للصورة",
        example = "/api/v1/public/files/PLATFORM/3q2-7wEjK9mZ0aBcDeFgHiJkLmNoPqRs")
    private String photoUrl;
}
