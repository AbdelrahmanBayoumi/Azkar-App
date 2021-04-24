package com.bayoumi.util.prayertimes.local;

import com.batoulapps.adhan.*;
import com.batoulapps.adhan.data.DateComponents;
import com.bayoumi.models.PrayerTimeSettings;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;

public class PrayerTimesUtil {

    public static void main(String[] args) {
        System.out.println(TimeZone.getDefault());
        System.out.println(TimeZone.getTimeZone("GMT+2"));
        final Coordinates coordinates = new Coordinates(30.05, 31.25);
        final DateComponents dateComponents = DateComponents.from(new Date());
        final CalculationParameters parameters = getPrayerTimesSettings();

        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");
        formatter.setTimeZone(TimeZone.getDefault());

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, parameters);
        System.out.println("Fajr: " + formatter.format(prayerTimes.fajr));
        System.out.println("Sunrise: " + formatter.format(prayerTimes.sunrise));
        System.out.println("Dhuhr: " + formatter.format(prayerTimes.dhuhr));
        System.out.println("Asr: " + formatter.format(prayerTimes.asr));
        System.out.println("Maghrib: " + formatter.format(prayerTimes.maghrib));
        System.out.println("Isha: " + formatter.format(prayerTimes.isha));
    }


    private static CalculationParameters getPrayerTimesSettings() {
        return setPrayerTimesSettings(CalculationMethod.EGYPTIAN, Madhab.SHAFI, null, null);
    }

    private static CalculationParameters setPrayerTimesSettings(CalculationMethod method, Madhab madhab,
                                                                int[] timeAdjustments, HighLatitudeRule highLatitudeRule) {
        final CalculationParameters parameters = method.getParameters();
        parameters.madhab = madhab;
        if (timeAdjustments != null) {
            parameters.adjustments.fajr = timeAdjustments[0];
            parameters.adjustments.sunrise = timeAdjustments[1];
            parameters.adjustments.dhuhr = timeAdjustments[2];
            parameters.adjustments.asr = timeAdjustments[3];
            parameters.adjustments.maghrib = timeAdjustments[4];
            parameters.adjustments.isha = timeAdjustments[5];
        }
        if (highLatitudeRule != null) {
            parameters.highLatitudeRule = highLatitudeRule;
        }
        return parameters;
    }

    public static PrayerTimes getPrayerTimesToday(PrayerTimeSettings prayerTimeSettings) {
        final Coordinates coordinates = new Coordinates(prayerTimeSettings.getLatitude(), prayerTimeSettings.getLongitude());
        final DateComponents dateComponents = DateComponents.from(new Date());
        final CalculationParameters parameters = CalculationMethod.valueOf(prayerTimeSettings.getMethod().getValue()).getParameters();
        parameters.madhab = prayerTimeSettings.getAsrJuristic() == 0 ? Madhab.SHAFI : Madhab.HANAFI;

        com.batoulapps.adhan.PrayerTimes prayerTimes = new com.batoulapps.adhan.PrayerTimes(coordinates, dateComponents, parameters);
        if (prayerTimeSettings.isSummerTiming()) {
            prayerTimes.fajr.setTime(prayerTimes.fajr.getTime() + 60 * 60 * 1000);
            prayerTimes.sunrise.setTime(prayerTimes.sunrise.getTime() + 60 * 60 * 1000);
            prayerTimes.dhuhr.setTime(prayerTimes.dhuhr.getTime() + 60 * 60 * 1000);
            prayerTimes.asr.setTime(prayerTimes.asr.getTime() + 60 * 60 * 1000);
            prayerTimes.maghrib.setTime(prayerTimes.maghrib.getTime() + 60 * 60 * 1000);
            prayerTimes.isha.setTime(prayerTimes.isha.getTime() + 60 * 60 * 1000);
        }
        return prayerTimes;
    }
}