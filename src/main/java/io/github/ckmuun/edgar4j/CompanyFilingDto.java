package io.github.ckmuun.edgar4j;

import java.io.InputStream;

/**
 * Data transfer object representing a company filing with its metadata and content.
 */
public record CompanyFilingDto(
        CompanyFilingMetadataDto metadata,
        InputStream file
) {
}
