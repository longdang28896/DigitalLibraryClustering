package com.dmml.library.ml.clustering;

public interface ClusterAlgorithm {
    String getName();
    int[] fitPredict(double[][] data, int k);
}
