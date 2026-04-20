package model;

import java.util.List;

public class Order {
    private int id;
    private int userId;
    private List<OrderItem> items;
    private double totalAmount;
    private String status;

    public Order(int id, int userId, List<OrderItem> items, double totalAmount, String status) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}