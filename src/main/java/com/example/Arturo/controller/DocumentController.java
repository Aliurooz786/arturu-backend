package com.example.Arturo.controller;

import com.example.Arturo.model.SearchResult;
import com.example.Arturo.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the document search API.
 *
 * <p>Thin API layer — all business logic is delegated to {@link DocumentService}.
 * This controller handles only HTTP request/response mapping.</p>
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Searches the knowledge base and returns the most relevant chunk.
     *
     * <p>Performs chunk-level semantic search: each document is split into sections,
     * and the query is compared against every chunk individually for precise matching.</p>
     *
     * <p>Currently uses a fixed query for MVP. To accept user input, add
     * {@code @RequestParam String query} as a parameter — a one-line change.</p>
     *
     * @return JSON response with {@code docId}, {@code score}, and {@code contentPreview},
     *         or HTTP 204 No Content if no chunks are found
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResult> search() {
        String query = "ucm draft kibana error";

        SearchResult result = documentService.searchBestChunk(query);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }
}
