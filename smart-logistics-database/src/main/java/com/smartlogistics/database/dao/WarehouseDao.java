package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.WarehouseStatus;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WarehouseDao {

    public int countWarehouses() throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM warehouses");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<WarehouseDTO> findAll() throws SQLException {
        String sql = "SELECT id, name, address, latitude, longitude, max_capacity, current_load, status FROM warehouses ORDER BY id";
        List<WarehouseDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapWarehouse(rs));
            }
        }
        return list;
    }

    public Optional<WarehouseDTO> findById(Long id) throws SQLException {
        String sql = "SELECT id, name, address, latitude, longitude, max_capacity, current_load, status FROM warehouses WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapWarehouse(rs)) : Optional.empty();
            }
        }
    }

    public WarehouseDTO save(WarehouseDTO w) throws SQLException {
        if (w.getId() == null) {
            String sql = """
                    INSERT INTO warehouses (name, address, latitude, longitude, max_capacity, current_load, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, w.getName());
                ps.setString(2, w.getAddress());
                ps.setDouble(3, w.getLatitude());
                ps.setDouble(4, w.getLongitude());
                ps.setInt(5, w.getMaxCapacity());
                ps.setInt(6, w.getCurrentLoad());
                ps.setString(7, (w.getStatus() != null ? w.getStatus() : WarehouseStatus.ACTIF).name());
                ps.executeUpdate();
                w.setId(JdbcHelper.extractGeneratedId(ps));
                return w;
            }
        } else {
            String sql = """
                    UPDATE warehouses
                    SET name = ?, address = ?, latitude = ?, longitude = ?, max_capacity = ?, current_load = ?, status = ?
                    WHERE id = ?
                    """;
            try (Connection conn = DataSourceFactory.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, w.getName());
                ps.setString(2, w.getAddress());
                ps.setDouble(3, w.getLatitude());
                ps.setDouble(4, w.getLongitude());
                ps.setInt(5, w.getMaxCapacity());
                ps.setInt(6, w.getCurrentLoad());
                ps.setString(7, w.getStatus().name());
                ps.setLong(8, w.getId());
                ps.executeUpdate();
                return w;
            }
        }
    }

    public void adjustLoad(Long warehouseId, int delta) throws SQLException {
        String sql = "UPDATE warehouses SET current_load = GREATEST(0, current_load + ?) WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, warehouseId);
            ps.executeUpdate();
        }
    }

    private WarehouseDTO mapWarehouse(ResultSet rs) throws SQLException {
        WarehouseDTO w = new WarehouseDTO();
        w.setId(rs.getLong("id"));
        w.setName(rs.getString("name"));
        w.setAddress(rs.getString("address"));
        w.setLatitude(rs.getDouble("latitude"));
        w.setLongitude(rs.getDouble("longitude"));
        w.setMaxCapacity(rs.getInt("max_capacity"));
        w.setCurrentLoad(rs.getInt("current_load"));
        w.setStatus(WarehouseStatus.valueOf(rs.getString("status")));
        return w;
    }
}
