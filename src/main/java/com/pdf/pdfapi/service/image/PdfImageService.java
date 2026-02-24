package com.pdf.pdfapi.service.image;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import com.pdf.pdfapi.service.support.ZipSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfImageService {

    private final PdfIoSupport pdfIoSupport;
    private final PageRangeResolver pageRangeResolver;
    private final PdfResultFactory pdfResultFactory;
    private final ZipSupport zipSupport;

    public List<PdfResult> convertImageToPDF(MultipartFile... file) {
        if (file == null || file.length == 0) {
            throw new PdfErrorException("At least one image file must be provided");
        }

        try {
            List<PdfResult> results = new ArrayList<>();

            for (MultipartFile currentFile : file) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputStream));
                Document document = new Document(pdfDocument);

                ImageData imageData = ImageDataFactory.create(currentFile.getBytes());
                Image image = new Image(imageData);
                image.setWidth(pdfDocument.getDefaultPageSize().getWidth() - 50);
                image.setAutoScaleHeight(true);

                document.add(image);
                int pageCount = pdfDocument.getNumberOfPages();
                pdfDocument.close();

                byte[] pdfBytes = outputStream.toByteArray();
                String originalFileName = currentFile.getOriginalFilename();
                String baseName = originalFileName != null && originalFileName.contains(".")
                        ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                        : "image";
                String fileName = String.format("%s_%s.pdf", baseName, pdfResultFactory.timestamp());

                log.info("Successfully converted image '{}' to PDF ({} bytes)", originalFileName, pdfBytes.length);

                results.add(PdfResult.builder()
                        .content(pdfBytes)
                        .suggestedFileName(fileName)
                        .sizeInBytes(pdfBytes.length)
                        .pageCount(pageCount)
                        .build());
            }

            log.info("Successfully converted {} images to PDF", results.size());
            return results;

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed while converting image to PDF", e);
            throw new PdfErrorException("Failed to convert image to PDF: " + e.getMessage(), e);
        }
    }

    public byte[] toImages(MultipartFile file, String format, Integer dpi,
                           Integer startPage, Integer endPage) {
        String fmt = (format != null && format.equalsIgnoreCase("jpg")) ? "jpg" : "png";
        int resolvedDpi = (dpi != null && dpi > 0) ? dpi : 150;

        try {
            PDDocument document = pdfIoSupport.loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageRangeResolver.PageBounds bounds = pageRangeResolver.resolve(startPage, endPage, totalPages);
            int start = bounds.start() - 1;
            int end = bounds.end() - 1;

            PDFRenderer renderer = new PDFRenderer(document);
            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
                String imageFormat = "jpg".equals(fmt) ? "jpeg" : "png";
                for (int i = start; i <= end; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(i, resolvedDpi, ImageType.RGB);
                    ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
                    ImageIO.write(image, imageFormat, imgOut);
                    zipSupport.addToZip(zos, String.format("page_%d.%s", i + 1, fmt), imgOut.toByteArray());
                }
            }

            document.close();
            log.info("Successfully converted {} pages to {} images at {} DPI", end - start + 1, fmt, resolvedDpi);
            return zipOut.toByteArray();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert PDF to images", e);
            throw new PdfErrorException("Failed to convert PDF to images: " + e.getMessage(), e);
        }
    }

    public byte[] extractImages(MultipartFile file, Integer minWidth, Integer minHeight) {
        try {
            PDDocument document = pdfIoSupport.loadPdfBoxDocument(file);

            ByteArrayOutputStream zipOut = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(zipOut)) {
                int pageNum = 0;
                for (PDPage page : document.getPages()) {
                    pageNum++;
                    extractImagesFromPage(page, pageNum, minWidth, minHeight, zos);
                }
            }

            document.close();
            log.info("Successfully extracted images from PDF");
            return zipOut.toByteArray();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract images from PDF", e);
            throw new PdfErrorException("Failed to extract images from PDF: " + e.getMessage(), e);
        }
    }

    private void extractImagesFromPage(PDPage page, int pageNum, Integer minWidth, Integer minHeight,
                                       ZipOutputStream zos) throws IOException {
        PDResources resources = page.getResources();
        int imageIndex = 0;
        for (var xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);
            if (xObject instanceof PDImageXObject imageXObject
                    && meetsMinDimensions(imageXObject, minWidth, minHeight)) {
                writeImageToZip(imageXObject, pageNum, imageIndex, zos);
                imageIndex++;
            }
        }
    }

    private boolean meetsMinDimensions(PDImageXObject image, Integer minWidth, Integer minHeight) {
        return (minWidth == null || image.getWidth() >= minWidth)
                && (minHeight == null || image.getHeight() >= minHeight);
    }

    private void writeImageToZip(PDImageXObject imageXObject, int pageNum, int index,
                                 ZipOutputStream zos) throws IOException {
        String suffix = imageXObject.getSuffix() != null ? imageXObject.getSuffix() : "png";
        String formatName = suffix.equalsIgnoreCase("jpg") ? "jpeg" : suffix;
        BufferedImage image = imageXObject.getImage();
        ByteArrayOutputStream imgOut = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, imgOut);
        zipSupport.addToZip(zos, String.format("image_page%d_%d.%s", pageNum, index, suffix), imgOut.toByteArray());
    }
}
