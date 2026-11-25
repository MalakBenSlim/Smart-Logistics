package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.exception.InsufficientStockException;
import com.smartlogistics.common.rmi.StockService;
import com.smartlogistics.common.security.InputValidator;
import com.smartlogistics.database.dao.ProductDao;
import com.smartlogistics.database.dao.StockDao;
import com.smartlogistics.database.dao.WarehouseDao;
import com.smartlogistics.jms.event.LogisticsEvent;
import com.smartlogistics.jms.producer.JmsEventPublisher;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

public class StockServiceImpl extends UnicastRemoteObject implements StockService {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();
    private final StockDao stockDao = new StockDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final JmsEventPublisher eventPublisher = new JmsEventPublisher();

    public StockServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public List<ProductDTO> getAllProducts(boolean includeInactive) throws RemoteException {
        try {
            return productDao.findAll(includeInactive);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture produits", ex);
        }
    }

    @Override
    public ProductDTO saveProduct(ProductDTO product) throws RemoteException, BusinessException {
        if (product == null) {
            throw new BusinessException("VALIDATION_ERROR", "Produit requis.");
        }
        InputValidator.requireNonBlank(product.getSku(), "SKU");
        InputValidator.requireNonBlank(product.getName(), "nom du produit");
        InputValidator.requireNonBlank(product.getCategory(), "catégorie");
        InputValidator.requireNonNegativeDouble(product.getUnitPrice(), "prix unitaire");

        try {
            boolean isNew = product.getId() == null;
            ProductDTO saved = productDao.save(product);
            if (isNew) {
                // Initialiser des entrées de stock à 0 dans tous les dépôts existants
                for (var wh : warehouseDao.findAll()) {
                    stockDao.upsertQuantity(wh.getId(), saved.getId(), 0);
                }
            }
            return saved;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur sauvegarde produit", ex);
        }
    }

    @Override
    public boolean deleteProduct(Long productId) throws RemoteException, BusinessException {
        try {
            if (!productDao.softDelete(productId)) {
                throw new BusinessException("PRODUCT_NOT_FOUND", "Produit introuvable.");
            }
            return true;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur suppression produit", ex);
        }
    }

    @Override
    public List<StockItemDTO> getStockByWarehouse(Long warehouseId) throws RemoteException {
        try {
            return stockDao.findByWarehouse(warehouseId);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture stock dépôt", ex);
        }
    }

    @Override
    public List<StockItemDTO> getAllStocks() throws RemoteException {
        try {
            return stockDao.findAll();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture tous les stocks", ex);
        }
    }

    @Override
    public List<StockItemDTO> getLowStockItems() throws RemoteException {
        try {
            return stockDao.findLowStock();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture stocks critiques", ex);
        }
    }

    @Override
    public List<StockItemDTO> getOutOfStockItems() throws RemoteException {
        try {
            return stockDao.findOutOfStock();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture ruptures de stock", ex);
        }
    }

    @Override
    public StockItemDTO updateStock(Long warehouseId, Long productId, int newQuantity)
            throws RemoteException, BusinessException {
        if (newQuantity < 0) {
            throw new BusinessException("VALIDATION_ERROR", "La quantité ne peut pas être négative.");
        }
        try {
            StockItemDTO updated = stockDao.upsertQuantity(warehouseId, productId, newQuantity);
            checkAndPublishLowStock(updated);
            return updated;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur mise à jour stock", ex);
        }
    }

    @Override
    public boolean transferStock(Long fromWarehouseId, Long toWarehouseId, Long productId, int quantity)
            throws RemoteException, BusinessException {
        if (fromWarehouseId == null || toWarehouseId == null || fromWarehouseId.equals(toWarehouseId)) {
            throw new BusinessException("VALIDATION_ERROR", "Les dépôts source et destination doivent être différents.");
        }
        InputValidator.requirePositiveInt(quantity, "quantité à transférer");

        try {
            boolean ok = stockDao.transferStockAtomic(fromWarehouseId, toWarehouseId, productId, quantity);
            if (!ok) {
                throw new InsufficientStockException("Stock disponible insuffisant dans le dépôt source pour ce transfert.");
            }
            stockDao.findByWarehouseAndProduct(fromWarehouseId, productId).ifPresent(this::checkAndPublishLowStock);
            return true;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur transfert de stock", ex);
        }
    }

    @Override
    public synchronized boolean reserveStockForOrder(Long warehouseId, List<OrderItemRequestDTO> items)
            throws RemoteException, BusinessException {
        try {
            boolean reserved = stockDao.reserveStockAtomic(warehouseId, items);
            if (!reserved) {
                throw new InsufficientStockException("Stock insuffisant lors de la réservation sur le dépôt #" + warehouseId);
            }
            for (OrderItemRequestDTO item : items) {
                stockDao.findByWarehouseAndProduct(warehouseId, item.getProductId())
                        .ifPresent(this::checkAndPublishLowStock);
            }
            return true;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur réservation stock", ex);
        }
    }

    @Override
    public void releaseStockForOrder(Long warehouseId, List<OrderItemRequestDTO> items)
            throws RemoteException, BusinessException {
        try {
            stockDao.releaseReservedStock(warehouseId, items);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur libération stock", ex);
        }
    }

    private void checkAndPublishLowStock(StockItemDTO item) {
        if (item.getAvailableQuantity() <= item.getLowStockThreshold()) {
            boolean isOut = item.getAvailableQuantity() <= 0;
            String title = isOut ? "Rupture de stock critique" : "Alerte Stock Faible";
            String msg = item.getProductName() + " (" + item.getWarehouseName() + ") : "
                    + item.getAvailableQuantity() + " unité(s) restante(s) (seuil : "
                    + item.getLowStockThreshold() + ").";

            LogisticsEvent ev = LogisticsEvent.of(EventType.LOW_STOCK, title, msg);
            ev.setWarehouseId(item.getWarehouseId());
            ev.setTargetRole(Role.GESTIONNAIRE_STOCK);
            eventPublisher.publish(ev);
        }
    }
}
