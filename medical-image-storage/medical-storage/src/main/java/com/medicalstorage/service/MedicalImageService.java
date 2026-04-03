package com.medicalstorage.service;

import com.medicalstorage.dto.*;
import com.medicalstorage.entity.MedicalImage;
import com.medicalstorage.exception.ImageNotFoundException;
import com.medicalstorage.exception.InvalidFileException;
import com.medicalstorage.repository.MedicalImageRepository;
import com.medicalstorage.util.SeaweedFSClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

/**
 * Core business logic for the medical image storage system.
 *
 * This service orchestrates:
 *   - Validation of incoming files
 *   - SeaweedFS assign + upload
 *   - MySQL metadata persistence
 *   - Retrieval and deletion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalImageService {

    /** Accepted MIME types for medical images */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/tiff",
            "application/dicom",          // DICOM files
            "application/octet-stream"    // Generic binary (for .dcm without proper MIME)
    );

    private final MedicalImageRepository imageRepository;
    private final SeaweedFSClient seaweedFSClient;

    // ─────────────────────────────────────────────────────────
    // UPLOAD
    // ─────────────────────────────────────────────────────────

    /**
     * Uploads a medical image to SeaweedFS and saves metadata in MySQL.
     *
     * Steps:
     *   1. Validate the multipart file
     *   2. Ask SeaweedFS master for a new FID (assign)
     *   3. POST the file bytes to the assigned volume node
     *   4. Persist metadata to MySQL
     *   5. Return the response DTO with the public image URL
     *
     * @param file    the raw image bytes from the HTTP request
     * @param request accompanying metadata (patientId, imageType, description)
     * @return populated ImageResponse including the SeaweedFS URL
     */
    @Transactional
    public ImageResponse uploadImage(MultipartFile file, ImageUploadRequest request) {
        log.info("Starting upload for patient={}, type={}", request.getPatientId(), request.getImageType());

        // 1. Validate
        validateFile(file);

        // 2. Assign a new FID from SeaweedFS master
        SeaweedFSClient.AssignResult assign = seaweedFSClient.assign();
        log.debug("Got FID={} on volume={}", assign.fid(), assign.volumeUrl());

        // 3. Upload file bytes to the volume node
        seaweedFSClient.upload(assign.volumeUrl(), assign.fid(), file);

        // 4. Persist metadata
        MedicalImage image = MedicalImage.builder()
                .patientId(request.getPatientId())
                .imageType(request.getImageType())
                .fileId(assign.fid())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .description(request.getDescription())
                .build();

        MedicalImage saved = imageRepository.save(image);
        log.info("Image saved to DB with id={}, fid={}", saved.getId(), saved.getFileId());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────
    // GET METADATA
    // ─────────────────────────────────────────────────────────

    /**
     * Returns the metadata record (including the SeaweedFS URL) for a single image.
     */
    @Transactional(readOnly = true)
    public ImageResponse getImageById(Long id) {
        MedicalImage image = findOrThrow(id);
        return toResponse(image);
    }

    // ─────────────────────────────────────────────────────────
    // DOWNLOAD BYTES
    // ─────────────────────────────────────────────────────────

    /**
     * Downloads the raw file bytes from SeaweedFS for streaming back to the client.
     *
     * @param id  database record ID
     * @return byte array of the image file
     */
    @Transactional(readOnly = true)
    public byte[] downloadImage(Long id) {
        MedicalImage image = findOrThrow(id);
        log.info("Downloading image id={}, fid={}", id, image.getFileId());
        return seaweedFSClient.download(image.getFileId());
    }

    /**
     * Helper: returns the content type of a stored image (needed for the response header).
     */
    @Transactional(readOnly = true)
    public String getContentType(Long id) {
        MedicalImage image = findOrThrow(id);
        return image.getContentType() != null ? image.getContentType() : "application/octet-stream";
    }

    // ─────────────────────────────────────────────────────────
    // LIST BY PATIENT
    // ─────────────────────────────────────────────────────────

    /**
     * Paginates all images for a given patient, sorted newest-first.
     *
     * @param patientId patient identifier
     * @param page      zero-indexed page number
     * @param size      page size (max 100)
     */
    @Transactional(readOnly = true)
    public PagedResponse<ImageResponse> getImagesByPatient(String patientId, int page, int size) {
        // Clamp size to prevent abuse
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadDate"));
        Page<MedicalImage> resultPage = imageRepository.findByPatientId(patientId, pageable);

        List<ImageResponse> content = resultPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PagedResponse.<ImageResponse>builder()
                .page(page)
                .size(size)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .content(content)
                .build();
    }

    // ─────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────

    /**
     * Deletes the image from both SeaweedFS and MySQL.
     * If SeaweedFS deletion fails, the MySQL record is NOT removed
     * (transaction rolls back) to prevent orphaned metadata.
     *
     * @param id  database record ID
     */
    @Transactional
    public void deleteImage(Long id) {
        MedicalImage image = findOrThrow(id);

        // Remove from SeaweedFS first; if this throws, the TX rolls back
        seaweedFSClient.delete(image.getFileId());

        // Only remove from MySQL after SeaweedFS confirms deletion
        imageRepository.delete(image);
        log.info("Image id={} deleted from SeaweedFS and MySQL", id);
    }

    // ─────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────

    private MedicalImage findOrThrow(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ImageNotFoundException(id));
    }

    /**
     * Validates that the file is non-empty and has an acceptable MIME type.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file must not be empty");
        }

        if (file.getSize() > 100L * 1024 * 1024) { // 100 MB guard (belt-and-suspenders)
            throw new InvalidFileException("File size exceeds maximum allowed 100 MB");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    "Unsupported file type: " + contentType +
                    ". Allowed: JPEG, PNG, TIFF, DICOM");
        }
    }

    /**
     * Maps a {@link MedicalImage} entity to the API response DTO.
     * Builds the public SeaweedFS URL from the stored FID.
     */
    private ImageResponse toResponse(MedicalImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .patientId(image.getPatientId())
                .imageType(image.getImageType())
                .fileId(image.getFileId())
                .originalFilename(image.getOriginalFilename())
                .contentType(image.getContentType())
                .fileSize(image.getFileSize())
                .description(image.getDescription())
                .uploadDate(image.getUploadDate())
                .imageUrl(seaweedFSClient.buildPublicUrl(image.getFileId()))
                .build();
    }
}
