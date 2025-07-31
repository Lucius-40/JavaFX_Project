package backend.models;

import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String category;
    private double price;
    private String description;
    private String imagePath;
    private int stockQuantity;
    private boolean isAvailable;

    public Product(String[] reference, String des) {
        this.id = reference[0];
        this.name = reference[1];
        this.category = reference[2];
        this.price = Double.parseDouble(reference[3]);
        this.imagePath = reference[4];
        this.stockQuantity = Integer.parseInt(reference[5]);
        this.isAvailable = Boolean.parseBoolean(reference[6]);
        this.description = des;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public boolean isAvailable() {
        return isAvailable;
    }
    public void setStock(int x){
        stockQuantity = x ; 
    }

    public void setStockQuantity(int newQuantity) {
        this.stockQuantity = newQuantity;
        // Update availability based on stock quantity
        this.isAvailable = newQuantity > 0;
    }

    //   Add method to check if product needs restocking
    public boolean needsRestocking(int threshold) {
        return this.stockQuantity <= threshold;
    }

    //   Add method to decrease stock (alternative approach)
    public boolean decreaseStock(int quantity) {
        if (quantity <= 0) {
            return false;
        }
        
        if (this.stockQuantity >= quantity) {
            this.stockQuantity -= quantity;
            this.isAvailable = this.stockQuantity > 0;
            return true;
        }
        
        return false; // Not enough stock
    }

    @Override
    public String toString(){
        String response = "Product of id: " + this.id + ", name: " + this.name + ", category: " + this.category + ", price: " + this.price;
        return response;
    }

    public void setAvailable(boolean available) {
       isAvailable = available;
    }
}
