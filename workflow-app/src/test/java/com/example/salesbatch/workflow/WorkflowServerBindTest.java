package com.example.salesbatch.workflow;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import com.example.salesbatch.workflow.web.ApproverAuth;

public class WorkflowServerBindTest {

    private static final ApproverAuth APPROVERS = new ApproverAuth("alice:alice-secret-token-1234");

    @Test
    public void loopbackAddressesAreLocal() {
        assertTrue(WorkflowServer.isLoopback("127.0.0.1"));
        assertTrue(WorkflowServer.isLoopback("localhost"));
        assertFalse(WorkflowServer.isLoopback("0.0.0.0"));
    }

    @Test
    public void loopbackNeedsNoApprovers() {
        WorkflowServer.requireApproversForRemoteBind("127.0.0.1", ApproverAuth.open());
        WorkflowServer.requireApproversForRemoteBind("localhost", ApproverAuth.open());
    }

    @Test(expected = IllegalStateException.class)
    public void refusesToListenOnAllInterfacesWithoutApprovers() {
        WorkflowServer.requireApproversForRemoteBind("0.0.0.0", ApproverAuth.open());
    }

    @Test
    public void allowsAnotherAddressWhenApproversAreConfigured() {
        WorkflowServer.requireApproversForRemoteBind("0.0.0.0", APPROVERS);
    }

    @Test
    public void theServerListensOnTheLoopbackAddressByDefault() {
        Server server = WorkflowServer.createServer(0, TestInfraConfig.class);
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        assertTrue("connector host: " + connector.getHost(), WorkflowServer.isLoopback(connector.getHost()));
    }
}
