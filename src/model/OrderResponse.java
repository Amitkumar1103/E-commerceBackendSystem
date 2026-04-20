package model;

import java.util.Map;

public class OrderResponse {
    private boolean success;
    private String message;
    private Order order;
    private OrderStatus status;
    private Map<Integer, Integer> availableStock;

    public OrderResponse(boolean success, String message, Order order,
            OrderStatus status, Map<Integer, Integer> availableStock) {
        this.success = success;
        this.message = message;
        this.order = order;
        this.status = status;
        this.availableStock = availableStock;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Map<Integer, Integer> getAvailableStock() {
        return availableStock;
    }
}