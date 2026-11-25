package com.smartlogistics.web.config;

import com.smartlogistics.common.dto.UserDTO;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de sessions HTTP côté passerelle avec cookie HttpOnly.
 */
public class SessionManager {
    public static final String SESSION_COOKIE = "SL_SESSION";
    private static final long TTL_SECONDS = 8 * 3600;

    private static class SessionEntry {
        final UserDTO user;
        Instant expiresAt;

        SessionEntry(UserDTO user, Instant expiresAt) {
            this.user = user;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public String createSession(Context ctx, UserDTO user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionEntry(user, Instant.now().plusSeconds(TTL_SECONDS)));
        ctx.cookie(SESSION_COOKIE, token, (int) TTL_SECONDS);
        return token;
    }

    public Optional<UserDTO> getCurrentUser(Context ctx) {
        String token = ctx.cookie(SESSION_COOKIE);
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionEntry entry = sessions.get(token);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            sessions.remove(token);
            return Optional.empty();
        }
        entry.expiresAt = Instant.now().plusSeconds(TTL_SECONDS);
        return Optional.of(entry.user);
    }

    public void destroySession(Context ctx) {
        String token = ctx.cookie(SESSION_COOKIE);
        if (token != null) {
            sessions.remove(token);
        }
        ctx.removeCookie(SESSION_COOKIE);
    }
}
