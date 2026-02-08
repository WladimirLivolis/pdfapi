package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.dto.*;
import com.pdf.pdfapi.service.PdfService;
import com.pdf.pdfapi.validator.PdfFileValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfControllerTest {

    @Mock
    private PdfService pdfService;

    @Mock
    private PdfFileValidator validator;

    @InjectMocks
    private PdfController pdfController;

    @Test
    void testMerge() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = {file};
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("merged.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.merge(null, files)).thenReturn(mockResult);

        pdfController.merge(files, null);

        verify(validator, times(1)).validatePdfFiles(files);
        verify(pdfService, times(1)).merge(null, files);
    }

    @Test
    void testMergeWithBookmarks() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = {file};
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("merged.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.merge(true, files)).thenReturn(mockResult);

        pdfController.merge(files, true);

        verify(validator, times(1)).validatePdfFiles(files);
        verify(pdfService, times(1)).merge(true, files);
    }

    @Test
    void testSplit() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("split_1.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.split(file, 1)).thenReturn(List.of(mockResult));

        pdfController.split(file, 1);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).split(file, 1);
    }

    @Test
    void testExtract() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("extracted.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.extract(file, 1, 2)).thenReturn(mockResult);

        pdfController.extract(file, 1, 2);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).extract(file, 1, 2);
    }

    @Test
    void testRemove() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("removed.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.remove(file, 1)).thenReturn(mockResult);

        pdfController.remove(file, 1);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).remove(file, 1);
    }

    @Test
    void testConvertImageToPDF() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("image.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.convertImageToPDF(file)).thenReturn(List.of(mockResult));

        pdfController.convertImageToPDF(file);

        verify(validator, times(1)).validateImageFiles(file);
        verify(pdfService, times(1)).convertImageToPDF(file);
    }

    @Test
    void testRotate() {
        MultipartFile file = mock(MultipartFile.class);
        Integer rotation = 90;
        Integer[] pages = {1, 2};
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("rotated.pdf")
                .sizeInBytes(3)
                .pageCount(2)
                .build();

        when(pdfService.rotate(file, rotation, pages)).thenReturn(mockResult);

        pdfController.rotate(file, rotation, pages);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).rotate(file, rotation, pages);
    }

    @Test
    void testInfo() {
        MultipartFile file = mock(MultipartFile.class);
        PdfInfoResponse mockResponse = PdfInfoResponse.success(
                2, 1024L, "1.7",
                PdfInfoResponse.PageDimensions.builder()
                        .width(595.0f)
                        .height(842.0f)
                        .unit("points")
                        .build(),
                true
        );

        when(pdfService.getInfo(file)).thenReturn(mockResponse);

        pdfController.info(file);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).getInfo(file);
    }

    @Test
    void testGetMetadata() {
        MultipartFile file = mock(MultipartFile.class);
        PdfMetadataResponse mockResponse = PdfMetadataResponse.success(
                "Test Title", "Test Author", "Test Subject",
                "Test Keywords", "Test Creator", "Test Producer",
                "D:20250101120000", "D:20250104120000"
        );

        when(pdfService.getMetadata(file)).thenReturn(mockResponse);

        pdfController.getMetadata(file);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).getMetadata(file);
    }

    @Test
    void testUpdateMetadata() {
        MultipartFile file = mock(MultipartFile.class);
        String title = "New Title";
        String author = "New Author";
        String subject = "New Subject";
        String keywords = "New Keywords";
        String creator = "New Creator";

        PdfMetadataRequest expectedRequest = PdfMetadataRequest.builder()
                .title(title)
                .author(author)
                .subject(subject)
                .keywords(keywords)
                .creator(creator)
                .build();

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("metadata_updated.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.updateMetadata(eq(file), eq(expectedRequest))).thenReturn(mockResult);

        pdfController.updateMetadata(file, title, author, subject, keywords, creator);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).updateMetadata(eq(file), eq(expectedRequest));
    }

    @Test
    void testAddPageNumbers() {
        MultipartFile file = mock(MultipartFile.class);
        String position = "bottom-center";
        String format = "Page {current} of {total}";
        Integer startPage = 1;
        Integer endPage = 10;

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("numbered.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.addPageNumbers(file, position, format, startPage, endPage)).thenReturn(mockResult);

        pdfController.addPageNumbers(file, position, format, startPage, endPage);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).addPageNumbers(file, position, format, startPage, endPage);
    }

    @Test
    void testWatermarkWithText() {
        MultipartFile file = mock(MultipartFile.class);
        String text = "CONFIDENTIAL";
        String position = "center";
        Float opacity = 0.3f;
        Float rotation = 45.0f;
        Float scale = 1.0f;
        String layer = "foreground";
        Integer startPage = 1;
        Integer endPage = 10;

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("watermarked.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.watermark(file, text, null, position, opacity, rotation, scale, layer, startPage, endPage))
                .thenReturn(mockResult);

        pdfController.watermark(file, text, null, position, opacity, rotation, scale, layer, startPage, endPage);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).watermark(file, text, null, position, opacity, rotation, scale, layer, startPage, endPage);
    }

    @Test
    void testWatermarkWithImage() {
        MultipartFile file = mock(MultipartFile.class);
        MultipartFile image = mock(MultipartFile.class);
        String position = "top-right";
        Float opacity = 0.5f;
        Float rotation = 0.0f;
        Float scale = 0.5f;
        String layer = "background";

        when(image.getContentType()).thenReturn("image/png");

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("watermarked.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.watermark(file, null, image, position, opacity, rotation, scale, layer, null, null))
                .thenReturn(mockResult);

        pdfController.watermark(file, null, image, position, opacity, rotation, scale, layer, null, null);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).watermark(file, null, image, position, opacity, rotation, scale, layer, null, null);
    }

    @Test
    void testCompress() {
        MultipartFile file = mock(MultipartFile.class);
        String level = "MEDIUM";

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("compressed_medium.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.compress(file, level)).thenReturn(mockResult);

        pdfController.compress(file, level);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).compress(file, level);
    }

    @Test
    void testEncrypt() {
        MultipartFile file = mock(MultipartFile.class);
        String userPassword = "user123";
        String ownerPassword = "owner456";
        Integer encryptionType = 256;
        Boolean allowPrinting = true;
        Boolean allowModifying = false;
        Boolean allowCopy = false;
        Boolean allowAnnotations = false;

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("encrypted.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.encrypt(file, userPassword, ownerPassword, encryptionType,
                allowPrinting, allowModifying, allowCopy, allowAnnotations))
                .thenReturn(mockResult);

        pdfController.encrypt(file, userPassword, ownerPassword, encryptionType,
                allowPrinting, allowModifying, allowCopy, allowAnnotations);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).encrypt(file, userPassword, ownerPassword, encryptionType,
                allowPrinting, allowModifying, allowCopy, allowAnnotations);
    }

    @Test
    void testDecrypt() {
        MultipartFile file = mock(MultipartFile.class);
        String password = "password123";

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("decrypted.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.decrypt(file, password)).thenReturn(mockResult);

        pdfController.decrypt(file, password);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).decrypt(file, password);
    }

    @Test
    void testOptimize() {
        MultipartFile file = mock(MultipartFile.class);

        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("optimized.pdf")
                .sizeInBytes(3)
                .pageCount(10)
                .build();

        when(pdfService.optimize(file)).thenReturn(mockResult);

        pdfController.optimize(file);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).optimize(file);
    }

    @Test
    void testToImages() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] mockZip = new byte[]{1, 2, 3};

        when(pdfService.toImages(file, "png", 150, null, null, null)).thenReturn(mockZip);

        pdfController.toImages(file, "png", 150, null, null, null);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).toImages(file, "png", 150, null, null, null);
    }

    @Test
    void testExtractImages() {
        MultipartFile file = mock(MultipartFile.class);
        byte[] mockZip = new byte[]{1, 2, 3};

        when(pdfService.extractImages(file, 100, 100)).thenReturn(mockZip);

        pdfController.extractImages(file, 100, 100);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).extractImages(file, 100, 100);
    }

    @Test
    void testCrop() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("cropped.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.crop(file, 0f, 0f, 500f, 700f, null, null)).thenReturn(mockResult);

        pdfController.crop(file, 0f, 0f, 500f, 700f, null, null);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).crop(file, 0f, 0f, 500f, 700f, null, null);
    }

    @Test
    void testFillForm() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("filled.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        String fieldsJson = "{\"name\":\"John\"}";
        Map<String, String> fields = Map.of("name", "John");

        when(pdfService.fillForm(file, fields, true)).thenReturn(mockResult);

        pdfController.fillForm(file, fieldsJson, true);

        verify(validator, times(1)).validatePdfFile(file);
        verify(pdfService, times(1)).fillForm(eq(file), eq(fields), eq(true));
    }

}
