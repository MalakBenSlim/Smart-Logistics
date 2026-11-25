package com.smartlogistics.common.jndi;

import com.smartlogistics.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.Remote;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServiceLocator centralisé utilisant JNDI pour enregistrer et localiser
 * les services RMI et les ressources JMS sans adresses codées en dur.
 */
public final class ServiceLocator {
    private static final Logger log = LoggerFactory.getLogger(ServiceLocator.class);

    public static final String AUTH_SERVICE = "AuthService";
    public static final String WAREHOUSE_SERVICE = "WarehouseService";
    public static final String STOCK_SERVICE = "StockService";
    public static final String ORDER_SERVICE = "OrderService";
    public static final String DELIVERY_SERVICE = "DeliveryService";
    public static final String NOTIFICATION_SERVICE = "NotificationService";

    private static final Map<String, Remote> RMI_CACHE = new ConcurrentHashMap<>();

    private ServiceLocator() {}

    public static Context createRmiContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiConfig.getRmiInitialContextFactory());
        env.put(Context.PROVIDER_URL, JndiConfig.getRmiProviderUrl());
        return new InitialContext(env);
    }

    public static Context createJmsContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, JndiConfig.getJmsInitialContextFactory());
        env.put(Context.PROVIDER_URL, JndiConfig.getJmsBrokerUrl());
        env.put("topic." + JndiConfig.getOrdersTopicName(), JndiConfig.getOrdersTopicName());
        env.put("topic." + JndiConfig.getDeliveryTopicName(), JndiConfig.getDeliveryTopicName());
        env.put("queue." + JndiConfig.getStockAlertsQueueName(), JndiConfig.getStockAlertsQueueName());
        return new InitialContext(env);
    }

    public static void bindRmiService(String jndiName, Remote service) throws NamingException {
        Context ctx = createRmiContext();
        try {
            ctx.rebind(jndiName, service);
            log.info("Service RMI enregistré dans JNDI sous '{}' ({})", jndiName, JndiConfig.getRmiProviderUrl());
        } finally {
            ctx.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Remote> T getRmiService(String jndiName, Class<T> serviceClass)
            throws ServiceUnavailableException {
        Remote cached = RMI_CACHE.get(jndiName);
        if (serviceClass.isInstance(cached)) {
            return (T) cached;
        }
        return lookupFreshRmiService(jndiName, serviceClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Remote> T lookupFreshRmiService(String jndiName, Class<T> serviceClass)
            throws ServiceUnavailableException {
        Context ctx = null;
        try {
            ctx = createRmiContext();
            Object lookedUp = ctx.lookup(jndiName);
            if (!serviceClass.isInstance(lookedUp)) {
                throw new ServiceUnavailableException("Type invalide retourné par JNDI pour " + jndiName);
            }
            T stub = (T) lookedUp;
            RMI_CACHE.put(jndiName, stub);
            return stub;
        } catch (NamingException ex) {
            RMI_CACHE.remove(jndiName);
            throw new ServiceUnavailableException(jndiName, ex);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }

    public static void invalidateCache(String jndiName) {
        RMI_CACHE.remove(jndiName);
    }

    public static boolean isRmiServiceBound(String jndiName) {
        Context ctx = null;
        try {
            ctx = createRmiContext();
            return ctx.lookup(jndiName) != null;
        } catch (NamingException ex) {
            return false;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {
                }
            }
        }
    }

    public static ConnectionFactory lookupJmsConnectionFactory() throws NamingException {
        Context ctx = createJmsContext();
        try {
            return (ConnectionFactory) ctx.lookup("ConnectionFactory");
        } finally {
            ctx.close();
        }
    }

    public static Destination lookupJmsDestination(String destinationName) throws NamingException {
        Context ctx = createJmsContext();
        try {
            return (Destination) ctx.lookup(destinationName);
        } finally {
            ctx.close();
        }
    }
}
