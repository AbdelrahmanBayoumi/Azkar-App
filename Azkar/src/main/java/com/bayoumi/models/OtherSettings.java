package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;

import java.sql.ResultSet;

public class OtherSettings {
    public static boolean isUpdated = false;
    private String language;
    private boolean enableDarkMode;
    private boolean enable24Format;
    private boolean minimized;
    private int hijriOffset;

    public OtherSettings() {
        loadSettings();
    }

    public static boolean getIsMinimizedDB(){
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
    private void loadSettings() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM other_settings").executeQuery();
            if (res.next()) {
                language = res.getString(1);
                enableDarkMode = res.getInt(2) == 1;
                enable24Format = res.getInt(3) == 1;
                hijriOffset = res.getInt(4);
                minimized = res.getInt(5) == 1;
            }
            System.out.println(this);
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public void save() {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE other_settings set language = ?, enable_darkmode = ?, enable24 = ?, hijri_offset = ?, minimized = ?");
            databaseManager.stat.setString(1, this.language);
            databaseManager.stat.setInt(2, this.enableDarkMode ? 1 : 0);
            databaseManager.stat.setInt(3, this.enable24Format ? 1 : 0);
            databaseManager.stat.setInt(4, this.hijriOffset);
            databaseManager.stat.setInt(5, this.minimized ? 1 : 0);
            databaseManager.stat.executeUpdate();
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


}
