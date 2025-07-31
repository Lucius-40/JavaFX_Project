package frontend.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
// import backend.models.Inventory;
import backend.models.Product;
import backend.network.NetworkService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClothesController {
    @FXML
    private GridPane clothesGrid;
    @FXML
    private javafx.scene.control.ScrollPane clothesScrollPane;

    private NetworkService networkService;
    private List<Product> allProducts = new ArrayList<>();
    private boolean inventoryObserverRegistered = false;
    
    @FXML
    public void initialize() {
        networkService = NetworkService.getInstance();
        
        if (!networkService.isConnected()) {
            System.out.println("WARNING: Not connected to server - clothes view will be empty");
            allProducts = new ArrayList<>(); 
        } else {
            System.out.println("Connected to server, getting product data...");
            loadClothesFromCache();
        }
        
        // Register inventory observer only once
        if (!inventoryObserverRegistered) {
            NetworkService.addInventoryObserver(this::refreshClothesFromGlobalInventory);
            inventoryObserverRegistered = true;
        }
        
        clothesScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateClothesGrid();
        });
        
        setupClothesGrid();
    }
    
    private List<Product> convertServerProducts(List<?> serverProducts) {
        List<Product> convertedProducts = new ArrayList<>();
        
        for(Object obj: serverProducts){
            try {
                if(obj instanceof Product){
                    convertedProducts.add((Product)obj);
                }else{
                    Product clientProduct = convertProductViaReflection(obj);
                    if (clientProduct != null) {
                        convertedProducts.add(clientProduct);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error converting produc: " + e.getMessage());
            }
        }
        return convertedProducts;
    }

    private Product convertProductViaReflection(Object serverProduct) {
        if(serverProduct == null) return null;

        try {
            Class<?> serverClass = serverProduct.getClass();

            String id = (String) serverClass.getMethod("getId").invoke(serverProduct);
            String name = (String) serverClass.getMethod("getName").invoke(serverProduct);
            String category = (String) serverClass.getMethod("getCategory").invoke(serverProduct);
            double price = (double) serverClass.getMethod("getPrice").invoke(serverProduct);
            String imagePath = (String) serverClass.getMethod("getImagePath").invoke(serverProduct);
            int stockQuantity = (int) serverClass.getMethod("getStockQuantity").invoke(serverProduct);
            boolean isAvailable = (boolean) serverClass.getMethod("isAvailable").invoke(serverProduct);

            // String description = "";

            // try {
            //     description = (String) serverClass.getMethod("getDescription").invoke(serverProduct);
            // } catch (Exception e) {
            //     // I am a mew :)
            // }

            String[] productData = {
                id, name, category, String.valueOf(price),
                imagePath, String.valueOf(stockQuantity), String.valueOf(isAvailable)
            };

            return new Product(productData, "");
        } catch (Exception e) {
            System.out.println("Error converting products from the server through reflection: " + e.getMessage());
            return null;
        }
    }

    private void setupClothesGrid() {
        updateClothesGrid();
    }
    
    private void updateClothesGrid() {
        // Always clear children and column constraints before repopulating
        clothesGrid.getChildren().clear();
        clothesGrid.getColumnConstraints().clear();

        // Filter clothes products with real-time stock checking
        List<Product> clothesProducts = allProducts.stream()
            .filter(p -> p != null && 
                   (p.getCategory().trim().equalsIgnoreCase("Clothes") || 
                    p.getCategory().trim().equalsIgnoreCase("Clothing")))
            .collect(Collectors.toList());

        System.out.println("Found: " + clothesProducts.size() + " clothes from the server");

        // Calculate grid layout
        double width = clothesScrollPane.getWidth();
        int minCardWidth = 220; 
        int columns = Math.max(1, (int) (width / minCardWidth));

        // Add new column constraints
        for (int i = 0; i < columns; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / columns);
            clothesGrid.getColumnConstraints().add(colConstraints);
        }

        // Restore spacing
        clothesGrid.setHgap(25); 
        clothesGrid.setVgap(25); 

        // Populate grid with products (including out-of-stock with visual indicators)
        int row = 0;
        int col = 0;
        for (Product product : clothesProducts) {
            if (product != null) {
                VBox productCard = createImageProductCard(product);
                clothesGrid.add(productCard, col, row);
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }

        System.out.println("  Grid updated with " + clothesProducts.size() + " products in " + columns + " columns");
    }
    
    
    
    private VBox createImageProductCard(Product product) {
        VBox productCard = new VBox();
        productCard.setSpacing(8);
        productCard.setAlignment(Pos.CENTER);
        
        //   Store product reference for dynamic updates
        productCard.setUserData(product.getId());
        
        // Create ImageView
        ImageView productImage = new ImageView();
        try {
            String imagePath = product.getImagePath();
            if (!imagePath.startsWith("/resources/")) {
                imagePath = "/resources" + imagePath;
            }
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            productImage.setImage(image);
        } catch (Exception e) {
            System.out.println("Cannot find image for: " + product.getName());
        }
        
        productImage.setFitWidth(140);
        productImage.setFitHeight(160);
        productImage.setPreserveRatio(false);
        productImage.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);");
        
        // Product Name
        Label productLabel = new Label(product.getName());
        productLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #2c2c2c;" +
            "-fx-alignment: center;" +
            "-fx-padding: 3px 0 2px 0;" +
            "-fx-wrap-text: true;" +
            "-fx-max-width: 150px;"
        );
        
        // Price and Stock Row
        HBox priceStockRow = new HBox(8);
        priceStockRow.setAlignment(Pos.CENTER);
        
        // Price label
        Label priceLabel = new Label("$" + String.format("%.2f", product.getPrice()));
        priceLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #e74c3c;" +
            "-fx-alignment: center;" +
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 2px 6px;" +
            "-fx-border-color: #ffeaa7;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;"
        );
        
        //   Dynamic stock label with real-time color updates
        Label stockLabel = new Label();
        updateStockLabel(stockLabel, product.getStockQuantity());
        
        priceStockRow.getChildren().addAll(priceLabel, stockLabel);
        
        //   Dynamic availability label
        Label availabilityLabel = new Label();
        updateAvailabilityLabel(availabilityLabel, product);
        
        // Add all elements
        productCard.getChildren().addAll(productImage, productLabel, priceStockRow, availabilityLabel);
        productCard.setPrefSize(180, 280);
        
        //   Apply dynamic styling based on stock
        if (product.getStockQuantity() <= 0) {
            productCard.setStyle("-fx-opacity: 0.7; -fx-background-color: #ffffff;");
            productCard.getStyleClass().add("out-of-stock-card");
        } else {
            productCard.setStyle("-fx-opacity: 1.0; -fx-background-color: #ffffff;");
            productCard.getStyleClass().add("product-card");
        }
        
        // Hover effects
        productCard.setOnMouseEntered(e -> {
            if (product.getStockQuantity() > 0) {
                productCard.setScaleX(1.02);
                productCard.setScaleY(1.02);
            }
        });
        
        productCard.setOnMouseExited(e -> {
            productCard.setScaleX(1.0);
            productCard.setScaleY(1.0);
        });
        
        //   Click handler with stock validation
        productCard.setOnMouseClicked(e -> {
            if (product.getStockQuantity() > 0) {
                openProductPage(product);
            } else {
                showOutOfStockAlert(product.getName());
            }
        });
        
        return productCard;
    }
    
    
    private void openProductPage(Product product) {
        try {
            Stage currentStage = (Stage) clothesGrid.getScene().getWindow();
            // load the TemplateProductPage FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Scene productScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            
            // get the controller and pass product data
            TemplateProductController controller = loader.getController();
            // Convert product ID string to int for compatibility with existing method
            controller.setProductData(product);
            
            // Get current stage and set the scene
            currentStage.setScene(productScene);
            currentStage.setTitle("EZ Shop - " + product.getName());
            
        } catch (Exception e) {
            System.err.println("Error loading product page: " + e.getMessage());
            e.printStackTrace();
            // Fallback: show a simple dialog
            
        }
    }

    private void loadClothesFromCache() {
        if (networkService != null) {
            List<Product> cachedInventory = networkService.getCachedInventory();
            if (cachedInventory != null && !cachedInventory.isEmpty()) {
                allProducts = new ArrayList<>(cachedInventory);
                updateClothesGrid();
                System.out.println("  Loaded " + allProducts.size() + " products from cache");
            } else {
                System.out.println("⚠️ No cached inventory available");
            }
        }
    }

    private void displayProducts(List<Product> clothesProducts) {
        allProducts = new ArrayList<>(clothesProducts);
        updateClothesGrid();
        System.out.println("  Updated clothes display with " + clothesProducts.size() + " products");
    }

    //   Enhanced inventory update handler that preserves grid structure
    private void handleInventoryUpdate(List<Product> updatedInventory) {
        Platform.runLater(() -> {
            System.out.println("🔄 Received inventory update: " + updatedInventory.size() + " products");
            
            // Store current scroll position to maintain user's view
            double currentVValue = clothesScrollPane.getVvalue();
            
            // Update the inventory
            allProducts = new ArrayList<>(updatedInventory);
            
            // Refresh the grid while preserving structure
            updateClothesGrid();
            
            // Restore scroll position
            Platform.runLater(() -> clothesScrollPane.setVvalue(currentVValue));
            
            System.out.println("  Clothes grid updated dynamically");
        });
    }
    
    //   Helper methods for dynamic updates
    private void updateStockLabel(Label stockLabel, int stockQuantity) {
        stockLabel.setText("Stock: " + stockQuantity);
        
        if (stockQuantity > 50) {
            stockLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #27ae60;" +
                "-fx-alignment: center; -fx-background-color: #ffffff; -fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px; -fx-border-color: #a8e6cf; -fx-border-width: 1px; -fx-border-radius: 6px;"
            );
        } else if (stockQuantity > 10) {
            stockLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #f39c12;" +
                "-fx-alignment: center; -fx-background-color: #ffffff; -fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px; -fx-border-color: #ffeaa7; -fx-border-width: 1px; -fx-border-radius: 6px;"
            );
        } else if (stockQuantity > 0) {
            stockLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: normal; -fx-text-fill: #e74c3c;" +
                "-fx-alignment: center; -fx-background-color: #ffffff; -fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px; -fx-border-color: #fab1a0; -fx-border-width: 1px; -fx-border-radius: 6px;"
            );
        } else {
            stockLabel.setText("Out of Stock");
            stockLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #721c24;" +
                "-fx-alignment: center; -fx-background-color: #ffffff; -fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px; -fx-border-color: #f5c6cb; -fx-border-width: 1px; -fx-border-radius: 6px;"
            );
        }
    }

    private void updateAvailabilityLabel(Label availabilityLabel, Product product) {
        if (product.isAvailable() && product.getStockQuantity() > 0) {
            availabilityLabel.setText("✓ Available");
            availabilityLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-alignment: center;"
            );
        } else {
            availabilityLabel.setText("✗ Out of Stock");
            availabilityLabel.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-alignment: center;"
            );
        }
    }

    private void showOutOfStockAlert(String productName) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Out of Stock");
        alert.setHeaderText("Product Unavailable");
        alert.setContentText(productName + " is currently out of stock. Please check back later.");
        alert.showAndWait();
    }
    
    //   ADD: New method to refresh from global inventory
    private void refreshClothesFromGlobalInventory() {
        List<Product> globalInventory = NetworkService.getGlobalInventory();
        
        //   FIX: Replace the entire product list with updated inventory
        allProducts = new ArrayList<>(globalInventory);
        
        System.out.println("🔄 ClothesController refreshed from global inventory: " + allProducts.size() + " products");
        
        // Store current scroll position
        double currentVValue = clothesScrollPane.getVvalue();
        
        // Update the grid with fresh data
        updateClothesGrid();
        
        // Restore scroll position
        Platform.runLater(() -> clothesScrollPane.setVvalue(currentVValue));
    }
    
    // Add this method:
    public void cleanup() {
        if (inventoryObserverRegistered) {
            NetworkService.removeInventoryObserver(this::refreshClothesFromGlobalInventory);
            inventoryObserverRegistered = false;
            System.out.println("🧹 ClothesController cleanup completed");
        }
    }

    // Add this to the controller when scene changes
    public void onSceneExit() {
        cleanup();
    }
}
