package com.smartlogistics.database.dao;

import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public static class UserAuthRecord {
        private final UserDTO user;
        private final String passwordHash;

        public UserAuthRecord(UserDTO user, String passwordHash) {
            this.user = user;
            this.passwordHash = passwordHash;
        }

        public UserDTO getUser() { return user; }
        public String getPasswordHash() { return passwordHash; }
    }

    public void ensureRolesExist() throws SQLException {
        String sql = "MERGE INTO roles (id, name) KEY (id) VALUES (?, ?)";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Role role : Role.values()) {
                ps.setInt(1, role.ordinal() + 1);
                ps.setString(2, role.name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public int countUsers() throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Optional<UserAuthRecord> findAuthByEmail(String email) throws SQLException {
        String sql = """
                SELECT u.id, u.full_name, u.email, u.password_hash, u.phone, u.address,
                       u.latitude, u.longitude, r.name AS role_name, u.active, u.created_at
                FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE LOWER(u.email) = LOWER(?)
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserAuthRecord(mapUser(rs), rs.getString("password_hash")));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<UserDTO> findById(Long id) throws SQLException {
        String sql = """
                SELECT u.id, u.full_name, u.email, u.phone, u.address,
                       u.latitude, u.longitude, r.name AS role_name, u.active, u.created_at
                FROM users u
                JOIN roles r ON r.id = u.role_id
                WHERE u.id = ?
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapUser(rs)) : Optional.empty();
            }
        }
    }

    public List<UserDTO> findAll() throws SQLException {
        String sql = """
                SELECT u.id, u.full_name, u.email, u.phone, u.address,
                       u.latitude, u.longitude, r.name AS role_name, u.active, u.created_at
                FROM users u
                JOIN roles r ON r.id = u.role_id
                ORDER BY u.id ASC
                """;
        List<UserDTO> list = new ArrayList<>();
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        }
        return list;
    }

    public UserDTO create(UserDTO user, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO users (full_name, email, password_hash, phone, address, latitude, longitude, role_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail().toLowerCase());
            ps.setString(3, passwordHash);
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setDouble(6, user.getLatitude());
            ps.setDouble(7, user.getLongitude());
            ps.setInt(8, user.getRole().ordinal() + 1);
            ps.setBoolean(9, user.isActive());
            ps.executeUpdate();
            long id = JdbcHelper.extractGeneratedId(ps);
            return findById(id).orElseThrow();
        }
    }

    public UserDTO updateRoleAndStatus(Long userId, Role role, boolean active) throws SQLException {
        String sql = "UPDATE users SET role_id = ?, active = ? WHERE id = ?";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, role.ordinal() + 1);
            ps.setBoolean(2, active);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
        return findById(userId).orElse(null);
    }

    public void saveResetToken(Long userId, String token, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at, used) VALUES (?, ?, ?, FALSE)";
        try (Connection conn = DataSourceFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public boolean consumeResetTokenAndUpdatePassword(String token, String newPasswordHash) throws SQLException {
        return JdbcHelper.inTransaction(conn -> {
            String select = "SELECT id, user_id, expires_at, used FROM password_reset_tokens WHERE token = ?";
            Long tokenId = null;
            Long userId = null;
            try (PreparedStatement ps = conn.prepareStatement(select)) {
                ps.setString(1, token);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    boolean used = rs.getBoolean("used");
                    LocalDateTime expires = rs.getTimestamp("expires_at").toLocalDateTime();
                    if (used || expires.isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    tokenId = rs.getLong("id");
                    userId = rs.getLong("user_id");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
                ps.setString(1, newPasswordHash);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE password_reset_tokens SET used = TRUE WHERE id = ?")) {
                ps.setLong(1, tokenId);
                ps.executeUpdate();
            }
            return true;
        });
    }

    private UserDTO mapUser(ResultSet rs) throws SQLException {
        UserDTO u = new UserDTO();
        u.setId(rs.getLong("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setAddress(rs.getString("address"));
        u.setLatitude(rs.getDouble("latitude"));
        u.setLongitude(rs.getDouble("longitude"));
        u.setRole(Role.valueOf(rs.getString("role_name")));
        u.setActive(rs.getBoolean("active"));
        u.setCreatedAt(JdbcHelper.toLocalDateTime(rs.getTimestamp("created_at")));
        return u;
    }
}
