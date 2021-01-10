package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseHandler;

import java.sql.ResultSet;

public class OtherSettings {
    private String language;
    private boolean enableDarkMode;
    private boolean enable24Format;
    public static boolean isUpdated= false;

    public OtherSettings() {
        loadSettings();
    }

    private void loadSettings() {
        try {
            ResultSet res = DatabaseHandler.getInstance().con.prepareStatement("SELECT * FROM other_settings").executeQuery();
            if (res.next()) {
                language = res.getString(1);
                enableDarkMode = res.getInt(2) == 1;
                enable24Format = res.getInt(3) == 1;
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public void save() {
        try {
            DatabaseHandler databaseHandler = DatabaseHandler.getInstance();
            databaseHandler.stat = databaseHandler.con.prepareStatement("UPDATE other_settings set language = ?, enable_darkmode = ?, enable24 = ?");
            databaseHandler.stat.setString(1, this.language);
            databaseHandler.stat.setInt(2, this.enableDarkMode ? 1 : 0);
            databaseHandler.stat.setInt(3, this.enable24Format ? 1 : 0);
            databaseHandler.stat.executeUpdate();
            isUpdated = true;
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    @Override
    public String toString() {
        return "OtherSettings{" +
                "language='" + language + '\'' +
                ", enableDarkMode=" + enableDarkMode +
                ", enable24Format=" + enable24Format +
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
}
