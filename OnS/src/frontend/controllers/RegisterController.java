package frontend.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import backend.network.NetworkService;
import backend.network.NetworkClient;
import java.util.Map;

public class RegisterController {
    @FXML
    private Button backToMainButton;
    @FXML
    private Button backToLoginButton;
    @FXML
    private Button registerButton;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField phoneField;

    public void backToMain(ActionEvent event) throws IOException {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        
        // Use fixed scene dimensions to prevent size drift
        double sceneWidth = stage.getWidth();
        double sceneHeight = stage.getHeight();
        
        // Load the main FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
        Parent mainRoot = loader.load();
        
        // Pre-set the opacity to 0 before creating the scene
        mainRoot.setOpacity(0.0);
        
        // Create the main scene with fixed dimensions and CSS
        Scene mainScene = new Scene(mainRoot, sceneWidth, sceneHeight);
        mainScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        
        // Set the scene background to match your app's background color
        mainScene.setFill(Color.web("#FAF6E9"));
        
        // Create fade out animation for current scene
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentScene.getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        fadeOut.setOnFinished(e -> {
            // Clear the current scene content before setting new scene
            if (currentScene.getRoot() != null) {
                currentScene.getRoot().setVisible(false);
            }
            
            // Set the scene with pre-set opacity 0 and matching background
            stage.setScene(mainScene);
            stage.setTitle("EZ Shop");
            
            // Use a very short delay to ensure the scene is set, then fade in
            Timeline fadeInDelay = new Timeline(
                new KeyFrame(Duration.millis(50), event2 -> {
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(250), mainRoot);
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

    public void backToLogin(ActionEvent event) throws IOException {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        
        // Use fixed scene dimensions to prevent size drift
        double sceneWidth = stage.getWidth();
        double sceneHeight = stage.getHeight();
        
        // Load the login FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
        Parent loginRoot = loader.load();
        
        // Pre-set the opacity to 0 before creating the scene
        loginRoot.setOpacity(0.0);
        
        // Create the login scene with fixed dimensions and CSS
        Scene loginScene = new Scene(loginRoot, sceneWidth, sceneHeight);
        loginScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        
        // Set the scene background to match your app's background color
        loginScene.setFill(Color.web("#FAF6E9"));
        
        // Create fade out animation for current scene
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentScene.getRoot());
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

    public void handleRegister(ActionEvent event) {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        
        // Basic validation
        if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill in all required fields");
            return;
        }
        
        processRegistration(username, password, email, fullName, address, phone);
    }
    
    private void processRegistration(String username, String password, String email, String fullName, String address, String phone) {
        NetworkService networkService = NetworkService.getInstance();
        
        if (!networkService.isConnected()) {
            showAlert("Connection Error", "Not connected to server. Please check your connection.");
            return;
        }
        
        // Set up registration listener
        networkService.setAuthListener(new NetworkClient.AuthListener() {
            @Override
            public void onRegisterSuccess(Map<String, Object> response) {
                Platform.runLater(() -> {
                    System.out.println("Registration successful: " + response);
                    showAlert("Success", "Registration successful! You can now login with your credentials.");
                    
                    // Clear all fields
                    clearAllFields();
                    
                    // Navigate to login page
                    try {
                        Stage stage = (Stage) registerButton.getScene().getWindow();
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Login.fxml"));
                        Parent loginRoot = loader.load();
                        
                        Scene loginScene = new Scene(loginRoot, stage.getWidth(), stage.getHeight());
                        loginScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                        loginScene.setFill(Color.web("#FAF6E9"));
                        
                        stage.setScene(loginScene);
                        stage.setTitle("EZ Shop - Login");
                        
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            
            @Override
            public void onRegisterFailed(Map<String, Object> error) {
                Platform.runLater(() -> {
                    System.out.println("Registration failed: " + error);
                    String errorMessage = (String) error.get("error");
                    showAlert("Registration Failed", errorMessage != null ? errorMessage : "Registration failed. Please try again.");
                    
                    // Clear password field
                    passwordField.clear();
                });
            }
            
            @Override
            public void onLoginSuccess(Map<String, Object> userData) {}
            @Override
            public void onLoginFailed(Map<String, String> error) {}
            @Override
            public void onAuthRequired() {}
        });
        
        // Send registration request
        networkService.register(username, password, email, fullName, address, phone);
    }
    
    private void clearAllFields() {
        fullNameField.clear();
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        addressField.clear();
        phoneField.clear();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Success") ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Add initialization method for any additional animations
    public void initialize() {
        // Add entrance animation for register form elements if needed
        if (backToMainButton != null) {
            backToMainButton.setOnMouseEntered(e -> {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), backToMainButton);
                pulse.setToX(1.1);
                pulse.setToY(1.1);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
            });
        }
        
        if (backToLoginButton != null) {
            backToLoginButton.setOnMouseEntered(e -> {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), backToLoginButton);
                pulse.setToX(1.05);
                pulse.setToY(1.05);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
            });
        }
        
        if (registerButton != null) {
            registerButton.setOnMouseEntered(e -> {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), registerButton);
                pulse.setToX(1.05);
                pulse.setToY(1.05);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
            });
        }
    }
}
