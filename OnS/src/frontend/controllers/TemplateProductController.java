package frontend.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.*;
import backend.models.Product;
import backend.models.UserSession;

import java.io.IOException;
import java.util.List;

import backend.models.Cart;
import javafx.application.Platform;
import backend.network.NetworkService;

public class TemplateProductController {
    @FXML
    private ImageView productImage;
    @FXML
    private Label titleLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label stockLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label stockWarningLabel;
    @FXML
    private TextArea productDescription;
    @FXML
    private Spinner<Integer> quantitySpinner;
    @FXML
    private Button addToCartButton;
    @FXML
    private Button seeCartButton;
    @FXML
    private Button backButton;

    private Product currentProduct;
    private int originalStock;

    @FXML
    public void initialize() {
        if (stockWarningLabel != null) {
            stockWarningLabel.setVisible(false);
        }
        
        //   REPLACE: existing inventory listener
        //   WITH: Centralized observer pattern
        NetworkService.addInventoryObserver(this::refreshProductFromGlobalInventory);
    }

    //   ADD: New method to refresh current product from global inventory
    private void refreshProductFromGlobalInventory() {
        if (currentProduct == null) {
            return;
        }

        System.out.println("Number of products: " + currentProduct.isAvailable());
        
        List<Product> globalInventory = NetworkService.getGlobalInventory();
        
        // Find updated product in the global inventory
        for (Product globalProduct : globalInventory) {
            if (globalProduct.getId().equals(currentProduct.getId())) {
                //   FIX: Update the current product with new stock
                int newStock = globalProduct.getStockQuantity();
                currentProduct.setStockQuantity(newStock);
                
                //   FIX: Update the UI with the NEW stock
                Platform.runLater(() -> {
                    stockLabel.setText(String.valueOf(newStock));
                    updateStockDisplay(newStock);
                });
                
                System.out.println("📊 TemplateProduct updated " + currentProduct.getId() + " stock to: " + newStock); 
                break;
            }
        }
    }

    //   Add this method to update UI when stock changes
    public void updateStockDisplay(int newStock) {
        // Update stock color
        if (newStock <= 10) {
            stockLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red for low stock
        } else if (newStock <= 50) {
            stockLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange for medium stock
        } else {
            stockLabel.setStyle("-fx-text-fill: #27ae60;"); // Green for high stock
        }
        
        // Update spinner max value and warning
        configureQuantitySpinner(newStock);
        updateStockWarning(newStock);
        
        // Disable button if out of stock
        if (newStock <= 0) {
            addToCartButton.setDisable(true);
            addToCartButton.setText("Out of Stock");
            quantitySpinner.setDisable(true);
        } else {
            addToCartButton.setDisable(false);
            addToCartButton.setText("Add to Cart");
            quantitySpinner.setDisable(false);
        }
    }

    public void setProductData(Product product) {
        this.currentProduct = product;
        this.originalStock = product.getStockQuantity();
        
        // Set product title
        titleLabel.setText(product.getName());
        
        // Set product price with dollar sign
        priceLabel.setText("$" + String.format("%.2f", product.getPrice()));
        
        // Set stock information with color coding
        stockLabel.setText(String.valueOf(product.getStockQuantity()));
        if (product.getStockQuantity() <= 10) {
            stockLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red for low stock
        } else if (product.getStockQuantity() <= 50) {
            stockLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange for medium stock
        } else {
            stockLabel.setStyle("-fx-text-fill: #27ae60;"); // Green for high stock
        }
        
        // Set category
        categoryLabel.setText(product.getCategory());
        
        // Set description (if available)
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            productDescription.setText(product.getDescription());
        } else {
            productDescription.setText("No description available for this product.");
        }
        
        // Load product image
        loadProductImage(product);
        
        // Configure quantity spinner with limits based on stock
        configureQuantitySpinner(product.getStockQuantity());
        
        // Update stock warning
        updateStockWarning(product.getStockQuantity());
        
