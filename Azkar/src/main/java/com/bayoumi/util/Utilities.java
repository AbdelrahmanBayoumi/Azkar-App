package com.bayoumi.util;

import com.bayoumi.Launcher;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utilities {

    /**
     * @param language is the language in which time is shown
     * @param date     obj to get the time from it
     * @return the time in a specific locale.
     */
    public static String getTime(String language, Date date) {
        return new SimpleDateFormat("hh:mm:ss a", new Locale(language)).format(date);
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
     * @return the day of week from the Date based on specific locale.
     */
    public static String getDay(String language, Date date) {
        return new SimpleDateFormat("EEEE", new Locale(language)).format(date);
    }

    public static void exitProgramAction() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Long exitTime = System.currentTimeMillis();
            Logger.info("App closed - Used for "
                    + (exitTime - Launcher.startTime) + " ms\n");
        }));
        System.exit(0);
    }

    public static String formatTime24To12String(String language, String time24Format) {
        try {
            return new SimpleDateFormat("hh:mm a", new Locale(language)).format(new SimpleDateFormat("hh:mm").parse(time24Format));
        } catch (ParseException e) {
//            e.printStackTrace();
        }
        return "00:00 am";
    }

    static String formatTimeOnly(String language, String timeString) {
        return formatTime24To12String(language, timeString.substring(0, timeString.indexOf('(')).trim());
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }


    public static void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

//    public static void main(String[] args) {
//        Date date = new Date();
//        System.out.println(getHijriDate("ar"));
//        System.out.println(getHijriDate("en"));
//        System.out.println("==================");
//        System.out.println(getHijriDateString("ar"));
//        System.out.println(getHijriDateString("en"));
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
//        System.out.println(getDateAndTime("en", date));
//        System.out.println(getDateAndTime("ar", date));
//        System.out.println("==================");
//    }
}

