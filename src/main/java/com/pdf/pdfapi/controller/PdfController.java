package com.pdf.pdfapi.controller;

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

import java.util.List;

@RestController
@RequestMapping("/pdfapi")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final PdfFileValidator validator;

    @PostMapping("/merge")
    @RateLimiter(name = "pdfapi-heavy")
    public ResponseEntity<Resource> merge(@RequestParam MultipartFile... file) {
        validator.validatePdfFiles(file);
        PdfResult result = pdfService.merge(file);
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

    private ResponseEntity<Resource> buildPdfResponse(PdfResult result) {
        ByteArrayResource resource = new ByteArrayResource(result.content());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.suggestedFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.sizeInBytes())
                .body(resource);
    }

}
