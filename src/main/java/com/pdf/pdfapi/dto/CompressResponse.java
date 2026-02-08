package com.pdf.pdfapi.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for PDF compression operations
 */
@Builder
public record CompressResponse(
        String status,
        String message,
        String fileName,
        long originalSizeBytes,
        long compressedSizeBytes,
        double reductionPercentage,
        int pageCount,
        LocalDateTime timestamp
) {
    public static CompressResponse success(String fileName, long originalSize, long compressedSize, int pageCount) {
        double reduction = ((originalSize - compressedSize) / (double) originalSize) * 100;
        return CompressResponse.builder()
                .status("success")
                .message("PDF compressed successfully")
                .fileName(fileName)
                .originalSizeBytes(originalSize)
                .compressedSizeBytes(compressedSize)
                .reductionPercentage(Math.round(reduction * 100.0) / 100.0) // 2 decimal places
                .pageCount(pageCount)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
