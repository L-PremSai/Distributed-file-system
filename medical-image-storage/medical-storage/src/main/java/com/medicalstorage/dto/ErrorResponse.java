package com.medicalstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Standardised error payload returned by the global exception handler.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error response body")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Short error label", example = "NOT_FOUND")
    private String error;

    @Schema(description = "Human-readable message", example = "Image not found with id: 99")
    private String message;

    @Schema(description = "Request path", example = "/api/images/99")
    private String path;

    @Schema(description = "Timestamp of the error")
    private LocalDateTime timestamp;
}
