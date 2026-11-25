package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class NotificationDao {

    public NotificationDTO create(NotificationDTO n) throws SQLException {
        String sql = """
                INSERT INTO notifications (user_id, target_role, event_type, title, message, is_read)
                VALUES (?, ?, ?, ?, ?, FALSE)
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (n.getUserId() != null) {
                ps.setLong(1, n.getUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, n.getTargetRole() != null ? n.getTargetRole().name() : null);
            ps.setString(3, n.getEventType().name());
            ps.setString(4, n.getTitle());
            ps.setString(5, n.getMessage());
            ps.executeUpdate();
            n.setId(JdbcHelper.extractGeneratedId(ps));
            return n;
        }
    }

    public List<NotificationDTO> findForUserOrRole(Long userId, Role role, int limit) throws SQLException {
        String sql = """
                SELECT id, user_id, target_role, event_type, title, message, is_read, created_at
                FROM notifications
                WHERE user_id = ? OR target_role = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;
        List<NotificationDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId != null ? userId : -1L);
            ps.setString(2, role != null ? role.name() : "");
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapNotification(rs));
                }
            }
        }
        return list;
    }

    public void markAsRead(Long notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            ps.executeUpdate();
        }
    }

    private NotificationDTO mapNotification(ResultSet rs) throws SQLException {
        NotificationDTO n = new NotificationDTO();
        n.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        n.setUserId(rs.wasNull() ? null : uid);
        String roleStr = rs.getString("target_role");
        n.setTargetRole(roleStr != null ? Role.valueOf(roleStr) : null);
        n.setEventType(EventType.valueOf(rs.getString("event_type")));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setRead(rs.getBoolean("is_read"));
        n.setCreatedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("created_at")));
        return n;
    }
}
