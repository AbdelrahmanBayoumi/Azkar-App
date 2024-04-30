package com.bayoumi.util.gui.notfication;

import com.bayoumi.controllers.notification.NotificationsControlsFXController;
import com.bayoumi.models.settings.Settings;
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
                Logger.debug("Closing Notification ...");
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
                            .borderColor(Settings.getInstance().getNotificationSettings().getColor())
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