package com.pdf.pdfapi.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdf.pdfapi.dto.*;
import com.pdf.pdfapi.service.PdfService;
import com.pdf.pdfapi.validator.PdfFileValidator;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pdfapi")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final PdfFileValidator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/merge")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> merge(@RequestParam MultipartFile[] file,
                                           @RequestParam(required = false) Boolean createBookmarks) {
        validator.validatePdfFiles(file);
        PdfResult result = pdfService.merge(createBookmarks, file);
        return buildPdfResponse(result);
    }

    @PostMapping("/split")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<List<PdfOperationResponse>> split(@RequestParam MultipartFile file, @RequestParam Integer maxPageCount) {
        validator.validatePdfFile(file);
        List<PdfResult> results = pdfService.split(file, maxPageCount);

        // For split, we return metadata about the files created
        // In a production scenario, you might want to return the files in a ZIP or save them temporarily with download links
        List<PdfOperationResponse> responses = results.stream()
                .map(r -> PdfOperationResponse.success(
                        "PDF split successfully",
                        r.suggestedFileName(),
                        r.sizeInBytes(),
                        r.pageCount()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/extract")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> extract(@RequestParam MultipartFile file,
                                            @RequestParam Integer startPage,
                                            @RequestParam Integer endPage) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.extract(file, startPage, endPage);
        return buildPdfResponse(result);
    }

    @PostMapping("/remove")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> remove(@RequestParam MultipartFile file, @RequestParam Integer... page) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.remove(file, page);
        return buildPdfResponse(result);
    }

    @PostMapping("/convertImageToPDF")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<List<PdfOperationResponse>> convertImageToPDF(@RequestParam MultipartFile... file) {
        validator.validateImageFiles(file);
        List<PdfResult> results = pdfService.convertImageToPDF(file);

        // Similar to split, return metadata for multiple files
        List<PdfOperationResponse> responses = results.stream()
                .map(r -> PdfOperationResponse.success(
                        "Image converted to PDF successfully",
                        r.suggestedFileName(),
                        r.sizeInBytes(),
                        r.pageCount()
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/rotate")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> rotate(@RequestParam MultipartFile file,
                                           @RequestParam Integer rotation,
                                           @RequestParam(required = false) Integer... pages) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.rotate(file, rotation, pages);
        return buildPdfResponse(result);
    }

    @PostMapping("/info")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<PdfInfoResponse> info(@RequestParam MultipartFile file) {
        validator.validatePdfFile(file);
        PdfInfoResponse info = pdfService.getInfo(file);
        return ResponseEntity.ok(info);
    }

    @PostMapping("/metadata")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<PdfMetadataResponse> getMetadata(@RequestParam MultipartFile file) {
        validator.validatePdfFile(file);
        PdfMetadataResponse metadata = pdfService.getMetadata(file);
        return ResponseEntity.ok(metadata);
    }

    @PutMapping("/metadata")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> updateMetadata(@RequestParam MultipartFile file,
                                                    @RequestParam(required = false) String title,
                                                    @RequestParam(required = false) String author,
                                                    @RequestParam(required = false) String subject,
                                                    @RequestParam(required = false) String keywords,
                                                    @RequestParam(required = false) String creator) {
        validator.validatePdfFile(file);

        PdfMetadataRequest metadata = PdfMetadataRequest.builder()
                .title(title)
                .author(author)
                .subject(subject)
                .keywords(keywords)
                .creator(creator)
                .build();

        PdfResult result = pdfService.updateMetadata(file, metadata);
        return buildPdfResponse(result);
    }

    @PostMapping("/addPageNumbers")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> addPageNumbers(@RequestParam MultipartFile file,
                                                    @RequestParam(required = false) String position,
                                                    @RequestParam(required = false) String format,
                                                    @RequestParam(required = false) Integer startPage,
                                                    @RequestParam(required = false) Integer endPage) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.addPageNumbers(file, position, format, startPage, endPage);
        return buildPdfResponse(result);
    }

    @PostMapping("/watermark")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> watermark(@RequestParam MultipartFile file,
                                               @RequestParam(required = false) String text,
                                               @RequestParam(required = false) MultipartFile image,
                                               @RequestParam(required = false) String position,
                                               @RequestParam(required = false) Float opacity,
                                               @RequestParam(required = false) Float rotation,
                                               @RequestParam(required = false) Float scale,
                                               @RequestParam(required = false) String layer,
                                               @RequestParam(required = false) Integer startPage,
                                               @RequestParam(required = false) Integer endPage) {
        validator.validatePdfFile(file);

        if (image != null) {
            String contentType = image.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                throw new IllegalArgumentException("Watermark image must be a valid image file (PNG, JPG, etc.)");
            }
        }

        WatermarkRequest request = WatermarkRequest.builder()
                .text(text).position(position).opacity(opacity).rotation(rotation)
                .scale(scale).layer(layer).startPage(startPage).endPage(endPage)
                .build();
        PdfResult result = pdfService.watermark(file, image, request);
        return buildPdfResponse(result);
    }

    @PostMapping("/compress")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> compress(@RequestParam MultipartFile file,
                                              @RequestParam(required = false) String level) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.compress(file, level);
        return buildPdfResponse(result);
    }

    @PostMapping("/encrypt")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> encrypt(@RequestParam MultipartFile file,
                                             @RequestParam(required = false) String userPassword,
                                             @RequestParam(required = false) String ownerPassword,
                                             @RequestParam(required = false) Integer encryptionType,
                                             @RequestParam(required = false) Boolean allowPrinting,
                                             @RequestParam(required = false) Boolean allowModifying,
                                             @RequestParam(required = false) Boolean allowCopy,
                                             @RequestParam(required = false) Boolean allowAnnotations) {
        validator.validatePdfFile(file);
        EncryptRequest request = new EncryptRequest(userPassword, ownerPassword, encryptionType,
                allowPrinting, allowModifying, allowCopy, allowAnnotations);
        PdfResult result = pdfService.encrypt(file, request);
        return buildPdfResponse(result);
    }

    @PostMapping("/decrypt")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> decrypt(@RequestParam MultipartFile file,
                                             @RequestParam String password) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.decrypt(file, password);
        return buildPdfResponse(result);
    }

    @PostMapping("/optimize")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> optimize(@RequestParam MultipartFile file) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.optimize(file);
        return buildPdfResponse(result);
    }

    @PostMapping("/toImages")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> toImages(@RequestParam MultipartFile file,
                                              @RequestParam(required = false) String format,
                                              @RequestParam(required = false) Integer dpi,
                                              @RequestParam(required = false) Integer startPage,
                                              @RequestParam(required = false) Integer endPage) {
        validator.validatePdfFile(file);
        byte[] zipBytes = pdfService.toImages(file, format, dpi, startPage, endPage);
        return buildZipResponse(zipBytes, String.format("images_%s.zip", System.currentTimeMillis()));
    }

    @PostMapping("/extractImages")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> extractImages(@RequestParam MultipartFile file,
                                                   @RequestParam(required = false) Integer minWidth,
                                                   @RequestParam(required = false) Integer minHeight) {
        validator.validatePdfFile(file);
        byte[] zipBytes = pdfService.extractImages(file, minWidth, minHeight);
        return buildZipResponse(zipBytes, String.format("extracted_images_%s.zip", System.currentTimeMillis()));
    }

    @PostMapping("/crop")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> crop(@RequestParam MultipartFile file,
                                          @RequestParam Float x,
                                          @RequestParam Float y,
                                          @RequestParam Float width,
                                          @RequestParam Float height,
                                          @RequestParam(required = false) Integer startPage,
                                          @RequestParam(required = false) Integer endPage) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.crop(file, x, y, width, height, startPage, endPage);
        return buildPdfResponse(result);
    }

    @PostMapping("/ocr")
    @RateLimiter(name = "pdfapi-ocr")
    public ResponseEntity<Resource> ocr(@RequestParam MultipartFile file,
                                         @RequestParam(required = false, defaultValue = "eng") String language,
                                         @RequestParam(required = false, defaultValue = "text") String outputType,
                                         @RequestParam(required = false) Integer startPage,
                                         @RequestParam(required = false) Integer endPage,
                                         @RequestParam(required = false) Integer dpi) {
        validator.validatePdfFile(file);
        validateLanguage(language);

        if ("pdf".equalsIgnoreCase(outputType)) {
            PdfResult result = pdfService.ocrToPdf(file, language, startPage, endPage, dpi);
            return buildPdfResponse(result);
        } else {
            byte[] textBytes = pdfService.ocrToText(file, language, startPage, endPage, dpi);
            ByteArrayResource resource = new ByteArrayResource(textBytes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ocr_result.txt\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(textBytes.length)
                    .body(resource);
        }
    }

    @PostMapping("/fillForm")
    @RateLimiter(name = "pdfapi")
    public ResponseEntity<Resource> fillForm(@RequestParam MultipartFile file,
                                              @RequestParam String fieldsJson,
                                              @RequestParam(required = false) Boolean flatten) {
        validator.validatePdfFile(file);
        Map<String, String> fields;
        try {
            byte[] jsonBytes = fieldsJson.getBytes(StandardCharsets.UTF_8);
            fields = objectMapper.readValue(jsonBytes, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid fieldsJson: must be a valid JSON object with string values");
        }

        PdfResult result = pdfService.fillForm(file, fields, flatten);
        return buildPdfResponse(result);
    }

    private void validateLanguage(String language) {
        if (!language.matches("[a-z_]{2,10}")) {
            throw new IllegalArgumentException(
                    "Invalid language code '" + language + "'. Use standard Tesseract language codes (e.g. eng, por, spa, fra, deu).");
        }
    }

    private ResponseEntity<Resource> buildZipResponse(byte[] zipBytes, String fileName) {
        ByteArrayResource resource = new ByteArrayResource(zipBytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(zipBytes.length)
                .body(resource);
    }

    private ResponseEntity<Resource> buildPdfResponse(PdfResult result) {
        ByteArrayResource resource = new ByteArrayResource(result.content());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.suggestedFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.sizeInBytes())
                .body(resource);
    }

}
