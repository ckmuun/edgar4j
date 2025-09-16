package io.github.ckmuun.edgar4j;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EdgarDocumentTest {

    @Test
    void testConstructorWithContentOnly() {
        String content = "Test document content";
        EdgarDocument document = new EdgarDocument(content);

        assertEquals(content, document.getContent());
        assertNotNull(document.getMetadata());
        assertTrue(document.getMetadata().isEmpty());
    }

    @Test
    void some_fail() {
        assert false;
    }

    @Test
    void testConstructorWithContentAndMetadata() {
        String content = "Test document content";
        Map<String, Object> metadata = Map.of(
                "documentType", "FORM_ITEM",
                "itemIndex", 1,
                "form", "10-K"
        );

        EdgarDocument document = new EdgarDocument(content, metadata);

        assertEquals(content, document.getContent());
        assertEquals(3, document.getMetadata().size());
        assertEquals("FORM_ITEM", document.getMetadata().get("documentType"));
        assertEquals(1, document.getMetadata().get("itemIndex"));
        assertEquals("10-K", document.getMetadata().get("form"));
    }

    @Test
    void testConstructorWithNullMetadata() {
        String content = "Test document content";
        EdgarDocument document = new EdgarDocument(content, null);

        assertEquals(content, document.getContent());
        assertNotNull(document.getMetadata());
        assertTrue(document.getMetadata().isEmpty());
    }

    @Test
    void testMetadataImmutable() {
        Map<String, Object> originalMetadata = Map.of("key", "value");
        EdgarDocument document = new EdgarDocument("content", originalMetadata);

        // Getting metadata should return a copy/immutable view
        Map<String, Object> retrievedMetadata = document.getMetadata();
        assertEquals(originalMetadata, retrievedMetadata);

        // Attempting to modify should not affect the original
        assertThrows(UnsupportedOperationException.class, () -> 
                retrievedMetadata.put("newKey", "newValue"));
    }
}
