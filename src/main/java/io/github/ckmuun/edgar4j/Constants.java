package io.github.ckmuun.edgar4j;

import java.util.regex.Pattern;

/**
 * Constants used by the Edgar client library for accessing SEC data.
 */
public final class Constants {

    private Constants() {
        // Utility class - prevent instantiation
    }

    public static final String TICKER_FILE_PATH = "/files/company_tickers_exchange.json";
    public static final String SEC_BASE = "https://www.sec.gov";
    public static final String SEC_BASE_DATA = "https://data.sec.gov";
    public static final String TEN_K_FORM = "10-K";
    public static final String TEN_Q_FORM = "10-Q";
    public static final Pattern TEN_K_ITEMS_REGEX = Pattern.compile("^\\s*Item\\s+[0-9][0-9]?[A-C]?.?\\s+[a-z\\[\\]'\"´`,;: A-Z-]+\\s*$");
    public static final Pattern TEN_Q_ITEMS_REGEX = Pattern.compile("^\\s*Item\\s+[1-6]A?.?\\s+[a-z\\[\\]'\"´`,;: A-Z-]+\\s*$");
    public static final String IX_HEADER = "ix:header";
}
