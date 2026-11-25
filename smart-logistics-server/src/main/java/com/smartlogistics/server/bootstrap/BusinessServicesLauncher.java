package com.smartlogistics.server.bootstrap;

import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.server.jms.JmsNotificationBridge;
import com.smartlogistics.server.rmi.impl.AuthServiceImpl;
import com.smartlogistics.server.rmi.impl.DeliveryServiceImpl;
import com.smartlogistics.server.rmi.impl.NotificationServiceImpl;
import com.smartlogistics.server.rmi.impl.OrderServiceImpl;
import com.smartlogistics.server.rmi.impl.StockServiceImpl;
import com.smartlogistics.server.rmi.impl.WarehouseServiceImpl;
import com.smartlogistics.socket.server.DeliveryTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Démarre les services métier distribués :
 * - Enregistrement JNDI des 6 services RMI
 * - Pont JMS -> Persistance Notifications
 * - Serveur Socket TCP multithreadé pour les livreurs
 */
public class BusinessServicesLauncher {
    private static final Logger log = LoggerFactory.getLogger(BusinessServicesLauncher.class);

    // Conserver des références fortes pour éviter le GC des objets RMI exportés
    private static AuthServiceImpl authService;
    private static WarehouseServiceImpl warehouseService;
    private static StockServiceImpl stockService;
    private static OrderServiceImpl orderService;
    private static DeliveryServiceImpl deliveryService;
    private static NotificationServiceImpl notificationService;
    private static DeliveryTcpServer tcpServer;
    private static JmsNotificationBridge jmsBridge;

    public static void startServices() throws Exception {
        tcpServer = new DeliveryTcpServer();
        tcpServer.startAsync();

        authService = new AuthServiceImpl();
        warehouseService = new WarehouseServiceImpl();
        stockService = new StockServiceImpl();
        orderService = new OrderServiceImpl();
        deliveryService = new DeliveryServiceImpl();
        notificationService = new NotificationServiceImpl();
        notificationService.setTcpServerRef(tcpServer);

        ServiceLocator.bindRmiService(ServiceLocator.AUTH_SERVICE, authService);
        ServiceLocator.bindRmiService(ServiceLocator.WAREHOUSE_SERVICE, warehouseService);
        ServiceLocator.bindRmiService(ServiceLocator.STOCK_SERVICE, stockService);
        ServiceLocator.bindRmiService(ServiceLocator.ORDER_SERVICE, orderService);
        ServiceLocator.bindRmiService(ServiceLocator.DELIVERY_SERVICE, deliveryService);
        ServiceLocator.bindRmiService(ServiceLocator.NOTIFICATION_SERVICE, notificationService);

        jmsBridge = new JmsNotificationBridge();
        jmsBridge.start();

        log.info("=== Tous les services distribués RMI + JMS + TCP sont EN LIGNE ===");
    }

    public static void main(String[] args) throws Exception {
        startServices();
        new CountDownLatch(1).await();
    }
}
