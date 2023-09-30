package com.pdf.pdfapi.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.pdf.pdfapi.config.PdfConfig;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    private PdfConfig pdfConfig;

    @InjectMocks
    private PdfService pdfService;

    @BeforeEach
    public void init() {
        deleteTemporaryFiles();
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

        when(file1.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file1.pdf")));
        when(file2.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/merge/file2.pdf")));

        pdfService.merge(file1, file2);

        compareFiles("src/test/resources/merge/merged_file.pdf", getFileNames().stream().findFirst().orElse(""));

    }

    @Test
    @SneakyThrows
    void split_given_one_file_expect_multiple_files() {

        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/split/original_file.pdf")));

        pdfService.split(originalFile, 1);

        compareFiles("src/test/resources/split/splitDocument_1.pdf", getFileNames().stream().min(Comparator.naturalOrder()).orElse(""));
        compareFiles("src/test/resources/split/splitDocument_2.pdf", getFileNames().stream().max(Comparator.naturalOrder()).orElse(""));

    }

    @Test
    @SneakyThrows
    void extract_given_one_file_expect_new_file() {

        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        pdfService.extract(originalFile, 2, 2);

        compareFiles("src/test/resources/extract/extractedPages.pdf", getFileNames().stream().findFirst().orElse(""));

    }

    @Test
    @SneakyThrows
    void remove_given_one_file_expect_new_file() {

        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf")));

        pdfService.remove(originalFile, 2);

        compareFiles("src/test/resources/remove/removedPages.pdf", getFileNames().stream().findFirst().orElse(""));

    }

    @Test
    @SneakyThrows
    void convertImageToPDF_given_image_expect_pdf() {

        MultipartFile originalFile = mock(MultipartFile.class);

        when(originalFile.getBytes()).thenReturn(Files.readAllBytes(Path.of("src/test/resources/image/image.png")));

        pdfService.convertImageToPDF(originalFile);

        assertThat(getFileNames().stream().findFirst().orElse("")).isNotBlank().endsWith(".pdf");

    }

    private void compareFiles(String expectedFileName, String actualFileName) {

        assertThat(actualFileName).isNotBlank().endsWith(".pdf");
        assertEquals(pdfToText(expectedFileName), pdfToText(actualFileName));

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
    private void deleteTemporaryFiles() {
        try (Stream<Path> files = Files.walk(Path.of("./output/")).filter(Files::isRegularFile)) {
            files.forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                }
            });
        }
    }

    @SneakyThrows
    private List<String> getFileNames() {
        List<String> fileNames = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("./output/")).filter(Files::isRegularFile)) {
            files.forEach(file -> fileNames.add(file.toString()));
        }
        return fileNames;
    }

}
