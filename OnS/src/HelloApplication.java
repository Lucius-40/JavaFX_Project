import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;


import backend.network.NetworkService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        NetworkService networkService = NetworkService.getInstance();
        
        // Set up the inventory listener FIRST
        networkService.setInventoryUpdateListener(inventory -> {
            System.out.println("  HelloApplication: Centralized inventory listener received: " + inventory.size() + " products");
        });
        
        CompletableFuture<Boolean> connectionFuture = networkService.connectToServer("127.0.0.1", 8888);
        
        connectionFuture.thenRun(() -> {
            Platform.runLater(() -> {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/resources/fxml/hello-view.fxml"));
                    Scene scene = new Scene(fxmlLoader.load(), 1200, 700);
                    
                    scene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
                    // Set application icon
                    stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/images/Logo.png")));
                    stage.setTitle("EZ Shop");
                    stage.setScene(scene);
                    stage.show();
                    
                    System.out.println("  UI loaded successfully!");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
     }

     @Override
     public void stop(){
        NetworkService.getInstance().disconnect();
    }
     

    public static void main(String[] args) {
        launch();
    }
}