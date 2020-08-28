package com.bayoumi.home;

import com.bayoumi.util.notfication.Notification;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class HomeController implements Initializable {
    int i = 0;
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
        sp.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                newScene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
                    if (oldWindow == null && newWindow != null) {
                        Stage window = (Stage) newWindow;
//                        window.setOnCloseRequest(event -> {
//                            System.exit(0);
//                        });
                        Timer timer = new Timer();
                        TimerTask myTask = new TimerTask() {
                            @Override
                            public void run() {

                                Platform.runLater(() -> {
                                    Notification.create(window,
                                            "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد",
                                            new Image("/com/bayoumi/images/icons8_basilica_50px.png"));
                                });
                            }
                        };

                        timer.schedule(myTask, 2000, (long) 200000);
                        System.out.println(444);
                    }
                });
            }
        });
    }


    @FXML
    private void goToMorningAzkar(ActionEvent event) {
    }

    @FXML
    private void goToNightAzkar(ActionEvent event) {
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
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Notification.create(window,
                "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ على نَبِيِّنَا مُحمَّد",
                new Image("/com/bayoumi/images/icons8_basilica_50px.png"));
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
