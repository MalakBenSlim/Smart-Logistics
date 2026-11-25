package com.smartlogistics.common.dto;

import java.io.Serializable;

public class ProductDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String sku;
    private String name;
    private String category;
    private String description;
    private double unitPrice;
    private int lowStockThreshold;
    private boolean active;
    private int totalAvailableStock;

    public ProductDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getTotalAvailableStock() { return totalAvailableStock; }
    public void setTotalAvailableStock(int totalAvailableStock) { this.totalAvailableStock = totalAvailableStock; }
}
