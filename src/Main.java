import java.util.List;
import java.util.Map;
import java.util.Scanner;
import model.*;
import service.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

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
        printAIRecommendation(user, productService, orderService, sc);
        // ADD TO CART (edge case)
        cartService.addToCart(cart, 101, 1);
        cartService.addToCart(cart, 102, 100); // exceeds stock
        cartService.addToCart(cart, 103, 2);

        // VIEW CART
        System.out.println("\nCart:");
        cartService.viewCart(cart);

        Order order = null;

        // 🔹 PLACE ORDER (Step 4 - Response based)
        OrderResponse response = orderService.placeOrder(cart, productService, 1, user.getId());

        if (response.isSuccess()) {
            order = response.getOrder();

            System.out.println("\nOrder Placed Successfully!");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Total: Rs." + order.getTotalAmount());
        } else {
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

        System.out.print("\nEnter Order ID to cancel: ");
        int cancelId = sc.nextInt();

        orderService.cancelOrder(cancelId);
        sc.close();

        // 🔹 SHOW UPDATED STOCK
        System.out.println("\nUpdated Products:");
        for (Product p : productService.getAllProducts()) {
            System.out.println(
                    p.getId() + " " + p.getName() +
                            " Rs." + p.getPrice() +
                            " Stock: " + p.getStock());
        }
    }

    private static void printAIRecommendation(User user, ProductService productService, OrderService orderService,
            Scanner sc) {
        StringBuilder prompt = new StringBuilder();
        int budget = 50000;

        System.out.print("Enter your budget: ");
        if (sc.hasNextInt()) {
            budget = sc.nextInt();
        } else {
            System.out.println("Invalid budget input. Using default budget: 50000");
            if (sc.hasNext()) {
                sc.next();
            }
        }

        prompt.append("User budget: ").append(budget).append("\n\n");

        List<Product> products = productService.getAllProducts();
        List<String> history = orderService.getUserPurchasedProducts(user.getId());

        prompt.append("User previously bought:\n");
        for (String h : history) {
            prompt.append("- ").append(h).append("\n");
        }

        prompt.append("\nAvailable products:\n");
        for (Product p : products) {
            if (p.getStock() > 0) {
                prompt.append(p.getName())
                        .append(" - Rs.")
                        .append(p.getPrice())
                        .append("\n");
            }
        }

        prompt.append("\nSuggest best products based on budget and previous purchases.");

        String aiResponse = AIService.askAI(prompt.toString());
        System.out.println("\nAI Recommendation:\n" + aiResponse);
    }
}