# [WIP] Edgar4j

A standalone Java library for accessing and parsing SEC EDGAR data. This library provides a simple API to download company filings, parse them into structured documentChunks, and work with SEC data programmatically.

## Features

- **Company Data Access**: Retrieve company tickers, CIKs, and basic company information
- **Filing Download**: Download SEC filings (10-K forms currently supported) 
- **Document Parsing**: Parse HTML filings into structured documentChunks with metadata
- **Reactive API**: Built on Spring WebFlux for non-blocking, reactive operations
- **Standalone**: No Spring Boot required - works in any Java application
- **Easy Integration**: Simple API that can be embedded in existing applications

## Requirements

- Java 17 or later
- Maven 3.6+

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ckmuun</groupId>
    <artifactId>edgar4j</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import io.github.ckmuun.edgar4j.EdgarService;

public class Example {
    public static void main(String[] args) {
        // Create service with your email (SEC requires this for production)
        EdgarService edgarService = new EdgarService("your-email@example.com");
        
        // Get company tickers
        edgarService.getTickers()
            .take(10)
            .subscribe(ticker -> 
                System.out.println(ticker.ticker() + " - " + ticker.name()));
        
        // Download and parse latest 10-K for Apple
        edgarService.loadLatest10KForTicker("AAPL")
            .subscribe(documentChunks -> {
                System.out.println("Parsed " + documentChunks.size() + " documentChunks");
                documentChunks.forEach(doc -> 
                    System.out.println("Type: " + doc.getMetadata().get("documentType")));
            });
    }
}
```

### Advanced Usage

```java
// Get specific filings
edgarService.getFilingsByTicker("MSFT")
    .filter(filing -> filing.form().equals("10-K"))
    .take(5)
    .subscribe(filing -> System.out.println(
        "Filing: " + filing.accessionNumber() + " - " + filing.filingDate()));

// Download specific filing
edgarService.downloadAndParseFiling(filingMetadata)
    .subscribe(documentChunks -> {
        // Process parsed documentChunks
        documentChunks.forEach(doc -> {
            String content = doc.getContent();
            Map<String, Object> metadata = doc.getMetadata();
            // Work with documentChunk content and metadata
        });
    });
```

## API Overview

### Core Classes

- **`EdgarService`**: Main service for high-level operations
- **`EdgarDownloadService`**: Low-level service for downloading SEC data  
- **`EdgarParsingService`**: Service for parsing SEC filings into documentChunks
- **`EdgarDocument`**: Represents a parsed documentChunk with content and metadata

### Data Classes

- **`CompanyTickerDto`**: Company ticker information (ticker, name, CIK, exchange)
- **`CompanyFilingDto`**: Complete filing with metadata and content stream
- **`CompanyFilingMetadataDto`**: Metadata about a SEC filing

### Key Methods

#### EdgarService

- `getTickers()`: Get all company tickers
- `loadLatest10KForTicker(String ticker)`: Download and parse latest 10-K for a ticker
- `getFilingsByTicker(String ticker)`: Get all filings for a company
- `get10KFilingsByTicker(String ticker)`: Get only 10-K filings for a company
- `downloadAndParseFiling(CompanyFilingMetadataDto metadata)`: Parse any filing

## Configuration

### User Agent

The SEC requires a proper User-Agent header for API access. For production use, provide your email address:

```java
EdgarService service = new EdgarService("your-email@example.com");
```

### Custom WebClient

If you need custom WebClient configuration:

```java
import io.github.ckmuun.edgar4j.WebClientFactory;
import org.springframework.web.reactive.function.client.WebClient;

WebClient customClient = WebClientFactory.createWebClient("your-email@example.com");
EdgarDownloadService downloadService = new EdgarDownloadService(customClient);
EdgarService service = new EdgarService(downloadService, new EdgarParsingService());
```

## Document Structure

Parsed documentChunks include:

### Metadata
- `cik`: Company Central Index Key
- `companyName`: Company name
- `accessionNumber`: SEC accession number  
- `filingDate`: Date of filing
- `form`: Form type (e.g., "10-K")
- `documentType`: Type of parsed content ("XBRL_HEADER" or "FORM_ITEM")
- `itemIndex`: For form items, the sequential index
- `itemTitle`: For form items, the item title (e.g., "Item 1A. Risk Factors")

### Content
- Raw text content extracted from the HTML filing
- XBRL headers are parsed separately from form items
- HTML formatting is stripped for cleaner text processing

## Error Handling

The library uses reactive error handling. Common errors:

```java
edgarService.loadLatest10KForTicker("INVALID")
    .subscribe(
        documentChunks -> System.out.println("Success"),
        error -> System.err.println("Error: " + error.getMessage())
    );
```

## Limitations

- Currently only supports 10-K form parsing
- Requires network access to SEC APIs
- Rate limiting may apply (follow SEC guidelines)
- Large filings may require significant memory

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Submit a pull request

## SEC Compliance

This library is designed to comply with SEC API guidelines:
- Always provide a proper User-Agent with contact information
- Respect rate limits and be considerate of SEC resources
- Cache data when appropriate to reduce API calls

For more information, see the [SEC Developer Resources](https://www.sec.gov/developer).
