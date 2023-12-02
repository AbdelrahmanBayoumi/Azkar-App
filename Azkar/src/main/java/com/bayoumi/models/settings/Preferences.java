package com.bayoumi.models.settings;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;

import java.sql.ResultSet;

public class Preferences {

    private static Preferences instance;

    public static Preferences getInstance() throws Exception {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }

    private Preferences() throws Exception {
        if (DatabaseManager.getInstance().con == null) {
            throw new Exception("Database not connected");
        } else {
            DatabaseManager.getInstance().con.prepareStatement("CREATE TABLE IF NOT EXISTS preferences ( key TEXT, value TEXT, PRIMARY KEY(key));").execute();
        }
    }

    private void save(PreferencesType key, String value) {
        try {
//            System.out.println("INSERT INTO preferences (key,value) VALUES('" + key + "','" + value + "')");
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
//        System.out.println("set >> " + key + ": [" + value + "]");
        if (isExist(key)) {
//            System.out.println("Key Exists..");
            update(key, value);
        } else {
//            System.out.println("New Key..");
            save(key, value);
        }
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

    public boolean getBoolean(PreferencesType key, String defaultValue) {
        return get(key, defaultValue).equalsIgnoreCase("true");
    }
}
