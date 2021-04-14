package com.bayoumi.util.prayertimes;

import com.bayoumi.models.PrayerTimes;
import javafx.beans.property.SimpleIntegerProperty;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class PrayerTimesValidation extends Thread {

    /**
     * [0]: still validating, [1]: finished successfully, [-1]: failed in validation
     */
    public static final SimpleIntegerProperty PRAYERTIMES_STATUS = new SimpleIntegerProperty(0);

    /**
     * checks if (lastDateStoredInDB - todayDate) <= 5 then fetch new Month data from the API
     *
     * @return true if fetching data and insertion is success
     * false if any error happens
     */
    public static boolean checkForSpareData() {
        LocalDate lastDateStored = PrayerTimesDBManager.getLastDateStored();
        if (lastDateStored == null) {
            return false;
        }
        System.out.println("lastDateStored: " + lastDateStored.toString() + ", " + "today: " + LocalDate.now());

        int daysBetween = (int) Math.abs(ChronoUnit.DAYS.between(lastDateStored, LocalDate.now()));
        System.out.println("daysBetween = " + daysBetween);

        // if (lastDateStoredInDB - todayDate) <= 5
        // if there is less than or equal to 5 days to end of days stored
        if (daysBetween <= 5) {
            // fetch new Month data from API
           ArrayList<PrayerTimes> prayerTimesMonth = WebService.getPrayerTimesMonth(lastDateStored.plusDays(1));
            // check if there is any days of fetched month in DB => delete it to insert the new data of the same month
            if (prayerTimesMonth.get(0).getLocalDate().getMonth().getValue() == lastDateStored.getMonth().getValue()) {
                System.out.println("deletePrayerTimesInSpecificMonth");
                PrayerTimesDBManager.deletePrayerTimesInSpecificMonth(lastDateStored);
            }
            // insert prayerTimes to DB
            System.out.println("prayerTimesMonth.size:"+ prayerTimesMonth.size());
            if (prayerTimesMonth.size() < 1 || !PrayerTimesDBManager.insertPrayerTimesData(prayerTimesMonth)) {
                return false;
            }
        }
        PRAYERTIMES_STATUS.setValue(1);
        return true;
    }

    /**
     * Validate if there is Prayer Times Stored Locally
     */
    @Override
    public void run() {
        PRAYERTIMES_STATUS.setValue(0);
        System.out.println("PrayerTimesValidation: Thread Starts ...");

        // if PrayerTimes data for today exist ?
        if (PrayerTimesDBManager.checkIfTodayExist()) {
            System.out.println("PrayerTimesValidation: Day Found");
        } else {
            System.out.println("PrayerTimesValidation: Day NOT Found");

            // Fetch current month data from API
            ArrayList<PrayerTimes> prayerTimesMonth = WebService.getPrayerTimesMonth(LocalDate.now());

            if (prayerTimesMonth.size() < 1 || !PrayerTimesDBManager.insertPrayerTimesData(prayerTimesMonth)) {
                PRAYERTIMES_STATUS.setValue(-1);
            }
        }

        if (!checkForSpareData()) {
            PRAYERTIMES_STATUS.setValue(-1);
        }
        System.out.println("PrayerTimesValidation: Thread Ends ...");
    }

}
