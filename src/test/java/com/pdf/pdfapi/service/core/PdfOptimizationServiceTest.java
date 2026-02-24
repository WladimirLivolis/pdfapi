package com.pdf.pdfapi.service.core;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
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
class PdfOptimizationServiceTest {

    private PdfOptimizationService pdfOptimizationService;

    @BeforeEach
    void setUp() {
        pdfOptimizationService = new PdfOptimizationService(new PdfIoSupport(), new PdfResultFactory());
    }

    @Test
    @SneakyThrows
    void compressGivenLowLevelExpectCompressedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfOptimizationService.compress(originalFile, "LOW");

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("compressed_low"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void compressGivenMediumLevelExpectCompressedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfOptimizationService.compress(originalFile, "MEDIUM");

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("compressed_medium"));
        assertEquals(2, result.pageCount());
    }

    @Test
    @SneakyThrows
    void compressGivenHighLevelExpectCompressedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfOptimizationService.compress(originalFile, "HIGH");

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("compressed_high"));
        assertEquals(2, result.pageCount());
    }

    @Test
    void compressGivenInvalidLevelExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);
        assertThrows(PdfErrorException.class, () -> pdfOptimizationService.compress(originalFile, "INVALID"));
    }

    @Test
    @SneakyThrows
    void optimizeGivenValidPdfExpectOptimizedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfOptimizationService.optimize(originalFile);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("optimized"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }
}
