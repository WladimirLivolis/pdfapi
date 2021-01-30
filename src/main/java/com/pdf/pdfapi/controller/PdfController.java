package com.pdf.pdfapi.controller;

import com.pdf.pdfapi.service.PdfService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/pdfapi")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/merge")
    public void merge(@RequestParam String file1, @RequestParam String file2, @RequestParam String outputFolder) throws IOException {

        pdfService.merge(file1, file2, outputFolder);

    }

    @PostMapping("/split")
    public void split(@RequestParam String orig, @RequestParam String outputFolder, @RequestParam Integer maxPageCount) throws IOException {

        pdfService.split(orig, outputFolder, maxPageCount);

    }

    @PostMapping("/convertImageToPDF")
    public void convertImageToPDF(@RequestParam String orig, @RequestParam String outputFolder) throws FileNotFoundException, MalformedURLException {

        pdfService.convertImageToPDF(orig, outputFolder);

    }

}
