package com.example.salesbatch.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * HBaseSalesRepository against a REAL HBase (the existing HBaseSalesRepositoryTest uses mocks). Skipped when no
 * HBase answers on localhost, like SalesAggregatorHBaseTest; scripts/integration-test.sh starts one and fails if
 * these tests were skipped. Uses far-future dates and its own audit id, and deletes what it wrote.
 */
public class HBaseSalesRepositoryRealTest {

    private static final String DATE = "20990103";

    private HBaseSalesRepository repo;
    private String auditId;

    /** ZooKeeper, the HBase master and the region server must all answer (ZooKeeper alone can outlive a dead HBase). */
    private static boolean hbaseReachable() {
        for (int port : new int[] {2181, 16000, 16020}) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", port), 500);
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue("HBase is not running on localhost (2181, 16000, 16020), skipping", hbaseReachable());
        repo = new HBaseSalesRepository();
        repo.initSchema();
        auditId = "it-" + System.nanoTime();
        cleanUp();
    }

    @After
    public void tearDown() throws Exception {
        if (repo != null) {
            cleanUp();
            repo.close();
        }
    }

    private void delete(String tableName, String row) throws Exception {
        try (Table table = repo.connection().getTable(TableName.valueOf(tableName))) {
            table.delete(new Delete(Bytes.toBytes(row)));
        }
    }

    private void cleanUp() throws Exception {
        for (SalesRecord r : repo.loadRaw(DATE)) {
            delete(SalesSchema.RAW_TABLE, RowKeys.raw(r.getDate(), r.getOrderId()));
        }
        delete(SalesSchema.AUDIT_TABLE, auditId);
    }

    @Test(timeout = 120000)
    public void rawRowsRoundTripThroughHBaseAndAreSeparatedByDate() {
        repo.saveRaw(Arrays.asList(
                new SalesRecord(DATE, "R1", "P1", "EU", 2, new BigDecimal("10.50")),
                new SalesRecord(DATE, "R2", "P2", "US", 1, new BigDecimal("4.00")),
                new SalesRecord("20990104", "R3", "P9", "APAC", 7, new BigDecimal("70.00"))));
        try {
            List<SalesRecord> loaded = repo.loadRaw(DATE);
            assertEquals(2, loaded.size());
            SalesRecord r1 = loaded.stream().filter(r -> "R1".equals(r.getOrderId())).findFirst().get();
            assertEquals("EU", r1.getRegion());
            assertEquals("P1", r1.getProductId());
            assertEquals(2, r1.getQty());
            assertEquals(new BigDecimal("10.50"), r1.getAmount());
            assertEquals("the other date must not be returned", 0, repo.loadAgg(DATE).size());
        } finally {
            try {
                delete(SalesSchema.RAW_TABLE, RowKeys.raw("20990104", "R3"));
            } catch (Exception ignored) {
                // best effort: the row has a far-future date and disturbs nothing
            }
        }
    }

    @Test(timeout = 120000)
    public void auditStatusApproverAndErrorAreStoredAndReadBack() {
        assertNull(repo.auditStatus(auditId));

        repo.updateAudit(auditId, "RUNNING", null);
        assertEquals("RUNNING", repo.auditStatus(auditId));

        repo.updateAudit(auditId, "WAITING_APPROVAL", null);
        assertEquals("WAITING_APPROVAL", repo.auditStatus(auditId));

        assertNull(repo.auditError(auditId));
        repo.recordError(auditId, "bad number on line 2");
        assertEquals("bad number on line 2", repo.auditError(auditId));

        repo.updateAudit(auditId, "FAILED", "carol");
        assertEquals("FAILED", repo.auditStatus(auditId));
        assertEquals("the error survives the status change", "bad number on line 2", repo.auditError(auditId));
    }
}
