package com.pdf.pdfapi.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.pdf.pdfapi.config.PdfConfig;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfService {

    private final PdfConfig pdfConfig;

    public PdfResult merge(MultipartFile... file) {
        if (file.length < 2) {
            String errorMsg = "Merge needs at least 2 documents";
            log.error(errorMsg);
            throw new PdfErrorException(errorMsg);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file[0].getBytes())),
                    new PdfWriter(outputStream)
            );
            PdfMerger merger = new PdfMerger(pdfDocument);

            for (int i = 1; i < file.length; i++) {
                PdfDocument pdfDocument2 = new PdfDocument(new PdfReader(new ByteArrayInputStream(file[i].getBytes())));
                merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());
                pdfDocument2.close();
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("merged_%s.pdf", timestamp());

            log.info("Successfully merged {} files into {} ({} pages, {} bytes)",
                    file.length, fileName, pageCount, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(pageCount)
                    .build();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed while merging files", e);
            throw new PdfErrorException("Failed to merge PDF files: " + e.getMessage(), e);
        }
    }

    public List<PdfResult> split(MultipartFile file, Integer maxPageCount) {
        if (maxPageCount == null || maxPageCount < 1) {
            throw new PdfErrorException("maxPageCount must be at least 1");
        }

        try {
            List<PdfResult> results = new ArrayList<>();
            List<ByteArrayOutputStream> outputStreams = new ArrayList<>();

            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));
            String baseFileName = String.format("splitDocument_%s_", timestamp());

            PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    outputStreams.add(outputStream);
                    return new PdfWriter(outputStream);
                }
            };

            List<PdfDocument> splitDocuments = pdfSplitter.splitByPageCount(maxPageCount);
            pdfDocument.close();

            // Convert each split document to PdfResult
            for (int i = 0; i < splitDocuments.size(); i++) {
                PdfDocument doc = splitDocuments.get(i);
                int pageCount = doc.getNumberOfPages();
                doc.close();

                byte[] pdfBytes = outputStreams.get(i).toByteArray();
                String fileName = String.format("%s%d.pdf", baseFileName, i + 1);

                results.add(PdfResult.builder()
                        .content(pdfBytes)
                        .suggestedFileName(fileName)
                        .sizeInBytes(pdfBytes.length)
                        .pageCount(pageCount)
                        .build());
            }

            log.info("Successfully split PDF into {} parts", results.size());
            return results;

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to split file", e);
            throw new PdfErrorException("Failed to split PDF file: " + e.getMessage(), e);
        }
    }

    public PdfResult extract(MultipartFile file, Integer startPage, Integer endPage) {
        if (startPage == null || endPage == null) {
            throw new PdfErrorException("startPage and endPage are required");
        }
        if (startPage < 1 || endPage < 1) {
            throw new PdfErrorException("Page numbers must be at least 1");
        }
        if (startPage > endPage) {
            throw new PdfErrorException("startPage must be less than or equal to endPage");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));

            int totalPages = pdfDocument.getNumberOfPages();
            if (endPage > totalPages) {
                pdfDocument.close();
                throw new PdfErrorException(String.format("endPage (%d) exceeds document page count (%d)", endPage, totalPages));
            }

            PdfSplitter pdfSplitter = new PdfSplitter(pdfDocument) {
                @Override
                protected PdfWriter getNextPdfWriter(PageRange documentPageRange) {
                    return new PdfWriter(outputStream);
                }
            };

            PdfDocument newPdfDocument = pdfSplitter.extractPageRange(new PageRange().addPageSequence(startPage, endPage));
            int pageCount = newPdfDocument.getNumberOfPages();
            newPdfDocument.close();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("extractedPages_%s.pdf", timestamp());

            log.info("Successfully extracted pages {}-{} ({} pages, {} bytes)", startPage, endPage, pageCount, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(pageCount)
                    .build();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to extract from file", e);
            throw new PdfErrorException("Failed to extract pages from PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult remove(MultipartFile file, Integer... page) {
        if (page == null || page.length == 0) {
            throw new PdfErrorException("At least one page number must be specified for removal");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file.getBytes())),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();

            // Validate all page numbers first
            for (Integer pageNumber : page) {
                if (pageNumber == null || pageNumber < 1) {
                    pdfDocument.close();
                    throw new PdfErrorException("Page numbers must be at least 1");
                }
                if (pageNumber > totalPages) {
                    pdfDocument.close();
                    throw new PdfErrorException(String.format("Page number %d exceeds document page count (%d)", pageNumber, totalPages));
                }
            }

            int removeCount = 0;
            for (Integer pageNumber : page) {
                pdfDocument.removePage(pageNumber - removeCount++);
            }

            int finalPageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("removedPages_%s.pdf", timestamp());

            log.info("Successfully removed {} pages ({} pages remaining, {} bytes)", page.length, finalPageCount, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(finalPageCount)
                    .build();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove from file", e);
            throw new PdfErrorException("Failed to remove pages from PDF: " + e.getMessage(), e);
        }
    }

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
                image.setWidth(pdfDocument.getDefaultPageSize().getWidth() - 50); // 50-point margin on each side
                image.setAutoScaleHeight(true);

                document.add(image);
                int pageCount = pdfDocument.getNumberOfPages();
                pdfDocument.close();

                byte[] pdfBytes = outputStream.toByteArray();
                String originalFileName = currentFile.getOriginalFilename();
                String baseName = originalFileName != null && originalFileName.contains(".")
                        ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                        : "image";
                String fileName = String.format("%s_%s.pdf", baseName, timestamp());

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

    public PdfResult rotate(MultipartFile file, Integer rotation, Integer... pages) {
        validateRotationAngle(rotation);
        int normalizedRotation = normalizeRotation(rotation);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file.getBytes())),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();

            if (shouldRotateAllPages(pages)) {
                rotateAllPages(pdfDocument, totalPages, normalizedRotation, rotation);
            } else {
                rotateSpecificPages(pdfDocument, pages, totalPages, normalizedRotation, rotation);
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("rotated_%s.pdf", timestamp());

            log.info("Successfully rotated PDF ({} pages, {} bytes)", pageCount, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(pageCount)
                    .build();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to rotate PDF", e);
            throw new PdfErrorException("Failed to rotate PDF: " + e.getMessage(), e);
        }
    }

    private void validateRotationAngle(Integer rotation) {
        if (rotation == null) {
            throw new PdfErrorException("Rotation angle is required");
        }
        if (rotation % 90 != 0) {
            throw new PdfErrorException("Rotation must be a multiple of 90 degrees");
        }
    }

    private int normalizeRotation(Integer rotation) {
        return ((rotation % 360) + 360) % 360;
    }

    private boolean shouldRotateAllPages(Integer... pages) {
        return pages == null || pages.length == 0;
    }

    private void rotateAllPages(PdfDocument pdfDocument, int totalPages, int normalizedRotation, Integer originalRotation) {
        for (int i = 1; i <= totalPages; i++) {
            rotatePageByIndex(pdfDocument, i, normalizedRotation);
        }
        log.info("Rotating all {} pages by {} degrees", totalPages, originalRotation);
    }

    private void rotateSpecificPages(PdfDocument pdfDocument, Integer[] pages, int totalPages,
                                     int normalizedRotation, Integer originalRotation) {
        validatePageNumbers(pages, totalPages, pdfDocument);

        for (Integer pageNumber : pages) {
            rotatePageByIndex(pdfDocument, pageNumber, normalizedRotation);
        }
        log.info("Rotating {} specific pages by {} degrees", pages.length, originalRotation);
    }

    private void validatePageNumbers(Integer[] pages, int totalPages, PdfDocument pdfDocument) {
        for (Integer pageNumber : pages) {
            if (pageNumber == null || pageNumber < 1) {
                pdfDocument.close();
                throw new PdfErrorException("Page numbers must be at least 1");
            }
            if (pageNumber > totalPages) {
                pdfDocument.close();
                throw new PdfErrorException(String.format("Page number %d exceeds document page count (%d)", pageNumber, totalPages));
            }
        }
    }

    private void rotatePageByIndex(PdfDocument pdfDocument, int pageIndex, int rotationAngle) {
        PdfPage page = pdfDocument.getPage(pageIndex);
        int currentRotation = page.getRotation();
        int newRotation = (currentRotation + rotationAngle) % 360;
        page.setRotation(newRotation);
    }

    public PdfInfoResponse getInfo(MultipartFile file) {
        try {
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));

            int pageCount = pdfDocument.getNumberOfPages();
            long fileSize = file.getSize();
            String pdfVersion = pdfDocument.getPdfVersion().toString();

            // Get first page dimensions
            PdfPage firstPage = pdfDocument.getPage(1);
            Rectangle firstPageSize = firstPage.getPageSize();
            PdfInfoResponse.PageDimensions firstPageDimensions = PdfInfoResponse.PageDimensions.builder()
                    .width(firstPageSize.getWidth())
                    .height(firstPageSize.getHeight())
                    .unit("points")
                    .build();

            // Check if all pages have the same dimensions
            boolean allPagesSameDimension = true;
            for (int i = 2; i <= pageCount; i++) {
                Rectangle pageSize = pdfDocument.getPage(i).getPageSize();
                if (Math.abs(pageSize.getWidth() - firstPageSize.getWidth()) > 0.1f ||
                        Math.abs(pageSize.getHeight() - firstPageSize.getHeight()) > 0.1f) {
                    allPagesSameDimension = false;
                    break;
                }
            }

            pdfDocument.close();

            log.info("Successfully retrieved PDF info: {} pages, {} bytes, version {}", pageCount, fileSize, pdfVersion);

            return PdfInfoResponse.success(pageCount, fileSize, pdfVersion, firstPageDimensions, allPagesSameDimension);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get PDF info", e);
            throw new PdfErrorException("Failed to get PDF info: " + e.getMessage(), e);
        }
    }

    public PdfMetadataResponse getMetadata(MultipartFile file) {
        try {
            PdfDocument pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(file.getBytes())));
            PdfDocumentInfo info = pdfDocument.getDocumentInfo();

            String title = info.getTitle();
            String author = info.getAuthor();
            String subject = info.getSubject();
            String keywords = info.getKeywords();
            String creator = info.getCreator();
            String producer = info.getProducer();

            // Get dates using getMoreInfo() which is public
            String creationDate = info.getMoreInfo(PdfName.CreationDate.getValue());
            String modificationDate = info.getMoreInfo(PdfName.ModDate.getValue());

            pdfDocument.close();

            log.info("Successfully retrieved PDF metadata for file");

            return PdfMetadataResponse.success(title, author, subject, keywords, creator, producer, creationDate, modificationDate);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get PDF metadata", e);
            throw new PdfErrorException("Failed to get PDF metadata: " + e.getMessage(), e);
        }
    }

    public PdfResult updateMetadata(MultipartFile file, PdfMetadataRequest metadata) {
        if (metadata == null) {
            throw new PdfErrorException("Metadata request cannot be null");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file.getBytes())),
                    new PdfWriter(outputStream)
            );
            PdfDocumentInfo info = pdfDocument.getDocumentInfo();

            // Update metadata fields if provided
            if (metadata.title() != null) {
                info.setTitle(metadata.title());
            }
            if (metadata.author() != null) {
                info.setAuthor(metadata.author());
            }
            if (metadata.subject() != null) {
                info.setSubject(metadata.subject());
            }
            if (metadata.keywords() != null) {
                info.setKeywords(metadata.keywords());
            }
            if (metadata.creator() != null) {
                info.setCreator(metadata.creator());
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("metadata_updated_%s.pdf", timestamp());

            log.info("Successfully updated PDF metadata ({} pages, {} bytes)", pageCount, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(pageCount)
                    .build();

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update PDF metadata", e);
            throw new PdfErrorException("Failed to update PDF metadata: " + e.getMessage(), e);
        }
    }

    public PdfResult addPageNumbers(MultipartFile file, String position, String format,
                                    Integer startPage, Integer endPage) {
        String pos = getPositionWithDefault(position);
        String fmt = getFormatWithDefault(format);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(file.getBytes())),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            int start = getValidStartPage(startPage);
            int end = getValidEndPage(endPage, totalPages);

            validatePageRange(start, end, pdfDocument);

            PdfFont font = PdfFontFactory.createFont();
            addPageNumbersToPdf(pdfDocument, font, pos, fmt, start, end, totalPages);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = String.format("numbered_%s.pdf", timestamp());

            log.info("Successfully added page numbers to {} pages ({} bytes)", end - start + 1, pdfBytes.length);

            return PdfResult.builder()
                    .content(pdfBytes)
                    .suggestedFileName(fileName)
                    .sizeInBytes(pdfBytes.length)
                    .pageCount(pageCount)
                    .build();

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

    private void validatePageRange(int start, int end, PdfDocument pdfDocument) {
        if (start > end) {
            pdfDocument.close();
            throw new PdfErrorException("startPage must be less than or equal to endPage");
        }
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

    /**
     * Represents the position configuration for page numbers
     */
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

    /**
     * Represents the coordinates and alignment for drawing page numbers
     */
    private record PageCoordinates(float x, float y, TextAlignment alignment) {
    }

    private String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return time.format(formatter);
    }

}
