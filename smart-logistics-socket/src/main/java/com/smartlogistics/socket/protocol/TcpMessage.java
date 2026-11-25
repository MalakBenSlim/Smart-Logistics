package com.smartlogistics.socket.protocol;

import java.time.LocalDateTime;

public class TcpMessage {
    private TcpMessageType type;
    private Long driverId;
    private Long deliveryId;
    private Double lat;
    private Double lng;
    private String status;
    private Double speedKmh;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public TcpMessage() {}

    public static TcpMessage ack(String message) {
        TcpMessage m = new TcpMessage();
        m.setType(TcpMessageType.ACK);
        m.setMessage(message);
        return m;
    }

    public static TcpMessage error(String message) {
        TcpMessage m = new TcpMessage();
        m.setType(TcpMessageType.ERROR);
        m.setMessage(message);
        return m;
    }

    public TcpMessageType getType() { return type; }
    public void setType(TcpMessageType type) { this.type = type; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Long getDeliveryId() { return deliveryId; }
    public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getSpeedKmh() { return speedKmh; }
    public void setSpeedKmh(Double speedKmh) { this.speedKmh = speedKmh; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
