package com.pdf.pdfapi.service.ocr;

import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.service.TesseractFactory;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.SneakyThrows;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfOcrServiceTest {

    @Mock
    private TesseractFactory tesseractFactory;

    private PdfOcrService pdfOcrService;

    @BeforeEach
    void setUp() {
        pdfOcrService = new PdfOcrService(
                tesseractFactory,
                new PdfIoSupport(),
                new PageRangeResolver(),
                new PdfResultFactory()
        );
    }

    @Test
    @SneakyThrows
    void ocrToTextGivenValidPdfExpectTextExtracted() {
        MultipartFile file = mock(MultipartFile.class);
        ITesseract mockTesseract = mock(ITesseract.class);

        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(tesseractFactory.create("eng")).thenReturn(mockTesseract);
        when(mockTesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello World");

        byte[] result = pdfOcrService.ocrToText(file, "eng", null, null);

        assertNotNull(result);
        String text = new String(result, StandardCharsets.UTF_8);
        assertTrue(text.contains("Hello World"));
        assertTrue(text.contains("--- Page 1 ---"));
        verify(tesseractFactory).create("eng");
        verify(mockTesseract).doOCR(any(BufferedImage.class));
    }

    @Test
    @SneakyThrows
    void ocrToPdfGivenValidPdfExpectSearchablePdf() {
        MultipartFile file = mock(MultipartFile.class);
        ITesseract mockTesseract = mock(ITesseract.class);

        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(tesseractFactory.create("eng")).thenReturn(mockTesseract);
        when(mockTesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello World");

        PdfResult result = pdfOcrService.ocrToPdf(file, "eng", null, null, null);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("ocr"));
        assertEquals(1, result.pageCount());
        verify(tesseractFactory).create("eng");
    }

    @Test
    @SneakyThrows
    void ocrToTextGivenNullLanguageExpectsDefaultLanguage() {
        MultipartFile file = mock(MultipartFile.class);
        ITesseract mockTesseract = mock(ITesseract.class);

        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(tesseractFactory.create("eng")).thenReturn(mockTesseract);
        when(mockTesseract.doOCR(any(BufferedImage.class))).thenReturn("text");

        pdfOcrService.ocrToText(file, null, null, null);

        verify(tesseractFactory).create("eng");
    }

    @Test
    @SneakyThrows
    void ocrToTextGivenDpiUsesProvidedValue() {
        MultipartFile file = mock(MultipartFile.class);
        ITesseract mockTesseract = mock(ITesseract.class);

        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(tesseractFactory.create("eng")).thenReturn(mockTesseract);
        when(mockTesseract.doOCR(any(BufferedImage.class))).thenReturn("dpi text");

        byte[] result = pdfOcrService.ocrToText(file, "eng", null, null, 200);

        assertNotNull(result);
        assertTrue(new String(result, StandardCharsets.UTF_8).contains("dpi text"));
        verify(tesseractFactory).create("eng");
    }
}
