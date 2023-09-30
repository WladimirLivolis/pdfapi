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
import com.pdf.pdfapi.config.PdfConfig;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfService {

    private final PdfConfig pdfConfig;

    public void merge(MultipartFile... file) {

        if (file.length < 2) {
            String errorMsg = "Merge needs at least 2 documents";
            log.error(errorMsg);
            throw new PdfErrorException(errorMsg);
        }

        try {

            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file[0].getBytes())),
                    new PdfWriter(String.format("%smerged_%s.pdf", pdfConfig.getOutputFolder(), timestamp()))
            );
            PdfMerger merger = new PdfMerger(pdfDocument);

            for (int i = 1; i < file.length; i++) {
                PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(new ByteArrayInputStream(file[i].getBytes())));
                merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());
                pdfDocument2.close();
            }

            pdfDocument.close();

        } catch (Exception e) {
            log.error("Failed while merging files", e);
        }

    }

    public void split(MultipartFile file, Integer maxPageCount) {

        try {

            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));
            String fileName = String.format("splitDocument_%s_", timestamp());
            PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
                int partNumber = 1;

                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    try {
                        return new PdfWriter(String.format("%s%s%d.pdf",pdfConfig.getOutputFolder(), fileName, partNumber++));
                    } catch (final FileNotFoundException ex) {
                        throw new PdfErrorException(ex.getMessage());
                    }
                }
            };

            pdfSplitter.splitByPageCount(maxPageCount, (pdfDoc, pageRange) -> pdfDoc.close());
            pdfDocument.close();

        } catch (Exception e) {
            log.error("Failed to split file", e);
        }

    }

    public void extract(MultipartFile file, Integer startPage, Integer endPage) {

        try {

            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));

            PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    try {
                        return new PdfWriter(String.format("%sextractedPages_%s.pdf", pdfConfig.getOutputFolder(), timestamp()));
                    } catch (final FileNotFoundException ex) {
                        throw new PdfErrorException(ex.getMessage());
                    }
                }
            };

            PdfDocument newPdfDocument = pdfSplitter.extractPageRange(new PageRange().addPageSequence(startPage, endPage));

            newPdfDocument.close();
            pdfDocument.close();

        } catch (Exception e) {
            log.error("Failed to extract from file", e);
        }

    }

    public void remove(MultipartFile file, Integer... page) {

        try {

            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file.getBytes())),
                    new PdfWriter(String.format("%sremovedPages_%s.pdf", pdfConfig.getOutputFolder(), timestamp()))
            );

            int removeCount = 0;
            for (Integer pageNumber : page) {
                pdfDocument.removePage(pageNumber - removeCount++);
            }

            pdfDocument.close();

        } catch (Exception e) {
            log.error("Failed to remove from file", e);
        }

    }

    public void convertImageToPDF(MultipartFile... file) {

        try {

            for (MultipartFile currentFile : file) {

                PdfDocument pdfDocument = new PdfDocument(new PdfWriter(String.format("%sImageToPdf_%s.pdf",pdfConfig.getOutputFolder(), timestamp())));
                Document document = new Document(pdfDocument);

                ImageData imageData = ImageDataFactory.create(currentFile.getBytes());
                Image image = new Image(imageData);
                image.setWidth(pdfDocument.getDefaultPageSize().getWidth() - 50);
                image.setAutoScaleHeight(true);

                document.add(image);
                pdfDocument.close();

            }

        } catch (Exception e) {
            log.error("Failed while converting image to PDF", e);
        }

    }

    private String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return time.format(formatter);
    }

}
