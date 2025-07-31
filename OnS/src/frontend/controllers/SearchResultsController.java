package frontend.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Stage;
// import backend.models.Inventory;
import backend.models.Product;
import backend.network.NetworkService;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;


public class SearchResultsController {
    @FXML
    private Label searchHeaderLabel;
    @FXML
    private Label resultsCountLabel;
    @FXML
    private Button backButton;
    @FXML
    private ScrollPane searchScrollPane;
    @FXML
    private GridPane searchResultsGrid;

    private String searchQuery;
    private List<Product> searchResults = new ArrayList<>();

    @FXML
    public void initialize(){
        searchScrollPane.widthProperty().addListener((obs, oldVal, newVal)->{
            updateResultsGrid();
        });
        
        //   ADD: Listen for real-time inventory updates via centralized system
        NetworkService.addInventoryObserver(this::refreshSearchResultsFromGlobalInventory);
    }

    public void setSearchQuery(String query){
        this.searchQuery = query;
        performSearch();
        resultsCountLabel.setText("Found " + searchResults.size() + " products");
        searchHeaderLabel.setText("Search Result(s) for '" + query + "'");
        setupResultsGrid();
    }

    // Add this method to fix the error in HelloController
    public void setResults(List<Product> products, String query) {
        this.searchResults = new ArrayList<>(products);
        this.searchQuery = query;
        
        if (resultsCountLabel != null) {
            resultsCountLabel.setText("Found " + searchResults.size() + " products");
        }
        
        if (searchHeaderLabel != null) {
            searchHeaderLabel.setText("Search Result(s) for '" + query + "'");
        }
        
        updateResultsGrid(); //   CHANGED: Use existing method instead of setupResultsGrid()
    }


    //   MODIFY: performSearch method to use global inventory
    private void performSearch(){
        List<Product> allProducts = NetworkService.getGlobalInventory(); //   Use centralized inventory
    
        String lowercaseQuery = searchQuery.toLowerCase().strip();
        searchResults = allProducts.stream()
                            .filter(product -> 
                                product != null && 
                                product.isAvailable() && 
                                (product.getName().toLowerCase().contains(lowercaseQuery) ||
                                product.getCategory().toLowerCase().contains(lowercaseQuery) ||
                                product.getId().toLowerCase().contains(lowercaseQuery) ||
                                product.getDescription().toLowerCase().contains(lowercaseQuery)
                                )
                            ).collect(Collectors.toList());
    }

