package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.service.PdfService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfControllerTest {

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private PdfController pdfController;

    @Test
    void test_merge() {

        MultipartFile file = mock(MultipartFile.class);

        pdfController.merge(file);

        verify(pdfService, times(1)).merge(file);

    }

    @Test
    void test_split() {

        MultipartFile file = mock(MultipartFile.class);

        pdfController.split(file, 1);

        verify(pdfService, times(1)).split(file, 1);

    }

    @Test
    void test_extract() {

        MultipartFile file = mock(MultipartFile.class);

        pdfController.extract(file, 1, 2);

        verify(pdfService, times(1)).extract(file, 1, 2);

    }

    @Test
    void test_remove() {

        MultipartFile file = mock(MultipartFile.class);

        pdfController.remove(file, 1);

        verify(pdfService, times(1)).remove(file, 1);

    }

    @Test
    void test_convertImageToPDF() {

        MultipartFile file = mock(MultipartFile.class);

        pdfController.convertImageToPDF(file);

        verify(pdfService, times(1)).convertImageToPDF(file);

    }

}
