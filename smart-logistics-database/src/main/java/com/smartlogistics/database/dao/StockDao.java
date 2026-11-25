package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StockDao {

    private static final String BASE_SELECT = """
            SELECT s.id, s.warehouse_id, w.name AS warehouse_name,
                   s.product_id, p.sku AS product_sku, p.name AS product_name,
                   p.category, p.unit_price, s.quantity, s.reserved_quantity,
                   p.low_stock_threshold, s.updated_at
            FROM stocks s
            JOIN warehouses w ON w.id = s.warehouse_id
            JOIN products p ON p.id = s.product_id
            WHERE p.active = TRUE
            """;

    public List<StockItemDTO> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY w.id, p.id";
        return queryList(sql, null);
    }

    public List<StockItemDTO> findByWarehouse(Long warehouseId) throws SQLException {
        String sql = BASE_SELECT + " AND s.warehouse_id = ? ORDER BY p.name";
        return queryList(sql, warehouseId);
    }

    public List<StockItemDTO> findLowStock() throws SQLException {
        String sql = BASE_SELECT + " AND (s.quantity - s.reserved_quantity) > 0 AND (s.quantity - s.reserved_quantity) <= p.low_stock_threshold ORDER BY (s.quantity - s.reserved_quantity) ASC";
        return queryList(sql, null);
    }

    public List<StockItemDTO> findOutOfStock() throws SQLException {
        String sql = BASE_SELECT + " AND (s.quantity - s.reserved_quantity) <= 0 ORDER BY w.name, p.name";
        return queryList(sql, null);
    }

    public Optional<StockItemDTO> findByWarehouseAndProduct(Long warehouseId, Long productId) throws SQLException {
        String sql = BASE_SELECT + " AND s.warehouse_id = ? AND s.product_id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapStock(rs)) : Optional.empty();
            }
        }
    }

    public StockItemDTO upsertQuantity(Long warehouseId, Long productId, int quantity) throws SQLException {
        String sql = """
                MERGE INTO stocks (warehouse_id, product_id, quantity, reserved_quantity, updated_at)
                KEY (warehouse_id, product_id)
                VALUES (?, ?, ?, COALESCE((SELECT reserved_quantity FROM stocks WHERE warehouse_id = ? AND product_id = ?), 0), CURRENT_TIMESTAMP)
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, warehouseId);
            ps.setLong(2, productId);
            ps.setInt(3, quantity);
            ps.setLong(4, warehouseId);
            ps.setLong(5, productId);
            ps.executeUpdate();
        }
        return findByWarehouseAndProduct(warehouseId, productId).orElseThrow();
    }

    /**
     * Réserve atomiquement le stock pour tous les articles d'une commande.
     * Utilise SELECT ... FOR UPDATE pour prévenir toute race condition.
     */
    public boolean reserveStockAtomic(Long warehouseId, List<OrderItemRequestDTO> items) throws SQLException {
        return JdbcHelper.inTransaction(conn -> {
            String lockSql = "SELECT quantity, reserved_quantity FROM stocks WHERE warehouse_id = ? AND product_id = ? FOR UPDATE";
            String updateSql = "UPDATE stocks SET reserved_quantity = reserved_quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE warehouse_id = ? AND product_id = ?";
            for (OrderItemRequestDTO item : items) {
                try (PreparedStatement lockPs = conn.prepareStatement(lockSql)) {
                    lockPs.setLong(1, warehouseId);
                    lockPs.setLong(2, item.getProductId());
                    try (ResultSet rs = lockPs.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        int available = rs.getInt("quantity") - rs.getInt("reserved_quantity");
                        if (available < item.getQuantity()) {
                            return false;
                        }
                    }
                }
                try (PreparedStatement updPs = conn.prepareStatement(updateSql)) {
                    updPs.setInt(1, item.getQuantity());
                    updPs.setLong(2, warehouseId);
                    updPs.setLong(3, item.getProductId());
                    updPs.executeUpdate();
                }
            }
            return true;
        });
    }

    public void releaseReservedStock(Long warehouseId, List<OrderItemRequestDTO> items) throws SQLException {
        JdbcHelper.inTransaction(conn -> {
            String sql = """
                    UPDATE stocks
                    SET reserved_quantity = GREATEST(0, reserved_quantity - ?), updated_at = CURRENT_TIMESTAMP
                    WHERE warehouse_id = ? AND product_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (OrderItemRequestDTO item : items) {
                    ps.setInt(1, item.getQuantity());
                    ps.setLong(2, warehouseId);
                    ps.setLong(3, item.getProductId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return true;
        });
    }

    public void commitReservedStockOnShip(Long warehouseId, List<OrderItemRequestDTO> items) throws SQLException {
        JdbcHelper.inTransaction(conn -> {
            String sql = """
                    UPDATE stocks
                    SET quantity = GREATEST(0, quantity - ?),
                        reserved_quantity = GREATEST(0, reserved_quantity - ?),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE warehouse_id = ? AND product_id = ?
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (OrderItemRequestDTO item : items) {
                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, item.getQuantity());
                    ps.setLong(3, warehouseId);
                    ps.setLong(4, item.getProductId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            return true;
        });
    }

    public boolean transferStockAtomic(Long fromWarehouseId, Long toWarehouseId, Long productId, int quantity)
            throws SQLException {
        return JdbcHelper.inTransaction(conn -> {
            String lockSql = "SELECT quantity, reserved_quantity FROM stocks WHERE warehouse_id = ? AND product_id = ? FOR UPDATE";
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setLong(1, fromWarehouseId);
                ps.setLong(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    int available = rs.getInt("quantity") - rs.getInt("reserved_quantity");
                    if (available < quantity) return false;
                }
            }
            try (PreparedStatement dec = conn.prepareStatement(
                    "UPDATE stocks SET quantity = quantity - ?, updated_at = CURRENT_TIMESTAMP WHERE warehouse_id = ? AND product_id = ?")) {
                dec.setInt(1, quantity);
                dec.setLong(2, fromWarehouseId);
                dec.setLong(3, productId);
                dec.executeUpdate();
            }
            String upsert = """
                    MERGE INTO stocks (warehouse_id, product_id, quantity, reserved_quantity, updated_at)
                    KEY (warehouse_id, product_id)
                    VALUES (?, ?, COALESCE((SELECT quantity FROM stocks WHERE warehouse_id = ? AND product_id = ?), 0) + ?,
                            COALESCE((SELECT reserved_quantity FROM stocks WHERE warehouse_id = ? AND product_id = ?), 0),
                            CURRENT_TIMESTAMP)
                    """;
            try (PreparedStatement inc = conn.prepareStatement(upsert)) {
                inc.setLong(1, toWarehouseId);
                inc.setLong(2, productId);
                inc.setLong(3, toWarehouseId);
                inc.setLong(4, productId);
                inc.setInt(5, quantity);
                inc.setLong(6, toWarehouseId);
                inc.setLong(7, productId);
                inc.executeUpdate();
            }
            return true;
        });
    }

    private List<StockItemDTO> queryList(String sql, Long warehouseIdParam) throws SQLException {
        List<StockItemDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (warehouseIdParam != null) {
                ps.setLong(1, warehouseIdParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapStock(rs));
                }
            }
        }
        return list;
    }

    private StockItemDTO mapStock(ResultSet rs) throws SQLException {
        StockItemDTO s = new StockItemDTO();
        s.setId(rs.getLong("id"));
        s.setWarehouseId(rs.getLong("warehouse_id"));
        s.setWarehouseName(rs.getString("warehouse_name"));
        s.setProductId(rs.getLong("product_id"));
        s.setProductSku(rs.getString("product_sku"));
        s.setProductName(rs.getString("product_name"));
        s.setCategory(rs.getString("category"));
        s.setUnitPrice(rs.getDouble("unit_price"));
        s.setQuantity(rs.getInt("quantity"));
        s.setReservedQuantity(rs.getInt("reserved_quantity"));
        s.setLowStockThreshold(rs.getInt("low_stock_threshold"));
        s.setUpdatedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("updated_at")));
        return s;
    }
}
