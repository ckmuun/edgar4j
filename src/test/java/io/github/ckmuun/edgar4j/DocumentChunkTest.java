package io.github.ckmuun.edgar4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentChunkTest {

    @Test
    void testConstructorWithContentOnly() {
        String content = "Test document content";
        DocumentChunk documentChunk = new DocumentChunk(content);

        assertEquals(content, documentChunk.getContent());
    }

}
