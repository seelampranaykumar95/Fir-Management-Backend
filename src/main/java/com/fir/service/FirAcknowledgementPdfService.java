package com.fir.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.fir.model.Fir;

@Service
public class FirAcknowledgementPdfService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public byte[] buildAcknowledgementPdf(Fir fir) {
        String complaintName = fir.getFiledBy() == null ? "Citizen" : sanitize(fir.getFiledBy().getName());
        String createdAt = fir.getCreatedAt() == null ? "NA" : fir.getCreatedAt().format(DATE_TIME_FORMATTER);
        String policeStation = fir.getPoliceStation() == null ? "Pending assignment" : sanitize(fir.getPoliceStation().getName());

        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 18 Tf 50 780 Td (FIR Acknowledgement) Tj\n");
        content.append("/F1 11 Tf 0 -30 Td (FIR ID: ").append(sanitize(String.valueOf(fir.getId()))).append(") Tj\n");
        content.append("0 -18 Td (Complainant: ").append(complaintName).append(") Tj\n");
        content.append("0 -18 Td (Status: ").append(sanitize(fir.getStatus().name())).append(") Tj\n");
        content.append("0 -18 Td (Category: ").append(sanitize(fir.getCategory() == null ? "OTHER" : fir.getCategory().name())).append(") Tj\n");
        content.append("0 -18 Td (Filed At: ").append(sanitize(createdAt)).append(") Tj\n");
        content.append("0 -18 Td (Police Station: ").append(policeStation).append(") Tj\n");
        content.append("0 -18 Td (Location: ").append(sanitize(fir.getLocation())).append(") Tj\n");
        content.append("0 -18 Td (Title: ").append(sanitize(fir.getTitle())).append(") Tj\n");
        content.append("0 -18 Td (Description: ").append(sanitize(trimToLength(fir.getDescription(), 110))).append(") Tj\n");
        content.append("ET");

        String stream = content.toString();
        StringBuilder pdf = new StringBuilder();
        int[] offsets = new int[6];

        pdf.append("%PDF-1.4\n");
        offsets[1] = pdf.length();
        pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        offsets[2] = pdf.length();
        pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        offsets[3] = pdf.length();
        pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n");
        offsets[4] = pdf.length();
        pdf.append("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
        offsets[5] = pdf.length();
        pdf.append("5 0 obj << /Length ").append(stream.getBytes(StandardCharsets.US_ASCII).length).append(" >> stream\n");
        pdf.append(stream).append("\nendstream endobj\n");

        int xrefOffset = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (int i = 1; i <= 5; i++) {
            pdf.append(String.format("%010d 00000 n %n", offsets[i]));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");

        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}

