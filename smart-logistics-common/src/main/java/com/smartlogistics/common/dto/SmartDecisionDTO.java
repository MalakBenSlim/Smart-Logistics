package com.smartlogistics.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SmartDecisionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long selectedWarehouseId;
    private String selectedWarehouseName;
    private double warehouseScore;
    private double selectedDistanceKm;
    private double selectedLoadPercent;
    private int selectedEstimatedMinutes;
    private double selectedEstimatedCost;
    private List<String> warehouseReasons = new ArrayList<>();
    private List<WarehouseCandidateScoreDTO> warehouseCandidates = new ArrayList<>();

    private Long selectedDriverId;
    private String selectedDriverName;
    private double driverScore;
    private List<String> driverReasons = new ArrayList<>();
    private List<DriverCandidateScoreDTO> driverCandidates = new ArrayList<>();
    private LocalDateTime createdAt;

    public SmartDecisionDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getSelectedWarehouseId() { return selectedWarehouseId; }
    public void setSelectedWarehouseId(Long selectedWarehouseId) { this.selectedWarehouseId = selectedWarehouseId; }
    public String getSelectedWarehouseName() { return selectedWarehouseName; }
    public void setSelectedWarehouseName(String selectedWarehouseName) { this.selectedWarehouseName = selectedWarehouseName; }
    public double getWarehouseScore() { return warehouseScore; }
    public void setWarehouseScore(double warehouseScore) { this.warehouseScore = warehouseScore; }
    public double getSelectedDistanceKm() { return selectedDistanceKm; }
    public void setSelectedDistanceKm(double selectedDistanceKm) { this.selectedDistanceKm = selectedDistanceKm; }
    public double getSelectedLoadPercent() { return selectedLoadPercent; }
    public void setSelectedLoadPercent(double selectedLoadPercent) { this.selectedLoadPercent = selectedLoadPercent; }
    public int getSelectedEstimatedMinutes() { return selectedEstimatedMinutes; }
    public void setSelectedEstimatedMinutes(int selectedEstimatedMinutes) { this.selectedEstimatedMinutes = selectedEstimatedMinutes; }
    public double getSelectedEstimatedCost() { return selectedEstimatedCost; }
    public void setSelectedEstimatedCost(double selectedEstimatedCost) { this.selectedEstimatedCost = selectedEstimatedCost; }
    public List<String> getWarehouseReasons() { return warehouseReasons; }
    public void setWarehouseReasons(List<String> warehouseReasons) { this.warehouseReasons = warehouseReasons; }
    public List<WarehouseCandidateScoreDTO> getWarehouseCandidates() { return warehouseCandidates; }
    public void setWarehouseCandidates(List<WarehouseCandidateScoreDTO> warehouseCandidates) { this.warehouseCandidates = warehouseCandidates; }
    public Long getSelectedDriverId() { return selectedDriverId; }
    public void setSelectedDriverId(Long selectedDriverId) { this.selectedDriverId = selectedDriverId; }
    public String getSelectedDriverName() { return selectedDriverName; }
    public void setSelectedDriverName(String selectedDriverName) { this.selectedDriverName = selectedDriverName; }
    public double getDriverScore() { return driverScore; }
    public void setDriverScore(double driverScore) { this.driverScore = driverScore; }
    public List<String> getDriverReasons() { return driverReasons; }
    public void setDriverReasons(List<String> driverReasons) { this.driverReasons = driverReasons; }
    public List<DriverCandidateScoreDTO> getDriverCandidates() { return driverCandidates; }
    public void setDriverCandidates(List<DriverCandidateScoreDTO> driverCandidates) { this.driverCandidates = driverCandidates; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
