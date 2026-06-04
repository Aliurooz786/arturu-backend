package com.example.Arturo.service;

import com.example.Arturo.loader.DocumentLoader;
import com.example.Arturo.model.Chunk;
import com.example.Arturo.model.ScoredChunk;
import com.example.Arturo.model.SearchResult;
import com.example.Arturo.util.ChunkingUtil;
import com.example.Arturo.util.SimilarityUtil;
import com.example.Arturo.util.TextCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core search service implementing the full RAG pipeline with Top-K chunk retrieval.
 *
 * <p>Pipeline: Load → Light clean → Chunk → Filter → Full clean → Embed →
 * Similarity → Top-K selection → Context building → LLM → Answer</p>
 *
 * <p>Instead of selecting a single best chunk, this service retrieves the top K most
 * relevant chunks, combines them into a rich context block, and feeds that to the LLM
 * for higher quality answer generation.</p>
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    /** Maximum characters to send per chunk for embedding. */
    private static final int MAX_EMBED_LENGTH = 500;

    /** Number of top chunks to retrieve for context building. */
    private static final int TOP_K = 3;

    /** Minimum chunk length (chars) to be considered meaningful. */
    private static final int MIN_CHUNK_LENGTH = 40;

    /** Low-value chunk content to filter out (case-insensitive exact match after trimming). */
    private static final Set<String> NOISE_CHUNKS = Set.of(
            "tags", "references", "see also", "links"
    );

    private final DocumentLoader documentLoader;
    private final EmbeddingService embeddingService;
    private final LLMService llmService;

    @Value("${knowledge-base.path}")
    private String knowledgeBasePath;

    public DocumentService(DocumentLoader documentLoader,
                           EmbeddingService embeddingService,
                           LLMService llmService) {
        this.documentLoader = documentLoader;
        this.embeddingService = embeddingService;
        this.llmService = llmService;
    }

    // ──────────────────────────────────────────────
    //  CHUNK EXTRACTION
    // ──────────────────────────────────────────────

    /**
     * Loads all documents and splits them into fully cleaned, filtered chunks.
     *
     * <p>For each document:
     * <ol>
     *   <li>Light clean — remove HTML tags, preserve markdown structure</li>
     *   <li>Chunk — split by heading markers ({@code ##})</li>
     *   <li>Full clean — strip all remaining markdown from each chunk</li>
     *   <li>Filter — discard short or noise-only chunks</li>
     * </ol></p>
     *
     * @return list of meaningful chunks across all documents; empty list if none found
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

            // Step 3: Full clean each chunk, then filter out noise
            int accepted = 0;
            for (String rawChunk : rawChunks) {
                String cleaned = TextCleaner.clean(rawChunk);

                if (isValidChunk(cleaned)) {
                    allChunks.add(new Chunk(docId, cleaned));
                    accepted++;
                } else {
                    log.debug("Filtered out low-value chunk from '{}': \"{}\"",
                            docId, truncate(cleaned, 50));
                }
            }

            log.info("Document '{}': {} raw chunks → {} accepted", docId, rawChunks.size(), accepted);
        }

        log.info("Total meaningful chunks extracted: {}", allChunks.size());
        return allChunks;
    }

    // ──────────────────────────────────────────────
    //  TOP-K RETRIEVAL
    // ──────────────────────────────────────────────

    /**
     * Scores all chunks against the query and returns the top K most similar ones.
     *
     * <p>Each chunk is embedded and compared to the query embedding using cosine similarity.
     * Results are sorted by score descending, and the top K are returned.</p>
     *
     * @param query          the user's search query
     * @param queryEmbedding pre-computed query embedding vector
     * @param chunks         all available chunks
     * @return top K scored chunks sorted by descending similarity; may return fewer than K
     *         if not enough chunks exist
     */
    private List<ScoredChunk> getTopKChunks(String query, List<Double> queryEmbedding, List<Chunk> chunks) {
        List<ScoredChunk> scoredChunks = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            String trimmed = truncate(chunk.getContent(), MAX_EMBED_LENGTH);

            List<Double> chunkEmbedding = embeddingService.generateEmbedding(trimmed);
            double score = SimilarityUtil.cosineSimilarity(queryEmbedding, chunkEmbedding);

            log.debug("  [{}][chunk-{}] → similarity: {}", chunk.getDocId(), i, String.format("%.4f", score));

            scoredChunks.add(new ScoredChunk(chunk, score));
        }

        // Sort descending by score and take top K
        Collections.sort(scoredChunks);
        List<ScoredChunk> topK = scoredChunks.stream()
                .limit(TOP_K)
                .collect(Collectors.toList());

        log.info("Top-{} chunks selected:", topK.size());
        for (int i = 0; i < topK.size(); i++) {
            ScoredChunk sc = topK.get(i);
            log.info("  #{}: '{}' (score: {})", i + 1, sc.getChunk().getDocId(), String.format("%.4f", sc.getScore()));
        }

        return topK;
    }

    // ──────────────────────────────────────────────
    //  CONTEXT BUILDING
    // ──────────────────────────────────────────────

    /**
     * Combines the top-K chunks into a single context block for the LLM prompt.
     * Each chunk is separated by a blank line for readability.
     *
     * @param topChunks the top-K scored chunks
     * @return combined context string
     */
    private String buildContext(List<ScoredChunk> topChunks) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < topChunks.size(); i++) {
            if (i > 0) {
                context.append("\n\n");
            }
            context.append(topChunks.get(i).getChunk().getContent());
        }

        log.debug("Combined context built ({} chars from {} chunks)", context.length(), topChunks.size());
        return context.toString();
    }

    // ──────────────────────────────────────────────
    //  FULL RAG PIPELINE
    // ──────────────────────────────────────────────

    /**
     * Performs the full RAG pipeline: Top-K chunk retrieval → context building → LLM answer.
     *
     * <p>Flow:
     * <ol>
     *   <li>Extract all chunks via {@link #getAllChunks()}</li>
     *   <li>Generate query embedding</li>
     *   <li>Retrieve top-K chunks via {@link #getTopKChunks}</li>
     *   <li>Build combined context via {@link #buildContext}</li>
     *   <li>Generate answer via {@link LLMService#generateAnswer}</li>
     * </ol></p>
     *
     * @param query the user's search query
     * @return {@link SearchResult} with docId (from top chunk), score, and LLM answer;
     *         or {@code null} if no chunks exist
     */
    public SearchResult searchBestChunk(String query) {
        // Step 1: Extract all cleaned, filtered chunks
        List<Chunk> chunks = getAllChunks();

        if (chunks.isEmpty()) {
            log.warn("No chunks available for search");
            return null;
        }

        // Step 2: Generate query embedding
        log.info("Starting Top-K search for query: \"{}\"", query);
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        log.debug("Query embedding generated (dimension: {})", queryEmbedding.size());

        // Step 3: Retrieve top-K most relevant chunks
        List<ScoredChunk> topChunks = getTopKChunks(query, queryEmbedding, chunks);

        if (topChunks.isEmpty()) {
            log.warn("No scored chunks available after ranking");
            return null;
        }

        // Step 4: Build combined context from top-K chunks
        String combinedContext = buildContext(topChunks);

        // Step 5: Generate LLM answer using the combined context
        String answer = llmService.generateAnswer(query, combinedContext);

        // Use top-1 chunk's docId and score for the response metadata
        ScoredChunk topChunk = topChunks.get(0);
        return new SearchResult(topChunk.getChunk().getDocId(), topChunk.getScore(), answer);
    }

    // ──────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────

    /**
     * Checks whether a chunk contains meaningful content worth embedding.
     * Filters out chunks that are too short or consist of noise words like "Tags".
     *
     * @param content the cleaned chunk text
     * @return true if the chunk should be kept; false if it should be discarded
     */
    private boolean isValidChunk(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        if (content.length() < MIN_CHUNK_LENGTH) {
            return false;
        }
        // Check if the entire chunk is a known noise label
        return !NOISE_CHUNKS.contains(content.trim().toLowerCase());
    }

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