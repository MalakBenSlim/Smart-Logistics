package com.smartlogistics.common.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardStatsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalUsers;
    private int totalOrders;
    private int activeOrders;
    private int deliveredOrders;
    private int cancelledOrders;
    private double totalRevenue;
    private int totalDrivers;
    private int availableDrivers;
    private int totalWarehouses;
    private int outOfStockCount;
    private int lowStockCount;

    private Map<String, Integer> ordersByDay = new LinkedHashMap<>();
    private Map<String, Integer> deliveriesByDay = new LinkedHashMap<>();
    private Map<String, Integer> topProducts = new LinkedHashMap<>();
    private Map<String, Double> warehouseLoads = new LinkedHashMap<>();
    private Map<String, Integer> driverPerformances = new LinkedHashMap<>();

    public DashboardStatsDTO() {}

    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public int getActiveOrders() { return activeOrders; }
    public void setActiveOrders(int activeOrders) { this.activeOrders = activeOrders; }
    public int getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(int deliveredOrders) { this.deliveredOrders = deliveredOrders; }
    public int getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalDrivers() { return totalDrivers; }
    public void setTotalDrivers(int totalDrivers) { this.totalDrivers = totalDrivers; }
    public int getAvailableDrivers() { return availableDrivers; }
    public void setAvailableDrivers(int availableDrivers) { this.availableDrivers = availableDrivers; }
    public int getTotalWarehouses() { return totalWarehouses; }
    public void setTotalWarehouses(int totalWarehouses) { this.totalWarehouses = totalWarehouses; }
    public int getOutOfStockCount() { return outOfStockCount; }
    public void setOutOfStockCount(int outOfStockCount) { this.outOfStockCount = outOfStockCount; }
    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }
    public Map<String, Integer> getOrdersByDay() { return ordersByDay; }
    public void setOrdersByDay(Map<String, Integer> ordersByDay) { this.ordersByDay = ordersByDay; }
    public Map<String, Integer> getDeliveriesByDay() { return deliveriesByDay; }
    public void setDeliveriesByDay(Map<String, Integer> deliveriesByDay) { this.deliveriesByDay = deliveriesByDay; }
    public Map<String, Integer> getTopProducts() { return topProducts; }
    public void setTopProducts(Map<String, Integer> topProducts) { this.topProducts = topProducts; }
    public Map<String, Double> getWarehouseLoads() { return warehouseLoads; }
    public void setWarehouseLoads(Map<String, Double> warehouseLoads) { this.warehouseLoads = warehouseLoads; }
    public Map<String, Integer> getDriverPerformances() { return driverPerformances; }
    public void setDriverPerformances(Map<String, Integer> driverPerformances) { this.driverPerformances = driverPerformances; }
}
