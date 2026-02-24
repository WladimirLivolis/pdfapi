package com.pdf.pdfapi.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.pdf.pdfapi.dto.*;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.core.PdfDocumentOpsService;
import com.pdf.pdfapi.service.core.PdfOptimizationService;
import com.pdf.pdfapi.service.form.PdfFormService;
import com.pdf.pdfapi.service.image.PdfImageService;
import com.pdf.pdfapi.service.metadata.PdfMetadataService;
import com.pdf.pdfapi.service.ocr.PdfOcrService;
import com.pdf.pdfapi.service.security.PdfProtectionService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private PdfOcrService pdfOcrService;

    @Mock
    private PdfImageService pdfImageService;

    @Mock
    private PdfFormService pdfFormService;

    @Mock
    private PdfMetadataService pdfMetadataService;

    @Mock
    private PdfProtectionService pdfProtectionService;

    @Mock
    private PdfOptimizationService pdfOptimizationService;

    @Mock
    private PdfDocumentOpsService pdfDocumentOpsService;

    @InjectMocks
    private PdfService pdfService;

    @Test
    void mergeDelegatesToPdfDocumentOpsService() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = new MultipartFile[]{file};
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("merged.pdf").sizeInBytes(1).pageCount(1).build();

        when(pdfDocumentOpsService.merge(files)).thenReturn(expected);

        PdfResult result = pdfService.merge(files);

        assertEquals(expected, result);
        verify(pdfDocumentOpsService).merge(files);
    }

    @Test
    void mergeWithBookmarksDelegatesToPdfDocumentOpsService() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("merged.pdf").sizeInBytes(1).pageCount(2).build();

        when(pdfDocumentOpsService.merge(true, file1, file2)).thenReturn(expected);

        PdfResult result = pdfService.merge(true, file1, file2);

        assertEquals(expected, result);
        verify(pdfDocumentOpsService).merge(true, file1, file2);
    }

    @Test
    void splitDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        List<PdfResult> expected = List.of(PdfResult.builder().content(new byte[]{1}).suggestedFileName("s1.pdf").sizeInBytes(1).pageCount(1).build());
        when(pdfDocumentOpsService.split(originalFile, 1)).thenReturn(expected);
        assertEquals(expected, pdfService.split(originalFile, 1));
        verify(pdfDocumentOpsService).split(originalFile, 1);
    }

    @Test
    void extractDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("e.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfDocumentOpsService.extract(originalFile, 2, 2)).thenReturn(expected);
        assertEquals(expected, pdfService.extract(originalFile, 2, 2));
        verify(pdfDocumentOpsService).extract(originalFile, 2, 2);
    }

    @Test
    void removeDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("r.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfDocumentOpsService.remove(originalFile, 2)).thenReturn(expected);
        assertEquals(expected, pdfService.remove(originalFile, 2));
        verify(pdfDocumentOpsService).remove(originalFile, 2);
    }

    @Test
    void removeWithDuplicatePagesDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("r.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfDocumentOpsService.remove(originalFile, 2, 2)).thenReturn(expected);
        assertEquals(expected, pdfService.remove(originalFile, 2, 2));
        verify(pdfDocumentOpsService).remove(originalFile, 2, 2);
    }

    @Test
    @SneakyThrows
    void convertImageToPDFDelegatesToPdfImageService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        List<PdfResult> expected = List.of(PdfResult.builder()
                .content(new byte[]{1})
                .suggestedFileName("image.pdf")
                .sizeInBytes(1)
                .pageCount(1)
                .build());

        when(pdfImageService.convertImageToPDF(originalFile)).thenReturn(expected);

        List<PdfResult> results = pdfService.convertImageToPDF(originalFile);

        assertEquals(expected, results);
        verify(pdfImageService).convertImageToPDF(originalFile);
    }

    @SneakyThrows
    private String pdfToText(String fileName) {
        StringBuilder text = new StringBuilder();

        try (PdfDocument document = new PdfDocument(new PdfReader(fileName))) {
            int pages = document.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(i)));
            }
        }

        return text.toString();
    }

    @SneakyThrows
    private String pdfToText(byte[] pdfBytes) {
        StringBuilder text = new StringBuilder();

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            int pages = document.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                text.append(PdfTextExtractor.getTextFromPage(document.getPage(i)));
            }
        }

        return text.toString();
    }

    @Test
    void rotateDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("rot.pdf").sizeInBytes(1).pageCount(2).build();
        when(pdfDocumentOpsService.rotate(originalFile, 90)).thenReturn(expected);
        assertEquals(expected, pdfService.rotate(originalFile, 90));
        verify(pdfDocumentOpsService).rotate(originalFile, 90);
    }

    @Test
    void rotateSpecificPagesDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("rot.pdf").sizeInBytes(1).pageCount(2).build();
        when(pdfDocumentOpsService.rotate(originalFile, 180, 1)).thenReturn(expected);
        assertEquals(expected, pdfService.rotate(originalFile, 180, 1));
        verify(pdfDocumentOpsService).rotate(originalFile, 180, 1);
    }

    @Test
    void rotateInvalidDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(pdfDocumentOpsService.rotate(originalFile, 45)).thenThrow(new PdfErrorException("Rotation must be a multiple of 90 degrees"));
        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, 45));
        verify(pdfDocumentOpsService).rotate(originalFile, 45);
    }

    @Test
    void rotateNullDelegatesToPdfDocumentOpsService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(pdfDocumentOpsService.rotate(originalFile, null)).thenThrow(new PdfErrorException("Rotation angle is required"));
        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, null));
        verify(pdfDocumentOpsService).rotate(originalFile, null);
    }

    @Test
    void getInfoDelegatesToPdfMetadataService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfInfoResponse expected = PdfInfoResponse.success(
                1, 100L, "PDF-1.7",
                PdfInfoResponse.PageDimensions.builder().width(100f).height(100f).unit("points").build(),
                true
        );

        when(pdfMetadataService.getInfo(originalFile)).thenReturn(expected);

        PdfInfoResponse response = pdfService.getInfo(originalFile);

        assertEquals(expected, response);
        verify(pdfMetadataService).getInfo(originalFile);
    }

    @Test
    void getMetadataDelegatesToPdfMetadataService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfMetadataResponse expected = PdfMetadataResponse.success(
                "t", "a", "s", "k", "c", "p", "cd", "md"
        );

        when(pdfMetadataService.getMetadata(originalFile)).thenReturn(expected);

        PdfMetadataResponse response = pdfService.getMetadata(originalFile);

        assertEquals(expected, response);
        verify(pdfMetadataService).getMetadata(originalFile);
    }

    @Test
    void updateMetadataDelegatesToPdfMetadataService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title("Test Title")
                .author("Test Author")
                .subject("Test Subject")
                .keywords("Test Keywords")
                .creator("Test Creator")
                .build();
        PdfResult expected = PdfResult.builder()
                .content(new byte[]{1})
                .suggestedFileName("metadata_updated_x.pdf")
                .sizeInBytes(1)
                .pageCount(1)
                .build();

        when(pdfMetadataService.updateMetadata(originalFile, metadata)).thenReturn(expected);

        PdfResult result = pdfService.updateMetadata(originalFile, metadata);

        assertEquals(expected, result);
        verify(pdfMetadataService).updateMetadata(originalFile, metadata);
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenValidParametersExpectNumberedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.addPageNumbers(originalFile, "bottom-center", "Page {current} of {total}", null, null);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("numbered"));
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenPageRangeExpectOnlyRangeNumbered() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        // Only number page 1
        PdfResult result = pdfService.addPageNumbers(originalFile, "top-right", "{page}", 1, 1);

        // Verify result
        assertNotNull(result);
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void addPageNumbersGivenDefaultParametersExpectDefaultFormatting() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        // Use null parameters to test defaults
        PdfResult result = pdfService.addPageNumbers(originalFile, null, null, null, null);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void watermarkGivenTextExpectWatermarkedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.watermark(originalFile, null, WatermarkRequest.builder()
                .text("CONFIDENTIAL").position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground")
                .build());

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("watermarked"));
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
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

        PdfResult result = pdfService.watermark(originalFile, imageFile, WatermarkRequest.builder()
                .position("top-right").opacity(0.5f).rotation(0.0f).scale(0.5f).layer("background")
                .build());

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("watermarked"));
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void watermarkGivenNoTextAndNoImageExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfService.watermark(originalFile, null, WatermarkRequest.builder()
                        .position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground").build()));
    }

    @Test
    @SneakyThrows
    void watermarkGivenBothTextAndImageExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);
        MultipartFile imageFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfService.watermark(originalFile, imageFile, WatermarkRequest.builder()
                        .text("TEST").position("center").opacity(0.3f).rotation(45.0f).scale(1.0f).layer("foreground").build()));
    }

    @Test
    void compressDelegatesToPdfOptimizationService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("c.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfOptimizationService.compress(originalFile, "LOW")).thenReturn(expected);
        assertEquals(expected, pdfService.compress(originalFile, "LOW"));
        verify(pdfOptimizationService).compress(originalFile, "LOW");
    }

    @Test
    void optimizeDelegatesToPdfOptimizationService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("o.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfOptimizationService.optimize(originalFile)).thenReturn(expected);
        assertEquals(expected, pdfService.optimize(originalFile));
        verify(pdfOptimizationService).optimize(originalFile);
    }

    @Test
    void encryptDelegatesToPdfProtectionService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        EncryptRequest request = new EncryptRequest("user", null, null, true, false, false, false);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("e.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfProtectionService.encrypt(originalFile, request)).thenReturn(expected);
        assertEquals(expected, pdfService.encrypt(originalFile, request));
        verify(pdfProtectionService).encrypt(originalFile, request);
    }

    @Test
    void decryptDelegatesToPdfProtectionService() {
        MultipartFile originalFile = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("d.pdf").sizeInBytes(1).pageCount(1).build();
        when(pdfProtectionService.decrypt(originalFile, "secret")).thenReturn(expected);
        assertEquals(expected, pdfService.decrypt(originalFile, "secret"));
        verify(pdfProtectionService).decrypt(originalFile, "secret");
    }

    @Test
    @SneakyThrows
    void ocrToTextDelegatesToPdfOcrService() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] response = "Hello World".getBytes();

        when(pdfOcrService.ocrToText(file, "eng", null, null)).thenReturn(response);

        byte[] result = pdfService.ocrToText(file, "eng", null, null);

        assertArrayEquals(response, result);
        verify(pdfOcrService).ocrToText(file, "eng", null, null);
    }

    @Test
    @SneakyThrows
    void ocrToPdfDelegatesToPdfOcrService() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder()
                .content(new byte[]{1, 2})
                .suggestedFileName("ocr_test.pdf")
                .sizeInBytes(2)
                .pageCount(1)
                .build();

        when(pdfOcrService.ocrToPdf(file, "eng", null, null, null)).thenReturn(expected);

        PdfResult result = pdfService.ocrToPdf(file, "eng", null, null, null);

        assertEquals(expected, result);
        verify(pdfOcrService).ocrToPdf(file, "eng", null, null, null);
    }

    @Test
    @SneakyThrows
    void ocrToTextWithDpiDelegatesToPdfOcrService() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] response = "text".getBytes();

        when(pdfOcrService.ocrToText(file, null, 1, 2, 200)).thenReturn(response);

        byte[] result = pdfService.ocrToText(file, null, 1, 2, 200);

        assertArrayEquals(response, result);
        verify(pdfOcrService).ocrToText(file, null, 1, 2, 200);
    }

    @Test
    void toImagesDelegatesToPdfImageService() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] expected = new byte[]{1, 2, 3};

        when(pdfImageService.toImages(file, "png", 150, 1, 2)).thenReturn(expected);

        byte[] result = pdfService.toImages(file, "png", 150, 1, 2);

        assertArrayEquals(expected, result);
        verify(pdfImageService).toImages(file, "png", 150, 1, 2);
    }

    @Test
    void extractImagesDelegatesToPdfImageService() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] expected = new byte[]{9};

        when(pdfImageService.extractImages(file, 100, 200)).thenReturn(expected);

        byte[] result = pdfService.extractImages(file, 100, 200);

        assertArrayEquals(expected, result);
        verify(pdfImageService).extractImages(file, 100, 200);
    }

    @Test
    void cropDelegatesToPdfDocumentOpsService() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult expected = PdfResult.builder().content(new byte[]{1}).suggestedFileName("crop.pdf").sizeInBytes(1).pageCount(1).build();

        when(pdfDocumentOpsService.crop(file, 0f, 0f, 100f, 100f, null, null)).thenReturn(expected);

        PdfResult result = pdfService.crop(file, 0f, 0f, 100f, 100f, null, null);

        assertEquals(expected, result);
        verify(pdfDocumentOpsService).crop(file, 0f, 0f, 100f, 100f, null, null);
    }

    @Test
    void fillFormDelegatesToPdfFormService() {
        MultipartFile file = mock(MultipartFile.class);
        Map<String, String> fields = Map.of("name", "John");
        PdfResult expected = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("filled_test.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfFormService.fillForm(file, fields, true)).thenReturn(expected);

        PdfResult result = pdfService.fillForm(file, fields, true);

        assertEquals(expected, result);
        verify(pdfFormService).fillForm(file, fields, true);
    }
}
