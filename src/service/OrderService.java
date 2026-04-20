package service;

import model.*;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OrderService {
    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    public List<Order> getOrdersByUser(int userId) {

        List<Order> orders = new ArrayList<>();

        String query = "SELECT * FROM orders WHERE user_id = ?";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Order order = new Order(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        new ArrayList<>(), // items empty for now
                        rs.getDouble("total"),
                        rs.getString("status"));

                orders.add(order);
            }

        } catch (Exception e) {
            System.out.println("Error fetching orders: " + e.getMessage());
        }

        return orders;
    }

    public OrderResponse placeOrder(Cart cart, ProductService productService, int orderId, int userId) {

        Connection con = null;

        try {
            con = DBConnection.getConnection();
            con.setAutoCommit(false); // 🔥 start transaction
            logger.info("Order started");
            Map<Integer, Integer> stockMap = new HashMap<>();
            Map<Integer, Product> productMap = new HashMap<>();
            List<OrderItem> orderItems = new ArrayList<>();
            double total = 0;
            cart.getItems().removeIf(item -> item.getQuantity() <= 0);
            if (cart.getItems().isEmpty()) {
                return new OrderResponse(false, "Cart is empty. Cannot place order.", null, OrderStatus.CART_EMPTY,
                        null);
            }

            // 🔹 VALIDATION
            for (CartItem item : cart.getItems()) {

                Product product = productService.getProduct(item.getProductId());
                if (item.getQuantity() <= 0) {
                    return new OrderResponse(false,
                            "Invalid quantity for product ID: " + item.getProductId(),
                            null,
                            OrderStatus.ERROR,
                            null);
                }
                if (product == null) {
                    return new OrderResponse(false, "Product not found", null, OrderStatus.ERROR, null);
                }
                productMap.put(product.getId(), product); // ✅ store once

                if (product.getStock() < item.getQuantity()) {
                    logger.warning("Out of stock for product ID: " + item.getProductId());
                    stockMap.put(product.getId(), product.getStock());
                }
            }
            if (!stockMap.isEmpty()) {
                return new OrderResponse(
                        false,
                        "Some items are out of stock. Please adjust your cart.",
                        null,
                        OrderStatus.OUT_OF_STOCK,
                        stockMap);
            }
            // 🔹 UPDATE STOCK
            for (CartItem item : cart.getItems()) {

                String query = "UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?";

                try (PreparedStatement ps = con.prepareStatement(query)) {

                    ps.setInt(1, item.getQuantity());
                    ps.setInt(2, item.getProductId());
                    ps.setInt(3, item.getQuantity());

                    int rowsAffected = ps.executeUpdate();

                    if (rowsAffected == 0) {
                        logger.warning("Out of stock for product ID: " + item.getProductId());
                        con.rollback();

                        return new OrderResponse(false,
                                "Out of stock for product ID: " + item.getProductId(),
                                null, OrderStatus.OUT_OF_STOCK, stockMap);
                    }
                }

                // ✅ use map instead of DB call
                Product product = productMap.get(item.getProductId());

                orderItems.add(new OrderItem(
                        product.getId(),
                        item.getQuantity(),
                        product.getPrice()));

                total += product.getPrice() * item.getQuantity();
            }

            // Save Order Table later
            String orderQuery = "INSERT INTO orders (user_id, total, status) VALUES (?, ?, ?)";
            int orderIdDB;

            try (PreparedStatement orderStmt = con.prepareStatement(
                    orderQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

                orderStmt.setInt(1, userId);
                orderStmt.setDouble(2, total);
                orderStmt.setString(3, "PLACED");

                orderStmt.executeUpdate();

                try (ResultSet rs = orderStmt.getGeneratedKeys()) {
                    rs.next();
                    orderIdDB = rs.getInt(1);
                }
            }

            String itemQuery = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";

            for (CartItem item : cart.getItems()) {

                Product product = productMap.get(item.getProductId());

                try (PreparedStatement itemStmt = con.prepareStatement(itemQuery)) {

                    itemStmt.setInt(1, orderIdDB);
                    itemStmt.setInt(2, product.getId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, product.getPrice());

                    itemStmt.executeUpdate();
                }
            }

            con.commit(); // ✅ SUCCESS

            Order order = new Order(orderIdDB, userId, orderItems, total, "PLACED");

            cart.getItems().clear();

            return new OrderResponse(true, "Order placed", order, OrderStatus.SUCCESS, null);

        } catch (SQLException e) {
            logger.severe("DB Error: " + e.getMessage());

            try {
                if (con != null)
                    con.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return new OrderResponse(false, "Transaction failed", null, OrderStatus.ERROR, null);
        } finally {
            try {
                if (con != null)
                    con.setAutoCommit(true);
                if (con != null)
                    con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}