package com.example.Arturo.service;

import com.example.Arturo.loader.DocumentLoader;
import com.example.Arturo.model.SearchResult;
import com.example.Arturo.util.ChunkingUtil;
import com.example.Arturo.util.SimilarityUtil;
import com.example.Arturo.util.TextCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Core search service implementing the document retrieval pipeline.
 *
 * <p>Pipeline: Load documents → Light clean → Chunk → Full clean → Embed → Similarity → Best match</p>
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

    /**
     * Searches the knowledge base for the chunk most similar to the given query.
     *
     * <p>For each document: performs light cleaning → chunking → full cleaning per chunk →
     * embedding → cosine similarity comparison. Returns the single best-matching chunk.</p>
     *
     * @param query the user's search query
     * @return {@link SearchResult} with the best match, or {@code null} if no documents are found
     */
    public SearchResult searchBestMatch(String query) {
        // Step 1: Load all documents from the knowledge base
        Map<String, String> documents = documentLoader.loadDocuments(knowledgeBasePath);

        if (documents.isEmpty()) {
            log.warn("No documents found in knowledge-base: {}", knowledgeBasePath);
            return null;
        }

        log.info("Loaded {} documents. Starting search for query: \"{}\"", documents.size(), query);

        // Step 2: Generate the query embedding once (reused against all chunks)
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        log.debug("Query embedding generated (dimension: {})", queryEmbedding.size());

        String bestDocId = null;
        String bestChunkPreview = null;
        double bestScore = -1;

        // Step 3: Process each document through the pipeline
        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String rawContent = entry.getValue();

            // Light clean: remove HTML but preserve markdown structure for chunking
            String lightlyCleaned = TextCleaner.removeHtml(rawContent);

            // Chunk: split into sections by heading markers
            List<String> chunks = ChunkingUtil.splitIntoChunks(lightlyCleaned);
            log.debug("Document '{}': split into {} chunks", docId, chunks.size());

            // Score each chunk independently
            for (int i = 0; i < chunks.size(); i++) {
                // Full clean: strip all markdown formatting from this chunk
                String cleaned = TextCleaner.clean(chunks.get(i));
                String trimmed = truncate(cleaned, MAX_EMBED_LENGTH);

                // Generate embedding and compute similarity
                List<Double> chunkEmbedding = embeddingService.generateEmbedding(trimmed);
                double score = SimilarityUtil.cosineSimilarity(queryEmbedding, chunkEmbedding);

                log.debug("  {}[chunk-{}] → similarity: {}", docId, i, String.format("%.4f", score));

                if (score > bestScore) {
                    bestScore = score;
                    bestDocId = docId;
                    bestChunkPreview = cleaned;
                }
            }
        }

        String preview = truncate(bestChunkPreview, MAX_PREVIEW_LENGTH);
        log.info("Best match: '{}' (score: {})", bestDocId, String.format("%.4f", bestScore));

        return new SearchResult(bestDocId, bestScore, preview);
    }

    /**
     * Loads all documents from the knowledge base.
     * Primarily used by controller endpoints that need raw document access.
     *
     * @return map of document ID (filename without extension) → raw content
     */
    public Map<String, String> getDocuments() {
        return documentLoader.loadDocuments(knowledgeBasePath);
    }

    /**
     * Truncates text to the specified maximum length without breaking mid-word.
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