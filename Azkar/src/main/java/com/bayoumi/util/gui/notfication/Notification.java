package com.bayoumi.util.gui.notfication;

import com.bayoumi.controllers.notification.NotificationsControlsFXController;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.load.Locations;
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
            FXMLLoader loader = new FXMLLoader(Notification.class.getResource(Locations.NotificationContent.getName()));
            Parent notificationView = loader.load();
            ((NotificationsControlsFXController) loader.getController()).setData(content.getText(), content.getImage());
            audio.play();
            final Runnable closeCallback = () -> {
                Logger.debug("Closing Notification ...");
                if (audio.getMediaPlayer() != null) {
                    audio.stop();
                }
            };
            EventHandler<ActionEvent> onClickHandler = null;
            if (onClickAction != null) {
                onClickHandler = event -> {
                    Logger.debug("[Notification] onClickAction");
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
                            .backgroundColor(Settings.getInstance().getNotificationSettings().getBackgroundColor())
                            .borderColor(Settings.getInstance().getNotificationSettings().getBorderColor())
                            .hideCloseButton()
                            .show(((NotificationsControlsFXController) loader.getController()).closeButton);
                } catch (Exception e) {
                    Logger.error(null, e, Notification.class.getName() + ".create()");
                }
            });
        } catch (Exception ex) {
            Logger.error(null, ex, Notification.class.getName() + ".create()");
        }
    }

}
