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
    void test_merge() {
        MultipartFile file = mock(MultipartFile.class);
        PdfResult mockResult = PdfResult.builder()
                .content(new byte[]{1, 2, 3})
                .suggestedFileName("merged.pdf")
                .sizeInBytes(3)
                .pageCount(1)
                .build();

        when(pdfService.merge(file)).thenReturn(mockResult);

        pdfController.merge(file);

        verify(validator, times(1)).validatePdfFiles(file);
        verify(pdfService, times(1)).merge(file);
    }

    @Test
    void test_split() {
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
    void test_extract() {
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
    void test_remove() {
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
    void test_convertImageToPDF() {
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
    void test_rotate() {
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
    void test_info() {
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
    void test_getMetadata() {
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
    void test_updateMetadata() {
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
    void test_addPageNumbers() {
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

}
