package com.dmml.library.ui;

import com.dmml.library.model.Document;
import com.dmml.library.service.ClusteringService;
import com.dmml.library.service.DocumentRepository;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modern Recommendation Panel:
 * - Cosine Similarity ranking for academic articles in the same cluster
 * - Clean layout without any emoji glyph issues on Windows
 * - Multi-language support (English & Vietnamese)
 * - Quick preview with split layout
 */
public class RecommendationPanel extends JPanel {

    private final MainLibraryFrame mainFrame;
    private final DocumentRepository repository;
    private final ClusteringService clusteringService;

    private JComboBox<DocumentItem> comboDocs;
    private JLabel lblCurrentDocInfo;
    private DefaultTableModel tableModel;
    private JTable recTable;
    private JTextArea txtPreview;

    private List<Document> currentRecs;

    public RecommendationPanel(MainLibraryFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.repository = DocumentRepository.getInstance();
        this.clusteringService = ClusteringService.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        initUI();
    }

    private void initUI() {
        // --- TOP SELECTOR PANEL ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 8));
        topPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        topPanel.setBackground(new Color(248, 250, 252));

        JPanel selectRow = new JPanel(new BorderLayout(10, 0));
        selectRow.setOpaque(false);

        JLabel lblPrompt = new JLabel("Chọn tài liệu nguồn cần gợi ý: ");
        lblPrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboDocs = new JComboBox<>();
        comboDocs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboDocs.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        JButton btnRunRec = new JButton("Tìm Tài Liệu Tương Tự (Cosine Similarity)");
        btnRunRec.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRunRec.setBackground(new Color(37, 99, 235));
        btnRunRec.setForeground(Color.WHITE);
        btnRunRec.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRunRec.addActionListener(e -> runRecommendation());

        selectRow.add(lblPrompt, BorderLayout.WEST);
        selectRow.add(comboDocs, BorderLayout.CENTER);
        selectRow.add(btnRunRec, BorderLayout.EAST);

        lblCurrentDocInfo = new JLabel("Chưa chọn tài liệu");
        lblCurrentDocInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblCurrentDocInfo.setForeground(new Color(100, 116, 139));

        topPanel.add(selectRow, BorderLayout.NORTH);
        topPanel.add(lblCurrentDocInfo, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Recommendations Table & Preview Split ---
        String[] columns = {"Thứ Hạng", "Mã ID", "Tiêu Đề Đề Xuất", "Tác Giả", "Ngôn Ngữ", "Độ Tương Đồng Cosine"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        recTable = new JTable(tableModel);
        recTable.setRowHeight(28);
        recTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        recTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recTable.setShowGrid(true);
        recTable.setGridColor(new Color(241, 245, 249));

        recTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        recTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        recTable.getColumnModel().getColumn(2).setPreferredWidth(440);
        recTable.getColumnModel().getColumn(3).setPreferredWidth(170);
        recTable.getColumnModel().getColumn(4).setPreferredWidth(95);
        recTable.getColumnModel().getColumn(5).setPreferredWidth(160);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        recTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        recTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);

        // Language renderer
        DefaultTableCellRenderer langRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(CENTER);
                String val = value != null ? value.toString() : "EN";
                if ("VI".equalsIgnoreCase(val)) {
                    lbl.setText("[VI]");
                    if (!isSelected) lbl.setForeground(new Color(220, 38, 38));
                } else {
                    lbl.setText("[EN]");
                    if (!isSelected) lbl.setForeground(new Color(37, 99, 235));
                }
                return lbl;
            }
        };
        recTable.getColumnModel().getColumn(4).setCellRenderer(langRender);

