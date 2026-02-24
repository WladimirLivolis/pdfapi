package com.pdf.pdfapi.service.annotate;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.dto.WatermarkRequest;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PageRangeResolver;
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
class PdfAnnotationServiceTest {

    private PdfAnnotationService pdfAnnotationService;

    @BeforeEach
    void setUp() {
        pdfAnnotationService = new PdfAnnotationService(
                new PdfIoSupport(),
                new PdfResultFactory(),
                new PageRangeResolver()
        );
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenValidParametersExpectNumberedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfAnnotationService.addPageNumbers(originalFile, "bottom-center", "Page {current} of {total}", null, null);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("numbered"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenPageRangeExpectOnlyRangeNumbered() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfAnnotationService.addPageNumbers(originalFile, "top-right", "{page}", 1, 1);

        assertNotNull(result);
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenDefaultParametersExpectDefaultFormatting() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfAnnotationService.addPageNumbers(originalFile, null, null, null, null);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void watermarkGivenTextExpectWatermarkedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfAnnotationService.watermark(originalFile, null, WatermarkRequest.builder()
                .text("CONFIDENTIAL").position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground")
                .build());

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("watermarked"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void watermarkGivenImageExpectWatermarkedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        MultipartFile imageFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));
        when(imageFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/image/image.png")));

        PdfResult result = pdfAnnotationService.watermark(originalFile, imageFile, WatermarkRequest.builder()
                .position("top-right").opacity(0.5f).rotation(0.0f).scale(0.5f).layer("background")
                .build());

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("watermarked"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    void watermarkGivenNoTextAndNoImageExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfAnnotationService.watermark(originalFile, null, WatermarkRequest.builder()
                        .position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground").build()));
    }

    @Test
    void watermarkGivenBothTextAndImageExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);
        MultipartFile imageFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfAnnotationService.watermark(originalFile, imageFile, WatermarkRequest.builder()
                        .text("TEST").position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground").build()));
    }

    @Test
    @SneakyThrows
    void watermarkGivenInvalidPositionAndLayerUsesFallbackSemantics() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfAnnotationService.watermark(originalFile, null, WatermarkRequest.builder()
                .text("TEST")
                .position("not-a-position")
                .opacity(0.3f)
                .rotation(0.0f)
                .scale(1.0f)
                .layer("not-a-layer")
                .build());

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertEquals(2, result.pageCount());
    }
}
