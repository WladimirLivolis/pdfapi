package com.pdf.pdfapi.service.core;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfOptimizationService {

    private static final String LOW_COMPRESSION_LEVEL = "LOW";
    private static final String MEDIUM_COMPRESSION_LEVEL = "MEDIUM";
    private static final String HIGH_COMPRESSION_LEVEL = "HIGH";

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;

    public PdfResult compress(MultipartFile file, String level) {
        String compressionLevel = validateAndNormalizeCompressionLevel(level);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            WriterProperties writerProperties = configureCompressionWriter(compressionLevel);

            PdfWriter writer = new PdfWriter(outputStream, writerProperties);
            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file), writer);

            applyCompressionToPages(pdfDocument, compressionLevel);

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            logCompressionResult(compressionLevel, file.getSize(), pdfBytes.length, pageCount);
            return pdfResultFactory.buildPdfResult(pdfBytes, "compressed_" + compressionLevel.toLowerCase(), pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to compress PDF", e);
            throw new PdfErrorException("Failed to compress PDF: " + e.getMessage(), e);
        }
    }

    public PdfResult optimize(MultipartFile file) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            WriterProperties writerProperties = new WriterProperties();
            writerProperties.setFullCompressionMode(true);
            writerProperties.addXmpMetadata();

            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file), new PdfWriter(outputStream, writerProperties));
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
            return pdfResultFactory.buildPdfResult(pdfBytes, "optimized", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to optimize PDF", e);
            throw new PdfErrorException("Failed to optimize PDF: " + e.getMessage(), e);
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
}
