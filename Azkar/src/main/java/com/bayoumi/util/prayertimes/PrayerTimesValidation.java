package com.bayoumi.util.prayertimes;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.util.time.Utilities;
import com.bayoumi.util.time.HijriDate;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;

public class PrayerTimesValidation extends Thread {

    // [0]: still validating, [1]: finished successfully, [-1]: failed in validation
    public static final SimpleIntegerProperty PRAYERTIMES_STATUS = new SimpleIntegerProperty(0);

    public static boolean checkForSpareData() {
        HijriDate lastDateStored = PrayerTimesDBManager.getLastDateStored();
        if (lastDateStored == null) {
            return false;
        }
        System.out.println("lastDateStored: " + lastDateStored.getHijriDateFormatted() + ", " + "today: " + new HijriDate().getHijriDateFormatted());

        int daysBetween = Utilities.daysBetween(lastDateStored.getCalendar(), new HijriDate().getCalendar());
        System.out.println("daysBetween = " + daysBetween);

        // if (lastDateStoredInDB - todayDate) <= 5
        // if there is less than or equal to 5 days to end of days stored
        if (daysBetween <= 5) {
            ArrayList<PrayerTimes> prayerTimesMonth = WebService.getPrayerTimesMonth(HijriDate.plusDay(lastDateStored, 1));
            // check if there is any days of fetched month in DB => delete it to insert the new data of the same month
            if (prayerTimesMonth.get(0).getHijriDate().getMonth() == lastDateStored.getMonth()) {
                PrayerTimesDBManager.deleteLastSpecificMonth(lastDateStored);
            }
            // insert prayerTimes to DB
            if (prayerTimesMonth.size() < 1 && !PrayerTimesDBManager.insertPrayerTimesData(prayerTimesMonth)) {
                return false;
            }
        }
        PRAYERTIMES_STATUS.setValue(1);
        return true;
    }

    @Override
    public void run() {
        PRAYERTIMES_STATUS.setValue(0);
        System.out.println("Get Prayer Times: Thread Starts ...");

        // if PrayerTimes data for today exist ?
        if (PrayerTimesDBManager.checkIfTodayExist()) {
            System.out.println("Get Prayer Times: Day Found");
        } else {
            System.out.println("Get Prayer Times: Day NOT Found");

            // Fetch current month data from API
            ArrayList<PrayerTimes> prayerTimesMonth = WebService.getPrayerTimesMonth(new HijriDate());

            if (prayerTimesMonth.size() < 1 || !PrayerTimesDBManager.insertPrayerTimesData(prayerTimesMonth)) {
                PRAYERTIMES_STATUS.setValue(-1);
            }
        }
        if (!checkForSpareData()) {
            PRAYERTIMES_STATUS.setValue(-1);
        }
        System.out.println("Get Prayer Times: Thread Ends ...");
    }

}
