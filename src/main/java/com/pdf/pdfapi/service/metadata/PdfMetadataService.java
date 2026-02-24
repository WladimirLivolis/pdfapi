package com.pdf.pdfapi.service.metadata;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
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
public class PdfMetadataService {

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;

    public PdfInfoResponse getInfo(MultipartFile file) {
        try {
            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file));

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
            PdfDocument pdfDocument = new PdfDocument(pdfIoSupport.toPdfReader(file));
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
                    pdfIoSupport.toPdfReader(file),
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
            return pdfResultFactory.buildPdfResult(pdfBytes, "metadata_updated", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update PDF metadata", e);
            throw new PdfErrorException("Failed to update PDF metadata: " + e.getMessage(), e);
        }
    }
}
