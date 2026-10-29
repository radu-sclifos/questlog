package com.radusclifos.questlog.batch.catalog;

import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Reads the persisted execution parameters used by validation and the games tasklet.
 */
public record CatalogImportParameters(
        int maxPages,
        int pageSize,
        Long syncStartedAt,
        Long syncSince
) {

    public static CatalogImportParameters from(JobParameters parameters) {
        long maxPages = parameters.getLong("maxPages", 0L);
        if (maxPages < 0 || maxPages > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maxPages must be between 0 and " + Integer.MAX_VALUE);
        }

        long size = parameters.getLong("pageSize", 25L);
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("pageSize must be between 1 and 100");
        }

        Long started = parameters.getLong("syncStartedAt");
        Long since = parameters.getLong("syncSince");
        if (started == null || started <= 0 || since == null || since < 0 || since > started) {
            throw new IllegalArgumentException("Catalog sync requires a valid persisted time window");
        }
        return new CatalogImportParameters((int) maxPages, (int) size, started, since);
    }

}
