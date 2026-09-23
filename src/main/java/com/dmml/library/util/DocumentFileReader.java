package com.dmml.library.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Utility to extract text and metadata from common document formats:
 * PDF (.pdf), Word (.docx), Plain Text (.txt, .md, .csv, .log), and JSON (.json).
 * Supports automatic UTF-8 Vietnamese character handling and language detection.
 */
public class DocumentFileReader {

    private static final Pattern VIETNAMESE_CHARS = Pattern.compile(
            "[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệđìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵ" +
            "ÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÈÉẺẼẸÊỀẾỂỄỆĐÌÍỈĨỊÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢÙÚỦŨỤƯỪỨỬỮỰỲÝỶỸỴ]");

    public static class ParsedDocument {
        private String title;
        private String author;
        private String summary;
        private String content;
        private String fileFormat;
        private String language; // "VI" or "EN"

        public ParsedDocument() {}

        public ParsedDocument(String title, String author, String summary, String content, String fileFormat, String language) {
            this.title = title;
            this.author = author;
            this.summary = summary;
            this.content = content;
            this.fileFormat = fileFormat;
            this.language = language;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getFileFormat() { return fileFormat; }
        public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    /**
     * Reads and parses a document file into title, abstract, content, format, and detected language.
     */
    public static ParsedDocument readFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File không tồn tại!");
        }

        String fileName = file.getName();
        String lowerName = fileName.toLowerCase();
        String rawText;
        String format;

        if (lowerName.endsWith(".pdf")) {
            format = "PDF";
            rawText = readPdf(file);
        } else if (lowerName.endsWith(".docx")) {
            format = "DOCX";
            rawText = readDocx(file);
        } else if (lowerName.endsWith(".json")) {
            format = "JSON";
            return parseJsonDocument(file);
        } else {
            // Default to plain text (txt, md, csv, log, etc.)
            format = "TXT";
            rawText = readPlainText(file);
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalStateException("Không trích xuất được nội dung từ file hoặc file rỗng!");
        }

        rawText = rawText.replace("\r\n", "\n").replace('\r', '\n').trim();

        // Extract title: from first non-empty line if short, else from file name
        String title = deriveTitle(fileName, rawText);
        String summary = deriveSummary(rawText);
        String language = detectLanguage(rawText);
        String author = "Tác giả từ file " + format;

        return new ParsedDocument(title, author, summary, rawText, format, language);
    }

    /**
     * Reads text from a PDF file using Apache PDFBox.
     */
    public static String readPdf(File file) throws Exception {
        try (PDDocument document = PDDocument.load(file)) {
            if (document.isEncrypted()) {
                throw new IllegalStateException("File PDF bị khóa mật khẩu mã hóa!");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /**
     * Reads text from a Word DOCX file using Apache POI.
     */
    public static String readDocx(File file) throws Exception {
        try (InputStream is = Files.newInputStream(file.toPath());
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    /**
     * Reads plain text file with UTF-8 encoding (fallback to ISO-8859-1).
     */
    public static String readPlainText(File file) throws Exception {
        Path path = file.toPath();
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return Files.readString(path, StandardCharsets.ISO_8859_1);
        }
    }

    private static ParsedDocument parseJsonDocument(File file) throws Exception {
        String json = readPlainText(file);
        try {
            JsonObject obj = new Gson().fromJson(json, JsonObject.class);
            String title = obj.has("title") ? obj.get("title").getAsString() : file.getName();
            String author = obj.has("author") ? obj.get("author").getAsString() : "Tác giả JSON";
            String summary = obj.has("summary") ? obj.get("summary").getAsString() : "";
            String content = obj.has("content") ? obj.get("content").getAsString() : json;
            String lang = detectLanguage(title + " " + content);
            if (summary.isEmpty()) summary = deriveSummary(content);
            return new ParsedDocument(title, author, summary, content, "JSON", lang);
        } catch (Exception e) {
            // Not structured document JSON, treat as raw text
            String lang = detectLanguage(json);
            return new ParsedDocument(file.getName(), "JSON Data", deriveSummary(json), json, "JSON", lang);
        }
    }

    /**
     * Heuristic to detect Vietnamese vs English text based on accented characters and markers.
     */
    public static String detectLanguage(String text) {
        if (text == null || text.isEmpty()) return "EN";
        if (VIETNAMESE_CHARS.matcher(text).find()) {
            return "VI";
        }
        return "EN";
    }

    private static String deriveTitle(String fileName, String rawText) {
        String baseName = fileName;
        int dotIdx = baseName.lastIndexOf('.');
        if (dotIdx > 0) {
            baseName = baseName.substring(0, dotIdx);
        }
        // Normalize underscores or hyphens in filename
        baseName = baseName.replace('_', ' ').replace('-', ' ').trim();

        // Check if first non-empty line can serve as a title
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.length() >= 5 && trimmed.length() <= 120 && !trimmed.contains("...")) {
                    return trimmed;
                }
                break;
            }
        }
        return baseName;
    }

    private static String deriveSummary(String rawText) {
        String[] paragraphs = rawText.split("\n\n+");
        for (String p : paragraphs) {
            String trimmed = p.replaceAll("\\s+", " ").trim();
            if (trimmed.length() > 30) {
                if (trimmed.length() > 220) {
                    return trimmed.substring(0, 217) + "...";
                }
                return trimmed;
            }
        }
        String clean = rawText.replaceAll("\\s+", " ").trim();
        return clean.length() > 200 ? clean.substring(0, 197) + "..." : clean;
    }
}
