package com.pdf.pdfapi.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    @Value("${pdfapi.output_folder}")
    private String OUTPUT_FOLDER;

    public void merge(MultipartFile file1, MultipartFile file2) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file1.getBytes())), new PdfWriter(OUTPUT_FOLDER + "merged_" + timestamp() + ".pdf"));
        PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(new ByteArrayInputStream(file2.getBytes())));

        PdfMerger merger = new PdfMerger(pdfDocument);
        merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());

        pdfDocument2.close();
        pdfDocument.close();

    }

    public void split(MultipartFile file, Integer maxPageCount) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));
        String fileName = "splitDocument_" + timestamp() + "_";
        PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
            int partNumber = 1;

            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    return new PdfWriter(OUTPUT_FOLDER + fileName + partNumber++ + ".pdf");
                } catch (final FileNotFoundException ignored) {
                    throw new RuntimeException();
                }
            }
        };

        pdfSplitter.splitByPageCount(maxPageCount, (pdfDoc, pageRange) -> pdfDoc.close());
        pdfDocument.close();

    }

    public void convertImageToPDF(MultipartFile file) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(OUTPUT_FOLDER + "ImageToPdf_" + timestamp() + ".pdf"));
        Document document = new Document(pdfDocument);

        ImageData imageData = ImageDataFactory.create(file.getBytes());
        Image image = new Image(imageData);
        image.setWidth(pdfDocument.getDefaultPageSize().getWidth() - 50);
        image.setAutoScaleHeight(true);

        document.add(image);
        pdfDocument.close();

    }

    private String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return time.format(formatter);
    }

}
