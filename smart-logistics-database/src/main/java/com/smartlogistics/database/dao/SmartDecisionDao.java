package com.smartlogistics.database.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

public class SmartDecisionDao {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public SmartDecisionDTO saveOrUpdate(SmartDecisionDTO decision) throws SQLException {
        String json;
        try {
            json = MAPPER.writeValueAsString(decision);
        } catch (JsonProcessingException ex) {
            throw new SQLException("Erreur de sérialisation SmartDecisionDTO", ex);
        }

        String sql = """
                MERGE INTO smart_decisions (order_id, selected_warehouse_id, warehouse_score, selected_driver_id, driver_score, decision_json)
                KEY (order_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, decision.getOrderId());
            if (decision.getSelectedWarehouseId() != null) {
                ps.setLong(2, decision.getSelectedWarehouseId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setDouble(3, decision.getWarehouseScore());
            if (decision.getSelectedDriverId() != null) {
                ps.setLong(4, decision.getSelectedDriverId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setDouble(5, decision.getDriverScore());
            ps.setString(6, json);
            ps.executeUpdate();
            return decision;
        }
    }

    public Optional<SmartDecisionDTO> findByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT decision_json, created_at FROM smart_decisions WHERE order_id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String json = rs.getString("decision_json");
                try {
                    SmartDecisionDTO dto = MAPPER.readValue(json, SmartDecisionDTO.class);
                    dto.setCreatedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("created_at")));
                    return Optional.of(dto);
                } catch (JsonProcessingException ex) {
                    throw new SQLException("Erreur de désérialisation SmartDecisionDTO", ex);
                }
            }
        }
    }
}
