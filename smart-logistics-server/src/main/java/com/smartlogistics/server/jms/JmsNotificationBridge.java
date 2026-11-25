package com.smartlogistics.server.jms;

import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.database.dao.NotificationDao;
import com.smartlogistics.jms.consumer.JmsEventSubscriber;
import com.smartlogistics.jms.event.LogisticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Consomme les événements JMS asynchrones et persiste les notifications métier
 * en base de données (Alertes Admin, Gestionnaire Stock, Client, Livreur).
 */
public class JmsNotificationBridge implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(JmsNotificationBridge.class);

    private final NotificationDao notificationDao = new NotificationDao();
    private final JmsEventSubscriber subscriber = new JmsEventSubscriber();

    public void start() throws Exception {
        subscriber.start();
        subscriber.subscribe(JndiConfig.getOrdersTopicName(), this::handleEvent);
        subscriber.subscribe(JndiConfig.getDeliveryTopicName(), this::handleEvent);
        subscriber.subscribe(JndiConfig.getStockAlertsQueueName(), this::handleEvent);
        log.info("JmsNotificationBridge démarré et abonné aux destinations JMS.");
    }

    private void handleEvent(LogisticsEvent ev) {
        if (ev == null || ev.getEventType() == null) {
            return;
        }
        // Les mises à jour GPS continues sont diffusées en SSE sans saturer la table notifications
        if (ev.getEventType() == EventType.POSITION_UPDATED) {
            return;
        }
        try {
            NotificationDTO notif = new NotificationDTO();
            notif.setUserId(ev.getTargetUserId());
            notif.setTargetRole(ev.getTargetRole());
            notif.setEventType(ev.getEventType());
            notif.setTitle(ev.getTitle() != null ? ev.getTitle() : ev.getEventType().name());
            notif.setMessage(ev.getMessage() != null ? ev.getMessage() : "");
            notificationDao.create(notif);

            // Pour les alertes de stock critique, notifier aussi les administrateurs
            if (ev.getEventType() == EventType.LOW_STOCK && ev.getTargetRole() != Role.ADMINISTRATEUR) {
                NotificationDTO adminNotif = new NotificationDTO();
                adminNotif.setTargetRole(Role.ADMINISTRATEUR);
                adminNotif.setEventType(EventType.LOW_STOCK);
                adminNotif.setTitle(notif.getTitle());
                adminNotif.setMessage(notif.getMessage());
                notificationDao.create(adminNotif);
            }
        } catch (SQLException ex) {
            log.error("Erreur lors de la persistance de la notification JMS", ex);
        }
    }

    @Override
    public void close() {
        subscriber.close();
    }
}
