package com.bayoumi.util.gui.notfication;

import com.bayoumi.controllers.notification.NotificationsControlsFXController;
import com.bayoumi.models.AzkarSettings;
import com.bayoumi.models.NotificationSettings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.Draggable;
import com.bayoumi.util.gui.HelperMethods;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

import java.io.File;

public class Notification {

    private static MediaPlayer MEDIA_PLAYER;
    private static Stage oldStage = null;


    public static void create(String msg, Image image) {
        try {
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

            System.out.println("Opening Notification ...");  // TODO : Delete println
            HelperMethods.setStageToBottom(stage, notification.getWidth(), notification.getHeight());
            playAlarmSound(AzkarSettings.getVolumeDB());
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".create()");
            ex.printStackTrace();
        }
    }

    public static void createControlsFX(String msg, Image image, Runnable callback, double duration) {
        try {
            FXMLLoader loader = new FXMLLoader(Notification.class.getResource("/com/bayoumi/views/notification/NotificationsControlsFX.fxml"));
            Parent notificationView = loader.load();
            ((NotificationsControlsFXController) loader.getController()).setData(msg, image);
            Platform.runLater(() -> Notifications.create()
                    .graphic(notificationView)
                    .hideCloseButton()
                    .hideAfter(Duration.seconds(duration))
                    .onAction(event -> {
                        System.out.println("Closing Notification ..."); // TODO : Delete println
                        if (MEDIA_PLAYER != null) {
                            MEDIA_PLAYER.stop();
                        }
                        if (callback != null) {
                            callback.run();
                        }
                    })
                    .position(new NotificationSettings().getPosition()).show());

            playAlarmSound(AzkarSettings.getVolumeDB());
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".createControlsFX()");
            ex.printStackTrace();
        }

     /*
        try {
            Platform.runLater(() -> Notifications.create()
                    .graphic(createGraphicX(msg, image))
                    .text(" ")
                    .hideAfter(Duration.seconds(10))
                    .position(Pos.BOTTOM_RIGHT).show());

            playAlarmSound(AzkarSettings.getVolumeDB());
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".createControlsFX()");
            ex.printStackTrace();
        }
     * */
    }

    private static HBox createGraphicX(String msg, Image image) {
        Label label = new Label(msg);
        label.getStyleClass().add("message");
//        label.setWrapText(true);
//        label.setTextAlignment(TextAlignment.JUSTIFY);
        label.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
//        label.setMaxWidth(330);
        VBox vBox = new VBox(label);
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
//                System.out.println("88888888");
                if (hBox.getScene().getStylesheets().contains("/com/bayoumi/css/style.css")) {
//                    System.out.println("55555555555555");
                    hBox.getScene().getStylesheets().remove("/com/bayoumi/css/style.css");
                }
                if (hBox.getScene().getStylesheets().isEmpty()) {
//                    System.out.println("7777777");
                    hBox.getScene().getStylesheets().add(0, "/com/bayoumi/css/controlsfx-notification.css");
                }
            }
        });
        hBox.setOnMouseClicked(event -> System.out.println(hBox.getScene().getStylesheets()));
        new Draggable(hBox);
        return hBox;
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
//                System.out.println("88888888");
                if (hBox.getScene().getStylesheets().contains("/com/bayoumi/css/style.css")) {
//                    System.out.println("55555555555555");
                    hBox.getScene().getStylesheets().remove("/com/bayoumi/css/style.css");
                }
                if (hBox.getScene().getStylesheets().isEmpty()) {
//                    System.out.println("7777777");
                    hBox.getScene().getStylesheets().add(0, "/com/bayoumi/css/controlsfx-notification.css");
                }
            }
        });
        hBox.setOnMouseClicked(event -> System.out.println(hBox.getScene().getStylesheets()));
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
    private static Popup popup = new Popup();
    private static final double PADDING = 15; // TODO DEL
    private static Window window;
  public static void createNew(String msg, Image image) {
        popup = new Popup();
        System.out.println("createNew ...");
        try {
            NotificationBox notification;
            if (image == null) {
                notification = new NotificationBox(msg);
            } else {
                notification = new NotificationBox(msg, image);
            }
            // for arabic text
            notification.getTextFlow().setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            // support drag & drop for notification body
            new Draggable(notification);
            // use a fake stage so that if the main is closed the notification does not affected
            if (window == null) {
                Stage fakeStage = new Stage(StageStyle.UTILITY);
                fakeStage.setOpacity(0);
                fakeStage.show();
                fakeStage.toBack();
                window = fakeStage;
            }
//            HBox node = createGraphic("text test" + new Random().nextInt(999), new Image("/com/bayoumi/images/Kaaba.png"));
            popup.getContent().add(notification);
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            System.out.println("x " + (bounds.getWidth() - (notification.getWidth())));
            System.out.println("y " + (bounds.getHeight() - (notification.getHeight())));
//            popup.show(window, bounds.getWidth() - (notification.getWidth()), bounds.getHeight() - (notification.getHeight()));
//            popup.setX(bounds.getWidth() - (notification.getWidth()));
//            popup.setY(bounds.getHeight() - (notification.getHeight()));

            setD(Pos.BOTTOM_CENTER, notification.getWidth(), notification.getHeight());
            popup.show(window);
            System.out.println("Opening Notification ...");  // TODO : Delete println


          popup.setOnHiding(event -> {
                System.out.println("Closing Notification ..."); // TODO : Delete println
                if (MEDIA_PLAYER != null) {
                    MEDIA_PLAYER.stop();
                }
            });

// play sound
        } catch (Exception ex) {
                System.out.println("ex: " + ex);
                ex.printStackTrace();
                }
                }
* */
/*
 private static void setD(Pos p, double barWidth, double barHeight) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double screenHeight = bounds.getHeight();
        double screenWidth = bounds.getWidth();
        double startX = bounds.getMinX();
        double startY = bounds.getMinY();
        double anchorX = 0, anchorY = 0;
        // get anchorX
        switch (p) {
            case TOP_LEFT:
            case CENTER_LEFT:
            case BOTTOM_LEFT:
                anchorX = PADDING + startX;
                break;

            case TOP_CENTER:
            case CENTER:
            case BOTTOM_CENTER:
                anchorX = startX + (screenWidth / 2.0) - barWidth / 2.0 - PADDING / 2.0;
                break;

            default:
            case TOP_RIGHT:
            case CENTER_RIGHT:
            case BOTTOM_RIGHT:
                anchorX = startX + screenWidth - barWidth - PADDING;
                break;
        }

        // get anchorY
        switch (p) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                anchorY = PADDING + startY;
                break;

            case CENTER_LEFT:
            case CENTER:
            case CENTER_RIGHT:
                anchorY = startY + (screenHeight / 2.0) - barHeight / 2.0 - PADDING / 2.0;
                break;

            default:
            case BOTTOM_LEFT:
            case BOTTOM_CENTER:
            case BOTTOM_RIGHT:
                anchorY = startY + screenHeight - barHeight - PADDING;
                break;
        }

        popup.setAnchorX(anchorX);
        popup.setAnchorY(anchorY);
    }
* */

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