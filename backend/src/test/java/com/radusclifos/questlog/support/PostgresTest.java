package com.radusclifos.questlog.support;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Import(PostgresTestConfiguration.class)
@TestExecutionListeners(listeners = PostgresResetTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ResourceLock("questlog-shared-postgres")
@Execution(ExecutionMode.SAME_THREAD)
@ActiveProfiles("test")
public @interface PostgresTest {

}
