package com.pdf.pdfapi.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record JobResponse(
        String jobId,
        String status,
        String message,
        String statusCheckUrl,
        LocalDateTime timestamp
) {
    public static JobResponse accepted(String jobId, String statusCheckUrl) {
        return JobResponse.builder()
                .jobId(jobId)
                .status("ACCEPTED")
                .message("Job accepted for processing")
                .statusCheckUrl(statusCheckUrl)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
