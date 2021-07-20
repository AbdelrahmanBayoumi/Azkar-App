package com.bayoumi.util.services.azkar;

import com.bayoumi.controllers.home.HomeController;
import com.bayoumi.models.AbsoluteZekr;
import com.bayoumi.models.NotificationSettings;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.services.EditablePeriodTimerTask;
import javafx.application.Platform;

import java.util.Random;

public class AzkarService {
    public static EditablePeriodTimerTask absoluteAzkarTask;

    public static void init(HomeController homeController, NotificationSettings notificationSettings) {
        homeController.setFrequencyLabel();
        absoluteAzkarTask = null;
        absoluteAzkarTask = new EditablePeriodTimerTask(()
                -> {
            if (AbsoluteZekr.absoluteZekrObservableList.isEmpty()) {
                return;
            }
            Platform.runLater(()
                    -> Notification.createControlsFX(
                    AbsoluteZekr.absoluteZekrObservableList.get(
                            new Random().nextInt(AbsoluteZekr.absoluteZekrObservableList.size())).getText(),
                    null, null, 10, notificationSettings));

        },
                homeController::getPeriod);
        absoluteAzkarTask.updateTimer();
    }
}
