package com.pdf.pdfapi.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pdfapi")
@Data
public class PdfConfig {

    @NotBlank
    private String outputFolder;

}
