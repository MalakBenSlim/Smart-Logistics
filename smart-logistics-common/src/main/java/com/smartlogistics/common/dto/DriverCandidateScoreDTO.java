package com.smartlogistics.common.dto;

import java.io.Serializable;

public class DriverCandidateScoreDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long driverId;
    private String driverName;
    private String vehicleType;
    private boolean eligible;
    private String ineligibilityReason;
    private double distanceToWarehouseKm;
    private int activeDeliveries;
    private int estimatedApproachMinutes;
    private double totalScore;

    public DriverCandidateScoreDTO() {}

    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }
    public String getIneligibilityReason() { return ineligibilityReason; }
    public void setIneligibilityReason(String ineligibilityReason) { this.ineligibilityReason = ineligibilityReason; }
    public double getDistanceToWarehouseKm() { return distanceToWarehouseKm; }
    public void setDistanceToWarehouseKm(double distanceToWarehouseKm) { this.distanceToWarehouseKm = distanceToWarehouseKm; }
    public int getActiveDeliveries() { return activeDeliveries; }
    public void setActiveDeliveries(int activeDeliveries) { this.activeDeliveries = activeDeliveries; }
    public int getEstimatedApproachMinutes() { return estimatedApproachMinutes; }
    public void setEstimatedApproachMinutes(int estimatedApproachMinutes) { this.estimatedApproachMinutes = estimatedApproachMinutes; }
    public double getTotalScore() { return totalScore; }
    public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
}
