package com.dmml.library.ui;

import com.dmml.library.model.ClusterInfo;
import com.dmml.library.model.ClusteringResult;
import com.dmml.library.model.Document;
import com.dmml.library.service.ClusteringService;
import com.dmml.library.service.DocumentRepository;
import com.dmml.library.util.DocumentFileReader;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Modern Add Document Panel:
 * - Clean layout with ZERO overlap (dedicated rows for file actions and file status)
 * - Safe standard Unicode text rendering without missing glyph boxes (□)
 * - File Import support: PDF, Word DOCX, Plain Text, JSON
 * - Automatic language detection ([VI] Tiếng Việt / [EN] Tiếng Anh)
 * - AI Confidence Meter & Manual Cluster Override
 */
public class AddDocumentPanel extends JPanel {

    private final MainLibraryFrame mainFrame;
    private final DocumentRepository repository;
    private final ClusteringService clusteringService;

    private JTextField txtTitle;
    private JTextField txtAuthor;
    private JTextField txtSummary;
    private JTextArea txtContent;

    private JLabel lblFileStatus;
    private JLabel lblPredictedCluster;
    private JProgressBar progressConfidence;
    private JLabel lblConfidenceText;
    private JLabel lblKeywords;
    private JComboBox<ClusterChoiceItem> comboFinalCluster;
    private JLabel lblLanguageBadge;

    private String currentFileFormat = "MANUAL";
    private String currentLanguage = "EN";
    private String currentFilePath = null;

    public AddDocumentPanel(MainLibraryFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.repository = DocumentRepository.getInstance();
        this.clusteringService = ClusteringService.getInstance();

        setLayout(new BorderLayout(14, 14));
        setBorder(new EmptyBorder(14, 18, 14, 18));

        initUI();
    }

