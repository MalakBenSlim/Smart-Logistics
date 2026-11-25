package com.smartlogistics.database.seed;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.NotificationDTO;
import com.smartlogistics.common.dto.OrderDTO;
import com.smartlogistics.common.dto.OrderItemDTO;
import com.smartlogistics.common.dto.PaymentDTO;
import com.smartlogistics.common.dto.ProductDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.UserDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.DeliveryStatus;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.OrderStatus;
import com.smartlogistics.common.enums.PaymentMethod;
import com.smartlogistics.common.enums.PaymentStatus;
import com.smartlogistics.common.enums.Role;
import com.smartlogistics.common.enums.WarehouseStatus;
import com.smartlogistics.common.security.PasswordUtil;
import com.smartlogistics.database.dao.DeliveryDao;
import com.smartlogistics.database.dao.DriverDao;
import com.smartlogistics.database.dao.NotificationDao;
import com.smartlogistics.database.dao.OrderDao;
import com.smartlogistics.database.dao.ProductDao;
import com.smartlogistics.database.dao.SmartDecisionDao;
import com.smartlogistics.database.dao.StockDao;
import com.smartlogistics.database.dao.UserDao;
import com.smartlogistics.database.dao.WarehouseDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Initialise automatiquement un jeu de données complet et réaliste
 * permettant de démontrer immédiatement les 5 scénarios académiques.
 */
