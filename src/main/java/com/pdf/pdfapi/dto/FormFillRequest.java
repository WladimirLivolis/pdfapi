package com.pdf.pdfapi.dto;

import java.util.Map;

public record FormFillRequest(Map<String, String> fields, boolean flatten) {
}
