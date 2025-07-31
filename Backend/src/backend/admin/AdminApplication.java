package backend.admin;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminApplication extends Application {
    
    private AdminController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/backend/admin/AdminPanel.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 800);
        
        controller = loader.getController();
        
        primaryStage.setTitle("EZ Shop - Admin Panel");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            controller.shutdown();
            Platform.exit();
        });
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