public class DemoDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserDao userDao = new UserDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();
    private final ProductDao productDao = new ProductDao();
    private final StockDao stockDao = new StockDao();
    private final DriverDao driverDao = new DriverDao();
    private final OrderDao orderDao = new OrderDao();
    private final DeliveryDao deliveryDao = new DeliveryDao();
    private final NotificationDao notificationDao = new NotificationDao();
    private final SmartDecisionDao smartDecisionDao = new SmartDecisionDao();

    public void seedIfEmpty() throws SQLException {
        userDao.ensureRolesExist();
        if (userDao.countUsers() > 0) {
            log.info("Base déjà peuplée ({} utilisateurs). Seeding ignoré.", userDao.countUsers());
            return;
        }
        log.info("Peuplement initial des données de démonstration Smart Logistics...");

        // 1. Utilisateurs (8 comptes couvrant les 4 rôles)
        UserDTO admin = createUser("Sami Ben Ali", "admin@smartlogistics.tn", "Admin123!",
                "+216 71 100 200", "Avenue Habib Bourguiba, Tunis", 36.8002, 10.1865, Role.ADMINISTRATEUR);
        UserDTO stockMgr = createUser("Nadia Trabelsi", "stock@smartlogistics.tn", "Stock123!",
                "+216 74 210 300", "Zone Industrielle Poudrière, Sfax", 34.7380, 10.7550, Role.GESTIONNAIRE_STOCK);
        UserDTO client1 = createUser("Youssef Mansouri", "client@smartlogistics.tn", "Client123!",
                "+216 55 320 410", "Lac 2, Les Berges du Lac, Tunis", 36.8435, 10.2682, Role.CLIENT);
        UserDTO client2 = createUser("Amira Gharbi", "amira@smartlogistics.tn", "Client123!",
                "+216 52 840 190", "Route de l'Aéroport Km 4, Sfax", 34.7250, 10.7180, Role.CLIENT);

        UserDTO drvUser1 = createUser("Ahmed Jaziri", "ahmed.driver@smartlogistics.tn", "Driver123!",
                "+216 22 410 510", "Centre Ville, Sfax", 34.7420, 10.7610, Role.LIVREUR);
        UserDTO drvUser2 = createUser("Karim Bouazizi", "karim.driver@smartlogistics.tn", "Driver123!",
                "+216 24 615 720", "Bab Bhar, Tunis", 36.7995, 10.1800, Role.LIVREUR);
        UserDTO drvUser3 = createUser("Mehdi Chaabane", "mehdi.driver@smartlogistics.tn", "Driver123!",
                "+216 29 512 830", "Sahloul, Sousse", 35.8310, 10.6100, Role.LIVREUR);
        UserDTO drvUser4 = createUser("Bilel Haddad", "bilel.driver@smartlogistics.tn", "Driver123!",
                "+216 21 903 115", "Sakiet Ezzit, Sfax", 34.7780, 10.7720, Role.LIVREUR);

        // 2. Dépôts (4 dépôts tunisiens)
        WarehouseDTO wTunis = createWarehouse("Dépôt Tunis", "Zone Logistique Charguia II, Tunis",
                36.8470, 10.2060, 500, 340);
        WarehouseDTO wSfax = createWarehouse("Dépôt Sfax", "Route de Gabès Km 3, Sfax",
                34.7320, 10.7450, 600, 252);
        WarehouseDTO wSousse = createWarehouse("Dépôt Sousse", "Pôle Industriel Sidi Abdelhamid, Sousse",
                35.8020, 10.6420, 450, 160);
        WarehouseDTO wGabes = createWarehouse("Dépôt Gabès", "Zone Industrielle Ghannouch, Gabès",
                33.9080, 10.0720, 350, 90);

        List<WarehouseDTO> warehouses = List.of(wTunis, wSfax, wSousse, wGabes);

        // 3. Produits (20 produits)
        List<ProductDTO> products = new ArrayList<>();
        products.add(createProduct("PRD-001", "Laptop Pro 15\"", "Informatique", "PC portable professionnel i7 16Go RAM SSD 512Go", 2850.0, 5));
        products.add(createProduct("PRD-002", "Souris Sans Fil Ergonomique", "Périphériques", "Souris optique Bluetooth haute précision", 85.0, 5));
        products.add(createProduct("PRD-003", "Clavier Mécanique AZERTY", "Périphériques", "Clavier rétroéclairé switches silencieux", 210.0, 5));
        products.add(createProduct("PRD-004", "Écran 27\" 4K IPS", "Informatique", "Moniteur professionnel USB-C HDR", 1190.0, 4));
        products.add(createProduct("PRD-005", "Station d'accueil USB-C", "Accessoires", "Hub 10-en-1 HDMI Ethernet PD 100W", 240.0, 5));
        products.add(createProduct("PRD-006", "Casque Audio ANC Pro", "Périphériques", "Casque sans fil réduction de bruit active", 420.0, 5));
        products.add(createProduct("PRD-007", "Routeur Wi-Fi 6 AX3000", "Réseau", "Routeur double bande gigabit entreprise", 340.0, 5));
        products.add(createProduct("PRD-008", "Switch Manageable 24 Ports", "Réseau", "Switch Rackable Gigabit PoE+", 890.0, 3));
        products.add(createProduct("PRD-009", "Imprimante Laser Multifonction", "Impression", "Impression recto-verso Wi-Fi 35 ppm", 960.0, 4));
        products.add(createProduct("PRD-010", "Scanner Code-Barres 2D", "Impression", "Lecteur optique sans fil pour entrepôt", 310.0, 5));
        products.add(createProduct("PRD-011", "Disque SSD NVMe 1To", "Informatique", "Stockage interne haute vitesse PCIe 4.0", 295.0, 6));
        products.add(createProduct("PRD-012", "Onduleur APC 1200VA", "Accessoires", "Protection électrique avec régulation AVR", 480.0, 4));
        products.add(createProduct("PRD-013", "Webcam Full HD 1080p", "Périphériques", "Caméra conférence double micro", 165.0, 5));
        products.add(createProduct("PRD-014", "Tablette Logistique Durcie", "Informatique", "Terminal mobile Android IP67 GPS", 1350.0, 3));
        products.add(createProduct("PRD-015", "Câble Réseau Cat6 50m", "Réseau", "Bobine RJ45 blindée S/FTP", 110.0, 8));
        products.add(createProduct("PRD-016", "Support PC Aluminium", "Accessoires", "Rehausseur ergonomique ventilé", 95.0, 6));
        products.add(createProduct("PRD-017", "Toner Laser Noir XL", "Impression", "Cartouche haute capacité 8000 pages", 190.0, 5));
        products.add(createProduct("PRD-018", "Point d'accès Wi-Fi Plafond", "Réseau", "Borne PoE entreprise longue portée", 410.0, 4));
        products.add(createProduct("PRD-019", "Sac à dos Business 16\"", "Accessoires", "Sac renforcé étanche port USB", 145.0, 6));
        products.add(createProduct("PRD-020", "Imprimante Thermique Étiquettes", "Impression", "Impression étiquettes colis 150mm/s", 540.0, 4));

        // 4. Stocks multi-dépôts (conçus pour démontrer les scénarios 1, 2 et 3)
        for (int i = 0; i < products.size(); i++) {
            ProductDTO p = products.get(i);
            int qTunis = 10 + (i * 3) % 18;
            int qSfax = 15 + (i * 5) % 25;
            int qSousse = 12 + (i * 4) % 20;
            int qGabes = 8 + (i * 2) % 14;

            // Scénario 2 : Souris (index 1) indisponible à Tunis -> force la sélection intelligente de Sousse/Sfax !
            if (i == 1) {
                qTunis = 0;
                qSfax = 35;
                qSousse = 22;
            }
            // Scénario 3 : Routeur Wi-Fi 6 (index 6) proche du seuil critique (stock=5, seuil=5)
            if (i == 6) {
                qTunis = 3;
                qSfax = 5;
            }
            // Quelques ruptures réalistes sur Gabès / Tunis pour le dashboard Gestionnaire Stock
            if (i == 13 || i == 19) {
                qGabes = 0;
            }
            if (i == 8) {
                qTunis = 2;
            }

            stockDao.upsertQuantity(wTunis.getId(), p.getId(), qTunis);
            stockDao.upsertQuantity(wSfax.getId(), p.getId(), qSfax);
            stockDao.upsertQuantity(wSousse.getId(), p.getId(), qSousse);
            stockDao.upsertQuantity(wGabes.getId(), p.getId(), qGabes);
        }

        // 5. Livreurs (5 livreurs dont certains occupés pour Scénario 4)
        DriverDTO dAhmed = createDriver(drvUser1.getId(), "Ahmed Jaziri", "+216 22 410 510",
                "Utilitaire Kangoo", 34.7406, 10.7580, DriverStatus.DISPONIBLE, 0, 42);
        DriverDTO dKarim = createDriver(drvUser2.getId(), "Karim Bouazizi", "+216 24 615 720",
                "Camionnette Partner", 36.8180, 10.1950, DriverStatus.DISPONIBLE, 0, 38);
        DriverDTO dMehdi = createDriver(drvUser3.getId(), "Mehdi Chaabane", "+216 29 512 830",
                "Moto Express", 35.8220, 10.6340, DriverStatus.DISPONIBLE, 0, 29);
        DriverDTO dBilel = createDriver(drvUser4.getId(), "Bilel Haddad", "+216 21 903 115",
                "Fourgon Boxer", 34.7360, 10.7500, DriverStatus.EN_LIVRAISON, 2, 51);
        DriverDTO dWalid = createDriver(null, "Walid Ben Salah", "+216 26 700 940",
                "Utilitaire Berlingo", 33.8950, 10.0880, DriverStatus.HORS_SERVICE, 0, 19);

        // 6. Commandes historiques et actives (10 commandes variées)
        seedSampleOrder("CMD-2026-1001", client1, wSousse, products.get(0), 1, products.get(1), 1,
                OrderStatus.DELIVERED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Lac 2, Les Berges du Lac, Tunis", 36.8435, 10.2682, dKarim, DeliveryStatus.LIVREE, 89.4, 92.1);

        seedSampleOrder("CMD-2026-1002", client2, wSfax, products.get(3), 1, products.get(4), 2,
                OrderStatus.IN_DELIVERY, PaymentMethod.COD, PaymentStatus.EN_ATTENTE,
                "Route de l'Aéroport Km 4, Sfax", 34.7250, 10.7180, dBilel, DeliveryStatus.EN_ROUTE, 91.2, 94.0);

        seedSampleOrder("CMD-2026-1003", client1, wTunis, products.get(2), 2, products.get(5), 1,
                OrderStatus.PREPARED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Ennasr 2, Ariana", 36.8620, 10.1650, null, null, 88.5, 0);

        seedSampleOrder("CMD-2026-1004", client2, wSfax, products.get(9), 1, products.get(19), 1,
                OrderStatus.DRIVER_ASSIGNED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Cité El Habib, Sfax", 34.7490, 10.7350, dAhmed, DeliveryStatus.ASSIGNEE, 93.0, 95.2);

        seedSampleOrder("CMD-2026-1005", client1, wTunis, products.get(10), 2, null, 0,
                OrderStatus.CREATED, PaymentMethod.COD, PaymentStatus.EN_ATTENTE,
                "La Marsa, Tunis", 36.8780, 10.3240, null, null, 90.1, 0);

        seedSampleOrder("CMD-2026-1006", client2, wSfax, products.get(7), 1, null, 0,
                OrderStatus.DELIVERED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Sakiet Eddaier, Sfax", 34.7950, 10.7850, dAhmed, DeliveryStatus.LIVREE, 92.4, 91.5);

        seedSampleOrder("CMD-2026-1007", client1, wSousse, products.get(11), 1, products.get(14), 1,
                OrderStatus.DELIVERED, PaymentMethod.COD, PaymentStatus.ACCEPTE,
                "Hammam Sousse", 35.8600, 10.5950, dMehdi, DeliveryStatus.LIVREE, 94.0, 93.8);

        seedSampleOrder("CMD-2026-1008", client2, wGabes, products.get(15), 3, null, 0,
                OrderStatus.DELIVERED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Menzel El Habib, Gabès", 33.8820, 10.0950, dWalid, DeliveryStatus.LIVREE, 87.0, 85.0);

        seedSampleOrder("CMD-2026-1009", client1, wTunis, products.get(12), 2, null, 0,
                OrderStatus.CANCELLED, PaymentMethod.COD, PaymentStatus.REFUSE,
                "Menzah 6, Ariana", 36.8510, 10.1720, null, null, 86.2, 0);

        seedSampleOrder("CMD-2026-1010", client2, wSfax, products.get(0), 1, products.get(2), 1,
                OrderStatus.SHIPPED, PaymentMethod.CARTE_SIMULEE, PaymentStatus.ACCEPTE,
                "Route Teniour Km 5, Sfax", 34.7650, 10.7420, dBilel, DeliveryStatus.EN_ROUTE, 91.0, 90.4);

        // 7. Notifications initiales
        createNotif(admin.getId(), Role.ADMINISTRATEUR, EventType.LOW_STOCK,
                "Alerte Stock Faible", "Routeur Wi-Fi 6 AX3000 à Dépôt Tunis : 3 unités (seuil : 5).");
        createNotif(stockMgr.getId(), Role.GESTIONNAIRE_STOCK, EventType.LOW_STOCK,
                "Rupture de Stock Détectée", "Souris Sans Fil Ergonomique en rupture à Dépôt Tunis (0 unité).");
        createNotif(client1.getId(), Role.CLIENT, EventType.ORDER_CREATED,
                "Commande CMD-2026-1005 créée", "Dépôt Tunis sélectionné (Score 90.1/100).");
        createNotif(drvUser1.getId(), Role.LIVREUR, EventType.DRIVER_ASSIGNED,
                "Nouvelle livraison CMD-2026-1004", "Colis prêt au Dépôt Sfax à destination de Cité El Habib.");

        log.info("Seeding terminé : {} dépôts, {} produits, 5 livreurs, 10 commandes.", warehouses.size(), products.size());
    }

    private UserDTO createUser(String name, String email, String rawPass, String phone,
                               String address, double lat, double lng, Role role) throws SQLException {
        UserDTO u = new UserDTO();
        u.setFullName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setAddress(address);
        u.setLatitude(lat);
        u.setLongitude(lng);
        u.setRole(role);
        u.setActive(true);
        return userDao.create(u, PasswordUtil.hash(rawPass));
    }

    private WarehouseDTO createWarehouse(String name, String address, double lat, double lng, int maxCap, int load)
            throws SQLException {
        WarehouseDTO w = new WarehouseDTO();
        w.setName(name);
        w.setAddress(address);
        w.setLatitude(lat);
        w.setLongitude(lng);
        w.setMaxCapacity(maxCap);
        w.setCurrentLoad(load);
        w.setStatus(WarehouseStatus.ACTIF);
        return warehouseDao.save(w);
    }

    private ProductDTO createProduct(String sku, String name, String category, String desc, double price, int threshold)
            throws SQLException {
        ProductDTO p = new ProductDTO();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setDescription(desc);
        p.setUnitPrice(price);
        p.setLowStockThreshold(threshold);
        p.setActive(true);
        return productDao.save(p);
    }

    private DriverDTO createDriver(Long userId, String name, String phone, String vehicle,
                                   double lat, double lng, DriverStatus status, int active, int total)
            throws SQLException {
        DriverDTO d = new DriverDTO();
        d.setUserId(userId);
        d.setFullName(name);
        d.setPhone(phone);
        d.setVehicleType(vehicle);
        d.setCurrentLat(lat);
        d.setCurrentLng(lng);
        d.setStatus(status);
        d.setActiveDeliveries(active);
        d.setTotalDeliveries(total);
        return driverDao.save(d);
    }

    private void seedSampleOrder(String code, UserDTO client, WarehouseDTO wh,
                                 ProductDTO p1, int q1, ProductDTO p2, int q2,
                                 OrderStatus status, PaymentMethod payMethod, PaymentStatus payStatus,
                                 String addr, double lat, double lng,
                                 DriverDTO driver, DeliveryStatus delStatus,
                                 double whScore, double drvScore) throws SQLException {
        List<OrderItemDTO> items = new ArrayList<>();
        OrderItemDTO i1 = new OrderItemDTO();
        i1.setProductId(p1.getId());
        i1.setQuantity(q1);
        i1.setUnitPrice(p1.getUnitPrice());
        items.add(i1);
        double total = p1.getUnitPrice() * q1;
        if (p2 != null && q2 > 0) {
            OrderItemDTO i2 = new OrderItemDTO();
            i2.setProductId(p2.getId());
            i2.setQuantity(q2);
            i2.setUnitPrice(p2.getUnitPrice());
            items.add(i2);
            total += p2.getUnitPrice() * q2;
        }

        OrderDTO order = new OrderDTO();
        order.setOrderCode(code);
        order.setClientId(client.getId());
        order.setWarehouseId(wh.getId());
        order.setStatus(status);
        order.setTotalAmount(total);
        order.setDeliveryCost(7.0);
        order.setDeliveryAddress(addr);
        order.setDeliveryLat(lat);
        order.setDeliveryLng(lng);
        order.setEstimatedMinutes(30);

        PaymentDTO payment = new PaymentDTO();
        payment.setMethod(payMethod);
        payment.setStatus(payStatus);
        payment.setAmount(total + 7.0);
        payment.setTransactionRef("TX-" + code);

        OrderDTO saved = orderDao.createOrderWithItemsAndPayment(order, items, payment);

        SmartDecisionDTO decision = new SmartDecisionDTO();
        decision.setOrderId(saved.getId());
        decision.setSelectedWarehouseId(wh.getId());
        decision.setSelectedWarehouseName(wh.getName());
        decision.setWarehouseScore(whScore);
        decision.setSelectedLoadPercent(wh.getLoadPercentage());
        decision.setSelectedEstimatedMinutes(30);
        decision.setSelectedEstimatedCost(7.0);
        decision.setWarehouseReasons(List.of(
                "Tous les produits disponibles en stock",
                "Charge du dépôt optimale (" + Math.round(wh.getLoadPercentage()) + "%)",
                "Meilleur score global multicritère (" + whScore + "/100)"
        ));
        if (driver != null) {
            decision.setSelectedDriverId(driver.getId());
            decision.setSelectedDriverName(driver.getFullName());
            decision.setDriverScore(drvScore);
            decision.setDriverReasons(List.of(
                    "Proximité du dépôt " + wh.getName(),
                    "Véhicule adapté : " + driver.getVehicleType(),
                    "Score livreur : " + drvScore + "/100"
            ));
            DeliveryDTO del = deliveryDao.createDelivery(saved.getId(), driver.getId(), wh.getLatitude(), wh.getLongitude(), 4.2);
            if (delStatus != null && delStatus != DeliveryStatus.ASSIGNEE) {
                deliveryDao.updateStatus(del.getId(), delStatus);
            }
        }
        smartDecisionDao.saveOrUpdate(decision);
    }

    private void createNotif(Long userId, Role role, EventType type, String title, String msg) throws SQLException {
        NotificationDTO n = new NotificationDTO();
        n.setUserId(userId);
        n.setTargetRole(role);
        n.setEventType(type);
        n.setTitle(title);
        n.setMessage(msg);
        notificationDao.create(n);
    }
}
