package com.dmml.library.ui;

import com.dmml.library.model.Document;
import com.dmml.library.util.PhysicalDocumentManager;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Modern Document Detail Dialog:
 * - Full text viewer with clean typography and content statistics (words, characters, lines)
 * - Badges for Language, File Format, Cluster assignment, and Manual Override status
 * - "Đổi Cụm / Chỉnh Sửa" action button directly in the dialog
 * - "Mở Bằng Trình Đọc Ngoài" to launch external viewer (PDF reader, Word, Notepad)
 * - "Xuất File Ra Máy" to save full text to disk
 * - Safe standard Unicode text rendering without missing glyph boxes (□)
 */
public class DocumentDetailDialog extends JDialog {

    private final MainLibraryFrame mainFrame;
    private final Document doc;
    private JLabel lblTopic;

    public DocumentDetailDialog(MainLibraryFrame parent, Document doc) {
        super(parent, "Chi Tiết Tài Liệu #" + doc.getId() + " - " + doc.getTitle(), true);
        this.mainFrame = parent;
        this.doc = doc;

        setSize(860, 680);
        setMinimumSize(new Dimension(720, 520));
        setLocationRelativeTo(parent);

        initUI();
    }

    private void initUI() {
        JPanel contentPane = new JPanel(new BorderLayout(12, 12));
        contentPane.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- TOP: Metadata Card ---
        JPanel metaCard = new JPanel(new BorderLayout(8, 8));
        metaCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        metaCard.setBackground(new Color(248, 250, 252));

        // Title Row
        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("<html><b style='font-size:14px; color:#1e3a8a;'>" + doc.getTitle() + "</b></html>");
        titleRow.add(lblTitle, BorderLayout.CENTER);

        // Language & Format Badges
        JPanel badgeGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badgeGroup.setOpaque(false);

        String lang = doc.getLanguage() != null ? doc.getLanguage().toUpperCase() : "EN";
        JLabel lblLang = new JLabel("VI".equals(lang) ? "[VI] Tiếng Việt" : "[EN] Tiếng Anh");
        lblLang.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblLang.setForeground("VI".equals(lang) ? new Color(220, 38, 38) : new Color(37, 99, 235));

        JLabel lblFmt = new JLabel("[" + doc.getFileFormat() + "]");
        lblFmt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblFmt.setForeground(new Color(71, 85, 105));

        badgeGroup.add(lblLang);
        badgeGroup.add(lblFmt);
        titleRow.add(badgeGroup, BorderLayout.EAST);
        metaCard.add(titleRow, BorderLayout.NORTH);

        // Details Grid
        JPanel detailGrid = new JPanel(new GridLayout(2, 2, 12, 4));
        detailGrid.setOpaque(false);

        JLabel lblAuthor = new JLabel("Tác giả: " + (doc.getAuthor() != null ? doc.getAuthor() : "Không rõ"));
        lblAuthor.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        String clusterName = com.dmml.library.service.ClusteringService.getInstance().getClusterDisplayName(doc.getClusterId());
        if (doc.isManuallyAssigned()) {
            clusterName += " [Đã sửa thủ công]";
        }
        lblTopic = new JLabel("Chủ đề: " + clusterName);
        lblTopic.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTopic.setForeground(new Color(16, 185, 129));

        JLabel lblId = new JLabel("Mã ID hệ thống: #" + doc.getId());
        lblId.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        File physicalFile = PhysicalDocumentManager.ensurePhysicalFile(doc);

        String sourceNote = physicalFile != null ? "File vật lý: " + physicalFile.getName() : "Thể loại gốc: " + (doc.getOriginalCategory() != null ? doc.getOriginalCategory() : "General");
        JLabel lblCat = new JLabel(sourceNote);
        lblCat.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (physicalFile != null) {
            lblCat.setToolTipText(physicalFile.getAbsolutePath());
        }

        detailGrid.add(lblAuthor);
        detailGrid.add(lblTopic);
        detailGrid.add(lblId);
        detailGrid.add(lblCat);

        metaCard.add(detailGrid, BorderLayout.CENTER);
        contentPane.add(metaCard, BorderLayout.NORTH);

        // --- CENTER: Summary & Full Content ---
        JPanel bodyPanel = new JPanel(new BorderLayout(8, 8));

        if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
            JPanel summaryPanel = new JPanel(new BorderLayout());
            summaryPanel.setBorder(new CompoundBorder(
                    new TitledBorder(new LineBorder(new Color(226, 232, 240), 1, true), " Tóm Tắt (Abstract) "),
                    new EmptyBorder(6, 10, 6, 10)
            ));
            summaryPanel.setBackground(new Color(250, 250, 250));

            JTextArea txtSummary = new JTextArea(doc.getSummary());
            txtSummary.setWrapStyleWord(true);
            txtSummary.setLineWrap(true);
            txtSummary.setEditable(false);
            txtSummary.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            txtSummary.setBackground(new Color(250, 250, 250));
            txtSummary.setRows(3);
            summaryPanel.add(new JScrollPane(txtSummary), BorderLayout.CENTER);
            bodyPanel.add(summaryPanel, BorderLayout.NORTH);
        }

        // Calculate Text Statistics
        String content = doc.getContent() != null ? doc.getContent() : "";
        String trimmed = content.trim();
        int wordCount = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
        int charCount = content.length();
        long lineCount = content.lines().count();
        int estReadTime = Math.max(1, (int) Math.ceil(wordCount / 180.0));

