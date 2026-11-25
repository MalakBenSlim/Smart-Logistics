package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.DashboardStatsDTO;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardStatsDao {

    public DashboardStatsDTO computeStats() throws SQLException {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        try (Connection conn = DataSourceFactory.getConnection()) {
            stats.setTotalUsers(querySingleInt(conn, "SELECT COUNT(*) FROM users"));
            stats.setTotalOrders(querySingleInt(conn, "SELECT COUNT(*) FROM orders"));
            stats.setActiveOrders(querySingleInt(conn,
                    "SELECT COUNT(*) FROM orders WHERE status NOT IN ('DELIVERED', 'CANCELLED')"));
            stats.setDeliveredOrders(querySingleInt(conn,
                    "SELECT COUNT(*) FROM orders WHERE status = 'DELIVERED'"));
            stats.setCancelledOrders(querySingleInt(conn,
                    "SELECT COUNT(*) FROM orders WHERE status = 'CANCELLED'"));
            stats.setTotalRevenue(querySingleDouble(conn,
                    "SELECT COALESCE(SUM(total_amount + delivery_cost), 0) FROM orders WHERE status <> 'CANCELLED'"));
            stats.setTotalDrivers(querySingleInt(conn, "SELECT COUNT(*) FROM drivers"));
            stats.setAvailableDrivers(querySingleInt(conn,
                    "SELECT COUNT(*) FROM drivers WHERE status = 'DISPONIBLE'"));
            stats.setTotalWarehouses(querySingleInt(conn, "SELECT COUNT(*) FROM warehouses"));
            stats.setOutOfStockCount(querySingleInt(conn,
                    "SELECT COUNT(*) FROM stocks s JOIN products p ON p.id = s.product_id WHERE p.active = TRUE AND (s.quantity - s.reserved_quantity) <= 0"));
            stats.setLowStockCount(querySingleInt(conn,
                    "SELECT COUNT(*) FROM stocks s JOIN products p ON p.id = s.product_id WHERE p.active = TRUE AND (s.quantity - s.reserved_quantity) > 0 AND (s.quantity - s.reserved_quantity) <= p.low_stock_threshold"));

            stats.setOrdersByDay(queryIntMap(conn,
                    "SELECT CAST(created_at AS DATE) AS d, COUNT(*) AS c FROM orders GROUP BY CAST(created_at AS DATE) ORDER BY d ASC LIMIT 10"));
            stats.setDeliveriesByDay(queryIntMap(conn,
                    "SELECT CAST(COALESCE(delivered_at, assigned_at) AS DATE) AS d, COUNT(*) AS c FROM deliveries GROUP BY d ORDER BY d ASC LIMIT 10"));
            stats.setTopProducts(queryIntMap(conn,
                    "SELECT p.name, COALESCE(SUM(oi.quantity), 0) AS qty FROM order_items oi JOIN products p ON p.id = oi.product_id GROUP BY p.name ORDER BY qty DESC LIMIT 6"));
            stats.setWarehouseLoads(queryDoubleMap(conn,
                    "SELECT name, CASE WHEN max_capacity > 0 THEN ROUND((current_load * 100.0) / max_capacity, 1) ELSE 0 END FROM warehouses ORDER BY id"));
            stats.setDriverPerformances(queryIntMap(conn,
                    "SELECT full_name, total_deliveries FROM drivers ORDER BY total_deliveries DESC"));
        }
        return stats;
    }

    private int querySingleInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double querySingleDouble(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    private Map<String, Integer> queryIntMap(Connection conn, String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(String.valueOf(rs.getObject(1)), rs.getInt(2));
            }
        }
        return map;
    }

    private Map<String, Double> queryDoubleMap(Connection conn, String sql) throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getDouble(2));
            }
        }
        return map;
    }
}
