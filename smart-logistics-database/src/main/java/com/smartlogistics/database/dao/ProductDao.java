package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDao {

    public int countProducts() throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE active = TRUE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<ProductDTO> findAll(boolean includeInactive) throws SQLException {
        String sql = """
                SELECT p.id, p.sku, p.name, p.category, p.description, p.unit_price, p.low_stock_threshold, p.active,
                       COALESCE(SUM(GREATEST(0, s.quantity - s.reserved_quantity)), 0) AS total_available
                FROM products p
                LEFT JOIN stocks s ON s.product_id = p.id
                """ + (includeInactive ? "" : "WHERE p.active = TRUE ") + """
                GROUP BY p.id, p.sku, p.name, p.category, p.description, p.unit_price, p.low_stock_threshold, p.active
                ORDER BY p.id ASC
                """;
        List<ProductDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        }
        return list;
    }

    public Optional<ProductDTO> findById(Long id) throws SQLException {
        String sql = """
                SELECT p.id, p.sku, p.name, p.category, p.description, p.unit_price, p.low_stock_threshold, p.active,
                       COALESCE(SUM(GREATEST(0, s.quantity - s.reserved_quantity)), 0) AS total_available
                FROM products p
                LEFT JOIN stocks s ON s.product_id = p.id
                WHERE p.id = ?
                GROUP BY p.id, p.sku, p.name, p.category, p.description, p.unit_price, p.low_stock_threshold, p.active
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapProduct(rs)) : Optional.empty();
            }
        }
    }

    public ProductDTO save(ProductDTO p) throws SQLException {
        if (p.getId() == null) {
            String sql = """
                    INSERT INTO products (sku, name, category, description, unit_price, low_stock_threshold, active)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getSku());
                ps.setString(2, p.getName());
                ps.setString(3, p.getCategory());
                ps.setString(4, p.getDescription());
                ps.setDouble(5, p.getUnitPrice());
                ps.setInt(6, p.getLowStockThreshold() > 0 ? p.getLowStockThreshold() : 5);
                ps.setBoolean(7, p.isActive());
                ps.executeUpdate();
                long id = JdbcHelper.extractGeneratedId(ps);
                return findById(id).orElseThrow();
            }
        } else {
            String sql = """
                    UPDATE products
                    SET sku = ?, name = ?, category = ?, description = ?, unit_price = ?, low_stock_threshold = ?, active = ?
                    WHERE id = ?
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getSku());
                ps.setString(2, p.getName());
                ps.setString(3, p.getCategory());
                ps.setString(4, p.getDescription());
                ps.setDouble(5, p.getUnitPrice());
                ps.setInt(6, p.getLowStockThreshold() > 0 ? p.getLowStockThreshold() : 5);
                ps.setBoolean(7, p.isActive());
                ps.setLong(8, p.getId());
                ps.executeUpdate();
                return findById(p.getId()).orElse(p);
            }
        }
    }

    public boolean softDelete(Long productId) throws SQLException {
        String sql = "UPDATE products SET active = FALSE WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            return ps.executeUpdate() > 0;
        }
    }

    private ProductDTO mapProduct(ResultSet rs) throws SQLException {
        ProductDTO p = new ProductDTO();
        p.setId(rs.getLong("id"));
        p.setSku(rs.getString("sku"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setDescription(rs.getString("description"));
        p.setUnitPrice(rs.getDouble("unit_price"));
        p.setLowStockThreshold(rs.getInt("low_stock_threshold"));
        p.setActive(rs.getBoolean("active"));
        p.setTotalAvailableStock(rs.getInt("total_available"));
        return p;
    }
}
