package com.pdf.pdfapi.integration;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "security.users.user.username=testuser",
        "security.users.user.password=testpass",
        "security.users.user.roles=USER",
        "security.users.admin.username=admin",
        "security.users.admin.password=adminpass",
        "security.users.admin.roles=USER,ADMIN"
})
class PdfApiSmokeIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void extractEndpointEndToEndReturnsParseablePdf() throws Exception {
        byte[] pdfBytes = Files.readAllBytes(Path.of("src/test/resources/extract/original_file.pdf"));
        MockMultipartFile file = new MockMultipartFile("file", "original_file.pdf", "application/pdf", pdfBytes);

        MvcResult result = mockMvc.perform(multipart("/pdfapi/extract")
                        .file(file)
                        .param("startPage", "2")
                        .param("endPage", "2")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("extracted")))
                .andReturn();

        byte[] responsePdf = result.getResponse().getContentAsByteArray();
        assertNotNull(responsePdf);

        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(responsePdf)))) {
            assertEquals(1, document.getNumberOfPages());
            String text = PdfTextExtractor.getTextFromPage(document.getPage(1));
            assertNotNull(text);
            assertFalse(text.isBlank());
        }
    }
}
