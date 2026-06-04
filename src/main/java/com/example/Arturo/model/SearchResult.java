package com.example.Arturo.model;

/**
 * Response DTO for the {@code GET /search} endpoint.
 * Encapsulates the search result with the best-matching document's ID,
 * its cosine similarity score, and a cleaned content preview.
 */
public class SearchResult {

    private final String docId;
    private final double score;
    private final String contentPreview;

    /**
     * Constructs a search result.
     *
     * @param docId          identifier of the matched document (derived from filename)
     * @param score          cosine similarity score (0.0 to 1.0)
     * @param contentPreview cleaned and truncated text preview of the matched document
     */
    public SearchResult(String docId, double score, String contentPreview) {
        this.docId = docId;
        this.score = score;
        this.contentPreview = contentPreview;
    }

    public String getDocId() {
        return docId;
    }

    public double getScore() {
        return score;
    }

    public String getContentPreview() {
        return contentPreview;
    }
}
