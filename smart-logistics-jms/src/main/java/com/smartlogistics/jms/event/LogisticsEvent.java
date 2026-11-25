package com.smartlogistics.jms.event;

import com.smartlogistics.common.enums.EventType;
import com.smartlogistics.common.enums.Role;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Événement métier transporté sur le bus JMS ActiveMQ.
 */
public class LogisticsEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private EventType eventType;
    private Long orderId;
    private String orderCode;
    private Long warehouseId;
    private Long driverId;
    private Long deliveryId;
    private Long targetUserId;
    private Role targetRole;
    private String title;
    private String message;
    private Double lat;
    private Double lng;
    private Double remainingKm;
    private Integer estimatedMinutes;
    private LocalDateTime timestamp = LocalDateTime.now();

    public LogisticsEvent() {}

    public static LogisticsEvent of(EventType type, String title, String message) {
        LogisticsEvent ev = new LogisticsEvent();
        ev.setEventType(type);
        ev.setTitle(title);
        ev.setMessage(message);
        return ev;
    }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public Role getTargetRole() { return targetRole; }
    public void setTargetRole(Role targetRole) { this.targetRole = targetRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Double getRemainingKm() { return remainingKm; }
    public void setRemainingKm(Double remainingKm) { this.remainingKm = remainingKm; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
