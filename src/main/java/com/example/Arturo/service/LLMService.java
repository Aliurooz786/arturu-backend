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
import java.util.Map;

/**
 * Service for generating natural language answers using the Ollama LLM API.
 *
 * <p>Takes a user query and retrieved context (chunk content) and constructs a prompt
 * that the LLM uses to generate a concise, context-aware answer.</p>
 *
 * <p>Uses the {@code /api/generate} endpoint (not the chat endpoint) with
 * {@code stream: false} for a single complete response.</p>
 */
@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${ollama.llm.url}")
    private String llmUrl;

    @Value("${ollama.llm.model}")
    private String llmModel;

    /**
     * Generates a natural language answer using the LLM, grounded in the provided context.
     *
     * @param query   the user's original question
     * @param context combined text from the top-K retrieved chunks
     * @return LLM-generated answer text; falls back to raw context if LLM fails
     */
    public String generateAnswer(String query, String context) {
        String prompt = buildPrompt(query, context);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", llmModel,
                    "prompt", prompt,
                    "stream", false
            );

            String json = mapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(llmUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            log.info("Sending prompt to LLM (model: {}, prompt length: {} chars)", llmModel, prompt.length());

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            Map<?, ?> result = mapper.readValue(response.body(), Map.class);
            String answer = (String) result.get("response");

            if (answer == null || answer.isBlank()) {
                log.warn("LLM returned empty response, falling back to raw context");
                return context;
            }

            log.info("LLM answer generated ({} chars)", answer.length());
            return answer.trim();

        } catch (Exception e) {
            log.error("LLM generation failed for query: \"{}\". Falling back to raw context.", query, e);
            return context;
        }
    }

    /**
     * Builds a structured prompt combining the user's query with the retrieved multi-chunk context.
     * Instructs the LLM to answer only from the provided context in a step-by-step format.
     *
     * @param query   the user's question
     * @param context combined content from the top-K relevant chunks
     * @return formatted prompt string for the LLM
     */
    private String buildPrompt(String query, String context) {
        return """
                You are a helpful assistant for debugging backend systems.

                Use ONLY the context below to answer the question.

                Context:
                %s

                Question:
                %s

                Answer in a clear, step-by-step way. Be concise but informative.\
                """.formatted(context, query);
    }
}
