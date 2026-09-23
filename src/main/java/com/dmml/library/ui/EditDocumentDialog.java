package com.dmml.library.ui;

import com.dmml.library.model.ClusterInfo;
import com.dmml.library.model.ClusteringResult;
import com.dmml.library.model.Document;
import com.dmml.library.service.ClusteringService;
import com.dmml.library.service.DocumentRepository;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Dialog allowing manual editing of document details and reassigning cluster
 * (Đáp ứng yêu cầu: "Trường hợp gán nhóm không đúng ý thì cho phép sửa").
 */
public class EditDocumentDialog extends JDialog {

    private final MainLibraryFrame mainFrame;
    private final Document document;
    private final DocumentRepository repository;
    private final ClusteringService clusteringService;

    private JTextField txtTitle;
    private JTextField txtAuthor;
    private JTextField txtSummary;
    private JTextArea txtContent;
    private JComboBox<ClusterChoice> comboClusters;
    private JLabel lblPredictedHint;

    public EditDocumentDialog(Window parent, Document doc, MainLibraryFrame mainFrame) {
        super(parent, "Chỉnh Sửa Thông Tin & Đổi Cụm Tài Liệu #" + doc.getId(), ModalityType.APPLICATION_MODAL);
        this.document = doc;
        this.mainFrame = mainFrame;
        this.repository = DocumentRepository.getInstance();
        this.clusteringService = ClusteringService.getInstance();

        setSize(780, 680);
        setMinimumSize(new Dimension(650, 550));
        setLocationRelativeTo(parent);

        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- TOP BANNER ---
        JPanel banner = new JPanel(new BorderLayout(8, 4));
        banner.setBorder(new CompoundBorder(
                new LineBorder(new Color(220, 230, 245), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        banner.setBackground(new Color(245, 249, 255));

        JLabel lblHeader = new JLabel("Chỉnh Sửa Tài Liệu & Can Thiệp Gán Cụm Phân Nhóm");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeader.setForeground(new Color(30, 64, 175));

        JLabel lblDesc = new JLabel("Bạn có thể sửa đổi nội dung và chủ động chuyển đổi cụm phân nhóm khi kết quả gán tự động chưa đúng ý.");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(new Color(100, 116, 139));

        banner.add(lblHeader, BorderLayout.NORTH);
        banner.add(lblDesc, BorderLayout.SOUTH);
        root.add(banner, BorderLayout.NORTH);

        // --- CENTER FORM ---
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // Title
        form.add(createFieldLabel("Tiêu đề tài liệu / sách:"));
        txtTitle = new JTextField(document.getTitle());
        txtTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtTitle.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(txtTitle);
        form.add(Box.createVerticalStrut(8));

        // Author
        form.add(createFieldLabel("Tác giả:"));
        txtAuthor = new JTextField(document.getAuthor());
        txtAuthor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAuthor.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtAuthor.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(txtAuthor);
        form.add(Box.createVerticalStrut(8));

        // Summary
        form.add(createFieldLabel("Tóm tắt (Abstract):"));
        txtSummary = new JTextField(document.getSummary() != null ? document.getSummary() : "");
        txtSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSummary.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(txtSummary);
        form.add(Box.createVerticalStrut(8));

        // Content
        form.add(createFieldLabel("Nội dung toàn văn:"));
        txtContent = new JTextArea(document.getContent(), 7, 30);
        txtContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtContent.setLineWrap(true);
        txtContent.setWrapStyleWord(true);
        JScrollPane scrollContent = new JScrollPane(txtContent);
        scrollContent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        form.add(scrollContent);
        form.add(Box.createVerticalStrut(12));

        // --- CLUSTER REASSIGNMENT CARD ---
        JPanel clusterCard = new JPanel(new BorderLayout(8, 8));
        clusterCard.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(16, 185, 129), 2, true), " Can Thiệp Gán Cụm Phân Nhóm (Cluster Override) "),
                new EmptyBorder(8, 12, 10, 12)
        ));
        clusterCard.setBackground(new Color(244, 253, 248));
        clusterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel clusterSelectRow = new JPanel(new BorderLayout(10, 0));
        clusterSelectRow.setOpaque(false);

        JLabel lblChooseCluster = new JLabel("Chọn Cụm Gán Mới: ");
        lblChooseCluster.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboClusters = new JComboBox<>();
        comboClusters.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        populateClusterChoices();

