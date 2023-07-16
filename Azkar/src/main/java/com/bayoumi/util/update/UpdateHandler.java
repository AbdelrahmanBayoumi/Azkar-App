package com.bayoumi.util.update;

import com.bayoumi.models.UpdateInfo;
import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.gui.BuilderUI;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.launcher.Variables;
import com.install4j.api.update.ApplicationDisplayMode;
import com.install4j.api.update.UpdateChecker;
import com.install4j.api.update.UpdateDescriptor;
import com.install4j.api.update.UpdateDescriptorEntry;
import javafx.application.Platform;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class UpdateHandler {
    private static final String UPDATE_APPLICATION_ID = "64";
    private static UpdateHandler instance = null;
    private UpdateDescriptorEntry validUpdateDescriptorEntry;
    private boolean error = false;
    private UpdateInfo updateInfo = null;

    private UpdateHandler() {
    }

    public static UpdateHandler getInstance() {
        if (instance == null) {
            instance = new UpdateHandler();
        }
        return instance;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    /**
     * Check for new Update
     *
     * @return 0   : No updates found
     * 1   : New update found
     * -1  :  error => only installers and single bundle archives on macOS are supported for background updates
     */
    public synchronized int checkUpdate() {
        CompletableFuture<UpdateDescriptorEntry> future = new CompletableFuture<>();
        try {
            getUpdateDescriptor(future);
        } catch (Exception ex) {
            Logger.info(getClass().getName() + ".checkUpdate()" + "Can't update current application: " + ex.getMessage());
        }
        UpdateDescriptorEntry updateDescriptorEntry = null;
        try {
            updateDescriptorEntry = future.get(); // wait for future to be assigned a result and retrieve it
        } catch (InterruptedException | ExecutionException ex) {
            Logger.info(getClass().getName() + ".checkUpdate(): " + "While waiting for file browser: " + ex.getMessage());
        }
        if (error) {
            Logger.info(getClass().getName() + ".checkUpdate(): " + "Update check Server Error");
            return -1;
        }
        if (updateDescriptorEntry == null) {
            Logger.info(getClass().getName() + ".checkUpdate()" + "No updates found (OK).");
            return 0;
        }
        // only installers and single bundle archives on macOS are supported for background updates
        if (updateDescriptorEntry.isArchive() && !updateDescriptorEntry.isSingleBundle()) {
            Logger.info(getClass().getName() + ".checkUpdate(): " + "Only installers and single bundle archives on macOS are supported for background update (can't update):" + updateDescriptorEntry.getURL());
            return -1;
        }
        validUpdateDescriptorEntry = updateDescriptorEntry;
        try {
            updateInfo = new UpdateInfo(updateDescriptorEntry.getNewVersion(),
                    Variables.getCompilerVariable("sys.updatesUrl"),
                    String.valueOf(updateDescriptorEntry.getURL()),
                    updateDescriptorEntry.getFileSizeVerbose(), updateDescriptorEntry.getFileName(),
                    updateDescriptorEntry.getComment());
        } catch (IOException ex) {
            Logger.error("While getting 'sys.updatesUrl' ", ex, getClass().getName() + ".checkUpdate()");
            return -1;
        }

        Logger.info(getClass().getName() + ".checkUpdate(): " + "A new version " +
                updateDescriptorEntry.getNewVersion() + " is available for update: " +
                updateDescriptorEntry.getFileName()
                + ". Url=" + updateDescriptorEntry.getURL());
        return 1;
    }

    public void showInstallPrompt() {
        Platform.runLater(() -> {
            String version;
            try {
                version = Variables.getCompilerVariable("sys.version");
            } catch (Exception ex) {
                Logger.info(getClass().getName() + ".showInstallPrompt(): " + "error => cannot get sys.version");
                version = Constants.VERSION;
            }
            if (BuilderUI.showUpdateDetails(UpdateHandler.getInstance().getUpdateInfo(), version)) {
                new Thread(() -> UpdateHandler.getInstance().install()).start();
            }
        });
    }


    /**
     * install the update launcher
     * NOTE: SHOULD NOT BE CALLED IN JAVAFX THREAD
     */
    public void install() {
        if (validUpdateDescriptorEntry == null) {
            Logger.info("Nothing to install. No valid update available.");
            return;
        }
        try {
            Logger.info(UpdateHandler.class.getName() + ".install(): " + "New update found");
            Logger.info(UpdateHandler.class.getName() + ".install(): " + "Current Version: " + Constants.VERSION);
            Logger.info(UpdateHandler.class.getName() + ".install(): " + "New Version: " + UpdateHandler.getInstance().getUpdateInfo());
            Logger.info(UpdateHandler.class.getName() + "Launching updater on local desktop.");

            ApplicationLauncher.launchApplication(UPDATE_APPLICATION_ID, null, false, new ApplicationLauncher.Callback() {
                        public void exited(int exitValue) {
                            Logger.info("Launcher exited.");
                        }

                        public void prepareShutdown() {
                            Logger.info("Shutdown in progress.");
                        }
                    }
            );
        } catch (IOException ex) {
            Logger.info("Error while updating: " + ex.getMessage());
        }
    }

    private void getUpdateDescriptor(CompletableFuture<UpdateDescriptorEntry> future) {
        error = false;
        // The compiler variable sys.updatesUrl holds the URL where the updates.xml file is hosted.
        // That URL is defined on the "Installer->Auto Update Options" step.
        String updateUrl;
        try {
            updateUrl = Variables.getCompilerVariable("sys.updatesUrl");
        } catch (IOException ex) {
            Logger.info(getClass().getName() + ".getUpdateDescriptor(): " + "Can't check update url: " + ex.getMessage());
            future.complete(null);
            error = true;
            return;
        }
        Logger.info(getClass().getName() + ".getUpdateDescriptor(): " + "Checking update: " + updateUrl);
        UpdateDescriptor updateDescriptor;
        try {
            updateDescriptor = UpdateChecker.getUpdateDescriptor(updateUrl, ApplicationDisplayMode.UNATTENDED);
        } catch (Exception ex) {
            Logger.info(getClass().getName() + ".getUpdateDescriptor(): " + "Can't get updates: " + ex.getMessage());
            future.complete(null);
            error = true;
            return;
        }
        // If getPossibleUpdateEntry returns a non-null value, the version number in the updates.xml file
        // is greater than the version number of the local installation.
        future.complete(updateDescriptor.getPossibleUpdateEntry());
    }
}