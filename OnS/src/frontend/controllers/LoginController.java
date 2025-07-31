package frontend.controllers;

import backend.models.UserSession;
import backend.network.NetworkService;
import backend.network.NetworkClient;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Map;

public class LoginController {
    @FXML
    private Button backButton;
    @FXML
    private Button signupButton;
    @FXML
    private Button loginButton;
    @FXML 
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;

    @FXML
    public void handleLogin(ActionEvent event){
        String username = usernameField.getText();
        String password = passwordField.getText();
        
        
        if(username.trim().isEmpty() || password.trim().isEmpty()){
            System.out.println("Please fill out all the fields");
            return;
        }
        
        // System.out.println("Username: " + username);
        // System.out.println("Password: " + password);
        System.out.println("about to call the jsonbuilder");
        processLogin(username, password);
    }

    public void processLogin(String username, String password) {
        NetworkService networkService = NetworkService.getInstance();
        
        if (!networkService.isConnected()) {
            System.out.println("Not connected to server");
            return;
        }
        
        // Set up authentication listener
        networkService.setAuthListener(new NetworkClient.AuthListener() {
            @Override
            public void onLoginSuccess(Map<String, Object> userData) {
                Platform.runLater(() -> {
                    System.out.println("Login successful: " + userData);
                    
                    // Extract user data
                    String sessionId = (String) userData.get("sessionId");
                    String username = (String) userData.get("username");
                    String fullName = (String) userData.get("fullName");
                    
                    // Create user session
                    UserSession.getInstance().login(username, fullName, sessionId);
                    
                    // Show success message
                    System.out.println("✅ Welcome back, " + username + "!");
                    
                    // Navigate back to main page
                    try {
                        Stage stage = (Stage) loginButton.getScene().getWindow();
                        Scene currentScene = stage.getScene();
                        
                        double sceneWidth = currentScene.getWidth();
                        double sceneHeight = currentScene.getHeight();
                        
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
                        Parent mainRoot = loader.load();
                        
                        mainRoot.setOpacity(0.0);
                        
                        Scene mainScene = new Scene(mainRoot, sceneWidth, sceneHeight);
                        mainScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                        mainScene.setFill(Color.web("#FAF6E9"));
                        
                        stage.setScene(mainScene);
                        stage.setTitle("EZ Shop - Welcome " + username);
                        
                        // IMPORTANT: Update login button on main page to show username
                        HelloController helloController = loader.getController();
                        helloController.refreshLoginButton();
                        
                        Timeline fadeInDelay = new Timeline(
                            new KeyFrame(Duration.millis(50), event -> {
                                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), mainRoot);
                                fadeIn.setFromValue(0.0);
                                fadeIn.setToValue(1.0);
                                fadeIn.setInterpolator(Interpolator.EASE_OUT);
                                fadeIn.play();
                            })
                        );
                        fadeInDelay.play();
                        
                        // Show welcome message
                        Timeline welcomeDelay = new Timeline(
                            new KeyFrame(Duration.millis(500), event -> {
                                Alert welcomeAlert = new Alert(Alert.AlertType.INFORMATION);
                                welcomeAlert.setTitle("Welcome Back!");
                                welcomeAlert.setHeaderText(null);
                                welcomeAlert.setContentText("Welcome back, " + fullName + "!\nYou are now logged in.");
                                welcomeAlert.getDialogPane().setStyle("-fx-background-color: #FAF6E9;");
                                welcomeAlert.show();
                            })
                        );
                        welcomeDelay.play();
                        
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            
            @Override
            public void onLoginFailed(Map<String, String> error) {
                Platform.runLater(() -> {
                    System.out.println("Login failed: " + error.get("error"));
                    // Show error message to user
                    usernameField.clear();
                    passwordField.clear();
                    usernameField.setPromptText("Login failed - try again");
                });
            }
            
            @Override
            public void onRegisterSuccess(Map<String, Object> response) {}
            @Override
            public void onRegisterFailed(Map<String, Object> error) {}
            @Override
            public void onAuthRequired() {}
        });
        
        // Send login request
        networkService.login(username, password);
    }

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
        mainScene.setFill(Color.web("#FAF6E9")); // Match your app's background
        
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
    
    public void loadRegisterPage(ActionEvent event) throws IOException {
        Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        Scene currentScene = stage.getScene();
        
        // Use fixed scene dimensions to prevent size drift
        double sceneWidth = stage.getWidth();
        double sceneHeight = stage.getHeight();
        
        // Load the register FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/Register.fxml"));
        Parent registerRoot = loader.load();
        
        // Pre-set the opacity to 0 before creating the scene
        registerRoot.setOpacity(0.0);
        
        // Create the register scene with fixed dimensions and CSS
        Scene registerScene = new Scene(registerRoot, sceneWidth, sceneHeight);
        registerScene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
        
        // Set the scene background to match your app's background color
        registerScene.setFill(Color.web("#FAF6E9"));
        
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
            stage.setScene(registerScene);
            stage.setTitle("EZ Shop - Register");
            
            // Use a very short delay to ensure the scene is set, then fade in
            Timeline fadeInDelay = new Timeline(
                new KeyFrame(Duration.millis(50), event2 -> {
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(250), registerRoot);
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
    
    // Add initialization method for any additional animations
    public void initialize() {
        // Add entrance animation for login form elements if needed
        if (backButton != null) {
            backButton.setOnMouseEntered(e -> {
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), backButton);
                pulse.setToX(1.1);
                pulse.setToY(1.1);
                pulse.setAutoReverse(true);
                pulse.setCycleCount(2);
                pulse.play();
            });
        }
    }
}
