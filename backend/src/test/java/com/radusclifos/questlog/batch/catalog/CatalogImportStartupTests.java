package com.radusclifos.questlog.batch.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class CatalogImportStartupTests {
    private final CatalogImportLauncher launcher = mock(CatalogImportLauncher.class);
    private final CatalogImportProperties properties = new CatalogImportProperties(true, 0, 25);

    @Test
    void failedJobDoesNotFailApplicationStartup() throws Exception {
        var execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.FAILED);
        when(launcher.start(25, 0)).thenReturn(execution);
        var runner = new CatalogImportConfiguration().catalogImportOnStartup(launcher, properties);
        assertDoesNotThrow(() -> runner.run(null));
        verify(launcher).start(25, 0);
    }

    @Test
    void launchOrRecoveryExceptionDoesNotFailApplicationStartup() throws Exception {
        when(launcher.start(25, 0)).thenThrow(new IllegalStateException("Database unavailable"));
        var runner = new CatalogImportConfiguration().catalogImportOnStartup(launcher, properties);
        assertDoesNotThrow(() -> runner.run(null));
    }
}
