package com.pdf.pdfapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PdfOperationResponse(
        String status,
        String message,
        String fileName,
        Long fileSizeBytes,
        Integer pageCount,
        LocalDateTime timestamp
) {
    public static PdfOperationResponse success(String message, String fileName) {
        return PdfOperationResponse.builder()
                .status("success")
                .message(message)
                .fileName(fileName)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static PdfOperationResponse success(String message, String fileName, Long fileSizeBytes, Integer pageCount) {
        return PdfOperationResponse.builder()
                .status("success")
                .message(message)
                .fileName(fileName)
                .fileSizeBytes(fileSizeBytes)
                .pageCount(pageCount)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
