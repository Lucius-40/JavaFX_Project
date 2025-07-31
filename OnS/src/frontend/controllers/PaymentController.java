package frontend.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class PaymentController {

    private CheckOutController checkOutController ;

    public void setCheckOutController(CheckOutController controller){
        this.checkOutController = controller; 
        }

        @FXML private Button okButton ;

        @FXML private void shutDown(){
        ((Stage)okButton.getScene().getWindow()).close();
        if(checkOutController != null) checkOutController.goBackToMain();
    } 

    @FXML public void initialize(){
        okButton.setOnAction(e -> shutDown());
    }

    
}
