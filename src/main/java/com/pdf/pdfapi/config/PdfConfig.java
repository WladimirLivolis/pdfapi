package com.pdf.pdfapi.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pdfapi")
@Setter
@Getter
public class PdfConfig {

    @NotBlank
    private String outputFolder;

}
