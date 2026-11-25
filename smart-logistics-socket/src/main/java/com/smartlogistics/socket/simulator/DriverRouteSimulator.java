package com.smartlogistics.socket.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.socket.protocol.TcpMessage;
import com.smartlogistics.socket.protocol.TcpMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client Socket TCP simulant un terminal mobile livreur :
 * envoie AUTH -> DELIVERY_STARTED -> N * POSITION_UPDATE -> DELIVERY_COMPLETED.
 */
public class DriverRouteSimulator {
    private static final Logger log = LoggerFactory.getLogger(DriverRouteSimulator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void simulateRouteAsync(long driverId, long deliveryId,
                                          double startLat, double startLng,
                                          double targetLat, double targetLng,
                                          int steps, long delayMs) {
        Thread t = new Thread(() -> runRoute(driverId, deliveryId, startLat, startLng, targetLat, targetLng, steps, delayMs),
                "driver-tcp-sim-" + deliveryId);
        t.setDaemon(true);
        t.start();
    }

    public static void runRoute(long driverId, long deliveryId,
                                double startLat, double startLng,
                                double targetLat, double targetLng,
                                int steps, long delayMs) {
        String host = JndiConfig.getTcpServerHost();
        int port = JndiConfig.getTcpServerPort();

        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // 1. Authentification TCP
            TcpMessage auth = new TcpMessage();
            auth.setType(TcpMessageType.AUTH);
            auth.setDriverId(driverId);
            sendAndWaitAck(writer, reader, auth);

            // 2. Démarrage livraison
            TcpMessage start = new TcpMessage();
            start.setType(TcpMessageType.DELIVERY_STARTED);
            start.setDriverId(driverId);
            start.setDeliveryId(deliveryId);
            start.setLat(startLat);
            start.setLng(startLng);
            sendAndWaitAck(writer, reader, start);

            // 3. Interpolation GPS temps réel
            for (int i = 1; i <= steps; i++) {
                Thread.sleep(delayMs);
                double ratio = (double) i / (steps + 1);
                double lat = startLat + (targetLat - startLat) * ratio;
                double lng = startLng + (targetLng - startLng) * ratio;

                TcpMessage pos = new TcpMessage();
                pos.setType(TcpMessageType.POSITION_UPDATE);
                pos.setDriverId(driverId);
                pos.setDeliveryId(deliveryId);
                pos.setLat(Math.round(lat * 100000.0) / 100000.0);
                pos.setLng(Math.round(lng * 100000.0) / 100000.0);
                pos.setSpeedKmh(42.0);
                sendAndWaitAck(writer, reader, pos);
            }

            Thread.sleep(delayMs);

            // 4. Livraison terminée
            TcpMessage done = new TcpMessage();
            done.setType(TcpMessageType.DELIVERY_COMPLETED);
            done.setDriverId(driverId);
            done.setDeliveryId(deliveryId);
            done.setLat(targetLat);
            done.setLng(targetLng);
            sendAndWaitAck(writer, reader, done);

            log.info("Simulation trajet TCP terminée pour livraison #{}", deliveryId);
        } catch (Exception ex) {
            log.error("Erreur dans DriverRouteSimulator", ex);
        }
    }

    private static void sendAndWaitAck(BufferedWriter writer, BufferedReader reader, TcpMessage msg) throws Exception {
        writer.write(MAPPER.writeValueAsString(msg));
        writer.newLine();
        writer.flush();
        String ack = reader.readLine();
        log.debug("TCP Server ACK: {}", ack);
    }

    /**
     * Permet de lancer le simulateur directement depuis IntelliJ comme processus indépendant.
     * Args optionnels : <driverId> <deliveryId>
     */
    public static void main(String[] args) {
        long driverId = args.length > 0 ? Long.parseLong(args[0]) : 1L;
        long deliveryId = args.length > 1 ? Long.parseLong(args[1]) : 4L;
        // Trajet simulé autour de Sfax
        runRoute(driverId, deliveryId, 34.7320, 10.7450, 34.7490, 10.7350, 8, 1200);
    }
}
