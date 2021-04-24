package com.bayoumi.util.db;

import com.bayoumi.util.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LocationsDBManager {

    private static LocationsDBManager databaseManager = null;  // static
    public PreparedStatement stat = null;
    public Connection con = null;

    private LocationsDBManager() throws Exception {
        if (!connectToDatabase()) {
            throw new Exception("Cannot init LocationsDBManager");
        }
    }

    public static LocationsDBManager getInstance() throws Exception {
        if (databaseManager == null) {
            databaseManager = new LocationsDBManager();
        }
        return databaseManager;
    }

    private boolean connectToDatabase() {
        try {
            if (con == null) {
                // .... Connect to SQlLite ....
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection("jdbc:sqlite:jarFiles/db/locations.db");
                con.prepareStatement("PRAGMA foreign_keys=ON").execute();
                return true;
            }
        } catch (ClassNotFoundException | SQLException ex) {
            con = null;
            Logger.error(ex.getLocalizedMessage(), ex, getClass().getName() + ".connectToDatabase()");
        }
        return false;
    }

}
/*
TODO
SELECT * from Countries;
-- User Chooses Egypt which code is EG
-- THEN
SELECT * from cityd WHERE country='EG';
* */