        // Score renderer
        DefaultTableCellRenderer scoreRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (!isSelected) {
                    setForeground(new Color(16, 185, 129));
                }
                return c;
            }
        };
        recTable.getColumnModel().getColumn(5).setCellRenderer(scoreRender);

        recTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreview();
            }
        });

        // Preview Box
        JPanel previewPanel = new JPanel(new BorderLayout(6, 6));
        previewPanel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(" Xem Nhanh Nội Dung Tài Liệu Đề Xuất "),
                new EmptyBorder(6, 8, 6, 8)
        ));
        previewPanel.setPreferredSize(new Dimension(0, 180));

        txtPreview = new JTextArea();
        txtPreview.setLineWrap(true);
        txtPreview.setWrapStyleWord(true);
        txtPreview.setEditable(false);
        txtPreview.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtPreview.setBackground(new Color(250, 250, 250));

        JPanel previewActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnViewFull = new JButton("Xem Toàn Văn Chi Tiết");
        btnViewFull.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewFull.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnViewFull.addActionListener(e -> viewSelectedRecommendation());
        previewActions.add(btnViewFull);

        previewPanel.add(new JScrollPane(txtPreview), BorderLayout.CENTER);
        previewPanel.add(previewActions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(recTable), previewPanel);
        split.setDividerLocation(340);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        populateDocList();
    }

    public void populateDocList() {
        comboDocs.removeAllItems();
        List<Document> docs = repository.findAll();
        for (Document d : docs) {
            String flag = "VI".equalsIgnoreCase(d.getLanguage()) ? "[VI] " : "[EN] ";
            comboDocs.addItem(new DocumentItem(d.getId(), flag + "#" + d.getId() + " - " + d.getTitle()));
        }
    }

    public void selectDocumentAndRun(int docId) {
        populateDocList();
        for (int i = 0; i < comboDocs.getItemCount(); i++) {
            if (comboDocs.getItemAt(i).id == docId) {
                comboDocs.setSelectedIndex(i);
                break;
            }
        }
        runRecommendation();
    }

    private void runRecommendation() {
        DocumentItem selected = (DocumentItem) comboDocs.getSelectedItem();
        if (selected == null) return;

        Document doc = repository.findById(selected.id).orElse(null);
        if (doc == null) return;

        String langName = "VI".equalsIgnoreCase(doc.getLanguage()) ? "Tiếng Việt" : "Tiếng Anh";
        lblCurrentDocInfo.setText("Đang đề xuất cho: [" + doc.getTitle() + "] - Tác giả: " + doc.getAuthor() +
                " - Thuộc cụm: " + (doc.getClusterId() >= 0 ? ("Cụm #" + (doc.getClusterId() + 1)) : "Chưa phân cụm") +
                " (" + langName + ")");

        currentRecs = clusteringService.recommendSimilarDocuments(doc.getId(), 10);

        tableModel.setRowCount(0);
        int rank = 1;
        for (Document rec : currentRecs) {
            tableModel.addRow(new Object[]{
                    "#" + rank++,
                    rec.getId(),
                    rec.getTitle(),
                    rec.getAuthor(),
                    rec.getLanguage(),
                    rec.getSimilarityScore() + "% tương đồng"
            });
        }

        if (!currentRecs.isEmpty()) {
            recTable.setRowSelectionInterval(0, 0);
        } else {
            txtPreview.setText("Không có tài liệu nào khác trong cùng cụm chủ đề.");
        }
    }

    private void updatePreview() {
        int row = recTable.getSelectedRow();
        if (row >= 0 && currentRecs != null && row < currentRecs.size()) {
            Document rec = currentRecs.get(row);
            txtPreview.setText("TIÊU ĐỀ: " + rec.getTitle() + "\n" +
                    "TÁC GIẢ: " + rec.getAuthor() + "\n" +
                    "NGÔN NGỮ: " + ("VI".equalsIgnoreCase(rec.getLanguage()) ? "Tiếng Việt" : "Tiếng Anh") + "\n" +
                    "ĐỘ TƯƠNG ĐỒNG COSINE: " + rec.getSimilarityScore() + "%\n\n" +
                    "TÓM TẮT:\n" + (rec.getSummary() != null ? rec.getSummary() : "") + "\n\n" +
                    "TRÍCH ĐOẠN NỘI DUNG:\n" + rec.getContent());
            txtPreview.setCaretPosition(0);
        }
    }

    private void viewSelectedRecommendation() {
        int row = recTable.getSelectedRow();
        if (row != -1 && currentRecs != null && row < currentRecs.size()) {
            new DocumentDetailDialog(mainFrame, currentRecs.get(row)).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài liệu từ danh sách đề xuất!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static class DocumentItem {
        final int id;
        final String title;

        DocumentItem(int id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