        JPanel fullContentPanel = new JPanel(new BorderLayout());
        fullContentPanel.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(226, 232, 240), 1, true), " Toàn Văn Tài Liệu "),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JTextArea txtContent = new JTextArea(content);
        txtContent.setWrapStyleWord(true);
        txtContent.setLineWrap(true);
        txtContent.setEditable(false);
        txtContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fullContentPanel.add(new JScrollPane(txtContent), BorderLayout.CENTER);

        // Stats Footer Bar
        JPanel statsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        statsBar.setOpaque(false);
        String fileExtName = physicalFile != null ? physicalFile.getName() : doc.getFileFormat();
        JLabel lblStats = new JLabel("Thống kê: " + wordCount + " từ  •  " + charCount + " ký tự  •  " + lineCount + " dòng  •  Ước lượng đọc: ~" + estReadTime + " phút  •  Tệp: " + fileExtName);
        lblStats.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStats.setForeground(new Color(100, 116, 139));
        statsBar.add(lblStats);
        fullContentPanel.add(statsBar, BorderLayout.SOUTH);

        bodyPanel.add(fullContentPanel, BorderLayout.CENTER);
        contentPane.add(bodyPanel, BorderLayout.CENTER);

        // --- BOTTOM: ACTIONS ---
        JPanel btnPanel = new JPanel(new BorderLayout());

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton btnOpenExternal = new JButton("Mở Bằng Ứng Dụng Ngoài (" + doc.getFileFormat() + ")");
        btnOpenExternal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenExternal.setBackground(new Color(37, 99, 235));
        btnOpenExternal.setForeground(Color.WHITE);
        btnOpenExternal.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnOpenExternal.setToolTipText("Mở file thật (.pdf/.docx/.txt) bằng ứng dụng tương ứng trên máy tính (Acrobat/Edge, MS Word, Notepad)");
        btnOpenExternal.addActionListener(e -> openInExternalViewer());

        JButton btnRevealFolder = new JButton("Vị Trí File (Explorer)");
        btnRevealFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRevealFolder.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRevealFolder.setToolTipText("Mở thư mục data/documents trong Windows File Explorer và chọn file này");
        btnRevealFolder.addActionListener(e -> revealInExplorer());

        JButton btnEditCluster = new JButton("Đổi Chủ Đề / Sửa");
        btnEditCluster.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnEditCluster.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnEditCluster.addActionListener(e -> {
            EditDocumentDialog dialog = new EditDocumentDialog(this, doc, mainFrame);
            dialog.setVisible(true);
            // Refresh labels
            String newClusterName = com.dmml.library.service.ClusteringService.getInstance().getClusterDisplayName(doc.getClusterId());
            if (doc.isManuallyAssigned()) newClusterName += " [Đã sửa thủ công]";
            lblTopic.setText("Chủ đề: " + newClusterName);
        });

        JButton btnExport = new JButton("Xuất Bản Sao...");
        btnExport.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnExport.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnExport.setToolTipText("Lưu thêm một bản sao tài liệu ra thư mục khác trên máy");
        btnExport.addActionListener(e -> exportToFile());

        leftActions.add(btnOpenExternal);
        leftActions.add(btnRevealFolder);
        leftActions.add(btnEditCluster);
        leftActions.add(btnExport);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnClose.addActionListener(e -> dispose());
        rightActions.add(btnClose);

        btnPanel.add(leftActions, BorderLayout.WEST);
        btnPanel.add(rightActions, BorderLayout.EAST);
        contentPane.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(contentPane);
    }

    private void openInExternalViewer() {
        File physical = PhysicalDocumentManager.ensurePhysicalFile(doc);
        if (physical != null && physical.exists()) {
            boolean ok = PhysicalDocumentManager.openWithDefaultApp(physical);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Không thể mở file bằng ứng dụng ngoài: " + physical.getAbsolutePath(), "Thông Báo", JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Không tìm thấy hoặc không thể tạo file vật lý!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void revealInExplorer() {
        File physical = PhysicalDocumentManager.ensurePhysicalFile(doc);
        PhysicalDocumentManager.revealInExplorer(physical);
    }

    private void exportToFile() {
        JFileChooser chooser = new JFileChooser();
        String safeTitle = doc.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (safeTitle.length() > 30) safeTitle = safeTitle.substring(0, 30);
        chooser.setSelectedFile(new File("TaiLieu_" + doc.getId() + "_" + safeTitle + ".txt"));
        chooser.setDialogTitle("Chọn vị trí lưu file tài liệu");
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("TIÊU ĐỀ: ").append(doc.getTitle()).append("\n");
                sb.append("TÁC GIẢ: ").append(doc.getAuthor() != null ? doc.getAuthor() : "").append("\n");
                sb.append("CỤM CHỦ ĐỀ: ").append(doc.getTopic()).append("\n");
                sb.append("====================================================\n\n");
                if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
                    sb.append("TÓM TẮT:\n").append(doc.getSummary()).append("\n\n");
                }
                sb.append("NỘI DUNG TOÀN VĂN:\n").append(doc.getContent()).append("\n");

                Files.writeString(target.toPath(), sb.toString(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Đã xuất file thành công tới:\n" + target.getAbsolutePath(), "Xuất File Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu file: " + ex.getMessage(), "Lỗi Lưu File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
