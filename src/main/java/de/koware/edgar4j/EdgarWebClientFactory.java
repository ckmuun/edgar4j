package de.koware.edgar4j;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * Factory for creating WebClient instances configured for SEC EDGAR API access.
 */
public final class EdgarWebClientFactory {
    
    private static final String ACCEPT_ENCODING = "gzip, deflate";
    private static final String DEFAULT_USER_AGENT = "edgar-client-library/1.0";
    
    private EdgarWebClientFactory() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a WebClient instance configured for accessing SEC EDGAR APIs.
     * Uses a default user agent. Note: SEC requires a real email address as User-Agent
     * for production use.
     */
    public static WebClient createWebClient() {
        return createWebClient(DEFAULT_USER_AGENT);
    }
    
    /**
     * Creates a WebClient instance configured for accessing SEC EDGAR APIs.
     * 
     * @param userAgent The user agent to use (SEC requires a real email address for production)
     * @return Configured WebClient instance
     */
    public static WebClient createWebClient(String userAgent) {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20 MB
                .defaultHeader("Accept-Encoding", ACCEPT_ENCODING)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Charset", "UTF-8")
                .build();
    }
}
