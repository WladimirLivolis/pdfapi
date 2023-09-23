package com.pdf.pdfapi.exception;

public class PdfErrorException extends RuntimeException {
    public PdfErrorException(String errorMessage) {
        super(errorMessage);
    }
}
