package com.pdf.pdfapi.integration;

import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.PdfService;
import com.pdf.pdfapi.validator.PdfFileValidator;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class PdfApiErrorResponsesIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private PdfService pdfService;

    @MockitoBean
    private PdfFileValidator pdfFileValidator;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void invalidOcrLanguageReturnsStructuredErrorResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        doNothing().when(pdfFileValidator).validatePdfFile(any());

        mockMvc.perform(multipart("/pdfapi/ocr")
                        .file(file)
                        .param("language", "en-US")
                        .param("outputType", "text")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.path").value("/pdfapi/ocr"));
    }

    @Test
    void pdfServiceErrorReturnsStructuredPdfOperationError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        doNothing().when(pdfFileValidator).validatePdfFile(any());
        when(pdfService.extract(any(), eq(1), eq(1))).thenThrow(new PdfErrorException("Boom"));

        mockMvc.perform(multipart("/pdfapi/extract")
                        .file(file)
                        .param("startPage", "1")
                        .param("endPage", "1")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("PDF_OPERATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Boom"))
                .andExpect(jsonPath("$.path").value("/pdfapi/extract"));
    }

    @Test
    void ocrRateLimitReturnsStructuredTooManyRequestsError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        doNothing().when(pdfFileValidator).validatePdfFile(any());
        RateLimiter limiter = RateLimiter.ofDefaults("test-ocr");
        when(pdfService.ocrToText(any(), eq("eng"), eq(null), eq(null), eq(null)))
                .thenThrow(RequestNotPermitted.createRequestNotPermitted(limiter));

        mockMvc.perform(multipart("/pdfapi/ocr")
                        .file(file)
                        .param("language", "eng")
                        .param("outputType", "text")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.path").value("/pdfapi/ocr"));
    }
}
