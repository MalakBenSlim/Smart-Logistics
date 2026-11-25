package com.smartlogistics.common.dto;

import com.smartlogistics.common.enums.WarehouseStatus;
import java.io.Serializable;

public class WarehouseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private int maxCapacity;
    private int currentLoad;
    private WarehouseStatus status;

    public WarehouseDTO() {}

    public double getLoadPercentage() {
        if (maxCapacity <= 0) return 100.0;
        return Math.min(100.0, (currentLoad * 100.0) / maxCapacity);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }
    public int getCurrentLoad() { return currentLoad; }
    public void setCurrentLoad(int currentLoad) { this.currentLoad = currentLoad; }
    public WarehouseStatus getStatus() { return status; }
    public void setStatus(WarehouseStatus status) { this.status = status; }
}
