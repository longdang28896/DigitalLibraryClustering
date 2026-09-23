package com.dmml.library.ui;

import com.dmml.library.model.ClusterInfo;
import com.dmml.library.model.ClusteringResult;
import com.dmml.library.model.Document;
import com.dmml.library.service.ClusteringService;
import com.dmml.library.service.DocumentRepository;
import com.dmml.library.util.PhysicalDocumentManager;
import com.formdev.flatlaf.FlatClientProperties;

import java.io.File;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Modern Document Catalog Panel:
 * - Clean layout with ZERO overlap (dedicated rows for buttons and status text)
 * - Safe standard Unicode text rendering without missing glyph boxes (□)
 * - Search by keyword + Filter by Language (VI / EN) + Filter by Cluster
 * - Action buttons: "Xem Chi Tiết", "Đổi Cụm / Sửa", "Gợi Ý Tương Tự", "Xóa", "Làm Mới"
 * - Context menu on right-click
 */
public class DocumentCatalogPanel extends JPanel {

    private final DocumentRepository repository;
    private final ClusteringService clusteringService;
    private final MainLibraryFrame mainFrame;

    private DefaultListModel<ClusterItem> clusterListModel;
    private JList<ClusterItem> clusterList;
    private DefaultTableModel tableModel;
    private JTable docTable;
    private JTextField txtSearch;
    private JComboBox<String> comboLangFilter;
    private JLabel lblStatus;

    private Integer currentSelectedCluster = null;

    public DocumentCatalogPanel(MainLibraryFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.repository = DocumentRepository.getInstance();
        this.clusteringService = ClusteringService.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(12, 14, 12, 14));

