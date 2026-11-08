package com.radusclifos.questlog.support;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class PostgresResetTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        // Initialize Flyway/Hibernate before cleaning, including when this is the
        // first class. This callback runs before @BeforeAll and test transactions.
        testContext.getApplicationContext();
        PostgresDatabaseReset.reset();
    }

}
