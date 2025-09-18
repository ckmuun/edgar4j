package io.github.ckmuun.edgar4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static io.github.ckmuun.edgar4j.Constants.*;

/**
 * Service for downloading company data and filings from SEC EDGAR API.
 */
@Slf4j
public class DownloadService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new EdgarDownloadService with the provided WebClient.
     * 
     * @param webClient WebClient instance configured for SEC access
     */
    public DownloadService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Creates a new EdgarDownloadService with a default WebClient.
     * Note: For production use, provide a user agent with a real email address.
     */
    public DownloadService() {
        this(WebClientFactory.createWebClient());
    }

    /**
     * Creates a new EdgarDownloadService with a WebClient using the specified user agent.
     * 
     * @param userAgent User agent to use (SEC requires real email for production)
     */
    public DownloadService(String userAgent) {
        this(WebClientFactory.createWebClient(userAgent));
    }

    /**
     * Retrieves all company tickers from SEC.
     * 
     * @return Flux of CompanyTickerDto objects
     */
    public Flux<CompanyTickerDto> getCompanyTickers() {
        log.info("Fetching company tickers...");
        return webClient.get()
                .uri(SEC_BASE + TICKER_FILE_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapIterable(this::parseCompanyTickerDtos);
    }

    /**
     * Retrieves filings for a specific company by CIK.
     * 
     * @param cik Company CIK (Central Index Key)
     * @return Flux of CompanyFilingMetadataDto objects
     */
    public Flux<CompanyFilingMetadataDto> getCompanyFilings(String cik) {
        cik = addLeadingZeroesToCik(cik);
        return webClient.get()
                .uri(SEC_BASE_DATA + "/submissions/CIK{cik}.json", cik)
                .retrieve()
                .bodyToMono(String.class)
                .flatMapIterable(this::parseFilings);
    }

    /**
     * Downloads a specific company filing.
     * 
     * @param metadata Filing metadata containing the information needed to download
     * @return Mono containing the CompanyFilingDto with filing content
     */
    public Mono<CompanyFilingDto> getCompanyFiling(CompanyFilingMetadataDto metadata) {
        var cik = removeLeadingZeroesFromCik(metadata.cik());
        var accessionNumber = metadata.accessionNumber().replace("-", "");
        var filename = metadata.primaryDocument();
        return webClient.get()
                .uri(SEC_BASE + "/Archives/edgar/data/{cik}/{accessionNumber}/{filename}", cik, accessionNumber, filename)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .reduce(DataBuffer::write)
                .map(dataBuffer -> new CompanyFilingDto(metadata, dataBuffer.asInputStream()));
    }

    private String removeLeadingZeroesFromCik(String cik) {
        return cik.replaceFirst("^0+(?!$)", "");
    }

    private String addLeadingZeroesToCik(String cik) {
        StringBuilder cikBuilder = new StringBuilder(cik);
        while (cikBuilder.length() < 10) {
            cikBuilder.insert(0, "0");
        }
        return cikBuilder.toString();
    }

    protected List<CompanyFilingMetadataDto> parseFilings(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode recent = root.path("filings").path("recent");

            String cik = root.path("cik").asText();
            String name = root.path("name").asText();

            List<String> accessionNumbers = extractTextArray(recent.path("accessionNumber"));
            List<String> filingDates = extractTextArray(recent.path("filingDate"));
            List<String> reportDates = extractTextArray(recent.path("reportDate"));
            List<String> acceptanceDateTimes = extractTextArray(recent.path("acceptanceDateTime"));
            List<String> acts = extractTextArray(recent.path("act"));
            List<String> forms = extractTextArray(recent.path("form"));
            List<String> fileNumbers = extractTextArray(recent.path("fileNumber"));
            List<String> filmNumbers = extractTextArray(recent.path("filmNumber"));
            List<String> items = extractTextArray(recent.path("items"));
            List<String> coreTypes = extractTextArray(recent.path("core_type"));
            List<String> sizes = extractTextArray(recent.path("size"));
            List<String> isXbrls = extractTextArray(recent.path("isXBRL"));
            List<String> isInlineXbrls = extractTextArray(recent.path("isInlineXBRL"));
            List<String> primaryDocuments = extractTextArray(recent.path("primaryDocument"));
            List<String> primaryDocumentDescriptions = extractTextArray(recent.path("primaryDocDescription"));

            int length = accessionNumbers.size();
            List<CompanyFilingMetadataDto> filings = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                filings.add(new CompanyFilingMetadataDto(
                        cik,
                        name,
                        accessionNumbers.get(i),
                        filingDates.get(i),
                        reportDates.get(i),
                        acceptanceDateTimes.get(i),
                        acts.get(i),
                        forms.get(i),
                        fileNumbers.get(i),
                        filmNumbers.get(i),
                        items.get(i),
                        coreTypes.get(i),
                        sizes.get(i),
                        "1".equals(isXbrls.get(i)),
                        "1".equals(isInlineXbrls.get(i)),
                        primaryDocuments.get(i),
                        primaryDocumentDescriptions.get(i)
                ));
            }

            return filings;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SEC filing response", e);
        }
    }

    private List<String> extractTextArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                result.add(node.asText());
            }
        }
        return result;
    }

    protected List<CompanyTickerDto> parseCompanyTickerDtos(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeRawResponse(rawResponse));
            JsonNode dataArray = root.path("data");

            List<CompanyTickerDto> result = new ArrayList<>();
            if (dataArray.isArray()) {
                for (JsonNode node : dataArray) {
                    result.add(parseCompanyTickerDto(node));
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse company tickers response", e);
        }
    }

    protected String sanitizeRawResponse(String rawResponse) {
        rawResponse = rawResponse.trim();
        return rawResponse.replaceAll("\\s+", " ");
    }
    /*
        The tickers come as valid, but rather strangely formatted JSON.
        All fields are in an array, in a certain order, but without any
        key-value semantics. The top-level JSON has one child element which
        describes the array formatting.
        Basically, this is CSV semantics in JSON syntax.
     */
    private CompanyTickerDto parseCompanyTickerDto(JsonNode node) {
        var arraynode = (ArrayNode) node;
        var iter = arraynode.elements();
        var cik = iter.next().asText();
        var name = iter.next().asText();
        var ticker = iter.next().asText();
        var exchange = iter.next().asText();

        return new CompanyTickerDto(cik, name, ticker, exchange);
    }
}
