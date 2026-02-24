package com.pdf.pdfapi.service.form;

import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfFormServiceTest {

    private PdfFormService pdfFormService;

    @BeforeEach
    void setUp() {
        pdfFormService = new PdfFormService(new PdfIoSupport(), new PdfResultFactory());
    }

    @Test
    void fillFormGivenEmptyFieldsExpectFailure() {
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class, () -> pdfFormService.fillForm(file, Map.of(), true));
    }

    @Test
    void fillFormGivenInvalidFieldNameExpectFailure() {
        MultipartFile file = mock(MultipartFile.class);

        assertThrows(PdfErrorException.class,
                () -> pdfFormService.fillForm(file, Map.of("../name", "John"), true));
    }

    @Test
    @SneakyThrows
    void fillFormGivenPdfWithoutAcroFormExpectFailure() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));

        assertThrows(PdfErrorException.class,
                () -> pdfFormService.fillForm(file, Map.of("name", "John"), true));
    }
}
