package com.smartlogistics.web.controller;

import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.NotificationService;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.RmiGatewayHelper;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class NotificationController {
    private final SessionManager sessionManager;

    public NotificationController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/notifications", ctx -> {
            UserDTO user = RbacMiddleware.requireAuthenticatedApi(ctx, sessionManager);
            List<NotificationDTO> list = RmiGatewayHelper.call(
                    ServiceLocator.NOTIFICATION_SERVICE, NotificationService.class,
                    svc -> svc.getNotificationsForUser(user.getId(), user.getRole()));
            ctx.json(list);
        });

        app.post("/api/notifications/{id}/read", ctx -> {
            RbacMiddleware.requireAuthenticatedApi(ctx, sessionManager);
            Long id = Long.parseLong(ctx.pathParam("id"));
            RmiGatewayHelper.call(
                    ServiceLocator.NOTIFICATION_SERVICE, NotificationService.class,
                    svc -> {
                        svc.markAsRead(id);
                        return true;
                    });
            ctx.json(Map.of("ok", true));
        });
    }
}
