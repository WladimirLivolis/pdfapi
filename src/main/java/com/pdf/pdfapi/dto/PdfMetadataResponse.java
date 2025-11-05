package com.pdf.pdfapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PdfMetadataResponse(
        String status,
        String message,
        String title,
        String author,
        String subject,
        String keywords,
        String creator,
        String producer,
        String creationDate,
        String modificationDate,
        LocalDateTime timestamp
) {
    public static PdfMetadataResponse success(String title, String author, String subject,
                                               String keywords, String creator, String producer,
                                               String creationDate, String modificationDate) {
        return PdfMetadataResponse.builder()
                .status("success")
                .message("PDF metadata retrieved successfully")
                .title(title)
                .author(author)
                .subject(subject)
                .keywords(keywords)
                .creator(creator)
                .producer(producer)
                .creationDate(creationDate)
                .modificationDate(modificationDate)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
