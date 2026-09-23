package com.dmml.library.ml.evaluation;

import java.util.*;

public class PurityCalculator {

    /**
     * Calculates the Purity metric comparing cluster labels against ground truth categories.
     * Range: [0, 1], higher is better.
     */
    public double computePurity(List<String> trueLabels, int[] clusterAssignments) {
        int n = trueLabels.size();
        if (n == 0 || n != clusterAssignments.length) return 0.0;

        // ClusterId -> (TrueLabel -> Count)
        Map<Integer, Map<String, Integer>> clusterClassCounts = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int cId = clusterAssignments[i];
            String tLabel = trueLabels.get(i);
            clusterClassCounts
                    .computeIfAbsent(cId, k -> new HashMap<>())
                    .merge(tLabel, 1, Integer::sum);
        }

        int maxTotal = 0;
        for (Map<String, Integer> classCounts : clusterClassCounts.values()) {
            int maxInCluster = 0;
            for (int count : classCounts.values()) {
                if (count > maxInCluster) {
                    maxInCluster = count;
                }
            }
            maxTotal += maxInCluster;
        }

        return (double) maxTotal / n;
    }
}
