package com.medicalstorage.controller;

import com.medicalstorage.dto.*;
import com.medicalstorage.entity.ImageType;
import com.medicalstorage.service.MedicalImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller exposing all medical-image operations.
 *
 * Base path: /api/images  and  /api/patients
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Medical Images", description = "Upload, retrieve, download, list, and delete medical images")
public class MedicalImageController {

    private final MedicalImageService imageService;

    // ──────────────────────────────────────────────────────────
    // POST /api/images/upload
    // ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Upload a medical image",
        description = "Accepts a multipart file plus metadata. The file is stored in SeaweedFS; " +
                      "metadata is persisted in MySQL. Returns the record with a direct download URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Image uploaded successfully",
            content = @Content(schema = @Schema(implementation = ImageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or file",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "SeaweedFS storage error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/api/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> uploadImage(
            @Parameter(description = "Medical image file (JPEG/PNG/TIFF/DICOM)", required = true)
            @RequestPart("file") MultipartFile file,

            @Parameter(description = "Patient ID", required = true, example = "PAT-001")
            @RequestPart("patientId") @NotBlank String patientId,

            @Parameter(description = "Imaging modality", required = true, example = "MRI")
            @RequestPart("imageType") String imageType,

            @Parameter(description = "Clinical notes", example = "Baseline brain MRI")
            @RequestPart(value = "description", required = false) String description) {

        // Build the request DTO from individual form parts
        ImageUploadRequest request = ImageUploadRequest.builder()
                .patientId(patientId)
                .imageType(ImageType.valueOf(imageType.toUpperCase()))
                .description(description)
                .build();

        ImageResponse response = imageService.uploadImage(file, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/images/{id}  – metadata + URL
    // ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Get image metadata",
        description = "Returns all metadata for a single image record, including the SeaweedFS URL."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = ImageResponse.class))),
        @ApiResponse(responseCode = "404", description = "Image not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/images/{id}")
    public ResponseEntity<ImageResponse> getImageById(
            @Parameter(description = "Database record ID", example = "1")
            @PathVariable Long id) {

        return ResponseEntity.ok(imageService.getImageById(id));
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/images/download/{id}  – raw binary
    // ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Download image binary",
        description = "Streams the raw image bytes from SeaweedFS directly through the API. " +
                      "The Content-Type and Content-Disposition headers are set automatically."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Binary image data"),
        @ApiResponse(responseCode = "404", description = "Image not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "SeaweedFS error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/images/download/{id}")
    public ResponseEntity<byte[]> downloadImage(
            @Parameter(description = "Database record ID", example = "1")
            @PathVariable Long id) {

        // Fetch content type before downloading bytes (single DB lookup, no waste)
        String contentType = imageService.getContentType(id);
        byte[] data = imageService.downloadImage(id);

        // Retrieve original filename for Content-Disposition
        String filename = imageService.getImageById(id).getOriginalFilename();
        if (filename == null) filename = "image-" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    // ──────────────────────────────────────────────────────────
    // GET /api/patients/{patientId}/images  – paginated list
    // ──────────────────────────────────────────────────────────

    @Operation(
        summary = "List images by patient",
        description = "Returns a paginated list of all medical images for the given patient, " +
                      "sorted by upload date descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    @GetMapping("/api/patients/{patientId}/images")
    public ResponseEntity<PagedResponse<ImageResponse>> getImagesByPatient(
            @Parameter(description = "Patient identifier", example = "PAT-001")
            @PathVariable String patientId,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(imageService.getImagesByPatient(patientId, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // DELETE /api/images/{id}
    // ──────────────────────────────────────────────────────────

    @Operation(
        summary = "Delete a medical image",
        description = "Removes the image from SeaweedFS and deletes the metadata record from MySQL. " +
                      "Both deletions are atomic: if SeaweedFS deletion fails, the MySQL record is retained."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Image not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "502", description = "SeaweedFS error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/api/images/{id}")
    public ResponseEntity<Void> deleteImage(
            @Parameter(description = "Database record ID", example = "1")
            @PathVariable Long id) {

        imageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}
