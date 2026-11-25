package com.smartlogistics.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {

    @Test
    void blocksAfterFiveFailuresAndResetsOnSuccess() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter();
        String key = "client@smartlogistics.tn";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
            assertFalse(limiter.isBlocked(key));
        }

        limiter.recordFailure(key);
        assertTrue(limiter.isBlocked(key));
        assertTrue(limiter.getRemainingLockSeconds(key) > 0);

        limiter.recordSuccess(key);
        assertFalse(limiter.isBlocked(key));
        assertEquals(5, limiter.getRemainingAttempts(key));
    }
}
