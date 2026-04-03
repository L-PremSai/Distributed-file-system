package com.medicalstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * Generic wrapper for paginated list responses.
 *
 * @param <T> element type
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Paginated response")
public class PagedResponse<T> {

    @Schema(description = "Current page (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total elements", example = "55")
    private long totalElements;

    @Schema(description = "Total pages", example = "3")
    private int totalPages;

    @Schema(description = "Data items on this page")
    private List<T> content;
}
