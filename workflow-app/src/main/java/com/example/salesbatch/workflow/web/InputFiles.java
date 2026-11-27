package com.example.salesbatch.workflow.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Limits which server-side files POST /batches may read. A caller-supplied csvPath must lie inside the
 * folder named by the system property {@code batch.input.dir} (default: the temp folder, where uploads
 * are saved). Symbolic links and ".." are resolved before the check.
 */
final class InputFiles {

    private InputFiles() {}

    static Path root() {
        return real(Paths.get(System.getProperty("batch.input.dir", System.getProperty("java.io.tmpdir"))));
    }

    /** Does nothing for a blank path (the service reports that); otherwise throws IllegalArgumentException. */
    static void requireInsideRoot(String csvPath) {
        if (csvPath == null || csvPath.trim().isEmpty()) {
            return;
        }
        Path root = root();
        Path file = real(Paths.get(csvPath)); // InvalidPathException is an IllegalArgumentException
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("csvPath must be inside " + root);
        }
    }

    private static Path real(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        try {
            return Files.exists(absolute) ? absolute.toRealPath() : absolute;
        } catch (IOException e) {
            return absolute;
        }
    }
}
