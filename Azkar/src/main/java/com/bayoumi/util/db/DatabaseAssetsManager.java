package com.bayoumi.util.db;


import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;

import java.sql.*;

public class DatabaseAssetsManager {

    private static DatabaseAssetsManager databaseAssetsManager = null;  // static
    public PreparedStatement stat = null;
    public Connection con = null;

    private DatabaseAssetsManager() {
    }

    public static DatabaseAssetsManager getInstance() {
        if (databaseAssetsManager == null) {
            databaseAssetsManager = new DatabaseAssetsManager();
        }
        return databaseAssetsManager;
    }

    public boolean init() {
        try {
            if (!connectToDatabase()) {
                throw new Exception("Cannot init DatabaseManager");
            }
            // create tables azkar_settings
            if (!createTable("CREATE TABLE IF NOT EXISTS azkar_settings ( " +
                    "morning_reminder TEXT NOT NULL DEFAULT 'لا تذكير', " +
                    "night_reminder TEXT NOT NULL DEFAULT 'لا تذكير'," +
                    "audio_name TEXT NOT NULL DEFAULT 'بدون صوت'," +
                    "high_period INTEGER NOT NULL DEFAULT 10, " +
                    "mid_period INTEGER NOT NULL DEFAULT 15, " +
                    "low_period INTEGER NOT NULL DEFAULT 30, " +
                    "rear_period INTEGER NOT NULL DEFAULT 45," +
                    "stop_azkar INTEGER NOT NULL DEFAULT 0," +
                    "selected_period TEXT DEFAULT 'عالي');") ||
                    !insertDefault("azkar_settings")) {
                throw new Exception("ERROR in CREATE azkar_settings TABLE");
            }
            if (!createTable("CREATE TABLE IF NOT EXISTS other_settings (" +
                    "language TEXT NOT NULL DEFAULT 'عربي - Arabic', " +
                    "enable_darkmode INTEGER NOT NULL DEFAULT 0" +
                    ", enable24 INTEGER NOT NULL DEFAULT 0" +
                    ", hijri_offset INTEGER NOT NULL DEFAULT 0" +
                    ", minimized INTEGER NOT NULL DEFAULT 0 ); ") ||
                    !insertDefault("other_settings")) {
                throw new Exception("ERROR in CREATE other_settings TABLE");
            }
            if (!createTable("CREATE TABLE IF NOT EXISTS prayertimes_settings (" +
                    "country TEXT NOT NULL DEFAULT 'Egypt', " +
                    "city TEXT NOT NULL DEFAULT 'Alexandria', " +
                    "method INTEGER NOT NULL DEFAULT 5, " +
                    "asr_juristic INTEGER NOT NULL DEFAULT 0, " +
                    "summer_timing INTEGER NOT NULL DEFAULT 0 ); " +
                    "INSERT INTO prayertimes_settings DEFAULT VALUES;") ||
                    !insertDefault("prayertimes_settings")) {
                throw new Exception("ERROR in CREATE prayertimes_settings TABLE");
            }
            if (!createTable("CREATE TABLE IF NOT EXISTS prayertimes ( " +
                    "date TEXT NOT NULL UNIQUE," +
                    "fajr TEXT NOT NULL ," +
                    "sunrise TEXT NOT NULL ," +
                    "dhuhr TEXT NOT NULL ," +
                    "asr TEXT NOT NULL ," +
                    "maghrib TEXT NOT NULL ," +
                    "isha TEXT NOT NULL );")) {
                throw new Exception("ERROR in CREATE prayertimes TABLE");
            }
            if (!createTable("CREATE TABLE IF NOT EXISTS program_characteristics ( " +
                    "version TEXT NOT NULL DEFAULT 0 );") ||
                    !insertDefault("program_characteristics")) {
                throw new Exception("ERROR in CREATE program_characteristics TABLE");
            }
            return true;
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
            ex.printStackTrace();
        }
        return false;
    }

    private boolean insertDefault(String tableName) {
        try {
            if (getCount(tableName) >= 1) {
                return true;
            }
            con.prepareStatement("INSERT INTO " + tableName + " DEFAULT VALUES;").execute();
            return true;
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".insertDefault()");
            return false;
        }
    }

    private boolean createTable(String sqlString) {
        try {
            con.prepareStatement(sqlString).execute();
            return true;
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".createTable()");
            return false;
        }
    }

    private int getCount(String tableName) {
        try {
            ResultSet res = con.prepareStatement("SELECT COUNT(*) FROM " + tableName).executeQuery();
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".getCount()");
        }
        return 0;
    }

    private boolean connectToDatabase() {
        try {
            if (con == null) {
                // .... Connect to SQlLite ....
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:" + Constants.assetsPath + "/db/assets.db");
                con.prepareStatement("PRAGMA foreign_keys=ON").execute();
                return true;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            con = null;
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".connectToDatabase()");
        }
        return false;
    }

    public String getVersion() {
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM program_characteristics").executeQuery();
            if (res.next()) {
                return res.getString("version");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".getVersion()");
        }
        return "0.0.0";
    }

    public void setVersion(String version) {
        try {
            DatabaseAssetsManager databaseAssetsManager = DatabaseAssetsManager.getInstance();
            databaseAssetsManager.stat = databaseAssetsManager.con.prepareStatement("UPDATE program_characteristics set version = ?");
            databaseAssetsManager.stat.setString(1, version);
            databaseAssetsManager.stat.executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, getClass().getName() + ".setVersion(version: " + version + ")");
        }
    }
}
