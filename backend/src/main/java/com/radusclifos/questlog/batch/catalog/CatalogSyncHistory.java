package com.radusclifos.questlog.batch.catalog;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Reads the start of the latest successful catalog sync from persisted Batch history.
 */
public class CatalogSyncHistory {

    private final JdbcTemplate jdbc;

    public CatalogSyncHistory(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns when the last successful catalog sync started as seconds (like Instant.ofEpochSecond(timestamp))
     * or null if no sync has succeeded yet.
     */
    public Long lastSuccessfulStart() {
        return jdbc.queryForObject("""
                select max(cast(p.PARAMETER_VALUE as bigint))
                from BATCH_JOB_EXECUTION e
                join BATCH_JOB_INSTANCE i on i.JOB_INSTANCE_ID = e.JOB_INSTANCE_ID
                join BATCH_JOB_EXECUTION_PARAMS p on p.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
                join BATCH_JOB_EXECUTION_PARAMS m on m.JOB_EXECUTION_ID = e.JOB_EXECUTION_ID
                where i.JOB_NAME = ? and e.STATUS = 'COMPLETED'
                  and p.PARAMETER_NAME = 'syncStartedAt'
                  and m.PARAMETER_NAME = 'syncMode' and m.PARAMETER_VALUE = 'CATALOG'
                """, Long.class, CatalogImportConfiguration.JOB_NAME);
    }

}
