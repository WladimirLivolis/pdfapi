package com.pdf.pdfapi.dto;

import lombok.Builder;

@Builder
public record PdfMetadataRequest(
        String title,
        String author,
        String subject,
        String keywords,
        String creator
) {}
