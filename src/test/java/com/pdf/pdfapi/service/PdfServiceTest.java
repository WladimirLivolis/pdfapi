package com.pdf.pdfapi.service;

import com.pdf.pdfapi.exception.PdfErrorException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    private PdfService pdfService;

    @BeforeEach
    public void init() {
        pdfService = new PdfService();
    }

    @Test
    void merge_when_there_is_only_one_file_expect_failure() {

        MultipartFile file = mock(MultipartFile.class);
        MultipartFile[] files = new MultipartFile[1];
        files[0] = file;

        Assertions.assertThrows(PdfErrorException.class, () -> pdfService.merge(files));

    }

}
