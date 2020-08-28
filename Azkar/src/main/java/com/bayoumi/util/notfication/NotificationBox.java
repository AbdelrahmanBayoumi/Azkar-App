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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;


public class NotificationBox extends AnchorPane {

    private double xOffset = 0;
    private double yOffset = 0;
    private Text text;
    private TextFlow textFlow;
    private ImageView image;
    private HBox notificationBox;

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
    }


    public NotificationBox(String msg, Image img) {
        this(msg);
        this.image = new ImageView(img);
        this.notificationBox.getChildren().add(0, this.image);
    }

    private void initAnimation() {
        try {
            //Setting Translate Transition
            TranslateTransition translate = new TranslateTransition(Duration.millis(2000), this);
            double layoutY = this.getLayoutX();
            this.setTranslateX(100000);
            translate.setToX(layoutY);
            //Setting Fade Transition
            FadeTransition fade = new FadeTransition(Duration.millis(2500), this);
            fade.setFromValue(0);
            fade.setToValue(10);
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
            ParallelTransition parallelTransition = new ParallelTransition(this, pause, translate, fade);
            parallelTransition.play();
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(null, e, getClass().getName() + ".initAnimation()");
        }
    }

    private void closeAction(Event event) {
        ((Stage) (text).getScene().getWindow()).close();
    }


    public void RootMousePressed(Event event) {
        MouseEvent e = (MouseEvent) event;
        xOffset = e.getSceneX();
        yOffset = e.getSceneY();
    }

    public void RootMouseDragged(Event event) {
        MouseEvent e = (MouseEvent) event;
        ((Stage) (((Node) (event.getSource())).getScene().getWindow())).setX(e.getScreenX() - xOffset);
        ((Stage) (((Node) (event.getSource())).getScene().getWindow())).setY(e.getScreenY() - yOffset);
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

    public void setTextFlow(TextFlow textFlow) {
        this.textFlow = textFlow;
    }

    public ImageView getImage() {
        return image;
    }

    public void setImage(ImageView image) {
        this.image = image;
    }

    public HBox getNotificationBox() {
        return notificationBox;
    }

    public void setNotificationBox(HBox notificationBox) {
        this.notificationBox = notificationBox;
    }
}
