package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.dto.SystemHealthDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.NotificationService;
import com.smartlogistics.database.config.DataSourceFactory;
import com.smartlogistics.database.dao.NotificationDao;
import com.smartlogistics.jms.broker.EmbeddedJmsBroker;
import com.smartlogistics.socket.server.DeliveryTcpServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NotificationServiceImpl extends UnicastRemoteObject implements NotificationService {
    private static final long serialVersionUID = 1L;

    private final NotificationDao notificationDao = new NotificationDao();
    private transient DeliveryTcpServer tcpServerRef;

    public NotificationServiceImpl() throws RemoteException {
        super();
    }

    public void setTcpServerRef(DeliveryTcpServer tcpServerRef) {
        this.tcpServerRef = tcpServerRef;
    }

    @Override
    public List<NotificationDTO> getNotificationsForUser(Long userId, Role role) throws RemoteException {
        try {
            return notificationDao.findForUserOrRole(userId, role, 30);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture notifications", ex);
        }
    }

    @Override
    public void markAsRead(Long notificationId) throws RemoteException {
        try {
            notificationDao.markAsRead(notificationId);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur marquage notification lue", ex);
        }
    }

    @Override
    public SystemHealthDTO getSystemHealth() throws RemoteException {
        SystemHealthDTO health = new SystemHealthDTO();
        health.setCheckedAt(LocalDateTime.now());
        health.setDatabaseOnline(DataSourceFactory.isDatabaseReachable());
        health.setJmsBrokerOnline(EmbeddedJmsBroker.isReachable());
        health.setTcpSocketOnline(tcpServerRef != null ? tcpServerRef.isRunning() : DeliveryTcpServer.isReachable());
        health.setActiveDriverSockets(tcpServerRef != null ? tcpServerRef.getActiveDriverCount() : 0);

        Map<String, Boolean> services = new LinkedHashMap<>();
        services.put("AuthService", ServiceLocator.isRmiServiceBound(ServiceLocator.AUTH_SERVICE));
        services.put("OrderService", ServiceLocator.isRmiServiceBound(ServiceLocator.ORDER_SERVICE));
        services.put("WarehouseService", ServiceLocator.isRmiServiceBound(ServiceLocator.WAREHOUSE_SERVICE));
        services.put("StockService", ServiceLocator.isRmiServiceBound(ServiceLocator.STOCK_SERVICE));
        services.put("DeliveryService", ServiceLocator.isRmiServiceBound(ServiceLocator.DELIVERY_SERVICE));
        services.put("NotificationService", ServiceLocator.isRmiServiceBound(ServiceLocator.NOTIFICATION_SERVICE));
        health.setServiceStatuses(services);
        health.setRmiRegistryOnline(services.values().stream().anyMatch(Boolean::booleanValue));

        return health;
    }
}
