package com.pdf.pdfapi.service.annotate;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.dto.WatermarkRequest;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfAnnotationService {

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;
    private final PageRangeResolver pageRangeResolver;

    public PdfResult addPageNumbers(MultipartFile file, String position, String format,
                                    Integer startPage, Integer endPage) {
        String pos = getPositionWithDefault(position);
        String fmt = getFormatWithDefault(format);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    pdfIoSupport.toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageRangeResolver.PageBounds bounds = pageRangeResolver.resolve(startPage, endPage, totalPages);

            PdfFont font = PdfFontFactory.createFont();
            addPageNumbersToPdf(pdfDocument, font, pos, fmt, bounds.start(), bounds.end(), totalPages);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully added page numbers to {} pages ({} bytes)", bounds.end() - bounds.start() + 1, pdfBytes.length);
            return pdfResultFactory.buildPdfResult(pdfBytes, "numbered", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add page numbers", e);
            throw new PdfErrorException("Failed to add page numbers: " + e.getMessage(), e);
        }
    }

    public PdfResult watermark(MultipartFile file, MultipartFile imageFile, WatermarkRequest request) {
        validateWatermarkInput(request.text(), imageFile);

        WatermarkConfig config = new WatermarkConfig(
                request.position(), request.opacity(), request.rotation(), request.scale(), request.layer());

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    pdfIoSupport.toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageRangeResolver.PageBounds pageRange = pageRangeResolver.resolve(request.startPage(), request.endPage(), totalPages);

            applyWatermark(pdfDocument, request.text(), imageFile, config, pageRange);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully added watermark to {} pages ({} bytes)",
                    pageRange.end() - pageRange.start() + 1, pdfBytes.length);
            return pdfResultFactory.buildPdfResult(pdfBytes, "watermarked", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to add watermark", e);
            throw new PdfErrorException("Failed to add watermark: " + e.getMessage(), e);
        }
    }

    private String getPositionWithDefault(String position) {
        return (position != null && !position.isBlank()) ? position : "bottom-center";
    }

    private String getFormatWithDefault(String format) {
        return (format != null && !format.isBlank()) ? format : "Page {current} of {total}";
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

    private void validateWatermarkInput(String text, MultipartFile imageFile) {
        if ((text == null || text.isBlank()) && imageFile == null) {
            throw new PdfErrorException("Either text or image must be provided for watermark");
        }
        if (text != null && imageFile != null) {
            throw new PdfErrorException("Please provide either text OR image, not both");
        }
    }

    private void applyWatermark(PdfDocument pdfDocument, String text, MultipartFile imageFile,
                                WatermarkConfig config, PageRangeResolver.PageBounds pageRange) throws IOException {
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

    private void applyTextWatermark(PdfDocument pdfDocument, String text,
                                    WatermarkConfig config, PageRangeResolver.PageBounds pageRange) throws IOException {
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
                                     WatermarkConfig config, PageRangeResolver.PageBounds pageRange) throws IOException {
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

    private record WatermarkConfig(String position, float opacity, float rotation, float scale, String layer) {
    }

    private record WatermarkPosition(float x, float y) {
    }
}
