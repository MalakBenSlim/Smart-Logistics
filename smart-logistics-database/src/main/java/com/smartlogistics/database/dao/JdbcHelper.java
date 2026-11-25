package com.smartlogistics.database.dao;

import com.smartlogistics.database.config.DataSourceFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Utilitaires JDBC partagés pour éviter la duplication dans les DAOs.
 */
public final class JdbcHelper {
    private JdbcHelper() {}

    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection conn) throws SQLException;
    }

    public static <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection conn = DataSourceFactory.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                T result = work.execute(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw ex;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public static long extractGeneratedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Aucun ID généré retourné par la base de données.");
        }
    }
}
