package com.dmml.library.model;

import java.util.ArrayList;
import java.util.List;

public class ClusteringResult {
    private String algorithmName;
    private int k;
    private List<ClusterInfo> clusters = new ArrayList<>();
    private EvaluationMetrics metrics = new EvaluationMetrics();
    private int totalDocuments;

    public ClusteringResult() {
    }

    public ClusteringResult(String algorithmName, int k) {
        this.algorithmName = algorithmName;
        this.k = k;
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public List<ClusterInfo> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterInfo> clusters) {
        this.clusters = clusters;
    }

    public EvaluationMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(EvaluationMetrics metrics) {
        this.metrics = metrics;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(int totalDocuments) {
        this.totalDocuments = totalDocuments;
    }
}
