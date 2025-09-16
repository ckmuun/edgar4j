package io.github.ckmuun.edgar4j;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.github.ckmuun.edgar4j.EdgarConstants.*;


/**
 * Service for parsing SEC EDGAR forms into structured documents.
 */
public class EdgarParsingService {

    /**
     * Creates a new EdgarParsingService.
     */
    public EdgarParsingService() {
        // Default constructor
    }

    /**
     * Convert an Edgar form filing into a list of structured documents.
     * 
     * @param companyFilingDto The filing to parse
     * @return List of EdgarDocument objects containing parsed content
     * @throws IllegalArgumentException if the form type is not supported
     */
    public List<EdgarDocument> convertEdgarFormToDocuments(CompanyFilingDto companyFilingDto) {
        List<EdgarDocument> documents = new ArrayList<>();

        org.jsoup.nodes.Document htmlDocument;
        try {
            htmlDocument = Jsoup.parse(companyFilingDto.file(), "UTF-8", "");
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to parse HTML document", ioe);
        }

        // Extract XBRL header and strip it from the main document
        byte[] xbrl = this.getXbrlHeader(htmlDocument);
        htmlDocument = this.stripFormHtml(htmlDocument);

        // Create metadata for documents
        Map<String, Object> metadata = createFilingMetadata(companyFilingDto.metadata());

        // Add XBRL header as a document
        Map<String, Object> xbrlMetadata = new HashMap<>(metadata);
        xbrlMetadata.put("documentType", "XBRL_HEADER");
        documents.add(new EdgarDocument(new String(xbrl, StandardCharsets.UTF_8), xbrlMetadata));

        // Parse form items - currently only 10-K supported
        if (!companyFilingDto.metadata().form().equals(TEN_K_FORM)) {
            throw new IllegalArgumentException("Currently only %s forms supported".formatted(TEN_K_FORM));
        }

        var formItems = getFormItemsFromHtml(htmlDocument, Pattern.compile(FORM_10K_ITEMS_REGEX), metadata);
        documents.addAll(formItems);
        
        return documents;
    }

    /**
     * Extract form items from HTML document using the provided regex pattern.
     * 
     * @param htmlDocument The HTML document to parse
     * @param separatorRegex Pattern to identify form item boundaries
     * @param baseMetadata Base metadata to include in each document
     * @return List of documents containing form items
     */
    protected List<EdgarDocument> getFormItemsFromHtml(org.jsoup.nodes.Document htmlDocument, 
                                                       Pattern separatorRegex, 
                                                       Map<String, Object> baseMetadata) {
        return getFormItemsFromHtml(htmlDocument, separatorRegex, separatorRegex, baseMetadata);
    }

    /**
     * Extract form items from HTML document using separate begin and end patterns.
     * 
     * @param htmlDocument The HTML document to parse
     * @param beginRegex Pattern to identify start of form items
     * @param endRegex Pattern to identify end of form items
     * @param baseMetadata Base metadata to include in each document
     * @return List of documents containing form items
     */
    protected List<EdgarDocument> getFormItemsFromHtml(org.jsoup.nodes.Document htmlDocument, 
                                                       Pattern beginRegex, 
                                                       Pattern endRegex,
                                                       Map<String, Object> baseMetadata) {
        List<EdgarDocument> documents = new ArrayList<>();

        boolean match = false;
        var content = new StringBuilder();
        String currentItemTitle = null;
        int itemIndex = 0;
        
        for (Element e : htmlDocument.getAllElements()) {
            if (match && endRegex.matcher(e.ownText()).matches()) {
                // End of current item - create document
                Map<String, Object> itemMetadata = new HashMap<>(baseMetadata);
                itemMetadata.put("documentType", "FORM_ITEM");
                itemMetadata.put("itemIndex", itemIndex++);
                itemMetadata.put("itemTitle", currentItemTitle);

                documents.add(new EdgarDocument(content.toString().trim(), itemMetadata));
                match = false;
                content = new StringBuilder();
                currentItemTitle = null;
            }
            
            if (beginRegex.matcher(e.ownText()).matches()) {
                // Start of new item
                match = true;
                currentItemTitle = e.ownText().trim();
            }
            
            if (match) {
                content.append(' ');
                content.append(e.ownText());
            }
        }
        
        // Handle case where document ends without a closing pattern
        if (match && !content.isEmpty()) {
            Map<String, Object> itemMetadata = new HashMap<>(baseMetadata);
            itemMetadata.put("documentType", "FORM_ITEM");
            itemMetadata.put("itemIndex", itemIndex);
            itemMetadata.put("itemTitle", currentItemTitle);
            documents.add(new EdgarDocument(content.toString().trim(), itemMetadata));
        }
        
        return documents;
    }

    /**
     * Extract XBRL header from HTML document and remove it from the document.
     * 
     * @param htmlDocument The HTML document to process
     * @return XBRL header content as byte array
     */
    protected byte[] getXbrlHeader(org.jsoup.nodes.Document htmlDocument) {
        var xbrlItems = htmlDocument.getElementsByTag(IX_HEADER);
        String xbrlContent = xbrlItems.html();
        htmlDocument.getElementsByTag(IX_HEADER).remove();
        return xbrlContent.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Strip unnecessary HTML attributes and empty elements from the document.
     * Removes inline styles, colspans, links and empty HTML tags to make
     * the document cleaner and more readable.
     * 
     * @param htmlDocument The HTML document to strip
     * @return The cleaned HTML document
     */
    protected org.jsoup.nodes.Document stripFormHtml(org.jsoup.nodes.Document htmlDocument) {
        // Remove inline styles (reduces file size and makes it more readable during development)
        Elements elementsWithStyle = htmlDocument.select("[style]");
        for (Element element : elementsWithStyle) {
            element.removeAttr("style");
        }
        
        Elements elementsWithColspan = htmlDocument.select("[colspan]");
        for (Element element : elementsWithColspan) {
            element.removeAttr("colspan");
        }
        
        // Select all elements and remove empty ones
        Elements allElements = htmlDocument.getAllElements();

        // Iterate in reverse to avoid modifying the tree while traversing
        for (int i = allElements.size() - 1; i >= 0; i--) {
            Element el = allElements.get(i);
            // Check if element is empty or contains only whitespace
            if (el.children().isEmpty() && el.text().trim().isEmpty()) {
                el.remove();
            }
        }
        
        // Remove all <a> elements
        Elements links = htmlDocument.select("a");
        for (Element link : links) {
            link.remove();
        }

        return htmlDocument;
    }

    /**
     * Create metadata map from filing metadata.
     * 
     * @param metadata The filing metadata
     * @return Map containing metadata for documents
     */
    private Map<String, Object> createFilingMetadata(CompanyFilingMetadataDto metadata) {
        Map<String, Object> result = new HashMap<>();
        if (metadata.cik() != null) result.put("cik", metadata.cik());
        if (metadata.name() != null) result.put("companyName", metadata.name());
        if (metadata.accessionNumber() != null) result.put("accessionNumber", metadata.accessionNumber());
        if (metadata.filingDate() != null) result.put("filingDate", metadata.filingDate());
        if (metadata.reportDate() != null) result.put("reportDate", metadata.reportDate());
        if (metadata.form() != null) result.put("form", metadata.form());
        if (metadata.primaryDocument() != null) result.put("primaryDocument", metadata.primaryDocument());
        return result;
    }
}
