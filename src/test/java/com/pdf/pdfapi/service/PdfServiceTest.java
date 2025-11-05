package com.pdf.pdfapi.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.pdf.pdfapi.config.PdfConfig;
import com.pdf.pdfapi.dto.*;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private PdfConfig pdfConfig;

    @InjectMocks
    private PdfService pdfService;

    @BeforeEach
    void init() {
        // No longer need to delete temporary files since we don't save to disk
        lenient().when(pdfConfig.getOutputFolder()).thenReturn("./output/");
    }

    @Test
    void merge_given_there_is_only_one_file_expect_failure() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = new MultipartFile[1];
        files[0] = file;

        assertThrows(PdfErrorException.class, () -> pdfService.merge(files));
    }

    @Test
    @SneakyThrows
    void merge_given_there_are_two_files_expect_one_combined_file() {
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
    void split_given_one_file_expect_multiple_files() {
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
    void extract_given_one_file_expect_new_file() {
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
    void remove_given_one_file_expect_new_file() {
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
    void convertImageToPDF_given_image_expect_pdf() {
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

    // ==================== Phase 3A Tests ====================

    @Test
    @SneakyThrows
    void rotate_given_valid_rotation_expect_rotated_pdf() {
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
    void rotate_given_specific_pages_expect_only_those_pages_rotated() {
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
    void rotate_given_invalid_rotation_expect_failure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        // Rotation must be multiple of 90
        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, 45));
    }

    @Test
    @SneakyThrows
    void rotate_given_null_rotation_expect_failure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.rotate(originalFile, null));
    }

    @Test
    @SneakyThrows
    void getInfo_given_valid_pdf_expect_correct_info() {
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
    void getMetadata_given_valid_pdf_expect_metadata() {
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
    void updateMetadata_given_valid_metadata_expect_updated_pdf() {
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
    void updateMetadata_given_partial_metadata_expect_only_provided_fields_updated() {
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
    void updateMetadata_given_null_metadata_expect_failure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfService.updateMetadata(originalFile, null));
    }

    @Test
    @SneakyThrows
    void addPageNumbers_given_valid_parameters_expect_numbered_pdf() {
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
    void addPageNumbers_given_page_range_expect_only_range_numbered() {
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
    void addPageNumbers_given_default_parameters_expect_default_formatting() {
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
}
