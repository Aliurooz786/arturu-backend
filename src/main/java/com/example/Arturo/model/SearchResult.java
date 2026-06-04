package com.example.Arturo.model;

/**
 * Response DTO for the /search endpoint.
 * Contains the matched document ID, similarity score, and a cleaned content preview.
 */
public class SearchResult {

    private String docId;
    private double score;
    private String contentPreview;

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
