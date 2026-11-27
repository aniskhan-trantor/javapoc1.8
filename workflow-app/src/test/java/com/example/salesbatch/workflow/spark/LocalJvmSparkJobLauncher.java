package com.example.salesbatch.workflow.spark;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * IDE only: runs the Scala job in a child JVM with the local[*] master, which is the easiest way to
 * debug it. The classpath comes from a file written by the build (one classpath string), the compiled
 * classes directory and a configuration directory that holds hbase-site.xml.
 */
public class LocalJvmSparkJobLauncher implements SparkJobLauncher {

    static final String MAIN_CLASS = "com.example.salesbatch.spark.SalesAggregator";

    private final String classpathFile;
    private final String classesDir;
    private final String confDir;
    private final int debugPort;
    private final boolean debugSuspend;

    public LocalJvmSparkJobLauncher(String classpathFile, String classesDir, String confDir,
                                    int debugPort, boolean debugSuspend) {
        this.classpathFile = classpathFile;
        this.classesDir = classesDir;
        this.confDir = confDir;
        this.debugPort = debugPort;
        this.debugSuspend = debugSuspend;
    }

    List<String> buildCommand(String businessDate, String dependencyClasspath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        if (debugPort > 0) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=" + (debugSuspend ? "y" : "n") + ",address=" + debugPort);
        }
        cmd.add("-Dspark.master=local[*]");
        cmd.add("-cp");
        cmd.add(classesDir + File.pathSeparator + confDir + File.pathSeparator + dependencyClasspath);
        cmd.add(MAIN_CLASS);
        cmd.add(businessDate);
        return cmd;
    }

    @Override
    public void run(String businessDate) {
        String dependencyClasspath;
        try {
            dependencyClasspath = new String(Files.readAllBytes(Paths.get(classpathFile)), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the Spark classpath file " + classpathFile
                    + " (build spark-job first): " + e.getMessage(), e);
        }
        ProcessRunner.run(buildCommand(businessDate, dependencyClasspath));
    }
}
