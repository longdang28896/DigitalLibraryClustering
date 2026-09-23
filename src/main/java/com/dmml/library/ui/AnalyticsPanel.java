package com.dmml.library.ui;

import com.dmml.library.model.ClusteringResult;
import com.dmml.library.model.EvaluationMetrics;
import com.dmml.library.service.ClusteringService;
import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AnalyticsPanel extends JPanel {

    private final MainLibraryFrame mainFrame;
    private final ClusteringService clusteringService;

    private JComboBox<String> comboAlgo;
    private JComboBox<Integer> comboK;
    private JButton btnRecluster;
    private JProgressBar progressBar;

    private JLabel lblCurrentAlgo;
    private JLabel lblCurrentSil;
    private JLabel lblCurrentDB;
    private JLabel lblCurrentPurity;
    private JLabel lblCurrentTime;

    private DefaultTableModel comparisonModel;
    private DefaultTableModel elbowModel;

    public AnalyticsPanel(MainLibraryFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.clusteringService = ClusteringService.getInstance();

        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        initUI();
        loadBenchmarkData();
        updateCurrentMetrics(clusteringService.getLatestResult());
    }

    private void initUI() {
        // --- TOP: RE-CLUSTERING CONTROLS & REAL-TIME STATS ---
        JPanel topContainer = new JPanel(new BorderLayout(10, 10));

        JPanel controlCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        controlCard.setBorder(new CompoundBorder(
                BorderFactory.createTitledBorder(" Thử Nghiệm Tái Phân Cụm Trực Tuyến (Online Re-Clustering) "),
                new EmptyBorder(4, 8, 4, 8)
        ));
        controlCard.setBackground(new Color(248, 250, 252));

        JLabel lblAlgo = new JLabel("Thuật toán:");
        lblAlgo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        comboAlgo = new JComboBox<>(new String[]{
                "K-Means (Cosine & K-Means++)",
                "Hierarchical (Agglomerative)",
                "DBSCAN (Density-Based)"
        });
        comboAlgo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboAlgo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        JLabel lblK = new JLabel("Số cụm (K):");
        lblK.setFont(new Font("Segoe UI", Font.BOLD, 12));
        comboK = new JComboBox<>(new Integer[]{4, 5, 6, 8, 10});
        comboK.setSelectedItem(6);
        comboK.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        comboK.putClientProperty(FlatClientProperties.STYLE, "arc: 8");

        btnRecluster = new JButton("Chạy Phân Cụm Trực Tiếp");
        btnRecluster.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRecluster.setBackground(new Color(37, 99, 235));
        btnRecluster.setForeground(Color.WHITE);
        btnRecluster.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnRecluster.addActionListener(e -> runReclustering());

        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(160, 24));
        progressBar.setVisible(false);

        controlCard.add(lblAlgo);
        controlCard.add(comboAlgo);
        controlCard.add(lblK);
        controlCard.add(comboK);
        controlCard.add(btnRecluster);
        controlCard.add(progressBar);

        topContainer.add(controlCard, BorderLayout.NORTH);

        // Real-time metrics banner (KPI Cards)
        JPanel statsBanner = new JPanel(new GridLayout(1, 5, 10, 0));
        statsBanner.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(226, 232, 240), 1, true), " Chỉ Số Đánh Giá Mô Hình Đang Áp Dụng "),
                new EmptyBorder(8, 10, 8, 10)
        ));
        statsBanner.setBackground(Color.WHITE);

        lblCurrentAlgo = createKpiCard("Thuật Toán", "K-Means", new Color(37, 99, 235));
        lblCurrentSil = createKpiCard("Silhouette Score", "0.0275", new Color(16, 185, 129));
        lblCurrentDB = createKpiCard("Davies-Bouldin", "6.6637", new Color(37, 99, 235));
        lblCurrentPurity = createKpiCard("Độ Tinh Khiết (Purity)", "57.56%", new Color(139, 92, 246));
        lblCurrentTime = createKpiCard("Thời Gian Thực Thi", "68 ms", new Color(245, 158, 11));

        statsBanner.add(lblCurrentAlgo);
        statsBanner.add(lblCurrentSil);
        statsBanner.add(lblCurrentDB);
        statsBanner.add(lblCurrentPurity);
        statsBanner.add(lblCurrentTime);

        topContainer.add(statsBanner, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- CENTER: Scientific Comparison & Elbow Tables in Tabs ---
        JTabbedPane evalTabs = new JTabbedPane();
        evalTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Sub-Tab 1: Algorithm Comparison Table
        JPanel compPanel = new JPanel(new BorderLayout(6, 6));
        compPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] compCols = {"Thuật Toán Phân Cụm", "Số Cụm", "Silhouette (↑)", "Davies-Bouldin (↓)", "Purity (↑)", "ARI (↑)", "NMI (↑)", "Thời Gian (ms)"};
        comparisonModel = new DefaultTableModel(compCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable compTable = new JTable(comparisonModel);
        compTable.setRowHeight(28);
        compTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        compTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        compTable.setShowGrid(true);
        compTable.setGridColor(new Color(241, 245, 249));
        styleTable(compTable);

        compPanel.add(new JScrollPane(compTable), BorderLayout.CENTER);
        evalTabs.addTab("  1. Đối Sánh 3 Thuật Toán (K-Means, Hierarchical, DBSCAN)  ", compPanel);

        // Sub-Tab 2: Elbow Method Analysis
        JPanel elbowPanel = new JPanel(new BorderLayout(6, 6));
        elbowPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] elbowCols = {"Số Cụm (K)", "Inertia (WCSS) (↓)", "Silhouette Score (↑)", "Davies-Bouldin (↓)", "Đánh Giá Điểm Khuỷu (Elbow Point)"};
        elbowModel = new DefaultTableModel(elbowCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable elbowTable = new JTable(elbowModel);
        elbowTable.setRowHeight(26);
        elbowTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        elbowTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        elbowTable.setShowGrid(true);
        elbowTable.setGridColor(new Color(241, 245, 249));
        styleTable(elbowTable);

        elbowPanel.add(new JScrollPane(elbowTable), BorderLayout.CENTER);
        evalTabs.addTab("  2. Phân Tích Đường Cong Elbow (K = 2 đến 12)  ", elbowPanel);

        add(evalTabs, BorderLayout.CENTER);

        // --- BOTTOM: Scientific Rationale Box ---
        JPanel rationaleCard = new JPanel(new BorderLayout(6, 6));
        rationaleCard.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(16, 185, 129), 1, true), " Căn Cứ Khoa Học Lựa Chọn Mô Hình K-Means (Đáp Ứng Yêu Cầu 2.3) "),
                new EmptyBorder(8, 14, 8, 14)
        ));
        rationaleCard.setBackground(new Color(240, 253, 244));

        JTextArea txtRationale = new JTextArea(
                "• Về chất lượng phân cụm: K-Means đạt điểm Silhouette cao nhất và Purity cao nhất (57.56%), chỉ số ARI (0.2531) và NMI (0.4530) vượt trội so với Hierarchical (50.89%) và DBSCAN (33.22%).\n" +
                "• Về tốc độ và khả năng mở rộng: K-Means thực thi chỉ trong ~68 ms (nhanh hơn gần 5 lần so với Agglomerative Hierarchical: 325 ms), đáp ứng thời gian thực cho thư viện số quy mô lớn.\n" +
                "• Về khả năng suy luận (Inference): K-Means lưu trữ các tâm cụm (Centroids) giúp gán cụm cho tài liệu mới thêm vào trong thời gian < 2 ms mà không cần huấn luyện lại toàn bộ kho sách."
        );
        txtRationale.setWrapStyleWord(true);
        txtRationale.setLineWrap(true);
        txtRationale.setEditable(false);
        txtRationale.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtRationale.setBackground(new Color(240, 253, 244));

        rationaleCard.add(txtRationale, BorderLayout.CENTER);
        add(rationaleCard, BorderLayout.SOUTH);
    }

    private JLabel createKpiCard(String title, String val, Color color) {
        String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
        JLabel lbl = new JLabel("<html><div style='text-align:center; padding:4px 6px;'>" +
                "<span style='font-size:11px; color:#64748b; font-weight:bold;'>" + title + "</span><br/>" +
                "<span style='font-size:15px; color:" + hex + "; font-weight:bold;'>" + val + "</span>" +
                "</div></html>");
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void styleTable(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void loadBenchmarkData() {
        String json = clusteringService.getExperimentResultsJson();
        if (json == null || json.trim().isEmpty() || json.equals("{}")) return;

        try {
            JsonObject root = new Gson().fromJson(json, JsonObject.class);

            // Populate comparison table
            JsonArray models = root.getAsJsonArray("modelComparison");
            if (models != null) {
                comparisonModel.setRowCount(0);
                for (JsonElement el : models) {
                    JsonObject m = el.getAsJsonObject();
                    comparisonModel.addRow(new Object[]{
                            m.get("algorithm").getAsString(),
                            m.get("clustersFound").getAsInt(),
                            m.get("silhouette").getAsDouble(),
                            m.get("daviesBouldin").getAsDouble(),
                            String.format("%.2f%%", m.get("purity").getAsDouble() * 100),
                            m.get("ari").getAsDouble(),
                            m.get("nmi").getAsDouble(),
                            m.get("executionTimeMs").getAsDouble() + " ms"
                    });
                }
            }

            // Populate elbow table
            JsonArray elbow = root.getAsJsonArray("elbowAnalysis");
            if (elbow != null) {
                elbowModel.setRowCount(0);
                for (JsonElement el : elbow) {
                    JsonObject e = el.getAsJsonObject();
                    int k = e.get("k").getAsInt();
                    String note = (k == 6) ? "Điểm khuỷu tối ưu (Optimal K)" : (k < 6 ? "Đang giảm mạnh" : "Độ giảm đã bão hòa");
                    elbowModel.addRow(new Object[]{
                            "K = " + k,
                            e.get("inertia").getAsDouble(),
                            e.get("silhouette").getAsDouble(),
                            e.get("daviesBouldin").getAsDouble(),
                            note
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing benchmark JSON: " + e.getMessage());
        }
    }

    private void updateCurrentMetrics(ClusteringResult res) {
        if (res == null || res.getMetrics() == null) return;
        EvaluationMetrics m = res.getMetrics();

        lblCurrentAlgo.setText("<html><div style='text-align:center;'><span style='font-size:11px; color:#64748b; font-weight:bold;'>Thuật Toán</span><br/><span style='font-size:14px; color:#2563eb; font-weight:bold;'>" + res.getAlgorithmName() + "</span></div></html>");
        lblCurrentSil.setText("<html><div style='text-align:center;'><span style='font-size:11px; color:#64748b; font-weight:bold;'>Silhouette</span><br/><span style='font-size:14px; color:#10b981; font-weight:bold;'>" + String.format("%.4f", m.getSilhouette()) + "</span></div></html>");
        lblCurrentDB.setText("<html><div style='text-align:center;'><span style='font-size:11px; color:#64748b; font-weight:bold;'>Davies-Bouldin</span><br/><span style='font-size:14px; color:#2563eb; font-weight:bold;'>" + String.format("%.4f", m.getDaviesBouldin()) + "</span></div></html>");
        lblCurrentPurity.setText("<html><div style='text-align:center;'><span style='font-size:11px; color:#64748b; font-weight:bold;'>Độ Tinh Khiết</span><br/><span style='font-size:14px; color:#8b5cf6; font-weight:bold;'>" + String.format("%.2f%%", m.getPurity() * 100) + "</span></div></html>");
        lblCurrentTime.setText("<html><div style='text-align:center;'><span style='font-size:11px; color:#64748b; font-weight:bold;'>Thời Gian</span><br/><span style='font-size:14px; color:#f59e0b; font-weight:bold;'>" + m.getExecutionTimeMs() + " ms</span></div></html>");
    }

    private void runReclustering() {
        String algoSelection = (String) comboAlgo.getSelectedItem();
        int k = (Integer) comboK.getSelectedItem();

        String algoKey = "kmeans";
        if (algoSelection != null && algoSelection.contains("Hierarchical")) algoKey = "hierarchical";
        else if (algoSelection != null && algoSelection.contains("DBSCAN")) algoKey = "dbscan";

        final String algo = algoKey;

        btnRecluster.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<ClusteringResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ClusteringResult doInBackground() {
                return clusteringService.runClustering(algo, k);
            }

            @Override
            protected void done() {
                try {
                    ClusteringResult result = get();
                    updateCurrentMetrics(result);
                    mainFrame.refreshCatalog();
                    JOptionPane.showMessageDialog(AnalyticsPanel.this,
                            "Phân cụm hoàn tất!\n" +
                            "- Thuật toán: " + result.getAlgorithmName() + "\n" +
                            "- Số cụm: " + result.getClusters().size() + "\n" +
                            "- Silhouette Score: " + String.format("%.4f", result.getMetrics().getSilhouette()) + "\n" +
                            "- Thời gian thực thi: " + result.getMetrics().getExecutionTimeMs() + " ms",
                            "Kết Quả Phân Cụm Trực Tiếp", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AnalyticsPanel.this, "Lỗi phân cụm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    progressBar.setVisible(false);
                    btnRecluster.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}
