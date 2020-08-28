package com.bayoumi.util.notfication;

import javafx.application.Platform;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Notification {
//    private static boolean isShown = false;
//
//    private Stage ownerStage;
//    private String msg;
    private static Stage oldStage = null;

    public static void create(Stage owner, String msg, Image image) {
        NotificationBox notification;
        if (image == null) {
            notification = new NotificationBox(msg);
        } else {
            notification = new NotificationBox(msg, image);
        }
        notification.getTextFlow().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        Scene scene = new Scene(notification);
        scene.setFill(Color.TRANSPARENT);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initStyle(StageStyle.UTILITY);
        stage.initStyle(StageStyle.TRANSPARENT);
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        Platform.runLater(() -> {
            stage.setX(bounds.getWidth() - (notification.getWidth()) - 0);
            stage.setY(bounds.getHeight() - (notification.getHeight()) - 0);
            System.out.println((notification.getWidth()));
            System.out.println((notification.getHeight()));
        });
        stage.setAlwaysOnTop(true);
        if(oldStage != null){
            oldStage.close();
        }
        stage.show();
        oldStage = stage;
    }
//
//    public void show() {
//        this.stage.show();
//    }

//    public Stage getOwnerStage() {
//        return ownerStage;
//    }
//
//    public void setOwnerStage(Stage owner) {
//        this.ownerStage = owner;
//    }
//
//    public String getMsg() {
//        return msg;
//    }
//
//    public void setMsg(String msg) {
//        this.msg = msg;
//    }
}
