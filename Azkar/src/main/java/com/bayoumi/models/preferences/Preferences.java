package com.bayoumi.models.preferences;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.update.UpdateHandler;

import java.sql.ResultSet;
import java.util.Map;
import java.util.Timer;

public class Preferences {
    private static Preferences instance;

    public static Preferences getInstance() {
        return instance;
    }

    public static void init() throws Exception {
        if (instance == null) {
            instance = new Preferences();
        }
    }

    private Preferences() throws Exception {
        // 1. create table if not exists
        if (DatabaseManager.getInstance().con == null) {
            throw new Exception("Database not connected");
        } else {
            DatabaseManager.getInstance().con.prepareStatement("CREATE TABLE IF NOT EXISTS preferences ( key TEXT, value TEXT, PRIMARY KEY(key));").execute();
        }

        // 2. check for updates if enabled
        checkForUpdate();
    }

    private void checkForUpdate() {
        Logger.debug("Start automaticCheckForUpdates");
        final Timer timer = new Timer();
        timer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Logger.debug("automaticCheckForUpdates: run()");
                        if (UpdateHandler.getInstance().checkUpdate() == 1 & Settings.getInstance().getAutomaticCheckForUpdates()) {
                            UpdateHandler.getInstance().showInstallPrompt();
                        }
                        // close the thread
                        timer.cancel();
                    }
                },
                390000 // 6.5min => to ensure that update will open when no notification is shown
        );
    }


    private void save(PreferencesType key, String value) {
        try {
            DatabaseManager.getInstance().con.prepareStatement("INSERT INTO preferences (key,value) VALUES('" + key + "','" + value + "')").execute();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".save()");
        }
    }

    private void update(PreferencesType key, String value) {
        try {
            DatabaseManager.getInstance().stat = DatabaseManager.getInstance().con.prepareStatement("UPDATE preferences set value=? WHERE key=?");
            DatabaseManager.getInstance().stat.setString(1, value);
            DatabaseManager.getInstance().stat.setString(2, key.toString());
            DatabaseManager.getInstance().stat.executeUpdate();
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".update()");
        }
    }

    private boolean isExist(PreferencesType key) {
        try {
            ResultSet result = DatabaseManager.getInstance().con.prepareStatement("SELECT count(*) as exist FROM preferences WHERE key='" + key + "'").executeQuery();
            if (result.next()) {
                return result.getString("exist").equals("1");
            }
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".isExist()");
        }
        return false;
    }

    public void set(PreferencesType key, String value) {
        if (isExist(key)) {
            Logger.debug("[Preferences] Update >> " + key + ": [" + value + "]");
            update(key, value);
        } else {
            Logger.debug("[Preferences] Insert >> " + key + ": [" + value + "]");
            save(key, value);
        }
//        notifyObservers(key, value);
    }

    public String get(PreferencesType key, String defaultValue) {
        try {
            // if exist => return found value
            ResultSet result = DatabaseManager.getInstance().con.prepareStatement("SELECT value FROM preferences WHERE key='" + key + "'").executeQuery();
            if (result.next()) {
                if (result.getString("value") != null) {
                    return result.getString("value");
                }
                return defaultValue;
            }
            // not exist => insert & return default value
            save(key, defaultValue);
        } catch (Exception e) {
            Logger.error(null, e, getClass().getName() + ".get()");
        }
        return defaultValue;
    }

    public String get(PreferencesType key) {
        return get(key, key.getDefaultValue());
    }

    public boolean getBoolean(PreferencesType key) {
        return get(key, key.getDefaultValue()).equalsIgnoreCase("true");
    }

    public int getInt(PreferencesType key) {
        return Integer.parseInt(get(key, key.getDefaultValue()));
    }

    public double getDouble(PreferencesType key) {
        return Double.parseDouble(get(key, key.getDefaultValue()));
    }


    public Map<String, String> getAll() {
        final Map<String, String> preferences = new java.util.HashMap<>();
        try {
            final ResultSet result = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM preferences;").executeQuery();
            while (result.next()) {
                preferences.put("preferences." + result.getString("key"), result.getString("value"));
            }
        } catch (Exception ignored) {
        }
        return preferences;
    }
}
