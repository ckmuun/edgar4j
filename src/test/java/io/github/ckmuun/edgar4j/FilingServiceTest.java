package io.github.ckmuun.edgar4j;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static io.github.ckmuun.edgar4j.Constants.SEC_BASE;
import static io.github.ckmuun.edgar4j.Constants.TICKER_FILE_PATH;

@Slf4j
public class FilingServiceTest {

    private final FilingService filingService = new FilingService("some@something.com");

    @Test
    @Disabled
    void getCompanyTickers_raw() {
        WebClient webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 2048)) // 20 MB
                // Do not set Accept-Encoding manually; let WebClient/Netty handle compression and decompression
                .defaultHeader("User-Agent", "some@something.com")
                .defaultHeader("Accept-Charset", "UTF-8")
                .build();

        var resp = webClient.get()
                .uri(SEC_BASE + TICKER_FILE_PATH)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Assertions.assertNotNull(resp);
        // Expect JSON text, not gzipped bytes
        Assertions.assertTrue(resp.trim().startsWith("{"), "Response should be JSON text");
        log.debug(resp);
    }

    @Test
    @Disabled
    void getCompanyTickers_basic() {
        var resp = filingService.getCompanyTickers().blockLast();
        log.info("resp={}", resp);
    }

    @Test
    @Disabled
    void get10QForm_basic() {
        var data = filingService.execFilingRequest(
                "320193",
                "000032019325000073",
                "aapl-20250628.htm"
        ).block();
        Assertions.assertNotNull(data);

        Assertions.assertDoesNotThrow(
                () -> {
                    Jsoup.parse(data.asInputStream(), "UTF-8", "");
                }
        );
    }
}
