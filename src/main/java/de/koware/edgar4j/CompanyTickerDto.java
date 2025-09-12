package de.koware.edgar4j;

/**
 * Data transfer object representing a company ticker information from SEC.
 */
public record CompanyTickerDto(String cik, String name, String ticker, String exchange) {
}
