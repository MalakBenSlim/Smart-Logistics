package com.smartlogistics.common.jndi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge la configuration JNDI depuis jndi.properties (avec surcharge possible via -D).
 */
public final class JndiConfig {
    private static final Logger log = LoggerFactory.getLogger(JndiConfig.class);
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = JndiConfig.class.getClassLoader().getResourceAsStream("jndi.properties")) {
            if (is != null) {
                PROPS.load(is);
            } else {
                log.warn("Fichier jndi.properties introuvable sur le classpath, utilisation des valeurs par défaut.");
            }
        } catch (IOException ex) {
            log.error("Erreur lors du chargement de jndi.properties", ex);
        }
    }

    private JndiConfig() {}

    public static String get(String key, String defaultValue) {
        return System.getProperty(key, PROPS.getProperty(key, defaultValue));
    }

    public static String getRmiInitialContextFactory() {
        return get("java.naming.factory.initial", "com.sun.jndi.rmi.registry.RegistryContextFactory");
    }

    public static String getRmiProviderUrl() {
        return get("java.naming.provider.url", "rmi://localhost:1099");
    }

    public static int getRmiPort() {
        String url = getRmiProviderUrl();
        int idx = url.lastIndexOf(':');
        if (idx > 0) {
            try {
                return Integer.parseInt(url.substring(idx + 1).replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return 1099;
    }

    public static String getJmsInitialContextFactory() {
        return get("jms.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
    }

    public static String getJmsBrokerUrl() {
        return get("jms.broker.url", "tcp://localhost:61616");
    }

    public static String getOrdersTopicName() {
        return get("jms.topic.orders", "smartlogistics.events.orders");
    }

    public static String getDeliveryTopicName() {
        return get("jms.topic.delivery", "smartlogistics.events.delivery");
    }

    public static String getStockAlertsQueueName() {
        return get("jms.queue.stockAlerts", "smartlogistics.alerts.stock");
    }

    public static String getTcpServerHost() {
        return get("tcp.server.host", "localhost");
    }

    public static int getTcpServerPort() {
        return Integer.parseInt(get("tcp.server.port", "9095"));
    }
}
