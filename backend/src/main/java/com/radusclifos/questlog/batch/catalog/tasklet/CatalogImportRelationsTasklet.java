package com.radusclifos.questlog.batch.catalog.tasklet;

import com.radusclifos.questlog.batch.catalog.CatalogImportWriter;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * Resolves one page of pending relations and checkpoints the last inspected ID.
 */
public class CatalogImportRelationsTasklet implements Tasklet {

    private final CatalogImportWriter writer;

    public CatalogImportRelationsTasklet(CatalogImportWriter writer) {
        this.writer = writer;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        var context = contribution.getStepExecution().getExecutionContext();

        long previous = context.getLong("lastLinkId", 0);
        long next = writer.resolvePage(previous, 100);
        context.putLong("lastLinkId", next);

        return next == previous ? RepeatStatus.FINISHED : RepeatStatus.CONTINUABLE;
    }

}
