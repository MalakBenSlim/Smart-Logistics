package com.smartlogistics.common.rmi;

import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.common.exception.BusinessException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Service RMI distant de gestion du catalogue produits et des stocks multi-dépôts.
 */
public interface StockService extends Remote {
    List<ProductDTO> getAllProducts(boolean includeInactive) throws RemoteException;

    ProductDTO saveProduct(ProductDTO product) throws RemoteException, BusinessException;

    boolean deleteProduct(Long productId) throws RemoteException, BusinessException;

    List<StockItemDTO> getStockByWarehouse(Long warehouseId) throws RemoteException;

    List<StockItemDTO> getAllStocks() throws RemoteException;

    List<StockItemDTO> getLowStockItems() throws RemoteException;

    List<StockItemDTO> getOutOfStockItems() throws RemoteException;

    StockItemDTO updateStock(Long warehouseId, Long productId, int newQuantity)
            throws RemoteException, BusinessException;

    boolean transferStock(Long fromWarehouseId, Long toWarehouseId, Long productId, int quantity)
            throws RemoteException, BusinessException;

    boolean reserveStockForOrder(Long warehouseId, List<OrderItemRequestDTO> items)
            throws RemoteException, BusinessException;

    void releaseStockForOrder(Long warehouseId, List<OrderItemRequestDTO> items)
            throws RemoteException, BusinessException;
}
