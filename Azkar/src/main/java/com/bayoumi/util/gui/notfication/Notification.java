package com.bayoumi.util.gui.notfication;

import com.bayoumi.models.AzkarSettings;
import com.bayoumi.util.gui.Draggable;
import com.bayoumi.util.gui.HelperMethods;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.io.File;

public class Notification {

    private static MediaPlayer MEDIA_PLAYER;
    private static Stage oldStage = null;

    public static void create(String msg, Image image) {
        NotificationBox notification;
        if (image == null) {
            notification = new NotificationBox(msg);
        } else {
            notification = new NotificationBox(msg, image);
        }
        // for arabic text
        notification.getTextFlow().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        new Draggable(notification);
        Scene scene = new Scene(notification);
        scene.setFill(Color.TRANSPARENT);

        Stage fakeStage = new Stage(StageStyle.UTILITY);
        fakeStage.setOpacity(0);
        fakeStage.show();

        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(fakeStage);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        if (oldStage != null) {
            oldStage.close();
        }
        oldStage = stage;
        stage.show();

        stage.setOnHiding(event -> {
            System.out.println("Closing Notification ..."); // TODO : Delete println
            if (MEDIA_PLAYER != null) {
                MEDIA_PLAYER.stop();
            }
        });

        System.out.println("Opening Notification ...");
        HelperMethods.setStageToBottom(stage, notification.getWidth(), notification.getHeight());
        playAlarmSound(AzkarSettings.getVolumeDB());
    }

    public static void createControlsFX(String msg, Image image) {
        Notifications.create()
                .graphic(createGraphic(msg, image))
                .hideAfter(Duration.seconds(10))
                .position(Pos.BOTTOM_RIGHT).show();

        playAlarmSound(AzkarSettings.getVolumeDB());
    }

    private static HBox createGraphic(String msg, Image image) {
        Text text = new Text(msg);
        text.getStyleClass().add("message");

        TextFlow textFlow = new TextFlow(text);
        textFlow.setTextAlignment(TextAlignment.JUSTIFY);
        textFlow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        VBox vBox = new VBox(textFlow);
        vBox.setAlignment(Pos.CENTER);

        HBox hBox = new HBox(vBox);
        hBox.setPadding(new Insets(30));
        hBox.setSpacing(20);
        AnchorPane.setRightAnchor(hBox, 20.0);
        AnchorPane.setLeftAnchor(hBox, 20.0);
        AnchorPane.setTopAnchor(hBox, 20.0);
        AnchorPane.setBottomAnchor(hBox, 20.0);
        hBox.setMaxWidth(500);
        hBox.setMinWidth(330);
        HBox.setHgrow(vBox, Priority.ALWAYS);

        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(50);
            imageView.setFitWidth(50);
            hBox.getChildren().add(0, imageView);
        }

        hBox.sceneProperty().addListener((observableScene, oldScene, newScene) -> {
            if (oldScene == null && newScene != null) {
                hBox.getScene().getStylesheets().clear();
                hBox.getScene().getStylesheets().add(0, "/com/bayoumi/css/controlsfx-notification.css");
            }
        });

        new Draggable(hBox);
        return hBox;
    }

    private static void playAlarmSound(int volume) {
        String fileName = AzkarSettings.getAudioNameDB();
        if (!fileName.equals("بدون صوت")) {
            MEDIA_PLAYER = new MediaPlayer(new Media(new File("jarFiles/audio/" + fileName).toURI().toString()));
            MEDIA_PLAYER.setVolume(volume / 100.0);
            MEDIA_PLAYER.play();
        }
    }
}


/*
        hBox.setOnMousePressed(event -> {
            System.out.println("1: " + ((Label) hBox.getParent()).getPadding().toString());
            System.out.println("2: " + ((GridPane) hBox.getParent().getParent()).getPadding());
            ((GridPane) hBox.getParent().getParent()).setPadding(new Insets(0));
            System.out.println("3: " + hBox.getParent().getParent().getParent());
            System.out.println("4: " + ((Pane) hBox.getParent().getParent().getParent().getParent()).getPadding().toString());
            System.out.println("getScene: " + hBox.getScene());
        });
* */