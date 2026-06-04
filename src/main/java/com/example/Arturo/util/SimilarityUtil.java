package com.example.Arturo.util;

import java.util.List;

/**
 * Utility for computing cosine similarity between two embedding vectors.
 * Used to rank document relevance against a query embedding.
 */
public class SimilarityUtil {

    private SimilarityUtil() {
        // Static utility — prevent instantiation
    }

    /**
     * Computes the cosine similarity between two vectors.
     * Returns a value between -1 (opposite) and 1 (identical), where higher means more similar.
     *
     * @param vectorA first embedding vector
     * @param vectorB second embedding vector
     * @return cosine similarity score
     * @throws IllegalArgumentException if vectors are null or have different sizes
     */
    public static double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
            throw new IllegalArgumentException("Vectors must be non-null and of equal size");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += vectorA.get(i) * vectorA.get(i);
            normB += vectorB.get(i) * vectorB.get(i);
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }

        return dotProduct / denominator;
    }
}