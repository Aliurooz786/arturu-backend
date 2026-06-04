package com.example.Arturo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits document text into meaningful chunks based on markdown heading structure.
 * Chunking is performed after light cleaning (HTML removal) but before full text cleaning,
 * so that heading delimiters (##) are still present for splitting.
 */
public class ChunkingUtil {

    /**
     * Splits a document into chunks using markdown heading markers (##) as delimiters.
     * Each chunk represents a logical section of the document.
     *
     * @param document the lightly-cleaned document text (HTML removed, structure preserved)
     * @return list of non-empty text chunks; empty list if input is null or blank
     */
    public static List<String> splitIntoChunks(String document) {
        List<String> chunks = new ArrayList<>();

        if (document == null || document.isBlank()) {
            return chunks;
        }

        // Split on markdown heading markers to create section-level chunks
        String[] sections = document.split("##");

        for (String section : sections) {
            String trimmed = section.trim();
            if (!trimmed.isEmpty()) {
                chunks.add(trimmed);
            }
        }

        return chunks;
    }
}
