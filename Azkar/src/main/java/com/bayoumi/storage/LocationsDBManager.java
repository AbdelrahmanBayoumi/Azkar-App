package com.bayoumi.storage;

import com.bayoumi.util.Constants;
import com.bayoumi.util.Logger;
import com.bayoumi.util.file.AppPathManager;
import org.sqlite.SQLiteConfig;

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
        Path bundledPath = AppPathManager.getAppInstallDir().resolve("jarFiles/db/locations.db").toAbsolutePath();
        Path fallbackPath = Paths.get(Constants.assetsPath + "/db/locations.db").toAbsolutePath();

        this.con = openDatabaseConnection(bundledPath, fallbackPath);
        if (this.con == null) {
            throw new Exception("LocationsDB does not exist or is invalid");
        }
    }

    public static LocationsDBManager getInstance() throws Exception {
        if (databaseManager == null) {
            databaseManager = new LocationsDBManager();
        }
        return databaseManager;
    }

    static Connection openDatabaseConnection(Path bundledPath, Path fallbackPath) {
        Path normBundled = (bundledPath != null) ? bundledPath.toAbsolutePath().normalize() : null;
        Path normFallback = (fallbackPath != null) ? fallbackPath.toAbsolutePath().normalize() : null;

        if (normBundled != null && Files.isRegularFile(normBundled)) {
            Connection candidate = tryConnectReadOnly(normBundled);
            if (candidate != null) {
                return candidate;
            }
        }
        if (normFallback != null && Files.isRegularFile(normFallback)) {
            if (normBundled != null && normFallback.equals(normBundled)) {
                return null;
            }
            Connection candidate = tryConnectReadOnly(normFallback);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Connection tryConnectReadOnly(Path dbPath) {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            SQLiteConfig config = new SQLiteConfig();
            config.setReadOnly(true);
            String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().normalize().toString();
            connection = DriverManager.getConnection(url, config.toProperties());
            try (java.sql.Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON;");
            }
            if (DatabaseHelper.checkIfTablesExist(connection, "Countries")
                    && DatabaseHelper.checkIfTablesExist(connection, "cityd")) {
                return connection;
            } else {
                connection.close();
                return null;
            }
        } catch (Exception ex) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
            Logger.error(ex.getLocalizedMessage(), ex, LocationsDBManager.class.getName() + ".tryConnectReadOnly()");
        }
        return null;
    }

}
