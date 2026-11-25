package com.smartlogistics.common.dto;

import com.smartlogistics.common.enums.PaymentMethod;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreateOrderRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long clientId;
    private String deliveryAddress;
    private double deliveryLat;
    private double deliveryLng;
    private PaymentMethod paymentMethod;
    private String simulatedCardNumber;
    private List<OrderItemRequestDTO> items = new ArrayList<>();

    public CreateOrderRequestDTO() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public double getDeliveryLat() { return deliveryLat; }
    public void setDeliveryLat(double deliveryLat) { this.deliveryLat = deliveryLat; }
    public double getDeliveryLng() { return deliveryLng; }
    public void setDeliveryLng(double deliveryLng) { this.deliveryLng = deliveryLng; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getSimulatedCardNumber() { return simulatedCardNumber; }
    public void setSimulatedCardNumber(String simulatedCardNumber) { this.simulatedCardNumber = simulatedCardNumber; }
    public List<OrderItemRequestDTO> getItems() { return items; }
    public void setItems(List<OrderItemRequestDTO> items) { this.items = items; }
}
