package com.pdf.pdfapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PdfInfoResponse(
        String status,
        String message,
        Integer pageCount,
        Long fileSizeBytes,
        String pdfVersion,
        PageDimensions firstPageDimensions,
        Boolean allPagesSameDimension,
        LocalDateTime timestamp
) {
    public static PdfInfoResponse success(Integer pageCount, Long fileSizeBytes, String pdfVersion,
                                          PageDimensions firstPageDimensions, Boolean allPagesSameDimension) {
        return PdfInfoResponse.builder()
                .status("success")
                .message("PDF info retrieved successfully")
                .pageCount(pageCount)
                .fileSizeBytes(fileSizeBytes)
                .pdfVersion(pdfVersion)
                .firstPageDimensions(firstPageDimensions)
                .allPagesSameDimension(allPagesSameDimension)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Builder
    public record PageDimensions(
            Float width,
            Float height,
            String unit
    ) {}
}
