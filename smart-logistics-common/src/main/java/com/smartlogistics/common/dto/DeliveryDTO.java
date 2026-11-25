package com.smartlogistics.common.dto;

import com.smartlogistics.common.enums.DeliveryStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public class DeliveryDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderCode;
    private Long driverId;
    private String driverName;
    private String driverPhone;
    private String vehicleType;
    private DeliveryStatus status;
    private double lastLat;
    private double lastLng;
    private double warehouseLat;
    private double warehouseLng;
    private double targetLat;
    private double targetLng;
    private String deliveryAddress;
    private double remainingKm;
    private int estimatedMinutesRemaining;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime deliveredAt;

    public DeliveryDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public double getLastLat() { return lastLat; }
    public void setLastLat(double lastLat) { this.lastLat = lastLat; }
    public double getLastLng() { return lastLng; }
    public void setLastLng(double lastLng) { this.lastLng = lastLng; }
    public double getWarehouseLat() { return warehouseLat; }
    public void setWarehouseLat(double warehouseLat) { this.warehouseLat = warehouseLat; }
    public double getWarehouseLng() { return warehouseLng; }
    public void setWarehouseLng(double warehouseLng) { this.warehouseLng = warehouseLng; }
    public double getTargetLat() { return targetLat; }
    public void setTargetLat(double targetLat) { this.targetLat = targetLat; }
    public double getTargetLng() { return targetLng; }
    public void setTargetLng(double targetLng) { this.targetLng = targetLng; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public double getRemainingKm() { return remainingKm; }
    public void setRemainingKm(double remainingKm) { this.remainingKm = remainingKm; }
    public int getEstimatedMinutesRemaining() { return estimatedMinutesRemaining; }
    public void setEstimatedMinutesRemaining(int estimatedMinutesRemaining) { this.estimatedMinutesRemaining = estimatedMinutesRemaining; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
