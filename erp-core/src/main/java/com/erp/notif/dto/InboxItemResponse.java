package com.erp.notif.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One in-app notification of the caller ({@code NOTIF_INBOX}, erp-core step 08). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "In-app notification - إشعار داخل التطبيق")
public class InboxItemResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "12")
    private Long id;

    @Schema(description = "Recipient user id - معرّف المستلِم", example = "42")
    private Long recipientUserId;

    @Schema(description = "Title (Arabic) - العنوان بالعربية", example = "تفعيل الحساب")
    private String titleAr;

    @Schema(description = "Title (English) - العنوان بالإنجليزية", example = "Activate your account")
    private String titleEn;

    @Schema(description = "Body (Arabic) - النص بالعربية", example = "تم إنشاء حساب لك")
    private String bodyAr;

    @Schema(description = "Body (English) - النص بالإنجليزية", example = "An account has been created for you")
    private String bodyEn;

    @Schema(description = "Whether the item was read - هل تمت القراءة", example = "false")
    private Boolean read;

    @Schema(description = "When it was marked read - تاريخ القراءة")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant readAt;

    @Schema(description = "Source entity reference type - نوع المرجع", example = "USER_ACCOUNT")
    private String referenceType;

    @Schema(description = "Source entity reference id - معرّف المرجع", example = "1001")
    private Long referenceId;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "system")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "alice")
    private String updatedBy;
}
