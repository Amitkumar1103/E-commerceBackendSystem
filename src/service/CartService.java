package service;

// this service will handle cart related operations like adding items to cart and viewing cart contents
import model.Cart;
import model.CartItem;

public class CartService {

    public void addToCart(Cart cart, int productId, int quantity) {
        for (CartItem item : cart.getItems()) {
            if (item.getProductId() == productId) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        cart.getItems().add(new CartItem(productId, quantity));
    }

    public void viewCart(Cart cart) {
        for (CartItem item : cart.getItems()) {
            System.out.println("Product ID: " + item.getProductId() + " Qty: " + item.getQuantity());
        }
    }
}