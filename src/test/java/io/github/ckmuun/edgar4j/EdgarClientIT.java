package io.github.ckmuun.edgar4j;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
public class EdgarClientIT {

    private final String userAgent = System.getProperty(
            "edgar.userAgent",
            System.getenv().getOrDefault("EDGAR_USER_AGENT", "cornelius.koller@online.de")
    );

    private final EdgarService edgarService = new EdgarService(userAgent);

    @Test
    void example_usage_end_to_end() {
        // Example 1: Get company tickers
        log.info("Fetching company tickers...");
        var firstFiveTickers = edgarService.getTickers()
                .take(5)
                .collectList()
                .block();

        Assertions.assertNotNull(firstFiveTickers, "Tickers list should not be null");
        Assertions.assertFalse(firstFiveTickers.isEmpty(), "Should receive at least one ticker");

        // Example 2: Get 10-K filings for Apple
        String ticker = "AAPL";
        log.info("Fetching 10-K filings for {}...", ticker);
        var firstTenKFilings = edgarService.get10KFilingsByTicker(ticker)
                .take(3)
                .collectList()
                .block();

        Assertions.assertNotNull(firstTenKFilings, "10-K filings should not be null");
        Assertions.assertFalse(firstTenKFilings.isEmpty(), "Should receive at least one 10-K filing");

        // Example 3: Download and parse the latest 10-K for Apple
        log.info("Downloading and parsing latest 10-K for {}...", ticker);
        Document document = edgarService.loadLatest10KForTicker(ticker).block();
        var chunks = document.chunks();
        
        Assertions.assertNotNull(chunks, "Parsed chunks should not be null");
        Assertions.assertFalse(chunks.isEmpty(), "Parsed chunks should not be empty");
        log.info("Successfully parsed {} chunks from the 10-K filing", chunks.size());
        
        // Print information about each chunk (limit to first 3)
        for (int i = 0; i < Math.min(3, chunks.size()); i++) {
            DocumentChunk chunk = chunks.get(i);
            String content = chunk.getContent();
            Assertions.assertNotNull(content, "Chunk content should not be null");
            Assertions.assertFalse(content.isEmpty(), "Chunk content should not be empty");
        
            
            log.info("Chunk {}: {} characters, type: {}",
                    i + 1,
                    content.length(),
                    chunk.getMetadata().get("documentType"));
            
            String preview = content.length() > 200
                    ? content.substring(0, 200) + "..."
                    : content;
            log.info("Content preview: {}", preview.replaceAll("\\s+", " "));
        }
    }
}
