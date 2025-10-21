package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.dto.PdfOperationResponse;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.service.PdfService;
import com.pdf.pdfapi.validator.PdfFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/pdfapi")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;
    private final PdfFileValidator validator;

    @PostMapping("/merge")
    public ResponseEntity<Resource> merge(@RequestParam MultipartFile... file) {
        validator.validatePdfFiles(file);
        PdfResult result = pdfService.merge(file);
        return buildPdfResponse(result);
    }

    @PostMapping("/split")
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
    public ResponseEntity<Resource> extract(@RequestParam MultipartFile file,
                                            @RequestParam Integer startPage,
                                            @RequestParam Integer endPage) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.extract(file, startPage, endPage);
        return buildPdfResponse(result);
    }

    @PostMapping("/remove")
    public ResponseEntity<Resource> remove(@RequestParam MultipartFile file, @RequestParam Integer... page) {
        validator.validatePdfFile(file);
        PdfResult result = pdfService.remove(file, page);
        return buildPdfResponse(result);
    }

    @PostMapping("/convertImageToPDF")
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

    private ResponseEntity<Resource> buildPdfResponse(PdfResult result) {
        ByteArrayResource resource = new ByteArrayResource(result.content());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.suggestedFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(result.sizeInBytes())
                .body(resource);
    }

}
