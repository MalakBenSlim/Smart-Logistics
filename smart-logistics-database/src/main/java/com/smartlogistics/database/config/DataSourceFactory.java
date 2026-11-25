package com.smartlogistics.database.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fabrique de DataSource utilisant HikariCP.
 */
public final class DataSourceFactory {
    private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);
    private static volatile HikariDataSource instance;

    private DataSourceFactory() {}

    public static DataSource getDataSource() {
        if (instance == null) {
            synchronized (DataSourceFactory.class) {
                if (instance == null) {
                    instance = createFromProperties();
                }
            }
        }
        return instance;
    }

    public static synchronized void configureCustom(String jdbcUrl, String username, String password) {
        if (instance != null) {
            instance.close();
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setPoolName("SmartLogisticsPool-Custom");
        instance = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static boolean isDatabaseReachable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    public static int getConfiguredTcpPort() {
        Properties props = loadProps();
        return Integer.parseInt(System.getProperty("db.h2.tcp.port",
                props.getProperty("db.h2.tcp.port", "9092")));
    }

    private static HikariDataSource createFromProperties() {
        Properties props = loadProps();
        String url = System.getProperty("db.url",
                props.getProperty("db.url", "jdbc:h2:tcp://localhost:9092/./data/smartlogistics;MODE=PostgreSQL;DATABASE_TO_UPPER=false"));
        String user = System.getProperty("db.username", props.getProperty("db.username", "sa"));
        String pass = System.getProperty("db.password", props.getProperty("db.password", ""));
        int maxPool = Integer.parseInt(props.getProperty("db.pool.max", "15"));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(5000);
        config.setPoolName("SmartLogisticsHikariPool");
        log.info("Pool HikariCP initialisé sur {}", url);
        return new HikariDataSource(config);
    }

    private static Properties loadProps() {
        Properties props = new Properties();
        try (InputStream is = DataSourceFactory.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException ex) {
            log.warn("Impossible de charger db.properties", ex);
        }
        return props;
    }

    public static synchronized void close() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
