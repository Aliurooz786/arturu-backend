package com.example.Arturo.model;

/**
 * Represents a single chunk of text extracted from a document.
 *
 * <p>Each document is split into multiple chunks (typically by heading sections).
 * This class ties each chunk back to its source document via {@code docId}
 * and holds the fully cleaned text content ready for embedding.</p>
 */
public class Chunk {

    private final String docId;
    private final String content;

    /**
     * Constructs a chunk.
     *
     * @param docId   identifier of the source document (filename without extension)
     * @param content fully cleaned chunk text, ready for embedding or display
     */
    public Chunk(String docId, String content) {
        this.docId = docId;
        this.content = content;
    }

    public String getDocId() {
        return docId;
    }

    public String getContent() {
        return content;
    }
}
