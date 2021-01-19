package com.bayoumi.models;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseAssetsManager;

import java.sql.ResultSet;

public class AzkarSettings {
    public static boolean isUpdated = false;
    private String morningAzkarReminder;
    private String nightAzkarReminder;
    private String audioName;
    private String selectedPeriod;
    private int highPeriod;
    private int midPeriod;
    private int lowPeriod;
    private int rearPeriod;
    private boolean isStopped;


    public AzkarSettings() {
        loadSettings();
    }

    private void loadSettings() {
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM azkar_settings").executeQuery();
            if (res.next()) {
                this.morningAzkarReminder = res.getString(1);
                this.nightAzkarReminder = res.getString(2);
                this.audioName = res.getString(3);
                this.highPeriod = res.getInt(4);
                this.midPeriod = res.getInt(5);
                this.lowPeriod = res.getInt(6);
                this.rearPeriod = res.getInt(7);
                this.isStopped = res.getInt(8) == 1;
                this.selectedPeriod = res.getString(9);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    public void save() {
        try {
            DatabaseAssetsManager databaseAssetsManager = DatabaseAssetsManager.getInstance();
            databaseAssetsManager.stat = databaseAssetsManager.con.prepareStatement("UPDATE azkar_settings set morning_reminder = ?, night_reminder = ?, audio_name = ?, high_period = ? , mid_period = ?, low_period = ?, rear_period = ?, stop_azkar = ?, selected_period = ?");
            databaseAssetsManager.stat.setString(1, this.getMorningAzkarReminder());
            databaseAssetsManager.stat.setString(2, this.getNightAzkarReminder());
            databaseAssetsManager.stat.setString(3, this.getAudioName());
            databaseAssetsManager.stat.setInt(4, this.getHighPeriod());
            databaseAssetsManager.stat.setInt(5, this.getMidPeriod());
            databaseAssetsManager.stat.setInt(6, this.getLowPeriod());
            databaseAssetsManager.stat.setInt(7, this.getRearPeriod());
            databaseAssetsManager.stat.setInt(8, this.isStopped() ? 1 : 0);
            databaseAssetsManager.stat.setString(9, this.getSelectedPeriod());
            databaseAssetsManager.stat.executeUpdate();
            isUpdated = true;
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    public void saveSelectedPeriod() {
        try {
            DatabaseAssetsManager.getInstance().con
                    .prepareStatement("UPDATE azkar_settings set selected_period = '" + this.getSelectedPeriod() + "'").
                    executeUpdate();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".saveSelectedPeriod()");
        }
    }

    public String getMorningAzkarReminder() {
        return morningAzkarReminder;
    }

    public void setMorningAzkarReminder(String morningAzkarReminder) {
        this.morningAzkarReminder = morningAzkarReminder;
    }

    public String getNightAzkarReminder() {
        return nightAzkarReminder;
    }

    public void setNightAzkarReminder(String nightAzkarReminder) {
        this.nightAzkarReminder = nightAzkarReminder;
    }

    public String getAudioName() {
        return audioName;
    }

    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }

    public int getHighPeriod() {
        return highPeriod;
    }

    public void setHighPeriod(int highPeriod) {
        this.highPeriod = highPeriod;
    }

    public int getMidPeriod() {
        return midPeriod;
    }

    public void setMidPeriod(int midPeriod) {
        this.midPeriod = midPeriod;
    }

    public int getLowPeriod() {
        return lowPeriod;
    }

    public void setLowPeriod(int lowPeriod) {
        this.lowPeriod = lowPeriod;
    }

    public int getRearPeriod() {
        return rearPeriod;
    }

    public void setRearPeriod(int rearPeriod) {
        this.rearPeriod = rearPeriod;
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        isStopped = stopped;
    }

    public String getSelectedPeriod() {
        return selectedPeriod;
    }

    public void setSelectedPeriod(String selectedPeriod) {
        this.selectedPeriod = selectedPeriod;
    }
}
