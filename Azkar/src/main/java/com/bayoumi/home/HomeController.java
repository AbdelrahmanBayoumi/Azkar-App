package com.bayoumi.home;

import com.jfoenix.controls.JFXButton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
    @FXML
    private StackPane sp;
    @FXML
    private Label frequencyLabel;
    @FXML
    private JFXButton highFrequency;
    @FXML
    private JFXButton midFrequency;
    @FXML
    private JFXButton lowFrequency;
    @FXML
    private JFXButton rearFrequency;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void goToMorningAzkar(ActionEvent event) {
//        System.out.println(frequencyLabel.getFont());
//        frequencyLabel.setFont(Font.font("System", FontWeight.BOLD, frequencyLabel.getFont().getSize() * 1.5 > 90 ? frequencyLabel.getFont().getSize() : frequencyLabel.getFont().getSize() * 1.5));
    }

    @FXML
    private void goToNightAzkar(ActionEvent event) {
//        System.out.println(frequencyLabel.getFont());
//        frequencyLabel.setFont(Font.font("System", FontWeight.BOLD, frequencyLabel.getFont().getSize() * (2 / 3.0) < 11 ? frequencyLabel.getFont().getSize() : frequencyLabel.getFont().getSize() * (2 / 3.0)));
//        frequencyLabel.setFont(Font.font("System", FontWeight.BOLD, frequencyLabel.getFont().getSize() * (2 / 3.0) < 11 ? frequencyLabel.getFont().getSize() : frequencyLabel.getFont().getSize() * (2 / 3.0)));
    }

    @FXML
    private void goToPrayerTimes(ActionEvent event) {
        Notifications notification = Notifications.create()
                .title("عنوان")
                .text("كلمة عربي")
                .graphic(new ImageView(new Image("com/bayoumi/images/icons8_basilica_50px.png")))
                .hideAfter(Duration.seconds(10))
                .position(Pos.BOTTOM_RIGHT)
                .onAction(event1 -> {
                    System.out.println("clicked");
                });
        notification.show();
    }

    @FXML
    private void goToSettings(ActionEvent event) {
    }

    @FXML
    private void highFrequencyAction(ActionEvent event) {

    }

    @FXML
    private void lowFrequencyAction(ActionEvent event) {

    }

    @FXML
    private void midFrequencyAction(ActionEvent event) {

    }

    @FXML
    private void rearFrequencyAction(ActionEvent event) {

    }

}
