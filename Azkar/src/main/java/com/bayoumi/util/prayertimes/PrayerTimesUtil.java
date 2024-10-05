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

        // Adjustments
        parameters.adjustments.fajr = prayerTimeSettings.getFajrAdjustment();
        parameters.adjustments.sunrise = prayerTimeSettings.getSunriseAdjustment();
        parameters.adjustments.dhuhr = prayerTimeSettings.getDhuhrAdjustment();
        parameters.adjustments.asr = prayerTimeSettings.getAsrAdjustment();
        parameters.adjustments.maghrib = prayerTimeSettings.getMaghribAdjustment();
        parameters.adjustments.isha = prayerTimeSettings.getIshaAdjustment();

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
}