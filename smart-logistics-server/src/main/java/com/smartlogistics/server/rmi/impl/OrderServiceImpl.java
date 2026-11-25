package com.smartlogistics.server.rmi.impl;

import com.smartlogistics.common.dto.CreateOrderRequestDTO;
import com.smartlogistics.common.dto.DashboardStatsDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.PaymentDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.OrderStatus;
import com.smartlogistics.common.enums.PaymentMethod;
import com.smartlogistics.common.enums.PaymentStatus;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.exception.AuthException;
import com.smartlogistics.common.exception.BusinessException;
import com.smartlogistics.common.jndi.ServiceLocator;
import com.smartlogistics.common.rmi.DeliveryService;
import com.smartlogistics.common.rmi.OrderService;
import com.smartlogistics.common.rmi.StockService;
import com.smartlogistics.common.rmi.WarehouseService;
import com.smartlogistics.common.security.InputValidator;
import com.smartlogistics.database.dao.DashboardStatsDao;
import com.smartlogistics.database.dao.OrderDao;
import com.smartlogistics.database.dao.ProductDao;
import com.smartlogistics.database.dao.SmartDecisionDao;
import com.smartlogistics.database.dao.WarehouseDao;
import com.smartlogistics.jms.event.LogisticsEvent;
import com.smartlogistics.jms.producer.JmsEventPublisher;
import com.smartlogistics.server.algorithm.GeoDistanceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderServiceImpl extends UnicastRemoteObject implements OrderService {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderDao orderDao = new OrderDao();
    private final ProductDao productDao = new ProductDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final SmartDecisionDao smartDecisionDao = new SmartDecisionDao();
    private final DashboardStatsDao statsDao = new DashboardStatsDao();
    private final JmsEventPublisher eventPublisher = new JmsEventPublisher();

    public OrderServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public OrderDTO createOrder(CreateOrderRequestDTO request) throws RemoteException, BusinessException {
        if (request == null || request.getClientId() == null) {
            throw new BusinessException("VALIDATION_ERROR", "Requête de commande invalide.");
        }
        InputValidator.requireNonBlank(request.getDeliveryAddress(), "adresse de livraison");
        InputValidator.requireValidCoordinates(request.getDeliveryLat(), request.getDeliveryLng());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("EMPTY_CART", "Le panier est vide.");
        }
        for (OrderItemRequestDTO item : request.getItems()) {
            InputValidator.requirePositiveInt(item.getQuantity(), "quantité");
        }

        // Simulation paiement par carte : carte terminant par 0000 simule un refus bancaire
        PaymentMethod method = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD;
        if (method == PaymentMethod.CARTE_SIMULEE) {
            String card = request.getSimulatedCardNumber();
            if (card == null || card.replaceAll("\\s+", "").length() < 12) {
                throw new BusinessException("PAYMENT_VALIDATION", "Numéro de carte simulée invalide (min. 12 chiffres).");
            }
            if (card.trim().endsWith("0000")) {
                throw new BusinessException("PAYMENT_REFUSED",
                        "Paiement refusé par le simulateur bancaire (carte de test terminant par 0000).");
            }
        }

        // 1. Découverte des services distants via JNDI -> RMI
        WarehouseService warehouseService = ServiceLocator.getRmiService(ServiceLocator.WAREHOUSE_SERVICE, WarehouseService.class);
        StockService stockService = ServiceLocator.getRmiService(ServiceLocator.STOCK_SERVICE, StockService.class);

        // 2. Exécution du Smart Warehouse Optimizer via RMI
        SmartDecisionDTO decision = warehouseService.findBestWarehouse(
                request.getItems(), request.getDeliveryLat(), request.getDeliveryLng());
        Long selectedWarehouseId = decision.getSelectedWarehouseId();

        // 3. Réservation atomique du stock via RMI
        stockService.reserveStockForOrder(selectedWarehouseId, request.getItems());

        try {
            // 4. Calcul des totaux côté serveur (ne jamais faire confiance aux prix envoyés par le client)
            List<OrderItemDTO> orderItems = new ArrayList<>();
            double totalAmount = 0.0;
            for (OrderItemRequestDTO reqItem : request.getItems()) {
                ProductDTO product = productDao.findById(reqItem.getProductId())
                        .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND",
                                "Produit introuvable #" + reqItem.getProductId()));
                OrderItemDTO item = new OrderItemDTO();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setProductSku(product.getSku());
                item.setQuantity(reqItem.getQuantity());
                item.setUnitPrice(product.getUnitPrice());
                orderItems.add(item);
                totalAmount += product.getUnitPrice() * reqItem.getQuantity();
            }

            double deliveryCost = decision.getSelectedEstimatedCost();
            String orderCode = "CMD-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

            OrderDTO order = new OrderDTO();
            order.setOrderCode(orderCode);
            order.setClientId(request.getClientId());
            order.setWarehouseId(selectedWarehouseId);
            order.setStatus(OrderStatus.CREATED);
            order.setTotalAmount(Math.round(totalAmount * 100.0) / 100.0);
            order.setDeliveryCost(deliveryCost);
            order.setDeliveryAddress(request.getDeliveryAddress());
            order.setDeliveryLat(request.getDeliveryLat());
            order.setDeliveryLng(request.getDeliveryLng());
            order.setEstimatedMinutes(decision.getSelectedEstimatedMinutes());

            PaymentDTO payment = new PaymentDTO();
            payment.setMethod(method);
            payment.setStatus(method == PaymentMethod.CARTE_SIMULEE ? PaymentStatus.ACCEPTE : PaymentStatus.EN_ATTENTE);
            payment.setAmount(Math.round((totalAmount + deliveryCost) * 100.0) / 100.0);
            payment.setTransactionRef("TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            OrderDTO saved = orderDao.createOrderWithItemsAndPayment(order, orderItems, payment);
            decision.setOrderId(saved.getId());
            smartDecisionDao.saveOrUpdate(decision);
            warehouseDao.adjustLoad(selectedWarehouseId, orderItems.stream().mapToInt(OrderItemDTO::getQuantity).sum());

            // 5. Publication des événements asynchrones JMS
            LogisticsEvent evCreated = LogisticsEvent.of(EventType.ORDER_CREATED,
                    "Commande " + saved.getOrderCode() + " créée",
                    "Votre commande a été enregistrée (" + saved.getTotalAmount() + " DT).");
            evCreated.setOrderId(saved.getId());
            evCreated.setOrderCode(saved.getOrderCode());
            evCreated.setTargetUserId(saved.getClientId());
            evCreated.setTargetRole(Role.CLIENT);
            eventPublisher.publish(evCreated);

            LogisticsEvent evWh = LogisticsEvent.of(EventType.WAREHOUSE_SELECTED,
                    "Dépôt sélectionné : " + decision.getSelectedWarehouseName(),
                    "Score Smart Optimizer : " + decision.getWarehouseScore() + "/100 pour " + saved.getOrderCode());
            evWh.setOrderId(saved.getId());
            evWh.setOrderCode(saved.getOrderCode());
            evWh.setWarehouseId(selectedWarehouseId);
            evWh.setTargetRole(Role.GESTIONNAIRE_STOCK);
            eventPublisher.publish(evWh);

            log.info("Commande {} créée -> Dépôt {} (Score {}/100)",
                    saved.getOrderCode(), decision.getSelectedWarehouseName(), decision.getWarehouseScore());

            return orderDao.findById(saved.getId()).orElse(saved);
        } catch (Exception ex) {
            // Compensation : libérer le stock réservé si la persistance échoue
            try {
                stockService.releaseStockForOrder(selectedWarehouseId, request.getItems());
            } catch (Exception ignored) {
            }
            if (ex instanceof BusinessException be) throw be;
            if (ex instanceof RemoteException re) throw re;
            throw new RemoteException("Erreur création commande", ex);
        }
    }

    @Override
    public OrderDTO getOrderById(Long orderId, Long requesterId, Role role)
            throws RemoteException, BusinessException {
        try {
            OrderDTO order = orderDao.findById(orderId)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Commande introuvable."));
            if (role == Role.CLIENT && !order.getClientId().equals(requesterId)) {
                throw new AuthException("FORBIDDEN", "Accès non autorisé à cette commande.");
            }
            return order;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture commande", ex);
        }
    }

    @Override
    public List<OrderDTO> getOrdersByClient(Long clientId) throws RemoteException {
        try {
            return orderDao.findByClient(clientId);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture commandes client", ex);
        }
    }

    @Override
    public List<OrderDTO> getOrdersByWarehouse(Long warehouseId) throws RemoteException {
        try {
            return orderDao.findByWarehouse(warehouseId);
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture commandes dépôt", ex);
        }
    }

    @Override
    public List<OrderDTO> getAllOrders() throws RemoteException {
        try {
            return orderDao.findAll();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur lecture commandes", ex);
        }
    }

    @Override
    public boolean cancelOrder(Long orderId, Long requesterId, Role role)
            throws RemoteException, BusinessException {
        try {
            OrderDTO order = orderDao.findById(orderId)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Commande introuvable."));
            if (role == Role.CLIENT && !order.getClientId().equals(requesterId)) {
                throw new AuthException("FORBIDDEN", "Vous ne pouvez annuler que vos propres commandes.");
            }
            if (order.getStatus() == OrderStatus.IN_DELIVERY
                    || order.getStatus() == OrderStatus.DELIVERED
                    || order.getStatus() == OrderStatus.CANCELLED) {
                throw new BusinessException("INVALID_STATE",
                        "Impossible d'annuler une commande au statut " + order.getStatus());
            }

            orderDao.updateStatus(orderId, OrderStatus.CANCELLED);
            if (order.getWarehouseId() != null) {
                StockService stockService = ServiceLocator.getRmiService(ServiceLocator.STOCK_SERVICE, StockService.class);
                List<OrderItemRequestDTO> items = order.getItems().stream()
                        .map(i -> new OrderItemRequestDTO(i.getProductId(), i.getQuantity()))
                        .toList();
                stockService.releaseStockForOrder(order.getWarehouseId(), items);
                int totalUnits = order.getItems().stream().mapToInt(OrderItemDTO::getQuantity).sum();
                warehouseDao.adjustLoad(order.getWarehouseId(), -totalUnits);
            }

            LogisticsEvent ev = LogisticsEvent.of(EventType.ORDER_CANCELLED,
                    "Commande " + order.getOrderCode() + " annulée",
                    "Le stock réservé a été automatiquement libéré.");
            ev.setOrderId(order.getId());
            ev.setOrderCode(order.getOrderCode());
            ev.setTargetUserId(order.getClientId());
            ev.setTargetRole(Role.CLIENT);
            eventPublisher.publish(ev);
            return true;
        } catch (SQLException ex) {
            throw new RemoteException("Erreur annulation commande", ex);
        }
    }

    @Override
    public OrderDTO markOrderPrepared(Long orderId) throws RemoteException, BusinessException {
        try {
            OrderDTO order = orderDao.findById(orderId)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Commande introuvable."));
            if (order.getStatus() != OrderStatus.CREATED) {
                throw new BusinessException("INVALID_STATE", "Seule une commande CREATED peut être préparée.");
            }

            orderDao.updateStatus(orderId, OrderStatus.PREPARED);

            LogisticsEvent prepEv = LogisticsEvent.of(EventType.ORDER_PREPARED,
                    "Commande " + order.getOrderCode() + " préparée",
                    "Colis emballé au " + order.getWarehouseName() + ". Recherche du meilleur livreur...");
            prepEv.setOrderId(orderId);
            prepEv.setOrderCode(order.getOrderCode());
            prepEv.setTargetUserId(order.getClientId());
            prepEv.setTargetRole(Role.CLIENT);
            eventPublisher.publish(prepEv);

            // Affectation automatique du meilleur livreur via JNDI -> DeliveryService RMI
            DeliveryService deliveryService = ServiceLocator.getRmiService(ServiceLocator.DELIVERY_SERVICE, DeliveryService.class);
            deliveryService.assignBestDriver(orderId);

            return orderDao.findById(orderId).orElseThrow();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur préparation commande", ex);
        }
    }

    @Override
    public OrderDTO shipOrder(Long orderId) throws RemoteException, BusinessException {
        try {
            OrderDTO order = orderDao.findById(orderId)
                    .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Commande introuvable."));
            if (order.getStatus() != OrderStatus.DRIVER_ASSIGNED && order.getStatus() != OrderStatus.PREPARED) {
                throw new BusinessException("INVALID_STATE", "La commande doit être prête avec un livreur affecté.");
            }

            if (order.getWarehouseId() != null) {
                List<OrderItemRequestDTO> items = order.getItems().stream()
                        .map(i -> new OrderItemRequestDTO(i.getProductId(), i.getQuantity()))
                        .toList();
                new com.smartlogistics.database.dao.StockDao().commitReservedStockOnShip(order.getWarehouseId(), items);
            }

            orderDao.updateStatus(orderId, OrderStatus.SHIPPED);

            LogisticsEvent shipEv = LogisticsEvent.of(EventType.ORDER_SHIPPED,
                    "Commande " + order.getOrderCode() + " expédiée",
                    "Votre colis a quitté le dépôt " + order.getWarehouseName() + ".");
            shipEv.setOrderId(orderId);
            shipEv.setOrderCode(order.getOrderCode());
            shipEv.setTargetUserId(order.getClientId());
            shipEv.setTargetRole(Role.CLIENT);
            eventPublisher.publish(shipEv);

            return orderDao.findById(orderId).orElseThrow();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur expédition commande", ex);
        }
    }

    @Override
    public double calculateDeliveryCost(double distanceKm) throws RemoteException {
        return GeoDistanceUtil.estimateDeliveryCost(distanceKm);
    }

    @Override
    public DashboardStatsDTO getAdminDashboardStats() throws RemoteException {
        try {
            return statsDao.computeStats();
        } catch (SQLException ex) {
            throw new RemoteException("Erreur calcul statistiques", ex);
        }
    }
}
