package com.dmml.library.ml.evaluation;

import com.dmml.library.ml.clustering.KMeansClusterer;

import java.util.*;

public class DaviesBouldinCalculator {

    /**
     * Calculates the Davies-Bouldin Index.
     * Lower value indicates better clustering (more compact and well-separated).
     */
    public double computeScore(double[][] data, int[] labels) {
        int n = data.length;
        if (n == 0) return 0.0;

        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int lbl = labels[i];
            if (lbl >= 0) {
                clusters.computeIfAbsent(lbl, k -> new ArrayList<>()).add(i);
            }
        }

        int k = clusters.size();
        if (k <= 1) return 0.0;

        int d = data[0].length;
        List<Integer> clusterIds = new ArrayList<>(clusters.keySet());

        // Compute centroids and average within-cluster dispersions S_i
        double[][] centroids = new double[k][d];
        double[] s = new double[k];

        for (int idx = 0; idx < k; idx++) {
            int cId = clusterIds.get(idx);
            List<Integer> members = clusters.get(cId);

            // Centroid
            for (int memberIdx : members) {
                for (int j = 0; j < d; j++) {
                    centroids[idx][j] += data[memberIdx][j];
                }
            }
            double normSq = 0.0;
            for (int j = 0; j < d; j++) {
                centroids[idx][j] /= members.size();
                normSq += centroids[idx][j] * centroids[idx][j];
            }
            if (normSq > 0) {
                double norm = Math.sqrt(normSq);
                for (int j = 0; j < d; j++) centroids[idx][j] /= norm;
            }

            // S_i: average distance to centroid
            double sumDist = 0.0;
            for (int memberIdx : members) {
                sumDist += KMeansClusterer.cosineDistance(data[memberIdx], centroids[idx]);
            }
            s[idx] = sumDist / members.size();
        }

        // Compute DB index
        double dbSum = 0.0;
        for (int i = 0; i < k; i++) {
            double maxR = 0.0;
            for (int j = 0; j < k; j++) {
                if (i != j) {
                    double m_ij = KMeansClusterer.cosineDistance(centroids[i], centroids[j]);
                    if (m_ij > 1e-6) {
                        double r_ij = (s[i] + s[j]) / m_ij;
                        if (r_ij > maxR) {
                            maxR = r_ij;
                        }
                    }
                }
            }
            dbSum += maxR;
        }

        return dbSum / k;
    }
}
