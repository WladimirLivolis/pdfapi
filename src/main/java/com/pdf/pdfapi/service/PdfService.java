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
                int partNumber = 1;

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

    private String timestamp() {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return time.format(formatter);
    }

}
