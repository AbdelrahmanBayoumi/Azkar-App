package com.bayoumi.util;

import com.bayoumi.Launcher;
import com.bayoumi.models.AbsoluteZekr;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
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
        } catch (ParseException ignored) {
        }
        return "00:00 am";
    }

    public static String formatTimeOnly(String language, String timeString) {
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

    public static String toUTF(String val) {
        try {
            return new String(val.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Logger.error(null, ex, AbsoluteZekr.class.getName() + ".toUTF()");
        }
        return val;
    }

    public static void main(String[] args) {
        Date date = new Date();
        System.out.println("==================");
        System.out.println(getGregorianDate("ar", date));
        System.out.println(getGregorianDate("en", date));
        System.out.println("==================");
        System.out.println(getDay("ar", date));
        System.out.println(getDay("en", date));
        System.out.println("==================");
        System.out.println(getTime("en", date));
        System.out.println(getTime("ar", date));
        System.out.println("==================");
//
        System.out.println("==================");
    }
}

