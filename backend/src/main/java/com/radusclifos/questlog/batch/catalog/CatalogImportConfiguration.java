package com.radusclifos.questlog.batch.catalog;

import com.radusclifos.questlog.batch.catalog.repository.PendingGameRelationRepository;
import com.radusclifos.questlog.batch.catalog.tasklet.CatalogImportGamesTasklet;
import com.radusclifos.questlog.batch.catalog.tasklet.CatalogImportRelationsTasklet;
import com.radusclifos.questlog.game.repository.*;
import com.radusclifos.questlog.integration.igdb.IgdbClient;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogImportProperties.class)
public class CatalogImportConfiguration {

    public static final String JOB_NAME = "catalogImportJob";

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    @ConditionalOnProperty(prefix = "questlog.catalog-import", name = "scheduled", havingValue = "true")
    static class Scheduling {
        @Bean
        CatalogImportScheduler catalogImportScheduler(CatalogImportLauncher launcher, CatalogImportProperties properties) {
            return new CatalogImportScheduler(launcher, properties);
        }
    }

    /**
     * Creates an optional startup runner that starts a new catalog import.
     * Logs failures without stopping application startup.
     * Used only for local testing.
     */
    @Bean
    @ConditionalOnProperty(prefix = "questlog.catalog-import", name = "on-startup", havingValue = "true")
    ApplicationRunner catalogImportOnStartup(CatalogImportLauncher launcher, CatalogImportProperties properties) {
        return args -> {
            try {
                var execution = launcher.start(properties.pageSize(), properties.maxPages());
                if (execution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("Catalog import execution {} completed", execution.getId());
                } else {
                    log.error("Catalog import execution {} finished with status {}; the backend will continue running.",
                            execution.getId(), execution.getStatus());
                    execution.getAllFailureExceptions().forEach(failure -> log.error("Catalog import failure", failure));
                }
            } catch (Exception exception) {
                log.error("Unable to complete startup catalog import; the backend will continue running.", exception);
            }
        };
    }

    // Works only if import from IGDB is enabled
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "questlog.igdb", name = "enabled", havingValue = "true")
    static class Enabled {
        @Bean
        Job catalogImportJob(JobRepository repository,
                             @Qualifier("catalogImportGames") Step games,
                             @Qualifier("catalogImportRelations") Step relations) {
            return new JobBuilder(JOB_NAME, repository).preventRestart().validator(parameters -> {
                try {
                    CatalogImportParameters.from(parameters);
                } catch (RuntimeException exception) {
                    throw new InvalidJobParametersException(exception.getMessage());
                }
            }).start(games).next(relations).build();
        }

        @Bean
        CatalogImportLauncher catalogImportLauncher(@Qualifier("catalogImportJob") Job job, JobRepository repository, CatalogSyncHistory history, CatalogImportRecovery recovery) throws Exception {
            return new CatalogImportLauncher(job, repository, history, recovery);
        }

        @Bean
        CatalogImportRecovery catalogImportRecovery(JobRepository repository, PlatformTransactionManager manager) {
            return new CatalogImportRecovery(repository, manager);
        }

        @Bean
        CatalogSyncHistory catalogSyncHistory(org.springframework.jdbc.core.JdbcTemplate jdbc) {
            return new CatalogSyncHistory(jdbc);
        }

        @Bean
        CatalogImportWriter catalogImportWriter(EntityManager em, PlatformTransactionManager manager,
                                                GameRepository games, GameCompanyRepository companies, GameRelationRepository relations,
                                                PendingGameRelationRepository pendingRelations, CatalogReferenceResolver references) {
            return new CatalogImportWriter(em, manager, games, companies, relations, pendingRelations, references);
        }

        @Bean
        CatalogReferenceResolver catalogReferenceResolver(PlatformRepository platforms, GenreRepository genres,
                                                          EngineRepository engines, FranchiseRepository franchises, CompanyRepository companies,
                                                          GameTypeRepository gameTypes, PlatformTransactionManager manager) {
            return new CatalogReferenceResolver(platforms, genres, engines, franchises, companies, gameTypes, manager);
        }

        @Bean
        Step catalogImportGames(JobRepository repository, PlatformTransactionManager manager,
                                IgdbClient client, CatalogImportWriter writer, CatalogReferenceResolver references) {
            var noTransaction = new DefaultTransactionAttribute();
            noTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

            return new StepBuilder("catalogImportGames", repository)
                    .tasklet(new CatalogImportGamesTasklet(client, writer, references))
                    .transactionManager(manager)
                    .transactionAttribute(noTransaction)
                    .build();
        }

        @Bean
        Step catalogImportRelations(JobRepository repository, PlatformTransactionManager manager,
                                    CatalogImportWriter writer) {
            var noTransaction = new DefaultTransactionAttribute();
            noTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

            return new StepBuilder("catalogImportRelations", repository)
                    .tasklet(new CatalogImportRelationsTasklet(writer))
                    .transactionManager(manager)
                    .transactionAttribute(noTransaction)
                    .build();
        }
    }
}
