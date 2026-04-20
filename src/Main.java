import java.util.List;
import java.util.Map;

import model.*;
import service.*;

public class Main {
    public static void main(String[] args) {

        // SERVICES
        ProductService productService = new ProductService();
        CartService cartService = new CartService();
        OrderService orderService = new OrderService();

        // USER
        UserService userService = new UserService();

        // SIGNUP
        User user = userService.login("mayank@mail.com", "1234");

        if (user == null) {
            System.out.println("User not found, registering...");
            user = userService.register("Mayank", "mayank@mail.com", "1234");
        }

        if (user != null) {
            System.out.println("User logged in: " + user.getName());
        } else {
            System.out.println("User login/registration failed");
            return;
        }

        // CART
        Cart cart = new Cart(1, user.getId());

        // ADD PRODUCTS and show available products
        System.out.println("Available Products:");
        for (Product p : productService.getAllProducts()) {
            System.out.println(
                    p.getId() + " " + p.getName() +
                            " Rs." + p.getPrice() +
                            " Stock: " + p.getStock());
        }

        // ADD TO CART (edge case)
        cartService.addToCart(cart, 101, 1);
        cartService.addToCart(cart, 102, 100); // exceeds stock

        // VIEW CART
        System.out.println("\nCart:");
        cartService.viewCart(cart);

        Order order = null;

        // 🔹 PLACE ORDER (Step 4 - Response based)
        OrderResponse response = orderService.placeOrder(cart, productService, 1, user.getId());

        if (!response.isSuccess()) {
            System.out.println("DEBUG STATUS: " + response.getStatus());
            System.out.println("\nOrder Failed: " + response.getMessage());

            // ✅ Only retry for stock issue
            if (OrderStatus.OUT_OF_STOCK.equals(response.getStatus())) {

                Map<Integer, Integer> stockMap = response.getAvailableStock();
                if (stockMap == null) {
                    System.out.println("Stock info missing. Cannot retry safely.");
                    return;
                }
                for (CartItem item : cart.getItems()) {

                    int available = stockMap.getOrDefault(item.getProductId(), item.getQuantity());

                    if (available < item.getQuantity()) {

                        System.out.println("Only " + available +
                                " available for product ID " + item.getProductId() +
                                ". Adjusting quantity.");

                        if (available == 0) {
                            System.out.println("Removing item: " + item.getProductId());
                            item.setQuantity(0);
                        } else {
                            item.setQuantity(available);
                        }
                    }
                }

                // clean cart
                cart.getItems().removeIf(item -> item.getQuantity() <= 0);

                if (cart.getItems().isEmpty()) {
                    System.out.println("All items out of stock. Cannot place order.");
                    return;
                }

                System.out.println("\nRetrying with updated quantity...");

                response = orderService.placeOrder(cart, productService, 2, user.getId());

                if (response.isSuccess()) {

                    order = response.getOrder();

                    System.out.println("\nOrder Placed Successfully (Adjusted)!");
                    System.out.println("Order ID: " + order.getId());
                    System.out.println("Total: Rs." + order.getTotalAmount());

                } else if (OrderStatus.OUT_OF_STOCK.equals(response.getStatus())) {
                    System.out.println("Still out of stock after retry. Aborting.");
                }
                System.out.println("\nOrder History:");

                List<Order> orders = orderService.getOrdersByUser(user.getId());

                for (Order o : orders) {
                    System.out.println("Order ID: " + o.getId() +
                            " Total: Rs." + o.getTotalAmount() +
                            " Status: " + o.getStatus());
                }

            } else {
                // ❌ Do NOT retry for other errors
                System.out.println("Retry Failed: " + response.getMessage());
            }
        }

        // 🔹 DISPLAY ORDER DETAILS
        if (order != null) {
            System.out.println("\nOrder Details:");
            for (OrderItem item : order.getItems()) {
                System.out.println(
                        "Product ID: " + item.getProductId() +
                                " Qty: " + item.getQuantity());
            }
        }

        // 🔹 VERIFY CART STATE AFTER ORDER
        cart.getItems().removeIf(item -> item.getQuantity() <= 0);

        System.out.println("\nCart After Order:");
        if (cart.getItems().isEmpty()) {
            if (order != null) {
                System.out.println("Cart is empty after successful order.");
            } else {
                System.out.println("All items out of stock. Cannot place order.");
            }
        } else {
            cartService.viewCart(cart);
        }

        // 🔹 SHOW UPDATED STOCK
        System.out.println("\nUpdated Products:");
        for (Product p : productService.getAllProducts()) {
            System.out.println(
                    p.getId() + " " + p.getName() +
                            " Rs." + p.getPrice() +
                            " Stock: " + p.getStock());
        }
    }
}