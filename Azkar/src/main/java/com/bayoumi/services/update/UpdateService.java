package com.bayoumi.services.update;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;

import java.util.Timer;

public class UpdateService {
    public static void checkForUpdate() {
        // TODO: move this check to a service
        final Timer timer = new Timer();
        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Logger.debug("[UpdateService] checkForUpdate()");
                        if (UpdateHandler.getInstance().checkUpdate() == 1 & Settings.getInstance().getAutomaticCheckForUpdates()) {
                            UpdateHandler.getInstance().showInstallPrompt();
                        }
                        // close the thread
                        timer.cancel();
                    }
                },
                390000 // 6.5min => to ensure that update will open when no notification is shown
        );
        Logger.debug("[UpdateService] checkForUpdate() scheduled");
    }
}
