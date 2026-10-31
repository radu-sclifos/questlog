package com.radusclifos.questlog.batch.catalog.tasklet;

import com.radusclifos.questlog.batch.catalog.CatalogReferenceResolver;
import com.radusclifos.questlog.integration.igdb.IgdbClient;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Refreshes genres and game types before any game pages are imported.
 */
public class CatalogRefreshReferencesTasklet implements Tasklet {

    private final IgdbClient client;
    private final CatalogReferenceResolver references;

    public CatalogRefreshReferencesTasklet(IgdbClient client, CatalogReferenceResolver references) {
        this.client = client;
        this.references = references;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var genres = client.fetchGenres();
        var gameTypes = client.fetchGameTypes();

        references.refresh(genres, gameTypes);

        for (int i = 0; i < genres.size() + gameTypes.size(); i++) {
            contribution.incrementReadCount();
        }

        contribution.incrementWriteCount(genres.size() + gameTypes.size());
        return RepeatStatus.FINISHED;
    }

}
