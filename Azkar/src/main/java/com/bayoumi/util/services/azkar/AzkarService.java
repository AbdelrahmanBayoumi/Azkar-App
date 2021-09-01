package com.bayoumi.util.services.azkar;

import com.bayoumi.controllers.home.periods.AzkarPeriodsController;
import com.bayoumi.models.AbsoluteZekr;
import com.bayoumi.models.settings.NotificationSettings;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.services.EditablePeriodTimerTask;
import javafx.application.Platform;

import java.util.Random;

public class AzkarService {

    private static EditablePeriodTimerTask absoluteAzkarTask;

    public static void stopService() {
        if (AzkarService.absoluteAzkarTask != null) {
            AzkarService.absoluteAzkarTask.stopTask();
        }
    }

    public static void updateTimer() {
        if (AzkarService.absoluteAzkarTask != null) {
            AzkarService.absoluteAzkarTask.updateTimer();
        }
    }

    public static void init(AzkarPeriodsController azkarPeriodsController, NotificationSettings notificationSettings) {
        azkarPeriodsController.setFrequencyLabel();
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
                azkarPeriodsController::getPeriod);
        absoluteAzkarTask.updateTimer();
    }
}
