package com.smartlogistics.web.controller;

import com.smartlogistics.common.dto.CreateOrderRequestDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.OrderService;
import com.smartlogistics.common.rmi.StockService;
import com.smartlogistics.common.rmi.WarehouseService;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.RmiGatewayHelper;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class ClientController {
    private final SessionManager sessionManager;

    public ClientController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static class OptimizePreviewRequest {
        public double lat;
        public double lng;
        public List<OrderItemRequestDTO> items;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/client/products", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT, Role.ADMINISTRATEUR);
            List<ProductDTO> products = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.getAllProducts(false));
            ctx.json(products);
        });

        app.get("/api/client/warehouses", ctx -> {
            RbacMiddleware.requireAuthenticatedApi(ctx, sessionManager);
            List<WarehouseDTO> list = RmiGatewayHelper.call(
                    ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class,
                    WarehouseService::getAllWarehouses);
            ctx.json(list);
        });

        app.post("/api/client/optimize-preview", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT, Role.ADMINISTRATEUR);
            OptimizePreviewRequest req = ctx.bodyAsClass(OptimizePreviewRequest.class);
            SmartDecisionDTO decision = RmiGatewayHelper.call(
                    ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class,
                    svc -> svc.findBestWarehouse(req.items, req.lat, req.lng));
            ctx.json(decision);
        });

        app.post("/api/client/orders", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT);
            CreateOrderRequestDTO req = ctx.bodyAsClass(CreateOrderRequestDTO.class);
            req.setClientId(user.getId());
            OrderDTO created = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.createOrder(req));
            ctx.status(201).json(created);
        });

        app.get("/api/client/orders", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT);
            List<OrderDTO> orders = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.getOrdersByClient(user.getId()));
            ctx.json(orders);
        });

        app.get("/api/client/orders/{id}", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            OrderDTO order = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.getOrderById(id, user.getId(), user.getRole()));
            ctx.json(order);
        });

        app.post("/api/client/orders/{id}/cancel", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.CLIENT, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            boolean ok = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.cancelOrder(id, user.getId(), user.getRole()));
            ctx.json(Map.of("cancelled", ok));
        });
    }
}
