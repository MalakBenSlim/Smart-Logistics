package com.smartlogistics.web.controller;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.AuthService;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.common.rmi.NotificationService;
import com.smartlogistics.common.rmi.OrderService;
import com.smartlogistics.common.rmi.WarehouseService;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.RmiGatewayHelper;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.Javalin;

public class AdminController {
    private final SessionManager sessionManager;

    public AdminController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static class UpdateUserRequest {
        public Role role;
        public boolean active;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/admin/stats", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    OrderService::getAdminDashboardStats));
        });

        app.get("/api/admin/health", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.NOTIFICATION_SERVICE, NotificationService.class,
                    NotificationService::getSystemHealth));
        });

        app.get("/api/admin/users", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    AuthService::getAllUsers));
        });

        app.put("/api/admin/users/{id}", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            UpdateUserRequest req = ctx.bodyAsClass(UpdateUserRequest.class);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.AUTH_SERVICE, AuthService.class,
                    svc -> svc.updateUserRoleAndStatus(id, req.role, req.active)));
        });

        app.get("/api/admin/warehouses", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class,
                    WarehouseService::getAllWarehouses));
        });

        app.post("/api/admin/warehouses", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            WarehouseDTO dto = ctx.bodyAsClass(WarehouseDTO.class);
            ctx.status(201).json(RmiGatewayHelper.call(
                    ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class,
                    svc -> svc.saveWarehouse(dto)));
        });

        app.get("/api/admin/drivers", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    DeliveryService::getAllDrivers));
        });

        app.post("/api/admin/drivers", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            DriverDTO dto = ctx.bodyAsClass(DriverDTO.class);
            ctx.status(201).json(RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> svc.saveDriver(dto)));
        });

        app.get("/api/admin/orders", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    OrderService::getAllOrders));
        });
    }
}
