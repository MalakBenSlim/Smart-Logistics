package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.exception.BusinessException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant de gestion des dépôts et d'exécution du Smart Warehouse Optimizer.
 */
public interface WarehouseService extends Remote {
    List<WarehouseDTO> getAllWarehouses() throws RemoteException;

    WarehouseDTO getWarehouseById(Long warehouseId) throws RemoteException, BusinessException;

    WarehouseDTO saveWarehouse(WarehouseDTO warehouse) throws RemoteException, BusinessException;

    SmartDecisionDTO findBestWarehouse(List<OrderItemRequestDTO> items, double clientLat, double clientLng)
            throws RemoteException, BusinessException;
}
