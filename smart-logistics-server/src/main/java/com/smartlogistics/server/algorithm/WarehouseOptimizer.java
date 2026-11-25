package com.smartlogistics.server.algorithm;

import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.OrderItemRequestDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.StockItemDTO;
import com.smartlogistics.common.dto.WarehouseCandidateScoreDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.enums.WarehouseStatus;
import com.smartlogistics.common.exception.InsufficientStockException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Smart Warehouse Optimizer :
 * Évalue tous les dépôts actifs sur 5 critères pondérés :
 * Distance (40%), Charge (25%), Temps estimé (15%), Coût (10%), Proximité livreurs (10%).
 */
public class WarehouseOptimizer {

    private static final double W_DISTANCE = 0.40;
    private static final double W_LOAD = 0.25;
    private static final double W_TIME = 0.15;
    private static final double W_COST = 0.10;
    private static final double W_DRIVER = 0.10;

    private static final double MAX_DISTANCE_KM = 500.0;
    private static final double MAX_TIME_MIN = 360.0;
    private static final double MAX_COST_DT = 230.0;

    public SmartDecisionDTO selectBestWarehouse(
            List<WarehouseDTO> warehouses,
            List<StockItemDTO> allStocks,
            List<DriverDTO> drivers,
            List<OrderItemRequestDTO> requestedItems,
            double clientLat,
            double clientLng) throws InsufficientStockException {

        // Indexer le stock disponible par (warehouseId -> (productId -> availableQty))
        Map<Long, Map<Long, Integer>> stockIndex = new HashMap<>();
        for (StockItemDTO s : allStocks) {
            stockIndex.computeIfAbsent(s.getWarehouseId(), k -> new HashMap<>())
                    .put(s.getProductId(), s.getAvailableQuantity());
        }

        List<WarehouseCandidateScoreDTO> candidates = new ArrayList<>();

        for (WarehouseDTO w : warehouses) {
            WarehouseCandidateScoreDTO candidate = new WarehouseCandidateScoreDTO();
            candidate.setWarehouseId(w.getId());
            candidate.setWarehouseName(w.getName());

            double distKm = GeoDistanceUtil.haversineKm(w.getLatitude(), w.getLongitude(), clientLat, clientLng);
            double loadPct = Math.round(w.getLoadPercentage() * 10.0) / 10.0;
            int prepMin = GeoDistanceUtil.estimatePreparationMinutes(loadPct);
            int transitMin = GeoDistanceUtil.estimateTransitMinutes(distKm, 52.0);
            int totalMin = prepMin + transitMin;
            double cost = GeoDistanceUtil.estimateDeliveryCost(distKm);

            int nearbyDrivers = countNearbyAvailableDrivers(w, drivers);

            candidate.setDistanceKm(distKm);
            candidate.setLoadPercent(loadPct);
            candidate.setEstimatedMinutes(totalMin);
            candidate.setEstimatedCost(cost);
            candidate.setAvailableDriversNearby(nearbyDrivers);

            // Vérification d'éligibilité stricte
            if (w.getStatus() != WarehouseStatus.ACTIF) {
                candidate.setEligible(false);
                candidate.setIneligibilityReason("Dépôt non actif (" + w.getStatus() + ")");
                candidates.add(candidate);
                continue;
            }
            if (loadPct >= 100.0) {
                candidate.setEligible(false);
                candidate.setIneligibilityReason("Dépôt saturé (100%)");
                candidates.add(candidate);
                continue;
            }

            String missingReason = checkStockSufficiency(stockIndex.getOrDefault(w.getId(), Map.of()), requestedItems);
            if (missingReason != null) {
                candidate.setEligible(false);
                candidate.setIneligibilityReason(missingReason);
                candidates.add(candidate);
                continue;
            }

            // Calcul des scores normalisés [0..100]
            double sDist = Math.max(0.0, 100.0 * (1.0 - distKm / MAX_DISTANCE_KM));
            double sLoad = Math.max(0.0, 100.0 - loadPct);
            double sTime = Math.max(0.0, 100.0 * (1.0 - totalMin / MAX_TIME_MIN));
            double sCost = Math.max(0.0, 100.0 * (1.0 - cost / MAX_COST_DT));
            double sDriver = Math.min(100.0, nearbyDrivers * 45.0);

            double totalScore = W_DISTANCE * sDist
                    + W_LOAD * sLoad
                    + W_TIME * sTime
                    + W_COST * sCost
                    + W_DRIVER * sDriver;

            candidate.setEligible(true);
            candidate.setDistanceScore(round2(sDist));
            candidate.setLoadScore(round2(sLoad));
            candidate.setTimeScore(round2(sTime));
            candidate.setCostScore(round2(sCost));
            candidate.setDriverScore(round2(sDriver));
            candidate.setTotalScore(round2(totalScore));
            candidates.add(candidate);
        }

        WarehouseCandidateScoreDTO winner = candidates.stream()
                .filter(WarehouseCandidateScoreDTO::isEligible)
                .max(Comparator.comparingDouble(WarehouseCandidateScoreDTO::getTotalScore))
                .orElseThrow(() -> new InsufficientStockException(
                        "Aucun dépôt ne dispose actuellement de l'intégralité du stock requis pour cette commande."));

        // Trier les candidats pour l'affichage comparatif dans l'UI
        candidates.sort(Comparator
                .comparing(WarehouseCandidateScoreDTO::isEligible).reversed()
                .thenComparing(Comparator.comparingDouble(WarehouseCandidateScoreDTO::getTotalScore).reversed()));

        SmartDecisionDTO decision = new SmartDecisionDTO();
        decision.setSelectedWarehouseId(winner.getWarehouseId());
        decision.setSelectedWarehouseName(winner.getWarehouseName());
        decision.setWarehouseScore(winner.getTotalScore());
        decision.setSelectedDistanceKm(winner.getDistanceKm());
        decision.setSelectedLoadPercent(winner.getLoadPercent());
        decision.setSelectedEstimatedMinutes(winner.getEstimatedMinutes());
        decision.setSelectedEstimatedCost(winner.getEstimatedCost());
        decision.setWarehouseCandidates(candidates);

        List<String> reasons = new ArrayList<>();
        reasons.add("Tous les produits disponibles en stock");
        reasons.add("Distance dépôt → client : " + winner.getDistanceKm() + " km");
        reasons.add("Charge actuelle du dépôt : " + winner.getLoadPercent() + "%");
        reasons.add("Livraison estimée : " + winner.getEstimatedMinutes() + " min");
        reasons.add("Coût estimé : " + winner.getEstimatedCost() + " DT");
        if (winner.getAvailableDriversNearby() > 0) {
            reasons.add(winner.getAvailableDriversNearby() + " livreur(s) disponible(s) à proximité");
        }
        long ineligibleCount = candidates.stream().filter(c -> !c.isEligible()).count();
        if (ineligibleCount > 0) {
            reasons.add(ineligibleCount + " autre(s) dépôt(s) écarté(s) pour rupture/indisponibilité");
        }
        decision.setWarehouseReasons(reasons);

        return decision;
    }

    private String checkStockSufficiency(Map<Long, Integer> warehouseStock, List<OrderItemRequestDTO> items) {
        for (OrderItemRequestDTO item : items) {
            int available = warehouseStock.getOrDefault(item.getProductId(), 0);
            if (available < item.getQuantity()) {
                return "Stock insuffisant pour produit #" + item.getProductId()
                        + " (dispo: " + available + ", requis: " + item.getQuantity() + ")";
            }
        }
        return null;
    }

    private int countNearbyAvailableDrivers(WarehouseDTO w, List<DriverDTO> drivers) {
        int count = 0;
        for (DriverDTO d : drivers) {
            if (d.getStatus() == DriverStatus.DISPONIBLE) {
                double dist = GeoDistanceUtil.haversineKm(w.getLatitude(), w.getLongitude(),
                        d.getCurrentLat(), d.getCurrentLng());
                if (dist <= 35.0) {
                    count++;
                }
            }
        }
        return count;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
