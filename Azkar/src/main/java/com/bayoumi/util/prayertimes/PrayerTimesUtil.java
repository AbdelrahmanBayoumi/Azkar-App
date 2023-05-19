package com.bayoumi.util.prayertimes;

import com.batoulapps.adhan.*;
import com.batoulapps.adhan.data.DateComponents;
import com.bayoumi.models.settings.PrayerTimeSettings;

import java.util.Date;

public class PrayerTimesUtil {

    public static PrayerTimes getPrayerTimesToday(PrayerTimeSettings prayerTimeSettings, Date nowDate) {
        final Coordinates coordinates = new Coordinates(prayerTimeSettings.getLatitude(), prayerTimeSettings.getLongitude());
        final DateComponents dateComponents = DateComponents.from(nowDate);
        final CalculationParameters parameters = CalculationMethod.valueOf(prayerTimeSettings.getMethod().getValue()).getParameters();
        parameters.madhab = prayerTimeSettings.getAsrJuristic() == 0 ? Madhab.SHAFI : Madhab.HANAFI;

        PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, parameters);
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
/*
    // ==== TEST =====
    public static void main(String[] args) {
        final Coordinates coordinates = new Coordinates(31.1981, 29.9192);
        Date todayDate = Utilities.getCurrentDate();
        final DateComponents dateComponents = DateComponents.from(todayDate);
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
        System.out.println("=========================================");
        System.out.println(prayerTimes.currentPrayer());
        System.out.println(prayerTimes.nextPrayer());
        if (prayerTimes.nextPrayer().equals(Prayer.NONE)) {
            System.out.println("ggggggggg");
            getTomorrowNextPrayer(todayDate, coordinates, parameters);


            // get counter from prayerTimes.currentPrayer() [ISHA]
        } else {
            System.out.println(prayerTimes.nextPrayer() + " => " + prayerTimes.timeForPrayer(prayerTimes.nextPrayer()));
        }
        com.bayoumi.util.time.Utilities.findDifference(todayDate, prayerTimes.timeForPrayer(prayerTimes.nextPrayer()));
        System.out.println("=========================================");
        SunnahTimes sunnahTimes = new SunnahTimes(prayerTimes);
        System.out.println("middleOfTheNight: " + sunnahTimes.middleOfTheNight);
        System.out.println("lastThirdOfTheNight: " + sunnahTimes.lastThirdOfTheNight);
        System.out.println("=========================================");
        System.out.println("Qibla direction: " + new Qibla(coordinates).direction);
    }

    private static Prayer getTomorrowNextPrayer(Date today, Coordinates coordinates, CalculationParameters calculationParameters) {
        final DateComponents tomorrowDate = DateComponents.from(com.bayoumi.util.time.Utilities.getTomorrowDate(today));
        final PrayerTimes tomorrowPrayerTimes = new PrayerTimes(coordinates, tomorrowDate, calculationParameters);
        System.out.println(tomorrowPrayerTimes.nextPrayer() + " => " + tomorrowPrayerTimes.timeForPrayer(tomorrowPrayerTimes.nextPrayer()));
        return tomorrowPrayerTimes.nextPrayer();
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
*/
}