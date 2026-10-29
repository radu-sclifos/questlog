package com.radusclifos.questlog.batch.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import static org.junit.jupiter.api.Assertions.*;

class CatalogImportParametersTests {
    @Test
    void defaultsToUnlimitedPages() {
        var properties = new CatalogImportProperties(false, null, null);
        assertEquals(0, properties.maxPages());
        assertEquals(25, properties.pageSize());
        var parameters = new JobParametersBuilder().addLong("syncStartedAt", 1000L)
                .addLong("syncSince", 0L).toJobParameters();
        assertEquals(0, CatalogImportParameters.from(parameters).maxPages());
    }

    @Test
    void rejectsNegativePageLimitsInConfigurationAndJobParameters() {
        assertThrows(IllegalArgumentException.class, () -> new CatalogImportProperties(false, -1, 25));
        var parameters = new JobParametersBuilder().addLong("maxPages", -1L)
                .addLong("syncStartedAt", 1000L).addLong("syncSince", 0L).toJobParameters();
        assertThrows(IllegalArgumentException.class, () -> CatalogImportParameters.from(parameters));
    }
}
