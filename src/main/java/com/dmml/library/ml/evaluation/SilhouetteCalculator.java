package com.dmml.library.ml.evaluation;

import com.dmml.library.ml.clustering.KMeansClusterer;

import java.util.*;

public class SilhouetteCalculator {

    /**
     * Calculates the mean Silhouette Coefficient across all clustered data points.
     * Range: [-1, 1], higher is better.
     */
    public double computeScore(double[][] data, int[] labels) {
        int n = data.length;
        if (n <= 1) return 0.0;

        // Group indices by cluster ID (ignore noise cluster -1)
        Map<Integer, List<Integer>> clusters = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int lbl = labels[i];
            if (lbl >= 0) {
                clusters.computeIfAbsent(lbl, k -> new ArrayList<>()).add(i);
            }
        }

        if (clusters.size() <= 1) return 0.0;

        // Precompute pair-wise distances on a representative sample if dataset is large
        // To maintain fast interactive performance in web UI
        int step = (n > 500) ? (n / 400) : 1;

        double totalSilhouette = 0.0;
        int sampleCount = 0;

        for (int i = 0; i < n; i += step) {
            int ownCluster = labels[i];
            if (ownCluster < 0) continue;

            List<Integer> sameClusterMembers = clusters.get(ownCluster);
            if (sameClusterMembers.size() <= 1) {
                sampleCount++;
                continue; // silhouette is 0 for singleton
            }

            // a(i): average distance within same cluster
            double a = 0.0;
            for (int memberIdx : sameClusterMembers) {
                if (memberIdx != i) {
                    a += KMeansClusterer.cosineDistance(data[i], data[memberIdx]);
                }
            }
            a /= (sameClusterMembers.size() - 1);

            // b(i): min average distance to any other cluster
            double b = Double.MAX_VALUE;
            for (Map.Entry<Integer, List<Integer>> entry : clusters.entrySet()) {
                if (entry.getKey() == ownCluster) continue;

                List<Integer> otherClusterMembers = entry.getValue();
                if (otherClusterMembers.isEmpty()) continue;

                double distToOther = 0.0;
                for (int memberIdx : otherClusterMembers) {
                    distToOther += KMeansClusterer.cosineDistance(data[i], data[memberIdx]);
                }
                distToOther /= otherClusterMembers.size();

                if (distToOther < b) {
                    b = distToOther;
                }
            }

            double maxVal = Math.max(a, b);
            double s = (maxVal > 0) ? ((b - a) / maxVal) : 0.0;
            totalSilhouette += s;
            sampleCount++;
        }

        return (sampleCount > 0) ? (totalSilhouette / sampleCount) : 0.0;
    }
}
