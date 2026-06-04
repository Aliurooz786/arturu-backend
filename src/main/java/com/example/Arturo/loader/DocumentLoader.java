package com.example.Arturo.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads markdown documents from the knowledge-base folder.
 * Returns a map of filename → raw content for downstream processing.
 */
@Component
public class DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    /**
     * Reads all .md files from the given folder and returns them as a map.
     *
     * @param folderPath path to the knowledge-base directory
     * @return map of filename (without extension) → file content
     */
    public Map<String, String> loadDocuments(String folderPath) {
        Map<String, String> documents = new LinkedHashMap<>();

        try {
            Files.walk(Paths.get(folderPath))
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            String filename = path.getFileName().toString().replace(".md", "");
                            documents.put(filename, content);
                            log.info("Loaded document: {}", filename);
                        } catch (IOException e) {
                            log.error("Failed to read file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to access knowledge-base folder: {}", folderPath, e);
        }

        return documents;
    }
}
