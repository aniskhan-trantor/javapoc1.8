package com.example.salesbatch.workflow.spark;

import java.util.ArrayList;
import java.util.List;

/**
 * IDE only: submits the Scala job to the Spark cluster of docker/local/docker-compose.yml with
 * "docker exec CONTAINER spark-submit". The jar is the path as seen inside the container.
 */
public class DockerClusterSparkJobLauncher implements SparkJobLauncher {

    private final String container;
    private final String master;
    private final String jar;
    private final int debugPort;
    private final boolean debugSuspend;

    public DockerClusterSparkJobLauncher(String container, String master, String jar,
                                         int debugPort, boolean debugSuspend) {
        this.container = container;
        this.master = master;
        this.jar = jar;
        this.debugPort = debugPort;
        this.debugSuspend = debugSuspend;
    }

    List<String> buildCommand(String businessDate) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("exec");
        cmd.add(container);
        cmd.add("spark-submit");
        cmd.add("--class");
        cmd.add(LocalJvmSparkJobLauncher.MAIN_CLASS);
        cmd.add("--master");
        cmd.add(master);
        if (debugPort > 0) {
            cmd.add("--driver-java-options");
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=" + (debugSuspend ? "y" : "n") + ",address=*:" + debugPort);
        }
        cmd.add(jar);
        cmd.add(businessDate);
        return cmd;
    }

    @Override
    public void run(String businessDate) {
        ProcessRunner.run(buildCommand(businessDate));
    }
}
