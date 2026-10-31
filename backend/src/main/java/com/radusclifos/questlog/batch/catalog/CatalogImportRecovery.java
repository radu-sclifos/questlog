package com.radusclifos.questlog.batch.catalog;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/** Recovers interrupted imports before a new attempt in this single-instance backend. */
@Slf4j
public class CatalogImportRecovery {
    private final JobRepository repository;
    private final TransactionTemplate transaction;

    public CatalogImportRecovery(JobRepository repository, PlatformTransactionManager manager) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(manager);
    }

    /** Marks unfinished executions as failed; the launcher must first exclude concurrent local imports. */
    public void recoverInterruptedExecutions() {
        transaction.executeWithoutResult(status -> {
            for (var running : repository.findRunningJobExecutions(CatalogImportConfiguration.JOB_NAME)) {
                var execution = repository.getJobExecution(running.getId());
                var now = LocalDateTime.now();
                var exit = ExitStatus.FAILED.addExitDescription("Previous catalog import did not finish; recovered before a new attempt.");
                for (var step : execution.getStepExecutions()) {
                    if (step.getStatus().isRunning()) {
                        step.setStatus(BatchStatus.FAILED);
                        step.setExitStatus(exit);
                        step.setEndTime(now);
                        repository.update(step);
                    }
                }
                execution.setStatus(BatchStatus.FAILED);
                execution.setExitStatus(exit);
                execution.setEndTime(now);
                repository.update(execution);
                log.error("Catalog execution {} was left unfinished; marked FAILED before starting a new import.", execution.getId());
            }
        });
    }
}
