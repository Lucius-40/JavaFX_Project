package frontend.controllers;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class PopUpController {

    @FXML
    private Label hourLabel;
    @FXML
    private Label hourNumberLabel;
    @FXML
    private Label minuteLabel;
    @FXML
    private Label minuteNumberLabel;
    @FXML
    private Label secondLabel;
    @FXML
    private Label secondNumberLabel;
    @FXML private Button crossButton ;

    int hours = 12;
    int minutes = 32;
    int seconds = 56;
    @FXML
     void initialize(){
        countDown();
        crossButton.setOnAction(e -> shutDown());
     }

    private void countDown() {
        hourNumberLabel.setText(String.valueOf(hours));
        minuteNumberLabel.setText(String.valueOf(minutes));
        secondNumberLabel.setText(String.valueOf(seconds));
        long[] lastUpdate = { 0 };
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long time) {
                if (lastUpdate[0] == 0) {
                    lastUpdate[0] = time;
                    return;
                }
                if (time - lastUpdate[0] >= 1000000000) {
                    lastUpdate[0]= time ;
                    if (seconds > 0)
                        seconds--;
                    else if (minutes > 0) {
                        minutes--;
                        seconds = 59;
                    } else if (hours > 0) {
                        hours--;
                        minutes = 59;
                        seconds = 59;
                    } else {
                        stop();
                    }
                }

                hourNumberLabel.setText(String.valueOf(hours));
                minuteNumberLabel.setText(String.valueOf(minutes));
                secondNumberLabel.setText(String.valueOf(seconds));

            }
        };
        timer.start();

    }
    @FXML
    private void shutDown(){
        ((Stage)crossButton.getScene().getWindow()).close();

    }

}
