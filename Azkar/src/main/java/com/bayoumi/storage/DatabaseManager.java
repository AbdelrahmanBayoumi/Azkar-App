package com.bayoumi.storage;


import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import org.flywaydb.core.Flyway;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private static DatabaseManager databaseManager = null;
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


    /**
     * Retrieves the ID from the database. If the ID is null or empty, a new one is generated and saved to the database.
     *
     * @return the ID as a string
     */
    public String getID() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT ID FROM program_characteristics").executeQuery();
            String id;
            if (res.next()) {
                id = res.getString("ID");
                if (id != null && !id.isEmpty()) return id;
            }
            // if ID is null or empty, generate new one
            id = UUID.randomUUID().toString();
            // save generated ID to DB
            DatabaseManager.getInstance().setID(id);
            return id;
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
