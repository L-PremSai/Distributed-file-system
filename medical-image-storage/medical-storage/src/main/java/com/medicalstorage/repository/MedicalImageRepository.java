package com.medicalstorage.repository;

import com.medicalstorage.entity.ImageType;
import com.medicalstorage.entity.MedicalImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for MedicalImage.
 * All CRUD and pagination is handled automatically by Spring Data.
 */
@Repository
public interface MedicalImageRepository extends JpaRepository<MedicalImage, Long> {

    /**
     * Returns a paginated list of images belonging to a specific patient.
     * Used by GET /api/patients/{patientId}/images
     */
    Page<MedicalImage> findByPatientId(String patientId, Pageable pageable);

    /**
     * Filter by patient AND modality (useful for advanced search).
     */
    Page<MedicalImage> findByPatientIdAndImageType(
            String patientId, ImageType imageType, Pageable pageable);

    /**
     * Lookup by the SeaweedFS file ID (must be unique per record).
     */
    Optional<MedicalImage> findByFileId(String fileId);

    /**
     * Check whether a given patient has any stored images.
     */
    boolean existsByPatientId(String patientId);
}
