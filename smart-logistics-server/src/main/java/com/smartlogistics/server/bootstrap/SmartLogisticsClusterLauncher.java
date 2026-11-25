package com.smartlogistics.server.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Lanceur combiné (Infrastructure + Services Métier) pour démarrer le backend distribué
 * en une seule commande lors des tests ou démonstrations locales.
 */
public class SmartLogisticsClusterLauncher {
    private static final Logger log = LoggerFactory.getLogger(SmartLogisticsClusterLauncher.class);

    public static void main(String[] args) throws Exception {
        CentralInfrastructureLauncher.startInfrastructure();
        BusinessServicesLauncher.startServices();
        log.info("Backend Smart Logistics prêt. Vous pouvez maintenant démarrer AppWebGateway.");
        new CountDownLatch(1).await();
    }
}
