package com.medicalstorage.dto;

import com.medicalstorage.entity.ImageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO returned to the client after upload or metadata fetch.
 * Exposes the SeaweedFS download URL so the client can retrieve the file directly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Medical image metadata response")
public class ImageResponse {

    @Schema(description = "Internal database ID", example = "1")
    private Long id;

    @Schema(description = "Patient identifier", example = "PAT-20240101-001")
    private String patientId;

    @Schema(description = "Imaging modality", example = "MRI")
    private ImageType imageType;

    @Schema(description = "SeaweedFS file ID", example = "3,01637037d6")
    private String fileId;

    @Schema(description = "Original filename", example = "brain_mri.dcm")
    private String originalFilename;

    @Schema(description = "MIME type", example = "image/jpeg")
    private String contentType;

    @Schema(description = "File size in bytes", example = "2048000")
    private Long fileSize;

    @Schema(description = "Clinical notes")
    private String description;

    @Schema(description = "Upload timestamp")
    private LocalDateTime uploadDate;

    /**
     * Direct URL to the SeaweedFS volume node where this file is stored.
     * Clients can use this URL to stream the image without going through the API.
     */
    @Schema(description = "Publicly accessible SeaweedFS image URL",
            example = "http://localhost:8080/3,01637037d6")
    private String imageUrl;
}
