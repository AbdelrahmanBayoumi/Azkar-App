package com.bayoumi.util.services.azkar;

import com.bayoumi.controllers.home.periods.AzkarPeriodsController;
import com.bayoumi.models.AbsoluteZekr;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.gui.notfication.NotificationAudio;
import com.bayoumi.util.gui.notfication.NotificationContent;
import com.bayoumi.util.services.EditablePeriodTimerTask;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Random;

public class AzkarService {

    private static EditablePeriodTimerTask absoluteAzkarTask;
    public static Stage FAKE_STAGE;

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

    public static void init(AzkarPeriodsController azkarPeriodsController) {
        if (FAKE_STAGE == null) {
           Platform.runLater(() -> {
               FAKE_STAGE = new Stage(StageStyle.UTILITY);
               FAKE_STAGE.setOpacity(0);
               FAKE_STAGE.show();
               FAKE_STAGE.toBack();
           });
        }
        azkarPeriodsController.setFrequencyLabel();
        absoluteAzkarTask = null;
        absoluteAzkarTask = new EditablePeriodTimerTask(()
                -> {
            if (AbsoluteZekr.absoluteZekrObservableList.isEmpty()) {
                return;
            }
            Platform.runLater(()
                    -> Notification.create(new NotificationContent(AbsoluteZekr.absoluteZekrObservableList.get(
                            new Random().nextInt(AbsoluteZekr.absoluteZekrObservableList.size())).getText(),
                            null),
                    30,
                    Settings.getInstance().getNotificationSettings().getPosition(),
                    null,
                    new NotificationAudio(Settings.getInstance().getAzkarSettings().getAudioName(), Settings.getInstance().getAzkarSettings().getVolume())));
        },
                azkarPeriodsController::getPeriod);
        absoluteAzkarTask.updateTimer();
    }
}
