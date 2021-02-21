package com.bayoumi.util.gui.notfication;

import com.bayoumi.util.gui.HelperMethods;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Notification {
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

        System.out.println("Opening Notification ...");
        HelperMethods.setStageToBottom(stage, notification.getWidth(), notification.getHeight());
    }
}