    private void setupResultsGrid() {
        searchResultsGrid.getChildren().clear();
        
        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("No products found matching \"" + searchQuery + "\"");
            noResultsLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #555555;" +
                "-fx-padding: 50px 0;"
            );
            searchResultsGrid.add(noResultsLabel, 0, 0);
            return;
        }
        
        int column = 0;
        int row = 0;
        int maxColumns = 3; // Adjust based on width
        
        for (Product product : searchResults) {
            VBox productCard = createProductCard(product);
            searchResultsGrid.add(productCard, column, row);
            
            column++;
            if (column >= maxColumns) {
                column = 0;
                row++;
            }
        }
    }

    private void updateResultsGrid(){
        searchResultsGrid.getChildren().clear();
        searchResultsGrid.getColumnConstraints().clear();

        double width = searchScrollPane.getWidth();
        int minCardWidth = 220;
        int columns = Math.max(1, (int)(width/minCardWidth));

        for(int i=0; i<columns; i++){
            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setPercentWidth(100.0/columns);
            searchResultsGrid.getColumnConstraints().add(columnConstraints);
        }

        searchResultsGrid.setHgap(25);
        searchResultsGrid.setVgap(25);

        int row = 0;
        int col = 0;

        for(Product product: searchResults){
            VBox productCard = createProductCard(product);
            searchResultsGrid.add(productCard, col, row);
            col++;
            if(col >= columns){
                col = 0;
                row++;
            }
        }

        if (searchResults.isEmpty()) {
            Label noResultsLabel = new Label("No products found matching \"" + searchQuery + "\"");
            noResultsLabel.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-text-fill: #555555;" +
                "-fx-padding: 50px 0;"
            );
            searchResultsGrid.add(noResultsLabel, 0, 0, columns, 1);
        }
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

    private VBox createProductCard(Product product) {
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
        
        // Add subtle border to image
        productImage.setStyle(
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 2, 0, 0, 1);"
        );
        
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
        
        // Category label
        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-style: italic;" +
            "-fx-text-fill: #666666;" +
            "-fx-alignment: center;"
        );
        
        // Price and Stock in a row
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
        if (product.getStockQuantity() > 50) {
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
        
        priceStockRow.getChildren().addAll(priceLabel, stockLabel);
        
        // Add all elements to the VBox
        productCard.getChildren().addAll(
            productImage,
            productLabel,
            categoryLabel,
            priceStockRow
        );
        
        productCard.setPrefSize(180, 280);
        
        // Canvas-style background
        productCard.getStyleClass().add("product-card");
        
        // Add hover effects
        productCard.setOnMouseEntered(e -> {
            productCard.setStyle(
                "-fx-background-color: #fbfbfb;" +
                "-fx-border-color: #b8956a;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 0px;" +
                "-fx-background-radius: 0px;" +
                "-fx-padding: 12px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(139,69,19,0.4), 10, 0, 3, 3);" +
                "-fx-cursor: hand;"
            );
            
            productCard.setScaleX(1.02);
            productCard.setScaleY(1.02);
        });
        
        productCard.setOnMouseExited(e -> {
            productCard.setStyle(
                "-fx-background-color: #FAF1E6;" +
                "-fx-border-color: #2c1d08ff;" +
                "-fx-border-width: 2px;" +
                "-fx-border-radius: 0px;" +
                "-fx-background-radius: 0px;" +
                "-fx-padding: 12px;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(139,69,19,0.2), 6, 0, 2, 2);"
            );
            
            productCard.setScaleX(1.0);
            productCard.setScaleY(1.0);
        });
        
        // Click handler
        productCard.setOnMouseClicked(e -> openProductPage(product));
        
        return productCard;
    }

    @FXML
    public void handleBackButton() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            Scene mainScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            
            // Apply styling
            mainScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Get current stage and set the scene
            currentStage.setScene(mainScene);
            currentStage.setTitle("EZ Shop");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openProductPage(Product product) {
        try {
            Stage currentStage = (Stage) searchResultsGrid.getScene().getWindow();
            // Load the product page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Scene productScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            
            // Get the controller and pass product data
            TemplateProductController controller = loader.getController();
            controller.setProductData(product);
            
            // Get current stage and set the scene
            currentStage.setScene(productScene);
            currentStage.setTitle("EZ Shop - " + product.getName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //   ADD: New method to refresh search results from global inventory
    private void refreshSearchResultsFromGlobalInventory() {
        if (searchResults == null || searchResults.isEmpty()) {
            return; // No search results to update
        }
        
        List<Product> globalInventory = NetworkService.getGlobalInventory();
        
        // Update existing search results with new stock values from global inventory
        boolean stockChanged = false;
        for (Product existingProduct : searchResults) {
            for (Product globalProduct : globalInventory) {
                if (globalProduct.getId().equals(existingProduct.getId())) {
                    if (existingProduct.getStockQuantity() != globalProduct.getStockQuantity()) {
                        existingProduct.setStockQuantity(globalProduct.getStockQuantity());
                        existingProduct.setAvailable(globalProduct.isAvailable());
                        stockChanged = true;
                        System.out.println("📊 SearchResults updated product " + existingProduct.getId() + 
                                           " stock to: " + existingProduct.getStockQuantity());
                    }
                    break;
                }
            }
        }
        
        if (stockChanged) {
            updateResultsGrid(); // Refresh the grid with updated stock
            System.out.println("  SearchResults grid updated with new stock values");
        }
    }

    // Add this method:
    public void cleanup() {
        NetworkService.removeInventoryObserver(this::refreshSearchResultsFromGlobalInventory);
        System.out.println("🧹 SearchResultsController cleanup completed");
    }
}
