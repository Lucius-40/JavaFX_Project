package frontend.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable; // ADD THIS IMPORT
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import backend.models.UserSession;
import backend.models.Product;
import backend.network.NetworkService;

// ADD IMPLEMENTS INITIALIZABLE
public class HelloController implements Initializable {
    Stage stage;
    Scene scene;
    FXMLLoader root;

    @FXML
    private StackPane contentArea;
    @FXML
    private Button homeButton;
    @FXML
    private Button collectionsButton;
    @FXML
    private Button aboutButton;
    @FXML
    private Button clothesCategory;
    @FXML
    private Button shoeCategory;
    @FXML
    private Button othersCategory;
    @FXML
    private Button electronicCategory;
    @FXML
    private Button groceriesCategory;
    @FXML
    private Button stationaryCategory;
    @FXML
    private Button loginButton;
    @FXML
    private Button floatingCartButton;
    @FXML
    private Button searchButton;
    @FXML
    private TextField searchBar;

    @FXML
    private void loadHomePage() {
        loadClothesPage(); 
    }
    @FXML
    private void loadCollectionsPage() {
        try {
            Stage currentStage = (Stage) collectionsButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/CollectionsPage.fxml"));
            Parent root = loader.load();
            
            // Create the scene
            Scene collectionsScene = new Scene(root, currentStage.getWidth(), currentStage.getHeight());
            collectionsScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Set the scene immediately without animation first
            currentStage.setScene(collectionsScene);
            currentStage.setTitle("Collections - EZ Shop");
            
            // Add a simple fade-in animation for the new scene
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), root);
            fadeIn.setFromValue(0.5);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
        } catch (Exception e) {
            e.printStackTrace();
            // Display an alert to show the error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Loading Collections");
            alert.setHeaderText("Failed to load Collections page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    @FXML
    private void loadAboutPage() {
        try {
            Stage currentStage = (Stage) aboutButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/resources/fxml/About.fxml"));
            Scene aboutScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            aboutScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            currentStage.setScene(aboutScene);
            currentStage.setTitle("About Us - EZ Shop");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadView(String fxmlFile) {
        try {
            Node newNode = FXMLLoader.load(getClass().getResource("/resources/fxml/" + fxmlFile + ".fxml"));
            
            // If there's no existing content, just add the new node with animation
            if (contentArea.getChildren().isEmpty()) {
                contentArea.getChildren().add(newNode);
                
                // Create simple fade-in animation
                newNode.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newNode);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
                return;
            }
            
            // Get the current node to animate out
            Node currentNode = contentArea.getChildren().get(0);
            
            // Add the new node (initially positioned off-screen)
            newNode.setTranslateX(100);
            newNode.setOpacity(0);
            contentArea.getChildren().add(newNode);
            
            // Create smooth slide-out animation
            Timeline exitAnimation = new Timeline(
                new KeyFrame(Duration.millis(200),
                    new KeyValue(currentNode.translateXProperty(), -100, Interpolator.EASE_IN),
                    new KeyValue(currentNode.opacityProperty(), 0, Interpolator.EASE_IN)
                )
            );
            
            // Create slide-in animation
            Timeline enterAnimation = new Timeline(
                new KeyFrame(Duration.millis(300),
                    new KeyValue(newNode.translateXProperty(), 0, Interpolator.EASE_OUT),
                    new KeyValue(newNode.opacityProperty(), 1, Interpolator.EASE_OUT)
                )
            );
            
            exitAnimation.setOnFinished(e -> {
                contentArea.getChildren().remove(currentNode);
                enterAnimation.play();
            });
            
            exitAnimation.play();
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("FXML Load Error");
            alert.setHeaderText("Could not load page: " + fxmlFile);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void setActiveCategory(Button activeButton) {
        Button[] categoryButtons = {
            clothesCategory, shoeCategory, electronicCategory, 
            groceriesCategory, stationaryCategory, othersCategory
        };
        
        for (Button button : categoryButtons) {
            button.getStyleClass().remove("active");
            
            if (button != activeButton) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), button);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.8);
                fadeOut.play();
            }
        }
        
        activeButton.getStyleClass().add("active");
        
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), activeButton);
        scaleUp.setFromX(1.0);
        scaleUp.setFromY(1.0);
        scaleUp.setToX(1.05);
        scaleUp.setToY(1.05);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), activeButton);
        scaleDown.setFromX(1.05);
        scaleDown.setFromY(1.05);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), activeButton);
        fadeIn.setFromValue(0.8);
        fadeIn.setToValue(1.0);
        
        scaleUp.setOnFinished(e -> scaleDown.play());
        scaleDown.setOnFinished(e -> fadeIn.play());
        
        scaleUp.play();
    }

    public void loadElectronicsPage() {
        setActiveCategory(electronicCategory);
        loadView("Electronics");
    }

    public void loadShoesPage() {
        setActiveCategory(shoeCategory);
        loadView("Shoes");
    }

    public void loadGroceriesPage() {
        setActiveCategory(groceriesCategory);
        loadView("Groceries");
    }

    public void loadStationaryPage() {
        setActiveCategory(stationaryCategory);
        loadView("Stationary");
    }

    public void loadOthersPage() {
        setActiveCategory(othersCategory);
        loadView("Others");
    }

    public void loadClothesPage() {
        loadView("Clothes");
    }

    public void loadLoginPage(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        
        // Use fixed scene dimensions to prevent size drift
        double sceneWidth = currentScene.getWidth();
        double sceneHeight = currentScene.getHeight();
        
        // Load the login FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
        Parent loginRoot = loader.load();
        
        // Pre-set the opacity to 0 before creating the scene
        loginRoot.setOpacity(0.0);
        
        // Create a new scene with fixed dimensions
        Scene loginScene = new Scene(loginRoot, sceneWidth, sceneHeight);
        loginScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        
        // Set the scene background to match app's background color
        loginScene.setFill(Color.web("#ffffff")); 
        
        // Create fade out animation for current scene
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentScene.getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            // Clear the current scene content before setting new scene
            if (currentScene.getRoot() != null) {
                currentScene.getRoot().setVisible(false);
            }
            
            // Set the scene with pre-set opacity 0 and matching background
            stage.setScene(loginScene);
            stage.setTitle("EZ Shop - Login");
            
            // Use a very short delay to ensure the scene is set, then fade in
            Timeline fadeInDelay = new Timeline(
                new KeyFrame(Duration.millis(50), event2 -> {
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(250), loginRoot);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.setInterpolator(Interpolator.EASE_OUT);
                    fadeIn.play();
                })
            );
            fadeInDelay.play();
        });
        
        fadeOut.play();
    }
    
    public void hoverEffect(Button button, String normal, String hover) {
        button.setStyle(normal);
        
        button.setOnMouseEntered(e -> {
            // Create smooth hover animation
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);
            
            button.setStyle(hover);
            scaleUp.play();
        });
        
        button.setOnMouseExited(e -> {
            // Create smooth return to normal
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            
            button.setStyle(normal);
            scaleDown.play();
        });
        
        // Add click feedback
        button.setOnMousePressed(e -> {
            ScaleTransition clickAnimation = new ScaleTransition(Duration.millis(100), button);
            clickAnimation.setToX(0.95);
            clickAnimation.setToY(0.95);
            clickAnimation.play();
        });
        
        button.setOnMouseReleased(e -> {
            ScaleTransition releaseAnimation = new ScaleTransition(Duration.millis(100), button);
            releaseAnimation.setToX(1.0);
            releaseAnimation.setToY(1.0);
            releaseAnimation.play();
        });
    }

    public void initialize(URL location, ResourceBundle resources) {
        String normal = "-fx-background-color: #000000 ; -fx-text-fill: white";
        String hover = "-fx-background-color: #D1D8BE; -fx-text-fill: black";

        hoverEffect(clothesCategory, normal, hover);
        hoverEffect(shoeCategory, normal, hover);
        hoverEffect(electronicCategory, normal, hover);
        hoverEffect(stationaryCategory, normal, hover);
        hoverEffect(groceriesCategory, normal, hover);
        hoverEffect(othersCategory, normal, hover);
        hoverEffect(floatingCartButton, normal, hover);

        String navNormal = "-fx-background-color: transparent; -fx-text-fill: #2c1d08;";
        String navHover = "-fx-background-color: rgba(44, 29, 8, 0.1); -fx-text-fill: #2c1d08;";
        
        hoverEffect(homeButton, navNormal, navHover);
        hoverEffect(collectionsButton, navNormal, navHover);
        hoverEffect(aboutButton, navNormal, navHover);

        loadClothesPage();
        
        searchBar.setOnAction(event -> {
            searchAndOpenProductPage();
        });
        
        updateLoginButton();
        
        // Register for real-time inventory updates
        NetworkService.addInventoryObserver(() -> {
            System.out.println("📱 HelloController received inventory update");
            Platform.runLater(() -> {
                try {
                    refreshCurrentPage(); // Use a method that exists
                    System.out.println("✅ Product display refreshed");
                } catch (Exception e) {
                    System.err.println("❌ Error refreshing product display: " + e.getMessage());
                }
            });
        });
    }

    // ADD this method to handle inventory updates
    private void refreshCurrentPage() {
        // This method refreshes the current page content
        // You can implement specific logic based on what page is currently active
        try {
            // For now, just reload the clothes page as default
            // You can enhance this to detect which page is currently loaded
            loadClothesPage();
        } catch (Exception e) {
            System.err.println("Error refreshing current page: " + e.getMessage());
        }
    }

    public void loadCategory(String category) {
        switch (category.toLowerCase()) {
            case "clothes":
                loadClothesPage();
                break;
            case "electronics":
                loadElectronicsPage();
                break;
            case "shoes":
                loadShoesPage();
                break;
            case "groceries":
                loadGroceriesPage();
                break;
            case "stationary":
                loadStationaryPage();
                break;
            case "others":
                loadOthersPage();
                break;
            default:
                loadClothesPage(); // Default to clothes
                break;
        }
    }

    @FXML
    private void openCart() {
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
                loadLoginPage(new ActionEvent());
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
        
        // User is authenticated, proceed to cart
        try {
            // Get the current stage from the floating cart button
            Stage stage = (Stage) floatingCartButton.getScene().getWindow();
            Scene currentScene = stage.getScene();
            
            // Use fixed scene dimensions
            double sceneWidth = stage.getWidth();
            double sceneHeight = stage.getHeight();
            
            // Load the cart FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/CartPage.fxml"));
            Parent cartRoot = loader.load();

            cartRoot.getStyleClass().add("main-container");
            
            // Pre-set the opacity to 0 before creating the scene
            cartRoot.setOpacity(0.0);
            
            // Create a new scene with fixed dimensions
            Scene cartScene = new Scene(cartRoot, sceneWidth, sceneHeight);
            cartScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            
            // Set the scene background to match app's background color
            cartScene.setFill(Color.web("#ffffff")); 
            
            // Create fade out animation for current scene
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                // Clear the current scene content before setting new scene
                if (currentScene.getRoot() != null) {
                    currentScene.getRoot().setVisible(false);
                }
                
                // Set the scene with pre-set opacity 0 and matching background
                stage.setScene(cartScene);
                stage.setTitle("Shopping Cart - EZ Shop");
                
                // Use a very short delay to ensure the scene is set, then fade in
                Timeline fadeInDelay = new Timeline(
                    new KeyFrame(Duration.millis(50), event -> {
                        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), cartRoot);
                        fadeIn.setFromValue(0.0);
                        fadeIn.setToValue(1.0);
                        fadeIn.setInterpolator(Interpolator.EASE_OUT);
                        fadeIn.play();
                    })
                );
                fadeInDelay.play();
            });
            
            fadeOut.play();
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("FXML Load Error");
            alert.setHeaderText("Could not load Cart page");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void searchAndOpenProductPage() {
        String query = searchBar.getText().trim();
        
        if (query.isEmpty()) {
            searchBar.setPromptText("Please enter a search term");
            return;
        }
        
        //   SAFE: Check if inventory is loaded
        List<Product> products = NetworkService.getGlobalInventory();
        
        if (products == null || products.isEmpty()) {
            //   Show loading message instead of error
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Loading");
            alert.setHeaderText("Products still loading");
            alert.setContentText("Please wait a moment for products to load from server, then try again.");
            alert.showAndWait();
            return;
        }
        
        // Search for products matching the query
        List<Product> matchingProducts = new ArrayList<>();
        
        for (Product product : products) {
            if (product.getName().toLowerCase().contains(query.toLowerCase()) ||
                product.getCategory().toLowerCase().contains(query.toLowerCase()) ||
                product.getId().toLowerCase().contains(query.toLowerCase()) ||
                (product.getDescription() != null && 
                 product.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                matchingProducts.add(product);
            }
        }
        
        if (matchingProducts.isEmpty()) {
            // No matching products
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Search Results");
            alert.setHeaderText("No products found");
            alert.setContentText("No products match your search term: " + query);
            alert.showAndWait();
        } else if (matchingProducts.size() == 1) {
            // Only one product found, open its page directly
            openProductPage(matchingProducts.get(0));
        } else {
            // Multiple products found, show search results
            try {
                // Get the current stage
                Stage stage = (Stage) searchBar.getScene().getWindow();
                Scene currentScene = stage.getScene();
                
                // Load the search results FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/searchResults.fxml"));
                Parent searchRoot = loader.load();
                
                // Pass search results to the controller
                SearchResultsController controller = loader.getController();
                controller.setResults(matchingProducts, query);
                
                // Create a new scene
                Scene searchScene = new Scene(searchRoot, currentScene.getWidth(), currentScene.getHeight());
                searchScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                searchScene.setFill(Color.web("#FAF6E9"));
                
                // Create fade out animation for current scene
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentScene.getRoot());
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                
                fadeOut.setOnFinished(e -> {
                    stage.setScene(searchScene);
                    stage.setTitle("Search Results - EZ Shop");
                    
                    // Fade in new scene
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), searchScene.getRoot());
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                
                fadeOut.play();
                
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Could not load search results");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            }
        }
        
        searchBar.clear();
    }

    // Helper method to open a product page
    private void openProductPage(Product product) {
        try {
            Stage stage = (Stage) searchBar.getScene().getWindow();
            Scene currentScene = stage.getScene();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/TemplateProductPage.fxml"));
            Parent productRoot = loader.load();
            
            // Get the controller and set the product data
            TemplateProductController controller = loader.getController();
            controller.setProductData(product);
            
            // Create new scene
            Scene productScene = new Scene(productRoot, currentScene.getWidth(), currentScene.getHeight());
            productScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            productScene.setFill(Color.web("#FAF6E9"));
            
            // Create fade transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentScene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                stage.setScene(productScene);
                stage.setTitle(product.getName() + " - EZ Shop");
                
                // Fade in new scene
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), productScene.getRoot());
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();
            
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open product page");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    // Replace the existing updateLoginButton method with this:
    public void updateLoginButton() {
        // Check if user is logged in
        if (UserSession.getInstance().isLoggedIn()) {
            // Show username instead of "Logout"
            String username = UserSession.getInstance().getUsername();
            loginButton.setText("👤 " + username);
            
            // Calculate appropriate width based on username length
            double textWidth = username.length() * 8; // Approximate character width
            double buttonWidth = Math.max(100, Math.min(150, textWidth + 40)); // Min 100, Max 150
            
            loginButton.setPrefWidth(buttonWidth);
            loginButton.setMinWidth(buttonWidth);
            loginButton.setMaxWidth(150); // Cap at 150 to prevent excessive width
            
            // Change styling to indicate logged in state
            loginButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 11px;");
            
            // Remove old action and set new logout action
            loginButton.setOnAction(this::showLogoutDialog);
            
        } else {
            loginButton.setText("Login");
            
            // Reset to original login button size
            loginButton.setPrefWidth(80);
            loginButton.setMinWidth(76);
            loginButton.setMaxWidth(80);
            
            // Reset to login style
            loginButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2c1d08; -fx-font-weight: bold; -fx-font-size: 12px;");
            
            // Remove old action and set login action
            loginButton.setOnAction(this::loadLoginPageFromButton);
        }
    }

    // Add this new method to show logout confirmation dialog
    private void showLogoutDialog(ActionEvent event) {
        Alert logoutAlert = new Alert(Alert.AlertType.CONFIRMATION);
        logoutAlert.setTitle("Logout");
        logoutAlert.setHeaderText("Are you sure you want to logout?");
        logoutAlert.setContentText("You will be logged out of your account and disconnected from the server.");
        
        // Style the dialog to match your app
        logoutAlert.getDialogPane().setStyle("-fx-background-color: #ffffff;");
        
        // Add custom button types
        ButtonType logoutButtonType = new ButtonType("Logout");
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
        logoutAlert.getButtonTypes().setAll(logoutButtonType, cancelButtonType);
        
        Optional<ButtonType> result = logoutAlert.showAndWait();
        if (result.isPresent() && result.get() == logoutButtonType) {
            handleLogout();
        }
    }

    // Add this new method to handle the logout process
    private void handleLogout() {
        try {
            // Disconnect from server if connected
            NetworkService networkService = NetworkService.getInstance();
            if (networkService.isConnected()) {
                networkService.disconnect();
                System.out.println("🔌 Disconnected from server during logout");
            }
            
            // Clear user session
            UserSession.getInstance().logout();
            System.out.println("👤 User logged out successfully");
            
            // Update the login button to show "Login" again
            updateLoginButton();
            
            // Show logout success message
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Logout Successful");
            successAlert.setHeaderText(null);
            successAlert.setContentText("You have been logged out successfully!");
            successAlert.getDialogPane().setStyle("-fx-background-color: #FAF6E9;");
            successAlert.showAndWait();
            
            System.out.println("✅ Logout process completed");
            
        } catch (Exception e) {
            System.err.println("❌ Error during logout: " + e.getMessage());
            e.printStackTrace();
            
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Logout Error");
            errorAlert.setHeaderText("Error during logout");
            errorAlert.setContentText("There was an error logging out: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    // Add this wrapper method for button action
    private void loadLoginPageFromButton(ActionEvent event) {
        try {
            loadLoginPage(event);
        } catch (IOException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("Failed to load login page");
            errorAlert.setContentText("Error: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    // Add a public method that can be called from LoginController
    public void refreshLoginButton() {
        updateLoginButton();
    }
}