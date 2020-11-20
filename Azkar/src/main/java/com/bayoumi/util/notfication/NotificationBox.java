package com.bayoumi.util.notfication;

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
import javafx.scene.input.MouseEvent;
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

    private static final MediaPlayer MEDIA_PLAYER = new MediaPlayer(new Media(new File("jarFiles/audio/notification.mp3").toURI().toString()));
    private final TextFlow textFlow;
    private final HBox notificationBox;
    private double xOffset = 0;
    private double yOffset = 0;
    private Text text;
    private ImageView image;

    public NotificationBox(String message) {
        text = new Text(message);
        text.getStyleClass().add("message");

        textFlow = new TextFlow(text);
        textFlow.setTextAlignment(TextAlignment.JUSTIFY);

        VBox vBox = new VBox(textFlow);
        vBox.setAlignment(Pos.CENTER);

        notificationBox = new HBox(vBox);
        notificationBox.setPadding(new Insets(30));
        notificationBox.setSpacing(20);
        AnchorPane.setRightAnchor(notificationBox, 20.0);
        AnchorPane.setLeftAnchor(notificationBox, 20.0);
        AnchorPane.setTopAnchor(notificationBox, 20.0);
        AnchorPane.setBottomAnchor(notificationBox, 20.0);
        notificationBox.setMaxWidth(500);
        notificationBox.setMinWidth(330);
        notificationBox.getStyleClass().add("message-box");
        HBox.setHgrow(vBox, Priority.ALWAYS);

        JFXButton close = new JFXButton("X");
        close.getStyleClass().add("close-button-stage");
        close.setOnAction(this::closeAction);
        close.setFocusTraversable(false);
        AnchorPane.setRightAnchor(close, 20.0);
        AnchorPane.setTopAnchor(close, 20.0);

        this.getChildren().addAll(notificationBox, close);
        this.getStyleClass().add("parent-bg");
        this.getStylesheets().add("/com/bayoumi/style/notification.css");
        this.setOnMouseDragged(this::RootMouseDragged);
        this.setOnMousePressed(this::RootMousePressed);
//        this.setOnMouseReleased(this::closeAction);
        Platform.runLater(this::initAnimation);

        MEDIA_PLAYER.play();
    }


    public NotificationBox(String msg, Image img) {
        this(msg);
        this.image = new ImageView(img);
        this.image.setFitHeight(50);
        this.image.setFitWidth(50);
        this.notificationBox.getChildren().add(0, this.image);
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
        System.out.println("Closing Notification ...");
        MEDIA_PLAYER.stop();
        ((Stage) (text).getScene().getWindow()).close();
    }


    public void RootMousePressed(Event event) {
        MouseEvent e = (MouseEvent) event;
        xOffset = e.getSceneX();
        yOffset = e.getSceneY();
    }

    public void RootMouseDragged(Event event) {
        MouseEvent e = (MouseEvent) event;
        ((Node) (event.getSource())).getScene().getWindow().setX(e.getScreenX() - xOffset);
        ((Node) (event.getSource())).getScene().getWindow().setY(e.getScreenY() - yOffset);
    }

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public TextFlow getTextFlow() {
        return textFlow;
    }


    public ImageView getImage() {
        return image;
    }

    public void setImage(ImageView image) {
        this.image = image;
    }

}
