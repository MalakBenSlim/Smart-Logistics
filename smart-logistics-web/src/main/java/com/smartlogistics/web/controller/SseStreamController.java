package com.smartlogistics.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.jms.consumer.JmsEventSubscriber;
import com.smartlogistics.jms.event.LogisticsEvent;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.http.sse.SseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pont temps réel JMS -> Server-Sent Events (SSE).
 * Reçoit les événements asynchrones ActiveMQ et les diffuse aux navigateurs connectés.
 */
public class SseStreamController {
    private static final Logger log = LoggerFactory.getLogger(SseStreamController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static class ClientSubscriber {
        final SseClient client;
        final UserDTO user;

        ClientSubscriber(SseClient client, UserDTO user) {
            this.client = client;
            this.user = user;
        }
    }

    private final SessionManager sessionManager;
    private final Set<ClientSubscriber> subscribers = ConcurrentHashMap.newKeySet();
    private final JmsEventSubscriber jmsSubscriber = new JmsEventSubscriber();

    public SseStreamController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void startJmsBridge() {
        try {
            jmsSubscriber.start();
            jmsSubscriber.subscribe(JndiConfig.getOrdersTopicName(), this::broadcast);
            jmsSubscriber.subscribe(JndiConfig.getDeliveryTopicName(), this::broadcast);
            log.info("Pont SSE <-> JMS démarré.");
        } catch (Exception ex) {
            log.warn("Impossible de démarrer le pont SSE <-> JMS immédiatement ({}). Le SSE basculera en mode passif.", ex.getMessage());
        }
    }

    public void handleSse(SseClient client) {
        var optUser = sessionManager.getCurrentUser(client.ctx());
        if (optUser.isEmpty()) {
            client.sendEvent("error", "{\"message\":\"Non authentifié\"}");
            client.close();
            return;
        }
        ClientSubscriber sub = new ClientSubscriber(client, optUser.get());
        subscribers.add(sub);
        client.keepAlive();
        client.onClose(() -> subscribers.remove(sub));
        client.sendEvent("connected", "{\"status\":\"CONNECTED\",\"role\":\"" + optUser.get().getRole() + "\"}");
    }

    public void broadcast(LogisticsEvent event) {
        if (event == null) return;
        try {
            String json = MAPPER.writeValueAsString(event);
            for (ClientSubscriber sub : subscribers) {
                try {
                    if (shouldDeliverTo(sub.user, event)) {
                        sub.client.sendEvent(event.getEventType().name(), json);
                    }
                } catch (Exception ex) {
                    subscribers.remove(sub);
                }
            }
        } catch (Exception ex) {
            log.error("Erreur diffusion SSE", ex);
        }
    }

    private boolean shouldDeliverTo(UserDTO user, LogisticsEvent event) {
        if (user.getRole().name().equals("ADMINISTRATEUR")) {
            return true;
        }
        if (event.getTargetUserId() != null && event.getTargetUserId().equals(user.getId())) {
            return true;
        }
        if (event.getTargetRole() != null && event.getTargetRole() == user.getRole()) {
            return true;
        }
        // Les mises à jour GPS sont diffusées aux clients/livreurs connectés qui filtrent par orderId côté UI
        return event.getEventType().name().equals("POSITION_UPDATED")
                || event.getEventType().name().equals("DELIVERY_STARTED")
                || event.getEventType().name().equals("DELIVERY_COMPLETED");
    }
}