        JButton btnAiSuggest = new JButton("AI Gợi Ý Thử");
        btnAiSuggest.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAiSuggest.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnAiSuggest.addActionListener(e -> runAiPrediction());

        clusterSelectRow.add(lblChooseCluster, BorderLayout.WEST);
        clusterSelectRow.add(comboClusters, BorderLayout.CENTER);
        clusterSelectRow.add(btnAiSuggest, BorderLayout.EAST);

        lblPredictedHint = new JLabel("Cụm hiện tại: " + (document.getClusterId() >= 0 ? "Cụm #" + (document.getClusterId() + 1) : "Chưa phân cụm") +
                (document.isManuallyAssigned() ? " (Đã sửa thủ công)" : " (Do AI tự gán)"));
        lblPredictedHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblPredictedHint.setForeground(new Color(55, 65, 81));

        clusterCard.add(clusterSelectRow, BorderLayout.NORTH);
        clusterCard.add(lblPredictedHint, BorderLayout.SOUTH);

        form.add(clusterCard);
        root.add(form, BorderLayout.CENTER);

        // --- BOTTOM ACTIONS ---
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton btnCancel = new JButton("Hủy Bỏ");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Lưu Thay Đổi & Cập Nhật Cụm");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSave.addActionListener(e -> saveChanges());

        bottomBar.add(btnCancel);
        bottomBar.add(btnSave);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void populateClusterChoices() {
        comboClusters.removeAllItems();
        ClusteringResult res = clusteringService.getLatestResult();
        int selectedIndex = 0;

        if (res != null && res.getClusters() != null) {
            int idx = 0;
            for (ClusterInfo c : res.getClusters()) {
                ClusterChoice choice = new ClusterChoice(c.getClusterId(), c.getClusterName());
                comboClusters.addItem(choice);
                if (c.getClusterId() == document.getClusterId()) {
                    selectedIndex = idx;
                }
                idx++;
            }
        } else {
            for (int i = 0; i < 6; i++) {
                comboClusters.addItem(new ClusterChoice(i, "Cụm #" + (i + 1)));
            }
        }

        if (comboClusters.getItemCount() > 0) {
            comboClusters.setSelectedIndex(selectedIndex);
        }
    }

    private void runAiPrediction() {
        String title = txtTitle.getText().trim();
        String content = txtContent.getText().trim();
        Map<String, Object> pred = clusteringService.autoClassifyDocument(title, content);

        int predId = (int) pred.getOrDefault("predictedClusterId", 0);
        double conf = (double) pred.getOrDefault("confidence", 0.0);
        String name = (String) pred.getOrDefault("clusterName", "");

        // Select the predicted cluster in combo
        for (int i = 0; i < comboClusters.getItemCount(); i++) {
            if (comboClusters.getItemAt(i).clusterId == predId) {
                comboClusters.setSelectedIndex(i);
                break;
            }
        }

        lblPredictedHint.setText("AI gợi ý: " + name + " (Độ tương đồng Cosine: " + conf + "%)");
    }

    private void saveChanges() {
        String title = txtTitle.getText().trim();
        String author = txtAuthor.getText().trim();
        String summary = txtSummary.getText().trim();
        String content = txtContent.getText().trim();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tiêu đề tài liệu không được để trống!", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ClusterChoice selectedChoice = (ClusterChoice) comboClusters.getSelectedItem();
        int newClusterId = (selectedChoice != null) ? selectedChoice.clusterId : document.getClusterId();
        String newClusterName = (selectedChoice != null) ? selectedChoice.clusterName : "Cụm #" + (newClusterId + 1);

        document.setTitle(title);
        document.setAuthor(author);
        document.setSummary(summary);
        document.setContent(content);
        document.setTopic(newClusterName);

        // Perform manual cluster reassignment
        clusteringService.manualReassignCluster(document.getId(), newClusterId, newClusterName);
        clusteringService.registerDocumentVector(document);
        repository.update(document);

        mainFrame.refreshCatalog();

        JOptionPane.showMessageDialog(this,
                "Đã cập nhật thông tin và gán tài liệu vào " + newClusterName + " thành công!",
                "Thành Công", JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }

    public static class ClusterChoice {
        final int clusterId;
        final String clusterName;

        public ClusterChoice(int clusterId, String clusterName) {
            this.clusterId = clusterId;
            this.clusterName = clusterName;
        }

        @Override
        public String toString() {
            return clusterName;
        }
    }
}
