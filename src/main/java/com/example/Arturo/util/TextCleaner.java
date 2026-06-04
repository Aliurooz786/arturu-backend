package com.example.Arturo.util;

/**
 * Utility for cleaning raw document text before embedding or display.
 * Strips HTML tags, markdown syntax, HTML entities, and normalizes whitespace.
 */
public class TextCleaner {

    /**
     * Cleans the given text by removing HTML, markdown noise, and normalizing whitespace.
     */
    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Remove HTML tags
        text = text.replaceAll("<[^>]+>", "");

        // Remove HTML entities (e.g. &amp; &lt;)
        text = text.replaceAll("&[^;]+;", "");

        // Remove markdown headings (e.g. ## Heading)
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");

        // Remove markdown bold/italic markers
        text = text.replaceAll("\\*{1,3}", "");
        text = text.replaceAll("_{1,3}", " ");

        // Remove markdown code fences
        text = text.replaceAll("```[\\s\\S]*?```", "");
        text = text.replaceAll("`", "");

        // Remove markdown blockquotes
        text = text.replaceAll("(?m)^>\\s?", "");

        // Remove markdown list markers (-, *, numbered)
        text = text.replaceAll("(?m)^\\s*[-*]\\s+", "");
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");

        // Normalize whitespace
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }
}
