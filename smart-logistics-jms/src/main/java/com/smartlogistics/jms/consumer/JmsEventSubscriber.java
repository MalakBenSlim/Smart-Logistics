package com.smartlogistics.jms.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.jms.event.LogisticsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abonné JMS générique résolvant les destinations par JNDI.
 */
public class JmsEventSubscriber implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(JmsEventSubscriber.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Connection connection;
    private Session session;
    private final List<MessageConsumer> consumers = new ArrayList<>();

    public synchronized void start() throws Exception {
        if (connection != null) {
            return;
        }
        ConnectionFactory cf = ServiceLocator.lookupJmsConnectionFactory();
        connection = cf.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    public synchronized void subscribe(String destinationName, Consumer<LogisticsEvent> handler) throws Exception {
        if (connection == null) {
            start();
        }
        Destination dest = ServiceLocator.lookupJmsDestination(destinationName);
        MessageConsumer consumer = session.createConsumer(dest);
        consumer.setMessageListener(message -> {
            if (message instanceof TextMessage textMsg) {
                try {
                    LogisticsEvent event = MAPPER.readValue(textMsg.getText(), LogisticsEvent.class);
                    handler.accept(event);
                } catch (Exception ex) {
                    log.error("Erreur lors du traitement du message JMS sur {}", destinationName, ex);
                }
            }
        });
        consumers.add(consumer);
        log.info("Consommateur JMS abonné à '{}'", destinationName);
    }

    @Override
    public synchronized void close() {
        for (MessageConsumer c : consumers) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        consumers.clear();
        if (session != null) {
            try {
                session.close();
            } catch (Exception ignored) {
            }
            session = null;
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
            connection = null;
        }
    }
}
