package com.radusclifos.questlog.batch.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class CatalogImportSchedulerTests {
    private final CatalogImportLauncher launcher = mock(CatalogImportLauncher.class);
    private final CatalogImportProperties properties = new CatalogImportProperties(false, 2, 25);

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withUserConfiguration(CatalogImportConfiguration.Scheduling.class)
                .withBean(CatalogImportLauncher.class, () -> launcher)
                .withBean(CatalogImportProperties.class, () -> properties);
    }

    @Test
    void schedulingRequiresExplicitOptIn() {
        context().run(ctx -> assertThat(ctx).doesNotHaveBean(CatalogImportScheduler.class));
        context().withPropertyValues("questlog.catalog-import.scheduled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(CatalogImportScheduler.class));
        context().withPropertyValues("questlog.catalog-import.scheduled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(CatalogImportScheduler.class));
    }

    @Test
    void nightlyRunIgnoresTestPageLimit() throws Exception {
        var execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(launcher.start(25, 0)).thenReturn(execution);

        new CatalogImportScheduler(launcher, properties).synchronizeCatalog();

        verify(launcher).start(25, 0);
    }

    @Test
    void launcherFailureDoesNotEscapeScheduledInvocation() throws Exception {
        when(launcher.start(25, 0)).thenThrow(new IllegalStateException("Already running"));
        var scheduler = new CatalogImportScheduler(launcher, properties);

        assertDoesNotThrow(scheduler::synchronizeCatalog);
    }
}
