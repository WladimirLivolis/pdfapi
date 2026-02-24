package com.pdf.pdfapi.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.pdf.pdfapi.dto.EncryptRequest;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.dto.WatermarkRequest;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.core.PdfDocumentOpsService;
import com.pdf.pdfapi.service.core.PdfOptimizationService;
import com.pdf.pdfapi.service.form.PdfFormService;
import com.pdf.pdfapi.service.image.PdfImageService;
import com.pdf.pdfapi.service.metadata.PdfMetadataService;
import com.pdf.pdfapi.service.ocr.PdfOcrService;
import com.pdf.pdfapi.service.security.PdfProtectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfService {

    private final PdfOcrService pdfOcrService;
    private final PdfImageService pdfImageService;
    private final PdfFormService pdfFormService;
    private final PdfMetadataService pdfMetadataService;
    private final PdfProtectionService pdfProtectionService;
    private final PdfOptimizationService pdfOptimizationService;
    private final PdfDocumentOpsService pdfDocumentOpsService;

    public PdfResult merge(MultipartFile... file) {
        return pdfDocumentOpsService.merge(file);
    }

    public PdfResult merge(Boolean createBookmarks, MultipartFile... file) {
        return pdfDocumentOpsService.merge(createBookmarks, file);
    }

    public List<PdfResult> split(MultipartFile file, Integer maxPageCount) {
        return pdfDocumentOpsService.split(file, maxPageCount);
    }

    public PdfResult extract(MultipartFile file, Integer startPage, Integer endPage) {
        return pdfDocumentOpsService.extract(file, startPage, endPage);
    }

    public PdfResult remove(MultipartFile file, Integer... page) {
        return pdfDocumentOpsService.remove(file, page);
    }

    public List<PdfResult> convertImageToPDF(MultipartFile... file) {
        return pdfImageService.convertImageToPDF(file);
    }

    public PdfResult rotate(MultipartFile file, Integer rotation, Integer... pages) {
        return pdfDocumentOpsService.rotate(file, rotation, pages);
    }

    public PdfInfoResponse getInfo(MultipartFile file) {
        return pdfMetadataService.getInfo(file);
    }

    public PdfMetadataResponse getMetadata(MultipartFile file) {
        return pdfMetadataService.getMetadata(file);
    }

    public PdfResult updateMetadata(MultipartFile file, PdfMetadataRequest metadata) {
        return pdfMetadataService.updateMetadata(file, metadata);
    }

    public PdfResult addPageNumbers(MultipartFile file, String position, String format,
                                    Integer startPage, Integer endPage) {
        String pos = getPositionWithDefault(position);
        String fmt = getFormatWithDefault(format);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageBounds bounds = resolvePageRange(startPage, endPage, totalPages);

            PdfFont font = PdfFontFactory.createFont();
            addPageNumbersToPdf(pdfDocument, font, pos, fmt, bounds.start(), bounds.end(), totalPages);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully added page numbers to {} pages ({} bytes)", bounds.end() - bounds.start() + 1, pdfBytes.length);
            return buildPdfResult(pdfBytes, "numbered", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add page numbers", e);
            throw new PdfErrorException("Failed to add page numbers: " + e.getMessage(), e);
        }
    }

    private String getPositionWithDefault(String position) {
        return (position != null && !position.isBlank()) ? position : "bottom-center";
    }

    private String getFormatWithDefault(String format) {
        return (format != null && !format.isBlank()) ? format : "Page {current} of {total}";
    }

    private int getValidStartPage(Integer startPage) {
        return (startPage != null && startPage >= 1) ? startPage : 1;
    }

    private int getValidEndPage(Integer endPage, int totalPages) {
        return (endPage != null && endPage <= totalPages) ? endPage : totalPages;
    }

    private PageBounds resolvePageRange(Integer startPage, Integer endPage, int totalPages) {
        int start = getValidStartPage(startPage);
        int end = getValidEndPage(endPage, totalPages);
        if (start > end) {
            throw new PdfErrorException("startPage must be less than or equal to endPage");
        }
        return new PageBounds(start, end);
    }

    private void addPageNumbersToPdf(PdfDocument pdfDocument, PdfFont font, String position,
                                      String format, int start, int end, int totalPages) {
        PageNumberPosition positionConfig = parsePosition(position);

        for (int i = start; i <= end; i++) {
            PdfPage page = pdfDocument.getPage(i);
            Rectangle pageSize = page.getPageSize();

            String pageNumberText = formatPageNumberText(format, i, totalPages);
            PageCoordinates coordinates = calculateCoordinates(pageSize, positionConfig);

            drawPageNumber(page, pageSize, pageNumberText, coordinates, font);
        }
    }

    private PageNumberPosition parsePosition(String position) {
        String[] posParts = position.toLowerCase().split("-");
        String vertical = posParts.length > 0 ? posParts[0] : "bottom";
        String horizontal = posParts.length > 1 ? posParts[1] : "center";
        return new PageNumberPosition(vertical, horizontal);
    }

    private String formatPageNumberText(String format, int currentPage, int totalPages) {
        return format
                .replace("{current}", String.valueOf(currentPage))
                .replace("{total}", String.valueOf(totalPages))
                .replace("{page}", String.valueOf(currentPage));
    }

    private PageCoordinates calculateCoordinates(Rectangle pageSize, PageNumberPosition position) {
        float y = position.isTopPosition()
                ? pageSize.getTop() - 30
                : pageSize.getBottom() + 20;

        TextAlignment alignment;
        float x;

        if (position.isLeftPosition()) {
            x = pageSize.getLeft() + 50;
            alignment = TextAlignment.LEFT;
        } else if (position.isRightPosition()) {
            x = pageSize.getRight() - 50;
            alignment = TextAlignment.RIGHT;
        } else {
            x = (pageSize.getLeft() + pageSize.getRight()) / 2;
            alignment = TextAlignment.CENTER;
        }

        return new PageCoordinates(x, y, alignment);
    }

    private void drawPageNumber(PdfPage page, Rectangle pageSize, String pageNumberText,
                               PageCoordinates coordinates, PdfFont font) {
        PdfCanvas pdfCanvas = new PdfCanvas(page);
        Canvas canvas = new Canvas(pdfCanvas, pageSize);
        Paragraph paragraph = new Paragraph(pageNumberText)
                .setFont(font)
                .setFontSize(10)
                .setFontColor(ColorConstants.BLACK);

        canvas.showTextAligned(paragraph, coordinates.x(), coordinates.y(), coordinates.alignment());
        canvas.close();
    }

    private static class PageNumberPosition {
        private final String vertical;
        private final String horizontal;

        PageNumberPosition(String vertical, String horizontal) {
            this.vertical = vertical;
            this.horizontal = horizontal;
        }

        boolean isTopPosition() {
            return "top".equals(vertical);
        }

        boolean isLeftPosition() {
            return "left".equals(horizontal);
        }

        boolean isRightPosition() {
            return "right".equals(horizontal);
        }
    }

    private record PageCoordinates(float x, float y, TextAlignment alignment) {
    }

    public PdfResult compress(MultipartFile file, String level) {
        return pdfOptimizationService.compress(file, level);
    }

    public PdfResult encrypt(MultipartFile file, EncryptRequest request) {
        return pdfProtectionService.encrypt(file, request);
    }

    public PdfResult decrypt(MultipartFile file, String password) {
        return pdfProtectionService.decrypt(file, password);
    }

    public PdfResult optimize(MultipartFile file) {
        return pdfOptimizationService.optimize(file);
    }

    public PdfResult watermark(MultipartFile file, MultipartFile imageFile, WatermarkRequest request) {
        validateWatermarkInput(request.text(), imageFile);

        WatermarkConfig config = new WatermarkConfig(
                request.position(), request.opacity(), request.rotation(), request.scale(), request.layer());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageBounds pageRange = resolvePageRange(request.startPage(), request.endPage(), totalPages);

            applyWatermark(pdfDocument, request.text(), imageFile, config, pageRange);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully added watermark to {} pages ({} bytes)",
                    pageRange.end() - pageRange.start() + 1, pdfBytes.length);
            return buildPdfResult(pdfBytes, "watermarked", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add watermark", e);
            throw new PdfErrorException("Failed to add watermark: " + e.getMessage(), e);
        }
    }

    private void validateWatermarkInput(String text, MultipartFile imageFile) {
        if ((text == null || text.isBlank()) && imageFile == null) {
            throw new PdfErrorException("Either text or image must be provided for watermark");
        }
        if (text != null && imageFile != null) {
            throw new PdfErrorException("Please provide either text OR image, not both");
        }
    }

    private void applyWatermark(PdfDocument pdfDocument, String text, MultipartFile imageFile,
                                WatermarkConfig config, PageBounds pageRange) throws IOException {
        if (text != null) {
            applyTextWatermark(pdfDocument, text, config, pageRange);
        } else {
            applyImageWatermark(pdfDocument, imageFile, config, pageRange);
        }
    }

    private PdfCanvas createLayeredCanvas(PdfPage page, String layer) {
        if ("background".equals(layer)) {
            return new PdfCanvas(page.newContentStreamBefore(), page.getResources(), page.getDocument());
        }
        return new PdfCanvas(page);
    }

    private void setupWatermarkCanvas(PdfCanvas pdfCanvas, WatermarkConfig config, WatermarkPosition pos) {
        pdfCanvas.saveState();
        pdfCanvas.setExtGState(createExtGraphicsState(config.opacity()));

        double rotationRadians = Math.toRadians(config.rotation());
        double cos = Math.cos(rotationRadians);
        double sin = Math.sin(rotationRadians);

        pdfCanvas.concatMatrix(1, 0, 0, 1, pos.x(), pos.y());
        pdfCanvas.concatMatrix(cos, sin, -sin, cos, 0, 0);
    }

    private record WatermarkConfig(String position, float opacity, float rotation,
                                    float scale, String layer) {
    }

    private record PageBounds(int start, int end) {
    }

    private void applyTextWatermark(PdfDocument pdfDocument, String text,
                                    WatermarkConfig config, PageBounds pageRange) throws IOException {
        PdfFont font = PdfFontFactory.createFont();

        for (int i = pageRange.start(); i <= pageRange.end(); i++) {
            PdfPage page = pdfDocument.getPage(i);
            Rectangle pageSize = page.getPageSize();
            WatermarkPosition watermarkPos = calculateWatermarkPosition(pageSize, config.position());
            PdfCanvas pdfCanvas = createLayeredCanvas(page, config.layer());

            setupWatermarkCanvas(pdfCanvas, config, watermarkPos);
            pdfCanvas.setFillColor(ColorConstants.LIGHT_GRAY);
            pdfCanvas.beginText();
            pdfCanvas.setFontAndSize(font, 60 * config.scale());
            pdfCanvas.showText(text);
            pdfCanvas.endText();
            pdfCanvas.restoreState();
        }
    }

    private void applyImageWatermark(PdfDocument pdfDocument, MultipartFile imageFile,
                                     WatermarkConfig config, PageBounds pageRange) throws IOException {
        ImageData imageData = ImageDataFactory.create(imageFile.getBytes());
        Image image = new Image(imageData);

        for (int i = pageRange.start(); i <= pageRange.end(); i++) {
            PdfPage page = pdfDocument.getPage(i);
            Rectangle pageSize = page.getPageSize();
            WatermarkPosition watermarkPos = calculateWatermarkPosition(pageSize, config.position());
            PdfCanvas pdfCanvas = createLayeredCanvas(page, config.layer());

            setupWatermarkCanvas(pdfCanvas, config, watermarkPos);

            float imgWidth = image.getImageWidth() * config.scale();
            float imgHeight = image.getImageHeight() * config.scale();

            image.scaleToFit(imgWidth, imgHeight);
            image.setFixedPosition(0, 0);

            Canvas canvas = new Canvas(pdfCanvas, pageSize);
            canvas.add(image);
            canvas.close();

            pdfCanvas.restoreState();
        }
    }

    private WatermarkPosition calculateWatermarkPosition(Rectangle pageSize, String position) {
        float x;

        float y = switch (position.toLowerCase()) {
            case "top-left" -> {
                x = pageSize.getLeft() + 50;
                yield pageSize.getTop() - 100;
            }
            case "top-center" -> {
                x = (pageSize.getLeft() + pageSize.getRight()) / 2;
                yield pageSize.getTop() - 100;
            }
            case "top-right" -> {
                x = pageSize.getRight() - 50;
                yield pageSize.getTop() - 100;
            }
            case "center-left" -> {
                x = pageSize.getLeft() + 50;
                yield (pageSize.getBottom() + pageSize.getTop()) / 2;
            }
            case "center-right" -> {
                x = pageSize.getRight() - 50;
                yield (pageSize.getBottom() + pageSize.getTop()) / 2;
            }
            case "bottom-left" -> {
                x = pageSize.getLeft() + 50;
                yield pageSize.getBottom() + 100;
            }
            case "bottom-center" -> {
                x = (pageSize.getLeft() + pageSize.getRight()) / 2;
                yield pageSize.getBottom() + 100;
            }
            case "bottom-right" -> {
                x = pageSize.getRight() - 50;
                yield pageSize.getBottom() + 100;
            }
            default -> {
                x = (pageSize.getLeft() + pageSize.getRight()) / 2;
                yield (pageSize.getBottom() + pageSize.getTop()) / 2;
            }
        };

        return new WatermarkPosition(x, y);
    }

    private PdfExtGState createExtGraphicsState(float opacity) {
        PdfExtGState extGState = new PdfExtGState();
        extGState.setFillOpacity(opacity);
        extGState.setStrokeOpacity(opacity);
        return extGState;
    }

    private record WatermarkPosition(float x, float y) {
    }

    public byte[] toImages(MultipartFile file, String format, Integer dpi,
                            Integer startPage, Integer endPage) {
        return pdfImageService.toImages(file, format, dpi, startPage, endPage);
    }

    public byte[] extractImages(MultipartFile file, Integer minWidth, Integer minHeight) {
        return pdfImageService.extractImages(file, minWidth, minHeight);
    }

    public PdfResult crop(MultipartFile file, Float x, Float y, Float width, Float height,
                           Integer startPage, Integer endPage) {
        return pdfDocumentOpsService.crop(file, x, y, width, height, startPage, endPage);
    }

    public PdfResult fillForm(MultipartFile file, Map<String, String> fields, Boolean flatten) {
        return pdfFormService.fillForm(file, fields, flatten);
    }

    public byte[] ocrToText(MultipartFile file, String language, Integer startPage, Integer endPage) {
        return pdfOcrService.ocrToText(file, language, startPage, endPage);
    }

    public byte[] ocrToText(MultipartFile file, String language, Integer startPage, Integer endPage, Integer dpi) {
        return pdfOcrService.ocrToText(file, language, startPage, endPage, dpi);
    }

    public PdfResult ocrToPdf(MultipartFile file, String language, Integer startPage, Integer endPage, Integer dpi) {
        return pdfOcrService.ocrToPdf(file, language, startPage, endPage, dpi);
    }

    private PdfReader toPdfReader(MultipartFile file) throws IOException {
        return new PdfReader(new ByteArrayInputStream(file.getBytes()));
    }

    private PDDocument loadPdfBoxDocument(MultipartFile file) throws IOException {
        return Loader.loadPDF(file.getBytes());
    }

    private PdfResult buildPdfResult(byte[] pdfBytes, String fileNamePrefix, int pageCount) {
        return PdfResult.builder()
                .content(pdfBytes)
                .suggestedFileName(String.format("%s_%s.pdf", fileNamePrefix, timestamp()))
                .sizeInBytes(pdfBytes.length)
                .pageCount(pageCount)
                .build();
    }

    private void addToZip(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        zos.write(data);
        zos.closeEntry();
    }

    private String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return time.format(formatter);
    }

}
