package com.bayoumi.models.settings;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.gui.BuilderUI;
import com.bayoumi.util.update.UpdateHandler;
import com.install4j.api.launcher.Variables;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Callback;

import java.sql.ResultSet;
import java.util.Objects;
import java.util.Observable;
import java.util.Timer;

public class OtherSettings extends Observable {

    private String language;
    private boolean enableDarkMode;
    private boolean enable24Format;
    private boolean minimized;
    private int hijriOffset;
    private boolean automaticCheckForUpdates;

    protected OtherSettings() {
        loadSettings();
    }

    public static boolean getIsMinimizedDB() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT minimized FROM other_settings").executeQuery();
            if (res.next()) {
                return res.getInt(1) == 1;
            }
        } catch (Exception ex) {
            Logger.error(null, ex, OtherSettings.class.getName() + ".GetIsMinimizedDB()");
        }
        return false;
    }

    public static void checkForUpdate(Callback<Void, Void> callback, boolean showAlerts) {
        new Thread(() -> {
            try {
                switch (UpdateHandler.getInstance().checkUpdate()) {
                    case 0:
                        Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "No Update Found");
                        if (showAlerts) {
                            Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.INFORMATION, "لا يوجد تحديثات جديدة", true));
                        }
                        break;
                    case 1:
                        Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "New update found");
                        Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "Current Version: " + DatabaseManager.getInstance().getVersion());
                        Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "New Version: " + UpdateHandler.getInstance().getUpdateInfo());
                        Platform.runLater(() -> {
                            String version;
                            try {
                                version = Variables.getCompilerVariable("sys.version");
                            } catch (Exception ex) {
                                Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "error => cannot get sys.version");
                                version = DatabaseManager.getInstance().getVersion();
                            }
                            if (BuilderUI.showUpdateDetails(UpdateHandler.getInstance().getUpdateInfo(), version)) {
                                new Thread(() -> UpdateHandler.getInstance().install()).start();
                            }
                        });
                        break;
                    case -1:
                        if (showAlerts) {
                            Platform.runLater(() -> BuilderUI.showOkAlert(Alert.AlertType.ERROR, "توجد مشكلة في البحث عن تحديثات جديدة", true));
                        }
                        Logger.info(OtherSettings.class.getName() + ".checkForUpdate(): " + "error => only installers and single bundle archives on macOS are supported for background updates");
                        break;
                }
                if (callback != null) {
                    callback.call(null);
                }
            } catch (Exception ex) {
                Logger.error(null, ex, OtherSettings.class.getName() + ".checkForUpdate()");
            }
        }).start();
    }

    private void loadSettings() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM other_settings").executeQuery();
            if (res.next()) {
                language = res.getString(1);
                enableDarkMode = res.getInt(2) == 1;
                enable24Format = res.getInt(3) == 1;
                hijriOffset = res.getInt(4);
                minimized = res.getInt(5) == 1;
                automaticCheckForUpdates = res.getInt(6) == 1;
            }
            if (automaticCheckForUpdates) {
                System.out.println("Start automaticCheckForUpdates");
                Timer t = new java.util.Timer();
                t.schedule(
                        new java.util.TimerTask() {
                            @Override
                            public void run() {
                                System.out.println("automaticCheckForUpdates: run()");
                                OtherSettings.checkForUpdate(null,false);
                                // close the thread
                                t.cancel();
                            }
                        },
                        300000 // 5min
                );
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    /**
     * update OtherSettings Object in Database
     */
    public void save() {
        try {
            if (this.equals(new OtherSettings())) {
                // if current obj is equal the one stored in DB then => do nothing (don't save)
                return;
            }
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE other_settings set language = ?, enable_darkmode = ?, enable24 = ?, hijri_offset = ?, minimized = ?, automatic_check_for_updates = ?");
            databaseManager.stat.setString(1, this.language);
            databaseManager.stat.setInt(2, this.enableDarkMode ? 1 : 0);
            databaseManager.stat.setInt(3, this.enable24Format ? 1 : 0);
            databaseManager.stat.setInt(4, this.hijriOffset);
            databaseManager.stat.setInt(5, this.minimized ? 1 : 0);
            databaseManager.stat.setInt(6, this.automaticCheckForUpdates ? 1 : 0);
            databaseManager.stat.executeUpdate();
            // fetch new updated data from database
            loadSettings();
            // fire change event to all listeners to update their values
            setChanged();
            notifyObservers();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OtherSettings that = (OtherSettings) o;
        return enableDarkMode == that.enableDarkMode &&
                enable24Format == that.enable24Format &&
                minimized == that.minimized &&
                automaticCheckForUpdates == that.automaticCheckForUpdates &&
                hijriOffset == that.hijriOffset &&
                language.equals(that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, enableDarkMode, enable24Format, minimized, hijriOffset);
    }

    // =========== SETTERS & GETTERS ===========
    @Override
    public String toString() {
        return "OtherSettings{" +
                "language='" + language + '\'' +
                ", enableDarkMode=" + enableDarkMode +
                ", enable24Format=" + enable24Format +
                ", minimized=" + minimized +
                ", hijriOffset=" + hijriOffset +
                ", automaticCheckForUpdates=" + automaticCheckForUpdates +
                '}';
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageLocal() {
        if (language.equals("عربي - Arabic")) {
            return "ar";
        }
        return "en";
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public boolean isEnableDarkMode() {
        return enableDarkMode;
    }

    public void setEnableDarkMode(boolean enableDarkMode) {
        this.enableDarkMode = enableDarkMode;
    }

    public boolean isEnable24Format() {
        return enable24Format;
    }

    public void setEnable24Format(boolean enable24Format) {
        this.enable24Format = enable24Format;
    }

    public int getHijriOffset() {
        return hijriOffset;
    }

    public void setHijriOffset(int hijriOffset) {
        this.hijriOffset = hijriOffset;
    }

    public boolean isAutomaticCheckForUpdates() {
        return automaticCheckForUpdates;
    }

    public void setAutomaticCheckForUpdates(boolean automaticCheckForUpdates) {
        this.automaticCheckForUpdates = automaticCheckForUpdates;
    }
}
