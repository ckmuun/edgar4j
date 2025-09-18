package io.github.ckmuun.edgar4j;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.github.ckmuun.edgar4j.Constants.TEN_K_FORM;

/**
 * High-level service that orchestrates Edgar data download and parsing operations.
 * This service connects the edgar form download with functionality for parsing
 * documents into structured EdgarDocument objects.
 */
public class EdgarService {

    private final FilingService filingService;
    private final ParsingService parsingService;

    /**
     * Creates a new EdgarService with the provided services.
     * 
     * @param filingService Service for downloading SEC data
     * @param parsingService Service for parsing SEC filings
     */
    public EdgarService(FilingService filingService, ParsingService parsingService) {
        this.filingService = filingService;
        this.parsingService = parsingService;
    }

    /**
     * Creates a new EdgarService with default services using the provided user agent.
     * 
     * @param userAgent User agent to use for SEC API requests (should be a real email for production)
     */
    public EdgarService(String userAgent) {
        this.filingService = new FilingService(userAgent);
        this.parsingService = new ParsingService();
    }

    /**
     * Creates a new EdgarService with default services using a default user agent.
     * Note: For production use, provide a real email address as the user agent.
     */
    public EdgarService() {
        this.filingService = new FilingService();
        this.parsingService = new ParsingService();
    }

    /**
     * Load and parse the latest 10-K filing for a given ticker symbol.
     * 
     * @param ticker The stock ticker symbol (e.g., "AAPL", "MSFT")
     * @return Mono containing a list of parsed EdgarDocument objects from the latest 10-K filing
     */
    public Mono<Document> loadLatest10KForTicker(String ticker) {
        return filingService
                .getCompanyTickers()
                .filter(dto -> dto.ticker().equalsIgnoreCase(ticker))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticker not found: " + ticker)))
                .flatMap(dto -> filingService.getCompanyFilings(dto.cik()))
                .filter(filingDto -> filingDto.form().equals(TEN_K_FORM))
                .take(1) // Get the most recent 10-K
                .flatMap(filingService::getCompanyFiling)
                .map(parsingService::parseEdgarForm)
                .single();
    }

    /**
     * Load and parse a specific 10-K filing by CIK and accession number.
     * 
     * @param cik The company's CIK (Central Index Key)
     * @param accessionNumber The specific filing's accession number
     * @return Mono containing a list of parsed EdgarDocument objects
     */
    public Mono<Document> load10KByCikAndAccessionNumber(String cik, String accessionNumber) {
        return filingService
                .getCompanyFilings(cik)
                .filter(filingDto -> filingDto.accessionNumber().equals(accessionNumber))
                .filter(filingDto -> filingDto.form().equals(TEN_K_FORM))
                .take(1)
                .flatMap(filingService::getCompanyFiling)
                .map(parsingService::parseEdgarForm)
                .single();
    }

    /**
     * Get all available company tickers.
     * 
     * @return Flux of CompanyTickerDto objects
     */
    public Flux<CompanyTickerDto> getTickers() {
        return filingService.getCompanyTickers();
    }

    /**
     * Get all filings for a specific company by ticker.
     * 
     * @param ticker The stock ticker symbol
     * @return Flux of CompanyFilingMetadataDto objects
     */
    public Flux<CompanyFilingMetadataDto> getFilingsByTicker(String ticker) {
        return filingService
                .getCompanyTickers()
                .filter(dto -> dto.ticker().equalsIgnoreCase(ticker))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Ticker not found: " + ticker)))
                .flatMap(dto -> filingService.getCompanyFilings(dto.cik()));
    }

    /**
     * Get all filings for a specific company by CIK.
     * 
     * @param cik The company's CIK (Central Index Key)
     * @return Flux of CompanyFilingMetadataDto objects
     */
    public Flux<CompanyFilingMetadataDto> getFilingsByCik(String cik) {
        return filingService.getCompanyFilings(cik);
    }

    /**
     * Get all 10-K filings for a specific company by ticker.
     * 
     * @param ticker The stock ticker symbol
     * @return Flux of CompanyFilingMetadataDto objects for 10-K forms only
     */
    public Flux<CompanyFilingMetadataDto> get10KFilingsByTicker(String ticker) {
        return getFilingsByTicker(ticker)
                .filter(filingDto -> filingDto.form().equals(TEN_K_FORM));
    }

    /**
     * Download and parse any filing by its metadata.
     * 
     * @param metadata The filing metadata
     * @return Mono containing a list of parsed EdgarDocument objects
     */
    public Mono<Document> downloadAndParseFiling(CompanyFilingMetadataDto metadata) {
        return filingService
                .getCompanyFiling(metadata)
                .map(parsingService::parseEdgarForm);
    }
}
