package io.github.ckmuun.edgar4j;

import lombok.Getter;

import java.util.Map;

/**
 * Represents a document extracted from an Edgar filing.
 * This is a standalone version that doesn't depend on Spring AI.
 */
@Getter
public class EdgarDocument {
    private final String content;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a new EdgarDocument with the given content and no metadata.
     */
    public EdgarDocument(String content) {
        this.content = content;
        this.metadata = Map.of();
    }
    
    /**
     * Creates a new EdgarDocument with the given content and metadata.
     */
    public EdgarDocument(String content, Map<String, Object> metadata) {
        this.content = content;
        if (metadata == null) {
            this.metadata = Map.of();
        } else {
            // Filter out null values which Map.copyOf() doesn't support
            this.metadata = metadata.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .collect(java.util.stream.Collectors.toUnmodifiableMap(
                            Map.Entry::getKey, 
                            Map.Entry::getValue));
        }
    }

}
