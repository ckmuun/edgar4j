package io.github.ckmuun.edgar4j;

import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a chunk of content extracted from an Edgar document.
 */
@Getter
public class DocumentChunk {
    private final String content;
    private final Map<String, Object> metadata;

    /**
     * Creates a new DocumentChunk with the given content.
     * Metadata defaults to an empty, unmodifiable map.
     */
    public DocumentChunk(String content) {
        this.content = content;
        this.metadata = Map.of();
    }

    /**
     * Creates a new DocumentChunk with the given content and metadata.
     * Null metadata will be treated as an empty map. Null keys/values are filtered out.
     */
    public DocumentChunk(String content, Map<String, Object> metadata) {
        this.content = content;
        if (metadata == null) {
            this.metadata = Map.of();
        } else {
            this.metadata = metadata.entrySet().stream()
                    .filter(e -> e.getKey() != null && e.getValue() != null)
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
