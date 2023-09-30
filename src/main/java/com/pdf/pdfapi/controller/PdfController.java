package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/pdfapi")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    @PostMapping("/merge")
    public void merge(@RequestParam MultipartFile... file) {

        pdfService.merge(file);

    }

    @PostMapping("/split")
    public void split(@RequestParam MultipartFile file, @RequestParam Integer maxPageCount) {

        pdfService.split(file, maxPageCount);

    }

    @PostMapping("/extract")
    public void extract(@RequestParam MultipartFile file, @RequestParam Integer startPage, @RequestParam Integer endPage) {

        pdfService.extract(file, startPage, endPage);

    }

    @PostMapping("/remove")
    public void remove(@RequestParam MultipartFile file, @RequestParam Integer... page) {

        pdfService.remove(file, page);

    }

    @PostMapping("/convertImageToPDF")
    public void convertImageToPDF(@RequestParam MultipartFile... file) {

        pdfService.convertImageToPDF(file);

    }

}
