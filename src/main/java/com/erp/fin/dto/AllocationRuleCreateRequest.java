package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-016 request body (SVC-API-CRUD.md): {@code {nameAr, nameEn, sourceAccountId,
 * targets: [...]}}. Excludes {allocationRulePk, isActiveFl, audit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a cost-allocation rule - إنشاء قاعدة توزيع تكلفة",
    example = """
        {
          "nameAr": "توزيع مصاريف الإدارة",
          "nameEn": "Administrative expense allocation",
          "sourceAccountId": 31,
          "targets": [
            {
              "targetAccountId": 21,
              "dimensionValueId": 5,
              "distributionTypeCode": "PERCENTAGE",
              "distributionValue": 25.0000,
              "isRemainderFl": false
            },
            {
              "targetAccountId": 22,
              "distributionTypeCode": "REMAINDER",
              "isRemainderFl": true
            }
          ]
        }""")
public class AllocationRuleCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "توزيع مصاريف الإدارة")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية",
        example = "Administrative expense allocation")
    private String nameEn;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Source account id - معرّف الحساب المصدر", example = "31")
    private Long sourceAccountId;

    @NotEmpty(message = "{validation.required}")
    @Valid
    @Schema(description = "Allocation targets — when any target uses PERCENTAGE the set must also "
        + "carry exactly one REMAINDER target (ENT-FIN-014, RULE-FIN-003 reused) - أهداف التوزيع",
        example = """
            [
              {
                "targetAccountId": 21,
                "dimensionValueId": 5,
                "distributionTypeCode": "PERCENTAGE",
                "distributionValue": 25.0000,
                "isRemainderFl": false
              },
              {
                "targetAccountId": 22,
                "distributionTypeCode": "REMAINDER",
                "isRemainderFl": true
              }
            ]""")
    private List<AllocationTargetCreateRequest> targets;
}