        initUI();
        refreshData();
    }

    private void initUI() {
        // --- LEFT PANEL: Clusters Explorer ---
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setPreferredSize(new Dimension(330, 0));
        leftPanel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(" Cụm Chủ Đề Học Thuật "),
                new EmptyBorder(6, 6, 6, 6)
        ));

        clusterListModel = new DefaultListModel<>();
        clusterList = new JList<>(clusterListModel);
        clusterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clusterList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clusterList.setCellRenderer(new ClusterListCellRenderer());
        clusterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ClusterItem selected = clusterList.getSelectedValue();
                currentSelectedCluster = (selected != null) ? selected.clusterId : null;
                filterTable();
            }
        });

        // Double-click to rename cluster or right-click to select
        clusterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    promptRenameCurrentCluster();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    int idx = clusterList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        clusterList.setSelectedIndex(idx);
                    }
                }
            }
        });

        // Context popup menu for cluster list
        JPopupMenu clusterPopup = new JPopupMenu();
        JMenuItem itemRename = new JMenuItem("✏️ Đổi Tên Cụm Này...");
        itemRename.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemRename.addActionListener(e -> promptRenameCurrentCluster());
        JMenuItem itemReset = new JMenuItem("🔄 Khôi Phục Tên Mặc Định");
        itemReset.addActionListener(e -> resetCurrentClusterName());
        clusterPopup.add(itemRename);
        clusterPopup.add(itemReset);
        clusterList.setComponentPopupMenu(clusterPopup);

        leftPanel.add(new JScrollPane(clusterList), BorderLayout.CENTER);

        // Bottom cluster action bar
        JPanel clusterActionPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        JButton btnRenameCls = new JButton("✏️ Đổi Tên Cụm");
        btnRenameCls.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRenameCls.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRenameCls.setToolTipText("Nhấp chuột phải hoặc bấm nút này để đổi tên cụm chuyên đề tùy ý");
        btnRenameCls.addActionListener(e -> promptRenameCurrentCluster());

        JButton btnResetCls = new JButton("🔄 Khôi Phục");
        btnResetCls.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnResetCls.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnResetCls.setToolTipText("Khôi phục tên chuyên ngành học thuật mặc định");
        btnResetCls.addActionListener(e -> resetCurrentClusterName());

        clusterActionPanel.add(btnRenameCls);
        clusterActionPanel.add(btnResetCls);
        leftPanel.add(clusterActionPanel, BorderLayout.SOUTH);

        // --- CENTER PANEL: Search, Table, and Actions ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // 1. Search & Filter Bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        searchBar.setBackground(new Color(248, 250, 252));

        JPanel searchInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchInputs.setOpaque(false);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));

        txtSearch = new JTextField(22);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tiêu đề, tác giả, tóm tắt...");
        txtSearch.addActionListener(e -> filterTable());

        JLabel lblLang = new JLabel("Ngôn ngữ:");
        lblLang.setFont(new Font("Segoe UI", Font.BOLD, 12));

        comboLangFilter = new JComboBox<>(new String[]{"Tất Cả", "Tiếng Việt (VI)", "Tiếng Anh (EN)"});
        comboLangFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboLangFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        comboLangFilter.addActionListener(e -> filterTable());

        searchInputs.add(lblSearch);
        searchInputs.add(txtSearch);
        searchInputs.add(lblLang);
        searchInputs.add(comboLangFilter);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchButtons.setOpaque(false);

        JButton btnSearch = new JButton("Tìm Kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(37, 99, 235));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnSearch.addActionListener(e -> filterTable());

        JButton btnReset = new JButton("Đặt Lại");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            comboLangFilter.setSelectedIndex(0);
            clusterList.setSelectedIndex(0);
            currentSelectedCluster = null;
            filterTable();
        });

        searchButtons.add(btnSearch);
        searchButtons.add(btnReset);

        searchBar.add(searchInputs, BorderLayout.CENTER);
        searchBar.add(searchButtons, BorderLayout.EAST);
        centerPanel.add(searchBar, BorderLayout.NORTH);

        // 2. Table
        String[] columns = {"ID", "Tiêu Đề Tài Liệu", "Tác Giả", "Cụm Phân Nhóm", "Ngôn Ngữ", "Định Dạng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        docTable = new JTable(tableModel);
        docTable.setRowHeight(28);
        docTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        docTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        docTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        docTable.setShowGrid(true);
        docTable.setGridColor(new Color(241, 245, 249));

        docTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        docTable.getColumnModel().getColumn(1).setPreferredWidth(440);
        docTable.getColumnModel().getColumn(2).setPreferredWidth(170);
        docTable.getColumnModel().getColumn(3).setPreferredWidth(190);
        docTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        docTable.getColumnModel().getColumn(5).setPreferredWidth(85);

        // Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        docTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        docTable.getColumnModel().getColumn(4).setCellRenderer(new LanguageCellRenderer());
        docTable.getColumnModel().getColumn(5).setCellRenderer(new FormatCellRenderer());
        docTable.getColumnModel().getColumn(3).setCellRenderer(new ClusterCellRenderer());

        // Right-Click Context Menu
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem menuView = new JMenuItem("Xem Toàn Văn Chi Tiết");
        menuView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        menuView.addActionListener(e -> viewSelectedDocument());

        JMenuItem menuOpenExt = new JMenuItem("Mở File Ngoài (PDF/Word/Text)");
        menuOpenExt.addActionListener(e -> openSelectedFileExternally());

        JMenuItem menuReveal = new JMenuItem("Vị Trí File (Windows Explorer)");
        menuReveal.addActionListener(e -> revealSelectedFileInExplorer());

        JMenuItem menuEditCluster = new JMenuItem("Đổi Cụm Phân Nhóm / Chỉnh Sửa");
        menuEditCluster.addActionListener(e -> editSelectedDocument());

        JMenuItem menuRec = new JMenuItem("Gợi Ý Tài Liệu Tương Tự");
        menuRec.addActionListener(e -> recommendForSelected());

        JMenuItem menuDel = new JMenuItem("Xóa Tài Liệu");
        menuDel.addActionListener(e -> deleteSelectedDocument());

        popupMenu.add(menuView);
        popupMenu.add(menuOpenExt);
        popupMenu.add(menuReveal);
        popupMenu.addSeparator();
        popupMenu.add(menuEditCluster);
        popupMenu.add(menuRec);
        popupMenu.addSeparator();
        popupMenu.add(menuDel);

        docTable.setComponentPopupMenu(popupMenu);

        docTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedDocument();
                }
            }
        });

        centerPanel.add(new JScrollPane(docTable), BorderLayout.CENTER);

        // 3. Bottom Toolbar (2 SEPARATE ROWS TO PREVENT OVERLAPPING COMPLETELY)
        JPanel bottomToolbar = new JPanel();
        bottomToolbar.setLayout(new BoxLayout(bottomToolbar, BoxLayout.Y_AXIS));

        // Row 1: Action Buttons (Right aligned)
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));

        JButton btnView = new JButton("Xem Chi Tiết");
        btnView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnView.setBackground(new Color(37, 99, 235));
        btnView.setForeground(Color.WHITE);
        btnView.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnView.addActionListener(e -> viewSelectedDocument());

        JButton btnOpenExt = new JButton("Mở File Ngoài");
        btnOpenExt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenExt.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnOpenExt.setToolTipText("Mở file thật (.pdf/.docx/.txt) bằng ứng dụng tương ứng trên máy tính");
        btnOpenExt.addActionListener(e -> openSelectedFileExternally());

        JButton btnRepoFolder = new JButton("Kho File (Explorer)");
        btnRepoFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRepoFolder.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRepoFolder.setToolTipText("Mở thư mục kho lưu trữ file vật lý data/documents trong Windows Explorer");
        btnRepoFolder.addActionListener(e -> openRepositoryFolder());

        JButton btnEditCluster = new JButton("Đổi Cụm / Sửa");
        btnEditCluster.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditCluster.setBackground(new Color(16, 185, 129));
        btnEditCluster.setForeground(Color.WHITE);
        btnEditCluster.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnEditCluster.addActionListener(e -> editSelectedDocument());

        JButton btnRecommend = new JButton("Gợi Ý Tương Tự");
        btnRecommend.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRecommend.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRecommend.addActionListener(e -> recommendForSelected());

        JButton btnDelete = new JButton("Xóa");
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnDelete.addActionListener(e -> deleteSelectedDocument());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRefresh.addActionListener(e -> refreshData());

        actionRow.add(btnView);
        actionRow.add(btnOpenExt);
        actionRow.add(btnRepoFolder);
        actionRow.add(btnEditCluster);
        actionRow.add(btnRecommend);
        actionRow.add(btnDelete);
        actionRow.add(btnRefresh);

        // Row 2: Status Bar (Full width, placed on its own line below buttons - CAN NEVER OVERLAP)
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(5, 6, 2, 6)
        ));
        lblStatus = new JLabel("Hiển thị 0 tài liệu");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(new Color(100, 116, 139));
        statusRow.add(lblStatus, BorderLayout.WEST);

        bottomToolbar.add(actionRow);
        bottomToolbar.add(statusRow);
        centerPanel.add(bottomToolbar, BorderLayout.SOUTH);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        splitPane.setDividerLocation(330);
        splitPane.setContinuousLayout(true);
        add(splitPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        clusterListModel.clear();
        clusterListModel.addElement(new ClusterItem(null, "📚 Tất Cả Tài Liệu", repository.count(), "Toàn bộ kho dữ liệu thư viện số"));

        ClusteringResult result = clusteringService.getLatestResult();
        if (result != null && result.getClusters() != null) {
            for (ClusterInfo c : result.getClusters()) {
                String kws = c.getDisplayKeywords();
                if (kws == null || kws.isEmpty()) {
                    kws = (c.getTopKeywords() != null && !c.getTopKeywords().isEmpty())
                            ? String.join(", ", c.getTopKeywords().subList(0, Math.min(4, c.getTopKeywords().size())))
                            : "";
                }
                clusterListModel.addElement(new ClusterItem(c.getClusterId(), c.getClusterName(), c.getDocumentCount(), kws));
            }
        }

        if (clusterList.getSelectedIndex() == -1) {
            clusterList.setSelectedIndex(0);
        }

        filterTable();
    }

    private void filterTable() {
        String query = txtSearch.getText().trim();
        String langChoice = (String) comboLangFilter.getSelectedItem();
        String langCode = "ALL";
        if (langChoice != null) {
            if (langChoice.contains("Việt")) langCode = "VI";
            else if (langChoice.contains("Anh")) langCode = "EN";
        }

        List<Document> docs = repository.search(query, currentSelectedCluster, langCode);

        tableModel.setRowCount(0);
        int viCount = 0;
        int enCount = 0;

        for (Document doc : docs) {
            String clusterStr = clusteringService.getClusterDisplayName(doc.getClusterId());
            if (doc.isManuallyAssigned()) {
                clusterStr += " [Sửa]";
            }

            String lang = doc.getLanguage() != null ? doc.getLanguage().toUpperCase() : "EN";
            if ("VI".equals(lang)) viCount++; else enCount++;

            tableModel.addRow(new Object[]{
                    doc.getId(),
                    doc.getTitle(),
                    doc.getAuthor(),
                    clusterStr,
                    lang,
                    doc.getFileFormat()
            });
        }

        if (docs.isEmpty()) {
            lblStatus.setText("Không có tài liệu nào phù hợp với bộ lọc hiện tại! Vui lòng chọn 'Tất Cả' hoặc bấm nút 'Đặt Lại'.");
        } else {
            String clusterNote = (currentSelectedCluster != null) ? " (Đang xem Cụm #" + (currentSelectedCluster + 1) + ")" : " (Tất cả cụm)";
            lblStatus.setText("Hiển thị: " + docs.size() + " / " + repository.count() + " tài liệu  •  Tiếng Việt: " + viCount + "  •  Tiếng Anh: " + enCount + "  •  " + clusterNote);
        }
    }

    private Document getSelectedDocument() {
        int selectedRow = docTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài liệu từ bảng!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        int docId = (int) tableModel.getValueAt(selectedRow, 0);
        return repository.findById(docId).orElse(null);
    }

    private void editSelectedDocument() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            new EditDocumentDialog(mainFrame, doc, mainFrame).setVisible(true);
        }
    }

    private void viewSelectedDocument() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            new DocumentDetailDialog(mainFrame, doc).setVisible(true);
        }
    }

    private void openSelectedFileExternally() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            File f = PhysicalDocumentManager.ensurePhysicalFile(doc);
            if (f != null && f.exists()) {
                boolean ok = PhysicalDocumentManager.openWithDefaultApp(f);
                if (!ok) {
                    JOptionPane.showMessageDialog(this, "Không thể mở file bằng ứng dụng ngoài:\n" + f.getAbsolutePath(), "Thông Báo", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Không tìm thấy hoặc không thể tạo file vật lý!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void revealSelectedFileInExplorer() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            File f = PhysicalDocumentManager.ensurePhysicalFile(doc);
            PhysicalDocumentManager.revealInExplorer(f);
        } else {
            PhysicalDocumentManager.revealInExplorer(null);
        }
    }

    private void openRepositoryFolder() {
        PhysicalDocumentManager.revealInExplorer(null);
    }

    private void recommendForSelected() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            mainFrame.showRecommendationForDoc(doc.getId());
        }
    }

    private void deleteSelectedDocument() {
        Document doc = getSelectedDocument();
        if (doc != null) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn xóa tài liệu:\n\"" + doc.getTitle() + "\"?",
                    "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                repository.deleteById(doc.getId());
                refreshData();
                JOptionPane.showMessageDialog(this, "Đã xóa tài liệu khỏi thư viện số!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void promptRenameCurrentCluster() {
        ClusterItem selected = clusterList.getSelectedValue();
        if (selected == null || selected.clusterId == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một cụm chuyên đề để đổi tên (không áp dụng cho 'Tất Cả Tài Liệu')!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 6));
        panel.setPreferredSize(new Dimension(380, 110));
        JLabel lblTitle = new JLabel("Nhập tên mới cho Cụm #" + (selected.clusterId + 1) + ":");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtNewName = new JTextField(selected.name, 28);
        txtNewName.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblKws = new JLabel("Từ khóa mô tả đặc trưng (tùy chọn):");
        lblKws.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JTextField txtNewKws = new JTextField(selected.keywords, 28);
        txtNewKws.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        panel.add(lblTitle);
        panel.add(txtNewName);
        panel.add(lblKws);
        panel.add(txtNewKws);

        int result = JOptionPane.showConfirmDialog(this, panel, "✏️ Đổi Tên Cụm Chuyên Đề #" + (selected.clusterId + 1),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = txtNewName.getText().trim();
            String newKws = txtNewKws.getText().trim();
            if (!newName.isEmpty()) {
                clusteringService.renameCluster(selected.clusterId, newName, newKws);
                refreshData();
                JOptionPane.showMessageDialog(this, "Đã cập nhật tên cụm thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void resetCurrentClusterName() {
        ClusterItem selected = clusterList.getSelectedValue();
        if (selected == null || selected.clusterId == null) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn khôi phục tên mặc định chuẩn học thuật cho Cụm #" + (selected.clusterId + 1) + "?",
                "Xác Nhận Khôi Phục", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            clusteringService.resetClusterName(selected.clusterId);
            refreshData();
        }
    }

    // --- INNER CLASSES FOR RENDERERS ---
    public static class ClusterItem {
        final Integer clusterId;
        final String name;
        final int count;
        final String keywords;

        ClusterItem(Integer clusterId, String name, int count, String keywords) {
            this.clusterId = clusterId;
            this.name = name;
            this.count = count;
            this.keywords = keywords;
        }
    }

    private static class ClusterListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ClusterItem item) {
                String countBadge = "<span style='font-weight:bold; color:" + (isSelected ? "#ffffff" : "#2563eb") + ";'> (" + item.count + ")</span>";
                String subColor = isSelected ? "#e2e8f0" : "#64748b";
                String sub = !item.keywords.isEmpty()
                        ? "<div style='font-size:10.5px; color:" + subColor + "; margin-top:2px; line-height:1.25;'><i>Từ khóa: " + item.keywords + "</i></div>"
                        : "";
                String title = "<div style='font-size:12px; font-weight:600;'>" + item.name + countBadge + "</div>";
                lbl.setText("<html><div style='padding:5px 7px;'>" + title + sub + "</div></html>");
            }
            return lbl;
        }
    }

    private static class LanguageCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(CENTER);
            String val = value != null ? value.toString() : "EN";
            if ("VI".equalsIgnoreCase(val)) {
                lbl.setText("[VI] Tiếng Việt");
                if (!isSelected) lbl.setForeground(new Color(220, 38, 38));
            } else {
                lbl.setText("[EN] Tiếng Anh");
                if (!isSelected) lbl.setForeground(new Color(37, 99, 235));
            }
            return lbl;
        }
    }

    private static class FormatCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(CENTER);
            String val = value != null ? value.toString().toUpperCase() : "TXT";
            lbl.setText(val);
            if (!isSelected) {
                if ("PDF".equals(val)) lbl.setForeground(new Color(220, 38, 38));
                else if ("DOCX".equals(val)) lbl.setForeground(new Color(37, 99, 235));
                else lbl.setForeground(new Color(100, 116, 139));
            }
            return lbl;
        }
    }

    private static class ClusterCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String val = value != null ? value.toString() : "";
            lbl.setText(val);
            if (val.contains("[Sửa thủ công]") && !isSelected) {
                lbl.setForeground(new Color(16, 185, 129));
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            } else if (!isSelected) {
                lbl.setForeground(new Color(15, 23, 42));
                lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
            }
            return lbl;
        }
    }
}
