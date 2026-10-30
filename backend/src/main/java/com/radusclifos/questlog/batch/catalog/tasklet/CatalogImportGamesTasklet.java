package com.radusclifos.questlog.batch.catalog.tasklet;

import com.radusclifos.questlog.batch.catalog.CatalogImportParameters;
import com.radusclifos.questlog.batch.catalog.CatalogImportWriter;
import com.radusclifos.questlog.integration.igdb.IgdbClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.util.concurrent.TimeUnit;

/**
 * Imports catalog pages and keeps track of the last processed game ID.
 */
@Slf4j
public class CatalogImportGamesTasklet implements Tasklet {

    private final IgdbClient client;
    private final CatalogImportWriter writer;

    public CatalogImportGamesTasklet(IgdbClient client, CatalogImportWriter writer) {
        this.client = client;
        this.writer = writer;
    }

    /**
     * Runs the normal catalog import, stopping after the requested number of pages for test runs.
     * A limit of zero lets the import continue until IGDB has no more games.
     */
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var execution = contribution.getStepExecution();
        var parameters = CatalogImportParameters.from(execution.getJobExecution().getJobParameters());

        var result = importCatalogPage(contribution, parameters);

        long pagesProcessed = execution.getExecutionContext().getLong("pagesProcessed", 0L);
        if (parameters.maxPages() > 0 && pagesProcessed >= parameters.maxPages()) {
            log.info("Catalog execution {}: reached the test limit of {} pages",
                    execution.getJobExecution().getId(), parameters.maxPages());
            return RepeatStatus.FINISHED;
        }
        return result;
    }

    /**
     * Fetches and saves the next page of games matching the sync time window.
     * Finishes when IGDB returns no more games.
     */
    private RepeatStatus importCatalogPage(StepContribution contribution, CatalogImportParameters parameters) {
        var execution = contribution.getStepExecution();
        var context = execution.getExecutionContext();

        // last ID imported
        long lastGameId = context.getLong("lastGameId", 0L);

        // Includes HTTP, client throttling, retries and response deserialization.
        long pageStarted = System.nanoTime();
        var games = client.fetchCatalogGames(parameters.syncSince(), parameters.syncStartedAt(), lastGameId, parameters.pageSize());
        long igdbMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pageStarted);
        if (games.isEmpty()) {
            log.info("Catalog execution {}: no more games after cursor {}, igdbMs={}.",
                    execution.getJobExecution().getId(), lastGameId, igdbMs);
            return RepeatStatus.FINISHED;
        }

        // Includes page preloading, persistence, flush and transaction commit.
        long writeStarted = System.nanoTime();
        var result = writer.writePageSkippingInvalid(games);
        int saved = result.saved();
        long dbMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - writeStarted);
        context.putLong("lastGameId", games.getLast().id());
        context.putLong("pagesProcessed", context.getLong("pagesProcessed", 0L) + 1);

        // Update Batch statistics
        // Count each game received from IGDB (+1 for each game)
        for (var ignored : games) {
            contribution.incrementReadCount();
        }
        // Count games inserted or updated by the writer (writePage returns the number of saved records)
        contribution.incrementWriteCount(saved);
        // Count games excluded by our import filters (like bundles, special editions...)
        contribution.incrementFilterCount(result.filtered());
        contribution.incrementWriteSkipCount(result.skipped());

        long pageMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - pageStarted);
        log.info("Catalog execution {}: cursor {}, fetched {}, saved {}, filtered {}, skipped {} | igdbMs={}, dbMs={}, pageMs={}.",
                execution.getJobExecution().getId(), games.getLast().id(), games.size(), saved, result.filtered(), result.skipped(),
                igdbMs, dbMs, pageMs);

        return RepeatStatus.CONTINUABLE;
    }

}
