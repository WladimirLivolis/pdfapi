package com.pdf.pdfapi.service.support;

import com.pdf.pdfapi.exception.PdfErrorException;
import org.springframework.stereotype.Component;

@Component
public class PageRangeResolver {

    public PageBounds resolve(Integer startPage, Integer endPage, int totalPages) {
        int start = getValidStartPage(startPage);
        int end = getValidEndPage(endPage, totalPages);
        if (start > end) {
            throw new PdfErrorException("startPage must be less than or equal to endPage");
        }
        return new PageBounds(start, end);
    }

    private int getValidStartPage(Integer startPage) {
        return (startPage != null && startPage >= 1) ? startPage : 1;
    }

    private int getValidEndPage(Integer endPage, int totalPages) {
        return (endPage != null && endPage <= totalPages) ? endPage : totalPages;
    }

    public record PageBounds(int start, int end) {
    }
}
