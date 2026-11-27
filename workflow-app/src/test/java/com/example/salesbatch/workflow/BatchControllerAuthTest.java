package com.example.salesbatch.workflow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.salesbatch.workflow.config.AppConfig;
import com.example.salesbatch.workflow.service.BatchService;
import com.example.salesbatch.workflow.web.ApproverAuth;
import com.example.salesbatch.workflow.web.BatchController;

/** Approver authentication and the limit on which server files a batch may read. */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AppConfig.class, TestInfraConfig.class})
public class BatchControllerAuthTest {

    private static final String ALICE_TOKEN = "alice-secret-token-1234";

    @BeforeClass
    public static void reportDirectory() throws Exception {
        System.setProperty("report.dir", Files.createDirectories(
                java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "sales-batch-test-reports")).toString());
    }

    @Autowired BatchService batchService;
    private Path csv;

    @Before
    public void setUp() throws Exception {
        TestInfraConfig.failSpark = false;
        csv = Files.createTempFile("sales-auth", ".csv");
        Files.write(csv, Collections.singletonList("A1,P1,EU,3,5.00"), StandardCharsets.UTF_8);
    }

    @After
    public void clearInputRoot() {
        System.clearProperty("batch.input.dir");
    }

    private MockMvc mvcWithApprovers() {
        return MockMvcBuilders.standaloneSetup(new BatchController(batchService, new ApproverAuth("alice:" + ALICE_TOKEN))).build();
    }

    private static String startBody(Path file, String date) {
        return "{\"csvPath\":\"" + file.toString().replace("\\", "\\\\") + "\",\"businessDate\":\"" + date + "\"}";
    }

    @Test
    public void approveNeedsAValidTokenWhenApproversAreConfigured() throws Exception {
        MockMvc mvc = mvcWithApprovers();
        String id = batchService.start(csv.toString(), "20261130");
        Await.status(batchService, id, "WAITING_APPROVAL");

        mvc.perform(post("/batches/" + id + "/approve").param("approver", "mallory"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(post("/batches/" + id + "/approve").header("Authorization", "Bearer not-the-right-token-1"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/batches/" + id)).andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));

        mvc.perform(post("/batches/" + id + "/approve").param("approver", "mallory")
                .header("Authorization", "Bearer " + ALICE_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    public void approveStaysOpenWhenNoApproverIsConfigured() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BatchController(batchService, ApproverAuth.open())).build();
        String id = batchService.start(csv.toString(), "20261201");
        Await.status(batchService, id, "WAITING_APPROVAL");
        mvc.perform(post("/batches/" + id + "/approve").param("approver", "carol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    public void aBatchMayOnlyReadFilesInsideTheInputFolder() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BatchController(batchService)).build();
        Path root = Files.createTempDirectory("input-root");
        System.setProperty("batch.input.dir", root.toString());

        Path inside = Files.createTempFile(root, "ok", ".csv");
        Files.write(inside, Collections.singletonList("B1,P2,EU,1,2.00"), StandardCharsets.UTF_8);
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content(startBody(inside, "20261202")))
                .andExpect(status().isAccepted());

        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content(startBody(csv, "20261203")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.CoreMatchers.containsString("must be inside")));

        Path traversal = root.resolve("..").resolve(csv.getFileName());
        mvc.perform(post("/batches").contentType(MediaType.APPLICATION_JSON).content(startBody(traversal, "20261204")))
                .andExpect(status().isBadRequest());
    }
}
