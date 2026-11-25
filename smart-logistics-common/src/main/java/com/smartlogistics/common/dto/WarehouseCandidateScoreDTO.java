package com.smartlogistics.common.dto;

import java.io.Serializable;

public class WarehouseCandidateScoreDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long warehouseId;
    private String warehouseName;
    private boolean eligible;
    private String ineligibilityReason;
    private double distanceKm;
    private double loadPercent;
    private int estimatedMinutes;
    private double estimatedCost;
    private int availableDriversNearby;
    private double totalScore;
    private double distanceScore;
    private double loadScore;
    private double timeScore;
    private double costScore;
    private double driverScore;

    public WarehouseCandidateScoreDTO() {}

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public String getIneligibilityReason() { return ineligibilityReason; }
    public void setIneligibilityReason(String ineligibilityReason) { this.ineligibilityReason = ineligibilityReason; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public double getLoadPercent() { return loadPercent; }
    public void setLoadPercent(double loadPercent) { this.loadPercent = loadPercent; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
    public int getAvailableDriversNearby() { return availableDriversNearby; }
    public void setAvailableDriversNearby(int availableDriversNearby) { this.availableDriversNearby = availableDriversNearby; }
    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
    public double getDistanceScore() { return distanceScore; }
    public void setDistanceScore(double distanceScore) { this.distanceScore = distanceScore; }
    public double getLoadScore() { return loadScore; }
    public void setLoadScore(double loadScore) { this.loadScore = loadScore; }
    public double getTimeScore() { return timeScore; }
    public void setTimeScore(double timeScore) { this.timeScore = timeScore; }
    public double getCostScore() { return costScore; }
    public void setCostScore(double costScore) { this.costScore = costScore; }
    public double getDriverScore() { return driverScore; }
    public void setDriverScore(double driverScore) { this.driverScore = driverScore; }
}
