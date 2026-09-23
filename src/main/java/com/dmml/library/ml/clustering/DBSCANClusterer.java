package com.dmml.library.ml.clustering;

import java.util.*;

public class DBSCANClusterer implements ClusterAlgorithm {

    private final double eps;
    private final int minPts;

    public DBSCANClusterer() {
        this(0.72, 4); // Default cosine distance threshold
    }

    public DBSCANClusterer(double eps, int minPts) {
        this.eps = eps;
        this.minPts = minPts;
    }

    @Override
    public String getName() {
        return "DBSCAN (Density-Based)";
    }

    @Override
    public int[] fitPredict(double[][] data, int k) {
        int n = data.length;
        int[] labels = new int[n];
        Arrays.fill(labels, -1); // -1 = noise or unvisited
        boolean[] visited = new boolean[n];

        int clusterId = 0;

        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;
            visited[i] = true;

            List<Integer> neighbors = getNeighbors(data, i);
            if (neighbors.size() < minPts) {
                labels[i] = -1; // Noise
            } else {
                labels[i] = clusterId;
                expandCluster(data, i, neighbors, clusterId, labels, visited);
                clusterId++;
            }
        }

        return labels;
    }

    private void expandCluster(double[][] data, int pointIdx, List<Integer> neighbors, int clusterId, int[] labels, boolean[] visited) {
        Queue<Integer> seeds = new LinkedList<>(neighbors);

        while (!seeds.isEmpty()) {
            int current = seeds.poll();
            if (!visited[current]) {
                visited[current] = true;
                List<Integer> currentNeighbors = getNeighbors(data, current);
                if (currentNeighbors.size() >= minPts) {
                    seeds.addAll(currentNeighbors);
                }
            }
            if (labels[current] == -1) {
                labels[current] = clusterId;
            }
        }
    }

    private List<Integer> getNeighbors(double[][] data, int pointIdx) {
        List<Integer> neighbors = new ArrayList<>();
        double[] target = data[pointIdx];
        for (int i = 0; i < data.length; i++) {
            double dist = KMeansClusterer.cosineDistance(target, data[i]);
            if (dist <= eps) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }
}
