package com.bayoumi.repositry;

import com.bayoumi.util.Logger;
import com.bayoumi.storage.DatabaseManager;

import java.sql.ResultSet;

public class OnboardingRepository {

    public static boolean isFirstTimeOpened() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM onboarding").executeQuery();
            if (res.next()) {
                return res.getInt("isFirstTimeOpened") == 1;
            }
        } catch (Exception ex) {
            Logger.error(ex.getLocalizedMessage(), ex, OnboardingRepository.class.getName() + ".isFirstTimeOpened()");
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
            Logger.error(ex.getLocalizedMessage(), ex, OnboardingRepository.class.getName() + ".setFirstTimeOpened()");
        }
    }
}
