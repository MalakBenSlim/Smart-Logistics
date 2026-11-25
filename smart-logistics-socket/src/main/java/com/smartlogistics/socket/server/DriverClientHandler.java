package com.smartlogistics.socket.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.enums.DeliveryStatus;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.socket.protocol.TcpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Handler concurrent exécuté dans un thread dédié par terminal livreur connecté.
 */
public class DriverClientHandler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(DriverClientHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Socket socket;
    private final Map<Long, DriverClientHandler> activeDrivers;
    private Long authenticatedDriverId;

    public DriverClientHandler(Socket socket, Map<Long, DriverClientHandler> activeDrivers) {
        this.socket = socket;
        this.activeDrivers = activeDrivers;
    }

    @Override
    public void run() {
        String remote = socket.getRemoteSocketAddress().toString();
        log.info("Connexion TCP livreur acceptée depuis {}", remote);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                TcpMessage response = processLine(line);
                writer.write(MAPPER.writeValueAsString(response));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException ex) {
            log.debug("Connexion TCP fermée ({}) : {}", remote, ex.getMessage());
        } finally {
            if (authenticatedDriverId != null) {
                activeDrivers.remove(authenticatedDriverId, this);
                log.info("Livreur #{} déconnecté du serveur TCP.", authenticatedDriverId);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private TcpMessage processLine(String line) {
        try {
            TcpMessage msg = MAPPER.readValue(line, TcpMessage.class);
            if (msg.getType() == null) {
                return TcpMessage.error("Type de message TCP manquant.");
            }
            switch (msg.getType()) {
                case AUTH -> {
                    if (msg.getDriverId() == null) {
                        return TcpMessage.error("driverId requis pour AUTH.");
                    }
                    this.authenticatedDriverId = msg.getDriverId();
                    activeDrivers.put(authenticatedDriverId, this);
                    return TcpMessage.ack("Authentification TCP acceptée pour livreur #" + authenticatedDriverId);
                }
                case HEARTBEAT -> {
                    return TcpMessage.ack("PONG");
                }
                case DELIVERY_STARTED -> {
                    DeliveryService ds = ServiceLocator.getRmiService(ServiceLocator.DELIVERY_SERVICE, DeliveryService.class);
                    if (msg.getDeliveryId() != null) {
                        ds.updateDeliveryStatus(msg.getDeliveryId(), DeliveryStatus.EN_ROUTE);
                    }
                    if (msg.getDriverId() != null && msg.getLat() != null && msg.getLng() != null) {
                        ds.updateDriverPosition(msg.getDriverId(), msg.getDeliveryId(), msg.getLat(), msg.getLng());
                    }
                    return TcpMessage.ack("Livraison démarrée (EN_ROUTE)");
                }
                case POSITION_UPDATE -> {
                    if (msg.getDriverId() == null || msg.getLat() == null || msg.getLng() == null) {
                        return TcpMessage.error("driverId, lat et lng requis pour POSITION_UPDATE.");
                    }
                    DeliveryService ds = ServiceLocator.getRmiService(ServiceLocator.DELIVERY_SERVICE, DeliveryService.class);
                    ds.updateDriverPosition(msg.getDriverId(), msg.getDeliveryId(), msg.getLat(), msg.getLng());
                    return TcpMessage.ack("Position GPS mise à jour");
                }
                case STATUS_UPDATE -> {
                    if (msg.getDeliveryId() == null || msg.getStatus() == null) {
                        return TcpMessage.error("deliveryId et status requis pour STATUS_UPDATE.");
                    }
                    DeliveryService ds = ServiceLocator.getRmiService(ServiceLocator.DELIVERY_SERVICE, DeliveryService.class);
                    ds.updateDeliveryStatus(msg.getDeliveryId(), DeliveryStatus.valueOf(msg.getStatus()));
                    return TcpMessage.ack("Statut livraison mis à jour : " + msg.getStatus());
                }
                case DELIVERY_COMPLETED -> {
                    if (msg.getDeliveryId() == null) {
                        return TcpMessage.error("deliveryId requis pour DELIVERY_COMPLETED.");
                    }
                    DeliveryService ds = ServiceLocator.getRmiService(ServiceLocator.DELIVERY_SERVICE, DeliveryService.class);
                    if (msg.getDriverId() != null && msg.getLat() != null && msg.getLng() != null) {
                        ds.updateDriverPosition(msg.getDriverId(), msg.getDeliveryId(), msg.getLat(), msg.getLng());
                    }
                    ds.updateDeliveryStatus(msg.getDeliveryId(), DeliveryStatus.LIVREE);
                    return TcpMessage.ack("Livraison confirmée (LIVREE)");
                }
                default -> {
                    return TcpMessage.error("Type non supporté : " + msg.getType());
                }
            }
        } catch (Exception ex) {
            log.warn("Erreur traitement message TCP : {}", ex.getMessage());
            return TcpMessage.error(ex.getMessage());
        }
    }
}
