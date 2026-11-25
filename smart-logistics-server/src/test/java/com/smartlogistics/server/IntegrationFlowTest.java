package com.smartlogistics.server;

import com.smartlogistics.common.dto.CreateOrderRequestDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.enums.OrderStatus;
import com.smartlogistics.common.enums.PaymentMethod;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.AuthService;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.common.rmi.OrderService;
import com.smartlogistics.common.rmi.StockService;
import com.smartlogistics.common.rmi.WarehouseService;
import com.smartlogistics.database.config.DataSourceFactory;
import com.smartlogistics.database.config.SchemaInitializer;
import com.smartlogistics.database.seed.DemoDataSeeder;
import com.smartlogistics.server.rmi.impl.AuthServiceImpl;
import com.smartlogistics.server.rmi.impl.DeliveryServiceImpl;
import com.smartlogistics.server.rmi.impl.OrderServiceImpl;
import com.smartlogistics.server.rmi.impl.StockServiceImpl;
import com.smartlogistics.server.rmi.impl.WarehouseServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationFlowTest {

    private static AuthServiceImpl authSvc;
    private static WarehouseServiceImpl whSvc;
    private static StockServiceImpl stockSvc;
    private static OrderServiceImpl orderSvc;
    private static DeliveryServiceImpl delSvc;

    @BeforeAll
    static void setupCluster() throws Exception {
        System.setProperty("java.naming.provider.url", "rmi://localhost:11099");
        LocateRegistry.createRegistry(11099);

        DataSourceFactory.configureCustom(
                "jdbc:h2:mem:sl_test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
                "sa", "");
        SchemaInitializer.initialize();
        new DemoDataSeeder().seedIfEmpty();

        authSvc = new AuthServiceImpl();
        whSvc = new WarehouseServiceImpl();
        stockSvc = new StockServiceImpl();
        orderSvc = new OrderServiceImpl();
        delSvc = new DeliveryServiceImpl();

        ServiceLocator.bindRmiService(ServiceLocator.AUTH_SERVICE, authSvc);
        ServiceLocator.bindRmiService(ServiceLocator.WAREHOUSE_SERVICE, whSvc);
        ServiceLocator.bindRmiService(ServiceLocator.STOCK_SERVICE, stockSvc);
        ServiceLocator.bindRmiService(ServiceLocator.ORDER_SERVICE, orderSvc);
        ServiceLocator.bindRmiService(ServiceLocator.DELIVERY_SERVICE, delSvc);
    }

    @AfterAll
    static void tearDown() {
        DataSourceFactory.close();
        System.clearProperty("java.naming.provider.url");
    }

    @Test
    void endToEndOrderFallsBackFromTunisWhenMouseOutOfStock() throws Exception {
        AuthService auth = ServiceLocator.getRmiService(ServiceLocator.AUTH_SERVICE, AuthService.class);
        var login = auth.login("client@smartlogistics.tn", "Client123!", "127.0.0.1");
        assertTrue(login.isSuccess());
        assertEquals(Role.CLIENT, login.getUser().getRole());

        OrderService orders = ServiceLocator.getRmiService(ServiceLocator.ORDER_SERVICE, OrderService.class);

        CreateOrderRequestDTO req = new CreateOrderRequestDTO();
        req.setClientId(login.getUser().getId());
        req.setDeliveryAddress("Lac 2, Tunis");
        req.setDeliveryLat(36.8435);
        req.setDeliveryLng(10.2682);
        req.setPaymentMethod(PaymentMethod.CARTE_SIMULEE);
        req.setSimulatedCardNumber("4111222233334444");
        // Produit 1 (Laptop) + Produit 2 (Souris en rupture à Tunis !)
        req.setItems(List.of(new OrderItemRequestDTO(1L, 1), new OrderItemRequestDTO(2L, 1)));

        OrderDTO created = orders.createOrder(req);
        assertNotNull(created.getId());
        assertEquals(OrderStatus.CREATED, created.getStatus());
        assertNotEquals("Dépôt Tunis", created.getWarehouseName());
        assertNotNull(created.getSmartDecision());
        assertTrue(created.getSmartDecision().getWarehouseScore() > 50);

        // Préparation -> affectation automatique du meilleur livreur
        OrderDTO prepared = orders.markOrderPrepared(created.getId());
        assertEquals(OrderStatus.DRIVER_ASSIGNED, prepared.getStatus());
        assertNotNull(prepared.getDelivery());
    }
}
