package com.example.salesbatch.workflow.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Who may approve a batch. Approvers are configured as "name:token,name:token" in the system property
 * {@code approvers} (or the environment variable {@code APPROVERS}). When at least one is configured, the
 * approve endpoint needs "Authorization: Bearer TOKEN" and the approver recorded for the run is the name
 * that belongs to the token, never a name sent by the caller. When none is configured the endpoint stays
 * open, which is only safe because the server then listens on the loopback address (see WorkflowServer).
 */
@Component
public class ApproverAuth {

    static final int MIN_TOKEN_LENGTH = 16;

    private final Map<String, String> nameByToken = new LinkedHashMap<>();

    /** Reads the configuration from the system property or the environment. */
    public ApproverAuth() {
        this(configured());
    }

    public ApproverAuth(String spec) {
        if (spec == null) {
            return;
        }
        for (String entry : spec.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon == trimmed.length() - 1) {
                throw new IllegalArgumentException("approvers must look like name:token,name:token");
            }
            String name = trimmed.substring(0, colon).trim();
            String token = trimmed.substring(colon + 1).trim();
            if (token.length() < MIN_TOKEN_LENGTH) {
                throw new IllegalArgumentException("the token of approver '" + name + "' must be at least "
                        + MIN_TOKEN_LENGTH + " characters");
            }
            nameByToken.put(token, name);
        }
    }

    /** No approvers configured: the approve endpoint behaves as before. */
    public static ApproverAuth open() {
        return new ApproverAuth("");
    }

    private static String configured() {
        String value = System.getProperty("approvers");
        return value != null ? value : System.getenv("APPROVERS");
    }

    public boolean enabled() {
        return !nameByToken.isEmpty();
    }

    /** The approver's name for an "Authorization: Bearer TOKEN" header, or null when it is missing or wrong. */
    public String identify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        byte[] given = authorizationHeader.substring(7).trim().getBytes(StandardCharsets.UTF_8);
        String found = null;
        for (Map.Entry<String, String> e : nameByToken.entrySet()) {
            // Compare every token in constant time per comparison, and do not stop at the first match.
            if (MessageDigest.isEqual(e.getKey().getBytes(StandardCharsets.UTF_8), given)) {
                found = e.getValue();
            }
        }
        return found;
    }
}
