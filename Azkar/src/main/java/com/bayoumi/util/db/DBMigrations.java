package com.bayoumi.util.db;

import com.bayoumi.models.preferences.Preferences;
import com.bayoumi.models.preferences.PreferencesType;
import com.bayoumi.util.Logger;

import java.sql.SQLException;

public class DBMigrations {

    public static void init() throws SQLException {
        try {
            DatabaseManager.getInstance().con.setAutoCommit(false);
            switch (Integer.parseInt(Preferences.getInstance().get(PreferencesType.DB_VERSION, "0"))) {
                case 0:
                    // TODO: write migration description
                    v1();
            }
            DatabaseManager.getInstance().con.commit(); // Commit transaction
        } catch (Exception ex) {
            Logger.error(null, ex, DBMigrations.class.getName() + ".init()");
            DatabaseManager.getInstance().con.rollback(); // Rollback if an exception occurs
        } finally {
            DatabaseManager.getInstance().con.setAutoCommit(true); // Reset auto-commit mode
        }
    }

    /**
     * TODO: handle migration
     */
    private static void v1() throws Exception {
        Logger.debug("-- v1() --");

        Preferences.getInstance().set(PreferencesType.DB_VERSION, "1");
    }
}
