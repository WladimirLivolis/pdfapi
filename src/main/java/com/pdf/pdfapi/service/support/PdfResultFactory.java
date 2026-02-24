package com.pdf.pdfapi.service.support;

import com.pdf.pdfapi.dto.PdfResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PdfResultFactory {

    public PdfResult buildPdfResult(byte[] pdfBytes, String fileNamePrefix, int pageCount) {
        return PdfResult.builder()
                .content(pdfBytes)
                .suggestedFileName(String.format("%s_%s.pdf", fileNamePrefix, timestamp()))
                .sizeInBytes(pdfBytes.length)
                .pageCount(pageCount)
                .build();
    }

    public String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return time.format(formatter);
    }
}
