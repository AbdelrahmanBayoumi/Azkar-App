package com.bayoumi.util.prayertimes;

import com.bayoumi.models.PrayerTimes;
import com.bayoumi.models.PrayerTimesBuilder;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utilities;
import com.bayoumi.util.db.DatabaseAssetsManager;
import com.bayoumi.util.time.HijriDate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class PrayerTimesDBManager {

    public static boolean checkIfTodayExist() {
        HijriDate hijriDateToday = new HijriDate();
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes WHERE date='" + hijriDateToday.getHijriDateFormatted() + "'").executeQuery();
            if (res.next()) {
                return true;
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".checkIfTodayExist()");
            ex.printStackTrace();
        }
        return false;
    }

    public static PrayerTimes getPrayerTimesForToday() {
        HijriDate hijriDateToday = new HijriDate();
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes WHERE date='" + hijriDateToday.getYear() + "-" + Utilities.formatIntToTwoDigit(hijriDateToday.getMonth() + 1) + "-" + Utilities.formatIntToTwoDigit(hijriDateToday.getDay()) + "'").executeQuery();
            if (res.next()) {
                return new PrayerTimesBuilder().
                        hijriDate(hijriDateToday)
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

    public static boolean insertPrayerTimesData(ArrayList<PrayerTimes> prayerTimes) {
        try {
            DatabaseAssetsManager databaseAssetsManager = DatabaseAssetsManager.getInstance();
            for (PrayerTimes prayerTime : prayerTimes) {
                databaseAssetsManager.stat = databaseAssetsManager.con.prepareStatement("INSERT INTO prayertimes (date,fajr,sunrise,dhuhr,asr,maghrib,isha) VALUES(?,?,?,?,?,?,?)");
                databaseAssetsManager.stat.setString(1, prayerTime.getHijriDate().getHijriDateFormatted());
                databaseAssetsManager.stat.setString(2, prayerTime.getFajr());
                databaseAssetsManager.stat.setString(3, prayerTime.getSunrise());
                databaseAssetsManager.stat.setString(4, prayerTime.getDhuhr());
                databaseAssetsManager.stat.setString(5, prayerTime.getAsr());
                databaseAssetsManager.stat.setString(6, prayerTime.getMaghrib());
                databaseAssetsManager.stat.setString(7, prayerTime.getIsha());

                databaseAssetsManager.stat.execute();
            }
            return true;
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".insertPrayerTimesData()");
            ex.printStackTrace();
        }
        return false;
    }

    public static HijriDate getLastDateStored() {
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes ORDER BY date DESC LIMIT 1").executeQuery();
            if (res.next()) {
                // value like: "1442-07-06"
                return getDateFromString(res.getString(1));
            }
        } catch (Exception ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".getPrayerTimesForToday()");
            ex.printStackTrace();
        }
        return null;
    }

    private static HijriDate getDateFromString(String s) {
        String[] dateArr = s.split("-");
        return new HijriDate(Integer.parseInt(dateArr[0]), Integer.parseInt(dateArr[1]) - 1, Integer.parseInt(dateArr[2]));
    }

    public static void deleteLastSpecificMonth(HijriDate hijriDate) {
        try {
            DatabaseAssetsManager.getInstance().con
                    .prepareStatement("DELETE FROM prayertimes WHERE prayertimes.date like '" + hijriDate.getYear() + "-" + Utilities.formatIntToTwoDigit(hijriDate.getMonth() + 1) + "-__" + "'")
                    .executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".deleteLastSpecificMonth()");
        }
    }

    public static void deleteAll() {
        try {
            DatabaseAssetsManager.getInstance().con
                    .prepareStatement("DELETE FROM prayertimes")
                    .executeUpdate();
        } catch (SQLException ex) {
            Logger.error(null, ex, PrayerTimesDBManager.class.getName() + ".deleteAll()");
        }
    }

    public static ArrayList<PrayerTimes> getAll() {
        ArrayList<PrayerTimes> prayerTimesArrayList = new ArrayList<>();
        try {
            ResultSet res = DatabaseAssetsManager.getInstance().con.prepareStatement("SELECT * FROM prayertimes").executeQuery();
            while (res.next()) {
                prayerTimesArrayList.add(new PrayerTimesBuilder().
                        hijriDate(getDateFromString(res.getString(1)))
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
