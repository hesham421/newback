package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-013 response — all fields. {@code reviewedBy} / {@code reviewedAt} stay null until
 * API-SEC-011 takes a decision; this table carries no createdBy/updatedBy columns of its own.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sign-up request - طلب تسجيل")
public class SignupRequestResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long signupRequestPk;

    @Schema(description = "Email address, becomes the login on approval - البريد الإلكتروني", example = "new@example.com")
    private String email;

    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;

    @Schema(description = "Submitted timestamp - تاريخ التقديم")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant submittedAt;

    @Schema(description = "SIGNUP_STATUS code - رمز الحالة", example = "PENDING")
    private String statusCode;

    @Schema(description = "Reviewed by - روجع بواسطة", example = "admin")
    private String reviewedBy;

    @Schema(description = "Reviewed timestamp - تاريخ المراجعة")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant reviewedAt;
}
