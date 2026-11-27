package com.example.salesbatch.workflow.spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class IdeLauncherCommandTest {

    @Test
    public void dockerLauncherSubmitsTheJobToTheContainer() {
        List<String> cmd = new DockerClusterSparkJobLauncher("spark-master", "spark://spark-master:7077", "/work/job.jar", 0, false)
                .buildCommand("20261120");
        assertEquals("docker", cmd.get(0));
        assertTrue(cmd.contains("spark-master"));
        assertTrue(cmd.contains("spark://spark-master:7077"));
        assertEquals("/work/job.jar", cmd.get(cmd.size() - 2));
        assertEquals("20261120", cmd.get(cmd.size() - 1));
        assertFalse(cmd.contains("--driver-java-options"));
    }

    @Test
    public void dockerLauncherAddsADebugAgentWhenAPortIsGiven() {
        List<String> cmd = new DockerClusterSparkJobLauncher("c", "m", "j.jar", 5006, true).buildCommand("20261120");
        assertTrue(cmd.contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5006"));
    }

    @Test
    public void localLauncherRunsTheAggregatorInAChildJvmWithLocalMaster() {
        List<String> cmd = new LocalJvmSparkJobLauncher("cp.txt", "classes", "conf", 0, false)
                .buildCommand("20261120", "a.jar");
        assertTrue(cmd.contains("-Dspark.master=local[*]"));
        assertTrue(cmd.contains(LocalJvmSparkJobLauncher.MAIN_CLASS));
        assertEquals("20261120", cmd.get(cmd.size() - 1));
        assertTrue(cmd.get(cmd.indexOf("-cp") + 1).startsWith("classes"));
    }

    @Test(expected = IllegalStateException.class)
    public void localLauncherFailsClearlyWhenTheClasspathFileIsMissing() {
        new LocalJvmSparkJobLauncher("no/such/classpath.txt", "classes", "conf", 0, false).run("20261120");
    }
}
