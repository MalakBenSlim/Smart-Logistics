package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.DeliveryDTO;
import com.smartlogistics.common.enums.DeliveryStatus;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeliveryDao {

    private static final String BASE_SELECT = """
            SELECT del.id, del.order_id, o.order_code, del.driver_id, drv.full_name AS driver_name,
                   drv.phone AS driver_phone, drv.vehicle_type, del.status,
                   del.last_lat, del.last_lng, del.remaining_km,
                   w.latitude AS warehouse_lat, w.longitude AS warehouse_lng,
                   o.delivery_lat AS target_lat, o.delivery_lng AS target_lng, o.delivery_address,
                   del.assigned_at, del.started_at, del.delivered_at
            FROM deliveries del
            JOIN orders o ON o.id = del.order_id
            JOIN drivers drv ON drv.id = del.driver_id
            LEFT JOIN warehouses w ON w.id = o.warehouse_id
            """;

    public DeliveryDTO createDelivery(Long orderId, Long driverId, double startLat, double startLng, double remainingKm)
            throws SQLException {
        String sql = """
                INSERT INTO deliveries (order_id, driver_id, status, last_lat, last_lng, remaining_km, assigned_at)
                VALUES (?, ?, 'ASSIGNEE', ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        long id;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, orderId);
            ps.setLong(2, driverId);
            ps.setDouble(3, startLat);
            ps.setDouble(4, startLng);
            ps.setDouble(5, remainingKm);
            ps.executeUpdate();
            id = JdbcHelper.extractGeneratedId(ps);
        }
        return findById(id).orElseThrow();
    }

    public Optional<DeliveryDTO> findById(Long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE del.id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDelivery(rs)) : Optional.empty();
            }
        }
    }

    public Optional<DeliveryDTO> findByOrderId(Long orderId) throws SQLException {
        String sql = BASE_SELECT + " WHERE del.order_id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDelivery(rs)) : Optional.empty();
            }
        }
    }

    public List<DeliveryDTO> findByDriverId(Long driverId) throws SQLException {
        String sql = BASE_SELECT + " WHERE del.driver_id = ? ORDER BY del.assigned_at DESC";
        List<DeliveryDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapDelivery(rs));
                }
            }
        }
        return list;
    }

    public void updatePosition(Long deliveryId, double lat, double lng, double remainingKm) throws SQLException {
        String sql = "UPDATE deliveries SET last_lat = ?, last_lng = ?, remaining_km = ? WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lng);
            ps.setDouble(3, remainingKm);
            ps.setLong(4, deliveryId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(Long deliveryId, DeliveryStatus status) throws SQLException {
        String sql;
        if (status == DeliveryStatus.EN_ROUTE) {
            sql = "UPDATE deliveries SET status = ?, started_at = COALESCE(started_at, CURRENT_TIMESTAMP) WHERE id = ?";
        } else if (status == DeliveryStatus.LIVREE) {
            sql = "UPDATE deliveries SET status = ?, remaining_km = 0, delivered_at = CURRENT_TIMESTAMP WHERE id = ?";
        } else {
            sql = "UPDATE deliveries SET status = ? WHERE id = ?";
        }
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, deliveryId);
            ps.executeUpdate();
        }
    }

    private DeliveryDTO mapDelivery(ResultSet rs) throws SQLException {
        DeliveryDTO d = new DeliveryDTO();
        d.setId(rs.getLong("id"));
        d.setOrderId(rs.getLong("order_id"));
        d.setOrderCode(rs.getString("order_code"));
        d.setDriverId(rs.getLong("driver_id"));
        d.setDriverName(rs.getString("driver_name"));
        d.setDriverPhone(rs.getString("driver_phone"));
        d.setVehicleType(rs.getString("vehicle_type"));
        d.setStatus(DeliveryStatus.valueOf(rs.getString("status")));
        d.setLastLat(rs.getDouble("last_lat"));
        d.setLastLng(rs.getDouble("last_lng"));
        d.setRemainingKm(rs.getDouble("remaining_km"));
        d.setWarehouseLat(rs.getDouble("warehouse_lat"));
        d.setWarehouseLng(rs.getDouble("warehouse_lng"));
        d.setTargetLat(rs.getDouble("target_lat"));
        d.setTargetLng(rs.getDouble("target_lng"));
        d.setDeliveryAddress(rs.getString("delivery_address"));
        d.setEstimatedMinutesRemaining(Math.max(1, (int) Math.ceil((d.getRemainingKm() / 38.0) * 60.0)));
        d.setAssignedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("assigned_at")));
        d.setStartedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("started_at")));
        d.setDeliveredAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("delivered_at")));
        return d;
    }
}
