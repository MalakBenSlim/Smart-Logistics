package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DriverDao {

    public int countDrivers() throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM drivers");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<DriverDTO> findAll() throws SQLException {
        String sql = "SELECT id, user_id, full_name, phone, vehicle_type, current_lat, current_lng, status, active_deliveries, total_deliveries FROM drivers ORDER BY id";
        List<DriverDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapDriver(rs));
            }
        }
        return list;
    }

    public Optional<DriverDTO> findById(Long id) throws SQLException {
        String sql = "SELECT id, user_id, full_name, phone, vehicle_type, current_lat, current_lng, status, active_deliveries, total_deliveries FROM drivers WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDriver(rs)) : Optional.empty();
            }
        }
    }

    public Optional<DriverDTO> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT id, user_id, full_name, phone, vehicle_type, current_lat, current_lng, status, active_deliveries, total_deliveries FROM drivers WHERE user_id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapDriver(rs)) : Optional.empty();
            }
        }
    }

    public DriverDTO save(DriverDTO d) throws SQLException {
        if (d.getId() == null) {
            String sql = """
                    INSERT INTO drivers (user_id, full_name, phone, vehicle_type, current_lat, current_lng, status, active_deliveries, total_deliveries)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (d.getUserId() != null) {
                    ps.setLong(1, d.getUserId());
                } else {
                    ps.setNull(1, Types.BIGINT);
                }
                ps.setString(2, d.getFullName());
                ps.setString(3, d.getPhone());
                ps.setString(4, d.getVehicleType());
                ps.setDouble(5, d.getCurrentLat());
                ps.setDouble(6, d.getCurrentLng());
                ps.setString(7, (d.getStatus() != null ? d.getStatus() : DriverStatus.DISPONIBLE).name());
                ps.setInt(8, d.getActiveDeliveries());
                ps.setInt(9, d.getTotalDeliveries());
                ps.executeUpdate();
                d.setId(JdbcHelper.extractGeneratedId(ps));
                return d;
            }
        } else {
            String sql = """
                    UPDATE drivers
                    SET full_name = ?, phone = ?, vehicle_type = ?, current_lat = ?, current_lng = ?, status = ?
                    WHERE id = ?
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, d.getFullName());
                ps.setString(2, d.getPhone());
                ps.setString(3, d.getVehicleType());
                ps.setDouble(4, d.getCurrentLat());
                ps.setDouble(5, d.getCurrentLng());
                ps.setString(6, d.getStatus().name());
                ps.setLong(7, d.getId());
                ps.executeUpdate();
                return findById(d.getId()).orElse(d);
            }
        }
    }

    public void updateStatus(Long driverId, DriverStatus status) throws SQLException {
        String sql = "UPDATE drivers SET status = ? WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, driverId);
            ps.executeUpdate();
        }
    }

    public void updatePosition(Long driverId, double lat, double lng) throws SQLException {
        String sql = "UPDATE drivers SET current_lat = ?, current_lng = ? WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, lat);
            ps.setDouble(2, lng);
            ps.setLong(3, driverId);
            ps.executeUpdate();
        }
    }

    public void markAssigned(Long driverId) throws SQLException {
        String sql = "UPDATE drivers SET status = 'EN_LIVRAISON', active_deliveries = active_deliveries + 1 WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            ps.executeUpdate();
        }
    }

    public void markCompleted(Long driverId) throws SQLException {
        String sql = """
                UPDATE drivers
                SET active_deliveries = GREATEST(0, active_deliveries - 1),
                    total_deliveries = total_deliveries + 1,
                    status = CASE WHEN active_deliveries - 1 <= 0 THEN 'DISPONIBLE' ELSE status END
                WHERE id = ?
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            ps.executeUpdate();
        }
    }

    private DriverDTO mapDriver(ResultSet rs) throws SQLException {
        DriverDTO d = new DriverDTO();
        d.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        d.setUserId(rs.wasNull() ? null : uid);
        d.setFullName(rs.getString("full_name"));
        d.setPhone(rs.getString("phone"));
        d.setVehicleType(rs.getString("vehicle_type"));
        d.setCurrentLat(rs.getDouble("current_lat"));
        d.setCurrentLng(rs.getDouble("current_lng"));
        d.setStatus(DriverStatus.valueOf(rs.getString("status")));
        d.setActiveDeliveries(rs.getInt("active_deliveries"));
        d.setTotalDeliveries(rs.getInt("total_deliveries"));
        return d;
    }
}
