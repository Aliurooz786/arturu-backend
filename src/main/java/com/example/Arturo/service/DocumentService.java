package com.example.Arturo.service;

import com.example.Arturo.loader.DocumentLoader;
import com.example.Arturo.model.SearchResult;
import com.example.Arturo.util.SimilarityUtil;
import com.example.Arturo.util.TextCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Core service for document search.
 * Loads documents, generates embeddings, computes similarity, and returns the best match.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    /** Maximum characters to embed per document (keeps embedding calls fast). */
    private static final int MAX_EMBED_LENGTH = 500;

    /** Maximum characters for the content preview in the response. */
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
     * Searches the knowledge base for the document most similar to the given query.
     *
     * @param query the search query text
     * @return SearchResult containing the best match, or null if no documents found
     */
    public SearchResult searchBestMatch(String query) {
        Map<String, String> documents = documentLoader.loadDocuments(knowledgeBasePath);

        if (documents.isEmpty()) {
            log.warn("No documents found in knowledge-base: {}", knowledgeBasePath);
            return null;
        }

        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        String bestDocId = null;
        String bestCleanedContent = null;
        double bestScore = -1;

        for (Map.Entry<String, String> entry : documents.entrySet()) {
            String docId = entry.getKey();
            String rawContent = entry.getValue();

            // Clean text before embedding to remove noise
            String cleaned = TextCleaner.clean(rawContent);
            String trimmed = truncate(cleaned, MAX_EMBED_LENGTH);

            List<Double> docEmbedding = embeddingService.generateEmbedding(trimmed);
            double score = SimilarityUtil.cosineSimilarity(queryEmbedding, docEmbedding);

            log.debug("{} → similarity: {}", docId, score);

            if (score > bestScore) {
                bestScore = score;
                bestDocId = docId;
                bestCleanedContent = cleaned;
            }
        }

        String preview = truncate(bestCleanedContent, MAX_PREVIEW_LENGTH);
        log.info("Best match: {} (score: {})", bestDocId, bestScore);

        return new SearchResult(bestDocId, bestScore, preview);
    }

    /**
     * Truncates text to the given maximum length.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.substring(0, Math.min(text.length(), maxLength));
    }
}