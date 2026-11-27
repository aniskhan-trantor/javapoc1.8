package com.example.salesbatch.workflow.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApproverAuthTest {

    private static final String SPEC = "alice:alice-secret-token-1234, bob:bob-secret-token-56789:with-colon";

    @Test
    public void isOpenWhenNothingIsConfigured() {
        assertFalse(ApproverAuth.open().enabled());
        assertFalse(new ApproverAuth("").enabled());
        assertFalse(new ApproverAuth("  , ").enabled());
        assertFalse(new ApproverAuth((String) null).enabled());
    }

    @Test
    public void identifiesTheApproverByToken() {
        ApproverAuth auth = new ApproverAuth(SPEC);
        assertTrue(auth.enabled());
        assertEquals("alice", auth.identify("Bearer alice-secret-token-1234"));
        assertEquals("alice", auth.identify("bearer alice-secret-token-1234"));
        assertEquals("bob", auth.identify("Bearer bob-secret-token-56789:with-colon"));
    }

    @Test
    public void refusesMissingWrongOrMalformedCredentials() {
        ApproverAuth auth = new ApproverAuth(SPEC);
        assertNull(auth.identify(null));
        assertNull(auth.identify(""));
        assertNull(auth.identify("Bearer "));
        assertNull(auth.identify("Bearer wrong-token-0123456789"));
        assertNull(auth.identify("Basic alice-secret-token-1234"));
        assertNull(auth.identify("alice-secret-token-1234"));
        assertNull(auth.identify("Bearer alice-secret-token-123")); // a prefix of the token is not the token
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAWeakToken() {
        new ApproverAuth("alice:short");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnEntryWithoutAToken() {
        new ApproverAuth("alice");
    }
}
