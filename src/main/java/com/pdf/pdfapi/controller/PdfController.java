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
    public void merge(@RequestParam MultipartFile... file) throws Exception {

        pdfService.merge(file);

    }

    @PostMapping("/split")
    public void split(@RequestParam MultipartFile file, @RequestParam Integer maxPageCount) throws IOException {

        pdfService.split(file, maxPageCount);

    }

    @PostMapping("/extract")
    public void extract(@RequestParam MultipartFile file, @RequestParam Integer startPage, @RequestParam Integer endPage) throws IOException {

        pdfService.extract(file, startPage, endPage);

    }

    @PostMapping("/remove")
    public void remove(@RequestParam MultipartFile file, @RequestParam Integer... page) throws IOException {

        pdfService.remove(file, page);

    }

    @PostMapping("/convertImageToPDF")
    public void convertImageToPDF(@RequestParam MultipartFile file) throws IOException {

        pdfService.convertImageToPDF(file);

    }

}
