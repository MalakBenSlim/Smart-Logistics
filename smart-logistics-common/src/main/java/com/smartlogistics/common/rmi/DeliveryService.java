package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.DeliveryDTO;
import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.enums.DeliveryStatus;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.exception.BusinessException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant de gestion des livreurs, d'affectation intelligente et de suivi GPS.
 */
public interface DeliveryService extends Remote {
    List<DriverDTO> getAllDrivers() throws RemoteException;

    DriverDTO getDriverByUserId(Long userId) throws RemoteException, BusinessException;

    DriverDTO saveDriver(DriverDTO driver) throws RemoteException, BusinessException;

    DriverDTO updateDriverStatus(Long driverId, DriverStatus status) throws RemoteException, BusinessException;

    SmartDecisionDTO assignBestDriver(Long orderId) throws RemoteException, BusinessException;

    List<DeliveryDTO> getDeliveriesByDriver(Long driverId) throws RemoteException;

    DeliveryDTO getDeliveryByOrderId(Long orderId) throws RemoteException;

    void updateDriverPosition(Long driverId, Long deliveryId, double lat, double lng)
            throws RemoteException, BusinessException;

    DeliveryDTO updateDeliveryStatus(Long deliveryId, DeliveryStatus status)
            throws RemoteException, BusinessException;
}
