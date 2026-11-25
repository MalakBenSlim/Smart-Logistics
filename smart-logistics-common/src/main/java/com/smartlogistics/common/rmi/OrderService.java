package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.CreateOrderRequestDTO;
import com.smartlogistics.common.dto.DashboardStatsDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.BusinessException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant orchestrant le cycle de vie des commandes et les statistiques.
 */
public interface OrderService extends Remote {
    OrderDTO createOrder(CreateOrderRequestDTO request) throws RemoteException, BusinessException;

    OrderDTO getOrderById(Long orderId, Long requesterId, Role role) throws RemoteException, BusinessException;

    List<OrderDTO> getOrdersByClient(Long clientId) throws RemoteException;

    List<OrderDTO> getOrdersByWarehouse(Long warehouseId) throws RemoteException;

    List<OrderDTO> getAllOrders() throws RemoteException;

    boolean cancelOrder(Long orderId, Long requesterId, Role role) throws RemoteException, BusinessException;

    OrderDTO markOrderPrepared(Long orderId) throws RemoteException, BusinessException;

    OrderDTO shipOrder(Long orderId) throws RemoteException, BusinessException;

    double calculateDeliveryCost(double distanceKm) throws RemoteException;

    DashboardStatsDTO getAdminDashboardStats() throws RemoteException;
}
