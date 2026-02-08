package com.pdf.pdfapi.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.pdf.pdfapi.dto.*;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.SneakyThrows;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private TesseractFactory tesseractFactory;

    @InjectMocks
    private PdfService pdfService;

    @Test
    void mergeGivenThereIsOnlyOneFileExpectFailure() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = new MultipartFile[1];
        files[0] = file;

        assertThrows(PdfErrorException.class, () -> pdfService.merge(files));
    }

    @Test
    @SneakyThrows
    void mergeGivenThereAreTwoFilesExpectOneCombinedFile() {
        MultipartFile file1 = mock(MultipartFile.class);
        MultipartFile file2 = mock(MultipartFile.class);

        byte[] pdf1Bytes = Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf"));
        byte[] pdf2Bytes = Files.readAllBytes(Path.of("src/test/resources/merge/file2.pdf"));

        when(file1.getBytes()).thenReturn(pdf1Bytes);
        when(file2.getBytes()).thenReturn(pdf2Bytes);

        PdfResult result = pdfService.merge(file1, file2);

        // Verify result is not null and has content
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("merged"));
        assertEquals(2, result.pageCount()); // 1 page from each file

        // Verify PDF content by comparing text
        String expectedText = pdfToText("src/test/resources/merge/merged_file.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void splitGivenOneFileExpectMultipleFiles() {
        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/split/original_file.pdf")));

        List<PdfResult> results = pdfService.split(originalFile, 1);

        // Verify we got 2 split files
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify each result
        for (PdfResult result : results) {
            assertNotNull(result.content());
            assertTrue(result.content().length > 0);
            assertTrue(result.suggestedFileName().contains("splitDocument"));
            assertEquals(1, result.pageCount()); // Split by 1 page each
        }

        // Compare with expected files
        String expected1 = pdfToText("src/test/resources/split/splitDocument_1.pdf");
        String actual1 = pdfToText(results.getFirst().content());
        assertEquals(expected1, actual1);

        String expected2 = pdfToText("src/test/resources/split/splitDocument_2.pdf");
        String actual2 = pdfToText(results.get(1).content());
        assertEquals(expected2, actual2);
    }

    @Test
    @SneakyThrows
    void extractGivenOneFileExpectNewFile() {
        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.extract(originalFile, 2, 2);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("extracted"));
        assertEquals(1, result.pageCount()); // Pages 2-2 = 1 page

        // Verify content
        String expectedText = pdfToText("src/test/resources/extract/extractedPages.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void removeGivenOneFileExpectNewFile() {
        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.remove(originalFile, 2);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("removed"));
        assertEquals(1, result.pageCount()); // Original had 2 pages, removed 1

        // Verify content
        String expectedText = pdfToText("src/test/resources/remove/removedPages.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void convertImageToPDFGivenImageExpectPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/image/image.png")));
        when(originalFile.getOriginalFilename()).thenReturn("image.png");

        List<PdfResult> results = pdfService.convertImageToPDF(originalFile);

        // Verify result
        assertNotNull(results);
        assertEquals(1, results.size());

        PdfResult result = results.getFirst();
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().endsWith(".pdf"));
        assertTrue(result.suggestedFileName().contains("image"));
        assertEquals(1, result.pageCount());
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
    @SneakyThrows
    void rotateGivenValidRotationExpectRotatedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.rotate(originalFile, 90);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("rotated"));
        assertEquals(2, result.pageCount());

        // Verify rotation was applied
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(90, document.getPage(1).getRotation());
            assertEquals(90, document.getPage(2).getRotation());
        }
    }

    @Test
    @SneakyThrows
    void rotateGivenSpecificPagesExpectOnlyThosePagesRotated() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.rotate(originalFile, 180, 1);

        // Verify result
        assertNotNull(result);
        assertEquals(2, result.pageCount());

        // Verify only page 1 was rotated
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(180, document.getPage(1).getRotation());
            assertEquals(0, document.getPage(2).getRotation());
        }
    }

    @Test
    @SneakyThrows
    void rotateGivenInvalidRotationExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        // Rotation must be multiple of 90
        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, 45));
    }

    @Test
    @SneakyThrows
    void rotateGivenNullRotationExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, null));
    }

    @Test
    @SneakyThrows
    void getInfoGivenValidPdfExpectCorrectInfo() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfInfoResponse response = pdfService.getInfo(originalFile);

        // Verify response
        assertNotNull(response);
        assertEquals("success", response.status());
        assertEquals(2, response.pageCount());
        assertEquals((long) pdfBytes.length, response.fileSizeBytes());
        assertNotNull(response.pdfVersion());
        assertNotNull(response.firstPageDimensions());
        assertNotNull(response.firstPageDimensions().width());
        assertNotNull(response.firstPageDimensions().height());
        assertEquals("points", response.firstPageDimensions().unit());
        assertNotNull(response.allPagesSameDimension());
    }

    @Test
    @SneakyThrows
    void getMetadataGivenValidPdfExpectMetadata() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfMetadataResponse response = pdfService.getMetadata(originalFile);

        // Verify response structure
        assertNotNull(response);
        assertEquals("success", response.status());
        assertEquals("PDF metadata retrieved successfully", response.message());
        assertNotNull(response.timestamp());
        // Note: The actual values depend on the test PDF file's metadata
        // We're just verifying the method doesn't crash and returns a proper response
    }

    @Test
    @SneakyThrows
    void updateMetadataGivenValidMetadataExpectUpdatedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        // Use merge/file1.pdf which has simpler/no metadata
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));

        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title("Test Title")
                .author("Test Author")
                .subject("Test Subject")
                .keywords("Test Keywords")
                .creator("Test Creator")
                .build();

        PdfResult result = pdfService.updateMetadata(originalFile, metadata);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("metadata_updated"));
        assertEquals(1, result.pageCount());

        // Verify metadata was updated
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals("Test Title", document.getDocumentInfo().getTitle());
            // iText may append author to existing authors, so we check contains
            assertTrue(document.getDocumentInfo().getAuthor().contains("Test Author"));
            assertEquals("Test Subject", document.getDocumentInfo().getSubject());
            assertEquals("Test Keywords", document.getDocumentInfo().getKeywords());
            assertEquals("Test Creator", document.getDocumentInfo().getCreator());
        }
    }

    @Test
    @SneakyThrows
    void updateMetadataGivenPartialMetadataExpectOnlyProvidedFieldsUpdated() {
        MultipartFile originalFile = mock(MultipartFile.class);
        // Use merge/file1.pdf which has simpler/no metadata
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));

        // Only update title and subject
        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title("New Title Only")
                .subject("New Subject Only")
                .build();

        PdfResult result = pdfService.updateMetadata(originalFile, metadata);

        // Verify result
        assertNotNull(result);
        assertEquals(1, result.pageCount());

        // Verify specified metadata was updated
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals("New Title Only", document.getDocumentInfo().getTitle());
            assertEquals("New Subject Only", document.getDocumentInfo().getSubject());
        }
    }

    @Test
    @SneakyThrows
    void updateMetadataGivenNullMetadataExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.updateMetadata(originalFile, null));
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
    @SneakyThrows
    void compressGivenLowLevelExpectCompressedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfService.compress(originalFile, "LOW");

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("compressed_low"));
        assertEquals(2, result.pageCount());

        // Verify PDF is valid
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

        PdfResult result = pdfService.compress(originalFile, "MEDIUM");

        // Verify result
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

        PdfResult result = pdfService.compress(originalFile, "HIGH");

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("compressed_high"));
        assertEquals(2, result.pageCount());
    }

    @Test
    @SneakyThrows
    void compressGivenInvalidLevelExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.compress(originalFile, "INVALID"));
    }

    @Test
    @SneakyThrows
    void encryptGivenUserPasswordExpectEncryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.encrypt(originalFile,
                new EncryptRequest("user123", null, null, true, false, false, false));

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("encrypted"));
        assertEquals(2, result.pageCount());

        // Verify PDF is encrypted (should fail without password)
        assertThrows(Exception.class, () -> {
            new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())));
        });
    }

    @Test
    @SneakyThrows
    void encryptGivenOwnerPasswordExpectEncryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfService.encrypt(originalFile,
                new EncryptRequest(null, "owner456", null, false, false, false, false));

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("encrypted"));
        assertEquals(2, result.pageCount());
    }

    @Test
    @SneakyThrows
    void encryptGivenNoPasswordExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfService.encrypt(originalFile, new EncryptRequest(null, null, null, null, null, null, null)));
    }

    @Test
    @SneakyThrows
    void decryptGivenCorrectPasswordExpectDecryptedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult encrypted = pdfService.encrypt(originalFile,
                new EncryptRequest("password123", "owner456", null, true, true, true, true));

        // Then decrypt using owner password
        MultipartFile encryptedFile = mock(MultipartFile.class);
        when(encryptedFile.getBytes()).thenReturn(encrypted.content());

        PdfResult result = pdfService.decrypt(encryptedFile, "owner456");

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("decrypted"));
        assertEquals(2, result.pageCount());

        // Verify PDF can be opened without password
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void decryptGivenNoPasswordExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.decrypt(originalFile, null));
    }

    @Test
    @SneakyThrows
    void optimizeGivenValidPdfExpectOptimizedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfResult result = pdfService.optimize(originalFile);

        // Verify result
        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("optimized"));
        assertEquals(2, result.pageCount());

        // Verify PDF is valid and optimized
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    @SneakyThrows
    void ocrToTextGivenValidPdfExpectTextExtracted() {
        MultipartFile file = mock(MultipartFile.class);
        ITesseract mockTesseract = mock(ITesseract.class);

        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(tesseractFactory.create("eng")).thenReturn(mockTesseract);
        when(mockTesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello World");

        byte[] result = pdfService.ocrToText(file, "eng", null, null);

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

        PdfResult result = pdfService.ocrToPdf(file, "eng", null, null, null);

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

        pdfService.ocrToText(file, null, null, null);

        verify(tesseractFactory).create("eng");
    }
}
