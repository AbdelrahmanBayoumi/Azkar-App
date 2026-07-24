package com.bayoumi.storage;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.AppPathManager;
import com.bayoumi.util.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class LocationsDBManager {

    private static LocationsDBManager databaseManager = null;  // static
    public Connection con = null;

    private LocationsDBManager() throws Exception {
        try {
            copyDatabaseToAssetsPath();
        } catch (IOException e) {
            Logger.error(e.getLocalizedMessage(), e, getClass().getName() + ".copyDatabaseToAssetsPath()");
        }
        try {
            if (!Files.exists(Paths.get(Constants.assetsPath + "/db/locations.db"))) {
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
                    new File(Constants.assetsPath + "/db/locations.db").delete();
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
            final String url = "jdbc:sqlite:" + Constants.assetsPath + "/db/locations.db";
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

    private void copyDatabaseToAssetsPath() throws IOException {
        final Path from = AppPathManager.getAppInstallDir().resolve("jarFiles/db/locations.db").toAbsolutePath();
        final Path to = Paths.get(Constants.assetsPath + "/db/locations.db").toAbsolutePath();
        if (!FileUtils.copySeedIfNotExist(from, to)) {
            Logger.warn("[LocationsDBManager] Required locations.db seed file missing: " + to);
        }
    }

}
