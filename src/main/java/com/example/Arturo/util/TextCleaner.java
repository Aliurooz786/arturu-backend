package com.example.Arturo.util;

/**
 * Utility for cleaning raw document text at two stages of the pipeline:
 * <ol>
 *   <li>{@link #removeHtml(String)} — Light cleaning before chunking (preserves markdown structure)</li>
 *   <li>{@link #clean(String)} — Full cleaning after chunking (removes all markdown/formatting noise)</li>
 * </ol>
 *
 * Pipeline order: Document → removeHtml → Chunking → clean → Embedding
 */
public class TextCleaner {

    private TextCleaner() {
        // Static utility — prevent instantiation
    }

    /**
     * Light cleaning pass: removes only HTML tags while preserving markdown structure.
     * This should be called BEFORE chunking, so that heading markers (##) remain intact for splitting.
     *
     * @param text raw document text
     * @return text with HTML tags stripped; markdown structure preserved
     */
    public static String removeHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "");
    }

    /**
     * Full cleaning pass: removes all markdown syntax, HTML entities, and normalizes whitespace.
     * This should be called AFTER chunking, on individual chunks before embedding or display.
     *
     * @param text a chunk of text with markdown formatting
     * @return plain text with all formatting noise removed
     */
    public static String clean(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Remove HTML entities (e.g. &amp; &lt; &nbsp;)
        text = text.replaceAll("&[^;]+;", "");

        // Remove markdown headings (# Heading)
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");

        // Remove markdown bold/italic markers (* and _)
        text = text.replaceAll("\\*{1,3}", "");
        text = text.replaceAll("_{1,3}", " ");

        // Remove fenced code blocks and inline code
        text = text.replaceAll("```[\\s\\S]*?```", "");
        text = text.replaceAll("`", "");

        // Remove blockquote markers
        text = text.replaceAll("(?m)^>\\s?", "");

        // Remove unordered list markers (- or *)
        text = text.replaceAll("(?m)^\\s*[-*]\\s+", "");

        // Remove ordered list markers (1. 2. etc.)
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");

        // Collapse multiple newlines into one for readability
        text = text.replaceAll("\\n+", "\n");

        return text.trim();
    }
}
