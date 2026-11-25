# 🚚 Smart Logistics — Intelligent Delivery Platform

Projet académique distribué réalisé en **Java 17** avec **Maven Multi-Module** pour **IntelliJ IDEA**.

---

## 1. Architecture et technologies distribuées

| Technologie | Module | Rôle réel dans la plateforme |
| :--- | :--- | :--- |
| **Java RMI** | `common`, `server` | Appels distants synchrones entre la passerelle Web et les 6 services métier (`AuthService`, `OrderService`, `WarehouseService`, `StockService`, `DeliveryService`, `NotificationService`). |
| **JNDI** | `common` (`ServiceLocator`) | Découverte dynamique des stubs RMI (`RegistryContextFactory` sur `:1099`) et des destinations JMS (`ActiveMQInitialContextFactory` sur `:61616`) sans adresses codées en dur. |
| **Apache ActiveMQ (JMS)** | `jms`, `server`, `web` | Bus événementiel asynchrone (Topics `smartlogistics.events.orders` & `smartlogistics.events.delivery`, Queue `smartlogistics.alerts.stock`). |
| **Sockets TCP Multithread** | `socket` | Serveur TCP concurrent (`DeliveryTcpServer` sur `:9095`, pool de threads + `ConcurrentHashMap`) recevant les positions GPS et statuts des livreurs. |
| **H2 TCP Server + HikariCP** | `database` | Base relationnelle centralisée (port `:9092`) accessible simultanément par tous les nœuds JVM (mode compatible PostgreSQL). |
| **Javalin + SSE** | `web` | Passerelle Web HTTP/REST sur `:8080`, sessions sécurisées, contrôle d'accès par rôle (RBAC) et streaming temps réel Server-Sent Events (SSE) vers le navigateur. |
| **BCrypt + Rate Limiter** | `common`, `server` | Hachage salé des mots de passe, validation serveur, protection anti brute-force (blocage après 5 échecs). |

---

## 2. Comment ouvrir et lancer le projet dans IntelliJ IDEA

1. Ouvrir **IntelliJ IDEA** → **File → Open...** → sélectionner le dossier `smart-logistics` (contenant le `pom.xml` parent).
2. Laisser IntelliJ importer les 6 modules Maven (cliquer sur **Load Maven Project** si proposé).
3. Les configurations de lancement sont déjà prêtes dans le menu déroulant en haut à droite (`.run/`) :

### Mode A — Lancement rapide (2 clics)
1. Lancer **`QuickStart: All Backend Cluster`** (`SmartLogisticsClusterLauncher`) — attend que la console affiche :
   `=== Tous les services distribués RMI + JMS + TCP sont EN LIGNE ===`
2. Lancer **`3. Web Gateway (Javalin :8080)`** (`AppWebGateway`).
3. Ouvrir un navigateur sur **http://localhost:8080**.

### Mode B — Lancement distribué multi-JVM (recommandé pour la soutenance)
Lancer dans l'ordre ces 3 processus séparés pour prouver l'architecture distribuée devant le jury :
1. **`1. Central Infrastructure (H2 + RMI + JMS)`** (`CentralInfrastructureLauncher`)
2. **`2. Business Services (RMI + TCP Socket)`** (`BusinessServicesLauncher`)
3. **`3. Web Gateway (Javalin :8080)`** (`AppWebGateway`)
4. *(Optionnel)* **`4. Driver TCP Simulator (Optional CLI)`** (`DriverRouteSimulator`) pour simuler un terminal livreur externe.

### Lancer les tests unitaires et d'intégration
```bash
mvn test
```

---

## 3. Comptes de démonstration pré-remplis

Des boutons de remplissage rapide sont intégrés directement sur `/login` :

| Rôle | Email | Mot de passe |
| :--- | :--- | :--- |
| **Client** | `client@smartlogistics.tn` | `Client123!` |
| **Gestionnaire Stock** | `stock@smartlogistics.tn` | `Stock123!` |
| **Livreur (Ahmed)** | `ahmed.driver@smartlogistics.tn` | `Driver123!` |
| **Administrateur** | `admin@smartlogistics.tn` | `Admin123!` |

---

## 4. Guide des 5 scénarios de démonstration

1. **Scénario 1 (Commande nominale + Smart Decision)** : Connectez-vous en `Client`, ajoutez *Clavier Mécanique* au panier, cliquez sur **Simuler Smart Optimizer** puis **Valider**. Observez le score multicritère et les explications du dépôt choisi.
2. **Scénario 2 (Stock insuffisant → bascule intelligente)** : Toujours en `Client` à Tunis, commandez **Laptop Pro 15" + Souris Sans Fil Ergonomique**. La souris étant en rupture à Tunis (0), l'algorithme écarte Tunis et sélectionne **Dépôt Sousse** ou **Sfax** en expliquant pourquoi.
3. **Scénario 3 (Stock faible → JMS `LOW_STOCK`)** : Connectez-vous en `Gestionnaire Stock` (ou `Admin`), passez un stock sous son seuil (ex. `<= 5`) dans **Stocks & Transferts**. Un événement JMS est publié dans `smartlogistics.alerts.stock` et une alerte apparaît instantanément dans la cloche de notification.
4. **Scénario 4 (Livreur occupé → sélection du meilleur livreur disponible)** : Dans l'onglet **Préparation Commandes** du `Gestionnaire Stock`, cliquez sur **Préparer & Affecter Livreur**. Les livreurs `EN_LIVRAISON` ou `HORS_SERVICE` sont ignorés et le meilleur livreur `DISPONIBLE` est affecté.
5. **Scénario 5 (Suivi GPS temps réel via Socket TCP → JMS → SSE)** : Ouvrez deux onglets navigateur : `Client` sur **Suivi Temps Réel** et `Livreur` sur **Télémétrie Socket TCP**. Cliquez sur **🚀 Simuler trajet GPS (Socket TCP)** côté Livreur et observez le marqueur du livreur avancer en direct sur la carte du Client.
