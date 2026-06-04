package com.example.Arturo.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads markdown documents from the knowledge-base folder on disk.
 *
 * <p>Responsible only for file system access. Returns raw file content as-is;
 * all text cleaning and processing is handled downstream by service/util layers.</p>
 */
@Component
public class DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(DocumentLoader.class);

    /**
     * Reads all {@code .md} files from the given folder and returns them as a map.
     *
     * @param folderPath path to the knowledge-base directory (relative or absolute)
     * @return ordered map of document ID (filename without {@code .md}) → raw file content;
     *         empty map if the folder is missing or contains no markdown files
     */
    public Map<String, String> loadDocuments(String folderPath) {
        Map<String, String> documents = new LinkedHashMap<>();

        if (folderPath == null || folderPath.isBlank()) {
            log.error("Knowledge-base folder path is null or empty");
            return documents;
        }

        Path rootPath = Paths.get(folderPath);

        if (!Files.exists(rootPath)) {
            log.error("Knowledge-base folder does not exist: {}", folderPath);
            return documents;
        }

        try {
            Files.walk(rootPath)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".md"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path).trim();
                            String docId = path.getFileName().toString().replace(".md", "");
                            documents.put(docId, content);
                            log.info("Loaded document: {}", docId);
                        } catch (IOException e) {
                            log.error("Failed to read file: {}", path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk knowledge-base folder: {}", folderPath, e);
        }

        log.info("Total documents loaded: {}", documents.size());
        return documents;
    }
}
