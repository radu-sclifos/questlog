package com.radusclifos.questlog.batch.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-instance, manual launcher. Runs synchronously on the invoking thread.
 */
@Slf4j
public class CatalogImportLauncher {

    private final CatalogImportRecovery recovery;
    private final CatalogSyncHistory history;
    private final Job job;
    private final TaskExecutorJobOperator operator;
    private final AtomicBoolean running = new AtomicBoolean();

    public CatalogImportLauncher(Job job, JobRepository repository, CatalogSyncHistory history, CatalogImportRecovery recovery) throws Exception {
        this.recovery = recovery;
        this.history = history;
        this.job = job;
        operator = new TaskExecutorJobOperator();
        operator.setJobRepository(repository);
        var registry = new MapJobRegistry();
        registry.register(job);
        operator.setJobRegistry(registry);
        operator.setTaskExecutor(new SyncTaskExecutor());
        operator.afterPropertiesSet();
    }

    public JobExecution start(int pageSize, int maxPages) throws Exception {
        return exclusively(() -> {
            long started = java.time.Instant.now().getEpochSecond();
            Long previous = history.lastSuccessfulStart();
            // Replay five minutes to tolerate timestamp boundaries and small clock differences.
            long since = previous == null ? 0 : Math.max(0, previous - 300);
            log.info("Starting {} catalog sync: window {} to {}, page limit {} (0 means unlimited).",
                    previous == null ? "initial" : "incremental", since, started, maxPages);
            var parameters = new JobParametersBuilder()
                    .addLong("pageSize", (long) pageSize)
                    .addLong("maxPages", (long) maxPages)
                    // Test runs must never mark a partial catalog as fully synchronized.
                    .addString("syncMode", maxPages == 0 ? "CATALOG" : "LIMITED")
                    .addLong("syncStartedAt", started).addLong("syncSince", since)
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters();
            return operator.start(job, parameters);
        });
    }

    private JobExecution exclusively(Launch operation) throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Catalog import is already running");
        }
        try {
            // Only one backend instance is supported
            // The local guard proves no import is active here
            recovery.recoverInterruptedExecutions();
            return operation.run();
        } finally {
            running.set(false);
        }
    }

    @FunctionalInterface
    private interface Launch {
        JobExecution run() throws Exception;
    }

}
