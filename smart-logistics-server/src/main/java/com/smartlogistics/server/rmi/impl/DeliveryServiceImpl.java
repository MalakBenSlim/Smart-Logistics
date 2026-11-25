package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.DeliveryDTO;
import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.DeliveryStatus;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.OrderStatus;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.common.security.InputValidator;
import com.smartlogistics.database.dao.DeliveryDao;
import com.smartlogistics.database.dao.DriverDao;
import com.smartlogistics.database.dao.OrderDao;
import com.smartlogistics.database.dao.SmartDecisionDao;
import com.smartlogistics.database.dao.WarehouseDao;
import com.smartlogistics.jms.event.LogisticsEvent;
import com.smartlogistics.jms.producer.JmsEventPublisher;
import com.smartlogistics.server.algorithm.DriverOptimizer;
import com.smartlogistics.server.algorithm.GeoDistanceUtil;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;

public class DeliveryServiceImpl extends UnicastRemoteObject implements DeliveryService {
    private static final long serialVersionUID = 1L;

    private final DriverDao driverDao = new DriverDao();
    private final DeliveryDao deliveryDao = new DeliveryDao();
    private final OrderDao orderDao = new OrderDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final SmartDecisionDao smartDecisionDao = new SmartDecisionDao();
    private final DriverOptimizer driverOptimizer = new DriverOptimizer();
    private final JmsEventPublisher eventPublisher = new JmsEventPublisher();

    public DeliveryServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public List<DriverDTO> getAllDrivers() throws RemoteException {
        try {
            return driverDao.findAll();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture livreurs", ex);
        }
    }

    @Override
    public DriverDTO getDriverByUserId(Long userId) throws RemoteException, BusinessException {
        try {
            return driverDao.findByUserId(userId)
                    .orElseThrow(() -> new BusinessException("DRIVER_NOT_FOUND", "Profil livreur introuvable."));
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture livreur", ex);
        }
    }

    @Override
    public DriverDTO saveDriver(DriverDTO driver) throws RemoteException, BusinessException {
        if (driver == null) {
            throw new BusinessException("VALIDATION_ERROR", "Données livreur requises.");
        }
        InputValidator.requireNonBlank(driver.getFullName(), "nom du livreur");
        InputValidator.requireValidPhone(driver.getPhone());
        InputValidator.requireNonBlank(driver.getVehicleType(), "véhicule");
        InputValidator.requireValidCoordinates(driver.getCurrentLat(), driver.getCurrentLng());
        try {
            return driverDao.save(driver);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur sauvegarde livreur", ex);
        }
    }

    @Override
    public DriverDTO updateDriverStatus(Long driverId, DriverStatus status)
            throws RemoteException, BusinessException {
        if (driverId == null || status == null) {
            throw new BusinessException("VALIDATION_ERROR", "driverId et status requis.");
        }
        try {
            driverDao.updateStatus(driverId, status);
            return driverDao.findById(driverId)
                    .orElseThrow(() -> new BusinessException("DRIVER_NOT_FOUND", "Livreur introuvable."));
        } catch (SQLException ex) {
            throw new RemoteException("Erreur mise à jour statut livreur", ex);
        }
    }

    @Override
    public synchronized SmartDecisionDTO assignBestDriver(Long orderId)
            throws RemoteException, BusinessException {
        try {
            OrderDTO order = orderDao.findById(orderId)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Commande introuvable."));
            if (order.getWarehouseId() == null) {
                throw new BusinessException("NO_WAREHOUSE", "Aucun dépôt assigné à cette commande.");
            }
            WarehouseDTO warehouse = warehouseDao.findById(order.getWarehouseId())
                    .orElseThrow(() -> new BusinessException("WAREHOUSE_NOT_FOUND", "Dépôt introuvable."));

            List<DriverDTO> drivers = driverDao.findAll();
            SmartDecisionDTO existing = smartDecisionDao.findByOrderId(orderId).orElse(new SmartDecisionDTO());
            existing.setOrderId(orderId);

            SmartDecisionDTO updated = driverOptimizer.selectBestDriver(warehouse, drivers, existing);
            Long selectedDriverId = updated.getSelectedDriverId();

            double distToClient = GeoDistanceUtil.haversineKm(
                    warehouse.getLatitude(), warehouse.getLongitude(),
                    order.getDeliveryLat(), order.getDeliveryLng());

            deliveryDao.createDelivery(orderId, selectedDriverId,
                    warehouse.getLatitude(), warehouse.getLongitude(), distToClient);
            driverDao.markAssigned(selectedDriverId);
            orderDao.updateStatus(orderId, OrderStatus.DRIVER_ASSIGNED);
            smartDecisionDao.saveOrUpdate(updated);

            LogisticsEvent ev = LogisticsEvent.of(EventType.DRIVER_ASSIGNED,
                    "Livreur affecté : " + updated.getSelectedDriverName(),
                    "Commande " + order.getOrderCode() + " confiée à " + updated.getSelectedDriverName()
                            + " (Score " + updated.getDriverScore() + "/100).");
            ev.setOrderId(orderId);
            ev.setOrderCode(order.getOrderCode());
            ev.setDriverId(selectedDriverId);
            ev.setTargetUserId(order.getClientId());
            ev.setTargetRole(Role.CLIENT);
            eventPublisher.publish(ev);

            return updated;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur affectation livreur", ex);
        }
    }

