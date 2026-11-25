package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.rmi.WarehouseService;
import com.smartlogistics.common.security.InputValidator;
import com.smartlogistics.database.dao.DriverDao;
import com.smartlogistics.database.dao.StockDao;
import com.smartlogistics.database.dao.WarehouseDao;
import com.smartlogistics.server.algorithm.WarehouseOptimizer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

public class WarehouseServiceImpl extends UnicastRemoteObject implements WarehouseService {
    private static final long serialVersionUID = 1L;

    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final StockDao stockDao = new StockDao();
    private final DriverDao driverDao = new DriverDao();
    private final WarehouseOptimizer optimizer = new WarehouseOptimizer();

    public WarehouseServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public List<WarehouseDTO> getAllWarehouses() throws RemoteException {
        try {
            return warehouseDao.findAll();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture dépôts", ex);
        }
    }

    @Override
    public WarehouseDTO getWarehouseById(Long warehouseId) throws RemoteException, BusinessException {
        try {
            return warehouseDao.findById(warehouseId)
                    .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Dépôt introuvable."));
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture dépôt", ex);
        }
    }

    @Override
    public WarehouseDTO saveWarehouse(WarehouseDTO warehouse) throws RemoteException, BusinessException {
        if (warehouse == null) {
            throw new BusinessException("VALIDATION_ERROR", "Dépôt manquant.");
        }
        InputValidator.requireNonBlank(warehouse.getName(), "nom du dépôt");
        InputValidator.requireNonBlank(warehouse.getAddress(), "adresse");
        InputValidator.requireValidCoordinates(warehouse.getLatitude(), warehouse.getLongitude());
        InputValidator.requirePositiveInt(warehouse.getMaxCapacity(), "capacité maximale");

        try {
            return warehouseDao.save(warehouse);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur sauvegarde dépôt", ex);
        }
    }

    @Override
    public SmartDecisionDTO findBestWarehouse(List<OrderItemRequestDTO> items, double clientLat, double clientLng)
            throws RemoteException, BusinessException {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("EMPTY_ORDER", "La commande doit contenir au moins un produit.");
        }
        InputValidator.requireValidCoordinates(clientLat, clientLng);

        try {
            List<WarehouseDTO> warehouses = warehouseDao.findAll();
            List<StockItemDTO> stocks = stockDao.findAll();
            List<DriverDTO> drivers = driverDao.findAll();
            return optimizer.selectBestWarehouse(warehouses, stocks, drivers, items, clientLat, clientLng);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lors de l'optimisation du dépôt", ex);
        }
    }
}