    private void initUI() {
        // --- TOP: HEADER & IMPORT BAR ---
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel lblHeader = new JLabel("Thêm Tài Liệu Mới & Tự Động Phân Cụm (Hỗ Trợ File PDF, DOCX, TXT, Tiếng Việt)");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeader.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Tự động trích xuất nội dung từ file, nhận diện ngôn ngữ tiếng Việt, tính TF-IDF và gợi ý cụm chủ đề.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        headerPanel.add(lblHeader, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        topContainer.add(headerPanel);
        topContainer.add(Box.createVerticalStrut(10));

        // File Import & Samples Card (2 DEDICATED ROWS TO PREVENT ANY COLLISION)
        JPanel importCard = new JPanel();
        importCard.setLayout(new BoxLayout(importCard, BoxLayout.Y_AXIS));
        importCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        importCard.setBackground(new Color(248, 250, 252));

        // Row 1: Buttons
        JPanel row1Buttons = new JPanel(new BorderLayout(10, 0));
        row1Buttons.setOpaque(false);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtons.setOpaque(false);

        JButton btnChooseFile = new JButton("Chọn File Từ Máy Tính (PDF, DOCX, TXT, JSON)");
        btnChooseFile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChooseFile.setBackground(new Color(37, 99, 235));
        btnChooseFile.setForeground(Color.WHITE);
        btnChooseFile.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnChooseFile.addActionListener(e -> chooseDocumentFile());

        JButton btnOpenSampleFolder = new JButton("Mở Thư Mục File Mẫu (data/samples)");
        btnOpenSampleFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenSampleFolder.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnOpenSampleFolder.addActionListener(e -> openSampleFolder());

        leftButtons.add(btnChooseFile);
        leftButtons.add(btnOpenSampleFolder);

        JPanel rightSamples = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightSamples.setOpaque(false);

        JLabel lblSamplePrompt = new JLabel("Điền mẫu nhanh:");
        lblSamplePrompt.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSamplePrompt.setForeground(new Color(100, 116, 139));

        JButton btnSampleViAI = new JButton("Mẫu: AI & Học Máy (VI)");
        btnSampleViAI.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSampleViAI.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSampleViAI.addActionListener(e -> fillSampleVietnameseAI());

        JButton btnSampleViMed = new JButton("Mẫu: Y Học & Dược (VI)");
        btnSampleViMed.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSampleViMed.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSampleViMed.addActionListener(e -> fillSampleVietnameseMedical());

        JButton btnSampleEnSpace = new JButton("Mẫu: Space Astronomy (EN)");
        btnSampleEnSpace.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSampleEnSpace.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSampleEnSpace.addActionListener(e -> fillSampleEnglishSpace());

        rightSamples.add(lblSamplePrompt);
        rightSamples.add(btnSampleViAI);
        rightSamples.add(btnSampleViMed);
        rightSamples.add(btnSampleEnSpace);

        row1Buttons.add(leftButtons, BorderLayout.WEST);
        row1Buttons.add(rightSamples, BorderLayout.EAST);
        importCard.add(row1Buttons);
        importCard.add(Box.createVerticalStrut(6));

        // Row 2: Status Line (Full width - CAN NEVER OVERLAP WITH BUTTONS)
        JPanel row2Status = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row2Status.setOpaque(false);

        lblFileStatus = new JLabel("Trạng thái file: Chưa chọn file (Nhập tay vào form bên dưới hoặc bấm nút chọn file ở trên)");
        lblFileStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFileStatus.setForeground(new Color(100, 116, 139));
        row2Status.add(lblFileStatus);

        importCard.add(row2Status);
        topContainer.add(importCard);

        add(topContainer, BorderLayout.NORTH);

        // --- CENTER: TWO COLUMN LAYOUT (FORM on LEFT, AI CLUSTERING on RIGHT) ---
        JPanel centerSplit = new JPanel(new GridLayout(1, 2, 16, 0));

        // Column 1: Document Form Fields
        JPanel formCol = new JPanel();
        formCol.setLayout(new BoxLayout(formCol, BoxLayout.Y_AXIS));
        formCol.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(226, 232, 240), 1, true), " Thông Tin Tài Liệu / Sách "),
                new EmptyBorder(8, 12, 10, 12)
        ));

        // Title row with language badge
        JPanel titleLabelRow = new JPanel(new BorderLayout());
        titleLabelRow.setOpaque(false);
        titleLabelRow.add(createFieldLabel("Tiêu đề tài liệu: *"), BorderLayout.WEST);

        lblLanguageBadge = new JLabel("[EN] Tiếng Anh");
        lblLanguageBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLanguageBadge.setForeground(new Color(37, 99, 235));
        titleLabelRow.add(lblLanguageBadge, BorderLayout.EAST);
        formCol.add(titleLabelRow);

        txtTitle = new JTextField();
        txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTitle.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtTitle.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tiêu đề hoặc nạp tự động từ file...");
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        formCol.add(txtTitle);
        formCol.add(Box.createVerticalStrut(8));

        // Author & Abstract
        JPanel rowAuthor = new JPanel(new BorderLayout(0, 4));
        rowAuthor.setOpaque(false);
        rowAuthor.add(createFieldLabel("Tác giả / Nguồn tài liệu:"), BorderLayout.NORTH);
        txtAuthor = new JTextField();
        txtAuthor.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtAuthor.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtAuthor.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Ví dụ: TS. Nguyễn Văn An, NASA...");
        txtAuthor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        rowAuthor.add(txtAuthor, BorderLayout.CENTER);
        formCol.add(rowAuthor);
        formCol.add(Box.createVerticalStrut(8));

        JPanel rowSummary = new JPanel(new BorderLayout(0, 4));
        rowSummary.setOpaque(false);
        rowSummary.add(createFieldLabel("Tóm tắt ngắn (Abstract):"), BorderLayout.NORTH);
        txtSummary = new JTextField();
        txtSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSummary.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtSummary.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tóm tắt ngắn gọn 1-2 câu về nội dung...");
        txtSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        rowSummary.add(txtSummary, BorderLayout.CENTER);
        formCol.add(rowSummary);
        formCol.add(Box.createVerticalStrut(8));

        // Full Content
        formCol.add(createFieldLabel("Nội dung toàn văn: *"));
        txtContent = new JTextArea(10, 30);
        txtContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        JScrollPane scrollContent = new JScrollPane(txtContent);
        formCol.add(scrollContent);

        centerSplit.add(formCol);

        // Column 2: AI Cluster Prediction & Manual Override
        JPanel aiCol = new JPanel(new BorderLayout(10, 10));
        aiCol.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(37, 99, 235), 2, true), " Phân Cụm Tự Động & Tùy Chỉnh Gán Nhóm "),
                new EmptyBorder(12, 14, 12, 14)
        ));
        aiCol.setBackground(new Color(248, 250, 255));

        JPanel aiContent = new JPanel();
        aiContent.setLayout(new BoxLayout(aiContent, BoxLayout.Y_AXIS));
        aiContent.setOpaque(false);

        // Prediction Box
        JPanel predBox = new JPanel(new GridLayout(3, 1, 4, 4));
        predBox.setOpaque(false);

        lblPredictedCluster = new JLabel("Cụm AI dự đoán: (Chưa phân tích)");
        lblPredictedCluster.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPredictedCluster.setForeground(new Color(37, 99, 235));

        JPanel confRow = new JPanel(new BorderLayout(8, 0));
        confRow.setOpaque(false);
        lblConfidenceText = new JLabel("Độ tương đồng: 0.0%");
        lblConfidenceText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        progressConfidence = new JProgressBar(0, 100);
        progressConfidence.setValue(0);
        progressConfidence.setStringPainted(true);
        progressConfidence.setPreferredSize(new Dimension(140, 18));
        progressConfidence.setForeground(new Color(16, 185, 129));
        confRow.add(lblConfidenceText, BorderLayout.WEST);
        confRow.add(progressConfidence, BorderLayout.EAST);

        lblKeywords = new JLabel("Từ khóa đặc trưng của cụm: Không có");
        lblKeywords.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKeywords.setForeground(new Color(71, 85, 105));

        predBox.add(lblPredictedCluster);
        predBox.add(confRow);
        predBox.add(lblKeywords);

        aiContent.add(predBox);
        aiContent.add(Box.createVerticalStrut(16));
        aiContent.add(new JSeparator());
        aiContent.add(Box.createVerticalStrut(16));

        // --- MANUAL OVERRIDE SECTION ---
        JPanel overridePanel = new JPanel();
        overridePanel.setLayout(new BoxLayout(overridePanel, BoxLayout.Y_AXIS));
        overridePanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(16, 185, 129), 1, true), " Can Thiệp Sửa Cụm (Nếu Không Đúng Ý) "),
                new EmptyBorder(8, 10, 8, 10)
        ));
        overridePanel.setBackground(new Color(240, 253, 244));

        JLabel lblOverrideDesc = new JLabel("<html>Nếu AI gán cụm chưa đúng ý bạn, hãy chọn cụm mong muốn bên dưới trước khi lưu:</html>");
        lblOverrideDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblOverrideDesc.setForeground(new Color(55, 65, 81));
        overridePanel.add(lblOverrideDesc);
        overridePanel.add(Box.createVerticalStrut(6));

        comboFinalCluster = new JComboBox<>();
        comboFinalCluster.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboFinalCluster.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        populateClusterDropdown();
        overridePanel.add(comboFinalCluster);

        aiContent.add(overridePanel);
        aiContent.add(Box.createVerticalGlue());

        aiCol.add(aiContent, BorderLayout.CENTER);

        // AI Action button inside column
        JButton btnPreview = new JButton("Dự Đoán Cụm Thử (Phân Tích TF-IDF)");
        btnPreview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPreview.setBackground(new Color(37, 99, 235));
        btnPreview.setForeground(Color.WHITE);
        btnPreview.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnPreview.addActionListener(e -> previewCluster());

        aiCol.add(btnPreview, BorderLayout.SOUTH);

        centerSplit.add(aiCol);
        add(centerSplit, BorderLayout.CENTER);

        // --- BOTTOM: MAIN ACTION BUTTONS ---
        JPanel bottomBar = new JPanel(new BorderLayout());

        JPanel bottomLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton btnClear = new JButton("Làm Mới Form");
        btnClear.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnClear.addActionListener(e -> clearForm());
        bottomLeft.add(btnClear);

        JPanel bottomRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton btnSave = new JButton("Lưu Vào Thư Viện Số");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSave.addActionListener(e -> saveDocument());

        bottomRight.add(btnSave);

        bottomBar.add(bottomLeft, BorderLayout.WEST);
        bottomBar.add(bottomRight, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    public void populateClusterDropdown() {
        comboFinalCluster.removeAllItems();
        ClusteringResult res = clusteringService.getLatestResult();
        if (res != null && res.getClusters() != null) {
            for (ClusterInfo c : res.getClusters()) {
                comboFinalCluster.addItem(new ClusterChoiceItem(c.getClusterId(), c.getClusterName()));
            }
        } else {
            for (int i = 0; i < 6; i++) {
                comboFinalCluster.addItem(new ClusterChoiceItem(i, "Chủ Đề #" + (i + 1)));
            }
        }
    }

    private void chooseDocumentFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn Tài Liệu Cần Thêm (PDF, DOCX, TXT, JSON, MD)");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Tất cả định dạng tài liệu (*.pdf, *.docx, *.txt, *.json, *.md)",
                "pdf", "docx", "txt", "json", "md", "csv"));

        File samplesDir = new File("data/samples");
        if (samplesDir.exists()) {
            chooser.setCurrentDirectory(samplesDir);
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            loadFromFile(selectedFile);
        }
    }

    private void openSampleFolder() {
        File samplesDir = new File("data/samples");
        if (!samplesDir.exists()) {
            samplesDir.mkdirs();
        }
        JFileChooser chooser = new JFileChooser(samplesDir);
        chooser.setDialogTitle("Chọn file mẫu từ data/samples");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            loadFromFile(chooser.getSelectedFile());
        }
    }

    private void loadFromFile(File file) {
        try {
            currentFilePath = file.getAbsolutePath();
            DocumentFileReader.ParsedDocument parsed = DocumentFileReader.readFile(file);
            txtTitle.setText(parsed.getTitle());
            txtAuthor.setText(parsed.getAuthor());
            txtSummary.setText(parsed.getSummary());
            txtContent.setText(parsed.getContent());

            currentFileFormat = parsed.getFileFormat();
            currentLanguage = parsed.getLanguage();

            updateLanguageBadge();
            String langDesc = "VI".equals(currentLanguage) ? "Tiếng Việt" : "Tiếng Anh";
            lblFileStatus.setText("Trạng thái file: Đã nạp \"" + file.getName() + "\" (" + currentFileFormat + ")  •  Ngôn ngữ: " + langDesc);
            lblFileStatus.setForeground(new Color(16, 185, 129));

            // Automatically run AI clustering preview
            previewCluster();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc file: " + ex.getMessage(), "Lỗi Trích Xuất File", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateLanguageBadge() {
        if ("VI".equals(currentLanguage)) {
            lblLanguageBadge.setText("[VI] Tiếng Việt");
            lblLanguageBadge.setForeground(new Color(220, 38, 38));
        } else {
            lblLanguageBadge.setText("[EN] Tiếng Anh");
            lblLanguageBadge.setForeground(new Color(37, 99, 235));
        }
    }

    private void previewCluster() {
        String title = txtTitle.getText().trim();
        String content = txtContent.getText().trim();

        if (title.isEmpty() && content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tiêu đề hoặc nội dung trước khi dự đoán!", "Cảnh Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Auto-detect language if not set
        currentLanguage = DocumentFileReader.detectLanguage(title + " " + content);
        updateLanguageBadge();

        Map<String, Object> pred = clusteringService.autoClassifyDocument(title, content);

        int predId = (int) pred.getOrDefault("predictedClusterId", 0);
        String clusterName = (String) pred.getOrDefault("clusterName", "Chưa phân cụm");
        double confidence = (double) pred.getOrDefault("confidence", 0.0);
        @SuppressWarnings("unchecked")
        List<String> kws = (List<String>) pred.getOrDefault("topKeywords", List.of());

        lblPredictedCluster.setText("Cụm AI dự đoán: " + clusterName);
        lblConfidenceText.setText("Độ tương đồng: " + confidence + "%");
        progressConfidence.setValue((int) Math.min(100, Math.round(confidence)));
        lblKeywords.setText("Từ khóa đặc trưng: " + (kws.isEmpty() ? "Không có" : String.join(", ", kws)));

        // Auto-select predicted cluster in the override combo box
        for (int i = 0; i < comboFinalCluster.getItemCount(); i++) {
            if (comboFinalCluster.getItemAt(i).clusterId == predId) {
                comboFinalCluster.setSelectedIndex(i);
                break;
            }
        }
    }

    private void saveDocument() {
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String summary = txtSummary.getText().trim();
        String content = txtContent.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề và Nội dung tài liệu không được để trống!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (author.isEmpty()) author = "Thủ thư số";
        if (summary.isEmpty()) {
            summary = content.length() > 180 ? content.substring(0, 177) + "..." : content;
        }

        // Check if user chose a manual cluster or accepts predicted
        ClusterChoiceItem finalChoice = (ClusterChoiceItem) comboFinalCluster.getSelectedItem();
        int finalClusterId = (finalChoice != null) ? finalChoice.clusterId : 0;
        String finalClusterName = (finalChoice != null) ? finalChoice.clusterName : clusteringService.getClusterDisplayName(finalClusterId);

        Document doc = new Document();
        doc.setTitle(title);
        doc.setAuthor(author);
        doc.setSummary(summary);
        doc.setContent(content);
        doc.setTopic(finalClusterName);
        doc.setClusterId(finalClusterId);
        doc.setFileFormat(currentFileFormat);
        doc.setLanguage(currentLanguage);
        doc.setFilePath(currentFilePath);

        // Check if user altered cluster from AI prediction
        Map<String, Object> pred = clusteringService.autoClassifyDocument(title, content);
        int predId = (int) pred.getOrDefault("predictedClusterId", 0);
        boolean manuallyOverridden = (finalClusterId != predId);
        doc.setManuallyAssigned(manuallyOverridden);

        Document saved = repository.save(doc);
        clusteringService.manualReassignCluster(saved.getId(), finalClusterId, finalClusterName);
        clusteringService.registerDocumentVector(saved);

        JOptionPane.showMessageDialog(this,
                "Lưu tài liệu thành công vào Thư viện số!\n" +
                "- Mã tài liệu: #" + saved.getId() + "\n" +
                "- Cụm phân nhóm: " + finalClusterName + (manuallyOverridden ? " (Người dùng tự gán)" : " (AI tự gán)") + "\n" +
                "- Ngôn ngữ: " + ("VI".equals(currentLanguage) ? "Tiếng Việt" : "Tiếng Anh") + "\n" +
                "- Định dạng nguồn: " + currentFileFormat,
                "Thành Công", JOptionPane.INFORMATION_MESSAGE);

        clearForm();
        mainFrame.refreshCatalog();
    }

    private void clearForm() {
        txtTitle.setText("");
        txtAuthor.setText("");
        txtSummary.setText("");
        txtContent.setText("");
        currentFileFormat = "MANUAL";
        currentLanguage = "EN";
        currentFilePath = null;
        updateLanguageBadge();
        lblFileStatus.setText("Trạng thái file: Chưa chọn file (Nhập tay hoặc bấm chọn file)");
        lblFileStatus.setForeground(new Color(100, 116, 139));
        lblPredictedCluster.setText("Cụm AI dự đoán: (Chưa phân tích)");
        lblConfidenceText.setText("Độ tương đồng: 0.0%");
        progressConfidence.setValue(0);
        lblKeywords.setText("Từ khóa đặc trưng của cụm: Không có");
        if (comboFinalCluster.getItemCount() > 0) {
            comboFinalCluster.setSelectedIndex(0);
        }
    }

    private void fillSampleVietnameseAI() {
        txtTitle.setText("Ứng Dụng Mạng Nơ-ron Tích Chập Và Học Máy Trong Xử Lý Văn Bản Tiếng Việt");
        txtAuthor.setText("TS. Nguyễn Văn An, ĐHQG Hà Nội");
        txtSummary.setText("Nghiên cứu kiến trúc mô hình học sâu kết hợp xử lý ngôn ngữ tự nhiên tiếng Việt và phân cụm tài liệu tự động.");
        txtContent.setText("Trí tuệ nhân tạo và học máy đã chứng minh khả năng vượt trội trong việc phân loại tài liệu số hóa. " +
                "Bằng việc kết hợp kỹ thuật tiền xử lý văn bản tiếng Việt gồm tách từ, loại bỏ từ dừng và tính ma trận TF-IDF, " +
                "thuật toán phân cụm K-Means cho phép nhóm các bài báo khoa học có cùng chủ đề về công nghệ thông tin và học sâu với độ chính xác cao.");
        currentFileFormat = "DOCX";
        currentLanguage = "VI";
        updateLanguageBadge();
        lblFileStatus.setText("Trạng thái file: Đã điền mẫu Tiếng Việt (Trí Tuệ Nhân Tạo & Học Máy)");
        lblFileStatus.setForeground(new Color(16, 185, 129));
        previewCluster();
    }

    private void fillSampleVietnameseMedical() {
        txtTitle.setText("Nghiên Cứu Thử Nghiệm Lâm Sàng Hiệu Quả Kháng Sinh Và Chẩn Đoán Bệnh");
        txtAuthor.setText("BS.CKII. Phạm Đức Thịnh, Bệnh Viện Bạch Mai");
        txtSummary.setText("Đánh giá phác đồ điều trị kháng sinh thế hệ mới ở bệnh nhân nhiễm khuẩn huyết và bệnh lý hô hấp.");
        txtContent.setText("Nghiên cứu lâm sàng thực hiện trên 120 bệnh nhân nhằm khảo sát độc tính và khả năng đáp ứng miễn dịch " +
                "của kháng sinh phổ rộng. Các chỉ số sinh hóa máu, số lượng bạch cầu và tình trạng hồi phục của bệnh nhân được theo dõi liên tục " +
                "tại khoa hồi sức tích cực, mang lại cơ sở khoa học quan trọng cho phác đồ điều trị y học hiện đại.");
        currentFileFormat = "PDF";
        currentLanguage = "VI";
        updateLanguageBadge();
        lblFileStatus.setText("Trạng thái file: Đã điền mẫu Tiếng Việt (Y Học & Sức Khỏe)");
        lblFileStatus.setForeground(new Color(16, 185, 129));
        previewCluster();
    }

    private void fillSampleEnglishSpace() {
        txtTitle.setText("Mars Rover Orbital Trajectory and Atmospheric Spectroscopy");
        txtAuthor.setText("Dr. Carl Sagan & NASA Team");
        txtSummary.setText("Investigation of Mars planetary orbit telemetry, atmospheric density, and spectrometer imaging.");
        txtContent.setText("The exploration of Mars has entered an advanced phase with orbiters transmitting telemetry regarding atmospheric pressure, methane signatures, and planetary dust dynamics. Spacecraft sensors indicate consistent solar radiation interactions with robotic rover instrumentation in deep outer space exploration.");
        currentFileFormat = "TXT";
        currentLanguage = "EN";
        updateLanguageBadge();
        lblFileStatus.setText("Trạng thái file: Đã điền mẫu Tiếng Anh (Space & Astronomy)");
        lblFileStatus.setForeground(new Color(16, 185, 129));
        previewCluster();
    }

    private static class ClusterChoiceItem {
        final int clusterId;
        final String clusterName;

        ClusterChoiceItem(int clusterId, String clusterName) {
            this.clusterId = clusterId;
            this.clusterName = clusterName;
        }

        @Override
        public String toString() {
            return clusterName;
        }
    }
}
