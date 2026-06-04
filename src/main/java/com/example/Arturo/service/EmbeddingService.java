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
 * Calls the Ollama embedding API to generate vector embeddings for text.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${ollama.api.url}")
    private String ollamaUrl;

    @Value("${ollama.model}")
    private String ollamaModel;

    /**
     * Generates a vector embedding for the given text using the Ollama API.
     *
     * @param text the input text to embed
     * @return list of doubles representing the embedding vector
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
            return embedding;

        } catch (Exception e) {
            log.error("Embedding generation failed for text: {}...", text.substring(0, Math.min(50, text.length())), e);
            throw new RuntimeException("Embedding generation failed", e);
        }
    }
}