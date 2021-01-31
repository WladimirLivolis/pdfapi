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

    public void merge(MultipartFile... file) throws Exception {

        if (file.length < 2) { throw new Exception("MERGE_NEEDS_AT_LEAST_TWO_DOCUMENTS"); }

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file[0].getBytes())), new PdfWriter(OUTPUT_FOLDER + "merged_" + timestamp() + ".pdf"));
        PdfMerger merger = new PdfMerger(pdfDocument);

        for (int i = 1; i < file.length; i++) {
            PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(new ByteArrayInputStream(file[i].getBytes())));
            merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());
            pdfDocument2.close();
        }

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

    public void extract(MultipartFile file, Integer startPage, Integer endPage) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));

        PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    return new PdfWriter(OUTPUT_FOLDER + "extractPages_" + timestamp() + ".pdf");
                } catch (final FileNotFoundException ignored) {
                    throw new RuntimeException();
                }
            }
        };

        PdfDocument newPdfDocument = pdfSplitter.extractPageRange(new PageRange().addPageSequence(startPage, endPage));

        newPdfDocument.close();
        pdfDocument.close();

    }

    public void remove(MultipartFile file, Integer... page) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())), new PdfWriter(OUTPUT_FOLDER + "removePages_" + timestamp() + ".pdf"));

        int removeCount = 0;
        for (Integer pageNumber : page) {
            pdfDocument.removePage(pageNumber - removeCount++);
        }

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
