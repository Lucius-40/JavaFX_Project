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

public class ShoesController {
    @FXML
    private GridPane shoesGrid;
    @FXML
    private ScrollPane shoesScrollPane;
    
    private NetworkService networkService;
    private List<Product> allProducts = new ArrayList<>();
    
    @FXML
    public void initialize() {
        networkService = NetworkService.getInstance();
        
        // Only use server data, no local fallback
        if (!networkService.isConnected()) {
            System.out.println("WARNING: Not connected to server - shoes view will be empty");
            allProducts = new ArrayList<>();
        } else {
            System.out.println("Connected to server, getting shoes data...");
            this.allProducts = networkService.getCachedInventory();
        }
        
        // Set update listener
        networkService.setInventoryUpdateListener(serverProducts -> {
            List<Product> convertedProducts = convertServerProducts(serverProducts);
            Platform.runLater(() -> {
                System.out.println("Received " + convertedProducts.size() + " products from server");
                allProducts = convertedProducts;
                updateShoesGrid();
            });
        });
        
        // Listen for width changes to make grid responsive
        shoesScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateShoesGrid();
        });
        setupShoesGrid();
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

    private void setupShoesGrid() {
        updateShoesGrid();
    }

    private void updateShoesGrid() {
        // Clear any existing constraints and content
        shoesGrid.getColumnConstraints().clear();
        shoesGrid.getChildren().clear();

        // Filter for shoes products only
        List<Product> shoesProducts = allProducts.stream()
            .filter(p -> p != null && "Shoes".equalsIgnoreCase(p.getCategory()) && p.isAvailable())
            .collect(Collectors.toList());

        System.out.println("Found: " + shoesProducts.size() + " shoes from server");

        // Calculate number of columns based on available width
        double width = shoesScrollPane.getWidth();
        int minCardWidth = 180; // Minimum width per card (including gaps)
        int columns = Math.max(1, (int) (width / minCardWidth));

        // Create equal column constraints programmatically
        for (int i = 0; i < columns; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / columns);
            shoesGrid.getColumnConstraints().add(colConstraints);
        }

        // Set spacing for the grid
        shoesGrid.setHgap(15); // Horizontal spacing between items
        shoesGrid.setVgap(15); // Vertical spacing between items

        // Populate grid with products from server
        int row = 0;
        int col = 0;
        for (Product product : shoesProducts) {
            if (product != null && product.isAvailable()) {
                VBox productCard = createImageProductCard(product);
                shoesGrid.add(productCard, col, row);
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    // Keep your existing createImageProductCard method - the same as others
    private VBox createImageProductCard(Product product) {
        // Same implementation as OthersController and StationaryController
        VBox productCard = new VBox();
        productCard.setSpacing(8);
        productCard.setAlignment(Pos.CENTER);
        
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
            try {
                Image fallbackImage = new Image(getClass().getResourceAsStream("/resources/images/Dummy_Product.jpg"));
                productImage.setImage(fallbackImage);
            } catch (Exception ex) {
                // Silent fallback
            }
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
        
        Label stockLabel = new Label("Stock: " + product.getStockQuantity());
        String stockStyle = "-fx-font-size: 11px;" +
            "-fx-font-weight: normal;" +
            "-fx-alignment: center;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 2px 6px;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;";
        
        if (product.getStockQuantity() > 50) {
            stockLabel.setStyle(stockStyle + 
                "-fx-text-fill: #27ae60;" +
                "-fx-background-color: #d5f4e6;" +
                "-fx-border-color: #a8e6cf;");
        } else if (product.getStockQuantity() > 10) {
            stockLabel.setStyle(stockStyle + 
                "-fx-text-fill: #f39c12;" +
                "-fx-background-color: #fef9e7;" +
                "-fx-border-color: #ffeaa7;");
        } else {
            stockLabel.setStyle(stockStyle + 
                "-fx-text-fill: #e74c3c;" +
                "-fx-background-color: #fdedec;" +
                "-fx-border-color: #fab1a0;");
        }
        
        priceStockRow.getChildren().addAll(priceLabel, stockLabel);
        
        // Availability
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
        
        productCard.getChildren().addAll(productImage, productLabel, priceStockRow, availabilityLabel);
        productCard.setPrefSize(180, 280);
        productCard.setStyle("-fx-background-color: #ffffff;");
        productCard.getStyleClass().add("product-card");
        
        // Hover effects
        productCard.setOnMouseEntered(e -> {
            productCard.setScaleX(1.02);
            productCard.setScaleY(1.02);
        });
        
        productCard.setOnMouseExited(e -> {
            productCard.setScaleX(1.0);
            productCard.setScaleY(1.0);
        });
        
        productCard.setOnMouseClicked(e -> openProductPage(product));
        
        return productCard;
    }
    
    private void openProductPage(Product product) {
        try {
            Stage currentStage = (Stage) shoesGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Scene productScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            
            TemplateProductController controller = loader.getController();
            controller.setProductData(product);
            
            currentStage.setScene(productScene);
            currentStage.setTitle("EZ Shop - " + product.getName());
            
        } catch (Exception e) {
            System.err.println("Error loading product page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
