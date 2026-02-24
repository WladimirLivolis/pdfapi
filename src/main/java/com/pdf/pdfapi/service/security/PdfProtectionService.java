package com.pdf.pdfapi.service.security;

import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.pdf.pdfapi.dto.EncryptRequest;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfProtectionService {

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;

    public PdfResult encrypt(MultipartFile file, EncryptRequest request) {
        validatePasswordsProvided(request.userPassword(), request.ownerPassword());

        EncryptionConfig config = buildEncryptionConfig(request);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WriterProperties writerProperties = createEncryptedWriter(config);

            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file), new PdfWriter(outputStream, writerProperties));

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully encrypted PDF with {} encryption ({} pages)",
                    getEncryptionTypeName(config.encryptionType()), pageCount);
            return pdfResultFactory.buildPdfResult(pdfBytes, "encrypted", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to encrypt PDF", e);
            throw new PdfErrorException("Failed to encrypt PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult decrypt(MultipartFile file, String password) {
        if (password == null || password.isBlank()) {
            throw new PdfErrorException("Password is required to decrypt PDF");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ReaderProperties readerProperties = new ReaderProperties();
            readerProperties.setPassword(password.getBytes());

            PdfReader reader = new PdfReader(new ByteArrayInputStream(file.getBytes()), readerProperties);
            PdfDocument pdfDocument = new PdfDocument(reader, new PdfWriter(outputStream));

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully decrypted PDF ({} pages)", pageCount);
            return pdfResultFactory.buildPdfResult(pdfBytes, "decrypted", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decrypt PDF", e);
            throw new PdfErrorException("Failed to decrypt PDF. Check if password is correct: " + e.getMessage(), e);
        }
    }

    private void validatePasswordsProvided(String userPassword, String ownerPassword) {
        if ((userPassword == null || userPassword.isBlank()) &&
                (ownerPassword == null || ownerPassword.isBlank())) {
            throw new PdfErrorException("At least one password (user or owner) must be provided");
        }
    }

    private EncryptionConfig buildEncryptionConfig(EncryptRequest request) {
        String userPwd = (request.userPassword() != null && !request.userPassword().isBlank()) ? request.userPassword() : "";
        String ownerPwd = (request.ownerPassword() != null && !request.ownerPassword().isBlank()) ? request.ownerPassword() : "";
        int encType = (request.encryptionType() != null) ? request.encryptionType() : EncryptionConstants.ENCRYPTION_AES_256;

        validateEncryptionType(encType);

        int permissions = calculatePermissions(request.allowPrinting(), request.allowModifying(), request.allowCopy(), request.allowAnnotations());

        return new EncryptionConfig(userPwd, ownerPwd, encType, permissions);
    }

    private void validateEncryptionType(int encryptionType) {
        if (encryptionType != EncryptionConstants.STANDARD_ENCRYPTION_40 &&
                encryptionType != EncryptionConstants.STANDARD_ENCRYPTION_128 &&
                encryptionType != EncryptionConstants.ENCRYPTION_AES_128 &&
                encryptionType != EncryptionConstants.ENCRYPTION_AES_256) {
            throw new PdfErrorException("Invalid encryption type. Use 40, 128, 256, or AES256");
        }
    }

    private int calculatePermissions(Boolean allowPrinting, Boolean allowModifying,
                                     Boolean allowCopy, Boolean allowAnnotations) {
        int permissions = 0;
        if (Boolean.TRUE.equals(allowPrinting)) permissions |= EncryptionConstants.ALLOW_PRINTING;
        if (Boolean.TRUE.equals(allowModifying)) permissions |= EncryptionConstants.ALLOW_MODIFY_CONTENTS;
        if (Boolean.TRUE.equals(allowCopy)) permissions |= EncryptionConstants.ALLOW_COPY;
        if (Boolean.TRUE.equals(allowAnnotations)) permissions |= EncryptionConstants.ALLOW_MODIFY_ANNOTATIONS;
        return permissions;
    }

    private WriterProperties createEncryptedWriter(EncryptionConfig config) {
        WriterProperties writerProperties = new WriterProperties();
        writerProperties.setStandardEncryption(
                config.userPassword().getBytes(),
                config.ownerPassword().getBytes(),
                config.permissions(),
                config.encryptionType()
        );
        return writerProperties;
    }

    private String getEncryptionTypeName(int encryptionType) {
        return switch (encryptionType) {
            case EncryptionConstants.STANDARD_ENCRYPTION_40 -> "40-bit";
            case EncryptionConstants.STANDARD_ENCRYPTION_128 -> "128-bit";
            case EncryptionConstants.ENCRYPTION_AES_128 -> "AES-128";
            case EncryptionConstants.ENCRYPTION_AES_256 -> "AES-256";
            default -> "Unknown";
        };
    }

    private record EncryptionConfig(String userPassword, String ownerPassword,
                                    int encryptionType, int permissions) {
    }
}
