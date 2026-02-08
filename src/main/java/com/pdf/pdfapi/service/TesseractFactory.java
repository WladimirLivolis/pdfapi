package com.pdf.pdfapi.service;

import net.sourceforge.tess4j.ITesseract;

@FunctionalInterface
public interface TesseractFactory {
    ITesseract create(String language);
}
