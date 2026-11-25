package com.smartlogistics.jms.broker;

import com.smartlogistics.common.jndi.JndiConfig;
import org.apache.activemq.broker.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Broker Apache ActiveMQ Classic embarqué permettant la communication asynchrone JMS.
 */
public final class EmbeddedJmsBroker {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedJmsBroker.class);
    private static BrokerService broker;

    private EmbeddedJmsBroker() {}

    public static synchronized void start() throws Exception {
        if (broker != null && broker.isStarted()) {
            return;
        }
        String bindUrl = JndiConfig.getJmsBrokerUrl().replace("localhost", "0.0.0.0");
        broker = new BrokerService();
        broker.setBrokerName("smart-logistics-broker");
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector(bindUrl);
        broker.start();
        broker.waitUntilStarted();
        log.info("Broker JMS ActiveMQ démarré sur {}", bindUrl);
    }

    public static synchronized void stop() {
        if (broker != null) {
            try {
                broker.stop();
                log.info("Broker JMS ActiveMQ arrêté.");
            } catch (Exception ex) {
                log.warn("Erreur lors de l'arrêt du broker JMS", ex);
            }
            broker = null;
        }
    }

    public static boolean isReachable() {
        String url = JndiConfig.getJmsBrokerUrl();
        String host = "localhost";
        int port = 61616;
        try {
            String stripped = url.replace("tcp://", "");
            String[] parts = stripped.split(":");
            if (parts.length >= 2) {
                host = parts[0];
                port = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            }
        } catch (Exception ignored) {
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
