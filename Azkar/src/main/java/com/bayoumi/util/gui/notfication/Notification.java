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
        audio.play();
        for (javafx.stage.Screen screen : javafx.stage.Screen.getScreens()) {
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(Notification.class.getResource(com.bayoumi.util.gui.load.Locations.NotificationContent.getName()));
                    javafx.scene.Parent notificationView = loader.load();
                    ((NotificationsControlsFXController) loader.getController()).setData(content.getText(), content.getImage());

                    final Runnable closeCallback = () -> {
                        Logger.debug("Closing Notification ...");
                        if (audio.isPlaying()) {
                            audio.stop();
                        }
                    };
                    javafx.event.EventHandler<javafx.event.ActionEvent> onClickHandler = null;
                    if (onClickAction != null) {
                        onClickHandler = event -> {
                            Logger.debug("[Notification] onClickAction");
                            onClickAction.run();
                        };
                    }

                    Notifications.create()
                            .owner(screen)
                            .graphic(notificationView)
                            .hideAfter(Duration.seconds(duration))
                            .onAction(onClickHandler)
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
        }
    }

}
