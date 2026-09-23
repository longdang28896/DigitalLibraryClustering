package com.dmml.library.ml.feature;

import java.util.*;

public class TfIdfVectorizer {

    private final Map<String, Integer> termToIndex = new HashMap<>();
    private final List<String> indexToTerm = new ArrayList<>();
    private double[] idf;
    private int totalDocs;
    private int maxFeatures = 1500;

    public TfIdfVectorizer() {
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public int getVocabularySize() {
        return indexToTerm.size();
    }

    public String getTerm(int index) {
        if (index >= 0 && index < indexToTerm.size()) {
            return indexToTerm.get(index);
        }
        return "";
    }

    /**
     * Fits the vectorizer on tokenized documents:
     * 1. Count document frequency (DF) for each term.
     * 2. Filter terms with minDf and maxDf.
     * 3. Select top maxFeatures.
     * 4. Compute IDF weights.
     */
    public synchronized void fit(List<List<String>> tokenizedDocs, int minDf, double maxDfRatio) {
        termToIndex.clear();
        indexToTerm.clear();
        totalDocs = tokenizedDocs.size();

        if (totalDocs == 0) return;

        Map<String, Integer> docFrequencies = new HashMap<>();
        for (List<String> doc : tokenizedDocs) {
            Set<String> uniqueTerms = new HashSet<>(doc);
            for (String term : uniqueTerms) {
                docFrequencies.put(term, docFrequencies.getOrDefault(term, 0) + 1);
            }
        }

        int maxDf = (int) (totalDocs * maxDfRatio);
        List<Map.Entry<String, Integer>> candidates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : docFrequencies.entrySet()) {
            int df = entry.getValue();
            if (df >= minDf && df <= maxDf) {
                candidates.add(entry);
            }
        }

        // Sort descending by document frequency
        candidates.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int limit = Math.min(candidates.size(), maxFeatures);
        idf = new double[limit];

        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = candidates.get(i);
            String term = entry.getKey();
            int df = entry.getValue();

            termToIndex.put(term, i);
            indexToTerm.add(term);

            // Smoothed IDF formula
            idf[i] = Math.log((1.0 + totalDocs) / (1.0 + df)) + 1.0;
        }
    }

    /**
     * Transforms a tokenized document into an L2-normalized TF-IDF vector.
     */
    public double[] transform(List<String> tokens) {
        int vocabSize = indexToTerm.size();
        double[] vector = new double[vocabSize];
        if (vocabSize == 0 || tokens == null || tokens.isEmpty()) {
            return vector;
        }

        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.put(token, counts.getOrDefault(token, 0) + 1);
        }

        double normSq = 0.0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Integer idx = termToIndex.get(entry.getKey());
            if (idx != null) {
                int count = entry.getValue();
                // Sublinear TF scaling
                double tf = 1.0 + Math.log(count);
                double weight = tf * idf[idx];
                vector[idx] = weight;
                normSq += weight * weight;
            }
        }

        // L2 Normalization
        if (normSq > 0) {
            double norm = Math.sqrt(normSq);
            for (int i = 0; i < vocabSize; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    /**
     * Transforms a collection of tokenized documents into a matrix of TF-IDF vectors.
     */
    public double[][] fitTransform(List<List<String>> tokenizedDocs) {
        fit(tokenizedDocs, 2, 0.85);
        double[][] matrix = new double[tokenizedDocs.size()][];
        for (int i = 0; i < tokenizedDocs.size(); i++) {
            matrix[i] = transform(tokenizedDocs.get(i));
        }
        return matrix;
    }
}