    @Override
    public List<DeliveryDTO> getDeliveriesByDriver(Long driverId) throws RemoteException {
        try {
            return deliveryDao.findByDriverId(driverId);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture livraisons", ex);
        }
    }

    @Override
    public DeliveryDTO getDeliveryByOrderId(Long orderId) throws RemoteException {
        try {
            return deliveryDao.findByOrderId(orderId).orElse(null);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture livraison commande", ex);
        }
    }

    @Override
    public void updateDriverPosition(Long driverId, Long deliveryId, double lat, double lng)
            throws RemoteException, BusinessException {
        InputValidator.requireValidCoordinates(lat, lng);
        try {
            driverDao.updatePosition(driverId, lat, lng);
            if (deliveryId != null) {
                DeliveryDTO del = deliveryDao.findById(deliveryId)
                        .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Livraison introuvable."));
                double remainingKm = GeoDistanceUtil.haversineKm(lat, lng, del.getTargetLat(), del.getTargetLng());
                deliveryDao.updatePosition(deliveryId, lat, lng, remainingKm);

                LogisticsEvent ev = LogisticsEvent.of(EventType.POSITION_UPDATED,
                        "Position livreur actualisée",
                        "Distance restante : " + remainingKm + " km");
                ev.setOrderId(del.getOrderId());
                ev.setOrderCode(del.getOrderCode());
                ev.setDriverId(driverId);
                ev.setDeliveryId(deliveryId);
                ev.setLat(lat);
                ev.setLng(lng);
                ev.setRemainingKm(remainingKm);
                ev.setEstimatedMinutes(Math.max(1, (int) Math.ceil((remainingKm / 38.0) * 60.0)));
                eventPublisher.publish(ev);
            }
        } catch (SQLException ex) {
            throw new RemoteException("Erreur mise à jour position GPS", ex);
        }
    }

    @Override
    public DeliveryDTO updateDeliveryStatus(Long deliveryId, DeliveryStatus status)
            throws RemoteException, BusinessException {
        if (deliveryId == null || status == null) {
            throw new BusinessException("VALIDATION_ERROR", "deliveryId et status requis.");
        }
        try {
            DeliveryDTO del = deliveryDao.findById(deliveryId)
                    .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Livraison introuvable."));
            OrderDTO order = orderDao.findById(del.getOrderId()).orElseThrow();

            deliveryDao.updateStatus(deliveryId, status);

            if (status == DeliveryStatus.EN_ROUTE) {
                orderDao.updateStatus(order.getId(), OrderStatus.IN_DELIVERY);
                LogisticsEvent ev = LogisticsEvent.of(EventType.DELIVERY_STARTED,
                        "Livraison en cours (" + order.getOrderCode() + ")",
                        del.getDriverName() + " est en route vers " + order.getDeliveryAddress());
                ev.setOrderId(order.getId());
                ev.setOrderCode(order.getOrderCode());
                ev.setDriverId(del.getDriverId());
                ev.setDeliveryId(deliveryId);
                ev.setTargetUserId(order.getClientId());
                ev.setTargetRole(Role.CLIENT);
                eventPublisher.publish(ev);
            } else if (status == DeliveryStatus.LIVREE) {
                orderDao.updateStatus(order.getId(), OrderStatus.DELIVERED);
                orderDao.markPaymentAccepted(order.getId());
                driverDao.markCompleted(del.getDriverId());
                if (order.getWarehouseId() != null) {
                    int units = order.getItems().stream().mapToInt(OrderItemDTO::getQuantity).sum();
                    warehouseDao.adjustLoad(order.getWarehouseId(), -units);
                }

                LogisticsEvent ev = LogisticsEvent.of(EventType.DELIVERY_COMPLETED,
                        "Commande " + order.getOrderCode() + " livrée ✓",
                        "Colis remis avec succès par " + del.getDriverName() + ".");
                ev.setOrderId(order.getId());
                ev.setOrderCode(order.getOrderCode());
                ev.setDriverId(del.getDriverId());
                ev.setDeliveryId(deliveryId);
                ev.setTargetUserId(order.getClientId());
                ev.setTargetRole(Role.CLIENT);
                eventPublisher.publish(ev);
            }

            return deliveryDao.findById(deliveryId).orElseThrow();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur mise à jour statut livraison", ex);
        }
    }
}
