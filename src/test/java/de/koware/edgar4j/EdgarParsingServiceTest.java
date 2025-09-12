package de.koware.edgar4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class EdgarParsingServiceTest {

    private EdgarParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new EdgarParsingService();
    }

    @Test
    void testStripFormHtml() {
        String htmlContent = """
            <html>
                <body>
                    <div style="color: red; font-size: 12px;" colspan="3">
                        <p>Some content</p>
                        <a href="http://example.com">Link</a>
                        <span></span>
                        <div>   </div>
                    </div>
                </body>
            </html>
            """;

        Document doc = Jsoup.parse(htmlContent);
        Document stripped = parsingService.stripFormHtml(doc);

        // Style and colspan attributes should be removed
        assertFalse(stripped.html().contains("style="));
        assertFalse(stripped.html().contains("colspan="));
        
        // Links should be removed
        assertFalse(stripped.html().contains("<a "));
        assertFalse(stripped.html().contains("href="));
        
        // Content should remain
        assertTrue(stripped.html().contains("Some content"));
    }

    @Test
    void testGetXbrlHeader() {
        String htmlWithXbrl = """
            <html>
                <ix:header>
                    <ix:resources>
                        <ix:relationship fromRefs="ref1" toRefs="ref2"/>
                    </ix:resources>
                </ix:header>
                <body>
                    <p>Main content</p>
                </body>
            </html>
            """;

        Document doc = Jsoup.parse(htmlWithXbrl);
        byte[] xbrlHeader = parsingService.getXbrlHeader(doc);

        // XBRL header should be extracted
        String xbrlString = new String(xbrlHeader);
        assertTrue(xbrlString.contains("ix:resources"));
        assertTrue(xbrlString.contains("ix:relationship"));
        
        // XBRL header should be removed from main document
        assertFalse(doc.html().contains("ix:header"));
        assertTrue(doc.html().contains("Main content"));
    }

    @Test
    void testGetFormItemsFromHtml() {
        String htmlContent = """
            <html>
                <body>
                    <h1>Item 1. Business</h1>
                    <p>This is the business section content.</p>
                    <p>More business information.</p>
                    <h1>Item 2. Properties</h1>
                    <p>This is about properties.</p>
                    <h1>Item 3. Legal Proceedings</h1>
                    <p>Legal information here.</p>
                </body>
            </html>
            """;

        Document doc = Jsoup.parse(htmlContent);
        Pattern pattern = Pattern.compile("^Item\\s+[0-9][0-9]?[A-C]?.?\\s+.*$");
        Map<String, Object> metadata = Map.of("form", "10-K");

        List<EdgarDocument> documents = parsingService.getFormItemsFromHtml(doc, pattern, metadata);

        // Should extract 3 items
        assertEquals(3, documents.size());

        // Check first document
        EdgarDocument firstDoc = documents.get(0);
        assertTrue(firstDoc.getContent().contains("Item 1. Business"));
        assertTrue(firstDoc.getContent().contains("business section content"));
        assertEquals("FORM_ITEM", firstDoc.getMetadata().get("documentType"));
        assertEquals(0, firstDoc.getMetadata().get("itemIndex"));
        assertEquals("Item 1. Business", firstDoc.getMetadata().get("itemTitle"));

        // Check that base metadata is included
        assertEquals("10-K", firstDoc.getMetadata().get("form"));
    }

    @Test
    void testConvertEdgarFormToDocuments() {
        String htmlContent = """
            <html>
                <ix:header>
                    <ix:resources>XBRL data</ix:resources>
                </ix:header>
                <body>
                    <h1>Item 1A. Risk Factors</h1>
                    <p>Risk factor content here.</p>
                    <h1>Item 2. Properties</h1>
                    <p>Properties content here.</p>
                </body>
            </html>
            """;

        CompanyFilingMetadataDto metadata = CompanyFilingMetadataDto.builder()
                .cik("0000320193")
                .name("Apple Inc.")
                .accessionNumber("0000320193-24-000006")
                .filingDate("2024-01-26")
                .form("10-K")
                .primaryDocument("aapl-20231230.htm")
                .build();

        CompanyFilingDto filing = new CompanyFilingDto(
                metadata,
                new ByteArrayInputStream(htmlContent.getBytes())
        );

        List<EdgarDocument> documents = parsingService.convertEdgarFormToDocuments(filing);

        // Should have XBRL header + form items
        assertTrue(documents.size() >= 2);

        // First document should be XBRL header
        EdgarDocument xbrlDoc = documents.get(0);
        assertEquals("XBRL_HEADER", xbrlDoc.getMetadata().get("documentType"));
        assertTrue(xbrlDoc.getContent().contains("XBRL data"));

        // Should have form items
        boolean hasFormItems = documents.stream()
                .anyMatch(doc -> "FORM_ITEM".equals(doc.getMetadata().get("documentType")));
        assertTrue(hasFormItems);

        // Check metadata propagation
        EdgarDocument formItem = documents.stream()
                .filter(doc -> "FORM_ITEM".equals(doc.getMetadata().get("documentType")))
                .findFirst()
                .orElseThrow();

        assertEquals("0000320193", formItem.getMetadata().get("cik"));
        assertEquals("Apple Inc.", formItem.getMetadata().get("companyName"));
        assertEquals("10-K", formItem.getMetadata().get("form"));
    }

    @Test
    void testConvertEdgarFormToDocuments_UnsupportedForm() {
        CompanyFilingMetadataDto metadata = CompanyFilingMetadataDto.builder()
                .form("8-K")
                .build();

        CompanyFilingDto filing = new CompanyFilingDto(
                metadata,
                new ByteArrayInputStream("<html></html>".getBytes())
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parsingService.convertEdgarFormToDocuments(filing)
        );

        assertTrue(exception.getMessage().contains("Currently only 10-K forms supported"));
    }
}
