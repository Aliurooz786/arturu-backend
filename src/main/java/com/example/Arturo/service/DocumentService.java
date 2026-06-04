package com.example.Arturo.service;

import com.example.Arturo.loader.DocumentLoader;
import com.example.Arturo.model.Chunk;
import com.example.Arturo.model.SearchResult;
import com.example.Arturo.util.ChunkingUtil;
import com.example.Arturo.util.SimilarityUtil;
import com.example.Arturo.util.TextCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core search service implementing chunk-level semantic retrieval.
 *
 * <p>Pipeline: Load documents → Light clean → Chunk → Full clean → Embed → Similarity → Best chunk</p>
 *
 * <p>Unlike document-level search, this service splits every document into logical chunks
 * (by heading sections) and compares the query against each chunk independently.
 * This produces more precise matches because embeddings are generated on focused,
 * topic-specific text rather than entire documents.</p>
 *
 * <p>Orchestrates {@link DocumentLoader} for file access, {@link TextCleaner} for cleaning,
 * {@link ChunkingUtil} for splitting, {@link EmbeddingService} for vector generation,
 * and {@link SimilarityUtil} for scoring.</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    /** Maximum characters to send per chunk for embedding (keeps API calls efficient). */
    private static final int MAX_EMBED_LENGTH = 500;

    /** Maximum characters for the content preview in the search response. */
    private static final int MAX_PREVIEW_LENGTH = 500;

    private final DocumentLoader documentLoader;
    private final EmbeddingService embeddingService;

    @Value("${knowledge-base.path}")
    private String knowledgeBasePath;

    public DocumentService(DocumentLoader documentLoader, EmbeddingService embeddingService) {
        this.documentLoader = documentLoader;
        this.embeddingService = embeddingService;
    }

    // ──────────────────────────────────────────────
    //  CHUNK EXTRACTION
    // ──────────────────────────────────────────────

    /**
     * Loads all documents and splits them into fully cleaned chunks.
     *
     * <p>For each document:
     * <ol>
     *   <li>Light clean — remove HTML tags, preserve markdown structure</li>
     *   <li>Chunk — split by heading markers ({@code ##})</li>
     *   <li>Full clean — strip all remaining markdown from each chunk</li>
     * </ol>
     * Each returned {@link Chunk} carries its source {@code docId} and cleaned text content.</p>
     *
     * @return list of all chunks across all documents; empty list if no documents found
     */
    public List<Chunk> getAllChunks() {
        Map<String, String> documents = documentLoader.loadDocuments(knowledgeBasePath);

        if (documents.isEmpty()) {
            log.warn("No documents found in knowledge-base: {}", knowledgeBasePath);
            return List.of();
        }

        List<Chunk> allChunks = new ArrayList<>();

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String rawContent = entry.getValue();

            // Step 1: Light clean — remove HTML but keep markdown headings for chunking
            String lightlyCleaned = TextCleaner.removeHtml(rawContent);

            // Step 2: Split into sections by heading markers
            List<String> rawChunks = ChunkingUtil.splitIntoChunks(lightlyCleaned);

            // Step 3: Full clean each chunk and wrap in Chunk model
            for (String rawChunk : rawChunks) {
                String cleaned = TextCleaner.clean(rawChunk);
                if (!cleaned.isBlank()) {
                    allChunks.add(new Chunk(docId, cleaned));
                }
            }

            log.info("Document '{}': extracted {} chunks", docId, rawChunks.size());
        }

        log.info("Total chunks extracted across all documents: {}", allChunks.size());
        return allChunks;
    }

    // ──────────────────────────────────────────────
    //  CHUNK-LEVEL SEARCH
    // ──────────────────────────────────────────────

    /**
     * Searches all chunks in the knowledge base and returns the one most similar to the query.
     *
     * <p>Flow:
     * <ol>
     *   <li>Extract all chunks via {@link #getAllChunks()}</li>
     *   <li>Generate query embedding once</li>
     *   <li>For each chunk: truncate → embed → compute cosine similarity</li>
     *   <li>Track and return the highest-scoring chunk</li>
     * </ol></p>
     *
     * @param query the user's search query
     * @return {@link SearchResult} with the best-matching chunk, or {@code null} if no chunks exist
     */
    public SearchResult searchBestChunk(String query) {
        // Step 1: Extract all cleaned chunks from all documents
        List<Chunk> chunks = getAllChunks();

        if (chunks.isEmpty()) {
            log.warn("No chunks available for search");
            return null;
        }

        // Step 2: Generate query embedding (reused against every chunk)
        log.info("Starting chunk search for query: \"{}\"", query);
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        log.debug("Query embedding generated (dimension: {})", queryEmbedding.size());

        // Step 3: Compare query against each chunk and track the best match
        Chunk bestChunk = null;
        double bestScore = -1;

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            String trimmed = truncate(chunk.getContent(), MAX_EMBED_LENGTH);

            List<Double> chunkEmbedding = embeddingService.generateEmbedding(trimmed);
            double score = SimilarityUtil.cosineSimilarity(queryEmbedding, chunkEmbedding);

            log.debug("  [{}][chunk-{}] → similarity: {}", chunk.getDocId(), i, String.format("%.4f", score));

            if (score > bestScore) {
                bestScore = score;
                bestChunk = chunk;
            }
        }

        // Step 4: Build and return the result
        String preview = truncate(bestChunk.getContent(), MAX_PREVIEW_LENGTH);
        log.info("Best chunk match: '{}' (score: {})", bestChunk.getDocId(), String.format("%.4f", bestScore));

        return new SearchResult(bestChunk.getDocId(), bestScore, preview);
    }

    // ──────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────

    /**
     * Truncates text to the specified maximum length.
     *
     * @param text      the text to truncate
     * @param maxLength maximum allowed length
     * @return truncated text, or empty string if input is null
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength);
    }
}