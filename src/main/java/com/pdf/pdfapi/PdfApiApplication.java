package com.pdf.pdfapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PdfApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfApiApplication.class, args);
    }

}
