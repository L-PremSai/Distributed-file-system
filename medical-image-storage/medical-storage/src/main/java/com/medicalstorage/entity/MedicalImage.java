package com.medicalstorage.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a medical image record in MySQL.
 * The actual binary image is stored in SeaweedFS; this table
 * holds only metadata plus the SeaweedFS file ID (fid).
 */
@Entity
@Table(name = "medical_images", indexes = {
    @Index(name = "idx_patient_id", columnList = "patient_id"),
    @Index(name = "idx_image_type", columnList = "image_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Hospital / clinic patient identifier */
    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    /** Scan modality: MRI, CT, XRAY */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    private ImageType imageType;

    /**
     * SeaweedFS File ID returned by /dir/assign.
     * Format: <volumeId>,<fileKey><cookie>  e.g. "3,01637037d6"
     * This is the primary pointer used to retrieve the raw file.
     */
    @Column(name = "file_id", nullable = false, unique = true, length = 50)
    private String fileId;

    /** Original filename as uploaded by the client */
    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    /** MIME type of the uploaded file (e.g. image/jpeg) */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** File size in bytes */
    @Column(name = "file_size")
    private Long fileSize;

    /** Free-text notes entered by the clinician */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Set automatically at INSERT time by Hibernate */
    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;
}
