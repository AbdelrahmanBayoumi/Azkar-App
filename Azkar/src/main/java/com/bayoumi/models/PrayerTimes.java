package com.bayoumi.models;

import com.bayoumi.util.time.Utilities;

import java.time.LocalDate;
import java.time.LocalTime;

public class PrayerTimes {
    private LocalDate localDate;
    private String fajr;
    private String sunrise;
    private String dhuhr;
    private String asr;
    private String maghrib;
    private String isha;
    private PrayerTimeSettings prayerTimeSettings;


    public PrayerTimes(LocalDate localDate, String fajr, String sunrise, String dhuhr, String asr, String maghrib, String isha) {
        this.localDate = localDate;
        this.fajr = fajr;
        this.sunrise = sunrise;
        this.dhuhr = dhuhr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.isha = isha;
        prayerTimeSettings = new PrayerTimeSettings();
    }

    public static PrayerTimesBuilder builder() {
        return new PrayerTimesBuilder();
    }

    public void formatTime24To12(String language) {
        this.fajr = Utilities.formatTime24To12String(language, this.fajr);
        this.sunrise = Utilities.formatTime24To12String(language, this.sunrise);
        this.dhuhr = Utilities.formatTime24To12String(language, this.dhuhr);
        this.asr = Utilities.formatTime24To12String(language, this.asr);
        this.maghrib = Utilities.formatTime24To12String(language, this.maghrib);
        this.isha = Utilities.formatTime24To12String(language, this.isha);
    }

    public void enableSummerTime() {
        this.fajr = LocalTime.parse(this.fajr).plusHours(1).toString();
        this.sunrise = LocalTime.parse(this.sunrise).plusHours(1).toString();
        this.dhuhr = LocalTime.parse(this.dhuhr).plusHours(1).toString();
        this.asr = LocalTime.parse(this.asr).plusHours(1).toString();
        this.maghrib = LocalTime.parse(this.maghrib).plusHours(1).toString();
        this.isha = LocalTime.parse(this.isha).plusHours(1).toString();
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public String getFajr() {
        return fajr;
    }

    public void setFajr(String fajr) {
        this.fajr = fajr;
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public String getDhuhr() {
        return dhuhr;
    }

    public void setDhuhr(String dhuhr) {
        this.dhuhr = dhuhr;
    }

    public String getAsr() {
        return asr;
    }

    public void setAsr(String asr) {
        this.asr = asr;
    }

    public String getMaghrib() {
        return maghrib;
    }

    public void setMaghrib(String maghrib) {
        this.maghrib = maghrib;
    }

    public String getIsha() {
        return isha;
    }

    public void setIsha(String isha) {
        this.isha = isha;
    }

    public PrayerTimeSettings getPrayerTimeSettings() {
        return prayerTimeSettings;
    }

    public void setPrayerTimeSettings(PrayerTimeSettings prayerTimeSettings1) {
        prayerTimeSettings = prayerTimeSettings1;
    }

    @Override
    public String toString() {
        return "PrayerTimes{" +
                "localDate=" + localDate +
                ", fajr='" + fajr + '\'' +
                ", sunrise='" + sunrise + '\'' +
                ", dhuhr='" + dhuhr + '\'' +
                ", asr='" + asr + '\'' +
                ", maghrib='" + maghrib + '\'' +
                ", isha='" + isha + '\'' +
                '}';
    }

}
