package com.smartlogistics.database.config;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Démarre le serveur TCP H2 afin que plusieurs processus JVM distribués
 * partagent une unique base de données relationnelle centralisée.
 */
public final class H2TcpServerBootstrap {
    private static final Logger log = LoggerFactory.getLogger(H2TcpServerBootstrap.class);
    private static Server tcpServer;

    private H2TcpServerBootstrap() {}

    public static synchronized void startIfNotRunning(int port) throws SQLException {
        if (tcpServer != null && tcpServer.isRunning(false)) {
            return;
        }
        try {
            tcpServer = Server.createTcpServer(
                    "-tcp",
                    "-tcpPort", String.valueOf(port),
                    "-tcpAllowOthers",
                    "-ifNotExists"
            ).start();
            log.info("Serveur H2 TCP démarré sur le port {}", port);
        } catch (SQLException ex) {
            // Si un autre processus a déjà démarré le serveur sur ce port, on l'utilise
            if (ex.getMessage() != null && ex.getMessage().contains("Address already in use")) {
                log.info("Serveur H2 TCP déjà actif sur le port {} (mode partagé).", port);
            } else {
                throw ex;
            }
        }
    }

    public static synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
            log.info("Serveur H2 TCP arrêté.");
        }
    }
}
