package com.example.salesbatch.workflow.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.example.salesbatch.workflow.InMemorySalesRepository;

/**
 * The run's audit status becomes WAITING_APPROVAL before the engine transaction that creates the approval task
 * has committed, so a client that sees the status and approves at once can find no task yet. These tests stand in
 * for the task service and make the task appear after a number of queries, so they do not depend on timing.
 */
public class BatchServiceApproveRaceTest {

    private static final String RUN = "run-1";

    private InMemorySalesRepository repository;
    private final AtomicInteger queries = new AtomicInteger();
    private final List<String> completed = new ArrayList<>();
    private final List<Map<String, Object>> completedVars = new ArrayList<>();
    private int emptyAnswersBeforeTheTask;

    @Before
    public void setUp() {
        repository = new InMemorySalesRepository();
        System.setProperty("approval.task.wait.ms", "300");
    }

    @After
    public void clear() {
        System.clearProperty("approval.task.wait.ms");
    }

    private BatchService service() {
        Task task = (Task) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Task.class},
                (proxy, method, args) -> "getId".equals(method.getName()) ? "task-1" : null);
        TaskQuery query = (TaskQuery) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {TaskQuery.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("list".equals(method.getName())) {
                            return queries.incrementAndGet() > emptyAnswersBeforeTheTask
                                    ? Collections.singletonList(task) : Collections.emptyList();
                        }
                        return proxy; // processInstanceId(..), taskDefinitionKey(..)
                    }
                });
        TaskService taskService = (TaskService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {TaskService.class},
                (proxy, method, args) -> {
                    if ("createTaskQuery".equals(method.getName())) {
                        return query;
                    }
                    if ("complete".equals(method.getName())) {
                        completed.add((String) args[0]);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> vars = (Map<String, Object>) args[1];
                        completedVars.add(vars);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new BatchService(null, taskService, repository);
    }

    @Test
    public void approveWaitsForTheTaskToBeCommittedWhileTheRunIsWaitingForApproval() {
        repository.updateAudit(RUN, "WAITING_APPROVAL", null);
        emptyAnswersBeforeTheTask = 3;

        service().approve(RUN, true, "carol");

        assertEquals(Collections.singletonList("task-1"), completed);
        assertEquals("carol", completedVars.get(0).get("approver"));
        assertEquals(Boolean.TRUE, completedVars.get(0).get("approved"));
        assertTrue("it asked again until the task was there", queries.get() >= 4);
    }

    @Test
    public void approveFailsAtOnceWhenTheRunIsNotWaitingForApproval() {
        repository.updateAudit(RUN, "COMPLETED", "carol"); // already approved: the second approve must be a conflict
        emptyAnswersBeforeTheTask = Integer.MAX_VALUE;

        long start = System.currentTimeMillis();
        try {
            service().approve(RUN, true, "dave");
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("No pending approval"));
        }
        assertEquals("no waiting when the run is not waiting for approval", 1, queries.get());
        assertTrue(System.currentTimeMillis() - start < 250);
    }

    @Test
    public void approveGivesUpWhenTheTaskNeverAppears() {
        repository.updateAudit(RUN, "WAITING_APPROVAL", null);
        emptyAnswersBeforeTheTask = Integer.MAX_VALUE;

        try {
            service().approve(RUN, true, "dave");
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("No pending approval"));
        }
        assertTrue(completed.isEmpty());
        assertTrue("it did wait and ask again", queries.get() > 2);
    }
}
