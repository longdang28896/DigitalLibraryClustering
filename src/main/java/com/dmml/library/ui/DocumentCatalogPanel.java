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
import javax.swing.border.TitledBorder;
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

    private static final Color TEXT_PRIMARY = new Color(51, 65, 85);
    private static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color SURFACE_MUTED = new Color(248, 250, 252);

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

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 12, 10, 12));

        initUI();
        refreshData();
    }

    private void initUI() {
        // --- LEFT PANEL: compact topic explorer ---
        JPanel leftPanel = new JPanel(new BorderLayout(6, 6));
        leftPanel.setPreferredSize(new Dimension(335, 0));
        leftPanel.setMinimumSize(new Dimension(335, 0));
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        " Danh Mục Chủ Đề ",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 12),
                        new Color(59, 79, 118)
                ),
                new EmptyBorder(4, 4, 4, 4)
        ));

        clusterListModel = new DefaultListModel<>();
        clusterList = new JList<>(clusterListModel);
        clusterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clusterList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clusterList.setFixedCellHeight(64);
        clusterList.setBackground(Color.WHITE);
        clusterList.setCellRenderer(new ClusterListCellRenderer());
        clusterList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ClusterItem selected = clusterList.getSelectedValue();
                currentSelectedCluster = (selected != null) ? selected.clusterId : null;
                filterTable();
            }
        });

        // Double-click to rename topic (nhấp đúp để đổi tên chủ đề theo yêu cầu)
        clusterList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    promptRenameCurrentCluster();
                }
            }
        });

        JScrollPane clusterScrollPane = new JScrollPane(clusterList);
        clusterScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        clusterScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        clusterScrollPane.setBorder(BorderFactory.createEmptyBorder());
        clusterScrollPane.getViewport().setBackground(Color.WHITE);
        leftPanel.add(clusterScrollPane, BorderLayout.CENTER);

        // --- CENTER PANEL: Search, Table, and Actions ---
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setMinimumSize(new Dimension(620, 0));

        // 1. Search & Filter Bar
        JPanel searchBar = new JPanel(new GridBagLayout());
        searchBar.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(7, 10, 7, 10)
        ));
        searchBar.setBackground(SURFACE_MUTED);

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(TEXT_PRIMARY);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setMinimumSize(new Dimension(130, 28));
        txtSearch.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 4,8,4,8");
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nhập tiêu đề, tác giả, tóm tắt...");
        txtSearch.addActionListener(e -> filterTable());

        JLabel lblLang = new JLabel("Ngôn ngữ:");
        lblLang.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLang.setForeground(TEXT_PRIMARY);

        comboLangFilter = new JComboBox<>(new String[]{"Tất Cả", "Tiếng Việt (VI)", "Tiếng Anh (EN)"});
        comboLangFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboLangFilter.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        comboLangFilter.addActionListener(e -> filterTable());

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

        GridBagConstraints searchGbc = new GridBagConstraints();
        searchGbc.gridy = 0;
        searchGbc.anchor = GridBagConstraints.CENTER;
        searchGbc.insets = new Insets(0, 0, 0, 8);
        searchBar.add(lblSearch, searchGbc);

        searchGbc.gridx = 1;
        searchGbc.weightx = 1.0;
        searchGbc.fill = GridBagConstraints.HORIZONTAL;
        searchBar.add(txtSearch, searchGbc);

        searchGbc.gridx = 2;
        searchGbc.weightx = 0;
        searchGbc.fill = GridBagConstraints.NONE;
        searchBar.add(lblLang, searchGbc);

        searchGbc.gridx = 3;
        searchBar.add(comboLangFilter, searchGbc);

        searchGbc.gridx = 4;
        searchGbc.insets = new Insets(0, 2, 0, 6);
        searchBar.add(btnSearch, searchGbc);

        searchGbc.gridx = 5;
        searchGbc.insets = new Insets(0, 0, 0, 0);
        searchBar.add(btnReset, searchGbc);
        centerPanel.add(searchBar, BorderLayout.NORTH);

        // 2. Table
        String[] columns = {"ID", "Tiêu Đề Tài Liệu", "Tác Giả", "Chủ Đề", "Ngôn Ngữ", "Định Dạng"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        docTable = new JTable(tableModel);
        docTable.setRowHeight(26);
        docTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        docTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        docTable.setForeground(TEXT_PRIMARY);
        docTable.setSelectionBackground(new Color(219, 234, 254));
        docTable.setSelectionForeground(new Color(30, 64, 175));
        docTable.getTableHeader().setBackground(new Color(241, 245, 249));
        docTable.getTableHeader().setForeground(new Color(59, 79, 118));
        docTable.getTableHeader().setReorderingAllowed(false);
        docTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        docTable.setShowVerticalLines(true);
        docTable.setShowHorizontalLines(true);
        docTable.setGridColor(new Color(232, 238, 245));
        docTable.setFillsViewportHeight(true);
        docTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        configureColumn(0, 48, 42);
        configureColumn(1, 330, 210);
        configureColumn(2, 150, 105);
        configureColumn(3, 170, 115);
        configureColumn(4, 100, 92);
        configureColumn(5, 82, 70);

        // Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        docTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        docTable.getColumnModel().getColumn(1).setCellRenderer(new DocumentCellRenderer());
        docTable.getColumnModel().getColumn(2).setCellRenderer(new DocumentCellRenderer());
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

        JMenuItem menuEditCluster = new JMenuItem("Đổi Chủ Đề / Chỉnh Sửa");
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

        JScrollPane documentScrollPane = new JScrollPane(docTable);
        documentScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        documentScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        documentScrollPane.setBorder(new LineBorder(BORDER_COLOR, 1));
        documentScrollPane.getViewport().setBackground(Color.WHITE);
        centerPanel.add(documentScrollPane, BorderLayout.CENTER);

        // 3. Bottom Toolbar (2 SEPARATE ROWS TO PREVENT OVERLAPPING COMPLETELY)
        JPanel bottomToolbar = new JPanel();
        bottomToolbar.setLayout(new BoxLayout(bottomToolbar, BoxLayout.Y_AXIS));

        // Row 1: action buttons remain on a dedicated line.
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 3));

        JButton btnView = new JButton("Xem Chi Tiết");
        btnView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnView.setBackground(new Color(37, 99, 235));
        btnView.setForeground(Color.WHITE);
        btnView.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnView.addActionListener(e -> viewSelectedDocument());

        JButton btnOpenExt = new JButton("Mở Ngoài");
        btnOpenExt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenExt.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnOpenExt.setToolTipText("Mở file thật (.pdf/.docx/.txt) bằng ứng dụng tương ứng trên máy tính");
        btnOpenExt.addActionListener(e -> openSelectedFileExternally());

        JButton btnRepoFolder = new JButton("Thư Mục File");
        btnRepoFolder.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRepoFolder.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnRepoFolder.setToolTipText("Mở thư mục kho lưu trữ file vật lý data/documents trong Windows Explorer");
        btnRepoFolder.addActionListener(e -> openRepositoryFolder());

        JButton btnEditCluster = new JButton("Đổi Chủ Đề");
        btnEditCluster.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditCluster.setBackground(new Color(16, 185, 129));
        btnEditCluster.setForeground(Color.WHITE);
        btnEditCluster.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnEditCluster.addActionListener(e -> editSelectedDocument());

        JButton btnRecommend = new JButton("Gợi Ý");
        btnRecommend.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRecommend.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnRecommend.addActionListener(e -> recommendForSelected());

        JButton btnDelete = new JButton("Xóa");
        btnDelete.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
        btnDelete.addActionListener(e -> deleteSelectedDocument());

        JButton btnRefresh = new JButton("Làm Mới");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 8; margin: 3,7,3,7");
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
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(5, 6, 2, 6)
        ));
        lblStatus = new JLabel("Hiển thị 0 tài liệu");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(TEXT_SECONDARY);
        statusRow.add(lblStatus, BorderLayout.WEST);

        bottomToolbar.add(actionRow);
        bottomToolbar.add(statusRow);
        centerPanel.add(bottomToolbar, BorderLayout.SOUTH);

        // A two-column GridBag layout keeps both tables in separate bounds at every supported size.
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints contentGbc = new GridBagConstraints();
        contentGbc.gridy = 0;
        contentGbc.weighty = 1.0;
        contentGbc.fill = GridBagConstraints.BOTH;

        contentGbc.gridx = 0;
        contentGbc.weightx = 0;
        contentGbc.insets = new Insets(0, 0, 0, 10);
        contentPanel.add(leftPanel, contentGbc);

        contentGbc.gridx = 1;
        contentGbc.weightx = 1.0;
        contentGbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(centerPanel, contentGbc);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void configureColumn(int index, int preferredWidth, int minimumWidth) {
        docTable.getColumnModel().getColumn(index).setPreferredWidth(preferredWidth);
        docTable.getColumnModel().getColumn(index).setMinWidth(minimumWidth);
    }

    public void refreshData() {
        clusterListModel.clear();
        clusterListModel.addElement(new ClusterItem(null, "Tất Cả Tài Liệu", repository.count(), "Toàn bộ tài liệu thư viện"));

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
            String clusterNote = (currentSelectedCluster != null)
                    ? " (Đang xem: " + clusteringService.getClusterDisplayName(currentSelectedCluster) + ")"
                    : " (Tất cả chủ đề)";
            lblStatus.setText("Hiển thị: " + docs.size() + " / " + repository.count() + " tài liệu  •  Tiếng Việt: "
                    + viCount + "  •  Tiếng Anh: " + enCount + "  •  " + clusterNote);
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
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một chủ đề cụ thể để đổi tên (không áp dụng cho 'Tất Cả Tài Liệu')!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
        panel.setPreferredSize(new Dimension(340, 100));
        JLabel lblTitle = new JLabel("Nhập tên mới cho chủ đề:");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtNewName = new JTextField(selected.name, 26);
        txtNewName.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lblKws = new JLabel("Từ khóa đặc trưng (tùy chọn):");
        lblKws.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JTextField txtNewKws = new JTextField(selected.keywords, 26);
        txtNewKws.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        panel.add(lblTitle);
        panel.add(txtNewName);
        panel.add(lblKws);
        panel.add(txtNewKws);

        int result = JOptionPane.showConfirmDialog(this, panel, "Đổi Tên Chủ Đề",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = txtNewName.getText().trim();
            String newKws = txtNewKws.getText().trim();
            if (!newName.isEmpty()) {
                clusteringService.renameCluster(selected.clusterId, newName, newKws);
                refreshData();
                JOptionPane.showMessageDialog(this, "Đã cập nhật tên chủ đề thành công!", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
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
        private static final Color SELECTED_BG = new Color(239, 246, 255);
        private static final Color SELECTED_BORDER = new Color(191, 219, 254);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!(value instanceof ClusterItem item)) return label;

            String titleColor = isSelected ? "#315b9a" : "#334155";
            String subColor = isSelected ? "#5272a3" : "#64748b";
            String keywords = item.keywords == null ? "" : escapeHtml(item.keywords);

            label.setOpaque(true);
            label.setBackground(isSelected ? SELECTED_BG : Color.WHITE);
            label.setBorder(isSelected
                    ? new CompoundBorder(new LineBorder(SELECTED_BORDER, 1, true), new EmptyBorder(5, 7, 5, 7))
                    : new EmptyBorder(6, 8, 6, 8));
            label.setText("<html><table width='300' cellpadding='0' cellspacing='0'>"
                    + "<tr><td valign='top'><span style='font-size:11px;font-weight:bold;color:" + titleColor + ";'>"
                    + formatTitle(item.name) + "</span></td>"
                    + "<td width='42' align='right' valign='top'><span style='font-size:10px;font-weight:bold;color:#3b82f6;'>"
                    + "(" + item.count + ")</span></td></tr>"
                    + (!keywords.isEmpty()
                    ? "<tr><td colspan='2'><span style='font-size:9.5px;font-style:italic;color:" + subColor + ";'>" + keywords + "</span></td></tr>"
                    : "")
                    + "</table></html>");
            label.setToolTipText("<html><b>" + escapeHtml(item.name) + "</b> (" + item.count + " tài liệu)" +
                    (!keywords.isEmpty() ? "<br/><i>Từ khóa: " + keywords + "</i>" : "") +
                    (item.clusterId != null ? "<br/><span style='color:gray;'>Nhấp đúp chuột để đổi tên</span>" : "") + "</html>");
            return label;
        }

        private static String formatTitle(String text) {
            if (text == null) return "";
            final int wrapAt = 31;
            if (text.length() <= wrapAt) return escapeHtml(text);

            int split = text.lastIndexOf(' ', wrapAt);
            if (split < 20) split = text.indexOf(' ', wrapAt);
            if (split < 0) return escapeHtml(text);
            return escapeHtml(text.substring(0, split)) + "<br>"
                    + escapeHtml(text.substring(split + 1));
        }

        private static String escapeHtml(String text) {
            if (text == null) return "";
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
        }
    }

    private static class DocumentCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setBorder(new EmptyBorder(0, 7, 0, 7));
            if (!isSelected) lbl.setForeground(TEXT_PRIMARY);
            lbl.setToolTipText(value != null ? value.toString() : null);
            return lbl;
        }
    }

    private static class LanguageCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            lbl.setHorizontalAlignment(CENTER);
            lbl.setBorder(new EmptyBorder(0, 5, 0, 5));
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
            lbl.setBorder(new EmptyBorder(0, 5, 0, 5));
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
            lbl.setBorder(new EmptyBorder(0, 7, 0, 7));
            lbl.setToolTipText(val);
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