        // Disable add to cart if out of stock
        if (product.getStockQuantity() <= 0) {
            addToCartButton.setDisable(true);
            addToCartButton.setText("Out of Stock");
            
            // Make spinner disabled
            quantitySpinner.setDisable(true);
        }
    }

    private void loadProductImage(Product product) {
        try {
            String imagePath = product.getImagePath();
            if (!imagePath.startsWith("/resources/")) {
                imagePath = "/resources" + imagePath;
            }
            
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            productImage.setImage(image);
            
            // subtle zoom animation on load
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(300), productImage);
            scaleUp.setFromX(0.95);
            scaleUp.setFromY(0.95);
            scaleUp.setToX(1.0);
            scaleUp.setToY(1.0);
            scaleUp.play();
            
        } catch (Exception e) {
            // Fallback to dummy image
            try {
                Image fallbackImage = new Image(getClass().getResourceAsStream("/resources/images/Dummy_Product.jpg"));
                productImage.setImage(fallbackImage);
            } catch (Exception ex) {
                System.err.println("Could not load product image: " + ex.getMessage());
            }
        }
    }
    
    private void configureQuantitySpinner(int maxStock) {
        // value factory with limits
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Math.max(1, maxStock), 1);
        
        quantitySpinner.setValueFactory(valueFactory);
        quantitySpinner.setEditable(true);
        
        // Add listener to prevent invalid values being typed
        quantitySpinner.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                return;
            }
            
            try {
                int value = Integer.parseInt(newValue);
                
                // If value exceeds stock, reset to max stock
                if (value > maxStock) {
                    quantitySpinner.getValueFactory().setValue(maxStock);
                    updateStockWarning(maxStock);
                    
                    // Briefly show warning
                    showMaxStockWarning(maxStock);
                } else {
                    stockWarningLabel.setVisible(false);
                }
            } catch (NumberFormatException e) {
                // If input is not a number, reset to old value
                quantitySpinner.getEditor().setText(oldValue);
            }
        });
        
        // Listen for spinner value changes
        quantitySpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue > maxStock) {
                quantitySpinner.getValueFactory().setValue(maxStock);
                showMaxStockWarning(maxStock);
            } else {
                stockWarningLabel.setVisible(false);
            }
        });
    }
    
    private void showMaxStockWarning(int maxStock) {
        if (stockWarningLabel != null) {
            stockWarningLabel.setText("Maximum stock: " + maxStock);
            stockWarningLabel.setVisible(true);
            
            // Add fade animation for warning
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), stockWarningLabel);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), stockWarningLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setDelay(Duration.seconds(3));
            
            // Chain animations
            fadeIn.setOnFinished(e -> fadeOut.play());
            fadeIn.play();
        }
    }
    
    private void updateStockWarning(int currentStock) {
        if (stockWarningLabel != null) {
            if (currentStock <= 5 && currentStock > 0) {
                stockWarningLabel.setText("Only " + currentStock + " left in stock!");
                stockWarningLabel.setVisible(true);
            } else {
                stockWarningLabel.setVisible(false);
            }
        }
    }

    @FXML
    private void handleAddToCart() {
        if (currentProduct == null) return;
        
        // Check if user is authenticated
        if (!UserSession.getInstance().isLoggedIn()) {
            // Show login required message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Required");
            alert.setHeaderText("Authentication Required");
            alert.setContentText("Please login to add items to your cart.");
            alert.showAndWait();
            
            // Redirect to login page
            try {
                Stage currentStage = (Stage) addToCartButton.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
                Parent loginRoot = loader.load();
                
                Scene loginScene = new Scene(loginRoot, currentStage.getWidth(), currentStage.getHeight());
                loginScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                loginScene.setFill(Color.web("#FAF6E9"));
                
                // Fade transition
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentStage.getScene().getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                
                fadeOut.setOnFinished(e -> {
                    currentStage.setScene(loginScene);
                    currentStage.setTitle("EZ Shop - Login");
                    
                    // Fade in new scene
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), loginScene.getRoot());
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                
                fadeOut.play();
            } catch (IOException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to load login page");
                errorAlert.setContentText("Error: " + e.getMessage());
                errorAlert.showAndWait();
            }
            return;
        }
        
        int quantity = quantitySpinner.getValue();
        
        // Check if enough stock available
        if (quantity > currentProduct.getStockQuantity()) {
            showAlert("Insufficient Stock", 
                     "Only " + currentProduct.getStockQuantity() + " items available in stock.");
            return;
        }
        
        // Add to cart - using cart instance
        Cart cart = Cart.getInstance();
        cart.addItem(currentProduct, quantity);
        
        // Show success animation and message
        showAddToCartAnimation();
        
        // Update stock display
        int newStock = currentProduct.getStockQuantity() - quantity;
        currentProduct.setStockQuantity(newStock);
        stockLabel.setText(String.valueOf(newStock));
        
        // Update stock color based on new value
        if (newStock <= 10) {
            stockLabel.setStyle("-fx-text-fill: #e74c3c;"); // Red for low stock
        } else if (newStock <= 50) {
            stockLabel.setStyle("-fx-text-fill: #f39c12;"); // Orange for medium stock
        } else {
            stockLabel.setStyle("-fx-text-fill: #27ae60;"); // Green for high stock
        }
        
        // Update spinner max value and warning
        configureQuantitySpinner(newStock);
        updateStockWarning(newStock);
        
        // Disable button if out of stock
        if (newStock <= 0) {
            addToCartButton.setDisable(true);
            addToCartButton.setText("Out of Stock");
            quantitySpinner.setDisable(true);
        }
    }
    
    private void showAddToCartAnimation() {
        // Create success alert with animation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Added to Cart");
        alert.setHeaderText("Item added successfully!");
        alert.setContentText(quantitySpinner.getValue() + " x " + currentProduct.getName() + " added to your cart.");
        
        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        dialogPane.getStyleClass().add("cart-alert");
        
        // Show alert
        alert.showAndWait();
    }

    @FXML
    private void handleSeeCart() {
        // Check if user is authenticated
        if (!UserSession.getInstance().isLoggedIn()) {
            // Show login required message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Required");
            alert.setHeaderText("Authentication Required");
            alert.setContentText("Please login to access your cart.");
            alert.showAndWait();
            
            // Redirect to login page
            try {
                Stage currentStage = (Stage) seeCartButton.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
                Parent loginRoot = loader.load();
                
                Scene loginScene = new Scene(loginRoot, currentStage.getWidth(), currentStage.getHeight());
                loginScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                loginScene.setFill(Color.web("#FAF6E9"));
                
                // Fade transition
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentStage.getScene().getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                
                fadeOut.setOnFinished(e -> {
                    currentStage.setScene(loginScene);
                    currentStage.setTitle("EZ Shop - Login");
                    
                    // Fade in new scene
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), loginScene.getRoot());
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                
                fadeOut.play();
            } catch (IOException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Failed to load login page");
                errorAlert.setContentText("Error: " + e.getMessage());
                errorAlert.showAndWait();
            }
            return;
        }
        
        try {
            Stage currentStage = (Stage) seeCartButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/CartPage.fxml"));
            Scene cartScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            cartScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            
            // Fade transition for smooth navigation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentStage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                currentStage.setScene(cartScene);
                currentStage.setTitle("Shopping Cart - EZ Shop");
                
                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), cartScene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open cart page: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackButton() {
        try {
            Stage currentStage = (Stage) backButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            Scene mainScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            mainScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Fade transition for smooth navigation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentStage.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                currentStage.setScene(mainScene);
                currentStage.setTitle("EZ Shop");
                
                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), mainScene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not return to main page: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        
        alert.showAndWait();
    }
    
    // Add this method:
    public void cleanup() {
        NetworkService.removeInventoryObserver(this::refreshProductFromGlobalInventory);
        System.out.println("🧹 TemplateProductController cleanup completed");
    }
}
