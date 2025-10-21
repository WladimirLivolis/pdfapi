package com.pdf.pdfapi.exception;

public class PdfErrorException extends RuntimeException {
    public PdfErrorException(String errorMessage) {
        super(errorMessage);
    }

    public PdfErrorException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }
}
