package com.bayoumi.util.gui.notfication;

import com.bayoumi.models.AzkarSettings;
import com.bayoumi.util.Logger;
import com.jfoenix.controls.JFXButton;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;


public class NotificationBox extends AnchorPane {

    private static MediaPlayer MEDIA_PLAYER;
    private final TextFlow textFlow;
    private final HBox hBox;
    private final Text text;
    // to control drag & drop
    private double xOffset = 0;
    private double yOffset = 0;

    public NotificationBox(String message) {
        text = new Text(message);
        text.getStyleClass().add("message");

        textFlow = new TextFlow(text);
        textFlow.setTextAlignment(TextAlignment.JUSTIFY);

        VBox vBox = new VBox(textFlow);
        vBox.setAlignment(Pos.CENTER);

        hBox = new HBox(vBox);
        hBox.setPadding(new Insets(30));
        hBox.setSpacing(20);
        AnchorPane.setRightAnchor(hBox, 20.0);
        AnchorPane.setLeftAnchor(hBox, 20.0);
        AnchorPane.setTopAnchor(hBox, 20.0);
        AnchorPane.setBottomAnchor(hBox, 20.0);
        hBox.setMaxWidth(500);
        hBox.setMinWidth(330);
        hBox.getStyleClass().add("message-box");
        HBox.setHgrow(vBox, Priority.ALWAYS);

        JFXButton close = new JFXButton("X");
        close.getStyleClass().add("close-button-stage");
        close.setOnAction(this::closeAction);
        close.setFocusTraversable(false);
        AnchorPane.setRightAnchor(close, 20.0);
        AnchorPane.setTopAnchor(close, 20.0);

        this.getChildren().addAll(hBox, close);
        this.getStyleClass().add("parent-bg");
        this.getStylesheets().add("/com/bayoumi/css/notification.css");

        setDraggable(this);

        Platform.runLater(this::initAnimation);

        playAlarmSound(AzkarSettings.getVolumeDB());
    }

    public NotificationBox(String msg, Image img) {
        this(msg);
        ImageView image = new ImageView(img);
        image.setFitHeight(50);
        image.setFitWidth(50);
        this.hBox.getChildren().add(0, image);
    }

    public void setDraggable(Node node) {
        node.setOnMouseDragged(e -> {
            ((Node) (e.getSource())).getScene().getWindow().setX(e.getScreenX() - xOffset);
            ((Node) (e.getSource())).getScene().getWindow().setY(e.getScreenY() - yOffset);
        });
        node.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });
    }

    public void playAlarmSound(int volume) {
        String fileName = AzkarSettings.getAudioNameDB();
        if (!fileName.equals("بدون صوت")) {
            MEDIA_PLAYER = new MediaPlayer(new Media(new File("jarFiles/audio/" + fileName).toURI().toString()));
            MEDIA_PLAYER.setVolume(volume / 100.0);
            MEDIA_PLAYER.play();
        }
    }

    private void initAnimation() {
        try {
            //Setting Translate Transition
            TranslateTransition translate = new TranslateTransition(Duration.millis(2000), this);
            double layoutY = this.getLayoutY();
            this.setTranslateY(Screen.getPrimary().getVisualBounds().getHeight() + 1000);
            translate.setToY(layoutY);
            //Setting Pause Transition
            PauseTransition pause = new PauseTransition(Duration.seconds(10));
            pause.setOnFinished(event -> {
                FadeTransition fade2 = new FadeTransition(Duration.millis(2500), this);
                fade2.setFromValue(10);
                fade2.setToValue(0);
                fade2.play();
                fade2.setOnFinished(this::closeAction);
            });
            //Setting Parallel Transition
            ParallelTransition parallelTransition = new ParallelTransition(this, pause, translate);
            parallelTransition.play();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(null, e, getClass().getName() + ".initAnimation()");
        }
    }

    private void closeAction(Event event) {
        System.out.println("Closing Notification ..."); // TODO : Delete println
        if (MEDIA_PLAYER != null) {
            MEDIA_PLAYER.stop();
        }
        ((Stage) (text).getScene().getWindow()).close();
    }

    public TextFlow getTextFlow() {
        return textFlow;
    }

}
