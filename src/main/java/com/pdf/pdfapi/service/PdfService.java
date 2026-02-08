package com.pdf.pdfapi.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.pdf.pdfapi.dto.EncryptRequest;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.dto.WatermarkRequest;
import com.pdf.pdfapi.exception.PdfErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfService {

    private final TesseractFactory tesseractFactory;

    private static final String LOW_COMPRESSION_LEVEL = "LOW";
    private static final String MEDIUM_COMPRESSION_LEVEL = "MEDIUM";
    private static final String HIGH_COMPRESSION_LEVEL = "HIGH";

    public PdfResult merge(MultipartFile... file) {
        return merge(false, file);
    }

    public PdfResult merge(Boolean createBookmarks, MultipartFile... file) {
        if (file.length < 2) {
            String errorMsg = "Merge needs at least 2 documents";
            log.error(errorMsg);
            throw new PdfErrorException(errorMsg);
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            PdfDocument pdfDocument = new PdfDocument(
                    toPdfReader(file[0]),
                    new PdfWriter(outputStream)
            );
            PdfMerger merger = new PdfMerger(pdfDocument);

            boolean addBookmarks = Boolean.TRUE.equals(createBookmarks);
            PdfOutline rootOutline = addBookmarks ? pdfDocument.getOutlines(true) : null;

            if (addBookmarks && rootOutline != null) {
                String firstName = file[0].getOriginalFilename() != null ? file[0].getOriginalFilename() : "Document 1";
                PdfOutline outline = rootOutline.addOutline(firstName);
                outline.addDestination(PdfDestination.makeDestination(pdfDocument.getPage(1).getPdfObject()));
            }

            for (int i = 1; i < file.length; i++) {
                PdfDocument pdfDocument2 = new PdfDocument(toPdfReader(file[i]));
                int prevCount = pdfDocument.getNumberOfPages();
                merger.merge(pdfDocument2, 1, pdfDocument2.getNumberOfPages());
                pdfDocument2.close();

                if (addBookmarks && rootOutline != null) {
                    String name = file[i].getOriginalFilename() != null ? file[i].getOriginalFilename() : "Document " + (i + 1);
                    PdfOutline outline = rootOutline.addOutline(name);
                    outline.addDestination(PdfDestination.makeDestination(pdfDocument.getPage(prevCount + 1).getPdfObject()));
                }
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully merged {} files into {} pages ({} bytes)", file.length, pageCount, pdfBytes.length);
            return buildPdfResult(pdfBytes, "merged", pageCount);

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

            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file));
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
            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file));

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
            log.info("Successfully extracted pages {}-{} ({} pages, {} bytes)", startPage, endPage, pageCount, pdfBytes.length);
            return buildPdfResult(pdfBytes, "extractedPages", pageCount);

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
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            validatePageNumbers(page, totalPages, pdfDocument);

            int removeCount = 0;
            for (Integer pageNumber : page) {
                pdfDocument.removePage(pageNumber - removeCount++);
            }

            int finalPageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully removed {} pages ({} pages remaining, {} bytes)", page.length, finalPageCount, pdfBytes.length);
            return buildPdfResult(pdfBytes, "removedPages", finalPageCount);

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
                    toPdfReader(file),
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
            log.info("Successfully rotated PDF ({} pages, {} bytes)", pageCount, pdfBytes.length);
            return buildPdfResult(pdfBytes, "rotated", pageCount);

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
            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file));

            int pageCount = pdfDocument.getNumberOfPages();
            long fileSize = file.getSize();
            String pdfVersion = pdfDocument.getPdfVersion().toString();

            PdfPage firstPage = pdfDocument.getPage(1);
            Rectangle firstPageSize = firstPage.getPageSize();
            PdfInfoResponse.PageDimensions firstPageDimensions = PdfInfoResponse.PageDimensions.builder()
                    .width(firstPageSize.getWidth())
                    .height(firstPageSize.getHeight())
                    .unit("points")
                    .build();

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
            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file));
            PdfDocumentInfo info = pdfDocument.getDocumentInfo();

            String title = info.getTitle();
            String author = info.getAuthor();
            String subject = info.getSubject();
            String keywords = info.getKeywords();
            String creator = info.getCreator();
            String producer = info.getProducer();

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
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );
            PdfDocumentInfo info = pdfDocument.getDocumentInfo();

            if (metadata.title() != null) info.setTitle(metadata.title());
            if (metadata.author() != null) info.setAuthor(metadata.author());
            if (metadata.subject() != null) info.setSubject(metadata.subject());
            if (metadata.keywords() != null) info.setKeywords(metadata.keywords());
            if (metadata.creator() != null) info.setCreator(metadata.creator());

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully updated PDF metadata ({} pages, {} bytes)", pageCount, pdfBytes.length);
            return buildPdfResult(pdfBytes, "metadata_updated", pageCount);

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
        String compressionLevel = validateAndNormalizeCompressionLevel(level);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WriterProperties writerProperties = configureCompressionWriter(compressionLevel);

            PdfWriter writer = new PdfWriter(outputStream, writerProperties);
            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file), writer);

            applyCompressionToPages(pdfDocument, compressionLevel);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            logCompressionResult(compressionLevel, file.getSize(), pdfBytes.length, pageCount);
            return buildPdfResult(pdfBytes, "compressed_" + compressionLevel.toLowerCase(), pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to compress PDF", e);
            throw new PdfErrorException("Failed to compress PDF: " + e.getMessage(), e);
        }
    }

    private String validateAndNormalizeCompressionLevel(String level) {
        String normalized = (level != null && !level.isBlank()) ? level.toUpperCase() : MEDIUM_COMPRESSION_LEVEL;
        if (!normalized.matches("LOW|MEDIUM|HIGH")) {
            throw new PdfErrorException("Invalid compression level. Must be LOW, MEDIUM, or HIGH");
        }
        return normalized;
    }

    private WriterProperties configureCompressionWriter(String compressionLevel) {
        WriterProperties writerProperties = new WriterProperties();
        if (MEDIUM_COMPRESSION_LEVEL.equals(compressionLevel) || HIGH_COMPRESSION_LEVEL.equals(compressionLevel)) {
            writerProperties.setFullCompressionMode(true);
        }
        return writerProperties;
    }

    private void applyCompressionToPages(PdfDocument pdfDocument, String compressionLevel) {
        if (LOW_COMPRESSION_LEVEL.equals(compressionLevel)) {
            return;
        }
        int totalPages = pdfDocument.getNumberOfPages();
        for (int i = 1; i <= totalPages; i++) {
            compressPageResources(pdfDocument.getPage(i));
        }
    }

    private void logCompressionResult(String level, long originalSize, long compressedSize, int pageCount) {
        double reductionPercentage = ((originalSize - compressedSize) / (double) originalSize) * 100;
        log.info("Successfully compressed PDF with {} level: {} bytes -> {} bytes ({} % reduction, {} pages)",
                level, originalSize, compressedSize, String.format("%.2f", reductionPercentage), pageCount);
    }

    private void compressPageResources(PdfPage page) {
        try {
            PdfDictionary resources = page.getPdfObject().getAsDictionary(PdfName.Resources);
            if (resources == null) return;
            PdfDictionary xObject = resources.getAsDictionary(PdfName.XObject);
            if (xObject != null) {
                compressXObjectImages(xObject);
            }
        } catch (Exception e) {
            log.warn("Failed to compress resources on page, continuing: {}", e.getMessage());
        }
    }

    private void compressXObjectImages(PdfDictionary xObject) {
        for (PdfName imgName : xObject.keySet()) {
            PdfStream stream = xObject.getAsStream(imgName);
            if (stream != null && PdfName.Image.equals(stream.get(PdfName.Subtype))) {
                compressImageStream(stream);
            }
        }
    }

    private void compressImageStream(PdfStream imageStream) {
        try {
            PdfObject filter = imageStream.get(PdfName.Filter);
            if (!PdfName.DCTDecode.equals(filter)) {
                imageStream.put(PdfName.Filter, PdfName.FlateDecode);
                imageStream.setModified();
            }
        } catch (Exception e) {
            log.warn("Failed to compress image stream: {}", e.getMessage());
        }
    }

    public PdfResult encrypt(MultipartFile file, EncryptRequest request) {
        validatePasswordsProvided(request.userPassword(), request.ownerPassword());

        EncryptionConfig config = buildEncryptionConfig(request);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WriterProperties writerProperties = createEncryptedWriter(config);

            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file), new PdfWriter(outputStream, writerProperties));

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully encrypted PDF with {} encryption ({} pages)",
                    getEncryptionTypeName(config.encryptionType()), pageCount);
            return buildPdfResult(pdfBytes, "encrypted", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to encrypt PDF", e);
            throw new PdfErrorException("Failed to encrypt PDF: " + e.getMessage(), e);
        }
    }

    private void validatePasswordsProvided(String userPassword, String ownerPassword) {
        if ((userPassword == null || userPassword.isBlank()) &&
            (ownerPassword == null || ownerPassword.isBlank())) {
            throw new PdfErrorException("At least one password (user or owner) must be provided");
        }
    }

    private EncryptionConfig buildEncryptionConfig(EncryptRequest request) {
        String userPwd = (request.userPassword() != null && !request.userPassword().isBlank()) ? request.userPassword() : "";
        String ownerPwd = (request.ownerPassword() != null && !request.ownerPassword().isBlank()) ? request.ownerPassword() : "";
        int encType = (request.encryptionType() != null) ? request.encryptionType() : EncryptionConstants.ENCRYPTION_AES_256;

        validateEncryptionType(encType);

        int permissions = calculatePermissions(request.allowPrinting(), request.allowModifying(), request.allowCopy(), request.allowAnnotations());

        return new EncryptionConfig(userPwd, ownerPwd, encType, permissions);
    }

    private void validateEncryptionType(int encryptionType) {
        if (encryptionType != EncryptionConstants.STANDARD_ENCRYPTION_40 &&
            encryptionType != EncryptionConstants.STANDARD_ENCRYPTION_128 &&
            encryptionType != EncryptionConstants.ENCRYPTION_AES_128 &&
            encryptionType != EncryptionConstants.ENCRYPTION_AES_256) {
            throw new PdfErrorException("Invalid encryption type. Use 40, 128, 256, or AES256");
        }
    }

    private int calculatePermissions(Boolean allowPrinting, Boolean allowModifying,
                                     Boolean allowCopy, Boolean allowAnnotations) {
        int permissions = 0;
        if (Boolean.TRUE.equals(allowPrinting)) permissions |= EncryptionConstants.ALLOW_PRINTING;
        if (Boolean.TRUE.equals(allowModifying)) permissions |= EncryptionConstants.ALLOW_MODIFY_CONTENTS;
        if (Boolean.TRUE.equals(allowCopy)) permissions |= EncryptionConstants.ALLOW_COPY;
        if (Boolean.TRUE.equals(allowAnnotations)) permissions |= EncryptionConstants.ALLOW_MODIFY_ANNOTATIONS;
        return permissions;
    }

    private WriterProperties createEncryptedWriter(EncryptionConfig config) {
        WriterProperties writerProperties = new WriterProperties();
        writerProperties.setStandardEncryption(
                config.userPassword().getBytes(),
                config.ownerPassword().getBytes(),
                config.permissions(),
                config.encryptionType()
        );
        return writerProperties;
    }

    private record EncryptionConfig(String userPassword, String ownerPassword,
                                     int encryptionType, int permissions) {
    }

    public PdfResult decrypt(MultipartFile file, String password) {
        if (password == null || password.isBlank()) {
            throw new PdfErrorException("Password is required to decrypt PDF");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ReaderProperties readerProperties = new ReaderProperties();
            readerProperties.setPassword(password.getBytes());

            PdfReader reader = new PdfReader(new ByteArrayInputStream(file.getBytes()), readerProperties);
            PdfDocument pdfDocument = new PdfDocument(reader, new PdfWriter(outputStream));

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully decrypted PDF ({} pages)", pageCount);
            return buildPdfResult(pdfBytes, "decrypted", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decrypt PDF", e);
            throw new PdfErrorException("Failed to decrypt PDF. Check if password is correct: " + e.getMessage(), e);
        }
    }

    private String getEncryptionTypeName(int encryptionType) {
        return switch (encryptionType) {
            case EncryptionConstants.STANDARD_ENCRYPTION_40 -> "40-bit";
            case EncryptionConstants.STANDARD_ENCRYPTION_128 -> "128-bit";
            case EncryptionConstants.ENCRYPTION_AES_128 -> "AES-128";
            case EncryptionConstants.ENCRYPTION_AES_256 -> "AES-256";
            default -> "Unknown";
        };
    }

    public PdfResult optimize(MultipartFile file) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            WriterProperties writerProperties = new WriterProperties();
            writerProperties.setFullCompressionMode(true);
            writerProperties.addXmpMetadata();

            PdfDocument pdfDocument = new PdfDocument(toPdfReader(file), new PdfWriter(outputStream, writerProperties));
            pdfDocument.setFlushUnusedObjects(true);

            int pageCount = pdfDocument.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                pdfDocument.getPage(i).flush();
            }

            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            double reductionPercentage = ((file.getSize() - pdfBytes.length) / (double) file.getSize()) * 100;
            log.info("Successfully optimized PDF: {} bytes -> {} bytes ({} % reduction, {} pages)",
                    file.getSize(), pdfBytes.length, String.format("%.2f", reductionPercentage), pageCount);
            return buildPdfResult(pdfBytes, "optimized", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to optimize PDF", e);
            throw new PdfErrorException("Failed to optimize PDF: " + e.getMessage(), e);
        }
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
        String fmt = (format != null && format.equalsIgnoreCase("jpg")) ? "jpg" : "png";
        int resolvedDpi = (dpi != null && dpi > 0) ? dpi : 150;

        try {
            PDDocument document = loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageBounds bounds = resolvePageRange(startPage, endPage, totalPages);
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
                    addToZip(zos, String.format("page_%d.%s", i + 1, fmt), imgOut.toByteArray());
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
            PDDocument document = loadPdfBoxDocument(file);

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
        addToZip(zos, String.format("image_page%d_%d.%s", pageNum, index, suffix), imgOut.toByteArray());
    }

    public PdfResult crop(MultipartFile file, Float x, Float y, Float width, Float height,
                           Integer startPage, Integer endPage) {
        if (x == null || y == null || width == null || height == null) {
            throw new PdfErrorException("x, y, width and height are required for crop");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageBounds bounds = resolvePageRange(startPage, endPage, totalPages);

            Rectangle cropBox = new Rectangle(x, y, width, height);
            for (int i = bounds.start(); i <= bounds.end(); i++) {
                pdfDocument.getPage(i).setCropBox(cropBox);
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully cropped {} pages ({} bytes)", bounds.end() - bounds.start() + 1, pdfBytes.length);
            return buildPdfResult(pdfBytes, "cropped", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to crop PDF", e);
            throw new PdfErrorException("Failed to crop PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult fillForm(MultipartFile file, Map<String, String> fields, Boolean flatten) {
        if (fields == null || fields.isEmpty()) {
            throw new PdfErrorException("Fields map must not be empty");
        }
        validateFormFieldNames(fields);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDocument, false);
            if (acroForm == null) {
                pdfDocument.close();
                throw new PdfErrorException("PDF does not contain an AcroForm");
            }

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                PdfFormField field = acroForm.getField(entry.getKey());
                if (field != null) {
                    field.setValue(entry.getValue());
                } else {
                    log.warn("Form field '{}' not found in PDF", entry.getKey());
                }
            }

            if (Boolean.TRUE.equals(flatten)) {
                acroForm.flattenFields();
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully filled form with {} fields ({} bytes)", fields.size(), pdfBytes.length);
            return buildPdfResult(pdfBytes, "filled", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fill PDF form", e);
            throw new PdfErrorException("Failed to fill PDF form: " + e.getMessage(), e);
        }
    }

    private void validateFormFieldNames(Map<String, String> fields) {
        for (String name : fields.keySet()) {
            if (name == null || name.isBlank()) {
                throw new PdfErrorException("Form field name must not be empty");
            }
            if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains("\0")) {
                throw new PdfErrorException("Invalid form field name contains path traversal characters");
            }
        }
    }

    public byte[] ocrToText(MultipartFile file, String language, Integer startPage, Integer endPage) {
        String lang = resolveLanguage(language);
        try {
            PDDocument document = loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageBounds bounds = resolvePageRange(startPage, endPage, totalPages);
            int start = bounds.start() - 1;
            int end = bounds.end() - 1;

            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = tesseractFactory.create(lang);
            StringBuilder text = new StringBuilder();

            for (int i = start; i <= end; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 300, ImageType.RGB);
                String pageText = performOcr(tesseract, image, i + 1);
                text.append("--- Page ").append(i + 1).append(" ---\n");
                text.append(pageText).append("\n\n");
            }

            document.close();
            log.info("Successfully performed OCR text extraction on {} pages", end - start + 1);
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
            PDDocument document = loadPdfBoxDocument(file);
            int totalPages = document.getNumberOfPages();
            PageBounds bounds = resolvePageRange(startPage, endPage, totalPages);
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
            return buildPdfResult(resultBytes, "ocr", end - start + 1);

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
