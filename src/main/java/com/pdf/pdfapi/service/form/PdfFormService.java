package com.pdf.pdfapi.service.form;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.pdf.pdfapi.dto.PdfResult;
import com.pdf.pdfapi.exception.PdfErrorException;
import com.pdf.pdfapi.service.support.PdfIoSupport;
import com.pdf.pdfapi.service.support.PdfResultFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class PdfFormService {

    private final PdfIoSupport pdfIoSupport;
    private final PdfResultFactory pdfResultFactory;

    public PdfResult fillForm(MultipartFile file, Map<String, String> fields, Boolean flatten) {
        if (fields == null || fields.isEmpty()) {
            throw new PdfErrorException("Fields map must not be empty");
        }
        validateFormFieldNames(fields);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(
                    pdfIoSupport.toPdfReader(file),
                    new PdfWriter(outputStream)
            );

            PdfAcroForm acroForm = PdfAcroForm.getAcroForm(pdfDocument, false);
            if (acroForm == null) {
                pdfDocument.close();
                throw new PdfErrorException("PDF does not contain an AcroForm");
            }

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                PdfFormField field = acroForm.getField(entry.getKey());
                if (field != null) {
                    field.setValue(entry.getValue());
                } else {
                    log.warn("Form field '{}' not found in PDF", entry.getKey());
                }
            }

            if (Boolean.TRUE.equals(flatten)) {
                acroForm.flattenFields();
            }

            int pageCount = pdfDocument.getNumberOfPages();
            pdfDocument.close();

            byte[] pdfBytes = outputStream.toByteArray();
            log.info("Successfully filled form with {} fields ({} bytes)", fields.size(), pdfBytes.length);
            return pdfResultFactory.buildPdfResult(pdfBytes, "filled", pageCount);

        } catch (PdfErrorException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fill PDF form", e);
            throw new PdfErrorException("Failed to fill PDF form: " + e.getMessage(), e);
        }
    }

    private void validateFormFieldNames(Map<String, String> fields) {
        for (String name : fields.keySet()) {
            if (name == null || name.isBlank()) {
                throw new PdfErrorException("Form field name must not be empty");
            }
            if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains("\0")) {
                throw new PdfErrorException("Invalid form field name contains path traversal characters");
            }
        }
    }
}
