package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.service.PdfService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/pdfapi")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/merge")
    public void merge(@RequestParam MultipartFile file1, @RequestParam MultipartFile file2) throws IOException {

        pdfService.merge(file1, file2);

    }

    @PostMapping("/split")
    public void split(@RequestParam MultipartFile file, @RequestParam Integer maxPageCount) throws IOException {

        pdfService.split(file, maxPageCount);

    }

    @PostMapping("/convertImageToPDF")
    public void convertImageToPDF(@RequestParam MultipartFile file) throws IOException {

        pdfService.convertImageToPDF(file);

    }

}
