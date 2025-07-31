package frontend.controllers;


import javafx.fxml.FXML;
import javafx.scene.control.Button;



public class AboutController{
    @FXML Button backToMain ;
    @FXML
    public void backToMain() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) backToMain.getScene().getWindow();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/resources/fxml/hello-view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/resources/styles/styles.css").toExternalForm());
            // Get the current stage from the button
            stage.setScene(scene);
            stage.setTitle("EZ Shop");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}