package com.pdf.pdfapi.validator;

import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Component
@Log4j2
public class PdfFileValidator {

    private static final List<String> ALLOWED_PDF_MIME_TYPES = Arrays.asList(
            "application/pdf",
            "application/x-pdf"
    );

    private static final List<String> ALLOWED_IMAGE_MIME_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/bmp",
            "image/tiff"
    );

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100MB

    public void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfErrorException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new PdfErrorException(String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    file.getSize(), MAX_FILE_SIZE));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PDF_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Invalid PDF file type: {}", contentType);
            throw new PdfErrorException("Only PDF files are allowed. Received: " + contentType);
        }

        // Additional validation: check file signature (magic bytes)
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 4) {
                throw new PdfErrorException("File is too small to be a valid PDF");
            }

            // PDF files start with %PDF
            if (!(bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46)) {
                throw new PdfErrorException("File does not appear to be a valid PDF (invalid file signature)");
            }
        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new PdfErrorException("Failed to validate PDF file: " + e.getMessage(), e);
        }
    }

    public void validatePdfFiles(MultipartFile... files) {
        if (files == null || files.length == 0) {
            throw new PdfErrorException("At least one file must be provided");
        }

        for (MultipartFile file : files) {
            validatePdfFile(file);
        }
    }

    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfErrorException("File cannot be null or empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new PdfErrorException(String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    file.getSize(), MAX_FILE_SIZE));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Invalid image file type: {}", contentType);
            throw new PdfErrorException("Only image files (PNG, JPEG, GIF, BMP, TIFF) are allowed. Received: " + contentType);
        }
    }

    public void validateImageFiles(MultipartFile... files) {
        if (files == null || files.length == 0) {
            throw new PdfErrorException("At least one image file must be provided");
        }

        for (MultipartFile file : files) {
            validateImageFile(file);
        }
    }
}
