package com.bayoumi.models.settings;

import com.bayoumi.storage.preferences.Preferences;
import com.bayoumi.storage.preferences.PreferencesType;
import com.bayoumi.util.gui.notfication.NotificationAudio;

import java.util.Observable;

public class AzkarSettings extends Observable {

    private int morningAzkarReminder;
    private int nightAzkarReminder;
    private String audioName;
    private String selectedPeriod;
    private int highPeriod;
    private int midPeriod;
    private int lowPeriod;
    private int rearPeriod;
    private boolean isStopped;
    private int volume;
    private int timedAzkarFontSize;
    private int prayerVolume;
    private int azkarDuration;

    protected AzkarSettings() {
        loadSettings();
    }


    public void loadSettings() {
        this.morningAzkarReminder = Preferences.getInstance().getInt(PreferencesType.MORNING_AZKAR_REMINDER);
        this.nightAzkarReminder = Preferences.getInstance().getInt(PreferencesType.NIGHT_AZKAR_REMINDER);
        this.audioName = Preferences.getInstance().get(PreferencesType.AUDIO_NAME);
        this.highPeriod = Preferences.getInstance().getInt(PreferencesType.HIGH_PERIOD);
        this.midPeriod = Preferences.getInstance().getInt(PreferencesType.MID_PERIOD);
        this.lowPeriod = Preferences.getInstance().getInt(PreferencesType.LOW_PERIOD);
        this.rearPeriod = Preferences.getInstance().getInt(PreferencesType.REAR_PERIOD);
        this.isStopped = Preferences.getInstance().getBoolean(PreferencesType.IS_AZKAR_STOPPED);
        this.selectedPeriod = Preferences.getInstance().get(PreferencesType.SELECTED_PERIOD);
        this.volume = Preferences.getInstance().getInt(PreferencesType.VOLUME);
        this.prayerVolume = Preferences.getInstance().getInt(PreferencesType.PRAYER_VOLUME);
        this.timedAzkarFontSize = Preferences.getInstance().getInt(PreferencesType.TIMED_AZKAR_FONT_SIZE);
        this.azkarDuration = Preferences.getInstance().getInt(PreferencesType.AZKAR_DURATION);

    }

    public int getTimedAzkarFontSize() {
        return timedAzkarFontSize;
    }

    public void setTimedAzkarFontSize(int timedAzkarFontSize) {
        setChanged();
        // 1. set value to local variable
        this.timedAzkarFontSize = timedAzkarFontSize;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.TIMED_AZKAR_FONT_SIZE, timedAzkarFontSize + "");
    }

    public int getMorningAzkarOffset() {
        return morningAzkarReminder;
    }

    public int getNightAzkarOffset() {
        return nightAzkarReminder;
    }

    public int getMorningAzkarReminder() {
        return morningAzkarReminder;
    }

    public void setMorningAzkarReminder(int morningAzkarReminder) {
        setChanged();
        // 1. set value to local variable
        this.morningAzkarReminder = morningAzkarReminder;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.MORNING_AZKAR_REMINDER, morningAzkarReminder + "");
    }

    public int getNightAzkarReminder() {
        return nightAzkarReminder;
    }

    public void setNightAzkarReminder(int nightAzkarReminder) {
        setChanged();
        // 1. set value to local variable
        this.nightAzkarReminder = nightAzkarReminder;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.NIGHT_AZKAR_REMINDER, nightAzkarReminder + "");
    }

    public String getAudioName() {
        if (NotificationAudio.getAudioList().contains(audioName)) {
            return audioName;
        }
        return "بدون صوت";
    }

    public void setAudioName(String audioName) {
        setChanged();
        // 1. set value to local variable
        this.audioName = audioName;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.AUDIO_NAME, audioName);
    }

    public int getHighPeriod() {
        return highPeriod;
    }

    public void setHighPeriod(int highPeriod) {
        setChanged();
        // 1. set value to local variable
        this.highPeriod = highPeriod;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.HIGH_PERIOD, highPeriod + "");
    }

    public int getMidPeriod() {
        return midPeriod;
    }

    public void setMidPeriod(int midPeriod) {
        setChanged();
        // 1. set value to local variable
        this.midPeriod = midPeriod;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.MID_PERIOD, midPeriod + "");
    }

    public int getLowPeriod() {
        return lowPeriod;
    }

    public void setLowPeriod(int lowPeriod) {
        setChanged();
        // 1. set value to local variable
        this.lowPeriod = lowPeriod;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.LOW_PERIOD, lowPeriod + "");
    }

    public int getRearPeriod() {
        return rearPeriod;
    }

    public void setRearPeriod(int rearPeriod) {
        setChanged();
        // 1. set value to local variable
        this.rearPeriod = rearPeriod;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.REAR_PERIOD, rearPeriod + "");
    }

    public boolean isStopped() {
        return isStopped;
    }

    public void setStopped(boolean stopped) {
        setChanged();
        // 1. set value to local variable
        isStopped = stopped;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.IS_AZKAR_STOPPED, stopped + "");
    }

    public String getSelectedPeriod() {
        return selectedPeriod;
    }

    public void setSelectedPeriod(String selectedPeriod) {
        setChanged();
        // 1. set value to local variable
        this.selectedPeriod = selectedPeriod;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.SELECTED_PERIOD, selectedPeriod);
    }

    public int getAzkarDuration() {
        return azkarDuration;
    }

    public void setAzkarDuration(int azkarDuration) {
        setChanged();
        // 1. set value to local variable
        this.azkarDuration = azkarDuration;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.AZKAR_DURATION, azkarDuration + "");
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        setChanged();
        // 1. set value to local variable
        this.volume = volume;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.VOLUME, volume + "");
    }

    public int getPrayerVolume() {
        return prayerVolume;
    }

    public void setPrayerVolume(int volume) {
        setChanged();
        // 1. set value to local variable
        this.prayerVolume = volume;
        // 2. save value to DB
        Preferences.getInstance().set(PreferencesType.PRAYER_VOLUME, volume + "");
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
                ", prayerVolume=" + prayerVolume +

                '}';
    }

}
