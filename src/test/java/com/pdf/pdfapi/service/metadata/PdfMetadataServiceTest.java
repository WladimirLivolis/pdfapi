package com.pdf.pdfapi.service.metadata;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
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
class PdfMetadataServiceTest {

    private PdfMetadataService pdfMetadataService;

    @BeforeEach
    void setUp() {
        pdfMetadataService = new PdfMetadataService(new PdfIoSupport(), new PdfResultFactory());
    }

    @Test
    @SneakyThrows
    void getInfoGivenValidPdfExpectCorrectInfo() {
        MultipartFile originalFile = mock(MultipartFile.class);
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        when(originalFile.getBytes()).thenReturn(pdfBytes);
        when(originalFile.getSize()).thenReturn((long) pdfBytes.length);

        PdfInfoResponse response = pdfMetadataService.getInfo(originalFile);

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

        PdfMetadataResponse response = pdfMetadataService.getMetadata(originalFile);

        assertNotNull(response);
        assertEquals("success", response.status());
        assertEquals("PDF metadata retrieved successfully", response.message());
        assertNotNull(response.timestamp());
    }

    @Test
    @SneakyThrows
    void updateMetadataGivenValidMetadataExpectUpdatedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));

        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title("Test Title")
                .author("Test Author")
                .subject("Test Subject")
                .keywords("Test Keywords")
                .creator("Test Creator")
                .build();

        PdfResult result = pdfMetadataService.updateMetadata(originalFile, metadata);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("metadata_updated"));
        assertEquals(1, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals("Test Title", document.getDocumentInfo().getTitle());
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
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));

        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title("New Title Only")
                .subject("New Subject Only")
                .build();

        PdfResult result = pdfMetadataService.updateMetadata(originalFile, metadata);

        assertNotNull(result);
        assertEquals(1, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals("New Title Only", document.getDocumentInfo().getTitle());
            assertEquals("New Subject Only", document.getDocumentInfo().getSubject());
        }
    }

    @Test
    void updateMetadataGivenNullMetadataExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfMetadataService.updateMetadata(originalFile, null));
    }
}
