package com.smartlogistics.common.dto;

import java.io.Serializable;

public class OrderItemRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private int quantity;

    public OrderItemRequestDTO() {}

    public OrderItemRequestDTO(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
