package com.medicalstorage.service;

import com.medicalstorage.dto.*;
import com.medicalstorage.entity.ImageType;
import com.medicalstorage.entity.MedicalImage;
import com.medicalstorage.exception.ImageNotFoundException;
import com.medicalstorage.exception.InvalidFileException;
import com.medicalstorage.repository.MedicalImageRepository;
import com.medicalstorage.util.SeaweedFSClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalImageServiceTest {

    @Mock private MedicalImageRepository imageRepository;
    @Mock private SeaweedFSClient seaweedFSClient;
    @InjectMocks private MedicalImageService service;

    private MedicalImage sampleImage;

    @BeforeEach
    void setUp() {
        sampleImage = MedicalImage.builder()
                .id(1L).patientId("PAT-001").imageType(ImageType.MRI)
                .fileId("3,abc123").originalFilename("brain.jpg")
                .contentType("image/jpeg").fileSize(1024L)
                .description("Test scan").uploadDate(LocalDateTime.now()).build();
    }

    @Test
    void uploadImage_validFile_returnsResponse() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "brain.jpg", "image/jpeg", new byte[1024]);
        ImageUploadRequest request = ImageUploadRequest.builder()
                .patientId("PAT-001").imageType(ImageType.MRI).description("Test").build();

        when(seaweedFSClient.assign())
                .thenReturn(new SeaweedFSClient.AssignResult("3,abc123", "http://localhost:8080"));
        doNothing().when(seaweedFSClient).upload(anyString(), anyString(), any());
        when(imageRepository.save(any())).thenReturn(sampleImage);
        when(seaweedFSClient.buildPublicUrl("3,abc123")).thenReturn("http://localhost:8080/3,abc123");

        ImageResponse response = service.uploadImage(file, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFileId()).isEqualTo("3,abc123");
        verify(imageRepository).save(any(MedicalImage.class));
    }

    @Test
    void uploadImage_emptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        ImageUploadRequest request = ImageUploadRequest.builder()
                .patientId("PAT-001").imageType(ImageType.CT).build();
        assertThatThrownBy(() -> service.uploadImage(emptyFile, request))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void getImageById_missing_throwsNotFoundException() {
        when(imageRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getImageById(99L))
                .isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    void deleteImage_callsSeaweedFSAndRepository() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(sampleImage));
        doNothing().when(seaweedFSClient).delete("3,abc123");
        service.deleteImage(1L);
        verify(seaweedFSClient).delete("3,abc123");
        verify(imageRepository).delete(sampleImage);
    }
}
