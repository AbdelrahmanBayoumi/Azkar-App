package com.bayoumi.models;

import com.bayoumi.util.time.HijriDate;

public class PrayerTimesBuilder {
    private HijriDate hijriDate;
    private String fajr;
    private String sunrise;
    private String dhuhr;
    private String asr;
    private String maghrib;
    private String isha;

    public PrayerTimesBuilder() {
    }

    public PrayerTimesBuilder hijriDate(HijriDate hijriDate) {
        this.hijriDate = hijriDate;
        return this;
    }

    public PrayerTimesBuilder fajr(String fajr) {
        this.fajr = fajr;
        return this;
    }

    public PrayerTimesBuilder sunrise(String sunrise) {
        this.sunrise = sunrise;
        return this;
    }

    public PrayerTimesBuilder dhuhr(String dhuhr) {
        this.dhuhr = dhuhr;
        return this;
    }

    public PrayerTimesBuilder asr(String asr) {
        this.asr = asr;
        return this;
    }

    public PrayerTimesBuilder maghrib(String maghrib) {
        this.maghrib = maghrib;
        return this;
    }

    public PrayerTimesBuilder isha(String isha) {
        this.isha = isha;
        return this;
    }

    public PrayerTimes build() {
        return new PrayerTimes(hijriDate, fajr, sunrise, dhuhr, asr, maghrib, isha);
    }
}

