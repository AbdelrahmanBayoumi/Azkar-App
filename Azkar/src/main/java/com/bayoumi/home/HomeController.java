package com.bayoumi.home;

import com.bayoumi.azkar.timed.TimedAzkarController;
import com.bayoumi.datamodel.AbsoluteZekr;
import com.bayoumi.main.Launcher;
import com.bayoumi.util.EditablePeriodTimerTask;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.notfication.Notification;
import com.bayoumi.util.validation.SingleInstance;
import com.jfoenix.controls.JFXButton;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class HomeController implements Initializable {

    //    private Timeline timeline_debug;
//    private LocalTime time_debug = LocalTime.parse("00:00:00");
    private EditablePeriodTimerTask absoluteAzkarTask;
    @FXML
    private Label hijriDate;
    @FXML
    private Label day;
    @FXML
    private Label gregorianDate;
    @FXML
    private Label timeLabel;
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
    private JFXButton currentFrequency;
    private boolean isLoaded;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentFrequency = highFrequency;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

        if (!AbsoluteZekr.fetchData()) {
            Launcher.workFine.setValue(false);
            return;
        }
        absoluteAzkarTask = new EditablePeriodTimerTask(()
                -> Platform.runLater(()
                -> Notification.create(
                AbsoluteZekr.absoluteZekrObservableList.get(
                        new Random().nextInt(AbsoluteZekr.absoluteZekrObservableList.size())).getText(),
                new Image("/com/bayoumi/images/"
//                        + (new Random().nextInt(10) % 2 == 0 ? "11" : "22")
                        + "22"
                        + ".png"))),
                this::getPeriod);
        absoluteAzkarTask.updateTimer("initialize()");

//        timeline_debug = new Timeline(new KeyFrame(Duration.millis(1000), ae -> {
//            time_debug = time_debug.plusSeconds(1);
//            System.out.println(time_debug.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
//        }));
//        timeline_debug.setCycleCount(Animation.INDEFINITE);
//        timeline_debug.play();
        initClock();

        Date date = new Date();
        day.setText(Utility.getDay("ar", date));
        hijriDate.setText(Utility.getGregorianDate("ar"));
        gregorianDate.setText(Utility.getHijriDate("ar"));
    }

    private void initClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm:ss a");
//            timeLabel.setText(LocalDateTime.now().format(formatter));
            timeLabel.setText(Utility.getTime("ar", new Date()));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private Long getPeriod() {
        if (currentFrequency.equals(highFrequency)) {
            return 300000L; // 5 min
        } else if (currentFrequency.equals(midFrequency)) {
            return 600000L; // 10 min
        } else if (currentFrequency.equals(lowFrequency)) {
            return 1200000L; // 20 min
        } else if (currentFrequency.equals(rearFrequency)) {
            return 1800000L; // 30 min
        }
        return 300000L;

//        if (currentFrequency.equals(highFrequency)) {
//            return 15000L;
//        } else if (currentFrequency.equals(midFrequency)) {
//            return 30000L;
//        } else if (currentFrequency.equals(lowFrequency)) {
//            return 40000L;
//        } else if (currentFrequency.equals(rearFrequency)) {
//            return 50000L;
//        }
//        return 50000L;
    }

    @FXML
    private void goToMorningAzkar() {
        showTimedAzkar("morning");
    }

    @FXML
    private void goToNightAzkar() {
        showTimedAzkar("night");
    }

    private void showTimedAzkar(String type) {
        try {
            Stage stage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bayoumi/fxml/azkar/timed/TimedAzkar.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            stage.initModality(Modality.APPLICATION_MODAL);
            Utility.SetIcon(stage);
            TimedAzkarController controller = loader.getController();
            controller.setData(type);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".showTimedAzkar()");
        }
    }

    @FXML
    private void highFrequencyAction() {
        String msg = "ظهور كل" + " " + 5 + " " + "دقائق";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(highFrequency);
    }

    @FXML
    private void midFrequencyAction() {
        String msg = "ظهور كل" + " " + 10 + " " + "دقائق";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(midFrequency);
    }

    @FXML
    private void lowFrequencyAction() {
        String msg = "ظهور كل" + " " +  20 + " " + "دقيقة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(lowFrequency);
    }

    @FXML
    private void rearFrequencyAction() {
        String msg = "ظهور كل" + " " +  30 + " " + "دقيقة";
        frequencyLabel.setText(msg);
        toggleFrequencyBTN(rearFrequency);
    }

    private void toggleFrequencyBTN(JFXButton b) {
        currentFrequency.getStyleClass().remove("frequency-btn-selected");
        currentFrequency = b;
        currentFrequency.getStyleClass().add("frequency-btn-selected");

//        timeline_debug.stop();
//        time_debug = LocalTime.parse("00:00:00");
//        timeline_debug.play();
        absoluteAzkarTask.updateTimer("toggle()");
    }

    @FXML
    private void goToAzkar() {
        System.out.println("goToAzkar() ..");
        try {
            Stage stage = new Stage();
            stage.setScene(new Scene(FXMLLoader.load(getClass().getResource("/com/bayoumi/fxml/azkar/absolute/AbsoluteAzkar.fxml"))));
            stage.initOwner(SingleInstance.getInstance().getCurrentStage());
            Utility.SetIcon(stage);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".goToAzkar()");
        }

    }

    @FXML
    private void goToPrayerTimes() {
        isLoaded = false;
        System.out.println("goToPrayerTimes");
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        webView.setZoom(1.5);  //zoom in 25%.
        engine.load("https://prayer-times-bayoumi.herokuapp.com");
        engine.setUserAgent("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        VBox.setVgrow(root, Priority.ALWAYS);

        String s = "Loading ";
        Label label = new Label(s);
        label.setFont(Font.font("system", FontWeight.BOLD, 30));

        engine.getLoadWorker().stateProperty().addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        if (newValue == Worker.State.SUCCEEDED) {
                            isLoaded = true;
                            root.getChildren().remove(label);
                            webView.setVisible(true);
                        }
                    }
                }
        );
        webView.setContextMenuEnabled(false);
        webView.setVisible(false);
        root.getChildren().addAll(label, webView);
        Scene scene = new Scene(root, 540, 580);
        Stage stage = new Stage();
        stage.setResizable(false);
        Utility.SetAppDecoration(stage);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.setOnCloseRequest((event) -> isLoaded = true);
        stage.show();
        new Thread(() -> {
            while (!isLoaded) {
                for (int i = 0; i < 3; i++) {
                    Platform.runLater(() -> label.setText(label.getText() + "."));
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                Platform.runLater(() -> label.setText(s));
            }
        }).start();

    }

    @FXML
    private void goToSettings() {
        System.out.println("goToSettings");
    }
}
