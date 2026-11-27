package com.example.salesbatch.workflow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.salesbatch.workflow.spark.SparkJobLauncher;

/**
 * Test wiring: an in-memory repository and a Spark launcher that aggregates in memory, so the
 * whole workflow runs without HBase or Spark. Set {@link #failSpark} to make the Spark step fail.
 */
@Configuration
public class TestInfraConfig {

    /** When true, the fake Spark job fails like a spark-submit that exits non-zero. */
    static volatile boolean failSpark = false;

    @Bean
    public InMemorySalesRepository salesRepository() {
        return new InMemorySalesRepository();
    }

    @Bean
    public SparkJobLauncher sparkJobLauncher(InMemorySalesRepository repository) {
        return businessDate -> {
            if (failSpark) {
                throw new IllegalStateException("spark-submit exited with code 1 (simulated)");
            }
            repository.aggregateRaw(businessDate);
        };
    }
}
