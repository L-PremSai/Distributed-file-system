package com.medicalstorage.dto;

import com.medicalstorage.entity.ImageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO for multipart upload requests.
 * The actual file bytes come via MultipartFile in the controller;
 * these fields are the accompanying form parameters.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Metadata accompanying the medical image upload")
public class ImageUploadRequest {

    @NotBlank(message = "Patient ID must not be blank")
    @Schema(description = "Hospital patient identifier", example = "PAT-20240101-001")
    private String patientId;

    @NotNull(message = "Image type is required")
    @Schema(description = "Imaging modality", example = "MRI")
    private ImageType imageType;

    @Schema(description = "Clinical notes or description", example = "Brain MRI - baseline scan")
    private String description;
}
