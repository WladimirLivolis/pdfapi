package com.pdf.pdfapi.config;

import com.pdf.pdfapi.service.TesseractFactory;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OcrConfig {

    @Value("${ocr.tesseract.datapath:}")
    private String datapath;

    @Bean
    public TesseractFactory tesseractFactory() {
        return language -> {
            Tesseract instance = new Tesseract();
            if (StringUtils.hasText(datapath)) {
                instance.setDatapath(datapath);
            }
            instance.setLanguage(language);
            return instance;
        };
    }
}
