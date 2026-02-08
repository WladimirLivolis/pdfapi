package com.pdf.pdfapi.dto;

/**
 * Request parameters for PDF encryption operations.
 */
public record EncryptRequest(
        String userPassword,
        String ownerPassword,
        Integer encryptionType,
        Boolean allowPrinting,
        Boolean allowModifying,
        Boolean allowCopy,
        Boolean allowAnnotations
) {
}
