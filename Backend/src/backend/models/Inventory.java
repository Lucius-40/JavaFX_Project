package backend.models;

import java.io.*;
import java.util.*;

public class Inventory implements Serializable{

private static List<Product> products = new ArrayList<>();
private static final java.util.concurrent.locks.ReentrantReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private static String PRODUCTS_FILE = "./src/backend/data/products.txt";
    private static String descriptionFilePath = "./src/backend/data/products.txt";
 

    public static void setProductsFilePath(String path) {
        PRODUCTS_FILE = path;
    }
    public static void setDescriptionFilePath(String path) {
        descriptionFilePath = path;
    }


    public static List<Product> getProductsByCategory(String category) {
        List<Product> temp = new ArrayList<>();
        if (category == null || category.trim().isEmpty()) {
            return temp; // Return empty list
        }
        for (Product p : products) {
            if (p == null)
                continue;
            if (p.getCategory().equalsIgnoreCase(category))
                temp.add(p);
        }
        return temp;
    }

    public static Product getProductById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null; 
        }

        for (Product p : products) {
            if (p == null)
                continue;
            if (p.getId().equalsIgnoreCase(id))
                return p;
        }
        return null;
    }

    public static Product getProductByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null; 
        }

        for (Product p : products) {
            if (p == null)
                continue;
            if (p.getName().equalsIgnoreCase(name))
                return p;
        }
        return null;
    }

    public static void updateStock(String productId, int quantity) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }
        
        Product product = getProductById(productId);
        if (product != null) {
            
            System.out.println("Stock update requested for product " + productId + " with quantity " + quantity);
            
        }
    }

    public static void loadFromFile() {
        lock.writeLock().lock();
        try {
            products.clear();
            Map<String, String> descriptions = loadDescriptions();
            try (BufferedReader reader = new BufferedReader(new FileReader(PRODUCTS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\\|");
                    if (tokens.length >= 7) {
                        String description = descriptions.get(tokens[0]);
                        if (description == null) {
                            description = "No description available";
                        }
                        Product temp = new Product(tokens, description);
                        products.add(temp);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading products: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static Map<String, String> loadDescriptions() {
        Map<String, String> descriptions = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(descriptionFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    descriptions.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            // Silently handle missing descriptions file
        }
        return descriptions;
    }

    //   Add this method to properly update stock and save to file
    public static void updateProductStock(String productId, int newQuantity) {
        lock.writeLock().lock();
        try {
            if (productId == null || productId.trim().isEmpty()) {
                return;
            }
            Product product = getProductById(productId);
            if (product != null) {
                int oldStock = product.getStockQuantity();
                product.setStockQuantity(newQuantity);
                System.out.println("Stock updated for " + product.getName() + " from " + oldStock + " to " + newQuantity);
                saveToFile();
                if (newQuantity <= 0) {
                    System.out.println("Product " + product.getName() + " is now out of stock");
                }
            } else {
                System.out.println("Product not found for ID: " + productId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    //   Update the saveToFile method to ensure it saves correctly
    public static void saveToFile() {
        lock.writeLock().lock();
        try {
            try (PrintWriter writer = new PrintWriter(new FileWriter(PRODUCTS_FILE))) {
                for (Product product : products) {
                    if (product != null) {
                        writer.println(
                            product.getId() + "|" +
                            product.getName() + "|" +
                            product.getCategory() + "|" +
                            product.getPrice() + "|" +
                            product.getImagePath() + "|" +
                            product.getStockQuantity() + "|" +
                            product.isAvailable()
                        );
                    }
                }
                System.out.println("  Products file updated successfully with " + products.size() + " products");
            } catch (IOException e) {
                System.err.println("Error saving products to file: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public static List<Product> getAllProducts() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(products);
        } finally {
            lock.readLock().unlock();
        }
    }
}