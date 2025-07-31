package backend.models;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Cart {
    private static Cart instance = new Cart();
    private Map<String, CartItem> items;
    private double totalAmount = 0;

    // inner class
    public class CartItem {
        private Product product;
        private int quantity;
        private double subtotal;

        CartItem(Product product, int quantity, double subtotal) {
            this.product = product;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public Product getProduct() {
            return product;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public double calculateSubtotal() {
            return product.getPrice() * quantity;
        }
    }

    private Cart() {
        items = new HashMap<>();
        totalAmount = 0.0;
    }

    public static synchronized Cart getInstance() {
        return instance;
    }

    public void addItem(Product product, int quantity) {
        String id = product.getId();
        if (items.containsKey(id)) {
            CartItem existingItem = items.get(id);
            existingItem.quantity += quantity;
            existingItem.subtotal = product.getPrice() * existingItem.quantity;
        } else {
            CartItem temp = new CartItem(product, quantity, product.getPrice() * quantity);
            items.put(product.getId(), temp);
        }
        calculateTotal(); // Update the total amount
    }

    public void removeItem(String productId) {
        items.remove(productId);
        calculateTotal();
    }

    public void updateQuantity(String productId, int newQuantity) {
        CartItem item = items.get(productId); 
        if (item != null) { 
            item.quantity = newQuantity; 
            item.subtotal = item.product.getPrice() * newQuantity; 
            calculateTotal();
        }

    }

    public List<CartItem> getItems() {
        List<CartItem> allValues = new ArrayList<>(items.values());
        return allValues ;
    }

    public double getTotalAmount() {
        return totalAmount ;

    }

    public void clear() {
        items.clear();
        totalAmount = 0.0 ;

    }

    private void calculateTotal() {
        totalAmount = 0.0;
        for (CartItem item : items.values()) {
            totalAmount += item.getSubtotal();
        }
    }
}
