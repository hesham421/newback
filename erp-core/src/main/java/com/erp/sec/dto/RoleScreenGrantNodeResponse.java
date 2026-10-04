package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Screen level of the role grant tree. {@code granted} is the ENT-SEC-008 grant itself; a node may
 * carry {@code granted = false} when the role holds an action under this screen but not the screen
 * grant — RULE-SEC-002 forbids creating that state through the API, so it can only arrive as data
 * drift, and hiding it would hide exactly what an audit is looking for.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Screen grant held by a role - شاشة ممنوحة للدور")
public class RoleScreenGrantNodeResponse {

    @Schema(description = "Screen registry identifier - معرف الشاشة", example = "1")
    private Long screenRegPk;

    @Schema(description = "Page code - رمز الصفحة", example = "SEC_USERS")
    private String pageCode;

    @Schema(description = "Screen name (Arabic) - اسم الشاشة بالعربية", example = "المستخدمون")
    private String nameAr;

    @Schema(description = "Screen name (English) - اسم الشاشة بالإنجليزية", example = "Users")
    private String nameEn;

    @Schema(description = "Screen grant held - هل الشاشة ممنوحة", example = "true")
    private Boolean granted;

    @Schema(description = "Granted timestamp, null when not granted - تاريخ المنح")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant grantedAt;

    @Schema(description = "Action grants held beneath this screen - الإجراءات الممنوحة ضمن الشاشة")
    private List<RoleActionGrantNodeResponse> actions;
}
