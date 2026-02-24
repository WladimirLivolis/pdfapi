package com.pdf.pdfapi.service.core;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.utils.PageRange;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.kernel.utils.PdfSplitter;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PageRangeResolver;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfDocumentOpsService {

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;
    private final PageRangeResolver pageRangeResolver;

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
                    pdfIoSupport.toPdfReader(file[0]),
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
                PdfDocument pdfDocument2 = new PdfDocument(pdfIoSupport.toPdfReader(file[i]));
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
            return pdfResultFactory.buildPdfResult(pdfBytes, "merged", pageCount);

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

            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file));
            String baseFileName = String.format("splitDocument_%s_", pdfResultFactory.timestamp());

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
            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file));

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
            return pdfResultFactory.buildPdfResult(pdfBytes, "extractedPages", pageCount);

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
                    pdfIoSupport.toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            validatePageNumbers(page, totalPages, pdfDocument);
            List<Integer> pagesToRemove = Arrays.stream(page)
                    .distinct()
                    .sorted((a, b) -> Integer.compare(b, a))
                    .toList();

            for (Integer pageNumber : pagesToRemove) {
                pdfDocument.removePage(pageNumber);
            }

            int finalPageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully removed {} pages ({} pages remaining, {} bytes)", pagesToRemove.size(), finalPageCount, pdfBytes.length);
            return pdfResultFactory.buildPdfResult(pdfBytes, "removedPages", finalPageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove from file", e);
            throw new PdfErrorException("Failed to remove pages from PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult rotate(MultipartFile file, Integer rotation, Integer... pages) {
        validateRotationAngle(rotation);
        int normalizedRotation = normalizeRotation(rotation);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    pdfIoSupport.toPdfReader(file),
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
            return pdfResultFactory.buildPdfResult(pdfBytes, "rotated", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to rotate PDF", e);
            throw new PdfErrorException("Failed to rotate PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult crop(MultipartFile file, Float x, Float y, Float width, Float height,
                          Integer startPage, Integer endPage) {
        if (x == null || y == null || width == null || height == null) {
            throw new PdfErrorException("x, y, width and height are required for crop");
        }

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    pdfIoSupport.toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            int totalPages = pdfDocument.getNumberOfPages();
            PageRangeResolver.PageBounds bounds = pageRangeResolver.resolve(startPage, endPage, totalPages);

            Rectangle cropBox = new Rectangle(x, y, width, height);
            for (int i = bounds.start(); i <= bounds.end(); i++) {
                pdfDocument.getPage(i).setCropBox(cropBox);
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully cropped {} pages ({} bytes)", bounds.end() - bounds.start() + 1, pdfBytes.length);
            return pdfResultFactory.buildPdfResult(pdfBytes, "cropped", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to crop PDF", e);
            throw new PdfErrorException("Failed to crop PDF: " + e.getMessage(), e);
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
}
