package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.dto.PdfResult;
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

}
