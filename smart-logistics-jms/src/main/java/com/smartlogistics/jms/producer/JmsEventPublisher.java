package com.smartlogistics.jms.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.jms.event.LogisticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 * Producteur JMS résolvant ConnectionFactory et Destinations via JNDI.
 */
public class JmsEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(JmsEventPublisher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void publish(LogisticsEvent event) {
        String destinationName = resolveDestinationName(event.getEventType());
        try {
            ConnectionFactory cf = ServiceLocator.lookupJmsConnectionFactory();
            Destination destination = ServiceLocator.lookupJmsDestination(destinationName);

            try (Connection conn = cf.createConnection();
                 Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                 MessageProducer producer = session.createProducer(destination)) {
                conn.start();
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
                String payload = MAPPER.writeValueAsString(event);
                TextMessage msg = session.createTextMessage(payload);
                msg.setStringProperty("eventType", event.getEventType().name());
                producer.send(msg);
                log.debug("Événement JMS publié [{}] -> {} : {}", event.getEventType(), destinationName, event.getTitle());
            }
        } catch (Exception ex) {
            log.warn("Impossible de publier l'événement JMS [{}] (broker peut-être arrêté) : {}",
                    event.getEventType(), ex.getMessage());
        }
    }

    private String resolveDestinationName(EventType type) {
        if (type == EventType.LOW_STOCK) {
            return JndiConfig.getStockAlertsQueueName();
        }
        if (type == EventType.DRIVER_ASSIGNED
                || type == EventType.DELIVERY_STARTED
                || type == EventType.POSITION_UPDATED
                || type == EventType.DELIVERY_COMPLETED) {
            return JndiConfig.getDeliveryTopicName();
        }
        return JndiConfig.getOrdersTopicName();
    }
}
