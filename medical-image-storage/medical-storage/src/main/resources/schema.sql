-- ============================================================
-- Medical Imaging System – MySQL Schema
-- Run this once to create the database and table.
-- JPA will handle DDL via spring.jpa.hibernate.ddl-auto=update,
-- but this file is provided for reference / CI migrations.
-- ============================================================

CREATE DATABASE IF NOT EXISTS medical_imaging
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE medical_imaging;

CREATE TABLE IF NOT EXISTS medical_images (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    patient_id        VARCHAR(100)    NOT NULL COMMENT 'Hospital patient identifier',
    image_type        ENUM('MRI','CT','XRAY') NOT NULL COMMENT 'Imaging modality',
    file_id           VARCHAR(50)     NOT NULL UNIQUE COMMENT 'SeaweedFS FID e.g. 3,01637037d6',
    original_filename VARCHAR(255)    COMMENT 'Filename as uploaded',
    content_type      VARCHAR(100)    COMMENT 'MIME type e.g. image/jpeg',
    file_size         BIGINT          COMMENT 'Size in bytes',
    description       TEXT            COMMENT 'Clinical notes',
    upload_date       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_patient_id  (patient_id),
    INDEX idx_image_type  (image_type),
    INDEX idx_upload_date (upload_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
