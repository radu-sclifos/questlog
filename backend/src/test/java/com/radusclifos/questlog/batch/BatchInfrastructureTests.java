package com.radusclifos.questlog.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchJobLauncherAutoConfiguration;
import org.springframework.boot.batch.jdbc.autoconfigure.BatchJdbcAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BatchInfrastructureTests {
    @Test
    void repositoryPersistsMetadataAndDoesNotLaunchJobsAtStartup() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
                BatchAutoConfiguration.class, BatchJdbcAutoConfiguration.class,
                BatchJobLauncherAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:batch-infrastructure;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                        "spring.datasource.username=sa", "spring.datasource.password=",
                        "spring.batch.job.enabled=false", "spring.batch.jdbc.initialize-schema=never")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed().hasSingleBean(JobRepository.class)
                            .hasSingleBean(JobOperator.class)
                            .doesNotHaveBean("jobLauncherApplicationRunner");
                    var dataSource = ctx.getBean(DataSource.class);
                    // Exercise the exact production metadata migration, not Hibernate DDL.
                    new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__baseline.sql"),
                            new ClassPathResource(
                            "db/migration/V2__spring_batch_metadata.sql")).execute(dataSource);
                    var repository = ctx.getBean(JobRepository.class);
                    var instance = repository.createJobInstance("infrastructure-check", new JobParameters());
                    assertEquals(instance.getId(), repository.getLastJobInstance("infrastructure-check").getId());
                    assertEquals(1, new JdbcTemplate(dataSource).queryForObject(
                            "select count(*) from BATCH_JOB_INSTANCE where JOB_NAME = 'infrastructure-check'",
                            Integer.class));
                    assertThat(ctx).doesNotHaveBean("igdbAuthenticationCheck");
                });
    }
}
