package com.pdf.pdfapi.service.ocr;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.TesseractFactory;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfOcrService {

    private final TesseractFactory tesseractFactory;
    private final PdfIoSupport pdfIoSupport;
    private final PageRangeResolver pageRangeResolver;
    private final PdfResultFactory pdfResultFactory;

    public byte[] ocrToText(MultipartFile file, String language, Integer startPage, Integer endPage) {
        return ocrToText(file, language, startPage, endPage, null);
    }

    public byte[] ocrToText(MultipartFile file, String language, Integer startPage, Integer endPage, Integer dpi) {
        String lang = resolveLanguage(language);
        int resolvedDpi = (dpi != null && dpi > 0) ? dpi : 300;
        try {
            PDDocument document = pdfIoSupport.loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageRangeResolver.PageBounds bounds = pageRangeResolver.resolve(startPage, endPage, totalPages);
            int start = bounds.start() - 1;
            int end = bounds.end() - 1;

            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = tesseractFactory.create(lang);
            StringBuilder text = new StringBuilder();

            for (int i = start; i <= end; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, resolvedDpi, ImageType.RGB);
                String pageText = performOcr(tesseract, image, i + 1);
                text.append("--- Page ").append(i + 1).append(" ---\n");
                text.append(pageText).append("\n\n");
            }

            document.close();
            log.info("Successfully performed OCR text extraction on {} pages at {} DPI", end - start + 1, resolvedDpi);
            return text.toString().getBytes(StandardCharsets.UTF_8);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to perform OCR text extraction", e);
            throw new PdfErrorException("Failed to perform OCR: " + e.getMessage(), e);
        }
    }

    public PdfResult ocrToPdf(MultipartFile file, String language, Integer startPage, Integer endPage, Integer dpi) {
        String lang = resolveLanguage(language);
        int resolvedDpi = (dpi != null && dpi > 0) ? dpi : 300;
        try {
            PDDocument document = pdfIoSupport.loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageRangeResolver.PageBounds bounds = pageRangeResolver.resolve(startPage, endPage, totalPages);
            int start = bounds.start() - 1;
            int end = bounds.end() - 1;

            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = tesseractFactory.create(lang);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument resultPdf = new PdfDocument(new PdfWriter(outputStream));
            PdfFont font = PdfFontFactory.createFont();

            for (int i = start; i <= end; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, resolvedDpi, ImageType.RGB);
                String pageText = performOcr(tesseract, image, i + 1);

                PDRectangle mediaBox = document.getPage(i).getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                PdfPage pdfPage = resultPdf.addNewPage(new PageSize(pageWidth, pageHeight));
                PdfCanvas canvas = new PdfCanvas(pdfPage);

                ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", imgBaos);
                ImageData imageData = ImageDataFactory.create(imgBaos.toByteArray());
                canvas.addImageWithTransformationMatrix(imageData, pageWidth, 0, 0, pageHeight, 0, 0);

                String cleanText = pageText.replace("\n", " ").replace("\r", " ").trim();
                if (!cleanText.isEmpty()) {
                    try {
                        canvas.beginText()
                                .setFontAndSize(font, 1)
                                .setTextRenderingMode(3)
                                .moveText(0, pageHeight / 2)
                                .showText(cleanText)
                                .endText();
                    } catch (Exception e) {
                        log.warn("Could not add invisible text layer for page {}", i + 1);
                    }
                }
            }

            resultPdf.close();
            document.close();

            byte[] resultBytes = outputStream.toByteArray();
            log.info("Successfully created searchable PDF with {} pages ({} bytes)", end - start + 1, resultBytes.length);
            return pdfResultFactory.buildPdfResult(resultBytes, "ocr", end - start + 1);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create searchable PDF", e);
            throw new PdfErrorException("Failed to create searchable PDF: " + e.getMessage(), e);
        }
    }

    private String performOcr(ITesseract tesseract, BufferedImage image, int pageNumber) {
        try {
            return tesseract.doOCR(image);
        } catch (UnsatisfiedLinkError e) {
            log.error("Tesseract native library not found", e);
            throw new PdfErrorException("Tesseract OCR is not available. Please install Tesseract on the system.");
        } catch (TesseractException e) {
            log.error("OCR failed on page {}", pageNumber, e);
            throw new PdfErrorException("OCR failed on page " + pageNumber + ": " + e.getMessage(), e);
        }
    }

    private String resolveLanguage(String language) {
        return (language != null && !language.isBlank()) ? language : "eng";
    }
}
