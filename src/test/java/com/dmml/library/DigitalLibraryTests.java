package com.dmml.library;

import com.dmml.library.ml.clustering.KMeansClusterer;
import com.dmml.library.ml.evaluation.DaviesBouldinCalculator;
import com.dmml.library.ml.evaluation.SilhouetteCalculator;
import com.dmml.library.ml.feature.TfIdfVectorizer;
import com.dmml.library.ml.nlp.PorterStemmer;
import com.dmml.library.ml.nlp.TextPreprocessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DigitalLibraryTests {

    @Test
    public void testPorterStemmer() {
        PorterStemmer stemmer = new PorterStemmer();
        assertEquals("connect", stemmer.stemWord("connecting"));
        assertEquals("cluster", stemmer.stemWord("clustering"));
        assertEquals("comput", stemmer.stemWord("computer"));
    }

    @Test
    public void testTextPreprocessor() {
        TextPreprocessor preprocessor = new TextPreprocessor();
        String raw = "The space exploration mission reached Mars orbit in 2026!";
        List<String> tokens = preprocessor.preprocess(raw);

        assertNotNull(tokens);
        assertFalse(tokens.contains("the")); // stopword removed
        assertFalse(tokens.contains("in"));  // stopword removed
        assertTrue(tokens.contains("space"));
        assertTrue(tokens.contains("orbit"));
    }

    @Test
    public void testTfIdfAndClustering() {
        TextPreprocessor preprocessor = new TextPreprocessor();
        TfIdfVectorizer vectorizer = new TfIdfVectorizer();

        List<String> rawDocs = Arrays.asList(
                "Astronomy telescope observes distant galaxies and deep space stars",
                "Planetary rover explores Mars crater and orbital satellites in outer space",
                "Computer graphics rendering algorithms using OpenGL 3D ray tracing shaders",
                "GPU hardware acceleration for realtime graphics shader rasterization pipelines"
        );

        List<List<String>> tokenized = new ArrayList<>();
        for (String doc : rawDocs) {
            tokenized.add(preprocessor.preprocess(doc));
        }

        double[][] vectors = vectorizer.fitTransform(tokenized);
        assertEquals(4, vectors.length);

        KMeansClusterer clusterer = new KMeansClusterer();
        int[] assignments = clusterer.fitPredict(vectors, 2);
        assertEquals(4, assignments.length);

        // Doc 0 & 1 (space) should belong to the same cluster
        // Doc 2 & 3 (graphics) should belong to the same cluster
        assertEquals(assignments[0], assignments[1], "Space documents should share the same cluster");
        assertEquals(assignments[2], assignments[3], "Graphics documents should share the same cluster");
        assertNotEquals(assignments[0], assignments[2], "Space and Graphics should be in different clusters");

        // Test Silhouette & Davies-Bouldin
        SilhouetteCalculator silhouetteCalc = new SilhouetteCalculator();
        double sil = silhouetteCalc.computeScore(vectors, assignments);
        assertTrue(sil >= -1.0 && sil <= 1.0);

        DaviesBouldinCalculator dbCalc = new DaviesBouldinCalculator();
        double db = dbCalc.computeScore(vectors, assignments);
        assertTrue(db >= 0.0);
    }

    @Test
    public void testVietnamesePreprocessing() {
        TextPreprocessor preprocessor = new TextPreprocessor();
        String viText = "Nghiên cứu ứng dụng trí tuệ nhân tạo và học máy trong xử lý ngôn ngữ tự nhiên tiếng Việt!";
        List<String> tokens = preprocessor.preprocess(viText);

        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());
        // Verify accents are NOT stripped
        assertTrue(tokens.contains("trí") || tokens.contains("tuệ") || tokens.contains("trí_tuệ"),
                "Tokens should contain Vietnamese accented words: " + tokens);
        assertTrue(tokens.contains("học") || tokens.contains("máy") || tokens.contains("học_máy"));
        // Stopwords 'và', 'trong' should be removed
        assertFalse(tokens.contains("và"));
        assertFalse(tokens.contains("trong"));
    }

    @Test
    public void testFileReaderAndSampleGeneration() throws Exception {
        java.io.File samplesDir = new java.io.File("data/samples");
        if (!samplesDir.exists()) {
            samplesDir.mkdirs();
        }

        // 1. Generate & Test Sample TXT (Vietnamese Medical Paper)
        java.io.File txtFile = new java.io.File(samplesDir, "mau_y_duoc_tieng_viet.txt");
        String txtContent = "BÁO CÁO NGHIÊN CỨU LÂM SÀNG: HIỆU QUẢ CỦA PHÁC ĐỒ KHÁNG SINH THẾ HỆ MỚI\n\n" +
                "Tác giả: GS.TS. Trần Văn Y, Viện Nghiên Cứu Y Dược Học Lâm Sàng\n\n" +
                "TÓM TẮT:\n" +
                "Bài báo trình bày thử nghiệm lâm sàng ngẫu nhiên có đối chứng đánh giá hiệu quả diệt khuẩn, tính an toàn và khả năng dung nạp của kháng sinh phổ rộng thế hệ mới trong điều trị nhiễm trùng hô hấp và nhiễm khuẩn huyết nặng tại bệnh viện.\n\n" +
                "1. ĐẶT VẤN ĐỀ VÀ MỤC TIÊU\n" +
                "Tình trạng vi khuẩn gram âm kháng đa thuốc ngày càng gia tăng là thách thức nghiêm trọng đối với các khoa hồi sức tích cực. Các chủng vi khuẩn sinh enzym beta-lactamase phổ rộng (ESBL) và carbapenemase làm suy giảm đáng kể hiệu quả của các phác đồ kinh điển. Nghiên cứu này nhằm mục tiêu đánh giá tỷ lệ đáp ứng lâm sàng và sự an toàn khi áp dụng phác đồ kháng sinh phối hợp mới.\n\n" +
                "2. ĐỐI TƯỢNG VÀ PHƯƠNG PHÁP NGHIÊN CỨU\n" +
                "Thử nghiệm được thực hiện trên 80 bệnh nhân nội trú từ 20 đến 75 tuổi có chẩn đoán nhiễm khuẩn huyết hoặc viêm phổi bệnh viện. Bệnh nhân được chia thành hai nhánh điều trị: nhánh can thiệp nhận kháng sinh thế hệ mới phối hợp truyền tĩnh mạch liên tục, nhánh đối chứng áp dụng phác đồ chuẩn. Các chỉ số men gan, độ thanh thải creatinin và nồng độ procalcitonin được theo dõi chặt chẽ tại các mốc 24h, 48h, 72h và ngày thứ 7.\n\n" +
                "3. KẾT QUẢ ĐẠT ĐƯỢC\n" +
                "Kết quả cho thấy tỷ lệ làm sạch khuẩn máu sau 72 giờ ở nhóm can thiệp đạt 82.5%, cao hơn có ý nghĩa thống kê so với nhóm đối chứng (57.5%, p < 0.05). Thời gian hạ sốt trung bình rút ngắn từ 4.2 ngày xuống còn 2.6 ngày. Không ghi nhận trường hợp nào suy thận cấp tính hoặc phản ứng dị ứng nghiêm trọng cần ngừng thuốc.\n\n" +
                "4. KẾT LUẬN VÀ KHUYẾN CÁO\n" +
                "Phác đồ kháng sinh thế hệ mới mang lại hiệu quả điều trị vượt trội và độ an toàn cao, là lựa chọn khả thi trong phác đồ điều trị cứu vãn cho bệnh nhân nhiễm khuẩn nặng.";
        java.nio.file.Files.writeString(txtFile.toPath(), txtContent, java.nio.charset.StandardCharsets.UTF_8);

        com.dmml.library.util.DocumentFileReader.ParsedDocument parsedTxt =
                com.dmml.library.util.DocumentFileReader.readFile(txtFile);
        assertNotNull(parsedTxt);
        assertEquals("VI", parsedTxt.getLanguage());
        assertEquals("TXT", parsedTxt.getFileFormat());
        assertTrue(parsedTxt.getContent().contains("kháng sinh"));

        // 2. Generate & Test Sample DOCX (Vietnamese AI & Machine Learning Paper)
        java.io.File docxFile = new java.io.File(samplesDir, "mau_tri_tue_nhan_tao.docx");
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             java.io.FileOutputStream fos = new java.io.FileOutputStream(docxFile)) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("NGHIÊN CỨU MÔ HÌNH HỌC MÁY VÀ MẠNG NƠ-RON TÍCH CHẬP TRONG PHÂN TÍCH VĂN BẢN");
            
            org.apache.poi.xwpf.usermodel.XWPFParagraph pAuth = doc.createParagraph();
            pAuth.createRun().setText("Tác giả: Nhóm Nghiên Cứu Trí Tuệ Nhân Tạo & Xử Lý Ngôn Ngữ Tự Nhiên");

            org.apache.poi.xwpf.usermodel.XWPFParagraph pAbs = doc.createParagraph();
            pAbs.createRun().setText("Tóm tắt: Bài viết khảo sát kiến trúc mạng nơ-ron học sâu kết hợp xử lý ngôn ngữ tự nhiên tiếng Việt và thuật toán phân cụm văn bản K-Means để tự động phân loại kho tài liệu thư viện số quy mô lớn.");

            org.apache.poi.xwpf.usermodel.XWPFParagraph pSec1 = doc.createParagraph();
            pSec1.createRun().setText("1. Tổng quan nghiên cứu: Trí tuệ nhân tạo và học máy đã chứng minh khả năng vượt trội trong việc phân loại tài liệu học thuật số hóa. Bằng việc kết hợp các kỹ thuật trích xuất đặc trưng TF-IDF và vector nhúng ngữ nghĩa, hệ thống có thể nhận diện các chủ đề khoa học một cách chính xác.");

            org.apache.poi.xwpf.usermodel.XWPFParagraph pSec2 = doc.createParagraph();
            pSec2.createRun().setText("2. Phương pháp thực nghiệm: Dữ liệu văn bản tiếng Việt được tiền xử lý qua các bước chuẩn hóa ký tự Unicode, tách từ ghép, loại bỏ từ dừng và tính ma trận TF-IDF 1.500 chiều. Thuật toán K-Means kết hợp khoảng cách Cosine phân chia tài liệu thành các cụm chuyên đề độc lập.");

            org.apache.poi.xwpf.usermodel.XWPFParagraph pSec3 = doc.createParagraph();
            pSec3.createRun().setText("3. Kết luận: Mô hình đạt độ tinh khiết phân cụm (Purity) cao và thời gian hội tụ nhanh, phù hợp cho việc triển khai vào các thư viện điện tử thông minh.");

            doc.write(fos);
        }

        com.dmml.library.util.DocumentFileReader.ParsedDocument parsedDocx =
                com.dmml.library.util.DocumentFileReader.readFile(docxFile);
        assertNotNull(parsedDocx);
        assertEquals("DOCX", parsedDocx.getFileFormat());
        assertTrue(parsedDocx.getContent().contains("Trí tuệ nhân tạo"));

        // 3. Generate & Test Sample PDF (Full Multi-Section Space & Astronomy Report)
        java.io.File pdfFile = new java.io.File(samplesDir, "mau_thien_van_vu_tru.pdf");
        try (org.apache.pdfbox.pdmodel.PDDocument pdfDoc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            pdfDoc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(pdfDoc, page)) {
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 13);
                cs.newLineAtOffset(50, 740);
                cs.showText("SCIENTIFIC REPORT: MARS ATMOSPHERIC EXPLORATION AND SPECTROSCOPY");

                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 10);
                cs.newLineAtOffset(0, -18);
                cs.showText("Author: Dr. Vu Quoc Hung - Space Science Center and Planetary Research Group");

                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(0, -22);
                cs.showText("ABSTRACT:");

                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9.5f);
                String[] pdfLines = {
                    "This research presents orbital spectrometer data collected from Mars rover exploration missions.",
                    "We analyze high-resolution spectral infrared signals to detect trace atmospheric gases",
                    "including seasonal fluctuations of methane (CH4) and carbon dioxide isotopes across the Martian surface.",
                    "",
                    "1. INTRODUCTION AND MISSION OVERVIEW",
                    "Deep space planetary exploration provides key insights into the historical evolution",
                    "of solar system atmospheres. Recent orbiter spectrometry has identified localized plume",
                    "emissions in the Gale Crater and Jezero Crater regions during northern summer seasons.",
                    "",
                    "2. SENSOR PAYLOAD AND SPECTRAL CALIBRATION",
                    "The planetary probe carries a Fourier Transform Infrared Spectrometer (FTIR) operating",
                    "across the 2.0 to 5.0 micrometer wavelength range. Real-time digital signal processing",
                    "algorithms filter thermal solar radiance and Martian surface mineral reflection noise.",
                    "",
                    "3. DATA OBSERVATIONS AND METHANE DETECTION",
                    "Spectroscopic inversion confirms background atmospheric methane concentration baseline",
                    "at 0.41 plus-or-minus 0.15 parts per billion by volume (ppbv). Transient spike peaks",
                    "exceeding 2.1 ppbv were recorded during sol 1420 to 1485, suggesting subsurface release.",
                    "",
                    "4. CONCLUSION AND FUTURE EXPLORATION",
                    "These findings validate satellite spectrometry as an essential tool for deep planetary science.",
                    "Subsequent interplanetary missions will deploy sub-surface seismic and drill probes."
                };
                for (String line : pdfLines) {
                    cs.newLineAtOffset(0, -15);
                    if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.") || line.startsWith("4.")) {
                        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 9.5f);
                    } else {
                        cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 9.5f);
                    }
                    cs.showText(line);
                }
                cs.endText();
            }
            pdfDoc.save(pdfFile);
        }

        com.dmml.library.util.DocumentFileReader.ParsedDocument parsedPdf =
                com.dmml.library.util.DocumentFileReader.readFile(pdfFile);
        assertNotNull(parsedPdf);
        assertEquals("PDF", parsedPdf.getFileFormat());
        assertTrue(parsedPdf.getContent().contains("atmospheric"));
    }

    @Test
    public void testManualClusterReassignment() {
        com.dmml.library.service.DocumentRepository repo = com.dmml.library.service.DocumentRepository.getInstance();
        com.dmml.library.model.Document doc = new com.dmml.library.model.Document();
        doc.setTitle("Bài Viết Thử Nghiệm Sửa Cụm");
        doc.setContent("Nội dung tài liệu kiểm tra tính năng can thiệp sửa cụm khi thuật toán gán chưa đúng ý.");
        doc.setClusterId(0);
        doc = repo.save(doc);

        com.dmml.library.service.ClusteringService service = com.dmml.library.service.ClusteringService.getInstance();
        service.manualReassignCluster(doc.getId(), 3, "Y Học Lâm Sàng (Thủ Công)");

        com.dmml.library.model.Document updated = repo.findById(doc.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(3, updated.getClusterId());
        assertTrue(updated.isManuallyAssigned());

        // Clean up test document
        repo.deleteById(doc.getId());
    }

    @Test
    public void testClusterRenamingAndReset() {
        com.dmml.library.service.ClusteringService service = com.dmml.library.service.ClusteringService.getInstance();
        service.renameCluster(0, "Tên Tùy Chỉnh: An Ninh Nâng Cao", "Bảo mật mạng, Khóa mật mã");
        assertEquals("Tên Tùy Chỉnh: An Ninh Nâng Cao", service.getClusterDisplayName(0));

        service.resetClusterName(0);
        assertTrue(service.getClusterDisplayName(0).contains("An Ninh Mạng & Mật Mã"));
    }

    @Test
    public void testPhysicalDocumentManager() {
        com.dmml.library.model.Document docPdf = new com.dmml.library.model.Document();
        docPdf.setId(8881);
        docPdf.setTitle("Nghiên Cứu Thử Nghiệm Tạo File PDF Vật Lý");
        docPdf.setAuthor("TS. Kiểm Thử Hệ Thống");
        docPdf.setTopic("Trí Tuệ Nhân Tạo & Học Máy");
        docPdf.setSummary("Tóm tắt thử nghiệm tạo file PDF thực tế trên đĩa cứng.");
        docPdf.setContent("1. ĐẶT VẤN ĐỀ\nNội dung toàn văn của bài báo khoa học PDF thực tế có đầy đủ dấu tiếng Việt.\n\n2. KẾT LUẬN\nFile PDF được tạo thành công với định dạng chuẩn.");
        docPdf.setFileFormat("PDF");
        docPdf.setLanguage("VI");

        java.io.File pdfFile = com.dmml.library.util.PhysicalDocumentManager.ensurePhysicalFile(docPdf);
        assertNotNull(pdfFile);
        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 500);
        assertTrue(pdfFile.getName().endsWith(".pdf"));

        com.dmml.library.model.Document docDocx = new com.dmml.library.model.Document();
        docDocx.setId(8882);
        docDocx.setTitle("Nghiên Cứu Thử Nghiệm Tạo File DOCX Vật Lý");
        docDocx.setAuthor("ThS. Kiểm Thử Word");
        docDocx.setTopic("Y Học & Dược Phẩm");
        docDocx.setSummary("Tóm tắt thử nghiệm tạo file DOCX trên đĩa cứng.");
        docDocx.setContent("1. TỔNG QUAN\nNội dung toàn văn file Word thử nghiệm.\n\n2. KẾT QUẢ\nThực hiện thành công.");
        docDocx.setFileFormat("DOCX");
        docDocx.setLanguage("VI");

        java.io.File docxFile = com.dmml.library.util.PhysicalDocumentManager.ensurePhysicalFile(docDocx);
        assertNotNull(docxFile);
        assertTrue(docxFile.exists());
        assertTrue(docxFile.length() > 500);
        assertTrue(docxFile.getName().endsWith(".docx"));
    }

    @Test
    public void testDebugCheckDocuments() {
        com.dmml.library.service.DocumentRepository repo = com.dmml.library.service.DocumentRepository.getInstance();
        com.dmml.library.service.ClusteringService service = com.dmml.library.service.ClusteringService.getInstance();
        com.dmml.library.model.ClusteringResult res = service.runClustering("kmeans", 6);
        
        System.out.println("=== CLUSTER BREAKDOWN ===");
        for (com.dmml.library.model.ClusterInfo c : res.getClusters()) {
            long viCount = c.getDocuments().stream().filter(d -> "VI".equalsIgnoreCase(d.getLanguage())).count();
            java.util.Map<String, Long> catCounts = c.getDocuments().stream().collect(java.util.stream.Collectors.groupingBy(d -> d.getOriginalCategory() != null ? d.getOriginalCategory() : "null", java.util.stream.Collectors.counting()));
            System.out.println("Cluster " + c.getClusterId() + " (" + c.getClusterName() + ") : Total=" + c.getDocumentCount() + " (VI=" + viCount + ") | Categories: " + catCounts + " | Keywords: " + c.getTopKeywords());
        }
        
        List<com.dmml.library.model.Document> viDocs = repo.search("", null, "VI");
        System.out.println("=== VI DOCS FOUND: " + viDocs.size() + " ===");
        double[][] centroids = service.getKMeansClusterer().getCentroids();
        for (com.dmml.library.model.Document d : viDocs) {
            double[] vec = service.getDocumentVector(d.getId());
            StringBuilder sims = new StringBuilder();
            if (vec != null && centroids != null) {
                for (int c = 0; c < centroids.length; c++) {
                    double sim = com.dmml.library.ml.clustering.KMeansClusterer.cosineSimilarity(vec, centroids[c]);
                    sims.append(String.format(" C%d=%.3f", c, sim));
                }
            }
            System.out.println("-> ID: " + d.getId() + " | Cat: " + d.getOriginalCategory() + " | Cluster: " + d.getClusterId() + " | Sims:" + sims);
        }
        assertEquals(24, viDocs.size(), "Should have exactly 24 Vietnamese documents");
        for (com.dmml.library.model.ClusterInfo c : res.getClusters()) {
            long viCount = c.getDocuments().stream().filter(d -> "VI".equalsIgnoreCase(d.getLanguage())).count();
            assertTrue(viCount > 0, "Cluster " + c.getClusterId() + " must have Vietnamese documents, but found 0");
        }
    }
}
