package de.koware.edgar4j;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class EdgarWebClientFactoryTest {

    @Test
    void testCreateWebClient_DefaultUserAgent() {
        WebClient webClient = EdgarWebClientFactory.createWebClient();
        assertNotNull(webClient);
        // WebClient is properly instantiated
    }

    @Test
    void testCreateWebClient_CustomUserAgent() {
        String userAgent = "test@example.com";
        WebClient webClient = EdgarWebClientFactory.createWebClient(userAgent);
        assertNotNull(webClient);
        // WebClient is properly instantiated with custom user agent
    }

    @Test
    void testCreateWebClient_NullUserAgent() {
        // Should handle null user agent gracefully
        assertDoesNotThrow(() -> EdgarWebClientFactory.createWebClient(null));
    }

    @Test
    void testCreateWebClient_EmptyUserAgent() {
        // Should handle empty user agent gracefully
        assertDoesNotThrow(() -> EdgarWebClientFactory.createWebClient(""));
    }
}
