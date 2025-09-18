package io.github.ckmuun.edgar4j;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class WebClientFactoryTest {

    @Test
    void testCreateWebClient_DefaultUserAgent() {
        WebClient webClient = WebClientFactory.createWebClient();
        assertNotNull(webClient);
        // WebClient is properly instantiated
    }

    @Test
    void testCreateWebClient_CustomUserAgent() {
        String userAgent = "test@example.com";
        WebClient webClient = WebClientFactory.createWebClient(userAgent);
        assertNotNull(webClient);
        // WebClient is properly instantiated with custom user agent
    }

    @Test
    void testCreateWebClient_NullUserAgent() {
        // Should handle null user agent gracefully
        assertDoesNotThrow(() -> WebClientFactory.createWebClient(null));
    }

    @Test
    void testCreateWebClient_EmptyUserAgent() {
        // Should handle empty user agent gracefully
        assertDoesNotThrow(() -> WebClientFactory.createWebClient(""));
    }
}
