package frontend.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.IOException;
import java.util.*;

import backend.models.Cart;
import backend.network.NetworkClient;
import backend.network.NetworkService;
import backend.models.UserSession;

public class CartController {

    @FXML
    private Button cartBackButton;

    @FXML
    private ScrollPane cartScrollPane;
    @FXML
    private Label numberOfProductsLabel;
    @FXML
    private Label totalPriceLabel;
    @FXML
    private TextArea productListArea;
    @FXML
    private Button payButton;

    @FXML
    private void handleBackButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            Parent mainRoot = loader.load();

            mainRoot.getStylesheets().add("main-container");
            
            // Get the current stage (cart window)
            Stage currentStage = (Stage) cartBackButton.getScene().getWindow();
            
            // Use fixed scene dimensions
            double sceneWidth = currentStage.getWidth();
            double sceneHeight = currentStage.getHeight();
            
            // Load the main page FXML
            System.out.println(loader.toString());

            // Pre-set the opacity to 0 before creating the scene
            mainRoot.setOpacity(0.0);

            // Create a new scene with fixed dimensions
            Scene mainScene = new Scene(mainRoot, sceneWidth, sceneHeight);
            mainScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());

            // Set the scene background to match app's background color
            mainScene.setFill(Color.web("#FFFFFF"));

            // Clear the current scene content before setting new scene
            Scene currentScene = currentStage.getScene();
            if (currentScene != null && currentScene.getRoot() != null) {
                currentScene.getRoot().setVisible(false);
            }

            // Set the scene to the current stage
            currentStage.setScene(mainScene);
            currentStage.setTitle("EZ Shop");

            // Create fade in animation for the main page
            Timeline fadeInDelay = new Timeline(
                    new KeyFrame(Duration.millis(50), event -> {
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), mainRoot);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.setInterpolator(Interpolator.EASE_OUT);
                        fadeIn.play();
                    }));
            fadeInDelay.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadCartData() {
        Cart cart = Cart.getInstance();
        List<Cart.CartItem> items = cart.getItems();

        numberOfProductsLabel.setText(String.valueOf(items.size()));

        totalPriceLabel.setText("$" + String.format("%.2f",cart.getTotalAmount()));

        updateProductList(items);

        createCartItemDisplay(items);
    }

    private void updateProductList(List<Cart.CartItem> items) {
        StringBuilder productList = new StringBuilder();
        if (items.isEmpty()) {
            productList.append("Cart is Empty! Add products to get Started !");
        } else {
            productList.append("Cart Content : \n\n");
            for (Cart.CartItem i : items) {
                productList.append("📦 ").append(i.getProduct().getName()).append("\n");
                productList.append("   Category: ").append(i.getProduct().getCategory()).append("\n");
                productList.append("   Quantity: ").append(i.getQuantity()).append("\n");
                productList.append("   Unit Price: $").append(String.format("%.2f", i.getProduct().getPrice()))
                        .append("\n");
                productList.append("   Subtotal: $").append(String.format("%.2f", i.getSubtotal())).append("\n");
                productList.append("\n\n");
            }
        }
        productListArea.setText(productList.toString());
        productListArea.setStyle("-fx-background-color: #FFFFFF; -fx-control-inner-background: #FFFFFF;");

    }

    private HBox createCartItemRow(Cart.CartItem item) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #ffffff; -fx-border-radius: 5px; -fx-background-radius: 5px;");

        // Product name
        ImageView imageView = new ImageView();
        imageView.setFitWidth(48);
        imageView.setFitHeight(48);
        imageView.setPreserveRatio(true);
        try {
            String imagePath = item.getProduct().getImagePath();
            if(!imagePath.startsWith("/resources")){
                imagePath = "/resources" + imagePath;
            }
            imageView.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            System.out.println("No image found");
            e.printStackTrace();
        }
        Label nameLabel = new Label(item.getProduct().getName());
        nameLabel.setPrefWidth(120);
        nameLabel.setStyle("-fx-font-weight: bold;");
        nameLabel.setAlignment(Pos.CENTER);

        VBox imageAndLabel = new VBox(4, imageView, nameLabel);
        imageAndLabel.setAlignment(Pos.CENTER);

        // Quantity controls
        Button minusBtn = new Button("-");
        minusBtn.setPrefWidth(30);
        minusBtn.setStyle("-fx-background-color:rgb(115, 8, 8); -fx-text-fill: white;");

        Label qtyLabel = new Label(String.valueOf(item.getQuantity()));
        qtyLabel.setPrefWidth(30);
        qtyLabel.setAlignment(Pos.CENTER);
        qtyLabel.setStyle("-fx-border-color: #FFFFFF; -fx-border-width: 1px; -fx-padding: 5px;");

        Button plusBtn = new Button("+");
        plusBtn.setPrefWidth(30);
        plusBtn.setStyle("-fx-background-color:rgb(11, 88, 13); -fx-text-fill: white;");

        // Price
        Label priceLabel = new Label(String.format("$%.2f", item.getSubtotal()));
        priceLabel.setPrefWidth(60);
        priceLabel.setStyle(" -fx-text-fill: #2196F3;");

        // Remove button
        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-background-color:rgb(132, 22, 14); -fx-text-fill: white;");

        // Add event handlers
        minusBtn.setOnAction(e -> updateQuantity(item.getProduct().getId(), item.getQuantity() - 1));
        plusBtn.setOnAction(e -> updateQuantity(item.getProduct().getId(), item.getQuantity() + 1));
        removeBtn.setOnAction(e -> removeItem(item.getProduct().getId()));

        row.getChildren().addAll(imageAndLabel, minusBtn, qtyLabel, plusBtn, priceLabel, removeBtn);
        // Already set above, no need to override with cream
        return row;
    }

    private void createCartItemDisplay(List<Cart.CartItem> items) {
        VBox cartContainer = new VBox(10);
        cartContainer.setPadding(new Insets(10));
        cartContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ffffff;");

        cartScrollPane.setFitToWidth(true); // Makes content fit scroll pane width
        cartScrollPane.setFitToHeight(false);
        cartScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); 
        cartScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        
        // Set ScrollPane background to cream color
        cartScrollPane.setStyle("-fx-background: #ffffff; -fx-background-color: #ffffff; -fx-border-color: #ffffff; -fx-focus-color: transparent;"); 
        cartContainer.setStyle("-fx-background-color: #FFFFFF;");
        
        for (Cart.CartItem item : items) {
            HBox itemRow = createCartItemRow(item);
            cartContainer.getChildren().add(itemRow);
        }

        cartScrollPane.setContent(cartContainer);
    }

    private void updateQuantity(String productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(productId);
            return;
        }

        Cart.getInstance().updateQuantity(productId, newQuantity);
        loadCartData(); // Refresh display
    }

    private void removeItem(String productId) {
        Cart.getInstance().removeItem(productId);
        loadCartData(); // Refresh display
    }

    @FXML
    private void proceedToCheckout() {
        // Check if user is authenticated
        if (!UserSession.getInstance().isLoggedIn()) {
            // Show login required message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Login Required");
            alert.setHeaderText("Authentication Required");
            alert.setContentText("Please login to proceed to checkout.");
            alert.showAndWait();
            
            // Redirect to login page
            try {
                Stage currentStage = (Stage) payButton.getScene().getWindow();
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
        
        // Continue with existing checkout logic
        Cart cart = Cart.getInstance();
        double p = cart.getTotalAmount();
        if( p == 0) return ;
        try {
            Stage currentStage = (Stage) payButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Checkout.fxml"));
            Scene checkoutScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            // Get the current stage from the pay button
            currentStage.setScene(checkoutScene);
            currentStage.setTitle("Checkout");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


    }

    @FXML
    public void initialize() {
        loadCartData();
        payButton.setOnAction(e -> proceedToCheckout());

        NetworkService networkService = NetworkService.getInstance();
        if (networkService.isConnected()) {
            networkService.setPurchaseListener(new NetworkClient.PurchaseListener() {
                @Override
                public void onPurchaseSuccess(Map<String, Object> response) {
                   System.out.println("Purhase successful");
                }
                
                @Override
                public void onPurchaseFailure(Map<String, Object> error) {
                    Platform.runLater(() -> {
                        System.out.println("Purchase failed: " + error);
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Purchase Failed");
                        alert.setHeaderText("Unable to process your order");
                        alert.setContentText("Error: " + error.get("errors"));
                        alert.showAndWait();
                    });
                }
            });
        }
    }

}
