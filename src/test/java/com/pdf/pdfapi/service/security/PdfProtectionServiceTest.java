package com.pdf.pdfapi.service.security;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.pdf.pdfapi.dto.EncryptRequest;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfProtectionServiceTest {

    private PdfProtectionService pdfProtectionService;

    @BeforeEach
    void setUp() {
        pdfProtectionService = new PdfProtectionService(new PdfIoSupport(), new PdfResultFactory());
    }

    @Test
    @SneakyThrows
    void encryptGivenUserPasswordExpectEncryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfProtectionService.encrypt(originalFile,
                new EncryptRequest("user123", null, null, true, false, false, false));

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("encrypted"));
        assertEquals(2, result.pageCount());

        assertThrows(Exception.class, () -> new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content()))));
    }

    @Test
    @SneakyThrows
    void encryptGivenOwnerPasswordExpectEncryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfProtectionService.encrypt(originalFile,
                new EncryptRequest(null, "owner456", null, false, false, false, false));

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("encrypted"));
        assertEquals(2, result.pageCount());
    }

    @Test
    void encryptGivenNoPasswordExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfProtectionService.encrypt(originalFile, new EncryptRequest(null, null, null, null, null, null, null)));
    }

    @Test
    @SneakyThrows
    void decryptGivenCorrectPasswordExpectDecryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult encrypted = pdfProtectionService.encrypt(originalFile,
                new EncryptRequest("password123", "owner456", null, true, true, true, true));

        MultipartFile encryptedFile = mock(MultipartFile.class);
        when(encryptedFile.getBytes()).thenReturn(encrypted.content());

        PdfResult result = pdfProtectionService.decrypt(encryptedFile, "owner456");

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("decrypted"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    void decryptGivenNoPasswordExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfProtectionService.decrypt(originalFile, null));
    }
}
