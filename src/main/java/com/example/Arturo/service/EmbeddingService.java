package com.example.Arturo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Service for generating vector embeddings via the Ollama API.
 *
 * <p>Calls the configured Ollama endpoint with the {@code nomic-embed-text} model
 * (or whichever model is set in {@code application.properties}) and returns
 * the resulting embedding vector as a list of doubles.</p>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    /** Reusable JSON mapper — thread-safe, no need to recreate per call. */
    private final ObjectMapper mapper = new ObjectMapper();

    /** Reusable HTTP client — thread-safe, connection-pooled internally. */
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    /**
     * Generates a vector embedding for the given text by calling the Ollama API.
     *
     * @param text the input text to embed (should be pre-cleaned)
     * @return embedding vector as a list of doubles
     * @throws RuntimeException if the API call fails or the response cannot be parsed
     */
    public List<Double> generateEmbedding(String text) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", ollamaModel,
                    "prompt", text
            );

            String json = mapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            Map<?, ?> result = mapper.readValue(response.body(), Map.class);

            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) result.get("embedding");

            log.debug("Embedding generated (dimension: {}) for text: \"{}...\"",
                    embedding.size(), text.substring(0, Math.min(40, text.length())));

            return embedding;

        } catch (Exception e) {
            log.error("Embedding generation failed for text: \"{}...\"",
                    text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
}