package com.doliuw.service;

import com.doliuw.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Extracts plain text from an uploaded resume (PDF or DOCX) for the AI Career Agent. */
@Service
@Slf4j
public class ResumeParsingService {

    private static final int MAX_CHARS = 12_000; // keeps prompt size/cost bounded

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("Resume file is empty.", HttpStatus.BAD_REQUEST);
        }

        String name = file.getOriginalFilename() != null
            ? file.getOriginalFilename().toLowerCase(Locale.ROOT)
            : "";

        try (InputStream is = file.getInputStream()) {
            String text;
            if (name.endsWith(".pdf")) {
                text = extractPdf(is);
            } else if (name.endsWith(".docx")) {
                text = extractDocx(is);
            } else {
                throw new AppException("Unsupported file type. Please upload a PDF or DOCX resume.", HttpStatus.BAD_REQUEST);
            }

            if (text == null || text.isBlank()) {
                throw new AppException("Could not read any text from the resume file.", HttpStatus.BAD_REQUEST);
            }

            return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;

        } catch (AppException ae) {
            throw ae;
        } catch (IOException e) {
            log.error("Failed to parse resume file", e);
            throw new AppException("Failed to read the resume file.", HttpStatus.BAD_REQUEST);
        }
    }

    private String extractPdf(InputStream is) throws IOException {
        try (PDDocument doc = PDDocument.load(is)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(InputStream is) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
