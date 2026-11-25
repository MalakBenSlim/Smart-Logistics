package com.smartlogistics.server.bootstrap;

import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.database.config.DataSourceFactory;
import com.smartlogistics.database.config.H2TcpServerBootstrap;
import com.smartlogistics.database.config.SchemaInitializer;
import com.smartlogistics.database.seed.DemoDataSeeder;
import com.smartlogistics.jms.broker.EmbeddedJmsBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;

/**
 * Lance l'infrastructure centrale distribuée :
 * 1) Base de données H2 en mode Serveur TCP (port 9092) + Schéma + Données démo
 * 2) Annuaire RMI / JNDI (port 1099)
 * 3) Broker JMS Apache ActiveMQ (port 61616)
 */
public class CentralInfrastructureLauncher {
    private static final Logger log = LoggerFactory.getLogger(CentralInfrastructureLauncher.class);
    private static Registry registry;

    public static void startInfrastructure() throws Exception {
        int dbPort = DataSourceFactory.getConfiguredTcpPort();
        H2TcpServerBootstrap.startIfNotRunning(dbPort);
        SchemaInitializer.initialize();
        new DemoDataSeeder().seedIfEmpty();

        int rmiPort = JndiConfig.getRmiPort();
        try {
            registry = LocateRegistry.createRegistry(rmiPort);
            log.info("Annuaire RMI / JNDI démarré sur le port {}", rmiPort);
        } catch (RemoteException ex) {
            registry = LocateRegistry.getRegistry(rmiPort);
            log.info("Annuaire RMI / JNDI déjà actif sur le port {}", rmiPort);
        }

        EmbeddedJmsBroker.start();
    }

    public static Registry getRegistry() {
        return registry;
    }

    public static void main(String[] args) throws Exception {
        startInfrastructure();
        log.info("=== Infrastructure Centrale Smart Logistics PRÊTE ===");
        new CountDownLatch(1).await();
    }
}
