package com.dmml.library.util;

import com.dmml.library.model.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Manages the physical repository of real document files (.pdf, .docx, .txt)
 * stored locally on disk under data/documents/.
 * 
 * Ensures every document in the digital library has an authentic, multi-page,
 * formatted file that can be opened in native desktop viewers (Adobe Acrobat,
 * Microsoft Word, Notepad, etc.) or viewed in Windows File Explorer.
 */
public class PhysicalDocumentManager {

    public static final Path REPO_DIR = Paths.get("data", "documents");

    private static final String WIN_ARIAL = "C:/Windows/Fonts/arial.ttf";
    private static final String WIN_ARIAL_BOLD = "C:/Windows/Fonts/arialbd.ttf";

    /**
     * Initializes the physical document storage and generates real files
     * on disk for all documents that do not already have one.
     */
    public static void initializeRepository(List<Document> documents) {
        try {
            if (!Files.exists(REPO_DIR)) {
                Files.createDirectories(REPO_DIR);
            }

            for (Document doc : documents) {
                // If document doesn't have an existing physical file on disk, generate one
                if (doc.getFilePath() == null || !new File(doc.getFilePath()).exists()) {
                    File file = generatePhysicalFile(doc);
                    if (file != null && file.exists()) {
                        doc.setFilePath(file.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not fully initialize physical document repository: " + e.getMessage());
        }
    }

    /**
     * Ensures a real physical file exists on disk for the given document, creating it if needed.
     */
    public static File ensurePhysicalFile(Document doc) {
        if (doc.getFilePath() != null) {
            File existing = new File(doc.getFilePath());
            if (existing.exists()) {
                return existing;
            }
        }
        File created = generatePhysicalFile(doc);
        if (created != null && created.exists()) {
            doc.setFilePath(created.getAbsolutePath());
        }
        return created;
    }

    /**
     * Generates an authentic physical file (.pdf, .docx, or .txt) matching the document's format.
     */
    public static File generatePhysicalFile(Document doc) {
        try {
            if (!Files.exists(REPO_DIR)) {
                Files.createDirectories(REPO_DIR);
            }

            String format = (doc.getFileFormat() != null ? doc.getFileFormat().toUpperCase().trim() : "TXT");
            String safeTitle = slugify(doc.getTitle());
            if (safeTitle.length() > 40) safeTitle = safeTitle.substring(0, 40);

            String ext = "txt";
            if ("PDF".equals(format)) ext = "pdf";
            else if ("DOCX".equals(format) || "DOC".equals(format)) ext = "docx";

            File targetFile = REPO_DIR.resolve("Doc_" + doc.getId() + "_" + safeTitle + "." + ext).toFile();

            // Only generate if not already present
            if (targetFile.exists() && targetFile.length() > 0) {
                return targetFile;
            }

            switch (ext) {
                case "pdf":
                    createRealPdf(doc, targetFile);
                    break;
                case "docx":
                    createRealDocx(doc, targetFile);
                    break;
                default:
                    createRealTxt(doc, targetFile);
                    break;
            }

            return targetFile;
        } catch (Exception e) {
            System.err.println("Error creating physical file for Doc #" + doc.getId() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Creates a high-quality multi-page PDF document using Apache PDFBox
     * with TrueType Arial font for full Vietnamese diacritics support.
     */
    private static void createRealPdf(Document doc, File targetFile) throws Exception {
        try (PDDocument pdfDoc = new PDDocument()) {
            PDFont fontRegular;
            PDFont fontBold;

            File arialFile = new File(WIN_ARIAL);
            File arialBoldFile = new File(WIN_ARIAL_BOLD);

            if (arialFile.exists()) {
                fontRegular = PDType0Font.load(pdfDoc, arialFile);
                fontBold = arialBoldFile.exists() ? PDType0Font.load(pdfDoc, arialBoldFile) : fontRegular;
            } else {
                fontRegular = PDType1Font.HELVETICA;
                fontBold = PDType1Font.HELVETICA_BOLD;
            }

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 50f;
            float usableWidth = pageSize.getWidth() - 2 * margin; // ~495 pt
            float startY = pageSize.getHeight() - 60f;
            float bottomLimit = 55f;

            PDPage currentPage = new PDPage(pageSize);
            pdfDoc.addPage(currentPage);
            PDPageContentStream cs = new PDPageContentStream(pdfDoc, currentPage);

            float currentY = startY;

            // 1. Header Banner
            cs.beginText();
            cs.setFont(fontBold, 15f);
            cs.newLineAtOffset(margin, currentY);
            List<String> titleLines = wrapText(doc.getTitle(), fontBold, 15f, usableWidth);
            for (int i = 0; i < titleLines.size(); i++) {
                if (i > 0) cs.newLineAtOffset(0, -18f);
                safeShowText(cs, fontBold, titleLines.get(i));
                currentY -= 18f;
            }
            cs.endText();

            // 2. Author & Metadata Line
            currentY -= 6f;
            cs.beginText();
            cs.setFont(fontRegular, 10f);
            cs.newLineAtOffset(margin, currentY);
            String metaStr = "Tác giả: " + (doc.getAuthor() != null ? doc.getAuthor() : "Thư viện số")
                    + "  |  Cụm: " + (doc.getTopic() != null ? doc.getTopic() : "Khoa học")
                    + "  |  Mã tài liệu: #" + doc.getId();
            safeShowText(cs, fontRegular, metaStr);
            cs.endText();

            // Divider Line
            currentY -= 14f;
            cs.setLineWidth(0.8f);
            cs.moveTo(margin, currentY);
            cs.lineTo(pageSize.getWidth() - margin, currentY);
            cs.stroke();
            currentY -= 18f;

            // 3. Abstract Section (if present)
            if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
                cs.beginText();
                cs.setFont(fontBold, 10.5f);
                cs.newLineAtOffset(margin, currentY);
                safeShowText(cs, fontBold, "TÓM TẮT (ABSTRACT):");
                cs.endText();
                currentY -= 14f;

                List<String> absLines = wrapText(doc.getSummary().trim(), fontRegular, 9.5f, usableWidth);
                for (String line : absLines) {
                    cs.beginText();
                    cs.setFont(fontRegular, 9.5f);
                    cs.newLineAtOffset(margin + 8f, currentY);
                    safeShowText(cs, fontRegular, line);
                    cs.endText();
                    currentY -= 13f;
                }
                currentY -= 10f;
            }

            // 4. Main Body Content (Multi-section, multi-page)
            String rawContent = doc.getContent() != null ? doc.getContent() : "";
            String[] paragraphs = rawContent.split("\n+");

            for (String para : paragraphs) {
                String trimmed = para.trim();
                if (trimmed.isEmpty()) continue;

                boolean isHeading = isHeadingLine(trimmed);
                float fontSize = isHeading ? 11f : 10f;
                PDFont currentFont = isHeading ? fontBold : fontRegular;
                float lineHeight = isHeading ? 16f : 14f;

                if (isHeading) {
                    currentY -= 8f; // Extra space before headings
                }

                List<String> lines = wrapText(trimmed, currentFont, fontSize, usableWidth);
                for (String line : lines) {
                    // Check page overflow
                    if (currentY - lineHeight < bottomLimit) {
                        cs.close();
                        currentPage = new PDPage(pageSize);
                        pdfDoc.addPage(currentPage);
                        cs = new PDPageContentStream(pdfDoc, currentPage);
                        currentY = startY;
                    }

                    cs.beginText();
                    cs.setFont(currentFont, fontSize);
                    cs.newLineAtOffset(margin, currentY);
                    safeShowText(cs, currentFont, line);
                    cs.endText();
                    currentY -= lineHeight;
                }

                currentY -= 5f; // Paragraph spacing
            }

            cs.close();
            pdfDoc.save(targetFile);
        }
    }

    /**
     * Creates an authentic formatted Microsoft Word document (.docx) using Apache POI.
     */
    private static void createRealDocx(Document doc, File targetFile) throws Exception {
        try (XWPFDocument wordDoc = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            // Title
            XWPFParagraph titlePara = wordDoc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(doc.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setColor("1E3A8A"); // Navy blue
            titleRun.setFontFamily("Segoe UI");

            // Metadata
            XWPFParagraph metaPara = wordDoc.createParagraph();
            XWPFRun metaRun = metaPara.createRun();
            metaRun.setText("Tác giả: " + (doc.getAuthor() != null ? doc.getAuthor() : "Thư viện số")
                    + "  •  Chủ đề: " + (doc.getTopic() != null ? doc.getTopic() : "Học thuật")
                    + "  •  Mã hệ thống: #" + doc.getId());
            metaRun.setFontSize(10);
            metaRun.setColor("64748B"); // Gray
            metaRun.setFontFamily("Segoe UI");
            metaRun.setItalic(true);

            // Abstract Callout
            if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
                XWPFParagraph absHeading = wordDoc.createParagraph();
                XWPFRun absHeadRun = absHeading.createRun();
                absHeadRun.setText("TÓM TẮT (ABSTRACT)");
                absHeadRun.setBold(true);
                absHeadRun.setFontSize(11);
                absHeadRun.setFontFamily("Segoe UI");

                XWPFParagraph absBody = wordDoc.createParagraph();
                absBody.setIndentationLeft(360); // 0.25 inch indent
                XWPFRun absRun = absBody.createRun();
                absRun.setText(doc.getSummary());
                absRun.setItalic(true);
                absRun.setFontSize(10);
                absRun.setFontFamily("Segoe UI");
            }

            // Body Paragraphs
            String rawContent = doc.getContent() != null ? doc.getContent() : "";
            String[] paragraphs = rawContent.split("\n+");

            for (String p : paragraphs) {
                String trimmed = p.trim();
                if (trimmed.isEmpty()) continue;

                XWPFParagraph bodyPara = wordDoc.createParagraph();
                XWPFRun bodyRun = bodyPara.createRun();
                bodyRun.setFontFamily("Segoe UI");

                if (isHeadingLine(trimmed)) {
                    bodyRun.setText(trimmed);
                    bodyRun.setBold(true);
                    bodyRun.setFontSize(12);
                    bodyRun.setColor("0F172A");
                } else {
                    bodyRun.setText(trimmed);
                    bodyRun.setFontSize(11);
                    bodyRun.setColor("334155");
                }
            }

            wordDoc.write(fos);
        }
    }

    /**
     * Creates a structured UTF-8 Plain Text file (.txt).
     */
    private static void createRealTxt(Document doc, File targetFile) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("======================================================================\n");
        sb.append("  HỆ THỐNG THƯ VIỆN SỐ - TÀI LIỆU LƯU TRỮ VẬT LÝ #").append(doc.getId()).append("\n");
        sb.append("======================================================================\n\n");
        sb.append("TIÊU ĐỀ: ").append(doc.getTitle()).append("\n");
        sb.append("TÁC GIẢ: ").append(doc.getAuthor() != null ? doc.getAuthor() : "Thư viện số").append("\n");
        sb.append("CHỦ ĐỀ:  ").append(doc.getTopic() != null ? doc.getTopic() : "Khoa học").append("\n");
        sb.append("ĐỊNH DẠNG GỐC: ").append(doc.getFileFormat()).append("  |  NGÔN NGỮ: ").append(doc.getLanguage()).append("\n\n");

        if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
            sb.append("----------------------------------------------------------------------\n");
            sb.append("TÓM TẮT (ABSTRACT):\n");
            sb.append(doc.getSummary()).append("\n");
            sb.append("----------------------------------------------------------------------\n\n");
        }

        sb.append("NỘI DUNG TOÀN VĂN:\n\n");
        sb.append(doc.getContent() != null ? doc.getContent() : "").append("\n");

        Files.writeString(targetFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Opens a document with Windows default native application (Acrobat/Edge, Word, Notepad).
     */
    public static boolean openWithDefaultApp(File file) {
        if (file == null || !file.exists()) return false;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error opening file with Desktop.open: " + e.getMessage());
        }
        return false;
    }

    /**
     * Opens Windows Explorer highlighting the file or opening the storage directory.
     */
    public static void revealInExplorer(File file) {
        try {
            if (file != null && file.exists()) {
                new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
            } else {
                new ProcessBuilder("explorer.exe", REPO_DIR.toAbsolutePath().toString()).start();
            }
        } catch (Exception e) {
            try {
                Desktop.getDesktop().open(REPO_DIR.toFile());
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean isHeadingLine(String line) {
        return line.matches("^[0-9]+[\\.\\)].*")
                || line.startsWith("BÁO CÁO")
                || line.startsWith("CÔNG TRÌNH")
                || line.startsWith("BÀI BÁO")
                || line.startsWith("NGHIÊN CỨU")
                || line.startsWith("SCIENTIFIC")
                || line.startsWith("1.") || line.startsWith("2.")
                || line.startsWith("3.") || line.startsWith("4.")
                || line.startsWith("5.");
    }

    private static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            float width = 0;
            try {
                width = font.getStringWidth(candidate) / 1000f * fontSize;
            } catch (Exception e) {
                width = candidate.length() * (fontSize * 0.55f);
            }

            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private static void safeShowText(PDPageContentStream cs, PDFont font, String text) {
        try {
            cs.showText(text);
        } catch (Exception e) {
            // Fallback: strip unencodable symbols
            StringBuilder safe = new StringBuilder();
            for (char c : text.toCharArray()) {
                try {
                    font.encode(String.valueOf(c));
                    safe.append(c);
                } catch (Exception ignored) {
                    safe.append(' ');
                }
            }
            try {
                cs.showText(safe.toString());
            } catch (Exception ignored) {
            }
        }
    }

    private static String slugify(String input) {
        if (input == null) return "doc";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String ascii = pattern.matcher(normalized).replaceAll("");
        ascii = ascii.replaceAll("[^a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "_");
        return ascii.isEmpty() ? "doc" : ascii;
    }
}
