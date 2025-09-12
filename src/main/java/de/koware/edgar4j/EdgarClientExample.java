package de.koware.edgar4j;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Example usage of the Edgar4j Library.
 * This class demonstrates how to use the library to fetch and parse SEC filings.
 */
@Slf4j
public class EdgarClientExample {

    public static void main(String[] args) {
        // For production use, replace with a real email address
        String userAgent = "your-email@example.com";
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
            List<EdgarDocument> documents = edgarService.loadLatest10KForTicker(ticker)
                    .block(); // Block for demo purposes

            if (documents != null) {
                log.info("Successfully parsed {} documents from the 10-K filing", documents.size());
                
                // Print information about each document
                for (int i = 0; i < Math.min(3, documents.size()); i++) {
                    EdgarDocument doc = documents.get(i);
                    log.info("Document {}: {} characters, type: {}", 
                        i + 1, 
                        doc.getContent().length(),
                        doc.getMetadata().get("documentType"));
                    
                    // Print first 200 characters of content
                    String preview = doc.getContent().length() > 200 
                        ? doc.getContent().substring(0, 200) + "..." 
                        : doc.getContent();
                    log.info("Content preview: {}", preview.replaceAll("\\s+", " "));
                }
            }
        } catch (Exception e) {
            log.error("Error processing 10-K filing for {}: {}", ticker, e.getMessage());
        }

        log.info("Example completed!");
    }
}
