package com.bayoumi.util.prayertimes;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.models.PrayerTimesBuilder;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;
import com.bayoumi.util.db.DatabaseManager;
import com.bayoumi.util.time.Utilities;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

public class PrayerTimesDBManager {

    /**
     * @return true if today Gregorian date is exist in Database
     * false today date is not exist
     */
    public static boolean checkIfTodayExist() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes WHERE date='" + LocalDate.now().toString() + "'").executeQuery();
            if (res.next()) {
                return true;
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".checkIfTodayExist()");
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * @return Prayer Times of Today using LocalDate.now()
     */
    public static PrayerTimes getPrayerTimesForToday() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes WHERE date='" + LocalDate.now().toString() + "'").executeQuery();
            if (res.next()) {
                return new PrayerTimesBuilder().
                        localDate(LocalDate.now())
                        .fajr(res.getString(2))
                        .sunrise(res.getString(3))
                        .dhuhr(res.getString(4))
                        .asr(res.getString(5))
                        .maghrib(res.getString(6))
                        .isha(res.getString(7)).build();
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".getPrayerTimesForToday()");
            ex.printStackTrace();
        }
        return PrayerTimes.builder()
                .fajr("--:--")
                .sunrise("--:--")
                .dhuhr("--:--")
                .asr("--:--")
                .maghrib("--:--")
                .isha("--:--")
                .build();
    }


    /**
     * @param prayerTimes list to be inserted in Database
     * @return true if insertion success, false if any Exception happens
     */
    public static boolean insertPrayerTimesData(ArrayList<PrayerTimes> prayerTimes) {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            for (PrayerTimes prayerTime : prayerTimes) {
                System.out.println("prayerTime: "+ prayerTime);
                databaseManager.stat = databaseManager.con.prepareStatement("INSERT INTO prayertimes (date,fajr,sunrise,dhuhr,asr,maghrib,isha) VALUES(?,?,?,?,?,?,?)");
                databaseManager.stat.setString(1, prayerTime.getLocalDate().toString());
                databaseManager.stat.setString(2, prayerTime.getFajr());
                databaseManager.stat.setString(3, prayerTime.getSunrise());
                databaseManager.stat.setString(4, prayerTime.getDhuhr());
                databaseManager.stat.setString(5, prayerTime.getAsr());
                databaseManager.stat.setString(6, prayerTime.getMaghrib());
                databaseManager.stat.setString(7, prayerTime.getIsha());

                databaseManager.stat.execute();
            }
            return true;
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".insertPrayerTimesData()");
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * @return last date stored in prayertimes data as LocalDate Obj
     */
    public static LocalDate getLastDateStored() {
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes ORDER BY date DESC LIMIT 1").executeQuery();
            if (res.next()) {
                // value like: "1442-07-06"
                return Utilities.getDateFromString(res.getString(1));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".getPrayerTimesForToday()");
            ex.printStackTrace();
        }
        return null;
    }


    /**
     * @param localDate required (year & month) to delete prayertimes in that date
     */
    public static void deletePrayerTimesInSpecificMonth(LocalDate localDate) {
        try {
            DatabaseManager.getInstance().con
                    .prepareStatement("DELETE FROM prayertimes WHERE prayertimes.date like '" + localDate.getYear() + "-" + Utility.formatIntToTwoDigit(localDate.getMonth().getValue()) + "-__" + "'")
                    .executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".deleteLastSpecificMonth()");
        }
    }

    /**
     * delete all prayertimes
     */
    public static void deleteAll() {
        try {
            DatabaseManager.getInstance().con
                    .prepareStatement("DELETE FROM prayertimes")
                    .executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".deleteAll()");
        }
    }

    /**
     * @return PrayerTimes List of all prayer times data in Database
     */
    public static ArrayList<PrayerTimes> getAll() {
        ArrayList<PrayerTimes> prayerTimesArrayList = new ArrayList<>();
        try {
            ResultSet res = DatabaseManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes").executeQuery();
            while (res.next()) {
                prayerTimesArrayList.add(new PrayerTimesBuilder().
                        localDate(Utilities.getDateFromString(res.getString(1)))
                        .fajr(res.getString(2))
                        .sunrise(res.getString(3))
                        .dhuhr(res.getString(4))
                        .asr(res.getString(5))
                        .maghrib(res.getString(6))
                        .isha(res.getString(7)).build());
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".getAll()");
            ex.printStackTrace();
        }
        return prayerTimesArrayList;
    }

}
