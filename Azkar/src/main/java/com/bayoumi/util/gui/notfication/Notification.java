package com.bayoumi.util.gui.notfication;

import com.bayoumi.controllers.notification.NotificationsControlsFXController;
import com.bayoumi.models.settings.Preferences;
import com.bayoumi.models.settings.PreferencesType;
import com.bayoumi.util.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.util.Duration;

public class Notification {

    public static void create(NotificationContent content, double duration, Pos position, Runnable onClickAction, NotificationAudio audio) {
        try {
            FXMLLoader loader = new FXMLLoader(Notification.class.getResource("/com/bayoumi/views/notification/NotificationsControlsFX.fxml"));
            Parent notificationView = loader.load();
            ((NotificationsControlsFXController) loader.getController()).setData(content.getText(), content.getImage());
            audio.play();
            final Runnable closeCallback = () -> {
                Logger.debug("Closing Notification ..."); // TODO : Delete println
                if (audio.getMediaPlayer() != null) {
                    audio.stop();
                }
//                if (onClickAction != null) {
//                    onClickAction.run();
//                }
            };
            EventHandler<ActionEvent> onClickHandler = null;
            if (onClickAction != null) {
                onClickHandler = event -> {
                    Logger.debug("Clicked: " + onClickAction);
                    onClickAction.run();
                };
            }
            EventHandler<ActionEvent> finalOnClickHandler = onClickHandler;

            Platform.runLater(() -> {
                try {
                    Notifications.create()
                            .graphic(notificationView)
                            .hideAfter(Duration.seconds(duration))
                            .onAction(finalOnClickHandler)
                            .closeHandler(closeCallback)
                            .position(position)
                            .borderColor(Preferences.getInstance().get(PreferencesType.NOTIFICATION_BORDER_COLOR, "#E9C46A"))
                            .hideCloseButton()
                            .show(((NotificationsControlsFXController) loader.getController()).closeButton);
                } catch (Exception e) {
                    Logger.error(null, e, Notification.class.getName() + ".createControlsFX()");
                }
            });
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".createControlsFX()");
        }
    }

}
/*
 private static void playAlarmSound(int volume, String fileName) {
        if (!fileName.equals("بدون صوت")) {
            MEDIA_PLAYER = new MediaPlayer(new Media(new File("jarFiles/audio/" + fileName).toURI().toString()));
            MEDIA_PLAYER.setVolume(volume / 100.0);
            MEDIA_PLAYER.play();
        }
    }
* */

/*
* === OLD Notification ===
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
            playAlarmSound(Settings.getInstance().getAzkarSettings().getVolume());
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".create()");
        }
    }
* */