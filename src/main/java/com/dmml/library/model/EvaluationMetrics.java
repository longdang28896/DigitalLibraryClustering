package com.dmml.library.model;

public class EvaluationMetrics {
    private double inertia;           // Within-Cluster Sum of Squares (WCSS)
    private double silhouette;        // Overall Silhouette Coefficient [-1, 1]
    private double daviesBouldin;     // Davies-Bouldin Index (lower is better)
    private double purity;            // Cluster purity against ground truth
    private double ari;               // Adjusted Rand Index
    private double nmi;               // Normalized Mutual Information
    private long executionTimeMs;     // Execution time in milliseconds
    private int numberOfClusters;     // Number of clusters formed

    public EvaluationMetrics() {
    }

    public double getInertia() {
        return inertia;
    }

    public void setInertia(double inertia) {
        this.inertia = inertia;
    }

    public double getSilhouette() {
        return silhouette;
    }

    public void setSilhouette(double silhouette) {
        this.silhouette = silhouette;
    }

    public double getDaviesBouldin() {
        return daviesBouldin;
    }

    public void setDaviesBouldin(double daviesBouldin) {
        this.daviesBouldin = daviesBouldin;
    }

    public double getPurity() {
        return purity;
    }

    public void setPurity(double purity) {
        this.purity = purity;
    }

    public double getAri() {
        return ari;
    }

    public void setAri(double ari) {
        this.ari = ari;
    }

    public double getNmi() {
        return nmi;
    }

    public void setNmi(double nmi) {
        this.nmi = nmi;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public int getNumberOfClusters() {
        return numberOfClusters;
    }

    public void setNumberOfClusters(int numberOfClusters) {
        this.numberOfClusters = numberOfClusters;
    }
}
