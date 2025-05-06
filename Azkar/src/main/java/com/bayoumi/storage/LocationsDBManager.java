package com.bayoumi.storage;

import com.bayoumi.util.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LocationsDBManager {

    private static LocationsDBManager databaseManager = null;  // static
    public Connection con = null;

    private LocationsDBManager() throws Exception {
        try {
            if (!Files.exists(Paths.get("jarFiles/db/locations.db"))) {
                // Throw error to download the DB again
                throw new Exception("LocationsDB does not exist");
            } else {
                if (!connectToDatabase()) {
                    throw new Exception("Cannot connect to LocationsDB");
                }
                if (!DatabaseHelper.checkIfTablesExist(con, "Countries")
                        || !DatabaseHelper.checkIfTablesExist(con, "cityd")) {
                    // close connection
                    con.close();
                    con = null;
                    // Delete created locations.db file
                    new File("jarFiles/db/locations.db").delete();
                    // Throw error to download the DB again
                    throw new Exception("LocationsDB does not exist");
                }
            }
        } catch (Exception ex) {
            // close connection
            if (con != null) {
                con.close();
                con = null;
            }
            throw ex;
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
            final String url = "jdbc:sqlite:jarFiles/db/locations.db";
            if (con != null && con.getMetaData().getURL().equals(url)) {
                return true;
            }
            if (con == null) {
                // .... Connect to SQlLite ....
                Class.forName("org.sqlite.JDBC");
                con = DriverManager.getConnection(url);
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
