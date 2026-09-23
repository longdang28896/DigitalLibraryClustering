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
            String name = (cId == -1) ? "Nhiễu / Ngoại lai (Outlier)" : "Cụm #" + (cId + 1);

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

            if (!topWords.isEmpty() && cId >= 0) {
                String desc = topWords.stream().limit(3).collect(Collectors.joining(" • "));
                cInfo.setClusterName("Cụm #" + (cId + 1) + ": [" + desc.toUpperCase() + "]");
            }

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
            result.put("clusterName", "Cụm #" + (predictedClusterId + 1));
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
                String displayName = newClusterName != null && !newClusterName.isEmpty() ? newClusterName : "Cụm #" + (newClusterId + 1);
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
