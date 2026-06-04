package com.example.Arturo.controller;

import com.example.Arturo.model.SearchResult;
import com.example.Arturo.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for document search.
 * Thin API layer — all business logic lives in DocumentService.
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Searches the knowledge base for the most relevant document.
     * Currently uses a fixed query; swap to @RequestParam when ready for user input.
     *
     * @return JSON response with docId, score, and contentPreview
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResult> search() {
        String query = "ucm draft kibana error";

        SearchResult result = documentService.searchBestMatch(query);

        if (result == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(result);
    }
}