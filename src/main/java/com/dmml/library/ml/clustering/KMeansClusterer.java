package com.dmml.library.ml.clustering;

import java.util.Arrays;
import java.util.Random;

public class KMeansClusterer implements ClusterAlgorithm {

    private double[][] centroids;
    private double inertia;
    private int k;
    private final int maxIterations;
    private final Random random;

    public KMeansClusterer() {
        this(100, 42);
    }

    public KMeansClusterer(int maxIterations, long seed) {
        this.maxIterations = maxIterations;
        this.random = new Random(seed);
    }

    @Override
    public String getName() {
        return "K-Means (K-Means++ & Cosine)";
    }

    public double[][] getCentroids() {
        return centroids;
    }

    public double getInertia() {
        return inertia;
    }

    @Override
    public int[] fitPredict(double[][] data, int k) {
        int n = data.length;
        if (n == 0 || k <= 0) return new int[0];
        if (k > n) k = n;
        this.k = k;
        int d = data[0].length;

        // 1. K-Means++ Initialization
        centroids = new double[k][d];
        int firstIdx = random.nextInt(n);
        System.arraycopy(data[firstIdx], 0, centroids[0], 0, d);

        double[] minDistances = new double[n];
        for (int i = 0; i < n; i++) {
            minDistances[i] = cosineDistance(data[i], centroids[0]);
        }

        for (int c = 1; c < k; c++) {
            double totalDist = 0.0;
            for (double dist : minDistances) {
                totalDist += dist * dist;
            }

            double r = random.nextDouble() * totalDist;
            double cumulative = 0.0;
            int chosenIdx = n - 1;
            for (int i = 0; i < n; i++) {
                cumulative += minDistances[i] * minDistances[i];
                if (cumulative >= r) {
                    chosenIdx = i;
                    break;
                }
            }

            System.arraycopy(data[chosenIdx], 0, centroids[c], 0, d);

            for (int i = 0; i < n; i++) {
                double dist = cosineDistance(data[i], centroids[c]);
                if (dist < minDistances[i]) {
                    minDistances[i] = dist;
                }
            }
        }

        // 2. Iterative optimization
        int[] assignments = new int[n];
        Arrays.fill(assignments, -1);
        int[] clusterCounts = new int[k];
        double[][] newCentroids = new double[k][d];

        for (int iter = 0; iter < maxIterations; iter++) {
            boolean changed = false;
            Arrays.fill(clusterCounts, 0);
            for (int c = 0; c < k; c++) {
                Arrays.fill(newCentroids[c], 0.0);
            }

            // Assign step
            for (int i = 0; i < n; i++) {
                int nearestCluster = 0;
                double nearestDist = Double.MAX_VALUE;
                for (int c = 0; c < k; c++) {
                    double dist = cosineDistance(data[i], centroids[c]);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestCluster = c;
                    }
                }

                if (assignments[i] != nearestCluster) {
                    assignments[i] = nearestCluster;
                    changed = true;
                }

                clusterCounts[nearestCluster]++;
                for (int j = 0; j < d; j++) {
                    newCentroids[nearestCluster][j] += data[i][j];
                }
            }

            // Update step
            for (int c = 0; c < k; c++) {
                if (clusterCounts[c] > 0) {
                    double normSq = 0.0;
                    for (int j = 0; j < d; j++) {
                        newCentroids[c][j] /= clusterCounts[c];
                        normSq += newCentroids[c][j] * newCentroids[c][j];
                    }
                    // Normalize centroid
                    if (normSq > 0) {
                        double norm = Math.sqrt(normSq);
                        for (int j = 0; j < d; j++) {
                            newCentroids[c][j] /= norm;
                        }
                    }
                    System.arraycopy(newCentroids[c], 0, centroids[c], 0, d);
                } else {
                    // Handle empty cluster: re-assign to a random data point
                    int randomPoint = random.nextInt(n);
                    System.arraycopy(data[randomPoint], 0, centroids[c], 0, d);
                }
            }

            if (!changed) {
                break;
            }
        }

        // 3. Compute WCSS / Inertia
        inertia = 0.0;
        for (int i = 0; i < n; i++) {
            double dist = cosineDistance(data[i], centroids[assignments[i]]);
            inertia += dist * dist;
        }

        return assignments;
    }

    /**
     * Predicts cluster ID for a new document vector.
     */
    public int predict(double[] vector) {
        if (centroids == null || centroids.length == 0) return 0;
        int nearest = 0;
        double minDist = Double.MAX_VALUE;
        for (int c = 0; c < centroids.length; c++) {
            double dist = cosineDistance(vector, centroids[c]);
            if (dist < minDist) {
                minDist = dist;
                nearest = c;
            }
        }
        return nearest;
    }

    /**
     * Predicts cluster ID and returns similarity score [0, 1].
     */
    public double[] predictWithSimilarity(double[] vector) {
        if (centroids == null || centroids.length == 0) return new double[]{0.0, 0.0};
        int nearest = 0;
        double maxSim = -1.0;
        for (int c = 0; c < centroids.length; c++) {
            double sim = cosineSimilarity(vector, centroids[c]);
            if (sim > maxSim) {
                maxSim = sim;
                nearest = c;
            }
        }
        return new double[]{nearest, Math.max(0.0, Math.min(1.0, maxSim))};
    }

    /**
     * Cosine distance = 1 - cosine_similarity.
     */
    public static double cosineDistance(double[] a, double[] b) {
        return 1.0 - cosineSimilarity(a, b);
    }

    /**
     * Computes dot product (for L2-normalized vectors this is cosine similarity).
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return 0.0;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
