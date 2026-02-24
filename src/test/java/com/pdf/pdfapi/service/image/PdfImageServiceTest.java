package com.pdf.pdfapi.service.image;

import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import com.pdf.pdfapi.service.support.ZipSupport;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfImageServiceTest {

    private PdfImageService pdfImageService;

    @BeforeEach
    void setUp() {
        pdfImageService = new PdfImageService(
                new PdfIoSupport(),
                new PageRangeResolver(),
                new PdfResultFactory(),
                new ZipSupport()
        );
    }

    @Test
    @SneakyThrows
    void convertImageToPDFGivenImageExpectPdf() {
        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/image/image.png")));
        when(originalFile.getOriginalFilename()).thenReturn("image.png");

        List<PdfResult> results = pdfImageService.convertImageToPDF(originalFile);

        assertNotNull(results);
        assertEquals(1, results.size());

        PdfResult result = results.getFirst();
        assertNotNull(result.content());
        assertTrue(result.content().length > 0);
        assertTrue(result.suggestedFileName().endsWith(".pdf"));
        assertTrue(result.suggestedFileName().contains("image"));
        assertEquals(1, result.pageCount());
    }

    @Test
    @SneakyThrows
    void toImagesGivenValidPdfExpectZipWithPageImages() {
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf"));
        MultipartFile file = new MockMultipartFile("file", "file1.pdf", "application/pdf", pdfBytes);

        byte[] zipBytes = pdfImageService.toImages(file, "png", 150, null, null);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertTrue(entry.getName().startsWith("page_1."));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            zis.transferTo(out);
            assertTrue(out.size() > 0);
        }
    }

    @Test
    @SneakyThrows
    void extractImagesGivenPdfWithImagesExpectZipEntries() {
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/image/ImageToPdf.pdf"));
        MultipartFile file = new MockMultipartFile("file", "ImageToPdf.pdf", "application/pdf", pdfBytes);

        byte[] zipBytes = pdfImageService.extractImages(file, null, null);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull(entry);
            assertTrue(entry.getName().startsWith("image_page"));
        }
    }
}
