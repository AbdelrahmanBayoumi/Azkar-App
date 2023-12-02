package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;

import java.sql.ResultSet;

public class Onboarding {

    public static boolean isFirstTimeOpened() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM onboarding").executeQuery();
            if (res.next()) {
                return res.getInt("isFirstTimeOpened") == 1;
            }
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, Onboarding.class.getName() + ".isFirstTimeOpened()");
        }
        return false;
    }

    public static void setFirstTimeOpened(int isFirstTimeOpened) {
        try {
            DatabaseManager.getInstance()
                    .con
                    .prepareStatement("UPDATE onboarding set isFirstTimeOpened = " + isFirstTimeOpened)
                    .executeUpdate();
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, Onboarding.class.getName() + ".setFirstTimeOpened()");
        }
    }
}
