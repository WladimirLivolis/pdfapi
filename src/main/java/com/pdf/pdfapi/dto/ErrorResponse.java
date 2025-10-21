package com.pdf.pdfapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String status,
        String message,
        String error,
        List<String> details,
        String path,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .status("error")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String message, String error) {
        return ErrorResponse.builder()
                .status("error")
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(String message, List<String> details) {
        return ErrorResponse.builder()
                .status("error")
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
