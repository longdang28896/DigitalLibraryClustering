package com.dmml.library.service;

import com.dmml.library.ml.clustering.ClusterAlgorithm;
import com.dmml.library.ml.clustering.DBSCANClusterer;
import com.dmml.library.ml.clustering.HierarchicalClusterer;
import com.dmml.library.ml.clustering.KMeansClusterer;
import com.dmml.library.ml.evaluation.DaviesBouldinCalculator;
import com.dmml.library.ml.evaluation.PurityCalculator;
import com.dmml.library.ml.evaluation.SilhouetteCalculator;
import com.dmml.library.ml.feature.TfIdfVectorizer;
import com.dmml.library.ml.nlp.TextPreprocessor;
import com.dmml.library.model.ClusterInfo;
import com.dmml.library.model.ClusteringResult;
import com.dmml.library.model.Document;
import com.dmml.library.model.EvaluationMetrics;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ClusteringService {

    private static ClusteringService instance;

    private final DocumentRepository documentRepository;
    private final TextPreprocessor textPreprocessor;
    private final TfIdfVectorizer tfIdfVectorizer;
    private final KMeansClusterer kMeansClusterer;
    private final HierarchicalClusterer hierarchicalClusterer;
    private final DBSCANClusterer dbscanClusterer;
    private final SilhouetteCalculator silhouetteCalculator;
    private final DaviesBouldinCalculator daviesBouldinCalculator;
    private final PurityCalculator purityCalculator;

    private ClusteringResult latestResult;
    private double[][] cachedVectors;
    private List<Document> cachedDocs = new ArrayList<>();
    private final Map<Integer, double[]> docVectorMap = new HashMap<>();
    private final Map<Integer, String> customClusterNames = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, String> customClusterKeywords = new java.util.concurrent.ConcurrentHashMap<>();

    public static class TopicMeta {
        public final String name;
        public final String keywords;

        public TopicMeta(String name, String keywords) {
            this.name = name;
            this.keywords = keywords;
        }
    }

    public TopicMeta resolveAcademicTopic(int clusterId, List<String> topWords) {
        if (clusterId == -1) {
            return new TopicMeta("Tài Liệu Dị Biệt / Ngoại Lai", "Các tài liệu dị biệt");
        }
        if (customClusterNames.containsKey(clusterId)) {
            String customName = customClusterNames.get(clusterId);
            String customKw = customClusterKeywords.getOrDefault(clusterId, "");
            return new TopicMeta(customName, customKw);
        }

        int displayNum = clusterId + 1;
        String allTokens = (topWords != null ? String.join(" ", topWords).toLowerCase() : "");

        if (allTokens.contains("encrypt") || allTokens.contains("crypt") || allTokens.contains("clipper") || allTokens.contains("kei") || allTokens.contains("chip") || allTokens.contains("escrow")) {
            return new TopicMeta(
                "An Ninh Mạng & Mật Mã Học",
                "Mã hóa, Khóa bảo mật, An toàn thông tin"
            );
        } else if (allTokens.contains("space") || allTokens.contains("nasa") || allTokens.contains("orbit") || allTokens.contains("satellit") || allTokens.contains("launch") || allTokens.contains("astronomi")) {
            return new TopicMeta(
                "Khoa Học Vũ Trụ & Thiên Văn",
                "Thám hiểm không gian, Vệ tinh, NASA"
            );
        } else if (allTokens.contains("graphic") || allTokens.contains("imag") || allTokens.contains("polygon") || allTokens.contains("render") || allTokens.contains("shading") || allTokens.contains("3d")) {
            return new TopicMeta(
                "Đồ Họa Máy Tính & Thị Giác Số",
                "Đồ họa máy tính, Xử lý ảnh 3D, Lưới đa giác"
            );
        } else if (allTokens.contains("israel") || allTokens.contains("armenian") || allTokens.contains("arab") || allTokens.contains("jew") || allTokens.contains("peac") || allTokens.contains("polici") || allTokens.contains("turkish")) {
            return new TopicMeta(
                "Chính Sách Ngoại Giao & Quan Hệ Quốc Tế",
                "Ngoại giao hòa bình, Hiệp ước, Chính sách"
            );
        } else if (allTokens.contains("basebal") || allTokens.contains("player") || allTokens.contains("game") || allTokens.contains("season") || allTokens.contains("team") || allTokens.contains("pitch")) {
            return new TopicMeta(
                "Khoa Học Thể Thao & Vận Động",
                "Bóng chày, Cầu thủ, Mùa giải thi đấu"
            );
        } else if (allTokens.contains("pitt") || allTokens.contains("bank") || allTokens.contains("gordon") || allTokens.contains("geb") || allTokens.contains("medic") || allTokens.contains("patient") || allTokens.contains("diseas") || allTokens.contains("doctor") || allTokens.contains("skeptic")) {
            return new TopicMeta(
                "Y Học Lâm Sàng & Dược Phẩm",
                "Điều trị y khoa, Bệnh nhân, Kháng sinh"
            );
        } else {
            List<String> cleanWords = new ArrayList<>();
            if (topWords != null) {
                Set<String> noise = Set.of("edu", "com", "apr", "year", "don", "just", "article", "writes", "bank", "pitt", "gordon");
                for (String w : topWords) {
                    if (!noise.contains(w.toLowerCase())) {
                        cleanWords.add(w);
                    }
                }
            }
            String kwStr = cleanWords.isEmpty() ? (topWords != null ? String.join(", ", topWords) : "") : String.join(", ", cleanWords);
            return new TopicMeta(
                "Chuyên Đề #" + displayNum,
                kwStr
            );
        }
    }

    public synchronized void renameCluster(int clusterId, String newName, String newKeywords) {
        if (newName != null && !newName.trim().isEmpty()) {
            customClusterNames.put(clusterId, newName.trim());
        }
        if (newKeywords != null) {
            customClusterKeywords.put(clusterId, newKeywords.trim());
        }
        if (latestResult != null && latestResult.getClusters() != null) {
            for (ClusterInfo c : latestResult.getClusters()) {
                if (c.getClusterId() == clusterId) {
                    if (newName != null && !newName.trim().isEmpty()) {
                        c.setClusterName(newName.trim());
                    }
                    if (newKeywords != null) {
                        c.setDisplayKeywords(newKeywords.trim());
                    }
                }
            }
        }
    }

    public synchronized void resetClusterName(int clusterId) {
        customClusterNames.remove(clusterId);
        customClusterKeywords.remove(clusterId);
        if (latestResult != null && latestResult.getClusters() != null) {
            for (ClusterInfo c : latestResult.getClusters()) {
                if (c.getClusterId() == clusterId) {
                    TopicMeta meta = resolveAcademicTopic(clusterId, c.getTopKeywords());
                    c.setClusterName(meta.name);
                    c.setDisplayKeywords(meta.keywords);
                }
            }
        }
    }

    public String getClusterDisplayName(int clusterId) {
        if (clusterId < 0) return "Chưa phân loại";
        if (latestResult != null && latestResult.getClusters() != null) {
            for (ClusterInfo c : latestResult.getClusters()) {
                if (c.getClusterId() == clusterId) {
                    return c.getClusterName();
                }
            }
        }
        return "Chủ Đề #" + (clusterId + 1);
    }

    private ClusteringService() {
        this.documentRepository = DocumentRepository.getInstance();
        this.textPreprocessor = new TextPreprocessor();
        this.tfIdfVectorizer = new TfIdfVectorizer();
        this.kMeansClusterer = new KMeansClusterer();
        this.hierarchicalClusterer = new HierarchicalClusterer();
        this.dbscanClusterer = new DBSCANClusterer();
        this.silhouetteCalculator = new SilhouetteCalculator();
        this.daviesBouldinCalculator = new DaviesBouldinCalculator();
        this.purityCalculator = new PurityCalculator();

        // Run initial clustering
        runClustering("kmeans", 6);
    }

    public static synchronized ClusteringService getInstance() {
        if (instance == null) {
            instance = new ClusteringService();
        }
        return instance;
    }

    public synchronized ClusteringResult runClustering(String algorithm, int k) {
        long startTime = System.currentTimeMillis();
        List<Document> docs = documentRepository.findAll();
        if (docs.isEmpty()) {
            return new ClusteringResult(algorithm, k);
        }

        // 1. Text Preprocessing (Tokenize, Stopwords, Stemming)
        List<List<String>> tokenized = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            String combinedText = doc.getTitle() + " " + ((doc.getSummary() != null) ? doc.getSummary() : "") + " " + doc.getContent();
            tokenized.add(textPreprocessor.preprocess(combinedText));
        }

        // 2. Vectorization (TF-IDF)
        tfIdfVectorizer.setMaxFeatures(1500);
        cachedVectors = tfIdfVectorizer.fitTransform(tokenized);
        cachedDocs = docs;
        docVectorMap.clear();
        for (int i = 0; i < docs.size(); i++) {
            docVectorMap.put(docs.get(i).getId(), cachedVectors[i]);
        }

        // 3. Clustering Execution
        int[] assignments;
        String algoName;

        if ("hierarchical".equalsIgnoreCase(algorithm)) {
            algoName = hierarchicalClusterer.getName();
            assignments = hierarchicalClusterer.fitPredict(cachedVectors, k);
        } else if ("dbscan".equalsIgnoreCase(algorithm)) {
            algoName = dbscanClusterer.getName();
            assignments = dbscanClusterer.fitPredict(cachedVectors, k);
        } else {
            algoName = kMeansClusterer.getName();
            assignments = kMeansClusterer.fitPredict(cachedVectors, k);
        }

        long elapsedMs = System.currentTimeMillis() - startTime;

        // 4. Update Document Cluster Assignments
        Map<Integer, List<Document>> clusterDocMap = new HashMap<>();
        for (int i = 0; i < docs.size(); i++) {
            int cId = assignments[i];
            Document doc = docs.get(i);
            doc.setClusterId(cId);
            clusterDocMap.computeIfAbsent(cId, x -> new ArrayList<>()).add(doc);
        }

        // 5. Build Cluster Details & Top Keywords
        List<ClusterInfo> clusterInfos = new ArrayList<>();
        int vocabSize = tfIdfVectorizer.getVocabularySize();

        List<Integer> sortedClusterIds = new ArrayList<>(clusterDocMap.keySet());
        Collections.sort(sortedClusterIds);

        for (int cId : sortedClusterIds) {
            List<Document> members = clusterDocMap.get(cId);
            String name = (cId == -1) ? "Tài Liệu Dị Biệt / Ngoại Lai" : "Chủ Đề #" + (cId + 1);

            ClusterInfo cInfo = new ClusterInfo(cId, name);
            cInfo.setDocumentCount(members.size());
            cInfo.setDocuments(members);

            // Compute centroid of cluster to find top keywords
            double[] centroid = new double[vocabSize];
            for (Document d : members) {
                double[] vec = docVectorMap.get(d.getId());
                if (vec != null) {
                    for (int v = 0; v < vocabSize; v++) {
                        centroid[v] += vec[v];
                    }
                }
            }

            // Find top 6 keywords with highest weights
            List<Integer> termIndices = new ArrayList<>(vocabSize);
            for (int v = 0; v < vocabSize; v++) termIndices.add(v);
            termIndices.sort((a, b) -> Double.compare(centroid[b], centroid[a]));

            List<String> topWords = new ArrayList<>();
            for (int rank = 0; rank < Math.min(6, termIndices.size()); rank++) {
                String word = tfIdfVectorizer.getTerm(termIndices.get(rank));
                if (!word.isEmpty()) {
                    topWords.add(word);
                }
            }
            cInfo.setTopKeywords(topWords);

            TopicMeta meta = resolveAcademicTopic(cId, topWords);
            cInfo.setClusterName(meta.name);
            cInfo.setDisplayKeywords(meta.keywords);

            clusterInfos.add(cInfo);
        }

        // 6. Scientific Model Evaluation
        EvaluationMetrics metrics = new EvaluationMetrics();
        metrics.setExecutionTimeMs(elapsedMs);
        metrics.setNumberOfClusters(clusterInfos.size());

        if (cachedVectors.length > 0 && assignments.length > 0) {
            metrics.setSilhouette(silhouetteCalculator.computeScore(cachedVectors, assignments));
            metrics.setDaviesBouldin(daviesBouldinCalculator.computeScore(cachedVectors, assignments));

            List<String> trueLabels = docs.stream()
                    .map(d -> (d.getOriginalCategory() != null) ? d.getOriginalCategory() : "unknown")
                    .collect(Collectors.toList());
            metrics.setPurity(purityCalculator.computePurity(trueLabels, assignments));

            if ("kmeans".equalsIgnoreCase(algorithm)) {
                metrics.setInertia(kMeansClusterer.getInertia());
            }
        }

        latestResult = new ClusteringResult(algoName, k);
        latestResult.setClusters(clusterInfos);
        latestResult.setMetrics(metrics);
        latestResult.setTotalDocuments(docs.size());

        System.out.println("Desktop Clustering completed: " + algoName + " (K=" + k + "), Docs=" + docs.size() +
                ", Silhouette=" + String.format("%.4f", metrics.getSilhouette()) +
                ", Time=" + elapsedMs + "ms");

        return latestResult;
    }

    /**
     * Auto-clustering for a newly introduced document (Yêu cầu 2.4).
     */
    public Map<String, Object> autoClassifyDocument(String title, String content) {
        Map<String, Object> result = new HashMap<>();
        String fullText = (title != null ? title : "") + " " + (content != null ? content : "");
        List<String> tokens = textPreprocessor.preprocess(fullText);

        if (tokens.isEmpty() || latestResult == null || latestResult.getClusters().isEmpty()) {
            result.put("predictedClusterId", 0);
            result.put("confidence", 0.0);
            result.put("clusterName", "Chưa phân cụm");
            result.put("topKeywords", Collections.emptyList());
            return result;
        }

        double[] docVector = tfIdfVectorizer.transform(tokens);
        double[] pred = kMeansClusterer.predictWithSimilarity(docVector);
        int predictedClusterId = (int) pred[0];
        double confidence = pred[1];

        result.put("predictedClusterId", predictedClusterId);
        result.put("confidence", Math.round(confidence * 1000.0) / 10.0);

        ClusterInfo matchedCluster = latestResult.getClusters().stream()
                .filter(c -> c.getClusterId() == predictedClusterId)
                .findFirst()
                .orElse(null);

        if (matchedCluster != null) {
            result.put("clusterName", matchedCluster.getClusterName());
            result.put("topKeywords", matchedCluster.getTopKeywords());
        } else {
            result.put("clusterName", "Chủ Đề #" + (predictedClusterId + 1));
            result.put("topKeywords", Collections.emptyList());
        }

        return result;
    }

    /**
     * Manually reassign a document to a different cluster (Yêu cầu cho phép sửa khi gán nhóm không đúng ý).
     */
    public synchronized void manualReassignCluster(int docId, int newClusterId, String newClusterName) {
        Optional<Document> opt = documentRepository.findById(docId);
        if (opt.isEmpty()) return;
        Document doc = opt.get();
        int oldClusterId = doc.getClusterId();

        documentRepository.reassignCluster(docId, newClusterId, newClusterName);

        if (latestResult != null && latestResult.getClusters() != null) {
            for (ClusterInfo c : latestResult.getClusters()) {
                if (c.getClusterId() == oldClusterId && c.getDocuments() != null) {
                    c.getDocuments().removeIf(d -> d.getId() == docId);
                    c.setDocumentCount(c.getDocuments().size());
                }
            }
            boolean found = false;
            for (ClusterInfo c : latestResult.getClusters()) {
                if (c.getClusterId() == newClusterId) {
                    if (c.getDocuments() == null) c.setDocuments(new ArrayList<>());
                    c.getDocuments().removeIf(d -> d.getId() == docId);
                    c.getDocuments().add(doc);
                    c.setDocumentCount(c.getDocuments().size());
                    found = true;
                    break;
                }
            }
            if (!found) {
                String displayName = newClusterName != null && !newClusterName.isEmpty() ? newClusterName : "Chủ Đề #" + (newClusterId + 1);
                ClusterInfo newInfo = new ClusterInfo(newClusterId, displayName);
                List<Document> docList = new ArrayList<>();
                docList.add(doc);
                newInfo.setDocuments(docList);
                newInfo.setDocumentCount(1);
                latestResult.getClusters().add(newInfo);
                latestResult.getClusters().sort(Comparator.comparingInt(ClusterInfo::getClusterId));
            }
        }
    }

    public void registerDocumentVector(Document doc) {
        if (doc == null) return;
        String text = (doc.getTitle() != null ? doc.getTitle() : "") + " " +
                (doc.getSummary() != null ? doc.getSummary() : "") + " " +
                (doc.getContent() != null ? doc.getContent() : "");
        List<String> tokens = textPreprocessor.preprocess(text);
        if (!tokens.isEmpty() && tfIdfVectorizer != null) {
            double[] vec = tfIdfVectorizer.transform(tokens);
            docVectorMap.put(doc.getId(), vec);
        }
    }

    /**
     * Recommends similar documents in the same cluster using Cosine Similarity (Yêu cầu 2.4).
     */
    public List<Document> recommendSimilarDocuments(int docId, int topN) {
        Optional<Document> optTarget = documentRepository.findById(docId);
        if (optTarget.isEmpty()) return Collections.emptyList();
        Document target = optTarget.get();

        double[] targetVector = docVectorMap.get(docId);
        if (targetVector == null) {
            String text = target.getTitle() + " " + ((target.getSummary() != null) ? target.getSummary() : "") + " " + target.getContent();
            targetVector = tfIdfVectorizer.transform(textPreprocessor.preprocess(text));
        }

        List<Document> candidates = documentRepository.findByClusterId(target.getClusterId());
        final double[] queryVec = targetVector;

        List<Document> scoredDocs = new ArrayList<>();
        for (Document cand : candidates) {
            if (cand.getId() == target.getId()) continue;

            double[] candVec = docVectorMap.get(cand.getId());
            double sim = (candVec != null) ? KMeansClusterer.cosineSimilarity(queryVec, candVec) : 0.0;

            Document copy = new Document(cand.getId(), cand.getTitle(), cand.getAuthor(), cand.getTopic(), cand.getSummary(), cand.getContent());
            copy.setClusterId(cand.getClusterId());
            copy.setSimilarityScore(Math.round(sim * 10000.0) / 100.0);
            scoredDocs.add(copy);
        }

        scoredDocs.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        return scoredDocs.stream().limit(topN).collect(Collectors.toList());
    }

    public double[] getDocumentVector(int docId) {
        return docVectorMap.get(docId);
    }

    public KMeansClusterer getKMeansClusterer() {
        return kMeansClusterer;
    }

    public ClusteringResult getLatestResult() {
        return latestResult;
    }

    public String getExperimentResultsJson() {
        Path p = Paths.get("data", "experiment_results.json");
        if (Files.exists(p)) {
            try {
                return Files.readString(p, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "{}";
            }
        }
        return "{}";
    }
}
