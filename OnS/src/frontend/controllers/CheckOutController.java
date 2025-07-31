package frontend.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import backend.models.Product;
import backend.models.UserSession;
import backend.network.NetworkClient;
import backend.network.NetworkService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import backend.models.Cart;
import javafx.scene.control.Alert;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.PrintWriter;

public class CheckOutController {
    @FXML
    private Button backToMainButton;
    @FXML
    TextField nameField;
    @FXML
    TextField addressField;
    @FXML
    TextField contactField;
    @FXML
    TextField postCodeField;
    @FXML
    Button checkoutButton;

    @FXML
    public void initialize() {
        //   Auto-populate fields with user data if logged in
        populateUserFields();
        
        // Only require postal code to be filled (since it's not stored in registration)
        checkoutButton.disableProperty()
                .bind(postCodeField.textProperty().isEmpty());
    }

    //   New method to populate fields with user data
    private void populateUserFields() {
        UserSession userSession = UserSession.getInstance();
        
        if (userSession.isLoggedIn()) {
            // Get user data from session
            String fullName = userSession.getFullName();
            String username = userSession.getUsername();
            
            // Set the name field (use full name if available, otherwise username)
            if (fullName != null && !fullName.trim().isEmpty() && !fullName.equals("N/A")) {
                nameField.setText(fullName);
            } else {
                nameField.setText(username);
            }
            
            // Request additional user data from server
            requestUserDataFromServer();
        } else {
            // If not logged in, show placeholder text
            nameField.setPromptText("Enter your full name");
            addressField.setPromptText("Enter your address");
            contactField.setPromptText("Enter your phone number");
            postCodeField.setPromptText("Enter postal code");
        }
    }

    //   Request user data from server
    private void requestUserDataFromServer() {
        NetworkService networkService = NetworkService.getInstance();
        
        if (networkService.isConnected()) {
            // Set up listener for user data response
            NetworkClient client = NetworkClient.getInstance();
            client.setUserDataListener(new NetworkClient.UserDataListener() {
                @Override
                public void onUserDataReceived(Map<String, String> userData) {
                    Platform.runLater(() -> {
                        // Populate address field
                        String address = userData.get("address");
                        if (address != null && !address.trim().isEmpty() && !address.equals("N/A")) {
                            addressField.setText(address);
                        }
                        
                        // Populate contact field
                        String phone = userData.get("phone");
                        if (phone != null && !phone.trim().isEmpty() && !phone.equals("N/A")) {
                            contactField.setText(phone);
                        }
                        
                        System.out.println("User data populated in checkout form");
                    });
                }
                
                @Override
                public void onUserDataError(String error) {
                    System.out.println("Could not fetch user data: " + error);
                    // Fields will remain empty, user can fill manually
                }
            });
            
            // Request user data from server
            String sessionId = UserSession.getInstance().getSessionId();
            client.requestUserData(sessionId);
        }
    }

    @FXML
    void goBackToMain() {
        try {
            Stage currentStage = (Stage) backToMainButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            Scene mainScene = new Scene(loader.load(), currentStage.getWidth(), currentStage.getHeight());
            currentStage.setScene(mainScene);
            currentStage.setTitle("EZ Shop");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void checkOut() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String contact = contactField.getText().trim();
        String postCode = postCodeField.getText().trim();

        //   Enhanced validation with better messages
        if (name.isEmpty()) {
            showAlert("Missing Information", "Please enter your name.");
            nameField.requestFocus();
            return;
        }
        
        if (address.isEmpty()) {
            showAlert("Missing Information", "Please enter your address.");
            addressField.requestFocus();
            return;
        }
        
        if (contact.isEmpty()) {
            showAlert("Missing Information", "Please enter your contact number.");
            contactField.requestFocus();
            return;
        }
        
        if (postCode.isEmpty()) {
            showAlert("Missing Information", "Please enter your postal code.");
            postCodeField.requestFocus();
            return;
        }

        Cart cart = Cart.getInstance();
        NetworkService networkService = NetworkService.getInstance();
        
        if (networkService.isConnected()) {
            // Create complete order data
            Map<String, Object> orderData = new HashMap<>();
            
            // Add customer information
            Map<String, String> customerInfo = new HashMap<>();
            customerInfo.put("name", name);
            customerInfo.put("address", address);
            customerInfo.put("contact", contact);
            customerInfo.put("postCode", postCode);
            orderData.put("customerInfo", customerInfo);
            
            // Add cart items
            Map<String, Integer> purchaseItems = new HashMap<>();
            for (Cart.CartItem item : cart.getItems()) {
                purchaseItems.put(item.getProduct().getId(), item.getQuantity());
            }
            orderData.put("items", purchaseItems);
            
            // Send complete order to server
            networkService.sendCompleteOrder(orderData);  // You'll need to add this method
            
            // Clear cart
            Cart.getInstance().clear();
            nameField.clear();
            addressField.clear();
            contactField.clear();
            postCodeField.clear();
            showAlert("Order Submitted", "Your order has been submitted successfully! Please pay cash on delivery ");
            
        } else {
            showAlert("Connection Error", "Cannot process purchase - not connected to server");
        }
    }

    //   Helper method to show alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}