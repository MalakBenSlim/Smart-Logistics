package com.smartlogistics.server.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protection anti brute-force thread-safe : bloque temporairement après 5 échecs consécutifs.
 */
public class LoginAttemptLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 120;

    private static class AttemptState {
        int failures;
        Instant lockedUntil;
    }

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptState state = attempts.get(normalize(key));
        if (state == null || state.lockedUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(state.lockedUntil)) {
            attempts.remove(normalize(key));
            return false;
        }
        return true;
    }

    public long getRemainingLockSeconds(String key) {
        AttemptState state = attempts.get(normalize(key));
        if (state == null || state.lockedUntil == null) {
            return 0;
        }
        long seconds = state.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, seconds);
    }

    public void recordFailure(String key) {
        String k = normalize(key);
        attempts.compute(k, (ignored, current) -> {
            AttemptState st = current != null ? current : new AttemptState();
            st.failures++;
            if (st.failures >= MAX_ATTEMPTS) {
                st.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_SECONDS);
            }
            return st;
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(normalize(key));
    }

    public int getRemainingAttempts(String key) {
        AttemptState state = attempts.get(normalize(key));
        if (state == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - state.failures);
    }

    private String normalize(String key) {
        return key == null ? "unknown" : key.trim().toLowerCase();
    }
}
