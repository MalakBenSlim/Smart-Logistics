package com.smartlogistics.server.algorithm;

import com.smartlogistics.common.dto.DriverCandidateScoreDTO;
import com.smartlogistics.common.dto.DriverDTO;
import com.smartlogistics.common.dto.SmartDecisionDTO;
import com.smartlogistics.common.dto.WarehouseDTO;
import com.smartlogistics.common.enums.DriverStatus;
import com.smartlogistics.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Smart Driver Optimizer :
 * Sélectionne le meilleur livreur selon :
 * Proximité au dépôt (50%), Disponibilité/Charge (30%), Temps d'approche estimé (20%).
 */
public class DriverOptimizer {

    private static final double W_PROXIMITY = 0.50;
    private static final double W_LOAD = 0.30;
    private static final double W_ETA = 0.20;

    public SmartDecisionDTO selectBestDriver(
            WarehouseDTO warehouse,
            List<DriverDTO> drivers,
            SmartDecisionDTO existingDecision) throws BusinessException {

        List<DriverCandidateScoreDTO> candidates = new ArrayList<>();

        for (DriverDTO d : drivers) {
            DriverCandidateScoreDTO c = new DriverCandidateScoreDTO();
            c.setDriverId(d.getId());
            c.setDriverName(d.getFullName());
            c.setVehicleType(d.getVehicleType());
            c.setActiveDeliveries(d.getActiveDeliveries());

            double distKm = GeoDistanceUtil.haversineKm(
                    warehouse.getLatitude(), warehouse.getLongitude(),
                    d.getCurrentLat(), d.getCurrentLng());
            c.setDistanceToWarehouseKm(distKm);

            double speed = speedForVehicle(d.getVehicleType());
            int etaMin = GeoDistanceUtil.estimateTransitMinutes(distKm, speed);
            c.setEstimatedApproachMinutes(etaMin);

            if (d.getStatus() != DriverStatus.DISPONIBLE) {
                c.setEligible(false);
                c.setIneligibilityReason("Statut actuel : " + d.getStatus());
                candidates.add(c);
                continue;
            }

            double sProx = Math.max(0.0, 100.0 * (1.0 - distKm / 120.0));
            double sLoad = Math.max(0.0, 100.0 - d.getActiveDeliveries() * 25.0);
            double sEta = Math.max(0.0, 100.0 * (1.0 - etaMin / 150.0));

            double score = Math.round((W_PROXIMITY * sProx + W_LOAD * sLoad + W_ETA * sEta) * 100.0) / 100.0;
            c.setEligible(true);
            c.setTotalScore(score);
            candidates.add(c);
        }

        DriverCandidateScoreDTO winner = candidates.stream()
                .filter(DriverCandidateScoreDTO::isEligible)
                .max(Comparator.comparingDouble(DriverCandidateScoreDTO::getTotalScore))
                .orElseThrow(() -> new BusinessException("NO_DRIVER_AVAILABLE",
                        "Aucun livreur disponible actuellement pour prendre en charge cette livraison."));

        candidates.sort(Comparator
                .comparing(DriverCandidateScoreDTO::isEligible).reversed()
                .thenComparing(Comparator.comparingDouble(DriverCandidateScoreDTO::getTotalScore).reversed()));

        SmartDecisionDTO decision = existingDecision != null ? existingDecision : new SmartDecisionDTO();
        decision.setSelectedDriverId(winner.getDriverId());
        decision.setSelectedDriverName(winner.getDriverName());
        decision.setDriverScore(winner.getTotalScore());
        decision.setDriverCandidates(candidates);

        List<String> reasons = new ArrayList<>();
        reasons.add("Distance jusqu'au dépôt : " + winner.getDistanceToWarehouseKm() + " km");
        reasons.add("État : Disponible (" + winner.getActiveDeliveries() + " livraison en cours)");
        reasons.add("Véhicule : " + winner.getVehicleType() + " (approche ~" + winner.getEstimatedApproachMinutes() + " min)");
        reasons.add("Score livreur : " + winner.getTotalScore() + "/100");
        decision.setDriverReasons(reasons);

        return decision;
    }

    private double speedForVehicle(String vehicleType) {
        if (vehicleType == null) return 38.0;
        String v = vehicleType.toLowerCase();
        if (v.contains("moto")) return 48.0;
        if (v.contains("fourgon") || v.contains("camion")) return 34.0;
        return 40.0;
    }
}
