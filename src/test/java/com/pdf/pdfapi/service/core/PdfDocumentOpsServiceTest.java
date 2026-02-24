package com.pdf.pdfapi.service.core;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.pdf.pdfapi.dto.PdfResult;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfDocumentOpsServiceTest {

    private PdfDocumentOpsService pdfDocumentOpsService;

    @BeforeEach
    void setUp() {
        pdfDocumentOpsService = new PdfDocumentOpsService(
                new PdfIoSupport(),
                new PdfResultFactory(),
                new PageRangeResolver()
        );
    }

    @Test
    void mergeGivenThereIsOnlyOneFileExpectFailure() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = new MultipartFile[]{file};

        assertThrows(PdfErrorException.class, () -> pdfDocumentOpsService.merge(files));
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

        PdfResult result = pdfDocumentOpsService.merge(file1, file2);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("merged"));
        assertEquals(2, result.pageCount());

        String expectedText = pdfToText("src/test/resources/merge/merged_file.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void splitGivenOneFileExpectMultipleFiles() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/split/original_file.pdf")));

        List<PdfResult> results = pdfDocumentOpsService.split(originalFile, 1);

        assertNotNull(results);
        assertEquals(2, results.size());

        for (PdfResult result : results) {
            assertNotNull(result.content());
            assertTrue(result.content().length > 0);
            assertTrue(result.suggestedFileName().contains("splitDocument"));
            assertEquals(1, result.pageCount());
        }

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

        PdfResult result = pdfDocumentOpsService.extract(originalFile, 2, 2);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("extracted"));
        assertEquals(1, result.pageCount());

        String expectedText = pdfToText("src/test/resources/extract/extractedPages.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void removeGivenOneFileExpectNewFile() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfDocumentOpsService.remove(originalFile, 2);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("removed"));
        assertEquals(1, result.pageCount());

        String expectedText = pdfToText("src/test/resources/remove/removedPages.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void removeGivenDuplicateAndUnsortedPagesExpectNormalizedRemoval() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfDocumentOpsService.remove(originalFile, 2, 2);

        assertNotNull(result);
        assertEquals(1, result.pageCount());

        String expectedText = pdfToText("src/test/resources/remove/removedPages.pdf");
        String actualText = pdfToText(result.content());
        assertEquals(expectedText, actualText);
    }

    @Test
    @SneakyThrows
    void rotateGivenValidRotationExpectRotatedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfDocumentOpsService.rotate(originalFile, 90);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("rotated"));
        assertEquals(2, result.pageCount());

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

        PdfResult result = pdfDocumentOpsService.rotate(originalFile, 180, 1);

        assertNotNull(result);
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(180, document.getPage(1).getRotation());
            assertEquals(0, document.getPage(2).getRotation());
        }
    }

    @Test
    void rotateGivenInvalidRotationExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);
        assertThrows(PdfErrorException.class, () -> pdfDocumentOpsService.rotate(originalFile, 45));
    }

    @Test
    void rotateGivenNullRotationExpectFailure() {
        MultipartFile originalFile = mock(MultipartFile.class);
        assertThrows(PdfErrorException.class, () -> pdfDocumentOpsService.rotate(originalFile, null));
    }

    @Test
    @SneakyThrows
    void cropGivenValidParametersExpectCroppedPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);
        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        PdfResult result = pdfDocumentOpsService.crop(originalFile, 0f, 0f, 500f, 700f, null, null);

        assertNotNull(result);
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().contains("cropped"));
        assertEquals(2, result.pageCount());

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(result.content())))) {
            assertEquals(2, document.getNumberOfPages());
        }
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
}
