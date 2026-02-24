package com.pdf.pdfapi.service.support;

import com.itextpdf.kernel.pdf.PdfReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class PdfIoSupport {

    public PdfReader toPdfReader(MultipartFile file) throws IOException {
        return new PdfReader(new ByteArrayInputStream(file.getBytes()));
    }

    public PDDocument loadPdfBoxDocument(MultipartFile file) throws IOException {
        return Loader.loadPDF(file.getBytes());
    }
}
