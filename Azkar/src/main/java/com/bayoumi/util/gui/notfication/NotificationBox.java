package com.bayoumi.util.gui.notfication;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


public class NotificationBox extends AnchorPane {

    private final TextFlow textFlow;
    private final HBox hBox;
    private final Text text;

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

        Platform.runLater(this::initAnimation);
    }

    public NotificationBox(String msg, Image img) {
        this(msg);
        ImageView image = new ImageView(img);
        image.setFitHeight(50);
        image.setFitWidth(50);
        this.hBox.getChildren().add(0, image);
    }


    private void initAnimation() {
        try {
            //Setting Translate Transition
            TranslateTransition translate = new TranslateTransition(Duration.millis(2000), this);
            double layoutY = this.getLayoutY();
            Logger.debug("layoutY: " + layoutY);
            Logger.debug("XGXG: " + Screen.getPrimary().getVisualBounds().getHeight() + 1000);
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
            Logger.error(null, e, getClass().getName() + ".initAnimation()");
        }
    }

    private void closeAction(Event event) {
        ((Stage) (text).getScene().getWindow()).close();
    }

    public TextFlow getTextFlow() {
        return textFlow;
    }

}
