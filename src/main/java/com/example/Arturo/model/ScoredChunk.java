package com.example.Arturo.model;

/**
 * A {@link Chunk} paired with its cosine similarity score against a query.
 *
 * <p>Used internally during Top-K retrieval to sort and select the most relevant chunks.
 * Implements {@link Comparable} so a list of scored chunks can be sorted by score descending.</p>
 */
public class ScoredChunk implements Comparable<ScoredChunk> {

    private final Chunk chunk;
    private final double score;

    /**
     * Constructs a scored chunk.
     *
     * @param chunk the original chunk with docId and content
     * @param score cosine similarity score against the query (0.0 to 1.0)
     */
    public ScoredChunk(Chunk chunk, double score) {
        this.chunk = chunk;
        this.score = score;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public double getScore() {
        return score;
    }

    /**
     * Sorts in descending order by score (highest similarity first).
     */
    @Override
    public int compareTo(ScoredChunk other) {
        return Double.compare(other.score, this.score);
    }
}
