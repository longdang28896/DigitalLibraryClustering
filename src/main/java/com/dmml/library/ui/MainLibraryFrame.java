package com.dmml.library.ui;

import com.dmml.library.model.Document;
import com.dmml.library.service.ClusteringService;
import com.dmml.library.service.DocumentRepository;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;

public class MainLibraryFrame extends JFrame {

    private final JTabbedPane tabbedPane;
    private final DocumentCatalogPanel catalogPanel;
    private final AddDocumentPanel addDocPanel;
    private final RecommendationPanel recommendationPanel;
    private final AnalyticsPanel analyticsPanel;
    private final JLabel lblBottomStatus;
    private final JLabel lblStatDocs;
    private final JLabel lblStatClusters;

    public MainLibraryFrame() {
        super("HỆ THỐNG QUẢN LÝ THƯ VIỆN SỐ TÍCH HỢP PHÂN CỤM TÀI LIỆU - ĐỀ TÀI DMML 2508");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1180, 680));
        setLocationRelativeTo(null);

        initMenuBar();

        JPanel rootPane = new JPanel(new BorderLayout());

        // --- TOP BRANDING BANNER ---
        JPanel topBanner = new JPanel(new BorderLayout(15, 0));
        topBanner.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(12, 20, 12, 20)
        ));
        topBanner.setBackground(new Color(255, 255, 255));

        // Left Branding (Clean text without emoji to avoid square boxes on Windows)
        JPanel brandLeft = new JPanel();
        brandLeft.setLayout(new BoxLayout(brandLeft, BoxLayout.Y_AXIS));
        brandLeft.setOpaque(false);

        JLabel lblTitle = new JLabel("HỆ THỐNG QUẢN LÝ THƯ VIỆN SỐ & PHÂN CỤM TÀI LIỆU");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(30, 58, 138));

        JLabel lblSub = new JLabel("BÀI TẬP LỚN MÔN: NHẬP MÔN KHAI PHÁ DỮ LIỆU VÀ MÁY HỌC (DMML) • ĐỀ TÀI SỐ 2508 • CÔNG NGHỆ: JAVA");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(100, 116, 139));

        brandLeft.add(lblTitle);
        brandLeft.add(Box.createVerticalStrut(2));
        brandLeft.add(lblSub);

        // Right Stats Badges
        JPanel badgeGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        badgeGroup.setOpaque(false);

        lblStatDocs = createPillBadge("Tài Liệu: " + DocumentRepository.getInstance().count(), new Color(239, 246, 255), new Color(37, 99, 235));
        lblStatClusters = createPillBadge("Chủ Đề: 6", new Color(236, 253, 245), new Color(16, 185, 129));
        JLabel lblLangs = createPillBadge("Ngôn Ngữ: VI / EN", new Color(254, 243, 199), new Color(217, 119, 6));
        JLabel lblFormats = createPillBadge("PDF • DOCX • TXT", new Color(243, 244, 246), new Color(75, 85, 99));

        badgeGroup.add(lblStatDocs);
        badgeGroup.add(lblStatClusters);
        badgeGroup.add(lblLangs);
        badgeGroup.add(lblFormats);

        topBanner.add(brandLeft, BorderLayout.WEST);
        topBanner.add(badgeGroup, BorderLayout.EAST);
        rootPane.add(topBanner, BorderLayout.NORTH);

        // --- MAIN TABS ---
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, "underlined");
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, true);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, 36);

        catalogPanel = new DocumentCatalogPanel(this);
        addDocPanel = new AddDocumentPanel(this);
        recommendationPanel = new RecommendationPanel(this);
        analyticsPanel = new AnalyticsPanel(this);

        tabbedPane.addTab("   1. Kho Tài Liệu & Duyệt Theo Cụm   ", catalogPanel);
        tabbedPane.addTab("   2. Thêm Sách Mới & Phân Cụm Tự Động   ", addDocPanel);
        tabbedPane.addTab("   3. Đề Xuất Tài Liệu Tương Tự   ", recommendationPanel);
        tabbedPane.addTab("   4. Đánh Giá Khoa Học (DMML)   ", analyticsPanel);

        rootPane.add(tabbedPane, BorderLayout.CENTER);

        // --- STATUS BAR ---
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(5, 14, 5, 14)
        ));
        statusBar.setBackground(new Color(248, 250, 252));

        lblBottomStatus = new JLabel();
        lblBottomStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        updateStatusBarText();

        JLabel lblCopyright = new JLabel("Khoa Công nghệ Thông tin | Hệ thống Thư viện số Thông minh (Java 17, Swing, FlatLaf)");
        lblCopyright.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCopyright.setForeground(new Color(148, 163, 184));

        statusBar.add(lblBottomStatus, BorderLayout.WEST);
        statusBar.add(lblCopyright, BorderLayout.EAST);
        rootPane.add(statusBar, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(rootPane, BorderLayout.CENTER);
    }

    private JLabel createPillBadge(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(" " + text + " ");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, fg),
                new EmptyBorder(3, 8, 3, 8)
        ));
        return lbl;
    }

    private void initMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Hệ Thống
        JMenu menuSystem = new JMenu("Hệ Thống");
        JMenuItem itemRefresh = new JMenuItem("Làm Mới Toàn Bộ");
        itemRefresh.addActionListener(e -> refreshCatalog());
        JMenuItem itemExit = new JMenuItem("Thoát Ứng Dụng");
        itemExit.addActionListener(e -> System.exit(0));

        menuSystem.add(itemRefresh);
        menuSystem.addSeparator();
        menuSystem.add(itemExit);

        // Menu Khai Phá Dữ Liệu
        JMenu menuML = new JMenu("Khai Phá Dữ Liệu");
        JMenuItem itemRunKMeans = new JMenuItem("Tái phân cụm K-Means (K=6)");
        itemRunKMeans.addActionListener(e -> {
            ClusteringService.getInstance().runClustering("kmeans", 6);
            refreshCatalog();
            JOptionPane.showMessageDialog(this, "Đã chạy lại phân cụm K-Means!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
        });
        JMenuItem itemRunHierarchical = new JMenuItem("Tái phân cụm Hierarchical (K=6)");
        itemRunHierarchical.addActionListener(e -> {
            ClusteringService.getInstance().runClustering("hierarchical", 6);
            refreshCatalog();
            JOptionPane.showMessageDialog(this, "Đã chạy lại phân cụm Hierarchical!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
        });

        menuML.add(itemRunKMeans);
        menuML.add(itemRunHierarchical);

        // Menu Trợ Giúp
        JMenu menuHelp = new JMenu("Trợ Giúp");
        JMenuItem itemSamples = new JMenuItem("Thư Mục File Mẫu (data/samples)");
        itemSamples.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new java.io.File("data/samples"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Thư mục: data/samples\n" + ex.getMessage(), "File Mẫu", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JMenuItem itemAbout = new JMenuItem("Giới Thiệu Đề Tài");
        itemAbout.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "BÀI TẬP LỚN: NHẬP MÔN KHAI PHÁ DỮ LIỆU VÀ MÁY HỌC\n" +
                    "ĐỀ SỐ 2508: Ứng dụng kỹ thuật phân cụm tích hợp vào hệ thống quản lý thư viện số trên công nghệ Java.\n\n" +
                    "• Giao diện: Java Swing Desktop + FlatLaf hiện đại, thanh thoát\n" +
                    "• Định dạng nạp file: PDF (PDFBox), Word DOCX (Apache POI), TXT, JSON, MD\n" +
                    "• Hỗ trợ ngôn ngữ: Tiếng Việt (Unicode đầy đủ dấu) & Tiếng Anh (20 Newsgroups)\n" +
                    "• Tùy chỉnh gán cụm: Cho phép người dùng can thiệp sửa đổi nhóm phân cụm theo ý muốn\n" +
                    "• Thuật toán ML: K-Means++, Agglomerative Hierarchical, DBSCAN\n" +
                    "• Độ đo: Silhouette, Davies-Bouldin, Elbow Method (WCSS), Purity, ARI, NMI.",
                    "Thông Tin Đề Tài BTL", JOptionPane.INFORMATION_MESSAGE);
        });

        menuHelp.add(itemSamples);
        menuHelp.addSeparator();
        menuHelp.add(itemAbout);

        menuBar.add(menuSystem);
        menuBar.add(menuML);
        menuBar.add(menuHelp);

        setJMenuBar(menuBar);
    }

    private void updateStatusBarText() {
        int total = DocumentRepository.getInstance().count();
        List<Document> docs = DocumentRepository.getInstance().findAll();
        long viCount = docs.stream().filter(d -> "VI".equalsIgnoreCase(d.getLanguage())).count();
        long enCount = total - viCount;

        lblBottomStatus.setText("Hệ thống sẵn sàng. Tổng số: " + total + " tài liệu (" +
                "Tiếng Việt: " + viCount + ", Tiếng Anh: " + enCount + ") • Thuật toán tối ưu: K-Means (K=6)");
    }

    public void refreshCatalog() {
        catalogPanel.refreshData();
        addDocPanel.populateClusterDropdown();
        recommendationPanel.populateDocList();
        updateStatusBarText();
        int total = DocumentRepository.getInstance().count();
        lblStatDocs.setText(" Tài Liệu: " + total + " ");
        if (ClusteringService.getInstance().getLatestResult() != null &&
            ClusteringService.getInstance().getLatestResult().getClusters() != null) {
            lblStatClusters.setText(" Cụm Chủ Đề: " + ClusteringService.getInstance().getLatestResult().getClusters().size() + " ");
        }
    }

    public void showRecommendationForDoc(int docId) {
        tabbedPane.setSelectedIndex(2); // Switch to Tab 3 (Recommendation)
        recommendationPanel.selectDocumentAndRun(docId);
    }
}
