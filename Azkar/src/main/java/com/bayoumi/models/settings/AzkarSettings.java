package com.bayoumi.models.settings;

import com.bayoumi.util.Logger;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.file.FileUtils;

import java.sql.ResultSet;
import java.util.Observable;

public class AzkarSettings extends Observable {

    private String morningAzkarReminder;
    private String nightAzkarReminder;
    private String audioName;
    private String selectedPeriod;
    private int highPeriod;
    private int midPeriod;
    private int lowPeriod;
    private int rearPeriod;
    private boolean isStopped;
    private int volume;


    protected AzkarSettings() {
        loadSettings();
    }

    public int getMorningAzkarOffset() {
        return getOffset(morningAzkarReminder);
    }

    public int getNightAzkarOffset() {
        return getOffset(nightAzkarReminder);
    }

    private int getOffset(String reminder) {
        // TODO : about timed azkar reminder
        int offset = 0;
        if (reminder.equals("بـ نصف ساعة")) {
            offset = 30;
        } else if (reminder.equals("بـ ساعة")) {
            offset = 60;
        }
        return offset;
    }

    public void loadSettings() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM azkar_settings").executeQuery();
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
                this.volume = res.getInt(10);
            }
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".loadSettings()");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzkarSettings) {
            AzkarSettings azkarSettings = (AzkarSettings) obj;
            return azkarSettings.getMorningAzkarReminder().equals(this.getMorningAzkarReminder()) &&
                    azkarSettings.getNightAzkarReminder().equals(this.getNightAzkarReminder()) &&
                    azkarSettings.getAudioName().equals(this.getAudioName()) &&
                    azkarSettings.getHighPeriod() == this.getHighPeriod() &&
                    azkarSettings.getMidPeriod() == this.getMidPeriod() &&
                    azkarSettings.getLowPeriod() == this.getLowPeriod() &&
                    azkarSettings.getRearPeriod() == this.getRearPeriod() &&
                    azkarSettings.isStopped() == this.isStopped() &&
                    azkarSettings.getSelectedPeriod().equals(this.getSelectedPeriod()) &&
                    azkarSettings.getVolume() == this.getVolume();
        }
        return false;
    }

    public void save() {
        try {
            if (this.equals(new AzkarSettings())) {
                // if current obj is equal the one stored in DB then => do nothing (don't save)
                return;
            }

            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.stat = databaseManager.con.prepareStatement("UPDATE azkar_settings set morning_reminder = ?, night_reminder = ?, audio_name = ?, high_period = ? , mid_period = ?, low_period = ?, rear_period = ?, stop_azkar = ?, selected_period = ?, volume = ?");
            databaseManager.stat.setString(1, this.getMorningAzkarReminder());
            databaseManager.stat.setString(2, this.getNightAzkarReminder());
            databaseManager.stat.setString(3, this.getAudioName());
            databaseManager.stat.setInt(4, this.getHighPeriod());
            databaseManager.stat.setInt(5, this.getMidPeriod());
            databaseManager.stat.setInt(6, this.getLowPeriod());
            databaseManager.stat.setInt(7, this.getRearPeriod());
            databaseManager.stat.setInt(8, this.isStopped() ? 1 : 0);
            databaseManager.stat.setString(9, this.getSelectedPeriod());
            databaseManager.stat.setInt(10, this.getVolume());
            databaseManager.stat.executeUpdate();
            // fetch new updated data from database
            loadSettings();
            // fire change event to all listeners to update their values
            setChanged();
            notifyObservers();
        } catch (Exception ex) {
            Logger.error(null, ex, getClass().getName() + ".save()");
        }
    }

    /**
     * saveSelectedPeriod to DB without using save() to save all fields
     */
    public void saveSelectedPeriod() {
        try {
            DatabaseManager.getInstance().con
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
        if (FileUtils.getAudioList().contains(audioName)) {
            return audioName;
        }
        return "بدون صوت";
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

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "AzkarSettings{" +
                "morningAzkarReminder='" + morningAzkarReminder + '\'' +
                ", nightAzkarReminder='" + nightAzkarReminder + '\'' +
                ", audioName='" + audioName + '\'' +
                ", selectedPeriod='" + selectedPeriod + '\'' +
                ", highPeriod=" + highPeriod +
                ", midPeriod=" + midPeriod +
                ", lowPeriod=" + lowPeriod +
                ", rearPeriod=" + rearPeriod +
                ", isStopped=" + isStopped +
                ", volume=" + volume +
                '}';
    }
}
