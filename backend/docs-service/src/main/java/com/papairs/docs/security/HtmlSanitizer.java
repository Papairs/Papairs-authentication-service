package com.papairs.docs.security;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * HTML sanitizer for preventing XSS attacks while preserving rich text formatting.
 * Uses jsoup to clean HTML content and remove potentially malicious scripts.
 */
@Component
public class HtmlSanitizer {

    private final Safelist safelist;

    public HtmlSanitizer() {
        // Create a custom safelist that allows Tiptap's formatting tags
        this.safelist = Safelist.relaxed()
            // Basic formatting
            .addTags("p", "br", "span", "div")
            .addTags("h1", "h2", "h3", "h4", "h5", "h6")
            .addTags("strong", "b", "em", "i", "u", "s", "code", "pre")
            // Lists
            .addTags("ul", "ol", "li")
            // Quotes
            .addTags("blockquote")
            // Tables (if needed in future)
            .addTags("table", "thead", "tbody", "tr", "th", "td")
            // Links (with safe attributes)
            .addTags("a")
            .addAttributes("a", "href", "title", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            // Images (if needed in future)
            .addTags("img")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addProtocols("img", "src", "http", "https", "data")
            // Preserve some safe attributes for styling
            .addAttributes(":all", "class", "id")
            .addAttributes("p", "style")
            .addAttributes("span", "style")
            .addAttributes("div", "style")
            // Horizontal rule
            .addTags("hr");
    }

    /**
     * Sanitizes HTML content to prevent XSS attacks while preserving formatting.
     * 
     * @param html The HTML content to sanitize
     * @return Sanitized HTML content safe for storage and display
     */
    public String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return "<p></p>"; // Default empty paragraph for Tiptap
        }
        
        // Clean the HTML using jsoup with the configured safelist
        String cleaned = Jsoup.clean(html, safelist);
        
        // If cleaning resulted in empty content, return default
        if (cleaned.isEmpty() || cleaned.isBlank()) {
            return "<p></p>";
        }
        
        return cleaned;
    }

    /**
     * Checks if HTML content is safe without modifying it.
     * 
     * @param html The HTML content to check
     * @return true if the content is already safe, false otherwise
     */
    public boolean isSafe(String html) {
        if (html == null || html.isEmpty()) {
            return true;
        }
        
        String cleaned = sanitize(html);
        return html.equals(cleaned);
    }
}
