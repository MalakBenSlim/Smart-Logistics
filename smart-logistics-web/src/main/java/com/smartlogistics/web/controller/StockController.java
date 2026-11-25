package com.smartlogistics.web.controller;

import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.StockItemDTO;
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

public class StockController {
    private final SessionManager sessionManager;

    public StockController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static class AdjustStockRequest {
        public Long warehouseId;
        public Long productId;
        public int quantity;
    }

    public static class TransferStockRequest {
        public Long fromWarehouseId;
        public Long toWarehouseId;
        public Long productId;
        public int quantity;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/stock/warehouses", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class,
                    WarehouseService::getAllWarehouses));
        });

        app.get("/api/stock/products", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            ctx.json(RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.getAllProducts(false)));
        });

        app.post("/api/stock/products", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            ProductDTO dto = ctx.bodyAsClass(ProductDTO.class);
            dto.setActive(true);
            ProductDTO saved = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.saveProduct(dto));
            ctx.status(201).json(saved);
        });

        app.delete("/api/stock/products/{id}", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            boolean deleted = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.deleteProduct(id));
            ctx.json(Map.of("deleted", deleted));
        });

        app.get("/api/stock/inventory", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            String whParam = ctx.queryParam("warehouseId");
            List<StockItemDTO> stocks = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> whParam != null && !whParam.isBlank()
                            ? svc.getStockByWarehouse(Long.parseLong(whParam))
                            : svc.getAllStocks());
            ctx.json(stocks);
        });

        app.get("/api/stock/alerts", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            List<StockItemDTO> low = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class, StockService::getLowStockItems);
            List<StockItemDTO> out = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class, StockService::getOutOfStockItems);
            ctx.json(Map.of("lowStock", low, "outOfStock", out));
        });

        app.put("/api/stock/adjust", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            AdjustStockRequest req = ctx.bodyAsClass(AdjustStockRequest.class);
            StockItemDTO updated = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.updateStock(req.warehouseId, req.productId, req.quantity));
            ctx.json(updated);
        });

        app.post("/api/stock/transfer", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            TransferStockRequest req = ctx.bodyAsClass(TransferStockRequest.class);
            boolean ok = RmiGatewayHelper.call(
                    ServiceLocator.STOCK_SERVICE, StockService.class,
                    svc -> svc.transferStock(req.fromWarehouseId, req.toWarehouseId, req.productId, req.quantity));
            ctx.json(Map.of("transferred", ok));
        });

        app.get("/api/stock/orders", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            List<OrderDTO> orders = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class, OrderService::getAllOrders);
            ctx.json(orders);
        });

        app.post("/api/stock/orders/{id}/prepare", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            OrderDTO updated = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.markOrderPrepared(id));
            ctx.json(updated);
        });

        app.post("/api/stock/orders/{id}/ship", ctx -> {
            RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.GESTIONNAIRE_STOCK, Role.ADMINISTRATEUR);
            Long id = Long.parseLong(ctx.pathParam("id"));
            OrderDTO updated = RmiGatewayHelper.call(
                    ServiceLocator.ORDER_SERVICE, OrderService.class,
                    svc -> svc.shipOrder(id));
            ctx.json(updated);
        });
    }
}
