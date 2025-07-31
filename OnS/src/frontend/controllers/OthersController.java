package frontend.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.control.ScrollPane;
import backend.models.Product;
import backend.network.NetworkService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OthersController {
    @FXML
    private GridPane othersGrid;
    @FXML
    private ScrollPane othersScrollPane;
    
    private NetworkService networkService;
    private List<Product> allProducts = new ArrayList<>();

    @FXML
    public void initialize() {
        networkService = NetworkService.getInstance();
        
        // Only use server data, no local fallback
        if (!networkService.isConnected()) {
            System.out.println("WARNING: Not connected to server - others view will be empty");
            allProducts = new ArrayList<>();
        } else {
            System.out.println("Connected to server, getting others data...");
            this.allProducts = networkService.getCachedInventory();
        }
        
        // Set update listener
        networkService.setInventoryUpdateListener(serverProducts -> {
            List<Product> convertedProducts = convertServerProducts(serverProducts);
            Platform.runLater(() -> {
                System.out.println("Received " + convertedProducts.size() + " products from server");
                allProducts = convertedProducts;
                updateOthersGrid();
            });
        });
        
        // Listen for width changes to make grid responsive
        othersScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateOthersGrid();
        });
        setupOthersGrid();
    }
    
    private List<Product> convertServerProducts(List<?> serverProducts) {
        List<Product> convertedProducts = new ArrayList<>();
        
        for(Object obj : serverProducts){
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
                System.out.println("Error converting product: " + e.getMessage());
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

            String[] productData = {
                id, name, category, String.valueOf(price),
                imagePath, String.valueOf(stockQuantity), String.valueOf(isAvailable)
            };

            return new Product(productData, "");
        } catch (Exception e) {
            System.out.println("Error converting products from server: " + e.getMessage());
            return null;
        }
    }

    private void setupOthersGrid() {
        updateOthersGrid();
    }

    private void updateOthersGrid() {
        // Clear any existing constraints and content
        othersGrid.getColumnConstraints().clear();
        othersGrid.getChildren().clear();

        // Filter for others products only
        List<Product> otherProducts = allProducts.stream()
            .filter(p -> p != null && "Others".equalsIgnoreCase(p.getCategory()) && p.isAvailable())
            .collect(Collectors.toList());

        System.out.println("Found: " + otherProducts.size() + " others from server");

        // Calculate number of columns based on available width
        double width = othersScrollPane.getWidth();
        int minCardWidth = 180; // Minimum width per card (including gaps)
        int columns = Math.max(1, (int) (width / minCardWidth));

        // Create equal column constraints programmatically
        for (int i = 0; i < columns; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / columns);
            othersGrid.getColumnConstraints().add(colConstraints);
        }

        // Set spacing for the grid
        othersGrid.setHgap(15); // Horizontal spacing between items
        othersGrid.setVgap(15); // Vertical spacing between items

        // Populate grid with products from server
        int row = 0;
        int col = 0;
        for (Product product : otherProducts) {
            if (product != null && product.isAvailable()) {
                VBox productCard = createImageProductCard(product);
                othersGrid.add(productCard, col, row);
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private VBox createImageProductCard(Product product) {
        VBox productCard = new VBox();
        productCard.setSpacing(8);
        productCard.setAlignment(Pos.CENTER);
        
        // Create ImageView
        ImageView productImage = new ImageView();
        try {
            // Use the image path from the product
            String imagePath = product.getImagePath();
            
            if (!imagePath.startsWith("/resources/")) {
                imagePath = "/resources" + imagePath;
            }
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            productImage.setImage(image);
        } catch (Exception e) {
            // If specific image loading fails, fallback to dummy image
            try {
                Image fallbackImage = new Image(getClass().getResourceAsStream("/resources/images/Dummy_Product.jpg"));
                productImage.setImage(fallbackImage);
            } catch (Exception ex) {
                // Silent fallback - no console output
            }
        }
        
        productImage.setFitWidth(140);
        productImage.setFitHeight(160);
        productImage.setPreserveRatio(false);
        
        // Add subtle border to image for canvas effect
        productImage.setStyle(
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );
        
        // LINE 1: Product Name Only
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
        
        // LINE 2: Price and Stock Side by Side
        HBox priceStockRow = new HBox(8);
        priceStockRow.setAlignment(Pos.CENTER);
        
        // Price label
        Label priceLabel = new Label("$" + String.format("%.2f", product.getPrice()));
        priceLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #e74c3c;" +
            "-fx-alignment: center;" +
            "-fx-background-color: #fff3cd;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 2px 6px;" +
            "-fx-border-color: #ffeaa7;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;"
        );
        
        // Stock label with color coding
        Label stockLabel = new Label();
        if (product.getStockQuantity() > 50) {
            stockLabel.setText("Stock: " + product.getStockQuantity());
            stockLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: normal;" +
                "-fx-text-fill: #27ae60;" +
                "-fx-alignment: center;" +
                "-fx-background-color: #d5f4e6;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px;" +
                "-fx-border-color: #a8e6cf;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px;"
            );
        } else if (product.getStockQuantity() > 10) {
            stockLabel.setText("Stock: " + product.getStockQuantity());
            stockLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: normal;" +
                "-fx-text-fill: #f39c12;" +
                "-fx-alignment: center;" +
                "-fx-background-color: #fef9e7;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px;" +
                "-fx-border-color: #ffeaa7;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px;"
            );
        } else {
            stockLabel.setText("Stock: " + product.getStockQuantity());
            stockLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: normal;" +
                "-fx-text-fill: #e74c3c;" +
                "-fx-alignment: center;" +
                "-fx-background-color: #fdedec;" +
                "-fx-background-radius: 6px;" +
                "-fx-padding: 2px 6px;" +
                "-fx-border-color: #fab1a0;" +
                "-fx-border-width: 1px;" +
                "-fx-border-radius: 6px;"
            );
        }
        
        // Add price and stock to horizontal row
        priceStockRow.getChildren().addAll(priceLabel, stockLabel);
        
        // LINE 3: Availability Status Only
        Label availabilityLabel = new Label();
        if (product.isAvailable()) {
            availabilityLabel.setText("✓ Available");
            availabilityLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #27ae60;" +
                "-fx-alignment: center;"
            );
        } else {
            availabilityLabel.setText("✗ Out of Stock");
            availabilityLabel.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #e74c3c;" +
                "-fx-alignment: center;"
            );
        }
        
        // Add all elements to the main VBox in clean lines
        productCard.getChildren().addAll(
            productImage,     // Image at top
            productLabel,     // Line 1: Product Name
            priceStockRow,    // Line 2: Price | Stock (side by side)
            availabilityLabel // Line 3: Availability
        );
        
        productCard.setPrefSize(180, 280); // Adjusted height for cleaner layout
        
        // Apply canvas-style background
        productCard.setStyle("-fx-background-color: #ffffff;");
        productCard.getStyleClass().add("product-card");
        
        // Add canvas-style hover effects
        productCard.setOnMouseEntered(e -> {
           productCard.getStyleClass().add("product-card");
            
            // Subtle scale up
            productCard.setScaleX(1.02);
            productCard.setScaleY(1.02);
        });
        
        productCard.setOnMouseExited(e -> {
            productCard.getStyleClass().add("product-card");
            
            // Return to normal scale
            productCard.setScaleX(1.0);
            productCard.setScaleY(1.0);
        });
        
        // Click handler using Product data
        productCard.setOnMouseClicked(e -> openProductPage(product));
        
        return productCard;
    }

    private void openProductPage(Product product) {
        try {
            // Load the TemplateProductPage FXML
            Stage currentStage = (Stage) othersGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Scene productScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            
            // Get the controller and pass product data
            TemplateProductController controller = loader.getController();
            controller.setProductData(product);
            
            // Get current stage and set the scene
            currentStage.setScene(productScene);
            currentStage.setTitle("EZ Shop - " + product.getName());
            
        } catch (Exception e) {
            System.err.println("Error loading product page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
