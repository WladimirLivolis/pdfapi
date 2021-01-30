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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

@Service
public class PdfService {

    public void merge(String file1, String file2, String outputFolder) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(file1), new PdfWriter(outputFolder + "merged.pdf"));
        PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(file2));

        PdfMerger merger = new PdfMerger(pdfDocument);
        merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());

        pdfDocument2.close();
        pdfDocument.close();

    }

    public void split(String orig, String outputFolder, Integer maxPageCount) throws IOException {

        PdfDocument pdfDocument = new PdfDocument(new PdfReader(new File(orig)));
        PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
            int partNumber = 1;

            @Override
            protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                try {
                    return new PdfWriter(outputFolder + "splitDocument_" + partNumber++ + ".pdf");
                } catch (final FileNotFoundException ignored) {
                    throw new RuntimeException();
                }
            }
        };

        pdfSplitter.splitByPageCount(maxPageCount, (pdfDoc, pageRange) -> pdfDoc.close());
        pdfDocument.close();

    }

    public void convertImageToPDF(String orig, String outputFolder) throws FileNotFoundException, MalformedURLException {

        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputFolder + "ImageToPdf.pdf"));
        Document document = new Document(pdfDocument);

        ImageData imageData = ImageDataFactory.create(orig);
        Image image = new Image(imageData);
        image.setWidth(pdfDocument.getDefaultPageSize().getWidth() - 50);
        image.setAutoScaleHeight(true);

        document.add(image);
        pdfDocument.close();

    }

}
