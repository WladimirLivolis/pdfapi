package com.pdf.pdfapi.service;

import com.pdf.pdfapi.dto.EncryptRequest;
import com.pdf.pdfapi.dto.PdfInfoResponse;
import com.pdf.pdfapi.dto.PdfMetadataRequest;
import com.pdf.pdfapi.dto.PdfMetadataResponse;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.dto.WatermarkRequest;
import com.pdf.pdfapi.service.annotate.PdfAnnotationService;
import com.pdf.pdfapi.service.core.PdfDocumentOpsService;
import com.pdf.pdfapi.service.core.PdfOptimizationService;
import com.pdf.pdfapi.service.form.PdfFormService;
import com.pdf.pdfapi.service.image.PdfImageService;
import com.pdf.pdfapi.service.metadata.PdfMetadataService;
import com.pdf.pdfapi.service.ocr.PdfOcrService;
import com.pdf.pdfapi.service.security.PdfProtectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfService {

    private final PdfOcrService pdfOcrService;
    private final PdfImageService pdfImageService;
    private final PdfFormService pdfFormService;
    private final PdfMetadataService pdfMetadataService;
    private final PdfProtectionService pdfProtectionService;
    private final PdfOptimizationService pdfOptimizationService;
    private final PdfDocumentOpsService pdfDocumentOpsService;
    private final PdfAnnotationService pdfAnnotationService;

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
        return pdfAnnotationService.addPageNumbers(file, position, format, startPage, endPage);
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
        return pdfAnnotationService.watermark(file, imageFile, request);
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

}
