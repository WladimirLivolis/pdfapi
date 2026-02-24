package com.pdf.pdfapi.integration;

import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.service.PdfService;
import com.pdf.pdfapi.validator.PdfFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "security.users.user.username=testuser",
        "security.users.user.password=testpass",
        "security.users.user.roles=USER",
        "security.users.admin.username=admin",
        "security.users.admin.password=adminpass",
        "security.users.admin.roles=USER,ADMIN"
})
class PdfApiSecurityIntegrationTest {

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
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPdfEndpointRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());

        mockMvc.perform(multipart("/pdfapi/info").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedMultipartEndpointReturnsPdfForAuthenticatedUser() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        PdfResult result = PdfResult.builder()
                .content(new byte[]{1, 2, 3, 4})
                .suggestedFileName("extracted_test.pdf")
                .sizeInBytes(4)
                .pageCount(1)
                .build();

        doNothing().when(pdfFileValidator).validatePdfFile(any());
        when(pdfService.extract(any(), eq(1), eq(1))).thenReturn(result);

        mockMvc.perform(multipart("/pdfapi/extract")
                        .file(file)
                        .param("startPage", "1")
                        .param("endPage", "1")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"extracted_test.pdf\""));
    }

    @Test
    void actuatorMetricsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorMetricsAllowsAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedOcrTextEndpointReturnsTextAttachment() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        doNothing().when(pdfFileValidator).validatePdfFile(any());
        when(pdfService.ocrToText(any(), eq("eng"), eq(null), eq(null), eq(null)))
                .thenReturn("hello".getBytes());

        mockMvc.perform(multipart("/pdfapi/ocr")
                        .file(file)
                        .param("language", "eng")
                        .param("outputType", "text")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ocr_result.txt\""));
    }

    @Test
    void authenticatedWatermarkEndpointWithImageReturnsPdfAttachment() throws Exception {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-test".getBytes());
        MockMultipartFile imageFile = new MockMultipartFile("image", "logo.png", "image/png", new byte[]{1, 2, 3});
        PdfResult result = PdfResult.builder()
                .content(new byte[]{5, 6})
                .suggestedFileName("watermarked_test.pdf")
                .sizeInBytes(2)
                .pageCount(1)
                .build();

        doNothing().when(pdfFileValidator).validatePdfFile(any());
        when(pdfService.watermark(any(), any(), any())).thenReturn(result);

        mockMvc.perform(multipart("/pdfapi/watermark")
                        .file(pdfFile)
                        .file(imageFile)
                        .param("position", "top-right")
                        .param("opacity", "0.5")
                        .with(httpBasic("testuser", "testpass")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"watermarked_test.pdf\""));
    }

    @Test
    void corsPreflightAllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/pdfapi/info")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreflightRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/pdfapi/info")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}
