package model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private int id;
    private int userId;
    private List<CartItem> items;

    public Cart(int id, int userId) {
        this.id = id;
        this.userId = userId;
        this.items = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return items;
    }
}