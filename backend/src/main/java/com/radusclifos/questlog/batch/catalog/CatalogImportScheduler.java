package com.radusclifos.questlog.batch.catalog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class CatalogImportScheduler {

    private final CatalogImportLauncher launcher;
    private final CatalogImportProperties properties;

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Rome")
    public void synchronizeCatalog() {
        try {
            // Nightly synchronization must not inherit the startup test page limit.
            var execution = launcher.start(properties.pageSize(), 0);
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("Scheduled catalog import execution {} completed", execution.getId());
            } else {
                log.error("Scheduled catalog import execution {} finished with status {}",
                        execution.getId(), execution.getStatus());
                execution.getAllFailureExceptions().forEach(failure -> log.error("Scheduled catalog import failure", failure));
            }
        } catch (Exception exception) {
            log.error("Unable to complete scheduled catalog import", exception);
        }
    }

}
