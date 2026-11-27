package com.example.salesbatch.workflow.spark;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs a command, logs its output and fails the run when it exits non-zero. Shared by the IDE launchers. */
final class ProcessRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessRunner.class);

    private ProcessRunner() {}

    static void run(List<String> cmd) {
        LOG.info("Starting Spark job: {}", cmd);
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.info("[spark] {}", line);
                }
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("spark job exited with code " + exit);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start the Spark job: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the Spark job", e);
        }
    }
}
