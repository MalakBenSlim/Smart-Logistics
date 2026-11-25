package com.smartlogistics.web.controller;

import com.smartlogistics.common.dto.AuthResultDTO;
import com.smartlogistics.common.dto.RegisterRequestDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.AuthService;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.RmiGatewayHelper;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.Javalin;

import java.util.Map;

public class AuthController {
    private final SessionManager sessionManager;

    public AuthController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/auth/login", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String email = String.valueOf(body.get("email"));
            String password = String.valueOf(body.get("password"));
            String clientIp = ctx.ip();

            AuthResultDTO result = RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    svc -> svc.login(email, password, clientIp));

            sessionManager.createSession(ctx, result.getUser());
            ctx.json(result);
        });

        app.post("/api/auth/register", ctx -> {
            RegisterRequestDTO req = ctx.bodyAsClass(RegisterRequestDTO.class);
            UserDTO created = RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    svc -> svc.registerClient(req));
            ctx.status(201).json(created);
        });

        app.post("/api/auth/forgot-password", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String email = String.valueOf(body.get("email"));
            String token = RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    svc -> svc.requestPasswordReset(email));
            ctx.json(Map.of(
                    "message", "Un jeton temporaire de réinitialisation a été généré (simulation).",
                    "simulatedResetToken", token
            ));
        });

        app.post("/api/auth/reset-password", ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String token = String.valueOf(body.get("token"));
            String newPassword = String.valueOf(body.get("newPassword"));
            boolean ok = RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    svc -> svc.resetPassword(token, newPassword));
            ctx.json(Map.of("success", ok, "message", "Mot de passe réinitialisé avec succès."));
        });

        app.post("/api/auth/logout", ctx -> {
            sessionManager.destroySession(ctx);
            ctx.json(Map.of("success", true));
        });

        app.get("/api/auth/me", ctx -> {
            UserDTO user = RbacMiddleware.requireAuthenticatedApi(ctx, sessionManager);
            ctx.json(user);
        });
    }
}
