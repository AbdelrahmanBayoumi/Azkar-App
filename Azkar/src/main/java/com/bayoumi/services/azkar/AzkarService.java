package com.bayoumi.services.azkar;

import com.bayoumi.controllers.home.periods.AzkarPeriodsController;
import com.bayoumi.models.azkar.AbsoluteZekr;
import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.gui.notfication.Notification;
import com.bayoumi.util.gui.notfication.NotificationAudio;
import com.bayoumi.util.gui.notfication.NotificationContent;
import com.bayoumi.services.EditablePeriodTimerTask;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AzkarService {

    private static EditablePeriodTimerTask absoluteAzkarTask;
    public static Stage FAKE_STAGE;
    private static int currentZekrIndex = 0;

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

            AbsoluteZekr currentZekr;
            if (currentZekrIndex >= 0 && currentZekrIndex < AbsoluteZekr.absoluteZekrObservableList.size()) {
                currentZekr = AbsoluteZekr.absoluteZekrObservableList.get(currentZekrIndex);
            } else {
                currentZekr = AbsoluteZekr.absoluteZekrObservableList.get(0); // Fallback to the first item
            }

            Platform.runLater(()
                    -> Notification.create(new NotificationContent(currentZekr.getText(), null),
                    Settings.getInstance().getAzkarSettings().getAzkarDuration(),
                    Settings.getInstance().getNotificationSettings().getPosition(),
                    null,
                    new NotificationAudio(Settings.getInstance().getAzkarSettings().getAudioName(), Settings.getInstance().getAzkarSettings().getVolume())));
            currentZekrIndex = (currentZekrIndex + 1) % AbsoluteZekr.absoluteZekrObservableList.size();
        },
                azkarPeriodsController::getPeriod);
        absoluteAzkarTask.updateTimer();
    }
}
