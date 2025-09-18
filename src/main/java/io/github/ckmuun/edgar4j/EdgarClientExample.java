package io.github.ckmuun.edgar4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Example usage of the Edgar4j Library.
 * This class demonstrates how to use the library to fetch and parse SEC filings.
 */
public class EdgarClientExample {
    private static final Logger log = LoggerFactory.getLogger(EdgarClientExample.class);

    public static void main(String[] args) {
        // For production use, replace with a real email address
        String userAgent = "a.b@.com";
        EdgarService edgarService = new EdgarService(userAgent);

        // Example 1: Get company tickers
        log.info("Fetching company tickers...");
        edgarService.getTickers()
                .take(5) // Just take first 5 for demo
                .doOnNext(ticker -> log.info("Ticker: {} - {} ({})", ticker.ticker(), ticker.name(), ticker.exchange()))
                .blockLast(); // Block for demo purposes - use subscribe() in real applications

        // Example 2: Get 10-K filings for Apple
        String ticker = "AAPL";
        log.info("Fetching 10-K filings for {}...", ticker);
        edgarService.get10KFilingsByTicker(ticker)
                .take(3) // Just take first 3 for demo
                .doOnNext(filing -> log.info("10-K Filing: {} - {} ({})", 
                    filing.accessionNumber(), filing.filingDate(), filing.form()))
                .blockLast();

        // Example 3: Download and parse the latest 10-K for Apple
        log.info("Downloading and parsing latest 10-K for {}...", ticker);
        try {
            Document document = edgarService.loadLatest10KForTicker(ticker)
                    .block(); // Block for demo purposes

            if (document != null) {
                var chunks = document.getChunks();
                log.info("Successfully parsed {} chunks from the 10-K filing", chunks.size());

                // Print information about first few chunks
                for (int i = 0; i < Math.min(3, chunks.size()); i++) {
                    DocumentChunk chunk = chunks.get(i);
                    int contentLength = chunk.getContent() == null ? 0 : chunk.getContent().length();
                    log.info("Chunk {}: {} characters, type: {}",
                            i + 1,
                            contentLength,
                            chunk.getMetadata().get("documentType"));

                    // Print first 200 characters of chunk content
                    String content = chunk.getContent() == null ? "" : chunk.getContent();
                    String preview = content.length() > 200
                            ? content.substring(0, 200) + "..."
                            : content;
                    log.info("Content preview: {}", preview.replaceAll("\\s+", " "));
                }
            }
        } catch (Exception e) {
            log.error("Error processing 10-K filing for {}: {}", ticker, e.getMessage());
        }

        log.info("Example completed!");
    }
}
