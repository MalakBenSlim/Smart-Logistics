package com.smartlogistics.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartlogistics.common.dto.DeliveryDTO;
import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.jndi.JndiConfig;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.socket.protocol.TcpMessage;
import com.smartlogistics.socket.protocol.TcpMessageType;
import com.smartlogistics.socket.simulator.DriverRouteSimulator;
import com.smartlogistics.web.config.RbacMiddleware;
import com.smartlogistics.web.config.RmiGatewayHelper;
import com.smartlogistics.web.config.SessionManager;
import io.javalin.Javalin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class DriverController {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final SessionManager sessionManager;

    public DriverController(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public static class StatusUpdateRequest {
        public DriverStatus status;
    }

    public static class PositionRequest {
        public double lat;
        public double lng;
    }

    public void registerRoutes(Javalin app) {
        app.get("/api/driver/profile", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR, Role.ADMINISTRATEUR);
            DriverDTO driver = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> svc.getDriverByUserId(user.getId()));
            ctx.json(driver);
        });

        app.put("/api/driver/status", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR);
            StatusUpdateRequest req = ctx.bodyAsClass(StatusUpdateRequest.class);
            DriverDTO driver = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> {
                        DriverDTO d = svc.getDriverByUserId(user.getId());
                        return svc.updateDriverStatus(d.getId(), req.status);
                    });
            ctx.json(driver);
        });

        app.get("/api/driver/deliveries", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR);
            List<DeliveryDTO> list = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> {
                        DriverDTO d = svc.getDriverByUserId(user.getId());
                        return svc.getDeliveriesByDriver(d.getId());
                    });
            ctx.json(list);
        });

        app.post("/api/driver/deliveries/{id}/start", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR);
            Long deliveryId = Long.parseLong(ctx.pathParam("id"));
            DriverDTO driver = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> svc.getDriverByUserId(user.getId()));

            TcpMessage msg = new TcpMessage();
            msg.setType(TcpMessageType.DELIVERY_STARTED);
            msg.setDriverId(driver.getId());
            msg.setDeliveryId(deliveryId);
            msg.setLat(driver.getCurrentLat());
            msg.setLng(driver.getCurrentLng());
            String ack = sendViaTcpSocket(driver.getId(), msg);
            ctx.json(Map.of("ok", true, "tcpAck", ack));
        });

        app.post("/api/driver/deliveries/{id}/position", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR);
            Long deliveryId = Long.parseLong(ctx.pathParam("id"));
            PositionRequest req = ctx.bodyAsClass(PositionRequest.class);
            DriverDTO driver = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> svc.getDriverByUserId(user.getId()));

            TcpMessage msg = new TcpMessage();
            msg.setType(TcpMessageType.POSITION_UPDATE);
            msg.setDriverId(driver.getId());
            msg.setDeliveryId(deliveryId);
            msg.setLat(req.lat);
            msg.setLng(req.lng);
            msg.setSpeedKmh(40.0);
            String ack = sendViaTcpSocket(driver.getId(), msg);
            ctx.json(Map.of("ok", true, "tcpAck", ack));
        });

        app.post("/api/driver/deliveries/{id}/complete", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR);
            Long deliveryId = Long.parseLong(ctx.pathParam("id"));
            DriverDTO driver = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> svc.getDriverByUserId(user.getId()));

            TcpMessage msg = new TcpMessage();
            msg.setType(TcpMessageType.DELIVERY_COMPLETED);
            msg.setDriverId(driver.getId());
            msg.setDeliveryId(deliveryId);
            String ack = sendViaTcpSocket(driver.getId(), msg);
            ctx.json(Map.of("ok", true, "tcpAck", ack));
        });

        app.post("/api/driver/deliveries/{id}/simulate-tcp", ctx -> {
            UserDTO user = RbacMiddleware.requireRolesApi(ctx, sessionManager, Role.LIVREUR, Role.ADMINISTRATEUR);
            Long deliveryId = Long.parseLong(ctx.pathParam("id"));

            DeliveryDTO target = RmiGatewayHelper.call(
                    ServiceLocator.DELIVERY_SERVICE, DeliveryService.class,
                    svc -> {
                        if (user.getRole() == Role.LIVREUR) {
                            DriverDTO d = svc.getDriverByUserId(user.getId());
                            return svc.getDeliveriesByDriver(d.getId()).stream()
                                    .filter(del -> del.getId().equals(deliveryId))
                                    .findFirst()
                                    .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Livraison introuvable."));
                        }
                        for (DriverDTO d : svc.getAllDrivers()) {
                            var found = svc.getDeliveriesByDriver(d.getId()).stream()
                                    .filter(del -> del.getId().equals(deliveryId))
                                    .findFirst();
                            if (found.isPresent()) return found.get();
                        }
                        throw new BusinessException("DELIVERY_NOT_FOUND", "Livraison introuvable.");
                    });

            double startLat = target.getWarehouseLat() != 0 ? target.getWarehouseLat() : target.getLastLat();
            double startLng = target.getWarehouseLng() != 0 ? target.getWarehouseLng() : target.getLastLng();
            DriverRouteSimulator.simulateRouteAsync(
                    target.getDriverId(),
                    target.getId(),
                    startLat, startLng,
                    target.getTargetLat(), target.getTargetLng(),
                    8, 1200);

            ctx.json(Map.of(
                    "started", true,
                    "message", "Simulation GPS TCP démarrée (8 étapes toutes les 1.2s vers " + target.getDeliveryAddress() + ")"
            ));
        });
    }

    private String sendViaTcpSocket(Long driverId, TcpMessage payload) throws BusinessException {
        try (Socket socket = new Socket(JndiConfig.getTcpServerHost(), JndiConfig.getTcpServerPort());
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            TcpMessage auth = new TcpMessage();
            auth.setType(TcpMessageType.AUTH);
            auth.setDriverId(driverId);
            writer.write(MAPPER.writeValueAsString(auth));
            writer.newLine();
            writer.flush();
            reader.readLine();

            writer.write(MAPPER.writeValueAsString(payload));
            writer.newLine();
            writer.flush();
            return reader.readLine();
        } catch (Exception ex) {
            throw new BusinessException("TCP_SERVER_ERROR",
                    "Impossible de joindre le serveur Socket TCP : " + ex.getMessage());
        }
    }
}
