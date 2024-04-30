package com.bayoumi.util.time;

import com.bayoumi.models.settings.Settings;
import com.bayoumi.util.Logger;
import com.bayoumi.util.Utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Utilities {
    public static Date getCurrentDate() {
        Date date = new Date();
        if (Settings.getInstance().getPrayerTimeSettings().isSummerTiming()) {
            return addHoursToJavaUtilDate(date, 1);
        }
        return date;
    }

    public static Date addHoursToJavaUtilDate(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    public static String convertMillisecondsToMin(long milliseconds) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = (TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);

        return milliseconds + " Milliseconds = "
                + minutes + " minutes, "
                + seconds + " seconds, "
                + (milliseconds - seconds * 1000) + " milliseconds";
    }

    public static boolean isFriday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
    }

    public static String findDifference(Date d1, Date d2) {
        try {
            // Calculate time difference
            // in milliseconds
            long difference_In_Time = d2.getTime() - d1.getTime();

            int difference_In_Seconds = (int) Math.abs(Math.round((difference_In_Time / 1000.0)) % 60);
            int difference_In_Minutes = Math.abs((int) (difference_In_Time / (1000.0 * 60)) % 60);
            int difference_In_Hours = Math.abs((int) (difference_In_Time / (1000.0 * 60 * 60)) % 24);

            return Utility.formatIntToTwoDigit(difference_In_Hours) + ":" + Utility.formatIntToTwoDigit(difference_In_Minutes) + ":" + Utility.formatIntToTwoDigit(difference_In_Seconds);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage(), e, Utilities.class.getName() + ".findDifference()");
        }
        return null;
    }


    /**
     * @param language is the language in which time is shown
     * @param date     obj to get the time from it
     * @return the time in 12-hour format and a specific locale.
     */
    public static String getTime(String language, Date date) {
        return new SimpleDateFormat("hh:mm:ss a", new Locale(language)).format(date);
    }

    /**
     * @param language is the language in which time is shown
     * @param date     obj to get the time from it
     * @return the time in 24-hour format and a specific locale.
     */
    public static String getTime24(String language, Date date) {
        return new SimpleDateFormat("HH:mm:ss", new Locale(language)).format(date);
    }

    /**
     * @param language is the language in which Date is shown
     * @param date     obj to get the day from it
     * @return the Date in a specific locale.
     */
    public static String getGregorianDate(String language, Date date) {
        return new SimpleDateFormat("dd MMMM y", new Locale(language)).format(date);
    }

    /**
     * @param language is the language in which Date is shown
     * @param date     obj to get the day from it
     * @return the name of the day from the Date based on specific locale.
     */
    public static String getDay(String language, Date date) {
        return new SimpleDateFormat("EEEE", new Locale(language)).format(date);
    }

    /**
     * @param language     language is the language in which Date is shown
     * @param time24Format time in 24-hour format
     * @return time in 12-hour format
     */
    public static String formatTime24To12String(String language, String time24Format) {
        try {
            return new SimpleDateFormat("hh:mm a", new Locale(language)).format(new SimpleDateFormat("hh:mm").parse(time24Format));
        } catch (ParseException ignored) {
        }
        return "00:00 am";
    }

    /**
     * @param timeString time expected in format '00:00 (GMT)'
     * @return time string only without timezone '00:00'
     */
    public static String formatTimeOnly(String timeString) {
        return timeString.substring(0, timeString.indexOf('(')).trim();
    }

    /**
     * @param day1 first day
     * @param day2 second day
     * @return number of days between the two dates
     */
    public static int daysBetween(Calendar day1, Calendar day2) {
        Calendar dayOne = (Calendar) day1.clone(),
                dayTwo = (Calendar) day2.clone();

        if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
            return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
        } else {
            if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
                //swap them
                Calendar temp = dayOne;
                dayOne = dayTwo;
                dayTwo = temp;
            }
            int extraDays = 0;

            int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

            while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
                dayOne.add(Calendar.YEAR, -1);
                // getActualMaximum() important for leap years
                extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
            }

            return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays;
        }
    }

    /**
     * @param s date as string ex: '2021-04-15'
     * @return LocalDate obj of that date
     */
    public static LocalDate getDateFromString(String s) {
        String[] dateArr = s.split("-");
        return LocalDate.of(Integer.parseInt(dateArr[0]), Integer.parseInt(dateArr[1]), Integer.parseInt(dateArr[2]));
    }

    /**
     * @param s date as string ex: '2021-04-15'
     * @return LocalDate obj of that date
     */
    private static HijriDate getHijriDateFromString(String s) {
        String[] dateArr = s.split("-");
        return new HijriDate(Integer.parseInt(dateArr[0]), Integer.parseInt(dateArr[1]) - 1, Integer.parseInt(dateArr[2]));
    }

    /**
     * @param date initial date
     * @return next date (initial date + 1)
     * with: Hours=0, Minutes=0, Seconds=0
     */
    public static Date getTomorrowDate(Date date) {
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getDefault());
        calendar.setTime(date);
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }

//    public static void main(String[] args) {
//        Date date = Utilities.getCurrentDate();
//        System.out.println("==================");
//        System.out.println(getGregorianDate("ar", date));
//        System.out.println(getGregorianDate("en", date));
//        System.out.println("==================");
//        System.out.println(getDay("ar", date));
//        System.out.println(getDay("en", date));
//        System.out.println("==================");
//        System.out.println(getTime("en", date));
//        System.out.println(getTime("ar", date));
//        System.out.println("==================");
//        HijriDate dateToday = new HijriDate();
//        HijriDate dateRandom = new HijriDate(1442, 6, 1);
//        System.out.println(daysBetween(dateToday.getCalendar(), dateRandom.getCalendar()));
//        System.out.println("==================");
//    }
}

