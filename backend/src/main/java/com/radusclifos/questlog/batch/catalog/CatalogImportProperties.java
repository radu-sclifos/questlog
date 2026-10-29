package com.radusclifos.questlog.batch.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("questlog.catalog-import")
public record CatalogImportProperties(
        boolean onStartup,
        Integer maxPages,
        Integer pageSize
) {
    public CatalogImportProperties {
        maxPages = maxPages == null ? 0 : maxPages;
        pageSize = pageSize == null ? 100 : pageSize;
        if (maxPages < 0) {
            throw new IllegalArgumentException("Catalog max-pages cannot be negative; use 0 for no limit");
        }
        if (pageSize < 10 || pageSize > 500) {
            throw new IllegalArgumentException("Catalog page-size must be between 1 and 100");
        }
    }
}
