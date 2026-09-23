package com.dmml.library.model;

import java.util.ArrayList;
import java.util.List;

public class ClusterInfo {
    private int clusterId;
    private String clusterName;
    private List<String> topKeywords = new ArrayList<>();
    private String displayKeywords;
    private int documentCount;
    private double avgSilhouette;
    private List<Document> documents = new ArrayList<>();

    public ClusterInfo() {
    }

    public ClusterInfo(int clusterId, String clusterName) {
        this.clusterId = clusterId;
        this.clusterName = clusterName;
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public List<String> getTopKeywords() {
        return topKeywords;
    }

    public void setTopKeywords(List<String> topKeywords) {
        this.topKeywords = topKeywords;
    }

    public String getDisplayKeywords() {
        return displayKeywords;
    }

    public void setDisplayKeywords(String displayKeywords) {
        this.displayKeywords = displayKeywords;
    }

    public int getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(int documentCount) {
        this.documentCount = documentCount;
    }

    public double getAvgSilhouette() {
        return avgSilhouette;
    }

    public void setAvgSilhouette(double avgSilhouette) {
        this.avgSilhouette = avgSilhouette;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Document> documents) {
        this.documents = documents;
    }
}
