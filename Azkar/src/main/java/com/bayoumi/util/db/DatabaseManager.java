package com.bayoumi.util.db;


import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import org.flywaydb.core.Flyway;

import java.sql.*;

public class DatabaseManager {

    private static DatabaseManager databaseManager = null;  // static
    public PreparedStatement stat = null;
    public Connection con = null;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        if (databaseManager == null) {
            databaseManager = new DatabaseManager();
        }
        return databaseManager;
    }

    public boolean init() {
        try {
            Flyway.configure()
                    .dataSource("jdbc:sqlite:" + Constants.assetsPath + "/db/data.db", "", "")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
            if (!connectToDatabase()) {
                throw new Exception("Cannot init DatabaseManager");
            }
            return true;
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".init()");
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
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".getCount()");
        }
        return 0;
    }

    private boolean connectToDatabase() {
        try {
            if (con == null) {
                // .... Connect to SQlLite ....
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:" + Constants.assetsPath + "/db/data.db");
                con.prepareStatement("PRAGMA foreign_keys=ON").execute();
                return true;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            con = null;
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".connectToDatabase()");
        }
        return false;
    }

    public String getID() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT ID FROM program_characteristics").executeQuery();
            if (res.next()) {
                return res.getString("ID");
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".getID()");
        }
        return "";
    }

    public void setID(String ID) {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE program_characteristics set ID = ?");
            databaseManager.stat.setString(1, ID);
            databaseManager.stat.executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, getClass().getName() + ".setID(ID: " + ID + ")");
        }
    }
}
