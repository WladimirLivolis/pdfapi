package com.pdf.pdfapi.dto;

import lombok.Builder;

@Builder
public record PdfResult(
        byte[] content,
        String suggestedFileName,
        long sizeInBytes,
        int pageCount
) {
}
