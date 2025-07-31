package frontend.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import backend.models.Product;
import backend.network.NetworkService;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionsController {
    
    @FXML private VBox categoryContainer;
    @FXML private ComboBox<String> sortingComboBox;
    @FXML private Slider minPriceSlider;
    @FXML private Slider maxPriceSlider;
    @FXML private Label minPriceLabel;
    @FXML private Label maxPriceLabel;
    @FXML private Label productsCountLabel;
    @FXML private ScrollPane productsScrollPane;
    @FXML private GridPane productsGrid;
    @FXML private Button applyFilterButton;
    @FXML private Button resetFilterButton;
    @FXML private Button backButton;
    
    // Store all products
    private List<Product> allProducts = new ArrayList<>();
    
    // Store currently filtered/displayed products
    private List<Product> filteredProducts = new ArrayList<>();
    
    // Store category checkboxes for easy access
    private Map<String, CheckBox> categoryCheckboxes = new HashMap<>();
    
    // Store min/max prices in the system
    private double absoluteMinPrice = 0;
    private double absoluteMaxPrice = 1000;
    private NetworkService networkService;
    
    @FXML
    public void initialize() {
        System.out.println("Initializing Collections Controller...");
        
        // Add this code to fix the SplitPane divider
        Platform.runLater(() -> {
            if (productsScrollPane.getScene() != null) {
                SplitPane splitPane = (SplitPane) productsScrollPane.getScene().lookup(".split-pane");
                if (splitPane != null) {
                    splitPane.setDividerPosition(0, 0.2);
                }
            }
        });
        
        // Inventory.loadFromFile();
        
        loadAllProducts();
        
        setupCategorySelectors();
        
        // Setup sorting options
        setupSortingOptions();
        
        // Setup price range sliders
        setupPriceRangeSliders();
        
        // Initial display of all products
        updateProductsDisplay(allProducts);
        
        // Add listener to category checkboxes to update price range
        for (CheckBox checkbox : categoryCheckboxes.values()) {
            checkbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updatePriceRangeForCategories();
            });
        }
        
        // Add back button functionality
        backButton.setOnAction(e -> handleBackButton());
    }
    
    private void loadAllProducts() {
        networkService = NetworkService.getInstance();
        
        if (networkService.isConnected()) {
            allProducts = convertServerProducts(networkService.getCachedInventory());
            
            networkService.setInventoryUpdateListener(serverProducts -> {
                List<Product> convertedProducts = convertServerProducts(serverProducts);
                Platform.runLater(() -> {
                    allProducts = convertedProducts;
                    handleApplyFilters(); // Refresh the display
                });
            });
        } else {
            allProducts = new ArrayList<>();
        }
        
        filteredProducts = new ArrayList<>(allProducts);
        
        // Calculate absolute min and max prices from all products
        if (!allProducts.isEmpty()) {
            absoluteMinPrice = allProducts.stream()
                    .mapToDouble(Product::getPrice)
                    .min()
                    .orElse(0);
            
            absoluteMaxPrice = allProducts.stream()
                    .mapToDouble(Product::getPrice)
                    .max()
                    .orElse(1000);
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

    private void setupCategorySelectors() {
        // Get unique categories from all products
        Set<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .collect(Collectors.toSet());
        
        // Create checkbox for each category
        for (String category : categories) {
            CheckBox categoryCheckBox = new CheckBox(category);
            categoryCheckBox.setUserData(category);
            
            // Store for easy access
            categoryCheckboxes.put(category, categoryCheckBox);
            
            // Add to the container
            categoryContainer.getChildren().add(categoryCheckBox);
        }
    }
    
    private void setupSortingOptions() {
        // Define sorting options
        ObservableList<String> sortOptions = FXCollections.observableArrayList(
                "Name (A to Z)",
                "Name (Z to A)",
                "Price (Low to High)",
                "Price (High to Low)"
        );
        
        // Set items and default
        sortingComboBox.setItems(sortOptions);
        sortingComboBox.setValue("Name (A to Z)");
    }
    
    private void setupPriceRangeSliders() {
        // Configure min price slider
        minPriceSlider.setMin(absoluteMinPrice);
        minPriceSlider.setMax(absoluteMaxPrice);
        minPriceSlider.setValue(absoluteMinPrice);
        
        // Configure max price slider
        maxPriceSlider.setMin(absoluteMinPrice);
        maxPriceSlider.setMax(absoluteMaxPrice);
        maxPriceSlider.setValue(absoluteMaxPrice);
        
        // Update labels when sliders move
        minPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure min doesn't exceed max
            if (newVal.doubleValue() > maxPriceSlider.getValue()) {
                minPriceSlider.setValue(maxPriceSlider.getValue());
                return;
            }
            
            // Update label
            minPriceLabel.setText(String.format("$%.2f", newVal.doubleValue()));
        });
        
        maxPriceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ensure max doesn't go below min
            if (newVal.doubleValue() < minPriceSlider.getValue()) {
                maxPriceSlider.setValue(minPriceSlider.getValue());
                return;
            }
            
            // Update label
            maxPriceLabel.setText(String.format("$%.2f", newVal.doubleValue()));
        });
        
        // Set initial labels
        minPriceLabel.setText(String.format("$%.2f", absoluteMinPrice));
        maxPriceLabel.setText(String.format("$%.2f", absoluteMaxPrice));
    }
    
    @FXML
    private void handleApplyFilters() {
        // Get selected categories
        List<String> selectedCategories = categoryCheckboxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // Get price range
        double minPrice = minPriceSlider.getValue();
        double maxPrice = maxPriceSlider.getValue();
        
        // Filter products
        filteredProducts = allProducts.stream()
                .filter(product -> {
                    // If no categories selected, include all
                    if (selectedCategories.isEmpty()) {
                        return true;
                    }
                    
                    // Otherwise only include selected categories
                    return selectedCategories.contains(product.getCategory());
                })
                .filter(product -> {
                    // Filter by price range
                    double price = product.getPrice();
                    return price >= minPrice && price <= maxPrice;
                })
                .collect(Collectors.toList());
        
        // Sort filtered products
        sortProducts(filteredProducts);
        
        // Update display
        updateProductsDisplay(filteredProducts);
    }
    
    private void sortProducts(List<Product> products) {
        String sortOption = sortingComboBox.getValue();
        
        switch (sortOption) {
            case "Name (A to Z)":
                products.sort(Comparator.comparing(Product::getName));
                break;
            case "Name (Z to A)":
                products.sort(Comparator.comparing(Product::getName).reversed());
                break;
            case "Price (Low to High)":
                products.sort(Comparator.comparing(Product::getPrice));
                break;
            case "Price (High to Low)":
                products.sort(Comparator.comparing(Product::getPrice).reversed());
                break;
            default:
                // Default sort by name
                products.sort(Comparator.comparing(Product::getName));
                break;
        }
    }
    
    @FXML
    private void handleResetFilters() {
        // Clear category selections
        categoryCheckboxes.values().forEach(cb -> cb.setSelected(false));
        
        // Reset price sliders
        minPriceSlider.setValue(absoluteMinPrice);
        maxPriceSlider.setValue(absoluteMaxPrice);
        
        // Reset sorting
        sortingComboBox.setValue("Name (A to Z)");
        
        // Reset to all products
        filteredProducts = new ArrayList<>(allProducts);
        sortProducts(filteredProducts);
        
        // Update display
        updateProductsDisplay(filteredProducts);
    }
    
    @FXML
    private void handleBackButton() {
        try {
            // Load the main view
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            Scene scene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Fade transition for smooth navigation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentStage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                currentStage.setScene(scene);
                currentStage.setTitle("EZ Shop");
                
                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), scene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateProductsDisplay(List<Product> products) {
        // Clear existing grid
        productsGrid.getChildren().clear();
        productsGrid.getColumnConstraints().clear();
        
        // Update products count label
        productsCountLabel.setText("Showing " + products.size() + " products");
        
        // Calculate columns based on available width
        double width = productsScrollPane.getWidth();
        int minCardWidth = 180; // Minimum width per card
        int columns = Math.max(1, (int) (width / minCardWidth));
        
        // Create column constraints
        for (int i = 0; i < columns; i++) {
            ColumnConstraints colConstraints = new ColumnConstraints();
            colConstraints.setPercentWidth(100.0 / columns);
            productsGrid.getColumnConstraints().add(colConstraints);
        }
        
        // Add products to grid
        int row = 0;
        int col = 0;
        
        for (Product product : products) {
            if (product != null && product.isAvailable()) {
                VBox productCard = createProductCard(product);
                productsGrid.add(productCard, col, row);
                
                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }
        }
        
        // Listen for width changes to make grid responsive
        productsScrollPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateProductsDisplay(filteredProducts);
        });
    }
    
    private VBox createProductCard(Product product) {
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
        
        // LINE 2: Price and Category Side by Side
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
        
        // Category label
        Label categoryLabel = new Label(product.getCategory());
        categoryLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-font-weight: normal;" +
            "-fx-text-fill: #3498db;" +
            "-fx-alignment: center;" +
            "-fx-background-color: #e1f0fa;" +
            "-fx-background-radius: 6px;" +
            "-fx-padding: 2px 6px;" +
            "-fx-border-color: #bde0f7;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 6px;"
        );
        
        // Add price and category to horizontal row
        priceStockRow.getChildren().addAll(priceLabel, categoryLabel);
        
        // LINE 3: Stock Status
        Label stockLabel = new Label();
        if (product.getStockQuantity() > 50) {
            stockLabel.setText("In Stock: " + product.getStockQuantity());
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
            stockLabel.setText("Limited Stock: " + product.getStockQuantity());
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
            stockLabel.setText("Low Stock: " + product.getStockQuantity());
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
        
        // Add all elements to the main VBox in clean lines
        productCard.getChildren().addAll(
            productImage,     // Image at top
            productLabel,     // Line 1: Product Name
            priceStockRow,    // Line 2: Price | Category (side by side)
            stockLabel        // Line 3: Stock status
        );
        
        productCard.setPrefSize(180, 280); // Adjusted height for cleaner layout
        
        // Apply canvas-style background using style class for consistent styling
        productCard.getStyleClass().add("product-card");
        
        // Add hover effects
        productCard.setOnMouseEntered(e -> {
            productCard.setScaleX(1.02);
            productCard.setScaleY(1.02);
        });
        
        productCard.setOnMouseExited(e -> {
            productCard.setScaleX(1.0);
            productCard.setScaleY(1.0);
        });
        
        // Click handler to open product details page
        productCard.setOnMouseClicked(e -> openProductPage(product));
        
        return productCard;
    }
    
    private void openProductPage(Product product) {
        try {
            Stage currentStage = (Stage) productsGrid.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Scene productScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            productScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Get the controller and pass product data
            TemplateProductController controller = loader.getController();
            controller.setProductData(product);
            
            // Set the scene
            currentStage.setScene(productScene);
            currentStage.setTitle("EZ Shop - " + product.getName());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to update price range sliders based on selected categories
    private void updatePriceRangeForCategories() {
        // Get selected categories
        List<String> selectedCategories = categoryCheckboxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // If no categories selected, use all products' price range
        if (selectedCategories.isEmpty()) {
            minPriceSlider.setMin(absoluteMinPrice);
            maxPriceSlider.setMax(absoluteMaxPrice);
            minPriceSlider.setValue(absoluteMinPrice);
            
            maxPriceSlider.setMin(absoluteMinPrice);
            maxPriceSlider.setMax(absoluteMaxPrice);
            maxPriceSlider.setValue(absoluteMaxPrice);
            
            minPriceLabel.setText(String.format("$%.2f", absoluteMinPrice));
            maxPriceLabel.setText(String.format("$%.2f", absoluteMaxPrice));
            return;
        }
        
        // Get products from selected categories
        List<Product> categoryProducts = allProducts.stream()
                .filter(product -> selectedCategories.contains(product.getCategory()))
                .collect(Collectors.toList());
        
        // Calculate min/max prices for selected categories
        double categoryMinPrice = categoryProducts.stream()
                .mapToDouble(Product::getPrice)
                .min()
                .orElse(absoluteMinPrice);
        
        double categoryMaxPrice = categoryProducts.stream()
                .mapToDouble(Product::getPrice)
                .max()
                .orElse(absoluteMaxPrice);
        
        // Update sliders
        minPriceSlider.setMin(categoryMinPrice);
        minPriceSlider.setMax(categoryMaxPrice);
        minPriceSlider.setValue(categoryMinPrice);
        
        maxPriceSlider.setMin(categoryMinPrice);
        maxPriceSlider.setMax(categoryMaxPrice);
        maxPriceSlider.setValue(categoryMaxPrice);
        
        // Update labels
        minPriceLabel.setText(String.format("$%.2f", categoryMinPrice));
        maxPriceLabel.setText(String.format("$%.2f", categoryMaxPrice));
    }
}