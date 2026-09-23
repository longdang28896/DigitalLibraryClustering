package com.dmml.library.ml.clustering;

import java.util.*;

public class HierarchicalClusterer implements ClusterAlgorithm {

    @Override
    public String getName() {
        return "Hierarchical (Agglomerative - Average Linkage)";
    }

    @Override
    public int[] fitPredict(double[][] data, int k) {
        int n = data.length;
        if (n == 0 || k <= 0) return new int[0];
        if (k >= n) {
            int[] res = new int[n];
            for (int i = 0; i < n; i++) res[i] = i;
            return res;
        }

        // Initialize clusters: each data point is its own cluster
        List<List<Integer>> currentClusters = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Integer> single = new ArrayList<>();
            single.add(i);
            currentClusters.add(single);
        }

        // Precompute pair-wise cosine distance matrix between points
        float[][] distMatrix = new float[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float d = (float) KMeansClusterer.cosineDistance(data[i], data[j]);
                distMatrix[i][j] = d;
                distMatrix[j][i] = d;
            }
        }

        // Iteratively merge closest clusters
        while (currentClusters.size() > k) {
            int bestA = -1, bestB = -1;
            float minDist = Float.MAX_VALUE;

            int numClusters = currentClusters.size();
            for (int a = 0; a < numClusters; a++) {
                List<Integer> clusterA = currentClusters.get(a);
                for (int b = a + 1; b < numClusters; b++) {
                    List<Integer> clusterB = currentClusters.get(b);

                    // Average Linkage distance
                    float sumDist = 0.0f;
                    for (int idxA : clusterA) {
                        for (int idxB : clusterB) {
                            sumDist += distMatrix[idxA][idxB];
                        }
                    }
                    float avgDist = sumDist / (clusterA.size() * clusterB.size());

                    if (avgDist < minDist) {
                        minDist = avgDist;
                        bestA = a;
                        bestB = b;
                    }
                }
            }

            // Merge bestB into bestA, and remove bestB
            currentClusters.get(bestA).addAll(currentClusters.get(bestB));
            currentClusters.remove(bestB);
        }

        // Construct labels array
        int[] labels = new int[n];
        for (int c = 0; c < currentClusters.size(); c++) {
            for (int docIdx : currentClusters.get(c)) {
                labels[docIdx] = c;
            }
        }

        return labels;
    }
}
