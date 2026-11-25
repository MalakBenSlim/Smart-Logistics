package com.smartlogistics.web.config;

import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.AuthException;
import io.javalin.http.Context;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contrôle d'accès par rôle (RBAC) pour les routes API et les pages Web.
 */
public final class RbacMiddleware {
    private RbacMiddleware() {}

    public static UserDTO requireAuthenticatedApi(Context ctx, SessionManager sessions) throws AuthException {
        return sessions.getCurrentUser(ctx)
                .orElseThrow(() -> new AuthException("UNAUTHENTICATED", "Session expirée ou non connectée."));
    }

    public static UserDTO requireRolesApi(Context ctx, SessionManager sessions, Role... allowed) throws AuthException {
        UserDTO user = requireAuthenticatedApi(ctx, sessions);
        Set<Role> allowedSet = Arrays.stream(allowed).collect(Collectors.toSet());
        if (!allowedSet.contains(user.getRole())) {
            throw new AuthException("FORBIDDEN", "Accès refusé pour le rôle " + user.getRole());
        }
        return user;
    }

    public static boolean guardPage(Context ctx, SessionManager sessions, Role... allowed) {
        var opt = sessions.getCurrentUser(ctx);
        if (opt.isEmpty()) {
            ctx.redirect("/login");
            return false;
        }
        Set<Role> allowedSet = Arrays.stream(allowed).collect(Collectors.toSet());
        if (!allowedSet.contains(opt.get().getRole())) {
            ctx.redirect("/403.html");
            return false;
        }
        return true;
    }
}
