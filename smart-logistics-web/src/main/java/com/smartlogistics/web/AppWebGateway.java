package com.smartlogistics.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.AuthException;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.exception.InsufficientStockException;
import com.smartlogistics.common.exception.ServiceUnavailableException;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.SessionManager;
import com.smartlogistics.web.controller.AdminController;
import com.smartlogistics.web.controller.AuthController;
import com.smartlogistics.web.controller.ClientController;
import com.smartlogistics.web.controller.DriverController;
import com.smartlogistics.web.controller.NotificationController;
import com.smartlogistics.web.controller.SseStreamController;
import com.smartlogistics.web.controller.StockController;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Passerelle Web Javalin (Port 8080) :
 * - Sert l'interface Web SaaS responsive
 * - Gère les sessions HTTP et le contrôle d'accès par rôle (RBAC)
 * - Délègue toute la logique métier via JNDI -> Java RMI
 * - Diffuse les événements JMS ActiveMQ au navigateur via SSE
 */
public class AppWebGateway {
    private static final Logger log = LoggerFactory.getLogger(AppWebGateway.class);

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getProperty("web.port", "8080"));
        start(port);
    }

    public static Javalin start(int port) {
        SessionManager sessionManager = new SessionManager();
        SseStreamController sseController = new SseStreamController(sessionManager);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));
            config.staticFiles.add("/public", Location.CLASSPATH);
        });

        // Gestion centralisée et sécurisée des erreurs
        app.exception(AuthException.class, (ex, ctx) -> {
            int status = "FORBIDDEN".equals(ex.getErrorCode()) ? 403 : 401;
            ctx.status(status).json(Map.of("errorCode", ex.getErrorCode(), "message", ex.getMessage()));
        });
        app.exception(InsufficientStockException.class, (ex, ctx) ->
                ctx.status(409).json(Map.of("errorCode", ex.getErrorCode(), "message", ex.getMessage())));
        app.exception(ServiceUnavailableException.class, (ex, ctx) ->
                ctx.status(503).json(Map.of("errorCode", ex.getErrorCode(), "message", ex.getMessage())));
        app.exception(BusinessException.class, (ex, ctx) ->
                ctx.status(400).json(Map.of("errorCode", ex.getErrorCode(), "message", ex.getMessage())));
        app.exception(Exception.class, (ex, ctx) -> {
            log.error("Erreur serveur non gérée sur {}", ctx.path(), ex);
            if (ctx.path().startsWith("/api/")) {
                ctx.status(500).json(Map.of("errorCode", "INTERNAL_ERROR", "message", "Erreur interne du serveur."));
            } else {
                ctx.status(500).html(loadResourceHtml("/public/error.html"));
            }
        });

        app.error(404, ctx -> {
            if (ctx.path().startsWith("/api/")) {
                ctx.json(Map.of("errorCode", "NOT_FOUND", "message", "Endpoint introuvable."));
            } else {
                ctx.html(loadResourceHtml("/public/404.html"));
            }
        });

        // Routes pages Web (avec redirection par rôle & protection RBAC)
        app.get("/", ctx -> {
            var opt = sessionManager.getCurrentUser(ctx);
            if (opt.isEmpty()) {
                ctx.redirect("/login");
                return;
            }
            ctx.redirect(dashboardPathFor(opt.get()));
        });
        app.get("/login", ctx -> ctx.html(loadResourceHtml("/public/login.html")));
        app.get("/register", ctx -> ctx.html(loadResourceHtml("/public/register.html")));
        app.get("/forgot-password", ctx -> ctx.html(loadResourceHtml("/public/forgot-password.html")));

        app.get("/client/dashboard", ctx -> {
            if (RbacMiddleware.guardPage(ctx, sessionManager, Role.CLIENT)) {
                ctx.html(loadResourceHtml("/public/client-dashboard.html"));
            }
        });
        app.get("/stock/dashboard", ctx -> {
            if (RbacMiddleware.guardPage(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR)) {
                ctx.html(loadResourceHtml("/public/stock-dashboard.html"));
            }
        });
        app.get("/driver/dashboard", ctx -> {
            if (RbacMiddleware.guardPage(ctx, sessionManager, Role.LIVREUR, Role.ADMINISTRATEUR)) {
                ctx.html(loadResourceHtml("/public/driver-dashboard.html"));
            }
        });
        app.get("/admin/dashboard", ctx -> {
            if (RbacMiddleware.guardPage(ctx, sessionManager, Role.ADMINISTRATEUR)) {
                ctx.html(loadResourceHtml("/public/admin-dashboard.html"));
            }
        });

        // Enregistrement des contrôleurs API + SSE
        new AuthController(sessionManager).registerRoutes(app);
        new ClientController(sessionManager).registerRoutes(app);
        new StockController(sessionManager).registerRoutes(app);
        new DriverController(sessionManager).registerRoutes(app);
        new AdminController(sessionManager).registerRoutes(app);
        new NotificationController(sessionManager).registerRoutes(app);
        app.sse("/api/stream/events", sseController::handleSse);

        sseController.startJmsBridge();
        app.start(port);
        log.info("=== Smart Logistics Web Gateway démarrée sur http://localhost:{} ===", port);
        return app;
    }

    private static String dashboardPathFor(UserDTO user) {
        return switch (user.getRole()) {
            case ADMINISTRATEUR -> "/admin/dashboard";
            case GESTIONNAIRE_STOCK -> "/stock/dashboard";
            case LIVREUR -> "/driver/dashboard";
            case CLIENT -> "/client/dashboard";
        };
    }

    private static String loadResourceHtml(String path) {
        try (InputStream is = AppWebGateway.class.getResourceAsStream(path)) {
            if (is == null) {
                return "<h1>Page introuvable</h1>";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "<h1>Erreur chargement page</h1>";
        }
    }
}
