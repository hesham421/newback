package com.erp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of {@code PATCH /api/v1/files/{id}/visibility} (erp-core step 07): {@code PUBLIC} publishes the
 * document under a fresh random slug (kept if it is already public), {@code PRIVATE} withdraws it and
 * drops the slug.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Change a file's visibility - تغيير مستوى إتاحة الملف")
public class VisibilityUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "PUBLIC|PRIVATE", message = "{validation.pattern}")
    @Schema(description = "PUBLIC or PRIVATE - عام أو خاص", example = "PUBLIC", allowableValues = {"PUBLIC", "PRIVATE"})
    private String visibility;
}
