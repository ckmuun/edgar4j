package io.github.ckmuun.edgar4j;

/**
 * Constants used by the Edgar client library for accessing SEC data.
 */
public final class EdgarConstants {
    
    private EdgarConstants() {
        // Utility class - prevent instantiation
    }
    
    public static final String TICKER_FILE_PATH = "/files/company_tickers_exchange.json";
    public static final String SEC_BASE = "https://www.sec.gov";
    public static final String SEC_BASE_DATA = "https://data.sec.gov";
    public static final String TEN_K_FORM = "10-K";
    public static final String FORM_10K_ITEMS_REGEX = "^Item\\s+[0-9][0-9]?[A-C]?.?\\s+[a-z\\[\\]''\"´`,;: A-Z-]+\\s*$";
    public static final String IX_HEADER = "ix:header";
}
