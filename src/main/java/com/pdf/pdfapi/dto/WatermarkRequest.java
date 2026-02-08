package com.pdf.pdfapi.dto;

import lombok.Builder;

/**
 * Request DTO for watermark operations
 */
@Builder
public record WatermarkRequest(
        String text,
        String position,         // e.g., "center", "top-left", "bottom-right"
        Float opacity,           // 0.0 to 1.0
        Float rotation,          // rotation angle in degrees
        Float scale,             // scale factor (1.0 = 100%)
        String layer,            // "foreground" or "background"
        Integer startPage,       // first page to apply watermark (optional)
        Integer endPage          // last page to apply watermark (optional)
) {
    /**
     * Creates a WatermarkRequest with default values
     */
    public WatermarkRequest {
        if (position == null || position.isBlank()) {
            position = "center";
        }
        if (opacity == null) {
            opacity = 0.3f;
        }
        if (rotation == null) {
            rotation = 45.0f;
        }
        if (scale == null) {
            scale = 1.0f;
        }
        if (layer == null || layer.isBlank()) {
            layer = "foreground";
        }
    }
}
