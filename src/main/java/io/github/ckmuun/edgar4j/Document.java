package io.github.ckmuun.edgar4j;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a logical document extracted from an Edgar filing.
 * A document consists of one or more content chunks and associated metadata.
 */
@Getter
public record Document(DocumentChunk xbrlHeader, List<DocumentChunk> chunks, Map<String, Object> metadata) {
    /**
     * Creates a new Document with the given chunks and metadata.
     *
     * @param chunks   the list of content chunks
     * @param metadata the metadata for the document
     */
    public Document(DocumentChunk xbrlHeader, List<DocumentChunk> chunks, Map<String, Object> metadata) {
        this.xbrlHeader = xbrlHeader;
        if (chunks == null) {
            this.chunks = List.of();
        } else {
            // filter out null chunks
            this.chunks = chunks.stream()
                    .filter(Objects::nonNull).toList();
        }

        if (metadata == null) {
            this.metadata = Map.of();
            return;
        }
        // Filter out null keys/values which Map.copyOf() doesn't support
        this.metadata = metadata.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }
}
